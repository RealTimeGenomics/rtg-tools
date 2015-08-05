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
package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.EnumSet;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;

/**
 * Creates typical vcfeval output files with separate VCF files and ROC files.
 */
class DefaultEvalSynchronizer extends EvalSynchronizer {

  private static final String FN_FILE_NAME = "fn.vcf";
  private static final String FP_FILE_NAME = "fp.vcf";
  private static final String TP_FILE_NAME = "tp.vcf";
  private static final String TPBASE_FILE_NAME = "tp-baseline.vcf";

  static final String FULL_ROC_FILE = "weighted_roc.tsv";
  static final String HOMOZYGOUS_FILE = "homozygous_roc.tsv";
  static final String HETEROZYGOUS_FILE = "heterozygous_roc.tsv";
  static final String SIMPLE_FILE = "simple_roc.tsv";
  static final String COMPLEX_FILE = "complex_roc.tsv";
  private static final String HOMOZYGOUS_SIMPLE_FILE = "homozygous_simple_roc.tsv";
  private static final String HOMOZYGOUS_COMPLEX_FILE = "homozygous_complex_roc.tsv";
  private static final String HETEROZYGOUS_SIMPLE_FILE = "heterozygous_simple_roc.tsv";
  private static final String HETEROZYGOUS_COMPLEX_FILE = "heterozygous_complex_roc.tsv";

  private final VcfWriter mTpCalls;
  private final VcfWriter mTpBase;
  private final VcfWriter mFp;
  private final VcfWriter mFn;
  protected final RocContainer mRoc;
  private final RocSortValueExtractor mRocExtractor;
  private final int mCallSampleNo;
  private final boolean mZip;
  private final boolean mSlope;
  private final boolean mRtgStats;
  private final File mOutDir;
  int mBaselineTruePositives = 0;
  int mCallTruePositives = 0;
  int mFalseNegatives = 0;
  int mFalsePositives = 0;
  private int mUnphasable = 0;
  private int mMisPhasings = 0;
  private int mCorrectPhasings = 0;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param callsSampleName the name of the sample used in the calls
   * @param extractor extractor of ROC scores
   * @param outdir the outdir directory into which result files are written
   * @param zip true if outdir files should be compressed
   * @param outputTpBase true if the baseline true positive file should be written
   * @param slope true to outdir ROC slope files
   * @param rtgStats true to outdir additional ROC curves for RTG specific attributes
   * @throws IOException if there is a problem opening outdir files
   */
  DefaultEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                          String callsSampleName, RocSortValueExtractor extractor,
                          File outdir, boolean zip, boolean outputTpBase, boolean slope, boolean rtgStats) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);
    final RocContainer roc = new RocContainer(extractor.getSortOrder(), extractor.toString());
    roc.addFilter(RocFilter.ALL, new File(outdir, FULL_ROC_FILE));
    roc.addFilter(RocFilter.HETEROZYGOUS, new File(outdir, HETEROZYGOUS_FILE));
    roc.addFilter(RocFilter.HOMOZYGOUS, new File(outdir, HOMOZYGOUS_FILE));
    if (rtgStats) {
      roc.addFilter(RocFilter.SIMPLE, new File(outdir, SIMPLE_FILE));
      roc.addFilter(RocFilter.COMPLEX, new File(outdir, COMPLEX_FILE));
      roc.addFilter(RocFilter.HETEROZYGOUS_SIMPLE, new File(outdir, HETEROZYGOUS_SIMPLE_FILE));
      roc.addFilter(RocFilter.HETEROZYGOUS_COMPLEX, new File(outdir, HETEROZYGOUS_COMPLEX_FILE));
      roc.addFilter(RocFilter.HOMOZYGOUS_SIMPLE, new File(outdir, HOMOZYGOUS_SIMPLE_FILE));
      roc.addFilter(RocFilter.HOMOZYGOUS_COMPLEX, new File(outdir, HOMOZYGOUS_COMPLEX_FILE));
    }
    mRoc = roc;
    mRocExtractor = extractor;
    mZip = zip;
    mSlope = slope;
    mRtgStats = rtgStats;
    mOutDir = outdir;
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final File tpFile = new File(outdir, TP_FILE_NAME + zipExt);
    final File fpFile = new File(outdir, FP_FILE_NAME + zipExt);
    final File fnFile = new File(outdir, FN_FILE_NAME + zipExt);
    final File tpBaseFile = outputTpBase ? new File(outdir, TPBASE_FILE_NAME + zipExt) : null;
    mTpCalls = new VcfWriter(variants.calledHeader(), tpFile, null, zip, true);
    mTpBase = outputTpBase ? new VcfWriter(variants.baseLineHeader(), tpBaseFile, null, zip, true) : null;
    mFp = new VcfWriter(variants.calledHeader(), fpFile, null, zip, true);
    mFn = new VcfWriter(variants.baseLineHeader(), fnFile, null, zip, true);
    mCallSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");
  }

  @Override
  protected void writeInternal(String sequenceName, Collection<? extends VariantId> baseline, Collection<? extends VariantId> calls) throws IOException {
    final ReferenceRanges<String> subRanges = mRanges.forSequence(sequenceName);
    writeVariants(true, calls, subRanges);
    writeVariants(false, baseline, subRanges);
    Diagnostic.developerLog("Number of baseline variants processed: " + (mBaselineTruePositives + mFalseNegatives));
  }

  @Override
  protected void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
    mMisPhasings += misPhasings;
    mUnphasable += unphasable;
    mCorrectPhasings += correctPhasings;
  }

  int getUnphasable() {
    return mUnphasable;
  }

  int getMisPhasings() {
    return mMisPhasings;
  }

  int getCorrectPhasings() {
    return mCorrectPhasings;
  }


  /**
   * Dump all of the variants in a Collection to an output stream
   * @param calls true if the set we are processing is calls, false if baseline
   * @param variants a collection of variants
   * @param subRanges the regions being processed on the current sequence
   * @throws IOException IO exceptions require too many comments.
   */
  private void writeVariants(boolean calls, Collection<? extends VariantId> variants, ReferenceRanges<String> subRanges) throws IOException {
    if (variants.size() == 0) {
      return;
    }
    final VcfWriter positive = calls ? mTpCalls : mTpBase;
    final VcfWriter negative = calls ? mFp : mFn;
    try (final VcfReader vcfReader = VcfReader.openVcfReader(calls ? mCallsFile : mBaseLineFile, subRanges)) {
      int id = 0;
      for (final VariantId v : variants) {
        if (v instanceof SkippedVariant) {
          continue;
        }
        final boolean included = v instanceof OrientedVariant;

        final Classification status = calls
          ? (included ? Classification.TP_CALL : Classification.FP)
          : (included ? Classification.TP_BASE : Classification.FN);
        switch (status) {
          case TP_BASE:
            mBaselineTruePositives++;
            break;
          case TP_CALL:
            mCallTruePositives++;
            break;
          case FN:
            mFalseNegatives++;
            break;
          case FP:
            mFalsePositives++;
            break;
          default:
            break;
        }

        final Variant dv = included ? ((OrientedVariant) v).variant() : (Variant) v;
        final VcfWriter out = included ? positive : negative;
        VcfRecord rec = null;
        while (vcfReader.hasNext()) {
          final VcfRecord r = vcfReader.next();
          id++;

          if (id == dv.getId()) {
            rec = r;
            break;
          }
        }

        if (rec != null) {

          if (calls) { // Add to ROC container
            final EnumSet<RocFilter> filters = EnumSet.noneOf(RocFilter.class);
            for (final RocFilter filter : RocFilter.values()) {
              if (filter.accept(rec, mCallSampleNo)) {
                filters.add(filter);
              }
            }
            double score = Double.NaN;
            try {
              score = mRocExtractor.getSortValue(rec, mCallSampleNo);
            } catch (IndexOutOfBoundsException ignored) {
            }
            if (included) {
              final OrientedVariant ov = (OrientedVariant) v;
              mRoc.addRocLine(score, ov.getWeight(), filters);
            } else {
              mRoc.addRocLine(score, 0, filters);
            }
          }

          if (out != null) {
            out.write(rec);
          }
        } else {
          throw new SlimException("Variant object \"" + v.toString() + "\"" + " does not have a corresponding VCF record in given reader");
        }
      }
    }
  }

  @Override
  void finish() throws IOException {
    super.finish();
    if (mRoc.getNumberOfIgnoredVariants() > 0) {
      final String rocLabel = mRocExtractor.toString();
      Diagnostic.warning("There were " + mRoc.getNumberOfIgnoredVariants() + " variants not included in ROC data files due to missing or invalid " + rocLabel + " values.");
    }
    Diagnostic.developerLog("Writing ROC");
    mRoc.writeRocs(mBaselineTruePositives + mFalseNegatives, mZip);
    if (mSlope) {
      produceSlopeFiles(mRtgStats);
    }
    writePhasingInfo();

    mRoc.writeSummary(new File(mOutDir, CommonFlags.SUMMARY_FILE), mBaselineTruePositives, mFalsePositives, mFalseNegatives);

  }

  private void writePhasingInfo() throws IOException {
    final File phasingFile = new File(mOutDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + getCorrectPhasings() + StringUtils.LS
      + "Incorrect phasings: " + getMisPhasings() + StringUtils.LS
      + "Unresolvable phasings: " + getUnphasable() + StringUtils.LS, phasingFile);
  }

  private void produceSlopeFiles(boolean rtgStats) throws IOException {
    final String suffix = mZip ? FileUtils.GZ_SUFFIX : "";
    final File fullFile = new File(mOutDir, FULL_ROC_FILE + suffix);
    produceSlopeFile(fullFile, new File(mOutDir, "weighted_slope.tsv" + suffix));
    final File heteroFile = new File(mOutDir, HETEROZYGOUS_FILE + suffix);
    produceSlopeFile(heteroFile, new File(mOutDir, "heterozygous_slope.tsv" + suffix));
    final File homoFile = new File(mOutDir, HOMOZYGOUS_FILE + suffix);
    produceSlopeFile(homoFile, new File(mOutDir, "homozygous_slope.tsv" + suffix));
    if (rtgStats) {
      final File simpleFile = new File(mOutDir, SIMPLE_FILE + suffix);
      produceSlopeFile(simpleFile, new File(mOutDir, "simple_slope.tsv" + suffix));
      final File complexFile = new File(mOutDir, COMPLEX_FILE + suffix);
      produceSlopeFile(complexFile, new File(mOutDir, "complex_slope.tsv" + suffix));
      final File heteroSimpleFile = new File(mOutDir, HETEROZYGOUS_SIMPLE_FILE + suffix);
      produceSlopeFile(heteroSimpleFile, new File(mOutDir, "heterozygous_simple_slope.tsv" + suffix));
      final File heteroComplexFile = new File(mOutDir, HETEROZYGOUS_COMPLEX_FILE + suffix);
      produceSlopeFile(heteroComplexFile, new File(mOutDir, "heterozygous_complex_slope.tsv" + suffix));
      final File homoSimpleFile = new File(mOutDir, HOMOZYGOUS_SIMPLE_FILE + suffix);
      produceSlopeFile(homoSimpleFile, new File(mOutDir, "homozygous_simple_slope.tsv" + suffix));
      final File homoComplexFile = new File(mOutDir, HOMOZYGOUS_COMPLEX_FILE + suffix);
      produceSlopeFile(homoComplexFile, new File(mOutDir, "homozygous_complex_slope.tsv" + suffix));
    }
  }

  private void produceSlopeFile(File input, File output) throws IOException {
    if (input.exists() && input.length() > 0) {
      try (final PrintStream printOut = new PrintStream(FileUtils.createOutputStream(output, mZip));
           final InputStream in = mZip ? FileUtils.createGzipInputStream(input, false) : FileUtils.createFileInputStream(input, false)) {
        RocSlope.writeSlope(in, printOut);
      }
    }
  }


  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mTpBase;
         VcfWriter ignored2 = mTpCalls;
         VcfWriter ignored3 = mFn;
         VcfWriter ignored4 = mFp) {
      // done for nice closing side effects
    }
  }
}
