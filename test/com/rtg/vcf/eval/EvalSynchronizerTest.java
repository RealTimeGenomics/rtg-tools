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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rtg.sam.SamRangeUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.IORunnable;
import com.rtg.util.Pair;
import com.rtg.util.SimpleThreadPool;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 *         Date: 12/12/11
 *         Time: 10:57 AM
 */
public class EvalSynchronizerTest extends TestCase {
  private static class MockVariantSet implements VariantSet {
    int mSetId = 0;

    @Override
    public Pair<String, Map<VariantSetType, List<Variant>>> nextSet() {
      if (mSetId >= 3) {
        return null;
      }
      mSetId++;
      final HashMap<VariantSetType, List<Variant>> result = new HashMap<>();
      final List<Variant> empty = Collections.emptyList();
      result.put(VariantSetType.CALLS, empty);
      result.put(VariantSetType.BASELINE, empty);
      return new Pair<String, Map<VariantSetType, List<Variant>>>("name" + mSetId, result);
    }

    @Override
    public VcfHeader baseLineHeader() {
      return null;
    }

    @Override
    public VcfHeader calledHeader() {
      return null;
    }

    @Override
    public int getNumberOfSkippedBaselineVariants() {
      return 0;
    }

    @Override
    public int getNumberOfSkippedCalledVariants() {
      return 0;
    }

  }

  private static final String REC1_1 = "name1 3 . A G 0.0 PASS . GT 1/1".replaceAll(" ", "\t");
  private static final String REC1_2 = REC1_1.replaceAll("name1", "name2");
  private static final String REC2_1 = "name1 4 . C G 0.0 PASS . GT 1/1".replaceAll(" ", "\t");
  private static final String REC2_2 = REC2_1.replaceAll("name1", "name2");
  private static final String REC3_1 = "name1 5 . A G 0.0 PASS . GT 1/1".replaceAll(" ", "\t");
  private static final String REC3_2 = REC3_1.replaceAll("name1", "name2");
  private static final String REC4_1 = "name1 6 . C G 0.0 PASS . GT 1/1".replaceAll(" ", "\t");
  private static final String REC4_2 = REC4_1.replaceAll("name1", "name2");
  private static final String REC5_1 = "name1 7 . A G 0.0 PASS . GT 1/1".replaceAll(" ", "\t");
  private static final String REC5_2 = REC5_1.replaceAll("name1", "name2");
  private static final String REC6_1 = "name1 8 . C G 0.0 PASS . GT 1/1".replaceAll(" ", "\t");
  private static final String REC6_2 = REC6_1.replaceAll("name1", "name2");

  private static final String NAME1_RECS = REC1_1 + "\n" + REC2_1 + "\n" + REC3_1 + "\n" + REC4_1 + "\n" + REC5_1 + "\n" + REC6_1 + "\n";
  private static final String NAME2_RECS = REC1_2 + "\n" + REC2_2 + "\n" + REC3_2 + "\n" + REC4_2 + "\n" + REC5_2 + "\n" + REC6_2 + "\n";

  private static final String FAKE_HEADER = VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n";
  private static final String FAKE_VCF = FAKE_HEADER + NAME1_RECS + NAME2_RECS;

  public void testEvalSynchronizer() throws IOException, UnindexableDataException {
    final MemoryPrintStream mp = new MemoryPrintStream();
    Diagnostic.setLogStream(mp.printStream());
    try {
      final ByteArrayOutputStream tp = new ByteArrayOutputStream();
      final ByteArrayOutputStream fp = new ByteArrayOutputStream();
      final ByteArrayOutputStream fn = new ByteArrayOutputStream();
      try (final TestDirectory dir = new TestDirectory()) {
        final File fake = FileHelper.stringToGzFile(FAKE_VCF, new File(dir, "fake.vcf.gz"));
        new TabixIndexer(fake).saveVcfIndex();
        final VcfHeader header = new VcfHeader();
        header.addLine(VcfHeader.VERSION_LINE);
        header.addSampleName("SAMPLE");
        final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(new RegionRestriction("name1:1-30"), new RegionRestriction("name2:1-30"));
        final EvalSynchronizer sync = new EvalSynchronizer(ranges, new MockVariantSet(),
          new VcfWriter(header, tp),
          new VcfWriter(header, fp),
          new VcfWriter(header, fn),
          null,
          fake, fake, new RocContainer(RocSortOrder.DESCENDING, null));
        final Pair<String, Map<VariantSetType, List<Variant>>> pair = sync.nextSet();
        final Pair<String, Map<VariantSetType, List<Variant>>> pair2 = sync.nextSet();
        assertEquals("name1", pair.getA());
        assertEquals("name2", pair2.getA());

        final SimpleThreadPool simpleThreadPool = new SimpleThreadPool(3, "pool", true);
        simpleThreadPool.execute(new IORunnable() {
          @Override
          public void run() throws IOException {
            sync.write("name2",
              Arrays.asList(OrientedVariantTest.createOrientedVariant(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC1_2), 1, 0, RocSortValueExtractor.NULL_EXTRACTOR), true)),
              Arrays.asList(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC3_2), 3, 0, RocSortValueExtractor.NULL_EXTRACTOR)),
              Arrays.asList(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC5_2), 5, 0, RocSortValueExtractor.NULL_EXTRACTOR)), null);
            sync.addVariants(10, 0, 0);
          }
        });
        simpleThreadPool.execute(new IORunnable() {
          @Override
          public void run() throws IOException {
            sync.write("name1",
              Arrays.asList(OrientedVariantTest.createOrientedVariant(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC2_1), 2, 0, RocSortValueExtractor.NULL_EXTRACTOR), true)),
              Arrays.asList(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC4_1), 4, 0, RocSortValueExtractor.NULL_EXTRACTOR)),
              Arrays.asList(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC6_1), 6, 0, RocSortValueExtractor.NULL_EXTRACTOR)), null);
            sync.addVariants(22, 0, 0);
          }
        });
        simpleThreadPool.terminate();

        assertEquals(FAKE_HEADER + REC2_1 + "\n" + REC1_2 + "\n", tp.toString());
        assertEquals(FAKE_HEADER + REC4_1 + "\n" + REC3_2 + "\n", fp.toString());
        assertEquals(FAKE_HEADER + REC6_1 + "\n" + REC5_2 + "\n", fn.toString());
        assertEquals(32, sync.mTruePositives);
        assertEquals("name3", sync.nextSet().getA());
        assertEquals(null, sync.nextSet());
      }
      assertTrue(mp.toString().contains("Number of baseline variants processed: 32"));
      assertTrue(mp.toString().contains("Number of baseline variants processed: 22") || mp.toString().contains("Number of baseline variants processed: 10"));
    } finally {
      Diagnostic.setLogStream();
    }

  }
  public void testAbort() throws IOException, UnindexableDataException {
    final ByteArrayOutputStream tp = new ByteArrayOutputStream();
    final OutputStream fp = TestUtils.getNullOutputStream();
    final OutputStream fn = TestUtils.getNullOutputStream();
    try (final TestDirectory dir = new TestDirectory()) {
      final File fake = FileHelper.stringToGzFile(FAKE_VCF, new File(dir, "fake.vcf.gz"));
      new TabixIndexer(fake).saveVcfIndex();
      final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(new RegionRestriction("name1:1-30"), new RegionRestriction("name2:1-30"));
      final EvalSynchronizer sync = new EvalSynchronizer(ranges, new MockVariantSet(),
        new VcfWriter(new VcfHeader(), tp), new VcfWriter(new VcfHeader(), fp),
        new VcfWriter(new VcfHeader(), fn), null,
        fake, fake, new RocContainer(RocSortOrder.DESCENDING, null));
      final Pair<String, Map<VariantSetType, List<Variant>>> pair = sync.nextSet();
      final Pair<String, Map<VariantSetType, List<Variant>>> pair2 = sync.nextSet();
      assertEquals("name1", pair.getA());
      assertEquals("name2", pair2.getA());

      final SimpleThreadPool simpleThreadPool = new SimpleThreadPool(3, "pool", true);
      simpleThreadPool.execute(new IORunnable() {
        @Override
        public void run() throws IOException {
          sync.write("name2", Arrays.asList(OrientedVariantTest.createOrientedVariant(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC1_2), 0, RocSortValueExtractor.NULL_EXTRACTOR), true)), null, null, null);
          fail("Should have aborted in thread");
        }
      });
      simpleThreadPool.execute(new IORunnable() {
        @Override
        public void run() throws IOException {
          throw new IOException();
        }
      });
      try {
        simpleThreadPool.terminate();
      } catch (final IOException e) {

      }
    }
  }



  public void testInterrupt() throws IOException, InterruptedException, UnindexableDataException {
    final ByteArrayOutputStream tp = new ByteArrayOutputStream();
    final OutputStream fp = TestUtils.getNullOutputStream();
    final OutputStream fn = TestUtils.getNullOutputStream();
    try (final TestDirectory dir = new TestDirectory()) {
      final File fake = FileHelper.stringToGzFile(FAKE_VCF, new File(dir, "fake.vcf.gz"));
      new TabixIndexer(fake).saveVcfIndex();
      final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(new RegionRestriction("name1:1-30"), new RegionRestriction("name2:1-30"));
      final EvalSynchronizer sync = new EvalSynchronizer(ranges, new MockVariantSet(),
        new VcfWriter(new VcfHeader(), tp), new VcfWriter(new VcfHeader(), fp),
        new VcfWriter(new VcfHeader(), fn), null,
        fake, fake, new RocContainer(RocSortOrder.DESCENDING, null));
      final Pair<String, Map<VariantSetType, List<Variant>>> pair = sync.nextSet();
      final Pair<String, Map<VariantSetType, List<Variant>>> pair2 = sync.nextSet();
      assertEquals("name1", pair.getA());
      assertEquals("name2", pair2.getA());

      final Exception[] internalException = new Exception[1];
      final Thread t = new Thread() {
        @Override
        public void run() {
          try {
            sync.write("name2", Arrays.asList(OrientedVariantTest.createOrientedVariant(VariantTest.createVariant(VcfReader.vcfLineToRecord(REC1_2), 0, RocSortValueExtractor.NULL_EXTRACTOR), true)), null, null, null);
          } catch (final IllegalStateException e) {
            internalException[0] = e;
          } catch (final IOException e) {
          }
        }
      };
      t.start();
      t.interrupt();
      t.join();
      assertEquals("Interrupted. Unexpectedly", internalException[0].getMessage());
    }
  }
}
