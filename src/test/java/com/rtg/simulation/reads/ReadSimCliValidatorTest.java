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
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class ReadSimCliValidatorTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new ReadSimCli();
  }

  public void testCliValidator1() throws IOException, InvalidParamsException {
    Diagnostic.setLogStream();
    try (final TestDirectory tempDir = new TestDirectory("readsimclitest")) {
      final File genomeDir = new File(tempDir, "genome.sdf");
      ReaderTestUtils.getReaderDNA(">seq1" + StringUtils.LS + "acgt", genomeDir, null).close();
      final String easy = ""
        + "# HEADER Line should be ignored" + StringUtils.LS
        + "0.5\t0" + StringUtils.LS
        + "0.5\t2" + StringUtils.LS;
      final File distFile = FileUtils.stringToFile(easy, new File(tempDir, "dist.txt"));
      //final File xgenomeFile = new File(tempDir, "xgenome");
      final File reads = new File(tempDir, "reads");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-r", "36", "-n", "100"), "You must provide values for -t SDF --machine STRING");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-r", "36", "-n", "100", "--machine", "illumina_se"), "You must provide a value for -o SDF");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "-R", "20", "-L", "20", "-r", "36", "-n", "100", "--machine", "illumina_pe"), "The flag --read-length is not permitted for this set of arguments");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "-R", "20", "-L", "20", "-n", "100", "--machine", "illumina_se"), "The flag --read-length is required");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "-n", "100", "--machine", "illumina_pe"), "The flag --left-read-length is required");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "--left-read-length", "30", "-n", "100", "--machine", "illumina_pe"), "The flag --right-read-length is required");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-c", "0", "--machine", "illumina_se"), "--coverage must be greater than 0.0");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "0", "--machine", "illumina_se"), "--num-reads must be at least 1");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "0", "--machine", "illumina_se"), "--read-length", " at least 2");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "10", "--machine", "illumina_se", "--max-fragment-size", "10", "--min-fragment-size", "11"), "--min-fragment-size cannot be greater than the value for --max-fragment-size");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "5", "--machine", "illumina_se", "--max-fragment-size", "10", "--min-fragment-size", "10", "--distribution", distFile.getPath()), "--abundance or --dna-fraction must be set");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "5", "--machine", "illumina_se", "--max-fragment-size", "10", "--min-fragment-size", "10", "--taxonomy-distribution", distFile.getPath()), "--abundance or --dna-fraction must be set");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "5", "--machine", "illumina_se", "--max-fragment-size", "10", "--min-fragment-size", "10", "--abundance", "--dna-fraction"), "Cannot set both --abundance and --dna-fraction");
    }
  }
}
