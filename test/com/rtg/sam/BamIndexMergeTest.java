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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.rtg.tabix.IndexTestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.NanoRegression;

import junit.framework.TestCase;

/**
 */
public class BamIndexMergeTest extends TestCase {

  private NanoRegression mNano;
  @Override
  public void setUp() {
    mNano = new NanoRegression(BamIndexMergeTest.class);
  }
  @Override
  public void tearDown() throws IOException {
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }

  private static final String BAM_RESOURCE = "com/rtg/sam/resources";
  private static final String BAM_FILES = "indexmerge%d.bam";

  public void testBam() throws Exception {
    final File dir = FileUtils.createTempDir("indexmerge", "test");
    try {
      final ArrayList<File> files = new ArrayList<>();
      final ArrayList<Long> dataFileSizes = new ArrayList<>();
      for (int i = 1; i <= 4; i++) {
        final String samFileName = String.format(BAM_FILES, i);
        final File bamFile = new File(dir, samFileName);
        final File baiFile = new File(dir, samFileName + ".bai");
        FileHelper.resourceToFile(String.format("%s/%s", BAM_RESOURCE, samFileName), bamFile);
        FileHelper.resourceToFile(String.format("%s/%s.bai", BAM_RESOURCE, samFileName), baiFile);
        files.add(baiFile);
        dataFileSizes.add(bamFile.length());
      }
      final File mergedIndex = new File(dir, "merged.bam.bai");
      BamIndexMerge.mergeBamIndexFiles(mergedIndex, files, dataFileSizes);
      try (InputStream fis = new FileInputStream(mergedIndex)) {
        final String indexDebug = IndexTestUtils.bamIndexToUniqueString(fis);
        mNano.check("merged.bam.bai.debug", indexDebug);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
}
