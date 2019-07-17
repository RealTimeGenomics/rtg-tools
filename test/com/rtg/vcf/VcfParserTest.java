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
public class VcfParserTest extends TestCase {

  public void testEmpty() throws IOException {
    checkVcfHeaderException("", "VCF file format version header line");
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
        new VcfParser().parseHeader(in);
        fail();
      } catch (final NoTalkbackSlimException ex) {
        TestUtils.containsAll(ex.getMessage(), "VCF", "header");
      }
    }
  }

  protected void checkVcfHeaderException(String vcfHeader, String... contains) throws IOException {
    try (final BufferedReader r = new BufferedReader(new StringReader(vcfHeader))) {
      new VcfParser().parseHeader(r);
      fail("VcfFormatException expected from .vcf file: " + vcfHeader);
    } catch (final VcfFormatException ex) {
      TestUtils.containsAll(ex.getMessage(), contains);
    }
  }
  protected void checkVcfRecordException(String vcfRec) {
    try {
      new VcfParser().parseLine(vcfRec);
      fail("VcfFormatException expected from .vcf file: " + vcfRec);
    } catch (final VcfFormatException ex) {
      // Expected
    }
  }

  public void testContigLength() throws IOException {
    checkVcfHeaderException(""
      + "##fileformat=VCFv4.1" + LS
      + "##contig=<ID=foo,length=abcde>" + LS
      + "#CHROM" + TAB + "POS" + TAB + "ID" + TAB + "REF" + TAB + "ALT" + TAB + "QUAL" + TAB + "FILTER" + TAB
      + "INFO" + LS,
      "Invalid VCF header", "on line:",
      "Non-integer contig length");
  }

  static final String[] BAD_RECORD = {
    "chr1   .      .    G    A    29    PASS    DP=7",                           // Missing POS column
    "chr1   123    ." + TAB + TAB + "A    29    PASS    DP=7",                            // Empty REF value
 // "chr1   123    .    .    A    29    PASS    DP=7",                           // Missing REF value?
    "chr1   123    .    G    A    29    PASS",                                   // Missing INFO column
    "chr1   123    .    G    A,    29    PASS    DP=7",                          // Empty ALT allele
    "chr1   123    .    G    A,,T    29    PASS    DP=7",                        // Empty ALT allele
  };
  static final String[] BAD_RECORD_SAMPLE = {
    "chr1   123    foo  A    T    100   PASS    XRX;XRX      GT:PR   1/0",       // Duplicated INFO field
    "chr1   123    foo  A    T    100   PASS    XRX          GT:PR:PR 1/0",      // Duplicated FORMAT field
  };

  public void testBadRecords() {
    for (final String recText : BAD_RECORD) {
      final String badrec = recText.replaceAll(" +", TAB);
      checkVcfRecordException(badrec);
    }
    for (final String recText : BAD_RECORD_SAMPLE) {
      final String badrec = recText.replaceAll(" +", TAB);
      checkVcfRecordException(badrec);
    }
  }

  /** standard header to share */
  public static final String HEADER0 = ""
    + "##fileformat=VCFv4.1" + LS
    + "#CHROM" + TAB + "POS" + TAB + "ID" + TAB + "REF" + TAB + "ALT" + TAB + "QUAL" + TAB + "FILTER" + TAB
    + "INFO" + LS;

  public void testHeader0() throws IOException {
    final Reader in = new StringReader(HEADER0);
    final VcfHeader hdr = new VcfParser().parseHeader(new BufferedReader(in));
    assertNotNull(hdr);
    assertEquals(0, hdr.getGenericMetaInformationLines().size());
    assertEquals(0, hdr.getNumberOfSamples());
  }

  // Also test we can parse a 4.2 header line
  private static final String HEADER4_2 = ""
    + "##fileformat=VCFv4.2" + LS
    + "#CHROM" + TAB + "POS" + TAB + "ID" + TAB + "REF" + TAB + "ALT" + TAB + "QUAL" + TAB + "FILTER" + TAB + "INFO" + LS;

  public void testHeader4v2() throws IOException {
    final Reader in = new StringReader(HEADER4_2);
    final VcfHeader hdr = new VcfParser().parseHeader(new BufferedReader(in));
    assertNotNull(hdr);
    assertEquals(0, hdr.getGenericMetaInformationLines().size());
    assertEquals(0, hdr.getNumberOfSamples());
  }

  protected static final String REC = "chr1" + TAB + "123" + TAB + "." + TAB + "G" + TAB + "A,G" + TAB + "29" + TAB + "q3;s5;c" + TAB + "X=yy;DP=7" + TAB + "GT:GQ" + TAB + "0|0:7";
  public void testNext() {
    final VcfRecord rec = new VcfParser().parseLine(REC);
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
    assertEquals("yy", rec.getInfo("X"));
    assertEquals("7", rec.getInfo("DP"));

    assertEquals(2, rec.getFormats().size());
    assertEquals(Collections.singletonList("0|0"), rec.getFormat("GT"));
    assertEquals(Collections.singletonList("7"), rec.getFormat("GQ"));
  }
}
