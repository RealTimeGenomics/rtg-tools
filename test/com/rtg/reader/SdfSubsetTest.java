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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.Utils;
import com.rtg.util.io.TestDirectory;

/**
 * Test for SdfSubset
 *
 */
public class SdfSubsetTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new SdfSubset();
  }

  public void testHelp() {
    checkHelp("Extracts a subset of sequences from one SDF and outputs them to another SDF.",
              "output SDF",
              "SDF containing sequences",
              "id of sequence",
              "file containing sequence ids, or sequence names if --names flag is set, one per line");
  }

  public void testValidator() throws Exception {
    final String err = checkHandleFlagsErr("-i", "genome", "-o", "out");
    TestUtils.containsAll(err, "Sequences to extract must be specified");
  }

  private void checkPairedSplittiness(String... extraParams) throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("sdfsubsettest")) {
      final String left = ">seq1" + StringUtils.LS + "acgt" + StringUtils.LS + ">seq2" + StringUtils.LS + "cgta";
      final String right = ">seq1" + StringUtils.LS + "tgca" + StringUtils.LS + ">seq2" + StringUtils.LS + "atgc";


      final File readLeftDir = new File(tempDir, "left");
      ReaderTestUtils.getReaderDNA(left, readLeftDir, null).close();
      final File readRightDir = new File(tempDir, "right");
      ReaderTestUtils.getReaderDNA(right, readRightDir, null).close();
      final File outDir = new File(tempDir, "outDir");


      checkMainInitOk(Utils.append(new String[] {"-i", tempDir.getPath(), "-o", outDir.getPath()}, extraParams));

      SdfId leftGuid;
      SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(new File(outDir, "left"));
      try {
        assertEquals(4, dsr.totalLength());
        assertEquals(4, dsr.maxLength());
        assertEquals(4, dsr.minLength());
        assertEquals(1, dsr.numberSequences());
        assertNotNull(dsr.getSdfId());
        leftGuid = dsr.getSdfId();
        assertTrue(dsr.hasNames());
        assertEquals("seq2", dsr.name(0));
        assertFalse(dsr.hasQualityData());
      } finally {
        dsr.close();
      }
      dsr = SequencesReaderFactory.createDefaultSequencesReader(new File(outDir, "right"));
      try {
        assertEquals(4, dsr.totalLength());
        assertEquals(4, dsr.maxLength());
        assertEquals(4, dsr.minLength());
        assertEquals(1, dsr.numberSequences());
        assertNotNull(dsr.getSdfId());
        assertEquals(leftGuid, dsr.getSdfId());
        assertTrue(dsr.hasNames());
        assertEquals("seq2", dsr.name(0));
        assertFalse(dsr.hasQualityData());
      } finally {
        dsr.close();
      }
    }

  }
  public void testPairedSplittiness() throws Exception {
    checkPairedSplittiness("1");
  }
  public void testPairedSplittinessNames() throws Exception {
    checkPairedSplittiness("-n", "seq2");
  }
  public void testPairedSplittinessRange() throws Exception {
    checkPairedSplittiness("--start-id", "1", "--end-id", "2");
  }
}
