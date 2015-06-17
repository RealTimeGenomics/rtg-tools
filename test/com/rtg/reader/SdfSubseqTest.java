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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.reader.FastqSequenceDataSource.FastQScoreType;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

/**
 *
 *
 */
public class SdfSubseqTest extends AbstractCliTest {


  @Override
  protected AbstractCli getCli() {
    return new SdfSubseq();
  }

  private InputStream createStream(final String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  private static final String EX2 = "@TEST"
    + StringUtils.LS
    + "acatgctgtacgtcgagtcagtcatgcagtcagtcatgcagtcagtcagtcatgcagtcagtcatgcagtcagtcagtcagtcagtcgcatgca"
    + StringUtils.LS
    + "+"
    + StringUtils.LS
    + "ACATGCTGTACGTCGAGTCAGTCATGCAGTCAGTCATGCAGTCAGTCAGTCATGCAGTCAGTCATGCAGTCAGTCAGTCAGTCAGTCGCATGCA"
    + StringUtils.LS
    + "@TEst2 extra information"
    + StringUtils.LS
    + "tgactgcatgcatgcatgcatgcatgcatgcatgcatgcagtcagtcgtcgtactgcatgcatgcagtcagtcagtcatgcagtcgctgctagtcgtc"
    + StringUtils.LS
    + "+"
    + StringUtils.LS
    + "TGACTGCATGCATGCATGCATGCATGCATGCATGCATGCAGTCAGTCGTCGTACTGCATGCATGCAGTCAGTCAGTCATGCAGTCGCTGCTAGTCGTC"
    + StringUtils.LS;

  private static final String PROT = ">TEST"
    + StringUtils.LS
    + "acatgctgtacgtcgagtcagtcatgcagtcagtcatgcagtcagtcagtcatgcagtcagtcatgcagtcagtcagtcagtcagtcgcatgca"
    + StringUtils.LS
    + ">TEst2"
    + StringUtils.LS
    + "tgactgcatgcatgcatgcatgcatgcatgcatgcatgcagtcagtcgtcgtactgcatgcatgcagtcagtcagtcatgcagtcgctgctagtcgtc"
    + StringUtils.LS;

  public void testInitFlags() {
    checkHelp("rtg sdfsubseq"
            , "Prints a subsequence of a given sequence in an SDF."
            , "-i,", "--input=SDF", "input SDF"
            , "the range to display. The format is one of <sequence_name>, <sequence_name>:start-end or <sequence_name>:start+length"
            , "-r,", "--reverse-complement", "if set, output in reverse complement"
            , "-f,", "--fasta", "if set, output in FASTA format"
            , "-q,", "--fastq", "if set, output in FASTQ format"
            , "-I,", "--sequence-id", "if set, use sequence id instead of sequence name in region (0-based)"
            );
  }

  public void testErrorMessages() throws IOException {
    String err;
    ArrayList<InputStream> al;
    FastaSequenceDataSource ds;
    SequencesWriter sw;
    final File normal = FileUtils.createTempDir("normal", "SDFTest");
    try {
      al = new ArrayList<>();
      al.add(createStream(EX2));
      ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      sw = new SequencesWriter(ds, normal, 20, PrereadType.UNKNOWN, false);
      sw.processSequences();

      err = checkHandleFlagsErr("-i", normal.toString(), "TEST:-5+6");
      TestUtils.containsAll(err, "The region value \"TEST:-5+6\" is malformed.");

      err = checkHandleFlagsErr("-i", normal.toString(), "TEST:1+1", "-f", "-q");
      TestUtils.containsAll(err, "Only one of --fasta or --fastq");

      err = checkHandleFlagsErr("-i", normal.toString(), "TEST:4+2", "--Xpreserve-coordinates", "--reverse-complement");
      TestUtils.containsAll(err, "Only one of --Xpreserve-coordinates or --reverse-complement");

      err = checkHandleFlagsErr("-i", normal.toString(), "-I", "TEST:1+1");
      TestUtils.containsAll(err.replaceAll("\\s+", " "), "When --sequence-id is set the <sequence_name> of the region \"TEST:1+1\" must be an integer greater than or equal to 0.");

      err = checkMainInitBadFlags("-i", normal.toString(), "TEST:1-60000");
      TestUtils.containsAll(err, "Supplied end position \"60000\" reads past sequence end");

      err = checkMainInitBadFlags("-i", normal.toString(), "TEST:60000+1");
      TestUtils.containsAll(err, "Supplied start position \"60000\" reads past sequence end");

      err = checkMainInitBadFlags("-i", normal.toString(), "BLARG:1+1");
      TestUtils.containsAll(err, "The sequence \"BLARG\" could not be found.");

      err = checkMainInitBadFlags("-i", normal.toString(), "-I", "2:1+1");
      TestUtils.containsAll(err, "The sequence id 2 is out of range, must be from 0 to 1.");
    } finally {
      assertTrue(FileHelper.deleteAll(normal));
    }
    final File noQualName = FileUtils.createTempDir("noQualityName", "SDFTest");
    try {
      al = new ArrayList<>();
      al.add(createStream(EX2));
      ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      sw = new SequencesWriter(ds, noQualName, 20, PrereadType.UNKNOWN, false);
      sw.processSequences(false, false);

      err = checkMainInitBadFlags("-i", noQualName.toString(), "TEST:1+1");
      TestUtils.containsAll(err, "The input SDF does not have name data.");

      err = checkMainInitBadFlags("-i", noQualName.toString(), "-I", "0:1+1", "-q");
      TestUtils.containsAll(err, "The input SDF does not have quality data.");
    } finally {
      assertTrue(FileHelper.deleteAll(noQualName));
    }
    final File protein = FileUtils.createTempDir("protein", "SDFTest");
    try {
      al = new ArrayList<>();
      al.add(createStream(PROT));
      ds = new FastaSequenceDataSource(al, new ProteinFastaSymbolTable());
      sw = new SequencesWriter(ds, protein, 20, PrereadType.UNKNOWN, false);
      sw.processSequences();

      err = checkMainInitBadFlags("-i", protein.toString(), "TEST:1+1", "-r");
      TestUtils.containsAll(err, "Reverse complement cannot be used with protein SDFs.");
    } finally {
      assertTrue(FileHelper.deleteAll(protein));
    }
  }

  public void testNormalOperation() throws IOException {
    String out;
    ArrayList<InputStream> al;
    FastaSequenceDataSource ds;
    SequencesWriter sw;
    final File normal = FileUtils.createTempDir("normal", "SDFTest");
    try {
      al = new ArrayList<>();
      al.add(createStream(EX2));
      ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      sw = new SequencesWriter(ds, normal, 20, PrereadType.UNKNOWN, false);
      sw.processSequences();
      out = checkMainInitOk("-i", normal.toString(), "TEST:1+5");
      assertEquals("ACATG" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEST:90+5");
      assertEquals("ATGCA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEST");
      assertEquals("ACATGCTGTACGTCGAGTCAGTCATGCAGTCAGTCATGCAGTCAGTCAGTCATGCAGTCAGTCATGCAGTCAGTCAGTCAGTCAGTCGCATGCA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEST:50");
      assertEquals("TCATGCAGTCAGTCATGCAGTCAGTCAGTCAGTCAGTCGCATGCA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEst2:1+6", "-r");
      assertEquals("CAGTCA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEst2:91+8", "-r");
      assertEquals("GACGACTA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEST:1+5", "-q");
      assertEquals("@TEST[1,5]" + StringUtils.LS + "ACATG" + StringUtils.LS + "+" + StringUtils.LS + "ACATG" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "-I", "0:90+5", "-q");
      assertEquals("@TEST[90,94]" + StringUtils.LS + "ATGCA" + StringUtils.LS + "+" + StringUtils.LS + "ATGCA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEst2:1+6", "-r", "-q");
      assertEquals("@TEst2[1,6]-rc" + StringUtils.LS + "CAGTCA" + StringUtils.LS + "+" + StringUtils.LS + "GTCAGT" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEst2:91+8", "-r", "-q");
      assertEquals("@TEst2[91,98]-rc" + StringUtils.LS + "GACGACTA" + StringUtils.LS + "+" + StringUtils.LS + "CTGCTGAT" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEst2:91+8", "-r", "-f");
      assertEquals(">TEst2[91,98]-rc" + StringUtils.LS + "GACGACTA" + StringUtils.LS, out);

      out = checkMainInitOk("-i", normal.toString(), "TEst2:91+8", "--Xpreserve-coordinates", "-f");
      assertEquals(">TEst2 extra information" + StringUtils.LS + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNTAGTCGTC" + StringUtils.LS, out);

    } finally {
      assertTrue(FileHelper.deleteAll(normal));
    }
    final File fasta = FileUtils.createTempDir("fasta", "SDFTest");
    try {
      al = new ArrayList<>();
      al.add(createStream(PROT));
      ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
      sw = new SequencesWriter(ds, fasta, 20, PrereadType.UNKNOWN, false);
      sw.processSequences();

      out = checkMainInitOk("-i", fasta.toString(), "TEST:1+5");
      assertEquals("ACATG" + StringUtils.LS, out);
    } finally {
      assertTrue(FileHelper.deleteAll(fasta));
    }
  }

  public void testPairedSdf() throws Exception {
    final File tempDir = FileUtils.createTempDir("readsimclitest", "checkcli");
    try {
      final String left = ">seq1" + StringUtils.LS + "acgt" + StringUtils.LS + ">seq2" + StringUtils.LS + "cgta";
      final String right = ">seq1" + StringUtils.LS + "tgca" + StringUtils.LS + ">seq2" + StringUtils.LS + "atgc";


      final File readLeftDir = new File(tempDir, "left");
      ReaderTestUtils.getReaderDNA(left, readLeftDir, null).close();
      final File readRightDir = new File(tempDir, "right");
      ReaderTestUtils.getReaderDNA(right, readRightDir, null).close();

      assertTrue(checkHandleFlagsErr("-i", tempDir.getPath(), "1").contains("Paired-end SDF not supported"));
    } finally {
      FileHelper.deleteAll(tempDir);
    }
  }
}
