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

package com.rtg.tabix;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.sam.SamRangeUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.NanoRegression;

import junit.framework.TestCase;

/**
 */
public class TabixLineReaderTest extends TestCase {

  protected NanoRegression mNano = null;

  @Override
  public void setUp() throws IOException {
    Diagnostic.setLogStream();
    mNano = new NanoRegression(this.getClass());
  }

  @Override
  public void tearDown() throws IOException {
    Diagnostic.setLogStream();
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }

  public static String extractRecords(File input, File tabix, RegionRestriction range) throws IOException {
    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      final OutputStream out = mps.outputStream();
      try (TabixLineReader reader = new TabixLineReader(input, tabix, range)) {
        String line;
        while ((line = reader.readLine()) != null) {
          out.write(line.getBytes());
          out.write(StringUtils.LS.getBytes());
        }
      }
      return mps.toString();
    }
  }

  public static String extractRecords(File input, File tabix, ReferenceRanges<String> ranges) throws IOException {
    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      final OutputStream out = mps.outputStream();
      try (TabixLineReader reader = new TabixLineReader(input, tabix, ranges)) {
        String line;
        while ((line = reader.readLine()) != null) {
          out.write(line.getBytes());
          out.write(StringUtils.LS.getBytes());
        }
      }
      return mps.toString();
    }
  }

  public void testSingleRegion() throws IOException {
    try (final TestDirectory dir = new TestDirectory("TabixLineReader")) {

      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);

      final RegionRestriction region = new RegionRestriction("simulatedSequence19", 500, 1000);
      final String result = extractRecords(input, tabix, region);
      mNano.check("tlr-single-region", result);

      final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(region);
      final String result2 = extractRecords(input, tabix, ranges);
      assertEquals(result, result2);
    }
  }

  public void testMultiRegion() throws IOException {
    try (final TestDirectory dir = new TestDirectory("TabixLineReader")) {

      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(
        new RegionRestriction("simulatedSequence2", 215, 3345),
        new RegionRestriction("simulatedSequence14", 0, 1567),
        new RegionRestriction("simulatedSequence14", 1567, 10000),
        new RegionRestriction("simulatedSequence19")
        );
      final String result = extractRecords(input, tabix, ranges);
      mNano.check("tlr-multi-region", result);
    }
  }
}
