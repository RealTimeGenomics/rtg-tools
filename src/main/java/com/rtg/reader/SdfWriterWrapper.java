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
import com.rtg.util.Constants;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.io.FileUtils;

/**
 * Wrapper for SDF writers that can handle single end or paired end reads
 *
 */
@TestClass(value = {"com.rtg.reader.SdfSplitterTest", "com.rtg.reader.SdfSubsetTest"})
public final class SdfWriterWrapper implements WriterWrapper {

  private final boolean mIsPaired;
  private final boolean mHasQuality;
  private final boolean mHasNames;
  private SdfReaderWrapper mReader;
  final SdfWriter mLeft;
  final SdfWriter mRight;
  final SdfWriter mSingle;

  /**
   * Convenience wrapper for writing.
   * @param baseDir base directory.
   * @param reader the reader that this writer is writing from.
   * @param forceCompression Force SDF to be compressed
   * @throws IOException if there are problems writing to the output SDFs
   */
  public SdfWriterWrapper(File baseDir, SdfReaderWrapper reader, boolean forceCompression) throws IOException {
    assert reader != null;
    mReader = reader;
    mIsPaired = reader.isPaired();
    mHasQuality = reader.hasQualityData();
    mHasNames = reader.hasNames();

    if (mIsPaired) {
      FileUtils.ensureOutputDirectory(baseDir);
      mSingle = null;
      mLeft = new SdfWriter(new File(baseDir, "left"), Constants.MAX_FILE_SIZE, reader.getPrereadType(), mHasQuality, mHasNames, forceCompression || reader.left().compressed(), reader.type());
      copyMetadata(reader.left(), mLeft);
      mRight = new SdfWriter(new File(baseDir, "right"), Constants.MAX_FILE_SIZE, reader.getPrereadType(), mHasQuality, mHasNames, forceCompression || reader.right().compressed(), reader.type());
      copyMetadata(reader.right(), mRight);
      mRight.setSdfId(mLeft.getSdfId());
    } else {
      mLeft = null;
      mRight = null;
      final SequencesReader single = reader.single();
      mSingle = new SdfWriter(baseDir, Constants.MAX_FILE_SIZE, reader.getPrereadType(), mHasQuality, mHasNames, forceCompression || single.compressed(), reader.type());
      copyMetadata(single, mSingle);
    }
    setSdfId(new SdfId());
  }

  private static void copyMetadata(SequencesReader src, SdfWriter dest) throws IOException {
    dest.setPrereadArm(src.getArm());
    dest.setCommandLine(CommandLine.getCommandLine());
    dest.setReadGroup(src.index().getSamReadGroup());

    // Preserve record of which genome this SDF was derived from during simulation
    SourceTemplateReadWriter.writeTemplateMappingFile(dest.directory(), SourceTemplateReadWriter.readTemplateMap(src.path()));
    SourceTemplateReadWriter.copyMutationMappingFile(src.path(), dest.directory());
  }

  /**
   * Set a new reader from which sequences will be selected -- only
   * needed for <code>sdfsplit</code> during merge operation!
   * @param reader the new reader to supply sequence data.
   */
  void setReader(SdfReaderWrapper reader) {
    mReader = reader;
  }

  /**
   * Convenience method.
   * @param id the identifier.
   */
  public void setSdfId(SdfId id) {
    if (mIsPaired) {
      mLeft.setSdfId(id);
      mRight.setSdfId(id);
    } else {
      mSingle.setSdfId(id);
    }
  }

  @Override
  public void writeSequence(long seqId, byte[] dataBuffer, byte[] qualityBuffer) throws IOException {
    if (mIsPaired) {
      writeSequence(mReader.left(), seqId, mLeft, dataBuffer, qualityBuffer);
      writeSequence(mReader.right(), seqId, mRight, dataBuffer, qualityBuffer);
    } else {
      writeSequence(mReader.single(), seqId, mSingle, dataBuffer, qualityBuffer);
    }
  }

  private void writeSequence(SequencesReader reader, long seqId, SdfWriter writer, byte[] dataBuffer, byte[] qualityBuffer) throws IOException {
    final int length = reader.read(seqId, dataBuffer);
    if (mHasQuality) {
      reader.readQuality(seqId, qualityBuffer);
    }
    writer.startSequence(mHasNames ? reader.fullName(seqId) : null);
    writer.write(dataBuffer, mHasQuality ? qualityBuffer : null, length);
    writer.endSequence();
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

