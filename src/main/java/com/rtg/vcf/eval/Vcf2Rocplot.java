/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.vcf.eval.VcfEvalCli.CRITERIA_PRECISION;
import static com.rtg.vcf.eval.VcfEvalCli.CRITERIA_SCORE;
import static com.rtg.vcf.eval.VcfEvalCli.CRITERIA_SENSITIVITY;
import static com.rtg.vcf.eval.VcfEvalCli.SCORE_FIELD;
import static com.rtg.vcf.eval.VcfEvalCli.SORT_ORDER;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.LogStream;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReaderFactory;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Take one or more vcfeval annotated VCFs and produce rocplot compatible output files.
 * You need to ensure that both baseline and calls annotated VCFs are provided.
 */
public class Vcf2Rocplot extends LoggedCli {

  private RocContainer mRoc = null;
  private RocSortValueExtractor mRocExtractor = null;

  @Override
  public String moduleName() {
    return "vcf2rocplot";
  }

  @Override
  public String description() {
    return "produce rocplot compatible ROC data files from vcfeval annotated VCFs";
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(OUTPUT_FLAG);
  }

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription(StringUtils.sentencify(description()));
    CommonFlags.initOutputDirFlag(mFlags);
    CommonFlags.initRegionOrBedRegionsFlags(mFlags);
    mFlags.registerRequired(File.class, FILE, "input VCF files containing vcfeval annotations")
      .setMinCount(1)
      .setMaxCount(Integer.MAX_VALUE)
      .setCategory(INPUT_OUTPUT);

    VcfEvalCli.registerVcfRocFlags(mFlags);

    CommonFlags.initThreadsFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateInputFile(flags)
      && CommonFlags.validateThreads(flags)
      && CommonFlags.validateRegions(flags)
      && VcfEvalCli.validateVcfRocFlags(flags)
    );
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final boolean gzip = !mFlags.isSet(NO_GZIP);

    // Create ROC container / extractor
    final RocSortOrder sortOrder = (RocSortOrder) mFlags.getValue(SORT_ORDER);
    mRocExtractor = RocSortValueExtractor.getRocSortValueExtractor((String) mFlags.getValue(SCORE_FIELD), sortOrder);
    final RocPointCriteria criteria;
    if (mFlags.isSet(CRITERIA_PRECISION)) {
      criteria = new PrecisionThreshold((Double) mFlags.getValue(CRITERIA_PRECISION));
    } else if (mFlags.isSet(CRITERIA_SENSITIVITY)) {
      criteria = new SensitivityThreshold((Double) mFlags.getValue(CRITERIA_SENSITIVITY));
    } else if (mFlags.isSet(CRITERIA_SCORE)) {
      criteria = new FixedScoreThreshold((Double) mFlags.getValue(CRITERIA_SCORE), sortOrder.comparator());
    } else {
      criteria = new FMeasureThreshold();
    }
    mRoc = new RocContainer(mRocExtractor);
    mRoc.setRocPointCriteria(criteria);
    mRoc.addFilters(VcfEvalCli.getRocFilters(mFlags));

    final EvaluatedVariantsLoader[] loaders = {
      new WithInfoVcfLoader(),
      new Ga4ghVcfLoader()
    };
    for (final Object o : mFlags.getAnonymousValues(0)) {
      boolean loaded = false;
      final File vcf = (File) o;
      final VcfReaderFactory fact = new VcfReaderFactory(mFlags);
      try (final VcfIterator vr = fact.make(vcf)) {
        for (EvaluatedVariantsLoader l : loaders) {
          if (l.isCompatible(vr.getHeader())) {
            Diagnostic.userLog("VCF file " + vcf + " can be parsed as " + l);
            l.loadVariants(vr, vcf);
            loaded = true;
            break;
          }
        }
      }
      if (!loaded) {
        Diagnostic.userLog("VCF file " + vcf + " doesn't look to contain any recognized annotations");
      }
    }

    mRoc.missingScoreWarning();
    mRoc.writeRocs(outputDirectory(), gzip, false);
    mRoc.writeSummary(outputDirectory());

    return 0;
  }

  // Find the named sample or 0 otherwise
  private static int getSampleIndex(VcfHeader header, String sample) {
    final int index = header.getSampleIndex(sample);
    return index == -1 ? 0 : index;
  }


  private interface EvaluatedVariantsLoader {
    boolean isCompatible(VcfHeader vh);
    void loadVariants(VcfIterator vr, File vcf) throws IOException;
  }


  // Parses annotations created by vcfeval --output-mode annotate or --output-mode combine
  private class WithInfoVcfLoader implements EvaluatedVariantsLoader {

    public String toString() {
      return "vcfeval standard annotations";
    }

    @Override
    public boolean isCompatible(VcfHeader vh) {
      return vh.getInfoField(WithInfoEvalSynchronizer.INFO_CALL) != null
        || vh.getInfoField(WithInfoEvalSynchronizer.INFO_BASE) != null;
    }

    @Override
    public void loadVariants(VcfIterator vr, File vcf) throws IOException {
      final VcfHeader header = vr.getHeader();
      // If the VCF is from combined output, look up the appropriate sample indexes, otherwise assume 0
      final int bSample = getSampleIndex(header, CombinedEvalSynchronizer.SAMPLE_BASELINE);
      final int cSample = getSampleIndex(header, CombinedEvalSynchronizer.SAMPLE_CALLS);
      final boolean isCombined = bSample != 0 || cSample != 0;
      if (mRocExtractor.requiresSample()) {
        if (!isCombined && header.getNumberOfSamples() > 1) {
          Diagnostic.warning("VCF file " + vcf + " contains multiple samples, assuming first");
          // If this is wrong, the user should vcfsubset to pick out the sample they want.
        }
      }
      mRoc.filters().forEach(f -> f.setHeader(header));

      while (vr.hasNext()) {
        final VcfRecord rec = vr.next();

        final String base = rec.getInfo(WithInfoEvalSynchronizer.INFO_BASE);
        if (base != null) {
          switch (base) {
            case WithInfoEvalSynchronizer.STATUS_TP:
              mRoc.incrementBaselineCount(rec, bSample, true);
              break;
            case WithInfoEvalSynchronizer.STATUS_FN:
            case WithInfoEvalSynchronizer.STATUS_FN_CA:
              mRoc.incrementBaselineCount(rec, bSample, false);
              break;
            case WithInfoEvalSynchronizer.STATUS_OUTSIDE:
            case WithInfoEvalSynchronizer.STATUS_HARD:
            case WithInfoEvalSynchronizer.STATUS_IGNORED:
              // ignore these known values silently
              break;
            default:
              Diagnostic.warning("Unrecognized annotation for " + WithInfoEvalSynchronizer.INFO_BASE + " " + base);
              // ignore
          }
        }
        final String call = rec.getInfo(WithInfoEvalSynchronizer.INFO_CALL);
        if (call != null) {
          switch (call) {
            case WithInfoEvalSynchronizer.STATUS_TP:
              final double weight = VcfUtils.getDoubleInfoFieldFromRecord(rec, WithInfoEvalSynchronizer.INFO_CALL_WEIGHT);
              mRoc.addRocLine(rec, cSample, Double.isNaN(weight) ? 1 : weight, 0, 1);
              break;
            case WithInfoEvalSynchronizer.STATUS_FP:
            case WithInfoEvalSynchronizer.STATUS_FP_CA:
              mRoc.addRocLine(rec, cSample, 0, 1, 0);
              break;
            case WithInfoEvalSynchronizer.STATUS_OUTSIDE:
            case WithInfoEvalSynchronizer.STATUS_HARD:
            case WithInfoEvalSynchronizer.STATUS_IGNORED:
              // ignore these known values silently
              break;
            default:
              Diagnostic.warning("Unrecognized annotation for " + WithInfoEvalSynchronizer.INFO_CALL + " " + call);
              // ignore
          }
        }
      }
    }
  }

  // Parses annotations created by vcfeval --output-mode ga4gh
  private class Ga4ghVcfLoader implements EvaluatedVariantsLoader {

    public String toString() {
      return "vcfeval ga4gh annotations";
    }

    @Override
    public boolean isCompatible(VcfHeader vh) {
      return vh.getInfoField(Ga4ghEvalSynchronizer.FORMAT_DECISION) != null;
    }

    @Override
    public void loadVariants(VcfIterator vr, File vcf) throws IOException {
      while (vr.hasNext()) {
        final VcfRecord rec = vr.next();

        final List<String> decision = rec.getFormat(Ga4ghEvalSynchronizer.FORMAT_DECISION);
        if (decision == null) {
          continue;
        }

        switch (decision.get(Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX)) {
          case Ga4ghEvalSynchronizer.DECISION_TP:
            mRoc.incrementBaselineCount(rec, Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX, true);
            break;
          case Ga4ghEvalSynchronizer.DECISION_FN:
            mRoc.incrementBaselineCount(rec, Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX, false);
            break;
          default:
            // ignore
        }
        switch (decision.get(Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX)) {
          case Ga4ghEvalSynchronizer.DECISION_TP:
            mRoc.addRocLine(rec, Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX, 1, 0, 1);
            break;
          case Ga4ghEvalSynchronizer.DECISION_FP:
            mRoc.addRocLine(rec, Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX, 0, 1, 0);
            break;
          default:
            // ignore
        }
      }
    }
  }

}
