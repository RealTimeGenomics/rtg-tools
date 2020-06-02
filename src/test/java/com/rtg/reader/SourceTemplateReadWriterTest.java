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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class SourceTemplateReadWriterTest extends TestCase {

  public void test() throws IOException {
    final File temp = FileUtils.createTempDir("sourcetemplate", "tempdir");
    try {
      final File templateFile = new File(temp, "simTemplates");
      SourceTemplateReadWriter.writeTemplateMappingFile(temp, null);
      assertFalse(templateFile.exists());
      SdfId[] resultArray = SourceTemplateReadWriter.readTemplateMap(temp);
      assertNull(resultArray);
      final SdfId[] testArray = {new SdfId(Long.MIN_VALUE), new SdfId(0), new SdfId(Long.MAX_VALUE)};
      SourceTemplateReadWriter.writeTemplateMappingFile(temp, testArray);
      assertTrue(templateFile.exists());
      resultArray = SourceTemplateReadWriter.readTemplateMap(temp);
      assertEquals(testArray.length, resultArray.length);
      for (int i = 0; i < testArray.length; ++i) {
        assertEquals(testArray[i], resultArray[i]);
      }
      assertTrue(templateFile.delete());
      assertTrue(templateFile.createNewFile());
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(templateFile))) {
        bw.write("blah");
      }
      try {
        SourceTemplateReadWriter.readTemplateMap(temp);
        fail();
      } catch (CorruptSdfException e) {
        assertEquals("Malformed simulator template map line blah", e.getMessage());
      }
    } finally {
      FileHelper.deleteAll(temp);
    }
  }

  public void test2() throws IOException {
    final File temp = FileUtils.createTempDir("sourcetemplate", "tempdir");
    try {
      final File templateFile = new File(temp, "mutTemplate");
      SourceTemplateReadWriter.writeMutationMappingFile(temp, null);
      assertFalse(templateFile.exists());
      SdfId result = SourceTemplateReadWriter.readMutationMap(temp);
      assertNull(result);
      final SdfId testSdf = new SdfId(Long.MIN_VALUE);
      SourceTemplateReadWriter.writeMutationMappingFile(temp, testSdf);
      assertTrue(templateFile.exists());
      result = SourceTemplateReadWriter.readMutationMap(temp);
      assertEquals(testSdf, result);
      final File newDir = new File(temp, "newDir");
      assertTrue(newDir.mkdir());
      SourceTemplateReadWriter.copyMutationMappingFile(temp, newDir);
      result = SourceTemplateReadWriter.readMutationMap(newDir);
      assertEquals(testSdf, result);
      assertTrue(templateFile.delete());
      assertTrue(templateFile.createNewFile());
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(templateFile))) {
        bw.write("blah");
      }
      try {
        SourceTemplateReadWriter.readMutationMap(temp);
        fail();
      } catch (CorruptSdfException e) {
        assertEquals("Malformed simulator template map line blah", e.getMessage());
      }
    } finally {
      FileHelper.deleteAll(temp);
    }
  }
}
