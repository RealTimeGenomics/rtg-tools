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

package com.rtg.reader;

import java.io.File;
import java.io.IOException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class SimplePrereadNamesTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final SimplePrereadNames sprn = new SimplePrereadNames();
    sprn.setName(0, "first");
    sprn.setName(1, "second");
    sprn.setName(2, "third");
    sprn.setName(3, "fourth");
    assertEquals(4L, sprn.length());
    assertEquals("first", sprn.name(0));
    assertEquals("second", sprn.name(1));
    assertEquals("third", sprn.name(2));
    assertEquals("fourth", sprn.name(3));
    assertEquals(62, sprn.bytes());
    StringBuilder sb = new StringBuilder();
    sprn.writeName(sb, 2);
    assertEquals("third", sb.toString());
    MemoryPrintStream mps = new MemoryPrintStream();
    sprn.writeName(mps.outputStream(), 1);
    assertEquals("second", mps.toString());
  }

  public void testViaFile() throws Exception {
    Diagnostic.setLogStream();
    final File dir = FileHelper.createTempDirectory();
    try {
      final File queryDir = ReaderTestUtils.getDNADir(PrereadNamesTest.SEQ_DNA_A2, new File(dir, "q"));
      final PrereadNames names = new PrereadNames(queryDir, LongRange.NONE);
      final SimplePrereadNames sprn = new SimplePrereadNames();
      for (long i = 0; i < names.length(); i++) {
        sprn.setName(i, names.name(i));
      }
      assertEquals(names.calcChecksum(), sprn.calcChecksum());
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }


}
