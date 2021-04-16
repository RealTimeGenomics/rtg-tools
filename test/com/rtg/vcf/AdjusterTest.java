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

import java.io.IOException;

import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 * Test the corresponding class.
 */
public class AdjusterTest extends TestCase {

  private static final String ID = "AD";

  private VcfHeader makeHeader() {
    return makeHeader(MetaType.INTEGER, VcfNumber.REF_ALTS);
  }
  private VcfHeader makeHeader(MetaType type, VcfNumber number) {
    VcfHeader h = new VcfHeader();
    h.addFormatField(new FormatField(ID, type, number, "Allelic depth"));
    h.addInfoField(new InfoField(ID, type, number, "Allelic depth"));
    return h;
  }

  public void testMalformedInfo() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader());
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat");
    inrec.setInfo(ID, "43.5,4,15");
    try {
      adjuster.adjust(inrec, new VcfRecord("bar", 4, "aaa").addAltCall("gac"), new int[] {0, 1, 1});
      fail("Should not accept float for integer annotation");
    } catch (VcfFormatException e) {
      // Expected
    }
    inrec.setInfo(ID, "a", "b", "c");
    try {
      adjuster.adjust(inrec, new VcfRecord("bar", 4, "aaa").addAltCall("gac"), new int[] {0, 1, 1});
      fail("Should not accept non-integer annotation");
    } catch (VcfFormatException e) {
      // Expected
    }
  }

  public void testMalformedInfoNumber() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader(MetaType.INTEGER, VcfNumber.GENOTYPES));
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat");
    inrec.setInfo(ID, "43,4,15");
    try {
      adjuster.adjust(inrec, new VcfRecord("bar", 4, "aaa").addAltCall("gac"), new int[] {0, 1, 1});
      fail("Should not accept G type annotation");
    } catch (VcfFormatException e) {
      // Expected
    }
  }

  public void testMalformedFormat() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader());
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42,3,4,5");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac");
    try {
      adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
      fail("Should not accept annotation with too many values");
    } catch (VcfFormatException e) {
      // Expected
    }
  }

  public void testMalformedFormatType() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader(MetaType.STRING, VcfNumber.REF_ALTS));
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42,3,4");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac");
    try {
      adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
      fail("Should not accept String type annotations");
    } catch (VcfFormatException e) {
      // Expected
    }
  }

  public void testSum() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader());
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setInfo(ID, "43", "4", "15")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42,3,14");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").setNumberOfSamples(1);
    adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
    assertEquals("42,17", rec.getFormat(ID).get(0));
    assertEquals("43", rec.getInfoSplit(ID)[0]);
    assertEquals("19", rec.getInfoSplit(ID)[1]);
  }
  
  public void testSumMissingElement() throws IOException {
    // We skip over sub-elements with missing value during summation
    final Adjuster adjuster = new Adjuster(makeHeader());
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setInfo(ID, "43", ".", "15")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42,.,14");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").setNumberOfSamples(1);
    adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
    assertEquals("42,14", rec.getFormat(ID).get(0));
    assertEquals("43", rec.getInfoSplit(ID)[0]);
    assertEquals("15", rec.getInfoSplit(ID)[1]);
  }

  public void testSumFloat() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader(MetaType.FLOAT, VcfNumber.REF_ALTS));
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setInfo(ID, "43.1", "4.2", "15.3")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42.1,3.2,14.3");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").setNumberOfSamples(1);
    adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
    assertEquals("42.100,17.500", rec.getFormat(ID).get(0));
    assertEquals("43.100", rec.getInfoSplit(ID)[0]);
    assertEquals("19.500", rec.getInfoSplit(ID)[1]);
  }

  public void testNoPolicy() throws IOException {
    final VcfHeader header = makeHeader();
    final Adjuster adjuster = new Adjuster(header);
    assertTrue(adjuster.hasPolicy(header.getFormatField(ID)));
    assertTrue(adjuster.hasPolicy(header.getInfoField(ID)));
    adjuster.setPolicy("FORMAT." + ID, null);
    adjuster.setPolicy("INFO." + ID, null);
    assertFalse(adjuster.hasPolicy(header.getFormatField(ID)));
    assertFalse(adjuster.hasPolicy(header.getInfoField(ID)));
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42,3,14");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac")
      .setInfo(ID, "a,b")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "142,13,114");
    adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
    assertEquals("142,13,114", rec.getFormat(ID).get(0));
    assertEquals("a,b", rec.getInfo(ID));
  }

  public void testDropPolicy() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader());
    adjuster.setPolicy("FORMAT." + ID, Adjuster.Policy.DROP);
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "42,3,14");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac");
    adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
    assertNull(rec.getFormat(ID));
  }

  public void testRetainPolicy() throws IOException {
    final Adjuster adjuster = new Adjuster(makeHeader(MetaType.STRING, VcfNumber.REF_ALTS));
    adjuster.setPolicy("INFO." + ID, Adjuster.Policy.RETAIN);
    adjuster.setPolicy("FORMAT." + ID, Adjuster.Policy.RETAIN);
    final VcfRecord inrec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").addAltCall("cat")
      .setInfo(ID, "a,b")
      .setNumberOfSamples(1)
      .addFormatAndSample(ID, "a,b,c");
    final VcfRecord rec = new VcfRecord("bar", 4, "aaa").addAltCall("gac").setNumberOfSamples(1);
    adjuster.adjust(inrec, rec, new int[] {0, 1, 1});
    assertEquals("a,b,c", rec.getFormat(ID).get(0));
    assertEquals("a,b", rec.getInfo(ID));
  }
}
