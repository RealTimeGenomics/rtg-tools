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
package com.rtg.jmx;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 */
public class DiskStatsTest extends TestCase {

  public void test() throws IOException {
    final DiskStats ds = new DiskStats("rubbish");
    final StringBuffer sb = new StringBuffer();
    ds.addHeader(sb);
    assertEquals(sb.toString(), 0, sb.length());
    sb.setLength(0);
    ds.addColumnData(sb);
    assertEquals(sb.toString(), 0, sb.length());
    sb.setLength(0);
    ds.addColumnLabelsTop(sb);
    assertEquals(sb.toString(), 0, sb.length());
    sb.setLength(0);
    ds.addColumnLabelsBottom(sb);
    assertEquals(sb.toString(), 0, sb.length());
  }

  public void test2() throws IOException {
    final DiskStats ds = new DiskStats("sda");
    final StringBuffer sb = new StringBuffer();
    ds.addHeader(sb);
    assertEquals(0, sb.length());

    ds.addColumnLabelsTop(sb);
    if (sb.length() > 0) {
      assertTrue(sb.toString().contains("sda"));

      sb.setLength(0);
      ds.addColumnData(sb);
      assertTrue(sb.length() > 0);
    }
  }

  public static Test suite() {
    return new TestSuite(DiskStatsTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(DiskStatsTest.class);
  }

}

