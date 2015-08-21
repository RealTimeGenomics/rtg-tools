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

import com.rtg.util.TestUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.vcf.VcfFilterStatistics.Stat;

import junit.framework.TestCase;

/**
 */
@SuppressWarnings("fallthrough")
public class VcfFilterStatisticsTest extends TestCase {

  public void test() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final MemoryPrintStream stream = new MemoryPrintStream();
    stats.printStatistics(stream.outputStream());
    final String expected = "" + LS
        + "Total records : 0" + LS
        + "Remaining records : 0" + LS
        ;
    assertEquals(expected, stream.toString());
    stream.reset();
    stats.increment(Stat.TOTAL_COUNT);
    stats.incrementInfoTag("Bar");
    stats.incrementFilterTag("Foo");
    stats.printStatistics(stream.outputStream());
    final String expected2  = "" + LS
                            + "Total records : 1" + LS
                            + "Filtered due to Foo : 1" + LS
                            + "Filtered due to Bar : 1" + LS
                            + "Remaining records : 0" + LS
        ;
    assertEquals(expected2, stream.toString());
    stream.reset();

    for (int i = 0; i < 15; i++) {
      for (VcfFilterStatistics.Stat s : Stat.values()) {
        if (s.ordinal() > i - 2) {
          stats.increment(s);
        }
      }
    }

    stats.incrementInfoTag("Bar");
    stats.incrementFilterTag("Foo");
    stats.printStatistics(stream.outputStream());
    TestUtils.containsAll(stream.toString(),
        "Total records",
        "Filtered due to Foo",
        "Filtered due to Bar",
        "Filtered due to quality",
        "Filtered due to genotype quality",
        "Filtered due to AVR score",
        "Filtered due to sample read depth",
        "Filtered due to combined read depth",
        "Filtered due to ambiguity ratio",
        "Filtered due to allele balance",
        "Filtered due to same as reference",
        "Filtered due to all samples same as reference",
        "Filtered due to not a SNP",
        "Filtered due to simple SNP",
        "Filtered due to not in keep set",
        "Filtered due to overlap with previous",
        "Filtered due to density window",
        "Filtered due to include file",
        "Filtered due to exclude file",
        "Remaining records");
  }
}
