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
package com.rtg.util.array.objectindex;

/**
 * Index is implemented using a straight forward long array.
 * This is so that short instances of IntIndex can be as efficient as possible.
 *
 */
class ObjectArray<A> extends ObjectIndex<A> {

  private Object[] mArray;

  ObjectArray(final long length) {
    super(length);
    assert length <= MAX_LENGTH;
    mArray = new Object[(int) length];
  }

  @Override
  @SuppressWarnings("unchecked")
  public A get(final long index) {
    final int ii = (int) index;
    if (ii != index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    return (A) mArray[ii];
  }

  @Override
  public void set(final long index, final A value) {
    final int ii = (int) index;
    if (ii != index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    mArray[ii] = value;
  }

  @Override
  public void close() {
    mArray = null;
  }

  @Override
  public boolean integrity() {
    super.integrity();
    assert mArray == null || mArray.length == mLength;
    return true;
  }
}


