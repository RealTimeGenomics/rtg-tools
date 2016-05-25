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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rtg.launcher.GlobalFlags;
import com.rtg.launcher.NoStatistics;
import com.rtg.launcher.ParamsTask;
import com.rtg.reader.PrereadNamesInterface;
import com.rtg.reader.SdfId;
import com.rtg.reader.SdfUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.sam.SamRangeUtils;
import com.rtg.util.Pair;
import com.rtg.util.SimpleThreadPool;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;


/**
 * Works out if calls are consistent with a baseline or not, always produces an ROC curve
 */
public final class VcfEvalTask extends ParamsTask<VcfEvalParams, NoStatistics> {

  static final String MODE_ANNOTATE = "annotate";
  static final String MODE_COMBINE = "combine";
  static final String MODE_SPLIT = "split";
  static final String MODE_PHASE_TRANSFER = "phase-transfer";
  static final String MODE_RECODE = "recode";
  static final String MODE_ALLELES = "alleles";
  static final String MODE_GA4GH = "ga4gh";
  static final String MODE_ROC_ONLY = "roc-only";

  protected VcfEvalTask(VcfEvalParams params, OutputStream reportStream, NoStatistics stats) {
    super(params, reportStream, stats, null);
  }

  @Override
  protected void exec() throws IOException {
    evaluateCalls(mParams);
  }

  /**
   * @param params the evaluation parameters.
   * @throws IOException when an IO exception occurs
   */
  static void evaluateCalls(VcfEvalParams params) throws IOException {
    SdfUtils.validateHasNames(params.templateFile());
    try (final SequencesReader templateSequences = SequencesReaderFactory.createDefaultSequencesReader(params.templateFile(), LongRange.NONE)) {
      SdfUtils.validateNoDuplicates(templateSequences, false);

      final File baseline = params.baselineFile();
      final File calls = params.callsFile();
      checkHeader(VcfUtils.getHeader(baseline), VcfUtils.getHeader(calls), templateSequences.getSdfId());

      final ReferenceRanges<String> ranges = getReferenceRanges(params, templateSequences);
      final VariantSet variants = getVariants(params, templateSequences, ranges);

      evaluateCalls(params, ranges, templateSequences, variants);
    }
  }

  private static void evaluateCalls(VcfEvalParams params, ReferenceRanges<String> ranges, SequencesReader templateSequences, VariantSet variants) throws IOException {
    final File outdir = params.directory();
    if (!outdir.exists() && !outdir.mkdirs()) {
      throw new IOException("Unable to create directory \"" + outdir.getPath() + "\"");
    }

    final PrereadNamesInterface names = templateSequences.names();
    final Map<String, Long> nameMap = new HashMap<>();
    for (long i = 0; i < names.length(); i++) {
      nameMap.put(names.name(i), i);
    }

    final List<Pair<Orientor, Orientor>> o;
    if (params.twoPass() && params.squashPloidy()) {
      throw new IllegalStateException("Cannot run two-pass with squash-ploidy set");
    }
    final String bvf = VariantFactory.getFactoryName(VariantSetType.BASELINE, params.baselineSample());
    final String cvf = VariantFactory.getFactoryName(VariantSetType.CALLS, params.callsSample());
    if (params.twoPass()) {
      o = new ArrayList<>();
      o.add(new Pair<>(getOrientor(bvf, false, params.baselinePhaseOrientor()), getOrientor(cvf, false, params.callsPhaseOrientor())));
      o.add(new Pair<>(getOrientor(bvf, true, params.baselinePhaseOrientor()), getOrientor(cvf, true, params.callsPhaseOrientor())));
    } else {
      o = Collections.singletonList(new Pair<>(getOrientor(bvf, params.squashPloidy(), params.baselinePhaseOrientor()), getOrientor(cvf, params.squashPloidy(), params.callsPhaseOrientor())));
    }
    try (final EvalSynchronizer sync = getPathProcessor(params, ranges, variants)) {
      final SimpleThreadPool threadPool = new SimpleThreadPool(params.numberThreads(), "VcfEval", true);
      threadPool.enableBasicProgress(templateSequences.numberSequences());
      for (int i = 0; i < templateSequences.numberSequences(); i++) {
        threadPool.execute(new SequenceEvaluator(sync, nameMap, templateSequences, o));
      }

      threadPool.terminate();

      sync.finish();
    }
  }

  private static EvalSynchronizer getPathProcessor(VcfEvalParams params, ReferenceRanges<String> ranges, VariantSet variants) throws IOException {
    final File outdir = params.directory();
    final EvalSynchronizer processor;
    final String outputMode = GlobalFlags.isSet(GlobalFlags.VCFEVAL_PATH_PROCESSOR) ? GlobalFlags.getStringValue(GlobalFlags.VCFEVAL_PATH_PROCESSOR) : params.outputMode();
    final RocSortValueExtractor rocExtractor = getRocSortValueExtractor(params.scoreField(), params.sortOrder());
    switch (outputMode) {
      case MODE_ALLELES:
        if (params.squashPloidy()) {
          processor = new SquashedAlleleAccumulator(params.baselineFile(), params.callsFile(), variants, ranges, outdir, params.outputParams().isCompressed());
        } else {
          processor = new AlleleAccumulator(params.baselineFile(), params.callsFile(), variants, ranges, outdir, params.outputParams().isCompressed());
        }
        break;
      case MODE_RECODE:
        if (params.squashPloidy()) {
          throw new UnsupportedOperationException();
        }
        processor = new SampleRecoder(params.baselineFile(), params.callsFile(), variants, ranges, outdir, params.outputParams().isCompressed(), params.callsSample());
        break;
      case MODE_PHASE_TRANSFER:
        if (params.squashPloidy() || params.twoPass()) {
          throw new UnsupportedOperationException();
        }
        processor = new PhaseTransferEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_ANNOTATE:
        processor = new AnnotatingEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_COMBINE:
        processor = new CombinedEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.baselineSample(), params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_SPLIT:
        processor = new SplitEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_GA4GH:
        processor = new Ga4ghEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.baselineSample(), params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed());
        break;
      case MODE_ROC_ONLY:
        processor = new RocOnlyEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      default:
        throw new NoTalkbackSlimException("Unsupported output mode:" + outputMode);
    }
    return processor;
  }


  // Look up an appropriate orientor depending on the type of comparison we're doing
  static Orientor getOrientor(String variantFactoryName, boolean squashPloidy, Orientor phaseOrientor) {
    switch (variantFactoryName) {
      case VariantFactory.SAMPLE_FACTORY:
        return squashPloidy ? Orientor.SQUASH_GT : phaseOrientor;
      case VariantFactory.ALL_FACTORY:
        return squashPloidy ? Orientor.SQUASH_POP : Orientor.RECODE_POP;
      default:
        throw new RuntimeException("Could not determine orientor for " + variantFactoryName);
    }
  }

  /**
   * Builds a variant set of the best type for the supplied files
   * @param params the parameters
   * @param templateSequences template sequences
   * @param ranges controls which regions the variants will be loaded from
   * @return a VariantSet for the provided files
   * @throws IOException if IO is broken
   */
  static VariantSet getVariants(VcfEvalParams params, SequencesReader templateSequences, ReferenceRanges<String> ranges) throws IOException {
    final File calls = params.callsFile();
    final File baseline = params.baselineFile();
    final String baselineSample = params.baselineSample();
    final String callsSample = params.callsSample();

    final List<Pair<String, Integer>> nameOrdering = new ArrayList<>();
    for (long i = 0; i < templateSequences.names().length(); i++) {
      nameOrdering.add(new Pair<>(templateSequences.names().name(i), templateSequences.length(i)));
    }

    return new TabixVcfRecordSet(baseline, calls, ranges, nameOrdering, baselineSample, callsSample, !params.useAllRecords(), params.refOverlap(), params.maxLength());
  }

  static ReferenceRanges<String> getReferenceRanges(VcfEvalParams params, SequencesReader templateSequences) throws IOException {
    final ReferenceRanges<String> ranges;
    if (params.bedRegionsFile() != null) {
      Diagnostic.developerLog("Loading BED regions");
      ranges = SamRangeUtils.createBedReferenceRanges(params.bedRegionsFile());
    } else if (params.restriction() != null) {
      ranges = SamRangeUtils.createExplicitReferenceRange(params.restriction());
    } else {
      ranges = SamRangeUtils.createFullReferenceRanges(templateSequences);
    }
    return ranges;
  }

  private static RocSortValueExtractor getRocSortValueExtractor(String scoreField, RocSortOrder sortOrder) {
    final RocScoreField fieldType;
    final String fieldName;
    if (scoreField != null) {
      if (scoreField.contains("=")) {
        final int pIndex = scoreField.indexOf('=');
        final String fieldTypeName = scoreField.substring(0, pIndex).toUpperCase(Locale.getDefault());
        try {
          fieldType = RocScoreField.valueOf(fieldTypeName);
        } catch (IllegalArgumentException e) {
          throw new NoTalkbackSlimException("Unrecognized field type \"" + fieldTypeName + "\", must be one of " + Arrays.toString(RocScoreField.values()));
        }
        fieldName = scoreField.substring(pIndex + 1);
      } else if (scoreField.equals(VcfUtils.QUAL)) {
        fieldType = RocScoreField.QUAL;
        fieldName = "UNUSED";
      } else {
        fieldType = RocScoreField.FORMAT;
        fieldName = scoreField;
      }
    } else {
      fieldType = RocScoreField.FORMAT;
      fieldName = VcfUtils.FORMAT_GENOTYPE_QUALITY;
    }
    return fieldType.getExtractor(fieldName, sortOrder);
  }

  static void checkHeader(VcfHeader baseline, VcfHeader calls, SdfId referenceSdfId) throws IOException {
    final SdfId baselineTemplateSdfId = getSdfId(baseline);
    final SdfId callsTemplateSdfId = getSdfId(calls);

    if (!baselineTemplateSdfId.check(referenceSdfId)) {
      Diagnostic.warning("Reference template ID mismatch, baseline variants were not created from the given reference");
    }

    if (!callsTemplateSdfId.check(referenceSdfId)) {
      Diagnostic.warning("Reference template ID mismatch, called variants were not created from the given reference");
    }

    if (!baselineTemplateSdfId.check(callsTemplateSdfId)) {
      Diagnostic.warning("Reference template ID mismatch, baseline and called variants were created with different references");
    }
  }

  static SdfId getSdfId(VcfHeader header) {
    for (final String s : header.getGenericMetaInformationLines()) {
      if (s.startsWith("##TEMPLATE-SDF-ID=")) {
        final String[] split = s.split("=");
        if (split.length != 2) {
          throw new NoTalkbackSlimException("Invalid VCF template SDF ID header line : " + s);
        }
        final SdfId sdfId;
        try {
          sdfId = new SdfId(split[1]);
        } catch (final NumberFormatException ex) {
          throw new NoTalkbackSlimException("Invalid VCF template SDF ID header line : " + s);
        }
        return sdfId;
      }
    }
    return new SdfId(0);
  }

}

