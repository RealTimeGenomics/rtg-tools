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

package com.rtg.launcher;

import java.io.File;
import java.io.IOException;

import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 */
public class DummyStatisticsTest extends TestCase {

  private static class DummyStatistics extends AbstractStatistics {

    protected String mStats = null;

    /**
     * @param outputDirectory The base output directory to generate statistics and reports in. May be null if no statistics or reports are to be generated.
     */
    DummyStatistics(File outputDirectory) {
      super(outputDirectory);
    }

    @Override
    protected String getStatistics() {
      return mStats;
    }

    @Override
    public void generateReport() { }
  }

  @Override
  public void tearDown() {
    Diagnostic.setLogStream();
  }

  public void test() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    final MemoryPrintStream ps2 = new MemoryPrintStream();

    Diagnostic.setLogStream(ps2.printStream());
    final DummyStatistics stat = new DummyStatistics(null);
    stat.printStatistics(null);
    assertEquals("", ps.toString());
    stat.printStatistics(null);
    assertEquals("", ps.toString());
    stat.mStats = "Line1" + StringUtils.LS + "Line2";

    stat.printStatistics(ps.outputStream());
    assertEquals("Line1" + StringUtils.LS + "Line2", ps.toString());
    assertEquals(2, ps.toString().split(StringUtils.LS).length);

    TestUtils.containsAll(ps2.toString(), "Line1", "Line2"); //logged statistics
    assertEquals(2, ps2.toString().split(StringUtils.LS).length);
  }
}
