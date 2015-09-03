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

import junit.framework.TestCase;

/**
 */
public class VcfAltCleanerTest extends TestCase {

  public void testNoRedundantAlt() {
    final VcfAltCleaner ann = new VcfAltCleaner();
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .setNumberOfSamples(2)
      .addFormatAndSample("GT", "0/0")
      .addFormatAndSample("GT", "1/2");
    ann.annotate(rec);
    assertEquals("chr1\t1210\t.\ta\tc,t\t12.8\t.\t.\tGT\t0/0\t1/2", rec.toString());
  }

  public void testOneRedundantAlt() {
    final VcfAltCleaner ann = new VcfAltCleaner();
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .setNumberOfSamples(2)
      .addFormatAndSample("GT", "0/0")
      .addFormatAndSample("GT", "0/2");
    ann.annotate(rec);
    assertEquals("chr1\t1210\t.\ta\tt\t12.8\t.\t.\tGT\t0/0\t0/1", rec.toString());
  }

  public void testPhasingAndPloidy() {
    final VcfAltCleaner ann = new VcfAltCleaner();
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
      .setQuality("12.8")
      .addAltCall("c")
      .addAltCall("t")
      .addAltCall("g")
      .setNumberOfSamples(3)
      .addFormatAndSample("GT", "0/0")
      .addFormatAndSample("GT", "2")
      .addFormatAndSample("GT", ".|2")
    ;
    ann.annotate(rec);
    assertEquals("chr1\t1210\t.\ta\tt\t12.8\t.\t.\tGT\t0/0\t1\t.|1", rec.toString());
  }
}
