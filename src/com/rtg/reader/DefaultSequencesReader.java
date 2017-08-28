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
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.CRC32;

import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.SeekableStream;

/**
 * Default implementation of SequencesReader.
 * Needs a lot of work.
 * - Buffering
 * - Proper error handling
 */
public final class DefaultSequencesReader extends AbstractSequencesReader implements Integrity {

  private final File mDirectory;
  private final File mCanonicalDirectory;
  private final boolean mHasQualityData;

  //decided to use random access files, this means we have to sort out buffering input (for performance)
  //ourselves but should overall be less complicated than using inputstreams
  private final IndexFile mIndex;
  private final SequenceStreamManager mSequenceManager;
  private final LabelStreamManager mLabelManager;
  private final LabelStreamManager mSuffixManager;
  private final SequenceType mSequenceType;
  private final LongRange mRegion;
  private final long mStart;
  private final long mEnd;
  private final CRC32 mChecksum = new CRC32();
  private Names mNames = null;

  /**
   * @param dir directory containing the SDF files.
   * @param region restriction on part of SDF to be read.
   * @throws IOException when accessing SDF files.
   */
  public DefaultSequencesReader(File dir, LongRange region) throws IOException {
    //System.err.println("Creating: " + dir + " " + region);
    mIndex = new IndexFile(dir);
    mRegion = SequencesReaderFactory.resolveRange(mIndex, region);
    mStart = mRegion.getStart();
    mEnd = mRegion.getEnd();
    assert mEnd >= mStart;
    assert mEnd <= mIndex.getNumberSequences();

    if (mIndex.getSequenceType() < 0 || mIndex.getSequenceType() > SequenceType.values().length) {
      throw new CorruptSdfException(dir);
    }

    mDirectory = dir;
    mCanonicalDirectory = mDirectory.getCanonicalFile();
    mHasQualityData = mIndex.hasQuality();
    mSequenceType = SequenceType.values()[mIndex.getSequenceType()];
    final DataFileOpenerFactory openerFactory = new DataFileOpenerFactory(mIndex.getSequenceEncoding(), mIndex.getQualityEncoding(), mSequenceType);
    if (mIndex.getNumberSequences() > 0) {
      try {
        mSequenceManager = new SequenceStreamManager(dir, mIndex.getNumberSequences(), mHasQualityData, mIndex, openerFactory);
        if (mIndex.hasNames()) {
          mLabelManager = LabelStreamManager.getNameStreamManager(dir, mIndex.getNumberSequences(), mIndex.dataIndexVersion(), openerFactory);
        } else {
          mLabelManager = null;
        }
        if (mIndex.hasSequenceNameSuffixes()) {
          mSuffixManager = LabelStreamManager.getSuffixStreamManager(dir, mIndex.getNumberSequences(), mIndex.dataIndexVersion(), openerFactory);
        } else {
          mSuffixManager = null;
        }
      } finally {
        close();
      }
    } else {
      mSequenceManager = null;
      mLabelManager = null;
      mSuffixManager = null;
    }
  }

  // Access-by-index methods

  @Override
  public String name(long sequenceIndex) throws IOException {
    if (!mIndex.hasNames()) {
      throw new IllegalStateException("SDF has no names");
    }
    mLabelManager.seek(sequenceIndex + mStart);
    return readStringInternal(mLabelManager);
  }

  @Override
  public String nameSuffix(long sequenceIndex) throws IOException {
    if (!mIndex.hasNames()) {
      throw new IllegalStateException("SDF has no names");
    }
    if (!mIndex.hasSequenceNameSuffixes()) {
      return "";
    }
    mSuffixManager.seek(sequenceIndex + mStart);
    return readStringInternal(mSuffixManager);
  }

  @Override
  public int readQuality(final long sequenceIndex, final byte[] dest) throws IllegalArgumentException, IOException {
    if (!mHasQualityData) {
      return 0;
    }
    mSequenceManager.seek(sequenceIndex + mStart);
    return readQualityInternal(dest, 0, (int) mSequenceManager.getDataLength());
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IllegalArgumentException, IOException {
    if (!mHasQualityData) {
      return 0;
    }
    mSequenceManager.seek(sequenceIndex + mStart);
    return readQualityInternal(dest, start, length);
  }

  @Override
  public int read(final long sequenceIndex, final byte[] dataOut) throws IOException {
    mSequenceManager.seek(sequenceIndex + mStart);
    return readDataInternal(dataOut, 0, (int) mSequenceManager.getDataLength());
  }

  @Override
  public int read(final long sequenceIndex, final byte[] dataOut, int start, int length) throws IOException {
    mSequenceManager.seek(sequenceIndex + mStart);
    return readDataInternal(dataOut, start, length);
  }

  @Override
  public int length(long sequenceIndex) throws IOException {
    mSequenceManager.seek(sequenceIndex + mStart);
    return (int) mSequenceManager.getDataLength();
  }

  @Override
  public byte sequenceDataChecksum(long sequenceIndex) throws IOException {
    mSequenceManager.seek(sequenceIndex + mStart);
    return mSequenceManager.sequenceChecksum();
  }

  private String readStringInternal(LabelStreamManager manager) throws IOException {
    final SeekableStream data = manager.getData();
    final int length = (int) manager.getDataLength();
    final byte[] bytes = new byte[length];
    IOUtils.readFully(data, bytes, 0, length); //data.readFully(bytes);
    if (data.read() != 0) {
      throw new CorruptSdfException(mDirectory);
    }
    return new String(bytes, "iso8859-1");
    //return new String(bytes);
  }

  private int readDataInternal(byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException {
    final int ret = mSequenceManager.readData(dataOut, start, length);
    if (mIndex.hasPerSequenceChecksums() && ret == mSequenceManager.getDataLength()) {
      checkChecksum(dataOut, start, ret, mSequenceManager.sequenceChecksum());
    }
    return ret;
  }

  private int readQualityInternal(byte[] dest, int start, int length) throws IllegalArgumentException, IllegalStateException, IOException {
    if (!mHasQualityData) {
      return 0;
    }
    final int ret = mSequenceManager.readQuality(dest, start, length);
    if (mIndex.hasPerSequenceChecksums() && ret == mSequenceManager.getDataLength()) {
      checkChecksum(dest, start, ret, mSequenceManager.qualityChecksum());
    }
    return ret;
  }
  private void checkChecksum(byte[] data, int offset, int length, byte checksum) throws CorruptSdfException {
    mChecksum.update(data, offset, length);
    try {
      if (checksum != (byte) mChecksum.getValue()) {
        throw new CorruptSdfException("Checksum failed on sequence data");
      }
    } finally {
      mChecksum.reset();
    }
  }



  @Override
  public boolean hasQualityData() {
    return mHasQualityData;
  }

  @Override
  public long numberSequences() {
    return mEnd - mStart;
  }


  @Override
  public SequenceType type() {
    return mSequenceType;
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mDirectory.isDirectory());
    if (!Exam.assertTrue(mSequenceManager != null && mLabelManager != null)) {
      return false; //for findbugs benefit
    }
    Exam.assertTrue(mSequenceType.ordinal() == mIndex.getSequenceType());
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

  @Override
  public File path() {
    return mDirectory;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || !o.getClass().equals(DefaultSequencesReader.class)) {
      return false;
    }
    final DefaultSequencesReader other = (DefaultSequencesReader) o;
    return mCanonicalDirectory.equals(other.mCanonicalDirectory);
  }

  @Override
  public int hashCode() {
    return mDirectory.hashCode();
  }

  @Override
  public void close() throws IOException {
    if (mSequenceManager != null) {
      mSequenceManager.close();
    }
    if (mLabelManager != null) {
      mLabelManager.close();
    }
    if (mSuffixManager != null) {
      mSuffixManager.close();
    }
  }

  @Override
  public Names names() throws IOException {
    if (mNames == null) {
      mNames = new Names(mDirectory, LongRange.NONE);
      if (mIndex.getVersion() >= IndexFile.SEPARATE_CHECKSUM_VERSION) {
        if (mNames.calcChecksum() != mIndex.getNameChecksum()) {
          throw new CorruptSdfException("Sequence names failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
        } else {
          Diagnostic.developerLog("Sequence names passed checksum");
        }
      }
    }
    return mNames;
  }

  @Override
  public long lengthBetween(final long start, final long end) throws IOException {
    final int entrySize = mIndex.getVersion() >= IndexFile.PER_SEQUENCE_CHECKSUM_VERSION ? (mIndex.hasQuality() ? 6 : 5) : 4;
    final int checksumSize = entrySize - 4;
    final long totalSequences = numberSequences();
    if (start > totalSequences || end > totalSequences) {
      throw new IndexOutOfBoundsException("range " + start + ":" + end + " not available in SDF of size: " + totalSequences);
    }
    final long internalStart = start + mStart;
    final long internalEnd = end + mStart;
    //eliminate easy case
    final long max = mIndex.getMaxLength();
    final long min = mIndex.getMinLength();
    if (max == min) {
      return (internalEnd - internalStart) * max;
    }
    if (internalStart == totalSequences && internalEnd == totalSequences) {
      return 0;
    }
    //the general formula is:
    //endSeqPointer + fileLength1 + fileLength2 + ... + fileLengthN - startSeqPointer
    //where fileLengths 1 through N are files from startSeqPointer (inc) to endSeqPointer (exc).
    //endSeqPointer is not added if it is not a sequence.
    final DataFileIndex dataIndex = mSequenceManager.sequenceIndex();
    final long[] numSequences = new long[dataIndex.numberEntries()]; //mSequenceManager.sequenceIndex();
    long retLength = 0;
    int startFileNo = -1;
    int endFileNo = -1;
    for (int i = 0; i < numSequences.length; ++i) {
      if (i > 0) {
        numSequences[i] = numSequences[i - 1] + dataIndex.numberSequences(i);
      } else {
        numSequences[0] = dataIndex.numberSequences(0);
      }
      if (internalStart < numSequences[i]) {
        //identify start file
        if (startFileNo == -1) {
          startFileNo = i;
        }
        //identify end file
        if (internalEnd < numSequences[i]) {
          endFileNo = i;
          break;
        }
        retLength += dataIndex.dataSize(i); //Bsd.sequenceDataFile(mDirectory, i).length(); //fileLengthX
      }
    }
    final long startLower = startFileNo == 0 ? 0 : numSequences[startFileNo - 1];
    final RandomAccessFile rafEnd;
    try (RandomAccessFile rafStart = new RandomAccessFile(SdfFileUtils.sequencePointerFile(mDirectory, startFileNo), "r")) {
      rafStart.seek((internalStart - startLower) * entrySize + checksumSize);
      retLength -= rafStart.readInt(); //startSeqPointer
      //check if exclusive end sequence exists
      if (endFileNo != -1) {
        if (endFileNo == startFileNo) {
          rafEnd = rafStart;
        } else {
          rafEnd = new RandomAccessFile(SdfFileUtils.sequencePointerFile(mDirectory, endFileNo), "r");
        }
        final long endLower = endFileNo == 0 ? 0 : numSequences[endFileNo - 1];
        try {
          rafEnd.seek((internalEnd - endLower) * entrySize + checksumSize);
          retLength += rafEnd.readInt(); //endSeqPointer
        } finally {
          rafEnd.close();
        }
      }
    }
    return retLength;
  }

  /**
   * Puts lengths of sequences in an array and returns it.
   * Lightly tested
   *
   * @param start starting sequence
   * @param end ending sequence (excl)
   * @return array of lengths
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int[] sequenceLengths(final long start, final long end) throws IOException {
    final int entrySize = mIndex.getVersion() >= IndexFile.PER_SEQUENCE_CHECKSUM_VERSION ? (mIndex.hasQuality() ? 6 : 5) : 4;
    final long internalStart = start + mStart;
    final long internalEnd = end + mStart;
    final int[] ret = new int[(int) (internalEnd - internalStart)];
    if (ret.length == 0) {
      return ret;
    }
    if (mIndex.getMaxLength() == mIndex.getMinLength()) {
      Arrays.fill(ret, (int) mIndex.getMaxLength());
      return ret;
    }
    final byte[] buffer = new byte[entrySize * 1024];
    final DataFileIndex dataIndex = mSequenceManager.sequenceIndex();
    final long[] numSequences = new long[dataIndex.numberEntries()]; //mSequenceManager.sequenceIndex();
    int startFileNo = -1;
    int endFileNo = -1;
    for (int i = 0; i < numSequences.length; ++i) {
      if (i > 0) {
        numSequences[i] = numSequences[i - 1] + dataIndex.numberSequences(i);
      } else {
        numSequences[0] = dataIndex.numberSequences(0);
      }
      if (internalStart < numSequences[i]) {
        //identify start file
        if (startFileNo == -1) {
          startFileNo = i;
        }
        //identify end file
        if (internalEnd < numSequences[i]) {
          endFileNo = i;
          break;
        }
      }
    }
    final long startLower = startFileNo == 0 ? 0 : numSequences[startFileNo - 1];
    final long endLower = endFileNo < 1 ? 0 : numSequences[endFileNo - 1];
    int seqNo = 0;
    for (int i = startFileNo; (endFileNo == -1 || i <= endFileNo) && i < numSequences.length; ++i) {
      try (RandomAccessFile raf = new RandomAccessFile(SdfFileUtils.sequencePointerFile(mDirectory, i), "r")) {
        final long pos;
        if (i == startFileNo) {
          pos = (internalStart - startLower) * entrySize;
          raf.seek(pos);
        } else {
          pos = 0;
        }
        final long endPos = endFileNo == i ? (internalEnd - endLower) * entrySize : raf.length();
        seqNo = sequenceLengthsHelper(raf, buffer, ret, seqNo, pos, endPos, entrySize);

        //goal is for ret[x] to have length of sequence x in it
        //general case this is achieved by: ret[x] = pointer[x + 1] - pointer[x]
        //when spanning files this becomes: ret[x] = pointer[x + 1] + filelengths - pointer[x]
        //filelengths = all data files from start of sequence until its end (not including file it ends in)
        //because we're using the same array for pointers and final lengths we'd like to simplify this in
        //general to be ret[x] = ret[x + 1] - ret[x]
        //to do this I subtract filelength as appropriate from ret[x] after having dealt with all up to ret[x - 1]
        if (endFileNo != i && seqNo > 0) {
          final long fileLength = dataIndex.dataSize(i); //Bsd.sequenceDataFile(mDirectory, i).length();
          ret[seqNo - 1] = ret[seqNo - 1] - (int) fileLength; // to adjust for later processing
        } else {
          if (seqNo != ret.length) {
            throw new CorruptSdfException("Should have read " + ret.length + " sequence pointers, got " + seqNo);
          }
          //pointer must be in file, otherwise endFileNo was calculated incorrectly
          raf.read();
          ret[seqNo - 1] -= raf.readInt();
        }
      }
    }
    //we have already returned if arraylength = 0
    ret[ret.length - 1] = 0 - ret[ret.length - 1];
    return ret;
  }

  /**
   * Reads int pointers between positions <code>pos</code> and <code>endPos</code> of <code>raf</code> into
   * array <code>lengths</code> starting from position <code>initSeqNo</code> using <code>buffer</code> as
   * a scratch array to read data into.
   *
   * @param raf File to read from
   * @param buffer scratch buffer, should be length multiple of <code>entrySize</code>
   * @param lengths array to read integers into, all but last contain lengths not pointers
   * @param initSeqNo position to start reading integers into
   * @param initPos starting position in file
   * @param endPos ending position in file
   * @param entrySize the number of bytes for each entry
   * @return position after last read into array
   * @throws java.io.IOException We be reading from files here, you know what to expect.
   */
  static int sequenceLengthsHelper(final RandomAccessFile raf, final byte[] buffer, final int[] lengths, final int initSeqNo, final long initPos, final long endPos, int entrySize) throws IOException {
    int seqNo = initSeqNo;
    int bufStart = 0;
    long pos = initPos;
    while (pos < endPos) {
      final int maxReadLength;
      if (buffer.length - bufStart > endPos - pos) {
        maxReadLength = (int) (endPos - pos);
      } else {
        maxReadLength = buffer.length - bufStart;
      }
      final int readLength = raf.read(buffer, bufStart, maxReadLength);
      if (readLength < 0) {
        throw new IllegalArgumentException();
      }
      pos += readLength;
      final int length = readLength + bufStart;
      final int mod = length % entrySize;
      final int effLength = mod == 0 ? length : length - mod;
      int seqLimit = seqNo;
      for (int i = 0; i < effLength / entrySize; ++i) {
        lengths[seqLimit++] = ByteArrayIOUtils.bytesToIntBigEndian(buffer, i * entrySize + (entrySize - 4));
      }
      //final int seqLimit = ByteArrayIOUtils.convertToIntInLongArray(buffer, 0, effLength, lengths, seqNo, lengths.length) + seqNo;
      if (seqNo == 0 && seqLimit > 0) {
        seqNo = 1;
      }
      for (; seqNo < seqLimit; ++seqNo) {
        lengths[seqNo - 1] = lengths[seqNo] - lengths[seqNo - 1];
      }
      //adjust buffer if we didn't read multiple of 4 bytes
      bufStart = 0;
      for (int j = effLength; j < length; ++j, ++bufStart) {
        buffer[bufStart] = buffer[j];
      }
    }
    return seqNo;
  }


  @Override
  public SequencesReader copy() {
    try {
      return new DefaultSequencesReader(mDirectory, mRegion);
    } catch (final IOException e) {
      Diagnostic.userLog(e);
      Diagnostic.userLog("Error copying sequences reader for SDF at " + mDirectory);
      Diagnostic.userLog("Region: " + mRegion);
      throw new IllegalStateException("An error occurred trying to make a copy of the sequences reader backed by " + mDirectory + " on region " + mRegion + " (possibly the SDF was moved or deleted part way through the run.", e);
    }
  }

  @Override
  public IndexFile index() {
    return mIndex;
  }

}
