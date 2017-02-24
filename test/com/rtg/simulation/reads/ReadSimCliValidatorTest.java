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
import com.rtg.util.test.FileHelper;

/**
 */
public class ReadSimCliValidatorTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new ReadSimCli();
  }

  public void testCliValidator1() throws IOException, InvalidParamsException {
    Diagnostic.setLogStream();
    final File tempDir = FileUtils.createTempDir("readsimclitest", "checkcli");
    try {
      final File genomeDir = FileHelper.createTempDirectory();
      try {
        ReaderTestUtils.getReaderDNA(">seq1" + StringUtils.LS + "acgt", genomeDir, null).close();
        //final File xgenomeFile = new File(tempDir, "xgenome");
        final File reads = new File(tempDir, "reads");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-r", "36", "-n", "100"), "You must provide values for -t SDF --machine STRING");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-r", "36", "-n", "100", "--machine", "illumina_se"), "You must provide a value for -o SDF");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "-R", "20", "-L", "20", "-r", "36", "-n", "100", "--machine", "illumina_pe"), "The flag --read-length is not permitted for this set of arguments");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "-R", "20", "-L", "20", "-n", "100", "--machine", "illumina_se"), "The flag --read-length is required");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "-n", "100", "--machine", "illumina_pe"), "The flag --left-read-length is required");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-t", genomeDir.getPath(), "--left-read-length", "30", "-n", "100", "--machine", "illumina_pe"), "The flag --right-read-length is required");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-c", "0", "--machine", "illumina_se"), "Coverage should be positive");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-c", "1000001", "--machine", "illumina_se"), "Coverage cannot be greater than 1000000.0");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "0", "--machine", "illumina_se"), "Number of reads should be greater than 0");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "0", "--machine", "illumina_se"), "Read length is too small");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "10", "--machine", "illumina_se", "--max-fragment-size", "10", "--min-fragment-size", "11"), "--max-fragment-size should not be smaller than --min-fragment-size");

        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-r", "11", "--machine", "illumina_se", "--max-fragment-size", "10", "--min-fragment-size", "10"), "Read length is too large for selected fragment size");

      } finally {
        assertTrue(FileHelper.deleteAll(genomeDir));
      }

    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }
}
