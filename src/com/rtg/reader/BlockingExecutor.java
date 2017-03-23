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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.rtg.util.ProgramState;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Provides multi-threaded processing with limited read-ahead
 */
class BlockingExecutor extends ThreadPoolExecutor {
  private final Semaphore mSemaphore;
  private final AtomicReference<Throwable> mThrown = new AtomicReference<>(null);

  BlockingExecutor(final int poolSize, final int queueSize) {
    super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    mSemaphore = new Semaphore(poolSize + queueSize);
  }

  @Override
  public void execute(final Runnable task) {
    checkAndRethrow();
    boolean acquired = false;
    do {
      try {
        mSemaphore.acquire();
        acquired = true;
      } catch (final InterruptedException e) {
        Diagnostic.userLog("InterruptedException whilst aquiring semaphore" + e.getMessage());
      }
    } while (!acquired);

    try {
      super.execute(task);
    } catch (final RejectedExecutionException e) {
      mSemaphore.release();
      throw e;
    }
    checkAndRethrow();
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    ProgramState.checkAbort();
    super.beforeExecute(t, r);
  }

  @Override
  protected void afterExecute(final Runnable r, Throwable t) {
    super.afterExecute(r, t);

    Throwable t2 = t;
    if (t == null && r instanceof Future<?>) {
      try {
        ((Future<?>) r).get();
      } catch (CancellationException ce) {
        t2 = ce;
      } catch (ExecutionException ee) {
        t2 = ee.getCause();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
    if (t2 != null) {
      mThrown.compareAndSet(null, t2);
    }
    mSemaphore.release();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    final boolean res = super.awaitTermination(timeout, unit);
    checkAndRethrow();
    return res;
  }

  // Called from the submission thread, passes out any exception from inner jobs
  private void checkAndRethrow() {
    final Throwable t = this.mThrown.getAndSet(null);
    if (t != null) {
      ProgramState.setAbort();
      if (t instanceof Error) {
        throw (Error) t;
      }
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }
}
