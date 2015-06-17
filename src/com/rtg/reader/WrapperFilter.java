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
package com.rtg.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;

/**
 * Facilitates transferring sequences by ID from a SdfReaderWrapper to a WriterWrapper.
 */
@TestClass("com.rtg.reader.SdfSubsetTest")
class WrapperFilter {

  private final SdfReaderWrapper mReader;
  private final WriterWrapper mWriter;

  private byte[] mData = null;
  private byte[] mQualities = null;
  protected int mWarnCount = 0;
  private long mWritten = 0;

  WrapperFilter(SdfReaderWrapper reader, WriterWrapper writer) {
    mReader = reader;
    mWriter = writer;
  }

  /**
   * @return the number of sequences written to the WriterWrapper
   */
  public long getWritten() {
    return mWritten;
  }

  protected void warnInvalidSequence(String seqid) {
    if (mWarnCount < 5) {
      Diagnostic.warning("Invalid sequence id " + seqid + ", must be from 0 to " + (mReader.numberSequences() - 1));
      mWarnCount++;
      if (mWarnCount == 5) {
        Diagnostic.warning("(Only the first 5 messages shown.)");
      }
    } else {
      Diagnostic.userLog("Invalid sequence id " + seqid);
    }
  }

  /**
   * Transfer the sequence with the specified ID from the reader to the writer.
   * @param seqid the sequence ID
   * @throws IOException if there was a problem during writing
   */
  protected void transfer(long seqid) throws IOException {
    if ((seqid < 0) || (seqid >= mReader.numberSequences())) {
      warnInvalidSequence("" + seqid);
      return;
    }
    final int length = mReader.maxLength();
    if (mData == null || mData.length < length) {
      mData = new byte[length];
      if (mReader.hasQualityData()) {
        mQualities = new byte[length];
      }
    }
    mWriter.writeSequence(seqid, mData, mQualities);
    if (++mWritten % 1000 == 0) {
      Diagnostic.progress("Extracted " + mWritten + " sequences");
    }
  }

  /**
   * Transfer an interpreted sequence or set of sequences from the reader to the writer.
   * This implementation allows IDs to be specified as a range, e.g. 123-456
   * @param seqRange the sequence id specifier
   * @throws IOException if there was a problem during writing
   */
  protected void transfer(String seqRange) throws IOException {
    try {
      final int rangePos = seqRange.indexOf('-');
      if (rangePos == -1) {
        transfer(Long.parseLong(seqRange));
      } else {
        final String start = seqRange.substring(0, rangePos);
        final String end = seqRange.substring(rangePos + 1);
        final long startIdx;
        if (start.length() == 0) {
          startIdx = 0;
        } else {
          startIdx = Long.parseLong(start);
        }
        final long endIdx;
        if (end.length() == 0) {
          endIdx = mReader.numberSequences() - 1;
        } else {
          endIdx = Long.parseLong(end);
        }
        if (startIdx > endIdx) {
          throw new NumberFormatException("Invalid range: " + seqRange);
        }
        //System.out.println("Getting sequences from " + startIdx + " to " + endIdx);
        for (long i = startIdx; i <= endIdx; i++) {
          transfer(i);
        }
      }
    } catch (final NumberFormatException e) {
      warnInvalidSequence(seqRange);
    }
  }

  /**
   * Transfer all the sequences listed in the supplied file, interpreting entries appropriately.
   * @param idFile file containing sequences to transfer, one per line.
   * @throws IOException if there was a problem during writing
   */
  protected void transferFromFile(File idFile) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(idFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.length() > 0) {
          transfer(line);
        }
      }
    }
  }


  /**
   * Transfer all the sequences listed in the specified range
   * @param range the range of ids to transfer.
   * @throws IOException if there was a problem during writing
   */
  protected void transfer(LongRange range) throws IOException {
    final LongRange r = SequencesReaderFactory.resolveRange(range, mReader.numberSequences());
    for (long seq = r.getStart(); seq < r.getEnd(); seq++) {
      transfer(seq);
    }
  }
}
