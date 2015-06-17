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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.sam.SamRangeUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;

/**
 * Common implementation between Tabix and BAM index readers.
 *
 */
@TestClass("com.rtg.tabix.TabixIndexReaderTest")
public abstract class AbstractIndexReader implements LocusIndex {

  private static final int MAX_BIN = ((1 << 18) - 1) / 7 + 1;

  private static final int NUM_BINS = MAX_BIN + 1;

  private static final int LINEAR_INDEX_SHIFT = 14;

  private static final int MAX_COORD = 1 << 29;


  protected final File mIndexFile;

  protected long[] mBinPositions;
  protected long[] mLinearIndexPositions;

  protected String[] mSequenceNames;
  protected Map<String, Integer> mSequenceLookup;

  /**
   * Constructor. Subclasses must appropriately initialize the protected members.
   * @param indexFile the index file
   */
  public AbstractIndexReader(File indexFile) {
    mIndexFile = indexFile;
  }

  /**
   * @return a stream supplying the index data
   * @throws IOException if there was a problem opening the index
   */
  public abstract InputStream openIndexFile() throws IOException;


  @Override
  public String[] sequenceNames() {
    return mSequenceNames.clone();
  }


  @Override
  public VirtualOffsets getFilePointers(SequenceNameLocus region) throws IOException {
    if (region == null) {
      throw new NullPointerException();
    }
    return getFilePointers(SamRangeUtils.createExplicitReferenceRange(region));
  }


  @Override
  public VirtualOffsets getFilePointers(ReferenceRanges<String> ranges) throws IOException {
    final VirtualOffsets offsets = new VirtualOffsets();
    for (String seqName : ranges.sequenceNames()) {
      final Integer seqId = mSequenceLookup.get(seqName); // This id may not be the same as header sequence id in the case of tabix.
      if (seqId == null) {
        continue; // Just means that no data for that sequence was indexed.
      }
      try (InputStream is = openIndexFile()) {
        // TODO this could be further optimized so it doesn't have to re-open for each one, but will require processing the ranges sequences in index id order and with careful skipping between index sections
        getFilePointers(ranges, seqName, is, mBinPositions[seqId], (int) (mLinearIndexPositions[seqId] - mBinPositions[seqId]), offsets);
      }
    }
    offsets.sort();
    return offsets.size() == 0 ? null : offsets;
  }


  /**
   * Retrieve the file pointers associated with region specified on a specific chromosome
   *
   * @param ranges stores all the ranges we care about
   * @param seqName the chromosome name to extract pointers for
   * @param indexStream stream containing index
   * @param binsPosition position of start of bins in index
   * @param binsSize of bins
   * @param results storage space for offset information
   * @throws java.io.IOException if an IO exception occurs
   */
  public static void getFilePointers(ReferenceRanges<String> ranges, String seqName, InputStream indexStream, long binsPosition, int binsSize, VirtualOffsets results) throws IOException {

    //seek to sequence
    FileUtils.skip(indexStream, binsPosition);
    final byte[] binsBuf = new byte[binsSize];
    //read number of bins
    IOUtils.readFully(indexStream, binsBuf, 0, binsBuf.length);
    final int numBins = ByteArrayIOUtils.bytesToIntLittleEndian(binsBuf, 0);

    final byte[] tinyBuf = new byte[4];
    IOUtils.readFully(indexStream, tinyBuf, 0, 4);
    final int linearIndexSize = ByteArrayIOUtils.bytesToIntLittleEndian(tinyBuf, 0);
    final byte[] linIndexBuf = new byte[linearIndexSize * 8]; //TODO check for overflow
    IOUtils.readFully(indexStream, linIndexBuf, 0, linearIndexSize * 8);
    final long[] linearIndex = new long[linearIndexSize];
    //create linear index
    ByteArrayIOUtils.convertToLongArrayLittleEndian(linIndexBuf, linearIndex);

    final boolean[] bins = new boolean[NUM_BINS];

    // Extract bin chunk bounds info
    final int[] binNum = new int[numBins];
    final int[] binNumChunks = new int[numBins];
    final long[][] binChunkBounds = new long[numBins][];
    int binStart = 4;
    for (int i = 0; i < numBins; i++) {
      //find out bin id and number of chunks
      binNum[i] = ByteArrayIOUtils.bytesToIntLittleEndian(binsBuf, binStart + 0);
      final int numChunks = binNumChunks[i] = ByteArrayIOUtils.bytesToIntLittleEndian(binsBuf, binStart + 4);
      final int chunkStart = binStart + 8;
      final int chunkSize = numChunks * 16; //check for overflow
      final long[] chunkBounds = binChunkBounds[i] = new long[numChunks * 2];
      for (int k = 0, j = 0; k < numChunks; k++) {
        chunkBounds[j++] = ByteArrayIOUtils.bytesToLongLittleEndian(binsBuf, chunkStart + k * 16);
        chunkBounds[j++] = ByteArrayIOUtils.bytesToLongLittleEndian(binsBuf, chunkStart + k * 16 + 8);
      }
      binStart = chunkStart + chunkSize;
    }

    // Do queries
    for (RangeList.RangeData<String> range : ranges.get(seqName).getRangeList()) {
      final SequenceNameLocus region = new SequenceNameLocusSimple(seqName, range.getStart(), range.getEnd());

      final int beg = (region.getStart() <= -1 || region.getStart() == Integer.MIN_VALUE) ? 0 : region.getStart();
      final int end = (region.getEnd() <= -1 || region.getEnd() == Integer.MAX_VALUE) ? MAX_COORD : region.getEnd();
      if (end > MAX_COORD || beg > MAX_COORD) {
        throw new NoTalkbackSlimException("The requested region " + region + " contains coordinates greater than can be addressed by tabix/bam indexes");
      }
      final int linearBeg = beg >> LINEAR_INDEX_SHIFT;
      reg2bins(bins, beg, end);

      long begFilePointer = -1;
      long endFilePointer = -1;

      for (int i = 0; i < numBins; i++) {
        final int binNo = binNum[i];
        if (bins[binNo]) {
          final int numChunks = binNumChunks[i];
          final long[] chunkBounds = binChunkBounds[i];
          for (int k = 0, j = 0; k < numChunks; k++) {
            final long chunkBeg = chunkBounds[j++];
            final long chunkEnd = chunkBounds[j++];
            //TODO optimize for bins 1 through 4680
            if (binNo >= 4681 || (linearBeg < linearIndex.length && isLessThanUnsigned(linearIndex[linearBeg], chunkEnd))) {
              if (begFilePointer == -1 || isLessThanUnsigned(chunkBeg, begFilePointer)) {
                begFilePointer = chunkBeg;
              }
              if (endFilePointer == -1 || isLessThanUnsigned(endFilePointer, chunkEnd)) {
                endFilePointer = chunkEnd;
              }
            }
          }
        }
      }
      if (begFilePointer != -1) {
        results.add(begFilePointer, endFilePointer, region);
      }
    }
  }


  /**
   * comparison function to compare two longs as if they were unsigned
   * @param n1 first long
   * @param n2 second long
   * @return true if unsigned version of <code>n1</code> is strictly less than unsigned version of <code>n2</code>
   */
  public static boolean isLessThanUnsigned(long n1, long n2) {
    return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
  }

  /**
   * Set bin status to true for all bins which overlap a region.
   * @param bins array into which bin membership should be stored
   * @param beg 0-based start position (inclusive)
   * @param end 0-based end position (exclusive)
   */
  private static void reg2bins(boolean[] bins, int beg, int end) {
    Arrays.fill(bins, false);
    assert bins.length == NUM_BINS;
    final int cEnd = end - 1;
    int k;
    bins[0] = true;
    for (k = 1 + (beg >> 26); k <= 1 + (cEnd >> 26); ++k) {
      bins[k] = true;
    }
    for (k = 9 + (beg >> 23); k <= 9 + (cEnd >> 23); ++k) {
      bins[k] = true;
    }
    for (k = 73 + (beg >> 20); k <= 73 + (cEnd >> 20); ++k) {
      bins[k] = true;
    }
    for (k = 585 + (beg >> 17); k <= 585 + (cEnd >> 17); ++k) {
      bins[k] = true;
    }
    for (k = 4681 + (beg >> 14); k <= 4681 + (cEnd >> 14); ++k) {
      bins[k] = true;
    }
  }
}
