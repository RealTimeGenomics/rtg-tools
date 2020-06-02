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

package com.rtg.tabix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import com.rtg.util.StringUtils;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import junit.framework.TestCase;

/**
 * Test class
 */
public class GenericPositionReaderTest extends TestCase {

  private static final String SNP_FILE = "#name\tposition\ttype\treference\tprediction\tposterior\tcoverage\tcorrection\tsupport_statistics" + StringUtils.LS
    + "simulatedSequence1\t184\te\tT\tA:T\t5.0\t19\t0.378\tA\t6\t0.119\tT\t13\t0.259" + StringUtils.LS
    + "simulatedSequence1\t2180\te\tC\tG:T\t28.7\t35\t0.697\tG\t18\t0.358\tT\t17\t0.338" + StringUtils.LS;

  private static final String INVALID = "simulatedSequence1\t2180.9\te\tC\tG:T\t28.7\t35\t0.697\tG\t18\t0.358\tT\t17\t0.338" + StringUtils.LS;

  private static GenericPositionReader makeGpr(String contents) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (final BlockCompressedOutputStream out = new BlockCompressedOutputStream(baos, (File) null)) {
      out.write(contents.getBytes());
    }
    final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    return new GenericPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(bais)), new TabixIndexer.TabixOptions(TabixIndexer.TabixOptions.FORMAT_GENERIC, 0, 1, 1, '#', 0, false));
  }

  public void testNormalRetrieval() throws IOException {
    try (GenericPositionReader gpr = makeGpr(SNP_FILE)) {
      final int[] pos = {183, 2179};
      final int[] len = {1, 1};
      int i = 0;
      while (gpr.hasNext()) {
        gpr.next();
        assertEquals("simulatedSequence1", gpr.getReferenceName());
        assertEquals(pos[i], gpr.getStartPosition());
        assertEquals(len[i], gpr.getLengthOnReference());
        ++i;
      }
      assertEquals(2, i);
    }
  }

  public void testNiceException() {
    try (GenericPositionReader gpr = makeGpr(SNP_FILE + INVALID)) {
      while (gpr.hasNext()) {
        gpr.next();
      }
      fail("Expected exception");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("did not contain an integer"));
    }
  }

}
