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

package com.rtg.util.array.atomic;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * AtomicIndex backed by {@link AtomicLongArray}'s
 */
public class AtomicLongChunks implements AtomicIndex {

  /**
   * Information used in creating "chunks" in some of the implementations. Be
   * wary of changing CHUNK_BITS.
   */
  protected static final int CHUNK_BITS = 30;

  private final int mChunkBits;

  private final int mChunkMask;

  private final AtomicLongArray[] mArray;

  private final long mLength;

  /**
   * Constructs an index by splitting into array chunks.
   *
   * @param length of the index being created.
   */
  public AtomicLongChunks(final long length) {
    this(length, CHUNK_BITS);
  }

  /**
   * Constructs an index by splitting into array chunks.
   * This version sets the size of the chunks - it should only be used for testing.
   * @param length of the index being created.
   * @param chunkBits number of bits used for an entry in a chunk.
   */
  AtomicLongChunks(final long length, final int chunkBits) {
    mLength = length;
    assert chunkBits >= 0 && chunkBits <= 31;
    mChunkBits = chunkBits;
    final int chunkSize = 1 << mChunkBits;
    mChunkMask = chunkSize - 1;

    final long ch = (length + chunkSize - 1) / chunkSize;
    if (ch > Integer.MAX_VALUE) {
      throw new RuntimeException("length requested too long length=" + length + " mChunkSize=" + chunkSize);
    }
    final int chunks = (int) ch;
    mArray = new AtomicLongArray[chunks];
    long left = mLength;
    for (int i = 0; i < chunks; ++i) {
      final int assignedLength = left <= chunkSize ? (int) left : chunkSize;
      assert assignedLength != 0;
      mArray[i] =  new AtomicLongArray(assignedLength);
      left -= assignedLength;
    }
    assert left == 0;
  }

  @Override
  public long get(final long index) {
    final int chunk = (int) (index >> mChunkBits);
    final int offset = (int) (index & mChunkMask);
    return mArray[chunk].get(offset);
  }

  @Override
  public void set(final long index, final long value) {
    final int chunk = (int) (index >> mChunkBits);
    final int offset = (int) (index & mChunkMask);
    mArray[chunk].set(offset, value);
  }

  @Override
  public boolean compareAndSet(final long index, final long expected, final long value) {
    final int chunk = (int) (index >> mChunkBits);
    final int offset = (int) (index & mChunkMask);
    return mArray[chunk].compareAndSet(offset, expected, value);
  }

  @Override
  public long length() {
    return mLength;
  }

  @Override
  public int fieldBits() {
    return 64;
  }

  @Override
  public String toString() {
    return String.format("AtomicLongChunks[%d]", mLength);
  }
}
