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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rtg.util.PortableRandom;

import junit.framework.TestCase;


/**
 */
public class ConcurrentByteQueueTest extends TestCase {

  private ConcurrentByteQueue mQueue = null;
  private static final byte[] DATA = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

  @Override
  public void setUp() {
    mQueue = new ConcurrentByteQueue(10);
  }

  @Override
  public void tearDown() {
    mQueue = null;
  }

  public void testMaxSize() {
    assertEquals(10, mQueue.maxSize());
  }

  public void testAvailable() throws InterruptedException {
    assertEquals(0, mQueue.available());
    mQueue.write(DATA, 0, 3);
    assertEquals(3, mQueue.available());
    final byte[] buf = new byte[DATA.length];
    assertEquals(3, mQueue.read(buf, 0, 5));
    assertEquals(0, mQueue.available());
  }

  public void testWrapAround() throws InterruptedException {
    assertEquals(0, mQueue.available());
    mQueue.write(DATA, 0, 3);
    assertEquals(3, mQueue.available());
    final byte[] buf = new byte[DATA.length];
    assertEquals(2, mQueue.read(buf, 0, 2));
    assertEquals(0, buf[0]);
    assertEquals(1, buf[1]);
    assertEquals(1, mQueue.available());
    mQueue.write(DATA, 0, 9);
    mQueue.close();
    assertEquals(10, mQueue.available());
    assertEquals(8, mQueue.read(buf, 0, buf.length));
    assertEquals(2, buf[0]);
    assertEquals(0, buf[1]);
    assertEquals(1, buf[2]);
    assertEquals(2, buf[3]);
    assertEquals(3, buf[4]);
    assertEquals(4, buf[5]);
    assertEquals(5, buf[6]);
    assertEquals(6, buf[7]);
    assertEquals(2, mQueue.available());
    assertEquals(2, mQueue.read(buf, 0, buf.length));
    assertEquals(7, buf[0]);
    assertEquals(8, buf[1]);
    assertEquals(0, mQueue.available());
    assertEquals(-1, mQueue.read(buf, 0, buf.length));
  }



  /**
   * An executable test sequence that contains named synchronization points.
   *
   * The <code>run()</code> method should contain several chunks of code
   * (that operate on some data shared with other TestRunnable objects).
   * Each chunk of code should be preceded by <code>begin("msg...")</code>
   * and the whole sequence should finish with <code>end()</code>.
   *
   */
  public abstract static class TestRunnable implements Runnable {
    private String mNext;
    private long mStartTime;
    private long mDelays;
    private final List<Long> mTime = new ArrayList<>();
    private volatile Exception mException;

    public void setId() {
    }

    public synchronized void setDelays(long delays) {
      mDelays = delays;
    }

    /**
     * Call this method before each block of code.
     * @param msg Describes what this thread is about to do.
     */
    protected synchronized void begin(String msg) {
      if (mStartTime > 0) {
        final long time = System.nanoTime() - mStartTime;
        println(mNext + "\t" + time / 1000 + "us");
        mTime.add(time);
      }
      // TODO: block rather than just delay.
      if ((mDelays & 1) == 1) {
        println("Wait");
        final long time = System.nanoTime();
        // busy wait for a few milliseconds seconds
        while (System.nanoTime() - time < 10 * 1000 * 1000) {
          // do nothing.
        }
      }
      mDelays = mDelays >>> 1;

      // start the next operation.
      mNext = msg;
      mStartTime = System.nanoTime();
    }

    protected synchronized void end() {
      final long time = System.nanoTime() - mStartTime;
      println(mNext + ", " + time / 1000 + "us");
      mTime.add(time);
    }

    protected void println(String msg) {
      //System.err.println(msg);
    }

    protected synchronized void exception(Exception e) {
      if (mException == null) {
        mException = e;
      }
      println("Exception: " + e);
    }
  }

  /**
   * A concurrent test driver that tries random interleaving of two threads.
   */
  public static class ConcurrentTester {
    private final TestRunnable[] mSequence;
    /** The number of delay bits that we have for each thread. */
    private final int mSteps;

    public ConcurrentTester(TestRunnable t1, TestRunnable t2) {
      mSequence = new TestRunnable[] {t1, t2};
      mSteps = 64 / mSequence.length;
    }

    /**
     * Executes all the threads, inserting a delay for each 1 bit in <code>delays</code>.
     * The 64 bits of <code>delays</code> are shared out evenly between the threads.
     * @param delays Each bit that is set causes a delay in one of the threads.
     */
    public void run(long delays) {
      final Thread[] thread = new Thread[mSequence.length];
      long bits = delays;
      final long mask = (2L << mSteps) - 1;
      for (int i = 0; i < thread.length; ++i) {
        mSequence[i].setId();
        mSequence[i].setDelays(bits & mask);
        mSequence[i].println("Thread " + (i + 1) + ", " + Long.toBinaryString(bits & mask));
        bits = bits >>> mSteps;
        thread[i] = new Thread(mSequence[i], "t" + (i + 1));
      }
      // start all threads.
      for (Thread aThread : thread) {
        aThread.start();
      }
      // wait for all threads to finish.
      for (int i = 0; i < thread.length; ++i) {
        try {
          thread[i].join();
        } catch (InterruptedException e) {
          mSequence[i].println("join() threw " + e);
        }
      }
    }
  }


  public void testConcurrent() {
    final TestRunnable writer = new TestRunnable() {
      @Override
      public void run() {
        try {
          begin("write 7");
          mQueue.write(DATA, 0, 7);
          begin("write 4");
          mQueue.write(DATA, 7, 4);
          end();
        } catch (InterruptedException e) {
          exception(e);
        }
      }
    };

    final byte[] out = new byte[DATA.length];

    final boolean[] readWasNotPositive = new boolean[1];

    final TestRunnable reader = new TestRunnable() {

      @Override
      public void run() {
        int pos = 0;
        try {
          while (pos < out.length) {
            final int request = out.length - pos;
            begin("read " + request);
            final int read = mQueue.read(out, pos, out.length - pos);
            if (read == -1) {
              end();
              break;
            }
            if (!(read > 0)) {
              readWasNotPositive[0] = true;
              throw new RuntimeException();
            }
            pos += read;
          }
        } catch (InterruptedException e) {
          exception(e);
        }
      }
    };

    final PortableRandom rand = new PortableRandom(42);
    final ConcurrentTester tester = new ConcurrentTester(reader, writer);
    for (int run = 0; run < 4; ++run) {
      //System.err.println("===== run " + run + " =====");
      Arrays.fill(out, (byte) 0);
      tester.run(rand.nextLong());
      assertFalse(readWasNotPositive[0]);
      // now check that the correct contents have been transferred.
      for (int i = 0; i < out.length; ++i) {
        assertEquals("i=" + i, DATA[i], out[i]);
      }
    }
  }
}
