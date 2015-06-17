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
 * Extend this if you want to make some kind of mock SequencesReader for testing.
 */
public abstract class DummySequencesReader implements SequencesReader {

  @Override
  public long maxLength() {
    return 0;
  }

  @Override
  public void close() {  }
  @Override
  public SequencesReader copy() {
    return this;
  }
  @Override
  public long dataChecksum() {
    return 0;
  }
  @Override
  public File path() {
    return null;
  }
  @Override
  public PrereadArm getArm() {
    return null;
  }
  @Override
  public PrereadType getPrereadType() {
    return null;
  }
  @Override
  public double globalQualityAverage() {
    return 0;
  }
  @Override
  public SdfId getSdfId() {
    return new SdfId(0);
  }
  @Override
  public boolean hasHistogram() {
    return false;
  }
  @Override
  public boolean hasNames() {
    return false;
  }
  @Override
  public boolean hasQualityData() {
    return false;
  }
  @Override
  public long[] histogram() {
    return null;
  }


  @Override
  public int length(long sequenceIndex) {
    return 0;
  }
  @Override
  public long lengthBetween(long start, long end) {
    return 0;
  }
  @Override
  public long longestNBlock() {
    return 0;
  }
  @Override
  public long minLength() {
    return 0;
  }
  @Override
  public long nameChecksum() {
    return 0;
  }
  @Override
  public PrereadNamesInterface names() {
    return null;
  }
  @Override
  public long nBlockCount() {
    return 0;
  }

  @Override
  public long numberSequences() {
    return 0;
  }
  @Override
  public long[] posHistogram() {
    return null;
  }
  @Override
  public double[] positionQualityAverage() {
    return null;
  }
  @Override
  public long qualityChecksum() {
    return 0;
  }
  @Override
  public String name(long index) {
    return null;
  }
  @Override
  public String fullName(long sequenceIndex) {
    return name(sequenceIndex);
  }
  @Override
  public byte[] read(long index) {
    return null;
  }
  @Override
  public int read(long sequenceIndex, byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException {
    return 0;
  }
  @Override
  public int read(long sequenceIndex, byte[] dataOut) throws IllegalArgumentException, IOException {
    return 0;
  }
  @Override
  public byte[] readQuality(long index) {
    return null;
  }
  @Override
  public int readQuality(long sequenceIndex, byte[] dest) throws IllegalArgumentException, IOException {
    return 0;
  }
  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IllegalArgumentException, IllegalStateException, IOException {
    return 0;
  }
  @Override
  public long[] residueCounts() {
    return null;
  }
  @Override
  public long sdfVersion() {
    return 0;
  }
  @Override
  public int[] sequenceLengths(long start, long end) {
    return null;
  }
  @Override
  public long totalLength() {
    return 0;
  }
  @Override
  public SequenceType type() {
    return null;
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
    return null;
  }
  @Override
  public IndexFile index() {
    return null;
  }
}
