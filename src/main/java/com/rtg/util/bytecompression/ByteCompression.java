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
 * Interface for classes holding a compressed set of
 * byte arrays.
 */
public interface ByteCompression {

  /**
   * Method to add a byte array to the set.
   * This is <b>not</b> safe for access from multiple threads.
   * @param buffer contains the bytes to be compressed and added.
   * @param offset the offset into the buffer of the start.
   * @param length the number of bytes from the buffer to use.
   */
  void add(byte[] buffer, int offset, int length);

  /**
   * Method to get the byte array at the given index.
   * This is safe for access from multiple threads.
   * @param buffer a byte array of sufficient size to contain uncompressed data.
   * @param index the index of the byte array to fetch.
   * @param offset the offset into the uncompressed data to start from.
   * @param length the length of the uncompressed data to fetch.
   */
  void get(byte[] buffer, long index, int offset, int length);

  /**
   * Trim internal arrays to minimise total memory.
   * Prevents further adds.
   */
  void freeze();

  /**
   * Get the approximate number of bytes used.
   * @return the approximate number of bytes used.
   */
  long bytes();
}
