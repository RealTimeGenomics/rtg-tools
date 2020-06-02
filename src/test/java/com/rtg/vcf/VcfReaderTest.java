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

import com.rtg.util.TestUtils;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfReaderTest extends TestCase {

  /**
   * Turn a single line of <code>VCF</code> output into a {@link VcfRecord} for tests.
   * @param line line of file
   * @return the corresponding record
   */
  public static VcfRecord vcfLineToRecord(String line) {
    return new VcfParser().parseLine(line);
  }

  protected void checkVcfFormatException(String vcfTxt, String... contains) throws IOException {
    try (final BufferedReader r = new BufferedReader(new StringReader(vcfTxt))) {
      new VcfReaderFactory().make(r);
      fail("VcfFormatException expected from .vcf file: " + vcfTxt);
    } catch (final VcfFormatException ex) {
      TestUtils.containsAll(ex.getMessage(), contains);
    }
  }

  static final String[] BAD_RECORD = {
    "chr1   123    foo  G    A    29    PASS    X=yy;DP=7    GT:GQ",             // HEADER0 says should not be any samples
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34",    // HEADER0 says should not be any samples
  };
  static final String[] BAD_RECORD_SAMPLE = {
    "chr1   123    foo  A    T    100   PASS    XRX          GT      1/0   1/0", // Too many samples
  };

  public void testBadRecords() throws IOException {
    for (final String recText : BAD_RECORD) {
      final String badrec = recText.replaceAll(" +", TAB);
      checkVcfFormatException(HEADER0 + badrec + LS, "Invalid VCF record");
    }
    for (final String recText : BAD_RECORD_SAMPLE) {
      final String badrec = recText.replaceAll(" +", TAB);
      checkVcfFormatException(HEADER0_B + badrec + LS, "Invalid VCF record");
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
    final VcfReader reader = new VcfReaderFactory().make(new BufferedReader(in));
    final VcfHeader hdr = reader.getHeader();
    assertNotNull(hdr);
    assertEquals(0, hdr.getGenericMetaInformationLines().size());
    assertEquals(0, hdr.getNumberOfSamples());
    assertFalse(reader.hasNext());
  }
}
