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

package com.rtg.bed;

import static com.rtg.util.StringUtils.TAB;

import java.util.Arrays;

import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.util.StringUtils;

/**
 * A BED record class.  Positions in BED format are zero-based.
 *
 */
public class BedRecord extends SequenceNameLocusSimple {

  protected final String[] mAnnotations;

  /**
   * Constructor.
   * @param sequence the sequence name
   * @param start the start of the region (0-based inclusive)
   * @param end the end of the region (0-based exclusive)
   * @param annotations the list of annotation fields
   */
  public BedRecord(String sequence, int start, int end, String... annotations) {
    super(sequence, start, end);
    if (annotations == null) {
      mAnnotations = new String[0];
    } else {
      mAnnotations = annotations;
    }
  }

  /**
   * Get annotations, which are from columns 3 onwards in the BED file.
   * @return Returns the annotations.
   */
  public String[] getAnnotations() {
    return mAnnotations;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(mSequence);
    sb.append(TAB).append(getStart());
    sb.append(TAB).append(getEnd());
    for (final String s : mAnnotations) {
      sb.append(TAB).append(s);
    }
    return sb.toString();
  }

  /**
   * Get a BedRecord from a single line.
   * @param line the BED line to parse
   * @return the BedRecord
   * @throws NumberFormatException if the start or end cannot be parsed
   * @throws ArrayIndexOutOfBoundsException if there are not enough fields
   */
  public static BedRecord fromString(String line) {
    final String[] parts = StringUtils.split(line, '\t');
    return new BedRecord(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Arrays.copyOfRange(parts, 3, parts.length));
  }
}
