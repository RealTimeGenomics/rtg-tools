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

import java.util.ArrayList;

import com.rtg.AbstractTest;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfHeaderMerge;
import com.rtg.vcf.header.VcfNumber;

/**
 * Tests the corresponding class.
 */
public class VcfRecordMergerTest extends AbstractTest {

  private static final String VCF_RECORD_FORMAT = "%1$s\t%2$d\t.\t%3$s\t%4$s\t.\tPASS\t.\tGT:E1:E2\t%5$d/%6$d:.:.";
  private static final String VCF_RECORD_FORMAT_MISSING = "%1$s\t%2$d\t.\t%3$s\t.\t.\tPASS\t.\tGT:E1:E2\t.";

  static VcfRecord createRecord(String chrom, int pos, String ref, int gt1, int gt2, String... alts) {
    final String altsStr = StringUtils.join(",", alts, false);
    return VcfReaderTest.vcfLineToRecord(String.format(VCF_RECORD_FORMAT, chrom, pos, ref, altsStr, gt1, gt2));
  }

  static VcfRecord createRecord(String chrom, int pos, String ref) {
    return VcfReaderTest.vcfLineToRecord(String.format(VCF_RECORD_FORMAT_MISSING, chrom, pos, ref));
  }

  public void testAllelesChanged() {
    VcfRecord r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    VcfRecord r2 = createRecord("chr1", 50, "A", 1, 2, "C", "G");
    VcfRecord r3 = createRecord("chr1", 50, "A", 1, 2, "G", "C");

    VcfRecordMerger.AlleleMap map = new VcfRecordMerger.AlleleMap("A", new ArrayList<>(), new VcfRecord[]{r2, r2});
    assertFalse(map.altsChanged());

    map = new VcfRecordMerger.AlleleMap("A", new ArrayList<>(), new VcfRecord[]{r2, r1});
    assertTrue(map.altsChanged());

    map = new VcfRecordMerger.AlleleMap("A", new ArrayList<>(), new VcfRecord[]{r1, r2});
    assertTrue(map.altsChanged());

    map = new VcfRecordMerger.AlleleMap("A", new ArrayList<>(), new VcfRecord[]{r1, r3});
    assertTrue(map.altsChanged());
  }

  public void testMerge() {
    final VcfHeader h1 = new VcfHeader();
    h1.addSampleName("sample1");
    final VcfHeader h2 = new VcfHeader();
    h2.addSampleName("sample2");
    final VcfHeader mh = new VcfHeader();
    mh.addSampleName("sample1");
    mh.addSampleName("sample2");
    VcfRecord r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    VcfRecord r2 = createRecord("chr1", 50, "A", 0, 1, "G");
    assertEquals(2, new VcfRecordMerger().setHeader(mh).setAllowMerging(false).mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2}).length);
    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh);
    VcfRecord[] mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(1, mergedArr.length);
    VcfRecord merged = mergedArr[0];
    assertEquals("chr1", merged.getSequenceName());
    assertEquals("A", merged.getRefCall());
    assertEquals(50, merged.getOneBasedStart());
    assertEquals(2, merged.getAltCalls().size());
    assertEquals("C", merged.getAltCalls().get(0));
    assertEquals("G", merged.getAltCalls().get(1));
    assertEquals(2, merged.getFormat(VcfUtils.FORMAT_GENOTYPE).size());
    assertEquals("0/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0));
    assertEquals("0/2", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(1));

    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "A", 1, 2, "G", "C");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(1, mergedArr.length);
    merged = mergedArr[0];
    assertEquals("chr1", merged.getSequenceName());
    assertEquals("A", merged.getRefCall());
    assertEquals(50, merged.getOneBasedStart());
    assertEquals(2, merged.getAltCalls().size());
    assertEquals("C", merged.getAltCalls().get(0));
    assertEquals("G", merged.getAltCalls().get(1));
    assertEquals(2, merged.getFormat(VcfUtils.FORMAT_GENOTYPE).size());
    assertEquals("0/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0));
    assertEquals("2/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(1));

    // Outputs separate records due to different REF
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "AC", 1, 2, "GG", "CG");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(2, mergedArr.length);
    VcfRecordTest.checkRecord(mergedArr[0], r1, new String[]{"0/1", "."});
    VcfRecordTest.checkRecord(mergedArr[1], r2, new String[]{".", "1/2"});

    // Outputs separate records due to different REF
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "AC", 1, 1, "AC"); //this tests the preservation of a buggy case
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(2, mergedArr.length);
    VcfRecordTest.checkRecord(mergedArr[0], r1, new String[]{"0/1", "."});
    VcfRecordTest.checkRecord(mergedArr[1], r2, new String[0], new String[]{".", "1/1"});

    //test blank filling
    mh.addSampleName("sample3");
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "AC", 1, 1, "AG");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(2, mergedArr.length);
    VcfRecordTest.checkRecord(mergedArr[0], r1, new String[]{"0/1", "."});
    VcfRecordTest.checkRecord(mergedArr[1], r2, new String[0], new String[]{".", "1/1"});

    //test reading of GT containing missing values
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "A");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(1, mergedArr.length);
    merged = mergedArr[0];
    assertEquals("chr1", merged.getSequenceName());
    assertEquals("A", merged.getRefCall());
    assertEquals(50, merged.getOneBasedStart());
    assertEquals(1, merged.getAltCalls().size());
    assertEquals("C", merged.getAltCalls().get(0));
    assertEquals(3, merged.getFormat(VcfUtils.FORMAT_GENOTYPE).size());
    assertEquals("0/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0));
    assertEquals(".", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(1));
    assertEquals(".", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(2));

    //test merge handling with multiple refs, some mergeable, some not
    final VcfHeader h3 = new VcfHeader();
    h3.addSampleName("sample3");
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "A", 1, 2, "G", "C");
    final VcfRecord r3 = createRecord("chr1", 50, "AC", 0, 1, "AG");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2, r3}, new VcfHeader[]{h1, h2, h3});
    assertEquals(2, mergedArr.length);
    merged = mergedArr[0];
    assertEquals("chr1", merged.getSequenceName());
    assertEquals("A", merged.getRefCall());
    assertEquals(50, merged.getOneBasedStart());
    assertEquals(2, merged.getAltCalls().size());
    assertEquals("C", merged.getAltCalls().get(0));
    assertEquals("G", merged.getAltCalls().get(1));
    assertEquals(3, merged.getFormat(VcfUtils.FORMAT_GENOTYPE).size());
    assertEquals("0/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0));
    assertEquals("2/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(1));
    assertEquals(".", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(2));
    merged = mergedArr[1];
    assertEquals("chr1", merged.getSequenceName());
    assertEquals("AC", merged.getRefCall());
    assertEquals(50, merged.getOneBasedStart());
    assertEquals(1, merged.getAltCalls().size());
    assertEquals("AG", merged.getAltCalls().get(0));
    assertEquals(3, merged.getFormat(VcfUtils.FORMAT_GENOTYPE).size());
    assertEquals(".", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0));
    assertEquals(".", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(1));
    assertEquals("0/1", merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(2));
  }

  // Test behaviour in presence of unmergeable FORMAT fields
  public void testUnmergeableHandling() {
    final FormatField unmergeable = new FormatField("Unmergeable", MetaType.FLOAT, VcfNumber.ALTS, "Some number per alt");
    final VcfHeader h1 = new VcfHeader();
    h1.addCommonHeader();
    h1.addSampleName("sample1");
    h1.addFormatField(unmergeable);
    final VcfHeader h2 = new VcfHeader();
    h2.addCommonHeader();
    h2.addSampleName("sample2");
    h2.addFormatField(unmergeable);
    final VcfHeader mh = VcfHeaderMerge.mergeHeaders(h1, h2, null);
//    final Set<String> hardSet = VcfMerge.alleleBasedFormats(mh);
//    assertTrue(hardSet.contains(unmergeable.getId()));

    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh);

    // Alts have same set, but the ordering is different
    VcfRecord r1 = createRecord("chr1", 50, "A", 0, 1, "C", "G");
    r1.addFormatAndSample(unmergeable.getId(), "1.0");
    VcfRecord r2 = createRecord("chr1", 50, "A", 1, 2, "G", "C");
    r2.addFormatAndSample(unmergeable.getId(), "2.0,3.0");
    VcfRecord[] mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(2, mergedArr.length);
    assertTrue(mergedArr[0].hasFormat(unmergeable.getId()));
    assertTrue(mergedArr[1].hasFormat(unmergeable.getId()));

    // Alts have different alleles
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r1.addFormatAndSample(unmergeable.getId(), "1.0");
    r2 = createRecord("chr1", 50, "A", 1, 2, "G", "C");
    r2.addFormatAndSample(unmergeable.getId(), "2.0,3.0");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(2, mergedArr.length);
    assertTrue(mergedArr[0].hasFormat(unmergeable.getId()));
    assertTrue(mergedArr[1].hasFormat(unmergeable.getId()));

    // Alts have different alleles, but allow merge by dropping the FORMAT fields
    merger.setDropUnmergeable(true);
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, new VcfHeader[]{h1, h2});
    assertEquals(1, mergedArr.length);
    assertFalse(mergedArr[0].hasFormat(unmergeable.getId()));

    // Single record edge cases.
    // These are kind of dodgy records since they have redundant alleles, so allele normalization on these can cause problems
    // We just pass them through after reordering the samples into the order expected in the destination.
    merger.setDropUnmergeable(false);
    for (String rr : new String[]{"chr1\t100\t.\tg\ta,C,a\t.\tPASS\t.", "chr1\t100\t.\tg\ta,G\t.\tPASS\t.", "chr1\t100\t.\tg\tg\t.\tPASS\t.", "chr1\t100\t.\tg\ta,C,A\t.\tPASS\t."}) {
      r2 = VcfReaderTest.vcfLineToRecord(rr);
      r2.setNumberOfSamples(1);
      r2.addFormatAndSample(unmergeable.getId(), "1.0");
      mergedArr = merger.mergeRecords(new VcfRecord[]{r2}, new VcfHeader[] {h2});
      assertEquals(1, mergedArr.length);
      final VcfRecord mr = mergedArr[0];
      assertNotNull(mr);
      assertTrue(mr.hasFormat(unmergeable.getId()));
      assertEquals(2, mr.getNumberOfSamples());
      assertEquals(2, mr.getFormat(unmergeable.getId()).size());
      assertEquals(".", mr.getFormat(unmergeable.getId()).get(0));
      assertEquals("1.0", mr.getFormat(unmergeable.getId()).get(1));
    }
  }

  public void testMergeFormat() {
    //Testing the merging of records with FORMAT fields with a number equal to genotype or alternate allele count
    final VcfHeader h1 = new VcfHeader();
    h1.addSampleName("sample1");
    h1.addFormatField("ZZ", MetaType.INTEGER, new VcfNumber("A"), "Number on Alt Allele");
    h1.addFormatField("ZX", MetaType.INTEGER, new VcfNumber("G"), "Number on Genotype");
    final VcfHeader h2 = new VcfHeader();
    h2.addSampleName("sample2");
    h2.addFormatField("ZZ", MetaType.INTEGER, new VcfNumber("A"), "Number on Alt Allele");
    h2.addFormatField("ZX", MetaType.INTEGER, new VcfNumber("G"), "Number on Genotype");
    final VcfHeader[] heads = {h1, h2};
    final VcfHeader mh = new VcfHeader();
    mh.addSampleName("sample1");
    mh.addSampleName("sample2");
    mh.addFormatField("ZZ", MetaType.INTEGER, new VcfNumber("A"), "Number on Alt Allele");
    mh.addFormatField("ZX", MetaType.INTEGER, new VcfNumber("G"), "Number on Genotype");

    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh);

    VcfRecord r1 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ\t1/2:1,2");
    VcfRecord r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZX\t1/2:3,4,5");
    VcfRecord[] mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(1, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ:ZX\t1/2:1,2\t1/2:.:3,4,5", mergedArr[0].toString());

    r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tC,A\t.\tPASS\t.\tGT:ZX\t1/2:3,4,5");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(2, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ\t1/2:1,2\t.", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tG\tC,A\t.\tPASS\t.\tGT:ZX\t.\t1/2:3,4,5", mergedArr[1].toString());

    r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA\t.\tPASS\t.\tGT:ZZ\t1/1:3");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(2, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ\t1/2:1,2\t.", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tG\tA\t.\tPASS\t.\tGT:ZZ\t.\t1/1:3", mergedArr[1].toString());

    r1 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tGGG\tAAA\t.\tPASS\t.\tGT:ZX\t0/1:1,2,3");
    r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tGGG\tCCC\t.\tPASS\t.\tGT:ZX\t0/1:4,5,6");
    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(2, mergedArr.length);
    assertEquals("chr1\t100\t.\tGGG\tAAA\t.\tPASS\t.\tGT:ZX\t0/1:1,2,3\t.", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tGGG\tCCC\t.\tPASS\t.\tGT:ZX\t.\t0/1:4,5,6", mergedArr[1].toString());
  }

  public void testMergeInfo() {
    //Testing clearing of INFO fields with a number equal to genotype or alternate allele count
    final VcfHeader h1 = new VcfHeader();
    InfoField zz = new InfoField("ZZ", MetaType.INTEGER, new VcfNumber("A"), "Number on Alt Allele");
    InfoField zx = new InfoField("ZX", MetaType.INTEGER, new VcfNumber("A"), "Number on Genotype");
    h1.addSampleName("sample1");
    h1.addInfoField(zz);
    h1.addInfoField(zx);
    final VcfHeader h2 = new VcfHeader();
    h2.addSampleName("sample2");
    h2.addInfoField(zz);
    h2.addInfoField(zx);
    final VcfHeader[] heads = {h1, h2};
    final VcfHeader mh = new VcfHeader();
    mh.addSampleName("sample1");
    mh.addSampleName("sample2");
    mh.addInfoField(zz);
    mh.addInfoField(zx);

    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh);

    VcfRecord r1 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\tZZ=1,2\tGT\t1/2");

    VcfRecord r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\tZX=3,4,5\tGT\t1/2");  // No ALT change
    VcfRecord[] mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(1, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\tZZ=1,2\tGT\t1/2\t1/2", mergedArr[0].toString());
  }

  public void testIdMerge() {
    Diagnostic.setLogStream();
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId("b;c")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .addFilter("TEST1")
      .addFilter("TEST2")
      .setInfo("DP", "23")
      .setInfo("TEST", "45", "46", "47", "48")
      .addFormatAndSample("GT", "0/0")
      .addFormatAndSample("GQ", "100")
    ;
    final VcfRecord rec2 = new VcfRecord("chr1", 1209, "a");
    rec2.setId("a")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .addFilter("TEST1")
      .addFilter("TEST2")
      .setInfo("DP", "23")
      .setInfo("TEST", "45", "46", "47", "48")
      .addFormatAndSample("GT", "0/1")
      .addFormatAndSample("GQ", "95")
    ;
    final VcfHeader h1 = new VcfHeader();
    h1.addSampleName("sample1");
    final VcfHeader h2 = new VcfHeader();
    h2.addSampleName("sample2");
    final VcfRecord[] recs = {rec, rec2};

    final VcfHeader mh = VcfHeaderMerge.mergeHeaders(h1, h2, null);
    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh);

    final VcfRecord[] mergedArr = merger.mergeRecords(recs, new VcfHeader[]{h1, h2});
    assertEquals(1, mergedArr.length);
    final VcfRecord merged = mergedArr[0];
    assertEquals("b;c;a", merged.getId());
  }

  public void testPreserveDuplicateSamples() {
    final VcfHeader h1 = new VcfHeader();
    h1.addSampleName("sample1");
    final VcfHeader h2 = new VcfHeader();
    h2.addSampleName("sample1");
    final VcfHeader mh = VcfHeaderMerge.mergeHeaders(h1, h2, null);
    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh).setDropUnmergeable(true);

    final VcfHeader[] heads = {h1, h2};
    final VcfRecord r1 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA\t.\tPASS\t.\tGT\t1/1");
    final VcfRecord r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tC\t.\tPASS\t.\tGT\t0/1");

    VcfRecord[] mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(1, mergedArr.length);
    // All alleles have nucleotide case normalized
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT\t1/1", mergedArr[0].toString());

    merger.setDropUnmergeable(false);

    mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2}, heads);
    assertEquals(2, mergedArr.length);
    // All alleles have nucleotide case normalized
    assertEquals("chr1\t100\t.\tG\tA\t.\tPASS\t.\tGT\t1/1", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tG\tC\t.\tPASS\t.\tGT\t0/1", mergedArr[1].toString());
  }

  public void testCaseHandling() {
    final VcfHeader h1 = new VcfHeader();
    final VcfHeader h2 = new VcfHeader();
    final VcfHeader h3 = new VcfHeader();
    VcfHeader mh = VcfHeaderMerge.mergeHeaders(h1, h2, null);
    mh = VcfHeaderMerge.mergeHeaders(mh, h3, null);
    final VcfRecordMerger merger = new VcfRecordMerger().setHeader(mh);

    final VcfHeader[] heads = {h1, h2, h3};
    final VcfRecord r1 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tg\ta,C\t.\tPASS\t.");
    final VcfRecord r2 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tc,t\t.\tPASS\t.");
    final VcfRecord r3 = VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tc]Chr1:123]g\t.\tPASS\t.");

    final VcfRecord[] mergedArr = merger.mergeRecords(new VcfRecord[]{r1, r2, r3}, heads);
    assertEquals(1, mergedArr.length);
    // All alleles have nucleotide case normalized
    assertEquals("chr1\t100\t.\tG\tA,C,T,C]Chr1:123]G\t.\tPASS\t.", mergedArr[0].toString());
  }
}
