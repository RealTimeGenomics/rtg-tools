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
package com.rtg.vcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.rtg.launcher.AbstractStatistics;
import com.rtg.reference.Ploidy;
import com.rtg.report.ReportUtils;
import com.rtg.report.VelocityReportUtils;
import com.rtg.util.Histogram;
import com.rtg.util.HtmlReportHelper;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.Range;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Statistics object for variants.
 */
public class VariantStatistics extends AbstractStatistics {

  private static final int MISSING_ALLELE = -1;

  protected long mTotalFiltered = 0;
  protected long mTotalNoGenotype = 0;
  protected long mTotalPassed = 0; // Does not include filtered or no-call records.

  protected long mComplexCalled = 0;

  protected long mExcessCoverage = 0; // Only for those where over-coverage short-circuit used
  protected long mExcessHypotheses = 0; // Only for those where over-hypotheses short-circuit used
  protected long mNoHypotheses = 0; // Only for those where no-hypotheses short-circuit used

  protected boolean mShowLengthHistograms = false; // whether to collect data and display histograms for allele lengths
  protected boolean mShowAlleleCountHistograms = false; // whether to collect data and display histograms for alleles per variant site
  protected final Histogram mAltAlleleCounts = new Histogram();

  protected String mReference;

  /**
   * @param outputDirectory The base output directory to generate statistics and reports in. May be null if no statistics or reports are to be generated.
   */
  public VariantStatistics(File outputDirectory) {
    super(outputDirectory);
  }

  protected final Map<String, PerSampleVariantStatistics> mPerSampleStats = new LinkedHashMap<>();

  private List<String> mOnlySample = null; // If set, only accumulate statistics for the named samples
  private Boolean mOnlyKnown = null; // If set, only accumulate statistics for known (or novel if false) variants

  /**
   * Tell the VariantStatistics to only accumulate statistics for specified samples
   * @param samples the samples to select
   */
  public void onlySamples(String... samples) {
    mOnlySample = Arrays.asList(samples);
  }

  /**
   * Tell the VariantStatistics to select records based on known/novel
   * @param known if true, only count statistics on known variants. if false, only unknown variants. If null, ignore known/unknown status.
   */
  public void onlyKnown(Boolean known) {
    mOnlyKnown = known;
  }

  PerSampleVariantStatistics ensurePerSampleStats(String sampleName) {
    if (!mPerSampleStats.containsKey(sampleName)) {
      mPerSampleStats.put(sampleName, new PerSampleVariantStatistics());
    }
    return mPerSampleStats.get(sampleName);
  }

  protected static String percent(long numerator, long denominator) {
    final StringBuilder sb = new StringBuilder();
    if (denominator == 0) {
      sb.append("-");
    } else {
      sb.append(Utils.realFormat((double) numerator / denominator * 100, 1)).append("%");
    }
    sb.append(" (").append(numerator).append("/").append(denominator).append(")");
    return sb.toString();
  }

  protected static String divide(long numerator, long denominator) {
    final StringBuilder sb = new StringBuilder();
    if (denominator == 0) {
      sb.append("-");
    } else {
      sb.append(Utils.realFormat((double) numerator / denominator, 2));
    }
    sb.append(" (").append(numerator).append("/").append(denominator).append(")");
    return sb.toString();
  }

  public long getTotalPassed() {
    return mTotalPassed;
  }

  public long getTotalFiltered() {
    return mTotalFiltered;
  }

  public long getExcessCoverage() {
    return mExcessCoverage;
  }

  public void setExcessiveCoverageCount(final long excessCoverage) {
    mExcessCoverage = excessCoverage;
  }

  public void setExcessiveHypothesesCount(final long excessHypotheses) {
    mExcessHypotheses = excessHypotheses;
  }

  public void setNoHypothesesCount(final long noHypotheses) {
    mNoHypotheses = noHypotheses;
  }
  public void setReference(String reference) {
    mReference = reference;

  }

  /**
   * Sets whether to collect and display a histogram of allele lengths.
   * @param show show histograms
   */
  public void showLengthHistograms(boolean show) {
    mShowLengthHistograms = show;
  }

  /**
   * Sets whether to collect data and display a histogram of variant site alleles
   * @param show show histograms
   */
  public void showAlleleCountHistograms(boolean show) {
    mShowAlleleCountHistograms = show;
  }


  Pair<List<String>, List<String>> statisticsMap() {
    final List<String> names = new ArrayList<>();
    final List<String> values = new ArrayList<>();
    if (mOnlyKnown != null) {
      names.add("Identified Variants");
      values.add((mOnlyKnown ? "known" : "novel") + " only");
    }
    names.add("Failed Filters");
    values.add(Long.toString(mTotalFiltered));
    if (mExcessCoverage > 0) {
      names.add("Excessive Coverage");
      values.add(Long.toString(mExcessCoverage));
    }
    if (mExcessHypotheses > 0) {
      names.add("Excessive Hypotheses");
      values.add(Long.toString(mExcessHypotheses));
    }
    if (mNoHypotheses > 0) {
      names.add("No Hypotheses");
      values.add(Long.toString(mNoHypotheses));
    }
    names.add("Passed Filters");
    values.add(Long.toString(mTotalPassed));
    if (mTotalNoGenotype > 0) {
      names.add("No Genotypes");
      values.add(Long.toString(mTotalNoGenotype));
    }
    if (mComplexCalled > 0) {
      names.add("Complex Called");
      values.add(Long.toString(mComplexCalled));
    }
    return Pair.create(names, values);
  }

  static int maxLabel(List<String> outputNames) {
    int max = 0;
    for (String name : outputNames) {
      max = Math.max(max, name.length());
    }
    return max;
  }
  static void printCounts(List<String> names, List<String> values, StringBuilder out) {
    final int pad = Math.max(maxLabel(names) + 2, 31);
    for (int i = 0; i < names.size(); i++) {
      final String name = names.get(i);
      final String value = values.get(i);
      if (value != null) {
        out.append(StringUtils.padBetween(name, pad, ": ")).append(value).append(StringUtils.LS);
      }
    }
  }

  @Override
  public String getStatistics() {
    final StringBuilder out = new StringBuilder();
    final Pair<List<String>, List<String>> outputStatistics = statisticsMap();
    printCounts(outputStatistics.getA(), outputStatistics.getB(), out);
    if (mShowAlleleCountHistograms) {
      String spaces = "Number of Alleles            : ";
      for (int i = 0; i < mAltAlleleCounts.getLength(); i++) {
        if (mAltAlleleCounts.getValue(i) > 0) {
          out.append(spaces);
          spaces = "                               ";
          out.append(i)
          .append("\t")
          .append(mAltAlleleCounts.getValue(i))
          .append(StringUtils.LS);
        }
      }
    }

    // per sample stats
    for (final Entry<String, PerSampleVariantStatistics> e : mPerSampleStats.entrySet()) {
      if (mPerSampleStats.size() > 1) {
        out.append(StringUtils.LS).append("Sample Name: ").append(e.getKey()).append(StringUtils.LS);
      }
      e.getValue().appendStatistics(out);
      if (mShowLengthHistograms) {
        out.append(StringUtils.LS);
        e.getValue().appendHistograms(out);
      }
    }

    return out.toString();
  }

  /**
   * Add the given VCF record to the statistics
   * @param header the VCF header
   * @param rec the VCF record to tally
   */
  public void tallyVariant(VcfHeader header, VcfRecord rec) {
    tallyVariant(rec, header.getSampleNames());
  }

  /**
   * Add per sample information for the given VCF record to the statistics
   * @param rec record to add
   * @param sampleNames samples to consider
   */
  public void tallyVariant(VcfRecord rec, List<String> sampleNames) {

    if (mOnlyKnown != null) {
      final boolean currentKnown = !VcfRecord.MISSING.equals(rec.getId());
      if (currentKnown != mOnlyKnown) {
        return;
      }
    }
    if (rec.isFiltered()) {
      mTotalFiltered++;
      return;
    }
    mTotalPassed++;
    final ArrayList<String> gts = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
    if (gts == null) {
      mTotalNoGenotype++;
      return;
    }
    if (VcfUtils.isComplexScored(rec)) {
      mComplexCalled++;
    }

    final String[] alleles = VcfUtils.getAlleleStrings(rec);
    final String ref = alleles[0];

    final HashSet<Integer> altAlleles = new HashSet<>();
    final ArrayList<String> denovo = rec.getFormat(VcfUtils.FORMAT_DENOVO);
    final ArrayList<String> somaticstatus = rec.getFormat(VcfUtils.FORMAT_SOMATIC_STATUS);
    for (int i = 0; i < sampleNames.size(); i++) {
      final String sampleName = sampleNames.get(i);
      if (mOnlySample == null || mOnlySample.contains(sampleName)) {
        final String gtStr = gts.get(i);
        final int[] splitGt;
        try {
          splitGt = VcfUtils.splitGt(gtStr);
        } catch (VcfFormatException e) {
          Diagnostic.warning("VCF record GT is invalid, record: " + rec.toString());
          continue;
        }
        if (!VcfUtils.isValidGt(rec, splitGt)) {
          Diagnostic.warning("VCF record GT contains allele ID out of range, record: " + rec.toString());
          continue;
        }
        final PerSampleVariantStatistics sampleStats = ensurePerSampleStats(sampleName);
        if ((somaticstatus != null) && "2".equals(somaticstatus.get(i))) {
          sampleStats.mSomatic++;
        }
        if ((denovo != null) && "Y".equals(denovo.get(i))) {
          sampleStats.mDeNovo++;
        }
        if (VcfUtils.isPhasedGt(gtStr)) {
          sampleStats.mPhased++;
        }
        if (splitGt.length == 1) {
          final int alleleindex = splitGt[0];
          if (alleleindex == MISSING_ALLELE) {
            sampleStats.mMissingGenotype++;
          } else {
            tallyNonFiltered(ref, alleles[alleleindex], alleles[alleleindex], Ploidy.HAPLOID, sampleStats);
          }
        } else if (splitGt.length == 2) { // Regular diploid call
          if (splitGt[0] == MISSING_ALLELE || splitGt[1] == MISSING_ALLELE) {
            if (splitGt[0] == MISSING_ALLELE && splitGt[1] == MISSING_ALLELE) {
              sampleStats.mMissingGenotype++;
            } else {
              sampleStats.mPartialCalls++;
            }
          } else {
            tallyNonFiltered(ref, alleles[splitGt[0]], alleles[splitGt[1]], Ploidy.DIPLOID, sampleStats);
          }
        } else {
          Diagnostic.warning("Unexpected " + splitGt.length + " subfields in fields GT \"" + gtStr + "\" for sample " + sampleName + " in record " + rec);
          sampleStats.mPolyploidCalls++;
        }
        for (int alleleId : splitGt) {
          if (alleleId != MISSING_ALLELE) {
            altAlleles.add(alleleId);
          }
        }
      }
    }

    if (mShowAlleleCountHistograms) {
      mAltAlleleCounts.increment(altAlleles.size());
    }
  }

  protected void tallyNonFiltered(String ref, String predA, String predB, Ploidy ploidy, PerSampleVariantStatistics sampleStats) {
    if (ref.equals(predA) && ref.equals(predB)) { // Identity
      sampleStats.mTotalUnchanged++;
    } else {
      tallyNonIdentity(ref, predA, predB, ploidy, sampleStats);
    }
  }

  protected void tallyNonIdentity(String ref, String predA, String predB, Ploidy ploidy, PerSampleVariantStatistics sampleStats) {
    //System.err.println("Ref:" + ref + " Call:" + predA + "/" + predB);
    String refAtrimmed = ref;
    String predAtrimmed = predA;
    String refBtrimmed = ref;
    String predBtrimmed = predB;
    if (ref.length() > 1) {
      if (ref.length() == predA.length() && !ref.equals(predA)) {
        final Range range = mnpUniquenessRange(ref, predA);
        if (range.getLength() < ref.length()) {
          refAtrimmed = ref.substring(range.getStart(), range.getStart() + range.getLength());
          predAtrimmed = predA.substring(range.getStart(), range.getStart() + range.getLength());
        }
      }
      if (ref.length() == predB.length() && !ref.equals(predB)) {
        final Range range = mnpUniquenessRange(ref, predB);
        if (range.getLength() < ref.length()) {
          refBtrimmed = ref.substring(range.getStart(), range.getStart() + range.getLength());
          predBtrimmed = predB.substring(range.getStart(), range.getStart() + range.getLength());
        }
      }
    }
    tallyNonIdentity(refAtrimmed, refBtrimmed, predAtrimmed, predBtrimmed, VariantType.getType(refAtrimmed, predAtrimmed), VariantType.getType(refBtrimmed, predBtrimmed), ploidy, sampleStats);
  }

  protected void tallyNonIdentity(String refA, String refB, String predA, String predB, VariantType typeA, VariantType typeB, Ploidy ploidy, PerSampleVariantStatistics sampleStats) {
    // Based on ref and preds determine:
    //   haploid vs heterozygous vs homozygous
    //   Kind of variant: SNP, MNP, INS, DEL, INDEL, COMPLEX
    //   Transition (A <-> G) (C <-> T) vs Transversion (all others) for SNP's only

    final boolean heterozygous;
    if (ploidy == Ploidy.HAPLOID) {
      sampleStats.mHaploid++;
      heterozygous = false;
    } else {
      heterozygous = !predA.equals(predB);
      if (heterozygous) {
        sampleStats.mHeterozygous++;
      } else {
        sampleStats.mHomozygous++;
      }
    }
    final VariantType precedence = VariantType.getPrecedence(typeA, typeB);
    if (precedence == VariantType.SNP) {
      tallyTransitionTransversionRatio(refA, predA, typeA, sampleStats);
      if (ploidy != Ploidy.HAPLOID) {
        tallyTransitionTransversionRatio(refB, predB, typeB, sampleStats);
      }
    }
    tally(precedence, heterozygous, ploidy, sampleStats);

    if (mShowLengthHistograms) {
      tallyLength(typeA, refA, predA, sampleStats);
      if (ploidy != Ploidy.HAPLOID) {
        tallyLength(typeB, refB, predB, sampleStats);
      }
    }
  }

  private void tallyTransitionTransversionRatio(String ref, String pred, VariantType type, PerSampleVariantStatistics sampleStats) {
    if (type == VariantType.SNP) {
      final boolean transition = "AG".contains(ref) && "AG".contains(pred) || "CT".contains(ref) && "CT".contains(pred);
      if (transition) {
        sampleStats.mTransitions++;
      } else {
        sampleStats.mTransversions++;
      }
    }
  }

  private void tallyLength(VariantType alleleType, String ref, String pred, PerSampleVariantStatistics sampleStats) {
    if (!alleleType.isNonVariant() && !alleleType.isSvType()) {
      final int alleleLength;
      switch (alleleType) {
        case UNCHANGED:
        case SNP:
        case MNP:
          alleleLength = pred.length();
          break;
        default:
          alleleLength = Math.abs(pred.length() - ref.length());
          break;
      }
      sampleStats.mAlleleLengths[alleleType.ordinal()].increment(alleleLength);
    }
  }

  /**
   * Also know as <code>isActuallyASnp</code>. Only works on input with length greater than 1
   * @param ref the reference bases
   * @param pred the prediction bases
   * @return range representing the portion that is unique (useful for determining if a MNP is actually a SNP)
   */
  private static Range mnpUniquenessRange(String ref, String pred) {
    assert ref.length() == pred.length() && ref.length() > 1 && !ref.equals(pred);
    int diffStart = 0;
    int diffEnd = ref.length() - 1;
    while (ref.charAt(diffStart) == pred.charAt(diffStart)) {
      diffStart++;
    }
    while (ref.charAt(diffEnd) == pred.charAt(diffEnd)) {
      diffEnd--;
    }
    return new Range(diffStart, diffEnd + 1);
  }

  private void tally(VariantType type, boolean heterozygous, Ploidy ploidy, PerSampleVariantStatistics sampleStats) {
    if (ploidy == Ploidy.HAPLOID) {
      switch (type) {
        case SNP:
          sampleStats.mHaploidSnps++;
          sampleStats.mTotalSnps++;
          break;
        case MNP:
          sampleStats.mHaploidMnps++;
          sampleStats.mTotalMnps++;
          break;
        case INSERTION:
          sampleStats.mHaploidInsertions++;
          sampleStats.mTotalInsertions++;
          break;
        case DELETION:
          sampleStats.mHaploidDeletions++;
          sampleStats.mTotalDeletions++;
          break;
        case INDEL:
          sampleStats.mHaploidIndels++;
          sampleStats.mTotalIndels++;
          break;
        case SV_BREAKEND:
          sampleStats.mHaploidBreakends++;
          sampleStats.mTotalBreakends++;
          break;
        case SV_SYMBOLIC:
          sampleStats.mHaploidSymbolicSvs++;
          sampleStats.mTotalSymbolicSvs++;
          break;
        default:
          break;
      }
    } else if (heterozygous) {
      switch (type) {
        case SNP:
          sampleStats.mHeterozygousSnps++;
          sampleStats.mTotalSnps++;
          break;
        case MNP:
          sampleStats.mHeterozygousMnps++;
          sampleStats.mTotalMnps++;
          break;
        case INSERTION:
          sampleStats.mHeterozygousInsertions++;
          sampleStats.mTotalInsertions++;
          break;
        case DELETION:
          sampleStats.mHeterozygousDeletions++;
          sampleStats.mTotalDeletions++;
          break;
        case INDEL:
          sampleStats.mHeterozygousIndels++;
          sampleStats.mTotalIndels++;
          break;
        case SV_BREAKEND:
          sampleStats.mHeterozygousBreakends++;
          sampleStats.mTotalBreakends++;
          break;
        case SV_SYMBOLIC:
          sampleStats.mHeterozygousSymbolicSvs++;
          sampleStats.mTotalSymbolicSvs++;
          break;
        default:
          break;
      }
    } else {
      switch (type) {
        case SNP:
          sampleStats.mHomozygousSnps++;
          sampleStats.mTotalSnps++;
          break;
        case MNP:
          sampleStats.mHomozygousMnps++;
          sampleStats.mTotalMnps++;
          break;
        case INSERTION:
          sampleStats.mHomozygousInsertions++;
          sampleStats.mTotalInsertions++;
          break;
        case DELETION:
          sampleStats.mHomozygousDeletions++;
          sampleStats.mTotalDeletions++;
          break;
        case INDEL:
          sampleStats.mHomozygousIndels++;
          sampleStats.mTotalIndels++;
          break;
        case SV_BREAKEND:
          sampleStats.mHomozygousBreakends++;
          sampleStats.mTotalBreakends++;
          break;
        case SV_SYMBOLIC:
          sampleStats.mHomozygousSymbolicSvs++;
          sampleStats.mTotalSymbolicSvs++;
          break;
        default:
          break;
      }
    }
  }

  Pair<List<String>, Map<String, List<String>>> perSampleMap() {
    List<String> names = new ArrayList<>();
    final Map<String, List<String>> values = new LinkedHashMap<>();
    // per sample stats
    int length = 0;
    for (final Entry<String, PerSampleVariantStatistics> e : mPerSampleStats.entrySet()) {
      final Pair<List<String>, List<String>> statistics = e.getValue().getStatistics();
      names = statistics.getA();
      final List<String> vals = statistics.getB();
      values.put(e.getKey(), vals);
      length = names.size();
    }

    for (int i = length - 1; i >= 0; i--) {
      boolean present = false;
      for (List<String> stats : values.values()) {
        if (stats.get(i) != null) {
          present = true;
          break;
        }
      }
      if (!present) {
        names.remove(i);
        for (final List<String> stats : values.values()) {
          stats.remove(i);
        }
      }
    }
    return Pair.create(names, values);
  }
  @Override
  public void generateReport() throws IOException {
    final HtmlReportHelper helper = getReportHelper();
    helper.copyResources(ReportUtils.TEMPLATE_DIR + "/rtg_logo.png", ReportUtils.TEMPLATE_DIR + "/rtg.css"); //copy resources up here to create the report files sub dir as well
    // write html
    final HashMap<String, Object> replBody = new HashMap<>();
    final Pair<List<String>, List<String>> outputStatistics = statisticsMap();
    replBody.put("commandLine", CommandLine.getCommandLine());
    replBody.put("variantCountNames", outputStatistics.getA());
    replBody.put("variantCountValues", outputStatistics.getB());
    final Pair<List<String>, Map<String, List<String>>> samples = perSampleMap();
    replBody.put("perSampleNames", samples.getA());
    replBody.put("perSampleValues", samples.getB());
    replBody.put("reference", mReference);
    final String body = VelocityReportUtils.processTemplate("variant.vm", replBody);


    final String coverage = VelocityReportUtils.wrapDefaultTemplate(body, "Variant", helper);

    FileUtils.stringToFile(coverage, helper.getReportFile());
  }
}
