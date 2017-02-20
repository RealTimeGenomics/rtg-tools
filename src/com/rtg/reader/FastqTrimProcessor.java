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

import java.util.ArrayList;
import java.util.List;

/**
 * Process a block of fastq sequences
 */
class FastqTrimProcessor implements Runnable {
  final Batch<FastqSequence> mBatch;
  final boolean mDiscardZeroLengthReads;
  final ReadTrimmer mTrimmer;
  final BatchReorderingWriter<FastqSequence> mWriter;

  /**
   *
   * @param sequences List of sequences to trim
   * @param discardZeroLengthReads true if reads that end up zero length should be discarded
   * @param trimmer the read trimmer
   * @param writer a writer
   */
  FastqTrimProcessor(Batch<FastqSequence> sequences, boolean discardZeroLengthReads, ReadTrimmer trimmer, BatchReorderingWriter<FastqSequence> writer) {
    mBatch = sequences;
    mDiscardZeroLengthReads = discardZeroLengthReads;
    mTrimmer = trimmer;
    mWriter = writer;
  }

  @Override
  public void run() {
    final List<FastqSequence> list = new ArrayList<>();
    for (FastqSequence sequence : mBatch.getItems()) {
      sequence.trim(mTrimmer);
      if (!mDiscardZeroLengthReads || sequence.length() > 0) {
        list.add(sequence);
      }
    }
    mWriter.writeBatch(new Batch<>(mBatch.getBatchNumber(), list));
  }
}
