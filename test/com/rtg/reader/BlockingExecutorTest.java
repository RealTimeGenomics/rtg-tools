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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class BlockingExecutorTest {

  static class LockedRunnable implements Runnable {
    CountDownLatch latch = new CountDownLatch(1);
    public void run() {
      try {
        latch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      ;
    }
  }

  static class JobSubmission implements Runnable {
    private final BlockingExecutor mExecutor;
    private final LockedRunnable mTask;
    CountDownLatch mLatch;
    JobSubmission(CountDownLatch latch, BlockingExecutor executor) {
      mLatch = latch;
      mExecutor = executor;
      mTask = new LockedRunnable();
    }
    public void run() {
      mExecutor.submit(mTask);
      mLatch.countDown();
    }
  }

  @Test
  public void testBlockingExecutor() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(4);
    final BlockingExecutor blockingExecutor = new BlockingExecutor(2, 2);
    blockingExecutor.getTaskCount();
    final List<JobSubmission> jobs = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final JobSubmission job = new JobSubmission(latch, blockingExecutor);
      new Thread(job).start();
      jobs.add(job);
    }
    latch.await();
    assertEquals(4, blockingExecutor.getTaskCount());
    for (JobSubmission job : jobs) {
      job.mTask.latch.countDown();
    }
  }

}