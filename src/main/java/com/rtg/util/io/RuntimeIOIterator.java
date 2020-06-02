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
package com.rtg.util.io;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

import com.rtg.util.iterators.IteratorForEach;

import htsjdk.samtools.util.RuntimeIOException;

/**
 * Adaptor between IOIterator and regular Iterator.
 */
public class RuntimeIOIterator<T> implements Iterator<T>, IteratorForEach<T> {

  IOIterator<T> mIt;

  /**
   * Constructor
   * @param it the iterator being wrapped
   */
  public RuntimeIOIterator(IOIterator<T> it) {
    mIt = it;
  }

  @Override
  public boolean hasNext() {
    try {
      return mIt.hasNext();
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  @Override
  public T next() {
    try {
      return mIt.next();
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    IteratorForEach.forEach(this, action);
  }
}
