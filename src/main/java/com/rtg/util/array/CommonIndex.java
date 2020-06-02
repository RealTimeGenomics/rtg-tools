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
package com.rtg.util.array;


/**
 */
public interface CommonIndex {

  /**
   * Swap the values at the two specified locations.
   *
   * @param index1 the first index to be swapped
   * @param index2 the second index to be swapped
   */
  void swap(long index1, long index2);

  /**
   * Note: the length returned by this function should be precisely controlled
   * by the user, as such it should not reflect the amount of memory allocated
   * internally. Instead it should reflect the number of elements requested.
   * @return the length of the array
   */
  long length();

  /**
   * The length of this index in bytes.
   *
   * @return the number of bytes
   */
  long bytes();

  /**
   * Gets the value at the specified position.
   * The exact bit pattern as stored in <code>set</code> is returned.
   * In particular stored values which have their high order (sign) bit
   * set in the underlying implementations will not be returned
   * with this sign bit extended to the full long.
   *
   * @param offset the position in this index
   * @return the value as a long, regardless of the underlying type
   */
  long get(long offset);

  /**
   * Sets the value at the specified position.
   * Checks that the value is compatible with the underlying storage type.
   * In particular if the underlying storage type is n bits then the value
   * must have bit positions (n+1)..64 (inclusive, counting from 1) all 0.
   * This ensures that information is not lost.
   *
   * @param offset the position in this index
   * @param value the value as a long, regardless of the underlying type
   */
  void set(long offset, long value);

  /**
   * Interrogate whether this index implementation is safe from word tearing for get and set operations.
   * Specifically is it safe for different threads to update adjacent elements without risking concurrent update errors.
   * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.6">JLS on word tearing</a>
   * @return true if this implementation is not subject to word tearing.
   */
  boolean safeFromWordTearing();
}
