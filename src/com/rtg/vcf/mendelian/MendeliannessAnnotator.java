/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rtg.vcf.mendelian;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.reference.Ploidy;
import com.rtg.reference.Sex;
import com.rtg.reference.SexMemo;
import com.rtg.relation.Family;
import com.rtg.relation.GenomeRelationships;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.vcf.VcfAnnotator;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Check calls in a VCF obey rules of Mendelian inheritance.
 */
@TestClass("com.rtg.vcf.mendelian.MendeliannessCheckerTest")
public final class MendeliannessAnnotator implements VcfAnnotator {


  private static final String FORMAT_PLOIDY = "MCP";
  private static final String INFO_VIOLATION = "MCV";

  private static final String[] REFERENCE_GENOTYPES = {VcfRecord.MISSING, "0", "0/0", "0/0/0", "0/0/0/0"};

  private static final NumberFormat NF = NumberFormat.getNumberInstance();
  static {
    NF.setMinimumFractionDigits(2);
    NF.setMaximumFractionDigits(2);
  }


  private final Set<Family> mFamilies;
  private final SexMemo mSexMemo;
  private final GenotypeProportions mAggregate;

  private final boolean mStrictMissingPloidy; // If true, grumble when the ploidy of missing values is not what is expected. Only of interest for checking RTG output really.
  private final boolean mMissingAsRef;
  private final boolean mAllowHomozygousAsHaploid;
  private final boolean mAnnotate; // If true, annotate records with mendelian violation information


  private VcfHeader mHeader;
  private Sex[] mSampleToSex;
  private TrioConcordance[] mTrioCounts;

  private String mTemplateName = "";
  private boolean mLastWasInconsistent = false;

  private long mTotalRecords;
  private long mNoGtRecords;
  private long mStrangeChildPloidyRecords;
  private long mNonRefFamilyRecords;
  private long mBadMendelianRecords;
  private long mBadPloidyRecords;


  // Updated as we see each chromosome throughout the VCF file
  private int[] mExpectedPloidy;
  private SexMemo.EffectivePloidyList[] mParPloidy;

  /**
   * Create the annotator
   * @param families the families for which mendelian constraints should be checked
   * @param sexMemo specifies expected allele counts for each chromosome for each sex
   * @param aggregate if non null, will accumulate aggregate genotype statistics
   * @param annotate if true, perform VCF record annotation (otherwise just compute statistics)
   * @param lenient if true, allow homozygous diploid to slip through as haploid, and treat missing genotypes as reference
   * @param strictMissingPloidy if true, grumble when the ploidy of missing values is not what is expected. Only of interest for checking RTG output really because no-one else gets it right.
   */
  public MendeliannessAnnotator(Set<Family> families, SexMemo sexMemo, GenotypeProportions aggregate, boolean annotate, boolean lenient, boolean strictMissingPloidy) {
    if (families == null || families.size() == 0) {
      throw new IllegalArgumentException("At least one family is required");
    }
    mFamilies = families;
    mSexMemo = sexMemo;
    mAggregate = aggregate;
    mMissingAsRef = lenient;
    mAllowHomozygousAsHaploid = lenient;
    mStrictMissingPloidy = strictMissingPloidy;
    mAnnotate = annotate;
  }


  private static int expectedPloidy(final Ploidy ploidy) {
    switch (ploidy) {
      case NONE:
        return 0;
      case HAPLOID:
        return 1;
      default:
        return 2;
    }
  }

  private int expectedPloidy(SequenceNameLocus locus, int sample) {
    if (mParPloidy[sample] != null) {
      final int pos = locus.getStart() + locus.getLength() / 2;
      for (Pair<RegionRestriction, Ploidy> region : mParPloidy[sample]) {
        if (region.getA().contains(locus.getSequenceName(), pos)) {
          return expectedPloidy(region.getB());
        }
      }
    }
    return mExpectedPloidy[sample];
  }

  private static int calledPloidy(final String vcfGenotype) {
    int c = 1;
    for (int k = 1; k < vcfGenotype.length(); ++k) {
      final char v = vcfGenotype.charAt(k);
      if (v == VcfUtils.PHASED_SEPARATOR || v == VcfUtils.UNPHASED_SEPARATOR) {
        ++c;
      }
    }
    return c;
  }

  private static boolean isHomozygousDiploid(final String vcfGenotype) {
    final int[] p = VcfUtils.splitGt(vcfGenotype);
    return p.length == 2 && p[0] == p[1];
  }

  private static String ploidyMessage(final int n) {
    switch (n) {
      case 0:
        return "none";
      case 1:
        return "haploid";
      case 2:
        return "diploid";
      default:
        return "polyploid";
    }
  }

  // If we are in lenient mode, fill in empty values with the appropriate reference call
  private String referenceFix(SequenceNameLocus locus, final List<String> calls, final int column) {
    final String vcfGenotype = calls.get(column);
    if (mMissingAsRef && VcfUtils.isMissingGt(vcfGenotype)) {
      return REFERENCE_GENOTYPES[expectedPloidy(locus, column)];
    }
    return vcfGenotype;
  }


  private void updateExpectedAlleleCounts(final SexMemo sexMemo, String templateName) {
    // Update expected allele counts for each sample, depends on sex and reference.txt
    for (int k = 0; k < mSampleToSex.length; ++k) {
      if (mSampleToSex[k] != null) {
        mExpectedPloidy[k] = expectedPloidy(sexMemo.getEffectivePloidy(mSampleToSex[k], templateName));
        mParPloidy[k] = sexMemo.getParEffectivePloidy(mSampleToSex[k], templateName);
      }
    }
  }

  // Child is diploid
  // If it is homozygous, both father and mother must contain a copy of the allele
  // If it is heterozygous, it must be possible to assign one allele to each parent
  static boolean isBadDiploidChild(final Genotype fatherCall, final Genotype motherCall, final Genotype childCall) {
    assert childCall.length() == 2;
    boolean badDiploid = false;
    final int childAllele1 = childCall.get(0);
    final int childAllele2 = childCall.get(1);
    if (childAllele1 == childAllele2) {
      if (!fatherCall.contains(childAllele1) || !motherCall.contains(childAllele1)) {
        badDiploid = true;
      }
    } else {
      // Two possible paths, one allele must be from each parent
      if (!((fatherCall.contains(childAllele1) && motherCall.contains(childAllele2))
         || (motherCall.contains(childAllele1) && fatherCall.contains(childAllele2)))) {
        badDiploid = true;
      }
    }
    return badDiploid;
  }

  // Child is haploid, must inherit its allele from exactly one parent.
  // If the sequence is diploid in one parent, it should be from that parent.
  // If the sequence is none in one parent, it should come from the other parent.
  static boolean isBadHaploidChild(final Genotype fatherCall, final Genotype motherCall, final Genotype childCall) {
    assert childCall.length() == 1;
    boolean badhaploid = false;
    final int childAllele = childCall.get(0);
    if (motherCall.length() > 1) {
      if (!motherCall.contains(childAllele)) {
        badhaploid = true;
      }
    } else if (fatherCall.length() > 1) {
      if (!fatherCall.contains(childAllele)) {
        badhaploid = true;
      }
    } else if (!fatherCall.contains(childAllele) && !motherCall.contains(childAllele)) {
      // Neither parent is diploid
      badhaploid = true;
    }
    return badhaploid;
  }

  // Returns true if any of the calls have unexpected call ploidy
  private boolean hasBadCallPloidy(VcfRecord rec) {
    final List<String> calls = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
    if (mSampleToSex.length != calls.size()) {
      throw new NoTalkbackSlimException("Number of samples in VCF record is inconsistent with previous records: " + rec.toString());
    }
    boolean badPloidy = false;
    for (int k = 0; k < calls.size(); ++k) {
      if (mSampleToSex[k] != null) {
        final String call = calls.get(k);
        if (VcfUtils.isNonMissingGt(call) || mStrictMissingPloidy) {
          final int expectedPloidy = expectedPloidy(rec, k);
          final int calledPloidy = expectedPloidy == 0 && VcfRecord.MISSING.equals(call) ? 0 : calledPloidy(call);
          if (calledPloidy != expectedPloidy) {
            if (mAllowHomozygousAsHaploid && expectedPloidy == 1 && isHomozygousDiploid(call)) {
              // User explicitly asked that homozygous diploid can slip through as haploid
              continue;
            }
            if (mAnnotate) {
              rec.setFormatAndSample(FORMAT_PLOIDY, ploidyMessage(expectedPloidy), k);
            }
            badPloidy = true;
          }
        }
      }
    }
    if (badPloidy) {
      ++mBadPloidyRecords;
    }
    return badPloidy;
  }

  // Returns true if this trio genotype has bad mendelianness
  private boolean hasBadMendelianness(int childIndex, Genotype fatherGt, Genotype motherGt, Genotype childGt) {
    //aggregate stuff
    if (mAggregate != null) {
      mAggregate.addRecord(fatherGt, motherGt, childGt);
    }
    mTrioCounts[childIndex].add(fatherGt, motherGt, childGt);
    final boolean badMendelian = ((childGt.length() == 1) && isBadHaploidChild(fatherGt, motherGt, childGt))
      || ((childGt.length() == 2) && isBadDiploidChild(fatherGt, motherGt, childGt));
    mTrioCounts[childIndex].addTrioStatus(badMendelian);
    return badMendelian;
  }

  // Returns true if any of the testable trio genotypes has bad mendelianness
  private boolean hasBadMendelianness(VcfRecord rec) {
    final List<String> calls = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
    boolean nonMendelian = false;
    boolean nonRef = false;
    boolean strangePloidy = false;

    for (Family f : mFamilies) { // Check all families, but violation counts are recorded at a per-record level

      final int fatherIndex = mHeader.getSampleIndex(f.getFather());
      final int motherIndex = mHeader.getSampleIndex(f.getMother());
      final String fatherCall = referenceFix(rec, calls, fatherIndex);
      final String motherCall = referenceFix(rec, calls, motherIndex);

      for (String child : f.getChildren()) {
        final int childIndex = mHeader.getSampleIndex(child);
        final String childCall = referenceFix(rec, calls, childIndex);

        // If there is no child call for this trio then we can say nothing about the
        // Mendelianness of the parents, and nothing of the child phasing
        if (VcfUtils.isMissingGt(childCall)) {
          continue;
        }

        // If none of the individuals are variant, skip this call and dont count in the total.
        if (VcfUtils.isNonVariantGt(fatherCall) && VcfUtils.isNonVariantGt(motherCall) && VcfUtils.isNonVariantGt(childCall)) {
          continue;
        }
        nonRef = true;

        final Genotype fatherGt = new Genotype(VcfUtils.splitGt(fatherCall));
        final Genotype motherGt = new Genotype(VcfUtils.splitGt(motherCall));
        final Genotype childGt = new Genotype(VcfUtils.splitGt(childCall));

        // Cases of incorrect ploidy in calls have already been removed by hasBadCallPloidy.
        if (childGt.length() == 0 || childGt.length() > 2) {
          if (mAnnotate) {
            rec.addInfo(INFO_VIOLATION, child + ":" + fatherCall + "+" + motherCall + "->" + childCall);
          }
          strangePloidy = true;
        } else if (hasBadMendelianness(childIndex, fatherGt, motherGt, childGt)) {
          if (mAnnotate) {
            rec.addInfo(INFO_VIOLATION, child + ":" + fatherCall + "+" + motherCall + "->" + childCall);
          }
          nonMendelian = true;
        }
      }
    }

    if (nonRef) {
      ++mNonRefFamilyRecords;
    }
    if (strangePloidy) {
      ++mStrangeChildPloidyRecords;
    }
    if (nonMendelian) {
      ++mBadMendelianRecords;
    }
    return nonMendelian;
  }


  private void initLookups() {
    mExpectedPloidy = new int[mHeader.getNumberOfSamples()];
    mParPloidy = new SexMemo.EffectivePloidyList[mHeader.getNumberOfSamples()];
    mSampleToSex = new Sex[mHeader.getNumberOfSamples()];
    final GenomeRelationships pedigree = mFamilies.iterator().next().pedigree();
    for (final String sampleName : mHeader.getSampleNames()) {
      final Sex s = pedigree.getSex(sampleName);
      if (s != null && s != Sex.EITHER) {
        mSampleToSex[mHeader.getSampleIndex(sampleName)] = s;
      }
    }
    mTrioCounts = new TrioConcordance[mHeader.getNumberOfSamples()];
    for (Family f : mFamilies) {
      for (String child : f.getChildren()) {
        mTrioCounts[mHeader.getSampleIndex(child)] = new TrioConcordance(child, f.getFather(), f.getMother());
      }
    }
  }

  private void checkSampleOk(final String sample, final String label) {
    if (!mHeader.getSampleNames().contains(sample)) {
      throw new NoTalkbackSlimException("Pedigree " + label + " specification '" + sample + "' not contained in samples");
    }
    if (mSampleToSex[mHeader.getSampleIndex(sample)] == null) {
      throw new NoTalkbackSlimException("No sex specified for sample '" + sample + "'");
    }
  }


  private void checkHeader() {
    if (!mHeader.getFormatLines().stream().anyMatch((FormatField t) -> t.getId().equals(VcfUtils.FORMAT_GENOTYPE))) {
      throw new NoTalkbackSlimException("Supplied VCF does not contain GT FORMAT fields");
    }
    for (Family f : mFamilies) {
      checkSampleOk(f.getFather(), "father");
      checkSampleOk(f.getMother(), "mother");
      for (String child : f.getChildren()) {
        checkSampleOk(child, "child");
      }
    }
  }

  @Override
  public void updateHeader(VcfHeader header) {
    mHeader = header;
    initLookups();

    checkHeader();

    if (mAnnotate) {
      header.ensureContains(new InfoField(INFO_VIOLATION, MetaType.STRING, new VcfNumber("."), "Variant violates mendelian inheritance constraints"));
      header.ensureContains(new FormatField(FORMAT_PLOIDY, MetaType.STRING, new VcfNumber("."), "Describes the expected genotype ploidy in cases where the given genotype does not match the expected ploidy"));
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    if (!mTemplateName.equals(rec.getSequenceName())) {
      mTemplateName = rec.getSequenceName();
      updateExpectedAlleleCounts(mSexMemo, mTemplateName);
    }
    if (!rec.hasFormat(VcfUtils.FORMAT_GENOTYPE)) {
      throw new NoTalkbackSlimException("Record does not contain GT field: " + rec.toString());
    }
    ++mTotalRecords;
    mLastWasInconsistent = hasBadCallPloidy(rec) | hasBadMendelianness(rec);
  }

  /**
   * @return true if the last record to pass through <code>annotate()</code> was inconsistent
   */
  public boolean wasLastInconsistent() {
    return mLastWasInconsistent;
  }

  /**
   * Print summary statistics.
   * @param out destination for summary stats.
   * @param minVariants minimum number of variants needed to identify inconsistent samples
   * @param minConcordance minimum degree of concordance for consistent samples
   */
  public void printInconsistentSamples(final PrintStream out, int minVariants, double minConcordance) {
    //StringBuillder warnings = new StringBuilder();
    for (TrioConcordance tc : mTrioCounts) {
      if (tc != null) {
        out.println(tc.toString());
        tc.check(minVariants, minConcordance, out);
      }
    }
  }

  /**
   * Print summary statistics.
   * @param out destination for summary stats.
   */
  public void printSummary(final PrintStream out) {
    assert mBadPloidyRecords <= mTotalRecords;
    final long goodPloidy = mTotalRecords - mBadPloidyRecords;
    assert mBadMendelianRecords <= goodPloidy;


    if (mTotalRecords == 0) {
      out.println("No variants processed");
    } else {
      out.println(mBadPloidyRecords + "/" + mTotalRecords + " (" + NF.format(100.0 * mBadPloidyRecords / mTotalRecords) + "%) records did not conform to expected call ploidy");
      if (mNonRefFamilyRecords < mTotalRecords) {
        out.println(mNonRefFamilyRecords + "/" + mTotalRecords + " (" + NF.format(100.0 * mNonRefFamilyRecords / mTotalRecords) + "%) records were variant in at least 1 family member and checked for Mendelian constraints");
      }
      if (mNonRefFamilyRecords > 0) {
        out.println(mBadMendelianRecords + "/" + mNonRefFamilyRecords + " (" + NF.format(100.0 * mBadMendelianRecords / mNonRefFamilyRecords) + "%) records contained a violation of Mendelian constraints");
      }
      if (mStrangeChildPloidyRecords > 0) {
        out.println(mStrangeChildPloidyRecords + "/" + mNonRefFamilyRecords + " (" + NF.format(100.0 * mStrangeChildPloidyRecords / mNonRefFamilyRecords) + "%) records were not adequately checked due to a child call that was neither haploid nor diploid");
      }
    }
  }



}
