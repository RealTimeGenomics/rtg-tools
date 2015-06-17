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
package com.rtg.util.array.longindex;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.rtg.util.array.AbstractIndex;
import com.rtg.util.format.FormatInteger;

/**
 * Common code used in implementing all the long index variants. Holds some handy
 * constants as well as the length of the index.
 *
 */
public abstract class LongIndex extends AbstractIndex {

  /**
   * Information used in creating "chunks" in some of the implementations. Be
   * wary of changing CHUNK_BITS.
   */
  protected static final int CHUNK_BITS = 27;

  /**
   * Number of bytes in a long.
   */
  protected static final int LONG_SIZE = 8;

  /**
   * @param length of the array.
   * @exception NegativeArraySizeException if length less than 0
   */
  protected LongIndex(final long length) {
    super(length);
  }

  /**
   * @return the number of bytes consumed.
   */
  @Override
  public long bytes() {
    return LONG_SIZE * mLength;
  }

  static final FormatInteger FORMAT_VALUE = new FormatInteger(20);

  @Override
  protected FormatInteger formatValue() {
    return FORMAT_VALUE;
  }

  /**
   * Save this index such that it can be loaded again from {@link LongCreate#loadIndex(java.io.ObjectInputStream)}
   * @param dos steam to save to
   * @throws IOException if an IO error occurs
   */
  public abstract void save(ObjectOutputStream dos) throws IOException;
}
