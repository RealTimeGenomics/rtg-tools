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
package com.rtg.vcf;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 */
public class VcfAnnotatorCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new VcfAnnotatorCli();
  }

  public void testFlags() {
    checkHelp("rtg vcfannotate"
      , "Adds annotations to a VCF file"
      , "-i,", "--input=FILE", "VCF file containing variants"
      , "-o,", "--output=FILE", "output VCF file"
      , "--bed-ids=FILE", "add variant IDs"
      , "--bed-info=FILE", "add INFO annotations"
      , "--vcf-ids=FILE"
      , "--fill-an-ac", "add or update the AN and AC INFO fields"
      , "-Z,", "--no-gzip", "do not gzip the output"
      , "--no-index", "do not produce indexes for output files"
      , "-A", "--annotation=STRING", "add computed annotation to VCF records"
    );
    checkExtendedHelp("rtg vcfannotate"
      , "--Xstr=SDF", "annotate records with simple tandem repeat indicator based on given SDF"
    );
  }

  public void testValidator() throws IOException {
    try (final TestDirectory temp = new TestDirectory("validator")) {
      final File fake = new File(temp, "fake.vcf");
      assertTrue(fake.createNewFile());
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", "blahBed", "-i", "blahInput"), "file \"blahInput\" does not exist.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", "blahBed", "-i", temp.getPath()), "file \"" + temp.getPath() + "\" is a directory.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", "blahBed", "-i", fake.getPath()), "file \"blahBed\" does not exist.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", temp.getPath(), "-i", fake.getPath()), "file \"" + temp.getPath() + "\" is a directory.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-ids", "blahBed", "-i", fake.getPath()), "file \"blahBed\" does not exist.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-ids", temp.getPath(), "-i", fake.getPath()), "file \"" + temp.getPath() + "\" is a directory.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", fake.getPath(), "--bed-info", fake.getPath(), "-i", fake.getPath(), "-Z"), "The file \"" + fake.getPath() + "\" already exists. Please remove it first or choose a different file");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", "blahOutput", "--bed-ids", fake.getPath(), "--vcf-ids", fake.getPath(), "-i", fake.getPath(), "-Z"), "Cannot set both --bed-ids and --vcf-ids");
      checkHandleFlagsOut("-o", "blahOutput", "-i", fake.getPath());
    }
  }

  public void testNanoSmall() throws IOException {
    check("snpAnnotate_small.bed", "snpAnnotate_small", false);
  }

  public void testNanoSmallIds() throws IOException {
    check("snpAnnotate_small.bed", "snpAnnotate_small_ids", true);
  }

  public void testNanoVcfIds() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File inVcf = FileUtils.stringToFile(mNano.loadReference("snpAnnotate_small.vcf"), new File(dir, "input.vcf"));
      final File idVcf = FileUtils.stringToFile(mNano.loadReference("snpAnnotate_small_ids_vcf.vcf"), new File(dir, "id.vcf"));
      final File outFile = new File(dir, "output.vcf.gz");

      final String str = checkMainInitOk("-i", inVcf.getPath(), "--vcf-ids", idVcf.getPath(), "-o", outFile.getPath(), "--fill-an-ac", "--annotation", "NAA,ZY,PD");
      assertEquals("", str);
      assertTrue(outFile.isFile());
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(outFile));
      final String actual = StringUtils.grep(FileHelper.gzFileToString(outFile), "^[^#]").replaceAll("[\r\n]+", "\n");
      mNano.check("snpAnnotate_small_vcf_ids_exp.vcf", actual, false);
    }
  }

  private void check(String bed, String id, boolean ids) throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File inVcf = FileUtils.stringToFile(mNano.loadReference(id + ".vcf"), new File(dir, "input.vcf"));
      final File inBed = FileUtils.stringToFile(mNano.loadReference(bed), new File(dir, "input.bed"));
      final File outFile = new File(dir, "output.vcf");

      final String str;
      if (ids) {
        str = checkMainInitOk("-i", inVcf.getPath(), "--bed-ids", inBed.getPath(), "-o", outFile.getPath(), "-Z", "--fill-an-ac", "--annotation", "NAA,ZY", "--annotation", "PD");
      } else {
        str = checkMainInitOk("-i", inVcf.getPath(), "--bed-info", inBed.getPath(), "-o", outFile.getPath(), "-Z", "--fill-an-ac", "--annotation", "NAA,ZY,PD");
      }
      assertEquals("", str);
      assertTrue(outFile.isFile());
      final String actual = StringUtils.grep(FileUtils.fileToString(outFile), "^[^#]").replaceAll("[\r\n]+", "\n");
      mNano.check(id + "_exp.vcf", actual, false);
    }
  }
}
