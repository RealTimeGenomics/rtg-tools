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
package com.rtg.sam;

import java.io.File;
import java.util.Arrays;

import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;


/**
 * Uses the first SAM record set up by SamReaderTest to test the
 * extra named field getters of the SamBamRecord wrapper class.
 *
 */
public class SamBamRecordImplTest extends TestCase {

  public void testGetReadName() throws Exception {

    final File tmpDir = FileUtils.createTempDir("bamreader", "blah");
    try {
      final File samFile = new File(tmpDir, "sam.sam");
      final File bamFile = new File(tmpDir, "sam.bam");
      final File bamIndex = new File(tmpDir, "sam.bai");
      final String header = "@HD\tVN:1.0\tSO:coordinate\n"
        + "@SQ\tSN:simulatedSequence\tLN:10000\n"
        + "@PG\tID:rtg\tVN:v2.0-EAP2dev build 20721 (2009-10-01)\n";
      final String content = header
                       + "962\t163\tsimulatedSequence\t16\t255\t5H2I3=5P6M3X5D5N6S\t=\t133\t152\tGTTTCCTCNCCTAATGAGAC\taaaaaaaaaaaaaaaaaaaa\tAS:A:c\tNM:i:1\tMQ:f:2.55\tIH:i:1\tHH:Z:blah\n";
      FileUtils.stringToFile(content, samFile);
      SamUtilsTest.convertSamToBam(bamFile, bamIndex, samFile);

      try (BamReader br = new BamReader(bamFile)) {
        assertTrue(br.hasNext());
        br.next();

        final SamBamRecordImpl sbri = new SamBamRecordImpl(br);

        assertNotNull(sbri.mReader);

        assertEquals("962", sbri.getReadName());
        assertEquals(962, sbri.getReadNumber());
        sbri.getFlags();
        assertTrue(sbri.hasAttribute("MQ"));
        assertEquals('f', sbri.getAttributeType("MQ"));
        assertEquals("blah", (String) sbri.getAttributeValue("HH"));
        assertEquals(5, sbri.getNumFields());
        assertEquals("5H2I3=5P6M3X5D5N6S", sbri.getField(SamBamConstants.CIGAR_FIELD));
        assertEquals(163, sbri.getIntField(1));

        assertEquals("[AS, NM, MQ, IH, HH]", Arrays.toString(sbri.getAttributeTags()));

        assertEquals(1, sbri.getIntAttribute("IH"));
        assertEquals(163, sbri.getFlags());

        try {
          sbri.getFieldNumFromTag("MQ");
          fail();
        } catch (final UnsupportedOperationException uoe) {
          assertEquals("Not supported.", uoe.getMessage());
        }
      }
    } finally {
      FileHelper.deleteAll(tmpDir);
    }
  }
}
