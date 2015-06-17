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

import static com.rtg.util.StringUtils.FS;

import java.io.File;

import junit.framework.TestCase;

/**
 */
public class SdfFileUtilsTest extends TestCase {

  /**
   * Test method for {@link SdfFileUtils#sequenceDataFile(java.io.File, int)}.
   */
  public final void testSequenceDataFile() {
    final File dir = new File("dir");
    final File act = SdfFileUtils.sequenceDataFile(dir, 42);
    assertEquals("dir" + FS + "seqdata42", act.toString());
  }

  /**
   * Test method for {@link SdfFileUtils#sequencePointerFile(java.io.File, int)}.
   */
  public final void testSequencePointerFile() {
    final File dir = new File("dir");
    final File act = SdfFileUtils.sequencePointerFile(dir, 42);
    assertEquals("dir" + FS + "seqpointer42", act.toString());
  }

  /**
   * Test method for {@link SdfFileUtils#sequenceIndexFile(java.io.File)}.
   */
  public final void testSequenceIndexFile() {
    final File dir = new File("dir");
    final File act = SdfFileUtils.sequenceIndexFile(dir);
    assertEquals("dir" + FS + "sequenceIndex0", act.toString());
  }

  /**
   * Test method for {@link SdfFileUtils#labelDataFile(java.io.File, int)}.
   */
  public final void testLabelDataFile() {
    final File dir = new File("dir");
    final File act = SdfFileUtils.labelDataFile(dir, 42);
    assertEquals("dir" + FS + "namedata42", act.toString());
  }

  /**
   * Test method for {@link SdfFileUtils#labelPointerFile(java.io.File, int)}.
   */
  public final void testLabelPointerFile() {
    final File dir = new File("dir");
    final File act = SdfFileUtils.labelPointerFile(dir, 42);
    assertEquals("dir" + FS + "namepointer42", act.toString());
  }

  /**
   * Test method for {@link SdfFileUtils#labelIndexFile(java.io.File)}.
   */
  public final void testLabelIndexFile() {
    final File dir = new File("dir");
    final File act = SdfFileUtils.labelIndexFile(dir);
    assertEquals("dir" + FS + "nameIndex0", act.toString());
  }
}
