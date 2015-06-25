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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.launcher.OutputParams;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.Timer;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.BufferedOutputStreamFix;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogStream;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfHeaderMerge;
import com.rtg.vcf.header.VcfNumber;


/**
 * Takes two files and produces intersection for SNP's, the output directory contains four files:
 * <ul>
 * <li><code>same.vcf</code>: file containing same calls at same location, it has two lines per entry, first from -i and second from -I</li>
 * <li><code>different.vcf</code>: file containing different calls at same location, it has two lines per entry, first from -i and second from -I</li>
 * <li><code>first-only.vcf</code>: file containing calls from file given to -i flag, that was not called in -I file
 * <li><code>second-only.vcf</code>: file containing calls from file given to -I flag, that was not called in -i file
 * </ul>
 *
 * Two calls are considered to be same iff both calls have
 * <ul>
 * <li>same chromosome</li>
 * <li>same location</li>
 * <li>same prediction</li>
 * </ul>
 *
 * Superceded by <code>vcfeval</code>
 */
public final class SnpIntersection extends LoggedCli {

  private static final String MODULE_NAME = "snpintersect";

  private static final String FIRST_INPUT_FILE = "input-first";
  private static final String SECOND_INPUT_FILE = "input-second";
  private static final String COMPARE_ALTS = "compare-alts";
  private static final String FORCE_MERGE = "force-merge";

  private static final String SAME_OUT = "same.vcf";
  private static final String DIFFERENT_OUT = "different.vcf";
  private static final String FIRST_ONLY_OUT = "first-only.vcf";
  private static final String SECOND_ONLY_OUT = "second-only.vcf";

  private static final String REGION_FLAG = "region";

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  protected static void initFlags(CFlags flags) {
    CommonFlagCategories.setCategories(flags);
    flags.registerRequired('i', FIRST_INPUT_FILE, File.class, CommonFlags.FILE, "first file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerRequired('I', SECOND_INPUT_FILE, File.class, CommonFlags.FILE, "second file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, CommonFlags.DIR, "output directory").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerOptional('c', COMPARE_ALTS, "do a basic comparison on ALT calls in addition to position").setCategory(CommonFlagCategories.REPORTING);
CommonFlags.initNoGzip(flags);
    final Flag forceMerge = flags.registerOptional(FORCE_MERGE, String.class, "STRING", "allow merging of specified header ID even when not compatible").setCategory(CommonFlagCategories.UTILITY);
    forceMerge.setMinCount(0);
    forceMerge.setMaxCount(Integer.MAX_VALUE);
    flags.registerOptional(REGION_FLAG, String.class, "STRING", "if set, only process the SNPs within the specified range. The format is one of <template_name>, <template_name>:start-end or <template_name>:start+length").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    flags.setDescription("Produces intersection information between two SNP files.");
    flags.setValidator(new SnpIntersectionFlagValidator());
  }

  protected static final class SnpIntersectionFlagValidator implements Validator {
    @Override
    public boolean isValid(CFlags flags) {
      if (!CommonFlags.validateOutputDirectory(flags)) {
        return false;
      }
      if (!checkInputFile(flags, FIRST_INPUT_FILE) || !checkInputFile(flags, SECOND_INPUT_FILE)) {
        return false;
      }
      if (flags.isSet(REGION_FLAG)) {
        final String region = (String) flags.getValue(REGION_FLAG);
        if (!RegionRestriction.validateRegion(region)) {
          flags.setParseMessage("The value \"" + region + "\" for \"--" + REGION_FLAG + "\" is not a well formed region.");
          return false;
        }
        final File first = (File) flags.getValue(FIRST_INPUT_FILE);
        final String errStringFirst = "\"" + first.getPath() + "\" is not in bgzip format. Cannot use \"--" + REGION_FLAG + "\".";
        try {
          if (!TabixIndexer.isBlockCompressed(first)) {
            throw new NoTalkbackSlimException(errStringFirst);
          }
        } catch (IOException e) {
          throw new NoTalkbackSlimException(errStringFirst);
        }
        final File second = (File) flags.getValue(SECOND_INPUT_FILE);
        final String errStringSecond = "\"" + second.getPath() + "\" is not in bgzip format. Cannot use \"--" + REGION_FLAG + "\".";
        try {
          if (!TabixIndexer.isBlockCompressed(second)) {
            throw new NoTalkbackSlimException(errStringSecond);
          }
        } catch (IOException e) {
          throw new NoTalkbackSlimException(errStringSecond);
        }
        final File indexFirst = TabixIndexer.indexFileName(first);
        if (!indexFirst.exists()) {
          throw new NoTalkbackSlimException("Index not found for file: \"" + first.getPath() + "\" expected index called: \"" + indexFirst.getPath() + "\"");
        }
        final File indexSecond = TabixIndexer.indexFileName(second);
        if (!indexSecond.exists()) {
          throw new NoTalkbackSlimException("Index not found for file: \"" + second.getPath() + "\" expected index called: \"" + indexSecond.getPath() + "\"");
        }
      }
      return true;
    }
  }

  protected static boolean checkInputFile(CFlags flags, String flag) {
    final File file = (File) flags.getValue(flag);
    if (!file.exists()) {
      Diagnostic.error(ErrorType.INFO_ERROR, "The file \"" + file.getPath() + "\", specified for \"--" + flag + "\", does not exist.");
      return false;
    }
    if (file.isDirectory()) {
      Diagnostic.error(ErrorType.INFO_ERROR, "The file \"" + file.getPath() + "\", specified for \"--" + flag + "\", is a directory.");
      return false;
    }
    return true;
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    process(out);
    return 0;
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "simple position-based intersection of VCF files";
  }

  private void process(final OutputStream out) throws IOException {
    final boolean compareAlts = mFlags.isSet(COMPARE_ALTS);
    final File first = (File) mFlags.getValue(FIRST_INPUT_FILE);
    final File second = (File) mFlags.getValue(SECOND_INPUT_FILE);
    final File outputFile = (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
    final RegionRestriction region = mFlags.isSet(REGION_FLAG) ? new RegionRestriction((String) mFlags.getValue(REGION_FLAG)) : null;
    final boolean gzip = !mFlags.isSet(CommonFlags.NO_GZIP);
    final List<Object> forceMergeRaw = mFlags.getValues(FORCE_MERGE);
    final HashSet<String> forceMerge = new HashSet<>();
    for (Object o : forceMergeRaw) {
      forceMerge.add((String) o);
    }
    final OutputParams output = new OutputParams(outputFile, false, gzip);
    int totalSecondCount = 0;
    int firstOnlyCount = 0;
    int secondOnlyCount = 0;
    int differentCount = 0;
    int sameCount = 0;
    Diagnostic.userLog(output.toString());
    final int totalFirstCount;
    final HashMap<String, List<LineHolder>> map = new HashMap<>();
    final VcfHeader firstOnlyHeader;
    try (VcfReader firstReader = VcfReader.openVcfReader(first, region)) {
      totalFirstCount = loadFirstSnps(firstReader, map);
      firstOnlyHeader = firstReader.getHeader().copy();
      firstOnlyHeader.addMetaInformationLine("##sourceFiles=0:" + first.getPath() + ",1:" + second.getPath());
      final InfoField sf = new InfoField("SF", MetaType.STRING, VcfNumber.DOT, "Source File (index into sourceFiles)");
      firstOnlyHeader.ensureContains(sf);

      Diagnostic.progress("Start loading second file...");
      try (VcfReader secondReader = VcfReader.openVcfReader(second, region)) {
        final VcfHeader secondOnlyHeader = secondReader.getHeader().copy();
        secondOnlyHeader.addMetaInformationLine("##sourceFiles=0:" + first.getPath() + ",1:" + second.getPath());
        secondOnlyHeader.ensureContains(sf);
        final VcfHeader combinedHeader = VcfHeaderMerge.mergeHeaders(firstReader.getHeader(), secondReader.getHeader(), forceMerge);
        combinedHeader.addMetaInformationLine("##sourceFiles=0:" + first.getPath() + ",1:" + second.getPath());
        combinedHeader.ensureContains(sf);
        try (BufferedWriter secondOnlyWriter = new BufferedWriter(new OutputStreamWriter(output.outStream(SECOND_ONLY_OUT)));
             BufferedWriter sameWriter = new BufferedWriter(new OutputStreamWriter(output.outStream(SAME_OUT)));
             BufferedWriter differentWriter = new BufferedWriter(new OutputStreamWriter(output.outStream(DIFFERENT_OUT)))) {
          writeHeader(secondOnlyWriter, secondOnlyHeader);
          writeHeader(sameWriter, combinedHeader);
          writeHeader(differentWriter, combinedHeader);
          while (secondReader.hasNext()) {
            totalSecondCount++;
            final VcfRecord vc = secondReader.next();
            vc.addInfo("SF", "1");
            final String key = vc.getSequenceName() + "_" + vc.getOneBasedStart();
            //System.err.println("key2" + key);
            if (map.containsKey(key)) {
              final List<LineHolder> list = map.get(key);
              for (LineHolder lh : list) {
                lh.mMatched = true;
                final VcfRecord vcFirst = lh.mLine;
                vcFirst.addInfo("SF", "0");
                if (!compareAlts || comparePredictions(vcfRecordToPrediction(vcFirst), vcfRecordToPrediction(vc))) {
                  //System.err.println("same " + key);
                  sameCount++;
                  write(sameWriter, vcFirst);
                  write(sameWriter, vc);
                } else {
                  differentCount++;
                  write(differentWriter, vcFirst);
                  write(differentWriter, vc);
                }
              }
            } else {
              secondOnlyCount++;
              write(secondOnlyWriter, vc);
            }
          }
        }
      }
    }
    try (BufferedWriter firstOnlyWriter = new BufferedWriter(new OutputStreamWriter(output.outStream(FIRST_ONLY_OUT)))) {
      writeHeader(firstOnlyWriter, firstOnlyHeader);
      final List<LineHolder> firstOnlyLines = new ArrayList<>();
      for (Map.Entry<String, List<LineHolder>> e : map.entrySet()) {
        firstOnlyLines.addAll(e.getValue());
      }
      Collections.sort(firstOnlyLines);
      for (LineHolder lh : firstOnlyLines) {
        if (!lh.mMatched) {
          firstOnlyCount++;
          final VcfRecord rec = lh.mLine;
          rec.addInfo("SF", "0");
          write(firstOnlyWriter, lh.mLine);
        }
      }
    }
    if (output.isBlockCompressed()) {
      final Timer indexing = new Timer("SnpIndex");
      indexing.start();
      final String[] outputFiles = {SAME_OUT, DIFFERENT_OUT, FIRST_ONLY_OUT, SECOND_ONLY_OUT};
      for (final String outputName : outputFiles) {
        final File outFile = output.file(outputName + FileUtils.GZ_SUFFIX);
        final File tabixFile = output.file(outputName + FileUtils.GZ_SUFFIX + TabixIndexer.TABIX_EXTENSION);
        try {
          new TabixIndexer(outFile, tabixFile).saveVcfIndex();
        } catch (UnindexableDataException e) {
          Diagnostic.warning("Cannot produce TABIX index for: " + outFile.getPath() + ": " + e.getMessage());
        }
      }
      indexing.stop();
      indexing.log();
    }
    if (firstOnlyCount + sameCount + differentCount != totalFirstCount) {
      Diagnostic.warning("Counts not adding up for first file, probably due to complex calls/region");
    }

    if (secondOnlyCount + sameCount + differentCount != totalSecondCount) {
      Diagnostic.warning("Counts not adding up for second file, probably due to complex calls/region");
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("Total results in first file           : ").append(totalFirstCount).append(StringUtils.LS);
    sb.append("Total results in second file          : ").append(totalSecondCount).append(StringUtils.LS);
    sb.append("Number of results same                : ").append(sameCount).append(StringUtils.LS);
    if (compareAlts) {
      sb.append("Number of results different           : ").append(differentCount).append(StringUtils.LS);
    }
    sb.append("Number of results only in first file  : ").append(firstOnlyCount).append(StringUtils.LS);
    sb.append("Number of results only in second file : ").append(secondOnlyCount).append(StringUtils.LS);
    writeSummary(out, sb.toString());
    try (OutputStream summary = new BufferedOutputStreamFix(new FileOutputStream(output.file(CommonFlags.SUMMARY_FILE)))) {
      writeSummary(summary, sb.toString());
    }
  }

  private String vcfRecordToPrediction(VcfRecord rec) {
    final StringBuilder sb = new StringBuilder();
    final String[] preds = rec.getFormatAndSample().get(VcfUtils.FORMAT_GENOTYPE).get(0).split("[/|]");
    for (int i = 0; i < preds.length; i++) {
      final int pred = ".".equals(preds[i]) ? 0 : Integer.parseInt(preds[i]);
      if (i > 0) {
        sb.append(":");
      }
      if (pred == 0) {
        sb.append(rec.getRefCall());
      } else {
        sb.append(rec.getAltCalls().get(pred - 1));
      }
    }
    return sb.toString();
  }

  private int loadFirstSnps(VcfReader vorr, HashMap<String, List<LineHolder>> map) throws IOException {
    int totalFirstCount = 0;
    Diagnostic.progress("Start loading first file...");

    while (vorr.hasNext()) {
      totalFirstCount++;
      final VcfRecord vc = vorr.next();
      //map.put(l.getReference() + l.getLocation(), value)
      final String key = vc.getSequenceName() + "_" + vc.getOneBasedStart();
      //System.err.println("key1" + key);
      if (map.containsKey(key)) {
        final List<LineHolder> list = map.get(key);
        list.add(new LineHolder(vc));
      } else {
        final List<LineHolder> list = new ArrayList<>();
        list.add(new LineHolder(vc));
        map.put(key, list);
      }
    }
    return totalFirstCount;
  }

  protected static boolean comparePredictions(String prediction, String prediction2) {
    return prediction.equals(prediction2) || prediction.equals(reverseDiploidPrediction(prediction2));
  }

  protected static String reverseDiploidPrediction(String source) {
    final String[] parts = StringUtils.split(source, ':');
    if (parts.length == 2) {
      return parts[1] + ":" + parts[0];
    } else {
      return source;
    }
  }

  private void writeSummary(OutputStream reportStream, String lines) throws IOException {
    if (reportStream != null) {
      reportStream.write(lines.getBytes());
    }
  }

  private void write(BufferedWriter w, VcfRecord vc) throws IOException {
    if (w != null) {
      w.write(vc.toString());
      w.newLine();
    }
  }

  private void writeHeader(BufferedWriter w, VcfHeader header) throws IOException {
    w.write(header.toString());
  }


  protected static class LineHolder implements Comparable<LineHolder> {
    static int sInstances;
    static synchronized int instanceNumber() {
      return ++sInstances;
    }

    final VcfRecord mLine;
    final int mInstance = instanceNumber();
    boolean mMatched;

    public LineHolder(VcfRecord vc) {
      mLine = vc;
    }

    @Override
    public int compareTo(LineHolder o) {
      return mInstance - o.mInstance;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LineHolder) {
        return compareTo((LineHolder) obj) == 0;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return mInstance;
    }
  }
}
