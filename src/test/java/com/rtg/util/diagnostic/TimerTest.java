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
package com.rtg.util.diagnostic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;

/**
 */
public class TimerTest extends TestCase {

  /**
   * Test method for {@link com.rtg.util.diagnostic.Timer}.
   */
  public final void testForce() {
    final Timer ti = new Timer("B_timer");
    ti.start();
    ti.stop();
    ti.start();
    ti.stop();
    ti.setTime(1000000000);
    final String s = ti.toString();
    assertTrue(s.contains("Timer B_timer      1.00  count 2       0.50 bytes read 0"));
  }

  /**
   * Test method for {@link com.rtg.util.diagnostic.Timer}.
   * @throws IOException if an I/O error occurs.
   */
  public final void testLog1() throws IOException {
    final ByteArrayOutputStream ba = new ByteArrayOutputStream();
    final PrintStream pr = new PrintStream(ba);
    Diagnostic.setLogStream(pr);
    final Timer ti = new Timer("A_timer");
    checkUninitialized(ti);
    ti.reset();
    checkUninitialized(ti);

    ti.start();
    checkRunning(ti);
    ti.stop();
    checkStopped(ti);

    final String s1 = ti.toString();
    assertTrue(s1.contains("Timer A_timer "));
    assertTrue(s1.contains(" count 1 "));

    ti.start();
    checkRunning(ti);
    ti.stop();
    checkStopped(ti);

    final String s2 = ti.toString();
    assertTrue(s2.contains("Timer A_timer "));
    assertTrue(s2.contains(" count 2 "));
    ti.log();
    ti.log("foo");
    final String s3 = ba.toString();
    //System.err.println(s3);
    assertTrue(s3.contains(" Timer A_timer "));
    assertTrue(s3.contains(" Timer A_timer_foo "));
    assertTrue(s3.contains(" count 2 "));
    pr.close();
    ba.close();

    ti.reset();
    Diagnostic.setLogStream();
  }

  /**
   * Test method for {@link com.rtg.util.diagnostic.Timer}.
   * Supply names on reset.
   * @throws IOException if an I/O error occurs.
   */
  public final void testLog2() throws IOException {
    final ByteArrayOutputStream ba = new ByteArrayOutputStream();
    final PrintStream pr = new PrintStream(ba);
    Diagnostic.setLogStream(pr);
    final Timer ti = new Timer("A_timer");
    checkUninitialized(ti);
    ti.reset("B_timer");
    assertTrue(ti.toString().contains("Timer B_timer empty"));
    checkUninitialized(ti);

    ti.start();
    checkRunning(ti);
    ti.stop();
    checkStopped(ti);

    final String s1 = ti.toString();
    assertTrue(s1.contains("Timer B_timer "));
    assertTrue(s1.contains(" count 1 "));

    ti.start();
    checkRunning(ti);
    ti.stop(5);
    checkStopped(ti);
    ti.start();
    ti.stop(10);
    final String s2 = ti.toString();
    assertTrue(s2.contains("Timer B_timer "));
    assertTrue(s2.contains(" count 3 "));
    assertTrue(s2.contains("bytes read 15"));
    ti.log();
    ti.log("foo");
    final String s3 = ba.toString();
    //System.err.println(s3);
    assertTrue(s3.contains(" Timer B_timer "));
    assertTrue(s3.contains(" Timer B_timer_foo "));
    assertTrue(s3.contains(" count 3 "));
    pr.close();
    ba.close();

    ti.reset("C_timer");
    Diagnostic.setLogStream();
  }

  public final void testIncrement() {
    final Timer ti = new Timer("A_timer");
    checkUninitialized(ti);
    ti.increment(4200000000L);
    ti.increment(4200000000L);
    assertEquals("Timer A_timer      8.40  count 2       4.20 bytes read 0", ti.toString());
  }

  private void checkUninitialized(final Timer ti) {
    ti.integrity();
    try {
      ti.stop();
      fail();
    } catch (final IllegalStateException e) {
      //expected
    }
  }

  private void checkStopped(final Timer ti) {
    ti.integrity();
    try {
      ti.stop();
      fail();
    } catch (final IllegalStateException e) {
      //expected
    }
  }


  private void checkRunning(final Timer ti) {
    ti.integrity();
    try {
      ti.start();
      fail();
    } catch (final IllegalStateException e) {
      //expected
    }
    try {
      ti.toString();
      fail();
    } catch (final IllegalStateException e) {
      //expected
    }
  }

  public void testBadName() {
    try {
      new Timer("a b c");
      fail();
    } catch (final IllegalArgumentException e) {
      assertTrue(e.getMessage().startsWith("Name contains spaces:"));
    }
  }

  public void testEnum() {
    assertEquals("UNINITIALIZED", Timer.State.UNINITIALIZED.toString());
    //
    assertEquals(0, Timer.State.UNINITIALIZED.ordinal());
    assertEquals(1, Timer.State.STOPPED.ordinal());
    assertEquals(2, Timer.State.RUNNING.ordinal());
    assertEquals(Timer.State.UNINITIALIZED, Timer.State.valueOf("UNINITIALIZED"));
    assertEquals(Timer.State.STOPPED, Timer.State.valueOf("STOPPED"));
    assertEquals(Timer.State.RUNNING, Timer.State.valueOf("RUNNING"));

  }
}

