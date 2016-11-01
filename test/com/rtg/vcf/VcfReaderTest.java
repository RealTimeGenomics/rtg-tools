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

import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.StringUtils.TAB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;

import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfReaderTest extends TestCase {

  public void testEmpty() throws IOException {
    checkVcfFormatException("", "VCF file format version header line");
  }

  static final String[] BAD_HDR = {
    "#CHRM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO",
    "#CHROM\tPOS\tID\tREF\tALT\tqual\tFILTER\tINFO",
    "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER",
    "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT", // needs some sample IDs
    "##INFO=<Description=foo>" + LS + "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO",
  };

  public void testBadHdr() throws Exception {
    for (final String h : BAD_HDR) {
      final Reader in = new StringReader(""
        + VcfHeader.VERSION_LINE + LS
        + h + LS);
      try {
        new VcfReader(new BufferedReader(in)).hasNext();
        fail();
      } catch (final NoTalkbackSlimException ex) {
        TestUtils.containsAll(ex.getMessage(), "VCF", "header");
      }
    }
  }

  protected void checkVcfFormatException(String header, String... contains) throws IOException {
    final Reader in = new StringReader(header);
    try {
      new VcfReader(new BufferedReader(in)).hasNext();
      fail("VcfFormatException expected from .vcf file: " + header);
    } catch (final VcfFormatException ex) {
      TestUtils.containsAll(ex.getMessage(), contains);
    }
  }

  static final String[] BAD_RECORD = {
    "chr1   123    .    G    A    29    PASS",                                   // Missing INFO column
    "chr1   .      .    G    A    29    PASS    DP=7",                           // Missing INFO column
    "chr1   123    foo  G    A    29    PASS    X=yy;DP=7    GT:GQ",             // HEADER0 says should not be any samples
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34",    // HEADER0 says should not be any samples
  };

  public void testBadRecords() throws IOException {
    for (final String recText : BAD_RECORD) {
      final String badrec = recText.replaceAll("  *", TAB);
      checkVcfFormatException(HEADER0 + badrec + LS, "Invalid VCF record");
    }
  }

  /** standard header to share */
  public static final String HEADER0 = ""
    + "##fileformat=VCFv4.1" + LS
    + "#CHROM" + TAB + "POS" + TAB + "ID" + TAB + "REF" + TAB + "ALT" + TAB + "QUAL" + TAB + "FILTER" + TAB
        + "INFO" + LS
    ;
  /** standard header to share */
  public static final String HEADER0_B = ""
    + "##fileformat=VCFv4.1" + LS
    + "#CHROM" + TAB + "POS" + TAB + "ID" + TAB + "REF" + TAB + "ALT" + TAB + "QUAL" + TAB + "FILTER" + TAB
        + "INFO" + TAB + "FORMAT" + TAB + "SAMPLE" + LS
    ;
  public void testHeader0() throws IOException {
    final Reader in = new StringReader(HEADER0);
    final VcfReader reader = new VcfReader(new BufferedReader(in));
    final VcfHeader hdr = reader.getHeader();
    assertNotNull(hdr);
    assertEquals(0, hdr.getGenericMetaInformationLines().size());
    assertEquals(0, hdr.getNumberOfSamples());
    assertFalse(reader.hasNext());
  }

  protected static final String HEADER1 = ""
    + "##fileformat=VCFv4.1" + LS
    + "##X=YYY" + LS
    + "##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Total Depth\">" + LS
    + "#CHROM" + TAB + "POS" + TAB + "ID" + TAB + "REF" + TAB + "ALT" + TAB + "QUAL" + TAB + "FILTER" + TAB + "INFO" + TAB + "FORMAT" + TAB + "sample1" + LS
    + "chr1" + TAB + "123" + TAB + "." + TAB + "G" + TAB + "A,G" + TAB + "29" + TAB + "q3;s5;c" + TAB + "X=yy;DP=7" + TAB + "GT:GQ" + TAB + "0|0:7" + LS
    ;
  public void testNext() throws IOException {
    final Reader in = new StringReader(HEADER1);
    final VcfReader reader = new VcfReader(new BufferedReader(in));
    final VcfHeader hdr = reader.getHeader();
    assertNotNull(hdr);
    assertEquals(2, hdr.getGenericMetaInformationLines().size() + hdr.getInfoLines().size() + hdr.getFormatLines().size() + hdr.getFilterLines().size());
    assertEquals(1, hdr.getNumberOfSamples());
    assertTrue(reader.hasNext());
    final VcfRecord rec = reader.next();
    assertNotNull(rec);
    assertEquals("chr1", rec.getSequenceName());
    assertEquals(123, rec.getOneBasedStart());
    assertEquals(".", rec.getId());
    assertEquals("G", rec.getRefCall());

    assertEquals(2, rec.getAltCalls().size());
    assertEquals("A", rec.getAltCalls().get(0));
    assertEquals("G", rec.getAltCalls().get(1));
    assertEquals("29", rec.getQuality());

    assertEquals(3, rec.getFilters().size());
    assertEquals("q3", rec.getFilters().get(0));
    assertEquals("s5", rec.getFilters().get(1));
    assertEquals("c", rec.getFilters().get(2));

    assertEquals(2, rec.getInfo().size());
    assertEquals("yy", rec.getInfo().get("X").iterator().next());
    assertEquals("7", rec.getInfo().get("DP").iterator().next());

    assertEquals(2, rec.getFormats().size());
    assertEquals(Collections.singletonList("0|0"), rec.getFormat("GT"));
    assertEquals(Collections.singletonList("7"), rec.getFormat("GQ"));
  }
}
