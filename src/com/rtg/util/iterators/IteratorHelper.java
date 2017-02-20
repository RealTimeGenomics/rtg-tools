/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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
import java.util.NoSuchElementException;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * A class to help build iterators where it is necessary to do a look ahead to check if the next
 * case is available.
 * @param <X> type the iterator operates over.
 */
@TestClass({"com.rtg.util.iterators.ArrayToIteratorTest", "com.rtg.util.iterators.IteratorHelperTest"})
public abstract class IteratorHelper<X> implements Iterator<X> {

  protected abstract void step();

  protected abstract boolean atEnd();

  protected boolean isOK() {
    return true;
  }

  protected abstract X current();

  @Override
  public final boolean hasNext() {
    check();
    return !atEnd() && isOK();
  }

  private void check() {
    while (!atEnd() && !isOK()) {
      step();
    }
  }

  private void stepAll() {
    step();
    check();
  }

  @Override
  public final X next() {
    check();
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final X res = current();
    stepAll();
    return res;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
