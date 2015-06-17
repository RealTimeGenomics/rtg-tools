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

/**
 */
public abstract class ByteArray {
  /**
   * Get a single byte.
   * If multiple bytes are required, the array version of get should be used
   * rather than this method, since it is faster.
   *
   * @param offset which byte to get.
   * @return a byte
   */
  public abstract byte get(long offset);

  /**
   * Reads <code>count</code> bytes, starting at <code>offset</code>.
   * @param dest the array to copy into, starting from position 0.
   * @param offset the position to start reading from.
   * @param count how many bytes to cover.
   */
  public abstract void get(final byte[] dest, final long offset, final int count);

  /**
   * Set a single byte.
   * If multiple bytes must be set, the array version of set should be used
   * rather than this method, since it is faster.
   *
   * @param offset which byte to set.
   * @param value the new value to put into the array.
   */
  public abstract void set(long offset, byte value);

  /**
   * Writes <code>buffer[0 .. count-1]</code> into the byte array, starting at <code>offset</code>.
   * @param offset the position to copy to.
   * @param buffer the bytes to copy.
   * @param count how many bytes to copy.
   */
  public abstract void set(final long offset, final byte[] buffer, final int count);

  /**
   * Writes <code>buffer[0 .. count-1]</code> into the byte array, starting at <code>offset</code>.
   * @param offset the position to copy to.
   * @param buffer the bytes to copy.
   * @param bOffset offset into buffer to start copying from
   * @param count how many bytes to copy.
   */
  public abstract void set(final long offset, final byte[] buffer, final int bOffset, final int count);

  /** @return the total number of values that can be stored in this array. */
  public abstract long length();

  /** @return the total amount of memory used by this array. */
  public long bytes() {
    return length();  // by default
  }

  /**
   * A factory method that allocates a suitable subclass of ByteArray.
   *
   * @param size the required length
   * @return a subclass of ByteArray
   */
  public static ByteArray allocate(final long size) {
    if (size <= Integer.MAX_VALUE) {
      return new SingleByteArray((int) size);
    } else {
      return new MultiByteArray(size);
    }
  }
}
