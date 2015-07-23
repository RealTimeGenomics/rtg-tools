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

import java.io.IOException;
import java.util.Arrays;

import com.rtg.tabix.TabixIndexer.TabixOptions;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.IOUtils;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Helper class for header field encapsulation
 */
public class TabixHeader {

  private static final int FIXED_SIZE = 36;
  private final int mNumSequences;
  private final TabixIndexer.TabixOptions mOptions;
  private final byte[] mSequenceNames;
  private final String[] mSequenceNamesUnpacked;

  TabixHeader(int numSequences, TabixIndexer.TabixOptions options, byte[] sequenceNames) {
    mNumSequences = numSequences;
    mOptions = options;
    mSequenceNames = sequenceNames;
    mSequenceNamesUnpacked = new String[mNumSequences];
    int seqNo = 0;
    int sp = 0;
    for (int i = 0; i < mSequenceNames.length; i++) {
      if (mSequenceNames[i] == 0) {
        mSequenceNamesUnpacked[seqNo++] = new String(mSequenceNames, sp, i - sp);
        sp = i + 1;
      }
    }
  }

  static TabixHeader readHeader(BlockCompressedInputStream is) throws IOException {
    final byte[] fixedData = new byte[FIXED_SIZE];
    IOUtils.readFully(is, fixedData, 0, FIXED_SIZE);
    final int numberReferences = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 4);
    final int format = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 8);
    final int seqCol = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 12) - 1;
    final int begCol = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 16) - 1;
    final int endCol = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 20) - 1;
    final int meta = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 24);
    final int skip = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 28);
    final int sequenceNameLength = ByteArrayIOUtils.bytesToIntLittleEndian(fixedData, 32);
    final byte[] sequenceNames = new byte[sequenceNameLength];
    IOUtils.readFully(is, sequenceNames, 0, sequenceNameLength);
    return new TabixHeader(numberReferences, new TabixIndexer.TabixOptions(format, seqCol, begCol, endCol, meta, skip), sequenceNames);
  }

  static TabixHeader mergeHeaders(TabixHeader firstHeader, TabixHeader nextHeader) {
    int startPos = 0;
    int numSequences;
    if (firstHeader.mNumSequences > 0 && nextHeader.mNumSequences > 0
            && firstHeader.mSequenceNamesUnpacked[firstHeader.mNumSequences - 1].equals(nextHeader.mSequenceNamesUnpacked[0])) {
      final byte[] secondNames = nextHeader.mSequenceNames;
      for (int i = 0; i < secondNames.length; i++) {
        if (secondNames[i] == 0) {
          startPos = i + 1;
          break;
        }
      }
      numSequences = firstHeader.mNumSequences + nextHeader.mNumSequences - 1;
    } else {
      numSequences = firstHeader.mNumSequences + nextHeader.mNumSequences;
    }
    final byte[] mergedNames = Arrays.copyOf(firstHeader.mSequenceNames, firstHeader.mSequenceNames.length + nextHeader.mSequenceNames.length - startPos);
    System.arraycopy(nextHeader.mSequenceNames, startPos, mergedNames, firstHeader.mSequenceNames.length, nextHeader.mSequenceNames.length - startPos);
    return new TabixHeader(numSequences, firstHeader.mOptions, mergedNames);
  }

  public int getNumSequences() {
    return mNumSequences;
  }

  public TabixOptions getOptions() {
    return mOptions;
  }

  public String[] getSequenceNamesUnpacked() {
    return mSequenceNamesUnpacked;
  }

}
