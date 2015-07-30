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

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 */
public class WorkerThreadTest extends TestCase {

  public void testThread() {
    final AtomicInteger val = new AtomicInteger();
    val.set(0);
    final Runnable run = new Runnable() {
      @Override
      public void run() {
        val.incrementAndGet();
      }
    };
    final Object signal = new Object();
    final WorkerThread wt = new WorkerThread("testWorkerThread", signal);
    assertTrue(wt.isDaemon()); // sigh jumble
    wt.start();
    for (int i = 0; i < 10000; i++) {
      synchronized (signal) {
        wt.enqueueJob(run);
        while (wt.hasJob()) {
          try {
            signal.wait(1000);
          } catch (final InterruptedException e) {
            //
          }
        }
      }
    }
    wt.die();
    assertEquals(10000, val.get());
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {

    }
    assertTrue(!wt.isAlive());
  }
}
