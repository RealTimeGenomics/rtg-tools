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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.util.TestUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.vcf.DefaultVcfWriter;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfWriter;

public class Ga4ghLooseMatchFilterTest extends AbstractNanoTest {

  protected void checkFiltering(int looseMatchDistance) throws IOException {
    final MemoryPrintStream p = new MemoryPrintStream();
    try (VcfReader r = new VcfReader(new BufferedReader(new StringReader(mNano.loadReference("ga4gh-loose-match_in.vcf"))))) {
      try (VcfWriter w = new Ga4ghLooseMatchFilter(new DefaultVcfWriter(r.getHeader(), p.outputStream()), looseMatchDistance)) {
        while (r.hasNext()) {
          w.write(r.next());
        }
      }
    }
    mNano.check("ga4gh-loose-match-" + looseMatchDistance + "_out.vcf", TestUtils.sanitizeVcfHeader(p.toString()));
  }

  public void testLoose0() throws IOException {
    checkFiltering(0);
  }
  public void testLoose1() throws IOException {
    checkFiltering(1);
  }
  public void testLoose30() throws IOException {
    checkFiltering(30);
  }
}
