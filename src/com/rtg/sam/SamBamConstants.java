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

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Class containing useful constants
 */
@TestClass(value = {"com.rtg.sam.BamReaderTest"})
public final class SamBamConstants {

  private SamBamConstants() {
  }

  /** These flags are defined in the SAM Tools specification. */
  public static final int
      SAM_READ_IS_PAIRED = 0x0001,
      SAM_READ_IS_MAPPED_IN_PROPER_PAIR = 0x0002,
      SAM_READ_IS_UNMAPPED = 0x0004,
      SAM_MATE_IS_UNMAPPED = 0x0008,
      SAM_READ_IS_REVERSE = 0x0010,
      SAM_MATE_IS_REVERSE = 0x0020,
      SAM_READ_IS_FIRST_IN_PAIR = 0x0040,
      SAM_READ_IS_SECOND_IN_PAIR = 0x0080,
      SAM_SECONDARY_ALIGNMENT = 0x0100,
      SAM_PCR_OR_OPTICAL_DUPLICATE = 0x0400;

  /**
   * The fixed field positions in SAM records.
   * We use these constants to access fields in BAM records too.
   */
  public static final int
      QNAME_FIELD = 0, // Query pair NAME if paired; or Query NAME if unpaired
      FLAG_FIELD = 1,  // bitwise FLAG
      RNAME_FIELD = 2, // Reference sequence NAME
      POS_FIELD = 3,   // 1-based leftmost POSition/coordinate of the clipped sequence
      MAPQ_FIELD = 4,  // MAPping Quality - phred-scaled posterior probability that position is incorrect
      CIGAR_FIELD = 5, // extended CIGAR string
      MRNM_FIELD = 6,  // Mate Reference sequence NaMe
      MPOS_FIELD = 7,  // 1-based leftmost Mate POSition of the clipped sequence
      ISIZE_FIELD = 8, // inferred Insert SIZE 5
      SEQ_FIELD = 9,   // query SEQuence; "=" for a match to the reference; n/N/. for ambiguity
      QUAL_FIELD = 10, // query QUALity; ASCII-33 gives the Phred base quality
      ATTRIBUTES_FIELD = 11; // optional TAG:TYPE:VALUE attribute fields start here.


}
