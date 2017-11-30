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

package com.rtg.vcf;

import static com.rtg.util.StringUtils.LS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;


/**
 */
public class ReorderingVcfWriterTest extends TestCase {

  public ReorderingVcfWriterTest(String name) {
    super(name);
  }

  public void test() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final VcfHeader header = new VcfHeader();
    header.addSampleName("SAMPLE");
    final ReorderingVcfWriter c = new ReorderingVcfWriter(new DefaultVcfWriter(header, bos));
    final VcfRecord rec = createRecord("chr2", 123);
    final VcfRecord rec1 = createRecord("chr1", 12);
    final VcfRecord rec2 = createRecord("chr1", 10).addAltCall("g"); //should be after rec3 since it has more alts
    final VcfRecord rec3 = createRecord("chr1", 10);


    c.addRecord(rec);
    c.addRecord(rec1);
    c.addRecord(rec2);
    c.addRecord(rec3);

    c.close();

    final String exp = ""
      + "chr2  123 . a c,t 12.8  . ." + LS // Note that reordering is only within chromosomes
      + "chr1  10  . a c,t 12.8  . ." + LS
      + "chr1  10  . a c,t,g 12.8  . ." + LS
      + "chr1  12  . a c,t 12.8  . ." + LS;

    assertEquals(exp.replaceAll("[ ]+", "\t"), TestUtils.stripLines(bos.toString(), "#", LS));
  }

  private static VcfRecord createRecord(String chr, int pos) {
    final VcfRecord rec = new VcfRecord(chr, pos - 1, "a");
    rec.setId(".")
    .setQuality("12.8")
    .addAltCall("c")
    .addAltCall("t");
    return rec;
  }

  public void testLimitExceeded() throws IOException {
    final MemoryPrintStream mps = orderLimit(10000);
    TestUtils.containsAll(mps.toString()
        , "VcfRecord dropped due to excessive out-of-order processing"
        , "chr2\t1\t.\ta\tc,t\t12.8\t.\t."
    );
  }

  private MemoryPrintStream orderLimit(int number) throws IOException {
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      final VcfHeader header = new VcfHeader();
      header.addSampleName("SAMPLE");
      final ReorderingVcfWriter c = new ReorderingVcfWriter(new DefaultVcfWriter(header, bos));
      // Why +3 ?
      c.addRecord(createRecord("chr2", 2)); //first output.
      c.addRecord(createRecord("chr2", number + 3)); //if number >  buffer length will cause first record to be written
      c.addRecord(createRecord("chr2", 1)); // before first output, should cause error if number is big enough
      c.close();

    } finally {
      Diagnostic.setLogStream();
    }
    return mps;
  }

  public void testUnderLimit() throws IOException {
    final MemoryPrintStream mps = orderLimit(9999);
    assertEquals("", mps.toString());
  }
}
