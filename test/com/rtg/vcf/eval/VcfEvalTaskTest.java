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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.OutputParams;
import com.rtg.mode.SequenceType;
import com.rtg.reader.MockArraySequencesReader;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfId;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.MathUtils;
import com.rtg.util.PosteriorUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.IntervalComparator;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.NanoRegression;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfEvalTaskTest extends TestCase {
  private TestDirectory mDir = null;
  NanoRegression mNano = null;
  @Override
  public void setUp() throws IOException {
    mDir = new TestDirectory("vcfevaltask");
    Diagnostic.setLogStream(TestUtils.getNullPrintStream());
    mNano = new NanoRegression(this.getClass());
  }

  @Override
  public void tearDown() throws IOException {
    try {
      mDir.close();
    } finally {
      mDir = null;
    }
    Diagnostic.setLogStream();
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }

  private static final String TEMPLATE = ">seq" + StringUtils.LS
    + "ACAGTCACGGTACGTACGTACGTACGT" + StringUtils.LS;

  private static final String[] SIMPLE_BOTH = {"seq 3 . A G 0.0 PASS . GT 1/1"};
  private static final String[] TP_OUT = SIMPLE_BOTH;
  private static final String[] SIMPLE_CALLED_ONLY = {"seq 6 . C T 0.0 PASS . GT 0/1"};
  private static final String[] FP_OUT = SIMPLE_CALLED_ONLY;
  private static final String[] SIMPLE_BASELINE_ONLY = {"seq 9 . G T 0.0 PASS . GT 1/1"};
  private static final String[] FN_OUT = SIMPLE_BASELINE_ONLY;

  /** vcf header */
  private static final String CALLS_HEADER = VcfHeader.MINIMAL_HEADER + "\tRTG";
  private static final String MUTATIONS_HEADER = VcfHeader.MINIMAL_HEADER + "\tRTG";

  public void test() throws IOException, UnindexableDataException {
    check(SIMPLE_BOTH, SIMPLE_CALLED_ONLY, SIMPLE_BASELINE_ONLY, TP_OUT, FP_OUT, FN_OUT, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }

  private void check(String[] both, String[] calledOnly, String[] baselineOnly, String[] tpOut, String[] fpOut, String[] fnOut, String sortField) throws IOException, UnindexableDataException {
    check(TEMPLATE, both, calledOnly, baselineOnly, tpOut, fpOut, fnOut, true, sortField);
    check(TEMPLATE, both, calledOnly, baselineOnly, tpOut, fpOut, fnOut, false, sortField);
  }
  private void check(String ref, String[] both, String[] calledOnly, String[] baselineOnly, String[] tpOut, String[] fpOut, String[] fnOut, boolean zip, String sortField) throws IOException, UnindexableDataException {
    try (TestDirectory tdir = new TestDirectory()) {
      createInput(tdir, both, calledOnly, baselineOnly);

      final File calls = new File(tdir, "calls.vcf.gz");
      final File mutations = new File(tdir, "mutations.vcf.gz");
      final File out = FileUtils.createTempDir("out", zip ? "zip" : "notZipped", tdir);
      final File template = new File(tdir, "template");
      //System.err.println("baseline \n" + FileUtils.fileToString(mutations));
      //System.err.println("calls \n" + FileUtils.fileToString(calls));
      ReaderTestUtils.getReaderDNA(ref, template, null).close();
      final VcfEvalParams params = VcfEvalParams.builder().baseLineFile(mutations).callsFile(calls)
        .templateFile(template).outputParams(new OutputParams(out, false, zip)).scoreField(sortField).create();
      VcfEvalTask.evaluateCalls(params);
      final String zipSuffix = zip ? ".gz" : "";
      final File tpOutFile = new File(out, "tp.vcf" + zipSuffix);
      final File fpOutFile = new File(out, "fp.vcf" + zipSuffix);
      final File fnOutFile = new File(out, "fn.vcf" + zipSuffix);
      final String tpResults = fileToString(tpOutFile, zip);
      final String fpResults = fileToString(fpOutFile, zip);
      final String fnResults = fileToString(fnOutFile, zip);
      final String header = "##fileformat=VCFv4.1\n" + "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tRTG";
      if (zip) {
        assertTrue(new File(out, "tp.vcf" + zipSuffix + TabixIndexer.TABIX_EXTENSION).exists());
        assertTrue(new File(out, "fp.vcf" + zipSuffix + TabixIndexer.TABIX_EXTENSION).exists());
        assertTrue(new File(out, "fn.vcf" + zipSuffix + TabixIndexer.TABIX_EXTENSION).exists());
      }

      //System.err.println("TP \n" + tpResults);
      //System.err.println("FP \n" + fpResults);
      //System.err.println("FN \n" + fnResults);
      TestUtils.containsAll(tpResults, tpOut);
      TestUtils.containsAll(tpResults, header.replaceAll("\t", " "));
      TestUtils.containsAll(fpResults, fpOut);
      TestUtils.containsAll(fpResults, header.replaceAll("\t", " "));
      TestUtils.containsAll(fnResults, fnOut);
      TestUtils.containsAll(fnResults, header.replaceAll("\t", " "));
      final File phase = new File(out, "phasing.txt");
      assertTrue(phase.exists());
      final String phasing = FileUtils.fileToString(phase);
      TestUtils.containsAll(phasing, "Correct phasings: ", "Incorrect phasings: ", "Unresolvable phasings: ");
    }
  }
  private String fileToString(File f, boolean zip) throws IOException {
    final String result;
    if (zip) {
      result = FileHelper.gzFileToString(f);
    } else {
      result = FileUtils.fileToString(f);
    }
    return result.replaceAll("\t", " ");
  }

  private void createInput(File dir, String[] both, String[] calledOnly, String[] baselineOnly) throws IOException, UnindexableDataException {
    final File calls = new File(dir, "calls.vcf.gz");
    final File mutations = new File(dir, "mutations.vcf.gz");
    final TreeMap<DetectedVariant, String> callList = new TreeMap<>();
    final TreeMap<DetectedVariant, String> mutationList = new TreeMap<>();
    for (final String var : both) {
      final VcfRecord rec = VcfReader.vcfLineToRecord(var.replaceAll(" ", "\t"));
      callList.put(new DetectedVariant(rec, 0, RocSortValueExtractor.NULL_EXTRACTOR, false), rec.toString());
      mutationList.put(new DetectedVariant(rec, 0, RocSortValueExtractor.NULL_EXTRACTOR, false), rec.toString());
    }
    for (final String var : calledOnly) {
      final VcfRecord rec = VcfReader.vcfLineToRecord(var.replaceAll(" ", "\t"));
      callList.put(new DetectedVariant(rec, 0, RocSortValueExtractor.NULL_EXTRACTOR, false), rec.toString());
    }
    for (final String var : baselineOnly) {
      final VcfRecord rec = VcfReader.vcfLineToRecord(var.replaceAll(" ", "\t"));
      mutationList.put(new DetectedVariant(rec, 0, RocSortValueExtractor.NULL_EXTRACTOR, false), rec.toString());
    }
    try (BufferedWriter callOut = new BufferedWriter(new OutputStreamWriter(FileUtils.createOutputStream(calls, true, false)))) {
      callOut.write(CALLS_HEADER.replaceAll(" ", "\t") + StringUtils.LS);
      for (final Entry<DetectedVariant, String> var : callList.entrySet()) {
        callOut.write(var.getValue() + "\n");
      }
    }
    new TabixIndexer(calls).saveVcfIndex();
    try (BufferedWriter mutOut = new BufferedWriter(new OutputStreamWriter(FileUtils.createOutputStream(mutations, true, false)))) {
      mutOut.write(MUTATIONS_HEADER.replaceAll(" ", "\t") + StringUtils.LS);
      for (final Entry<DetectedVariant, String> var : mutationList.entrySet()) {
        mutOut.write(var.getValue() + "\n");
      }
    }
    new TabixIndexer(mutations).saveVcfIndex();

  }
  //   12 3456 789 012 34567890 123 456789
  //   01 234 5 67 890 12345 6789 012 3456
  //  "AC AGT C AC GGT ACGTA CGTA CGT ACGT"
  //   AC GGT C AC TGT
  //   AC GGT T AC GGT

  private static final String[] TP_LARGE = {
      "seq 3 . A G 5.0 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(5.0 * MathUtils.LOG_10)
    , "seq 12 . A C 3.0 PASS . GT:GQ 0/1:" + PosteriorUtils.phredIfy(3.0 * MathUtils.LOG_10)
    , "seq 14 . G GA,T 1.0 PASS . GT:GQ 1/2:" + PosteriorUtils.phredIfy(1.0 * MathUtils.LOG_10)
  };
  private static final String[] FP_LARGE = {
      "seq 6 . C T 4.0 PASS . GT:GQ 1/0:" + PosteriorUtils.phredIfy(4.0 * MathUtils.LOG_10)
    , "seq 23 . T G 0.0 PASS . GT 1/1"
    , "seq 26 . G C 2.0 PASS . GT:GQ 0/1:" + PosteriorUtils.phredIfy(2.0 * MathUtils.LOG_10)
  };
  private static final String[] FN_LARGE = {
       "seq 9 . G T 0.0 PASS . GT 1/1"
    , "seq 16 . A T 0.0 PASS . GT 0/1"
    , "seq 20 . A T 0.0 PASS . GT 1/1"
  };

  public void testLarge() throws IOException, UnindexableDataException {
    check(TP_LARGE, FP_LARGE, FN_LARGE, TP_LARGE, FP_LARGE, FN_LARGE, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }


  public void testROC() throws IOException, UnindexableDataException {
    checkRoc("testroc", TEMPLATE, TP_LARGE, FP_LARGE, FN_LARGE);
  }

  private void checkRoc(String label, String template, String[] both, String[] calledOnly, String[] baselineOnly) throws IOException, UnindexableDataException {
    checkRoc(label, template, both, calledOnly, baselineOnly, true);
  }

  private void checkRoc(String label, String template, String[] both, String[] calledOnly, String[] baselineOnly, boolean checktotal) throws IOException, UnindexableDataException {
    checkRoc(label, template, both, calledOnly, baselineOnly, checktotal, true);
    checkRoc(label, template, both, calledOnly, baselineOnly, checktotal, false);
  }
  private void checkRoc(String label, String template, String[] both, String[] calledOnly, String[] baselineOnly, boolean checktotal, boolean rtgStats) throws IOException, UnindexableDataException {
    try (TestDirectory tdir = new TestDirectory()) {
      createInput(tdir, both, calledOnly, baselineOnly);

      final File calls = new File(tdir, "calls.vcf.gz");
      final File mutations = new File(tdir, "mutations.vcf.gz");
      final File out = FileUtils.createTempDir("out-rtgstats-" + rtgStats, "", tdir);
      final File genome = new File(tdir, "template");
      ReaderTestUtils.getReaderDNA(template, genome, null).close();
      final VcfEvalParams params = VcfEvalParams.builder().baseLineFile(mutations).callsFile(calls)
        .templateFile(genome).outputParams(new OutputParams(out, false, false))
        .rtgStats(rtgStats)
        .create();
      VcfEvalTask.evaluateCalls(params);
      final int tpCount = both.length;
      final int fnCount = baselineOnly.length;

      mNano.check(label + "-summary.txt", FileUtils.fileToString(new File(out, CommonFlags.SUMMARY_FILE)));
      checkRocResults(label + "-weighted.tsv", new File(out, VcfEvalTask.FULL_ROC_FILE), checktotal, tpCount, fnCount);
      checkRocResults(label + "-homo.tsv", new File(out, VcfEvalTask.HOMOZYGOUS_FILE), checktotal, tpCount, fnCount);
      checkRocResults(label + "-hetero.tsv", new File(out, VcfEvalTask.HETEROZYGOUS_FILE), checktotal, tpCount, fnCount);
      if (rtgStats) {
        checkRocResults(label + "-simple.tsv", new File(out, VcfEvalTask.SIMPLE_FILE), checktotal, tpCount, fnCount);
        checkRocResults(label + "-complex.tsv", new File(out, VcfEvalTask.COMPLEX_FILE), checktotal, tpCount, fnCount);
      } else {
        assertFalse(new File(out, VcfEvalTask.SIMPLE_FILE).exists());
        assertFalse(new File(out, VcfEvalTask.COMPLEX_FILE).exists());
      }

      final VcfEvalParams paramsrev = VcfEvalParams.builder().baseLineFile(mutations).callsFile(calls)
        .templateFile(genome).outputParams(new OutputParams(out, false, false))
        .sortOrder(RocSortOrder.ASCENDING)
        .rtgStats(rtgStats)
        .create();
      VcfEvalTask.evaluateCalls(paramsrev);
      checkRocResults(label + "-weighted-rev.tsv", new File(out, VcfEvalTask.FULL_ROC_FILE), checktotal, tpCount, fnCount);
    }
  }

  private void checkRocResults(String label, final File out, boolean checktotal, final int tpCount, final int fnCount) throws IOException {
    final String roc = FileUtils.fileToString(out);
    //System.err.println("ROC\n" + roc);
    final String[] homoLines = roc.split(StringUtils.LS);
    if (checktotal) {
      assertEquals("#total baseline variants: " + (tpCount + fnCount), homoLines[0]);
    } else {
      assertTrue(homoLines[0].startsWith("#total baseline variants: "));
    }
    assertTrue(homoLines[1].startsWith("#score field: "));
    assertTrue(homoLines[2].startsWith("#score\t"));
    mNano.check(label, roc);
  }

  private static final String[] EMPTY = {};
  private static final String[] CALLED_ONLY_TRICKY = {
      "seq 1 . A T 1 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(1 * MathUtils.LOG_10)
    , "seq 2 . A AT 9 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(9 * MathUtils.LOG_10)
    , "seq 5 . GT G 2 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(2 * MathUtils.LOG_10)
    , "seq 10 . CGT AGA 3 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(3 * MathUtils.LOG_10)
    , "seq 14 . C A 8 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(8 * MathUtils.LOG_10)
    , "seq 16 . T A 4 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(4 * MathUtils.LOG_10)
    , "seq 20 . C A 10 PASS . GT:GQ 0/1:" + PosteriorUtils.phredIfy(10 * MathUtils.LOG_10)
  };

  private static final String TRICKY_TEMPLATE = ">seq" + StringUtils.LS
      + "ACTTTCCCACGTACGTCCTCT" + StringUtils.LS;

  private static final String[] BASELINE_ONLY_TRICKY = {
      "seq 5 . TC T 0.0 PASS . GT 1/1"
    , "seq 7 . C CC 0.0 PASS . GT 1/1"
    , "seq 10 . C A 0.0 PASS . GT 1/1"
    , "seq 12 . T A 0.0 PASS . GT 1/1"
    , "seq 14 . CGT AGA 0.0 PASS . GT 1/1"
    , "seq 18 . C A 0.0 PASS . GT 1/1"
    , "seq 20 . C A 0.0 PASS . GT 0/1"

  };

  public void testROCTricky() throws IOException, UnindexableDataException {
    checkRoc("tricky", TRICKY_TEMPLATE, EMPTY, CALLED_ONLY_TRICKY, BASELINE_ONLY_TRICKY, false);
  }

  private static final String[] CALLED_ONLY_TRICKY_XRX = {
    "seq 1 . A T 1 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(1 * MathUtils.LOG_10)
    , "seq 2 . A AT 9 PASS XRX GT:GQ 1/1:" + PosteriorUtils.phredIfy(9 * MathUtils.LOG_10)
    , "seq 5 . GT G 2 PASS XRX GT:GQ 1/1:" + PosteriorUtils.phredIfy(2 * MathUtils.LOG_10)
    , "seq 10 . CGT AGA 3 PASS XRX GT:GQ 1/1:" + PosteriorUtils.phredIfy(3 * MathUtils.LOG_10)
    , "seq 14 . C A 8 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(8 * MathUtils.LOG_10)
    , "seq 16 . T A 4 PASS . GT:GQ 1/1:" + PosteriorUtils.phredIfy(4 * MathUtils.LOG_10)
    , "seq 20 . C A 10 PASS . GT:GQ 0/1:" + PosteriorUtils.phredIfy(10 * MathUtils.LOG_10)
  };

  private static final String[] BASELINE_ONLY_TRICKY_XRX = {
    "seq 5 . TC T 0.0 PASS XRX GT 1/1",
    "seq 7 . C CC 0.0 PASS XRX GT 1/1",
    "seq 10 . C A 0.0 PASS . GT 1/1",
    "seq 12 . T A 0.0 PASS . GT 1/1",
    "seq 14 . CGT AGA 0.0 PASS XRX GT 1/1",
    "seq 18 . C A 0.0 PASS . GT 1/1",
    "seq 20 . C A 0.0 PASS . GT 0/1"

  };

  // Same as testROCTricky but checks breakdown of roc files for XRX
  public void testROCTrickyXRX() throws IOException, UnindexableDataException {
    checkRoc("trickyxrx", TRICKY_TEMPLATE, EMPTY, CALLED_ONLY_TRICKY_XRX, BASELINE_ONLY_TRICKY_XRX);
  }

  private static final String[] FN_ROC_EMPTY = {
    "seq 20 . C A 0.0 PASS . GT 0/1"
  };
  private static final String[] FP_ROC_EMPTY = {
      "seq 1 . A T 1 PASS . GT 1/1"  // No GQ so will be omitted from ROC
  };

  public void testROCEmpty() throws IOException, UnindexableDataException {
    checkRoc("rocempty", TRICKY_TEMPLATE, EMPTY, FP_ROC_EMPTY, FN_ROC_EMPTY);
  }

  public void testOutsideRef() throws IOException, UnindexableDataException {
    try (TestDirectory tdir = new TestDirectory()) {

      createInput(tdir, new String[]{"seq 28 . A G 0.0 PASS . GT 1/1"}, new String[]{"seq 30 . C T 0.0 PASS . GT 0/1"}, new String[]{});
      final File calls = new File(tdir, "calls.vcf.gz");
      final File mutations = new File(tdir, "mutations.vcf.gz");
      final File out = FileUtils.createTempDir("outsideRef", "out", tdir);
      final File template = new File(tdir, "template");

      ReaderTestUtils.getReaderDNA(TEMPLATE, template, null).close();
      final MemoryPrintStream ps = new MemoryPrintStream();
      Diagnostic.setLogStream(ps.printStream());
      try {
        final VcfEvalParams params = VcfEvalParams.builder().baseLineFile(mutations).callsFile(calls)
          .restriction(new RegionRestriction("seq", 0, 300))
          .templateFile(template).outputParams(new OutputParams(out, false, false)).create();
        VcfEvalTask.evaluateCalls(params);
      } finally {
        Diagnostic.setLogStream();
      }
      TestUtils.containsAll(ps.toString(),
        "Variant in calls at seq:28 starts outside the length of the reference sequence (27).",
        "Variant in baseline at seq:28 starts outside the length of the reference sequence (27).",
        "Variant in calls at seq:30 starts outside the length of the reference sequence (27).",
        "There were 1 baseline variants skipped due to being too long, overlapping or starting outside the expected reference sequence length.",
        "There were 2 called variants skipped due to being too long, overlapping or starting outside the expected reference sequence length."
      );
    }
  }

  public void testGetSdfId() {
    Diagnostic.setLogStream();
    assertEquals(new SdfId(0), VcfEvalTask.getSdfId(null));
    final String label = "##TEMPLATE-SDF-ID=";
    final ArrayList<String> header = new ArrayList<>();
    header.add(label + "blahtblah");
    try {
      VcfEvalTask.getSdfId(header);
      fail();
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid header line : " + label + "blahtblah", e.getMessage());
    }
    header.clear();
    header.add(label + "blah");
    try {
      VcfEvalTask.getSdfId(header);
      fail();
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid header line : " + label + "blah", e.getMessage());
    }
    header.clear();
    header.add(label + new SdfId(42).toString());
    assertEquals(new SdfId(42), VcfEvalTask.getSdfId(header));
  }

  public void testPeek() throws IOException {
    Diagnostic.setLogStream();
    BufferedReader reader = new BufferedReader(new StringReader("ooogablah" + StringUtils.LS));
    assertEquals("o", VcfEvalTask.peek(reader));
    reader = new BufferedReader(new StringReader(""));
    assertEquals("", VcfEvalTask.peek(reader));
  }

  public void testCheckHeader() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    Diagnostic.setLogStream(ps.printStream());
    try {
      final String label = "##TEMPLATE-SDF-ID=";
      BufferedReader generated = new BufferedReader(new StringReader(""));
      BufferedReader detected = new BufferedReader(new StringReader(""));
      try {
        VcfEvalTask.checkHeader(generated, detected, new SdfId(42));
        fail();
      } catch (final NoTalkbackSlimException ntse) {
        assertEquals("No header found in baseline file", ntse.getMessage());
      }
      ps.reset();
      final SdfId idA = new SdfId();
      SdfId idB;
      do {
        idB = new SdfId();
      } while (idA.check(idB));
      generated = new BufferedReader(new StringReader(label + idA.toString() + StringUtils.LS));
      detected = new BufferedReader(new StringReader(label + idA.toString() + StringUtils.LS));
      VcfEvalTask.checkHeader(generated, detected, idA);
      assertEquals("", ps.toString());

      generated = new BufferedReader(new StringReader(label + idB.toString() + StringUtils.LS));
      detected = new BufferedReader(new StringReader(label + idA.toString() + StringUtils.LS));
      VcfEvalTask.checkHeader(generated, detected, idA);
      assertTrue(ps.toString(), ps.toString().contains("Reference template ID mismatch, baseline variants were not created from the given reference"));
      ps.reset();

      generated = new BufferedReader(new StringReader(label + idA.toString() + StringUtils.LS));
      detected = new BufferedReader(new StringReader(label + idB.toString() + StringUtils.LS));
      VcfEvalTask.checkHeader(generated, detected, idA);
      assertTrue(ps.toString(), ps.toString().contains("Reference template ID mismatch, called variants were not created from the given reference"));
      ps.reset();

      generated = new BufferedReader(new StringReader(label + idB.toString() + StringUtils.LS));
      detected = new BufferedReader(new StringReader(label + idA.toString() + StringUtils.LS));
      VcfEvalTask.checkHeader(generated, detected, new SdfId(0));
      assertTrue(ps.toString(), ps.toString().contains("Reference template ID mismatch, baseline and called variants were created with different references"));

      ps.reset();
    } finally {
      Diagnostic.setLogStream();
    }

  }

  public void testGetVariants() throws IOException {
    try (final TestDirectory dir = new TestDirectory("eval")) {
      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final File input2 = new File(dir, "snp_only_2.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input2);

      try {
        final MockArraySequencesReader templateReader = new MockArraySequencesReader(SequenceType.DNA, 32, 27);
        VcfEvalTask.getVariants(VcfEvalParams.builder().callsFile(input).baseLineFile(input).create(), templateReader);
        fail("Expected detection of no sequence name overlap");
      } catch (NoTalkbackSlimException e) {
        // Expected
      }

      final String[] names = new String[31];
      final int[] lengths = new int[31];
      for (int seq = 1; seq < 32; seq++) {
        names[seq - 1] = "simulatedSequence" + seq;
        lengths[seq - 1] = 1000;
      }
      final MockArraySequencesReader templateReader = new MockArraySequencesReader(SequenceType.DNA, lengths, names);
      assertTrue(VcfEvalTask.getVariants(VcfEvalParams.builder().callsFile(input).baseLineFile(input).create(), templateReader) instanceof TabixVcfRecordSet);
    }
  }

  private static final String TEMPLATE_PREVIOUS_NT = ">chr1" + StringUtils.LS
      + "AAGATGC";

  private static final String[] FN_BUG_PREVIOUS_NT = {
    "chr1 2 . AGA TGA,AATCC 69.9 PASS XRX GT:DP:RE:GQ:AR:AB:RS 0/1:7:0.007:45.5:0.000:0.429:A,3,0.003,C,1,0.001,T,3,0.003"
  };

  private static final String[] TP_BUG_PREVIOUS_NT = {
    "chr1 6 . G C 69.9 PASS XRX GT:DP:RE:GQ:AR:AB:RS 1/1:7:0.007:45.5:0.000:0.429:A,3,0.003,C,1,0.001,T,3,0.003"
  };


  public void testPreviousNt() throws IOException, UnindexableDataException {
    check(TEMPLATE_PREVIOUS_NT, TP_BUG_PREVIOUS_NT, EMPTY, FN_BUG_PREVIOUS_NT, TP_BUG_PREVIOUS_NT, new String[] {}, FN_BUG_PREVIOUS_NT, false, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }


  private static final String TEMPLATE_COMPLEX_OVERLAP_BUG = ">Chr1" + StringUtils.LS
    + "ACTAAGAAGATTTACAATATGAATTATCCTGATTCTAGTCACCACCACCACAACAACTATCCTCATCTGCTTCTGAGACACTCAAGAAGTTAATAGGTCA" + StringUtils.LS;

  private static final String[] TP_COMPLEX_OVERLAP_BUG = {
    "Chr1 51 . CAACAACTATCCTC CAACAACTATCCTCATCT,CCACAACTATCCTCATCT 1259.6 PASS DP=16;XRX GT:DP:RE:RQ:GQ 1/1:10:0.009:750.7:22.6",
    "Chr1 68 . T TATCT 1192.1 PASS DP=16;XRX GT:DP:RE:RQ:GQ 1/1:9:0.072:753.5:25.6"
  };

  public void testComplexOverlapBug() throws IOException, UnindexableDataException {
    // Test for bug # 1502
    checkRoc("complexoverlap", TEMPLATE_COMPLEX_OVERLAP_BUG, TP_COMPLEX_OVERLAP_BUG, EMPTY, EMPTY);
  }


  private static final String TEMPLATE_BUG = ">chr1" + StringUtils.LS
      + "AAGATG";

  private static final String[] FN_BUG = {
    "chr1 2 . AGA TGA,AATCC 69.9 PASS XRX GT:DP:RE:GQ:AR:AB:RS 0/1:7:0.007:45.5:0.000:0.429:A,3,0.003,C,1,0.001,T,3,0.003"
  };

  private static final String[] TP_BUG = {
    "chr1 6 . G C 69.9 PASS XRX GT:DP:RE:GQ:AR:AB:RS 1/1:7:0.007:45.5:0.000:0.429:A,3,0.003,C,1,0.001,T,3,0.003"
  };

  public void testInfiniteLoop() throws IOException, UnindexableDataException {
    check(TEMPLATE_BUG, TP_BUG, EMPTY, FN_BUG, TP_BUG, new String[] {}, FN_BUG, false, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }

  /* 123456789012 345
     GTTTTGTTA
      T
      _
      T  TGT
      _  GT_



   */
  private static final String ZERO_COUNT_REF = ">11" + StringUtils.LS
      + "gttttgtta";

  private static final String[] ZERO_COUNT_TP = {
    "11 1 . GT G 53.91 PASS . GT:AD:DP:GQ:PL 0/1:34,7:41:83.90:84,0,910",
  };

  private static final String[] ZERO_COUNT_FP = {
     "11 5 . T G 53.91 PASS . GT:AD:DP:GQ:PL 0/1:34,7:41:83.90:84,0,910",
     "11 6 . G T 58.40 PASS . GT:AD:DP:GQ:PL 0/1:32,9:41:99:132,0,698",
     "11 6 . GT G 255.25 PASS . GT:AD:DP:GQ:PL 0/1:24,12:41:5.19:271,0,5"
  };

  public void testZeroCountBug() throws IOException, UnindexableDataException {
    //the reconstructed reference after applying the FP is same as just applying the TP
    //MutationEval will choose the 3 FP as a TP in comparison to 1 TP as it explains more mutations
    check(ZERO_COUNT_REF, ZERO_COUNT_TP, ZERO_COUNT_FP, EMPTY, ZERO_COUNT_FP, ZERO_COUNT_TP, EMPTY, false, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }

  static final String REF = ">10"  + StringUtils.LS
      + "CTTTCTCTTTCTTTCTTTCTCTCTTTCCATCCTTCTTTCTTTCTTTCTCTTTCTTTCTTTCTCTCTTTCCATCCTTCTTTCTTTCTTTCCTTCCTTCTTTCTTTCCTTCCTTTCTTTCTTTTTCTTTCTTTCTCTTTCCTTTTTCTTTCTTTCTCTCTCCCTCTTTCTTTTTTTTTTGAGTACCTAAAATCTACTCTCTT"
      + "GGTAAAGTGCCTGCACACAATACAGTATTATTAACTACTCTCCTCATGTTGTACATTGCATCTCTAGACTTCTTCATAGGCTTTGGAAGCCGATAGTTTTGAAAGAACAGCATCATATTATTCAGCATTTCAGAGAGGCCCCCATCAAAAACGAAATGCTCTGTTAGCCTCTGGGCGTGCTGCCAGCTGAGGGCCCCAGC"
      + "TGAGCACCCAGGCCATGGAAGAAAGCTGCCTTGCCCAAGCTCATGTCCCCTCCCGGCGCAGTCCACGCCTGATGTCGGCTTGATGTGGAGAACGTGACCCAGGCTCTGTCTGAATTCAGGACATCCCAGGAGGCATCTCCACGCATCGGAGCTCCCGTGGCCTAGGCTGGAGCCTCCTCGGAGGCCTCTGTGCCACTCCC"
      + "TCCCAGGGTGGGTTATCCTGAGTGCTTCCTAATAGGGGTCTGCAGAGGCAAATCCCAGCCCAGGGAGCCCAACCTCACCCGGGACTTACCCCAGAGAGCATGACAAGGTCACCTGCTCAAAAGGTAGATTCCTAAGCAAAATCAACATTGTGTTCCTGAAGAATGTGGAAAGAAATGTGGGGTAAATCACAGTGCCTGAC"
      + "ATTTTTCTAAAAGGTGATTGGCTTTATCCTTAACATAGATTGTTAAGTCCCACAGCCTTACAAAACAAATTTCAATCACAATCTCAACGGCATTGAGTAAGTTTATATATATATATATATATACAAGTATACATATATATACACACACACACACGTAAAATACACATGGAGTTTTGAAAACTTAGTGCAACAATTTAAAA"
      + "AGGTGAAGTACCTCAATTGCTTTTTCTTATTGACTACTTGTTGAAATGCTATGTTTTTGATAGATTTGGTTAAATAAACATTACCATTATAATTAATCTCCCCTATTTCTCTTTTTTTATTTCTTCATACGGCTAGTAGAACATTTAAAATTGCGTATTTGGCTCTTTTTTACGGCTTGCCTTTTTTTTTCTAGTGAAAG"
      + "ATGGTGTATGATGCTTCTGTCTTTTATGCACTCAACATATTTGTTTCCTAAAATAAGAAAAAAAGAAGTAGCAAAAGGCATAACTCTTCAGAATGAGTCTGATATTAAAAAAAAAAAAAGCCATACATATTTTGTTTCTTCTACTGCTTGAAATTTCAGGTGGCAAATCCCTGGAGAATTTAATTTGTCTTCAATTAGAT"
      + "TTTTAATGTTCCTTTTAGTCACAATCAAAAAGACATTTTCTTAATCAGTGGTGTCTATTACAATAGTTAGGATAATGTAATTTGGCATAATTATATGAAATTAAGATTTAGTACACTCAAGGTTTTTTCCCTCCTTGAATGAAATAAATGCAAAAAGCAAAATAACCCTGAAATCTTACATTTAACAAAAAGTCCCAAAC"
      + "CTATTATTCTTTCCCATTATTGAATTGATGTCTGCAGTGACAGTACTAGGAAGAATAAAATAGCAATCTAGCTTTGAGGCTATAATTTTTAATTGATGGAAGCAGGTACAGGATTTATGCAATATGATATCATCTTAAGAGATGATGGAAGTATAAAACAAGGCAAATGAAACAAACAAATAAACTGTGAAGTGCAGAAA"
      + "GAGAAATTTGAACTGCTTTAACTTCTAACAATCAAGCAGTTGGAATATTTGAGCTGTTACCTGATTATATTTAAGGATTTAGATGTGAATTTAAAAAGTTCCCCAAATAAATCTCCTTGCCATCTCTCAAGTCAGCAGCATTCTGAGAGCTAAGCTGGAAACACACAAAACACATAGCCTGGTTTAATAATAGCTAGACT"
      + "GTGGAAGAAGAAAACTCCTTCACTCTGGCAGGCTAACAAAGTTGATTTAATTTAGCTCTCTTAATTGCTTCATATTGTGAATTTCAACATGCCATGTGTAACAGCTTTCTTTCTAGTGATTTTCTTGTAGAAGTCGTTGTTCCCATTAGGTTTTGAGGTATTTTTCTCTTCATTTCTTATTCTCAAAGGTACTGACTTTA"
      + "TGCTCACTTAAAATTGAATGATTAATGACTCTGTTAGCTACATGATACAATTTTAAAAGAATAAGCCCAAACCACTTAACTAAAATAAATAAGGTTTTTTGTTCATTAACATTAACATAGTATGGTCATAAACTGCCCAATTCCTAAATCACTCTTTATTTCCACTTGGCTTAAAATTATTTTTGCCTATTTAATTTATT"
      + "ATTAGGGAAATTAAACACACACACATTTTTTTCTTGAGCACTCATAATTTGAGATTCATTATGATCACTAAGCAAAAAGTTTACTTATTCAGTCATCATTGCAATGCAACTTAAAAGTCACAACAATCCCAATATAAAAATGTAACTTCATGTAATATCAAAAGACATGTTTTGTTTTATCTTAACAATTATAGTTAATT"
      + "TCCCTGCAGAGTAGATCTCCAGGACTGTCAACAGAATCCCCAAGGACATAGTCATTGGCAGTGGTGGATGCAAAACACACATTAAGATTAATTGCAGAAGCTCTGTCTAAACAGAATGTCTTCTGATATTACTTTATAACTTCTTATATGTAATGCAACTGCCTGAACACTTGCGCTCCTTCATGTGTATAATTCTGTTG"
      + "CACACTGCTTGATTCTGTGTAGGAAACAGTTTGGGAATATTGGGGTTGGGTAAAGTGGAGTCACGCATGTATTACATGCTCATGTTTCCATGTTTCATGCTGATGACGATAAGCCTGCACTGCATGCATCCAATGCATTGCAGATTCACTGGTAAATATATGACCAGTAATCAACACAAATATATTAAATTGCTAGTCAT"
      + "TTTGAATTTTGCTTATGCTGGAATAGGGCTCGTATCCATGAATTATCCAGTTATAACGCAGCTCAGATTCTTGCCGTATGCCTTGTATGTGTGAAACACTTGCCACTTTCAATAATTTTTCACATACAGTTTCCCATTTAACATGTTTGACCAAAAAATAAACACAATATATATATTCTTGACTTAAGACATTGCAACTA"
      + "ATAGAAGCACTCAAATTATGAGAAGATTTCCAATTGTCTATCTACCTAATTCATTCAACAGATAGAAATATTATGCAAGGTAATCTGGTATTTTAGAATGTATAAGAAGCTCTAGTTATTATCTGGATAGAATCAGGAGAATGTACTGATAGGTTTAAATGGATTAAAGACATCTTTCCTTCTTCCTTCAGACCTATAAA"
      + "AAATCAAATTAAATGGAATATAATTGAATGATAAATATAATTTATCTAACATGTCCTTTTGGTCTAGCCTAATATGATAAAGTTCATTGGATTGTTTATGAAATTTCACCTTTTATATTGAGCTTTCCAAGTGTGCTCAGAAAAACAAAAAGGTATATTTTCCAGCCTAAGTTATTAGAAAAATTTAGGACAAATAAAAT"
      + "GAGGTATCATATCTACATCTCTTGCATAACACATTGACCATAGCATGGATTCATGTCTGTTTTCTCTGTTCCAAAATCACAGTGACTATTCAAACCTGAAACTCATTGCTGCTCTCAAATTATAAAAAACCTTTCAAATGGTTCTTACCAATAAATTGTAACCATATTGATCAAGCTAATGAATTTTAACGAAAAAAATT"
      + "GTCTTTCTACATTGTCACATTATAGGTTCCAAAAAGATTCTAGAAATTTTAGATTCCAAAAATTCTAAATGTTTATTTTTCAATTGTTTTCCACTCTATTTGCTCTCCTACTTTTGAGTTTTTAAATTCTTGGGTGGACAGAAGTAAATTAAAATCTCATTTTTATTTATTTATAATTAAATAATCCAAATATATATTTT"
      + "TAGGAAAGCTATTTGATGTTTAGAAAACCTTGTTAGCCTCTTAAACATGTTTTTCTTTGAAGAGACAAGCTGTCTTTTGCCAAGCACAAATTTCAAACACCAACGGCTTCTACGTTCTCTTTGAAACACATAACGTCATTATGATAAATTCTTCTTTGGAGAAGTGAATGCATTTACCCTTCATGTGAAGATGATGCTGC"
      + "AACAGGGTATTTGGCCGTTTTTTTCAGATGGAGAAAGTATCATACATTCCTAATTATTTATCAGAACGATGTGAAATCAAGAAAAAGAAAATAGAAACAGGCGACTGTGGTATTTTTTCAAACATTCAAAGTCAAAAATGTTACCTTATTGTGCGAAATATTCAATTTGACTTCATTTTGACTTCTCTCTCTATTCGTGT"
      + "TCTGTGCCTGCCAGTCTCAAATCCCAAAGCACTGAGTCAGGCTAACGTGAGAAAGTGAAACATATCCACTTTATATTCCAGTTCAATTTTGTTTTAACATGATCTGATGAAAAACTTACGAGACCAAGGTATTAATGAAACTCTAACATTAGTTATTTTTTAATCACCTAATTTTTAGTTATTTTTACTTTTTATTTATT"
      + "CGTTTTTGAGGCATAGTTTATATATTATAAAACTCATTCTTTTAAGCATTCAAATCAGTGGGTTTTAATATATTCACAAAGCGGTGCAATCATCACCACTTTCTAACTCCAGAACATTTTCATAGCCCCAAAAACAAACCACATGCCCTGTGGCACTTCGTCACACCGTTTCTTCTCCTCTCAGGCCCCAGCAATCACTA"
      + "ATCTGTTTCCCGTCTTTATCGATTTGCCCATTCTGGATATTTCATACAGATGAAATAATACAATACACAGCTTTTTGTGTCTGGCTTTCTTCACTTAGCATAACACTTTCAAAGTTTATCCGTATTGTAGCATGTGCCATTACTTCATTTCTTTTTAAAATTGAATAATAATATTTTATTGTGATAAACATATCACATTT"
      + "TGCTTCTCCATGCATCCATTGATGAAAGATTAGTGTCGTGTTCAGTTTTTGGCTTTTTGAATAATGCGCCCATGGATATTCCTGTACAAGCTTCTGAGTGGACCAGTGTTTTCAGTTCTCTTAATATCTACCTGGGAATGGAATTGTTGTAGTCATACAGTAATTTTCCAACTGCCAAGATGTTTCCAAAGTAGCTGCAC"
      + "AATTTTATACTTCCAACAGCAAAGTATGAGGGTTTCAGTTTCTTCATATCCTCATCAACATTTGCAATTGTTTGTCCTTTTTTATTATAAACATGCTAGTGGGTGTCAAGTGGTATCTCATTGTGGTTTTGGTTTGCATTTCCTAATAGTAATCATATTGGCCATGTTTTTATGTACTTAATGGCCATTTGTGTTTATAA"
      + "CTTTCAAAAAGTTTTAGTAATGTTTTTTTCTCATTTTTAATTGGGTTGTTAGCCTTTTTTTTTCTTTTTTTGAGATACAGTCTCACTCTGTCACCTAGGCTGGAGTGCCGTGGTGCGATCTTGGCTCACTGCAACCTCTGCCTTCTGGGTTCAAGCAATTCTTCTGCCTCAGCCTCCCAAGTAGCTGGGACTACAGGTGT"
      + "GTGCCACCAAGCCGAGATAATTTTTGTGGTTTTTTTTAGTAGTGAAGGGGTTTCACCTTGTTGGCCAGGCTGGTTTCGAACTCCTGATCTCAGATGATCCACCCACCTTGGCCTCCCAAAGTGCTGCGATTACAAGCGT"
      ;

  private static final String[] REF_TP = {
    "10 1178 . G T 182.85 PASS . GT:AD:DP:GQ:PL 0/1:11,14:25:99:168,0,223",

  };

  private static final String[] REF_FN = {
    "10 5576 . C T 826.26 PASS . GT:AD:DP:GQ:PL 0/1:37,29:66:99:649,0,933"
  };

  private static final String[] REF_FP = {
    "10 1 . TTTCT T 231.2 PASS . GT:DP:RE:GQ 1/0:29:0.182:55.9",
    "10 31 . C CTTCCTTCCTTCCTTTTTTCTTT 601.9 PASS . GT:DP:RE:GQ 1/0:28:0.188:14.0"

  };

  public void testFun() throws IOException, UnindexableDataException {
    check(REF, REF_TP, REF_FP, REF_FN, REF_TP, REF_FP, REF_FN, false, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }


  public void testPositionComparator() {
    final IntervalComparator vc = new IntervalComparator();
    final VcfRecord rec = VcfReader.vcfLineToRecord("chr10 11 . G T 182.85 PASS . GT:AD:DP:GQ:PL 0/1:11,14:25:99:168,0,223".replaceAll(" ", "\t"));
    final DetectedVariant v = new DetectedVariant(rec, 0, RocSortValueExtractor.NULL_EXTRACTOR, false);


    final VcfRecord rec2 = VcfReader.vcfLineToRecord("chr10 12 . G T 182.85 PASS . GT:AD:DP:GQ:PL 0/1:11,14:25:99:168,0,223".replaceAll(" ", "\t"));
    final DetectedVariant v2 = new DetectedVariant(rec2, 0, RocSortValueExtractor.NULL_EXTRACTOR, false);

    final VcfRecord rec3 = VcfReader.vcfLineToRecord("chr10 11 . GT TT 182.85 PASS . GT:AD:DP:GQ:PL 0/1:11,14:25:99:168,0,223".replaceAll(" ", "\t"));
    final DetectedVariant v3 = new DetectedVariant(rec3, 0, RocSortValueExtractor.NULL_EXTRACTOR, false);

    assertEquals(-1, vc.compare(v, v2));
    assertEquals(1, vc.compare(v2, v));

    assertEquals(0, vc.compare(v, v));

    assertEquals(-1, vc.compare(v, v3));
    assertEquals(1, vc.compare(v3, v));
  }
}

