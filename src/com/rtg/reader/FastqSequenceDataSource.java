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

import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;


/**
 * FASTQ sequence reader. Reads an entire sequence at a time into a byte array.
 */
public class FastqSequenceDataSource extends FastaSequenceDataSource {

  private static final int MAX_SEQUENCE_LABEL_MISMATCH_WARNINGS = 10;

  private byte[] mQualityBuffer;
  private int mQualityBufferPosition;

  private boolean mQualityWarned;

  private final byte[] mQualityTable = new byte[FastaUtils.PHRED_UPPER_LIMIT_CHAR - FastaUtils.PHRED_LOWER_LIMIT_CHAR + 1];

  private int mSequenceCount = 0;

  private int mSequenceLabelMistmatchCount = 0;

  private final FastQScoreType mScoreType;

  /** Enumeration of score type. */
  public enum FastQScoreType {
    /** phred */
    PHRED,
    /** Solexa */
    SOLEXA,
    /** Solexa 1.3 */
    SOLEXA1_3
  }

  private void initQualityTable(final FastQScoreType scoreType) {
    if (scoreType.equals(FastQScoreType.SOLEXA)) {
      for (char c = (char) FastaUtils.PHRED_LOWER_LIMIT_CHAR; c <= FastaUtils.PHRED_UPPER_LIMIT_CHAR; c++) {
        mQualityTable[c - FastaUtils.PHRED_LOWER_LIMIT_CHAR] = (byte) (10 * Math.log10(1 + Math.pow(10, (c - 64) / 10.0)) + 0.5);
      }
    } else {
      for (int i = 0; i < mQualityTable.length; i++) {
        mQualityTable[i] = (byte) i;
      }
    }
  }

  /**
   * Read FASTQ sequences from given InputStreams. This constructor assumes all the
   * input streams are open.
   * @param iss list of InputStreams
   * @param scoreType what type of quality score is represented
   */
  public FastqSequenceDataSource(List<InputStream> iss, final FastQScoreType scoreType) {
    super(iss, new DNAFastaSymbolTable());
    mScoreType = scoreType;
    if (scoreType != null) {
      initQualityTable(scoreType);
    }
  }

  /**
   * Read FASTA sequences from given stream. Assumes that the opening and closing of
   * files is handled externally.  That is, when <code>next()</code> is called on
   * the iterator it is assumed that the returned stream open, but that the status
   * of all previous streams is undefined.
   * @param files List of files
   * @param scoreType what type of quality score is represented
   * @param file hack to overload constructor, should always be true
   * @param arm the arm that this source belongs to
   */
  public FastqSequenceDataSource(List<File> files, final FastQScoreType scoreType, boolean file, PrereadArm arm) {
    super(files, new DNAFastaSymbolTable(), file, arm);
    mScoreType = scoreType;
    if (scoreType != null) {
      initQualityTable(scoreType);
    }
  }

  /**
   * Used to loop through sources with no sequences
   * @return true if a sequence with a label is found
   * @throws IOException if an io exception occurs
   */
  private boolean searchSequenceLabel() throws IOException {
    if (!seekAndReadNextLabel(false)) {
      if (!nextSource() || !searchSequenceLabel()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean nextSequence() throws IOException {
    if (mSource == null || mSourceClosed) {
      if (!nextSource()) {
        return false;
      }
    }

    mCurrentSequenceName = null;

    if (!searchSequenceLabel()) {
      return false;
    }

    mBufferPosition = 0;
    mQualityBufferPosition = 0;
    //we're after the \n, so now is nucleotides
    readData('+');

    //now at quality line, skip label
    final String sequenceLabel = name();
    if (!seekAndReadNextLabel(true)) {
      throw new NoTalkbackSlimException(ErrorType.NO_QUALITY_LABEL, name());
    }
    if (name().length() > 0 && !name().equals(sequenceLabel)) {  //check both labels equal if there is a quality label
      if (mSequenceLabelMistmatchCount < MAX_SEQUENCE_LABEL_MISMATCH_WARNINGS) {
        Diagnostic.warning(WarningType.SEQUENCE_LABEL_MISMATCH, sequenceLabel, name());
      }
      mSequenceLabelMistmatchCount++;
      if (mSequenceLabelMistmatchCount == MAX_SEQUENCE_LABEL_MISMATCH_WARNINGS) {
        Diagnostic.warning("Subsequent warnings of this type will not be shown.");
      }
    } else {
      mCurrentSequenceName = sequenceLabel;     //reset name back to the other one
    }

    //read quality data
    readQualityData();

    return true;
  }

  private boolean processNewLine() throws IOException {
    readInputBuffer();
    while (mInputBufferLength > 0 && mInputBufferPosition < mInputBufferLength) {
      final char ch = (char) mInputBuffer[mInputBufferPosition];
      if (!Character.isWhitespace(ch)) {
        return false;
      } else if (ch == '\r' || ch == '\n') {
        return true;
      }
      mInputBufferPosition++;
      readInputBuffer();
    }
    return true;  //end of file is as good as a newline
  }

  /**
   * Will continue iterating through input buffer values until the previous
   * character is the FASTA label indicator (&gt;).
   * @return true If previous character is &gt;, false if not found in current source
   * @throws IOException if an I/O error occurs.
   */
  private boolean seekAndReadNextLabel(boolean qualityLabel) throws IOException {
    readInputBuffer();
    final char labelChar = qualityLabel ? '+' : '@';
    while (mInputBufferLength > 0 &&  mInputBufferPosition < mInputBufferLength) {
      final byte b = mInputBuffer[mInputBufferPosition];
      if (b == labelChar) {
        mInputBufferPosition++; //move one past the label pos
        mSequenceCount++;
        return readLabel();
      } else {
        if (b == '>') {
          throw new NoTalkbackSlimException(ErrorType.FASTA);
        } else if (!Character.isWhitespace((char) b)) {
          throw new NoTalkbackSlimException(ErrorType.BAD_FASTA_LABEL, name() != null ? name() : "<none>");
        }
      }
      mInputBufferPosition++;
      readInputBuffer();
    }
    if (mSequenceCount == 0) {
      final String filename = mSourceIt instanceof FileStreamIterator ? ((FileStreamIterator) mSourceIt).currentFile().getPath() : "<Not known>";
      Diagnostic.warning(WarningType.NO_SEQUENCE, filename);
    }
    return false;
  }

  private boolean isQualityCharacter(final byte c) {
    return c >= FastaUtils.PHRED_LOWER_LIMIT_CHAR && c <= FastaUtils.PHRED_UPPER_LIMIT_CHAR;
  }

  private void ensureQualityBuffer() {
    if (mQualityBuffer == null) {
      mQualityBuffer = new byte[mInputBufferLength * 10];
      mQualityBufferPosition = 0;
    } else {
      mQualityBuffer = Arrays.copyOf(mQualityBuffer, (int) Math.min(mQualityBuffer.length * 1.62, Integer.MAX_VALUE));
    }
  }

  private void readQualityData() throws IOException {
    readInputBuffer();
    if (mInputBufferLength == -1) {
      return;
    }
    if (mQualityBuffer == null) {
      ensureQualityBuffer();
    }
    byte inputByte;
    byte qualityValue;
    while (mInputBufferPosition < mInputBufferLength && mQualityBufferPosition < currentLength()) {
      inputByte = mInputBuffer[mInputBufferPosition];

      if (isQualityCharacter(inputByte)) {
        if (!mScoreType.equals(FastQScoreType.SOLEXA) && !mScoreType.equals(FastQScoreType.SOLEXA1_3) && inputByte > 90 && !mQualityWarned) {
          Diagnostic.warning(WarningType.POSSIBLY_SOLEXA);
          mQualityWarned = true;
        }
        if (mScoreType.equals(FastQScoreType.SOLEXA1_3)) {
          qualityValue = (byte) (inputByte - '@');
        } else {
          qualityValue = (byte) (inputByte - FastaUtils.PHRED_LOWER_LIMIT_CHAR);
        }
        if (qualityValue < 0 || qualityValue >= mQualityTable.length || (mScoreType.equals(FastQScoreType.SOLEXA) && inputByte < (byte) 59)) {
          throw new NoTalkbackSlimException(ErrorType.INVALID_QUALITY);
        }
        if (mQualityBufferPosition == mQualityBuffer.length) {
          ensureQualityBuffer();
        }
        mQualityBuffer[mQualityBufferPosition] = mQualityTable[qualityValue];
        mQualityBufferPosition++;
      }
      mInputBufferPosition++;
      readInputBuffer();
    }
    if (mQualityBufferPosition != mBufferPosition) {
      throw new NoTalkbackSlimException(ErrorType.NOT_ENOUGH_QUALITY, mCurrentSequenceName);
    } else if (mQualityBufferPosition != 0 && !processNewLine()) {
      throw new NoTalkbackSlimException(ErrorType.BAD_FASTQ_QUALITY, name());
    }
  }

  @Override
  public boolean hasQualityData() {
    return true;
  }

  @Override
  public byte[] qualityData() throws IllegalStateException, IOException {
    return mQualityBuffer;
  }

}
