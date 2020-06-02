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
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.io.LineWriter;

/**
 * Wrapper for writing SDF as FASTQ that can handle single end or paired end data
 */
@TestClass("com.rtg.reader.Sdf2FastqTest")
public final class FastqWriterWrapper extends FastaWriterWrapper {

  private static class FastqWriterFactory implements WriterFactory {
    private final int mLineLength; // Maximum residues per line -- 0 denotes infinite line length
    private final byte mDefaultQuality;
    FastqWriterFactory(int lineLength, byte defaultQuality) {
      mLineLength = lineLength;
      mDefaultQuality = defaultQuality;
    }

    @Override
    public SequenceWriter make(LineWriter w) {
      return new FastqWriter(w, mLineLength, mDefaultQuality, true);
    }
  }


  /**
   * Convenience wrapper for writing.
   * @param baseOutput base output file name.
   * @param reader the reader that this writer is writing from.
   * @param lineLength the maximum line length, 0 means no bound.
   * @param rename if true, rename sequences to their sequence id
   * @param gzip if true, compress the output.
   * @param def the default quality value to use if input data does not contain quality scores, 0 - 63.
   * @param interleavePaired if true, paired end output should be interleaved into a single output
   * @throws IOException if there is a problem constructing the writer.
   */
  public FastqWriterWrapper(File baseOutput, SdfReaderWrapper reader, int lineLength, boolean rename, boolean gzip, int def, boolean interleavePaired) throws IOException {
    super(reader, baseOutput, new FastqWriterFactory(lineLength, (byte) def), rename, gzip, interleavePaired, FastqUtils.extensions());
    if (reader.type() != SequenceType.DNA) {
      throw new InvalidParamsException(ErrorType.INFO_ERROR, "The input SDF contains protein data, which cannot be converted to FASTQ.");
    }
    if (!reader.hasQualityData() && def < 0) {
      throw new InvalidParamsException(ErrorType.INFO_ERROR, "The input SDF does not have quality data and no default was provided.");
    }
  }

  @Override
  protected void writeSequence(SequencesReader reader, long seqId, SequenceWriter writer, byte[] dataBuffer, byte[] qualityBuffer) throws IOException {
    assert reader.hasQualityData() == (qualityBuffer != null);
    if (reader.hasQualityData()) {
      reader.readQuality(seqId, qualityBuffer);
    }
    super.writeSequence(reader, seqId, writer, dataBuffer, qualityBuffer);
  }

}

