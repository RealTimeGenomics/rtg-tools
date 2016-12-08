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

import java.io.File;
import java.util.zip.CRC32;

import com.rtg.mode.SequenceType;
import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.array.longindex.LongCreate;
import com.rtg.util.bytecompression.BitwiseByteArray;
import com.rtg.util.bytecompression.ByteBaseCompression;
import com.rtg.util.bytecompression.ByteCompression;
import com.rtg.util.bytecompression.MultiByteArray;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 *
 */
public class CompressedMemorySequencesWriter extends AbstractSdfWriter {

  private Label mCurrentLabel;
  private final BitwiseByteArray mSeqData;
  private ByteCompression mQualData;
  //MultiByteArray is used here because it supports extending
  private final CRC32 mChecksumSeq;
  private CRC32 mChecksumQual;
  private final MultiByteArray mSeqChecksums;
  private MultiByteArray mQualChecksums;
  private final ExtensibleIndex mPositions;

  private int mCurrentLength;
  private long mCurrentPosition;

  private final SimplePrereadNames mNames;
  private final SimplePrereadNames mNameSuffixes;
  private boolean mHasData;
  private final boolean mWriteFullNames;

  private long mCurrentId;

  private CompressedMemorySequencesReader mReader;

  private final LongRange mOriginalRegion;
  private final LongRange mRestriction;
  private final File mOriginPath;

  /**
   *
   * @param originPath file/directory that is the source of this data
   * @param prereadType type of the data that is being written
   * @param hasQuality type of the data that is being written
   * @param names holder of names
   * @param suffixes holder of name suffixes
   * @param compressed whether <code>SDF</code> should be compressed
   * @param type preread type
   * @param restriction restrict reader to given range of reads
   */
  public CompressedMemorySequencesWriter(File originPath, PrereadType prereadType, boolean hasQuality, SimplePrereadNames names, SimplePrereadNames suffixes, boolean compressed, SequenceType type, LongRange restriction) {
    super(prereadType, hasQuality, names != null, compressed, type);
    setSdfId(new SdfId(0)); //we don't know the provenance of the reads
    final int range = type.numberKnownCodes() + type.firstValid();
    mOriginPath = originPath;
    mSeqData = new BitwiseByteArray(CompressedByteArray.minBits(range));
    mSeqChecksums = new MultiByteArray(0, 20);
    mChecksumSeq = new CRC32();
    mWriteFullNames = suffixes != null;
    if (mHasQuality) {
//      if (Boolean.getBoolean(System.getProperty("com.rtg.qual_compression", "false"))) {
//        mQualData = new ArithByteCompression(CompressedMemorySequencesReader.MAX_QUAL_VALUE, 10000, new Order0ModelBuilder(CompressedMemorySequencesReader.MAX_QUAL_VALUE));
//      } else {
      mQualData = new ByteBaseCompression(CompressedMemorySequencesReader.MAX_QUAL_VALUE);
//      }
      mQualChecksums = new MultiByteArray(0, 20);
      mChecksumQual = new CRC32();
    }
    mNames = names;
    mNameSuffixes = suffixes;

    mPositions = LongCreate.createExtensibleIndex();
    setPosition(0, 0);
    mReader = null;
    final long start;
    final long end;
    if (restriction.getStart() == -1) {
      start = 0;
    } else {
      start = restriction.getStart();
    }
    if (restriction.getEnd() == -1) {
      end = Long.MAX_VALUE;
    } else {
      end = restriction.getEnd();
    }
    mRestriction = new LongRange(start, end);
    mOriginalRegion = restriction;
  }


  @Override
  long getNumberOfSequences() {
    return mCurrentId;
  }

  /**
   * Get the in memory reader after processing is complete
   * @return the reader
   */
  public CompressedMemorySequencesReader getReader() {
    if (mReader == null) {
      throw new IllegalStateException("Close has not been called");
    }
    return mReader;
  }

  @Override
  public void close() {
    final IndexFile index = super.finish(0, mCurrentId);
    // create a new region to avoid IAE in CompressedMemorySequencesReader when total number of sequences is not known up front (ie fastq input).
    final LongRange region = SequencesReaderFactory.resolveRange(index, mOriginalRegion);
    mReader = new CompressedMemorySequencesReader(mOriginPath, index, mSeqData, mQualData, mSeqChecksums, mQualChecksums, mPositions, mNames, mNameSuffixes, region);
  }

  private void setPosition(long index, long position) {
    if (index >= mPositions.length()) {
      final long pos = mPositions.extendBy(1);
      assert  pos == index : "pos: " + pos + " index: " + index;
    }
    mPositions.set(index, position);
  }

  @Override
  public void startSequence(String label) {
    initSequenceStatistics();
    if (mHasNames && mRestriction.isInRange(mCurrentId)) {
      mCurrentLabel = handleSequenceName(label);
    }
    mHasData = false;
    mCurrentLength = 0;
    mChecksumSeq.reset();
    if (mHasQuality) {
      mChecksumQual.reset();
    }
  }

  @Override
  public void write(byte[] rs, byte[] qs, int length) {
    mHasData = true;
    if (length > 0) {
      if (mRestriction.isInRange(mCurrentId)) {
        mSeqData.set(mCurrentPosition, rs, length);
        mDataHashFunction.irvineHash(rs, length);
        mChecksumSeq.update(rs, 0, length);
        if (mHasQuality) {
          if (qs[0] == -1) {
            throw new NoTalkbackSlimException(ErrorType.INVALID_QUALITY_LENGTH, mCurrentLabel == null ? Long.toString(mCurrentId) : mCurrentLabel.toString());
          }
          clipQuality(qs, length);
          mQualData.add(qs, 0, length); // This only works if this method gets one sequence at a time
          mQualityHashFunction.irvineHash(qs, length);
          mChecksumQual.update(qs, 0, length);
        }
        mCurrentPosition += length;
      }
      mCurrentLength += length;
    } else {
      noSequenceWarning(mCurrentLabel);
    }
    updateCountStatistics(rs, qs, length);
  }

  @Override
  public boolean endSequence() {
    endSequenceStatistics();
    if (mHasData) {
      if (mRestriction.isInRange(mCurrentId)) {
        if (mHasNames) {
          mNames.setName(mCurrentId - mRestriction.getStart(), mCurrentLabel.label());
          if (mWriteFullNames) {
            mNameSuffixes.setName(mCurrentId - mRestriction.getStart(), mCurrentLabel.suffix());
          }
        }
        setPosition(mCurrentId - mRestriction.getStart() + 1, mCurrentPosition);
        if (mCurrentLength > 0) {
          mDataHashFunction.irvineHash(mCurrentLength);
          if (mHasQuality) {
            mQualityHashFunction.irvineHash(mCurrentLength);
          }
        }
        updateChecksums();
      }
      updateStatistics(mCurrentLength);
      ++mCurrentId;
      return true;
    } else {
      return false;
    }
  }

  private void updateChecksums() {
    mSeqChecksums.extendTo(mCurrentId - mRestriction.getStart() + 1);
    mSeqChecksums.set(mCurrentId - mRestriction.getStart(), (byte) mChecksumSeq.getValue());
    if (mHasQuality) {
      mQualChecksums.extendTo(mCurrentId - mRestriction.getStart() + 1);
      mQualChecksums.set(mCurrentId - mRestriction.getStart(), (byte) mChecksumQual.getValue());
    }
  }
}
