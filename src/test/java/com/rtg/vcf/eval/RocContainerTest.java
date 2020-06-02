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

import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 */
public class RocContainerTest extends TestCase {
  public void test() throws IOException {
    try (final TestDirectory dir = new TestDirectory("roc")) {
      final RocContainer roc = new RocContainer(new RocSortValueExtractor() {
        @Override
        public boolean requiresSample() {
          return false;
        }
        @Override
        public double getSortValue(VcfRecord rec, int sampleNo) {
          return 0;
        }
        @Override
        public RocSortOrder getSortOrder() {
          return RocSortOrder.DESCENDING;
        }
        @Override
        public String toString() {
          return "TEST";
        }
      });
      roc.addFilter(RocFilter.ALL);
      //EnumSet<RocFilter> v = EnumSet.of(RocFilter.ALL);
      final RocFilter v = RocFilter.ALL;
      roc.addRocLine(new RocPoint<>(0.1, 1.0, 0.0, 1.0), v);
      roc.addRocLine(new RocPoint<>(0.1, 0.0, 1.0, 0.0), v);
      roc.addRocLine(new RocPoint<>(0.2, 0.0, 1.0, 0.0), v);
      roc.addRocLine(new RocPoint<>(0.1, 0.5, 0.0, 1.0), v);
      roc.addRocLine(new RocPoint<>(0.2, 1.5, 0.0, 1.0), v);
      roc.addRocLine(new RocPoint<>(0.1, 0.5, 0.0, 1.0), v);
      roc.addRocLine(new RocPoint<>(0.3, 1.5, 0.0, 1.0), v);
      roc.mBaselineTotals.add(v, 5);
      roc.mBaselineTpTotal.add(v, 5);
      roc.writeRocs(dir, true, false);
      final File allFile = new File(dir, "weighted_roc.tsv.gz");
      final String all = FileHelper.gzFileToString(allFile);
      TestUtils.containsAll(all,
              "0.100\t5.00\t2",
              "0.200\t3.00\t1",
              "0.300\t1.50\t0");
    }
  }
}
