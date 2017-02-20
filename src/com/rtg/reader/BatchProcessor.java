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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.rtg.util.ProgramState;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Process elements in an iterator in chunks.
 */
class BatchProcessor<T> {
  private final Function<Batch<T>, Runnable> mRunnableSupplier;
  private final int mBatchSize;
  private final int mThreads;

  BatchProcessor(Function<Batch<T>, Runnable> runnableSupplier, int threads, int batchSize) {
    mRunnableSupplier = runnableSupplier;
    mThreads = threads;
    mBatchSize = batchSize;
  }

  void process(Iterator<T> iterator) {
    final ExecutorService stp = new BlockingExecutor(mThreads, 3);
    ArrayList<T> batch = new ArrayList<>(mBatchSize);
    int batchNumber = 0;
    while (true) {
      ProgramState.checkAbort();
      if (!iterator.hasNext()) {
        break;
      }
      batch.add(iterator.next());

      if (batch.size() >= mBatchSize) {
        Diagnostic.developerLog("Enqueuing batch of " + batch.size());
        stp.execute(mRunnableSupplier.apply(new Batch<>(batchNumber++, batch)));
        batch = new ArrayList<>(mBatchSize);
      }
    }
    if (batch.size() > 0) {
      Diagnostic.developerLog("Enqueuing batch of " + batch.size());
      stp.execute(mRunnableSupplier.apply(new Batch<>(batchNumber, batch)));
    }
    stp.shutdown();
    try {
      stp.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      Diagnostic.userLog("InterruptedException whilst waiting for jobs to finish. " + e.getMessage());
    }
  }
}
