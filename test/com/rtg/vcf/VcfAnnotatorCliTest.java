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
import java.util.Arrays;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.annotation.DerivedAnnotations;

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
        );
    checkExtendedHelp("rtg vcfannotate"
        , "--Xderived-annotations=STRING", "derived fields to add to VCF file. Allowed values are " + Arrays.toString(DerivedAnnotations.values())
        );
  }

  public void testValidator() throws IOException {
    final File temp = FileUtils.createTempDir("validator", "test");
    try {
      final File fake = new File(temp, "fake.vcf");
      assertTrue(fake.createNewFile());
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", "blahBed", "-i", "blahInput").replaceAll("\\s+", " "), "Given file \"blahInput\" does not exist.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", "blahBed", "-i", temp.getPath()).replaceAll("\\s+", " "), "Given file \"" + temp.getPath() + "\" is a directory.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", "blahBed", "-i", fake.getPath()).replaceAll("\\s+", " "), "Given file \"blahBed\" does not exist.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-info", temp.getPath(), "-i", fake.getPath()).replaceAll("\\s+", " "), "Given file \"" + temp.getPath() + "\" is a directory.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-ids", "blahBed", "-i", fake.getPath()).replaceAll("\\s+", " "), "Given file \"blahBed\" does not exist.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-ids", temp.getPath(), "-i", fake.getPath()).replaceAll("\\s+", " "), "Given file \"" + temp.getPath() + "\" is a directory.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", fake.getPath(), "--bed-info", fake.getPath(), "-i", fake.getPath(), "-Z").replaceAll("\\s+", " "), "The file \"" + fake.getPath() + "\" already exists. Please remove it first or choose a different file");
      TestUtils.containsAll(checkHandleFlagsErr("-o", "blahOutput", "--bed-ids", fake.getPath(), "--vcf-ids", fake.getPath(), "-i", fake.getPath(), "-Z").replaceAll("\\s+", " "), "Only one of --bed-ids or --vcf-ids can be set");
      checkHandleFlagsOut("-o", "blahOutput", "-i", fake.getPath());
    } finally {
      FileHelper.deleteAll(temp);
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
      final File inVcf = new File(dir, "input.vcf");
      final File idVcf = new File(dir, "id.vcf");
      final File outFile = new File(dir, "output.vcf");
      FileUtils.stringToFile(mNano.loadReference("snpAnnotate_small.vcf"), inVcf);
      FileUtils.stringToFile(mNano.loadReference("snpAnnotate_small_ids_vcf.vcf"), idVcf);
      final String str = checkMainInitOk("-i", inVcf.getPath(), "--vcf-ids", idVcf.getPath(), "-o", outFile.getPath(), "-Z", "--fill-an-ac", "--Xderived-annotations", "NAA,ZY,PD");
      assertEquals("", str);
      assertTrue(outFile.isFile());
      final String actual = StringUtils.grep(FileUtils.fileToString(outFile), "^[^#]").replaceAll("[\r|\n]+", "\n");
      mNano.check("snpAnnotate_small_vcf_ids_exp.vcf", actual, false);
    }
  }

  private void check(String bed, String id, boolean ids) throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File inVcf = new File(dir, "input.vcf");
      final File inBed = new File(dir, "input.bed");
      final File outFile = new File(dir, "output.vcf");
      FileUtils.stringToFile(mNano.loadReference(id + ".vcf"), inVcf);
      FileUtils.stringToFile(mNano.loadReference(bed), inBed);
      final String str;
      if (ids) {
        str = checkMainInitOk("-i", inVcf.getPath(), "--bed-ids", inBed.getPath(), "-o", outFile.getPath(), "-Z", "--fill-an-ac", "--Xderived-annotations", "NAA,ZY,PD");
      } else {
        str = checkMainInitOk("-i", inVcf.getPath(), "--bed-info", inBed.getPath(), "-o", outFile.getPath(), "-Z", "--fill-an-ac", "--Xderived-annotations", "NAA,ZY,PD");
      }
      assertEquals("", str);
      assertTrue(outFile.isFile());
      final String actual = StringUtils.grep(FileUtils.fileToString(outFile), "^[^#]").replaceAll("[\r|\n]+", "\n");
      mNano.check(id + "_exp.vcf", actual, false);
    }
  }
}
