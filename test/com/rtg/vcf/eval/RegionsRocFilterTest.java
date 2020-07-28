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

package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.Range;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfReaderTest;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

public class RegionsRocFilterTest extends AbstractNanoTest {

  private static final String TEMPLATE_RECORD = "chr1 %d . %s CC 20.0 PASS . GT 0/1".replaceAll(" ", "\t");

  private static final Range[] ACCEPT_INTERVALS = {
    new Range(0, 101),
    new Range(101, 101),
    new Range(110, 110),
    new Range(0, 200)
  };
  private static final Range[] REJECT_INTERVALS = {
    new Range(0, 99),
    new Range(0, 100),
    new Range(100, 100),
    new Range(110, 120),
  };

  private boolean accept(RocFilter ff, Range range) {
    final VcfRecord rec;
    if (range.getLength() == 0) {
      rec = VcfReaderTest.vcfLineToRecord(String.format(TEMPLATE_RECORD, range.getStart(), 'C'));
    } else {
      rec = VcfReaderTest.vcfLineToRecord(String.format(TEMPLATE_RECORD, range.getStart() + 1, StringUtils.repeat('A', range.getLength())));
    }
    return ff.accept(rec, VcfUtils.getValidGt(rec, 0));
  }

  private void checkRegions(RocFilter f) {
    final VcfHeader header = new VcfHeader();
    f.setHeader(header);
    for (Range r : ACCEPT_INTERVALS) {
      assertTrue(r.toString(), accept(f, r));
    }
    for (Range r : REJECT_INTERVALS) {
      assertFalse(r.toString(), accept(f, r));
    }
  }

  public void testBedRegions() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File regions = new File(dir, "regions.bed.gz");
      FileHelper.stringToGzFile(mNano.loadReference("regions_roc_filter.bed"), regions);
      final RocFilter f = new RegionsRocFilter("my_regions", regions);
      checkRegions(f);
    }
  }

  public void testVcfRegions() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File regions = new File(dir, "regions.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference("regions_roc_filter.vcf"), regions);

      final RocFilter f = new RegionsRocFilter("my_regions", regions);
      checkRegions(f);
    }
  }
}
