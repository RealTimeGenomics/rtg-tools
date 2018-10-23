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

import static com.rtg.util.StringUtils.LS;

import java.io.File;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

/**
 * Test the corresponding class
 */
public class PedSampleSimulatorCliTest extends AbstractCliTest {

  private static final String FAMILY_PED = ""
    + "0\tfather\t0\t0\t1\t0" + LS
    + "0\tmother\t0\t0\t2\t0" + LS
    + "0\tchild\tfather\tmother\t1\t0" + LS
    + "0\toddchild\tfather\t0\t1\t0" + LS
    + "0\tsingleton\t0\t0\t2\t0" + LS
    ;

  private static final String SMALL_VCF = VcfHeader.VERSION_LINE + '\n' + VcfHeader.HEADER_BASE + '\n'
    + "chr1\t4\t.\tT\tC\t.\tPASS\tAF=1.0\n"
    + "chr1\t8\t.\tT\tC\t.\tPASS\tAF=0.5\n"
    + "chr1\t30\t.\tT\tC\t.\tPASS\tAF=0.5\n"
    + "chr1\t49\t.\tT\tC\t.\tPASS\tAF=1.0\n"
    + "chr1\t55\t.\tT\tC\t.\tPASS\tAF=0.5\n"
    ;

  @Override
  protected AbstractCli getCli() {
    return new PedSampleSimulatorCli();
  }

  public void testInitParams() {
    checkHelp("reference", "reference genome",
        "-i", "input=", "input VCF",
        "-o", "output=", "output",
        "--pedigree", "--num-mutations", "--extra-crossovers",
        "print help on command-line flag usage",
        "seed=", "seed for the random number generator");
  }

  public void testValidator() throws Exception {
    try (TestDirectory tmpDir = new TestDirectory("pedsamplesim")) {
      final File pedFile = FileUtils.stringToFile(FAMILY_PED + "notvalid", new File(tmpDir, "family.ped"));
      final File inVcf = FileHelper.stringToGzFile(SMALL_VCF, new File(tmpDir, "input.vcf.gz"));

      final File refDir = new File(tmpDir, "ref");
      final File outDir = new File(tmpDir, "out");
      String err = checkHandleFlagsErr("-o", outDir.getPath(), "-t", refDir.getPath(), "-p", pedFile.getPath(), "-i", inVcf.getPath());
      TestUtils.containsAllUnwrapped(err, "Error: The specified SDF, \"" + refDir.getPath() + "\", does not exist.");
      ReaderTestUtils.getDNADir(">chr1\nacgtacgtacgtacgtacgt", refDir);

      err = checkHandleFlagsErr("-o", outDir.getPath(), "-t", refDir.getPath(), "-p", pedFile.getPath(), "-i", inVcf.getPath());
      TestUtils.containsAllUnwrapped(err, "Error: Index not found for file");
      new TabixIndexer(inVcf).saveVcfIndex();

      err = checkHandleFlagsErr("-o", outDir.getPath(), "-t", refDir.getPath(), "-p", pedFile.getPath(), "-i", inVcf.getPath(), "--extra-crossovers", "-1");
      TestUtils.containsAllUnwrapped(err, "extra-crossovers must be in the range");

      err = checkHandleFlagsErr("-o", outDir.getPath(), "-t", refDir.getPath(), "-p", pedFile.getPath(), "-i", inVcf.getPath(), "--num-mutations", "-1");
      TestUtils.containsAllUnwrapped(err, "num-mutations must be at least");

      err = checkMainInitBadFlags("-o", outDir.getPath(), "-t", refDir.getPath(), "-p", pedFile.getPath(), "-i", inVcf.getPath());
      TestUtils.containsAllUnwrapped(err, "PED line:", "notvalid");
    }
  }

  public void testFunction() throws Exception {
    try (TestDirectory tmpDir = new TestDirectory("pedsamplesim")) {
      final File pedFile = FileUtils.stringToFile(FAMILY_PED, new File(tmpDir, "family.ped"));
      final File inVcf = FileHelper.stringToGzFile(SMALL_VCF, new File(tmpDir, "input.vcf.gz"));
      new TabixIndexer(inVcf).saveVcfIndex();

      final File refDir = new File(tmpDir, "ref");
      ReaderTestUtils.getDNADir(">chr1\nacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgt\n"
        + ">chr2\nacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgtacgt\n", refDir);

      final File outDir = new File(tmpDir, "out");
      String err = checkMainInitWarn("-o", outDir.getPath(), "-t", refDir.getPath(), "-p", pedFile.getPath(), "-i", inVcf.getPath(),
        "--num-mutations", "2", "--extra-crossovers", "0.3", "--seed", "32", "--output-sdf", "--remove-unused");

      TestUtils.containsAllUnwrapped(err, "is a child of non-complete family");
      for (String sample : new String[] {"father", "mother", "child", "oddchild", "singleton"}) {
        assertTrue(new File(outDir, sample + ".sdf").exists());
      }
      mNano.check("pedsamplesim-summary.txt", FileUtils.fileToString(new File(outDir, "summary.txt")));
      mNano.check("pedsamplesim.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(new File(outDir, "pedsamplesim.vcf.gz"))));
    }
  }

}
