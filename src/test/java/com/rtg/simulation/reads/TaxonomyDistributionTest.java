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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 */
public class TaxonomyDistributionTest extends TestCase {

  @Override
  protected void tearDown() {
    Diagnostic.setLogStream();
  }

  static final String EASY = ""
      + "# HEADER Line should be ignored" + StringUtils.LS
      + "0.5\t0" + StringUtils.LS
      + "0.5\t2" + StringUtils.LS
      ;
  public void testEasy() throws IOException {
    final SequencesReader reader = ReaderTestUtils.getReaderDnaMemory(ReaderTestUtils.fasta("AAAAA", "AA", "AAA"));
    final InputStream in = new ByteArrayInputStream(EASY.getBytes());
    final Map<String, Integer> taxonLookup = new HashMap<>();
    taxonLookup.put("0", 0);
    taxonLookup.put("1", 1);
    taxonLookup.put("2", 2);
    try (final MemoryPrintStream mps = new MemoryPrintStream()) {
      Diagnostic.setLogStream(mps.printStream());
      final TaxonomyDistribution dist = new TaxonomyDistribution(in, taxonLookup, reader, TaxonomyDistribution.DistributionType.DNA_FRACTION);
      assertTrue(Arrays.equals(new double[]{0.5, 0, 0.5}, dist.getDistribution()));
      assertFalse(mps.toString().contains("Input distribution sums to"));
    }
  }
  public void testHarder() throws IOException {
    final SequencesReader reader = ReaderTestUtils.getReaderDnaMemory(ReaderTestUtils.fasta("AAAAA", "AA", "AAA", "AA"));
    final InputStream in = new ByteArrayInputStream(EASY.getBytes());
    final Map<String, Integer> taxonLookup = new HashMap<>();
    taxonLookup.put("0", 0);
    taxonLookup.put("1", 1);
    taxonLookup.put("2", 2);
    taxonLookup.put("3", 2);
    final TaxonomyDistribution dist = new TaxonomyDistribution(in, taxonLookup, reader, TaxonomyDistribution.DistributionType.DNA_FRACTION);
    assertTrue(Arrays.equals(new double[]{0.5, 0, 0.3, 0.2}, dist.getDistribution()));

  }

  public void testParser() throws IOException {
    final Map<Integer, Double> expected = new HashMap<>();
    expected.put(0, 0.5);
    expected.put(2, 0.5);
    assertEquals(expected, TaxonomyDistribution.parseTaxonDistribution(new ByteArrayInputStream(EASY.getBytes())));
  }
  public void testParserMalformed() {
    final String malformedLine = "0.5\t0\tI_don'tbelong";
    final String malformed = ""
                               + malformedLine  + StringUtils.LS
                               + "0.5\t2" + StringUtils.LS
        ;
    try {
      TaxonomyDistribution.parseTaxonDistribution(new ByteArrayInputStream(malformed.getBytes()));
      fail();
    } catch (IOException e) {
      TestUtils.containsAll(e.getMessage(), malformedLine);
    }
  }
  public void testParserNotDouble() {
    final String malformedLine = "0.monkey5\t0";
    final String malformed = ""
                             + malformedLine  + StringUtils.LS
                             + "0.5\t2" + StringUtils.LS
        ;
    try {
      TaxonomyDistribution.parseTaxonDistribution(new ByteArrayInputStream(malformed.getBytes()));
      fail();
    } catch (IOException e) {
      TestUtils.containsAll(e.getMessage(), malformedLine);
    }
  }
  public void testParserNotIntegral() {
    final String malformedLine = "0.5\t1.3";
    final String malformed = ""
                             + malformedLine  + StringUtils.LS
                             + "0.5\t2" + StringUtils.LS
        ;
    try {
      TaxonomyDistribution.parseTaxonDistribution(new ByteArrayInputStream(malformed.getBytes()));
      fail();
    } catch (IOException e) {
      TestUtils.containsAll(e.getMessage(), malformedLine);
    }
  }
  public void testParserDuplicateId() {
    final String malformedLine = "0.2\t2";
    final String malformed = ""
                             + "0.5\t2"  + StringUtils.LS
                             + malformedLine + StringUtils.LS
        ;
    try {
      TaxonomyDistribution.parseTaxonDistribution(new ByteArrayInputStream(malformed.getBytes()));
      fail();
    } catch (IOException e) {
      TestUtils.containsAll(e.getMessage(), malformedLine);
    }
  }

  public void testDoesntSum() throws IOException {
    final String malformed = ""
                             + "0.49\t0"  + StringUtils.LS
                             + "0.49\t2" + StringUtils.LS
        ;
    final SequencesReader reader = ReaderTestUtils.getReaderDnaMemory(ReaderTestUtils.fasta("AAAAA", "AA", "AAA"));
    final InputStream in = new ByteArrayInputStream(malformed.getBytes());
    final Map<String, Integer> taxonLookup = new HashMap<>();
    taxonLookup.put("0", 0);
    taxonLookup.put("1", 1);
    taxonLookup.put("2", 2);
    try (final MemoryPrintStream mps = new MemoryPrintStream()) {
      Diagnostic.setLogStream(mps.printStream());
      final TaxonomyDistribution dist = new TaxonomyDistribution(in, taxonLookup, reader, TaxonomyDistribution.DistributionType.DNA_FRACTION);
      assertTrue(Arrays.equals(new double[]{0.5, 0, 0.5}, dist.getDistribution()));
      TestUtils.containsAll(mps.toString(), "Input distribution sums to: 0.98");
    }
  }

  public void testAbundance() throws IOException {
    final SequencesReader reader = ReaderTestUtils.getReaderDnaMemory(ReaderTestUtils.fasta("AAAAAA", "AAA", "AA", "AAA"));
    final InputStream in = new ByteArrayInputStream(EASY.getBytes());
    final Map<String, Integer> taxonLookup = new HashMap<>();
    taxonLookup.put("0", 0);
    taxonLookup.put("1", 0);
    taxonLookup.put("2", 1);
    taxonLookup.put("3", 2);
    try (final MemoryPrintStream mps = new MemoryPrintStream()) {
      Diagnostic.setLogStream(mps.printStream());
      final TaxonomyDistribution dist = new TaxonomyDistribution(in, taxonLookup, reader, TaxonomyDistribution.DistributionType.ABUNDANCE);
      assertTrue(Arrays.toString(dist.getDistribution()), Arrays.equals(new double[] {0.5, 0.25, 0, 0.25}, dist.getDistribution()));
      assertFalse(mps.toString().contains("Input distribution sums to"));
    }
  }
}
