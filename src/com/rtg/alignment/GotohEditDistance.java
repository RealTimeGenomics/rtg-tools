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
package com.rtg.alignment;

import com.rtg.mode.DnaUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Implements the Needleman-Wunsch global alignment algorithm with the improvements
 * described by Osamu Gotoh for handling affine gap open penalties.
 * See Journal of Molecular Biology, Volume 162, Issue 3, 15 December 1982, Pages 705-708.
 * This handles affine gap penalties (a gap open penalty plus an extend penalty),
 * but still guarantees to find the lowest cost alignment.
 *
 * It constructs three conceptual 2D arrays using dynamic programming:
 * <ul>
 *   <li><code>insert</code> is the minimum cost of an alignment that ends with an insertion.</li>
 *   <li><code>distance</code> is the minimum cost of an alignment that ends with a diagonal.</li>
 *   <li><code>delete</code> is the minimum cost of an alignment that ends with a deletion.</li>
 * </ul>
 * For consistency, we always refer to the three arrays in the above (clockwise) order:
 * insert scores come from the left (<code>9 am</code>), distance scores come from the upper left
 * diagonal (<code>10:30 am</code>) and delete scores come from above (<code>12 am</code>).  The
 * dump display also follows this order.
 *
 * Each of these arrays has a getter method, for example <code>getDistance(refpos,readpos)</code>,
 * but there is just one setter for all three arrays (<code>setScore</code>).
 * This enables more flexibility in how we actually allocate the arrays in memory.
 *
 */
public class GotohEditDistance implements UnidirectionalEditDistance {

  /** True means that an alignment matrix will be printed after every alignment. */
  private static final boolean VERBOSE_ALIGNMENT = false;

  private final int mGapOpenPenalty;
  private final int mGapExtendPenalty;
  private final int mSubstitutionPenalty;
  private final int mUnknownsPenalty;

  private int[] mWorkspace = new int[ActionsHelper.ACTIONS_START_INDEX];

  /** true means the template start position is immovable. */
  private boolean mFixedStart;

  /** true means the template end position is immovable. */
  private boolean mFixedEnd;

  /** The start position of the segment of the read that must be matched. */
  private int mReadStartPos;

  /** The position in the template where we hope the match will start. */
  private int mZeroBasedStart;

  private int mMinScore;
  private int mMinScoreTemplatePos;
  private int mMinScoreReadPos;

  /**
   * The column in the matrices where the (hopefully) matching segment of the template starts.
   * More precisely, column 1 in the matrices corresponds to position
   * <code>mZeroBasedStart - mTemplatePositionOffset</code> in the template.
   */
  private int mTemplatePositionOffset;

  /** The size of the portion of the following matrices that is currently being used. */
  private int mRows, mCols;

  /** We use a one dimensional column-major array, with all three scores packed into a long. */
  private long[] mScores = new long[0];

  /** Number of bits used to store each score. */
  private static final int SCORE_BITS = 20;
  private static final int SCORE_MASK = (1 << SCORE_BITS) - 1;

  /** We offset all scores by this amount, to allow negative scores. */
  private static final int ZERO_SCORE = SCORE_MASK / 2;

  /** This is a large (bad) score. */
  private static final int LARGE_SCORE = ZERO_SCORE + (ZERO_SCORE / 2);

  private final int[] mMaxOffsetHistogram = new int[100];
  private int mOffsetTooBig = 0;

  private final boolean mStopWhenTemplateRunsOut;

  private final boolean mSupportsEarlyTermination;

  /**
   * Create an edit distance object with given penalties.
   *
   * @param openPenalty open open penalty.
   * @param extendPenalty gap extend penalty.
   * @param substitutionPenalty substitution penalty.
   * @param unknownsPenalty unknowns (n nucleotides, off template alignment) penalty.
   * @param stopWhenTemplateRunsOut stop aligning when either sequence runs out.
   */
  public GotohEditDistance(int openPenalty, int extendPenalty, int substitutionPenalty, int unknownsPenalty, boolean stopWhenTemplateRunsOut) {
    mGapOpenPenalty = openPenalty;
    mGapExtendPenalty = extendPenalty;
    mSubstitutionPenalty = substitutionPenalty;
    mUnknownsPenalty = unknownsPenalty;
    mStopWhenTemplateRunsOut = stopWhenTemplateRunsOut;
    mSupportsEarlyTermination = !(mGapOpenPenalty < 0 || mGapExtendPenalty < 0 || mSubstitutionPenalty < 0 || mUnknownsPenalty < 0);
  }


  /**
   * @return true if this implementation of GotohEditDistance supports early termination
   */
  public boolean supportsEarlyTermination() {
    return mSupportsEarlyTermination;
  }

  private int getInsert(int refpos, int readpos) {
    return (int) (mScores[refpos * mRows + readpos] >> (2 * SCORE_BITS));
  }

  private int getDistance(int refpos, int readpos) {
    return SCORE_MASK & (int) (mScores[refpos * mRows + readpos] >> SCORE_BITS);
  }

  private int getDelete(int refpos, int readpos) {
    return SCORE_MASK & (int) mScores[refpos * mRows + readpos];
  }

  private void setScores(int refpos, int readpos, int insert, int distance, int delete) {
    final long scores = (((long) insert) << (2 * SCORE_BITS)) + (((long) distance) << SCORE_BITS) + delete;
    mScores[refpos * mRows + readpos] = scores;
  }

  // alternative versions for unpacking a cached long.

  private long getScores(int refpos, int readpos) {
    return mScores[refpos * mRows + readpos];
  }

  private int getInsert(long score) {
    return (int) (score >> (2 * SCORE_BITS));
  }

  private int getDistance(long score) {
    return SCORE_MASK & (int) (score >> SCORE_BITS);
  }

  private int getDelete(long score) {
    return SCORE_MASK & (int) score;
  }

  // special case: update insert score only, leaving others unchanged.
  private void setInsertScore(int refpos, int readpos, int insert) {
    final int distance = getDistance(refpos, readpos);
    final int delete = getDelete(refpos, readpos);
    setScores(refpos, readpos, insert, distance, delete);
  }

  private void setFixedStart(final boolean isFixed) {
    if (mFixedStart != isFixed) {
      mFixedStart = isFixed;
      mRows = -1; // force penalties to be recalculated.
      mCols = -1;
    }
  }

  private void initMatrices(int rows, int cols, int maxShift, int grayWidth) {
    if (mScores.length < rows * cols) {
      mScores = new long[rows * cols];
    }
    mRows = rows;
    mCols = cols;
    // now set up the initial penalties.
    // Set the top row to all zeroes.
    final int startScore = mFixedStart ? LARGE_SCORE : ZERO_SCORE;
    final int endPenalty = Math.min(mTemplatePositionOffset + maxShift + grayWidth + 2, cols); //the last position to calculate on the top row
    for (int c = 0; c < endPenalty; ++c) {    //+2: 1 for the fence, and one for column 0
      setScores(c, 0, startScore, startScore, startScore + mGapOpenPenalty);
    }
    if (mFixedStart) {
      // the only desirable start is the position that corresponds to just before mZeroBasedStart.
      setScores(mTemplatePositionOffset, 0, ZERO_SCORE + mGapOpenPenalty, ZERO_SCORE, ZERO_SCORE + mGapOpenPenalty);
      // propagate the insert-at-start-of-read penalties along the top row.
      // TODO: do this for non-fixed start as well?  Merge with above loop?
      for (int c = mTemplatePositionOffset + 1; c < cols; ++c) {
        setInsertScore(c, 0, getInsert(c - 1, 0) + mGapExtendPenalty);
      }
    }
    // now go down column 0, setting the penalties.
    final int insert = getInsert(0, 0);
    int delete = getDelete(0, 0);
    setInsertScore(0, 0, insert + mGapOpenPenalty + mGapExtendPenalty); // TODO: why both?  Should be just mGapOpenPenalty? Should already be set above?
    for (int r = 1; r < Math.min(rows, maxShift + grayWidth * 2); ++r) {
      delete += mGapExtendPenalty;
      setScores(0, r, LARGE_SCORE, LARGE_SCORE, delete);
    }
  }

  /**
   * The real worker.
   * Fills in the matrices and finds the path of the best alignment.
   * Attempts to align <code>read[readStartPos..readEndPos-1]</code>
   * with <code>template[templateStart .. templateEnd-1]</code>.
   * Note that <code>mFixedStart</code> and <code>mFixedEnd</code> should
   * be set before this is called, since they determine whether the start
   * or end position (respectively) of the template is fixed or is allowed
   * to move to find the best alignment.
   *
   * @param read the whole read sequence
   * @param readStartPos the start of the read segment to match (inclusive).
   * @param readEndPos the end of the read segment to match (exclusive).
   * @param template the whole template sequence.
   * @param templateStart the hopeful start of the template match.
   * @param templateEnd the (exclusive) end of the template match.  Should equal <code>templateStart</code> if <code>mFixedEnd=false</code>.
   * @param maxScore alignments worse than this are not returned.
   * @param maxShift maximum allowed shift to either side
   * @return a non-null actions array.
   */
  private int[] calculate(byte[] read, int readStartPos, int readEndPos, byte[] template, int templateStart, int templateEnd, int maxScore, int maxShift) {
    assert mFixedEnd || templateEnd == templateStart;

    // We choose a template-length larger than the read length, to allow the ends to move.
    // We'd like (roughly) twice the read length in the template to allow for indels and
    // shifting the start position, balanced on each side of the template.
    // But for longer reads, we limit the extension on each side to TEMPLATE_EXTENSION.
    // However, for really long reads we add rlen/100, because that is the expected rate of indels.
    final int rlen = readEndPos - readStartPos;
    final int tlen = templateEnd - templateStart; //will be 0 unless mFixedEnd is true
    mReadStartPos = readStartPos;
    mZeroBasedStart = templateStart;
    int bLength = Math.max(rlen, tlen); // will be rlen unless mFixedEnd is true (because tlen will be 0)
    final int grayWidth = maxShift / 2 + 1;

    if (mFixedStart) {
      mTemplatePositionOffset = 0;
      if (!mFixedEnd) {
        bLength += maxShift + grayWidth;
      } else if (Math.abs(tlen - rlen) > maxShift) {    //fixed both ends and the size difference between template and read is larger than maxShift
        failure();
        return mWorkspace;
      }
    } else {    //not fixed start
      mTemplatePositionOffset = rlen == 0 ? 0 : Math.min((int) (rlen * 0.7), maxShift + grayWidth);
      if (mFixedEnd) {
        bLength += mTemplatePositionOffset;
      } else {
        bLength += mTemplatePositionOffset * 2;
      }
    }

    final long dpmSize = (1 + (long) rlen) * (1 + bLength);
    if (dpmSize < 1 || dpmSize > Integer.MAX_VALUE) {
      //DPM is huge, can't handle it return
      Diagnostic.developerLog("Can not create DPM for parameters rlen=" + (rlen + 1) + " bLength=" + (bLength + 1) + " dpmLength=" + dpmSize);
      failure();
      return mWorkspace;
    }

    initMatrices(1 + rlen, 1 + bLength, maxShift, grayWidth);

    mMinScore = Integer.MAX_VALUE / 2; // min score along bottom row.
    mMinScoreTemplatePos = Integer.MAX_VALUE / 2; // column position of the above minimum score.
    boolean earlyBailOut = false;
    int refPos;
    int terminationColumn = Integer.MAX_VALUE;
    for (refPos = 1; refPos <= bLength; ++refPos) {
      final int origin = refPos - mTemplatePositionOffset;

      final int templatePosition = origin - 1 + mZeroBasedStart;
      final boolean isWithinTemplate = 0 <= templatePosition && templatePosition < template.length;
      if (!isWithinTemplate && mStopWhenTemplateRunsOut && !mFixedEnd) {
        earlyBailOut = true;
        terminationColumn = refPos - 1;
        break;
      }
      final byte bb = isWithinTemplate ? template[templatePosition] : DnaUtils.UNKNOWN_RESIDUE;

      // calculate one column
      final int minReadPos = Math.max(1, origin - maxShift - grayWidth);
      if (minReadPos > 1 && minReadPos <= rlen) {
        setScores(refPos, minReadPos - 1, LARGE_SCORE, LARGE_SCORE, LARGE_SCORE);
      }
      final int maxReadPos = Math.min(rlen, origin + maxShift + grayWidth);
      if (maxReadPos >= 0 && maxReadPos < rlen) {
        setScores(refPos, maxReadPos + 1, LARGE_SCORE, LARGE_SCORE, LARGE_SCORE);
      }

      int colMinScore = Integer.MAX_VALUE;
      for (int readPos = minReadPos; readPos <= maxReadPos; ++readPos) {
        // Compute costs from three possible movement directions
        final int dc = diagonalCost(bb, read, readStartPos + readPos);
        final long leftScores = getScores(refPos - 1, readPos);
        final long diagScores = getScores(refPos - 1, readPos - 1);
        final long aboveScores = getScores(refPos   , readPos - 1);
        final int insert = mGapExtendPenalty
          + Math.min(getInsert(leftScores),  // gap already opened
            Math.min(getDistance(leftScores) + mGapOpenPenalty,
                     getDelete(leftScores)   + mGapOpenPenalty));
        final int distance = dc
          + Math.min(getInsert(diagScores),
            Math.min(getDistance(diagScores),
                     getDelete(diagScores)));
        final int delete = mGapExtendPenalty
          + Math.min(getInsert(aboveScores) + mGapOpenPenalty,
            Math.min(getDistance(aboveScores) + mGapOpenPenalty,
                     getDelete(aboveScores))); // gap already opened
        setScores(refPos, readPos, insert, distance, delete);
        if (supportsEarlyTermination()) {
          final int cellMinScore = Math.min(Math.min(insert, distance), delete);
          if (cellMinScore < colMinScore) {
            colMinScore = cellMinScore;
          }
        }
      }

      if (supportsEarlyTermination() && colMinScore - ZERO_SCORE > maxScore && !mFixedEnd) { //the early termination condition for columns
        terminationColumn = refPos;
        break;
      }
    }
    --refPos; // Stay at last column evaluated
    if (mFixedEnd) {
      // override the above min score by one of the three scores at the fixed end position.
      mMinScoreReadPos = rlen;
      mMinScoreTemplatePos = tlen + mTemplatePositionOffset;
      // we are careful here to include only moves that are valid.
      if (mMinScoreTemplatePos == 0 && rlen == 0) {
        // the trivial match: empty read and empty template.
        mMinScore = getDistance(0, 0);
      } else if (mMinScoreTemplatePos == 0) {
        // Column 0: so only the delete score is eligible
        mMinScore = getDelete(mMinScoreTemplatePos, rlen);
      } else if (rlen == 0) {
        // Row 0: so only the insert score is eligible
        mMinScore = getInsert(mMinScoreTemplatePos, rlen);
      } else {
        // we are are in the main part of the matrices, so all scores are eligible
        mMinScore = Math.min(getDistance(mMinScoreTemplatePos, rlen),
          Math.min(getInsert(mMinScoreTemplatePos, rlen),
            getDelete(mMinScoreTemplatePos, rlen)));
      }
    } else {    //not fixed end... scan for lowest score CLOSEST to expected diagonal
      final int refDiagEndPos = findBestReadEnd(maxShift, rlen, grayWidth, bLength, terminationColumn);
      if (earlyBailOut) {
        findBestTemplateEnd(maxShift, rlen, grayWidth, refPos, refDiagEndPos);
      }
    }

    mMinScore -= ZERO_SCORE;
    boolean validAlignment = false;
    if (mMinScore <= maxScore || VERBOSE_ALIGNMENT) {
      validAlignment = goBackwards(read, template, mMinScoreReadPos, mMinScoreTemplatePos, maxShift);
      mWorkspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = mMinScore;
    }
    if (VERBOSE_ALIGNMENT) {
      dump(read, template, maxShift, grayWidth);
    }
    if (!validAlignment) {
      failure();
    }
    return mWorkspace;
  }

  /**
   * Actually finds the best template position based on a given read length
   * @param maxShift maximum distance to shift from diagonal
   * @param rlen read length to evaluate template positions against
   * @param grayWidth width of gutters
   * @param bLength the rightmost column in the matrix
   * @param terminationColumn the rightmost column to evaluate (later columns should have min scores greater than the max score we allow)
   * @return the diagonal position on the template (where the alignment would end if there were no indels)
   */
  private int findBestReadEnd(int maxShift, int rlen, int grayWidth, int bLength, int terminationColumn) {
    mMinScoreReadPos = rlen;
    final int refDiagEndPos = mTemplatePositionOffset + rlen;    //end point if no indels
    final int startPos = Math.min(Math.min(bLength, refDiagEndPos + maxShift + grayWidth), terminationColumn);
    for (int i = startPos; i >= refDiagEndPos - maxShift - grayWidth && i >= 0; --i) {
      final long bottomScores = getScores(i, rlen);
      final boolean closerThanCurrentBest = Math.abs(mMinScoreTemplatePos - refDiagEndPos) > Math.abs(i - refDiagEndPos);

      int cellScore = getDistance(bottomScores);
      if (cellScore == LARGE_SCORE) {
        if (i == 0) {
          cellScore = getDelete(bottomScores);
          evaluateCellScore(cellScore, rlen, i, closerThanCurrentBest);
        }
        break;
      }
      evaluateCellScore(cellScore, rlen, i, closerThanCurrentBest);

      cellScore = getDelete(bottomScores);
      evaluateCellScore(cellScore, rlen, i, closerThanCurrentBest);

      cellScore = getInsert(bottomScores);
      evaluateCellScore(cellScore, rlen, i, closerThanCurrentBest);
    }
    return refDiagEndPos;
  }

  private void findBestTemplateEnd(int maxShift, int rlen, int grayWidth, int refPos, int refDiagEndPos) {
    int i = Math.min(rlen, refDiagEndPos + maxShift + grayWidth);
    while (i >= 0) {
      final long rightScores = getScores(refPos, i);
      final boolean closerThanCurrentBest = Math.abs(mMinScoreReadPos - refDiagEndPos) > Math.abs(i - refDiagEndPos);
      int cellScore = getDistance(rightScores);
      if (cellScore == LARGE_SCORE) {
        if (i == 0) {
          cellScore = getInsert(rightScores);
          evaluateCellScore(cellScore, i, refPos, closerThanCurrentBest);
        }
        break;
      }
      evaluateCellScore(cellScore, i, refPos, closerThanCurrentBest);

      cellScore = getDelete(rightScores);
      evaluateCellScore(cellScore, i, refPos, closerThanCurrentBest);

      cellScore = getInsert(rightScores);
      evaluateCellScore(cellScore, i, refPos, closerThanCurrentBest);
      --i;
    }
  }

  private void evaluateCellScore(int score, int readPos, int templatePos, boolean closerThanCurrentBest) {
    if (score < mMinScore || score == mMinScore && closerThanCurrentBest) {
      mMinScore = score;
      mMinScoreReadPos = readPos;
      mMinScoreTemplatePos = templatePos;
    }
  }

  private void failure() {
    mWorkspace[ActionsHelper.TEMPLATE_START_INDEX] = mZeroBasedStart;
    mWorkspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = Integer.MAX_VALUE;
    mWorkspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
  }

  /**
   * Go backwards through the matrices computing the actions.
   * @return true if a valid path found (stays within <code>maxShift</code> offset)
   */
  private boolean goBackwards(byte[] read, byte[] template, int readEndPos, int templateEndPos, int maxShift) {
    final int size = ActionsHelper.ACTIONS_START_INDEX + 1 + ((readEndPos + templateEndPos) >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    if (mWorkspace.length < size) {
      mWorkspace = new int[size];
    }
    boolean validResult = true;
    int readPos = readEndPos;
    int refPos = templateEndPos;
    int totalActions = 0;
    int buffer = 0;
    int actionPoint = ActionsHelper.ACTIONS_START_INDEX;
    // loop invariant: these costs are always up to date for the current cell: [refPos][readPos]
    int diagCost = getDistance(refPos, readPos);
    int delCost = getDelete(refPos, readPos);
    int insCost = getInsert(refPos, readPos);
    int minCost = Math.min(diagCost, Math.min(delCost, insCost));
    // This mustIndel variable locks us into continuing back through an indel
    // until we get to its start where the mGapOpenPenalty was applied.
    // It has one of these three values:
    //   0 means no constraints on what action we can do next;
    //   INSERTION_INTO_REFERENCE means we are in the middle of a deletion;
    //   DELETION_FROM_REFERENCE means we are in the middle of an insertion.
    int mustIndel = 0;
    int maxOffset = 0;
    while (readPos > 0 || (mFixedStart && refPos != mTemplatePositionOffset)) {        //TODO uh?? why stop here in fixed start case? - mTPO is always 0 in this case
      final int command;
      // diagonals have top priority, unless we are in the middle of a delete or insert
      final int realRefPos = refPos - 1 + mZeroBasedStart - mTemplatePositionOffset;
      if (mStopWhenTemplateRunsOut && realRefPos < 0) {
        // Correct minimum score to adjust for the unused part of the alignment
        mMinScore += ZERO_SCORE - Math.min(getDistance(refPos, readPos), Math.min(getInsert(refPos, readPos), getDelete(refPos, readPos)));
        break;
      }
      if (minCost == diagCost && refPos > 0 && mustIndel == 0) {
        final int realReadPos = readPos - 1 + mReadStartPos;
        if (realReadPos < 0) {
          failure();
          return false;
        }
        if (0 <= realRefPos && realRefPos < template.length && realReadPos < read.length && isSame(read[realReadPos], template[realRefPos])) {
          command = ActionsHelper.SAME;
        } else {
          command = ActionsHelper.MISMATCH;
        }
        --readPos;
        --refPos;
        // now update our current costs
        diagCost = getDistance(refPos, readPos);
        delCost = getDelete(refPos, readPos);
        insCost = getInsert(refPos, readPos);
        minCost = Math.min(diagCost, Math.min(delCost, insCost));
      } else if ((mustIndel == ActionsHelper.DELETION_FROM_REFERENCE
          || minCost == insCost && mustIndel != ActionsHelper.INSERTION_INTO_REFERENCE)
          && refPos > 0) {
        command = ActionsHelper.DELETION_FROM_REFERENCE;
        --refPos;
        // now update our current costs
        final int oldMinCost = minCost;
        diagCost = getDistance(refPos, readPos);
        delCost = getDelete(refPos, readPos);
        insCost = getInsert(refPos, readPos);
        minCost = Math.min(diagCost, Math.min(delCost, insCost));
        if (oldMinCost == minCost + mGapOpenPenalty + mGapExtendPenalty) {
          mustIndel = 0;
        } else {
          mustIndel = command;
          minCost = insCost; // we must take the insert path
        }
      } else {
        command = ActionsHelper.INSERTION_INTO_REFERENCE;
        --readPos;
        // now update our current costs
        final int oldMinCost = minCost;
        diagCost = getDistance(refPos, readPos);
        delCost = getDelete(refPos, readPos);
        insCost = getInsert(refPos, readPos);
        minCost = Math.min(diagCost, Math.min(delCost, insCost));
        if (oldMinCost == minCost + mGapOpenPenalty + mGapExtendPenalty) {
          mustIndel = 0;
        } else {
          mustIndel = command;
          minCost = delCost; // we must take the delete path
        }
      }
      buffer <<= ActionsHelper.BITS_PER_ACTION;
      buffer |= command;
      if ((++totalActions & ActionsHelper.ACTIONS_COUNT_MASK) == 0) {
        mWorkspace[actionPoint++] = buffer;
        buffer = 0;
      }

      final int thisOffset = Math.abs(readPos + mTemplatePositionOffset - refPos);
      if (thisOffset > maxShift) {
        validResult = false;
        if (!VERBOSE_ALIGNMENT) {
          break;
        }
      } else if (thisOffset > maxOffset) {
        maxOffset = thisOffset;
      }
    }

    if (!validResult) {
      ++mOffsetTooBig;
    } else {
      if (maxOffset >= mMaxOffsetHistogram.length) {
        mMaxOffsetHistogram[mMaxOffsetHistogram.length - 1]++;
      } else {
        mMaxOffsetHistogram[maxOffset]++;
      }
    }
    mWorkspace[actionPoint] = buffer << (32 - ActionsHelper.BITS_PER_ACTION * (totalActions & ActionsHelper.ACTIONS_COUNT_MASK));
    mWorkspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = totalActions;
    mWorkspace[ActionsHelper.TEMPLATE_START_INDEX] = refPos + mZeroBasedStart - mTemplatePositionOffset;
    return validResult;
  }

  /**
   * Dump the alignment matrices for the most recent alignment.
   *
   * @param read the read bytes
   * @param template the template bytes
   * @param maxShift the maximum allowed shift
   * @param grayWidth the gray area width
   */
  protected void dump(byte[] read, byte[] template, int maxShift, int grayWidth) {
    System.out.println("NOTE: each column contains: |min_insert_score (via left), min_diagonal_score, min_delete_score (via above)|");
    // Setup the path matrix.
    // Each cell [t,r] in path is 0 or a command ('=', 'S', 'D', 'I').
    final char[][] path = new char[mCols][];
    for (int i = 0; i < mCols; ++i) {
      path[i] = new char[mRows];
    }
    final ActionsHelper.CommandIterator iter = ActionsHelper.iterator(mWorkspace);
    // we start just before the beginning of the read and the template.
    int rPos = 0;
    // this is the start of the final match in the template.
    final int startMatch = ActionsHelper.zeroBasedTemplateStart(mWorkspace);
    // translate the starting position from template space to matrix space.
    int tPos = startMatch - mZeroBasedStart + mTemplatePositionOffset;

    while (iter.hasNext()) {
      final int cmd = iter.next();
      switch (cmd) {
      case ActionsHelper.INSERTION_INTO_REFERENCE:
        ++rPos;
        path[tPos][rPos] = 'I';
        break;
      case ActionsHelper.DELETION_FROM_REFERENCE:
        ++tPos;
        path[tPos][rPos] = 'D';
        break;
      case ActionsHelper.SAME:
        ++rPos;
        ++tPos;
        path[tPos][rPos] = '=';
        break;
      default:
        ++rPos;
        ++tPos;
        path[tPos][rPos] = 'S';
        break;
      }
    }
    System.out.print("             |");
    for (int refPos = 1; refPos < mCols; ++refPos) {
      System.out.print("     ");
      System.out.print(refPos == mTemplatePositionOffset + 1 ? "(" : " ");
      System.out.print(residue(template, refPos - 1 + mZeroBasedStart - mTemplatePositionOffset));
      System.out.print(refPos == mTemplatePositionOffset + 1 ? ")" : " ");
      System.out.print("   |");
    }
    System.out.println();
    for (int readPos = 0; readPos < mRows; ++readPos) {
      System.out.print(residue(read, mReadStartPos + readPos - 1) + "|");
      for (int refPos = 0; refPos < mCols; ++refPos) {
        if (Math.abs(readPos - (refPos - mTemplatePositionOffset)) > maxShift + grayWidth + 1) {
          System.out.print("  -,  -,  -|");
        } else {
          final int cmd = path[refPos][readPos];
          System.out.print(formatDumpScore(getInsert(refPos, readPos)));
          System.out.print(cmd == 'D' ? (char) cmd : ',');
          System.out.print(formatDumpScore(getDistance(refPos, readPos)));
          System.out.print((cmd == '=' || cmd == 'S') ? (char) cmd : ',');
          System.out.print(formatDumpScore(getDelete(refPos, readPos)));
          if (cmd == 'I') {
            System.out.print((char) cmd);
          } else {
            if (refPos == mTemplatePositionOffset + readPos - maxShift - 1
                || refPos == (mTemplatePositionOffset + readPos + maxShift)) {
              System.out.print("\\");
            } else {
              System.out.print("|");
            }
          }
        }
      }
      System.out.println();
    }
  }

  private String formatDumpScore(int rawScore) {
    final int score = rawScore - ZERO_SCORE;
    if (score > 999) {
      return "  x";
    }
    final String fmt3 = "%3d";
    return String.format(fmt3, score);
  }

  /**
   * Convert an internal code into a printable DNA/Protein character.
   * @param residues array of internal codes
   * @param pos zero-based position within <code>residues</code>.
   * @return a printable character ('N' if <code>pos</code> is outside the legal bounds).
   */
  protected char residue(byte[] residues, int pos) {
    return DnaUtils.base(residues, pos);
  }

  /**
   * Compute the cost of going diagonally to read the specified template and read
   * residues.
   *
   * @param templateResidue template residue. Supply <code>DnaUtils.UNKNOWN_RESIDUE</code> if position is not on the template
   * @param read read residues array
   * @param readPos the current position in read array plus one
   * @return cost of a diagonal move
   */
  protected int diagonalCost(int templateResidue, byte[] read, int readPos) {
    final int readResidue = read[readPos - 1];
    if (templateResidue == DnaUtils.UNKNOWN_RESIDUE || readResidue == DnaUtils.UNKNOWN_RESIDUE) {
      return mUnknownsPenalty;
    }
    return templateResidue == readResidue ? 0 : mSubstitutionPenalty;
  }

  /**
   * The most recent match was the same residues.
   * @param refNt the reference residue
   * @param readNt the read residue
   * @return true if neither residue is missing and they are the same
   */
  protected final boolean isSame(final int refNt, final int readNt) {
    return !(refNt == DnaUtils.UNKNOWN_RESIDUE || readNt == DnaUtils.UNKNOWN_RESIDUE) && refNt == readNt;
  }

  @Override
  public void logStats() {
    if (mScores != null) {
      Diagnostic.developerLog("GotohEditDistance maxbytes=" + mScores.length * 8
          + ", currsize=" + mRows + "x" + mCols);
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("Maximum offset Histogram").append(StringUtils.LS);
    for (int i = mMaxOffsetHistogram.length - 1; i >= 0; --i) {
      if (mMaxOffsetHistogram[i] > 0) {
        sb.append(i).append(" = ").append(mMaxOffsetHistogram[i]).append(StringUtils.LS);
      }
    }
    sb.append("Exceeded maxShift offset: ").append(mOffsetTooBig).append(StringUtils.LS);
    Diagnostic.developerLog(sb.toString());
  }

  @Override
  public int[] calculateEditDistance(byte[] read, int rlen, byte[] template, int zeroBasedStart, int maxScore, int maxShift, boolean cgFirst) {
    setFixedStart(false);
    mFixedEnd = false;
    return calculate(read, 0, rlen, template, zeroBasedStart, zeroBasedStart, maxScore, maxShift);
  }

  @Override
  public int[] calculateEditDistanceFixedBoth(byte[] read, int readStartPos, int readEndPos,
      byte[] template, int templateStart, int templateEnd, int maxScore, int maxShift) {
    setFixedStart(true);
    mFixedEnd = true;
    return calculate(read, readStartPos, readEndPos, template, templateStart, templateEnd, maxScore, maxShift);
  }

  @Override
  public int[] calculateEditDistanceFixedEnd(byte[] read, int readStartPos, int readEndPos, byte[] template,
      int templateExpectedStartPos, int templateEndPos, int maxScore, int maxShift) {
    setFixedStart(false);
    mFixedEnd = true;
    return calculate(read, readStartPos, readEndPos, template, templateExpectedStartPos, templateEndPos, maxScore, maxShift);
  }

  @Override
  public int[] calculateEditDistanceFixedStart(byte[] read, int readStartPos, int readEndPos,
      byte[] template, int templateStartPos, int maxScore, int maxShift) {
    setFixedStart(true);
    mFixedEnd = false;
    return calculate(read, readStartPos, readEndPos, template, templateStartPos, templateStartPos, maxScore, maxShift);
  }
}
