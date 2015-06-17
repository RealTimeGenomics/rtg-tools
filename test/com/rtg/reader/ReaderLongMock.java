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

/**
 * Mocks everything except the length of the sequence.
 */
public class ReaderLongMock extends DummySequencesReader {
  private final long mLength;
  /**
   * @param length length.
   */
  public ReaderLongMock(final long length) {
    mLength = length;
  }
  @Override
  public long maxLength() {
    return mLength;
  }
  @Override
  public long minLength() {
    return 0;
  }
  @Override
  public long numberSequences() {
    return 1;
  }

  private static class NullNames extends PrereadNames {
    @Override
    public String name(final long id) {
      return null;
    }
  }

  @Override
  public PrereadNames names() {
    return new NullNames();
  }

  @Override
  public long lengthBetween(final long start, final long end) {
    return mLength;
  }

  @Override
  public int[] sequenceLengths(final long start, final long end) {
    return new int[] {(int) mLength};
  }
  @Override
  public boolean hasNames() {
    return true;
  }

}
