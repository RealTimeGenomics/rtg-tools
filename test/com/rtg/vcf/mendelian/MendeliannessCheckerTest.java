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

import static com.rtg.vcf.mendelian.MendeliannessAnnotator.Consistency.CONSISTENT;
import static com.rtg.vcf.mendelian.MendeliannessAnnotator.Consistency.INCOMPLETE_CONSISTENT;
import static com.rtg.vcf.mendelian.MendeliannessAnnotator.Consistency.INCOMPLETE_INCONSISTENT;
import static com.rtg.vcf.mendelian.MendeliannessAnnotator.Consistency.INCOMPLETE_UNKNOWN;
import static com.rtg.vcf.mendelian.MendeliannessAnnotator.Consistency.INCONSISTENT;

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

  static MendeliannessAnnotator.Consistency checkHaploidChild(final String fatherCall, final String motherCall, final String childCall) {
    return MendeliannessAnnotator.checkHaploidChild(new Genotype(fatherCall), new Genotype(motherCall), new Genotype(childCall));
  }

  static MendeliannessAnnotator.Consistency checkDiploidChild(final String fatherCall, final String motherCall, final String childCall) {
    return MendeliannessAnnotator.checkDiploidChild(new Genotype(fatherCall), new Genotype(motherCall), new Genotype(childCall));
  }

  public void testHaploidChild() {
    final String ref = "0";
    final String var = "1";
    final String missing = ".";
    final String missing2 = "./.";
    final String homref = "0/0";
    final String homvar = "1/1";
    final String hetvar = "0/1";
    final String missingref = "./0";
    final String missingvar = "./1";

    assertEquals(CONSISTENT, checkHaploidChild(ref, hetvar, var));  // E.g. Chr X

    assertEquals(CONSISTENT,            checkHaploidChild(var, missing, var)); // E.g. Chr Y
    assertEquals(INCOMPLETE_UNKNOWN,    checkHaploidChild(var, missing2, var)); // E.g. Chr X
    assertEquals(INCOMPLETE_CONSISTENT, checkHaploidChild(var, missingvar, var)); // E.g. Chr X

    assertEquals(INCOMPLETE_UNKNOWN, checkHaploidChild(missing, missing, var)); // E.g. Chr Y, missing father

    assertEquals(INCOMPLETE_UNKNOWN, checkHaploidChild(var, missing, missing)); // E.g. Chr Y, missing child
    assertEquals(INCOMPLETE_UNKNOWN, checkHaploidChild(ref, missingref, var)); // E.g. Chr X, partial mother

    assertEquals(INCONSISTENT, checkHaploidChild(ref, homvar, ref));   // E.g. Chr X
    assertEquals(INCONSISTENT, checkHaploidChild(var, var, ref));      // E.g. Chr Y
    assertEquals(INCONSISTENT, checkHaploidChild(var, missing, ref));  // E.g. Chr Y

    // Not valid ploidy combinations. It would be acceptable to change the outcomes of these ones:
    assertEquals(CONSISTENT, checkHaploidChild(homvar, ref, var));  // Mother and father switched.
    assertEquals(CONSISTENT, checkHaploidChild(hetvar, var, ref));  // Mother and father switched.
    assertEquals(INCONSISTENT, checkHaploidChild(missing, var, var)); // Mother and father switched.
    assertEquals(INCONSISTENT, checkHaploidChild(homref, homref, var)); // Both parents are diploid, neither have the allele
    assertEquals(INCONSISTENT, checkHaploidChild(homvar, homref, var)); // Both parents are diploid, but we expect inheritance from mother
    assertEquals(INCOMPLETE_UNKNOWN, checkHaploidChild(homref, missing2, var)); // Edge case, both parents are diploid
  }

  public void testDiploidChild() {
    final String ref = "0";
    final String var = "1";
    final String missing = ".";
    final String missing2 = "./.";
    final String missingref = "./0";
    final String missingvar = "./1";
    final String homref = "0/0";
    final String homvar = "1/1";
    final String hetvar = "0/1";
    final String hetalt = "2/1";

    // E.g. Daughter on X (Father haploid)
    assertEquals(CONSISTENT, checkDiploidChild(var, hetvar, hetvar));
    assertEquals(INCONSISTENT, checkDiploidChild(var, homvar, hetvar));
    assertEquals(INCONSISTENT, checkDiploidChild(ref, homref, hetvar));
    assertEquals(INCONSISTENT, checkDiploidChild(ref, homvar, homvar));
    assertEquals(CONSISTENT, checkDiploidChild(var, hetvar, homvar));

    // Both parents diploid
    assertEquals(CONSISTENT, checkDiploidChild(hetvar, homref, hetvar));
    assertEquals(CONSISTENT, checkDiploidChild(hetvar, hetvar, hetvar));
    assertEquals(CONSISTENT, checkDiploidChild(hetvar, hetvar, homvar));

    assertEquals(INCONSISTENT, checkDiploidChild(homref, homref, hetvar));
    assertEquals(INCONSISTENT, checkDiploidChild(homref, homvar, homvar));
    assertEquals(INCONSISTENT, checkDiploidChild(homref, homvar, homref));

    // Missing values, homozygous child
    assertEquals(INCOMPLETE_INCONSISTENT, checkDiploidChild(homref, missing2, homvar));
    assertEquals(INCOMPLETE_INCONSISTENT, checkDiploidChild(missing2, homref, homvar));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(homref, homref, missing2));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(homref, missing2, homref));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missing2, homref, homref));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missing2, missing2, homvar));
    assertEquals(INCOMPLETE_CONSISTENT, checkDiploidChild(missingvar, missingvar, homvar));


    // Missing values, heterozygous child
    assertEquals(INCOMPLETE_CONSISTENT, checkDiploidChild(missingref, missingvar, hetvar));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(homref, missing2, hetvar));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missing2, homref, hetvar));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missing2, missing2, homvar));
    assertEquals(INCOMPLETE_INCONSISTENT, checkDiploidChild(homref, missing2, hetalt));
    assertEquals(INCOMPLETE_INCONSISTENT, checkDiploidChild(missing2, homref, hetalt));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missing2, hetvar, hetalt));

    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missing2, hetvar, missingvar));
    assertEquals(INCOMPLETE_INCONSISTENT, checkDiploidChild(homref, homref, missingvar));
    assertEquals(INCOMPLETE_UNKNOWN, checkDiploidChild(missingvar, hetvar, missingvar));
  }

  public void testOptions() throws IOException {
    try (TestDirectory dir = new TestDirectory("mendelianness")) {
      final File sdf = ReaderTestUtils.getDNADir(">chr21\nacgt", dir);
      final File file1 = FileHelper.resourceToFile("com/rtg/vcf/mendelian/resources/merge.vcf", new File(dir, "merge.vcf"));
      final File inconsistent = new File(dir, "failed.vcf.gz");
      final File consistent = new File(dir, "nonfailed.vcf.gz");
      final File annot = new File(dir, "checked.vcf.gz");
      final MainResult res = MainResult.run(getCli(), "-t", sdf.getPath(), "-i", file1.getPath(), "--all-records", "--output", annot.getPath(), "--output-inconsistent", inconsistent.getPath(), "--output-consistent", consistent.getPath());
      assertEquals(res.err(), 0, res.rc());
      final String s = res.out().replaceAll("Checking: [^\n]*\n", "Checking: \n");
      mNano.check("mendelian.out.txt", s);
      mNano.check("mendelian.annotated.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(annot)));
      mNano.check("mendelian.inconsistent.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(inconsistent)));
      mNano.check("mendelian.consistent.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(consistent)));
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(inconsistent));
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(consistent));

      final MainResult res2 = MainResult.run(getCli(), "-t", sdf.getPath(), "-i", file1.getPath());
      assertEquals(0, res2.rc());
      final String s3 = res2.out().replaceAll("Checking: [^\n]*\n", "Checking: \n");
      //System.err.println(s3);
      mNano.check("mendelian2.out.txt", s3);
    }

  }

}
