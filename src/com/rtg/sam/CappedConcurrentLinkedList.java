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
package com.rtg.sam;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.rtg.util.ProgramState;

/**
 * A queue object which has an upper limit on how many
 * objects can be contained in it at a time and all of
 * its methods are synchronised such that it can be used
 * to have one thread adding objects to the list and
 * another thread getting the objects from the list.
 * @param <E> the type for the LinkedList
 */
public final class CappedConcurrentLinkedList<E> implements Queue<E> {

  private static final int TIMEOUT = 100000;

  private final int mMaxSize;
  private final Queue<E> mInternalQueue;

  private boolean mClosed = false;
  private boolean mHasNext = true;
  private final int mId;

  /**
   * Constructor for the capped concurrent linked list
   * @param maxSize the maximum buffer size
   * @param id id number
   */
  public CappedConcurrentLinkedList(int maxSize, int id) {
    super();
    assert maxSize > 0;
    mMaxSize = maxSize;
    mId = id;
    mInternalQueue = new LinkedList<>();
  }

    /**
   * When there are no more elements to read the buffer can be closed
   */
  public synchronized void close() {
    mClosed = true;
    notifyAll();
  }

  @Override
  public synchronized boolean add(E e) {
    if (e != null) {
      return add(e, mHasNext);
    } else {
      return true;
    }
  }

  /**
   * Add method which also set whether there is a next value
   * @param e the object to add
   * @param hasNext whether there is a next record to follow
   * @return true as specified by Collections.add
   */
  public synchronized boolean add(E e, boolean hasNext) {
    while (mInternalQueue.size() >= mMaxSize && !mClosed) {
      notifyAll();
      try {
        wait(TIMEOUT);
      } catch (InterruptedException e1) {
        mClosed = true;
        ProgramState.checkAbort();
        throw new IllegalStateException("Interrupted but program not aborting?", e1);
      }
      ProgramState.checkAbort();
    }
    if (mClosed) {
      return true;
    }
    final boolean wasEmpty = mInternalQueue.isEmpty();
    mHasNext = hasNext;
    mInternalQueue.add(e);
    if (wasEmpty) {
      notifyAll();
    }
    return true;
  }

  /**
   * Set if there are records to follow.
   * @param hasNext whether there is a next record to follow
   */
  public synchronized void setHasNext(boolean hasNext) {
    mHasNext = hasNext;
    notifyAll();
  }

  //TODO : check what happens to interrupted exception before wait
  @Override
  public synchronized E peek() {
    while (mInternalQueue.isEmpty() && mHasNext && !mClosed) {
      try {
        wait(TIMEOUT);
      } catch (InterruptedException e) {
        mClosed = true;
        ProgramState.checkAbort();
        throw new IllegalStateException("Interrupted but program not aborting?", e);
      }
      ProgramState.checkAbort();
    }
    if (mInternalQueue.isEmpty() && (mClosed || !mHasNext)) {
      return null;
    }
    return mInternalQueue.peek();
  }

  @Override
  public synchronized E poll() {
    while (mInternalQueue.isEmpty() && mHasNext && !mClosed) {
      try {
        wait(TIMEOUT);
      } catch (InterruptedException e) {
        mClosed = true;
        ProgramState.checkAbort();
        throw new IllegalStateException("Interrupted but program not aborting?", e);
      }
      ProgramState.checkAbort();
    }
    if (mInternalQueue.isEmpty() && (mClosed || !mHasNext)) {
      return null;
    } else {
      final boolean wasFull = mInternalQueue.size() >= mMaxSize;
      final E rec = mInternalQueue.poll();
      if (wasFull) {
        notifyAll();
      }
      return rec;
    }
  }

  @Override
  public synchronized int size() {
    return mInternalQueue.size();
  }

  @Override
  public synchronized boolean isEmpty() {
    return mInternalQueue.isEmpty() && !mHasNext;
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized E element() {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized boolean offer(E e) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized E remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   */
  @Override
  public synchronized boolean contains(Object o) {
    return mInternalQueue.contains(o);
  }

  /**
   */
  @Override
  public synchronized boolean containsAll(Collection<?> c) {
    return mInternalQueue.containsAll(c);
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized Iterator<E> iterator() {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported
   */
  @Override
  public synchronized boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   */
  @Override
  public synchronized Object[] toArray() {
    return mInternalQueue.toArray();
  }

  /**
   */
  @Override
  public synchronized <T> T[] toArray(T[] a) {
    return mInternalQueue.toArray(a);
  }

  @Override
  public synchronized String toString() {
    return "CappedConcurrentLinkedList id: " + mId + " size: " + mInternalQueue.size() + " hasNext: " + mHasNext + " closed: " + mClosed;
  }
}
