/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import java.io.IOException;
import java.util.ArrayList;

import com.rtg.tabix.UnindexableDataException;

public class VcfEvalNanoTest extends AbstractVcfEvalTest {

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
