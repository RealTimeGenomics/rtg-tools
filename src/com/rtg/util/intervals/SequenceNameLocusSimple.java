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
package com.rtg.util.intervals;

/**
 * Implementation of a SequenceNameLocus
 */
public class SequenceNameLocusSimple extends Range implements SequenceNameLocus {

  protected final String mSequence;

  /**
   * @param sequence reference sequence name
   * @param start position on reference sequence (0 based)
   * @param end position on reference sequence (0 based, exclusive)
   */
  public SequenceNameLocusSimple(String sequence, int start, int end) {
    super(start, end);
    if (sequence == null) {
      throw new NullPointerException();
    }
    mSequence = sequence;
  }

  @Override
  public String getSequenceName() {
    return mSequence;
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    return overlaps(this, other);
  }

  @Override
  public boolean contains(String sequence, int pos) {
    return contains(this, sequence, pos);
  }

  /**
   * Test whether the supplied regions overlap each other
   * @param current the first range
   * @param other the other range
   * @return true if this region overlaps the other
   */
  public static boolean overlaps(SequenceNameLocus current, SequenceNameLocus other) {
    if (other.getStart() < 0 || other.getEnd() < 0
        || current.getStart() < 0 || current.getEnd() < 0) {
      throw new IllegalArgumentException();
    }
    if (!current.getSequenceName().equals(other.getSequenceName())) {
      return false;
    }
    if (other.getStart() < current.getEnd()
        && other.getEnd() > current.getStart()) {
      return true;
    }
    return false;
  }


  /**
   * Test whether the supplied position is within the current regions
   * @param current the current range
   * @param sequence the name of the sequence containing the other position
   * @param pos the position within the sequence
   * @return true if this region overlaps the other
   */
  public static boolean contains(SequenceNameLocus current, String sequence, int pos) {
    if (current.getStart() < 0 || current.getEnd() < 0) {
      throw new IllegalArgumentException();
    }
    return current.getSequenceName().equals(sequence) && pos >= current.getStart() && pos < current.getEnd();
  }

  @Override
  public String toString() {
    return getSequenceName() + ":" + (getStart() + 1) + "-" + getEnd(); // This representation (end is 1 based inclusive) is for consistency with RegionRestriction
  }

}
