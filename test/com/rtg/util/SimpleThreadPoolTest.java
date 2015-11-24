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


import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogRecord;
import com.rtg.util.io.LogStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class SimpleThreadPoolTest extends TestCase {

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    Diagnostic.setLogStream();
  }

  private class RunIncrement implements IORunnable {

    RunIncrement(final AtomicInteger atom) {
      mAtom = atom;
    }
    private final AtomicInteger mAtom;

    @Override
    public void run() {
      mAtom.incrementAndGet();
    }
  }

  static volatile boolean sDead = false;
  static void setDead() {
    sDead = true;
  }
  static volatile int sTriedRunningAfterDead = 0;
  static void setTriedRunningAfterDead() {
    sTriedRunningAfterDead++;
  }

  private static final RuntimeException EXCEPTION = new RuntimeException("my-aborting-gob");

  private class RunAddJobs implements IORunnable {
    final boolean mAlreadyDead;
    final int mMoreJobs;
    final int mErrorAfter;
    final SimpleThreadPool mSched;
    RunAddJobs(SimpleThreadPool sched, final int moreJobs) {
      this(sched, moreJobs, -1, false);
    }

    RunAddJobs(SimpleThreadPool sched, final int moreJobs, final int errorAfter, boolean alreadyDead) {
      mSched = sched;
      mMoreJobs = moreJobs;
      mErrorAfter = errorAfter;
      mAlreadyDead = alreadyDead;
    }

    @Override
    public void run() {
      if (mAlreadyDead) {
        setTriedRunningAfterDead();
      }
      if (mMoreJobs > 0) {
        mSched.execute(new RunAddJobs(mSched, mMoreJobs - 1, mErrorAfter - 1, sDead));
        if (mMoreJobs % 5 == 0) {
          mSched.execute(new RunAddJobs(mSched, mMoreJobs - 1, -1, sDead));
        }
        if (mErrorAfter == 0) {
          mSched.execute(new IORunnable() {
            @Override
            public void run() {
              // Note: This set-dead/throw-exception isn't atomic so it's possible that some
              // "alreadydead" jobs are enqueued/executed before the exception is trapped/recognized
              setDead();
              throw EXCEPTION;
            }
          });
        }
      }
    }
  }

  private class RunError implements IORunnable {
    @Override
    public void run() {
      throw new RuntimeException("testing");
    }
  }

  private class RunSlimError implements IORunnable {
    @Override
    public void run() {
      throw new SlimException(new RuntimeException("testing SlimException"));
    }
  }

  public void test() throws IOException {
    final SimpleThreadPool pool = new SimpleThreadPool(3, "Test", true);
    final AtomicInteger atom = new AtomicInteger();

    atom.set(0);
    for (int i = 0; i < 5; i++) {
      final IORunnable run = new RunIncrement(atom);
      pool.execute(run);
    }
    pool.terminate();
    assertEquals(5, atom.get());
  }


  public void testAddingDuringAbort() throws IOException {
    // Tests to see whether we prevent adding more jobs when trying to shut down
    final int numThreads = 3;
    final SimpleThreadPool pool = new SimpleThreadPool(numThreads, "TestAbort", true);

    pool.execute(new RunAddJobs(pool, 30));
    pool.execute(new RunAddJobs(pool, 30));
    pool.execute(new RunAddJobs(pool, 30));
    pool.execute(new RunAddJobs(pool, 30));
    pool.execute(new RunAddJobs(pool, 30, 10, false));
    try {
      pool.terminate();
    } catch (RuntimeException e) {
      assertEquals("my-aborting-job", e.getMessage());
    }
    assertTrue(sTriedRunningAfterDead <= numThreads); // Rarely one will attempt to run, but should not get more then numThreads
  }


  public void testError() throws IOException {
    final LogStream log = new LogRecord();
    Diagnostic.setLogStream(log);
    try {
      final SimpleThreadPool pool = new SimpleThreadPool(3, "search", true);
      assertTrue(log.toString().contains("search: Starting SimpleThreadPool with maximum 3 threads"));
      final AtomicInteger atom = new AtomicInteger();

      atom.set(0);
      for (int i = 0; i < 5; i++) {
        final IORunnable run = new RunIncrement(atom);
        pool.execute(run);
      }
      pool.execute(new RunError());
      try {
        pool.terminate();
        fail();
      } catch (final RuntimeException e) {
        assertTrue(e.getMessage().equals("testing"));
      }
      assertEquals(5, atom.get());
    } finally {
      Diagnostic.setLogStream();
    }
  }

  public void testSlimException() throws IOException {
    final LogStream log = new LogRecord();
    Diagnostic.setLogStream(log);
    try {
      final SimpleThreadPool pool = new SimpleThreadPool(3, "Test", true);
      final AtomicInteger atom = new AtomicInteger();

      atom.set(0);
      for (int i = 0; i < 5; i++) {
        final IORunnable run = new RunIncrement(atom);
        pool.execute(run);
      }
      pool.execute(new RunSlimError());
      try {
        pool.terminate();
        fail();
      } catch (final SlimException e) {
        //System.err.println(e.getMessage());
        assertTrue(e.getMessage().startsWith(java.lang.RuntimeException.class.getName() + ": testing SlimException"));
        e.logException();
      }
      final String logStr = log.toString();
      //System.err.println("log:" + logStr);
      assertTrue(logStr.contains(java.lang.RuntimeException.class.getName() + ": testing SlimException"));
      assertTrue(logStr.contains("RTG has encountered a difficulty, please contact support@"));
      assertEquals(5, atom.get());
    } finally {
      Diagnostic.setLogStream();
    }
  }


  public void testThread() throws IOException {
    final AtomicInteger val = new AtomicInteger();
    val.set(0);
    final IORunnable run = new IORunnable() {
      @Override
      public void run() {
        val.incrementAndGet();
      }
    };
    final SimpleThreadPool stp = new SimpleThreadPool(25, "Test", true);
    for (int i = 0; i < 100; i++) {
      stp.execute(run);
    }
    stp.terminate();
    assertEquals(100, val.get());
  }

  public void testTerminate() throws IOException {
    for (int i = 0; i < 1000; i++) {
      final SimpleThreadPool blah = new SimpleThreadPool(1, "Test", true);
      blah.execute(new RunSlimError());
      try {
        blah.terminate();
        fail();
      } catch (final SlimException e) {
        //good
      }
    }
  }

  public void testProgress() throws IOException {
    final File testDir = FileHelper.createTempDirectory();
    try {
      Diagnostic.switchLog(new File(testDir, "log"));
      final AtomicInteger val = new AtomicInteger();
      val.set(0);
      final IORunnable run = new IORunnable() {
        @Override
        public void run() {
          val.incrementAndGet();
        }
      };
      final SimpleThreadPool stp = new SimpleThreadPool(25, "Test", true);
      stp.enableBasicProgress(100);
      for (int i = 0; i < 100; i++) {
        stp.execute(run);
      }
      stp.terminate();
      assertEquals(100, val.get());
      final File progressFile = new File(testDir, FileUtils.PROGRESS_SUFFIX);
      final String mps = FileHelper.fileToString(progressFile);
      assertTrue(mps, mps.contains("Test: Starting 100 Jobs"));
      for (int i = 1; i <= 100; i++) {
        assertTrue(mps, mps.contains("Test: " + i + "/100 Jobs Finished"));
      }
      final SimpleThreadPool stp1 = new SimpleThreadPool(25, "Test2", true);
      for (int i = 0; i < 100; i++) {
        stp1.execute(run);
      }
      stp1.terminate();
      assertEquals(200, val.get());
      final String mps1 = FileHelper.fileToString(progressFile).substring(mps.length());
      assertFalse(mps1, mps1.contains("Starting"));
      assertFalse(mps1, mps1.contains("Finished"));
    } finally {
      FileHelper.deleteAll(testDir);
    }
  }

  public void testJumble() throws IOException {
    final SimpleThreadPool stp = new SimpleThreadPool(2, "TestQueue", true);
    final ThreadGroup tg = Thread.currentThread().getThreadGroup();
    final Thread[] threads = new Thread[tg.activeCount()];
    final int length = tg.enumerate(threads);
    stp.terminate();
    try {
      Thread.sleep(100);
    } catch (final InterruptedException ignored) {

    }
    boolean done = false;
    for (int i = 0; i < length; i++) {
      final Thread t = threads[i];
      if (t.getName().equals("SimpleThreadPool-TestQueue-Queue")) {
        assertTrue(t.isDaemon());
        assertTrue(!t.isAlive());
        done = true;
      }
    }
    assertTrue(done);
  }

// Testing simple thread pools interruption behaviour
//  private static class ExceptionRunnable implements IORunnable {
//    final int mNum1;
//    final int mNum2;
//
//    public ExceptionRunnable(int num1, int num2) {
//      mNum1 = num1;
//      mNum2 = num2;
//    }
//    @Override
//    public void run() {
//      if (mNum1 == 0 && mNum2 == 0) {
//        throw new RuntimeException("This is the test");
//      }
//      while (true) {
//        ProgramState.checkAbort();
//        try {
//          Thread.sleep(1000);
//        } catch (InterruptedException e) {
//          //Don't Care
//        }
//      }
//    }
//  }
//
//  private static class InnerThreadRunnable implements IORunnable {
//    final int mNum;
//    public InnerThreadRunnable(int num) {
//      mNum = num;
//    }
//    @Override
//    public void run() {
//      final int numThreads = mNum + 4;
//      final SimpleThreadPool innerThreadPool = new SimpleThreadPool(numThreads, "InnerThreadPool");
//      for (int i = numThreads - 1; i >= 0; i--) {
//        innerThreadPool.execute(new ExceptionRunnable(mNum, i));
//      }
//      innerThreadPool.terminate();
//    }
//  }
//
//  public void testInnerThreadPooling() {
//    Diagnostic.setLogStream(System.err);
//    final int numThreads = 8;
//    final SimpleThreadPool outerThreadPool = new SimpleThreadPool(numThreads, "OuterThreadPool");
//    for (int i = numThreads - 1; i >= 0; i--) {
//      outerThreadPool.execute(new InnerThreadRunnable(i));
//    }
//    outerThreadPool.terminate();
//  }
}
