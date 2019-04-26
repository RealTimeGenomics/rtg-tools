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

package com.rtg.vcf.header;

import static com.rtg.util.StringUtils.TAB;

import java.util.HashSet;

import com.rtg.reader.SdfId;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.VcfFormatException;

import junit.framework.TestCase;

/**
 */
public class VcfHeaderTest extends TestCase {

  public void testVcfHeader() {
    final VcfHeader head = new VcfHeader();
    head.addMetaInformationLine("##test1212121")
  .addMetaInformationLine("##test12").addMetaInformationLine("##PEDIGREE=<Child=NA19240,Mother=NA19239,Father=NA19238>");
    head.addSampleName("sample1")
      .addSampleName("sample2");

    assertEquals(2, head.getNumberOfSamples());
    assertEquals(2, head.getGenericMetaInformationLines().size());

    assertEquals("##test1212121", head.getGenericMetaInformationLines().get(0));
    assertEquals("##test12", head.getGenericMetaInformationLines().get(1));

    assertEquals("sample1", head.getSampleNames().get(0));
    assertEquals("sample2", head.getSampleNames().get(1));

    final String exp = "#"
    + "CHROM" + TAB
    + "POS" + TAB
    + "ID" + TAB
    + "REF" + TAB
    + "ALT" + TAB
    + "QUAL" + TAB
    + "FILTER" + TAB
    + "INFO" + TAB
    + "FORMAT" + TAB
    + "sample1" + TAB
    + "sample2"
    ;
    assertEquals(exp, head.getColumnHeader());
    assertEquals(1, head.getPedigreeLines().size());
  }

  public void testErrors() {
    Diagnostic.setLogStream();
    try {
      new VcfHeader().addSampleName("t1").addSampleName("t1");
      fail();
    } catch (final NoTalkbackSlimException ex) {
      assertEquals("Duplicate sample name \"t1\" in VCF header", ex.getMessage());
    }


    //checks that zero samples no longer explodes
    new VcfHeader().getColumnHeader();
  }

  public void testInfoParse() {
    InfoField f = VcfHeader.parseInfoLine("##INFO=<ID=yo, Number=5, Type=Float, Description=\"fun for the whole family\">");
    assertEquals("yo", f.getId());
    assertEquals(5, f.getNumber().getNumber());
    assertEquals(VcfNumberType.INTEGER, f.getNumber().getNumberType());
    assertEquals(MetaType.FLOAT, f.getType());
    assertEquals("fun for the whole family", f.getDescription());
    f = VcfHeader.parseInfoLine("##INFO=<ID=%$&*,Number=A,Type=Flag,Description=\"oh rearry\">");
    assertEquals("%$&*", f.getId());
    assertEquals(-1, f.getNumber().getNumber());
    assertEquals(VcfNumberType.ALTS, f.getNumber().getNumberType());
    assertEquals(MetaType.FLAG, f.getType());
    assertEquals("oh rearry", f.getDescription());
  }

  public void testDuplicateIds() {
    final VcfHeader head = new VcfHeader();
    head.addMetaInformationLine("##contig=<ID=xbox,Description=\"the original\">");
    try {
      head.addMetaInformationLine("##contig=<ID=xbox,Description=\"the sequel to the 360\">");
      fail();
    } catch (VcfFormatException e) {
      // expected
    }
    head.addMetaInformationLine("##FILTER=<ID=xbox,Number=1,Type=Float,Description=\"the original\">");
    try {
      head.addMetaInformationLine("##FILTER=<ID=xbox,Number=1,Type=Float,Description=\"the sequel to the 360\">");
      fail();
    } catch (VcfFormatException e) {
      // expected
    }
    head.addMetaInformationLine("##INFO=<ID=xbox,Number=1,Type=Float,Description=\"the original\">");
    try {
      head.addMetaInformationLine("##INFO=<ID=xbox,Number=1,Type=Float,Description=\"the sequel to the 360\">");
      fail();
    } catch (VcfFormatException e) {
      // expected
    }
    head.addMetaInformationLine("##FORMAT=<ID=xbox,Number=1,Type=Float,Description=\"the original\">");
    try {
      head.addMetaInformationLine("##FORMAT=<ID=xbox,Number=1,Type=Float,Description=\"the sequel to the 360\">");
      fail();
    } catch (VcfFormatException e) {
      // expected
    }
    head.addMetaInformationLine("##ALT=<ID=xbox,Description=\"the original\">");
    try {
      head.addMetaInformationLine("##ALT=<ID=xbox,Description=\"the sequel to the 360\">");
      fail();
    } catch (VcfFormatException e) {
      // expected
    }
    head.addMetaInformationLine("##SAMPLE=<ID=xbox,Description=\"the original\">");
    try {
      head.addMetaInformationLine("##SAMPLE=<ID=xbox,Description=\"the sequel to the 360\">");
      fail();
    } catch (VcfFormatException e) {
      // expected
    }
    assertEquals(0, head.getPedigreeLines().size());
    head.addMetaInformationLine("##PEDIGREE=<Child=daughter1,Mother=mother,Father=father>");
    assertEquals(1, head.getPedigreeLines().size());
    head.addMetaInformationLine("##PEDIGREE=<Child=daughter1,Mother=mother,Father=father>");
    assertEquals(1, head.getPedigreeLines().size());
  }

  public void testEscapedInfoParse() {
    InfoField f = VcfHeader.parseInfoLine("##INFO=<ID=yo, Number=5, Type=Float, Description=\"fun for the \\\"whole\\\" family, and \\\"more\\\" too!\">");
    assertEquals("yo", f.getId());
    assertEquals(5, f.getNumber().getNumber());
    assertEquals(VcfNumberType.INTEGER, f.getNumber().getNumberType());
    assertEquals(MetaType.FLOAT, f.getType());
    assertEquals("fun for the \"whole\" family, and \"more\" too!", f.getDescription());

    f = VcfHeader.parseInfoLine("##INFO=<ID=yo, Number=5, Type=Float, Description=\"have some \\ \\\\ kinda \\\" stuff yeah.\">");
    assertEquals("yo", f.getId());
    assertEquals(5, f.getNumber().getNumber());
    assertEquals(VcfNumberType.INTEGER, f.getNumber().getNumberType());
    assertEquals(MetaType.FLOAT, f.getType());
    assertEquals("have some \\ \\ kinda \" stuff yeah.", f.getDescription());
  }

  public void testSampleRemovalAll() {
    final VcfHeader head = new VcfHeader();
    head.addSampleName("xbox");
    head.addSampleName("xbox 360");
    head.addSampleName("xbox one");
    head.addMetaInformationLine("##SAMPLE=<ID=xbox,Description=\"the original\">");
    head.addMetaInformationLine("##SAMPLE=<ID=xbox one,Description=\"the sequel to the 360\">");

    assertNotNull(head.getSampleIndex("xbox"));
    assertNotNull(head.getSampleIndex("xbox 360"));
    assertNotNull(head.getSampleIndex("xbox one"));

    head.removeAllSamples();

    assertEquals(0, head.getNumberOfSamples());

    assertNotNull(head.getSampleNames());
    assertEquals(0, head.getSampleNames().size());

    assertNotNull(head.getSampleLines());
    assertEquals(0, head.getSampleLines().size());

    assertNull(head.getSampleIndex("xbox"));
    assertNull(head.getSampleIndex("xbox 360"));
    assertNull(head.getSampleIndex("xbox one"));
  }

  public void testSampleRemoval() {
    final VcfHeader head = new VcfHeader();
    head.addSampleName("xbox");
    head.addSampleName("xbox 360");
    head.addSampleName("xbox one");
    head.addMetaInformationLine("##SAMPLE=<ID=xbox,Description=\"the original\">");
    head.addMetaInformationLine("##SAMPLE=<ID=xbox one,Description=\"the sequel to the 360\">");

    assertNotNull(head.getSampleIndex("xbox"));
    assertNotNull(head.getSampleIndex("xbox 360"));
    assertNotNull(head.getSampleIndex("xbox one"));

    final HashSet<String> removes = new HashSet<>();
    removes.add("xbox 360");
    removes.add("xbox one");

    head.removeSamples(removes);

    assertEquals(1, head.getNumberOfSamples());

    assertNotNull(head.getSampleNames());
    assertEquals(1, head.getSampleNames().size());
    assertEquals("xbox", head.getSampleNames().get(0));

    assertNotNull(head.getSampleLines());
    assertEquals(1, head.getSampleLines().size());
    assertEquals("xbox", head.getSampleLines().get(0).getId());

    assertNotNull(head.getSampleIndex("xbox"));
    assertEquals(Integer.valueOf(0), head.getSampleIndex("xbox"));
    assertNull(head.getSampleIndex("xbox 360"));
    assertNull(head.getSampleIndex("xbox one"));
  }

  private static final String HEADER_SDF_PREFIX = "##TEMPLATE-SDF-ID=";

  public void testGetSdfId() {
    Diagnostic.setLogStream();
    VcfHeader header = new VcfHeader();
    header.addMetaInformationLine(HEADER_SDF_PREFIX + "blahtblah");
    try {
      header.getSdfId();
      fail();
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid VCF template SDF ID header line : " + HEADER_SDF_PREFIX + "blahtblah", e.getMessage());
    }
    header = new VcfHeader();
    header.addMetaInformationLine(HEADER_SDF_PREFIX + "blah");
    try {
      header.getSdfId();
      fail();
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid VCF template SDF ID header line : " + HEADER_SDF_PREFIX + "blah", e.getMessage());
    }
    header = new VcfHeader();
    header.addMetaInformationLine(HEADER_SDF_PREFIX + new SdfId(42).toString());
    assertEquals(new SdfId(42), header.getSdfId());
  }
}

