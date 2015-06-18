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
import java.io.IOException;
import java.io.InputStream;

import com.rtg.util.Resources;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import htsjdk.samtools.util.RuntimeIOException;
import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class BamReaderTest extends TestCase {

  private static BamReader readerFromResource(final String resource) throws IOException {
    final InputStream is = Resources.getResourceAsStream(resource);
    try {
      return new BamReader(is);
    } catch (final IOException e) {
      if (is != null) {
        is.close();
      }
      throw e;
    }
  }

  public void testNumReferences() throws IOException {
    try (BamReader br = readerFromResource("com/rtg/sam/resources/bam.bam")) {
      assertEquals(1, br.numReferences());
    }
  }

  public void testReferenceLength() throws IOException {
    try (BamReader br = readerFromResource("com/rtg/sam/resources/bam.bam")) {
      assertEquals(100000, br.referenceLength(0));
    }
  }

  public void testReferenceName() throws IOException {
    try (BamReader br = readerFromResource("com/rtg/sam/resources/bam.bam")) {
      assertEquals("reference", br.referenceName(0));
    }
  }

  public void testGetIntField() throws IOException {
    try (BamReader br = readerFromResource("com/rtg/sam/resources/bam.bam")) {
      assertTrue(br.hasNext());
      br.next();
      assertEquals(0, br.getIntField(SamBamConstants.RNAME_FIELD));
      assertEquals(16381, br.getIntField(SamBamConstants.POS_FIELD));
      assertEquals(0, br.getIntField(SamBamConstants.ISIZE_FIELD));
      assertEquals(585, br.getIntField(BamReader.BIN_FIELD));
      assertTrue(br.hasNext());
      br.next();
      assertEquals(0, br.getIntField(SamBamConstants.RNAME_FIELD));
      assertEquals(32763, br.getIntField(SamBamConstants.POS_FIELD));
      assertEquals(0, br.getIntField(SamBamConstants.ISIZE_FIELD));
      assertEquals(585, br.getIntField(BamReader.BIN_FIELD));
      assertFalse(br.hasNext());
    }
  }

  public void testGetStringField() throws IOException {
    try (BamReader br = readerFromResource("com/rtg/sam/resources/bam.bam")) {
      assertTrue(br.hasNext());
      br.next();
      assertEquals("reference", br.getField(SamBamConstants.RNAME_FIELD));
      assertEquals("foo", br.getField(SamBamConstants.QNAME_FIELD));
      assertEquals("10M", br.getField(SamBamConstants.CIGAR_FIELD));
      assertEquals("AAAAATTTTT", br.getField(SamBamConstants.SEQ_FIELD));
      assertEquals("*", br.getField(SamBamConstants.QUAL_FIELD));
      assertTrue(br.hasNext());
      br.next();
      assertEquals("reference", br.getField(SamBamConstants.RNAME_FIELD));
      assertEquals("foo", br.getField(SamBamConstants.QNAME_FIELD));
      assertEquals("10M", br.getField(SamBamConstants.CIGAR_FIELD));
      assertEquals("AAAAATTTTT", br.getField(SamBamConstants.SEQ_FIELD));
      assertFalse(br.hasNext());
    }
  }

  public void testArrays() throws Exception {
    try (final TestDirectory tmpDir = new TestDirectory("bamreader")) {
      final File samFile = new File(tmpDir, "sam.sam");
      final File bamFile = new File(tmpDir, "sam.bam");
      final File bamIndex = new File(tmpDir, "sam.bai");
      final String header = "@HD\tVN:1.4\tSO:coordinate\n"
        + "@SQ\tSN:simulatedSequence\tLN:10000\n"
        + "@PG\tID:rtg\tVN:v2.0-EAP2dev build 20721 (2009-10-01)\n";
      final String content = header
                       + "962\t163\tsimulatedSequence\t16\t255\t5H2I3=5P2M3X5D5N6S\t=\t133\t152\tGTTTCCTCNCCGTAGTGGAATCGATGCTAATGAGAC\taaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\tAS:A:c\tNM:i:1\tMQ:f:2.55\tIH:i:1\tHH:Z:blah\n";
      FileUtils.stringToFile(content, samFile);
      SamUtilsTest.convertSamToBam(bamFile, bamIndex, samFile);


      try (BamReader br = new BamReader(bamFile)) {
        try {
          br.getField(SamBamConstants.CIGAR_FIELD);
          fail();
        } catch (final IllegalStateException ise) {
        }
        assertTrue(br.hasNext());
        final SamBamRecord sbr = br.next();
        assertNotNull(sbr);
        try {
          br.getIntField(Integer.MAX_VALUE);
          fail();
        } catch (final IllegalArgumentException iae) {
          assertEquals("Invalid int field: " + Integer.MAX_VALUE, iae.getMessage());
        }
        try {
          br.getField(-1);
          fail();
        } catch (final IllegalArgumentException iae) {
          assertEquals("Invalid String field: -1", iae.getMessage());
        }
        assertEquals(16, br.getIntField(SamBamConstants.POS_FIELD));
        assertEquals(163, br.getIntField(SamBamConstants.FLAG_FIELD));
        assertEquals(255, br.getIntField(SamBamConstants.MAPQ_FIELD));
        assertEquals(0, br.getIntField(SamBamConstants.MRNM_FIELD));
        assertEquals(133, br.getIntField(SamBamConstants.MPOS_FIELD));
        assertEquals("5H2I3=5P2M3X5D5N6S", br.getField(SamBamConstants.CIGAR_FIELD));
        assertEquals("GTTTCCTCNCCGTAGTGGAATCGATGCTAATGAGAC", br.getField(SamBamConstants.SEQ_FIELD));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", br.getField(SamBamConstants.QUAL_FIELD));
        assertTrue(br.hasAttribute(SamUtils.ATTRIBUTE_NUM_MISMATCHES));
        assertFalse(br.hasAttribute("HB"));
        assertEquals(1, br.getAttributeValue(SamUtils.ATTRIBUTE_NUM_MISMATCHES));
        assertEquals(1, br.getIntAttribute(SamUtils.ATTRIBUTE_NUM_MISMATCHES));
        try {
          assertEquals(1, br.getIntAttribute("HH"));
          fail();
        } catch (final IllegalArgumentException iae) {
          assertEquals("Not an implemented int type: Z", iae.getMessage());
        }
        assertEquals('Z', br.getAttributeType("HH"));
        assertEquals('?', br.getAttributeType("QQ"));
        assertEquals("blah", br.getAttributeValue("HH"));
        assertEquals(2.55f, (Float) br.getAttributeValue("MQ"), 0.01);
        assertEquals('c', br.getAttributeValue(SamUtils.ATTRIBUTE_ALIGNMENT_SCORE));
        assertNull(br.getAttributeValue("QQ"));

        assertNotNull(br.virtualOffset());
        assertNotNull(br.nextVirtualOffset());
        try {
          br.next();
          fail();
        } catch (final RuntimeIOException sfe) {
          assertEquals("Unexpected end of file while reading BAM file", sfe.getMessage());
        }
        assertFalse(br.isSam());
        assertEquals(5, br.getNumFields());

        TestUtils.containsAll(br.getHeaderLines(), "@HD", "@SQ\tSN:simulatedSequence\tLN:10000", "@PG\tID:rtg");
      }
    }
  }
}
