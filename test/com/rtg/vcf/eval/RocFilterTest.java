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

import com.rtg.vcf.VcfReaderTest;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class RocFilterTest extends TestCase {

  private static final VcfRecord PASS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS . GT 1/1".replaceAll(" ", "\t"));
  private static final VcfRecord FAIL_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 RC . GT 1/1".replaceAll(" ", "\t"));

  private static final VcfRecord HOMOZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS . GT 1/1".replaceAll(" ", "\t"));
  private static final VcfRecord HETEROZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS . GT 0/1".replaceAll(" ", "\t"));
  private static final VcfRecord IDENTITY_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A . 20.0 PASS . GT 0/0".replaceAll(" ", "\t"));

  private static final VcfRecord COMPLEX_HOMOZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS XRX GT 1/1".replaceAll(" ", "\t"));
  private static final VcfRecord COMPLEX_HETEROZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS XRX GT 0/1".replaceAll(" ", "\t"));
  private static final VcfRecord COMPLEX_IDENTITY_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A . 20.0 PASS XRX GT 0/0".replaceAll(" ", "\t"));

//  public void testEnum() {
//    TestUtils.testEnum(RocFilter.class, "[ALL, HOM, HET, SNP, NON_SNP, MNP, INDEL, XRX, NON_XRX, HOM_XRX, HOM_NON_XRX, HET_XRX, HET_NON_XRX]");
//  }

  private boolean accept(RocFilter ff, VcfRecord rec) {
    return ff.accept(rec, VcfUtils.getValidGt(rec, 0));
  }

  public void testAll() {
    final RocFilter f = RocFilter.ALL;
    assertTrue(accept(f, PASS_RECORD));
    assertTrue(accept(f, FAIL_RECORD));
    assertTrue(accept(f, HOMOZYGOUS_RECORD));
    assertTrue(accept(f, HETEROZYGOUS_RECORD));
    assertTrue(accept(f, IDENTITY_RECORD));

    assertTrue(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_IDENTITY_RECORD));
  }

  public void testHomozygous() {
    final RocFilter f = RocFilter.HOM;
    assertTrue(accept(f, PASS_RECORD));
    assertTrue(accept(f, FAIL_RECORD));
    assertTrue(accept(f, HOMOZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_HOMOZYGOUS_RECORD));

    assertFalse(accept(f, HETEROZYGOUS_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
    assertFalse(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
  }

  public void testHeterozygous() {
    final RocFilter f = RocFilter.HET;
    assertTrue(accept(f, HETEROZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_HETEROZYGOUS_RECORD));

    assertFalse(accept(f, PASS_RECORD));
    assertFalse(accept(f, FAIL_RECORD));
    assertFalse(accept(f, HOMOZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
  }

  public void testComplex() {
    final RocFilter f = RocFilter.XRX;
    assertTrue(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_IDENTITY_RECORD));

    assertFalse(accept(f, HETEROZYGOUS_RECORD));
    assertFalse(accept(f, PASS_RECORD));
    assertFalse(accept(f, FAIL_RECORD));
    assertFalse(accept(f, HOMOZYGOUS_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
  }


  public void testSimple() {
    final RocFilter f = RocFilter.NON_XRX;
    assertTrue(accept(f, HETEROZYGOUS_RECORD));
    assertTrue(accept(f, FAIL_RECORD));
    assertTrue(accept(f, PASS_RECORD));
    assertTrue(accept(f, HOMOZYGOUS_RECORD));
    assertTrue(accept(f, IDENTITY_RECORD));

    assertFalse(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
  }

  public void testHomozygousSimple() {
    final RocFilter f = RocFilter.HOM_NON_XRX;
    assertTrue(accept(f, FAIL_RECORD));
    assertTrue(accept(f, PASS_RECORD));
    assertTrue(accept(f, HOMOZYGOUS_RECORD));

    assertFalse(accept(f, HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
  }

  public void testHomozygousComplex() {
    final RocFilter f = RocFilter.HOM_XRX;
    assertTrue(accept(f, COMPLEX_HOMOZYGOUS_RECORD));

    assertFalse(accept(f, FAIL_RECORD));
    assertFalse(accept(f, PASS_RECORD));
    assertFalse(accept(f, HOMOZYGOUS_RECORD));
    assertFalse(accept(f, HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
  }

  public void testHeterozygousSimple() {
    final RocFilter f = RocFilter.HET_NON_XRX;
    assertTrue(accept(f, HETEROZYGOUS_RECORD));

    assertFalse(accept(f, FAIL_RECORD));
    assertFalse(accept(f, PASS_RECORD));
    assertFalse(accept(f, HOMOZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
  }

  public void testHeterozygousComplex() {
    final RocFilter f = RocFilter.HET_XRX;
    assertTrue(accept(f, COMPLEX_HETEROZYGOUS_RECORD));

    assertFalse(accept(f, FAIL_RECORD));
    assertFalse(accept(f, PASS_RECORD));
    assertFalse(accept(f, HOMOZYGOUS_RECORD));
    assertFalse(accept(f, HETEROZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertFalse(accept(f, COMPLEX_IDENTITY_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));
  }

}
