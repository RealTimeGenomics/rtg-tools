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

import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Provides iterator-style access to a set of sequences provided by an existing SequencesReader.
 */
@TestClass("com.rtg.reader.DefaultSequencesReaderTest")
public class DefaultSequencesIterator implements SequencesIterator {

  private final SequencesReader mReader;
  private final long mNumberSequences;
  private long mSequenceId = -1;

  /**
   * Create the iterator accessor around an existing reader.
   * @param reader the reader
   */
  public DefaultSequencesIterator(SequencesReader reader) {
    mReader = reader;
    mNumberSequences = reader.numberSequences();
  }

  @Override
  public SequencesReader reader() {
    return mReader;
  }

  @Override
  public void reset() {
    mSequenceId = -1;
  }

  @Override
  public void seek(long sequenceId) throws IOException {
    mSequenceId = sequenceId;
    if (mSequenceId >= mNumberSequences || mSequenceId < 0) {
      throw new IllegalArgumentException("Failed to seek to sequence: " + sequenceId + " numberOfSequences: " + mNumberSequences);
    }
  }

  @Override
  public boolean nextSequence() throws IOException {
    return ++mSequenceId < mNumberSequences;
  }

  private void checkSequenceId() {
    if (mSequenceId >= mNumberSequences || mSequenceId < 0) {
      throw new IllegalStateException("Last call to nextSequence() or seek() failed and left current information unavailable.");
    }
  }

  @Override
  public long currentSequenceId() throws IllegalStateException {
    checkSequenceId();
    return mSequenceId;
  }

  @Override
  public int currentLength() throws IllegalStateException, IOException {
    checkSequenceId();
    return mReader.length(mSequenceId);
  }

  @Override
  public String currentName() throws IllegalStateException, IOException {
    checkSequenceId();
    return mReader.name(mSequenceId);
  }

  @Override
  public String currentFullName() throws IllegalStateException, IOException {
    checkSequenceId();
    return mReader.fullName(mSequenceId);
  }

  @Override
  public String currentNameSuffix() throws IllegalStateException, IOException {
    checkSequenceId();
    return mReader.nameSuffix(mSequenceId);
  }


  @Override
  public int readCurrent(byte[] dataOut) throws IllegalArgumentException, IllegalStateException, IOException {
    checkSequenceId();
    return mReader.read(mSequenceId, dataOut);
  }

  @Override
  public int readCurrent(byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException {
    checkSequenceId();
    return mReader.read(mSequenceId, dataOut, start, length);
  }

  @Override
  public int readCurrentQuality(byte[] dest) throws IllegalArgumentException, IllegalStateException, IOException {
    checkSequenceId();
    return mReader.readQuality(mSequenceId, dest);
  }

  @Override
  public int readCurrentQuality(byte[] dest, int start, int length) throws IllegalArgumentException, IllegalStateException, IOException {
    checkSequenceId();
    return mReader.readQuality(mSequenceId, dest, start, length);
  }

}
