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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.rtg.tabix.SequenceIndex;
import com.rtg.tabix.SequenceIndexContainer;
import com.rtg.tabix.TabixIndexMerge;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.IOUtils;

/**
 * Methods for merging BAM index files
 */
public final class BamIndexMerge {

  private BamIndexMerge() { }

  /**
   * Merge indexes for files that will be concatenated.
   * @param output output index file
   * @param files BAM index files
   * @param dataFileSizes file size of corresponding data files
   * @throws IOException if an IO error occurs
   */
  public static void mergeBamIndexFiles(File output, List<File> files, List<Long> dataFileSizes) throws IOException {
    long pointerAdjust = 0;
    final SequenceIndex[][] indexesSquared = new SequenceIndex[files.size()][];
    final String[][] sequenceNames = new String[files.size()][];
    for (int i = 0; i < files.size(); ++i) {
      final File baiFile = files.get(i);
      try (FileInputStream is = new FileInputStream(baiFile)) {
        final byte[] smallBuf = new byte[8];
        IOUtils.readFully(is, smallBuf, 0, 8);
        final int numSequences = ByteArrayIOUtils.bytesToIntLittleEndian(smallBuf, 4);
        sequenceNames[i] = new String[numSequences];
        for (int j = 0; j < numSequences; ++j) {
          sequenceNames[i][j] = Integer.toString(j);
        }

        indexesSquared[i] = TabixIndexMerge.loadFileIndexes(is, numSequences, pointerAdjust);
      }
      pointerAdjust += dataFileSizes.get(i);
    }

    final List<SequenceIndex> indexes = TabixIndexMerge.collapseIndexes(indexesSquared, sequenceNames);
    final SequenceIndexContainer cont = new SequenceIndexContainer(indexes, 0);
    TabixIndexer.mergeChunks(indexes);
    try (FileOutputStream fos = new FileOutputStream(output)) {
      BamIndexer.writeIndex(cont, fos);
    }
  }
}
