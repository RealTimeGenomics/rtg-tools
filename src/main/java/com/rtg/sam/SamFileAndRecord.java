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
import java.io.IOException;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * Adds some support to SkipInvalidRecordsIterator to allow externally coordinated reading of multiple SAM files.
 */

public final class SamFileAndRecord extends SkipInvalidRecordsIterator implements Comparable<SamFileAndRecord>, Integrity {

  private final int mId;
  private final String mPath;

  // work around to allow user to force file loading when sam header is malformed
  private static final boolean IGNORE_HEADER_SORTORDER = GlobalFlags.isSet(ToolsGlobalFlags.SAM_IGNORE_SORT_ORDER_FLAG);

  protected SamFileAndRecord(String path, int id, RecordIterator<SAMRecord> adaptor) throws IOException {
    super(path, adaptor, false);
    mId = id;
    mPath = path;
    validateParams(path);
  }

  /**
   * @param samFile file to be read.
   * @param id unique identifier within one instance of a <code>SAMMultifileIterator</code>.
   * @throws IOException if an IO Error occurs
   */
  public SamFileAndRecord(final File samFile, final int id) throws IOException {
    super(samFile);
    mPath = samFile.getPath();
    mId = id;
    validateParams(samFile.getPath());
  }

  private void validateParams(String path) throws IOException {
    if (mId < 0) {
      close();
      throw new IllegalArgumentException("id less than zero supplied: " + mId);
    }
    if (header().getSortOrder() != SAMFileHeader.SortOrder.coordinate) {
      if (!IGNORE_HEADER_SORTORDER) {
        throw new IOException("\"" + path + "\" is not sorted in coordinate order.");
      }
    }
  }

  /**
   * Get the name of the underlying file.
   * @return the name of the underlying file.
   */
  public String name() {
    return mPath;
  }

  @Override
  public boolean equals(Object obj) {
    // to keep findbugs quiet
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    // to keep findbugs quiet
    return super.hashCode();
  }

  @Override
  public int compareTo(final SamFileAndRecord that) {
    final int samCompare = SamCompareUtils.compareSamRecords(this.mRecord, that.mRecord);
    if (samCompare != 0) {
      return samCompare;
    }
    final int thisId = this.mId;
    final int thatId = that.mId;
    //System.err.println("id  this=" + thisId + " that=" + thatId);
    if (thisId < thatId) {
      return -1;
    }
    if (thisId > thatId) {
      return +1;
    }
    assert this == that;
    return 0;
  }

  @Override
  public String toString() {
    return "SamFileAndRecord:" + mId + " line=" + mRecordCount + " file=" + mPath + " record=" + mRecord.getSAMString().trim();
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mId >= 0);

    return true;
  }

  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

}
