/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.simulation.reads;

import java.io.File;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.reader.PrereadType;
import com.rtg.reader.Sdf2Fasta;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.test.AbstractTempFileHandler;
import com.rtg.util.test.FileHelper;

/**
 */
public class SdfReadWriterTest extends AbstractTempFileHandler {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    GlobalFlags.resetAccessedStatus();
    Diagnostic.setLogStream();
  }

  public void testPaired() throws Exception {
    final File sdf = new File(mTempDir, "sdf");
    final SdfReadWriter w = new SdfReadWriter(sdf, true, PrereadType.SOLEXA, true, true);
    assertEquals(0, w.readsWritten());
    try {
      w.writeRead("read", new byte[]{1, 2, 3, 4}, new byte[]{1, 2, 3, 4}, 4);
      fail();
    } catch (final IllegalStateException e) {
      // ok
    }
    w.writeLeftRead("read", new byte[]{1, 2, 3, 4}, new byte[]{1, 2, 3, 4}, 4);
    w.writeRightRead("read", new byte[]{1, 2, 3, 4}, new byte[]{1, 2, 3, 4}, 4);
    w.close();
    assertEquals(1, w.readsWritten());
    final File fasta = new File(mTempDir, "f.fasta.gz");
    assertEquals(0, new Sdf2Fasta().mainInit(new String[]{"-i", sdf.getPath(), "-o", fasta.getPath()}, TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream()));
    assertEquals(">0 read\n" + "ACGT\n", FileHelper.gzFileToString(new File(mTempDir, "f_1.fasta.gz")));
    assertEquals(">0 read\n" + "ACGT\n", FileHelper.gzFileToString(new File(mTempDir, "f_2.fasta.gz")));
  }

  public void testSingle() throws Exception {
    final File sdf = new File(mTempDir, "sdf");
    final SdfReadWriter w = new SdfReadWriter(sdf, false, PrereadType.SOLEXA, true, true);
    try {
      w.writeLeftRead("read", new byte[] {1, 2, 3, 4}, new byte[] {1, 2, 3, 4}, 4);
      fail();
    } catch (final IllegalStateException e) {
      // ok
    }
    try {
      w.writeRightRead("read", new byte[] {1, 2, 3, 4}, new byte[] {1, 2, 3, 4}, 4);
      fail();
    } catch (final IllegalStateException e) {
      // ok
    }
    w.writeRead("read", new byte[] {1, 2, 3, 4}, new byte[] {1, 2, 3, 4}, 4);
    w.close();
    assertEquals(1, w.readsWritten());
    final File fasta = new File(mTempDir, "f.fasta.gz");
    assertEquals(0, new Sdf2Fasta().mainInit(new String[] {"-i", sdf.getPath(), "-o", fasta.getPath()}, TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream()));
    assertEquals(">0 read\n" + "ACGT\n", FileHelper.gzFileToString(new File(mTempDir, "f.fasta.gz")));
  }
}
