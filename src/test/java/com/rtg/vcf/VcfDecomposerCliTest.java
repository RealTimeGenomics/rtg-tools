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
package com.rtg.vcf;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.Utils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Test the corresponding class.
 */
public class VcfDecomposerCliTest extends AbstractCliTest {

  private static final String RESOURCES = "com/rtg/vcf/resources/";

  @Override
  protected AbstractCli getCli() {
    return new VcfDecomposerCli();
  }

  public void testFlags() {
    checkHelp("rtg vcfdecompose", "Decomposes complex variants within a VCF file into smaller components"
      , "-i,", "--input=FILE", "VCF file containing variants"
      , "-o,", "--output=FILE", "output VCF file"
      , "-t", "--template=SDF"
      , "--no-gzip"
      , "--no-header"
    );
  }

  // Run a test where vcfdecompose is expected to complete normally
  private void runResourceTest(String inResourceLoc, String expResourceLoc, boolean useRef, String... extrArgs) throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File in = FileHelper.resourceToFile(inResourceLoc, new File(dir, new File(Resources.getResource(inResourceLoc).getFile()).getName()));
      final File out = new File(dir, "out.vcf.gz");
      String[] args = {
        "-i", in.getPath(), "-o", out.getPath()
      };
      if (useRef) {
        final File sdf = ReaderTestUtils.getDNASubDir(REF, dir);
        args = Utils.append(args, "-t", sdf.getPath());
      }
      args = Utils.append(args, extrArgs);
      final String output = checkMainInitOk(args);
      mNano.check(expResourceLoc + ".txt", output, true);

      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(out));

      final String o = StringUtils.grep(FileHelper.gzFileToString(out), "^[^#]").replaceAll("[\r\n]+", "\n");
      mNano.check(expResourceLoc, o, true);
    }
  }

  private static final String REF = ">1\n"
    //          1         2         3         4         5         6         7         8         9         0
    // 12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
    + "TGGGgaagcaaGGCaaAGTgcgcgcgcgcgTGTTCAATGtttgcgcgcttgcgcgtttGTGATGGAAGGGTGCTGGAAATGAGTGGTAGTGATGGCGGCACAACAGTGTGAATCT"
    + "ACTTAATCCCACTGAACTGTATGCTGAAAAATGGTTTAGACGGTGAATTTTAGGTTATGTATGTTTTACCACAATTTTTAAAAAGCTAGTGAAAAGCTGGTAAAAAGAAAGAAAA"
    + "GAGGCTTTTTTAAAAAGTTAAATATATAAAAAGAGCATCATCAGTCCAAAGTCCAGCAGTTGTCCCTCCTGGAATCCGTTGGCTTGCCTCCGGCATTTTTGGCCCTTGCCTTTTA"
    + "GGGTTGCCAGATTAAAAGACAGGATGCCCAGCTAGTTTGAATTTTAGATAAACAACGAATAATTTCGTAGCATAAATATGTCCCAAGCTTAGTTTGGGACATACTTATGCTAAAA"
    + "AACATTATTGGTTGTTTATCTGAGATTCAGAATTAAGCATTTTATATTTTATTTGCTGCCTCTGGCCACCCTACTCTCTTCCTAACACTCTCTCCCTCTCCCAGTTTTGTCCGCC"
    + "TTCCCTGCCTCCTCTTCTGGGGGAGTTAGATCGAGTTGTAACAAGAACATGCCACTGTCTCGCTGGCTGCAGCGTGTGGTCCCCTTACCAGAGGTAAAGAAGAGATGGATCTCCA"
    + "CTCATGTTGTAGACAGAATGTTTATGTCCTCTCCAAATGCTTATGTTGAAACCCTAACCCCTAATGTGATGGTATGTGGAGATGGGCCTTTGGTAGGTAATTACGGTTAGATGAG"
    + "GTCATGGGGTGGGGCCCTCATTATAGATCTGGTAAGAAAAGAGAGCATTGTCTCTGTGTCTCCCTCTCTCTCTCTCTCTCTCTCTCTCATTTCTCTCTATCTCATTTCTCTCTCT"
    + "CTCGCTATCTCATTTTTCTCTCTCTCTCTTTCTCTCCTCTGTCTTTTCCCACCAAGTGAgtgtctccctctAAGGTGGCT";

  public void testDecompose() throws IOException {
    runResourceTest(RESOURCES + "vcfdecompose_in.vcf", "vcfdecompose_out_ref.vcf", true);
    runResourceTest(RESOURCES + "vcfdecompose_in.vcf", "vcfdecompose_out_noref.vcf", false);
    runResourceTest(RESOURCES + "vcfdecompose_in.vcf", "vcfdecompose_out_mnps.vcf", true, "--break-mnps");
    runResourceTest(RESOURCES + "vcfdecompose_in.vcf", "vcfdecompose_out_indels.vcf", true, "--break-indels");
  }

  public void testSplode() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File in = FileHelper.resourceToFile(RESOURCES + "vcfdecompose_splode1.vcf", new File(dir, "in.vcf"));
      final File out = new File(dir, "out.vcf.gz");
      final String result = checkMainInitBadFlags("-i", in.getPath(), "-o", out.getPath());
      TestUtils.containsAll(result, "allele ID out of range");
    }
  }
}
