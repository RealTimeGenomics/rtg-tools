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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;

import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;

/**
 * This stores data in memory as it appears on disk. Allowing for direct load (no decompress/re-compress)
 */
public class CompressedMemorySequencesReader2 extends AbstractSequencesReader {

  private final IndexFile mIndexFile;

  private PrereadNamesInterface mNames;
  private PrereadNamesInterface mNameSuffixes;
  private final DataInMemory mData;
  private final long mNumberSequences;
  final File mDirectory;
  private final LongRange mRegion;
  private final long mStart;
  private final long mEnd;

  /**
   * Alternative to other one with similar name
   * @param directory directory containing SDF
   * @param indexFile index file
   * @param loadNames should we load names
   * @param loadFullNames should we load full names
   * @param region region to restrict to
   * @throws IOException IO exception occurs
   */
  CompressedMemorySequencesReader2(File directory, IndexFile indexFile, boolean loadNames, boolean loadFullNames, LongRange region) throws IOException {
    mIndexFile = indexFile;
    mRegion = SequencesReaderFactory.resolveRange(indexFile, region);
    mStart = mRegion.getStart();
    mEnd = mRegion.getEnd();
    mNumberSequences = mEnd - mStart;
    mData = DataInMemory.loadDelayQuality(directory, indexFile, DataFileIndex.loadSequenceDataFileIndex(indexFile.dataIndexVersion(), directory), mStart, mEnd);
    if (mNumberSequences > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Too many sequences in region: " + region + ", maximum is: " + Integer.MAX_VALUE);
    }
    mDirectory = directory;
    if (loadNames && mIndexFile.hasNames()) {
      loadNames();
      loadNameSuffixes(loadFullNames, mIndexFile.hasSequenceNameSuffixes());
    }
    final StringBuilder sb = new StringBuilder("CMSR2 statistics");
    sb.append(LS);
    this.infoString(sb);
    Diagnostic.userLog(sb.toString());
  }

  //copy constructor
  CompressedMemorySequencesReader2(CompressedMemorySequencesReader2 other) {
    mIndexFile = other.mIndexFile;
    mNames = other.mNames;
    mNameSuffixes = other.mNameSuffixes;
    mData = other.mData.copy();
    mNumberSequences = other.mNumberSequences;
    mDirectory = other.mDirectory;
    mRegion = other.mRegion;
    mStart = other.mStart;
    mEnd = other.mEnd;
  }

  /**
   * Load the names if they haven't already been loaded.
   * @throws IOException if an I/O related error occurs
   */
  private void loadNames() throws IOException {
    mNames = new PrereadNames(mDirectory, mRegion, false);
    if (mIndexFile.getVersion() >= IndexFile.SEPARATE_CHECKSUM_VERSION && mRegion.getStart() == 0 && mRegion.getEnd() == mIndexFile.getNumberSequences()) {
      if (mNames.calcChecksum() != mIndexFile.getNameChecksum()) {
        throw new CorruptSdfException("Sequence names failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
      } else {
        Diagnostic.developerLog("Sequence names passed checksum");
      }
    }
  }

  private void loadNameSuffixes(boolean attemptLoad, boolean suffixExists) throws IOException {
    mNameSuffixes = attemptLoad && suffixExists ? new PrereadNames(mDirectory, mRegion, true) : new EmptyStringPrereadNames(mEnd - mStart);
    if (attemptLoad && suffixExists) {
      if (mRegion.getStart() == 0 && mRegion.getEnd() == mIndexFile.getNumberSequences()) {
        if (mNameSuffixes.calcChecksum() != mIndexFile.getNameSuffixChecksum()) {
          throw new CorruptSdfException("Sequence name suffixes failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
        } else {
          Diagnostic.developerLog("Sequence name suffixes passed checksum");
        }
      }
    }
  }

  @Override
  public IndexFile index() {
    return mIndexFile;
  }

  @Override
  public long numberSequences() {
    return mNumberSequences;
  }

  @Override
  public File path() {
    return mDirectory;
  }


  // Direct access methods

  @Override
  public int length(long sequenceIndex) {
    return mData.length((int) sequenceIndex);
  }

  @Override
  public byte sequenceDataChecksum(long sequenceIndex) throws IOException {
    return mData.sequenceChecksum((int) sequenceIndex);
  }

  @Override
  public String name(long sequenceIndex) {
    return mNames.name(sequenceIndex);
  }

  @Override
  public String nameSuffix(long sequenceIndex) {
    return mNameSuffixes.name(sequenceIndex);
  }

  @Override
  public int read(long sequenceIndex, byte[] dataOut) throws IllegalArgumentException, IOException {
    if (sequenceIndex >= mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + sequenceIndex + ", maximum is: " + mNumberSequences);
    }
    return mData.readSequence((int) sequenceIndex, dataOut, 0, Integer.MAX_VALUE);
  }

  @Override
  public int read(long sequenceIndex, byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException {
    if (sequenceIndex >= mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + sequenceIndex + ", maximum is: " + mNumberSequences);
    }
    return mData.readSequence((int) sequenceIndex, dataOut, start, length);
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest) throws IllegalArgumentException, IOException {
    if (sequenceIndex >= mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + sequenceIndex + ", maximum is: " + mNumberSequences);
    }
    return mData.readQuality((int) sequenceIndex, dest, 0, Integer.MAX_VALUE);
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IllegalArgumentException, IOException {
    if (sequenceIndex >= mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + sequenceIndex + ", maximum is: " + mNumberSequences);
    }
    return mData.readQuality((int) sequenceIndex, dest, start, length);
  }

  @Override
  public void close() {
  }

  @Override
  public PrereadNamesInterface names() {
    return mNames;
  }

  @Override
  public long lengthBetween(long start, long end) {
    if (start > mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + start + ", maximum is: " + mNumberSequences);
    }
    if (end > mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + end + ", maximum is: " + mNumberSequences);
    }
    return mData.lengthBetween((int) start, (int) end);
  }

  @Override
  public int[] sequenceLengths(long start, long end) {
    if (start >= mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + start + ", maximum is: " + mNumberSequences);
    }
    if (end > mNumberSequences) {
      throw new IllegalArgumentException("Invalid sequence index: " + end + ", maximum is: " + mNumberSequences);
    }
    return mData.sequenceLengths((int) start, (int) end);
  }

  @Override
  public SequencesReader copy() {
    return new CompressedMemorySequencesReader2(this);
  }


  void initQuality() throws IOException {
    mData.initQuality();
  }

  void infoString(final StringBuilder sb) {
    sb.append("Memory Usage: ").append(mNumberSequences).append(" sequences").append(LS);
    long totalBytes = mData.infoString(sb);
    if (mNames != null) {
      sb.append("\t\t").append(StringUtils.commas(mNames.bytes())).append("\t").append(StringUtils.commas(mNames.length())).append("\tNames").append(LS);
      totalBytes +=  mNames.bytes();
    }
    if (mNameSuffixes != null) {
      sb.append("\t\t").append(StringUtils.commas(mNameSuffixes.bytes())).append("\t").append(StringUtils.commas(mNameSuffixes.length())).append("\tSuffixes").append(LS);
      totalBytes +=  mNameSuffixes.bytes();
    }
    sb.append("\t\t").append(StringUtils.commas(totalBytes)).append("\t\tTotal bytes").append(LS);
  }


}
