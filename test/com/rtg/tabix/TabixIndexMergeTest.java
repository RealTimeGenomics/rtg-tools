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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Test class
 */
public class TabixIndexMergeTest extends AbstractNanoTest {

  private static final String SAM_RESOURCE = "com/rtg/tabix/resources";
  private static final String SAM_FILES = "tabixmerge%d.sam.gz";

  public void testSam() throws Exception {
    final File dir = FileUtils.createTempDir("indexmerge", "test");
    try {
      final ArrayList<File> files = new ArrayList<>();
      final ArrayList<Long> dataFileSizes = new ArrayList<>();
      for (int i = 1; i <= 4; ++i) {
        final String samFileName = String.format(SAM_FILES, i);
        final File samFile = new File(dir, samFileName);
        final File tbiFile = new File(dir, samFileName + ".tbi");
        FileHelper.resourceToFile(String.format("%s/%s", SAM_RESOURCE, samFileName), samFile);
        FileHelper.resourceToFile(String.format("%s/%s.tbi", SAM_RESOURCE, samFileName), tbiFile);
        files.add(tbiFile);
        dataFileSizes.add(samFile.length());
      }
      final File mergedIndex = new File(dir, "merged.sam.gz.tbi");
      TabixIndexMerge.mergeTabixFiles(mergedIndex, files, dataFileSizes);
      try (InputStream fis = new BlockCompressedInputStream(new FileInputStream(mergedIndex))) {
        final String indexDebug = IndexTestUtils.tbiIndexToUniqueString(fis);
        mNano.check("merged.sam.gz.tbi.debug", indexDebug);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

}
