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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * class representing index (for writing)
 */
@TestClass("com.rtg.tabix.TabixIndexerTest")
public class SequenceIndex {

  private static final int LINEAR_INTERVAL = 16384;

  private final TreeMap<Integer, ArrayList<SequenceIndexChunk>> mBins;
  private long[] mLinearIndex;
  private int mLinearSize = 0;

  private int mPreviousChunk = -1;
  private long mPreviousOffset = -1;

  /**
   * Construct a SequenceIndex with a set sequence length
   * @param seqLength the sequence length
   */
  public SequenceIndex(final int seqLength) {
    mBins = new TreeMap<>();
    mLinearIndex = new long[(seqLength / LINEAR_INTERVAL) + 1];
  }

  /**
   * Construct a default SequenceIndex
   */
  public SequenceIndex() {
    mBins = new TreeMap<>();
    mLinearIndex = new long[5];
  }

  void addChunk(final int bin, final long chunkBegin, final long chunkEnd) {
    final ArrayList<SequenceIndexChunk> chunks;
    if (!mBins.containsKey(bin)) {
      chunks = new ArrayList<>();
      mBins.put(bin, chunks);
    } else {
      chunks = mBins.get(bin);
    }
    chunks.add(new SequenceIndexChunk(chunkBegin, chunkEnd));
  }

  private void ensureLinearSize(int chunk) {
    if (chunk >= mLinearIndex.length) {
      long newSize = mLinearIndex.length;
      while (chunk >= newSize) {
        newSize = newSize * 3L / 2L;
      }
      if (newSize > Integer.MAX_VALUE) {
        newSize = Integer.MAX_VALUE;
      }
      mLinearIndex = Arrays.copyOf(mLinearIndex, (int) newSize);
    }
  }

  private void setLinearIndexInternal(int chunk, long virtualOffset) {
    ensureLinearSize(chunk);
    if (mLinearIndex[chunk] == 0) {
      mLinearIndex[chunk] = virtualOffset;
      if (chunk >= mLinearSize) {
        mLinearSize = chunk + 1;
      }
    }
  }
  void setLinearIndex(final int chunk, final long virtualOffset, int minBin) {
    if (chunk > minBin) {
      if (mPreviousChunk != -1) {
        for (int i = mPreviousChunk + 1; i < chunk; i++) {
          setLinearIndexInternal(i, mPreviousOffset);
        }
      }
      setLinearIndexInternal(chunk, virtualOffset);
    }
    mPreviousChunk = chunk;
    mPreviousOffset = virtualOffset;
  }

  //for index merging
  void addChunks(SequenceIndex other) {
    for (final Map.Entry<Integer, ArrayList<SequenceIndexChunk>> entry : other.mBins.entrySet()) {
      final int binNo = entry.getKey();
      if (binNo != TabixIndexer.META_BIN || !mBins.containsKey(TabixIndexer.META_BIN)) {
        for (final SequenceIndexChunk chunk : entry.getValue()) {
          addChunk(binNo, chunk.mChunkBegin, chunk.mChunkEnd);
        }
      } else {
        final ArrayList<SequenceIndexChunk> metaChunk = mBins.get(TabixIndexer.META_BIN);
        final ArrayList<SequenceIndexChunk> otherMetaChunk = other.mBins.get(TabixIndexer.META_BIN);
        metaChunk.get(0).mChunkEnd = otherMetaChunk.get(0).mChunkEnd;
        metaChunk.get(1).mChunkBegin = metaChunk.get(1).mChunkBegin + otherMetaChunk.get(1).mChunkBegin;
        metaChunk.get(1).mChunkEnd = metaChunk.get(1).mChunkEnd + otherMetaChunk.get(1).mChunkEnd;
      }
    }
  }

  void addLinearIndex(SequenceIndex other) {
    for (int i = 0; i < other.mLinearSize; i++) {
      setLinearIndex(i, other.mLinearIndex[i], -1);
    }
  }

  public TreeMap<Integer, ArrayList<SequenceIndexChunk>> getBins() {
    return mBins;
  }

  /**
   * Geat the linear index at the specified index
   * @param index the index
   * @return the specified linear index
   */
  public long getLinearIndex(int index) {
    return mLinearIndex[index];
  }

  public int getLinearSize() {
    return mLinearSize;
  }

  /**
   * A chunk to be used by sequence index.
   */
  public static class SequenceIndexChunk {
    long mChunkBegin;
    long mChunkEnd;

    SequenceIndexChunk(final long chunkBegin, final long chunkEnd) {
      this.mChunkBegin = chunkBegin;
      this.mChunkEnd = chunkEnd;
    }
  }
}
