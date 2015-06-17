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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.rtg.tabix.AbstractIndexReader;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;

import htsjdk.samtools.SAMSequenceDictionary;

/**
 * Used for reading file block compressed file coordinates from BAM indexes
 */
public class BamIndexReader extends AbstractIndexReader {

  /** Size of fixed portion of header */
  private static final int FIXED_HEADER_SIZE = 8;
  private static final byte[] BAI_MAGIC = {'B', 'A', 'I', 1};

  /**
   * @param indexFile BAM index file to open
   * @param dict sequence dictionary from BAM file
   * @throws IOException if an IO error occurs
   */
  public BamIndexReader(File indexFile, SAMSequenceDictionary dict) throws IOException {
    super(indexFile);
    try (InputStream is = new FileInputStream(indexFile)) {
      final byte[] buf = new byte[FIXED_HEADER_SIZE];
      final int len = IOUtils.readAmount(is, buf, 0, FIXED_HEADER_SIZE);
      if (len != FIXED_HEADER_SIZE) {
        throw new EOFException("File: " + indexFile.getPath() + " is not a valid BAM index. (index does not have a complete header)");
      }
      for (int i = 0; i < BAI_MAGIC.length; i++) {
        if (BAI_MAGIC[i] != buf[i]) {
          throw new IOException("File: " + indexFile.getPath() + " is not a valid BAI index. (index does not have a valid header)");
        }
      }
      final int numRefs = ByteArrayIOUtils.bytesToIntLittleEndian(buf, 4);
      if (numRefs != dict.size()) {
        throw new IllegalArgumentException("Index file: " + indexFile.getPath() + " does not contain the same number of sequences as given sequence dictionary");
      }

      mSequenceNames = new String[numRefs];
      mSequenceLookup = new HashMap<>();
      for (int i = 0; i < dict.size(); i++) {
        mSequenceNames[i] = dict.getSequence(i).getSequenceName();
        mSequenceLookup.put(mSequenceNames[i], i);
      }
      int seqNo = 0;
      mBinPositions = new long[numRefs];
      mLinearIndexPositions = new long[numRefs];
      long pos = FIXED_HEADER_SIZE;
      final byte[] bBuf = new byte[8];
      for (; seqNo < numRefs; seqNo++) {
        mBinPositions[seqNo] = pos;
        IOUtils.readFully(is, bBuf, 0, 4);
        pos += 4;
        final int numBins = ByteArrayIOUtils.bytesToIntLittleEndian(bBuf, 0);
        for (int i = 0; i < numBins; i++) {
          IOUtils.readFully(is, bBuf, 0, 8);
          pos += 8;
          final int numChunks = ByteArrayIOUtils.bytesToIntLittleEndian(bBuf, 4);
          FileUtils.skip(is, numChunks * 16L);
          pos += numChunks * 16L;
        }
        mLinearIndexPositions[seqNo] = pos;
        IOUtils.readFully(is, bBuf, 0, 4);
        pos += 4;
        final int numLinear = ByteArrayIOUtils.bytesToIntLittleEndian(bBuf, 0);
        FileUtils.skip(is, numLinear * 8L);
        pos += numLinear * 8L;
      }
    }
  }

  @Override
  public InputStream openIndexFile() throws IOException {
    return new FileInputStream(mIndexFile);
  }
}
