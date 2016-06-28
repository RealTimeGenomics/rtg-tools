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

import static com.rtg.util.StringUtils.TAB;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.rtg.util.StringUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfHeaderMerge;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 */
public class VcfRecordTest extends TestCase {

  private static final String VCF_RECORD_FORMAT = "%1$s\t%2$d\t.\t%3$s\t%4$s\t.\tPASS\t.\tGT:E1:E2\t%5$d/%6$d:.:.";

  private static VcfRecord createRecord(String chrom, int pos, String ref, int gt1, int gt2, String... alts) {
    final String altsStr = StringUtils.implode(alts, ",", false);
    return VcfReader.vcfLineToRecord(String.format(VCF_RECORD_FORMAT, chrom, pos, ref, altsStr, gt1, gt2));
  }

  private static final String VCF_RECORD_FORMAT_MISSING = "%1$s\t%2$d\t.\t%3$s\t.\t.\tPASS\t.\tGT:E1:E2\t.";

  private static VcfRecord createRecord(String chrom, int pos, String ref) {
    return VcfReader.vcfLineToRecord(String.format(VCF_RECORD_FORMAT_MISSING, chrom, pos, ref));
  }

  public VcfRecordTest(String name) {
    super(name);
  }

  public void test() {
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .addFilter("TEST1")
      .addFilter("TEST2")
      .addInfo("DP", "23")
      .addInfo("TEST", "45", "46", "47", "48")
      .setNumberOfSamples(2)
      .addFormatAndSample("GT", "0/0")
      .addFormatAndSample("GT", "0/1")
      .addFormatAndSample("GQ", "100")
      .addFormatAndSample("GQ", "95")
    ;

    assertEquals("chr1", rec.getSequenceName());
    assertEquals(1210, rec.getOneBasedStart());
    assertEquals(".", rec.getId());
    assertEquals("a", rec.getRefCall());
    assertEquals("c", rec.getAltCalls().get(0));
    assertEquals("t", rec.getAltCalls().get(1));
    assertEquals("TEST1", rec.getFilters().get(0));
    assertEquals("TEST2", rec.getFilters().get(1));
    assertEquals("12.8", rec.getQuality());
    assertEquals("23", rec.getInfo().get("DP").iterator().next());
    final Iterator<String> iter = rec.getInfo().get("TEST").iterator();
    assertEquals("45", iter.next());
    assertEquals("46", iter.next());
    assertEquals("47", iter.next());
    assertEquals("48", iter.next());
    assertEquals("0/0", rec.getFormat("GT").get(0));
    assertEquals("0/1", rec.getFormat("GT").get(1));
    assertEquals("100", rec.getFormat("GQ").get(0));
    assertEquals("95", rec.getFormat("GQ").get(1));
    final String line = ""
      + "chr1" + TAB
      + "1210" + TAB
      + "." + TAB
      + "a" + TAB
      + "c,t" + TAB
      + "12.8" + TAB
      + "TEST1;TEST2" + TAB
      + "DP=23;TEST=45,46,47,48" + TAB
      + "GT:GQ" + TAB
      + "0/0:100" + TAB
      + "0/1:95";
    assertEquals(line, rec.toString());
  }

  public void testErrors() {
    try {
      new VcfRecord("chr1", 0, "a").addInfo("x", "1").addInfo("x", "2");
    } catch (final IllegalArgumentException ex) {
      assertEquals("key already present in the map key = x", ex.getMessage());
    }
  }


  public void testError2() {
    try {
      final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
      rec.setNumberOfSamples(2)
      .setId(".")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .addFilter("TEST1")
      .addFilter("TEST2")
      .addInfo("DP", "23")
      .addInfo("TEST", "45,46,47,48")
      .addFormatAndSample("GT", "0/0")
      .addFormatAndSample("GT", "0/1")
      .addFormatAndSample("GQ", "100")
      .toString()
      ;
    } catch (final IllegalStateException ex) {
      assertEquals("not enough data for all samples, first size = 2, current key = GQ count = 1", ex.getMessage());
    }
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
    VcfRecord[] mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, VcfMerge.alleleBasedFormats(mh), true);
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
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, VcfMerge.alleleBasedFormats(mh), true);
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
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(2, mergedArr.length);
    checkRecord(mergedArr[0], r1, new String[] {"0/1", "."});
    checkRecord(mergedArr[1], r2, new String[] {".", "1/2"});

    // Outputs separate records due to different REF
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "AC", 1, 1, "AC"); //this tests the repair of a buggy case
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(2, mergedArr.length);
    checkRecord(mergedArr[0], r1, new String[] {"0/1", "."});
    checkRecord(mergedArr[1], r2, new String[0], new String[] {".", "0/0"});

    //test blank filling
    mh.addSampleName("sample3");
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "AC", 1, 1, "AC");
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(2, mergedArr.length);
    checkRecord(mergedArr[0], r1, new String[] {"0/1", "."});
    checkRecord(mergedArr[1], r2, new String[0], new String[] {".", "0/0"});

    //test reading of GT containing missing values
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r2 = createRecord("chr1", 50, "A");
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, VcfMerge.alleleBasedFormats(mh), true);
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
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2, r3}, new VcfHeader[] {h1, h2, h3}, mh, VcfMerge.alleleBasedFormats(mh), true);
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
    final Set<String> hardSet = VcfMerge.alleleBasedFormats(mh);
    assertTrue(hardSet.contains(unmergeable.getId()));

    // Alts have same set, but the ordering is different
    VcfRecord r1 = createRecord("chr1", 50, "A", 0, 1, "C", "G");
    r1.addFormatAndSample(unmergeable.getId(), "1.0");
    VcfRecord r2 = createRecord("chr1", 50, "A", 1, 2, "G", "C");
    r2.addFormatAndSample(unmergeable.getId(), "2.0,3.0");
    VcfRecord[] mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, hardSet, true);
    assertEquals(2, mergedArr.length);
    assertTrue(mergedArr[0].hasFormat(unmergeable.getId()));
    assertTrue(mergedArr[1].hasFormat(unmergeable.getId()));

    // Alts have different alleles
    r1 = createRecord("chr1", 50, "A", 0, 1, "C");
    r1.addFormatAndSample(unmergeable.getId(), "1.0");
    r2 = createRecord("chr1", 50, "A", 1, 2, "G", "C");
    r2.addFormatAndSample(unmergeable.getId(), "2.0,3.0");
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, hardSet, true);
    assertEquals(2, mergedArr.length);
    assertTrue(mergedArr[0].hasFormat(unmergeable.getId()));
    assertTrue(mergedArr[1].hasFormat(unmergeable.getId()));

    // Alts have different alleles, but allow merge by dropping the FORMAT fields
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, new VcfHeader[] {h1, h2}, mh, hardSet, false);
    assertEquals(1, mergedArr.length);
    assertFalse(mergedArr[0].hasFormat(unmergeable.getId()));
  }

  private void checkRecord(VcfRecord merged, VcfRecord exp, String[] genotype) {
    final List<String> altCalls = exp.getAltCalls();
    checkRecord(merged, exp.getSequenceName(), exp.getRefCall(), exp.getOneBasedStart(), altCalls.toArray(new String[altCalls.size()]), genotype);
  }
  private void checkRecord(VcfRecord merged, VcfRecord exp, String[] altCalls, String[] genotype) {
    checkRecord(merged, exp.getSequenceName(), exp.getRefCall(), exp.getOneBasedStart(), altCalls, genotype);
  }

  private void checkRecord(VcfRecord merged, String chr, String refCall, int pos, String[] altCalls, String[] gts) {
    assertEquals(chr, merged.getSequenceName());
    assertEquals(refCall, merged.getRefCall());
    assertEquals(pos, merged.getOneBasedStart());
    for (int i = 0; i < altCalls.length; i++) {
      assertEquals(altCalls[i], merged.getAltCalls().get(i));
    }
    for (int i = 0; i < gts.length; i++) {
      assertEquals(gts[i], merged.getFormat(VcfUtils.FORMAT_GENOTYPE).get(i));
    }
  }

  public void testIdMerge() {
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId("b;c")
    .setQuality("12.8")
    .addAltCall("c")
    .addAltCall("t")
    .addFilter("TEST1")
    .addFilter("TEST2")
    .addInfo("DP", "23")
    .addInfo("TEST", "45", "46", "47", "48")
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
    .addInfo("DP", "23")
    .addInfo("TEST", "45", "46", "47", "48")
    .addFormatAndSample("GT", "0/1")
    .addFormatAndSample("GQ", "95")
    ;
    final VcfHeader h1 = new VcfHeader();
    h1.addSampleName("sample1");
    final VcfRecord[] recs = {rec, rec2};
    final VcfRecord[] mergedArr = VcfRecord.mergeRecords(recs, new VcfHeader[] {h1, h1}, h1, VcfMerge.alleleBasedFormats(h1), true);
    assertEquals(1, mergedArr.length);
    final VcfRecord merged = mergedArr[0];
    assertEquals("b;c;a", merged.getId());
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

    VcfRecord r1 = VcfReader.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ\t1/2:1,2");
    VcfRecord r2 = VcfReader.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZX\t1/2:3,4,5");
    VcfRecord[] mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, heads, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(1, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ:ZX\t1/2:1,2\t1/2:.:3,4,5", mergedArr[0].toString());

    r2 = VcfReader.vcfLineToRecord("chr1\t100\t.\tG\tC,A\t.\tPASS\t.\tGT:ZX\t1/2:3,4,5");
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, heads, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(2, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ\t1/2:1,2\t.", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tG\tC,A\t.\tPASS\t.\tGT:ZX\t.\t1/2:3,4,5", mergedArr[1].toString());

    r2 = VcfReader.vcfLineToRecord("chr1\t100\t.\tG\tA\t.\tPASS\t.\tGT:ZZ\t1/1:3");
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, heads, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(2, mergedArr.length);
    assertEquals("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT:ZZ\t1/2:1,2\t.", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tG\tA\t.\tPASS\t.\tGT:ZZ\t.\t1/1:3", mergedArr[1].toString());

    r1 = VcfReader.vcfLineToRecord("chr1\t100\t.\tGGG\tAAA\t.\tPASS\t.\tGT:ZX\t0/1:1,2,3");
    r2 = VcfReader.vcfLineToRecord("chr1\t100\t.\tGGG\tCCC\t.\tPASS\t.\tGT:ZX\t0/1:4,5,6");
    mergedArr = VcfRecord.mergeRecords(new VcfRecord[] {r1, r2}, heads, mh, VcfMerge.alleleBasedFormats(mh), true);
    assertEquals(2, mergedArr.length);
    assertEquals("chr1\t100\t.\tGGG\tAAA\t.\tPASS\t.\tGT:ZX\t0/1:1,2,3\t.", mergedArr[0].toString());
    assertEquals("chr1\t100\t.\tGGG\tCCC\t.\tPASS\t.\tGT:ZX\t.\t0/1:4,5,6", mergedArr[1].toString());
  }

  public void testSetMethods() {
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(3);
    rec.padFormatAndSample("PAD");
    assertNull(rec.getFormat("PAD"));
    rec.setFormatAndSample("PAD", "DAP", 1);
    rec.padFormatAndSample("PAD");
    assertEquals(3, rec.getFormat("PAD").size());
    assertEquals(".", rec.getFormat("PAD").get(0));
    assertEquals("DAP", rec.getFormat("PAD").get(1));
    assertEquals(".", rec.getFormat("PAD").get(2));
    rec.setFormatAndSample("PAD", "DAPDAP", 1);
    assertEquals("DAPDAP", rec.getFormat("PAD").get(1));
    rec.setInfo("INF", "VAL1", "VAL2");
    assertEquals(2, rec.getInfo().get("INF").size());
    assertEquals("VAL1", rec.getInfo().get("INF").get(0));
    assertEquals("VAL2", rec.getInfo().get("INF").get(1));
    rec.setInfo("INF", "VAL3", "VAL2", "VAL1");
    assertEquals(3, rec.getInfo().get("INF").size());
    assertEquals("VAL3", rec.getInfo().get("INF").get(0));
    assertEquals("VAL2", rec.getInfo().get("INF").get(1));
    assertEquals("VAL1", rec.getInfo().get("INF").get(2));
  }
  public void testFilterHackery() {
    // Test that if a filter is added then PASS is removed
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addFilter("PASS");
    assertEquals(Arrays.asList("PASS"), rec.getFilters());
    rec.addFilter("a1.0");
    assertEquals(Arrays.asList("a1.0"), rec.getFilters());
  }
}
