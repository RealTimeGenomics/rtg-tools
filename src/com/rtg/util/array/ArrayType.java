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
package com.rtg.util.array;

import com.rtg.util.MathUtils;
import com.rtg.util.array.bitindex.BitCreate;
import com.rtg.util.array.bitindex.BitIndex;


/**
 * Provides a way of keeping a handle to be used for creating arrays
 * of specified types. Enables memory estimation without actually
 * creating the arrays.
 */
public abstract class ArrayType {

  /** Similar to <code>PackedArray</code>, but the number of bits is rounded up to a power of 2. */
  static class ArrayTypeBits extends ArrayType {

    private final int mBits;

    /**
     * @param bits number of bits to be stored for each value.
     */
    ArrayTypeBits(final int bits) {
      super("Bits:" + BitIndex.roundUpBits(bits));
      mBits = BitIndex.roundUpBits(bits);
    }

    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return BitCreate.createIndex(length, mBits);
    }

    @Override
    public long bytes(final long size) {
      final int itemsPerWord = Long.SIZE / mBits;
      final long words = (size / itemsPerWord) + 1;
      return words * 8;
    }
  }

  /** Provides information about arrays where the underlying unit is an 8 byte long. */
  public static final ArrayType LONG = new ArrayType("LONG", 8) {
    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return new com.rtg.util.array.longindex.LongChunks(length);
    }
  };

  /** Provides information about arrays where the underlying unit is a 4 byte int. */
  public static final ArrayType INTEGER = new ArrayType("INTEGER", 4) {
    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return new com.rtg.util.array.intindex.IntChunks(length);
    }
  };

  /** Provides information about arrays where the underlying unit is a 2 byte short. */
  public static final ArrayType SHORT = new ArrayType("SHORT", 2) {
    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return new com.rtg.util.array.shortindex.ShortChunks(length);
    }
  };

  /** Provides information about arrays where the underlying unit is a single byte. */
  public static final ArrayType BYTE = new ArrayType("BYTE", 1) {
    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return new com.rtg.util.array.byteindex.ByteChunks(length);
    }
  };

  /** Provides information about arrays where nothing is stored. */
  public static final ArrayType ZERO = new ArrayType("ZERO", 0) {
    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return new com.rtg.util.array.zeroindex.ZeroIndex(length, 0);
    }
  };

  /**
   * Handle for variable number of bits.
   */
  public static class ArrayTypeBit extends ArrayType {
    private final int mBits;

    /**
     * @param bits number of bits to be stored in array.
     */
    public ArrayTypeBit(final int bits) {
      super("Bits:" + bits);
      mBits = bits;
    }

    @Override
    public long bytes(final long size) {
      final long totalBits = size * mBits;
      return (totalBits + 63) / 64;
    }

    @Override
    public ExtensibleIndex createUnsigned(final long length) {
      return BitCreate.createIndex(length, mBits);
    }

  }

  /**
   * Get an array type that is capable of storing the specified number of bits
   * in each value. It tries to return the smallest possible result.
   * @param bits to be stored in each unit.
   * @return an array type capable of storing the requested number of bits.
   */
  public static ArrayType bestForBitsAndSafeFromWordTearing(final int bits) {
    if (bits > 64) {
      throw new RuntimeException("Cannot store more than 64 bits per entry bits=" + bits);
    }
    if (bits > 32) {
      return LONG;
    }
    if (bits > 16) {
      return INTEGER;
    }
    if (bits > 8) {
      return SHORT;
    }
    if (bits > 0) {
      return BYTE;
      //return new ArrayTypeBits(bits);
    }
    if (bits == 0) {
      return ZERO;
    }
    throw new RuntimeException("bits should be positive:" + bits);
  }

  /**
   * Get an array type that is capable of storing the specified number of bits
   * in each value. It tries to return the smallest possible result.
   * This uses <code>ArrayTypeBits</code> where possible - there may be performance implications to this.
   * Use with care.
   * @param bits to be stored in each unit.
   * @return an array type capable of storing the requested number of bits.
   */
  public static ArrayType bestForBitsSpaceEfficientButNotSafeFromWordTearing(final int bits) {
    if (bits > 64) {
      throw new RuntimeException("Cannot store more than 64 bits per entry bits=" + bits);
    }
    if (bits > 32) {
      return LONG;
    }
    if (bits > 16) {
      return INTEGER;
    }
    if (bits > 8) {
      return SHORT;
    }
    if (bits == 8) {
      return BYTE;
    }
    if (bits > 0) {
      return new ArrayTypeBits(bits);
    }
    if (bits == 0) {
      return ZERO;
    }
    throw new RuntimeException("bits should be positive:" + bits);
  }

  /**
   * Get an array type that is capable of storing pointers into an array of the specified length.
   * It tries to return the smallest possible result.
   * @param length of the destination array.
   * @return an array type capable of storing the requested pointers.
   */
  public static ArrayType bestForLength(final long length) {
    if (length > (1L << 62)) {
      return bestForBitsAndSafeFromWordTearing(64);
    }
    if (length > 0) {
      final int bits = MathUtils.ceilPowerOf2Bits(length);
      return bestForBitsAndSafeFromWordTearing(bits);
    }
    if (length == 0) {
      return bestForBitsAndSafeFromWordTearing(0);
    }
    throw new RuntimeException("length should be positive:" + length);
  }

  /** Size of underlying storage units in bytes. */
  private final int mUnitSize;

  private final String mName;

  protected ArrayType(final String name, final int unitSize) {
    mUnitSize = unitSize;
    mName = name;
  }

  protected ArrayType(final String name) {
    this(name, 0);
  }

  @Override
  public String toString() {
    return mName;
  }

  /**
   * Create an unsigned array of the specified length.
   * @param length of the new array.
   * @return an unsigned array of the specified length.
   */
  public abstract ExtensibleIndex createUnsigned(long length);

  /**
   * Return the memory that would be consumed by an array of the specified size.
   * @param size length of the fictitious array.
   * @return the total memory consumed by an array of the specified size.
   */
  public long bytes(final long size) {
    if (size < 0) {
      throw new IllegalArgumentException("size must be positive:" + size);
    }
    return size * mUnitSize;
  }

}

