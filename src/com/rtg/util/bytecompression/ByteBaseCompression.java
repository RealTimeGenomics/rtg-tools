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

package com.rtg.util.bytecompression;

import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.array.byteindex.ByteChunks;
import com.rtg.util.array.longindex.LongChunks;

/**
 * Basic bit packing compression.
 */
public class ByteBaseCompression implements ByteCompression {

  private final ByteArray mBytes;
  private final ByteChunks mByteChunks;
  private final ExtensibleIndex mPointers;
  private long mTotalSize;
  private boolean mFrozen;

  /**
   * Basic bit packing for ranges that use less than or equal to 8 bits (no compression on 8 bits).
   * @param range the range of values that can be held.
   */
  //TODO eventually should not use this publicly
  public ByteBaseCompression(int range) {
    assert range <= 256 && range > 0;
    final int minBits = CompressedByteArray.minBits(range);
    if (minBits == 8) {
      mByteChunks = new ByteChunks(0);
      mBytes = null;
    } else {
      mBytes = new CompressedByteArray(0, range, true);
      mByteChunks = null;
    }
    mPointers = new LongChunks(1);
    mPointers.set(0, 0);
    mFrozen = false;
  }

  /**
   * Constructor to hold pre-existing data.
   * This is used by SDF reading.
   * Pointers are 0 based with pointer(i+1) being the exclusive end.
   * @param data the byte array data.
   * @param pointers the pointers into the byte array.
   */
  public ByteBaseCompression(ByteArray data, ExtensibleIndex pointers) {
    mByteChunks = null;
    mBytes = data;
    mPointers = pointers;
    mFrozen = true;
  }

  @Override
  public void add(byte[] buffer, int offset, int length) {
    if (mFrozen) {
      throw new RuntimeException("Adding to a frozen ByteCompression");
    }
    if (mBytes != null) {
      mBytes.set(mTotalSize, buffer, offset, length);
    } else {
      mByteChunks.extendBy(length);
      mByteChunks.copyBytes(buffer, offset, mTotalSize, length);
    }
    mTotalSize += length;
    mPointers.append(mTotalSize);
  }

  /**
   * @param index of a block (0 based).
   * @return the length of the block.
   */
  public int length(long index) {
    return (int) (mPointers.get(index + 1) - mPointers.get(index));
  }


  @Override
  public void get(byte[] buffer, long index, int offset, int length) {
    if (mBytes != null) {
      mBytes.get(buffer, mPointers.get(index) + offset, length);
    } else {
      mByteChunks.getBytes(buffer, 0, mPointers.get(index) + offset, length);
    }
  }

  @Override
  public void freeze() {
    mPointers.trim();
    if (mByteChunks != null) {
      mByteChunks.trim();
    }
    mFrozen = true;
  }

  @Override
  public long bytes() {
    return mPointers.bytes() + (mByteChunks != null ? mByteChunks.bytes() : mBytes.bytes());
  }
}
