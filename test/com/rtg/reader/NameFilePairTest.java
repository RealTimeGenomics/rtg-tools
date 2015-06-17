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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class NameFilePairTest extends TestCase {

  public void testPairs() throws Exception {
    final File tmpDir = FileUtils.createTempDir("namefilepairtest", "blah");
    final File names = new File(tmpDir, "names");
    final File ptrs = new File(tmpDir, "ptrs");
    try {
      final NameFilePair nfp = new NameFilePair(names, ptrs, 13);

      assertTrue(nfp.canWriteName(1));
      assertFalse(nfp.canWriteName(13));

      nfp.writeName("woo!");

      assertTrue(names.exists());
      assertTrue(ptrs.exists());

      assertTrue(nfp.canWriteName(1));
      nfp.forceWriteName("feck");

      assertTrue(nfp.canWriteName(2));
      assertFalse(nfp.canWriteName(3));

      nfp.forceWriteName("BLLLAAA");

      assertFalse(nfp.canWriteName(1));

      nfp.writeName("fail");
      try {
        nfp.forceWriteName("");
        fail();
      } catch (final Exception e) {
        //expected
      } finally {
        nfp.close();
      }

      final String namesStr = FileUtils.fileToString(names);
      assertTrue(namesStr.contains("woo!"));
      assertTrue(namesStr.contains("feck"));
      assertTrue(namesStr.contains("BL"));
      assertFalse(namesStr.contains("BLL"));
      assertTrue(namesStr.contains("fail"));
      try (DataInputStream dis = new DataInputStream(new FileInputStream(ptrs))) {
        assertEquals(0, dis.readInt());
        assertEquals(5, dis.readInt());
      }
    } finally {
      FileHelper.deleteAll(tmpDir);
    }
  }

}
