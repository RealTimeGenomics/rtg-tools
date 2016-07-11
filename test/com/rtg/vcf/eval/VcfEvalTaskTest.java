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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.rtg.launcher.AbstractNanoTest;
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
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class VcfEvalTaskTest extends AbstractNanoTest {

  private static final String HEADER_SDF_PREFIX = "##TEMPLATE-SDF-ID=";

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
  private static final String BASELINE_HEADER = VcfHeader.MINIMAL_HEADER + "\tRTG";

  public void test() throws IOException, UnindexableDataException {
    check(SIMPLE_BOTH, SIMPLE_CALLED_ONLY, SIMPLE_BASELINE_ONLY, TP_OUT, FP_OUT, FN_OUT, VcfUtils.FORMAT_GENOTYPE_QUALITY);
  }

  private void check(String[] both, String[] calledOnly, String[] baselineOnly, String[] tpOut, String[] fpOut, String[] fnOut, String sortField) throws IOException, UnindexableDataException {
    check(TEMPLATE, both, calledOnly, baselineOnly, tpOut, fpOut, fnOut, true, sortField);
    check(TEMPLATE, both, calledOnly, baselineOnly, tpOut, fpOut, fnOut, false, sortField);
  }

  private void check(String ref, String[] both, String[] calledOnly, String[] baselineOnly, String[] tpOut, String[] fpOut, String[] fnOut, boolean zip, String sortField) throws IOException, UnindexableDataException {
    try (final TestDirectory tdir = new TestDirectory()) {
      assertTrue(tdir.isDirectory());
      assertEquals(0, tdir.listFiles().length);
      createInput(tdir, both, calledOnly, baselineOnly);

      final File calls = new File(tdir, "calls.vcf.gz");
      final File baseline = new File(tdir, "baseline.vcf.gz");
      final File template = new File(tdir, "template");
      final File out = FileUtils.createTempDir("out", zip ? "zip" : "notZipped", tdir);
      assertTrue(out.isDirectory());
      //System.err.println("baseline \n" + FileUtils.fileToString(baseline));
      //System.err.println("calls \n" + FileUtils.fileToString(calls));
      ReaderTestUtils.getReaderDNA(ref, template, null).close();
      final VcfEvalParams params = VcfEvalParams.builder().baseLineFile(baseline).callsFile(calls)
        .templateFile(template).outputParams(new OutputParams(out, false, zip)).scoreField(sortField).create();
      VcfEvalTask.evaluateCalls(params);
      final String zipSuffix = zip ? ".gz" : "";
      final File tpFile = new File(out, "tp.vcf" + zipSuffix);
      assertTrue(Arrays.toString(out.listFiles()), tpFile.isFile());
      final String tpResults = fileToString(tpFile, zip);
      final String fpResults = fileToString(new File(out, "fp.vcf" + zipSuffix), zip);
      final String fnResults = fileToString(new File(out, "fn.vcf" + zipSuffix), zip);
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
    final File baseline = new File(dir, "baseline.vcf.gz");
    final TreeMap<Variant, String> callList = new TreeMap<>();
    final TreeMap<Variant, String> baselineList = new TreeMap<>();
    for (final String var : both) {
      final VcfRecord rec = VcfReader.vcfLineToRecord(var.replaceAll(" ", "\t"));
      callList.put(VariantTest.createVariant(rec, 0), rec.toString());
      baselineList.put(VariantTest.createVariant(rec, 0), rec.toString());
    }
    for (final String var : calledOnly) {
      final VcfRecord rec = VcfReader.vcfLineToRecord(var.replaceAll(" ", "\t"));
      callList.put(VariantTest.createVariant(rec, 0), rec.toString());
    }
    for (final String var : baselineOnly) {
      final VcfRecord rec = VcfReader.vcfLineToRecord(var.replaceAll(" ", "\t"));
      baselineList.put(VariantTest.createVariant(rec, 0), rec.toString());
    }
    try (final BufferedWriter callOut = new BufferedWriter(new OutputStreamWriter(FileUtils.createOutputStream(calls, true, false)))) {
      callOut.write(CALLS_HEADER.replaceAll(" ", "\t") + StringUtils.LS);
      for (final Entry<Variant, String> var : callList.entrySet()) {
        callOut.write(var.getValue() + "\n");
      }
    }
    new TabixIndexer(calls).saveVcfIndex();
    try (final BufferedWriter mutOut = new BufferedWriter(new OutputStreamWriter(FileUtils.createOutputStream(baseline, true, false)))) {
      mutOut.write(BASELINE_HEADER.replaceAll(" ", "\t") + StringUtils.LS);
      for (final Entry<Variant, String> var : baselineList.entrySet()) {
        mutOut.write(var.getValue() + "\n");
      }
    }
    new TabixIndexer(baseline).saveVcfIndex();

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
      final EnumSet<RocFilter> rocFilters = EnumSet.of(RocFilter.ALL, RocFilter.HET, RocFilter.HOM);
      if (rtgStats) {
        rocFilters.add(RocFilter.NON_XRX);
        rocFilters.add(RocFilter.XRX);
      }
      final File calls = new File(tdir, "calls.vcf.gz");
      final File baseline = new File(tdir, "baseline.vcf.gz");
      final File out = FileUtils.createTempDir("out-rtgstats-" + rtgStats, "", tdir);
      final File genome = new File(tdir, "template");
      ReaderTestUtils.getReaderDNA(template, genome, null).close();
      final VcfEvalParams params = VcfEvalParams.builder().baseLineFile(baseline).callsFile(calls)
        .templateFile(genome).outputParams(new OutputParams(out, false, false))
        .rocFilters(rocFilters)
        .create();
      VcfEvalTask.evaluateCalls(params);
      final int tpCount = both.length;
      final int fnCount = baselineOnly.length;

      mNano.check(label + "-summary.txt", FileUtils.fileToString(new File(out, CommonFlags.SUMMARY_FILE)));
      checkRocResults(label + "-weighted.tsv", new File(out, RocFilter.ALL.fileName()), checktotal, tpCount, fnCount);
      checkRocResults(label + "-homo.tsv", new File(out, RocFilter.HOM.fileName()), checktotal, tpCount, fnCount);
      checkRocResults(label + "-hetero.tsv", new File(out, RocFilter.HET.fileName()), checktotal, tpCount, fnCount);
      if (rtgStats) {
        checkRocResults(label + "-simple.tsv", new File(out, RocFilter.NON_XRX.fileName()), checktotal, tpCount, fnCount);
        checkRocResults(label + "-complex.tsv", new File(out, RocFilter.XRX.fileName()), checktotal, tpCount, fnCount);
      } else {
        assertFalse(new File(out, RocFilter.NON_XRX.fileName()).exists());
        assertFalse(new File(out, RocFilter.XRX.fileName()).exists());
      }

      final VcfEvalParams paramsrev = VcfEvalParams.builder().baseLineFile(baseline).callsFile(calls)
        .templateFile(genome).outputParams(new OutputParams(out, false, false))
        .sortOrder(RocSortOrder.ASCENDING)
        .rocFilters(rocFilters)
        .create();
      VcfEvalTask.evaluateCalls(paramsrev);
      checkRocResults(label + "-weighted-rev.tsv", new File(out, RocFilter.ALL.fileName()), checktotal, tpCount, fnCount);
    }
  }

  private void checkRocResults(String label, final File out, boolean checktotal, final int tpCount, final int fnCount) throws IOException {
    final String roc = AbstractVcfEvalTest.sanitizeHeader(FileUtils.fileToString(out));
    //System.err.println("ROC\n" + roc);
    final String[] homoLines = roc.split(StringUtils.LS);
    int line = 0;
    if (checktotal) {
      assertEquals("#total baseline variants: " + (tpCount + fnCount), homoLines[line++]);
    } else {
      assertTrue(homoLines[line++].startsWith("#total baseline variants: "));
    }
    assertTrue(homoLines[line++].startsWith("#total call variants: "));
    assertTrue(homoLines[line++].startsWith("#score field: "));
    assertTrue(homoLines[line].startsWith("#score\t"));
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
      final File baseline = new File(tdir, "baseline.vcf.gz");
      final File out = FileUtils.createTempDir("outsideRef", "out", tdir);
      final File template = new File(tdir, "template");

      ReaderTestUtils.getReaderDNA(TEMPLATE, template, null).close();
      final MemoryPrintStream ps = new MemoryPrintStream();
      Diagnostic.setLogStream(ps.printStream());
      final VcfEvalParams params = VcfEvalParams.builder().baseLineFile(baseline).callsFile(calls)
        .restriction(new RegionRestriction("seq", 0, 300))
        .templateFile(template).outputParams(new OutputParams(out, false, false)).create();
      VcfEvalTask.evaluateCalls(params);
      TestUtils.containsAll(ps.toString(),
        "Variant in calls at seq:28 ends outside the length of the reference sequence (27).",
        "Variant in baseline at seq:28 ends outside the length of the reference sequence (27).",
        "Variant in calls at seq:30 ends outside the length of the reference sequence (27).",
        "There were 1 problematic baseline variants skipped during loading (see vcfeval.log for details).",
        "There were 2 problematic called variants skipped during loading (see vcfeval.log for details)."
      );
    }
  }

  public void testGetSdfId() throws IOException {
    Diagnostic.setLogStream();
    VcfHeader header = new VcfHeader();
    header.addMetaInformationLine(HEADER_SDF_PREFIX + "blahtblah");
    try {
      VcfEvalTask.getSdfId(header);
      fail();
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid VCF template SDF ID header line : " + HEADER_SDF_PREFIX + "blahtblah", e.getMessage());
    }
    header = new VcfHeader();
    header.addMetaInformationLine(HEADER_SDF_PREFIX + "blah");
    try {
      VcfEvalTask.getSdfId(header);
      fail();
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid VCF template SDF ID header line : " + HEADER_SDF_PREFIX + "blah", e.getMessage());
    }
    header = new VcfHeader();
    header.addMetaInformationLine(HEADER_SDF_PREFIX + new SdfId(42).toString());
    assertEquals(new SdfId(42), VcfEvalTask.getSdfId(header));
  }

  public void testCheckHeader() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    Diagnostic.setLogStream(ps.printStream());
    final SdfId idA = new SdfId();
    SdfId idB;
    do {
      idB = new SdfId();
    } while (idA.check(idB));
    final VcfHeader hA = new VcfHeader();
    hA.addMetaInformationLine(HEADER_SDF_PREFIX + idA.toString());
    VcfEvalTask.checkHeader(hA, hA, idA);
    assertEquals("", ps.toString());

    final VcfHeader hB = new VcfHeader();
    hB.addMetaInformationLine(HEADER_SDF_PREFIX + idB.toString());
    VcfEvalTask.checkHeader(hB, hA, idA);
    assertTrue(ps.toString(), ps.toString().contains("Reference template ID mismatch, baseline variants were not created from the given reference"));
    ps.reset();

    VcfEvalTask.checkHeader(hA, hB, idA);
    assertTrue(ps.toString(), ps.toString().contains("Reference template ID mismatch, called variants were not created from the given reference"));
    ps.reset();

    VcfEvalTask.checkHeader(hB, hA, new SdfId(0));
    assertTrue(ps.toString(), ps.toString().contains("Reference template ID mismatch, baseline and called variants were created with different references"));

    ps.reset();
  }

  public void testGetVariants() throws IOException {
    try (final TestDirectory dir = new TestDirectory("eval")) {
      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final File input2 = new File(dir, "snp_only_2.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input2);

      final VcfEvalParams params = VcfEvalParams.builder().callsFile(input).baseLineFile(input).create();
      try {
        final MockArraySequencesReader templateReader = new MockArraySequencesReader(SequenceType.DNA, 32, 27);
        VcfEvalTask.getVariants(params, templateReader, VcfEvalTask.getReferenceRanges(params, templateReader), null);
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
      assertTrue(VcfEvalTask.getVariants(params, templateReader, VcfEvalTask.getReferenceRanges(params, templateReader), null) instanceof TabixVcfRecordSet);
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
    //VcfEval will choose the 3 FP as a TP in comparison to 1 TP as it explains more baseline variants
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
    final Variant v = VariantTest.createVariant(rec, 0);


    final VcfRecord rec2 = VcfReader.vcfLineToRecord("chr10 12 . G T 182.85 PASS . GT:AD:DP:GQ:PL 0/1:11,14:25:99:168,0,223".replaceAll(" ", "\t"));
    final Variant v2 = VariantTest.createVariant(rec2, 0);

    final VcfRecord rec3 = VcfReader.vcfLineToRecord("chr10 11 . GT TT 182.85 PASS . GT:AD:DP:GQ:PL 0/1:11,14:25:99:168,0,223".replaceAll(" ", "\t"));
    final Variant v3 = VariantTest.createVariant(rec3, 0);

    assertEquals(-1, vc.compare(v, v2));
    assertEquals(1, vc.compare(v2, v));

    assertEquals(0, vc.compare(v, v));

    assertEquals(-1, vc.compare(v, v3));
    assertEquals(1, vc.compare(v3, v));
  }
}

