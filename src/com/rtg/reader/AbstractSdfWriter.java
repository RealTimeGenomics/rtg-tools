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

import java.io.IOException;
import java.util.zip.CRC32;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.Protein;
import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;

/**
 *
 */
@TestClass(value = {"com.rtg.reader.SequencesWriterTest"})
public abstract class AbstractSdfWriter implements AutoCloseable {


  static final int MAX_HISTOGRAM = 1000;
  static final int MAX_SCORES = 100;

  /** Maximum quality value allowed in SDF */
  public static final int MAX_QUALITY_VALUE = 64;
  private static final int CLIPPED_QUALITY_VALUE = MAX_QUALITY_VALUE - 1;
  private static final double[] ERROR = new double[MAX_SCORES];
  private static final char[] DISALLOWED_START = {'*', '=', '@'};
  private static final boolean[] ALLOWED_NAME_START;
  private static final boolean[] ALLOWED_NAME_REST;
  private static final int MAX_ALLOW_CHAR = 126;
  private static final int MAX_WARNINGS = 10;
  static {
    for (int k = 0; k < ERROR.length; k++) {
      ERROR[k] = Math.pow(10, -0.1 * k);
    }
    ALLOWED_NAME_START = new boolean[MAX_ALLOW_CHAR + 1];
    ALLOWED_NAME_REST = new boolean[MAX_ALLOW_CHAR + 1];
    for (char c = '!'; c < '~'; c++) {
      ALLOWED_NAME_START[c] = true;
      ALLOWED_NAME_REST[c] = true;
    }
    for (int i : DISALLOWED_START) {
      ALLOWED_NAME_START[i] = false;
    }
  }

  protected final SequenceNameHandler mNameHandler;

  protected final SequenceType mSequenceType;
  private final int mFirstValid;
  protected final boolean mHasNames;

  protected final boolean mCompressed;
  protected final CRC32 mSeqDataChecksum;
  protected final CRC32 mQualDataChecksum;


  //Stats
  private long mTotalLength = 0;
  private long mMaxLength = 0;
  private long mMinLength = Long.MAX_VALUE;
  private final long[] mResidueCounts;
  private final long[] mNHistogram;
  private final long[] mPosHistogram;
  private final double[] mQSAveragePerPos;
  private final long[] mPositionCounts;

  private double mGlobalQSAverage;
  private long mNBlocks;
  private int mCurrentNBlockLength;
  private long mCurrentSeqPosition;
  private long mCurrentNCount;
  private boolean mPrevWasN;
  protected long mNLongestBlock;

  protected SdfId mSdfId;

  private long mNoSequenceData = 0;
  protected final PrereadHashFunction mDataHashFunction;
  protected final PrereadHashFunction mQualityHashFunction;
  protected final PrereadHashFunction mNameHashFunction;
  protected final PrereadHashFunction mNameSuffixHashFunction;
  private final PrereadType mPrereadType;
  private PrereadArm mPrereadArm;

  protected final boolean mHasQuality;

  private String mCommandLine;
  private String mComment;
  private String mReadGroup;


  /**
   * Creates a writer for processing sequences from provided data source.
   * @param type preread type
   * @param hasQuality whether to write quality data
   * @param hasNames whether to write name data
   * @param compressed whether <code>SDF</code> should be compressed
   * @param prereadType type of the data that is being written
   */
  public AbstractSdfWriter(final PrereadType prereadType, final boolean hasQuality, boolean hasNames, boolean compressed, final SequenceType type) {
    mPrereadType = prereadType;
    mPrereadArm = PrereadArm.UNKNOWN;
    mSdfId = new SdfId();
    mNameHandler = new SequenceNameHandler();
    mSequenceType = type;
    mFirstValid = mSequenceType.firstValid();
    mResidueCounts = new long[mSequenceType.numberKnownCodes() + mSequenceType.firstValid()];
    mNHistogram = new long[MAX_HISTOGRAM];
    mPosHistogram = new long[MAX_HISTOGRAM];
    mQSAveragePerPos = new double[MAX_HISTOGRAM];
    mPositionCounts = new long[MAX_HISTOGRAM];
    mGlobalQSAverage = 0.0;
    mDataHashFunction = new PrereadHashFunction();
    mQualityHashFunction = new PrereadHashFunction();
    mNameHashFunction = new PrereadHashFunction();
    mNameSuffixHashFunction =  new PrereadHashFunction();
    mHasQuality = hasQuality;
    mHasNames = hasNames;
    mCompressed = compressed;

    mSeqDataChecksum = new CRC32();
    mQualDataChecksum = new CRC32();
  }

  /**
   * Called before any calls to write for a given sequence
   * @param label name of the sequence
   * @throws IOException if an I/O Error occurs
   */
  public abstract void startSequence(final String label) throws IOException;

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
  public abstract void write(byte[] rs, byte[] qs, int length) throws IOException;

  /**
   * Should be called after all calls to write for a given sequence.
   * @return Whether any data was written for sequence
   * @throws IOException if an I/O error occurs
   */
  public abstract boolean endSequence() throws IOException;

  /**
   * Writes index file and closes streams
   * @throws IOException If an I/O error occurs
   */
  @Override
  public abstract void close() throws IOException;

  /**
   * @return the number Of Sequences
   */
  abstract long getNumberOfSequences();

  protected IndexFile finish(long sizeLimit, long numberSequences) {
      if (mNameHandler.mNoNameCount > 0) {
        Diagnostic.warning(""); //Ugly way to separate the warning.
        Diagnostic.warning("There were " + mNoSequenceData + " sequences with no name.");
      }
      if (mNameHandler.mSequenceLabelTooLong > 0) {
        Diagnostic.warning(""); //Ugly way to separate the warning.
        Diagnostic.warning("There were " + mNoSequenceData + " names too long and truncated.");
      }
      if (mNoSequenceData > 0) {
        Diagnostic.warning(""); //Ugly way to separate the warning.
        Diagnostic.warning("There were " + mNoSequenceData + " sequences with no data.");
      }

    //check accidental protein
    if (mSequenceType == SequenceType.PROTEIN) {
      long actgnTotal = 0;
      actgnTotal += mResidueCounts[Protein.A.ordinal()];
      actgnTotal += mResidueCounts[Protein.C.ordinal()];
      actgnTotal += mResidueCounts[Protein.G.ordinal()];
      actgnTotal += mResidueCounts[Protein.T.ordinal()];
      actgnTotal += mResidueCounts[Protein.N.ordinal()];
      if (actgnTotal / (double) mTotalLength >= 0.99d) {
        Diagnostic.warning(""); //Ugly way to separate the warning.
        Diagnostic.warning(WarningType.POSSIBLY_NOT_PROTEIN, Long.toString(actgnTotal), Long.toString(mTotalLength));
      }
    }


    final IndexFile indexFile = new IndexFile(sizeLimit, mSequenceType.ordinal(),
            mTotalLength, mMaxLength, mMinLength, numberSequences);
    indexFile.setResidueCounts(mResidueCounts);
    indexFile.setHasQuality(mHasQuality);
    indexFile.setHasNames(mHasNames);
    indexFile.setSequenceEncoding(mCompressed ? IndexFile.SEQUENCE_ENCODING_COMPRESSED : IndexFile.SEQUENCE_ENCODING_NORMAL);
    indexFile.setQualityEncoding(mCompressed ? IndexFile.QUALITY_ENCODING_COMPRESSED : IndexFile.QUALITY_ENCODING_NORMAL);
    indexFile.setNHistogram(mNHistogram);
    indexFile.setPosHistogram(mPosHistogram);
    final double globalAverage = mGlobalQSAverage / mTotalLength;
    indexFile.setGlobalQSAverage(globalAverage);
    indexFile.setNBlocksCount(mNBlocks);
    indexFile.setLongestNBlock(mNLongestBlock);
    indexFile.setPrereadArm(mPrereadArm);
    indexFile.setPrereadType(mPrereadType);
    indexFile.setSdfId(mSdfId);
    indexFile.setCommandLine(mCommandLine);
    indexFile.setComment(mComment);
    indexFile.setSamReadGroup(mReadGroup);
    final double[] posAverages = new double[MAX_HISTOGRAM];
    for (int i = 0; i < MAX_HISTOGRAM; i++) {
      if (mPositionCounts[i] > 0) {
        posAverages[i] = mQSAveragePerPos[i] / mPositionCounts[i];
      }
    }
    indexFile.setQSPostionAverageHistogram(posAverages);
    indexFile.setDataChecksum(mDataHashFunction.getHash());
    indexFile.setQualityChecksum(mQualityHashFunction.getHash());
    indexFile.setNameChecksum(mNameHashFunction.getHash());
    indexFile.setNameSuffixChecksum(mNameSuffixHashFunction.getHash());
    return indexFile;
  }

  protected void clipQuality(byte[] qs, int length) {
    for (int i = 0; i < length; i++) {
      if (qs[i] > CLIPPED_QUALITY_VALUE) {
        qs[i] = CLIPPED_QUALITY_VALUE;
      }
    }
  }

  protected Label handleSequenceName(String label0) {
    return mNameHandler.handleSequenceName(label0);
  }

  /**
   * Handle turning raw sequence names into <code>Label</code>s
   */
  public static class SequenceNameHandler {

    static final int MAX_LABEL_LENGTH = 240;

    private long mNoNameCount;
    private long mSequenceLabelTooLong = 0;

    static boolean fixSequenceName(final String name, final StringBuilder newName) {
      boolean ret = true;
      final char[] thename = name.toCharArray();
      for (int i = 0; i < thename.length; i++) {
        final char c = thename[i];
        if (c > MAX_ALLOW_CHAR || (i == 0 && !ALLOWED_NAME_START[c]) || !ALLOWED_NAME_REST[c]) {
          newName.append('X');
          ret = false;
        } else {
          newName.append(c);
        }
      }
      return ret;
    }

    /**
     * Splits name on first whitespace.
     * @param label0 name to split
     * @return A label object
     */
    public Label handleSequenceName(String label0) {
      String label = label0;
      String suffix = "";

      if (label.length() == 0) {
        label = "Unnamed_sequence_" + mNoNameCount;
        if (mNoNameCount < MAX_WARNINGS) {
          Diagnostic.warning(WarningType.NO_NAME, label);
        }
        mNoNameCount++;
        if (mNoNameCount == MAX_WARNINGS) {
          Diagnostic.warning("Subsequent warnings of this type will not be shown.");
        }
      }

      for (int k = 0; k < label.length(); k++) {
        if (Character.isWhitespace(label.charAt(k))) {
          suffix = label.substring(k);
          label = label.substring(0, k);
          break;
        }
      }

      final StringBuilder newName = new StringBuilder();
      if (!fixSequenceName(label, newName)) {
        throw new NoTalkbackSlimException(ErrorType.BAD_CHARS_NAME, label);
      }

      if (label.length() > MAX_LABEL_LENGTH) {
        if (mSequenceLabelTooLong < MAX_WARNINGS) {
          Diagnostic.warning(WarningType.SEQUENCE_LABEL_TOO_LONG, label);
        }
        mSequenceLabelTooLong++;
        if (mSequenceLabelTooLong == MAX_WARNINGS) {
          Diagnostic.warning("Subsequent warnings of this type will not be shown.");
        }
        suffix = label.substring(MAX_LABEL_LENGTH) + suffix;
        label = label.substring(0, MAX_LABEL_LENGTH);
      }
      return new Label(label, suffix);
    }
  }

  /**
   * @param prereadArm the preread arm to set
   */
  public final void setPrereadArm(final PrereadArm prereadArm) {
    mPrereadArm = prereadArm;
  }

  /**
   * @param id set the SDF-ID for this sequence
   */
  public void setSdfId(final SdfId id) {
    mSdfId = id;
  }

  /**
   * @return the SDF-ID for this sequence
   */
  public SdfId getSdfId() {
    return mSdfId;
  }

  /**
   * @param commandLine the command line used to generate this data
   */
  public void setCommandLine(final String commandLine) {
    mCommandLine = commandLine;
  }

  /**
   * @param comment a comment for this SDF file
   */
  public void setComment(final String comment) {
    mComment = comment;
  }

  /**
   * @param readGroup the sam read group to store in the SDF
   */
  public void setReadGroup(final String readGroup) {
    mReadGroup = readGroup;
  }

  /**
   * @return the total Length
   */
  long getTotalLength() {
    return mTotalLength;
  }

  /**
   * @return the maximum length
   */
  long getMaxLength() {
    return mMaxLength;
  }

  /**
   * @return the minimum length
   */
  long getMinLength() {
    return mMinLength;
  }


  protected void noSequenceWarning(Label label) {
    if (mNoSequenceData < MAX_WARNINGS) {
      Diagnostic.warning(WarningType.NO_SEQUENCE, label == null ? null : label.toString());
    }
    mNoSequenceData++;
    if (mNoSequenceData == MAX_WARNINGS) {
      Diagnostic.warning("Subsequent warnings of this type will not be shown.");
    }
  }
  protected void initSequenceStatistics() {
    mCurrentSeqPosition = 0;

    mPrevWasN = false;
    mCurrentNCount = 0;
    mCurrentNBlockLength = 0;
  }

  protected void endSequenceStatistics() {
    if (mPrevWasN && mCurrentNBlockLength > mNLongestBlock) {
      mNLongestBlock = mCurrentNBlockLength;
    }
    //update histogram
    if (mCurrentNCount < MAX_HISTOGRAM) {
      mNHistogram[(int) mCurrentNCount]++;
    } else {
      mNHistogram[MAX_HISTOGRAM - 1]++;
    }
  }

  protected void updateStatistics(final long size) {
    mTotalLength += size;
    if (size > mMaxLength) {
      mMaxLength = size;
    }
    if (size < mMinLength) {
      mMinLength = size;
    }
  }

  protected void updateCountStatistics(byte[] rs, byte[] qs, int length) {
    byte r;
    int q;
    for (int i = 0; i < length; i++) {
      r = rs[i];
      mResidueCounts[r]++;
      //update count for histogram for N's, for protein will keep track of X and STOP
      if (r < mFirstValid) {
        // compute position histogram
        if (mCurrentSeqPosition < MAX_HISTOGRAM) {
          mPosHistogram[(int) mCurrentSeqPosition]++;
        } else {
          mPosHistogram[MAX_HISTOGRAM - 1]++;
        }

        // compute numblocks
        if (!mPrevWasN) {
          mNBlocks++;
        }
        mPrevWasN = true;
        mCurrentNBlockLength++;
        mCurrentNCount++;
      } else {
        if (mCurrentNBlockLength > mNLongestBlock) {
          mNLongestBlock = mCurrentNBlockLength;
        }
        mCurrentNBlockLength = 0;
        mPrevWasN = false;
      }
      //end update histograms
      if (mHasQuality) {
        q = qs[i];
        if (q < 100) {
          mGlobalQSAverage += ERROR[q];
        }
        if (mCurrentSeqPosition < MAX_HISTOGRAM) {
          if (q < 100) {
            mQSAveragePerPos[(int) mCurrentSeqPosition] += ERROR[q];
          }
          mPositionCounts[(int) mCurrentSeqPosition]++;
        }
      }
      mCurrentSeqPosition++;
    }
  }


}
