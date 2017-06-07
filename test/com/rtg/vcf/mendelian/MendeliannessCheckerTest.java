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
package com.rtg.vcf.mendelian;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Test class
 */
public class MendeliannessCheckerTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new MendeliannessChecker();
  }

  public void testHelp() {
    checkHelp("rtg mendelian",
        "output only consistent calls",
        "output only non-Mendelian calls",
        "all records, regardless",
        "VCF file"
        );
  }

  static boolean isBadHaploidChild(final String fatherCall, final String motherCall, final String childCall) {
    return MendeliannessAnnotator.isBadHaploidChild(new Genotype(fatherCall), new Genotype(motherCall), new Genotype(childCall));
  }

  static boolean isBadDiploidChild(final String fatherCall, final String motherCall, final String childCall) {
    return MendeliannessAnnotator.isBadDiploidChild(new Genotype(fatherCall), new Genotype(motherCall), new Genotype(childCall));
  }

  public void testBadHaploidChild() {
    final String ref = "0";
    final String var = "1";
    final String missing = ".";
    final String missing2 = "./.";
    final String homref = "0/0";
    final String homvar = "1/1";
    final String hetvar = "0/1";

    assertFalse(isBadHaploidChild(ref, hetvar, var));  // E.g. Chr X
    assertFalse(isBadHaploidChild(homvar, ref, var));  // Just for symmetry
    assertFalse(isBadHaploidChild(hetvar, var, ref));  // Just for symmetry
    assertFalse(isBadHaploidChild(var, missing, var)); // E.g. Chr Y
    assertFalse(isBadHaploidChild(missing, var, var)); // Just for symmetry

    assertTrue(isBadHaploidChild(ref, homvar, ref));  // E.g. Chr X
    assertTrue(isBadHaploidChild(var, var, ref)); // E.g. Chr Y
    assertTrue(isBadHaploidChild(var, missing, ref));  // E.g. Chr Y

    // It would be acceptable to change the outcomes of these ones:
    assertTrue(isBadHaploidChild(homvar, homref, var));  // Edge case, both parents are diploid, but we prioritize mother
    assertTrue(isBadHaploidChild(homref, homref, var)); // Edge case, both parents are diploid
    assertTrue(isBadHaploidChild(homref, missing2, var)); // Edge case, both parents are diploid
  }

  public void testBadDiploidChild() {
    final String homref = "0/0";
    final String homvar = "1/1";
    final String hetvar = "0/1";
    assertFalse(isBadDiploidChild(hetvar, homref, hetvar));
    assertFalse(isBadDiploidChild(hetvar, hetvar, hetvar));
    assertFalse(isBadDiploidChild(hetvar, hetvar, homvar));

    assertTrue(isBadDiploidChild(homref, homref, hetvar));
    assertTrue(isBadDiploidChild(homref, homvar, homvar));
    assertTrue(isBadDiploidChild(homref, homvar, homref));
  }

  public void testOptions() throws IOException {
    try (TestDirectory dir = new TestDirectory("mendelianness")) {
      final File sdf = ReaderTestUtils.getDNADir(">chr21\nacgt", dir);
      final File file1 = FileHelper.resourceToFile("com/rtg/vcf/mendelian/resources/merge.vcf", new File(dir, "merge.vcf"));
      final File inconsistent = new File(dir, "failed.vcf.gz");
      final File consistent = new File(dir, "nonfailed.vcf.gz");
      final MainResult res = MainResult.run(getCli(), "-t", sdf.getPath(), "-i", file1.getPath(), "--all-records", "--output-inconsistent", inconsistent.getPath(), "--output-consistent", consistent.getPath());
      assertEquals(0, res.rc());
      final String s = res.out().replaceAll("Checking: [^\n]*\n", "Checking: \n");
      //System.err.println(s);
      mNano.check("mendelian1", s);
      final String s2 = TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(inconsistent));
      //System.err.println(s2);
      mNano.check("mendelian2", s2);
      final String s2b = TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(consistent));
      //System.err.println(s2);
      mNano.check("mendelian2b", s2b);
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(inconsistent));
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(consistent));

      final MainResult res2 = MainResult.run(getCli(), "-t", sdf.getPath(), "-i", file1.getPath());
      assertEquals(0, res2.rc());
      final String s3 = res2.out().replaceAll("Checking: [^\n]*\n", "Checking: \n");
      //System.err.println(s3);
      mNano.check("mendelian3", s3);
    }

  }

}
