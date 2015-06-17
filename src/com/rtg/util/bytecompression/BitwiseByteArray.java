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
package com.rtg.util.bytecompression;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 * A compressed array of small integer values.
 * This is typically used for storing lots of nucleotides (0..4) or
 * protein residues (0..22), or quality values (0..63).
 *
 * To improve cache hits, the longs are interleaved into one array.
 * So if there are 3 bits, longs 0, 3, 6, 9... contain the bit 0
 * bit vector, longs 1, 4, 7, 10... contain the bit 1 bit vector,
 * and 2, 5, 8, 11,... contain the bit 2 bit vector.
 *
 * To encourage efficiency, the single byte set method is not supported.
 *
 */
public final class BitwiseByteArray extends ByteArray implements Integrity {

  private static final int BITS_PER_LONG = 64;

  /** The right shift that corresponds to a division by 64. */
  private static final int WHICH_LONG = 6;

  /** The mask that gets the bit position with the long. */
  private static final long WITHIN_LONG = (1L << WHICH_LONG) - 1;

  /** The total number of values stored. */
  private long mSize;

  private long mCapacity;

  /** The number of bits needed to store each value. */
  private final int mBits;

  private static final int ARR_SIZE = 1 << 30;

  private final int mMaxLongsPerArray;

  /**
   * All the data is packed into these longs.
   * To improve cache hits, the longs are interleaved into one array.
   * So if there are 3 bits, longs 0, 3, 6, 9... contain the bit 0
   * bit vector, longs 1, 4, 7, 10... contain the bit 1 bit vector,
   * and 2, 5, 8, 11,... contain the bit 2 bit vector.
   */
  private long[][] mData;

  /** Just for checking that set calls are done in the right order. */
  private long mValuesSet = 0L;

  private final boolean mGrow;

  private long mOffset;

  /**
   * Create an array that grows as necessary.
   * @param bits is the width of each bit-field (must be less than 8).
   */
  public BitwiseByteArray(final int bits) {
    this(0, bits, ARR_SIZE, true);
  }

  //should only be used by tests, dummy is to prevent overloading causing problems
  BitwiseByteArray(long size, int bits, int arrSize, boolean growable) {
    assert 0 <= size;
    assert 1 <= bits && bits <= 7;
    mSize = size;
    mCapacity = Math.max(size, 20);
    mBits = bits;
    mGrow = growable;

    final long longs = numberLongs(mCapacity);
    final long valuesPerArray = arrSize / bits * 64L;
    mMaxLongsPerArray = (int) (valuesPerArray >>> WHICH_LONG) * mBits;  // Each array should be at most this long. This is also a multiple of mBits
    final int numArrays = (longs % mMaxLongsPerArray == 0) ? (int) (longs / mMaxLongsPerArray) : (int) (longs / mMaxLongsPerArray) + 1;
    mData = new long[numArrays][];
    int i = 0;
    long sizeLeft = longs;
    while (sizeLeft > 0) {
      final long cSize = Math.min(mMaxLongsPerArray, sizeLeft);
      mData[i++] = new long[(int) cSize];
      sizeLeft -= cSize;
    }
  }

  /**
   * Create a compressed array of bytes.
   *
   * @param size is the number of values that can be stored.
   * @param bits is the width of each bit-field (must be less than 8).
   */
  public BitwiseByteArray(final long size, final int bits) {
    this(size, bits, ARR_SIZE, false);
  }

  private long numberLongs(long size) {
    return ((size + BITS_PER_LONG - 1) / BITS_PER_LONG) * mBits;
  }

  /**
   * @see com.rtg.util.integrity.Integrity#integrity()
   * @return true
   */
  @Override
  public boolean integrity() {
    Exam.assertTrue(0 <= mSize);
    Exam.assertTrue(0 < mBits);
    Exam.assertNotNull(mData);
    for (int i = 0; i < mData.length; i++) {
      Exam.assertNotNull("" + i, mData[i]);
    }
    //Assert.assertTrue(mData.length >= mBits * (mCapacity / BITS_PER_LONG));
    //Assert.assertTrue(mSize <= BITS_PER_LONG * mData[0].length());
    Exam.assertTrue(0 <= mValuesSet && mValuesSet <= mSize);
    return true;
  }

  /**
   * @see com.rtg.util.integrity.Integrity#integrity()
   * @return true
   */
  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

  @Override
  public byte get(final long offset) {
    assert offset < mValuesSet : "offset: " + offset + " mValuesSet: " + mValuesSet ;  // only read values that have been set
    final long adjOffset = mOffset + offset;
    final long whichLong = (adjOffset >>> WHICH_LONG) * mBits;
    final int whichBit = (int) (adjOffset & WITHIN_LONG);
    final int whichArr = (int) (whichLong / mMaxLongsPerArray);
    final int accessLong = (int) (whichLong - (long) whichArr * mMaxLongsPerArray);
    int result = 0;
    for (int b = 0; b < mBits; b++) {
      //final long longValue = mData[b].get(whichLong);
      final long longValue = mData[whichArr][accessLong + b];
      result = (result << 1) | (int) ((longValue >> whichBit) & 1);
    }
    return (byte) result;
  }

  @Override
  public void get(byte[] dest, long offset, int count) {
    get(dest, offset, 0, count);
  }

  /**
   * like <code>get</code> but with ability to choose destination offset
   * @param dest destination array
   * @param srcOffset offset within source data
   * @param destOffset offset within destination array
   * @param length number of values to read
   */
  public void get(final byte[] dest, final long srcOffset, int destOffset, final int length) {
    assert srcOffset + length <= mValuesSet; // only read values that have been set
    if (length == 0) {
      return;
    }
    final long adjOffset = mOffset + srcOffset;
    //general idiom for this class is as follows:
    //whichLong refers to the logical long containing the first bit of the value we want
    //whichBit refers to the bit in which the value we want is stored (1 bit in each long between whichLong and whichLong + mBits)
    //whichArr refers to the subArray in which the value is contained (each long containing part of the value must reside in the same subArray)
    //nextSwitch refers to the value whichLong should reach before incrementing whichArr by 1
    //accessLong refers to the index of "whichLong" within the current array
    long whichLong = (adjOffset >>> WHICH_LONG) * mBits;
    int whichBit = (int) (adjOffset & WITHIN_LONG);
    int whichArr = (int) (whichLong / mMaxLongsPerArray);
    long nextSwitch = ((long) whichArr + 1) * mMaxLongsPerArray;
    int accessLong = (int) (whichLong - (long) whichArr * mMaxLongsPerArray);
    if (mBits == 3) {
      // special in-lined version of the code for the DNA case.
      long bits0 = mData[whichArr][accessLong];
      long bits1 = mData[whichArr][accessLong + 1];
      long bits2 = mData[whichArr][accessLong + 2];
      for (int pos = destOffset; pos < destOffset + length; pos++) {
        final long bit0 = (bits0 >>> whichBit) & 1;
        final long bit1 = (bits1 >>> whichBit) & 1;
        final long bit2 = (bits2 >>> whichBit) & 1;
        dest[pos] = (byte) ((bit0 << 2) | (bit1 << 1) | bit2);
        // now move along to the next bit
        whichBit++;
        if (whichBit == BITS_PER_LONG && pos < destOffset + length - 1) {
          whichBit = 0;
          whichLong += mBits;
          accessLong += mBits;
          if (whichLong == nextSwitch) {
            accessLong = 0;
            whichArr++;
            nextSwitch += mMaxLongsPerArray;
          }
          bits0 = mData[whichArr][accessLong];
          bits1 = mData[whichArr][accessLong + 1];
          bits2 = mData[whichArr][accessLong + 2];
        }
      }
    } else {
      for (int pos = destOffset; pos < destOffset + length; pos++) {
        int value = 0;
        for (int b = 0; b < mBits; b++) {
          //final long bits = mData[b].get(whichLong);
          final long bits = mData[whichArr][accessLong + b];
          value = (value << 1) | (int) ((bits >>> whichBit) & 1);
        }
        dest[pos] = (byte) value;
        // now move along to the next bit
        whichBit++;
        if (whichBit == BITS_PER_LONG) {
          whichBit = 0;
          whichLong += mBits;
          accessLong += mBits;
          if (whichLong == nextSwitch) {
            accessLong = 0;
            whichArr++;
            nextSwitch += mMaxLongsPerArray;
          }
        }
      }
    }
  }

  /**
   * Not supported.
   */
  @Override
  public void set(final long offset, final byte value) {
    throw new UnsupportedOperationException("not supported");
  }

  @Override
  public void set(long offset, byte[] buffer, int count) {
    set(offset, buffer, 0, count);
  }

  private void increaseCapacity() {
    final long newCapacity = (mCapacity * 3) / 2 + 1;
    final long longs = numberLongs(newCapacity);
    long remaining = longs - numberLongs(mCapacity);
    int i = mData.length - 1;
    if (mData[i].length != mMaxLongsPerArray && remaining > 0) {
      final int toExpand = (int) Math.min(mMaxLongsPerArray - mData[i].length, remaining);
      mData[i] = Arrays.copyOf(mData[i], mData[i].length + toExpand);
      remaining -= toExpand;
    }
    if (remaining != 0) {
      final int numArrays = (longs % mMaxLongsPerArray == 0) ? (int) (longs / mMaxLongsPerArray) : (int) (longs / mMaxLongsPerArray) + 1;
      mData = Arrays.copyOf(mData, numArrays);
      i++;
      while (remaining > 0) {
        final long cSize = Math.min(mMaxLongsPerArray, remaining);
        mData[i++] = new long[(int) cSize];
        remaining -= cSize;
      }
    }
    mCapacity = newCapacity;
  }

  //private byte[] mCopy;
  /**
   * Packs <code>data[0 .. length - 1]</code> into this byte array.
   * WARNING: Calls to set should be done with increasing <code>offset</code>
   * values, and a given location should never be set twice.
   *
   * @param offset the logical start position to start storing.
   * @param data the source values
   * @param bOffset position into data to start copying from
   * @param length how many values to copy.
   */
  @Override
  public void set(final long offset, final byte[] data, final int bOffset, final int length) {
    //mCopy = data.clone();
    if (offset < mValuesSet) {
      // each value should be set only once.
      throw new IllegalArgumentException("BitwiseByteArray.set called out of order");
    } else {
      if (mGrow) {
        while (offset + length > mCapacity) {
          increaseCapacity();
        }
        mSize += length;
      }
      assert offset + length <= mCapacity;
      mValuesSet = offset + length; // this number of bits have now been set
    }
    long whichLong = (offset >>> WHICH_LONG) * mBits;
    int whichBit = (int) (offset & WITHIN_LONG);
    int whichArr = (int) (whichLong / mMaxLongsPerArray);
    long nextSwitch = ((long) whichArr + 1) * mMaxLongsPerArray;
    int accessLong = (int) (whichLong - (long) whichArr * mMaxLongsPerArray);
    for (int pos = bOffset; pos < bOffset + length; pos++) {
      int value = data[pos];
      for (int b = mBits - 1; b >= 0; b--) {
        //final long old = mData[b].get(whichLong);
        //mData[b].set(whichLong, old | ((value & 1L) << whichBit));
        mData[whichArr][accessLong + b] |= (value & 1L) << whichBit;
        value = value >> 1;
      }
      // now move along to the next bit
      whichBit++;
      if (whichBit == BITS_PER_LONG) {
        whichBit = 0;
        whichLong += mBits;
        accessLong += mBits;
        if (whichLong == nextSwitch) {
          accessLong = 0;
          whichArr++;
          nextSwitch += mMaxLongsPerArray;
        }
      }
    }
  }

  @Override
  public long length() {
    return mSize;
  }

  @Override
  public long bytes() {
    //return mData[0].bytes() * mBits;
    long val = 0;
    for (final long[] l : mData) {
      val += l.length * 8L;
    }
    return val;
  }

  /**
   * load directly from disk
   * @param file file containing compressed values
   * @param startVal first value to load
   * @param endVal last value to load
   * @param range number of possible unique values
   * @return array containing values
   * @throws IOException if an IO error occurs
   */
  public static BitwiseByteArray loadBitwise(File file, long startVal, long endVal, int range) throws IOException {
    //we need mBits longs for each value, but BITS_PER_LONG values are stored in the same space.
    //necessarily we need to include all values in the same BITS_PER_LONG set
    final long adjStartVal = startVal / BITS_PER_LONG * BITS_PER_LONG;
    final long adjEndVal = endVal % BITS_PER_LONG == 0 ? endVal : (endVal / BITS_PER_LONG + 1) * BITS_PER_LONG;
    final BitwiseByteArray ret = new BitwiseByteArray(adjEndVal - adjStartVal, CompressedByteArray.minBits(range));
    ret.mOffset = startVal - adjStartVal;
    ret.mSize = endVal - startVal;
    ret.mValuesSet = adjEndVal - adjStartVal;
    final long startSeekPos = adjStartVal / BITS_PER_LONG * ret.mBits * 8;
    final long endSeekPos = adjEndVal / BITS_PER_LONG * ret.mBits * 8;
    final ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.BIG_ENDIAN);
    int dataArray = 0;
    long absolutePosition = startSeekPos;
    try (FileInputStream stream = new FileInputStream(file)) {
      try (FileChannel channel = stream.getChannel()) {
        if (startSeekPos > 0) {
          channel.position(startSeekPos);
        }
        int dFrom = 0;
        while (absolutePosition < endSeekPos && channel.read(buf) != -1) {
          buf.flip();
          final long maxLimit = endSeekPos - absolutePosition;
          final int size = (int) Math.min((long) buf.limit(), maxLimit); //limit is int, so result should be int
          int numLongs = size / 8;
          int dTo = Math.min(ret.mData[dataArray].length, dFrom + numLongs);
          while (numLongs > 0) {
            buf.asLongBuffer().get(ret.mData[dataArray], dFrom, dTo - dFrom);
            final int dataAdvance = (dTo - dFrom) * 8;
            buf.position(dataAdvance); //unfortunately positions are independent between the buffers, even though data isn't, position was 0
            absolutePosition += dataAdvance;
            //ByteArrayIOUtils.convertToLongArray(buf, sFrom, sTo, mData[dataArray], dFrom, dTo);
            numLongs -= dTo - dFrom;
            if (numLongs > 0) {
              dataArray++;
              dFrom = 0;
              dTo = Math.min(ret.mData[dataArray].length, dFrom + numLongs);
            }
            dFrom += dTo - dFrom;
          }
          //          mValuesSet = mSize;
          buf.compact();
        }
      }
    }

    return ret;
  }

  /**
   * This method dumps the contents for testing purposes
   * @param dos output stream
   * @param values I do not know.
   * @throws IOException if the data could not be dumped
   */
  public void dumpCompressedValues(DataOutputStream dos, long values) throws IOException {
    final long whichLong = ((values - 1) >>> WHICH_LONG) * mBits;
    int whichArr = 0;
    long nextSwitch = mMaxLongsPerArray;
    int accessLong = 0;
    for (long i = 0; i < whichLong + mBits; i++) {
      if (i == nextSwitch) {
        accessLong = 0;
        whichArr++;
        nextSwitch += mMaxLongsPerArray;
      }
      dos.writeLong(mData[whichArr][accessLong++]);
    }
  }
}
