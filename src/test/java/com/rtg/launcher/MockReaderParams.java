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
package com.rtg.launcher;

import java.io.File;
import java.io.IOException;

import com.rtg.mode.SequenceType;
import com.rtg.reader.MockSequencesReader;
import com.rtg.reader.SequencesReader;

/**
 */
public class MockReaderParams extends ReaderParams {

  private final long mMaxLength;

  private final SequencesReader mReader;

  /**
   * Create a new {@link MockReaderParams}
   * @param length mock length for the mock reader.
   * @param numberSequences number of sequences.
   * @param codeType {@link SequenceType}
   */
  public MockReaderParams(final long length, final long numberSequences, final SequenceType codeType) {
    this(new MockSequencesReader(codeType, numberSequences, length));
  }

  /**
   * Create a new {@link MockReaderParams}
   * @param reader MockReader
   */
  public MockReaderParams(final SequencesReader reader) {
    mReader = reader;
    long mx;
    try {
      mx = reader.maxLength();
    } catch (final UnsupportedOperationException e) {
      mx = -1;
    }
    mMaxLength = mx;
  }

  /**
   * @see ReaderParams#close()
   */
  @Override
  public void close() {
  }

  /**
   * @see ReaderParams#directory()
   */
  @Override
  public File directory() {
    //the serialize-r checks this
    return new File("temp");
  }

  /**
   * @see ReaderParams#maxLength()
   */
  @Override
  public long maxLength() {
    if (mMaxLength == -1) {
      throw new UnsupportedOperationException();
    }
    return mMaxLength;
  }

  /**
   * @see ReaderParams#reader()
   */
  @Override
  public SequencesReader reader() {
    return mReader;
  }

  @Override
  public int[] lengths() throws IOException {
    final int n = (int) mReader.numberSequences();
    final int[] lengths = new int[n];
    for (int i = 0; i < n; ++i) {
      lengths[i] = mReader.length(i);
    }
    return lengths;
  }

  void toString(final String prefix, final StringBuilder sb) {
    sb.append(prefix).append("ReaderParams directory=").append(directory());
  }

  @Override
  public int hashCode() {
    return directory().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    final MockReaderParams that = (MockReaderParams) obj;
    return this.directory().equals(that.directory());
  }

}

