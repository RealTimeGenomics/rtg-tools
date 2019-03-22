/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.simulation.variants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.rtg.AbstractTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.Sex;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class CrossoverSelectorTest extends AbstractTest {

  private static final String REF =
      ">ref1" + StringUtils.LS
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca" + StringUtils.LS
      + ">ref2" + StringUtils.LS
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca" + StringUtils.LS
      + ">ref3" + StringUtils.LS
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac" + StringUtils.LS
      + ">ref4" + StringUtils.LS
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac" + StringUtils.LS;

  private static final String REFTXT =
      "# Simulated reference" + StringUtils.LS
      + "version 1" + StringUtils.LS
      + "either  def     diploid linear" + StringUtils.LS
      + "# ref1 ~= ChrX, ref2 ~= ChrY" + StringUtils.LS
      + "male    seq     ref1    haploid linear  ref2" + StringUtils.LS
      + "male    seq     ref2    haploid linear  ref1" + StringUtils.LS
      + "female  seq     ref1    diploid linear" + StringUtils.LS
      + "female  seq     ref2    none    linear" + StringUtils.LS
      + "either  seq     ref3    polyploid       circular" + StringUtils.LS;

  private static final String REF2MAPTXT =
    CrossoverSelector.FileGeneticMap.GENETIC_MAP_HEADER + StringUtils.LS
      + "ref2\t50\t0\t0" + StringUtils.LS
      + "ref2\t60\t1.0\t1.0" + StringUtils.LS
      + "ref2\t61\t0.0\t1.0" + StringUtils.LS
      + "ref2\t119\t0.0\t1.0" + StringUtils.LS;


  public void testSelection() throws IOException {
    try (final TestDirectory dir = new TestDirectory("crossoverselector")) {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.getDNADir(REF, sdf);
      final SequencesReader sr = SequencesReaderFactory.createMemorySequencesReader(sdf, true, LongRange.NONE);
      FileUtils.stringToFile(REFTXT, new File(sdf, ReferenceGenome.REFERENCE_FILE));
      ReferenceGenome g = new ReferenceGenome(sr, Sex.FEMALE);
      FileUtils.stringToFile(REF2MAPTXT, new File(dir, CrossoverSelector.mapName(g.sequence("ref2"), Sex.FEMALE)));

      // Generate variants
      final int seed = 103;
      PortableRandom random = new PortableRandom(seed);
      CrossoverSelector cs = new CrossoverSelector(dir, 1.0, true);

      CrossoverSelector.GeneticMap m = cs.getGeneticMap(g.sequence("ref1"), Sex.FEMALE);
      assertEquals("Uniform:120", m.toString());
      m = cs.getGeneticMap(g.sequence("ref2"), Sex.FEMALE);
      assertEquals("Map:female.ref2.CDF.txt", m.toString());

      int[] positions;
      for (int i = 0; i < 50; i++) {
        positions = cs.getCrossoverPositions(random, g.sequence("ref1"), Sex.FEMALE);
        assertEquals(2, positions.length);
        for (int pos : positions) {
          //System.err.println("Uniform: " + pos);
          assertTrue(pos >= 0 && pos < 120);
        }
      }
      final ArrayList<Integer> sample = new ArrayList<>();
      for (int i = 0; i < 50; i++) {
        positions = cs.getCrossoverPositions(random, g.sequence("ref2"), Sex.FEMALE);
        assertEquals(2, positions.length);
        for (int pos : positions) {
          //System.err.println("Map: " + pos);
          sample.add(pos);
          assertTrue(pos >= 50 && pos < 60);
        }
      }
      assertEquals("[58, 59, 55, 58, 52, 56, 54, 58, 58, 58, 53, 59, 50, 55, 52, 54, 58, 59, 53, 54, 50, 53, 56, 57, 59, 59, 51, 54, 52, 57, 51, 51, 54, 56, 50, 50, 50, 59, 51, 54, 56, 58, 55, 59, 53, 54, 55, 59, 51, 56, 53, 54, 51, 54, 54, 57, 52, 53, 56, 58, 53, 58, 57, 58, 50, 52, 50, 51, 50, 54, 53, 58, 57, 58, 52, 53, 51, 54, 50, 52, 56, 57, 57, 59, 50, 59, 54, 57, 51, 52, 57, 59, 51, 57, 51, 59, 56, 57, 53, 59]", sample.toString());

    }
  }
}
