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
package com.rtg.util.io;

/**
 * Methods for dealing with byte arrays read from binary files.
 * Operations assume big-endian byte order, unless otherwise specified.
 */
public final class ByteArrayIOUtils {

  private ByteArrayIOUtils() { }

  private static void checkSource(final int length, final int mod) {
    if (length % mod != 0) {
      throw new IllegalArgumentException("Source data length needs to be multiple of " + mod + " was: " + length);
    }
  }

  private static void checkDestination(final int sLength, final int dLength, final int mod) {
    final int lLength = sLength / mod;
    if (dLength < lLength) {
      throw new IllegalArgumentException("Destination length needs to be at least: " + lLength + " was: " + dLength);
    }
  }

  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param vals source data
   * @return converted data
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 8
   */
  public static long[] convertToLongArray(final byte[] vals) {
    checkSource(vals.length, 8);
    final long[] dest = new long[vals.length / 8];
    convertToLongArrayInternal(vals, 0, vals.length, dest, 0);
    return dest;
  }

  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param vals source data
   * @param dest array to put longs into
   * @return space taken in destination
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 8
   * or length of <code>dest</code> is not length of <code>vals</code> divided by 8 or greater
   */
  public static int convertToLongArray(final byte[] vals, final long[] dest) {
    checkSource(vals.length, 8);
    checkDestination(vals.length, dest.length, 8);
    return convertToLongArrayInternal(vals, 0, vals.length, dest, 0);
  }

  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param src source data
   * @param sFrom starting source position
   * @param sTo ending source position (excl)
   * @param dest array to put longs into
   * @param dFrom starting destination position
   * @param dTo ending destination position (excl)
   * @return space taken in destination
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 8
   * or length of <code>dest</code> is not length of <code>vals</code> divided by 8 or greater
   */
  public static int convertToLongArray(final byte[] src, final int sFrom, final int sTo, final long[] dest, final int dFrom, final int dTo) {
    final int length = sTo - sFrom;
    checkSource(length, 8);
    checkDestination(length, dTo - dFrom, 8);
    return convertToLongArrayInternal(src, sFrom, sTo, dest, dFrom);
  }

  private static int convertToLongArrayInternal(final byte[] src, final int sFrom, final int sTo, final long[] dest, final int dFrom) {
    int i, j;
    for (i = dFrom, j = sFrom; j < sTo; i++, j += 8) {
      dest[i] = ((long) src[j] << 56)
                + ((long) (src[j + 1] & 0xFF) << 48)
                + ((long) (src[j + 2] & 0xFF) << 40)
                + ((long) (src[j + 3] & 0xFF) << 32)
                + ((long) (src[j + 4] & 0xFF) << 24)
                + ((src[j + 5] & 0xFF) << 16)
                + ((src[j + 6] & 0xFF) << 8)
                + (src[j + 7] & 0xFF);
    }
    return i - dFrom;
  }

  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param vals source data
   * @return converted data
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 4
   */
  public static int[] convertToIntArray(final byte[] vals) {
    checkSource(vals.length, 4);
    final int[] dest = new int[vals.length / 4];
    convertToIntArrayInternal(vals, 0, vals.length, dest, 0);
    return dest;
  }

  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param vals source data
   * @param dest destination array
   * @return space taken in destination
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 4
   * or length of <code>dest</code> is not length of <code>vals</code> divided by 4 or greater
   */
  public static int convertToIntArray(final byte[] vals, final int[] dest) {
    checkSource(vals.length, 4);
    checkDestination(vals.length, dest.length, 4);
    return convertToIntArrayInternal(vals, 0, vals.length, dest, 0);
  }

  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param src source data
   * @param sFrom starting source
   * @param sTo ending source (excl)
   * @param dest destination array
   * @param dFrom starting position in destination
   * @param dTo ending position in destination (excl)
   * @return space taken in destination
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 4
   * or length of <code>dest</code> is not length of <code>vals</code> divided by 4 or greater
   */
  public static int convertToIntArray(final byte[] src, final int sFrom, final int sTo, final int[] dest, final int dFrom, final int dTo) {
    final int sLength = sTo - sFrom;
    checkSource(sLength, 4);
    checkDestination(sLength, dTo - dFrom, 4);
    return convertToIntArrayInternal(src, sFrom, sTo, dest, dFrom);
  }

  private static int convertToIntArrayInternal(final byte[] vals, final int sFrom, final int sTo, final int[] dest, final int start) {
    int i, j;
    for (i = start, j = sFrom; j < sTo; i++, j += 4) {
      dest[i] = (vals[j] << 24)
                + ((vals[j + 1] & 0xFF) << 16)
                + ((vals[j + 2] & 0xFF) << 8)
                + (vals[j + 3] & 0xFF);
    }
    return i - start;
  }


  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming big-endian byte order.
   * @param src source data
   * @param sFrom starting source
   * @param sTo ending source (excl)
   * @param dest destination array
   * @param dFrom starting destination
   * @param dTo ending destination (excl)
   * @return space taken in destination
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 4
   * or length of <code>dest</code> is not length of <code>vals</code> divided by 4 or greater
   */
  public static int convertToIntInLongArray(final byte[] src, final int sFrom, final int sTo, final long[] dest, final int dFrom, final int dTo) {
    final int sLength = sTo - sFrom;
    checkSource(sLength, 4);
    checkDestination(sLength, dTo - dFrom, 4);
    return convertToIntInLongArrayInternal(src, sFrom, sTo, dest, dFrom);
  }

  private static int convertToIntInLongArrayInternal(final byte[] vals, final int sFrom, final int sTo, final long[] dest, final int start) {
    int i, j;
    for (i = start, j = sFrom; j < sTo; i++, j += 4) {
      dest[i] = (vals[j] << 24)
                + ((vals[j + 1] & 0xFF) << 16)
                + ((vals[j + 2] & 0xFF) << 8)
                + (vals[j + 3] & 0xFF);
    }
    return i - start;
  }

  /**
   * <code>bytes</code> into single <code>int</code>
   * @param vals raw data
   * @param from offset in array
   * @return int
   */
  public static int bytesToIntBigEndian(final byte[] vals, final int from) {
    return (vals[from] << 24)
          + ((vals[from + 1] & 0xFF) << 16)
          + ((vals[from + 2] & 0xFF) << 8)
          + (vals[from + 3] & 0xFF);
  }

  /**
   * <code>bytes</code> into single <code>int</code>
   * @param vals raw data
   * @param from offset in array
   * @return int
   */
  public static int bytesToIntLittleEndian(final byte[] vals, final int from) {
    return (vals[from + 3] << 24)
          + ((vals[from + 2] & 0xFF) << 16)
          + ((vals[from + 1] & 0xFF) << 8)
          + (vals[from] & 0xFF);
  }

  /**
   * <code>bytes</code> into single <code>long</code>
   * @param vals raw data
   * @param from offset in array
   * @return long
   */
  public static long bytesToLongLittleEndian(final byte[] vals, final int from) {
    final int bot = (vals[from + 3] << 24)
          + ((vals[from + 2] & 0xFF) << 16)
          + ((vals[from + 1] & 0xFF) << 8)
          + (vals[from] & 0xFF);
    final int top = (vals[from + 7] << 24)
          + ((vals[from + 6] & 0xFF) << 16)
          + ((vals[from + 5] & 0xFF) << 8)
          + (vals[from + 4] & 0xFF);
    return ((long) top << 32) + ((long) bot & 0xFFFFFFFFL);
  }

  /**
   * <code>bytes</code> into single <code>short</code>
   * @param vals raw data
   * @param from offset in array
   * @return int
   */
  public static int bytesToShortLittleEndian(final byte[] vals, final int from) {
    return ((vals[from + 1] & 0xFF) << 8)
            + (vals[from] & 0xFF);
  }

  /**
   * Single <code>int</code> into byte array
   * @param i data
   * @param dest output data
   * @param offset where to write to
   */
  public static void intToBytesLittleEndian(final int i, final byte[] dest, final int offset) {
    dest[offset] = (byte) (i & 0xFF);
    dest[offset + 1] = (byte) ((i >> 8) & 0xFF);
    dest[offset + 2] = (byte) ((i >> 16) & 0xFF);
    dest[offset + 3] = (byte) ((i >> 24) & 0xFF);
  }

  /**
   * Single <code>long</code> into byte array
   * @param i data
   * @param dest output data
   * @param offset where to write to
   */
  public static void longToBytesLittleEndian(final long i, final byte[] dest, final int offset) {
    dest[offset] = (byte) (i & 0xFF);
    dest[offset + 1] = (byte) ((i >> 8) & 0xFF);
    dest[offset + 2] = (byte) ((i >> 16) & 0xFF);
    dest[offset + 3] = (byte) ((i >> 24) & 0xFF);
    dest[offset + 4] = (byte) ((i >> 32) & 0xFF);
    dest[offset + 5] = (byte) ((i >> 40) & 0xFF);
    dest[offset + 6] = (byte) ((i >> 48) & 0xFF);
    dest[offset + 7] = (byte) ((i >> 56) & 0xFF);
  }



  /**
   * Converts <code>byte[]</code> to <code>long[]</code>, assuming little-endian byte order.
   * @param vals source data
   * @param dest array to put longs into
   * @return space taken in destination
   * @throws IllegalArgumentException If length of <code>vals</code> is not a multiple of 8
   * or length of <code>dest</code> is not length of <code>vals</code> divided by 8 or greater
   */
  public static int convertToLongArrayLittleEndian(final byte[] vals, final long[] dest) {
    checkSource(vals.length, 8);
    checkDestination(vals.length, dest.length, 8);
    return convertToLongArrayLittleEndianInternal(vals, 0, vals.length, dest, 0);
  }

  private static int convertToLongArrayLittleEndianInternal(final byte[] src, final int sFrom, final int sTo, final long[] dest, final int dFrom) {
    int i, j;
    for (i = dFrom, j = sFrom; j < sTo; i++, j += 8) {
      dest[i] = ((long) src[j + 7] << 56)
                + ((long) (src[j + 6] & 0xFF) << 48)
                + ((long) (src[j + 5] & 0xFF) << 40)
                + ((long) (src[j + 4] & 0xFF) << 32)
                + ((long) (src[j + 3] & 0xFF) << 24)
                + ((src[j + 2] & 0xFF) << 16)
                + ((src[j + 1] & 0xFF) << 8)
                + (src[j] & 0xFF);
    }
    return i - dFrom;
  }
}
