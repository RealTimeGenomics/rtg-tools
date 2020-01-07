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

package com.rtg.vcf;

import static com.rtg.launcher.CommonFlags.BOOL;
import static com.rtg.launcher.CommonFlags.STRING_OR_FILE;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.LogStream;
import com.rtg.vcf.header.VcfHeader;

/**
 * A command for splitting a multi-sample VCF into individual single-sample VCFs.
 */
public class VcfSplitCli extends LoggedCli {

  private static final String INPUT_FLAG = "input";
  private static final String KEEP_REF = "keep-ref";
  private static final String REMOVE_SAMPLE = "remove-sample";
  private static final String KEEP_SAMPLE = "keep-sample";
  private static final String ASYNC = "Xasync";

  @Override
  public String moduleName() {
    return "vcfsplit";
  }

  @Override
  public String description() {
    return "split a multi-sample VCF into one file per sample";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Splits samples contained within a VCF into separate files, one per sample.");
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired('i', INPUT_FLAG, File.class, CommonFlags.FILE, "the input VCF, or '-' to read from standard input").setCategory(INPUT_OUTPUT);
    CommonFlags.initOutputDirFlag(mFlags);
    mFlags.registerOptional(KEEP_REF, "keep records where the sample is reference").setCategory(FILTERING);
    mFlags.registerOptional(REMOVE_SAMPLE, String.class, STRING_OR_FILE, "file containing sample IDs to ignore, or a literal sample name").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_SAMPLE, String.class, STRING_OR_FILE, "file containing sample IDs to select, or a literal sample name").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    CommonFlags.initRegionOrBedRegionsFlags(mFlags);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    mFlags.registerOptional(ASYNC, Boolean.class, BOOL, "whether to write output files asynchronously", true).setCategory(UTILITY);
    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateInputFile(flags, INPUT_FLAG)
      && CommonFlags.validateRegions(flags)
      && flags.checkAtMostOne(REMOVE_SAMPLE, KEEP_SAMPLE));
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final boolean gzip = !mFlags.isSet(CommonFlags.NO_GZIP);
    final boolean keepRef = mFlags.isSet(KEEP_REF);
    final boolean asyncOutput = (Boolean) mFlags.getValue(ASYNC);
    final VcfSubset.VcfSampleStripperFactory sampleStripperFact = new VcfSubset.VcfSampleStripperFactory(mFlags);
    try (final VcfReader reader = new VcfReaderFactory(mFlags).parser(new VcfSubsetParser(sampleStripperFact)).make(mFlags)) {
      final int numberSamples = reader.getHeader().getNumberOfSamples();
      if (numberSamples == 0) {
        throw new NoTalkbackSlimException("No samples to be output.");
      }
      final VcfWriter[] writers = new VcfWriter[numberSamples];
      final ArrayList<String> samples = new ArrayList<>(reader.getHeader().getSampleNames());
      try {
        final VcfWriterFactory f = new VcfWriterFactory(mFlags).addRunInfo(true).async(asyncOutput);
        for (int i = 0; i < numberSamples; ++i) {
          final String sample = samples.get(i);
          final File of = new File(outputDirectory(), sample);
          final File vcfFile = VcfUtils.getZippedVcfFileName(gzip, of);
          Diagnostic.userLog("Outputting sample '" + sample + "' to: " + vcfFile);
          final VcfHeader header = reader.getHeader().copy();
          header.removeAllSamples();
          header.addSampleName(sample);
          writers[i] = f.make(header, vcfFile);
        }
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          if (rec.hasFormat(VcfUtils.FORMAT_GENOTYPE)) {
            final List<String> gts = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
            final VcfRecord template = new VcfRecord(rec); // Work on a copy
            for (int i = 0; i < numberSamples; ++i) {
              final String gt = gts.get(i);
              final int[] gtArr = VcfUtils.splitGt(gt);
              if (VcfUtils.isValidGt(rec, gtArr) && VcfUtils.isNonMissingGt(gt)
                && (keepRef || hasAlt(gtArr))) {
                template.removeSamples();
                template.setNumberOfSamples(1);
                // Copy sample field values from original
                for (Map.Entry<String, ArrayList<String>> e : rec.getFormatAndSample().entrySet()) {
                  template.setFormatAndSample(e.getKey(), e.getValue().get(i), 0);
                }
                writers[i].write(asyncOutput ? new VcfRecord(template) : template);
              }
            }
          }
        }
      } finally {
        for (VcfWriter w : writers) {
          if (w != null) {
            w.close();
          }
        }
      }
    }
    return 0;
  }

  private boolean hasAlt(int[] gt) {
    for (int id : gt) {
      if (id > 0) {
        return true;
      }
    }
    return false;
  }
}
