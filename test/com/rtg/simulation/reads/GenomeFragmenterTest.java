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
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reference.ReferenceGenome;
import com.rtg.util.ChiSquared;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class GenomeFragmenterTest extends TestCase {

  private static final class MyMachine extends DummyMachineTest.MockMachine {

    private boolean mSeen = false;
    private byte[] mLastFragment;
    boolean mAllGs = true;

    @Override
    public void processFragment(String id, int fragmentStart, byte[] data, int length) {
      if (mAllGs) {
        for (int i = 0; i < length; ++i) {
          assertEquals(3, data[i]);
        }
      }
      mSeen = true;
      mLastFragment = Arrays.copyOf(data, length);
    }
  }

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  public void testSetMachine() throws Exception {
    final File temp = FileUtils.createTempDir("genomefrag", "test");
    try {
      try (SequencesReader sr = ReaderTestUtils.getReaderDNA(">g\nggggggggggggggggggggggggggggggggggggggggggggggg", new File(temp, "seq"), null)) {
        final GenomeFragmenter gf = new GenomeFragmenter(42, sr);
        gf.setMaxFragmentSize(10);
        gf.setMaxFragmentSize(5);
        final MyMachine m = new MyMachine();
        gf.setMachine(m);
        gf.makeFragment();
        assertTrue(m.mSeen);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(temp));
    }
  }

  static String g(final int n) {
    final StringBuilder sb = new StringBuilder();
    for (int k = 0; k < n; ++k) {
      sb.append('g');
    }
    return sb.toString();
  }

  private static final long LOOPS = 10;
  // 0.999 corresponds to expected failure of 1 in 1000 runs
  private static final double CONFIDENCE_LEVEL = 0.999;

  public void testUniformityOfStartPosition() throws Exception {
    final int testSize = 100;
    final File temp = FileUtils.createTempDir("genomefrag", "test");
    try {
      try (SequencesReader sr = ReaderTestUtils.getReaderDNA(">0\n" + g(testSize), new File(temp, "seq"), null)) {
        final int[] counts0 = new int[testSize];
        //final GenomeFragmenter gf = new GenomeFragmenter(42, sr);
        final GenomeFragmenter gf = new GenomeFragmenter(0, sr);
        gf.setMaxFragmentSize(0);
        gf.setMachine(new DummyMachineTest.MockMachine() {
          @Override
          public void processFragment(final String id, final int fragmentStart, final byte[] data, final int length) {
            assertEquals(0, length);
            counts0[fragmentStart]++;
          }
        });
        for (long k = 0; k < LOOPS * testSize; ++k) {
          gf.makeFragment();
        }

        // Perform chi-squared test for uniformity
        double sum = 0;
        for (final int c : counts0) {
          final double g = c - LOOPS;
          sum += g * g / LOOPS;
        }
        assertTrue(sum < ChiSquared.chi(testSize - 1, CONFIDENCE_LEVEL));
        assertTrue(sum > ChiSquared.chi(testSize - 1, 1 - CONFIDENCE_LEVEL));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(temp));
    }
  }

  public void testBogusParameters() throws Exception {
    final File temp = FileUtils.createTempDir("genomefrag", "test");
    try {
      try (SequencesReader sr = ReaderTestUtils.getReaderDNA(">0\n" + g(42), new File(temp, "seq"), null)) {
        final GenomeFragmenter gf = new GenomeFragmenter(0, sr);
        gf.setMinFragmentSize(20);
        gf.setMaxFragmentSize(10);
        gf.setMachine(new DummyMachineTest.MockMachine() {
          @Override
          public void processFragment(final String id, final int fragmentStart, final byte[] data, final int length) {
            fail(); // should not get here
          }
        });
        try {
          gf.makeFragment();
          fail();
        } catch (final IllegalStateException e) {
          // expected
        }
      }
    } finally {
      assertTrue(FileHelper.deleteAll(temp));
    }
  }

  public void testUniformityOfStartPositionTwoUnequalTemplates() throws Exception {
    final int testSize = 100;
    Exam.assertEquals(testSize & 1, 0);
    final File temp = FileUtils.createTempDir("genomefrag", "test");
    try {
      try (SequencesReader sr = ReaderTestUtils.getReaderDNA(">0\n" + g(testSize) + "\n>1\n" + g(testSize / 2), new File(temp, "seq"), null)) {
        final int[] counts0 = new int[testSize];
        final int[] counts1 = new int[testSize / 2];
        //final GenomeFragmenter gf = new GenomeFragmenter(42, sr);
        final GenomeFragmenter gf = new GenomeFragmenter(0, sr);
        gf.setMaxFragmentSize(0);
        gf.setMachine(new DummyMachineTest.MockMachine() {
          @Override
          public void processFragment(final String id, final int fragmentStart, final byte[] data, final int length) {
            assertEquals(0, length);
            final char rid = id.charAt(id.length() - 2);
            if (rid == '0') {
              counts0[fragmentStart]++;
            } else if (rid == '1') {
              counts1[fragmentStart]++;
            } else {
              fail();
            }
          }
        });
        final int tlim = testSize + testSize / 2;
        for (long k = 0; k < LOOPS * tlim; ++k) {
          gf.makeFragment();
        }

        // Perform chi-squared test for uniformity
        double sum = 0;
        for (final int c : counts0) {
          final double g = c - LOOPS;
          sum += g * g / LOOPS;
        }
        for (final int c : counts1) {
          final double g = c - LOOPS;
          sum += g * g / LOOPS;
        }
        assertTrue(sum < ChiSquared.chi(tlim - 1, CONFIDENCE_LEVEL));
        assertTrue(sum > ChiSquared.chi(tlim - 1, 1 - CONFIDENCE_LEVEL));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(temp));
    }
  }

  public void checkNormalityOfLengths(final int min, final int max) throws Exception {
    final int testSize = 1000;
    final File temp = FileUtils.createTempDir("genomefrag", "test");
    try {
      try (SequencesReader sr = ReaderTestUtils.getReaderDNA(">0\n" + g(testSize), new File(temp, "seq"), null)) {
        final int[] counts0 = new int[max + 1];
        //final GenomeFragmenter gf = new GenomeFragmenter(42, sr);
        final GenomeFragmenter gf = new GenomeFragmenter(0, sr);
        gf.setMinFragmentSize(min);
        gf.setMaxFragmentSize(max);
        gf.setMachine(new  DummyMachineTest.MockMachine() {
          @Override
          public void processFragment(final String id, final int fragmentStart, final byte[] data, final int length) {
            assertTrue(length >= min);
            assertTrue(length <= max);
            counts0[length]++;
          }
        });
        for (long k = 0; k < testSize; ++k) {
          gf.makeFragment();
        }

        // Perform chi-squared test for uniformity
        final double mean = (max + min) * 0.5;
        final double variance = (max - min) * 0.25;
        double sum = 0;
        assertEquals(0.5, ChiSquared.normal(0), 1e-10);
        assertEquals(1, ChiSquared.normal(1e300), 1e-10);
        for (int k = min; k < counts0.length; ++k) {
          final int c = counts0[k];
          final double left = (k - 0.5 - mean) / variance;
          final double right = (k + 0.5 - mean) / variance;
          final double e = (ChiSquared.normal(right) - ChiSquared.normal(left)) * testSize;
          //System.out.println(k + " " + c + " " + e);
          final double g = c - e;
          sum += g * g / e;
        }
        assertTrue(sum < ChiSquared.chi(max - min, CONFIDENCE_LEVEL));
        assertTrue(sum > ChiSquared.chi(max - min, 1 - CONFIDENCE_LEVEL));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(temp));
    }
  }

  public void testNormalityOfLengths() throws Exception {
    checkNormalityOfLengths(5, 55);
    checkNormalityOfLengths(5, 6);
    checkNormalityOfLengths(0, 3);
  }

  public void testCircularRef() throws Exception {
    for (int k = 1; k < 7; ++k) {
      final File temp = FileUtils.createTempDir("genomefrag", "test");
      try {
        final SequencesReader sr = ReaderTestUtils.getReaderDNA(">g\n" + g(k), new File(temp, "seq"), null);
        FileUtils.stringToFile("version 1" + StringUtils.LS + "either\tdef\thaploid\tcircular", new File(sr.path(), ReferenceGenome.REFERENCE_FILE));
        try {
          final GenomeFragmenter gf = new GenomeFragmenter(42, sr);
          gf.setMaxFragmentSize(5);
          final MyMachine m = new MyMachine();
          gf.setMachine(m);
          gf.makeFragment();
          assertTrue(m.mSeen);
        } finally {
          sr.close();
        }
      } finally {
        assertTrue(FileHelper.deleteAll(temp));
      }
   }
  }

  public void testNCheck() throws Exception {
    try (TestDirectory temp = new TestDirectory()) {
      try (SequencesReader sr = ReaderTestUtils.getReaderDNA(">g\ngnggggnggggnggggggngggggggggggggggggggggggggggg", new File(temp, "seq"), null)) {
        final GenomeFragmenter gf = new GenomeFragmenter(42, sr);
        gf.setMaxFragmentSize(10);
        gf.setMinFragmentSize(10);
        final MyMachine m = new MyMachine();
        m.mAllGs = false;
        gf.setMachine(m);
        // No ns produced
        gf.allowNs(false);
        for (int i = 0; i < 100; ++i) {
          gf.makeFragment();
          for (byte b : m.mLastFragment) {
            assertFalse(DNA.N.ordinal() == b);
          }
        }
        gf.allowNs(true);
        // Ns allowed and below threshold
        boolean nsFound = false;
        for (int i = 0; i < 100; ++i) {
          gf.makeFragment();
          for (byte b : m.mLastFragment) {
            if (b == DNA.N.ordinal()) {
              nsFound = true;
            }
          }
        }
        assertTrue(nsFound);

        // Ns allowed but will be above threshold
        gf.setMaxFragmentSize(9);
        gf.setMinFragmentSize(9);
        for (int i = 0; i < 100; ++i) {
          gf.makeFragment();
          for (byte b : m.mLastFragment) {
            assertFalse(DNA.N.ordinal() == b);
          }
        }
      }
    }
  }
}
