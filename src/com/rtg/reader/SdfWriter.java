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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.io.FileUtils;

/**
 * Takes a sequence data source, and writes sequences out in the preread data format.
 * Assumes the destination directory is essentially empty and we can do whatever we want in it.
 */
@TestClass(value = {"com.rtg.reader.SequencesWriterTest"})
public class SdfWriter extends AbstractSdfWriter {

  static final long DEFAULT_SIZE_LIMIT = 2L * 1024 * 1024 * 1024; //2GB
  static final long MIN_SIZE_LIMIT = 20;
  static final long MAX_SIZE_LIMIT = 1024L * 1024 * 1024 * 1024; //1TB


  private boolean mHaveStartedWritingSequenceData;
  private final File mOutputDir;
  private long mSizeLimit = DEFAULT_SIZE_LIMIT;
  //extra fields from refactoring

  private int mMaxSuffixLength;

  private Label mCurrentLabel;

  private IndexFile mIndexFile;
  private long mNumberOfSequences = 0;

  private SequenceFilePair mCurrentSeq;
  private NameFilePair mCurrentName;
  private NameFilePair mCurrentNameSuffix;
  private RollingIndex mSeqIndex;
  private RollingIndex mLabelIndex;
  private RollingIndex mLabelSuffixIndex;
  private int mRollSeqNum;
  private int mRollLabelNum;
  private int mRollLabelSuffixNum;

  private long mCurrentLength;

  /**
   * Creates a writer for processing sequences from provided data source.
   * @param outputDir Destination of output files
   * @param sizeLimit Maximum size for output files
   * @param type preread type
   * @param hasQuality whether to write quality data
   * @param hasNames whether to write name data
   * @param compressed whether <code>SDF</code> should be compressed
   * @param prereadType type of the data that is being written
   */
  public SdfWriter(final File outputDir, final long sizeLimit, final PrereadType prereadType, final boolean hasQuality, boolean hasNames, boolean compressed, final SequenceType type) {
    super(prereadType, hasQuality, hasNames, compressed, type);
    mOutputDir = outputDir;
    //System.err.println("names: " + hasNames + " qual: " + hasQuality);
    FileUtils.ensureOutputDirectory(mOutputDir);
    setSizeLimit(sizeLimit);
    mMaxSuffixLength = 0;
    //create index file
    openSeqIndex();
    if (mHasNames) {
      openLabelIndex();
      openLabelSuffixIndex();
    }
  }

  private void setSizeLimit(final long limit) {
    if (limit < MIN_SIZE_LIMIT || limit > MAX_SIZE_LIMIT) {
      throw new IllegalArgumentException("Size limit of: " + limit + " is not within bounds of: " + MIN_SIZE_LIMIT + " and " + MAX_SIZE_LIMIT);
    }
    mSizeLimit = limit;
  }

  /**
   * Gets the root directory of this SDF
   * @return the root directory
   */
  public File directory() {
    return mOutputDir;
  }

  /**
   * Closes off streams for current sequence file pair
   * @throws IOException if an I/O error occurs.
   */
  private void closeCurrentSequenceFilePair() throws IOException {
    if (mCurrentSeq != null) {
      mCurrentSeq.close();
      mSeqIndex.incrementSize(mCurrentSeq.valuesWritten());
      mSeqIndex.writeEntry();
    }
  }

  /**
   * Closes off streams for current label file pair
   * @throws IOException if an I/O error occurs.
   */
  private void closeCurrentLabel() throws IOException {
    if (mCurrentName != null) {
      mCurrentName.close();
      mLabelIndex.incrementSize(mCurrentName.valuesWritten());
      mLabelIndex.writeEntry();
    }
  }

  private void closeCurrentLabelSuffix() throws IOException {
    if (mCurrentNameSuffix != null) {
      mCurrentNameSuffix.close();
      mLabelSuffixIndex.incrementSize(mCurrentNameSuffix.valuesWritten());
      mLabelSuffixIndex.writeEntry();
    }
  }

  private SequenceFilePair seqFilePair(int fileNo, long limit) throws IOException {
    if (mCompressed) {
      return new CompressedSequenceFilePair(mOutputDir, fileNo, mHasQuality, limit, mSequenceType.numberCodes(), mSeqDataChecksum, mQualDataChecksum);
    } else {
      return new NormalSequenceFilePair(mOutputDir, fileNo, mHasQuality, limit, mSeqDataChecksum, mQualDataChecksum);
    }
  }

  /**
   * Closes current files and creates next ones.
   * @throws IOException if an I/O error occurs.
   */
  private void rollSequence() throws IOException {
    closeCurrentSequenceFilePair();
    mCurrentSeq = seqFilePair(mRollSeqNum, mSizeLimit);
    ++mRollSeqNum;
  }

  private void openSeqIndex() {
    mSeqIndex = new RollingIndex(new File(mOutputDir, SdfFileUtils.SEQUENCE_INDEX_FILENAME));
  }
  private void closeSeqIndex() throws IOException {
    mSeqIndex.close();
  }

  private void openLabelIndex() {
    mLabelIndex = new RollingIndex(new File(mOutputDir, SdfFileUtils.LABEL_INDEX_FILENAME));
  }
  private void closeLabelIndex() throws IOException {
    mLabelIndex.close();
  }

  private void openLabelSuffixIndex() {
    mLabelSuffixIndex = new RollingIndex(new File(mOutputDir, SdfFileUtils.LABEL_SUFFIX_INDEX_FILENAME));
  }
  private void closeLabelSuffixIndex() throws IOException {
    mLabelSuffixIndex.close();
  }



  private void rollLabel() throws IOException {
    closeCurrentLabel();
    mCurrentName = new NameFilePair(new File(mOutputDir, SdfFileUtils.LABEL_DATA_FILENAME + mRollLabelNum),
        new File(mOutputDir, SdfFileUtils.LABEL_POINTER_FILENAME + mRollLabelNum),
        mSizeLimit);
    ++mRollLabelNum;
  }

  private void rollLabelSuffix() throws IOException {
    closeCurrentLabelSuffix();
    mCurrentNameSuffix = new NameFilePair(new File(mOutputDir, SdfFileUtils.LABEL_SUFFIX_DATA_FILENAME + mRollLabelSuffixNum),
        new File(mOutputDir, SdfFileUtils.LABEL_SUFFIX_POINTER_FILENAME + mRollLabelSuffixNum),
        mSizeLimit);
    ++mRollLabelSuffixNum;

  }

  /**
   * Called before any calls to write for a given sequence
   * @param label name of the sequence
   * @throws IOException if an I/O Error occurs
   */
  @Override
  public void startSequence(final String label) throws IOException {
    mHaveStartedWritingSequenceData = false;
    mCurrentLength = 0;
    initSequenceStatistics();
    if (mHasNames) {
      if (mCurrentName == null) {
        rollLabel();
      }
      if (mCurrentNameSuffix == null) {
        rollLabelSuffix();
      }
      mCurrentLabel = handleSequenceName(label);
    }
    //initialisation required for data integrity
    if (mCurrentSeq == null) {
      rollSequence();
    }
  }

  /**
   * Write multiple residues
   *
   * WARNING: any quality value in the array <code>qs</code> greater than or equal to {@link SdfWriter#MAX_QUALITY_VALUE} will be modified in place to
   * <code>MAX_QUALITY_VALUE - 1</code>
   * @param rs residues bytes to write
   * @param qs qualities to write (may be null)
   * @param length length of arrays to write
   * @throws IOException is an IO exception occurs
   */
  @Override
  public void write(byte[] rs, byte[] qs, int length) throws IOException {
    if (!mHaveStartedWritingSequenceData) {
      mHaveStartedWritingSequenceData = true;
      if (!mCurrentSeq.markNextSequence()) {
        rollSequence();
        if (!mCurrentSeq.markNextSequence()) {
          throw new SlimException(ErrorType.SLIM_ERROR);
        }
      }
      mSeqIndex.incrementCount();
    }

    if (mHasQuality) {
      clipQuality(qs, length);
    }

    int bytesWritten = 0;
    int bytesToWrite;
    while (bytesWritten < length) {
      if (mCurrentSeq.bytesFree() == 0) {
        rollSequence();
      }
      bytesToWrite = (int) Math.min(mCurrentSeq.bytesFree(), length - bytesWritten);

      if (!mCurrentSeq.write(rs, bytesWritten, bytesToWrite)) {
        throw new IllegalStateException("Too much data passed to write");
      }

      if (mHasQuality) {
        mCurrentSeq.writeQuality(qs, bytesWritten, bytesToWrite);
      }

      bytesWritten += bytesToWrite;
    }
    assert bytesWritten == length : "Not all bytes written";
    updateCountStatistics(rs, qs, length);
    mDataHashFunction.irvineHash(rs, length);
    if (mHasQuality) {
      mQualityHashFunction.irvineHash(qs, length);
    }
    mCurrentLength += length;
    if (mCurrentLength < 1) {
      noSequenceWarning(mCurrentLabel);
    }
  }

  /**
   * Should be called after all calls to write for a given sequence.
   * @return Whether any data was written for sequence
   * @throws IOException if an I/O error occurs
   */
  @Override
  public boolean endSequence() throws IOException {
    endSequenceStatistics();
    final boolean result;
    if (mHaveStartedWritingSequenceData) {
      if (mHasNames) {
        writeSequenceLabel(mCurrentLabel);
      }
      updateStatistics(mCurrentLength);
      mDataHashFunction.irvineHash(mCurrentLength);
      if (mHasQuality) {
        mQualityHashFunction.irvineHash(mCurrentLength);
      }
      result = true;
    } else {
      result = false;
    }
    if (result) {
      ++mNumberOfSequences;
    }
    return result;
  }

  private void writeSequenceLabel(final Label label0) throws IOException {
    String label = label0.label(); //handleSequenceName(label0);
    if (mCurrentName.canWriteName(label.length())) {
      mLabelIndex.incrementCount();
      mCurrentName.writeName(label);
    } else {
      rollLabel();
      mLabelIndex.incrementCount();
      if (mCurrentName.canWriteName(label.length())) {
        mCurrentName.writeName(label);
      } else {
        //truncate, this implies that max_file_size < MAX_LABEL_LENGTH
        label = mCurrentName.forceWriteName(label);
      }
    }

    String suffix = label0.suffix();
    if (suffix.length() > mMaxSuffixLength) {
      mMaxSuffixLength = suffix.length();
    }
    if (mCurrentNameSuffix.canWriteName(suffix.length())) {
      mLabelSuffixIndex.incrementCount();
      mCurrentNameSuffix.writeName(suffix);
    } else {
      rollLabelSuffix();
      mLabelSuffixIndex.incrementCount();
      if (mCurrentNameSuffix.canWriteName(suffix.length())) {
        mCurrentNameSuffix.writeName(suffix);
      } else {
        //truncate, this implies that max_file_size < MAX_LABEL_LENGTH
        suffix = mCurrentNameSuffix.forceWriteName(suffix);
      }
    }
    mNameHashFunction.irvineHash(label);
    mNameHashFunction.irvineHash(label.length());
    mNameSuffixHashFunction.irvineHash(suffix);
    mNameSuffixHashFunction.irvineHash(suffix.length());
  }

  /**
   * Writes index file and closes streams
   * @throws IOException If an I/O error occurs
   */
  @Override
  public void close() throws IOException {
    try {
      if (mCurrentSeq != null) {
        mCurrentSeq.lastSequence();
      }

    } finally {
      try {
        closeCurrentSequenceFilePair();
      } finally {
        closeSeqIndex();
        try {
        } finally {
          if (mHasNames) {
            try {
              closeCurrentLabel();
            } finally {
              try {
                closeLabelIndex();
              } finally {
                try {
                  closeCurrentLabelSuffix();
                } finally {
                  closeLabelSuffixIndex();
                }
              }
            }
          }
        }
      }
    }
    final boolean hasSuffixes = mMaxSuffixLength != 0;
    mIndexFile = super.finish(mSizeLimit, mNumberOfSequences);
    mIndexFile.setHasSuffixes(hasSuffixes);
    mIndexFile.save(mOutputDir);
    if (!hasSuffixes) {
      //done here so we have access to mIndexFile.dataIndexVersion()
      removeSuffixFiles();
    }
    Diagnostic.userLog("Writing SDF-Id: " + mSdfId);
  }

  private void removeSuffixFiles() throws IOException {
    final DataFileIndex dfi = DataFileIndex.loadLabelSuffixDataFileIndex(mIndexFile.dataIndexVersion(), mOutputDir);
    for (int i = 0; i < dfi.numberEntries(); ++i) {
      final File data = SdfFileUtils.labelSuffixDataFile(mOutputDir, i);
      deleteRedundantFile(data);
      final File pointer = SdfFileUtils.labelSuffixPointerFile(mOutputDir, i);
      deleteRedundantFile(pointer);
    }
    final File index = SdfFileUtils.labelSuffixIndexFile(mOutputDir);
    deleteRedundantFile(index);
  }

  void deleteRedundantFile(final File data) {
    if (data.exists() && !data.delete()) {
      throw new NoTalkbackSlimException("Failed to delete unneeded file: " + data.getPath());
    }
  }

  @Override
  /**
   * @return the number Of Sequences
   */
  long getNumberOfSequences() {
    return mNumberOfSequences;
  }
}

