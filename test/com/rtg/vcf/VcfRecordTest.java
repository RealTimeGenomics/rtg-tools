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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 */
public class VcfRecordTest extends TestCase {

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

  static void checkRecord(VcfRecord merged, VcfRecord exp, String[] genotype) {
    final List<String> altCalls = exp.getAltCalls();
    checkRecord(merged, exp.getSequenceName(), exp.getRefCall(), exp.getOneBasedStart(), altCalls.toArray(new String[altCalls.size()]), genotype);
  }
  static void checkRecord(VcfRecord merged, VcfRecord exp, String[] altCalls, String[] genotype) {
    checkRecord(merged, exp.getSequenceName(), exp.getRefCall(), exp.getOneBasedStart(), altCalls, genotype);
  }

  private static void checkRecord(VcfRecord merged, String chr, String refCall, int pos, String[] altCalls, String[] gts) {
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
    assertEquals(Collections.singletonList("PASS"), rec.getFilters());
    rec.addFilter("a1.0");
    assertEquals(Collections.singletonList("a1.0"), rec.getFilters());
  }
}
