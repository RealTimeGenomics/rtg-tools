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

import com.rtg.util.intervals.RegionRestriction;

import htsjdk.samtools.SAMSequenceDictionary;

/**
 * Class to handle SAM region restrictions
 */
public class SamRegionRestriction extends RegionRestriction {

  /**
   * Restriction to named template sequence, accepts restrictions of
   * the form name, name:start-end or name:start+length.
   * @param restriction restriction string
   */
  public SamRegionRestriction(String restriction) {
    super(restriction);
  }

  /**
   * Restriction to named template sequence, accepts restrictions of
   * the form name, name:start-end or name:start+length.
   * @param restriction restriction to be converted
   */
  public SamRegionRestriction(RegionRestriction restriction) {
    super(restriction.getSequenceName(), restriction.getStart(), restriction.getEnd());
  }

  /**
   * Restriction to region in named template
   * @param template sequence name
   * @param start start position of allowed region (0 based inclusive)
   * @param end end position of allowed region (0 based exclusive)
   */
  public SamRegionRestriction(String template, int start, int end) {
    super(template, start, end);
  }

  /**
   * Resolve any missing start position to the actual start position.
   * @return start position for restriction. 0 based inclusive
   */
  public int resolveStart() {
    return (getStart() == MISSING) ? 0 : getStart();
  }

  /**
   * Resolve any missing end position to the actual end position.
   * @param dict end position for restriction resolved to actual position
   * if a wildcard had been used.
   * @return end position for restriction. 0 base exclusive
   */
  public int resolveEnd(SAMSequenceDictionary dict) {
    final int sequenceLength = dict.getSequence(getSequenceName()).getSequenceLength();
    return (getEnd() == MISSING) ? sequenceLength : getEnd();
  }
}
