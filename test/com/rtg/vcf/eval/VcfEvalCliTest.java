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

import com.rtg.reader.ReaderTestUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

public class VcfEvalCliTest extends AbstractVcfEvalTest {

  public void testInitParams() {
    checkHelp("vcfeval [OPTION]... -b FILE -c FILE -o DIR -t SDF"
      , "Evaluates"
      , "called variants"
      , "baseline variants"
      , "directory for output"
      , "reference genome"
      , "--sample", "the name of the sample to select"
      , "-f,", "--vcf-score-field", "the name of the VCF FORMAT field to use as the ROC score"
      , "-O,", "--sort-order", "the order in which to sort the ROC scores"
      , "--no-gzip", "-Z,", "do not gzip the output"
    );
  }

  public void testValidator() throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfevalcli")) {
      final File out = new File(dir, "out");
      final File calls = new File(dir, "calls");
      final File mutations = new File(dir, "mutations");
      final File template = new File(dir, "template");
      final String[] flagStrings = {
        "-o", out.getPath()
        , "-c", calls.getPath()
        , "-b", mutations.getPath()
        , "-t", template.getPath()
      };
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--baseline file", "does not exist");

      assertTrue(mutations.createNewFile());
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
      new TabixIndexer(mutations).saveVcfIndex();
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--calls file", "does not exist");

      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
      new TabixIndexer(calls).saveVcfIndex();
      checkHandleFlagsErr(flagStrings);
      ReaderTestUtils.getDNADir(">t" + StringUtils.LS + "ACGT" + StringUtils.LS, template);

      TestUtils.containsAllUnwrapped(checkHandleFlagsErr( "-o", out.getPath(), "-c", calls.getPath(), "-b", mutations.getPath(), "-t", template.getPath(),
        "--output-mode", "combine", "--sample", "ALT"), "--output-mode=combine cannot be used");

      checkHandleFlags(flagStrings);

      assertTrue(out.mkdir());
      checkHandleFlagsErr(flagStrings);
    }
  }

  public void testEmpty() throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfevalcli")) {
      final File out = new File(dir, "out");
      final File calls = new File(dir, "calls.vcf.gz");
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
      new TabixIndexer(calls).saveVcfIndex();
      final File mutations = new File(dir, "mutations.vcf.gz");
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
      new TabixIndexer(mutations).saveVcfIndex();
      final File template = new File(dir, "template");
      ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "A", template, null);
      checkMainInitBadFlags("-o", out.getPath()
        , "-c", calls.getPath()
        , "-b", mutations.getPath()
        , "-t", template.getPath());
    }
  }

  public void testNanoSmall() throws IOException, UnindexableDataException {
    check("vcfeval_small", true, false, false, "--sample", "sample1", "--vcf-score-field", "QUAL", "--Xroc-subset", "all,snp,xrx");
  }

  public void testNanoTricky() throws IOException, UnindexableDataException {
    // Tricky cases where the notion of equivalence depends on the treatment of reference bases in the calls
    // Variant on 14 requires less conservative padding removal (use default-trim variant factory)
    // Variant on 21 requires ref base removal AND variant overlap consideration to be independent for each haplotype
    check("vcfeval_small_tricky/vcfeval_small_tricky", true, false, false, "--vcf-score-field", "QUAL", "-T", "1", "--ref-overlap");
  }

  public void testNanoTricky2() throws IOException, UnindexableDataException {
    // Default of maximizing the sum of baseline and calls variants leaves one out. New path preference includes them all.
    check("vcfeval_small_tricky2", true, false, true, "--vcf-score-field", "QUAL", "-T", "1", "--ref-overlap",
      "--sample", "ALT,sample"
    );
  }

  public void testNanoTricky3() throws IOException, UnindexableDataException {
    // Has two alternative variants starting at exactly the same position, needs overlap handling, now works.
    check("vcfeval_small_tricky3", true, false, false, "--vcf-score-field", "QUAL", "-T", "1", "--sample", "dummy,sample1", "--squash-ploidy", "--ref-overlap");
  }

  public void testNanoTricky4() throws IOException, UnindexableDataException {
    // Has an insert and delete that cancel, but they are placed on either side of a snp.
    // Ideally should just choose the snp as baseline TP, but vcfeval currently includes all three variants.
    endToEnd("vcfeval_small_tricky4", new String[] {"tp.vcf", "tp-baseline.vcf"}, false, "--vcf-score-field", "QUAL", "-T", "1");
  }

  public void testObeyPhasing() throws IOException, UnindexableDataException {
    endToEnd("obeyphasing/obeyphasing", new String[] {"tp.vcf"}, false, "--Xobey-phase=true");
  }

  public void testNanoSmallRegion() throws IOException, UnindexableDataException {
    check("vcfeval_small_region", false, false, false, "--sample", "sample1,sample1", "--vcf-score-field", "QUAL", "--region", "chr2:150-1000");
  }

  public void testNanoSmallDiffSamples() throws IOException, UnindexableDataException {
    check("vcfeval_small_samples", false, false, false, "--sample", "sample2,sample1", "--vcf-score-field", "QUAL");
  }

  public void testNanoTooComplex() throws IOException, UnindexableDataException {
    check("vcfeval_too_complex", false, false, true);
  }

  public void testNanoTooComplexAtEnd() throws IOException, UnindexableDataException {
    check("vcfeval_too_complex_end", false, false, true);
  }

  private void check(String id, boolean checkTp, boolean checkFp, boolean expectWarn, String... args) throws IOException, UnindexableDataException {
    final ArrayList<String> files = new ArrayList<>();
    files.add("weighted_roc.tsv");
    if (checkTp) {
      files.add("tp.vcf");
    }
    if (checkFp) {
      files.add("fp.vcf");
    }
    endToEnd(id, files.toArray(new String[files.size()]), expectWarn, args);
  }
}
