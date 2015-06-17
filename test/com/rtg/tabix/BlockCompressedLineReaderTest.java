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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rtg.util.gzip.GzipUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.BlockCompressedInputStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class BlockCompressedLineReaderTest extends TestCase {

  public void test() throws IOException {
    final File dir = FileUtils.createTempDir("bclr", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/readerWindow1.sam.gz", new File(dir, "readerWindow1.sam.gz"));
      final BlockCompressedLineReader bclr = new BlockCompressedLineReader(new BlockCompressedInputStream(sam));
      try {
        final long firstSeekPos = (44947L << 16) | 22870;
        bclr.seek(firstSeekPos);
        assertEquals(firstSeekPos, bclr.getFilePointer());
        final String line = bclr.readLine();
        assertTrue(line.startsWith("857\t147\tsimulatedSequence2\t32834"));
        assertEquals(firstSeekPos, bclr.getLineFilePointer());
        assertEquals(firstSeekPos + line.length() + 1, bclr.getFilePointer());
        final String line2 = bclr.readLine();
        assertTrue(line2.startsWith("251\t99\tsimulatedSequence2\t33229"));
        assertEquals((int) '9', bclr.peek());
        final String line3 = bclr.readLine();
        assertTrue(line3.startsWith("91\t163\tsimulatedSequence2\t33238"));
        assertEquals(3, bclr.getLineNumber());
      } finally {
        bclr.close();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testLinearRead() throws IOException {
    final File dir = FileUtils.createTempDir("bclr", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/readerWindow1.sam.gz", new File(dir, "readerWindow1.sam.gz"));
      final BlockCompressedLineReader bclr = new BlockCompressedLineReader(new BlockCompressedInputStream(sam));
      try {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(GzipUtils.createGzipInputStream(new FileInputStream(sam))))) {
          String lineA;
          String lineB;
          while (true) {
            lineA = br.readLine();
            lineB = bclr.readLine();
            if (lineA == null || lineB == null) {
              break;
            }
            assertEquals(lineA, lineB);
          }
          assertNull(lineA);
          assertNull(lineB);
        }
      } finally {
        bclr.close();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
}
