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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfUtils;


/**
 * Works out if calls are consistent with a baseline or not, always produces an ROC curve
 */
public final class VcfEvalTask extends ParamsTask<VcfEvalParams, NoStatistics> {

  private static BufferedReader getReader(final File f) throws IOException {
    return new BufferedReader(new InputStreamReader(FileUtils.createInputStream(f, true)));
  }

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
    final SequencesReader templateSequences = SequencesReaderFactory.createMemorySequencesReader(params.templateFile(), true, LongRange.NONE);
    SdfUtils.validateNoDuplicates(templateSequences, false);

    final File baseline = params.baselineFile();
    final File calls = params.callsFile();

    try (final BufferedReader baselineReader = getReader(baseline);
        final BufferedReader callReader = getReader(calls)) {
      checkHeader(baselineReader, callReader, templateSequences.getSdfId());
    }

    final ReferenceRanges<String> ranges = getReferenceRanges(params, templateSequences);
    final VariantSet variants = getVariants(params, templateSequences, ranges);

    evaluateCalls(params, ranges, templateSequences, variants);
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

    try (final EvalSynchronizer sync = getPathProcessor(params, ranges, variants)) {
      final SimpleThreadPool threadPool = new SimpleThreadPool(params.numberThreads(), "VcfEval", true);
      threadPool.enableBasicProgress(templateSequences.numberSequences());
      for (int i = 0; i < templateSequences.numberSequences(); i++) {
        threadPool.execute(new SequenceEvaluator(sync, nameMap, templateSequences.copy()));
      }

      threadPool.terminate();

      sync.finish();
    }
  }

  private static EvalSynchronizer getPathProcessor(VcfEvalParams params, ReferenceRanges<String> ranges, VariantSet variants) throws IOException {
    final File outdir = params.directory();
    final EvalSynchronizer processor;
    final String pathProcessor = GlobalFlags.getStringValue(GlobalFlags.VCFEVAL_PATH_PROCESSOR);
    switch (pathProcessor) {
      case "alleles":
        processor = new AlleleAccumulator(params.baselineFile(), params.callsFile(), variants, ranges, outdir, params.outputParams().isCompressed());
        break;
      case "hap-alleles":
        processor = new SquashedAlleleAccumulator(params.baselineFile(), params.callsFile(), variants, ranges, outdir, params.outputParams().isCompressed());
        break;
      case "recode":
        processor = new SampleRecoder(params.baselineFile(), params.callsFile(), variants, ranges, outdir, params.outputParams().isCompressed(), params.callsSample());
        break;
      default:
        final RocSortValueExtractor rocExtractor = getRocSortValueExtractor(params.scoreField(), params.sortOrder());
        processor = new DefaultEvalSynchronizer(params.baselineFile(), params.callsFile(), variants, ranges, params.callsSample(), rocExtractor, outdir, params.outputParams().isCompressed(), params.outputBaselineTp(), params.outputSlopeFiles(), params.rtgStats());
        break;
    }
    return processor;
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

    return new TabixVcfRecordSet(baseline, calls, ranges, nameOrdering, baselineSample, callsSample, !params.useAllRecords(), params.squashPloidy(), params.maxLength());
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

  static void checkHeader(BufferedReader baselineReader, BufferedReader callsReader, SdfId referenceSdfId) throws IOException {
    final ArrayList<String> baselineHeader = readHeader(baselineReader);
    final ArrayList<String> callsHeader = readHeader(callsReader);

    if (baselineHeader.size() == 0) {
      throw new NoTalkbackSlimException("No header found in baseline file");
    } else if (callsHeader.size() == 0) {
      throw new NoTalkbackSlimException("No header found in calls file");
    }

    final SdfId baselineTemplateSdfId = getSdfId(baselineHeader);
    final SdfId callsTemplateSdfId = getSdfId(callsHeader);

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

  static ArrayList<String> readHeader(BufferedReader reader) throws IOException {
    if (reader == null) {
      return null;
    }
    final ArrayList<String> header = new ArrayList<>();
    String line;
    while ("#".equals(peek(reader)) && (line = reader.readLine()) != null) {
      header.add(line);
    }
    return header;
  }

  static String peek(BufferedReader reader) throws IOException {
    reader.mark(1);
    final CharBuffer buff = CharBuffer.allocate(1);
    if (reader.read(buff) == 1) {
      buff.rewind();
      reader.reset();
      return buff.toString();
    }
    return "";
  }

  static SdfId getSdfId(ArrayList<String> header) {
    if (header != null) {
      for (final String s : header) {
        if (s.startsWith("##TEMPLATE-SDF-ID=")) { //NOTE: this is brittle and VCF specific
          final String[] split = s.split("=");
          if (split.length != 2) {
            throw new NoTalkbackSlimException("Invalid header line : " + s);
          }
          final SdfId sdfId;
          try {
           sdfId = new SdfId(split[1]);
          } catch (final NumberFormatException ex) {
            throw new NoTalkbackSlimException("Invalid header line : " + s);
          }
          return sdfId;
        }
      }
    }
    return new SdfId(0);
  }

}

