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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfInfoStripperTest extends TestCase {

  private VcfRecord createTestRecord() {
    final VcfRecord rec = new VcfRecord();
    rec.setSequence("chr1")
            .setStart(1209)
            .setId(".")
            .setQuality("12.8")
            .setRefCall("a")
            .addAltCall("c")
            .addAltCall("t")
            .addFilter("TEST1")
            .addFilter("TEST2")
            .addInfo("DP", "23")
            .addInfo("AC", "2")
            .addInfo("TEST", "45", "46", "47", "48")
            .addInfo("AN", "5")
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
    head.addInfoField(VcfHeader.parseInfoLine("##INFO=<ID=yo, Number=5, Type=Float, Description=\"fun for the whole family\">"));
    head.addInfoField(VcfHeader.parseInfoLine("##INFO=<ID=no, Number=5, Type=Float, Description=\"fun for the whole family\">"));
    head.addInfoField(VcfHeader.parseInfoLine("##INFO=<ID=AC, Number=5, Type=Float, Description=\"fun for the whole family\">"));
    head.addInfoField(VcfHeader.parseInfoLine("##INFO=<ID=go, Number=5, Type=Float, Description=\"fun for the whole family\">"));
    head.addInfoField(VcfHeader.parseInfoLine("##INFO=<ID=AN, Number=5, Type=Float, Description=\"fun for the whole family\">"));
    return head;
  }

  public void testKeep() {
    final HashSet<String> infos = new HashSet<>();
    infos.add("AN");
    infos.add("AC");

    final VcfInfoStripper ann = new VcfInfoStripper(infos, true);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Map<String, ArrayList<String>> infoMap = rec.getInfo();
    assertNotNull(infoMap);
    assertEquals(2, infoMap.size());

    assertTrue(infoMap.containsKey("AN"));
    assertTrue(infoMap.containsKey("AC"));

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<InfoField> headerinfos = header.getInfoLines();
    assertNotNull(headerinfos);
    assertEquals(2, headerinfos.size());

    assertEquals("AC", headerinfos.get(0).getId());
    assertEquals("AN", headerinfos.get(1).getId());
  }

  public void testRemove() {
    final HashSet<String> infos = new HashSet<>();
    infos.add("AN");
    infos.add("AC");

    final VcfInfoStripper ann = new VcfInfoStripper(infos, false);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Map<String, ArrayList<String>> infoMap = rec.getInfo();
    assertNotNull(infoMap);
    assertEquals(2, infoMap.size());

    assertTrue(infoMap.containsKey("DP"));
    assertTrue(infoMap.containsKey("TEST"));

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<InfoField> headerinfos = header.getInfoLines();
    assertNotNull(headerinfos);
    assertEquals(3, headerinfos.size());

    assertEquals("yo", headerinfos.get(0).getId());
    assertEquals("no", headerinfos.get(1).getId());
    assertEquals("go", headerinfos.get(2).getId());
  }

  public void testRemoveAll() {
    final VcfInfoStripper ann = new VcfInfoStripper(true);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Map<String, ArrayList<String>> infoMap = rec.getInfo();
    assertNotNull(infoMap);
    assertEquals(0, infoMap.size());

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<InfoField> infos = header.getInfoLines();
    assertNotNull(infos);
    assertEquals(0, infos.size());
  }
}
