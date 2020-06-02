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
 * Common interface between the array classes in the <code>longindex</code>,
 * <code>intindex</code> and <code>shortindex</code> packages.
 *
 */
public interface ExtensibleIndex extends CommonIndex {

  /**
   * Extend length by one and set newly created location to
   * value.
   * @param value the value as a long, regardless of the underlying type
   */
  void append(long value);

  /**
   * Extend length by one and set newly created location to
   * value.
   * Sign bits are preserved across <code>appendSigned</code> and <code>getSigned</code> calls.
   * @param value the value as a long, regardless of the underlying type
   */
  void appendSigned(long value);

  /**
   * Allocate an additional increment entries.
   * @param increment minimum number of new entries to be allocated.
   * @return initial position of the start of the newly allocated entries.
   */
  long extendBy(long increment);

  /**
   * Extend the array to support at least the specified size.
   * @param size new size
   */
  void extendTo(long size);

  /**
   * Reduce the length of the index to length and reduce the underlying memory used as far as possible.
   * The new length must be &le; the current length.
   * @param length new length of index.
   */
  void trim(long length);

  /**
   * Reduce the underlying memory used as far as possible to accomodate the current length.
   */
  void trim();

  /**
   * Gets the value at the specified position.
   * Sign bits are preserved across <code>setSigned</code> and <code>getSigned</code> calls.
   * This is not true of set and get calls where the low order bit pattern is preserved.
   * @param offset the position in this index
   * @return the value as a long, regardless of the underlying type
   */
  long getSigned(long offset);

  /**
   * Sets the value at the specified position.
   * Checks that the value is compatible with the underlying storage type.
   * In particular if the underlying storage type is n bits then a positive value
   * must have bit positions (n+1)..64 (inclusive, counting from 1) all 0.
   * If it is negative then it must have positions (n+1)..64 (inclusive, counting from 1) all 1.
   * This ensures that information is not lost.
   * Sign bits are preserved across <code>setSigned</code> and <code>getSigned</code> calls.
   * This is not true of set and get calls where the low order bit pattern is preserved.
   *
   * @param offset the position in this index
   * @param value the value as a long, regardless of the underlying type
   */
  void setSigned(long offset, long value);

}
