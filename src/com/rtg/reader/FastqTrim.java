/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.reader;

import static com.rtg.launcher.CommonFlags.MIN_READ_LENGTH;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.QUALITY_FLAG;
import static com.rtg.sam.SamFilterOptions.SUBSAMPLE_FLAG;
import static com.rtg.sam.SamFilterOptions.SUBSAMPLE_SEED_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.sam.SamFilterOptions;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.Timer;
import com.rtg.util.io.FileUtils;
import com.rtg.util.iterators.SubsampleIterator;

/**
 * Paired-end FASTQ read trimming. Allows various fixed trimming and uses arm-to-arm alignment to
 * identify cases of read overlap and/or read-through.
 */
public final class FastqTrim extends AbstractCli {

  private static final String TRIM_START_FLAG = "trim-start-bases";
  private static final String TRIM_END_FLAG = "trim-end-bases";

  private static final String START_TRIM_THRESHOLD = "start-quality-threshold";
  private static final String END_TRIM_THRESHOLD = "end-quality-threshold";

  private static final String REVERSE_FLAG = "reverse-complement";

  static final String BATCH_SIZE = "Xbatch-size";
  static final String DISCARD_EMPTY_READS = "discard-empty-reads";


  @Override
  public String moduleName() {
    return "fastqtrim";
  }

  @Override
  public String description() {
    return "trim reads in FASTQ files";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription(StringUtils.sentencify(description()));
    mFlags.registerExtendedHelp();
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired('i', CommonFlags.INPUT_FLAG, File.class, CommonFlags.FILE, "input FASTQ file, Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, CommonFlags.FILE, "output filename. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    CommonFlags.initQualityFormatFlag(mFlags);

    mFlags.registerOptional('S', START_TRIM_THRESHOLD, Integer.class, CommonFlags.INT, "trim read starts to maximise base quality above the given threshold", 0).setCategory(FILTERING);
    mFlags.registerOptional('E', END_TRIM_THRESHOLD, Integer.class, CommonFlags.INT, "trim read ends to maximise base quality above the given threshold", 0).setCategory(FILTERING);
    mFlags.registerOptional('s', TRIM_START_FLAG, Integer.class, CommonFlags.INT, "always trim the specified number of bases from read start", 0).setCategory(FILTERING);
    mFlags.registerOptional('e', TRIM_END_FLAG, Integer.class, CommonFlags.INT, "always trim the specified number of bases from read end", 0).setCategory(FILTERING);
    CommonFlags.initMinReadLength(mFlags);
    mFlags.registerOptional(DISCARD_EMPTY_READS, "if set, discard reads that have zero length after trimming. Should not be used with paired-end data").setCategory(FILTERING);

    mFlags.registerOptional('r', REVERSE_FLAG, "if set, output in reverse complement").setCategory(UTILITY);
    CommonFlags.initThreadsFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initForce(mFlags);
    SamFilterOptions.registerSubsampleFlags(mFlags);
    mFlags.registerOptional(BATCH_SIZE, Integer.class, CommonFlags.INT, "number of reads to process per batch", 10000).setCategory(UTILITY);

    mFlags.setValidator(innerFlags ->
      CommonFlags.validateInputFile(innerFlags, CommonFlags.INPUT_FLAG)
        && CommonFlags.validateOutputFile(innerFlags, FileUtils.getOutputFileName((File) innerFlags.getValue(OUTPUT_FLAG), !innerFlags.isSet(NO_GZIP), FastqUtils.extensions()))
        && innerFlags.checkInRange(BATCH_SIZE, 1, Integer.MAX_VALUE)
        && innerFlags.checkInRange(START_TRIM_THRESHOLD, 0, Integer.MAX_VALUE)
        && innerFlags.checkInRange(END_TRIM_THRESHOLD, 0, Integer.MAX_VALUE)
        && innerFlags.checkInRange(TRIM_START_FLAG, 0, Integer.MAX_VALUE)
        && innerFlags.checkInRange(TRIM_END_FLAG, 0, Integer.MAX_VALUE)
        && innerFlags.checkInRange(MIN_READ_LENGTH, 0, Integer.MAX_VALUE)
        && innerFlags.checkInRange(SUBSAMPLE_FLAG, 0.0, 1.0)
    );
  }

  private ReadTrimmer getTrimmer() {
    return getTrimmer((Integer) mFlags.getValue(TRIM_START_FLAG),
      (Integer) mFlags.getValue(START_TRIM_THRESHOLD), (Integer) mFlags.getValue(TRIM_END_FLAG),
      (Integer) mFlags.getValue(END_TRIM_THRESHOLD), (Integer) mFlags.getValue(MIN_READ_LENGTH),
      mFlags.isSet(REVERSE_FLAG));
  }
  static ReadTrimmer getTrimmer(int start, int startThreshold, int end, int endThreshold, int minReadLength, boolean reverse) {
    final ArrayList<ReadTrimmer> trimmers = new ArrayList<>();
    // If doing fixed trimming, ensure these come first
    if (start > 0) {
      trimmers.add(new FirstBasesReadTrimmer(start));
    }
    if (end > 0) {
      trimmers.add(new LastBasesReadTrimmer(end));
    }
    // Then quality trimming
    if (startThreshold > 0) {
      trimmers.add(new BestSumReadTrimmer(startThreshold, true));
    }
    if (endThreshold > 0) {
      trimmers.add(new BestSumReadTrimmer(endThreshold, false));
    }
    if (minReadLength > 0) {
      trimmers.add(new MinLengthReadTrimmer(minReadLength));
    }
    if (reverse) {
      trimmers.add(new ReverseComplementReadTrimmer());
    }
    if (trimmers.size() == 1) {
      return trimmers.get(0);
    } else if (trimmers.size() > 1) {
      return new MultiReadTrimmer(trimmers.toArray(new ReadTrimmer[0]));
    } else {
      return new NullReadTrimmer();
    }
  }

  static QualityFormat qualityFlagToFastQScoreType(String qualityFormat) {
    switch (qualityFormat) {
      case CommonFlags.SANGER_FORMAT:
        return QualityFormat.SANGER;
      case CommonFlags.SOLEXA_FORMAT:
        return QualityFormat.SOLEXA;
      case CommonFlags.ILLUMINA_FORMAT:
        return QualityFormat.SOLEXA1_3;
      default:
        throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Invalid quality format=" + qualityFormat);
    }
  }

  static <T> Iterator<T> maybeSubsample(CFlags flags, Iterator<T> inner) {
    if (flags.isSet(SUBSAMPLE_FLAG)) {
      final PortableRandom r = (flags.isSet(SUBSAMPLE_SEED_FLAG)) ? new PortableRandom((Integer) flags.getValue(SUBSAMPLE_SEED_FLAG)) : new PortableRandom();
      final double fraction = (Double) flags.getValue(SUBSAMPLE_FLAG);
      return new SubsampleIterator<>(inner, r, fraction);
    }
    return inner;
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final File output = FileUtils.getOutputFileName((File) mFlags.getValue(OUTPUT_FLAG), gzip, FastqUtils.extensions()) ;
    final QualityFormat encoding = qualityFlagToFastQScoreType((String) mFlags.getValue(QUALITY_FLAG));
    final int batchSize = (Integer) mFlags.getValue(BATCH_SIZE);
    final boolean discardZeroLengthReads = mFlags.isSet(DISCARD_EMPTY_READS);

    // All trimming and aligning is done in separate threads from reading
    final int threads = CommonFlags.parseThreads((Integer) mFlags.getValue(CommonFlags.THREADS_FLAG));
    final ReadTrimmer trimmer = getTrimmer();
    final File inFile = (File) mFlags.getValue(CommonFlags.INPUT_FLAG);
    final boolean stdin = FileUtils.isStdio(inFile);
    try (final SequenceDataSource fastqReader = new FastqSequenceDataSource(Collections.singletonList(stdin ? System.in : FileUtils.createInputStream(inFile, true)), encoding)) {

      final Timer t = new Timer("FastqPairTrimmer");
      t.start();
      try (final AsyncFastqSequenceWriter writer = new AsyncFastqSequenceWriter(new FastqWriter(new OutputStreamWriter(FileUtils.createOutputStream(output))))) {
        final BatchReorderingWriter<FastqSequence> batchWriter = new BatchReorderingWriter<>(writer);
        final Function<Batch<FastqSequence>, FutureTask<?>> listRunnableFunction = batch -> new FutureTask<>(new FastqTrimProcessor(batch, discardZeroLengthReads, trimmer, batchWriter), null);
        final BatchProcessor<FastqSequence> processor = new BatchProcessor<>(listRunnableFunction, threads, batchSize);
        processor.process(maybeSubsample(mFlags, new FastqIterator(fastqReader)));
      }
      t.log();
    }
    return 0;
  }
}


