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

package com.rtg.reference;

import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.StringUtils.TAB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome.DefaultFallback;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.RandomDna;

import junit.framework.TestCase;

/**
 */
public class SexMemoTest extends TestCase {


  public void testDiploidDefault() throws IOException {
    Diagnostic.setLogStream();
    try (final TestDirectory genomeDir = new TestDirectory("sexmemo")) {
      ;
      try (SequencesReader reader = ReaderTestUtils.getReaderDNA(">t\nacgt", genomeDir, null)) {
        final SexMemo sx = new SexMemo(reader, DefaultFallback.DIPLOID);
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.EITHER, "t"));
        assertEquals(Ploidy.NONE, sx.getEffectivePloidy(Sex.EITHER, "unknown"));
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.MALE, "t"));
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.FEMALE, "t"));
        assertFalse(sx.isAutosome("t"));
      }
    }
  }

  public void testHaploidDefault() throws IOException {
    Diagnostic.setLogStream();
    try (final TestDirectory genomeDir = new TestDirectory("sexmemo")) {
      try (SequencesReader reader = ReaderTestUtils.getReaderDNA(">t\nacgt", genomeDir, null)) {
        final SexMemo sx = new SexMemo(reader, DefaultFallback.HAPLOID);
        assertEquals(Ploidy.HAPLOID, sx.getEffectivePloidy(Sex.EITHER, "t"));
        assertEquals(Ploidy.NONE, sx.getEffectivePloidy(Sex.EITHER, "unknown"));
        assertEquals(Ploidy.HAPLOID, sx.getEffectivePloidy(Sex.MALE, "t"));
        assertEquals(Ploidy.HAPLOID, sx.getEffectivePloidy(Sex.FEMALE, "t"));
        assertFalse(sx.isAutosome("t"));
      }
    }
  }

  public void testRef() throws IOException {
    Diagnostic.setLogStream();
    try (final TestDirectory tempDir = new TestDirectory("sexmemo")) {
      final File genomeDir = ReaderTestUtils.getDNADir(">t\nacgt", tempDir, false, true, true);
      try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(genomeDir)) {
        final SexMemo sx = new SexMemo(reader, DefaultFallback.HAPLOID);
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.EITHER, "t"));
        assertEquals(Ploidy.NONE, sx.getEffectivePloidy(Sex.EITHER, "unknown"));
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.MALE, "t"));
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.FEMALE, "t"));
        assertFalse(sx.isAutosome("t"));
      }
    }
  }

  public void testVariousPloids() throws IOException {
    //Diagnostic.setLogStream();
    try (final TestDirectory tempDir = new TestDirectory("sexmemo")) {
      final File genomeDir = ReaderTestUtils.getDNADir(">s1\n" + RandomDna.random(4000) + "\n>s2\n" + RandomDna.random(5000) + "\n>s3\nacgt\n>s4\nacgt\n", tempDir, false, true, true);
      final File refFile = new File(genomeDir, ReferenceGenome.REFERENCE_FILE);
      try (FileOutputStream fo = new FileOutputStream(refFile, true)) {
        fo.write(("female" + TAB + "seq" + TAB + "s1" + TAB + "diploid" + TAB + "circular" + LS
            + "female" + TAB + "seq" + TAB + "s2" + TAB + "none" + TAB + "circular" + LS
            + "male" + TAB + "seq" + TAB + "s1" + TAB + "haploid" + TAB + "circular" + TAB + "s2" + LS
            + "male" + TAB + "seq" + TAB + "s2" + TAB + "haploid" + TAB + "circular" + TAB + "s1" + LS
            + "male" + TAB + "dup" + TAB + "s1:1000-3000" + TAB + "s2:2000-4000" + LS
            + LS
          + "either" + TAB + "seq" + TAB + "s3" + TAB + "polyploid" + TAB + "circular" + LS
          + "either" + TAB + "seq" + TAB + "s4" + TAB + "diploid" + TAB + "linear" + LS
            + LS).getBytes());
      }
      try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(genomeDir)) {
        final SexMemo sx = new SexMemo(reader, DefaultFallback.HAPLOID);

        assertEquals(Ploidy.DIPLOID, sx.getRealPloidy(Sex.EITHER, "s1"));
        assertEquals(Ploidy.NONE, sx.getRealPloidy(Sex.EITHER, "unknown"));
        assertEquals(Ploidy.HAPLOID, sx.getRealPloidy(Sex.MALE, "s1"));
        assertEquals(Ploidy.DIPLOID, sx.getRealPloidy(Sex.FEMALE, "s1"));
        assertFalse(sx.isAutosome("s1"));
        assertEquals(Ploidy.HAPLOID, sx.getRealPloidy(Sex.MALE, "s2"));
        assertEquals(Ploidy.NONE, sx.getRealPloidy(Sex.FEMALE, "s2"));
        assertEquals(Ploidy.HAPLOID, sx.getRealPloidy(Sex.MALE, "s2"));
        assertFalse(sx.isAutosome("s2"));

        // Some PAR specific queries
        assertEquals(999, sx.getParBoundary(Sex.MALE, new SequenceNameLocusSimple("s1", 0, 2000)));
        assertEquals(3000, sx.getParBoundary(Sex.MALE, new SequenceNameLocusSimple("s1", 2000, 4000)));
        assertEquals(Ploidy.DIPLOID, sx.getRealPloidy(Sex.MALE, "s1", 1000));
        assertEquals(Ploidy.DIPLOID, sx.getEffectivePloidy(Sex.MALE, "s1", 1000));
        assertEquals(Ploidy.DIPLOID, sx.getRealPloidy(Sex.MALE, "s2", 2000));
        assertEquals(Ploidy.NONE, sx.getEffectivePloidy(Sex.MALE, "s2", 2000));

        assertEquals(Ploidy.POLYPLOID, sx.getRealPloidy(Sex.FEMALE, "s3"));
        assertEquals(Ploidy.HAPLOID, sx.getEffectivePloidy(Sex.FEMALE, "s3"));
        assertFalse(sx.isAutosome("s3"));

        assertEquals(Ploidy.DIPLOID, sx.getRealPloidy(Sex.FEMALE, "s4"));
        assertTrue(sx.isAutosome("s4"));
      }
    }
  }

}
