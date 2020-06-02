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
import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.machine.MachineType;
import com.rtg.util.test.FileHelper;

/**
 * Test class
 */
public class CgSimCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new CgSimCli();
  }

  public void testFlags() {
    checkHelp("Simulate Complete Genomics Inc sequencing reads.");
  }


  public void testCliValidator1() throws IOException, InvalidParamsException {

    try (final TestDirectory tempDir = new TestDirectory("cgsimclitest")) {
      final File genomeDir = FileHelper.createTempDirectory();
      try {
        ReaderTestUtils.getReaderDNA(">seq1" + StringUtils.LS + "acgt", genomeDir, null).close();
        final File reads = new File(tempDir, "reads");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", reads.getPath(), "-n", "100"), "Usage: rtg cgsim [OPTION]... -V INT -t SDF -o SDF -c FLOAT", "You must provide values for -V INT -t SDF");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-n", "100"), "You must provide values for -V INT -o SDF");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-c", "0", "-V", "1"), "--coverage must be greater than 0.0");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "0", "-V", "1"), "--num-reads must be at least 1");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "--cg-read-version", "0"), "Version must be 1 or 2");
        TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "--cg-read-version", "3"), "Version must be 1 or 2");

        final CgSimCli cli = (CgSimCli) getCli();
        MainResult.run(cli, "-t", genomeDir.getPath(), "-o", reads.getPath(), "-n", "1", "-V", "1");
        assertEquals(MachineType.COMPLETE_GENOMICS, cli.getMachineType());
      } finally {
        assertTrue(FileHelper.deleteAll(genomeDir));
      }
    }
  }
}
