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

import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.SampleField;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfSampleStripperTest extends TestCase {

  private VcfRecord createTestRecord() {
    final VcfRecord rec = new VcfRecord();
    rec.setSequence("chr1")
            .setStart(1209)
            .setId(".")
            .setQuality("12.8")
            .setRefCall("a")
            .addAltCall("c")
            .addFilter("no")
            .addInfo("DP", "23")
            .addInfo("TEST", "45", "46", "47", "48")
            .setNumberOfSamples(3)
            .addFormatAndSample("GT", "0/0")
            .addFormatAndSample("GT", "0/1")
            .addFormatAndSample("GT", "1/1")
            .addFormatAndSample("GQ", "100")
            .addFormatAndSample("GQ", "95")
            .addFormatAndSample("GQ", "2")
    ;
    return rec;
  }

  private VcfHeader createTestHeader() {
    final VcfHeader head = new VcfHeader();
    head.addSampleName("xbox");
    head.addSampleName("xbox 360");
    head.addSampleName("xbox one");
    head.addMetaInformationLine("##SAMPLE=<ID=xbox,Description=\"the original\">");
    head.addMetaInformationLine("##SAMPLE=<ID=xbox one,Description=\"the sequel to the 360\">");
    head.addFormatField(VcfHeader.parseFormatLine("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">"));
    head.addFormatField(VcfHeader.parseFormatLine("##FORMAT=<ID=DS,Number=1,Type=Float,Description=\"Genotype dosage from MaCH/Thunder\">"));
    head.addFormatField(VcfHeader.parseFormatLine("##FORMAT=<ID=GL,Number=.,Type=Float,Description=\"Genotype Likelihoods\">"));
    return head;
  }

  public void testKeep() {
    final HashSet<String> filters = new HashSet<>();
    filters.add("xbox 360");
    filters.add("xbox one");

    final VcfSampleStripper ann = new VcfSampleStripper(filters, true);
    final VcfHeader header = createTestHeader();
    ann.updateHeader(header); //need this up here for annotate to have a header map of sample name to id

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Map<String, ArrayList<String>> formatAndSample = rec.getFormatAndSample();
    assertNotNull(formatAndSample);
    assertEquals(2, formatAndSample.size()); //gt, gq
    assertNotNull(formatAndSample.get("GT"));
    assertEquals(2, formatAndSample.get("GT").size());
    assertNotNull(formatAndSample.get("GQ"));
    assertEquals(2, formatAndSample.get("GQ").size());
    assertEquals(2, rec.getNumberOfSamples());

    assertTrue(formatAndSample.get("GT").get(0).equals("0/1"));
    assertTrue(formatAndSample.get("GT").get(1).equals("1/1"));
    assertTrue(formatAndSample.get("GQ").get(0).equals("95"));
    assertTrue(formatAndSample.get("GQ").get(1).equals("2"));

    final List<SampleField> headersamples = header.getSampleLines();

    assertEquals(2, header.getNumberOfSamples());

    assertNotNull(headersamples);
    assertEquals(1, headersamples.size());  //I didn't add one for the xbox 360, so only 1 left
    assertEquals("xbox one", headersamples.get(0).getId());


    final List<String> headersamplenames = header.getSampleNames();
    assertNotNull(headersamplenames);
    assertEquals(2, headersamplenames.size());
    assertEquals("xbox 360", headersamplenames.get(0));
    assertEquals("xbox one", headersamplenames.get(1));
  }

  public void testRemove() {
    final HashSet<String> filters = new HashSet<>();
    filters.add("xbox");
    filters.add("xbox 360");

    final VcfSampleStripper ann = new VcfSampleStripper(filters, false);

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header); //need this up here for annotate to have a header map of sample name to id

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Map<String, ArrayList<String>> formatAndSample = rec.getFormatAndSample();
    assertNotNull(formatAndSample);
    assertEquals(2, formatAndSample.size()); //gt, gq
    assertNotNull(formatAndSample.get("GT"));
    assertEquals(1, formatAndSample.get("GT").size());
    assertNotNull(formatAndSample.get("GQ"));
    assertEquals(1, formatAndSample.get("GQ").size());
    assertEquals(1, rec.getNumberOfSamples());

    assertTrue(formatAndSample.get("GT").get(0).equals("1/1"));
    assertTrue(formatAndSample.get("GQ").get(0).equals("2"));

    final List<SampleField> headersamples = header.getSampleLines();

    assertEquals(1, header.getNumberOfSamples());

    assertNotNull(headersamples);
    assertEquals(1, headersamples.size());
    assertEquals("xbox one", headersamples.get(0).getId());

    final List<String> headersamplenames = header.getSampleNames();
    assertNotNull(headersamplenames);
    assertEquals(1, headersamplenames.size());
    assertEquals("xbox one", headersamplenames.get(0));
  }

  public void testRemoveAll() {
    final VcfSampleStripper ann = new VcfSampleStripper(true);

    final VcfRecord rec = createTestRecord();
    ann.annotate(rec);

    final Map<String, ArrayList<String>> filterlist = rec.getFormatAndSample();
    assertNotNull(filterlist);
    assertEquals(0, filterlist.size());
    assertEquals(0, rec.getNumberOfSamples());

    final VcfHeader header = createTestHeader();
    ann.updateHeader(header);

    final List<SampleField> headerfilters = header.getSampleLines();
    assertNotNull(headerfilters);
    assertEquals(0, headerfilters.size());
    assertEquals(0, header.getNumberOfSamples());

    final List<FormatField> formatlines = header.getFormatLines();
    assertNotNull(formatlines);
    assertEquals(0, formatlines.size());
  }
}
