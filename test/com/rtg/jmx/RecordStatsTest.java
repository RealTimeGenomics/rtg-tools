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
public class RecordStatsTest extends TestCase {

  private static final String LS = System.lineSeparator();

  public void test() throws IOException {
    final StringBuffer sb = new StringBuffer();
    final RecordStats ds = new RecordStats(sb, 10000);
    ds.addStats(new MonStats() {
        @Override
        public void addHeader(Appendable out) throws IOException {
          out.append("# Test").append(LS);
        }

        @Override
        public void addColumnLabelsTop(Appendable out) throws IOException {
          out.append("Top ");
        }

        @Override
        public void addColumnLabelsBottom(Appendable out) throws IOException {
          out.append("Bot ");
        }

        @Override
        public void addColumnData(Appendable out) throws IOException {
          out.append("Data");
        }

      });
    ds.addHeader();
    assertTrue(sb.toString(), sb.toString().startsWith("#"));
    assertEquals(2, sb.toString().split(LS).length);
    sb.setLength(0);
    ds.addColumnLabels();
    assertTrue(sb.toString(), sb.toString().startsWith("#"));
    assertEquals(3, sb.toString().split(LS).length);
    sb.setLength(0);
    ds.addColumnData();
    assertTrue(sb.toString(), sb.toString().contains("Data"));
    assertEquals(1, sb.toString().split(LS).length);
  }

  public static Test suite() {
    return new TestSuite(RecordStatsTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(RecordStatsTest.class);
  }

}

