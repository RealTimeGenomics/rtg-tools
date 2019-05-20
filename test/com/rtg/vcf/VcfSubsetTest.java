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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 */
public class VcfSubsetTest extends AbstractCliTest {

  public void testFlags() {
    checkHelp("rtg vcfsubset", "Removes columnar data from VCF records");
  }

  @Override
  protected AbstractCli getCli() {
    return new VcfSubset();
  }

  public void testKeepInfoACAN() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf.gz");

      checkMainInitOk("-i", f.getPath(), "-o", out.getPath(), "--keep-info", "AC", "--keep-info", "AN");
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(out));
      mNano.check("vcfsubset-keepinfoACAN.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(out)));
    }
  }

  public void testKeepFilter() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf.gz");
      checkMainInitOk("-i", f.getPath(), "-o", out.getPath(), "--keep-filter", "YEA");
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(out));
      mNano.check("vcfsubset-keepfilter.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(out)));

      final File out2 = new File(td, "out2.vcf.gz");
      checkMainInitOk("-i", f.getPath(), "-o", out2.getPath(), "--keep-filter", "PASS");
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(out2));
      mNano.check("vcfsubset-keepfilter-pass.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(out2)));
    }
  }

  public void testKeepSamples() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      checkMainInitOk("-i", f.getPath(), "-o", out.getPath(), "--keep-sample", "HG00096", "--keep-sample", "HG00100", "-Z");
      mNano.check("vcfsubset-keepsamples.vcf", TestUtils.sanitizeVcfHeader(FileHelper.fileToString(out)));
    }
  }

  public void testRemoveFormat() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      checkMainInitOk("-i", f.getPath(), "-o", out.getPath(), "--remove-format", "DS", "-Z");
      mNano.check("vcfsubset-removeformat.vcf", TestUtils.sanitizeVcfHeader(FileHelper.fileToString(out)));
    }
  }

  public void testRemoveMulti() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      checkMainInitOk("-i", f.getPath(), "-o", out.getPath(), "--remove-ids", "--remove-qual", "--remove-samples", "--keep-info", "AN", "--keep-info", "AC", "--keep-filter", "YEA", "-Z");
      mNano.check("vcfsubset-multi.vcf", TestUtils.sanitizeVcfHeader(FileHelper.fileToString(out)));
    }
  }

  public void testValidation() throws Exception {
    try (TestDirectory main = new TestDirectory()) {
      final File in = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(main, "vcf.vcf.gz"));
      final File foo = new File(main, "foo");
      final File out = new File(main, "out.vcf.gz");
      final File out2 = new File(main, "out.vcf");
      String err = checkHandleFlagsErr("-i", foo.getPath(), "-o", out.getPath());
      TestUtils.containsAllUnwrapped(err, " does not exist.");

      err = checkHandleFlagsErr("-i", main.getPath(), "-o", out.getPath());
      TestUtils.containsAllUnwrapped(err, main.getPath() + "\" is a directory");

      assertTrue(out.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "out.vcf").getPath());
      TestUtils.containsAllUnwrapped(err, "The file \"" + out.getPath() + "\" already exists");

      assertTrue(out2.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out2.getPath(), "--no-gzip");
      TestUtils.containsAllUnwrapped(err, "The file \"" + out2.getPath() + "\" already exists");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-infos", "--remove-info", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-infos", "--keep-info", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-info", "feh", "--keep-info", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-filters", "--remove-filter", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-filters", "--keep-filter", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-filter", "feh", "--keep-filter", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-samples", "--remove-sample", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-samples", "--keep-sample", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-sample", "feh", "--keep-sample", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "newout.gz").getPath(), "--remove-format", "feh", "--keep-format", "blah");
      TestUtils.containsAllUnwrapped(err, "Cannot set both");
    }
  }

  public void testMissingSample() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      TestUtils.containsAll(checkMainInitBadFlags("-i", f.getPath(), "-o", out.getPath(), "--keep-sample", "HG00097", "--keep-sample", "HG00099", "--keep-sample", "BL", "--keep-sample", "RJ", "-Z"),
        "Error: 2 samples not contained in VCF: BL RJ" + StringUtils.LS);
    }
  }

  public void testMissingInfo() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      TestUtils.containsAll(checkMainInitWarn("-i", f.getPath(), "-o", out.getPath(), "--keep-info", "BL", "--keep-info", "RJ", "-Z"),
        "INFO fields not contained in VCF meta-information: BL RJ" + StringUtils.LS);
    }
  }

  public void testMissingFilter() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

        TestUtils.containsAll(checkMainInitWarn("-i", f.getPath(), "-o", out.getPath(), "--keep-filter", "BL", "--keep-filter", "RJ", "-Z"),
          "FILTER fields not contained in VCF meta-information: BL RJ" + StringUtils.LS);
    }
  }

  public void testMissingFormat() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      TestUtils.containsAll(checkMainInitWarn("-i", f.getPath(), "-o", out.getPath(), "--keep-format", "BL", "--keep-format", "RJ", "-Z"),
        "FORMAT fields not contained in VCF meta-information: BL RJ" + StringUtils.LS,
        "Records skipped due to no remaining FORMAT fields: 4");
    }
  }

  public void testExplosion() throws Exception {
    try (TestDirectory td = new TestDirectory()) {
      final File f = FileHelper.resourceToGzFile("com/rtg/vcf/resources/vcfsubset.vcf", new File(td, "vcf.vcf.gz"));
      final File out = new File(td, "out.vcf");

      TestUtils.containsAll(checkMainInitWarn("-i", f.getPath(), "-o", out.getPath(), "--remove-format", "GT", "--remove-format", "DS", "-Z"),
        "Records skipped due to no remaining FORMAT fields: 1");

      final String content = FileHelper.fileToString(out);
      final String nonheader = StringUtils.grepMinusV(content, "^#");
      assertTrue(nonheader.startsWith("X\t60052"));
    }
  }
}
