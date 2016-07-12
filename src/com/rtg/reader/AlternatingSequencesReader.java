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

import com.rtg.mode.SequenceType;

/**
 * Made for a special purpose to behave like two sequence readers are interleaved.
 * If you want more functionality you'll have to implement it.
 */
public class AlternatingSequencesReader implements SequencesReader {

  private final SequencesReader mFirst;
  private final SequencesReader mSecond;

  /**
   * Constructs a sequence reader which alternates between two given sequence readers.
   * @param first the first sequence reader
   * @param second the second sequence reader
   */
  public AlternatingSequencesReader(final SequencesReader first, final SequencesReader second) {
    mFirst = first;
    mSecond = second;
  }

  private SequencesReader select(long sequenceIndex) {
    return (sequenceIndex & 1L) == 0 ? mFirst : mSecond;
  }

  @Override
  public long dataChecksum() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  @Override
  public long qualityChecksum() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  @Override
  public long nameChecksum() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void close() throws IOException {
    try {
      mFirst.close();
    } finally {
      mSecond.close();
    }
  }

  @Override
  public SequencesReader copy() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public File path() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public PrereadArm getArm() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public PrereadType getPrereadType() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public double globalQualityAverage() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public SdfId getSdfId() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean hasHistogram() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean hasQualityData() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean hasNames() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long[] histogram() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long lengthBetween(long start, long end) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long longestNBlock() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long maxLength() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long minLength() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long nBlockCount() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public PrereadNames names() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long numberSequences() {
    return mFirst.numberSequences() + mSecond.numberSequences();
  }

  @Override
  public long[] posHistogram() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public double[] positionQualityAverage() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int length(long sequenceIndex) throws IOException {
    return select(sequenceIndex).length(sequenceIndex / 2);
  }

  @Override
  public byte sequenceDataChecksum(long sequenceIndex) throws IOException {
    return select(sequenceIndex).sequenceDataChecksum(sequenceIndex / 2);
  }

  @Override
  public String name(long sequenceIndex) throws IOException {
    return select(sequenceIndex).name(sequenceIndex / 2);
  }
  @Override
  public String fullName(long sequenceIndex) throws IOException {
    return name(sequenceIndex);
  }

  @Override
  public byte[] read(long sequenceIndex) throws IllegalStateException, IOException {
    final byte[] result = new byte[length(sequenceIndex)];
    read(sequenceIndex, result);
    return result;
  }
  @Override
  public int read(long sequenceIndex, byte[] dataOut) throws IllegalArgumentException, IOException {
    return read(sequenceIndex, dataOut, 0, length(sequenceIndex));
  }
  @Override
  public int read(long sequenceIndex, byte[] dataOut, int start, int length) throws IllegalArgumentException, IllegalStateException, IOException {
    return select(sequenceIndex).read(sequenceIndex, dataOut, start, length);
  }


  @Override
  public byte[] readQuality(long sequenceIndex) throws IOException {
    final byte[] result = new byte[length(sequenceIndex)];
    readQuality(sequenceIndex, result);
    return result;
  }
  @Override
  public int readQuality(long sequenceIndex, byte[] dest) throws IllegalArgumentException, IOException {
    return readQuality(sequenceIndex, dest, 0, length(sequenceIndex));
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IllegalArgumentException, IOException {
    return select(sequenceIndex).readQuality(sequenceIndex, dest, start, length);
  }

  @Override
  public long[] residueCounts() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long sdfVersion() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int[] sequenceLengths(long start, long end) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long totalLength() {
    return mFirst.totalLength() + mSecond.totalLength();
  }

  @Override
  public SequenceType type() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean compressed() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String nameSuffix(long sequenceIndex) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long suffixChecksum() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getReadMe() {
    return null;
  }

  @Override
  public SequencesIterator iterator() {
    return new DefaultSequencesIterator(this);
  }

  @Override
  public IndexFile index() {
    return null;
  }

}
