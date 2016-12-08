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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.rtg.mode.Protein;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;
import com.rtg.util.io.FileUtils;

/**
 * Handles reading and writing the main index file for preread
 *
 */
public class IndexFile implements Integrity {
  static final long VERSION = 13L;

  /** The version at which we started to calculate a global checksum */
  public static final long SINGLE_CHECKSUM_VERSION = 2L;

  //version in which quality data was added
  private static final long QUALITY_VERSION = 3L;

  //version in which histogram was added
  static final long N_HISTOGRAM_AND_SDF_TYPE_VERSION = 4L;

  private static final long DOUBLE_HASH_FIX_VERSION = 5L;
  private static final long PARAMETER_STORING_VERSION = 6L;

  /** The version at which we switched from global checksum to separate checksums for data/qualities/names */
  public static final long SEPARATE_CHECKSUM_VERSION = 7L;

  private static final long WORKING_DIR_AND_ENCODINGS_VERSION = 8L;

  private static final long DATASIZE_IN_DATA_INDICES_VERSION = 9L;

  /** The version we switched SDF-ID from a random long to a UUID */
  private static final long UUID_VERSION = 10L;

  /** The version we added the per sequence checksum files */
  static final long PER_SEQUENCE_CHECKSUM_VERSION = 11L;

  /** the version we added full sequence name storage */
  static final long FULL_SEQUENCE_NAME_VERSION = 12L;

  /** the version we added sam read group */
  static final long SAM_READ_GROUP_VERSION = 13L;

  /** Normal name encoding constant */
  public static final byte NAME_ENCODING_NORMAL = 0;
  /** Normal sequence encoding constant */
  public static final byte SEQUENCE_ENCODING_NORMAL = 0;
  /** Normal quality encoding constant */
  public static final byte QUALITY_ENCODING_NORMAL = 0;

  /** Compressed sequence encoding constant, using BitwiseByteArray */
  public static final byte SEQUENCE_ENCODING_COMPRESSED = 1;
  /** Compressed quality encoding constant, using CompressedByteArray */
  public static final byte QUALITY_ENCODING_COMPRESSED = 1;

  private long mSeqIndexVersion;

  private long mMaxFilesize;
  private int mSequenceType;

  private long mTotalLength;
  private long mMaxLength;
  private long mMinLength;
  private long mNumberSequences;
  private long mVersion;
  private long[] mResidueCount;
  private long[] mNHistogram;
  private long[] mPosHistogram;

  private double mGlobalQSAverage;
  private double[] mPositionAverageHistogram;

  private long mLongestNBlock;
  private long mNBlocksCount;
  private long mSdfIdLowBits; //only to be used in constructor for loading

  private long mDataChecksum;
  private long mQualityChecksum;
  private long mNameChecksum;
  private long mNameSuffixChecksum;
  private boolean mHasQuality;
  private PrereadType mPrereadType;
  private PrereadArm mPrereadArm;
  private String mCommandLine;
  private String mComment;
  private String mSamReadGroup = null;

  private byte mNameEncoding;
  private byte mSequenceEncoding;
  private byte mQualityEncoding;

  private static final int NUMBER_SEQUENCE_ENCODINGS = 1;
  private static final int NUMBER_QUALITY_ENCODINGS = 1;
  private boolean mHasNames = true;
  private boolean mHasSuffixes;

  //private UUID mSdfUuid;
  private SdfId mSdfId;

  /**
   * Constructs object, reading in all values from index
   * @param dir Directory containing Preread index
   * @throws IOException If an I/O error occurs
   */
  public IndexFile(final File dir) throws IOException {
    mPrereadArm = PrereadArm.UNKNOWN;
    mPrereadType = PrereadType.UNKNOWN;
    mSdfId = new SdfId(0);
    final File index = new File(dir, SdfFileUtils.INDEX_FILENAME);
    try (DataInputStream indexStream = new DataInputStream(new BufferedInputStream(new FileInputStream(index), FileUtils.BUFFERED_STREAM_SIZE))) {
      final PrereadHashFunction headerHash = new PrereadHashFunction();
      version1Load(indexStream, headerHash, dir);

      if (mVersion > VERSION) {
        throw new NoTalkbackSlimException("The SDF " + dir.toString() + " has been created with a newer version of RTG tools and cannot be read with this version.");
      }

      loadVersion3Fields(indexStream, headerHash);

      loadVersion4Fields(indexStream, headerHash, dir);

      loadVersion6Fields(indexStream, headerHash);

      loadVersion8Fields(indexStream, headerHash);

      loadVersion9Fields();

      loadVersion10Fields(indexStream, headerHash);

      loadVersion12Fields(indexStream, headerHash);

      loadVersion13Fields(indexStream, headerHash);

      checksumLoad(indexStream, headerHash, dir);
    } catch (final EOFException e) {
      throw new CorruptSdfException(dir);
    }

  }

  private void checksumLoad(DataInputStream indexStream, PrereadHashFunction headerHash, File dir) throws IOException {
    if (mVersion >= SINGLE_CHECKSUM_VERSION) {
      mDataChecksum = indexStream.readLong();
      headerHash.irvineHash(mDataChecksum);

      if (mVersion >= SEPARATE_CHECKSUM_VERSION) {
        mQualityChecksum = indexStream.readLong();
        headerHash.irvineHash(mQualityChecksum);
        mNameChecksum = indexStream.readLong();
        headerHash.irvineHash(mNameChecksum);
      }
      if (mVersion >= FULL_SEQUENCE_NAME_VERSION) {
        mNameSuffixChecksum = indexStream.readLong();
        headerHash.irvineHash(mNameSuffixChecksum);
      }

      final long original = indexStream.readLong();
      if (headerHash.getHash() != original) {
        throw new CorruptSdfException(dir);
      }
    }
  }

  //Initial version fields
  private void version1Load(DataInputStream indexStream, PrereadHashFunction headerHash, File dir) throws IOException {
    mVersion = indexStream.readLong();
    headerHash.irvineHash(mVersion);
    mMaxFilesize = indexStream.readLong();
    headerHash.irvineHash(mMaxFilesize);
    mSequenceType = indexStream.readInt();
    headerHash.irvineHash((long) mSequenceType);
    mMaxLength = indexStream.readLong();
    headerHash.irvineHash(mMaxLength);
    mMinLength = indexStream.readLong();
    headerHash.irvineHash(mMinLength);
    mTotalLength = indexStream.readLong();
    headerHash.irvineHash(mTotalLength);
    mNumberSequences = indexStream.readLong();
    headerHash.irvineHash(mNumberSequences);
    final int totalResidue = indexStream.readInt();
    headerHash.irvineHash((long) totalResidue);
    // Safety check to prevent possible OOM error on next allocation
    if (totalResidue > Protein.values().length || totalResidue < 0) {
      throw new CorruptSdfException(dir);
    }
    mResidueCount = new long[totalResidue];
    long count = 0;
    for (int i = 0; i < totalResidue; ++i) {
      mResidueCount[i] = indexStream.readLong();
      headerHash.irvineHash(mResidueCount[i]);
      count += mResidueCount[i];
    }
    if (count != mTotalLength) {
      throw new CorruptSdfException(dir);
    }
  }

  //quality data fields
  private void loadVersion3Fields(DataInputStream indexStream, PrereadHashFunction headerHash) throws IOException {
    if (mVersion >= QUALITY_VERSION) {
      final int qualityByte = indexStream.read();
      headerHash.irvineHash(qualityByte);
      mHasQuality = qualityByte != 0;
    } else {
      mHasQuality = false;
    }
  }

  //type and histogram fields/data
  private void loadVersion4Fields(DataInputStream indexStream, PrereadHashFunction headerHash, File dir) throws IOException {
    if (mVersion >= N_HISTOGRAM_AND_SDF_TYPE_VERSION) {
      mNBlocksCount = indexStream.readLong();
      headerHash.irvineHash(mNBlocksCount);
      mLongestNBlock = indexStream.readLong();
      headerHash.irvineHash(mLongestNBlock);
      //read N histogram
      mNHistogram = new long[SdfWriter.MAX_HISTOGRAM];
      for (int i = 0; i < mNHistogram.length; ++i) {
        mNHistogram[i] = indexStream.readLong();
        headerHash.irvineHash(mNHistogram[i]);
      }
      //read Pos histogram
      mPosHistogram = new long[SdfWriter.MAX_HISTOGRAM];
      for (int i = 0; i < mPosHistogram.length; ++i) {
        mPosHistogram[i] = indexStream.readLong();
        headerHash.irvineHash(mPosHistogram[i]);
      }
      //read QS average histogram
      mGlobalQSAverage = indexStream.readDouble();
      compatDoubleHash(headerHash, mVersion, mGlobalQSAverage);
      //headerHash.irvineHash(String.valueOf(mGlobalQSAverage));

      mPositionAverageHistogram = new double[SdfWriter.MAX_HISTOGRAM];
      for (int i = 0; i < mPositionAverageHistogram.length; ++i) {
        mPositionAverageHistogram[i] = indexStream.readDouble();
        compatDoubleHash(headerHash, mVersion, mPositionAverageHistogram[i]);
        //headerHash.irvineHash(String.valueOf(mPositionAverageHistogram[i]));
      }

      //read sdf type
      final int prereadArm = indexStream.readInt();
      if (prereadArm > PrereadArm.values().length || prereadArm < 0) {
        throw new CorruptSdfException(dir);
      }
      mPrereadArm = PrereadArm.values()[prereadArm];
      headerHash.irvineHash((long) mPrereadArm.ordinal());
      final int prereadType = indexStream.readInt();
      if (prereadType > PrereadType.values().length || prereadType < 0) {
        throw new CorruptSdfException(dir);
      }
      mPrereadType = PrereadType.values()[prereadType];
      headerHash.irvineHash((long) mPrereadType.ordinal());

      mSdfIdLowBits = indexStream.readLong();
      headerHash.irvineHash(mSdfIdLowBits);
    }
  }

  //command line field
  private void loadVersion6Fields(DataInputStream indexStream, PrereadHashFunction headerHash) throws IOException {
    if (mVersion >= PARAMETER_STORING_VERSION) {
      mCommandLine = readText(indexStream, headerHash);
      mComment = readText(indexStream, headerHash);
    } else {
      mCommandLine = null;
      mComment = null;
    }
  }

  private void loadVersion8Fields(DataInputStream indexStream, PrereadHashFunction headerHash) throws IOException {
    if (mVersion >= WORKING_DIR_AND_ENCODINGS_VERSION) {
      readText(indexStream, headerHash);
      headerHash.irvineHash(indexStream.readLong());
      mNameEncoding = (byte) indexStream.read();
      headerHash.irvineHash((int) mNameEncoding);
      mSequenceEncoding = (byte) indexStream.read();
      headerHash.irvineHash((int) mSequenceEncoding);
      mQualityEncoding = (byte) indexStream.read();
      headerHash.irvineHash((int) mQualityEncoding);
      final int nameByte = indexStream.read();
      headerHash.irvineHash(nameByte);
      mHasNames = nameByte != 0;
    } else {
      mNameEncoding = NAME_ENCODING_NORMAL;
      mSequenceEncoding = SEQUENCE_ENCODING_NORMAL;
      mQualityEncoding = QUALITY_ENCODING_NORMAL;
      mHasNames = true;
    }
  }

  private void loadVersion9Fields() {
    if (mVersion >= DATASIZE_IN_DATA_INDICES_VERSION) {
      mSeqIndexVersion = DataFileIndex.DATASIZE_VERSION;
    } else {
      mSeqIndexVersion = DataFileIndex.SEQ_COUNT_ONLY_VERSION;
    }
  }

  private void loadVersion10Fields(DataInputStream indexStream, PrereadHashFunction headerHash) throws IOException {
    if (mVersion >= UUID_VERSION) {
      final long sdfIdHighBits = indexStream.readLong();
      headerHash.irvineHash(sdfIdHighBits);
      mSdfId = new SdfId(new UUID(sdfIdHighBits, mSdfIdLowBits));
    } else {
      mSdfId = new SdfId(mSdfIdLowBits);
    }
  }

  private void loadVersion12Fields(DataInputStream indexStream, PrereadHashFunction headerHash) throws IOException {
    if (mVersion >= FULL_SEQUENCE_NAME_VERSION) {
      final int hasSuffixesByte = indexStream.read();
      headerHash.irvineHash(hasSuffixesByte);
      mHasSuffixes = hasSuffixesByte != 0;
    } else {
      mHasSuffixes = false;
    }
  }

  private void loadVersion13Fields(DataInputStream indexStream, PrereadHashFunction headerHash) throws IOException {
    if (mVersion >= SAM_READ_GROUP_VERSION) {
      mSamReadGroup = readText(indexStream, headerHash);
    } else {
      mSamReadGroup = null;
    }
  }

  private static String readText(final DataInputStream inputStream, final PrereadHashFunction headerHash) throws IOException {
    final int len = inputStream.readInt();
    headerHash.irvineHash(len);
    if (len == 0) {
      return "";
    } else if (len < 0) {
      throw new CorruptSdfException();
    } else {
      final byte[] bs = new byte[len];
      inputStream.readFully(bs);
      final String s = new String(bs);
      headerHash.irvineHash(s);
      return s;
    }
  }

  /**
   * Constructs object from parameters
   * @param maxFilesize value
   * @param sequenceType value
   * @param totalLength value
   * @param maxLength value
   * @param minLength value
   * @param numberSequences value
   */
  public IndexFile(final long maxFilesize, final int sequenceType, final long totalLength, final long maxLength, final long minLength, final long numberSequences) {
    mMaxFilesize = maxFilesize;
    mSequenceType = sequenceType;
    mTotalLength = totalLength;
    mMaxLength = maxLength;
    mMinLength = minLength;
    mNumberSequences = numberSequences;
    mPrereadArm = PrereadArm.UNKNOWN;
    mPrereadType = PrereadType.UNKNOWN;
    mSdfIdLowBits = 0;
    mSdfId = new SdfId(0);
    mSeqIndexVersion = DataFileIndex.DATASIZE_VERSION;
    mVersion = VERSION;
  }

  /**
   * @return maximum allowed file size
   */
  public long getMaxFileSize() {
    return mMaxFilesize;
  }
  /**
   * @return ordinal of sequence type
   */
  public int getSequenceType() {
    return mSequenceType;
  }
  /**
   * @return total length of all sequences
   */
  public long getTotalLength() {
    return mTotalLength;
  }
  /**
   * @return highest length among sequences
   */
  public long getMaxLength() {
    return mMaxLength;
  }
  /**
   * @return smallest length among sequences
   */
  public long getMinLength() {
    return mMinLength;
  }
  /**
   * @return total number of sequences
   */
  public long getNumberSequences() {
    return mNumberSequences;
  }
  /**
   * @return Returns version of saved data
   */
  public long getVersion() {
    return mVersion;
  }

  /**
   * Sets the residue count array
   * @param countArray array containing residue counts
   */
  void setResidueCounts(final long[] countArray) {
    mResidueCount = countArray;
  }

  /**
   * Sets the N Histogram
   * @param histogram array containing values
   */
  void setNHistogram(final long[] histogram) {
    mNHistogram = histogram;
  }

  /**
   * Sets the Position Histogram (counts Ns per position in read)
   * @param histogram array containing values
   */
  void setPosHistogram(final long[] histogram) {
    mPosHistogram = histogram;
  }

  void setLongestNBlock(final long val) {
    mLongestNBlock = val;
  }

  void setNBlocksCount(final long val) {
    mNBlocksCount = val;
  }

  void setGlobalQSAverage(final double average) {
    mGlobalQSAverage = average;
  }

  void setHasQuality(final boolean has) {
    mHasQuality = has;
  }

  /**
   * Whether the pre-read has quality data
   * @return true if the SDF has quality values
   */
  public boolean hasQuality() {
    return mHasQuality;
  }

  void setHasNames(boolean v) {
    mHasNames = v;
  }

  void setHasSuffixes(boolean v) {
    mHasSuffixes = v;
  }

  /**
   * Whether SDF has names for sequences stored or not
   * @return true if the SDF sequences have names
   */
  public boolean hasNames() {
    return mHasNames;
  }

  void setCommandLine(final String params) {
    mCommandLine = params;
  }
  /**
   * The command line parameters used to generated this SDF
   * @return the command line parameters
   */
  public String getCommandLine() {
    if (mCommandLine == null || mCommandLine.equals("")) {
      return null;
    }
    return mCommandLine;
  }

  void setComment(final String comment) {
    mComment = comment;
  }
  /**
   * A comment for this SDF
   * @return the comment text
   */
  public String getComment() {
    if (mComment == null || mComment.equals("")) {
      return null;
    }
    return mComment;
  }

  void setSamReadGroup(final String samReadGroup) {
    mSamReadGroup = samReadGroup;
  }
  /**
   * A comment for this SDF
   * @return the comment text
   */
  public String getSamReadGroup() {
    if (mSamReadGroup == null || mSamReadGroup.equals("")) {
      return null;
    }
    return mSamReadGroup;
  }

  /**
   * Return the residue counts
   * @return residue counts array
   */
  long[] getResidueCounts() {
    return mResidueCount;
  }
   /**
   * Return the histogram of Ns per sequence
   * @return array containing histogram
   */
  long[] getNHistogram() {
    return mNHistogram;
  }

  /**
   * Return the histogram of Ns per position in sequence
   * @return array containing histogram
   */
  long[] getPosHistogram() {
    return mPosHistogram;
  }

  /**
   * Return the global QS average
   * @return global QS average
   */
  double getQSAverage() {
    return mGlobalQSAverage;
  }

  long getLongestNBlock() {
    return mLongestNBlock;
  }

  long getNBlockCount() {
    return mNBlocksCount;
  }

  /**
   * Tell if we have a N statistics (histogram, blocks and longest block)
   * @return true if N statistics are available
   */
  boolean hasNStats() {
    return mVersion >= N_HISTOGRAM_AND_SDF_TYPE_VERSION;
  }

  /**
   * @param dir Saves index into supplied directory
   * @throws IOException If an I/O error occurs
   */
  public void save(final File dir) throws IOException {
    final File index = new File(dir, SdfFileUtils.INDEX_FILENAME);
    try (DataOutputStream indexStream = new DataOutputStream(new FileOutputStream(index))) {
      if (mResidueCount == null) {
        throw new IllegalStateException("Residue count cannot be null");
      }
      final PrereadHashFunction headerHash = new PrereadHashFunction();
      indexStream.writeLong(VERSION); //8
      headerHash.irvineHash(VERSION);
      indexStream.writeLong(mMaxFilesize); //16
      headerHash.irvineHash(mMaxFilesize);
      indexStream.writeInt(mSequenceType); //20
      headerHash.irvineHash((long) mSequenceType);
      indexStream.writeLong(mMaxLength); //28
      headerHash.irvineHash(mMaxLength);
      indexStream.writeLong(mMinLength); //36
      headerHash.irvineHash(mMinLength);
      indexStream.writeLong(mTotalLength); //44
      headerHash.irvineHash(mTotalLength);
      indexStream.writeLong(mNumberSequences); //52
      headerHash.irvineHash(mNumberSequences);
      indexStream.writeInt(mResidueCount.length); //56
      headerHash.irvineHash((long) mResidueCount.length);
      for (final long count : mResidueCount) {
        indexStream.writeLong(count); //96 for DNA, 232 for Protein
        headerHash.irvineHash(count);
      }
      final int qualityByte = mHasQuality ? 1 : 0;
      indexStream.writeByte(qualityByte); //97/233
      headerHash.irvineHash(qualityByte);

      //write histogram
      indexStream.writeLong(mNBlocksCount);
      headerHash.irvineHash(mNBlocksCount);
      indexStream.writeLong(mLongestNBlock);
      headerHash.irvineHash(mLongestNBlock);
      for (final long h : mNHistogram) {
        indexStream.writeLong(h);
        headerHash.irvineHash(h);
      }
      for (final long h : mPosHistogram) {
        indexStream.writeLong(h);
        headerHash.irvineHash(h);
      }

      indexStream.writeDouble(mGlobalQSAverage);
      compatDoubleHash(headerHash, VERSION, mGlobalQSAverage);
      //headerHash.irvineHash(String.valueOf(mGlobalQSAverage));

      for (final double h : mPositionAverageHistogram) {
        indexStream.writeDouble(h);
        compatDoubleHash(headerHash, VERSION, h);
        //headerHash.irvineHash(String.valueOf(mPositionAverageHistogram[i]));
      }
      //write sdf type
      indexStream.writeInt(mPrereadArm.ordinal());
      headerHash.irvineHash((long) mPrereadArm.ordinal());
      indexStream.writeInt(mPrereadType.ordinal());
      headerHash.irvineHash((long) mPrereadType.ordinal());
      indexStream.writeLong(mSdfId.getLowBits());
      headerHash.irvineHash(mSdfId.getLowBits());

      // V6
      writeText(mCommandLine, indexStream, headerHash);
      writeText(mComment, indexStream, headerHash);

      // V8
      writeText(System.getProperty("user.dir"), indexStream, headerHash);
      final long time = System.currentTimeMillis();
      indexStream.writeLong(time);
      headerHash.irvineHash(time);
      indexStream.writeByte(mNameEncoding);
      headerHash.irvineHash(mNameEncoding);
      indexStream.writeByte(mSequenceEncoding);
      headerHash.irvineHash(mSequenceEncoding);
      indexStream.writeByte(mQualityEncoding);
      headerHash.irvineHash(mQualityEncoding);
      final int nameByte = mHasNames ? 1 : 0;
      indexStream.writeByte(nameByte);
      headerHash.irvineHash(nameByte);

      // V10
      indexStream.writeLong(mSdfId.getHighBits());
      headerHash.irvineHash(mSdfId.getHighBits());

      // V12
      final int suffixByte = mHasSuffixes ? 1 : 0;
      indexStream.writeByte(suffixByte);
      headerHash.irvineHash(suffixByte);

      // V13
      writeText(mSamReadGroup, indexStream, headerHash);

      // Finishing checksums
      indexStream.writeLong(mDataChecksum);
      headerHash.irvineHash(mDataChecksum);
      indexStream.writeLong(mQualityChecksum);
      headerHash.irvineHash(mQualityChecksum);
      indexStream.writeLong(mNameChecksum);
      headerHash.irvineHash(mNameChecksum);
      indexStream.writeLong(mNameSuffixChecksum);
      headerHash.irvineHash(mNameSuffixChecksum);


      indexStream.writeLong(headerHash.getHash());
    }

  }

  private static void writeText(final String text, final DataOutputStream dos, final PrereadHashFunction headerHash) throws IOException {
    if (text == null) {
      dos.writeInt(0);
      headerHash.irvineHash(0);
      return;
    }
    final byte[] bs = text.getBytes();
    dos.writeInt(bs.length);
    headerHash.irvineHash(bs.length);
    if (bs.length > 0) {
      dos.write(bs);
      headerHash.irvineHash(text);
    }
  }

  @Override
  public boolean globalIntegrity() {
    integrity();
    return true;
  }
  @Override
  public boolean integrity() {
    Exam.assertTrue(mTotalLength >= mMaxLength);
    Exam.assertTrue(mMaxLength >= mMinLength);
    Exam.assertTrue(mMinLength > 0 || (mMinLength == 0 && mNumberSequences == 0));
    Exam.assertTrue(mNumberSequences >= 0);
    return true;
  }

  byte getNameEncoding() {
    return mNameEncoding;
  }

  void setSequenceEncoding(byte val) {
    if (val < 0 || val > NUMBER_SEQUENCE_ENCODINGS) {
      throw new IllegalArgumentException("" + val);
    }
    mSequenceEncoding = val;
  }

  byte getSequenceEncoding() {
    return mSequenceEncoding;
  }

  void setQualityEncoding(byte val) {
    if (val < 0 || val > NUMBER_QUALITY_ENCODINGS) {
      throw new IllegalArgumentException("" + val);
    }
    mQualityEncoding = val;
  }

  byte getQualityEncoding() {
    return mQualityEncoding;
  }

  /**
   * Set the checksum on data values
   * @param hash the data checksum
   */
  public final void setDataChecksum(final long hash) {
    mDataChecksum = hash;
  }

  /**
   * @return the data checksum
   */
  public final long getDataChecksum() {
    return mDataChecksum;
  }

  /**
   * Set the checksum on quality values
   * @param hash the quality checksum
   */
  public final void setQualityChecksum(final long hash) {
    mQualityChecksum = hash;
  }

  /**
   * @return the quality checksum
   */
  public final long getQualityChecksum() {
    return mQualityChecksum;
  }

  /**
   * Set the checksum on sequence names
   * @param hash the name checksum
   */
  public final void setNameChecksum(final long hash) {
    mNameChecksum = hash;
  }

  /**
   * Set the checksum on sequence name suffixes
   * @param hash the name checksum
   */
  public final void setNameSuffixChecksum(final long hash) {
    mNameSuffixChecksum = hash;
  }

  /**
   * @return the name checksum
   */
  public final long getNameChecksum() {
    return mNameChecksum;
  }

  /**
   * @return the name suffix checksum
   */
  public final long getNameSuffixChecksum() {
    return mNameSuffixChecksum;
  }

  /**
   * @return the preread arm for sequences in this SDF
   */
  public final PrereadArm getPrereadArm() {
    return mPrereadArm;
  }

  /**
   * @return version of data index files
   */
  public long dataIndexVersion() {
    return mSeqIndexVersion;
  }

  /**
   * Set the preread arm
   * @param arm preread arm
   */
  public final void setPrereadArm(final PrereadArm arm) {
    mPrereadArm = arm;
  }

  /**
   * @return preread type
   */
  public final PrereadType getPrereadType() {
    return mPrereadType;
  }

  /**
   * Gets the preread type from a format
   * @param format input format
   * @return the preread type
   */
  public static PrereadType typeFromFormat(InputFormat format) {
    switch(format) {
      case SDF:
      case FASTA:
      case FASTQ:
      case SAM_PE:
      case SAM_SE:
        return PrereadType.UNKNOWN;

      case TSV_CG:
      case FASTQ_CG:
      case SAM_CG:
        return PrereadType.CG;

      case SOLEXA:
      case SOLEXA1_3:
        return PrereadType.SOLEXA;

      default:
        throw new IllegalArgumentException("Unknown Input format " + format);
    }
  }

  /**
   * Set the preread type
   * @param type the type
   */
  public final void setPrereadType(final PrereadType type) {
    mPrereadType = type;
  }


  /**
   * Sets the id
   * @param id id of this SDF
   */
  public void setSdfId(final SdfId id) {
    mSdfId = id;
  }

  /**
   * @return the id of this SDF
   */
  public SdfId getSdfId() {
    return mSdfId;
  }

  /**
   * @return true if SDF has per sequence checksums
   */
  boolean hasPerSequenceChecksums() {
    return mVersion >= PER_SEQUENCE_CHECKSUM_VERSION;
  }

  /**
   * @return whether we have a separate file for full sequence names
   */
  public boolean hasSequenceNameSuffixes() {
    return hasNames() && mHasSuffixes;
  }

  void setQSPostionAverageHistogram(final double[] posAverages) {
    mPositionAverageHistogram = posAverages.clone();
  }

  /**
   * @return the histogram of average quality scores
   */
  public double[] getQSPositionAverageHistogram() {
    return mPositionAverageHistogram.clone();
  }

  private void compatDoubleHash(final PrereadHashFunction hash, final long version, final double val) {
    if (version >= DOUBLE_HASH_FIX_VERSION) {
      hash.irvineHash(Double.doubleToLongBits(val));
    } else {
      hash.irvineHash(String.valueOf(val));
    }
  }
}
