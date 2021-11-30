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
import java.util.List;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfReaderFactory;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 */
public class GenotypeProportionsTest extends AbstractNanoTest {

  public void test() throws IOException {
    check("gt_prop");
  }

  public void test2() throws IOException {
    check("gt_prop_ploidy");
  }

  private void check(String id) throws IOException {
    final GenotypeProportions prop = new GenotypeProportions();
    final String vcfResource = id + "_test.vcf";
    try (VcfReader r = new VcfReaderFactory().make(new BufferedReader(new StringReader(mNano.loadReference(vcfResource))))) {
      while (r.hasNext()) {
        final VcfRecord rec = r.next();
        final List<String> sampleGts = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
        prop.addRecord(new Genotype(sampleGts.get(0)), new Genotype(sampleGts.get(1)), new Genotype(sampleGts.get(2)));
      }
    }
    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      prop.writeResults(mps.printStream());
      mNano.check(id + "_results.txt", mps.toString());
    }
    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      prop.canonicalParents().writeResults(mps.printStream());
      mNano.check(id + "_can_results.txt", mps.toString());
    }
    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      prop.filterMultiallelic().writeResults(mps.printStream());
      mNano.check(id + "_biallelic_results.txt", mps.toString());
    }
    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      prop.filterNonDiploid().writeResults(mps.printStream());
      mNano.check(id + "_diploid_results.txt", mps.toString());
    }
  }
}
