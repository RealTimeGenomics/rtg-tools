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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import com.rtg.tabix.IndexTestUtils;
import com.rtg.util.Resources;

import junit.framework.TestCase;

/**
 *
 */
public class BamIndexerTest extends TestCase {

  public void test() throws Exception {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      try (InputStream is = Resources.getResourceAsStream("com/rtg/sam/resources/bam.bam")) {
        BamIndexer.saveBamIndex(is, os);
      }
    } finally {
      os.close();
    }
    final String myBai = IndexTestUtils.bamIndexToUniqueString(new ByteArrayInputStream(os.toByteArray()));
    final String exp;
    try (InputStream baiIs = Resources.getResourceAsStream("com/rtg/sam/resources/bam.bam.bai")) {
      exp = IndexTestUtils.bamIndexToUniqueString(baiIs);
    }
    assertEquals(exp, myBai);
  }

  public void test2() throws Exception {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      try (InputStream is = Resources.getResourceAsStream("com/rtg/sam/resources/mated.bam")) {
        BamIndexer.saveBamIndex(is, os);
      }
    } finally {
      os.close();
    }
    final String myBai = IndexTestUtils.bamIndexToUniqueString(new ByteArrayInputStream(os.toByteArray()));
    final String exp;
    try (InputStream baiIs = Resources.getResourceAsStream("com/rtg/sam/resources/mated.bam.bai")) {
      exp = IndexTestUtils.bamIndexToUniqueString(baiIs);
    }
    assertEquals(exp, myBai);
  }

  public void test3() throws Exception {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      try (InputStream is = Resources.getResourceAsStream("com/rtg/sam/resources/multiSequence.bam")) {
        BamIndexer.saveBamIndex(is, os);
      }
    } finally {
      os.close();
    }
    final String myBai = IndexTestUtils.bamIndexToUniqueString(new ByteArrayInputStream(os.toByteArray()));
    final String exp;
    try (InputStream baiIs = Resources.getResourceAsStream("com/rtg/sam/resources/multiSequence.bam.bai")) {
      exp = IndexTestUtils.bamIndexToUniqueString(baiIs);
    }
    assertEquals(exp, myBai);
  }

  //test combined mapped and unmapped file
  public void testMixed() throws Exception {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      try (InputStream is = Resources.getResourceAsStream("com/rtg/sam/resources/mixed.bam")) {
        BamIndexer.saveBamIndex(is, os);
      }
    } finally {
      os.close();
    }
    final String myBai = IndexTestUtils.bamIndexToUniqueString(new ByteArrayInputStream(os.toByteArray()));
    final String exp;
    try (InputStream baiIs = Resources.getResourceAsStream("com/rtg/sam/resources/mixed.bam.bai")) {
      exp = IndexTestUtils.bamIndexToUniqueString(baiIs);
    }
    assertEquals(exp, myBai);
  }

  public void testIndexFileName() {
    final File f1 = new File("test.bam");
    assertEquals("test.bam.bai", BamIndexer.indexFileName(f1).getName());
    assertEquals("test.bai", BamIndexer.secondaryIndexFileName(f1).getName());
    final File f2 = new File("test");
    assertEquals("test.bai", BamIndexer.indexFileName(f2).getName());
    assertEquals("test.bai", BamIndexer.secondaryIndexFileName(f2).getName());
  }
}
