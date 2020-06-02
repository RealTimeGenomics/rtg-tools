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

import java.util.HashSet;

import htsjdk.samtools.SAMRecord;

/**
 * A SAM filter which removes duplicate entries of reads which have not been mapped as primary.
 * This is intended to use when mapping from RTG-produced SAM/BAM output (we don't output a single primary alignment
 * for all alignments, due to lack of build-on-reference)
 * Uses quite a bit of memory (data dependent).
 */
public class DuplicateSamFilter implements SamFilter {

  /* set contains hashes of read names (with left/right arm-ness taken into account as well) */
  private final HashSet<Long> mReadNameSet = new HashSet<>();

  @Override
  public boolean acceptRecord(SAMRecord rec) {
    if (rec == null || rec.getReadName() == null) {
      return false;
    }
    if (!rec.isSecondaryAlignment()) {
      return true;
    }
    final long hash = internalHash(rec.getReadName(), !rec.getReadPairedFlag() || rec.getFirstOfPairFlag());
    if (mReadNameSet.contains(hash)) {
      return false;
    }
    mReadNameSet.add(hash);
    return true;
  }

  private static long internalHash(final String data, boolean firstOrSingleEnd) {
    long hash = 13L;
    final char[] charArray;
    final int length = (charArray = data.toCharArray()).length;
    for (int i = 0; i < length; ++i) {
      final char c = charArray[i];
      hash = hash * 31L + c;
    }

    //shift left to make room for a 'arm' bit
    hash <<= 1;
    if (!firstOrSingleEnd) {
      hash += 1;
    }
    return hash;
  }
}
