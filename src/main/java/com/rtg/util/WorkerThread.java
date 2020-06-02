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
package com.rtg.util;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;


/**
 * A daemon thread that can be given more than one <code>Runnable</code> over
 * its lifetime. Used by <code>SimpleThreadPool</code>
 */
public class WorkerThread extends Thread {

  private static final int SLEEP_TIME = 5 * 1000; //5 seconds
  private final Object mSleepSync = new Object();
  private final Object mJobSync = new Object();

  private final Object mCompleteNotify;

  private boolean mDie = false;

  private volatile Runnable mJob;

  /**
   * Constructs a worker thread
   * @param name name of thread
   * @param complete Object on which to call <code>notifyAll</code> on when a task is completed
   */
  public WorkerThread(final String name, final Object complete) {
    super(name);
    setDaemon(true);
    mCompleteNotify = complete;
  }

  /**
   * Enqueue a job for this thread
   * @param job ob to run
   */
  public void enqueueJob(final Runnable job) {
    synchronized (mSleepSync) { //prevent race condition
      if (hasJob()) {
        throw new IllegalStateException("Job already enqueued");
      }
      setJob(job);
      //interrupt();
      mSleepSync.notifyAll();
    }
  }

  /**
   * Kill the thread
   */
  public void die() {
    synchronized (mSleepSync) {
      mDie = true;
      mSleepSync.notifyAll();
    }
  }

  private void setJob(final Runnable job) {
    synchronized (mJobSync) {
      mJob = job;
    }
  }

  /**
   * Checks whether the job queue is empty
   * @return true if empty
   */
  public boolean hasJob() {
    return mJob != null;
  }

  /**
   * Runs jobs in the job queue
   */
  @Override
  public void run() {
    while (!mDie) {
      synchronized (mJobSync) {
        if (hasJob()) {
          final Runnable job = mJob;
          try {
            job.run();
          } catch (final Throwable t) {
            Diagnostic.error(ErrorType.SLIM_ERROR);
            Diagnostic.userLog(t);
          }
          synchronized (mCompleteNotify) {
            setJob(null);
            mCompleteNotify.notifyAll();
          }
        }
      }
      synchronized (mSleepSync) {
        try {
          if (!hasJob() && !mDie) {
            mSleepSync.wait(SLEEP_TIME);
          }
        } catch (final InterruptedException e) {
          //no problem
        }
      }
    }
  }
}
