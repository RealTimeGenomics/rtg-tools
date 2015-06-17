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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.reader.AbstractStreamManager.RollingFile;
import com.rtg.util.array.ArrayUtils;
import com.rtg.util.array.CommonIndex;
import com.rtg.util.bytecompression.ByteArray;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.SeekableStream;

/**
 * Class to look up sequence or quality score checksums
 */
abstract class PointerFileHandler {


  public static final int SEQUENCE_POINTER = 1;
  public static final int LABEL_POINTER = 2;

  private final byte[] mIntbuf = new byte[4];
  protected final long mEntrySize;
  protected final long mChecksumSize;
  protected long mSeqPos;
  protected long mSeqLength;
  protected byte mChecksum;
  protected byte mQualityChecksum;

  public PointerFileHandler(long baseSize, long checksumSize) {
    mEntrySize = baseSize + checksumSize;
    mChecksumSize = checksumSize;
  }

  /**
   * Prepare some of the other methods by loading a sequence pointer
   * @param currentFile file currently pointed to by <code>pointerRoller</code>
   * @param pointerInFile index in file of pointer desired
   * @param pointerRoller access to stream containing pointers
   * @param index index for data files
   * @throws IOException When IO errors occur
   */
  abstract void initialisePosition(int currentFile, long pointerInFile, AbstractStreamManager.RollingFile pointerRoller, DataFileIndex index) throws IOException;

  /**
   * Reads checksum part of entry, assumes stream points to the beginning of an entry
   * @param stream stream to read from
   * @throws IOException if an IO error occurs
   */
  abstract void readChecksums(SeekableStream stream) throws IOException;

  /**
   * @return position in data file of sequence start (called after <code>initialisePosition</code>)
   */
  long seqPosition() {
    return mSeqPos;
  }
  /**
   * @return length of sequence (called after <code>initialisePosition</code>)
   */
  long seqLength() {
    return mSeqLength;
  }

  /**
   * @return checksum for sequence data (called after <code>initialisePosition</code>)
   */
  byte checksum() {
    return mChecksum;
  }
  /**
   * @return checksum for quality data (called after <code>initialisePosition</code>)
   */
  byte qualityChecksum() {
    return mQualityChecksum;
  }

  /**
   * Read a single pointer value from given file.
   * @param f file containing pointer (must be from SDF type this was initialised against)
   * @param index index of sequence
   * @return the start position within corresponding data file of sequence
   * @throws IOException if an IO error occurs
   */
  abstract int readPointer(File f, long index) throws IOException;

  /**
   * Read a chunk of pointers from given file
   * @param f file containing pointers (must be from SDF type this was initialised against)
   * @param fileOffset index of pointer to start from
   * @param fileEnd exclusive final pointer index
   * @param a array to write pointers into
   * @param offset start index in array
   * @param addend constant value to add to each pointer before storing
   * @param checksums array to load checksums into
   * @param qualityChecksums array to load quality checksums into
   * @return number of pointers read
   * @throws IOException if an IO error occurs
   */
  abstract int readPointers(File f, int fileOffset, int fileEnd, CommonIndex a, long offset, long addend, ByteArray checksums, ByteArray qualityChecksums) throws IOException;

  abstract void readChecksums(File f, long index, ByteArray checksums, ByteArray qualityChecksums, int offset) throws IOException;

  /**
   * Initialises an returns a handler to use with pointer files in given directory
   * @param index index for given SDF
   * @param type type of pointers to handle
   * @return the handler
   */
  public static PointerFileHandler getHandler(IndexFile index, int type) {
    switch (type) {
      case SEQUENCE_POINTER:
        if (index.getVersion() < IndexFile.PER_SEQUENCE_CHECKSUM_VERSION) {
          return new OriginalPointerHandler();
        } else {
          return new PointerAndChecksumHandler(index);
        }
      case LABEL_POINTER:
      default:
        throw new UnsupportedOperationException();
    }
  }

  protected final void initPositionInternal(int currentFile, long pointerInFile, AbstractStreamManager.RollingFile pointerRoller, DataFileIndex index) throws IOException {
    pointerRoller.randomAccessFile().seek(pointerInFile * mEntrySize + mChecksumSize);
    final long seqpos = readInt(pointerRoller.randomAccessFile());   // mPointers.randomAccessFile().readInt();
    //determine length
    long length;
    final long remaining = pointerRoller.randomAccessFile().length() - pointerRoller.randomAccessFile().getPosition();
    if (remaining >= mEntrySize) {
      readChecksums(pointerRoller.randomAccessFile());
      int nextseqpos;
      nextseqpos = readInt(pointerRoller.randomAccessFile()); //.readInt();
      length = nextseqpos - seqpos;
    } else if (mChecksumSize > 0 && remaining >= mChecksumSize) {
      length = index.dataSize(currentFile) - seqpos; //mData.randomAccessFile().length() - seqpos;
      readChecksums(pointerRoller.randomAccessFile());
    } else {
      length = index.dataSize(currentFile) - seqpos; //mData.randomAccessFile().length() - seqpos;
      while (pointerRoller.rollFile()) {
        if (pointerRoller.randomAccessFile().length() == 0) {
          length += index.dataSize(pointerRoller.currentFileNo()); //  new File(mDir, Bsd.SEQUENCE_DATA_FILENAME + pointerRoller.currentFileNo()).length() ;
        } else {
          readChecksums(pointerRoller.randomAccessFile());
          if (pointerRoller.randomAccessFile().length() >= mEntrySize) {
            length += readInt(pointerRoller.randomAccessFile()); //.readInt();
          } else {
            length += index.dataSize(pointerRoller.currentFileNo());
          }
          break;
        }
      }
      if (!pointerRoller.openDataFile(currentFile)) {
        throw new CorruptSdfException("Expected file missing");
      }
    }
    mSeqLength = length;
    mSeqPos = seqpos;
  }

  int readInt(InputStream is) throws IOException {
    IOUtils.readFully(is, mIntbuf, 0, 4);
    return ByteArrayIOUtils.bytesToIntBigEndian(mIntbuf, 0);
  }

  private static class OriginalPointerHandler extends PointerFileHandler {

    public OriginalPointerHandler() {
      super(4, 0);
    }

    @Override
    void initialisePosition(int currentFile, long pointerInFile, RollingFile pointerRoller, DataFileIndex index) throws IOException {
      initPositionInternal(currentFile, pointerInFile, pointerRoller, index);
    }

    @Override
    void readChecksums(SeekableStream stream) throws IOException {
    }

    @Override
    byte checksum() {
      throw new UnsupportedOperationException();
    }
    @Override
    byte qualityChecksum() {
      throw new UnsupportedOperationException();
    }

    @Override
    int readPointer(File f, long index) throws IOException {
      return FileUtils.getIntFromFile(f, (int) index);
    }

    @Override
    int readPointers(File f, int fileOffset, int fileEnd, CommonIndex a, long offset, long addend, ByteArray checksums, ByteArray qualityChecksums) throws IOException {
      return ArrayUtils.readInts(f, fileOffset, fileEnd, a, offset, addend);
    }
    @Override
    void readChecksums(File f, long index, ByteArray checksums, ByteArray qualityChecksums, int offset) throws IOException {
    }

  }

  private static class PointerAndChecksumHandler extends PointerFileHandler {
    private final boolean mHasQuality;
    private final byte[] mBuf;
    public PointerAndChecksumHandler(IndexFile index) {
      super(4L, index.hasQuality() ? 2 : 1);
      mHasQuality = index.hasQuality();
      mBuf = new byte[(int) mChecksumSize];
    }

    @Override
    void initialisePosition(int currentFile, long pointerInFile, RollingFile pointerRoller, DataFileIndex index) throws IOException {
      initPositionInternal(currentFile, pointerInFile, pointerRoller, index);
    }

    @Override
    void readChecksums(SeekableStream stream) throws IOException {
      IOUtils.readFully(stream, mBuf, 0, mBuf.length);
      mChecksum = mBuf[0];
      if (mHasQuality) {
        mQualityChecksum = mBuf[1];
      }
    }

    @Override
    int readPointer(File f, long index) throws IOException {
      try (FileInputStream fis = new FileInputStream(f)) {
        final byte[] buf = new byte[4];
        FileUtils.skip(fis, index * mEntrySize + mChecksumSize);
        IOUtils.readFully(fis, buf, 0, buf.length);
        return ByteArrayIOUtils.bytesToIntBigEndian(buf, 0);
      }
    }

    @Override
    void readChecksums(File f, long index, ByteArray checksums, ByteArray qualityChecksums, int offset) throws IOException {
      try (FileInputStream fis = new FileInputStream(f)) {
        final byte[] buf = new byte[(int) mChecksumSize];
        FileUtils.skip(fis, index * mEntrySize);
        IOUtils.readFully(fis, buf, 0, buf.length);
        checksums.set(offset, buf[0]);
        if (mHasQuality) {
          qualityChecksums.set(offset, buf[1]);
        }
      }
    }

    /**
     * Read an array of longs from a file.
     *
     * @param f file
     * @param fileOffset offset into the file to start reading at
     * @param fileEnd position in the file to finish
     * @param a index to read into
     * @param offset in the array.
     * @param addend amount to add to each entry
     * @return length
     * @exception IOException if an I/O error occurs
     */
    @Override
    int readPointers(File f, int fileOffset, int fileEnd, CommonIndex a, long offset, long addend, ByteArray checksums, ByteArray qualityChecksums) throws IOException {
      try (FileInputStream fis = new FileInputStream(f)) {
        FileUtils.skip(fis, fileOffset * mEntrySize);
        final int entries = fileEnd - fileOffset;
        final byte[] buf = new byte[entries * (int) mEntrySize];
        IOUtils.readFully(fis, buf, 0, buf.length);
        for (int i = 0; i < entries; i++) {
          if (offset + i - 1 >= 0) {
            checksums.set(offset + i - 1, buf[i * (int) mEntrySize]);
            if (mHasQuality) {
              qualityChecksums.set(offset + i - 1, buf[i * (int) mEntrySize + 1]);
            }
          }
          a.set(offset + i, ByteArrayIOUtils.bytesToIntBigEndian(buf, (int) (i * mEntrySize + mChecksumSize)) + addend);
        }
        return entries;
      }
    }
  }
}
