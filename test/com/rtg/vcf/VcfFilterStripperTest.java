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

import java.util.HashSet;
import java.util.List;

import com.rtg.vcf.header.FilterField;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfFilterStripperTest extends TestCase {

  private VcfRecord createTestRecord() {
    final VcfRecord rec = new VcfRecord();
    rec.setSequence("chr1")
            .setStart(1209)
            .setId(".")
            .setQuality("12.8")
            .setRefCall("a")
            .addAltCall("c")
            .addAltCall("t")
            .addFilter("no")
            .addFilter("wo")
            .addFilter("go")
            .addInfo("DP", "23")
            .addInfo("TEST", "45", "46", "47", "48")
            .setNumberOfSamples(2)
            .addFormatAndSample("GT", "0/0")
            .addFormatAndSample("GT", "0/1")
            .addFormatAndSample("GQ", "100")
            .addFormatAndSample("GQ", "95")
    ;
    return rec;
  }

  private VcfHeader createTestHeader() {
    final VcfHeader head = new VcfHeader();
    head.addFilterField("no", "blah");
    head.addFilterField("wo", "blah");
    head.addFilterField("go", "blah");
    return head;
  }

  public void testKeep() {
    final HashSet<String> filters = new HashSet<>();
    filters.add("wo");

    final VcfFilterStripper ann = new VcfFilterStripper(filters, true);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final List<String> filterList = rec.getFilters();
    assertNotNull(filterList);
    assertEquals(1, filterList.size());

    assertTrue(filterList.get(0).equals("wo"));

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<FilterField> headerfilters = header.getFilterLines();
    assertNotNull(headerfilters);
    assertEquals(1, headerfilters.size());

    assertEquals("wo", headerfilters.get(0).getId());
  }

  public void testRemove() {
    final HashSet<String> filters = new HashSet<>();
    filters.add("no");
    filters.add("go");

    final VcfFilterStripper ann = new VcfFilterStripper(filters, false);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final List<String> filterlist = rec.getFilters();
    assertNotNull(filterlist);
    assertEquals(1, filterlist.size());

    assertTrue(filterlist.get(0).equals("wo"));

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<FilterField> headerfilters = header.getFilterLines();
    assertNotNull(headerfilters);
    assertEquals(1, headerfilters.size());

    assertEquals("wo", headerfilters.get(0).getId());
  }

  public void testRemoveAll() {
    final VcfFilterStripper ann = new VcfFilterStripper(true);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final List<String> filterlist = rec.getFilters();
    assertNotNull(filterlist);
    assertEquals(0, filterlist.size());

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<FilterField> headerfilters = header.getFilterLines();
    assertNotNull(headerfilters);
    assertEquals(0, headerfilters.size());
  }
}
