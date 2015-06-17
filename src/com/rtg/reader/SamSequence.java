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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.sam.SamBamConstants;

import htsjdk.samtools.SAMRecord;

/**
 * Minimal wrapper to hold only necessary information about a <code>SAMRecord</code> for the
 * <code>SamBamSequenceDataSource</code>.
 */
@TestClass("com.rtg.reader.MappedSamBamSequenceDataSourceTest")
public class SamSequence {

  private static final byte READ_PAIRED_FLAG = 0x01;
  private static final byte FIRST_OF_PAIR_FLAG = 0x02;
  private static final byte READ_STRAND_FLAG = 0x04;
  private static final byte NOT_PRIMARY_ALIGNMENT_FLAG = 0x08;

  private final byte[] mReadBases;
  private final byte[] mBaseQualities;
  private final String mReadName;
  private final byte mFlags;

  private final int mProjectedSplitReadPosition;

  /**
   * Turn a <code>SAMRecord</code> into a <code>SamSequence</code>.
   * @param record the <code>SAMRecord</code> to convert.
   */
  public SamSequence(SAMRecord record) {
    assert record != null;
    //Done this way to not keep entire string of SAMRecord just for the name.
    mReadName = new String(record.getReadName().toCharArray());
    mFlags = getFlags(record);
    mReadBases = record.getReadBases();
    mBaseQualities = record.getBaseQualities();

    mProjectedSplitReadPosition = record.getAlignmentStart() * ((record.getFlags() & SamBamConstants.SAM_MATE_IS_REVERSE) != 0 ? 1 : -1);
  }

  private SamSequence(SamSequence copy, String name) {
    mReadName = name;
    mFlags = copy.mFlags;
    mReadBases = copy.mReadBases;
    mBaseQualities = copy.mBaseQualities;

    mProjectedSplitReadPosition = copy.mProjectedSplitReadPosition;
  }

  SamSequence rename(String newName) {
    return new SamSequence(this, newName);
  }

  private static byte getFlags(SAMRecord record) {
    byte flags = 0;
    if (record.getReadPairedFlag()) {
      flags ^= READ_PAIRED_FLAG;
      if (record.getFirstOfPairFlag()) {
        flags ^= FIRST_OF_PAIR_FLAG;
      }
    }
    if (record.getReadNegativeStrandFlag()) {
      flags ^= READ_STRAND_FLAG;
    }
    if (record.getNotPrimaryAlignmentFlag()) {
      flags ^= NOT_PRIMARY_ALIGNMENT_FLAG;
    }
    return flags;
  }

  /**
   * Return true if record was paired.
   * @return true if paired record.
   */
  public boolean getReadPairedFlag() {
    return (mFlags & READ_PAIRED_FLAG) != 0;
  }

  /**
   * Return true if record was first of a pair of records.
   * @return true if record was first of a pair of records.
   */
  public boolean getFirstOfPairFlag() {
    return (mFlags & FIRST_OF_PAIR_FLAG) != 0;
  }

  /**
   * Return true if record was paired.
   * @return true if paired record.
   */
  public boolean getReadNegativeStrandFlag() {
    return (mFlags & READ_STRAND_FLAG) != 0;
  }

  /**
   * Get the read bases.
   * @return the read bases.
   */
  public byte[] getReadBases() {
    return mReadBases;
  }

  /**
   * Get the base quality bytes.
   * @return the base quality bytes.
   */
  public byte[] getBaseQualities() {
    return mBaseQualities;
  }

  /**
   * Get the read name.
   * @return the read name.
   */
  public String getReadName() {
    return mReadName;
  }

  /**
   * Get read base length.
   * @return number of bases in the read.
   */
  public int getReadLength() {
      return mReadBases.length;
  }

  /**
   * @return the estimated position (0 based), or -1 if none
   */
  public int getProjectedPosition() {
    return mProjectedSplitReadPosition;
  }
}
