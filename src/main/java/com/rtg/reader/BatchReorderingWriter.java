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

package com.rtg.reader;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Takes an out of order set of batches and sends the items to the consumer in order in a thread safe fashion
 */
public class BatchReorderingWriter<T> {
  private final Consumer<List<T>> mWriter;
  private final PriorityQueue<Batch<T>> mPriorityQueue = new PriorityQueue<>(Comparator.comparingInt(Batch::getBatchNumber));
  private int mNextBatch = 0;
  BatchReorderingWriter(Consumer<List<T>> writer) {
    mWriter = writer;
  }

  /**
   * Either write out the batch or store it until all prior batches are ready
   * @param batch the batch to write
   */
  synchronized void writeBatch(Batch<T> batch) {
    if (batch.getBatchNumber() != mNextBatch) {
      // We aren't ready for this batch yet. save it for later
      mPriorityQueue.add(batch);
    } else {
      mWriter.accept(batch.getItems());
      mNextBatch++;
      // Write all the sequential batches that are already waiting
      while (!mPriorityQueue.isEmpty() && mPriorityQueue.peek().getBatchNumber() == mNextBatch) {
        final Batch<T> next = mPriorityQueue.poll();
        mWriter.accept(next.getItems());
        mNextBatch++;
      }
    }
  }
}
