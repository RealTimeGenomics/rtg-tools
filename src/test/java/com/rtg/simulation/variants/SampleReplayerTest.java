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
package com.rtg.simulation.variants;

import java.io.File;
import java.io.IOException;

import com.rtg.mode.DnaUtils;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.BgzipFileHelper;

import junit.framework.TestCase;

/**
 */
public class SampleReplayerTest extends TestCase {


  private static final String REF = ">simulatedSequence1\n"
      + "GAGACTCGGATCCCCGCTTTTACCGTCTAAGCACTCAAGCTGGAGATTACCATACTTAGGCTCATGTAGCCACCCGCGCTCGTAAATTCTCGACATTCCGCAGTGGCAGCCCTATCGCCA"
      + "GATCAATCGTCGCTGTGGAACTAGACCCGCCCTACTGTTGTCGCTGACTCCTTACTCCCTCTCAACGTATTCATACAGGCCCTTTTCGAATAGCTGGCGTGCACGATTGGCGTGTACATG"
      + "CTGCCTTTGAGGCACAGTTTCGTAGGGTTTCGTAGCAATGGCTTTGTCTGAAGAGAAGGG\n";

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  // Play a deletion into one haplotype, and a snp into the overlapping area of the other haplotype
  public void testOverlappingVariants() throws IOException, UnindexableDataException {
    try (final TestDirectory dir = new TestDirectory("samplereplayer")) {
      final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
      final byte[] buffr = new byte[(int) sr.maxLength()];
      final int lenr = sr.read(0, buffr);
      final String sref = DnaUtils.bytesToSequenceIncCG(buffr, 0, lenr);

      final File invcf = new File(dir, "variants.vcf.gz");
      BgzipFileHelper.resourceToBgzipFile("com/rtg/simulation/variants/resources/samplereplayertest.vcf", invcf);
      new TabixIndexer(invcf).saveVcfIndex();
      // Generate SDF corresponding to the sample
      final File outsdf = new File(dir, "outsdf");
      final SampleReplayer vr = new SampleReplayer(sr);
      vr.replaySample(invcf, outsdf, "sm_mom");

      final SequencesReader srOut = SequencesReaderFactory.createMemorySequencesReader(outsdf, true, LongRange.NONE);
      final byte[] buff = new byte[(int) srOut.maxLength()];

      /*
      System.out.println("-- Ref --\n" + sref);
      System.out.println("-- Chromosomes for sample foo --");
      for (int i = 0; i < srOut.numberSequences(); ++i) {
        int len = srOut.read(i, buff);
        System.out.println("seq: " + srOut.name(i));
        System.out.println(DnaUtils.bytesToSequenceIncCG(buff, 0, len));
      }
     */

      assertEquals("GCGTGTACATGCTGC", sref.substring(229, 244));
      int len = srOut.read(0, buff);
      assertEquals("GCGTGAGCTC", DnaUtils.bytesToSequenceIncCG(buff, 0, len).substring(229, 239));
      len = srOut.read(1, buff);
      assertEquals("GCGTGTAAATGCTC", DnaUtils.bytesToSequenceIncCG(buff, 0, len).substring(229, 243));
    }
  }
}
