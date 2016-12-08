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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * JUnit tests for the InputFileUtils class.
 */
public class InputFileUtilsTest extends TestCase {

  public void testRemoveRedundantPaths() throws IOException {
    final List<File> files = new ArrayList<>();
    for (int i = 0; i < 5; ++i) {
      final File f = new File("file" + i + ".txt");
      for (int j = 0; j <= i; ++j) {
        files.add(f);
      }
    }
    final List<File> cleaned = InputFileUtils.removeRedundantPaths(files);
    assertEquals(15, files.size());
    assertEquals(5, cleaned.size());
    final Iterator<File> it = cleaned.iterator();
    for (int i = 0; i < 5; ++i) {
      final File f = it.next();
      assertEquals("file" + i + ".txt", f.getPath());
    }
  }

  public void testCheckIdenticalPaths() throws IOException {
    final File f1 = new File("f1");
    final File f2 = new File("f2");
    assertFalse(InputFileUtils.checkIdenticalPaths(f1, f2));
    assertTrue(InputFileUtils.checkIdenticalPaths(f1, f1));
  }
}
