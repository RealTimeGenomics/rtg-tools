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

import com.rtg.util.array.CommonIndex;
import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.array.longindex.LongCreate;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 * A compressed array of small integer values.
 * This is typically used for storing lots of nucleotides (0..4) or
 * protein residues (0..22), or quality values (0..63).
 * Internally it uses an array of longs, packing as many values as possible into each long.
 *
 * As an additional sanity check, callers are required to set each byte value
 * at most once (in increasing offset positions), so the single byte set
 * method is not supported.
 *
 * WARNING: this implementation clips values set if they fall above the given range.
 * i.e. if range is 22 and 30 is added it will be stored as 21.
 *
 */
public final class CompressedByteArray extends ByteArray implements Integrity {

  /** The total number of values stored. */
  private long mSize;

  private final boolean mGrow;

  /** All the data is packed into these longs. */
  private final CommonIndex mData;

  /** How many values we compress into each bit-field (via multiplication). */
  private final int mPerBitfield;

  /** How many bit-fields we can squash into each long (via bit shifting). */
  private final int mNumBitfields;

  /** The number of values packed into each long. */
  private final int mPerLong;

  /** Each bit-field of this width contains <code>mPerByte</code> values. */
  private final int mBits;

  /** Equals <code>Math.pow(2, mBits) - 1</code> */
  private final int mMask;

  /** The allowable values are <code>0 .. mRange-1</code>. */
  private final int mRange;

  /** Powers of <code>mRange</code>. */
  private final int[] mRangePowers;

  /** Just for checking that set calls are done in the right order. */
  private long mValuesSet = 0L;

  /** first externally visible value */
  private long mOffset;

  /**
   * A lookup table: given a bit-field with value <code>val</code>,
   * <code>mValue[i][v]</code> is the value of the <code>i'th</code> field in that bit-field.
   */
  private final byte[][] mValue;

  /**
   * Create a compressed array of bytes.
   *
   * @param size is the number of values that can be stored.
   * @param range means that integer values <code>0 .. range-1</code> can be stored.  Allowable values are 2..128.
   * @param pack is how many values to pack into each bit-field.
   * @param bits is the width of each bit-field (maximum 12).
   * @param grow set to true to grow array as necessary
   */
  public CompressedByteArray(long size, int range, int pack, int bits, boolean grow) {
    assert 0 <= size;
    assert 1 < range && range <= 128;
    assert 1 <= pack;
    assert 1 <= bits && bits <= 12; // so the lookup tables are not too big
    assert Math.pow(range, pack) <= Math.pow(2, bits);
    mRange = range;
    mPerBitfield = pack;
    mBits = bits;
    mMask = (int) Math.pow(2, mBits) - 1;
    mNumBitfields = 64 / mBits;
    mPerLong = mPerBitfield * mNumBitfields;
    mGrow = grow;
    if (grow) {
      mSize = 0;
      mData = LongCreate.createExtensibleIndex();
    } else {
      mSize = size;
      mData = LongCreate.createIndex(size / mPerLong + 1);
    }

    // precalculate our lookup tables
    mValue = new byte[mPerBitfield][];
    mRangePowers = new int[mPerBitfield];
    for (int withinBitfield = 0; withinBitfield < mPerBitfield; withinBitfield++) {
      mRangePowers[withinBitfield] = (int) Math.pow(mRange, withinBitfield);
      mValue[withinBitfield] = new byte[mMask + 1];
      final int divisor = (int) Math.pow(mRange, withinBitfield);
      for (int i = 0; i <= mMask; i++) {
        mValue[withinBitfield][i] = (byte) (i / divisor % mRange);
      }
    }
  }

  /**
   * Creates a compressed array with good compression for DNA or protein.
   *
   * @param size is the number of values that can be stored.
   * @param range means that integer values <code>0 .. range-1</code> can be stored.  At most 128.
   * @param grow set to true to grow array as necessary
   */
  public CompressedByteArray(long size, int range, boolean grow) {
    this(size, range,
        range == 5 ? 3 : range == 22 ? 2 : 1,
            range == 5 ? 7 : range == 22 ? 9 : minBits(range), grow);
  }

  /**
   * Calculate the number of bits required to store <code>0 .. range-1</code>.
   * @param range a positive integer.
   * @return the number of bits, between 1 and 32.
   */
  public static int minBits(int range) {
    int max = 2;
    int bits = 1;
    while (max > 0) {
      if (range <= max) {
        return bits;
      }
      max = max << 1;
      bits++;
    }
    return 31;
  }

  @Override
  public boolean integrity() {
    assert mMask == (int) Math.pow(2, mBits) - 1;
    Exam.assertEquals(mPerBitfield, mValue.length);
    for (int i = 0; i < mPerBitfield; i++) {
      Exam.assertEquals(mRange, mValue[i].length);
    }
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

  /** @return the range of values that can be stored in each location. */
  public int getRange() {
    return mRange;
  }

  @Override
  public byte get(long offset) {
    assert offset <= mValuesSet;  // only read values that have been set
    final long adjOffset = mOffset + offset;
    final long whichLong = adjOffset / mPerLong;
    long longValue = mData.get(whichLong);
    final int whichBitfield = (int) (adjOffset % mPerLong) / mPerBitfield;
    longValue = longValue >>> (whichBitfield * mBits);
    final int bitField = (int) (longValue & mMask);
    final int withinBitfield = (int) (adjOffset % mPerLong) % mPerBitfield;
    return mValue[withinBitfield][bitField];
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
  public void get(byte[] dest, long srcOffset, int destOffset, int length) {
    assert srcOffset <= mValuesSet;  // only read values that have been set
    final long adjOffset = mOffset + srcOffset;
    long whichLong = adjOffset / mPerLong;
    long longValue = mData.get(whichLong);
    int whichBitfield = (int) (adjOffset % mPerLong) / mPerBitfield;
    longValue = longValue >>> (whichBitfield * mBits);
    int bitField = (int) (longValue & mMask);
    int withinBitfield = (int) (adjOffset % mPerLong) % mPerBitfield;
    for (int i = destOffset; i < destOffset + length; i++) {
      // this line is a faster version of the following two, using a lookup table.
      dest[i] = mValue[withinBitfield][bitField];
      // the slow version...
      //final int divisor = (int) Math.pow(mRange, withinBitfield);
      //dest[i] = (byte) (bitField / divisor % mRange);
      withinBitfield++;
      if (withinBitfield == mPerBitfield) {
        // move along to next bitfield
        withinBitfield = 0;
        whichBitfield++;
        longValue = longValue >>> mBits;
        bitField = (int) (longValue & mMask);
        if (whichBitfield == mNumBitfields) {
          whichBitfield = 0;
          whichLong++;
          longValue = mData.get(whichLong);
          bitField = (int) (longValue & mMask);
        }
      }
    }
  }

  /**
   * Not supported.
   */
  @Override
  public void set(long offset, byte value) {
    throw new RuntimeException("not supported");
  }

  @Override
  public void set(long offset, byte[] buffer, int count) {
    set(offset, buffer, 0, count);
  }

  /**
   * Packs <code>data[0 .. length - 1]</code> into this byte array.
   * WARNING: Calls to set should be done with increasing <code>offset</code>
   * values, and a given location should never be set twice.
   *
   * @param offset the logical start position to start storing.
   * @param data the source values
   * @param bOffset position in data to start copying from
   * @param length how many values to copy.
   */
  @Override
  public void set(long offset, byte[] data, int bOffset, int length) {
    if (offset < mValuesSet) {
      // each value should be set only once.
      throw new RuntimeException("CompressedByteArray.set called out of order");
    }
    if (mGrow) {
      final long lLength = (offset + length) / mPerLong + 1;
      if (lLength > mData.length()) {
        ((ExtensibleIndex) mData).extendTo(lLength);
      }
      mSize += length;
    }
    // we must read the first long, in case it is already partially filled.
    long whichLong = offset / mPerLong;
    long longValue = mData.get(whichLong);
    int whichBitfield = (int) (offset % mPerLong) / mPerBitfield;
    int withinBitfield = (int) (offset % mPerLong) % mPerBitfield;
    for (int i = bOffset; i < bOffset + length; i++) {
      final int mult = mRangePowers[withinBitfield];
      // final int mult = (int) Math.pow(mRange, withinBitfield);
      final byte val = data[i];
      assert 0 <= val && val < mRange : "value: " + val + " i: " + i;
      longValue += (long) (val * mult) << (whichBitfield * mBits);
      withinBitfield++;
      if (withinBitfield == mPerBitfield) {
        withinBitfield = 0;
        whichBitfield++;
        if (whichBitfield == mNumBitfields) {
          mData.set(whichLong, longValue);
          whichBitfield = 0;
          whichLong++;
          longValue = 0L;
        }
      }
    }
    // store the last partial long.
    if (longValue != 0) {
      mData.set(whichLong, longValue);
    }
    mValuesSet = offset + length; // these values have now been set
  }

  @Override
  public long length() {
    return mSize;
  }

  @Override
  public long bytes() {
    return mData.bytes();
  }

  /**
   * read compressed data directly from disk
   * @param file file containing compressed byte array
   * @param startVal first value to read (inclusive)
   * @param endVal last value to read (exclusive)
   * @param range number of unique values possible in data
   * @return the array
   * @throws IOException if an IO error occurs
   */
  public static CompressedByteArray loadCompressed(File file, long startVal, long endVal, int range) throws IOException {
    final int perLong = new CompressedByteArray(0, range, true).mPerLong;
    final long adjStartVal = startVal / perLong * perLong;
    final long adjEndVal = endVal % perLong == 0 ? endVal : (endVal / perLong + 1) * perLong;
    final CompressedByteArray ret = new CompressedByteArray(adjEndVal - adjStartVal, range, false);
    ret.mOffset = startVal - adjStartVal;
    ret.mSize = endVal - startVal;
    ret.mValuesSet = adjEndVal - adjStartVal;
    final ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.BIG_ENDIAN);
    final long startSeekPos = adjStartVal / perLong * 8;
    final long endSeekPos = adjEndVal / perLong * 8;
    final long totLongs = ret.mData.length();
    long startLong = 0;
    try (FileInputStream stream = new FileInputStream(file)) {
      try (FileChannel channel = stream.getChannel()) {
        if (startSeekPos > 0) {
          channel.position(startSeekPos);
        }
        long dataLoadPosition = startSeekPos;
        while (dataLoadPosition < endSeekPos && channel.read(buf) != -1) {
          buf.flip();
          final int size = buf.limit();
          final int numLongs = size / 8;

          for (int i = 0; i < numLongs && startLong + i < totLongs; i++) {
            ret.mData.set(startLong + i, buf.getLong());
          }
          startLong += numLongs;
          final int dataAdvance = numLongs * 8;
          dataLoadPosition += dataAdvance;
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
    final long whichLong = values / mPerLong + 1;
    for (long i = 0; i < whichLong; i++) {
      dos.writeLong(mData.get(i));
    }
  }

}
