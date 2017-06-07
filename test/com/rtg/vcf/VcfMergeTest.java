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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.BgzipFileHelper;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Test class
 */
public class VcfMergeTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new VcfMerge();
  }

  private static final VcfHeader MINIMAL_HEADER;
  private static final VcfHeader HEADER2;
  static {
    MINIMAL_HEADER = new VcfHeader();
    MINIMAL_HEADER.setVersionValue("VCFv4.1");
    MINIMAL_HEADER.addFormatField("GT", MetaType.STRING, VcfNumber.ONE, "Genotype");
    MINIMAL_HEADER.addLine(VcfHeader.META_STRING + "contig=<ID=\"chr2\",length=10000>");
    MINIMAL_HEADER.addLine(VcfHeader.META_STRING + "contig=<ID=\"chr3\",length=10000>");
    MINIMAL_HEADER.addLine(VcfHeader.META_STRING + "contig=<ID=\"1\",length=10000000>");
    MINIMAL_HEADER.addSampleName("%1$s");

    HEADER2 = new VcfHeader();
    HEADER2.setVersionValue("VCFv4.1");
    HEADER2.addFormatField("GT", MetaType.STRING, VcfNumber.ONE, "Genotype");
    HEADER2.addLine(VcfHeader.META_STRING + "INFO=<ID=XRX,Number=0,Type=Flag,Description=\"RTG variant was called using complex caller\">");
    HEADER2.addLine(VcfHeader.META_STRING + "INFO=<ID=RCE,Number=0,Type=Flag,Description=\"RTG variant is equivalent to the previous variant\">");
    HEADER2.addLine(VcfHeader.META_STRING + "INFO=<ID=CT,Number=1,Type=Integer,Description=\"Coverage threshold that was applied\">");
    HEADER2.addLine(VcfHeader.META_STRING + "contig=<ID=\"chr2\",length=10000>");
    HEADER2.addLine(VcfHeader.META_STRING + "contig=<ID=\"chr3\",length=10000>");
    HEADER2.addSampleName("%1$s");
  }

  public void testZipper() throws Exception {
    final String resourcea = "vcfmerge-f1.vcf";
    final String resourceb = "vcfmerge-f2.vcf";
    try (final TestDirectory dir = new TestDirectory("vcfmerge")) {
      final File f1 = BgzipFileHelper.bytesToBgzipFile(FileHelper.resourceToString("com/rtg/vcf/resources/" + resourcea).getBytes(), new File(dir, "fileA.vcf.gz"));
      final File f2 = BgzipFileHelper.bytesToBgzipFile(FileHelper.resourceToString("com/rtg/vcf/resources/" + resourceb).getBytes(), new File(dir, "fileB.vcf.gz"));
      new TabixIndexer(f1, TabixIndexer.indexFileName(f1)).saveVcfIndex();
      new TabixIndexer(f2, TabixIndexer.indexFileName(f2)).saveVcfIndex();

      final int[] positions = {100, 200, 900, 200, 300, 500};
      final String[] templates = {"chr2", "chr2", "chr2", "chr3", "chr3", "chr3"};
      final String[][] alts = {new String[] {"C", "G"}, new String[] {"C"}, new String[] {"G"},
        new String[] {"G"}, new String[] {"C"}, new String[] {"C", "G"}};
      final String[][] samples = {new String[] {"sample1", "sample2"}, new String[] {"sample1"}, new String[] {"sample2"},
        new String[] {"sample2"}, new String[] {"sample1"}, new String[] {"sample1", "sample2"}};

      VcfMerge.VcfPositionZipper zipper = new VcfMerge.VcfPositionZipper(null, f1, f2);
      try {
        final VcfMerge.ZipperCallback callback = new ZipperCallbackTester(positions, templates, alts, samples);
        int count = 0;
        while (zipper.hasNextPosition()) {
          zipper.nextPosition(callback);
          ++count;
        }
        assertEquals(positions.length, count);
      } finally {
        zipper.close();
      }
      zipper = new VcfMerge.VcfPositionZipper(new RegionRestriction("chr3:300-500"), f1, f2);
      try {
        final VcfMerge.ZipperCallback callback = new ZipperCallbackTester(
                Arrays.copyOfRange(positions, 4, 6),
                Arrays.copyOfRange(templates, 4, 6),
                Arrays.copyOfRange(alts, 4, 6),
                Arrays.copyOfRange(samples, 4, 6));
        int count = 0;
        while (zipper.hasNextPosition()) {
          zipper.nextPosition(callback);
          ++count;
        }
        assertEquals(2, count);
      } finally {
        zipper.close();
      }
    }
  }


  private static class ZipperCallbackTester implements VcfMerge.ZipperCallback {
    private int mI = 0;
    private final int[] mPositions;
    private final String[] mTemplates;
    private final String[][] mAlts;
    private final String[][] mSamples;

    ZipperCallbackTester(int[] positions, String[] templates, String[][] alts, String[][] samples) {
      mPositions = positions;
      mTemplates = templates;
      mAlts = alts;
      mSamples = samples;
    }

    @Override
    public void vcfAtPosition(VcfRecord[] records, VcfHeader[] headers) {
      for (int i = 0; i < records.length; ++i) {
        assertEquals("" + mI, mPositions[mI], records[i].getOneBasedStart());
        assertEquals(mTemplates[mI], records[i].getSequenceName());
        assertEquals(mAlts[mI][i], records[i].getAltCalls().get(0));
        assertTrue(headers[i].toString().contains(mSamples[mI][i]));
      }
      ++mI;
    }
  }

  public void testSamePositionDifferentRef() throws Exception {
    try (final TestDirectory dir = new TestDirectory("vcfmerge")) {
      final String string1 = String.format(MINIMAL_HEADER.toString(), "NA12878")
          + "1       20200602        .       C       T       575.1   PASS    XRX     GT:DP:RE:GQ     0/1:42:4.891:56\n".replaceAll(" +", "\t")
          + "1       20200605        .       T       C       575.1   PASS    XRX     GT:DP:RE:GQ     0/1:42:4.891:56\n".replaceAll(" +", "\t")
          + "1       20200635        .       C       T       575.1   PASS    XRX     GT:DP:RE:GQ     1/1:42:4.891:56\n".replaceAll(" +", "\t");

      final String string2 = String.format(MINIMAL_HEADER.toString(), "NA12891")
          + "1 20200613        .       ACCACCATCAC     ACCACCAT,ATCACCATCAC    895.7   PASS    XRX     GT:DP:RE:GQ     1/2:54:2.680:43\n".replaceAll(" +", "\t")
          + "1       20200635        .       C       CCAT    601.1   PASS    XRX     GT:DP:RE:GQ     0/1:46:0.028:601\n".replaceAll(" +", "\t");

      final String string3 = String.format(MINIMAL_HEADER.toString(), "NA12892")
          + "1       20200602        .       CCATCACCACCACCACCATCACCACCACCACCAC      CCATCACCACCACCACCAT,CTATCACCACCATCACCATCACCACCACCACCACCAT       2562.9  PASS    XRX     GT:DP:RE:GQ     1/2:57:2.515:371\n".replaceAll(" +", "\t");

      final File f1 = BgzipFileHelper.bytesToBgzipFile(string1.getBytes(), new File(dir, "file1.vcf.gz"));
      new TabixIndexer(f1, TabixIndexer.indexFileName(f1)).saveVcfIndex();
      final File f2 = BgzipFileHelper.bytesToBgzipFile(string2.getBytes(), new File(dir, "file2.vcf.gz"));
      new TabixIndexer(f2, TabixIndexer.indexFileName(f2)).saveVcfIndex();
      final File f3 = BgzipFileHelper.bytesToBgzipFile(string3.getBytes(), new File(dir, "file3.vcf.gz"));
      new TabixIndexer(f3, TabixIndexer.indexFileName(f3)).saveVcfIndex();
      final File output = new File(dir, "out.vcf");
      checkMainInit("-Z", "-o", output.getPath(), "--Xnon-padding-aware", f1.getPath(), f2.getPath(), f3.getPath());
      mNano.check("vcfmerge_testSamePosDiffRef.vcf", TestUtils.sanitizeVcfHeader(FileUtils.fileToString(output)), false);
      final File output2 = new File(dir, "out2.vcf");
      checkMainInit("-Z", "-o", output2.getPath(), f1.getPath(), f2.getPath(), f3.getPath());
      mNano.check("vcfmerge_testSamePosDiffRef_pa.vcf", TestUtils.sanitizeVcfHeader(FileUtils.fileToString(output2)), false);
    }
  }

  public void testSamePosition() throws Exception {
    try (final TestDirectory dir = new TestDirectory("vcfmerge")) {
      final String string1 = String.format(MINIMAL_HEADER.toString(), "NA12878")
              + "1       20200602        .       C       T       575.1   PASS    XRX     GT:DP:RE:GQ     0/1:42:4.891:56\n".replaceAll(" +", "\t");
      final String string2 = String.format(MINIMAL_HEADER.toString(), "NA12891")
              + "1       20200602        .       CA       C       575.1   PASS    XRX     GT:DP:RE:GQ     0/1:42:4.891:56\n".replaceAll(" +", "\t");
      final String string3 = String.format(MINIMAL_HEADER.toString(), "NA12892")
              + "1       20200602        .       C       G       575.1   PASS    XRX     GT:DP:RE:GQ     0/1:42:4.891:56\n".replaceAll(" +", "\t");
      final File f1 = BgzipFileHelper.bytesToBgzipFile(string1.getBytes(), new File(dir, "file1.vcf.gz"));
      new TabixIndexer(f1, TabixIndexer.indexFileName(f1)).saveVcfIndex();
      final File f2 = BgzipFileHelper.bytesToBgzipFile(string2.getBytes(), new File(dir, "file2.vcf.gz"));
      new TabixIndexer(f2, TabixIndexer.indexFileName(f2)).saveVcfIndex();
      final File f3 = BgzipFileHelper.bytesToBgzipFile(string3.getBytes(), new File(dir, "file3.vcf.gz"));
      new TabixIndexer(f3, TabixIndexer.indexFileName(f3)).saveVcfIndex();
      final File output = new File(dir, "out.vcf.gz");
      checkMainInit("-o", output.getPath(), f2.getPath(), f3.getPath());
      final File output2 = new File(dir, "out2.vcf");
      checkMainInit("-Z", "-o", output2.getPath(), f1.getPath(), output.getPath());
      mNano.check("vcfmerge_testSamePos.vcf", TestUtils.sanitizeVcfHeader(FileUtils.fileToString(output2)), false);
    }
  }


  public void testSingleFileMerge() throws Exception {
    final String resourcea = "vcfmerge-f3.vcf";
    try (final TestDirectory dir = new TestDirectory("vcfmerge")) {
      final File f1 = BgzipFileHelper.bytesToBgzipFile(FileHelper.resourceToString("com/rtg/vcf/resources/" + resourcea).getBytes(), new File(dir, "fileA.vcf.gz"));
      new TabixIndexer(f1, TabixIndexer.indexFileName(f1)).saveVcfIndex();
      final File output = new File(dir, "out.vcf");
      checkMainInitOk("-Z", "-o", output.getPath(), "-a", "##extraline=foo", "-a", "##extraline2=bar", f1.getPath());
      mNano.check("vcfmerge_testSingle.vcf", TestUtils.sanitizeVcfHeader(FileUtils.fileToString(output)), false);
    }
  }

  public void checkMerge(String id, String resourcea, String resourceb, String... argsIn) throws Exception {
    try (final TestDirectory dir = new TestDirectory("vcfmerge")) {
      final File snpsA = BgzipFileHelper.bytesToBgzipFile(FileHelper.resourceToString("com/rtg/vcf/resources/" + resourcea).getBytes(), new File(dir, "fileA.vcf.gz"));
      new TabixIndexer(snpsA, TabixIndexer.indexFileName(snpsA)).saveVcfIndex();
      final File snpsB = BgzipFileHelper.bytesToBgzipFile(FileHelper.resourceToString("com/rtg/vcf/resources/" + resourceb).getBytes(), new File(dir, "fileB.vcf.gz"));
      new TabixIndexer(snpsB, TabixIndexer.indexFileName(snpsB)).saveVcfIndex();
      final File output = new File(dir, "out.vcf.gz");
      final List<String> args = new ArrayList<>();
      args.addAll(Arrays.asList("-o", output.toString(),
        "--stats",
        snpsA.toString(), snpsB.toString()));
      args.addAll(Arrays.asList(argsIn));
      final String out = checkMainInit(args.toArray(new String[args.size()])).out();
      assertEquals(BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK, BlockCompressedInputStream.checkTermination(output));
      assertTrue(new File(dir, output.getName() + ".tbi").isFile());
      mNano.check("vcfmerge_out_" + id + ".vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(output)), false);
      mNano.check("vcfmerge_stats_" + id + ".txt", out);
    }
  }

  public void testMerge() throws Exception {
    checkMerge("simple", "vcfmerge-f1.vcf", "vcfmerge-f2.vcf",
      "-a", "##extraline=foo",
      "-a", "##extraline2=bar");
  }

  public void testMergeFileOrderReversed() throws Exception {
    checkMerge("simple-rev", "vcfmerge-f2.vcf", "vcfmerge-f1.vcf",
      "-a", "##extraline=foo",
      "-a", "##extraline2=bar");
  }

  public void testMergingVariableNumberFormats() throws Exception {
    checkMerge("numberformats", "snpsA.vcf", "snpsB.vcf", "--preserve-formats");
  }

  public void testMergeDrop() throws Exception {
    checkMerge("drop", "vcfmerge-na12889.vcf", "vcfmerge-na12880.vcf",
      "-a", "##extraline=foo",
      "-a", "##extraline2=bar");
  }

  public void testMergePreserve() throws Exception {
    checkMerge("preserve", "vcfmerge-na12889.vcf", "vcfmerge-na12880.vcf",
      "-a", "##extraline=foo",
      "-a", "##extraline2=bar", "--preserve-formats");
  }

  public void testForce1() throws Exception {
    checkMerge("force1", "vcfmerge-f1.vcf", "vcfmerge-f3.vcf",
      "--force-merge", "GT");
  }

  public void testForceAll() throws Exception {
    checkMerge("force-all", "vcfmerge-f1.vcf", "vcfmerge-f3.vcf",
      "--force-merge-all");
  }

  public void testFlags() {
    checkMainInitBadFlags("-f", "-F");
  }

  public void testMixNoSample() throws Exception {
    checkMerge("mix-nosample", "vcfmerge-mix-nosample-1.vcf", "vcfmerge-mix-nosample-2.vcf");
  }
}
