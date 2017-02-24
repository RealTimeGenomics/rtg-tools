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

package com.rtg.simulation.genome;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

/**
 * Test the corresponding class
 */
public class GenomeSimulatorTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new GenomeSimulator();
  }

  public final void testGetCFlags() {

    checkHelp("seed for random number generator",
        "maximum sequence length",
        "minimum sequence length",
        "length of generated sequence",
        "relative frequencies of A,C,G,T in the generated sequence",
        "1,1,1,1",
        "comment to include in the generated SDF",
    "number of sequences to generate",
    "Simulates a reference genome.");

  }

  private File mTempFile;
  private File mTempDir;

  @Override
  public void setUp() throws IOException {
    super.setUp();
    mTempDir = FileUtils.createTempDir("testErr1", "GenomesimulatorTest");
    mTempFile = new File(mTempDir, "GenomesimulatorTest");
  }

  @Override
  public void tearDown() throws IOException {
    super.tearDown();
    FileHelper.deleteAll(mTempDir);
    mTempDir = null;
    mTempFile = null;
  }

  static final String USAGE1 = "rtg genomesim [OPTION]... -o SDF --max-length INT --min-length INT -n INT";
  static final String USAGE2 = "[OPTION]... -o SDF -l INT";

  private String checkError(final String... args) {
    final String[] newargs = new String[args.length + 2];
    System.arraycopy(args, 0, newargs, 0, args.length);
    newargs[newargs.length - 2] = "--seed";
    newargs[newargs.length - 1] = "65";
    final String error = checkHandleFlagsErr(newargs);
    TestUtils.containsAll(error, USAGE1, USAGE2);
    return error;
  }

  private void checkNoError(final String... args) {
    checkHandleFlagsOut(args);
  }

  public void testErr1() {

    final String err = checkError("--comment", "blahblah", "--min-length=45", "--max-length=44", "-o", mTempFile.getPath());
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "number of sequences");
   }
  public void testErr2() {
    final String err = checkError("--min-length=45", "--max-length=44", "-o", mTempFile.getPath(), "-n", "5");
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "cannot be greater than");
   }
  public void testErr3() {
    final String err = checkError("--length=2", "--min-length=45", "--max-length=44", "-o", mTempFile.getPath(), "-n", "5");
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "sequence length", "minimum", "maximum");
   }
  public void testErr4() {
    final String err = checkError("--length=2", "--min-length=45", "-o", mTempFile.getPath(), "-n", "5");
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "sequence length", "minimum", "maximum");
   }

  public void testErrSequenceLength() {
    final String err = checkError("-o", mTempFile.getPath(), "-l", "bob");
    TestUtils.containsAll(err, "Invalid value \"bob\" for flag -l.");
   }

  public void testErrNoError() {
    checkNoError("-o", mTempFile.getPath(), "-l", "1000");
   }

  public void testErrFrequency2() {
    final String err = checkError("-o", mTempFile.getPath(), "-l", "1000", "--freq", "1,1,1,-1");
    TestUtils.containsAll(err, "Expected non-negative integers for frequency distribution.");
   }

  public void testErrFrequency3() {
    final String err = checkError("-o", mTempFile.getPath(), "-l", "1000", "--freq", "0,0,0,0");
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "Expected at least one non-zero integer for frequency distribution.");
   }

  public void testFrequencyError() {
    final String err = checkError("--length=2", "-o", mTempFile.getPath(), "--freq", "1,2,5");
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "Expected four comma separated integers for frequency distribution.");
   }

  public void testFrequencyError2() {
    final String err = checkError("--length=2", "-o", mTempFile.getPath(), "--freq", "1,2,bob,5");
    //System.out.println("GenomeSimulatorTest\n" + err);
    TestUtils.containsAll(err, "Invalid frequency distribution format");
   }

  public void testExplicitLengths() throws IOException {
    checkMainInitOk("-o", mTempFile.getAbsolutePath(), "-l", "200", "-l", "300");

    final SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(mTempFile);
    assertEquals(2, reader.numberSequences());
    assertEquals(300, reader.maxLength());
    assertEquals(200, reader.minLength());
    assertEquals(500, reader.totalLength());
  }

  public void testVariableLengths() throws IOException {
    checkMainInitOk("-o", mTempFile.getAbsolutePath(), "-n", "200", "--max-length", "60", "--min-length", "30");

    final SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(mTempFile);
    assertEquals(200, reader.numberSequences());
    assertTrue(reader.maxLength() <= 60);
    assertTrue(reader.minLength() >= 30);
  }

  public void testVariableLengths2() throws IOException {
    checkMainInitOk("-o", mTempFile.getAbsolutePath(), "-n", "200", "--max-length", "4", "--min-length", "3", "-s", "77");

    final SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(mTempFile);
    assertEquals(200, reader.numberSequences());
    assertTrue(reader.maxLength() == 4);
    assertTrue(reader.minLength() == 3);
  }

  public void testVariableButFixedLengths() throws IOException {
    checkMainInitOk("-o", mTempFile.getAbsolutePath(), "-n", "5", "--max-length", "4", "--min-length", "4");

    final SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(mTempFile);
    assertEquals(5, reader.numberSequences());
    assertTrue(reader.maxLength() == 4);
    assertTrue(reader.minLength() == 4);
  }

  public void testDefaultFrequency() throws IOException {

    checkMainInitOk("-o", mTempFile.getAbsolutePath(), "-l", "100000");
    final SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(mTempFile);

    assertEquals(1, reader.numberSequences());
    assertEquals(100000, reader.totalLength());
    assertEquals(100000, reader.maxLength());
    assertEquals(100000, reader.minLength());
    final long[] residueCounts = reader.residueCounts();
    for (int i = 1; i < 5; ++i) {
      assertTrue(residueCounts[i] >= 24000);
      assertTrue(residueCounts[i] <= 26000);
    }
  }
  public void testFrequency() throws IOException {

    checkMainInitOk("-o", mTempFile.getAbsolutePath(), "-l", "100000", "--freq", "1,3,2,4");
    final SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(mTempFile);

    assertEquals(1, reader.numberSequences());
    assertEquals(100000, reader.totalLength());
    assertEquals(100000, reader.maxLength());
    assertEquals(100000, reader.minLength());
    final long[] residueCounts = reader.residueCounts();
    assertTrue(residueCounts[1] >= 9000);
    assertTrue(residueCounts[1] <= 11000);
    assertTrue(residueCounts[2] >= 29000);
    assertTrue(residueCounts[2] <= 31000);
    assertTrue(residueCounts[3] >= 19000);
    assertTrue(residueCounts[3] <= 21000);
    assertTrue(residueCounts[4] >= 39000);
    assertTrue(residueCounts[4] <= 41000);
  }
}
