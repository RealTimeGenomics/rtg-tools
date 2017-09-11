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

package com.rtg.util.machine;

import htsjdk.samtools.SAMRecord;

/**
 * Enumeration of the orientations of paired reads which is technology dependent.
 */
public enum MachineOrientation {

 /** Forward Reverse orientation in paired reads. */
  FR {

    @Override
    public boolean firstOnTemplate(SAMRecord sam) {
      return !sam.getReadNegativeStrandFlag();
    }

    @Override
    public boolean orientationOkay(boolean firstOnReferenceReverse, boolean firstOnReferenceLeft, boolean secondOnReferenceReverse, boolean secondOnReferenceLeft) {
      assert firstOnReferenceLeft ^ secondOnReferenceLeft;
      return !firstOnReferenceReverse && secondOnReferenceReverse;
    }

    @Override
    public PairOrientation getMateOrientation(PairOrientation pairOrientation) {
      if (PairOrientation.F1 == pairOrientation) {
        return PairOrientation.R2;
      } else if (PairOrientation.F2 == pairOrientation) {
        return PairOrientation.R1;
      } else if (PairOrientation.R1 == pairOrientation) {
        return PairOrientation.F2;
      } else if (PairOrientation.R2 == pairOrientation) {
        return PairOrientation.F1;
      }
      return null;
    }

    @Override
    public boolean isMateUpstream(PairOrientation pairOrientation) {
      return PairOrientation.F1 == pairOrientation || PairOrientation.F2 == pairOrientation;
    }
  },
  /** reverse forward orientation in paired reads */
  RF {

    @Override
    public boolean firstOnTemplate(SAMRecord sam) {
      return sam.getReadNegativeStrandFlag();
    }

    @Override
    public boolean orientationOkay(boolean firstOnReferenceReverse, boolean firstOnReferenceLeft, boolean secondOnReferenceReverse, boolean secondOnReferenceLeft) {
      assert firstOnReferenceLeft ^ secondOnReferenceLeft;
      return firstOnReferenceReverse && !secondOnReferenceReverse;
    }

    @Override
    public PairOrientation getMateOrientation(PairOrientation pairOrientation) {
      return FR.getMateOrientation(pairOrientation);
    }

    @Override
    public boolean isMateUpstream(PairOrientation pairOrientation) {
      return PairOrientation.F1 != pairOrientation && PairOrientation.F2 != pairOrientation;
    }
  },
  /** Forward Forward orientation in paired reads. */
  TANDEM {

    @Override
    public boolean firstOnTemplate(SAMRecord sam) {
      if (sam.getFirstOfPairFlag() && !sam.getReadNegativeStrandFlag()) {
        return true;
      }
      if (sam.getSecondOfPairFlag() && sam.getReadNegativeStrandFlag()) {
        return true;
      }
      return false;
    }

    @Override
    public boolean orientationOkay(boolean firstOnReferenceReverse, boolean firstOnReferenceLeft, boolean secondOnReferenceReverse, boolean secondOnReferenceLeft) {
      assert firstOnReferenceLeft ^ secondOnReferenceLeft;
      return /*F1F2*/ (firstOnReferenceLeft && !firstOnReferenceReverse && !secondOnReferenceReverse && !secondOnReferenceLeft)
            || /*R2R1*/ (firstOnReferenceReverse && !firstOnReferenceLeft && secondOnReferenceReverse && secondOnReferenceLeft);
    }

    @Override
    public PairOrientation getMateOrientation(PairOrientation pairOrientation) {
      if (PairOrientation.F1 == pairOrientation) {
        return PairOrientation.F2;
      } else if (PairOrientation.F2 == pairOrientation) {
        return PairOrientation.F1;
      } else if (PairOrientation.R1 == pairOrientation) {
        return PairOrientation.R2;
      } else if (PairOrientation.R2 == pairOrientation) {
        return PairOrientation.R1;
      }
      return null;
    }

    @Override
    public boolean isMateUpstream(PairOrientation pairOrientation) {
      return PairOrientation.F1 == pairOrientation || PairOrientation.R2 == pairOrientation;
    }
  },

  /** Allow any orientation, many methods unsupported */
  ANY {
    @Override
    public boolean firstOnTemplate(SAMRecord sam) {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean orientationOkay(boolean firstOnReferenceReverse, boolean firstOnReferenceLeft, boolean secondOnReferenceReverse, boolean secondOnReferenceLeft) {
      assert firstOnReferenceLeft ^ secondOnReferenceLeft;
      return true;
    }

    @Override
    public PairOrientation getMateOrientation(PairOrientation pairOrientation) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMateUpstream(PairOrientation pairOrientation) {
      throw new UnsupportedOperationException();
    }
  };

  /**
   * Test if the read has a mapped mate which is in the correct orientation
   * that the two could form a normal mate on the given template.
   * This does NOT test that the insert size lies within the expected distribution.
   * @param sam the SAM record being checked.
   * @return true iff there is a mate in the correct orientation.
   */
  public final boolean hasValidMate(final SAMRecord sam) {
    assert !sam.getReadUnmappedFlag();
    if (!sam.getReadPairedFlag()) {
      return false;
    }
    if (sam.getMateUnmappedFlag()) {
      return false;
    }
    if (!sam.getReferenceName().equals(sam.getMateReferenceName())) {
      return false;
    }
    if (sam.getFirstOfPairFlag()) {
      return orientationOkay(sam.getAlignmentStart(), sam.getReadNegativeStrandFlag(), sam.getMateAlignmentStart(), sam.getMateNegativeStrandFlag());
    } else {
      return orientationOkay(sam.getMateAlignmentStart(), sam.getMateNegativeStrandFlag(), sam.getAlignmentStart(), sam.getReadNegativeStrandFlag());
    }
  }

  /**
   * Test if record would be first on template if normally mated.
   * @param sam the SAM record being checked.
   * @return true iff the record would appear first on template in normal case.
   */
  public abstract boolean firstOnTemplate(SAMRecord sam);

  /**
   * check orientation of pairs against required orientation
   * @param aPos position of <code>a</code> read arm on reference (requires this is &quot;left&quot; arm)
   * @param aRev true if <code>a</code> read arm reverse complement  (requires this is &quot;left&quot; arm)
   * @param bPos position of <code>b</code> read arm on reference  (requires this is &quot;right&quot; arm)
   * @param bRev true if <code>b</code> read arm is reverse complement (requires this is &quot;right&quot; arm)
   * @return true if read arms obey required pair orientation
   */
  public boolean orientationOkay(int aPos, boolean aRev, int bPos, boolean bRev) {
    if (aPos <= bPos) {
      return orientationOkay(aRev, true, bRev, false);
    } else {
      return orientationOkay(bRev, false, aRev, true);
    }
  }

  /**
   * check orientation of pairs against required orientation
   * @param firstOnReferenceReverse true if smallest start position mapped read is reverse complement
   * @param firstOnReferenceLeft true if smallest start position mapped read is from &quot;left&quot; arm
   * @param secondOnReferenceReverse true if greatest start position mapped read is reverse complement
   * @param secondOnReferenceLeft true if greatest start position mapped read is from &quot;left&quot; arm
   * @return true if read arms obey required pair orientation
   */
  public abstract boolean orientationOkay(boolean firstOnReferenceReverse, boolean firstOnReferenceLeft, boolean secondOnReferenceReverse, boolean secondOnReferenceLeft);

  /**
   * Return the pair orientation of the mate of this pair orientation. Useful for
   * unmapped breakpoint/structural variation detection.
   * @param pairOrientation the pair orientation of the mapped read.
   * @return the pair orientation of the mate
   */
  public abstract PairOrientation getMateOrientation(PairOrientation pairOrientation);

  /**
   * Return whether the mate is up or downstream from a mapped read, assuming a concordant pair.
   * @param pairOrientation the pair orientation of the mapped read.
   * @return true if upstream (higher template start position), false if downstream (lower template start position).
   */
  public abstract boolean isMateUpstream(PairOrientation pairOrientation);
}
