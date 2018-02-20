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

  public void testNanoSmallSquash() throws IOException, UnindexableDataException {
    check("vcfeval_small_squash/vcfeval_small", true, false, false, "--sample", "sample1", "--vcf-score-field", "QUAL", "--Xroc-subset", "all,snp,xrx", "--squash-ploidy");
  }

  public void testNanoSmallAlt() throws IOException, UnindexableDataException {
    endToEnd("vcfeval_small_alt/vcfeval_small", new String[] {"baseline.vcf", "calls.vcf"}, false, "--sample", "ALT", "--vcf-score-field", "QUAL", "--output-mode", "annotate", "--squash-ploidy");
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

  public void testNanoAllMatches() throws IOException, UnindexableDataException {
    // This contains a few variants that should be flagged
    // One example is tricky, in that it is normally discarded before reaching a sync point
    endToEnd("vcfeval_all_matches", new String[] {"output.vcf"}, true, "--output-mode=combine", "--sample=BASELINE,CALLS", "--all-records", "--ref-overlap", "--Xtwo-pass=false", "--XXcom.rtg.vcf.eval.flag-alternates=true");
  }

  public void testNanoUpstreamDel() throws IOException, UnindexableDataException {
    // Here the calls are missing the deletion itself. We want a partial match against the SNP.
    // (previously this would not find any match)
    endToEnd("vcfeval_small_updel/updel", new String[] {"baseline.vcf", "calls.vcf"}, false, "--vcf-score-field", "QUAL", "--output-mode", "annotate", "--squash-ploidy");

    // Another case. We now treat the "*" as a skip so we can make a diploid match
    // if we allow variant replay overlap in order to deal with the fact that one of
    // these variants asserts reference bases.
    // Note that treating the * as skip also considers the
    // G->C,* 1/2
    // to exact match with:
    // G->C   0/1
    // G->C   ./1
    // but not match:
    // G->C   1/1  (which some might expected to match)
    endToEnd("vcfeval_small_updel/updel2", new String[] {"baseline.vcf", "calls.vcf"}, false, "--vcf-score-field", "QUAL", "--output-mode", "annotate", "--ref-overlap");
  }

  public void testNanoTrickySquash() throws IOException, UnindexableDataException {
    // Here diploid matching finds 3TP
    //endToEnd("vcfeval_small_trickysquash/trickysquash", new String[] {"baseline.vcf", "calls.vcf"}, false, "--vcf-score-field", "QUAL", "--output-mode", "annotate");
    // But normal squash-ploidy reports 2TP/1FP due to requiring more than one haplotype. We can do diploid allele-matching, to find all three TP
    endToEnd("vcfeval_small_trickysquash/trickysquash", new String[] {"baseline.vcf", "calls.vcf"}, false, "--vcf-score-field", "QUAL", "--output-mode", "annotate", "--squash-ploidy", "--XXcom.rtg.vcf.eval.haploid-allele-matching=false");
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
