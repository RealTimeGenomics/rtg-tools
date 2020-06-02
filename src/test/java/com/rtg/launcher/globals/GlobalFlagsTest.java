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

package com.rtg.launcher.globals;

import java.io.File;
import java.util.List;

import com.rtg.reader.FormatCli;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class GlobalFlagsTest extends TestCase {


  static final String TEST_FLAG = "test-flag";
  static final Integer TEST_DEFAULT = 20;

  static class TestGlobalFlags extends GlobalFlagsInitializer {
    TestGlobalFlags(List<Flag<?>> flags) {
      super(flags);
    }

    @Override
    public void registerFlags() {
      registerFlag(TEST_FLAG, Integer.class, TEST_DEFAULT);
    }
  }

  private CFlags mFlags = null;

  @Override
  public void setUp() {
    mFlags = new CFlags();
    GlobalFlags.registerExperimentalFlags(mFlags);
  }

  @Override
  public void tearDown() {
    GlobalFlags.resetAccessedStatus();
    mFlags = null;
  }

  public void testDefault() {
    mFlags.setFlags();
    assertEquals(TEST_DEFAULT, GlobalFlags.getFlag(TEST_FLAG).getValue());
  }

  public void testSet() {
    mFlags.setFlags("--XX" + TEST_FLAG, "902");
    assertEquals(902, GlobalFlags.getFlag(TEST_FLAG).getValue());
    assertEquals(902, GlobalFlags.getIntegerValue(TEST_FLAG));
    assertTrue(GlobalFlags.isSet(TEST_FLAG));
  }

  public void testAccessCheckOK() {
    assertTrue(GlobalFlags.initialAccessCheck());
    mFlags.setFlags("--XX" + TEST_FLAG, "902");
    assertTrue(GlobalFlags.initialAccessCheck());
  }

  public void testAccessCheckFail() {
    mFlags.setFlags("--XX" + TEST_FLAG, "902");
    GlobalFlags.getIntegerValue(TEST_FLAG);
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    try {
      assertFalse(GlobalFlags.initialAccessCheck());
    } finally {
      Diagnostic.setLogStream();
    }
  }

  public void testUnused() {
    mFlags.setFlags();
    assertTrue(GlobalFlags.finalAccessCheck());
    mFlags.setFlags("--XX" + TEST_FLAG, "34");
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    try {
      assertFalse(GlobalFlags.finalAccessCheck());
    } finally {
      Diagnostic.setLogStream();
    }
  }

  public void testUnusedRealWorld() throws Exception {
    final MemoryPrintStream mps = new MemoryPrintStream();
    final MemoryPrintStream err = new MemoryPrintStream();

    try (TestDirectory td = new TestDirectory()) {
      final File f = new File(td, "f.fq.gz");
      FileHelper.stringToGzFile(">blah\nacgt\n", f);

      final FormatCli cli = new FormatCli();
      cli.mainInit(new String[]{"-o", td.getPath() + "/meh", f.getPath(), "--XX" + TEST_FLAG, "33"}, mps.outputStream(), err.printStream());

      assertTrue(err.toString(), err.toString().contains("--XX" + TEST_FLAG + " is set but never accessed"));

    }
  }
}
