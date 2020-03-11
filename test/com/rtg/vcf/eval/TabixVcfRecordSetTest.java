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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.MathUtils;
import com.rtg.util.Pair;
import com.rtg.util.PosteriorUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.SimpleRangeMeta;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class TabixVcfRecordSetTest extends TestCase {

  private static final String CALLS = ""
    + VcfHeader.MINIMAL_HEADER + "\tSAMPLE" + LS
    + "simulatedSequence1\t583\t.\tA\tT\t50\tPASS\t.\tGT:GQ\t1/0:" + PosteriorUtils.phredIfy(18.5 * MathUtils.LOG_10) + LS
    + "simulatedSequence19\t637\t.\tG\tC\t50\tPASS\t.\tGT:GQ\t1/0:" + PosteriorUtils.phredIfy(5.3 * MathUtils.LOG_10) + LS
    + "simulatedSequence45\t737\t.\tG\tC\t50\tPASS\t.\tGT:GQ\t1/1:" + PosteriorUtils.phredIfy(7.4 * MathUtils.LOG_10) + LS;

  public void testSomeMethod() throws IOException, UnindexableDataException {
    Diagnostic.setLogStream();
    try (final TestDirectory dir = new TestDirectory("eval")) {
      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final File out = new File(dir, "other.vcf.gz");
      FileHelper.stringToGzFile(CALLS, out);
      new TabixIndexer(out).saveVcfIndex();
      final Collection<Pair<String, Integer>> names = new ArrayList<>();
      for (int seq = 1; seq < 32; ++seq) {
        names.add(new Pair<>("simulatedSequence" + seq, -1));
      }
      final ReferenceRanges<String> ranges = new ReferenceRanges<>(false);
      for (int seq = 1; seq < 32; ++seq) {
        ranges.put("simulatedSequence" + seq, new RangeList<>(new SimpleRangeMeta<>(-1, Integer.MAX_VALUE, "simulatedSequence" + seq)));
      }
      final ReferenceRegions highConf = new ReferenceRegions();
      highConf.add("simulatedSequence2", 0, Integer.MAX_VALUE);
      final VariantSet set = new TabixVcfRecordSet(input, out, ranges, highConf, names, null, null, true, false, 100, null);

      final Set<String> expected = new HashSet<>();
      for (int seq = 1; seq < 32; ++seq) {
        expected.add("simulatedSequence" + seq);
      }
      expected.remove("simulatedSequence12");
      // All other sequences either not contained in reference (N<32), or not in both baseline and calls (45)

      Pair<String, Map<VariantSetType, List<Variant>>> current;
      while ((current = set.nextSet()) != null) {
        final String currentName = current.getA();
        assertTrue("unexpected sequence <" + currentName + ">", expected.contains(currentName));
        expected.remove(currentName);
        for (Variant v : current.getB().get(VariantSetType.BASELINE)) {
          assertEquals(currentName.equals("simulatedSequence2"), !v.hasStatus(VariantId.STATUS_OUTSIDE_EVAL));
        }
        for (Variant v : current.getB().get(VariantSetType.CALLS)) {
          assertEquals(currentName.equals("simulatedSequence2"), !v.hasStatus(VariantId.STATUS_OUTSIDE_EVAL));
        }
        if (currentName.equals("simulatedSequence19")) {
          assertEquals(1, current.getB().get(VariantSetType.CALLS).size());
          assertEquals(6, current.getB().get(VariantSetType.BASELINE).size());
        }
        if (currentName.equals("simulatedSequence45")) {
          assertEquals(0, current.getB().get(VariantSetType.BASELINE).size());
          assertEquals(1, current.getB().get(VariantSetType.CALLS).size());
        }
      }
      assertTrue("these sequences weren't used: " + expected, expected.isEmpty());
    }
  }


  public void testMissingSample() throws Exception {
    final MemoryPrintStream mp = new MemoryPrintStream();
    Diagnostic.setLogStream(mp.printStream());
    try {
      try (final TestDirectory dir = new TestDirectory("indexercli")) {
        final File input = new File(dir, "foo.vcf.gz");
        FileHelper.resourceToFile("com/rtg/sam/resources/vcf.txt.gz", input);
        final File tabix = new File(dir, "foo.vcf.gz.tbi");
        FileHelper.resourceToFile("com/rtg/sam/resources/vcf.txt.gz.tbi", tabix);
        final ReferenceRanges<String> ranges = new ReferenceRanges<>(false);
        ranges.put("20", new RangeList<>(new SimpleRangeMeta<>(-1, Integer.MAX_VALUE, "20")));
        try {
          TabixVcfRecordSet.getVariantFactory(VariantSetType.CALLS, VcfUtils.getHeader(input), "asdf");
          fail();
        } catch (NoTalkbackSlimException e) {
          TestUtils.containsAll(e.toString(), "Sample \"asdf\" not found in calls VCF");
        }
      }
    } finally {
      Diagnostic.setLogStream();
    }
  }

}
