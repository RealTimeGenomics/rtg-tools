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

package com.rtg.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.rtg.mode.SequenceType;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.SeekableStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class DataFileOpenerFactoryTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final File dir = FileUtils.createTempDir("temp", "temp");
    try {
      ReaderTestUtils.getDNADir(">abcde\nnacgt", dir);
      final DataFileOpenerFactory fact = new DataFileOpenerFactory(IndexFile.SEQUENCE_ENCODING_COMPRESSED, IndexFile.QUALITY_ENCODING_COMPRESSED, SequenceType.DNA);
      final byte[] b = new byte[5];
      try (InputStream is = fact.getLabelOpener().open(new File(dir, "namedata0"), 6)) {
        final int len = is.read(b);
        assertEquals("abcde".substring(0, len), new String(b, 0, len));
      }
      try (SeekableStream s = fact.getSequenceOpener().openRandomAccess(new File(dir, "seqdata0"), 5)) {
        s.seek(1);
        IOUtils.readFully(s, b, 0, 4);
        assertTrue(Arrays.equals(new byte[]{1, 2, 3, 4, (byte) 'e'}, b));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testOtherMethods() throws Exception {
    final File dir = FileUtils.createTempDir("temp", "temp");
    try {
      final File blah = new File(dir, "blah");
      assertTrue(blah.createNewFile());
      final DataFileOpenerFactory fact = new DataFileOpenerFactory(IndexFile.SEQUENCE_ENCODING_COMPRESSED, IndexFile.QUALITY_ENCODING_COMPRESSED, SequenceType.DNA);
      InputStream is = fact.getSequenceOpener().open(blah, 0);
      try {
        assertTrue(fact.getSequenceOpener().getClass().getName(), is instanceof FileBitwiseInputStream);
      } finally {
        is.close();
      }
      is = fact.getSequenceOpener().openRandomAccess(blah, 0);
      try {
        assertTrue(fact.getSequenceOpener().getClass().getName(), is instanceof FileBitwiseInputStream);
      } finally {
        is.close();
      }
      is = fact.getQualityOpener().open(blah, 0);
      try {
        assertTrue(fact.getQualityOpener().getClass().getName(), is instanceof FileCompressedInputStream);
      } finally {
        is.close();
      }
      is = fact.getQualityOpener().openRandomAccess(blah, 0);
      try {
        assertTrue(fact.getQualityOpener().getClass().getName(), is instanceof FileCompressedInputStream);
      } finally {
        is.close();
      }
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

}
