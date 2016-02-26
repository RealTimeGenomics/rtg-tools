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

/**
 * Contains constants and utilities for dealing with Complete Genomics reads
 */
public final class CgUtils {

  /** Raw length of CG v1 reads */
  public static final int CG_RAW_READ_LENGTH = 35;

  /** The position of the overlap in a CG v1 read (left arm). */
  public static final int CG_OVERLAP_POSITION = 5;

  /** Raw length of Complete Genomics v2 reads */
  public static final int CG2_RAW_READ_LENGTH = 29;

  /** Length of Complete Genomics v2 reads, when it contains the single padding N */
  public static final int CG2_PADDED_LENGTH = CG2_RAW_READ_LENGTH + 1;

  /** The position of the overlap in a CG v2 read (both arms). */
  public static final int CG2_OVERLAP_POSITION = 10;

  /** The position of the padding N base in padded v2 reads */
  public static final int CG2_PAD_POSITION = 19;
  private static final int CG2_PAD_POSITION_REV = 10;

  /** Most likely overlap. */
  public static final int CG_EXPECTED_OVERLAP = 2;
  /** Most likely length of the small gap. */
  public static final int CG_EXPECTED_SMALL_GAP = 0;
  /** Most likely length of the large gap. */
  public static final int CG_EXPECTED_LARGE_GAP = 6;

  /** Most likely overlap, v2. */
  public static final int CG2_EXPECTED_OVERLAP = 3;

  /** Most likely length of sum of gaps and overlap. */
  public static final int CG_EXPECTED_LENGTH_OFFSET = CG_EXPECTED_SMALL_GAP + CG_EXPECTED_LARGE_GAP - CG_EXPECTED_OVERLAP;
  /** Most likely template length spanned by whole CG read. */
  public static final int CG_EXPECTED_LENGTH = CG_RAW_READ_LENGTH + CG_EXPECTED_LENGTH_OFFSET;
  /** Most likely length of sum of gaps and overlap. */
  public static final int CG2_EXPECTED_LENGTH_OFFSET = -CG2_EXPECTED_OVERLAP;
  /** Most likely template length spanned by whole CG v2 read. */
  public static final int CG2_EXPECTED_LENGTH = CG2_RAW_READ_LENGTH + CG2_EXPECTED_LENGTH_OFFSET;


  private CgUtils() {
  }

  /**
   * Strip single padding base that Complete Genomics v2 reads contain, if present.
   * @param seq input data to remove padding from. May be either DNA or quality data.
   * @param forward true if the data is in the normal, forward orientation
   * @return the data with any padding removed. If the data is not v2, returns the original array.
   */
  public static byte[] unPad(byte[] seq, boolean forward) {
    if (seq != null && seq.length == CG2_PADDED_LENGTH) {
      final int pos = forward ? CG2_PAD_POSITION : CG2_PAD_POSITION_REV;
//      assert seq[pos] == 0 || seq[pos] == 'N' || seq[pos] == FastaUtils.asciiToRawQuality('!');
      final byte[] result = new byte[seq.length - 1];
      System.arraycopy(seq, 0, result, 0, pos);
      System.arraycopy(seq, pos + 1, result, pos, seq.length - pos - 1);
      return result;
    } else {
      return seq;
    }
  }

  /**
   * Strip single padding base that Complete Genomics v2 reads contain, if present.
   * @param src input data to remove padding from. May be either DNA or quality data.
   * @param start start position within the source array to begin copying from
   * @param dest destination array where unpadded data is copied
   */
  public static void unPad(byte[] src, int start, byte[] dest) {
    System.arraycopy(src, start, dest, 0, CG2_PAD_POSITION);
    System.arraycopy(src, start + CG2_PAD_POSITION + 1, dest, CG2_PAD_POSITION, CG2_RAW_READ_LENGTH - CG2_PAD_POSITION);
  }
}
