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

package com.rtg.vcf.eval;

import static com.rtg.util.StringUtils.TAB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.util.PosteriorUtils;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class DetectedVariantTest extends TestCase {

  private static final RocSortValueExtractor DEFAULT_EXTRACTOR = RocScoreField.FORMAT.getExtractor(VcfUtils.FORMAT_GENOTYPE_QUALITY, RocSortOrder.DESCENDING);

  public void test() {
    final String vcf = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
    final VcfRecord rec = VcfReader.vcfLineToRecord(vcf);
    final DetectedVariant v = new DetectedVariant(rec, 0, RocSortValueExtractor.NULL_EXTRACTOR, false);
    assertEquals(22, v.getStart());
    assertEquals(0.0, v.getSortValue());
    assertEquals("someKindOfName", rec.getSequenceName());
  }

  static final String ORDER_FIRST = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  static final String ORDER_SECOND = "someKindOfName" + TAB + "24" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  static final String ORDER_THIRD = "someOtherKindOfName" + TAB + "19" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  static final String ORDER_FOURTH = "someOtherKindOfName" + TAB + "55" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";

  private DetectedVariant getDetectedVariant(String var) {
    return getDetectedVariant(var, false);
  }
  private DetectedVariant getDetectedVariant(String var, boolean squash) {
    final String vartab = var.replaceAll(" ", TAB);
    return new DetectedVariant(VcfReader.vcfLineToRecord(vartab), 0, DEFAULT_EXTRACTOR, squash);
  }

  public void testComparator() throws IOException {
    final ArrayList<DetectedVariant> allDetections = new ArrayList<>();
    final DetectedVariant dv2 = getDetectedVariant(ORDER_SECOND);
    final DetectedVariant dv4 = getDetectedVariant(ORDER_FOURTH);
    final DetectedVariant dv1 = getDetectedVariant(ORDER_FIRST);
    final DetectedVariant dv3 = getDetectedVariant(ORDER_THIRD);
    allDetections.add(dv2);
    allDetections.add(dv4);
    allDetections.add(dv1);
    allDetections.add(dv3);
    final DetectedVariant[] variantArray = allDetections.toArray(new DetectedVariant[allDetections.size()]);
    Arrays.sort(variantArray, new SequenceNameLocusComparator());
    assertEquals("someKindOfName:23-24 (T)", variantArray[0].toString());
    assertEquals("someKindOfName:24-25 (T)", variantArray[1].toString());
    assertEquals("someOtherKindOfName:19-20 (T)", variantArray[2].toString());
    assertEquals("someOtherKindOfName:55-56 (T)", variantArray[3].toString());
    final SequenceNameLocusComparator comparator = new SequenceNameLocusComparator();
    assertEquals(0, comparator.compare(dv1, dv1));
    assertEquals(-1, comparator.compare(dv1, dv2));
    assertEquals(1, comparator.compare(dv2, dv1));
    final DetectedVariant insert = getDetectedVariant("t 1 . G GAC 0.0 PASS . GT 1/1");
    final DetectedVariant snp = getDetectedVariant("t 2 . G T 0.0 PASS . GT 1/1");
    assertEquals(-1, insert.compareTo(snp));
    assertEquals(1, snp.compareTo(insert));
    assertEquals(0, insert.compareTo(insert));
  }

  static final String SNP_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testSnpConstruction() throws Exception {
    final DetectedVariant variant = getDetectedVariant(SNP_LINE);
    assertEquals(22, variant.getStart());
    assertEquals(12.8, variant.getSortValue(), 0.1);
    assertEquals(1, variant.nt(true).length);
    assertEquals(4, variant.nt(true)[0]);
    assertNull(variant.nt(false));
  }

  static final String SNP_LINE2 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T,C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/2:4:0.02:12.8";
  static final String SNP_LINE3 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "0/1:4:0.02:12.8";
  static final String SNP_LINE4 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "./1:4:0.02:12.8";
  public void testHeterozygousSnpConstruction() throws Exception {
    DetectedVariant variant = getDetectedVariant(SNP_LINE2);
    assertEquals(22, variant.getStart());
    assertEquals(12.8, variant.getSortValue(), 0.1);
    assertEquals(1, variant.nt(true).length);
    assertEquals(4, variant.nt(true)[0]);
    assertEquals(1, variant.nt(false).length);
    assertEquals(2, variant.nt(false)[0]);

    // Test half-call  ./1, no squash
    variant = getDetectedVariant(SNP_LINE4);
    assertEquals(1, variant.nt(true).length);
    assertEquals(0, variant.nt(true)[0]);
    assertEquals(4, variant.nt(false)[0]);

    // Test squashing ploidy  1/2 -> 2/2
    variant = getDetectedVariant(SNP_LINE2, true);
    assertEquals(1, variant.nt(true).length);
    assertEquals(2, variant.nt(true)[0]);
    assertNull(variant.nt(false));

    // Test squashing ploidy  0/1 -> 1/1
    variant = getDetectedVariant(SNP_LINE3, true);
    assertEquals(1, variant.nt(true).length);
    assertEquals(4, variant.nt(true)[0]);
    assertNull(variant.nt(false));

    // Test squashing ploidy  ./1 -> 1/1
    variant = getDetectedVariant(SNP_LINE4, true);
    assertEquals(1, variant.nt(true).length);
    assertEquals(4, variant.nt(true)[0]);
    assertNull(variant.nt(false));

  }

  static final String INSERT_LINE = "someKindOfName" + TAB + "22" + TAB + "." + TAB + "A" + TAB + "AACT" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testInsertConstruction() throws Exception {
    final DetectedVariant variant = getDetectedVariant(INSERT_LINE);
    assertEquals(22, variant.getStart());
    assertEquals(12.8, variant.getSortValue(), 0.1);
  }


  static final String DELETION_LINE = "someKindOfName" + TAB + "22" + TAB + "." + TAB + "GAGT" + TAB + "G" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testDeletionConstructor() throws Exception {
    final DetectedVariant variant = getDetectedVariant(DELETION_LINE);
    assertEquals(22, variant.getStart());
    assertEquals(12.8, variant.getSortValue(), 0.1);

  }
  static final String MNP_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "AGT" + TAB + "CTC" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testMnpConstructor() throws Exception {
    final DetectedVariant variant = getDetectedVariant(MNP_LINE);
    assertEquals(22, variant.getStart());
    assertEquals(12.8, variant.getSortValue(), 1);

    assertEquals(3, variant.nt(true).length);
    assertEquals(2, variant.nt(true)[0]);
    assertEquals(4, variant.nt(true)[1]);
    assertNull(variant.nt(false));
  }

  static final String UNCHANGED_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testUnchangedConstructor() throws Exception {
    final DetectedVariant variant = getDetectedVariant(UNCHANGED_LINE);
    assertEquals(22, variant.getStart());
    assertTrue(Arrays.equals(DnaUtils.encodeString("C"), variant.nt(true)));
    assertEquals(12.8, variant.getSortValue(), 0.1);
  }

  static final String SHORT_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "C" + TAB + "0.0" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  public void testShortConstructor() throws Exception {
    final DetectedVariant variant = getDetectedVariant(SHORT_LINE);
    assertEquals(22, variant.getStart());
    assertTrue(Arrays.equals(DnaUtils.encodeString("C"), variant.nt(true)));
    assertTrue(Double.isNaN(variant.getSortValue()));
  }

  static final String TAIL = TAB + "." + TAB + "A" + TAB + "C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";

  public void testOrdering() throws Exception {
    final DetectedVariant a1a = getDetectedVariant("a" + TAB + 1 + TAIL);
    final DetectedVariant a1b = getDetectedVariant("a" + TAB + 1 + TAIL);
    final DetectedVariant a2 = getDetectedVariant("a" + TAB + 2 + TAIL);
    final DetectedVariant b = getDetectedVariant("b" + TAB + 2 + TAIL);
    final DetectedVariant c = getDetectedVariant("c" + TAB + 2 + TAIL);
    TestUtils.testOrder(new DetectedVariant[][] {{a1a, a1b}, {a2}, {b}, {c}}, true);
  }

  public void testEquals() throws Exception {
    final DetectedVariant a1a = getDetectedVariant("a" + TAB + 1 + TAIL);
    assertFalse(a1a.equals(null));
    assertFalse(a1a.equals(new Object()));
  }


  private void checkArray(byte[] a, byte[] b) {
    final String fail = "expected <" + Arrays.toString(a) + "> but was :<" + Arrays.toString(b) + ">";
    assertEquals(fail, a.length, b.length);
    for (int i = 0; i < a.length; i++) {
      assertEquals(fail, a[i], b[i]);
    }
  }

  public void testVariantInterface() throws Exception {
    final String line = "simulatedSequence1\t2180\t.\tC\tT,G\t0.0" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/2";
    final DetectedVariant v = getDetectedVariant(line);
    assertEquals(2179, v.getStart());
    assertEquals(2180, v.getEnd());
    checkArray(new byte[] {4}, v.ntAlleleA());
    checkArray(new byte[] {3}, v.ntAlleleB());
    final String line2 = "simulatedSequence1 2180 . C G,T 31.0 PASS . GT 1/2".replaceAll(" ", "\t");
    final DetectedVariant v2 = getDetectedVariant(line2);
    assertEquals(2179, v2.getStart());
    assertEquals(2180, v2.getEnd());
    checkArray(new byte[] {3}, v2.ntAlleleA());
    checkArray(new byte[] {4}, v2.ntAlleleB());
  }

  public void testAllFieldsPresent() throws Exception {
    final String line = ("simulatedSequence1 2180 . C G,T " + PosteriorUtils.phredIfy(45.8) + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/2:35:0.697:31.0").replaceAll(" ", "\t");
    final DetectedVariant v = getDetectedVariant(line);
    assertEquals(2179, v.getStart());
    assertEquals(2180, v.getEnd());
    assertEquals(31.0, v.getSortValue(), 0.1);
    checkArray(new byte[] {3}, v.ntAlleleA());
    checkArray(new byte[] {4}, v.ntAlleleB());
  }

  public void testComplexHandling() throws Exception {
    final String line = "simulatedSequence1 2180 . C A 0.0" + TAB + "RC" + TAB + "." + TAB + "GT" + TAB + "0/0".replaceAll(" ", "\t");
    final DetectedVariant v = getDetectedVariant(line);
    assertEquals(2179, v.getStart());
  }

  public void testMissingGT() throws Exception {
    final String line = "simulatedSequence1 2180 . C A 0.0 PASS . GT .".replaceAll(" ", "\t");
    final DetectedVariant v = getDetectedVariant(line);
    assertEquals(2179, v.getStart());
  }

}
