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

import htsjdk.samtools.SAMRecord;

/**
 * Keep the comparison stuff in one place
 */
public final class SamCompareUtils {

  private SamCompareUtils() { }

  private static int reverse3bits(final int x) {
    // Take last three bits abc of x and return bca
    return (0b111_011_101_001_110_010_100_000 >>> ((x & 7) * 3)) & 7;
  }

  /**
   * Compares SAM records on reference index, start position and if paired, mated vs unmated.
   * @param a first record
   * @param b second record
   * @return -1 if first record comes before second record, 1 if the converse. 0 for equal on compared values
   */
  public static int compareSamRecords(SAMRecord a, SAMRecord b) {
    final int thisRef = a.getReferenceIndex() & 0x7FFFFFFF; // makes -1 a bignum
    final int thatRef = b.getReferenceIndex() & 0x7FFFFFFF;
    final int rc = Integer.compare(thisRef, thatRef);
    if (rc != 0) {
      return rc;
    }

    final int ac = Integer.compare(a.getAlignmentStart(), b.getAlignmentStart());
    if (ac != 0) {
      return ac;
    }

    // Do this ... (this one doesn't have same ordering as original)
    //return Integer.compare(~a.getFlags(), ~b.getFlags());

    // or this ...
    // Following compares READ_PAIRED_FLAG, PORPER_PAIRED_FLAG, UNMAPPED_FLAG in that order.
    // The ^3 toggles values to get the ordering we require
    // The reverse makes sure comparison is in the order we require
    return Integer.compare(reverse3bits(a.getFlags() ^ 3), reverse3bits(b.getFlags() ^ 3));

    // or this ...

//    final int rpc = Boolean.compare(b.getReadPairedFlag(), a.getReadPairedFlag());
//    if (rpc != 0) {
//      return rpc;
//    }
//
//    if (a.getReadPairedFlag()) {
//      assert b.getReadPairedFlag();
//      final int mc = Boolean.compare(b.getProperPairFlag(), a.getProperPairFlag());
//      if (mc != 0) {
//        return mc;
//      }
//    }
//
//    return Boolean.compare(a.getReadUnmappedFlag(), b.getReadUnmappedFlag());
  }
}
