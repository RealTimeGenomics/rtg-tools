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

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Iterator for file/stream reading classes where IOExceptions may be thrown during the iteration
 */
public interface IOIterator<T> extends Closeable {

  /**
   * @return <code>true</code> iff the iteration has more elements.
   * @throws IOException when I/O or format errors occur.
   */
  boolean hasNext() throws IOException;

  /**
   * @return the next element in the iteration.
   * @throws IOException when I/O or format errors occur.
   */
  T next() throws IOException;

  /**
   * Performs the given action for each element of the iterator until all elements
   * have been processed or the action throws an exception. Unless otherwise specified
   * by the implementing class, actions are performed in the order of iteration
   * (if an iteration order is specified). Exceptions thrown by the action are relayed to the caller.
   * @param action action to be performed on each element
   * @throws IOException if an exception is thrown while iterating the elements
   */
  void forEach(Consumer<? super T> action) throws IOException;

  /**
   * Performs the given action for each element of the iterator until all elements
   * have been processed or the action throws an exception. Unless otherwise specified
   * by the implementing class, actions are performed in the order of iteration
   * (if an iteration order is specified). Exceptions thrown by the action are relayed to the caller.
   * @param it the iterator
   * @param action action to be performed on each element
   * @param <U> the iterator element types
   * @throws IOException if an exception is thrown while iterating the elements
   */
  static <U> void forEach(IOIterator<U> it, Consumer<? super U> action) throws IOException {
    while (it.hasNext()) {
      action.accept(it.next());
    }
  }

}
