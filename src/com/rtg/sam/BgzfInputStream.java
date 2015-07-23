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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.rtg.util.io.ByteArrayIOUtils;

/**
 * Simple BAM reader
 */
public class BgzfInputStream extends InputStream {
  private static final int MAX_BGZF_BLOCK = 65535;

  private final byte[] mIOBuf;
  private byte[] mUncompressBuf;
  private final InputStream mInputStream;

  private long mBlockStartPos;
  private long mBlockEndPos;
  private final BgzfBlock mCurrentBlock;
  private final Inflater mInflater;
  private final CRC32 mCrc;

  private int mDataPosition;
  private int mDataLength;

  /**
   * Constructs from stream
   * @param input the stream to read from
   */
  public BgzfInputStream(InputStream input) {
    mInputStream = input;
    mCurrentBlock = new BgzfBlock();
    mCurrentBlock.mData = new byte[MAX_BGZF_BLOCK];
    mInflater = new Inflater(true);
    mIOBuf = new byte[4096];
    mUncompressBuf = new byte[MAX_BGZF_BLOCK * 2];
    mBlockEndPos = 0;
    mBlockStartPos = 0;
    mCrc = new CRC32();
  }

  /**
   * Constructs from file
   * @param bamFile file to read from
   * @throws FileNotFoundException if file not found
   */
  public BgzfInputStream(File bamFile) throws FileNotFoundException {
    this(new FileInputStream(bamFile));
  }

  /**
   * Offset in file or stream of current block.
   * @return the offset
   */
  public long blockStart() {
    if (mDataPosition == mDataLength) {
      return mBlockEndPos;
    }
    return mBlockStartPos;
  }

  /**
   * Offset in uncompressed block of current read pointer
   * @return the offset
   */
  public int dataOffset() {
    if (mDataPosition == mDataLength) {
      return 0;
    }
    return mDataPosition;
  }

  @Override
  public int read() throws IOException {
    final int remaining = mDataLength - mDataPosition;
    if (remaining == 0) {
      if (!readBlock()) {
        return -1;
      }
    }
    return mUncompressBuf[mDataPosition++] & 0xFF;
  }

  /**
   */
  @Override
  public int read(byte[] buf, int offset, int length) throws IOException {
    if (offset + length > buf.length) {
      throw new ArrayIndexOutOfBoundsException("ArrayLength: " + buf.length + " offset: " + offset + " length: " + length);
    }
    int ret = 0;
    int toRead = length - ret;
    while (toRead > 0) {
      final int remaining = mDataLength - mDataPosition;
      if (remaining > 0) {
        final int len = toRead <= remaining ? toRead : remaining;
        System.arraycopy(mUncompressBuf, mDataPosition, buf, offset + ret, len);
        mDataPosition += len;
        ret += len;
      } else {
        if (!readBlock()) {
          break;
        }
      }
      toRead = length - ret;
    }
    if (ret == 0) {
      return -1;
    }
    return ret;
  }

  private boolean readBlock() throws IOException {
    mBlockStartPos = mBlockEndPos;
    mDataPosition = 0;
    final int len = readBgzfBlock(mInputStream, mIOBuf, mCurrentBlock);
    if (len == -1) {
      mBlockEndPos = mBlockStartPos;
      mDataLength = 0;
      return false;
    }
    mBlockEndPos += len;
    mUncompressBuf = inflate(mInflater, mCurrentBlock.mData, mUncompressBuf, mCurrentBlock.mInputSize);
    mCrc.reset();
    mCrc.update(mUncompressBuf, 0, mCurrentBlock.mInputSize);
    if (((long) mCurrentBlock.mCrc & 0xFFFFFFFFL) != mCrc.getValue()) {
      throw new IOException("Crc doesn't match block");
    }
    mDataLength = mCurrentBlock.mInputSize;
    return true;
  }

  private static byte[] inflate(Inflater inf, byte[] input, byte[] dest, int size) throws IOException {
    inf.reset();
    inf.setInput(input);
    final byte[] ret = size > dest.length ? new byte[size] : dest;
    try {
      int len = inf.inflate(ret);
      while (len < size) {
        final int r = inf.inflate(ret, len, size - len);
        if (r <= 0) {
          throw new IOException("ISIZE: " + size + " actual: " + len + " r: " + r);
        }
        len += r;
      }
      assert len == size && inf.finished();
      return ret;
    } catch (final DataFormatException e) {
      throw new IOException("Invalid BGZF file", e);
    }
  }

  private static int readIOFully(InputStream is, byte[] buf, int amount) throws IOException {
    assert amount <= buf.length;
    assert amount >= 0;
    int tot = 0;
    int len;
     do {
      len = is.read(buf, tot, amount - tot);
      if (len <= 0) {
        throw new EOFException("Unexpected end of file while reading BAM file");
      }
      tot += len;
    } while (tot < amount);
    return tot;
  }

  /**
   * Reads a <code>BGZF</code> block
   * @param fis raw data stream to read from
   * @param buf buffer to use for disk I, must be <code>max(12, XLEN)</code> (see GZIP specification)
   * @param block block to read into
   * @return number of bytes read from disk, or -1 if not enough was read.
   * @throws IOException If an IO error occurs
   * @throws EOFException If end of file is reached in the middle of the block
   */
  private static int readBgzfBlock(InputStream fis, byte[] buf, BgzfBlock block) throws IOException {
    final int first = fis.read();
    if (first == -1) {
      return -1;
    }
    int tot = 1;
    tot += readIOFully(fis, buf, 11);
    //block.mGzId1 = (byte) (first & 0xFF);
    //block.mGzId2 = buf[0];
    //block.mCompressMethod = buf[1];
    //block.mFlags = buf[2];
    //block.mModTime = ByteArrayIOUtils.bytesToIntLittleEndian(buf, 3);
    //block.mExtraFlags = buf[7];
    //block.mOs = buf[8];
    block.mExtraLength = ByteArrayIOUtils.bytesToShortLittleEndian(buf, 9); // (((buf[10] & 0xFF) << 8) + (buf[9] & 0xFF));
    tot += readIOFully(fis, buf, block.mExtraLength);
    if (block.mExtraLength < 6) {
      throw new IOException("Not a valid BGZF file");
    }
    //block.mSubId1 = buf[0];
    //block.mSubId2 = buf[1];
    //block.mSubLength = ByteArrayIOUtils.bytesToShortLittleEndian(buf, 2); //short) (((buf[3] & 0xFF) << 8) + (buf[2] & 0xFF));
    block.mBlockSize = ByteArrayIOUtils.bytesToShortLittleEndian(buf, 4); //short) (((buf[5] & 0xFF) << 8) + (buf[4] & 0xFF)); //defined as entire block size - 1
    tot += readIOFully(fis, block.mData, block.mBlockSize - block.mExtraLength - 19); //20 + XLEN is size of headers
    tot += readIOFully(fis, buf, 8);
    block.mCrc = ByteArrayIOUtils.bytesToIntLittleEndian(buf, 0);
    block.mInputSize = ByteArrayIOUtils.bytesToIntLittleEndian(buf, 4);
    assert tot == block.mBlockSize + 1 : "tot: " + tot + " blocksize: " + block.mBlockSize;
    return tot;
  }

  /**
   */
  @Override
  public void close() throws IOException {
    mInputStream.close();
  }

  private static class BgzfBlock {
    //section 1, 12 bytes long
    //byte mGzId1;
    //byte mGzId2;
    //byte mCompressMethod;
    //byte mFlags;
    //int mModTime;
    //byte mExtraFlags;
    //byte mOs;
    int mExtraLength;

    //6 long + extralength - 6
    //byte mSubId1; //66
    //byte mSubId2; //67
    //int mSubLength; //2
    int mBlockSize;
    //extra fields skipped
    byte[] mData; //blockSize - extraLength - 19
    //8 long
    int mCrc;
    int mInputSize;
  }
}
