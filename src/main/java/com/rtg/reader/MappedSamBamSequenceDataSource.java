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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rtg.sam.SamFilter;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Sequence data source for the pre-mapped SAM and BAM file inputs from Illumina
 */
public class MappedSamBamSequenceDataSource extends SamBamSequenceDataSource {

  private static final byte[] EMPTY = {};

  private final boolean mKeepSingletons;
  private final Map<String, SamSequence> mRecordMap;
  private long mDuplicates = 0;

  MappedSamBamSequenceDataSource(FileStreamIterator inputs, boolean paired, boolean flattenPaired, boolean keepSingletons, SamFilter filter) {
    super(inputs, paired, flattenPaired, filter);
    if (paired) {
      mRecordMap = new LinkedHashMap<>();
    } else {
      mRecordMap = null;
    }
    mKeepSingletons = keepSingletons;
  }

  /**
   * Construct a pre-mapped SAM or BAM sequence data source from list of SAM or BAM files
   * @param files list of the SAM or BAM file to use as a sequence data source
   * @param paired true if input will be paired, false otherwise
   * @param flattenPaired if <code>paired</code> is false then this will load both arms into a single SDF
   * @param keepSingletons if true, then arms without a mate will be output with an empty mate, otherwise such singletons will be dropped
   * @param filter this filter will be applied to the sam records
   * @return SamBamSequenceDataSource the sequence data source for the inputs
   */
  public static MappedSamBamSequenceDataSource fromInputFiles(List<File> files, boolean paired, boolean flattenPaired, boolean keepSingletons, SamFilter filter) {
    return new MappedSamBamSequenceDataSource(new FileStreamIterator(files), paired, flattenPaired, keepSingletons, filter);
  }

  @Override
  protected void checkSortOrder() { }

  @Override
  protected boolean nextRecords() throws IOException {
    if (mPaired) {
      SamSequence rec;
      while ((rec = nextRecord()) != null) {
        checkRecordPaired(rec);
        final SamSequence pair = mRecordMap.remove(rec.getReadName());
        if (pair == null) {
          mRecordMap.put(rec.getReadName(), rec);
        } else {
          if (rec.getFirstOfPairFlag() == pair.getFirstOfPairFlag()) {
            if (mDuplicates < 5) {
              Diagnostic.warning("Read " + rec.getReadName() + " is duplicated in SAM input.");
              if (mDuplicates == 4) {
                Diagnostic.warning("Subsequent warnings of this type will not be shown.");
              }
            }
            mRecordMap.put(rec.getReadName(), rec);
            ++mDuplicates;
            continue;
          }

          placePairedRecord(rec);
          placePairedRecord(pair);
          assert haveNextRecords();
          return true;
        }
      }
      if (!mRecordMap.isEmpty()) {
        if (mKeepSingletons) {
          // Grab any one out of the map and return it with a dummy. Eventually we'll empty the whole map.
          final SamSequence singleton = mRecordMap.remove(mRecordMap.keySet().iterator().next());
          final SamSequence dummy = new SamSequence(singleton.getReadName(), EMPTY, EMPTY, (byte) (SamSequence.READ_PAIRED_FLAG | (singleton.getFirstOfPairFlag() ? 0 : SamSequence.FIRST_OF_PAIR_FLAG)), 0);
          placePairedRecord(singleton);
          placePairedRecord(dummy);
          assert haveNextRecords();
          return true;
        }
        Diagnostic.warning(mRecordMap.size() + " reads missing a pair when processing paired end SAM input.");
      }
      if (mDuplicates > 0) {
        Diagnostic.warning(mDuplicates + " records ignored as duplicates in input");
      }
      return false;
    } else {
      return super.nextRecords();
    }
  }
}
