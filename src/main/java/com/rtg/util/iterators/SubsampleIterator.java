/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.util.iterators;

import java.util.Iterator;

import com.rtg.util.PortableRandom;

/**
 * Subsamples the input source
 */
public class SubsampleIterator<T> extends IteratorHelper<T> {
  private final double mFraction;
  private final PortableRandom mRandom;
  private final Iterator<T> mInternal;
  private T mNext;
  private boolean mOk = true;

  /**
   * Constructor
   * @param inner the iterator to wrap around
   * @param random supplies random numbers
   * @param fraction the fraction of input elements to retain, between 0 and 1.0
   */
  public SubsampleIterator(Iterator<T> inner, PortableRandom random, double fraction) {
    mInternal = inner;
    mRandom = random;
    mFraction = fraction;
  }

  @Override
  protected void step() {
    mNext = mInternal.hasNext() ? mInternal.next() : null;
    mOk = mRandom.nextDouble() < mFraction;
  }

  @Override
  protected boolean atEnd() {
    return mNext == null && !mInternal.hasNext();
  }

  @Override
  protected boolean isOK() {
    return mNext != null && mOk;
  }

  @Override
  protected T current() {
      return mNext;
  }
}
