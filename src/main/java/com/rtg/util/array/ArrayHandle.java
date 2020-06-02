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
 * Holds an <code>ArrayType</code> and a length.
 * Can be used to create an array.
 */
public class ArrayHandle {

  private final ArrayType mType;

  private final long mLength;

  /**
   * Construct a handle.
   * @param type of underlying index.
   * @param length of index.
   */
  public ArrayHandle(final ArrayType type, final long length) {
    if (type == null || length < 0) {
      throw new IllegalArgumentException();
    }
    mType = type;
    mLength = length;
  }

  /**
   * Create  an unsigned index of the specified length and type.
   * @return an unsigned index of the specified length and type.
   */
  public ExtensibleIndex createUnsigned() {
    return mType.createUnsigned(mLength);
  }

  /**
   * Get  the length.
   * @return the length.
   */
  public long length() {
    return mLength;
  }

  /**
   * Get the type.
   * @return the type.
   */
  public ArrayType type() {
    return mType;
  }

  /**
   * Compute the number of bytes consumed by the created arrays.
   * @return the number of bytes consumed by the created arrays.
   */
  public long bytes() {
    return mType.bytes(mLength);
  }

  @Override
  public String toString() {
    return "";
  }

}

