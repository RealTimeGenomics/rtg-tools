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
import com.rtg.util.PosteriorUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class VariantTest extends TestCase {

  private static final RocSortValueExtractor DEFAULT_EXTRACTOR = RocScoreField.FORMAT.getExtractor(VcfUtils.FORMAT_GENOTYPE_QUALITY, RocSortOrder.DESCENDING);
  static Variant createVariant(VcfRecord rec, int sampleNo, RocSortValueExtractor extractor) {
    return createVariant(rec, 0, sampleNo, extractor);
  }
  static Variant createVariant(VcfRecord rec, int id, int sampleNo, RocSortValueExtractor extractor) {
    return new Variant.Factory(sampleNo, extractor).variant(rec, id);
  }
  static Variant createVariant(String var, int sampleNo, RocSortValueExtractor extractor) {
    final String vartab = var.replaceAll(" ", TAB);
    return createVariant(VcfReader.vcfLineToRecord(vartab), sampleNo, extractor);
  }
  static Variant createVariant(String var) {
    return createVariant(var, 0, RocSortValueExtractor.NULL_EXTRACTOR);
  }

  public void test() {
    final String vcf = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
    final VcfRecord rec = VcfReader.vcfLineToRecord(vcf);
    final Variant v = createVariant(rec, 0, DEFAULT_EXTRACTOR);
    assertEquals(22, v.getStart());
    assertTrue(Double.isNaN(v.getSortValue()));
    assertEquals("someKindOfName", rec.getSequenceName());
  }

  static final String ORDER_FIRST = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  static final String ORDER_SECOND = "someKindOfName" + TAB + "24" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  static final String ORDER_THIRD = "someOtherKindOfName" + TAB + "19" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  static final String ORDER_FOURTH = "someOtherKindOfName" + TAB + "55" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";

  public void testComparator() throws IOException {
    final ArrayList<Variant> allDetections = new ArrayList<>();
    final Variant dv2 = createVariant(ORDER_SECOND);
    final Variant dv4 = createVariant(ORDER_FOURTH);
    final Variant dv1 = createVariant(ORDER_FIRST);
    final Variant dv3 = createVariant(ORDER_THIRD);
    allDetections.add(dv2);
    allDetections.add(dv4);
    allDetections.add(dv1);
    allDetections.add(dv3);
    final Variant[] variantArray = allDetections.toArray(new Variant[allDetections.size()]);
    Arrays.sort(variantArray, new SequenceNameLocusComparator());
    assertEquals("someKindOfName:23-24 (T)", variantArray[0].toString());
    assertEquals("someKindOfName:24-25 (T)", variantArray[1].toString());
    assertEquals("someOtherKindOfName:19-20 (T)", variantArray[2].toString());
    assertEquals("someOtherKindOfName:55-56 (T)", variantArray[3].toString());
    final SequenceNameLocusComparator comparator = new SequenceNameLocusComparator();
    assertEquals(0, comparator.compare(dv1, dv1));
    assertEquals(-1, comparator.compare(dv1, dv2));
    assertEquals(1, comparator.compare(dv2, dv1));
    final Variant insert = createVariant("t 1 . G GAC 0.0 PASS . GT 1/1");
    final Variant snp = createVariant("t 2 . G T 0.0 PASS . GT 1/1");
    assertEquals(-1, insert.compareTo(snp));
    assertEquals(1, snp.compareTo(insert));
    assertEquals(0, insert.compareTo(insert));
  }

  static final String SNP_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testSnpConstruction() throws Exception {
    final Variant variant = createVariant(SNP_LINE, 0, DEFAULT_EXTRACTOR);
    assertEquals(22, variant.getStart());
    assertEquals(12.8, variant.getSortValue(), 0.1);
    assertEquals(1, variant.nt(0).length);
    assertEquals(4, variant.nt(0)[0]);
    assertNull(variant.nt(-1));
    try {
      variant.nt(1);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }
  }

  static final String SNP_LINE2 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T,C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/2:4:0.02:12.8";
  static final String SNP_LINE3 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "0/1:4:0.02:12.8";
  static final String SNP_LINE4 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "./1:4:0.02:12.8";
  public void testHeterozygousSnpConstruction() throws Exception {
    Variant variant = createVariant(SNP_LINE2);
    // Normal het call 1/2
    assertEquals(22, variant.getStart());
    assertEquals(1, variant.nt(0).length);
    assertEquals(4, variant.nt(0)[0]);
    assertEquals(1, variant.nt(1).length);
    assertEquals(2, variant.nt(1)[0]);
    OrientedVariant[] pos = variant.orientations();
    assertEquals(2, pos.length);
    assertTrue(pos[0].isAlleleA());
    assertFalse(pos[1].isAlleleA());
    assertTrue(pos[0].isHeterozygous());
    assertTrue(pos[1].isHeterozygous());
    assertEquals(0, pos[0].alleleId()); // REF allele doesn't get counted in ids
    assertEquals(1, pos[1].alleleId());

    // Test half-call  ./1, no squash
    variant = createVariant(SNP_LINE4);
    assertEquals(1, variant.nt(0).length);
    assertEquals(0, variant.nt(0)[0]);
    assertEquals(4, variant.nt(1)[0]);
  }

  static final String INSERT_LINE = "someKindOfName" + TAB + "22" + TAB + "." + TAB + "A" + TAB + "AACT" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testInsertConstruction() throws Exception {
    final Variant variant = createVariant(INSERT_LINE);
    assertEquals(22, variant.getStart());
    assertEquals(3, variant.nt(0).length);
    assertEquals(2, variant.nt(0)[1]);
  }


  static final String DELETION_LINE = "someKindOfName" + TAB + "22" + TAB + "." + TAB + "GAGT" + TAB + "G" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testDeletionConstructor() throws Exception {
    final Variant variant = createVariant(DELETION_LINE);
    assertEquals(22, variant.getStart());
  }
  static final String MNP_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "AGT" + TAB + "CTC" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testMnpConstructor() throws Exception {
    final Variant variant = createVariant(MNP_LINE);
    assertEquals(22, variant.getStart());

    assertEquals(3, variant.nt(0).length);
    assertEquals(2, variant.nt(0)[0]);
    assertEquals(4, variant.nt(0)[1]);
    try {
      variant.nt(1);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }

  }

  static final String UNCHANGED_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/1:4:0.02:12.8";
  public void testUnchangedConstructor() throws Exception {
    final Variant variant = createVariant(UNCHANGED_LINE);
    assertEquals(22, variant.getStart());
    assertTrue(Arrays.equals(DnaUtils.encodeString("C"), variant.nt(0)));
  }

  static final String SHORT_LINE = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "C" + TAB + "0.0" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";
  public void testShortConstructor() throws Exception {
    final Variant variant = createVariant(SHORT_LINE);
    assertEquals(22, variant.getStart());
    assertTrue(Arrays.equals(DnaUtils.encodeString("C"), variant.nt(0)));
  }

  static final String TAIL = TAB + "." + TAB + "A" + TAB + "C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT" + TAB + "1/1";

  public void testOrdering() throws Exception {
    final Variant a1a = createVariant("a" + TAB + 1 + TAIL);
    final Variant a1b = createVariant("a" + TAB + 1 + TAIL);
    final Variant a2 = createVariant("a" + TAB + 2 + TAIL);
    final Variant b = createVariant("b" + TAB + 2 + TAIL);
    final Variant c = createVariant("c" + TAB + 2 + TAIL);
    TestUtils.testOrder(new Variant[][]{{a1a, a1b}, {a2}, {b}, {c}}, true);
  }

  public void testEquals() throws Exception {
    final Variant a1a = createVariant("a" + TAB + 1 + TAIL);
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
    final Variant v = createVariant(line);
    assertEquals(2179, v.getStart());
    assertEquals(2180, v.getEnd());
    checkArray(new byte[] {4}, v.nt(0));
    checkArray(new byte[] {3}, v.nt(1));
    final String line2 = "simulatedSequence1 2180 . C G,T 31.0 PASS . GT 1/2".replaceAll(" ", "\t");
    final Variant v2 = createVariant(line2);
    assertEquals(2179, v2.getStart());
    assertEquals(2180, v2.getEnd());
    checkArray(new byte[] {3}, v2.nt(0));
    checkArray(new byte[] {4}, v2.nt(1));
  }

  public void testAllFieldsPresent() throws Exception {
    final String line = ("simulatedSequence1 2180 . C G,T " + PosteriorUtils.phredIfy(45.8) + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/2:35:0.697:31.0").replaceAll(" ", "\t");
    final Variant v = createVariant(line, 0, DEFAULT_EXTRACTOR);
    assertEquals(2179, v.getStart());
    assertEquals(2180, v.getEnd());
    assertEquals(31.0, v.getSortValue(), 0.1);
    checkArray(new byte[] {3}, v.nt(0));
    checkArray(new byte[]{4}, v.nt(1));
  }

  public void testMissingGT() throws Exception {
    final String line = "simulatedSequence1 2180 . C A 0.0 PASS . GT .".replaceAll(" ", "\t");
    assertNull(createVariant(line)); // Current default factory skips those with missing GT
  }

}
