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


/**
 * Given an <code>Iterator&lt;X&gt;</code> and a transformation method <code>subIterator</code> that constructs an
 * <code>Iterator&lt;Y&gt;</code> from an instance of <code>X</code> subclasses will act as an iterator of all
 * <code>Y</code> that can be produced
 * @param <X> type of source objects
 * @param <Y> resulting type objects
 */
public final class ComposeIterators<X, Y> implements Iterator<Y> {

  private final Iterator<X> mSourceIterator;
  private final Transform<X, Iterator<Y>> mTransform;
  private Iterator<Y> mCurrent;

  ComposeIterators(Iterator<X> sourceIterator, Transform<X, Iterator<Y>> transform) {
    mSourceIterator = sourceIterator;
    mTransform = transform;
  }

  private void stepWhile() {
    while (mCurrent != null && !mCurrent.hasNext()) {
      step();
    }
  }

  private void step() {
    if (mSourceIterator.hasNext()) {
      mCurrent = mTransform.trans(mSourceIterator.next());
    } else {
      mCurrent = null;
    }
  }

  @Override
  public boolean hasNext() {
    if (mCurrent == null) {
      step();
      stepWhile();
    }
    if (mCurrent == null) {
      return false;
    } else {
      return mCurrent.hasNext();
    }
  }

  @Override
  public Y next() {
    final Y res = mCurrent.next();
    stepWhile();
    return res;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
