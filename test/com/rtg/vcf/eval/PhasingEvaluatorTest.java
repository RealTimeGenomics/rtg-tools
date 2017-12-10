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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.sam.SamRangeUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

public class PhasingEvaluatorTest extends TestCase {

  static List<Variant> makeVariantList(List<String> variants, StringBuilder sb) {
    final List<Variant> callList = new ArrayList<>();
    int id = 0;
    for (String s : variants) {
      final String vartab = s.replaceAll(" ", "\t");
      ++id;
      callList.add(VariantTest.createVariant(VcfReader.vcfLineToRecord(vartab), id, 0));
      sb.append(vartab).append("\n");
    }
    return callList;
  }

  private static class MockVariantSet implements VariantSet {
    boolean mSent = false;
    Map<VariantSetType, List<Variant>> mMap;
    StringBuilder mBaselineVcf = new StringBuilder(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n");
    StringBuilder mCallsVcf = new StringBuilder(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n");
    final VcfHeader mHeader;
    MockVariantSet(List<String> base, List<String> calls) {
      mMap = new EnumMap<>(VariantSetType.class);
      mMap.put(VariantSetType.BASELINE, makeVariantList(base, mBaselineVcf));
      mMap.put(VariantSetType.CALLS, makeVariantList(calls, mCallsVcf));
      mHeader = new VcfHeader();
      mHeader.addLine(VcfHeader.VERSION_LINE);
      mHeader.addSampleName("SAMPLE");
    }
    @Override
    public Pair<String, Map<VariantSetType, List<Variant>>> nextSet() {
      final Map<VariantSetType, List<Variant>> map;
      if (!mSent) {
         map = mMap;
        mSent = true;
      } else {
        map = null;
      }
      return new Pair<>("10", map);
    }

    @Override
    public VcfHeader baselineHeader() {
      return mHeader;
    }

    @Override
    public VcfHeader calledHeader() {
      return mHeader;
    }

    @Override
    public int getNumberOfSkippedBaselineVariants() {
      return 0;
    }

    @Override
    public int getNumberOfSkippedCalledVariants() {
      return 0;
    }

    @Override
    public VcfIterator getBaselineVariants(String sequenceName) throws IOException {
      return null;
    }

    @Override
    public VcfIterator getCalledVariants(String sequenceName) throws IOException {
      return null;
    }

    @Override
    public void close() { }
  }

  public void testPhasing() throws IOException, UnindexableDataException {
    final int expectedUnphasable = 0;
    final int expectedMisPhasings = 1;
    final int expectedCorrect = 0;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
          "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 0|1"
    ), Arrays.asList(
          "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 1|0"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);
  }

  public void testDoublePhasing() throws IOException, UnindexableDataException {
    final int expectedUnphasable = 0;
    final int expectedMisPhasings = 2;
    final int expectedCorrect = 0;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 0|1"
        , "10 16 . G T 0.0 PASS . GT 0|1"
    ), Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 1|0"
        , "10 16 . G T 0.0 PASS . GT 0|1"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);
  }

  public void testPhasingUnphased() throws IOException, UnindexableDataException {
    // Test that phase counting resumes correctly after un phased call
    final int expectedUnphasable = 0;
    final int expectedMisPhasings = 2;
    final int expectedCorrect = 1;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 0|1"
        , "10 16 . G T 0.0 PASS . GT 0|1"
        , "10 19 . G T 0.0 PASS . GT 0|1"
        , "10 22 . G T 0.0 PASS . GT 0|1"
        , "10 25 . G T 0.0 PASS . GT 1|0"
    ), Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 1|0"
        , "10 16 . G T 0.0 PASS . GT 1|0"
        , "10 19 . G T 0.0 PASS . GT 0/1"
        , "10 22 . G T 0.0 PASS . GT 1|0"
        , "10 25 . G T 0.0 PASS . GT 1|0"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);

  }

  public void testPhasingUnphased2() throws IOException, UnindexableDataException {
    // Test that phase change after an un phased call isn't counted
    final int expectedUnphasable = 0;
    final int expectedMisPhasings = 1;
    final int expectedCorrect = 2;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 0|1"
        , "10 16 . G T 0.0 PASS . GT 0|1"
        , "10 19 . G T 0.0 PASS . GT 0|1"
        , "10 22 . G T 0.0 PASS . GT 0|1"
        , "10 25 . G T 0.0 PASS . GT 1|0"
    ), Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 1|0"
        , "10 16 . G T 0.0 PASS . GT 1|0"
        , "10 19 . G T 0.0 PASS . GT 0/1"
        , "10 22 . G T 0.0 PASS . GT 0|1"
        , "10 25 . G T 0.0 PASS . GT 1|0"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);
  }

  public void testCluster() throws IOException, UnindexableDataException {
    final int expectedUnphasable = 0;
    final int expectedMisPhasings = 2;
    final int expectedCorrect = 3;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 0|1"
        , "10 15 . G T 0.0 PASS . GT 0|1"
        , "10 17 . G T 0.0 PASS . GT 0|1"
        , "10 19 . G T 0.0 PASS . GT 0|1"
        , "10 25 . G T 0.0 PASS . GT 1|0"
    ), Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 0|1"
        , "10 13 . G T 0.0 PASS . GT 1|0"
        , "10 15 . G T 0.0 PASS . GT 1|0"
        , "10 17 . G T 0.0 PASS . GT 0|1"
        , "10 19 . G T 0.0 PASS . GT 1|0"
        , "10 25 . G T 0.0 PASS . GT 0|1"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);
  }

  public void testUnphaseable() throws IOException, UnindexableDataException {
    final int expectedUnphasable = 1;
    final int expectedMisPhasings = 0;
    final int expectedCorrect = 0;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 0/1"
    ), Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 1|0"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);
  }

  public void testFalsePositive() throws IOException, UnindexableDataException {
    final int expectedUnphasable = 0;
    final int expectedMisPhasings = 1;
    final int expectedCorrect = 0;
    final MockVariantSet variants = new MockVariantSet(Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 16 . G T 0.0 PASS . GT 0|1"
    ), Arrays.asList(
        "10 9 . G T 0.0 PASS . GT 1|0"
        , "10 13 . G T 0.0 PASS . GT 1|0"
        , "10 16 . G T 0.0 PASS . GT 1|0"
    ));
    checkPhasing(expectedCorrect, expectedUnphasable, expectedMisPhasings, variants);
  }

  private class MockEvalSynchronizer extends EvalSynchronizer {
    private int mMisPhasings;
    private int mUnphasable;
    private int mCorrectPhasings;
    MockEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges) {
      super(variants);
    }
    @Override
    void writeInternal(String sequenceName, Collection<? extends VariantId> baseline, Collection<? extends VariantId> calls, List<Integer> syncPoints, List<Integer> syncPoints2) throws IOException {
    }
    @Override
    void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
      mMisPhasings += misPhasings;
      mUnphasable += unphasable;
      mCorrectPhasings += correctPhasings;
    }
    @Override
    public void close() throws IOException {
    }
  }

  private void checkPhasing(int expectedCorrect, int expectedUnphasable, int expectedMisPhasings, MockVariantSet variants) throws IOException, UnindexableDataException {
    Diagnostic.setLogStream();
    try (final TestDirectory dir = new TestDirectory()) {
      final File calls = FileHelper.stringToGzFile(variants.mCallsVcf.toString(), new File(dir, "calls.vcf.gz"));
      new TabixIndexer(calls).saveVcfIndex();
      final File baseline = FileHelper.stringToGzFile(variants.mBaselineVcf.toString(), new File(dir, "baseline.vcf.gz"));
      new TabixIndexer(baseline).saveVcfIndex();
      final SequencesReader reader = ReaderTestUtils.getReaderDnaMemory(VcfEvalTaskTest.REF);
      try (final MockEvalSynchronizer sync = new MockEvalSynchronizer(baseline, calls, variants, SamRangeUtils.createFullReferenceRanges(reader))) {
        final SequenceEvaluator eval = new SequenceEvaluator(sync, Collections.singletonMap("10", 0L), reader);
        eval.run();
        assertEquals("correctphasings: " + sync.mCorrectPhasings + ", misphasings: " + sync.mMisPhasings + ", unphaseable: " + sync.mUnphasable, expectedCorrect, sync.mCorrectPhasings);
        assertEquals("misphasings: " + sync.mMisPhasings + ", unphaseable: " + sync.mUnphasable, expectedMisPhasings, sync.mMisPhasings);
        assertEquals(expectedUnphasable, sync.mUnphasable);
      }
    }
  }
}
