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

import com.rtg.util.io.SeekableStream;

/**
 * Stream manager for reading data, should only be used by DefaultSequencesReader
 */
class SequenceStreamManager extends AbstractStreamManager {

  private final RollingFile mQuality;

  private final boolean mOpenQuality;

  private final PointerFileHandler mPointerHandler;

  private boolean mDataSeeked = false;

  private boolean mQualitySeeked = false;

  /**
   * Creates a the stream manager
   * @param dir Directory containing sequence data
   * @param numberSequences Number of sequences in directory
   * @param quality true if quality data is available
   * @param mainIndex the index structure
   * @param openerFactory implementation for opening sequence and quality files.
   * @throws IOException If an I/O Error occurs
   */
  SequenceStreamManager(final File dir, final long numberSequences, final boolean quality, IndexFile mainIndex, DataFileOpenerFactory openerFactory) throws IOException {
    super(dir, numberSequences, SdfFileUtils.SEQUENCE_INDEX_FILENAME, SdfFileUtils.SEQUENCE_DATA_FILENAME, SdfFileUtils.SEQUENCE_POINTER_FILENAME, mainIndex.dataIndexVersion(), openerFactory.getSequenceOpener());
    mOpenQuality = quality;
    if (mOpenQuality) {
      mQuality = new DataRollingFile(mDir, SdfFileUtils.SEQUENCE_QUALITY_DATA_FILENAME, mIndex.numberEntries(), mIndex, openerFactory.getQualityOpener());
    } else {
      mQuality = null;
    }
    mPointerHandler = PointerFileHandler.getHandler(mainIndex, PointerFileHandler.SEQUENCE_POINTER);
  }

  @Override
  protected void seekImpl(final long seqNum) throws IOException {
    final long pointerpos = seqNum - mCurrentLower;
    mPointerHandler.initialisePosition(mIndexedSequenceFileNumber, pointerpos, mPointers, mIndex);
    if (mPointerHandler.seqLength() > Integer.MAX_VALUE || mPointerHandler.seqLength() < 0) {
      //we only allow single sequences up to 2gb
      throw new CorruptSdfException();
    }
    mDataLength = mPointerHandler.seqLength();
    mData.randomAccessFile().seek(mPointerHandler.seqPosition());
    mDataSeeked = true;
    if (mOpenQuality) {
      mQuality.randomAccessFile().seek(mPointerHandler.seqPosition());
      mQualitySeeked = true;
    }
  }

  @Override
  protected void openFiles() throws IOException {
    super.openFiles();
    if (mOpenQuality) {
      openQualityFile(mIndexedSequenceFileNumber);
    }
  }

  protected void openQualityFile(final int fileno) throws IOException {
    if (!mQuality.openDataFile(fileno)) {
      throw new CorruptSdfException("Expected file missing");
    }
  }

  @Override
  protected void ensureFiles() throws IOException {
    super.ensureFiles();
    if (mOpenQuality && mQuality.currentFileNo() != mIndexedSequenceFileNumber) {
      openQualityFile(mIndexedSequenceFileNumber);
    }
  }

  @Override
  public void close() throws IOException {
    super.close();
    if (mOpenQuality && mQuality != null) {
      mQuality.close();
    }
  }

  int read(byte[] dataOut, int start, int length, RollingFile src) throws IOException {
    if (length == 0) {
      return 0;
    }
    final int fullLength = (int) getDataLength();
    if ((start + length) > fullLength) {
      throw new IllegalArgumentException("Requested data not a subset of sequence data.");
    }
    if (length > dataOut.length) {
      throw new IllegalArgumentException("Array too small got: " + dataOut.length + " required: " + length);
    }
    SeekableStream raf = src.randomAccessFile();
    int count = 0;
    while ((start - count) >= (int) (raf.length() - raf.getPosition())) {
      count += (int) (raf.length() - raf.getPosition());
      if (!src.rollFile()) {
        throw new CorruptSdfException("expected SDF data missing");
      }
      raf = src.randomAccessFile();
    }
    raf.seek(raf.getPosition() + (start - count));
    count = 0;
    int last;
    while (count < length) {
      last = raf.read(dataOut, count, length - count);
      final int eof = -1;
      if (last == eof) {
        //src.rollFile();
        if (!src.rollFile()) {
          throw new CorruptSdfException("expected SDF data missing");
        }
        raf = src.randomAccessFile();
        last = raf.read(dataOut, count, length - count);
      }
      if (last >= 0) {
        count += last;
      }
    }
    return length;
  }

  int readData(byte[] dataOut, int start, int length) throws IOException {
    if (!mDataSeeked) {
      throw new IllegalStateException("readData called with pointer in unknown position.");
    }
    mDataSeeked = false;
    return read(dataOut, start, length, mData);
  }

  int readQuality(byte[] dataOut, int start, int length) throws IOException {
    if (!mQualitySeeked) {
      throw new IllegalStateException("readQuality called with pointer in unknown position.");
    }
    mQualitySeeked = false;
    return read(dataOut, start, length, mQuality);
  }

  byte sequenceChecksum() {
    return mPointerHandler.checksum();
  }
  byte qualityChecksum() {
    return mPointerHandler.qualityChecksum();
  }
}
