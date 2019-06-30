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
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.LogStream;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
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
    return "produce rocplot compatible data files from multiple vcfeval annotated VCFs";
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
    mFlags.registerRequired(File.class, FILE, "input VCF files containing vcfeval annotations")
      .setMinCount(1)
      .setMaxCount(Integer.MAX_VALUE)
      .setCategory(INPUT_OUTPUT);

    mFlags.registerOptional('f', VcfEvalCli.SCORE_FIELD, String.class, CommonFlags.STRING, "the name of the VCF FORMAT field to use as the ROC score. Also valid are \"QUAL\" or \"INFO.<name>\" to select the named VCF INFO field", VcfUtils.FORMAT_GENOTYPE_QUALITY).setCategory(REPORTING);
    mFlags.registerOptional('O', VcfEvalCli.SORT_ORDER, RocSortOrder.class, CommonFlags.STRING, "the order in which to sort the ROC scores so that \"good\" scores come before \"bad\" scores", RocSortOrder.DESCENDING).setCategory(REPORTING);

    CommonFlags.initThreadsFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateInputFile(flags)
      && CommonFlags.validateThreads(flags)
    );
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final boolean gzip = !mFlags.isSet(NO_GZIP);

    // Create ROC container / extractor
    mRocExtractor = RocSortValueExtractor.getRocSortValueExtractor((String) mFlags.getValue(VcfEvalCli.SCORE_FIELD), (RocSortOrder) mFlags.getValue(VcfEvalCli.SORT_ORDER));
    mRoc = new RocContainer(mRocExtractor);
    final Set<RocFilter> rocFilters = new LinkedHashSet<>(Collections.singletonList(RocFilter.ALL));  // We require the ALL entry in order to produce aggregate stats
    mRoc.addFilters(rocFilters);

    final Collection<?> values = mFlags.getAnonymousValues(0);
    for (final Object o : values) {
      final File vcf = (File) o;
      try (final VcfIterator vr = VcfReader.openVcfReader(vcf)) {
        if (vr.getHeader().getInfoField(WithInfoEvalSynchronizer.INFO_CALL) != null
          || vr.getHeader().getInfoField(WithInfoEvalSynchronizer.INFO_BASE) != null) {
          Diagnostic.userLog("VCF file " + vcf + " looks to contain regular vcfeval annotation");
          loadWithInfoVariants(vr, vcf);
        } else if (vr.getHeader().getInfoField(Ga4ghEvalSynchronizer.FORMAT_DECISION) != null) {
          Diagnostic.userLog("VCF file " + vcf + " looks to contain GA4GH annotations");
          loadGa4ghVariants(vr);
        } else {
          Diagnostic.userLog("VCF file " + vcf + " doesn't look to contain any recognized annotations");
        }
      }
    }

    mRoc.missingScoreWarning();
    mRoc.writeRocs(outputDirectory(), gzip, false);
    mRoc.writeSummary(outputDirectory());

    return 0;
  }


  private void loadWithInfoVariants(VcfIterator vr, File vcf) throws IOException {
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

    while (vr.hasNext()) {
      final VcfRecord rec = vr.next();

      final List<String> base = rec.getInfo().get(WithInfoEvalSynchronizer.INFO_BASE);
      if (base != null) {
        switch (base.get(0)) {
          case WithInfoEvalSynchronizer.STATUS_TP:
            mRoc.incrementBaselineCount(rec, bSample, true);
            break;
          case WithInfoEvalSynchronizer.STATUS_FN:
            mRoc.incrementBaselineCount(rec, bSample, false);
            break;
          default:
            // ignore
        }
      }
      final List<String> call = rec.getInfo().get(WithInfoEvalSynchronizer.INFO_CALL);
      if (call != null) {
        switch (call.get(0)) {
          case WithInfoEvalSynchronizer.STATUS_TP:
            final double weight = VcfUtils.getDoubleInfoFieldFromRecord(rec, WithInfoEvalSynchronizer.INFO_CALL_WEIGHT);
            mRoc.addRocLine(rec, cSample, Double.isNaN(weight) ? 1 : weight, 0, 1);
            break;
          case WithInfoEvalSynchronizer.STATUS_FP:
            mRoc.addRocLine(rec, cSample, 0, 1, 0);
            break;
          default:
            // ignore
        }
      }
    }
  }

  private void loadGa4ghVariants(VcfIterator vr) throws IOException {
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


  // Find the named sample or 0 otherwise
  private int getSampleIndex(VcfHeader header, String sample) {
    final int index = header.getSampleIndex(sample);
    return index == -1 ? 0 : index;
  }

}
