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
import java.util.Arrays;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfId;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class VcfEvalCliTest extends AbstractCliTest {
  private TestDirectory mDir = null;

  @Override
  public void setUp() throws IOException {
    super.setUp();
    mDir = new TestDirectory("vcfevalcli");
    Diagnostic.setLogStream(TestUtils.getNullPrintStream());

  }

  @Override
  public void tearDown() throws IOException {
    mDir.close();
    mDir = null;
    Diagnostic.setLogStream();
    super.tearDown();
  }


  @Override
  protected AbstractCli getCli() {
    return new VcfEvalCli();
  }

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

  public void testValidator() throws IOException {
    final VcfEvalCli.VcfEvalFlagsValidator v = new VcfEvalCli.VcfEvalFlagsValidator();
    final CFlags flags =  new CFlags();
    VcfEvalCli.initFlags(flags);
    final File out = new File(mDir, "out");
    final File calls = new File(mDir, "calls");
    final File mutations = new File(mDir, "mutations");
    final File template = new File(mDir, "template");
    final String[] flagStrings = {
        "-o" , out.getPath()
        , "-c" , calls.getPath()
        , "-b" , mutations.getPath()
        , "-t" , template.getPath()
    };
    flags.setValidator(null);
    flags.setFlags(flagStrings);
    assertFalse(v.isValid(flags));
    assertTrue(flags.getParseMessage().contains("baseline VCF file doesn't exist"));

    assertTrue(mutations.createNewFile());
    assertFalse(v.isValid(flags));
    assertTrue(flags.getParseMessage().contains("calls VCF file doesn't exist"));

    assertTrue(calls.createNewFile());
    assertFalse(v.isValid(flags));

    ReaderTestUtils.getDNADir(">t" + StringUtils.LS + "ACGT" + StringUtils.LS, template);
    assertTrue(v.isValid(flags));


    assertTrue(out.mkdir());
    assertFalse(v.isValid(flags));

    FileHelper.deleteAll(out);

  }

  public void testEmpty() throws IOException, UnindexableDataException {
    final File out = new File(mDir, "out");
    final File calls = new File(mDir, "calls.vcf.gz");
    FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
    new TabixIndexer(calls).saveVcfIndex();
    final File mutations = new File(mDir, "mutations.vcf.gz");
    FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
    new TabixIndexer(mutations).saveVcfIndex();
    final File template = new File(mDir, "template");
    ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "A", template, null);
    checkMainInitBadFlags("-o", out.getPath()
      , "-c", calls.getPath()
      , "-b", mutations.getPath()
      , "-t", template.getPath());
  }

  public void testNanoSmall() throws IOException, UnindexableDataException {
    check("vcfeval_small", false, false, true, false, "--sample", "sample1", "--vcf-score-field", "QUAL");
  }

  public void testNanoTricky() throws IOException, UnindexableDataException {
    // Tricky cases where the notion of equivalence depends on the treatment of reference bases in the calls

    //check("vcfeval_small_tricky", true, false, true, false, "--vcf-score-field", "QUAL", "-T", "1");
    //check("vcfeval_small_tricky", false, false, true, false, "--vcf-score-field", "QUAL", "-T", "1");
    //check("vcfeval_small_tricky", true, false, true, false, "--vcf-score-field", "QUAL", "--region", "14");

    // Variant on 14 requires less conservative padding removal (use default-trim variant factory)
    // Variant on 21 requires ref base removal AND variant overlap consideration to be independent for each haplotype (use default-trim-id variant factory)
    check("vcfeval_small_tricky", false, false, true, false, "--vcf-score-field", "QUAL", "-T", "1",
      "--XXcom.rtg.vcf.eval.custom-variant-factory=default-trim-id,default-trim-id");
  }

  public void testNanoTricky2() throws IOException, UnindexableDataException {
    // Default of maximizing the sum of baseline and calls variants leaves one out. Switching to maximizing calls includes them all.
    check("vcfeval_small_tricky2", true, false, true, false, "--vcf-score-field", "QUAL", "-T", "1",
      "--XXcom.rtg.vcf.eval.custom-variant-factory=dip-alt,default-trim-id",
      "--XXcom.rtg.vcf.eval.maximize=calls");
  }

  public void testNanoTricky3() throws IOException, UnindexableDataException {
    // Has two alternative variants starting at exactly the same position, needs overlap handling, now works.
    check("vcfeval_small_tricky3", false, false, true, false, "--vcf-score-field", "QUAL", "-T", "1", "--sample", "dummy,sample1",
      "--XXcom.rtg.vcf.eval.custom-variant-factory=hap-alt,squash");
  }

  public void testNanoSmallRegion() throws IOException, UnindexableDataException {
    check("vcfeval_small_region", false, false, false, false, "--sample", "sample1,sample1", "--vcf-score-field", "QUAL", "--region", "chr2:150-1000");
  }

  public void testNanoSmallDiffSamples() throws IOException, UnindexableDataException {
    check("vcfeval_small_samples", false, false, false, false, "--sample", "sample2,sample1", "--vcf-score-field", "QUAL");
  }

  public void testNanoTooComplex() throws IOException, UnindexableDataException {
    check("vcfeval_too_complex", true, false, false, false);
  }

  private String[] appendArgs(String[] args, String...moreArgs) {
    final String[] result = Arrays.copyOf(args, args.length + moreArgs.length);
    System.arraycopy(moreArgs, 0, result, args.length, moreArgs.length);
    return result;
  }

  private void check(String id, boolean expectWarn, boolean checkSlope, boolean checkTp, boolean checkFp, String... args) throws IOException, UnindexableDataException {
    final File template = new File(mDir, "template");
    final File baseline = new File(mDir, "baseline.vcf.gz");
    final File calls = new File(mDir, "calls.vcf.gz");
    final File output = new File(mDir, "output");
    ReaderTestUtils.getReaderDNA(mNano.loadReference(id + "_template.fa"), template, new SdfId(0));
    FileHelper.stringToGzFile(mNano.loadReference(id + "_baseline.vcf"), baseline);
    FileHelper.stringToGzFile(mNano.loadReference(id + "_calls.vcf"), calls);
    new TabixIndexer(baseline).saveVcfIndex();
    new TabixIndexer(calls).saveVcfIndex();
    String[] fullArgs = appendArgs(args, "-o", output.getPath(), "-c", calls.getPath(), "-b", baseline.getPath(), "-t", template.getPath(), "-Z");
    if (checkSlope) {
      fullArgs = appendArgs(fullArgs, "--Xslope-files");
    }
    if (expectWarn) {
      final String stderr = checkMainInitWarn(fullArgs);
      mNano.check(id + "_err.txt", stderr);
    } else {
      checkMainInitOk(fullArgs);
    }
    mNano.check(id + "_weighted_roc.txt", FileUtils.fileToString(new File(output, "weighted_roc.tsv")));
    if (checkSlope) {
      mNano.check(id + "_weighted_slope.txt", FileUtils.fileToString(new File(output, "weighted_slope.tsv")));
    }
    if (checkTp) {
      mNano.check(id + "_tp.vcf", TestUtils.stripVcfHeader(FileUtils.fileToString(new File(output, "tp.vcf"))));
    }
    if (checkFp) {
      mNano.check(id + "_fp.vcf", TestUtils.stripVcfHeader(FileUtils.fileToString(new File(output, "fp.vcf"))));
    }
  }
}
