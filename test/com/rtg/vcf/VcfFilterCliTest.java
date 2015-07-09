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
import java.io.InputStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.MathUtils;
import com.rtg.util.PosteriorUtils;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.Utils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class VcfFilterCliTest extends AbstractCliTest {

  private static final String RESOURCES = "com/rtg/vcf/resources/";

  @Override
  protected AbstractCli getCli() {
    return new VcfFilterCli();
  }
//
//  @Override
//  public void setUp() {
//    GlobalFlags.resetAccessedStatus();
//  }

  public void testFlags() {
    checkHelp("rtg vcffilter", "Filters VCF records based on various criteria."
        , "-i,", "--input=FILE", "VCF file containing variants"
        , "-o,", "--output=FILE", "output VCF file"
        , "-w,", "--density-window=INT", "window within which multiple variants are discarded"
        , "-q,", "--min-quality=FLOAT", "minimum allowed quality"
        , "-Q,", "--max-quality=FLOAT", "maximum allowed quality"
        , "-g,", "--min-genotype-quality=FLOAT", "minimum allowed genotype quality"
        , "-G,", "--max-genotype-quality=FLOAT", "maximum allowed genotype quality"
        , "-c,", "--min-combined-read-depth=INT", "minimum allowed combined read depth"
        , "-C,", "--max-combined-read-depth=INT", "maximum allowed combined read depth"
        , "-d,", "--min-read-depth=INT", "minimum allowed sample read depth"
        , "-D,", "--max-read-depth=INT", "maximum allowed sample read depth"
        , "-A,", "--max-ambiguity-ratio=FLOAT", "maximum allowed ambiguity ratio"
        , "-k,", "--keep-filter=STRING", "only keep variants with this FILTER tag"
        , "-K,", "--keep-info=STRING", "only keep variants with this INFO tag"
        , "-r,", "--remove-filter=STRING", "remove variants with this FILTER tag"
        , "-R,", "--remove-info=STRING", "remove variants with this INFO tag"
        , "--include-bed=FILE", "only keep variants within the regions in this BED file"
        , "--exclude-bed=FILE", "discard all variants within the regions in this BED file"
        , "--include-vcf=FILE", "only keep variants that overlap with the ones in this file"
        , "--exclude-vcf=FILE", "discard all variants that overlap with the ones in this file"
        , "--clear-failed-samples", "instead of removing failed records set the sample GT fields to missing"
        , "--fail=STRING", "instead of removing failed records set their filter field to the provided value"
        , "--remove-all-same-as-ref", "remove where all samples are same as reference"
        , "--remove-same-as-ref", "remove where sample is same as reference"
        , "--snps-only", "if set, will output simple SNPs only"
        , "--non-snps-only", "if set, will output MNPs and INDELs only"
        , "-Z,", "--no-gzip", "do not gzip the output"
        , "--no-index", "do not produce indexes for output files"
        );

    checkExtendedHelp("rtg vcffilter"
        , "-p,", "--Xmin-posterior-score=FLOAT", "minimum allowed posterior score"
        , "-P,", "--Xmax-posterior-score=FLOAT", "maximum allowed posterior score"
        );
  }

  private static final String INPUT1 = ""
  + VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n"
      + "chr1\t3037327\t.\tT\tC\t0.5\tPASS\t.\tGT:GQ\t1/1:" + PosteriorUtils.phredIfy(0.5 * MathUtils.LOG_10)
  + "\n" + "chr1\t3161541\t.\tT\tA\t1.0\tPASS\t.\tGT:GQ\t1/1:" + PosteriorUtils.phredIfy(1.0 * MathUtils.LOG_10)
  + "\n" + "chr1\t3167696\t.\tA\tG\t5.6\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(5.6 * MathUtils.LOG_10), 1)
  + "\n" + "chr1\t3174292\t.\tG\tC\t5.1\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(5.1 * MathUtils.LOG_10), 1)
  + "\n" + "chr1\t3181150\t.\tT\tG\t4.1\tPASS\t.\tGT:GQ\t1/1:" + PosteriorUtils.phredIfy(4.1 * MathUtils.LOG_10)
  + "\n" + "chr1\t3181157\t.\tC\tT\t6.1\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(6.1 * MathUtils.LOG_10), 1)
  + "\n" + "chr1\t3196572\t.\tG\tC\t17.1\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(17.2 * MathUtils.LOG_10), 1) + "\n";

  private static final String EXPECTED1 = ""
      + VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n"
  + "chr1\t3167696\t.\tA\tG\t5.6\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(5.6 * MathUtils.LOG_10), 1) + "\n"
  + "chr1\t3174292\t.\tG\tC\t5.1\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(5.1 * MathUtils.LOG_10), 1) + "\n"
  + "chr1\t3181157\t.\tC\tT\t6.1\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(6.1 * MathUtils.LOG_10), 1) + "\n"
  + "chr1\t3196572\t.\tG\tC\t17.1\tPASS\t.\tGT:GQ\t1/1:" + Utils.realFormat(PosteriorUtils.phredIfy(17.2 * MathUtils.LOG_10), 1) + "\n";

  public void testPosteriorFiltering() throws IOException {
    final File main = FileUtils.createTempDir("snpFilter", "posterior");
    try {
      final File in = new File(main, "input");
      FileUtils.stringToFile(INPUT1, in);
      final File out = new File(main, "out");

      final String output = checkMainInitOk("-i", in.getPath(), "-o", out.getPath(), "-Z", "-p", "5.0");

      final String o = FileUtils.fileToString(out);
      final String actual = StringUtils.grepMinusV(o, "^##(RUN-ID)|(CL)").replaceAll("[\r\n]+", "\n");
      assertEquals(EXPECTED1, actual);

      TestUtils.containsAll(output, "Total records : 7", "Filtered due to posterior : 3", "Remaining records : 4");
    } finally {
      assertTrue(FileHelper.deleteAll(main));
    }
  }

  public void testTabixWarningMessage() {
    assertEquals("Cannot produce TABIX index for: foo: message", TabixIndexer.getTabixWarningMessage(new File("foo"), new UnindexableDataException("message")));
  }

  private static final String INPUT3 = ""
  + VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n"
  +  "chr1\t3824544\t.\tGA\tGCT\t7.7\tPASS\tDP=8\tGT:GQ:DP:RE\t1/1:3.0:8:0.460" + "\n";

  private static final String EXPECTED3 = ""
      + VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n";

  public void testCoverageFiltering() throws IOException {
    try (final TestDirectory main = new TestDirectory()) {
      final File in = new File(main, "input");
      FileUtils.stringToFile(INPUT3, in);
      final File out = new File(main, "out");

      final String output = checkMainInitOk("-i", in.getPath(), "-o", out.getPath(), "-d", "9", "-Z");

      final String o = FileUtils.fileToString(out);
      final String actual = StringUtils.grepMinusV(o, "^##(RUN-ID)|(CL)").replaceAll("[\r\n]+", "\n");
      assertEquals(EXPECTED3, actual);

      TestUtils.containsAll(output, "Total records : 1", "Filtered due to sample read depth : 1", "Remaining records : 0");
    }
  }

  public void testCombinedCoverageFiltering() throws IOException {
    try (final TestDirectory main = new TestDirectory()) {
      final File in = new File(main, "input");
      FileUtils.stringToFile(INPUT3, in);
      final File out = new File(main, "out");

      final String output = checkMainInitOk("-i", in.getPath(), "-o", out.getPath(), "-c", "9", "-Z");

      final String o = FileUtils.fileToString(out);
      final String actual = StringUtils.grepMinusV(o, "^##(RUN-ID)|(CL)").replaceAll("[\r\n]+", "\n");
      assertEquals(EXPECTED3, actual);

      TestUtils.containsAll(output, "Total records : 1", "Filtered due to combined read depth : 1", "Remaining records : 0");
    }
  }

  private static final String INPUT4 = ""
      + VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n"
      + "chr1\t3824544\t.\tGA\tGCT\t7.7\tPASS\tDP=10\tGT:GQ:DP:RE\t1/1:3.0:10:0.460" + "\n";

  private static final String EXPECTED4 = ""
      + VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n"
        + "chr1\t3824544\t.\tGA\tGCT\t7.7\tPASS\tDP=10\tGT:GQ:DP:RE\t1/1:3.0:10:0.460" + "\n";

  public void testCoverageFiltering2() throws IOException {
    try (final TestDirectory main = new TestDirectory()) {
      final File in = new File(main, "input");
      FileUtils.stringToFile(INPUT4, in);
      final File out = new File(main, "out");

      final String output = checkMainInitOk("-i", in.getPath(), "-o", out.getPath(), "-d", "9", "-Z");

      final String o = FileUtils.fileToString(out);
      final String actual = StringUtils.grepMinusV(o, "^##(RUN-ID)|(CL)").replaceAll("[\r\n]+", "\n");
      assertEquals(EXPECTED4, actual);

      TestUtils.containsAll(output, "Total records : 1", "Remaining records : 1");
    }
  }

  public void testFlagValidator() throws IOException {
    final File main = FileUtils.createTempDir("snpFilter", "flag1");
    try {
      final File out = new File(main, "out.gz");
      final File out2 = new File(main, "out.txt");
      String err = checkHandleFlagsErr("-i", "foo", "-o", out.getPath());
      TestUtils.containsAll(err, "Given file \"foo\" does not exist.");

      err = checkHandleFlagsErr("-i", main.getPath(), "-o", out.getPath());
      TestUtils.containsAll(TestUtils.unwrap(err), main.getPath() + "\" is a directory");

      final File in = new File(main, "input");
      FileUtils.stringToFile(INPUT4, in);
      assertTrue(out.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "out").getPath());
      TestUtils.containsAll(TestUtils.unwrap(err), "The file \"" + out.getPath() + "\" already exists");

      assertTrue(out2.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out2.getPath(), "--no-gzip");
      TestUtils.containsAll(TestUtils.unwrap(err), "The file \"" + out2.getPath() + "\" already exists");

      assertTrue(out.delete());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-p", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--Xmin-posterior-score\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-p", "1.0", "-P", "0.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--Xmax-posterior-score\" has invalid value \"" + 0.0 + "\". It should be greater than or equal to " + 1.0 + " and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-Q", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-q", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-q", "1.0", "-Q", "0.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-quality\" has invalid value \"" + 0.0 + "\". It should be greater than or equal to " + 1.0 + " and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-Q", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-G", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-genotype-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-g", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-genotype-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-g", "1.0", "-G", "0.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-genotype-quality\" has invalid value \"" + 0.0 + "\". It should be greater than or equal to " + 1.0 + " and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-G", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-genotype-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-d", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-D", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-c", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-combined-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-C", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-combined-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-A", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-ambiguity-ratio\" has invalid value \"-1.0\". It should be greater than or equal to 0.0 and less than or equal to 1.0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-A", "1.1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-ambiguity-ratio\" has invalid value \"1.1\". It should be greater than or equal to 0.0 and less than or equal to 1.0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--density-window", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--density-window\" has invalid value \"-1\". It should be greater than or equal to \"1\".");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--min-genotype-quality",  "0" , "--Xmax-posterior-score", "10");
      TestUtils.containsAll(TestUtils.unwrap(err), "Use genotype-quality or posterior filters, not both.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--Xmin-posterior-score",  "0" , "--max-genotype-quality", "10");
      TestUtils.containsAll(TestUtils.unwrap(err), "Use genotype-quality or posterior filters, not both.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--min-denovo-score",  "10", "--sample", "alice", "--sample", "bob");
      TestUtils.containsAll(TestUtils.unwrap(err), "De Novo filtering requires a single sample to be specified");

      final File bedFile = new File(main, "foo.bed");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--include-bed\" file: \"" + bedFile + "\" doesn't exist");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--exclude-bed\" file: \"" + bedFile + "\" doesn't exist");

      assertTrue(bedFile.mkdir());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-vcf", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--include-vcf\" file: \"" + bedFile + "\" is a directory");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-vcf", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--exclude-vcf\" file: \"" + bedFile + "\" is a directory");

      assertTrue(bedFile.delete() && bedFile.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-vcf", bedFile.toString(), "--include-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --include-bed or --include-vcf");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-vcf", bedFile.toString(), "--exclude-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --exclude-bed or --exclude-vcf");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--snps-only", "--non-snps-only");
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --snps-only or --non-snps-only");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--fail", "BLAH", "--clear-failed-samples");
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --fail or --clear-failed-samples");

    } finally {
      assertTrue(FileHelper.deleteAll(main));
    }
  }

  public void testMoreComplexCase() throws IOException {
    final File dir = FileUtils.createTempDir("snpFilter", "complex");
    try {
      final File in = new File(dir, "input");

      final String res = RESOURCES + "snps_complex.vcf";
      try (InputStream stream = Resources.getResourceAsStream(res)) {
        assertNotNull("Can't find:" + res, stream);

        final String snps = FileUtils.streamToString(stream);
        FileUtils.stringToFile(snps, in);
      }
      final File out = new File(dir, "out");
      final String output = checkMainInitOk("-i", in.getPath(), "-o", out.getPath(), "-Z", "-d", "4", "-D", "6", "-p", "0.49", "-P", "1.21");

      final String o = FileUtils.fileToString(out);
      assertTrue(o.length() > 0);
      TestUtils.containsAll(o,
        "snp --max-ih=1 -m cg_errors --Xindels --max-as-mated=4 -o snp_mated_cgerrors_indels_as4 -t /rtgshare/data/human/sdf/hg18 map_GS000005015/mated.sam.gz map_GS000005016/mated.sam.gz",
        VcfHeader.HEADER_LINE,
        "chr1    44376   .       N       NCT     .       PASS    .       GT:DP:RE:GQ:RS  1/0:4:0.052:12.3:CT,1,0.022,i,3,0.030".replaceAll("\\s+", "\t"),
        "chr1    45418   .       N       NT      .       PASS    .       GT:DP:RE:GQ:RS  1/0:4:0.536:6.2:T,1,0.506,i,3,0.030".replaceAll("\\s+", "\t"),
        "chr1    46244   .       T       C       .       PASS    .       GT:DP:RE:GQ:RS  1/1:4:0.287:7.8:C,4,0.287".replaceAll("\\s+", "\t"),
        "chr1    82299   .       N       NA      .       PASS    .       GT:DP:RE:GQ:RS  1/0:4:0.071:8.6:A,1,0.041,i,3,0.030".replaceAll("\\s+", "\t"));

      TestUtils.containsAll(output, "Total records : 27", "Filtered due to posterior : 19", "Filtered due to sample read depth : 4", "Remaining records : 4");
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  // Run a test where vcffilter is expected to complete normally
  private void runResourceTest(String[] extraArgs, String inResourceLoc, String expResourceLoc) throws IOException {
    final File dir = FileUtils.createTempDir("vcffilter", "complex");
    try {
      final File in = new File(dir, new File(Resources.getResource(inResourceLoc).getFile()).getName());

      try (InputStream stream = Resources.getResourceAsStream(inResourceLoc)) {
        assertNotNull("Cant find:" + inResourceLoc, stream);

        final String snps = FileUtils.streamToString(stream);
        FileUtils.stringToFile(snps, in);
      }
      final File out = new File(dir, "out");
      final String[] args = {
          "-i", in.getPath(), "-o", out.getPath(), "-Z",
      };
      final String output = checkMainInitOk(TestUtils.append(args, extraArgs));
      mNano.check(expResourceLoc + ".txt", output, true);

      final String o = StringUtils.grep(FileUtils.fileToString(out), "^[^#]").replaceAll("[\r|\n]+", "\n");
      mNano.check(expResourceLoc, o, true);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  // Run a test where vcffilter is expected to fail
  private void runResourceTestError(String[] extraArgs, String inResourceLoc, String expResourceLoc) throws IOException {
    final File dir = FileUtils.createTempDir("vcffilter", "complex");
    try {
      final File in = new File(dir, new File(Resources.getResource(inResourceLoc).getFile()).getName());

      try (InputStream stream = Resources.getResourceAsStream(inResourceLoc)) {
        assertNotNull("Cant find:" + inResourceLoc, stream);

        final String snps = FileUtils.streamToString(stream);
        FileUtils.stringToFile(snps, in);
      }
      final File out = new File(dir, "out");
      final String[] args = {
        "-i", in.getPath(), "-o", out.getPath(), "-Z",
      };
      final String output = checkMainInitBadFlags(TestUtils.append(args, extraArgs));
      mNano.check(expResourceLoc, output, true);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testHighCoverage() throws IOException {
    final String[] args = {
        "-r",
        "OC"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest2.vcf",
        "snpfiltertest2_exp.vcf"
    );
  }

  public void testAmbiguous() throws IOException {
    final String[] args = {
        "-r",
        "a1000.0"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest.vcf",
        "snpfiltertest_exp.vcf"
    );
  }

  public void testAmbiguous2() throws IOException {
    final String[] args = {
        "-r",
        "a1000.0",
        "--clear-failed-samples"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest.vcf",
        "snpfiltertest_exp_clear.vcf"
    );
  }

  public void testSameAsRef() throws IOException {
    final String[] args = {
        "--remove-same-as-ref",
        "--sample", "SAMPLE"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestSR.vcf",
        "snpfiltertestSR_exp.vcf"
    );
  }

  public void testAllSameAsRef() throws IOException {
    final String[] args = {
        "--remove-all-same-as-ref"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestSR.vcf",
        "snpfiltertestASR_exp.vcf"
    );
  }

  public void testAvr() throws IOException {
    final String[] args = {
        "--min-avr-score", "0.3",
        "--sample", "SAMPLE"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp.vcf"
    );
  }

  public void testAvr2() throws IOException {
    final String[] args = {
        "--min-avr-score", "0.3",
        "--sample", "SAMPLE2"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp2.vcf"
    );
  }

  public void testAvr3() throws IOException {
    final String[] args = {
        "--min-avr-score", "0.3",
        "--sample", "SAMPLE",
        "--sample", "SAMPLE2"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp3.vcf"
    );
  }

  public void testAvr4() throws IOException {
    final String[] args = {
        "--min-avr-score", "0.3",
        "--all-samples"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp4.vcf"
    );
  }

  public void testAvr5() throws IOException {
    final String[] args = {
        "--min-avr-score", "0.3",
        "--all-samples",
        "--fail", "BLAH"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp5.vcf"
    );
  }

  public void testAvr6() throws IOException {
    final String[] args = {
        "--min-avr-score", "0.3",
        "--all-samples",
        "--clear-failed-samples"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp6.vcf"
    );
  }

  public void testAmbiguousMultisample() throws IOException {
    final String[] args = {
        "-r",
        "a1000.0",
        "--clear-failed-samples",
        "--all-samples"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp7.vcf"
    );
  }

  public void testAmbiguousMultisample2() throws IOException {
    final String[] args = {
        "-r",
        "a1000.0",
        "--clear-failed-samples",
        "--sample", "SAMPLE2"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertestAVR.vcf",
        "snpfiltertestAVR_exp8.vcf"
    );
  }

  public void testComplex() throws IOException {
    final String[] args = {
        "-r",
        "RC",
        "-r",
        "RX",
        "-r",
        "a1000.0",
        "--remove-same-as-ref"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest3.vcf",
        "snpfiltertest3_exp.vcf"
    );
  }

  public void testKeepAll() throws IOException {
    final String[] args = {
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest3.vcf",
        "snpfiltertest3_exp_all.vcf"
    );
  }

  public void testSnpsOnly() throws IOException {
    final String[] args = {
        "--snps-only",
        "-k",
        "PASS"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest4.vcf",
        "snpfiltertest4_exp.vcf"
    );
  }

  public void testNonSnpsOnly() throws IOException {
    final String[] args = {
        "--non-snps-only",
        "-k",
        "PASS"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest4.vcf",
        "snpfiltertest4_exp_nonsnps.vcf"
    );
  }

  public void testVcf() throws IOException {
    final String[] args = {
        "-q", "5.0",
        "-Q", "60.0",
        "-g", "5.0",
        "-G", "60.0",
        "-A", "0.1"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest5.vcf",
        "snpfiltertest5_exp.vcf"
    );
  }

  public void testVcfKeepAll() throws IOException {
    final String[] args = {
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest5.vcf",
        "snpfiltertest5_exp_all.vcf"
    );
  }

  public void testVcfFilterDotPass() throws IOException {
    final String[] args = {
      "--remove-filter", ".", "--remove-filter", "PASS"
    };
    runResourceTest(
      args,
      RESOURCES + "snpfiltertest5.vcf",
      "snpfiltertest5_DotPass_exp.vcf"
    );
  }

  public void testVcfFilterRx() throws IOException {
    final String[] args = {
      "--remove-filter", "RX"
    };
    runResourceTest(
      args,
      RESOURCES + "snpfiltertest5.vcf",
      "snpfiltertest5_rx_exp.vcf"
    );
  }

  public void testVcfDensityWindow() throws IOException {
    final String[] args = {
        "--density-window", "10"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest6.vcf",
        "snpfiltertest6_exp.vcf"
    );
  }

  public void testVcfDenovoPosteriorSon() throws IOException {
    final String[] args = {
        "--min-denovo-score", "6", "--max-denovo-score", "30", "--sample", "SON"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest_DNP.vcf",
        "snpfiltertest_DNP_exp.vcf"
    );
  }

  public void testVcfDenovoPosteriorDaughter() throws IOException {
    final String[] args = {
        "--min-denovo-score", "6", "--max-denovo-score", "30", "--sample", "DAUGHTER"
    };
    runResourceTest(
        args,
        RESOURCES + "snpfiltertest_DNP.vcf",
        "snpfiltertest_DNP_sample_exp.vcf"
    );
  }

  public void testIncludeExclude() throws IOException {
    final File tempDirectory = FileHelper.createTempDirectory();
    try {
      final File exclude = new File(tempDirectory, "exclude");
      FileHelper.resourceToFile(RESOURCES + "exclude.bed", exclude);
      final File include = new File(tempDirectory, "include");
      FileHelper.resourceToFile(RESOURCES + "include.bed", include);

      final String[] args = {
          "--include-bed", include.toString()
          , "--exclude-bed", exclude.toString()
      };
      runResourceTest(
          args,
          RESOURCES + "snpfiltertest_complex.vcf",
          "snpfiltertest_complex_inc_exc_bed.vcf"
      );
    } finally {
      FileHelper.deleteAll(tempDirectory);
    }
  }

  public void testIncludeExcludeVcf() throws IOException {
    final File tempDirectory = FileHelper.createTempDirectory();
    try {
      final File exclude = new File(tempDirectory, "exclude");
      FileHelper.resourceToFile(RESOURCES + "exclude.vcf", exclude);
      final File include = new File(tempDirectory, "include");
      FileHelper.resourceToFile(RESOURCES + "include.vcf", include);

      final String[] args = {
          "--include-vcf", include.toString()
          , "--exclude-vcf", exclude.toString()
      };
      runResourceTest(
          args,
          RESOURCES + "snpfiltertest_complex.vcf",
          "snpfiltertest_complex_inc_exc_vcf.vcf"
      );
    } finally {
      FileHelper.deleteAll(tempDirectory);
    }
  }

  public void testVcfRCE() throws Exception {
    runResourceTest(new String[0], RESOURCES + "snpfiltertestRCE.vcf", "snpfiltertestRCE_exp.vcf");
  }

  public void testDensityIndel() throws Exception {
    runResourceTest(new String[0], RESOURCES + "vcffilterIndDens.vcf", "vcffilterIndDens_exp.vcf");
  }

  public void testNoGt() throws Exception {
    runResourceTestError(new String[] {"--remove-same-as-ref"}, RESOURCES + "vcffilterNoGt.vcf", "vcffilterNoGt_exp1.txt");
    runResourceTestError(new String[] {"--remove-all-same-as-ref"}, RESOURCES + "vcffilterNoGt.vcf", "vcffilterNoGt_exp1.txt");

    runResourceTestError(new String[] {"--min-genotype-quality", "50", "--clear-failed-samples"}, RESOURCES + "vcffilterNoGt.vcf", "vcffilterNoGt_exp2.txt");
  }
}
