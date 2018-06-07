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

package com.rtg.vcf;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.vcf.header.VcfHeader;

/**
 */
public class AsyncVcfWriterTest extends AbstractVcfWriterTest {

  @Override
  protected VcfWriter getVcfWriter(VcfHeader head, OutputStream out) {
    return new AsyncVcfWriter(new DefaultVcfWriter(head, out));
  }


  public void testBrokenPipe() {
    final VcfHeader head = new VcfHeader();
    head.setVersionValue(VcfHeader.VERSION_VALUE);
    head.addMetaInformationLine("##test1212121");
    head.addSampleName("sample1").addSampleName("sample2");

    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
    .setQuality("12.8")
    .addAltCall("c")
    .addFilter("TEST1")
    .addInfo("DP", "23")
    .setNumberOfSamples(2)
    .addFormatAndSample("GT", "0/0")
    .addFormatAndSample("GT", "0/1")
    .addFormatAndSample("GQ", "100")
    .addFormatAndSample("GQ", "95")
    ;

    final int limit = 500;
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final FilterOutputStream fos = new FilterOutputStream(bos) {
      @Override
      public void write(int b) throws IOException {
        if (bos.size() > limit) {
          throw new IOException("Broken pipe");
        }
        super.write(b);
      }
    };

    try {
      try (final VcfWriter w = getVcfWriter(head, fos)) {
        for (int i = 0; i < 10000; ++i) {
          w.write(rec);
        }
      }
      fail();
    } catch (Exception e) {
      assertFalse(e.getMessage().contains("Self-suppression"));
    }
  }

}
