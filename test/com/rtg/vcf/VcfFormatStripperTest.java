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
import java.util.Set;

import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfFormatStripperTest extends TestCase {

  private VcfRecord createTestRecord() {
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
            .setQuality("12.8")
            .addAltCall("c")
            .addAltCall("t")
            .addFilter("TEST1")
            .addFilter("TEST2")
            .addInfo("DP", "23")
            .addInfo("AN", "5")
            .setNumberOfSamples(2)
            .addFormatAndSample("GT", "0/0")
            .addFormatAndSample("GT", "0/1")
            .addFormatAndSample("DS", "je")
            .addFormatAndSample("DS", "er")
            .addFormatAndSample("GL", "je")
            .addFormatAndSample("GL", "fe")
            .addFormatAndSample("GQ", "100")
            .addFormatAndSample("GQ", "95")
    ;
    return rec;
  }

  private VcfHeader createTestHeader() {
    final VcfHeader head = new VcfHeader();
    head.addFormatField(VcfHeader.parseFormatLine("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">"));
    head.addFormatField(VcfHeader.parseFormatLine("##FORMAT=<ID=DS,Number=1,Type=Float,Description=\"Genotype dosage from MaCH/Thunder\">"));
    head.addFormatField(VcfHeader.parseFormatLine("##FORMAT=<ID=GL,Number=.,Type=Float,Description=\"Genotype Likelihoods\">"));
    return head;
  }

  public void testKeep() {
    final HashSet<String> formats = new HashSet<>();
    formats.add("GT");

    final VcfFormatStripper ann = new VcfFormatStripper(formats, true);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Set<String> formatlist = rec.getFormats();
    assertNotNull(formatlist);
    assertEquals(1, formatlist.size());
    assertTrue(formatlist.contains("GT"));

    assertEquals(1, rec.getFormats().size());

    assertTrue(rec.hasFormat("GT"));

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<FormatField> headerformats = header.getFormatLines();
    assertNotNull(headerformats);
    assertEquals(1, headerformats.size());

    assertEquals("GT", headerformats.get(0).getId());
  }

  public void testRemove() {
    final HashSet<String> formats = new HashSet<>();
    formats.add("DS");
    formats.add("GL");

    final VcfFormatStripper ann = new VcfFormatStripper(formats, false);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Set<String> formatset = rec.getFormats();
    assertNotNull(formatset);

    assertEquals(2, formatset.size());
    assertTrue(formatset.contains("GT"));
    assertTrue(formatset.contains("GQ"));

    assertEquals(2, rec.getFormats().size());

    assertTrue(rec.hasFormat("GT"));
    assertTrue(rec.hasFormat("GQ"));

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<FormatField> formatLines = header.getFormatLines();
    assertNotNull(formatLines);
    assertEquals(1, formatLines.size()); //didn't add GQ in, so only 1

    assertEquals("GT", formatLines.get(0).getId());
  }
}
