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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;
import com.rtg.util.test.FileHelper;

/**
 * Tests for the corresponding class.
 */
public class Sdf2QualaTest extends AbstractCliTest {


  @Override
  protected AbstractCli getCli() {
    return new Sdf2Quala();
  }

  public void testProcess1() throws IOException, InvalidParamsException {
    final ByteArrayOutputStream seqStream = new ByteArrayOutputStream();
    final LineWriter seqWriter = new LineWriter(new OutputStreamWriter(seqStream));
    final ByteArrayOutputStream qualStream = new ByteArrayOutputStream();
    final LineWriter qualWriter = new LineWriter(new OutputStreamWriter(qualStream));
    Sdf2Quala.process(new ArraySequencesReader(new byte[][] {{1}}, new byte[][] {{0}}), seqWriter, qualWriter, false, (byte) -1);
    seqWriter.close();
    qualWriter.close();
    assertEquals(">sequence 0" + StringUtils.LS + "A" + StringUtils.LS, seqStream.toString());
    assertEquals(">sequence 0" + StringUtils.LS + "0" + StringUtils.LS, qualStream.toString());
  }

  public void testProcess2() throws IOException, InvalidParamsException {
    final ByteArrayOutputStream seqStream = new ByteArrayOutputStream();
    final LineWriter seqWriter = new LineWriter(new OutputStreamWriter(seqStream));
    final ByteArrayOutputStream qualStream = new ByteArrayOutputStream();
    final LineWriter qualWriter = new LineWriter(new OutputStreamWriter(qualStream));
    Sdf2Quala.process(new ArraySequencesReader(new byte[][] {{1, 2, 3, 4}}, new byte[][] {{0, 42, 85, 86}}), seqWriter, qualWriter, false, (byte) -1);
    seqWriter.close();
    qualWriter.close();
    assertEquals(">sequence 0" + StringUtils.LS + "ACGT" + StringUtils.LS, seqStream.toString());
    assertEquals(">sequence 0" + StringUtils.LS + "0 42 85 86" + StringUtils.LS, qualStream.toString());
  }

  private static final String FULL_NAME_SEQ_DATA = ""
          + ">name suffix" + StringUtils.LS
          + "ACGTCG" + StringUtils.LS
          + ">second suffixes" + StringUtils.LS
          + "ACGGGT" + StringUtils.LS
          ;

  private static final String FULL_NAME_QUAL_DATA = ""
          + ">name suffix" + StringUtils.LS
          + "16 17 18 19 20 21" + StringUtils.LS
          + ">second suffixes" + StringUtils.LS
          + "16 17 18 19 20 21" + StringUtils.LS
          ;

  public void testFullName() throws IOException {
    final File dir = FileUtils.createTempDir("testsdf2fasta", "fullname");
    try {
      final File sdf = ReaderTestUtils.getDNAFastqDir(Sdf2FastqTest.FULL_NAME_DATA, new File(dir, "sdf"), false);
      final File base = new File(dir, "fs");
      checkMainInitOk("-i", sdf.getPath(), "-o", base.getPath());
      assertEquals(FULL_NAME_SEQ_DATA, FileHelper.gzFileToString(new File(base + ".fasta.gz")));
      assertEquals(FULL_NAME_QUAL_DATA, FileHelper.gzFileToString(new File(base + ".quala.gz")));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testHelp() {
    checkHelp("basename for output files (extensions will be added)",
      "SDF containing sequences");
  }

  private void checkQualityBadValue(final int qual) {
    final String err = checkMainInitBadFlags("-i", "blah", "-o", "blaho", "-q", String.valueOf(qual));
    TestUtils.containsAll(err, "Error: The specified flag \"default-quality\" has invalid value \"" + qual + "\". It should be");
  }

  public void testValidator() {
    checkQualityBadValue(-1);
    checkQualityBadValue(64);
  }

}
