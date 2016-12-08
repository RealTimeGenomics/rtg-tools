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

import com.rtg.mode.SequenceType;

/**
 * Mock for constructing tests.
 * The lengths of the reads are specified but will fail if any attempt is made to get at the contents.
 */
public class MockArraySequencesReader extends MockSequencesReader {

  private final String[] mNames;

  private final long mMaxLength;

  private final long mMinLength;


  /**
   * @param sequenceType sequence type.
   * @param length number of sequences.
   */
  public MockArraySequencesReader(final SequenceType sequenceType, final long length) {
    this(sequenceType, length, 1);
  }

  /**
   * @param sequenceType sequence type.
   * @param length number of sequences.
   * @param maxLength maximum length
   */
  public MockArraySequencesReader(final SequenceType sequenceType, final long length, final long maxLength) {
    super(sequenceType, length);
    mNames = null;
    mMaxLength = maxLength;
    mMinLength = -1;
  }

  /**
   * @param sequenceType sequence type.
   * @param seqLengths lengths of sequences.
   */
  public MockArraySequencesReader(final SequenceType sequenceType, final int[] seqLengths) {
    this(sequenceType, seqLengths, null);
  }

  /**
   * @param sequenceType sequence type.
   * @param seqLengths lengths of sequences.
   * @param names names of sequences.
   */
  public MockArraySequencesReader(final SequenceType sequenceType, final int[] seqLengths, final String[] names) {
    super(sequenceType, seqLengths.length, -1);
    mNames = names == null ? null : names.clone();
    super.setLengths(seqLengths);
    long max = 0;
    long min = Integer.MAX_VALUE;
    for (final int length : seqLengths) {
      if (length > max) {
        max = length;
      }
      if (length < min) {
        min = length;
      }
    }
    mMaxLength = max;
    mMinLength = min;
  }

  @Override
  public String name(long index) {
    if (mNames == null) {
      return "seq" + index;
    } else {
      return mNames[(int) index];
    }
  }

  @Override
  public long maxLength() {
    if (mMaxLength == -1) {
      throw new UnsupportedOperationException();
    }
    return mMaxLength;
  }
  @Override
  public long minLength() {
    if (mMinLength == -1) {
      throw new UnsupportedOperationException();
    }
    return mMinLength;
  }

  @Override
  public int read(final long index, final byte[] out) {
    for (int i = 0; i < out.length; ++i) {
      out[i] = (byte) ((i % 4) + 1);
    }
    return out.length;
  }

  @Override
  public PrereadNames names() {
    return new PrereadNames() {
        @Override
        public String name(final long id) {
          if (mNames == null) {
            return "seq" + id;
          } else {
            return mNames[(int) id];
          }
        }

      @Override
      public long length() {
        return mNames == null ? super.length() : mNames.length;
      }
    };
  }

}
