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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rtg.launcher.CommonFlags;
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
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;


/**
 * Works out if calls are consistent with a baseline or not, always produces an ROC curve
 */
public final class VcfEvalTask extends ParamsTask<VcfEvalParams, NoStatistics> {

  private static final String FN_FILE_NAME = "fn.vcf";
  private static final String FP_FILE_NAME = "fp.vcf";
  private static final String TP_FILE_NAME = "tp.vcf";
  private static final String TPBASE_FILE_NAME = "tp-baseline.vcf";

  /** Filename used for the full ROC curve */
  public static final String FULL_ROC_FILE = "weighted_roc.tsv";
  /** Filename used for the homozygous curve */
  public static final String HOMOZYGOUS_FILE = "homozygous_roc.tsv";
  /** Filename used for the heterozygous curve */
  public static final String HETEROZYGOUS_FILE = "heterozygous_roc.tsv";
  /** Filename used for the simple ROC curve */
  public static final String SIMPLE_FILE = "simple_roc.tsv";
  /** Filename used for the complex ROC curve */
  public static final String COMPLEX_FILE = "complex_roc.tsv";
  /** Filename used for the homozygous simple ROC curve */
  public static final String HOMOZYGOUS_SIMPLE_FILE = "homozygous_simple_roc.tsv";
  /** Filename used for the homozygous complex ROC curve */
  public static final String HOMOZYGOUS_COMPLEX_FILE = "homozygous_complex_roc.tsv";
  /** Filename used for the heterozygous simple ROC curve */
  public static final String HETEROZYGOUS_SIMPLE_FILE = "heterozygous_simple_roc.tsv";
  /** Filename used for the heterozygous complex ROC curve */
  public static final String HETEROZYGOUS_COMPLEX_FILE = "heterozygous_complex_roc.tsv";

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
    final File output = params.directory();

    final boolean zip = params.outputParams().isCompressed();
    try (final BufferedReader baselineReader = getReader(baseline);
        final BufferedReader callReader = getReader(calls)) {
      checkHeader(baselineReader, callReader, templateSequences.getSdfId());
    }

    final VariantSet variants = getVariants(params, templateSequences);
    if (!output.exists() && !output.mkdirs()) {
      throw new IOException("Unable to create directory \"" + output.getPath() + "\"");
    }

    final boolean outputTpBase = params.outputBaselineTp();

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final File tpFile = new File(output, TP_FILE_NAME + zipExt);
    final File fpFile = new File(output, FP_FILE_NAME + zipExt);
    final File fnFile = new File(output, FN_FILE_NAME + zipExt);
    final File tpBaseFile = outputTpBase ? new File(output, TPBASE_FILE_NAME + zipExt) : null;

    try (final VcfWriter tp = new VcfWriter(variants.calledHeader(), tpFile, null, zip, true);
         final VcfWriter fp = new VcfWriter(variants.calledHeader(), fpFile, null, zip, true);
         final VcfWriter fn = new VcfWriter(variants.baseLineHeader(), fnFile, null, zip, true);
         final VcfWriter tpBase = outputTpBase ? new VcfWriter(variants.baseLineHeader(), tpBaseFile, null, zip, true) : null) {
      evaluateCalls(params, templateSequences, output, variants, tp, fp, fn, tpBase);
    }
  }

  private static void evaluateCalls(VcfEvalParams params, SequencesReader templateSequences, File output, VariantSet variants,
                                    VcfWriter tp, VcfWriter fp, VcfWriter fn, VcfWriter tpBase) throws IOException {
    final boolean zip = params.outputParams().isCompressed();

    final PrereadNamesInterface names = templateSequences.names();
    final Map<String, Long> nameMap = new HashMap<>();
    for (long i = 0; i < names.length(); i++) {
      nameMap.put(names.name(i), i);
    }
    final String rocLabel = getRocSortValueExtractor(params).toString();
    final RocContainer roc = new RocContainer(params.sortOrder(), rocLabel);
    roc.addFilter(RocFilter.ALL, new File(output, FULL_ROC_FILE));
    roc.addFilter(RocFilter.HETEROZYGOUS, new File(output, HETEROZYGOUS_FILE));
    roc.addFilter(RocFilter.HOMOZYGOUS, new File(output, HOMOZYGOUS_FILE));
    if (params.rtgStats()) {
      roc.addFilter(RocFilter.SIMPLE, new File(output, SIMPLE_FILE));
      roc.addFilter(RocFilter.COMPLEX, new File(output, COMPLEX_FILE));
      roc.addFilter(RocFilter.HETEROZYGOUS_SIMPLE, new File(output, HETEROZYGOUS_SIMPLE_FILE));
      roc.addFilter(RocFilter.HETEROZYGOUS_COMPLEX, new File(output, HETEROZYGOUS_COMPLEX_FILE));
      roc.addFilter(RocFilter.HOMOZYGOUS_SIMPLE, new File(output, HOMOZYGOUS_SIMPLE_FILE));
      roc.addFilter(RocFilter.HOMOZYGOUS_COMPLEX, new File(output, HOMOZYGOUS_COMPLEX_FILE));
    }
    final EvalSynchronizer sync = new EvalSynchronizer(variants, tp, fp, fn, tpBase, params.baselineFile(), params.callsFile(), roc);

    final SimpleThreadPool threadPool = new SimpleThreadPool(params.numberThreads(), "VcfEval", true);
    threadPool.enableBasicProgress(templateSequences.numberSequences());
    for (int i = 0; i < templateSequences.numberSequences(); i++) {
      threadPool.execute(new SequenceEvaluator(sync, nameMap, templateSequences.copy()));
    }

    threadPool.terminate();

    if (variants.getNumberOfSkippedBaselineVariants() > 0) {
      Diagnostic.warning("There were " + variants.getNumberOfSkippedBaselineVariants() + " baseline variants skipped due to being too long, overlapping or starting outside the expected reference sequence length.");
    }
    if (variants.getNumberOfSkippedCalledVariants() > 0) {
      Diagnostic.warning("There were " + variants.getNumberOfSkippedCalledVariants() + " called variants skipped due to being too long, overlapping or starting outside the expected reference sequence length.");
    }
    if (roc.getNumberOfIgnoredVariants() > 0) {
      Diagnostic.warning("There were " + roc.getNumberOfIgnoredVariants() + " variants not included in ROC data files due to missing or invalid " + rocLabel + " values.");
    }
    Diagnostic.developerLog("Writing ROC");
    roc.writeRocs(sync.mTruePositives + sync.mFalseNegatives, zip);
    if (params.outputSlopeFiles()) {
      produceSlopeFiles(params.directory(), zip, params.rtgStats());
    }
    writePhasingInfo(sync, params.directory());

    roc.writeSummary(new File(params.directory(), CommonFlags.SUMMARY_FILE), sync.mTruePositives, sync.mFalsePositives, sync.mFalseNegatives);
  }

  private static void writePhasingInfo(EvalSynchronizer sync, File outDir) throws IOException {
    final File phasingFile = new File(outDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + sync.getCorrectPhasings() + StringUtils.LS + "Incorrect phasings: " + sync.getMisPhasings() + StringUtils.LS + "Unresolvable phasings: " + sync.getUnphasable() + StringUtils.LS, phasingFile);
  }

  private static void produceSlopeFiles(File outDir, boolean zip, boolean rtgStats) throws IOException {
    final String suffix = zip ? FileUtils.GZ_SUFFIX : "";
    final File fullFile = new File(outDir, VcfEvalTask.FULL_ROC_FILE + suffix);
    produceSlopeFiles(fullFile, new File(outDir, "weighted_slope.tsv" + suffix), zip);
    final File heteroFile = new File(outDir, VcfEvalTask.HETEROZYGOUS_FILE + suffix);
    produceSlopeFiles(heteroFile, new File(outDir, "heterozygous_slope.tsv" + suffix), zip);
    final File homoFile = new File(outDir, VcfEvalTask.HOMOZYGOUS_FILE + suffix);
    produceSlopeFiles(homoFile, new File(outDir, "homozygous_slope.tsv" + suffix), zip);
    if (rtgStats) {
      final File simpleFile = new File(outDir, VcfEvalTask.SIMPLE_FILE + suffix);
      produceSlopeFiles(simpleFile, new File(outDir, "simple_slope.tsv" + suffix), zip);
      final File complexFile = new File(outDir, VcfEvalTask.COMPLEX_FILE + suffix);
      produceSlopeFiles(complexFile, new File(outDir, "complex_slope.tsv" + suffix), zip);
      final File heteroSimpleFile = new File(outDir, VcfEvalTask.HETEROZYGOUS_SIMPLE_FILE + suffix);
      produceSlopeFiles(heteroSimpleFile, new File(outDir, "heterozygous_simple_slope.tsv" + suffix), zip);
      final File heteroComplexFile = new File(outDir, VcfEvalTask.HETEROZYGOUS_COMPLEX_FILE + suffix);
      produceSlopeFiles(heteroComplexFile, new File(outDir, "heterozygous_complex_slope.tsv" + suffix), zip);
      final File homoSimpleFile = new File(outDir, VcfEvalTask.HOMOZYGOUS_SIMPLE_FILE + suffix);
      produceSlopeFiles(homoSimpleFile, new File(outDir, "homozygous_simple_slope.tsv" + suffix), zip);
      final File homoComplexFile = new File(outDir, VcfEvalTask.HOMOZYGOUS_COMPLEX_FILE + suffix);
      produceSlopeFiles(homoComplexFile, new File(outDir, "homozygous_complex_slope.tsv" + suffix), zip);
    }
  }

  private static void produceSlopeFiles(File input, File output, boolean zip) throws IOException {
    if (input.exists() && input.length() > 0) {
      try (final PrintStream printOut = new PrintStream(FileUtils.createOutputStream(output, zip));
          final InputStream in = zip ? FileUtils.createGzipInputStream(input, false) : FileUtils.createFileInputStream(input, false)) {
        RocSlope.writeSlope(in, printOut);
      }
    }
  }


  /**
   * Builds a variant set of the best type for the supplied files
   * @param params the parameters
   * @param templateSequences template sequences
   * @return a VariantSet for the provided files
   * @throws IOException if IO is broken
   */
  static VariantSet getVariants(VcfEvalParams params, SequencesReader templateSequences) throws IOException {
    final File calls = params.callsFile();
    final File baseline = params.baselineFile();
    final String sampleName = params.sampleName();
    final RocSortValueExtractor extractor = getRocSortValueExtractor(params);

    final ReferenceRanges<String> ranges;
    if (params.bedRegionsFile() != null) {
      Diagnostic.developerLog("Loading BED regions");
      ranges = SamRangeUtils.createBedReferenceRanges(params.bedRegionsFile());
    } else if (params.restriction() != null) {
      ranges = SamRangeUtils.createExplicitReferenceRange(params.restriction());
    } else {
      ranges = SamRangeUtils.createFullReferenceRanges(templateSequences);
    }

    final List<Pair<String, Integer>> nameOrdering = new ArrayList<>();
    for (long i = 0; i < templateSequences.names().length(); i++) {
      nameOrdering.add(new Pair<>(templateSequences.names().name(i), templateSequences.length(i)));
    }

    return new TabixVcfRecordSet(baseline, calls, ranges, nameOrdering, sampleName, extractor, !params.useAllRecords(), params.squashPloidy(), params.maxLength());
  }

  private static RocSortValueExtractor getRocSortValueExtractor(VcfEvalParams params) {
    final RocScoreField fieldType;
    final String fieldName;
    final String scoreField = params.scoreField();
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
    return fieldType.getExtractor(fieldName, params.sortOrder());
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

