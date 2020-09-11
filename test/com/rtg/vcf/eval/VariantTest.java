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

import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.vcf.VcfReaderTest;
import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 */
public class VariantTest extends TestCase {

  static VcfRecord createRecord(String var) {
    return VcfReaderTest.vcfLineToRecord(var.replaceAll(" +", TAB));
  }

  static Variant createVariant(VcfRecord rec, int sampleNo) {
    return createVariant(rec, 0, sampleNo);
  }
  static Variant createVariant(VcfRecord rec, int id, int sampleNo) {
    try {
      return new VariantFactory.SampleVariants(sampleNo, true).variant(rec, id);
    } catch (SkippedVariantException e) {
      return null;
    }
  }

  static Variant createVariant(String var) {
    return createVariant(var, 0, 0);
  }
  static Variant createVariant(String var, int id) {
    return createVariant(var, id, 0);
  }
  static Variant createVariant(String var, int id, int sampleNo) {
    return createVariant(createRecord(var), id, sampleNo);
  }

  public void test() {
    final String vcf = "someKindOfName 23 . A T 12.8 PASS . GT 1/1";
    final Variant v = createVariant(vcf);
    assertEquals(22, v.getStart());
  }

  static final String ORDER_FIRST = "someKindOfName 23 . A T 12.8 PASS . GT 1/1";
  static final String ORDER_SECOND = "someKindOfName 24 . A T 12.8 PASS . GT 1/1";
  static final String ORDER_THIRD = "someOtherKindOfName 19 . A T 12.8 PASS . GT 1/1";
  static final String ORDER_FOURTH = "someOtherKindOfName 55 . A T 12.8 PASS . GT 1/1";

  public void testComparator() {
    final ArrayList<Variant> allDetections = new ArrayList<>();
    final Variant dv2 = createVariant(ORDER_SECOND);
    final Variant dv4 = createVariant(ORDER_FOURTH);
    final Variant dv1 = createVariant(ORDER_FIRST);
    final Variant dv3 = createVariant(ORDER_THIRD);
    allDetections.add(dv2);
    allDetections.add(dv4);
    allDetections.add(dv1);
    allDetections.add(dv3);
    final Variant[] variantArray = allDetections.toArray(new Variant[0]);
    Arrays.sort(variantArray, new SequenceNameLocusComparator());
    assertEquals("someKindOfName:23-24 (T:T)", variantArray[0].toString());
    assertEquals("someKindOfName:24-25 (T:T)", variantArray[1].toString());
    assertEquals("someOtherKindOfName:19-20 (T:T)", variantArray[2].toString());
    assertEquals("someOtherKindOfName:55-56 (T:T)", variantArray[3].toString());
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

  public void testTriploid() throws SkippedVariantException {
    final VcfRecord rec = createRecord("someKindOfName 23 . A T,G 12.8 PASS . GT:DP:RE:GQ 1/1/2:4:0.02:12.8");
    try {
      new VariantFactory.SampleVariants(0, true, 2).variant(rec, 0);
      fail("Expected failure to process triploid call");
    } catch (SkippedVariantException ignored) {
    }
    GtIdVariant v = new VariantFactory.SampleVariants(0, true, 3).variant(rec, 0);
    assertEquals(3, v.ploidy());
    assertEquals("someKindOfName:23-24 (T:T:G)", v.toString());
  }

  static final String SNP_LINE = "someKindOfName 23 . A T 12.8 PASS . GT:DP:RE:GQ 1/1:4:0.02:12.8";
  public void testSnpConstruction() {
    final Variant variant = createVariant(SNP_LINE);
    variant.trimAlleles(); // To remove unused ref allele
    assertEquals(22, variant.getStart());
    assertNull(variant.allele(-1));
    assertNull(variant.allele(0));
    assertEquals(1, variant.nt(1).length);
    assertEquals(4, variant.nt(1)[0]);
    try {
      variant.nt(2);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }
  }

  static final String SNP_LINE2 = "someKindOfName 23 . A T,C 12.8 PASS . GT:DP:RE:GQ 1/2:4:0.02:12.8";
  static final String SNP_LINE4 = "someKindOfName 23 . A T 12.8 PASS . GT:DP:RE:GQ ./1:4:0.02:12.8";
  public void testHeterozygousSnpConstruction() {
    Variant variant = createVariant(SNP_LINE2);
    // Normal het call 1/2
    assertEquals(22, variant.getStart());
    assertNull(variant.allele(-1));
    assertNotNull(variant.allele(0)); // Kept for trimming purposes
    assertEquals(1, variant.nt(1).length);
    assertEquals(4, variant.nt(1)[0]);
    assertEquals(1, variant.nt(2).length);
    assertEquals(2, variant.nt(2)[0]);

    final OrientedVariant[] pos = new Orientor.UnphasedOrientor(2).orientations(variant);
    assertEquals(2, pos.length);
    assertTrue(pos[0].isOriginal());
    assertFalse(pos[1].isOriginal());
    assertTrue(pos[0].isHeterozygous());
    assertTrue(pos[1].isHeterozygous());
    assertEquals(1, pos[0].alleleId());
    assertEquals(2, pos[1].alleleId());

    // Test half-call  ./1
    variant = createVariant(SNP_LINE4);
    assertNotNull(variant.allele(0)); // Kept for trimming purposes
    assertEquals(1, variant.nt(-1).length);
    assertEquals(5, variant.nt(-1)[0]);
    assertEquals(4, variant.nt(1)[0]);
  }

  static final String INSERT_LINE = "someKindOfName 22 . A AACT 12.8 PASS . GT:DP:RE:GQ 1/1:4:0.02:12.8";
  public void testInsertConstruction() {
    final Variant variant = createVariant(INSERT_LINE);
    assertEquals(22, variant.getStart());
    assertEquals(3, variant.nt(1).length);
    assertEquals(2, variant.nt(1)[1]);
  }


  static final String DELETION_LINE = "someKindOfName 22 . GAGT G 12.8 PASS . GT:DP:RE:GQ 1/1:4:0.02:12.8";
  public void testDeletionConstructor() {
    final Variant variant = createVariant(DELETION_LINE);
    assertEquals(22, variant.getStart());
  }
  static final String MNP_LINE = "someKindOfName 23 . AGT CTC 12.8 PASS . GT:DP:RE:GQ 1/1:4:0.02:12.8";
  public void testMnpConstructor() {
    final Variant variant = createVariant(MNP_LINE);
    assertEquals(22, variant.getStart());

    assertEquals(3, variant.nt(1).length);
    assertEquals(2, variant.nt(1)[0]);
    assertEquals(4, variant.nt(1)[1]);
    try {
      variant.nt(2);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }

  }

  static final String UNCHANGED_LINE = "someKindOfName 23 . A C 12.8 PASS . GT:DP:RE:GQ 1/1:4:0.02:12.8";
  public void testUnchangedConstructor() {
    final Variant variant = createVariant(UNCHANGED_LINE);
    assertEquals(22, variant.getStart());
    assertTrue(Arrays.equals(DnaUtils.encodeString("C"), variant.nt(1)));
  }

  static final String SHORT_LINE = "someKindOfName 23 . A C 0.0 PASS . GT 1/1";
  public void testShortConstructor() {
    final Variant variant = createVariant(SHORT_LINE);
    assertEquals(22, variant.getStart());
    assertTrue(Arrays.equals(DnaUtils.encodeString("C"), variant.nt(1)));
  }

  static final String TAIL = TAB + ". A C 12.8 PASS . GT 1/1";

  public void testOrdering() {
    final Variant a1a = createVariant("a" + TAB + 1 + TAIL);
    final Variant a1b = createVariant("a" + TAB + 1 + TAIL);
    final Variant a2 = createVariant("a" + TAB + 2 + TAIL);
    final Variant b = createVariant("b" + TAB + 2 + TAIL);
    final Variant c = createVariant("c" + TAB + 2 + TAIL);
    TestUtils.testOrder(new Variant[][]{{a1a, a1b}, {a2}, {b}, {c}}, true);
  }

  public void testEquals() {
    final Variant a1a = createVariant("a" + TAB + 1 + TAIL);
    assertFalse(a1a.equals(null));
    assertFalse(a1a.equals(new Object()));
  }


  private void checkArray(byte[] a, byte[] b) {
    final String fail = "expected <" + Arrays.toString(a) + "> but was :<" + Arrays.toString(b) + ">";
    assertEquals(fail, a.length, b.length);
    for (int i = 0; i < a.length; ++i) {
      assertEquals(fail, a[i], b[i]);
    }
  }

  public void testVariantInterface() {
    final Variant v = createVariant("simulatedSequence1\t2180\t.\tC\tT,G\t0.0 PASS . GT 1/2");
    assertEquals(2179, v.getStart());
    assertEquals(2180, v.getEnd());
    checkArray(new byte[] {4}, v.nt(1));
    checkArray(new byte[] {3}, v.nt(2));
    final Variant v2 = createVariant("simulatedSequence1 2180 . C G,T 31.0 PASS . GT 1/2".replaceAll(" ", "\t"));
    assertEquals(2179, v2.getStart());
    assertEquals(2180, v2.getEnd());
    checkArray(new byte[] {3}, v2.nt(1));
    checkArray(new byte[] {4}, v2.nt(2));
  }

  public void testMissingGT() {
    final String line = "simulatedSequence1 2180 . C A 0.0 PASS . GT .".replaceAll(" ", "\t");
    assertNull(createVariant(line)); // Current default factory skips those with missing GT
  }

}
