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

import java.io.File;
import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.SequenceType;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.io.LineWriter;

/**
 * Wrapper for writing FASTA that can handle single end or paired end data
 */
@TestClass("com.rtg.reader.Sdf2FastqTest")
public final class FastqWriterWrapper extends FastaWriterWrapper {

  private static final String[] EXTS = {".fastq", ".fq"};

  private String mDefaultQualities = null;

  /**
   * Convenience wrapper for writing.
   * @param baseOutput base output file name.
   * @param reader the reader that this writer is writing from.
   * @param lineLength the maximum line length, 0 means no bound.
   * @param rename if true, rename sequences to their sequence id
   * @param gzip if true, compress the output.
   * @param def the default quality value to use if input data does not contain quality scores.
   * @param interleavePaired if true, paired end output should be interleaved into a single output
   * @throws IOException if there is a problem constructing the writer.
   */
  public FastqWriterWrapper(File baseOutput, SdfReaderWrapper reader, int lineLength, boolean rename, boolean gzip, int def, boolean interleavePaired) throws IOException {
    super(baseOutput, reader, lineLength, rename, gzip, interleavePaired, EXTS);
    if (reader.type() != SequenceType.DNA) {
      throw new InvalidParamsException(ErrorType.INFO_ERROR, "The input SDF contains protein data, which cannot be converted to FASTQ.");
    }
    if (!reader.hasQualityData()) {
      if (def >= (int) '!') {
        mDefaultQualities = StringUtils.getCharString((char) def, reader.maxLength());
      } else {
        throw new InvalidParamsException(ErrorType.INFO_ERROR, "The input SDF does not have quality data and no default was provided.");
      }
    }
  }

  @Override
  protected void writeSequence(SequencesReader reader, long seqId, LineWriter writer, byte[] dataBuffer, byte[] qualityBuffer) throws IllegalArgumentException, IllegalStateException, IOException {
    final int length = reader.read(seqId, dataBuffer);
    for (int i = 0; i < length; i++) {
      dataBuffer[i] = mCodeToBytes[dataBuffer[i]];
    }
    final String name = !mRename && mHasNames ? reader.fullName(seqId) : ("" + seqId);
    writer.writeln("@" + name);
    if (mLineLength == 0) {
      writer.writeln(new String(dataBuffer, 0, length));
      writer.writeln("+" + name);
      writer.writeln(getScore(reader, seqId, qualityBuffer));
    } else {
      for (long k = 0; k < length; k += mLineLength) {
        writer.writeln(new String(dataBuffer, (int) k, Math.min(mLineLength, length - (int) k)));
      }
      writer.writeln("+" + name);
      final String qual = getScore(reader, seqId, qualityBuffer);
      final int qualLen = qual.length();
      for (long i = 0; i < qualLen; i += mLineLength) {
        writer.writeln(qual.substring((int) i, (int) Math.min(i + mLineLength, qualLen)));
      }
    }
  }

  private String getScore(final SequencesReader read, long seqId, byte[] qualityBuffer) throws IOException {
    if (read.hasQualityData()) {
      final int length = read.readQuality(seqId, qualityBuffer);
      return FastaUtils.rawToAsciiString(qualityBuffer, length);
    } else {
      return mDefaultQualities.substring(0, read.length(seqId));
    }
  }

}

