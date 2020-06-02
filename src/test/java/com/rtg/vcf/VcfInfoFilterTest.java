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

import com.rtg.vcf.VcfFilterStatistics.Stat;

import junit.framework.TestCase;

/**
 */
public class VcfInfoFilterTest extends TestCase {

  public void testMinMaxIntFilter() {
    final VcfRecord withDepth = VcfReaderTest.vcfLineToRecord("g1\t8\t.\tAAAAA\tG\t.\tPASS\tDP=50\tGT\t1/1\t0/0");
    final VcfRecord withoutDepth = VcfReaderTest.vcfLineToRecord("g1\t8\t.\tAAAAA\tG\t.\tPASS\t.\tGT\t1/1\t0/0");
    check(withDepth, 5, 100, true);
    check(withDepth, 50, 100, true);
    check(withDepth, 5, 50, true);
    check(withDepth, 51, 100, false);
    check(withDepth, 5, 49, false);
    check(withoutDepth, 5, 100, true);
  }

  private void check(VcfRecord rec, int min, int max, boolean expected) {
    final VcfInfoFilter filter = new VcfInfoFilter.MinMaxIntFilter(new VcfFilterStatistics(), Stat.COMBINED_READ_DEPTH_FILTERED_COUNT, min, max, VcfUtils.INFO_COMBINED_DEPTH);
    assertEquals(expected, filter.accept(rec));
  }
}
