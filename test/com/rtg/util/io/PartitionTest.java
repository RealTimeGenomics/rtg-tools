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
package com.rtg.util.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * JUnit tests for the corresponding class.
 *
 */
public class PartitionTest extends TestCase {

  public void testNormalBehaviour() throws Exception {
    final File main = FileUtils.createTempDir("partition", "test");
    try {
      final ArrayList<File> list = new ArrayList<>();
      final File[] f = new File[10];
      final StringBuilder sb = new StringBuilder();
      for (int k = 0; k < f.length; ++k) {
        sb.append("X");
        f[k] = new File(main, String.valueOf(k));
        FileUtils.stringToFile(sb.toString(), f[k]);
        list.add(f[k]);
      }
      final List<List<File>> bins = Partition.partition(3, f);
      assertEquals(3, bins.size());
      assertEquals(f[0], bins.get(2).get(3));
      assertEquals(f[1], bins.get(2).get(2));
      assertEquals(f[2], bins.get(1).get(2));
      assertEquals(f[3], bins.get(0).get(2));
      assertEquals(f[4], bins.get(0).get(1));
      assertEquals(f[5], bins.get(1).get(1));
      assertEquals(f[6], bins.get(2).get(1));
      assertEquals(f[7], bins.get(2).get(0));
      assertEquals(f[8], bins.get(1).get(0));
      assertEquals(f[9], bins.get(0).get(0));

      final List<List<File>> bins2 = Partition.partition(3, list);
      assertEquals(3, bins2.size());
      assertEquals(f[0], bins2.get(2).get(3));
      assertEquals(f[1], bins2.get(2).get(2));
      assertEquals(f[2], bins2.get(1).get(2));
      assertEquals(f[3], bins2.get(0).get(2));
      assertEquals(f[4], bins2.get(0).get(1));
      assertEquals(f[5], bins2.get(1).get(1));
      assertEquals(f[6], bins2.get(2).get(1));
      assertEquals(f[7], bins2.get(2).get(0));
      assertEquals(f[8], bins2.get(1).get(0));
      assertEquals(f[9], bins2.get(0).get(0));
} finally {
      assertTrue(FileHelper.deleteAll(main));
    }
  }

}
