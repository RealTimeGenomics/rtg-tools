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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfStatistics;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;

/**
 */
public class ReadSimCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new ReadSimCli();
  }

  public void testHelp() {
    checkHelp("Generates reads from a reference genome",
      "File Input/Output",
      "Fragment Generation",
      "Illumina PE",
      "Illumina SE",
      "454 SE/PE",
      "Utility",
      "--machine=STRING", "select the sequencing technology to model. Allowed values are [illumina_se, illumina_pe,",
      "complete_genomics, complete_genomics_2, 454_pe, 454_se, iontorrent]",
      "--output=SDF", "name for reads output SDF",
      "--input=SDF", "SDF containing input genome",
      "--coverage=FLOAT", "coverage, must be positive",
      "--num-reads=INT", "number of reads to be generated",
      "--max-fragment-size=INT", "maximum fragment size (Default is 250)",
      "--min-fragment-size=INT", "minimum fragment size (Default is 200)",
      "--allow-unknowns", "allow reads to be drawn from template fragments containing unknown nucleotides",
      "--distribution=FILE", "file containing probability distribution for sequence selection",
      "--seed", "seed for random number generator",
      "--comment", "comment to include in the generated SDF",
      "--read-length=INT", "target read length, must be positive",
      "--left-read-length=INT", "target read length on the left side",
      "--right-read-length=INT", "target read length on the right side",
      "--454-max-total-size=INT", "maximum 454 read length (in paired end case the",
      "--454-min-total-size=INT", "minimum 454 read length (in paired end case the", "sum of the left and the right read lengths)",
      "--ion-max-total-size=INT", "maximum IonTorrent read length",
      "--ion-min-total-size=INT", "minimum IonTorrent read length",
      "",
      "--n-rate", "rate that the machine will generate new unknowns in the read"
    );
    checkExtendedHelp(
      "--Xmachine-errors=STRING", "selects the sequencer machine error settings. One of [default, illumina, ls454_se, ls454_pe, complete, completegenomics, iontorrent]"
        );
  }

  public void testExecReads() throws Exception {
    try (final TestDirectory tmpDir = new TestDirectory("readsimclitest")) {
      final File genomeDir = new File(tmpDir, "genome");
      final File outDir = new File(tmpDir, "out");

      String outstr = checkMainInitBadFlags("-t", genomeDir.getPath(), "--machine=illumina_se", "-o", outDir.getPath(), "-n", "2", "--read-length", "2", "-M", "10", "-m", "5", "--seed", "17");
      TestUtils.containsAll(outstr, "The specified SDF, ", "does not exist");

      ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "acgtgtcactacgacgtacgtactgatgcacgactactagctagtcgac", genomeDir, null).close();

      outstr = checkMainInitOk("-t", genomeDir.getPath(), "--machine=illumina_se", "-o", outDir.getPath(), "-n", "2", "--read-length", "2", "-M", "10", "-m", "5", "--seed", "17");
      assertEquals("Generated 2 reads, effective coverage 0.08" + StringUtils.LS
          + "Total action count:\t4" + StringUtils.LS
          + "Match count:\t4\t100.00%" + StringUtils.LS + StringUtils.LS, outstr);
    }
  }

  public void testExecCoverage() throws Exception {
    try (final TestDirectory tmpDir = new TestDirectory("readsimclitest")) {
      final File genomeDir = new File(tmpDir, "genome");
      final File outDir = new File(tmpDir, "out");

      ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "acgtgtcactacgacgtacgtactgatgcacgactactagctagtcgac" + StringUtils.LS
          + ">t2" + StringUtils.LS + "acgtgtcactacgacgtacgtactgatgcacgactactagctagtcgac", genomeDir, null).close();

      final String outstr = checkMainInitOk("-t", genomeDir.getPath(), "--machine=illumina_se", "-o", outDir.getPath(), "--coverage", "10", "--read-length", "2", "-M", "10", "-m", "5", "--seed", "17");
      assertEquals("Generated 490 reads, effective coverage 10.00" + StringUtils.LS
          + "Total action count:\t980" + StringUtils.LS
          + "Match count:\t968\t98.78%" + StringUtils.LS
          + "Mismatch count:\t12\t1.22%" + StringUtils.LS
          + "Total error count:\t12\t1.22%" + StringUtils.LS + StringUtils.LS, outstr);

      TestUtils.containsAll(FileUtils.fileToString(new File(outDir, "readsim.log")),
          "ReadSimParams",
          "input=" + genomeDir.getPath(),
          "machine=illumina_se",
          "output=" + outDir.getPath(),
          "coverage=10",
          "allow-unknowns=" + false,
          "max-fragment=10",
          "min-fragment=5",
          "seed=17"
      );

      try (final MemoryPrintStream mps = new MemoryPrintStream()) {
        SdfStatistics.performStatistics(SequencesReaderFactory.createDefaultSequencesReader(outDir), outDir, mps.printStream(), true, false, false);

        TestUtils.containsAll(mps.toString(),
          "Maximum length     : 2",
          "Minimum length     : 2",
          "N                  : 0",
          "A                  : 236",
          "C                  : 243",
          "G                  : 249",
          "T                  : 252",
          "Total residues     : 980"
                              );
      }
    }
  }
  public void testExecCoverageNs() throws Exception {
    try (final TestDirectory tmpDir = new TestDirectory("readsimclitest")) {
      final File genomeDir = new File(tmpDir, "genome");
      final File outDir = new File(tmpDir, "out");

      ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "acgtgtcactacgacgtacgtactgatgcacgactactagctagtcgac" + StringUtils.LS
          + ">t2" + StringUtils.LS + "acgtgtcactacgacgtacgtactgatgcacgactactagctagtcgac", genomeDir, null).close();

      final String outstr = checkMainInitOk("-t", genomeDir.getPath(), "--machine=illumina_se", "-o", outDir.getPath(), "--coverage", "10", "--read-length", "2", "-M", "10", "-m", "5", "--seed", "17", "--n-rate", "0.2");
      assertEquals("Generated 490 reads, effective coverage 10.00" + StringUtils.LS
          + "Total action count:\t980" + StringUtils.LS
          + "Match count:\t968\t98.78%" + StringUtils.LS
          + "Mismatch count:\t12\t1.22%" + StringUtils.LS
          + "Total error count:\t12\t1.22%" + StringUtils.LS + StringUtils.LS, outstr);

      TestUtils.containsAll(FileUtils.fileToString(new File(outDir, "readsim.log")),
          "ReadSimParams",
          "input=" + genomeDir.getPath(),
          "machine=illumina_se",
          "output=" + outDir.getPath(),
          "coverage=10",
          "allow-unknowns=" + false,
          "max-fragment=10",
          "min-fragment=5",
          "seed=17"
      );

      try (final MemoryPrintStream mps = new MemoryPrintStream()) {
        SdfStatistics.performStatistics(SequencesReaderFactory.createDefaultSequencesReader(outDir),
            outDir, mps.printStream(), true, false, false);
        TestUtils.containsAll(mps.toString(), "Total residues     : 980");
        final String res = StringUtils.grep(mps.toString(), "^N *:");
        final String count = res.replaceAll("^N *: ", "").replaceAll(StringUtils.LS, "");
        assertTrue(Integer.parseInt(count) > 100);
      }
    }
  }

}
