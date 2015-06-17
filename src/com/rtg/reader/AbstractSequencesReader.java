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
import com.rtg.util.io.FileUtils;

/**
 * Base class to make implementation of readers easier
 */
@TestClass(value = {"com.rtg.reader.DefaultSequencesReaderTest", "com.rtg.reader.CompressedMemorySequencesReaderTest"})
public abstract class AbstractSequencesReader implements AnnotatedSequencesReader {

  private static final String README_FILENAME = "readme.txt";

  @Override
  public abstract IndexFile index();

  @Override
  public SequencesIterator iterator() {
    return new DefaultSequencesIterator(this);
  }


  // For Direct sequence access

  @Override
  public String fullName(long sequenceIndex) throws IOException {
    return name(sequenceIndex) + nameSuffix(sequenceIndex);
  }

  @Override
  public byte[] read(long sequenceIndex) throws IllegalArgumentException, IOException {
    final byte[] dataOut = new byte[length(sequenceIndex)];
    read(sequenceIndex, dataOut);
    return dataOut;
  }

  @Override
  public byte[] readQuality(long sequenceIndex) throws IllegalArgumentException, IOException {
    final byte[] dataOut = new byte[length(sequenceIndex)];
    readQuality(sequenceIndex, dataOut);
    return dataOut;
  }

  @Override
  public long[] residueCounts() {
    return index().getResidueCounts();
  }

  @Override
  public long dataChecksum() {
    return index().getDataChecksum();
  }
  @Override
  public long qualityChecksum() {
    return index().getQualityChecksum();
  }
  @Override
  public long nameChecksum() {
    return index().getNameChecksum();
  }


  @Override
  public long[] histogram() {
    return index().getNHistogram();
  }

  @Override
  public long[] posHistogram() {
    return index().getPosHistogram();
  }

  @Override
  public double globalQualityAverage() {
    return index().getQSAverage();
  }

  @Override
  public boolean hasHistogram() {
    return index().hasNStats();
  }

  @Override
  public long longestNBlock() {
    return index().getLongestNBlock();
  }

  @Override
  public long nBlockCount() {
    return index().getNBlockCount();
  }

  @Override
  public PrereadArm getArm() {
    return index().getPrereadArm();
  }

  @Override
  public PrereadType getPrereadType() {
    return index().getPrereadType();
  }

  @Override
  public SdfId getSdfId() {
    return index().getSdfId();
  }

  @Override
  public double[] positionQualityAverage() {
    return index().getQSPositionAverageHistogram();
  }

  @Override
  public long sdfVersion() {
    return index().getVersion();
  }

  @Override
  public String comment() {
    return index().getComment();
  }

  @Override
  public String commandLine() {
    return index().getCommandLine();
  }

  @Override
  public String samReadGroup() {
    return index().getSamReadGroup();
  }

  @Override
  public boolean compressed() {
    return index().getSequenceEncoding() == IndexFile.SEQUENCE_ENCODING_COMPRESSED;
  }

  @Override
  public long suffixChecksum() {
    return index().getNameSuffixChecksum();
  }

  @Override
  public long totalLength() {
    return index().getTotalLength();
  }

  @Override
  public long maxLength() {
    return index().getMaxLength();
  }

  @Override
  public long minLength() {
    return index().getMinLength();
  }

  @Override
  public boolean hasNames() {
    return index().hasNames();
  }

  @Override
  public boolean hasQualityData() {
    return index().hasQuality();
  }

  @Override
  public SequenceType type() {
    return SequenceType.values()[index().getSequenceType()];
  }

  @Override
  public String getReadMe() throws IOException {
    if (path() == null) {
      return null;
    }
    final File readMe = new File(path(), README_FILENAME);
    if (!readMe.isFile()) {
      return null;
    }
    return FileUtils.fileToString(readMe);
  }
}
