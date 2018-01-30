/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.vcf.annotation;

import com.rtg.reader.SequencesReaderReferenceSource;
import com.rtg.vcf.VcfAnnotator;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Annotate records with the maximum extent of any adjacent simple tandem repeat.
 */
public class SimpleTandemRepeatAnnotator implements VcfAnnotator {

  private static final String SIMPLE_TANDEM_REPEAT_INFO = "STRL";
  private static final String SIMPLE_TANDEM_REPEAT_UNIT = "STRU";
  private static final int MAX_REP_UNIT = 3;
  private static final int MAX_STR = 100;
  private static final int MIN_REPEAT = 1; // repeat must exceed this to be output
  private final SequencesReaderReferenceSource mRefSequences;

  // Extra attributes used during evaluation for separate left and right
  //  private static final String SIMPLE_TANDEM_REPEAT_LEFT_INFO = "STRLL";
  //  private static final String SIMPLE_TANDEM_REPEAT_RIGHT_INFO = "STRLR";
  //  private static final String SIMPLE_TANDEM_REPEAT_LEFT_RU_INFO = "STRUL";
  //  private static final String SIMPLE_TANDEM_REPEAT_RIGHT_RU_INFO = "STRUR";

  /**
   * Construct a new simple tandem repeat annotator.
   * @param refSequencesSource reference sequences
   */
  public SimpleTandemRepeatAnnotator(final SequencesReaderReferenceSource refSequencesSource) {
    mRefSequences = refSequencesSource;
  }

  @Override
  public void updateHeader(final VcfHeader header) {
    header.ensureContains(new InfoField(SIMPLE_TANDEM_REPEAT_INFO, MetaType.INTEGER, VcfNumber.ONE, "Number of adjacent simple tandem repeats"));
    header.ensureContains(new InfoField(SIMPLE_TANDEM_REPEAT_UNIT, MetaType.INTEGER, VcfNumber.ONE, "Length of repeating unit in simple tandem repeat"));

    // temp for investigation
    //    header.ensureContains(new InfoField(SIMPLE_TANDEM_REPEAT_LEFT_INFO, MetaType.INTEGER, VcfNumber.ONE, "Number of adjacent simple tandem repeats, left"));
    //    header.ensureContains(new InfoField(SIMPLE_TANDEM_REPEAT_RIGHT_INFO, MetaType.INTEGER, VcfNumber.ONE, "Number of adjacent simple tandem repeats, right"));
    //    header.ensureContains(new InfoField(SIMPLE_TANDEM_REPEAT_LEFT_RU_INFO, MetaType.INTEGER, VcfNumber.ONE, "Repeat unit simple tandem repeats, left"));
    //    header.ensureContains(new InfoField(SIMPLE_TANDEM_REPEAT_RIGHT_RU_INFO, MetaType.INTEGER, VcfNumber.ONE, "Repeat unit simple tandem repeats, right"));
  }

  @Override
  public void annotate(final VcfRecord rec) {
    final String seq = rec.getSequenceName();
    final int pos = rec.getStart();
    final byte[] refSeq = mRefSequences.getReferenceBases(seq);
    final int refSpan = rec.getRefCall().length() - (VcfUtils.hasRedundantFirstNucleotide(rec) ? 1 : 0);
    final int[] str = strBidrectional(refSeq, pos, refSpan);
    if (str[0] > MIN_REPEAT) {
      rec.addInfo(SIMPLE_TANDEM_REPEAT_INFO, String.valueOf(str[0]));
      rec.addInfo(SIMPLE_TANDEM_REPEAT_UNIT, String.valueOf(str[1]));
    }

    // Extra attributes used during evaluation
//    final int[] left0 = str(refSeq, pos, -1);
//    final int[] left1 = str(refSeq, pos - 1, -1);
//    final int[] right0 = str(refSeq, pos + refSpan, 1);
//    final int[] right1 = str(refSeq, pos + refSpan + 1, 1);
//    final int[] left = left0[0] > left1[0] ? left0 : left1;
//    final int[] right = right0[0] > right1[0] ? right0 : right1;
//    rec.addInfo(SIMPLE_TANDEM_REPEAT_LEFT_INFO, String.valueOf(left[0]));
//    rec.addInfo(SIMPLE_TANDEM_REPEAT_LEFT_RU_INFO, String.valueOf(left[1]));
//    rec.addInfo(SIMPLE_TANDEM_REPEAT_RIGHT_INFO, String.valueOf(right[0]));
//    rec.addInfo(SIMPLE_TANDEM_REPEAT_RIGHT_RU_INFO, String.valueOf(right[1]));
  }

  static int[] strBidrectional(final byte[] refSeq, final int pos, final int refSpan) {
    // Find the maximum number of repeats in either direction and report the
    // number of repeats and the length of the repeating unit.

    // Repeats to left + repeats to right
    // Take the larger of off by one from current position
    final int[] leftA = str(refSeq, pos - 1, -1);
    final int[] leftB = str(refSeq, pos, -1);
    final int[] left = leftA[0] >= leftB[0] ? leftA : leftB;

    final int[] rightA = str(refSeq, pos + refSpan, 1);
    final int[] rightB = str(refSeq, pos + refSpan + 1, 1);
    final int[] right = rightA[0] >= rightB[0] ? rightA : rightB;

    return right[0] >= left[0] ? right : left;
  }

  private static int[] str(final byte[] refSeq, final int pos, final int direction) {
    assert direction != 0;
    int str = 0;
    int bestRepUnit = 0;
    for (int repUnitLength = 1; repUnitLength <= MAX_REP_UNIT; ++repUnitLength) {
      if (!selfRepeat(refSeq, pos, direction, repUnitLength)) {
        final int repeats = str(refSeq, pos, direction, repUnitLength);
        if (repeats > str) {
          str = repeats;
          bestRepUnit = repUnitLength;
        }
      }
    }
    return new int[] {str, bestRepUnit};
  }

  // Check if the repeating unit is itself a homopolymer, if it is there is no point in testing this repUnitLength
  private static boolean selfRepeat(final byte[] refSeq, final int pos0, final int direction, final int repUnitLength) {
    if (repUnitLength <= 1) {
      return false;
    }
    for (int k = 1, pos = pos0, p = pos0 + direction; k < repUnitLength; ++k, pos += direction) {
      if (p >= 0 && p < refSeq.length && refSeq[p] != refSeq[pos]) {
        return false;
      }
    }
    return true;
  }

  private static int str(final byte[] refSeq, final int pos, final int direction, final int repUnitLength) {
    assert repUnitLength != 0;
    assert direction != 0;
    int str = 0;
    if (pos >= 0 && pos < refSeq.length) {
      int p = pos + direction * repUnitLength;
      while (true) {
        for (int k = 0, j = pos; k < repUnitLength; ++k, j += direction, p += direction) {
          if (p < 0 || p >= refSeq.length || refSeq[j] != refSeq[p]) {
            return str;
          }
        }
        if (++str == MAX_STR) {
          // This ensures we don't spend ages scanning stupidly long repeats
          return str;
        }
      }
    }
    return 0;
  }
}
