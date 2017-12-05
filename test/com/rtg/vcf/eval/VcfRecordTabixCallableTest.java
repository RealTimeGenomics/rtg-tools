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
import java.util.List;

import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class VcfRecordTabixCallableTest extends TestCase {

  public void testSomeMethod() throws Exception {
    try (final TestDirectory dir = new TestDirectory("eval")) {
      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final ReferenceRanges<String> ranges = new ReferenceRanges<>(false);
      ranges.put("simulatedSequence2", new RangeList<>(new RangeList.RangeData<>(-1, Integer.MAX_VALUE, "simulatedSequence2")));
      ranges.put("simulatedSequence13", new RangeList<>(new RangeList.RangeData<>(-1, Integer.MAX_VALUE, "simulatedSequence13")));
      final ReferenceRegions highConf = new ReferenceRegions();
      highConf.add("simulatedSequence2", 0, Integer.MAX_VALUE);

      final VariantFactory fact = TabixVcfRecordSet.getVariantFactory(VariantSetType.BASELINE, VcfUtils.getHeader(input), null, false);
      final VcfRecordTabixCallable runner = new VcfRecordTabixCallable(input, ranges.forSequence("simulatedSequence13"), highConf, "simulatedSequence13", -1, VariantSetType.BASELINE, fact, true, 100, null);
      List<Variant> set = runner.call().mVariants;
      for (Variant v : set) {
        assertTrue(v.hasStatus(VariantId.STATUS_OUTSIDE_EVAL));
      }
      assertEquals(2, set.size());
      final VcfRecordTabixCallable runner2 = new VcfRecordTabixCallable(input, ranges.forSequence("simulatedSequence2"), highConf, "simulatedSequence2", -1, VariantSetType.BASELINE, fact, true, 100, null);
      set = runner2.call().mVariants;
      for (Variant v : set) {
        assertFalse(v.hasStatus(VariantId.STATUS_OUTSIDE_EVAL));
      }
      assertEquals(4, set.size());
      assertEquals(215, set.get(0).getStart());
    }
  }

}
