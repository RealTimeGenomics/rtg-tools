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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class BlockingExecutorTest {


  static class LockedRunnable implements Runnable {
    /** This latch prevents the jobs from finishing until the main thread gives it's OK */
    final CountDownLatch mLatch = new CountDownLatch(1);
    @Override
    public void run() {
      try {
        // Wait till main thread lets us through
        mLatch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  static class JobSubmission implements Runnable {
    private final BlockingExecutor mExecutor;
    private final LockedRunnable mTask;
    private final BlockingQueue<Future<?>> mQueue;

    JobSubmission(BlockingExecutor executor, BlockingQueue<Future<?>> queue) {
      mExecutor = executor;
      mTask = new LockedRunnable();
      mQueue = queue;
    }
    @Override
    public void run() {
      try {
        // Communicate the job status via a concurrent queue
        mQueue.put(mExecutor.submit(mTask));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    public void unlatch() {
      mTask.mLatch.countDown();
    }
  }

  @Test
  public void testBlockingExecutor() throws InterruptedException {
    final int numberOfJobs = 10;
    final int executorSize = 4;
    final BlockingExecutor blockingExecutor = new BlockingExecutor(2, 2);
    final BlockingQueue<Future<?>> taskQueue = new LinkedBlockingDeque<>();
    try {
      blockingExecutor.getTaskCount();
      final List<JobSubmission> submissions = new ArrayList<>();
      for (int i = 0; i < numberOfJobs; i++) {
        submissions.add(startJob(blockingExecutor, taskQueue));
      }
      final List<Future<?>> firstFutures = getFutures(executorSize, taskQueue);
      // No further jobs should have been queued as submission threads should be blocked by the executor
      assertEquals(0, taskQueue.size());
      assertTrue(blockingExecutor.getTaskCount() <= executorSize);
      // Let all the jobs run.
      for (JobSubmission submission : submissions) {
        submission.unlatch();
      }
      // All jobs should eventually finish
      finish(getFutures(numberOfJobs - executorSize, taskQueue));
      finish(firstFutures);
    } finally {
      blockingExecutor.shutdown();
    }

  }

  private JobSubmission startJob(BlockingExecutor blockingExecutor, BlockingQueue<Future<?>> taskQueue) {
    final JobSubmission job = new JobSubmission(blockingExecutor, taskQueue);
    final Thread thread = new Thread(job);
    thread.start();
    return job;
  }

  private void finish(List<Future<?>> futures) throws InterruptedException {
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        fail();
      }
    }
  }

  private List<Future<?>> getFutures(int count, BlockingQueue<Future<?>> taskQueue) throws InterruptedException {
    final List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      // This timeout should prevent a broken implementation from freezing unit tests forever.
      futures.add(taskQueue.poll(500, TimeUnit.MILLISECONDS));
    }
    return futures;
  }
}
