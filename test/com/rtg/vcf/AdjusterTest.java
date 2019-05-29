/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
 * Test the corresponding class.
 */
public class AdjusterTest extends TestCase {

  public void test() {
    final Adjuster adjuster = new Adjuster();
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "42,3,14");
    adjuster.adjust(rec, new int[] {0, 1, 1});
    assertEquals("42,17", rec.getFormat(VcfUtils.FORMAT_ALLELIC_DEPTH).get(0));
  }

  public void testNoPolicy() {
    final Adjuster adjuster = new Adjuster();
    adjuster.setPolicy(VcfUtils.FORMAT_ALLELIC_DEPTH, null);
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "42,3,14");
    adjuster.adjust(rec, new int[] {0, 1, 1});
    assertEquals("42,3,14", rec.getFormat(VcfUtils.FORMAT_ALLELIC_DEPTH).get(0));
  }

  public void testDropPolicy() {
    final Adjuster adjuster = new Adjuster();
    adjuster.setPolicy(VcfUtils.FORMAT_ALLELIC_DEPTH, Adjuster.Policy.DROP);
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "42,3,14");
    adjuster.adjust(rec, new int[] {0, 1, 1});
    assertNull(rec.getFormat(VcfUtils.FORMAT_ALLELIC_DEPTH));
  }
}
