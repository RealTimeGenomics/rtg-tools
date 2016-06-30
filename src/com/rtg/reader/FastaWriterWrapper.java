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
import java.io.OutputStreamWriter;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;

/**
 * Wrapper for writing FASTA that can handle single end or paired end data
 */
@TestClass("com.rtg.reader.Sdf2FastaTest")
public class FastaWriterWrapper implements WriterWrapper {


  private final boolean mIsPaired;
  private final SdfReaderWrapper mReader;
  protected final int mLineLength; // Maximum residues per line -- 0 denotes infinite line length
  protected final boolean mRename; // Replace sequence names with their SDF sequence ID
  protected final boolean mHasNames;
  protected final byte[] mCodeToBytes;

  private final LineWriter mLeft;
  private final LineWriter mRight;
  private final LineWriter mSingle;


  /**
   * Convenience wrapper for writing.
   * @param baseOutput base output file name.
   * @param reader the reader that this writer is writing from.
   * @param lineLength the maximum line length, 0 means no bound.
   * @param rename if true, rename sequences to their sequence id
   * @param gzip if true, compress the output.
   * @param interleavePaired if true, paired end output should be interleaved into a single output
   * @throws IOException if there is a problem constructing the writer.
   */
  public FastaWriterWrapper(File baseOutput, SdfReaderWrapper reader, int lineLength, boolean rename, boolean gzip, boolean interleavePaired) throws IOException {
    this(baseOutput, reader, lineLength, rename, gzip, interleavePaired, FastaUtils.extensions());
  }

  protected FastaWriterWrapper(File baseOutput, SdfReaderWrapper reader, int lineLength, boolean rename, boolean gzip, boolean interleavePaired, String[] extensions) throws IOException {
    assert reader != null;
    assert extensions.length > 0;
    mReader = reader;
    mIsPaired = reader.isPaired();
    mHasNames = reader.hasNames();
    final long maxLength = reader.maxLength();
    if (maxLength > Integer.MAX_VALUE) {
      throw new NoTalkbackSlimException(ErrorType.SEQUENCE_LENGTH_ERROR);
    }
    mCodeToBytes = SdfSubseq.getByteMapping(reader.type(), false);
    mLineLength = lineLength;
    mRename = rename;


    final BaseFile baseFile = FileUtils.getBaseFile(baseOutput, gzip, extensions);

    if (mIsPaired) {
      mSingle = null;
      if (interleavePaired) {
        mLeft = getStream(baseFile, "");
        mRight = mLeft;
      } else {
        if (CommonFlags.isStdio(baseOutput)) {
          throw new NoTalkbackSlimException("Sending non-interleaved paired-end data to stdout is not supported.");
        }
        mLeft = getStream(baseFile, "_1");
        mRight = getStream(baseFile, "_2");
      }
    } else {
      mLeft = null;
      mRight = null;
      mSingle = getStream(baseFile, "");
    }
  }

  static LineWriter getStream(final BaseFile baseFile, String fileSuffix) throws IOException {
    if (CommonFlags.isStdio(baseFile.getBaseFile())) {
      return new LineWriter(new OutputStreamWriter(FileUtils.getStdoutAsOutputStream()));
    }
    return new LineWriter(new OutputStreamWriter(FileUtils.createOutputStream(baseFile.suffixedFile(fileSuffix), baseFile.isGzip())));
  }

  @Override
  public void writeSequence(long seqId, byte[] dataBuffer, byte[] qualityBuffer) throws IllegalStateException, IOException {
    if (mIsPaired) {
      writeSequence(mReader.left(), seqId, mLeft, dataBuffer, qualityBuffer);
      writeSequence(mReader.right(), seqId, mRight, dataBuffer, qualityBuffer);
    } else {
      writeSequence(mReader.single(), seqId, mSingle, dataBuffer, qualityBuffer);
    }
  }

  protected void writeSequence(SequencesReader reader, long seqId, LineWriter writer, byte[] dataBuffer, byte[] qualityBuffer) throws IllegalArgumentException, IllegalStateException, IOException {
    final int length = reader.read(seqId, dataBuffer);
    for (int i = 0; i < length; i++) {
      dataBuffer[i] = mCodeToBytes[dataBuffer[i]];
    }
    final String name = !mRename && mHasNames ? reader.fullName(seqId) : ("" + seqId);
    writer.writeln(">" + name);
    if (mLineLength == 0) {
      writer.writeln(new String(dataBuffer, 0, length));
    } else {
      for (long k = 0; k < length; k += mLineLength) {
        writer.writeln(new String(dataBuffer, (int) k, Math.min(mLineLength, length - (int) k)));
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (mLeft != null) {
      mLeft.close();
    }
    if (mRight != null) {
      mRight.close();
    }
    if (mSingle != null) {
      mSingle.close();
    }
  }
}

