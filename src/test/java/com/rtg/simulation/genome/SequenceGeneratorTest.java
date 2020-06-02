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

import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Contains tests for related class
 *
 */
public class SequenceGeneratorTest extends TestCase {

  private File mOutDir = null;

  @Override
  public void setUp() throws Exception {
    mOutDir = FileUtils.createTempDir("sequencegenerator", "main");
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    FileHelper.deleteAll(mOutDir);
    mOutDir = null;
  }

  public void testMain() throws Exception {
    final PortableRandom rand = new PortableRandom(1);
    final int[] lengths = {2, 5};
    final int[] freq = {1, 1, 1, 1};
    final RandomDistribution rd = new RandomDistribution(freq, rand);
    final SequenceGenerator sdata = new SequenceGenerator(rand, rd, lengths, mOutDir, GenomeSimulator.DEFAULT_PREFIX);
    final long max = sdata.getSizeLimit();
    assertEquals(1000000000, max);
    sdata.createSequences();
    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mOutDir)) {
      //System.err.println("" + dsr.maxLength());
      assertEquals(5, dsr.maxLength());
      assertEquals(2, dsr.minLength());
      final String t = "" + dsr.type();
      assertEquals("DNA", t);
      assertEquals("simulatedSequence1", dsr.name(0));
      assertEquals(2, dsr.numberSequences());
    }
  }
}
