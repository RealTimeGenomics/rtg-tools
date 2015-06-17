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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;

import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMValidationError;
import htsjdk.samtools.SamReader;

/**
 * Connects between SamFileReaderAdaptor and <code>RecordIterator</code>, skipping (and warns for) records
 * that are invalid according to Picard. Use this only if you have a single
 * file to process and do not need additional filtering.
 */
public class SkipInvalidRecordsIterator extends AbstractSamRecordIterator {

  private static final int NUM_RECORDS_TO_WARN = 5;

  private final String mPath;
  private final RecordIterator<SAMRecord> mWrapped;
  private final boolean mSilent;

  private boolean mIsClosed = false;
  protected SAMRecord mRecord;

  /**
   * Constructor
   * @param path filename to use for errors and warnings
   * @param reader supplier of <code>SAMRecords</code>
   * @param silent true to not report warnings
   * @throws IOException if an IO error occurs
   */
  public SkipInvalidRecordsIterator(String path, RecordIterator<SAMRecord> reader, boolean silent) throws IOException {
    super(reader.header());
    mSilent = silent;
    mPath = path;
    mWrapped = reader;

    try {
      nextRecord();
    } catch (final SAMFormatException e) {
      mWrapped.close();
      throw new NoTalkbackSlimException(ErrorType.SAM_BAD_FORMAT, path, e.getMessage());
    } catch (final IllegalArgumentException e) {
      mWrapped.close();
      throw new NoTalkbackSlimException(e, ErrorType.SAM_BAD_FORMAT, path, e.getMessage());
    } catch (final RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("No M operator between pair of IDN operators in CIGAR")) {
        maybeWarn(e);
      } else {
        mWrapped.close();
        throw e;
      }
    }
  }

  /**
   * Constructor
   * @param path filename to use for errors and warnings
   * @param reader supplier of <code>SAMRecords</code>
   * @throws IOException if an IO error occurs
   */
  public SkipInvalidRecordsIterator(String path, RecordIterator<SAMRecord> reader) throws IOException {
    this(path, reader, false);
  }

  /**
   * Constructor
   * @param path filename to use for errors and warnings
   * @param reader to obtain base iterator from
   * @param silent true to not report warnings
   * @throws IOException if an IO error occurs
   */
  public SkipInvalidRecordsIterator(String path, final SamReader reader, boolean silent) throws IOException {
    this(path, new SamFileReaderAdaptor(reader, null), silent);
  }

  /**
   * Constructor
   * @param path filename to use for errors and warnings
   * @param reader to obtain base iterator from
   * @throws IOException if an IO error occurs
   */
  public SkipInvalidRecordsIterator(String path, SamReader reader) throws IOException {
    this(path, new SamFileReaderAdaptor(reader, null));
  }

  /**
   * Constructs an iterator on given file.
   * @param samFile file containing SAM data
   * @throws IOException if an IO Error occurs
   */
  public SkipInvalidRecordsIterator(File samFile) throws IOException {
    this(samFile.getPath(), SamUtils.makeSamReader(samFile));
  }

  private void maybeWarn(Throwable t) {
    mNumInvalidRecords++;
    if (!mSilent && mNumInvalidRecords <= NUM_RECORDS_TO_WARN) {
      Diagnostic.warning(WarningType.SAM_BAD_FORMAT_WARNING, mPath, LS + t.getMessage() + " at data line " + mRecordCount);
    }
  }

  private void nextRecord() {
    while (mWrapped.hasNext()) {
      mRecordCount++;
      try {
        final SAMRecord current = mWrapped.next();
        if (current != null) {
          if (current.getIsValid()) {
            mTotalNucleotides += current.getReadLength();
            mRecord = current;
            return;
          } else {
            // invalid records but we continue
            mNumInvalidRecords++;
            if (!mSilent && mNumInvalidRecords <= NUM_RECORDS_TO_WARN) {

              final List<SAMValidationError> valid = current.isValid();
              if (valid == null) {
                Diagnostic.warning(WarningType.SAM_BAD_FORMAT_WARNING, mPath, LS + "At data line " + mRecordCount);
              } else {
                Diagnostic.warning(WarningType.SAM_BAD_FORMAT_WARNING, mPath, ""
                    + LS + current.getSAMString().trim()
                    + LS + valid
                    + LS + "At data line " + mRecordCount
                    );
              }
            }
          }
        }
      } catch (final SAMFormatException e) {
        throw new NoTalkbackSlimException(e, ErrorType.SAM_BAD_FORMAT, mPath, e.getMessage());
      } catch (final IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("Malformed CIGAR string:")) {
          maybeWarn(e);
        } else {
          throw new NoTalkbackSlimException(e, ErrorType.SAM_BAD_FORMAT, mPath, e.getMessage());
        }
      } catch (final RuntimeException e) {
        if (e.getMessage() != null && e.getMessage().contains("No M operator between pair of IDN operators in CIGAR")) {
          maybeWarn(e);
        } else {
          throw e;
        }
      }
    }
    mRecord = null;
  }


  @Override
  public boolean hasNext() {
    return mRecord != null;
  }

  @Override
  public SAMRecord next() {
    if (mRecord == null) {
      throw new NoSuchElementException();
    }
    final SAMRecord ret = mRecord;
    nextRecord();
    return ret;
  }

  @Override
  public void close() throws IOException {
    if (mIsClosed) {
      return;
    }
    if (!mSilent && mNumInvalidRecords > 0) {
      Diagnostic.warning(WarningType.SAM_IGNORED_RECORDS, "" + mNumInvalidRecords, mPath);
    }
    mIsClosed = true;
    mWrapped.close();
  }

  @Override
  public String toString() {
    return "Iterator: line=" + mRecordCount + " file=" + mPath + " record=" + mRecord.getSAMString().trim();
  }

}
