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
package com.rtg.vcf.mendelian;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.NanoRegression;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class GenotypeProportionsTest extends TestCase {

  private NanoRegression mNano = null;
  @Override
  public void setUp() throws Exception {
    super.setUp();
    mNano = new NanoRegression(GenotypeProportionsTest.class);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }

  public void test() throws IOException {
    final String vcfResource = "gt_prop_test.vcf";
    final String resultName = "gt_prop_results.txt";
    check(vcfResource, resultName);
  }

  public void test2() throws IOException {
    final String vcfResource = "gt_prop_ploidy_test.vcf";
    final String resultName = "gt_prop_ploidy_results.txt";
    check(vcfResource, resultName);
  }

  private void check(String vcfResource, String resultName) throws IOException {
    final GenotypeProportions prop = new GenotypeProportions();

    try (VcfReader r = new VcfReader(new BufferedReader(new StringReader(mNano.loadReference(vcfResource))))) {
      while (r.hasNext()) {
        final VcfRecord rec = r.next();
        final ArrayList<String> sampleGts = rec.getFormatAndSample().get(VcfUtils.FORMAT_GENOTYPE);
        prop.addRecord(new Genotype(sampleGts.get(0)), new Genotype(sampleGts.get(1)), new Genotype(sampleGts.get(2)));
      }
    }
    final MemoryPrintStream mps = new MemoryPrintStream();
    prop.writeResults(mps.printStream());
    mNano.check(resultName, mps.toString());
  }
}
