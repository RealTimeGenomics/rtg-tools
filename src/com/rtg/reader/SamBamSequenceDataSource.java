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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.rtg.mode.DNA;
import com.rtg.mode.SequenceType;
import com.rtg.sam.RecordIterator;
import com.rtg.sam.SamFilter;
import com.rtg.sam.SamFilterIterator;
import com.rtg.sam.SamUtils;
import com.rtg.sam.SkipInvalidRecordsIterator;
import com.rtg.util.array.ArrayUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

/**
 * Sequence data source for SAM and BAM file inputs
 */
public class SamBamSequenceDataSource implements SequenceDataSource {

  private final FileStreamIterator mSourceIt;
  protected final SamSequence[] mRecords;
  protected final boolean mPaired;
  protected final boolean mFlattenPaired;

  private SamReader mSamReader;
  private RecordIterator<SAMRecord> mSamIterator; //RecordIterator<SAMRecord> of either SkipInvalidRecordsIt or some new SamFilterIterator which dedups
  private int mRecordIndex = 0;

  private long mMinLength = Long.MAX_VALUE;
  private long mMaxLength = Long.MIN_VALUE;

  private final int mIndexChanger;
  private final SamFilter mFilter;

  protected SamBamSequenceDataSource(FileStreamIterator inputs, boolean paired, boolean flattenPaired, SamFilter filter) {
    assert inputs != null;
    mSourceIt = inputs;
    mPaired = paired;
    mFlattenPaired = flattenPaired;
    if (mPaired) {
      mRecords = new SamSequence[2];
    } else {
      mRecords = new SamSequence[1];
    }
    mIndexChanger = mRecords.length - 1;
    mFilter = filter;
  }

  /**
   * Construct a SAM or BAM sequence data source from list of SAM or BAM files
   * @param files list of the SAM or BAM file to use as a sequence data source
   * @param paired true if input will be paired, false otherwise
   * @param flattenPaired if <code>paired</code> is false then this will load both arms into a single SDF
   * @param filter this filter will be applied to the sam records
   * @return SamBamSequenceDataSource the sequence data source for the inputs
   */
  public static SamBamSequenceDataSource fromInputFiles(List<File> files, boolean paired, boolean flattenPaired, SamFilter filter) {
    return new SamBamSequenceDataSource(new FileStreamIterator(files, null), paired, flattenPaired, filter);
  }

  @Override
  public SequenceType type() {
    // No such thing as SAM with Protein
    return SequenceType.DNA;
  }

  @Override
  public boolean hasQualityData() {
    // Because we are assuming that this data came from the Picard FastqToSam tool
    // Any SAM or BAM input without qualities on a record should throw an error
    return true;
  }

  @Override
  public boolean nextSequence() throws IOException {
    if (mSamReader == null) {
      if (!nextSource()) {
        return false;
      }
    }
    mRecords[mRecordIndex] = null;
    mRecordIndex = mIndexChanger - mRecordIndex;
    if (mRecords[mRecordIndex] == null && !nextRecords()) {
      return false;
    }
    mMinLength = Math.min(mMinLength, currentLength());
    mMaxLength = Math.max(mMaxLength, currentLength());
    return true;
  }

  protected void checkRecordPaired(SamSequence record) {
    if (!record.getReadPairedFlag()) {
      throw new NoTalkbackSlimException("SAM flags for read " + record.getReadName() + " indicate it is single end.");
    }
  }

  protected boolean placePairedRecord(SamSequence record) {
    if (record != null) {
      checkRecordPaired(record);
      mRecords[record.getFirstOfPairFlag() ? 1 : 0] = record;
      return true;
    }
    return false;
  }

  // Fill mRecords with new records
  protected boolean nextRecords() throws IOException {
    Arrays.fill(mRecords, null);
    if (mPaired) {
      if (placePairedRecord(nextRecord())) {
        if (placePairedRecord(nextRecord())) {
          if (mRecords[0] == null || mRecords[1] == null) {
            final SamSequence r = mRecords[0] == null ? mRecords[1] : mRecords[0];
            throw new NoTalkbackSlimException("Conflicting paired end flags detected in SAM input at read " + r.getReadName() + ".");
          }
        } else {
          throw new NoTalkbackSlimException("Unbalanced read arms detected when processing paired end SAM input.");
        }
      }
    } else {
      SamSequence record = nextRecord();
      if (record != null) {
        if (record.getReadPairedFlag()) {
          if (mFlattenPaired) {
            final String name = record.getReadName() + (record.getFirstOfPairFlag() ? "_1" : "_2") + (record.getProjectedPosition() == -1 ? "" : " " + record.getProjectedPosition()); //wow mega hack, sam can't have spaces so this is storing our horrible pos after a space
            record = record.rename(name);
          } else {
            throw new NoTalkbackSlimException("SAM flags for read " + record.getReadName() + " indicate it is paired end.");
          }
        }
      }
      mRecords[mRecordIndex] = record;
    }
    return haveNextRecords();
  }

  protected boolean haveNextRecords() {
    return mRecords[mRecordIndex] != null;
  }

  protected SamSequence nextRecord() throws IOException {
    if (!mSamIterator.hasNext()) {
      if (nextSource()) {
        return nextRecord();
      } else {
        return null;
      }
    } else {
      return new SamSequence(mSamIterator.next());
    }
  }

  /**
   * Filter out records that don't match the provided read group
   */
  public static class FilterReadGroups implements SamFilter {
    final String mReadGroupId;

    /**
     * @param readGroupId the read group to retain
     */
    public FilterReadGroups(String readGroupId) {
      mReadGroupId = readGroupId;
    }
    @Override
    public boolean acceptRecord(SAMRecord next) {
      return next.getReadGroup().getId().equals(mReadGroupId);
    }
  }

  /**
   * Step to the next input stream.
   * @return true if next source has been opened, false for no more sources.
   */
  private boolean nextSource() throws IOException {
    if (mSamReader != null) {
      mSamReader.close();
    }
    mSamReader = null;
    mSamIterator = null;
    if (mSourceIt.hasNext()) {
      final InputStream is = mSourceIt.next();
      if (is == null) {
        mSamReader = null;
        mSamIterator = null;
        return false;
      }
      mSamReader = SamUtils.makeSamReader(is);
      checkSortOrder();
      final RecordIterator<SAMRecord> it = new SkipInvalidRecordsIterator(mSourceIt.currentFile().getPath(), mSamReader);
      mSamIterator = mFilter == null ? it : new SamFilterIterator(it, mFilter);
      return true;
    }
    return false;
  }

  protected void checkSortOrder() {
    if (mSamReader.getFileHeader().getSortOrder() != SortOrder.queryname) {
      throw new NoTalkbackSlimException("SAM or BAM input must be sorted by queryname.");
    }
  }

  @Override
  public int currentLength() {
    assert mRecords[mRecordIndex] != null;
    return mRecords[mRecordIndex].getReadLength();
  }

  @Override
  public String name() throws IllegalStateException, IOException {
    assert mRecords[mRecordIndex] != null;
    return mRecords[mRecordIndex].getReadName();
  }

  @Override
  public byte[] sequenceData() throws IllegalStateException, IOException {
    assert mRecords[mRecordIndex] != null;
    final byte[] seq = DNA.byteDNAtoByte(mRecords[mRecordIndex].getReadBases());
    if (mRecords[mRecordIndex].getReadNegativeStrandFlag()) {
      DNA.reverseComplementInPlace(seq);
    }
    return seq;
  }

  @Override
  public byte[] qualityData() throws IllegalStateException, IOException {
    assert mRecords[mRecordIndex] != null;
    byte[] quals = mRecords[mRecordIndex].getBaseQualities();
    if (quals == null || quals.length == 0) {
      throw new NoTalkbackSlimException("SAM or BAM input must have qualities.");
    }
    if (mRecords[mRecordIndex].getReadNegativeStrandFlag()) {
      quals = Arrays.copyOf(quals, quals.length);
      ArrayUtils.reverseArrayInPlace(quals);
    }
    return quals;
  }

  @Override
  public void close() throws IOException {
    while (nextSource()) {
      //intentional
    }
  }

  @Override
  public void setDusting(boolean val) {
  }

  @Override
  public long getWarningCount() {
    return 0;
  }

  @Override
  public long getDusted() {
    return 0;
  }

  @Override
  public long getMaxLength() {
    return mMaxLength;
  }

  @Override
  public long getMinLength() {
    return mMinLength;
  }

}
