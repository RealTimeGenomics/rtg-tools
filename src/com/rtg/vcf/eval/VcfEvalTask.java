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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.rtg.bed.BedReader;
import com.rtg.launcher.NoStatistics;
import com.rtg.launcher.ParamsTask;
import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.reader.ReaderUtils;
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
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.io.IOIterator;
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
      final ReferenceRegions evalRegions;
      if (params.evalRegionsFile() != null) {
        evalRegions = new ReferenceRegions();
        try (IOIterator<? extends SequenceNameLocus> r = BedReader.openBedReader(null, params.evalRegionsFile(), 0)) {
          evalRegions.add(r);
        }
      } else {
        evalRegions = null;
      }
      final VariantSet variants = getVariants(params, templateSequences, ranges, evalRegions);

      evaluateCalls(params, templateSequences, variants);
    }
  }

  private static void evaluateCalls(VcfEvalParams params, SequencesReader templateSequences, VariantSet variants) throws IOException {
    final File outdir = params.directory();
    if (!outdir.exists() && !outdir.mkdirs()) {
      throw new IOException("Unable to create directory \"" + outdir.getPath() + "\"");
    }

    final Map<String, Long> nameMap = ReaderUtils.getSequenceNameMap(templateSequences);

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
    try (final EvalSynchronizer sync = getPathProcessor(params, variants)) {
      final SimpleThreadPool threadPool = new SimpleThreadPool(params.numberThreads(), "VcfEval", true);
      threadPool.enableBasicProgress(templateSequences.numberSequences());
      for (int i = 0; i < templateSequences.numberSequences(); ++i) {
        threadPool.execute(new SequenceEvaluator(sync, nameMap, templateSequences, o));
      }

      threadPool.terminate();

      sync.finish();
    }
  }

  private static EvalSynchronizer getPathProcessor(VcfEvalParams params, VariantSet variants) throws IOException {
    final File outdir = params.directory();
    final EvalSynchronizer processor;
    final String outputMode = GlobalFlags.isSet(ToolsGlobalFlags.VCFEVAL_PATH_PROCESSOR) ? GlobalFlags.getStringValue(ToolsGlobalFlags.VCFEVAL_PATH_PROCESSOR) : params.outputMode();
    final RocSortValueExtractor rocExtractor = RocSortValueExtractor.getRocSortValueExtractor(params.scoreField(), params.sortOrder());
    rocExtractor.setHeader(variants.calledHeader());
    switch (outputMode) {
      case MODE_ALLELES:
        if (params.squashPloidy()) {
          processor = new SquashedAlleleAccumulator(variants, outdir, params.outputParams().isCompressed());
        } else {
          processor = new AlleleAccumulator(variants, outdir, params.outputParams().isCompressed());
        }
        break;
      case MODE_RECODE:
        if (params.squashPloidy()) {
          throw new UnsupportedOperationException();
        }
        processor = new SampleRecoder(variants, outdir, params.outputParams().isCompressed(), params.callsSample());
        break;
      case MODE_PHASE_TRANSFER:
        if (params.squashPloidy() || params.twoPass()) {
          throw new UnsupportedOperationException();
        }
        processor = new PhaseTransferEvalSynchronizer(variants, rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_ANNOTATE:
        processor = new AnnotatingEvalSynchronizer(variants, rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_COMBINE:
        processor = new CombinedEvalSynchronizer(variants, rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_SPLIT:
        processor = new SplitEvalSynchronizer(variants, rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      case MODE_GA4GH:
        processor = new Ga4ghEvalSynchronizer(variants, rocExtractor, outdir, params.outputParams().isCompressed(), params.looseMatchDistance());
        break;
      case MODE_ROC_ONLY:
        processor = new RocOnlyEvalSynchronizer(variants, rocExtractor, outdir, params.outputParams().isCompressed(), params.outputSlopeFiles(), params.twoPass(), params.rocFilters());
        break;
      default:
        throw new NoTalkbackSlimException("Unsupported output mode:" + outputMode);
    }
    return processor;
  }


  // Look up an appropriate orientor depending on the type of comparison we're doing
  static Orientor getOrientor(String variantFactoryName, boolean alleleMatching, Orientor phaseOrientor) {
    final boolean hapAlleleMatching = alleleMatching && GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_HAPLOID_ALLELE_MATCHING);
    final boolean dipAlleleMatching = alleleMatching && !GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_HAPLOID_ALLELE_MATCHING);
    switch (variantFactoryName) {
      case VariantFactory.SAMPLE_FACTORY:
        return dipAlleleMatching ? Orientor.ALLELE_GT : hapAlleleMatching ? Orientor.SQUASH_GT : phaseOrientor;
      case VariantFactory.ALL_FACTORY:
        return hapAlleleMatching ? Orientor.HAPLOID_POP : Orientor.DIPLOID_POP;
      default:
        throw new RuntimeException("Could not determine orientor for " + variantFactoryName);
    }
  }

  /**
   * Builds a variant set of the best type for the supplied files
   * @param params the parameters
   * @param templateSequences template sequences
   * @param ranges controls which regions the variants will be loaded from
   * @param evalRegions optional evaluation regions
   * @return a VariantSet for the provided files
   * @throws IOException if IO is broken
   */
  static VariantSet getVariants(VcfEvalParams params, SequencesReader templateSequences, ReferenceRanges<String> ranges, ReferenceRegions evalRegions) throws IOException {
    final File calls = params.callsFile();
    final File baseline = params.baselineFile();
    final String baselineSample = params.baselineSample();
    final String callsSample = params.callsSample();

    final long numSequences = templateSequences.names().length();
    assert numSequences <= Integer.MAX_VALUE;
    final List<Pair<String, Integer>> nameOrdering = new ArrayList<>((int) numSequences);
    for (long i = 0; i < numSequences; ++i) {
      nameOrdering.add(new Pair<>(templateSequences.names().name(i), templateSequences.length(i)));
    }
    final File preprocessDir;
    if (params.decompose()) {
      preprocessDir = new File(params.outputParams().directory(), "intermediate");
      if (!preprocessDir.exists() && !preprocessDir.mkdir()) {
        throw new IOException("Could not create directory for intermediate files: " + preprocessDir);
      }
    } else {
      preprocessDir = null;
    }
    return new TabixVcfRecordSet(baseline, calls, ranges, evalRegions, nameOrdering, baselineSample, callsSample, !params.useAllRecords(), params.refOverlap(), params.maxLength(), preprocessDir);
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

  static void checkHeader(VcfHeader baseline, VcfHeader calls, SdfId referenceSdfId) {
    final SdfId baselineTemplateSdfId = baseline.getSdfId();
    final SdfId callsTemplateSdfId = calls.getSdfId();

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

}

