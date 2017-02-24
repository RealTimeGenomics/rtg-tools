/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.rtg.mode.Protein;
import com.rtg.mode.ProteinScoringMatrix;
import com.rtg.sam.SamUtils;

/**
 * Constants and utility functions relating to actions in an alignment.<p>
 * The result of an alignment is represented in an array of integer.</p>
 * <ul>
 * <li>Position 0 is the zero-based template start index.
 * <li>Position 1 is the raw alignment score
 * <li>Position 2 is the alignment length
 * <li>Position 3 onwards are a sequence of 4-bit actions defining the alignment
 * </ul>
 * These 4-bit actions have the following meanings:
 * <pre>
 * Name                     Char     readPos     templatePos
 * -------------------      ----     -------     -----------
 * SAME                     =        r++         t++
 * MISMATCH                 X        r++         t++
 * DELETION_FROM_REFERENCE  D        no change   t++
 * INSERTION_INTO_REFERENCE I        r++         no change
 * CG_GAP_IN_READ           N        no change   t++
 * CG_OVERLAP_IN_READ       B        no change   t--
 * UNKNOWN_TEMPLATE         T        r++         t++
 * UNKNOWN_READ             R        r++         t++
 * SOFT_CLIP                S        r++         no change
 * NOOP                              no change   no change
 * </pre>
 */
public final class ActionsHelper {

  private ActionsHelper() { }

  /** Action for match. */
  public static final int SAME = 0;
  /** Action for substitution. */
  public static final int MISMATCH = 1;
  /** Action for insertion into read. */
  public static final int DELETION_FROM_REFERENCE = 2;
  /** Action for deletion from read. */
  public static final int INSERTION_INTO_REFERENCE = 3;
  /** Action for soft clip */
  public static final int SOFT_CLIP = 4;
  /** Action for no action */
  public static final int NOOP = 7;

  //actions below are CG specific and are signified by the upper bit of the action being set.
  /** Action for CG gap in read.  Means skip over one template base with no penalty. 10 to match DEL_FROM_REF on lower bits.*/
  public static final int CG_GAP_IN_READ = 10;
  /** Action for CG overlap in read.  Means back up along the read by one base. 11 to match INS_INTO_REF on lower bits. */
  public static final int CG_OVERLAP_IN_READ = 11;
  /** Action for an unknown nucleotide in the template */
  public static final int UNKNOWN_TEMPLATE = 12;
  /** Action for an unknown nucleotide in the read */
  public static final int UNKNOWN_READ = 13;

  /** Array position of template start (which is zero based). */
  public static final int TEMPLATE_START_INDEX = 0;
  /** Array position of alignment score. */
  public static final int ALIGNMENT_SCORE_INDEX = 1;
  /** Array position of actions length. */
  public static final int ACTIONS_LENGTH_INDEX = 2;
  /** Array position of first packed actions. */
  public static final int ACTIONS_START_INDEX = 3;

  /** Number of bits to represent an action. */
  public static final int BITS_PER_ACTION = 4;
  /** Shift. */
  public static final int ACTIONS_PER_INT_SHIFT = 3;
  /** Number of actions in a single int. */
  public static final int ACTIONS_PER_INT = 1 << ACTIONS_PER_INT_SHIFT; // temp public
  /** Mask for a single action. */
  public static final int SINGLE_ACTION_MASK = (1 << BITS_PER_ACTION) - 1; // temp public
  /** Mask for counting actions per int. */
  public static final int ACTIONS_COUNT_MASK = ACTIONS_PER_INT - 1;


  // we use this just to avoid Java warnings from obvious assertions.
  private static boolean isEqual(int a, int b) {
    return a == b;
  }

  /**
   * Clear an actions array, so that it is empty.
   * This can be used before new commands are added using prepend etc.
   * @param actions actions array
   */
  protected static void clear(final int[] actions) {
    Arrays.fill(actions, 0);
  }

  /**
   * Return a minimal copy of an actions array.  The copy will contain all the
   * actions and sundry information of the original, but will not have extra
   * free space.
   * @param actions array to copy
   * @return copy
   */
  public static int[] copy(final int[] actions) {
    final int length = length(actions);
    assert length <= actions.length;
    return Arrays.copyOf(actions, length);
  }

  /**
   * Return the length of the actions array in use.
   * @param actions actions array
   * @return the length of the array in use
   */
  public static int length(int[] actions) {
    return ACTIONS_START_INDEX + (actionsCount(actions) + ACTIONS_PER_INT - 1) / ACTIONS_PER_INT;
  }

  /**
   * Return the number of actions.
   * @param actions actions array
   * @return number of actions
   */
  public static int actionsCount(final int[] actions) {
    return actions[ACTIONS_LENGTH_INDEX];
  }

  /**
   * Raw alignment score.
   * @param actions actions array
   * @return raw alignment score
   */
  public static int alignmentScore(final int[] actions) {
    return actions[ALIGNMENT_SCORE_INDEX];
  }

  /**
   * Set the raw alignment score.
   * @param actions an actions array
   * @param score the new score.
   */
  protected static void setAlignmentScore(final int[] actions, final int score) {
    actions[ALIGNMENT_SCORE_INDEX] = score;
  }

  /**
   * Zero-based start position on template.
   * @param actions actions array
   * @return start position
   */
  public static int zeroBasedTemplateStart(final int[] actions) {
    return actions[TEMPLATE_START_INDEX];
  }

  /**
   * Set the template start position.
   * @param actions an actions array.
   * @param start the new template start position.
   */
  protected static void setZeroBasedTemplateStart(final int[] actions, final int start) {
    actions[TEMPLATE_START_INDEX] = start;
  }

  /**
   * An iterator-like object for iterating through the commands in an actions array.
   * (This is not a standard Java iterator, to avoid the inefficiency of creating
   * an Integer object each time next is called.)
   * This iterator is nearly as fast as having all the horrible bit-twiddling
   * code in-line in the client - one small benchmark showed the iterator is
   * only 16% slower.
   */
  public static class CommandIterator {
    int[] mActions = new int[0];
    int mPosition;

    /**
     * Set the actions to iterate through
     * @param actions A packed actions array from an edit distance operation.
     */
    public void setActions(int[] actions) {
      mActions = actions;
      mPosition = ActionsHelper.actionsCount(actions);
    }

    /** @return true iff there are more actions. */
    public boolean hasNext() {
      return mPosition > 0;
    }

    static int currentCommand(int[] actions, int position) {
      final int buf = actions[ACTIONS_START_INDEX + (position >> ACTIONS_PER_INT_SHIFT)];
      return (buf >>> BITS_PER_ACTION * (ACTIONS_PER_INT - 1 - (position & ACTIONS_COUNT_MASK))) & SINGLE_ACTION_MASK;
    }

    /** @return the next action command. */
    public int next() {
      assert mPosition > 0;
      --mPosition;
      return currentCommand(mActions, mPosition);
    }
  }

  /** Like <code>CommandIterator</code>, but iterates in reverse. */
  public static final class CommandIteratorReverse extends CommandIterator {
    @Override
    public void setActions(int[] actions) {
      mActions = actions;
      mPosition = 0;
    }

    @Override
    public boolean hasNext() {
      return mPosition < mActions[ACTIONS_LENGTH_INDEX];
    }

    @Override
    public int next() {
      assert mPosition < mActions[ACTIONS_LENGTH_INDEX];
      final int cmd = currentCommand(mActions, mPosition);
      ++mPosition; // different convention to the forward iterator
      return cmd;
    }
  }

  /**
   * Iterate through all the action commands.
   * @param actions the result of an edit distance method.
   * @return an iterator-like object that returns commands (integers).
   */
  public static CommandIterator iterator(final int[] actions) {
    final CommandIterator it = new CommandIterator();
    it.setActions(actions);
    return it;
  }

  /**
   * Iterate through all the action commands in reverse order.
   * @param actions the result of an edit distance method.
   * @return an iterator-like object that returns commands (integers).
   */
  public static CommandIterator iteratorReverse(final int[] actions) {
    final CommandIterator it = new CommandIteratorReverse();
    it.setActions(actions);
    return it;
  }

  /**
   * Psuedo-population count.  Returns the number of 1-bits in odd positions in <code>x</code>.
   * @param x0 value to get population count of
   * @return population count
   */
  private static int pseudopopcount(final int x0) {
    int x = x0;
    x = (x & 0x33333333) + ((x >> 2) & 0x33333333);
    x += x >> 4;
    x &= 0x0F0F0F0F;
    x += x >> 8;
    x += x >> 16;
    return x & 0x3F;
  }

  /**
   * Return a count of the number of matches in this alignment.
   * @param actions actions array
   * @return match count
   */
  public static int matchCount(final int[] actions) {
    assert isEqual(SAME, 0); // this method relies critically on this
    if (actions.length <= ACTIONS_START_INDEX) {
      return 0;
    }
    int matches = actionsCount(actions);
    final int lim = ACTIONS_START_INDEX + ((matches - 1) >> ACTIONS_PER_INT_SHIFT);
    for (int k = ACTIONS_START_INDEX; k <= lim; ++k) {
      final int v = actions[k];
      matches -= pseudopopcount((v | (v >> 1) | (v >> 2) | (v >> 3)) & 0x11111111);
    }
    return matches;
  }

  /**
   * True if actions contains some special CG commands (<code>CG_GAP_IN_READ</code>
   * or (<code>CG_OVERLAP_IN_READ</code>).
   * This assumes all CG type commands have the highest bit set in the packed actions.
   * @param actions actions array.
   * @return match true if there are one or more special CG commands in actions.
   */
  public static boolean isCg(final int[] actions) {
    assert CG_OVERLAP_IN_READ >= 8; // this method relies critically on this
    assert CG_GAP_IN_READ >= 8; // this method relies critically on this
    if (actions.length <= ACTIONS_START_INDEX) {
      return false;
    }
    final int matches = actionsCount(actions);
    final int lim = ACTIONS_START_INDEX + ((matches - 1) >> ACTIONS_PER_INT_SHIFT);
    for (int k = ACTIONS_START_INDEX; k <= lim; ++k) {
      final int v = actions[k];
      if ((v & 0x88888888) != 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return a count of the number of deletions from read operations in this alignment
   * CG overlap commands are included in the count.
   * @param actions actions array
   * @return deletion and overlap count
   */
  public static int deletionFromReadAndOverlapCount(final int[] actions) {
    assert isEqual(INSERTION_INTO_REFERENCE, 3); // this method relies critically on this
    assert isEqual(CG_OVERLAP_IN_READ, INSERTION_INTO_REFERENCE + 8); // this method relies critically on this
    int deletions = 0;
    if (actions.length > ACTIONS_START_INDEX) {
      final int lim = ACTIONS_START_INDEX + ((actionsCount(actions) - 1) >> ACTIONS_PER_INT_SHIFT);
      for (int k = ACTIONS_START_INDEX; k <= lim; ++k) {
        final int v = actions[k] & 0x77777777;
        // Could actually do this popcount slightly faster since we only care about odd number bits are &
        final int delCmds = v & (v >> 1) & (~v >> 2) & (~v >> 3);
        deletions += pseudopopcount(delCmds & 0x11111111);
      }
    }
    return deletions;
  }

  /**
   * @param actions actions array
   * @return the length on the template covered by the operations in this alignment
   */
  public static int templateLength(final int[] actions) {
    int ret = 0;
    final CommandIterator ci = new CommandIterator();
    ci.setActions(actions);
    while (ci.hasNext()) {
      switch (ci.next()) {
        case SAME:
        case MISMATCH:
        case DELETION_FROM_REFERENCE:
        case CG_GAP_IN_READ:
        case UNKNOWN_TEMPLATE:
        case UNKNOWN_READ:
          ++ret;
          break;
        case CG_OVERLAP_IN_READ:
          --ret;
          break;
        default:
      }
    }
    return ret;
  }

  /**
   * @param actions actions array
   * @return the length on the read covered by the operations in this alignment
   */
  public static int readLength(final int[] actions) {
    int ret = 0;
    final CommandIterator ci = new CommandIterator();
    ci.setActions(actions);
    while (ci.hasNext()) {
      switch (ci.next()) {
        case SAME:
        case MISMATCH:
        case INSERTION_INTO_REFERENCE:
        case UNKNOWN_TEMPLATE:
        case UNKNOWN_READ:
        case SOFT_CLIP:
          ++ret;
          break;
        default:
      }
    }
    return ret;
  }

  /**
   * Calculates the read length difference due to indels.
   * Deletions result in negative scores, Insertions result in positive scores.
   * @param actions actions array
   * @return read length difference due to indels
   */
  protected static int indelLength(final int[] actions) {
    return insertionIntoReadAndGapCount(actions) - deletionFromReadAndOverlapCount(actions);
  }

  /**
   * Returns the zero based exclusive end position of this read/actions pair on the template.
   * @param actions the actions to find the end position of
   * @return the zero based exclusive end position
   */
  public static int zeroBasedTemplateEndPos(final int[] actions) {
    int k = ActionsHelper.actionsCount(actions) - 1;
    int p = ActionsHelper.ACTIONS_START_INDEX + (k >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    int actionsLeftInThisInt = (k & ActionsHelper.ACTIONS_COUNT_MASK) + 1;
    int buf = actions[p] >>> ActionsHelper.BITS_PER_ACTION * (ActionsHelper.ACTIONS_PER_INT - actionsLeftInThisInt);

    int tempPos = 0;
    while (k >= 0) {
      if (actionsLeftInThisInt == 0) {
        buf = actions[--p];
        actionsLeftInThisInt = ActionsHelper.ACTIONS_PER_INT;
      }
      switch (buf & SINGLE_ACTION_MASK) {
        case MISMATCH:
        case SAME:
        case UNKNOWN_READ:
        case UNKNOWN_TEMPLATE:
        case CG_GAP_IN_READ:
        case DELETION_FROM_REFERENCE:
          ++tempPos;
          break;
        case CG_OVERLAP_IN_READ:
          --tempPos;
          break;
        default:
          break;
      }

      buf >>= ActionsHelper.BITS_PER_ACTION;
      --actionsLeftInThisInt;
      --k;
    }
    return actions[TEMPLATE_START_INDEX] + tempPos;
  }

  /**
   * Return a count of the number of insertion into read operations in this alignment
   * CG gap commands are also included in this count.
   * @param actions actions array
   * @return insertion count
   */
  public static int insertionIntoReadAndGapCount(final int[] actions) {
    assert isEqual(DELETION_FROM_REFERENCE, 2); // this method relies critically on this
    assert isEqual(CG_GAP_IN_READ, DELETION_FROM_REFERENCE + 8); // this method relies critically on this
    int insertions = 0;
    if (actions.length > ACTIONS_START_INDEX) {
      final int lim = ACTIONS_START_INDEX + ((actionsCount(actions) - 1) >> ACTIONS_PER_INT_SHIFT);
      for (int k = ACTIONS_START_INDEX; k <= lim; ++k) {
        final int v = actions[k] & 0x77777777;
        // Could actually do this popcount slightly faster since we only care about odd number bits are &
        final int inserts = (~v >> 3) & (~v >> 2) & (v >> 1) & ~v;
        insertions += pseudopopcount(inserts & 0x11111111);
      }
    }
    return insertions;
  }

  /**
   * Write a protein to a stream accounting for the alignment.
   * @param os stream
   * @param sequence protein sequence
   * @param start offset into sequence
   * @param actions actions array
   * @param operation skip operation
   * @exception IOException if an I/O error occurs
   */
  public static void writeProtein(final OutputStream os, final byte[] sequence, final int start, final int[] actions, final int operation) throws IOException {
    int r = start;
    int k = actionsCount(actions) - 1;
    int p = ACTIONS_START_INDEX + (k >> ACTIONS_PER_INT_SHIFT);
    int actionsLeftInThisInt = (k & ACTIONS_COUNT_MASK) + 1;
    int buf = actions[p] >>> BITS_PER_ACTION * (ACTIONS_PER_INT - actionsLeftInThisInt);
    while (k >= 0) {
      if (actionsLeftInThisInt == 0) {
        buf = actions[--p];
        actionsLeftInThisInt = ACTIONS_PER_INT;
      }
      if ((buf & SINGLE_ACTION_MASK) == operation) {
        os.write('-');
      } else {
        os.write(Protein.pbase(sequence, r++));
      }
      buf >>= BITS_PER_ACTION;
      --actionsLeftInThisInt;
      --k;
    }
  }

  /**
   * Write a match line in tabular protein style.
   * @param os stream
   * @param read the read
   * @param template the template
   * @param actions alignment actions array
   * @param matrix scoring matrix
   * @return number of positive matches
   * @exception IOException if an error occurs
   */
  public static int writeProteinMatches(final OutputStream os, final byte[] read, final byte[] template, final int[] actions, final ProteinScoringMatrix matrix) throws IOException {
    int t = zeroBasedTemplateStart(actions);
    int positive = 0;
    int r = 0;
    int k = actionsCount(actions) - 1;
    int p = ACTIONS_START_INDEX + (k >> ACTIONS_PER_INT_SHIFT);
    int actionsLeftInThisInt = (k & ACTIONS_COUNT_MASK) + 1;
    int buf = actions[p] >>> BITS_PER_ACTION * (ACTIONS_PER_INT - actionsLeftInThisInt);
    while (k >= 0) {
      if (actionsLeftInThisInt == 0) {
        buf = actions[--p];
        actionsLeftInThisInt = ACTIONS_PER_INT;
      }
      final int c;
      switch (buf & SINGLE_ACTION_MASK) {
      case SAME:
        c = Protein.pbase(read, r++);
        ++t;
        ++positive;
        break;
      case INSERTION_INTO_REFERENCE:
        ++r;
        c = ' ';
        break;
      case DELETION_FROM_REFERENCE:
        ++t;
        c = ' ';
        break;
      default: // MISMATCH
        final int aa = t >= 0 && t < template.length ? template[t] : Protein.X.ordinal();
        if (matrix.score(read[r], aa) > 0) {
          c = '+';
          ++positive;
        } else {
          c = ' ';
        }
        ++t;
        ++r;
        break;
      }
      os.write(c);
      buf >>= BITS_PER_ACTION;
      --actionsLeftInThisInt;
      --k;
    }
   return positive;
  }

  private static final char[] TO_STRING_CHARACTERS = {SamUtils.CIGAR_SAME, SamUtils.CIGAR_MISMATCH, SamUtils.CIGAR_DELETION_FROM_REF, SamUtils.CIGAR_INSERTION_INTO_REF, SamUtils.CIGAR_SOFT_CLIP, (char) -1, (char) -1, (char) -1, (char) -1, (char) -1, SamUtils.CIGAR_GAP_IN_READ, SamUtils.CIGAR_OVERLAP_IN_READ, SamUtils.CIGAR_UNKNOWN_TEMPLATE, SamUtils.CIGAR_UNKNOWN_READ};
  private static final char[] TO_READSIM_CHARACTERS = {'.', 'X', 'D', 'I', 'S', (char) -1, (char) -1, (char) -1, (char) -1, (char) -1, 'N', 'B'};

  /** @return the number of actions we know about */
  public static int getNumActions() {
    return TO_STRING_CHARACTERS.length;
  }

  /**
   * Converts an actions array to a readable string for debugging purposes
   * @param actions the array of actions to convert
   * @return a string representation of the actions
   */
  public static String toString(final int[] actions) {
    final int count = actionsCount(actions);
    final StringBuilder matches = new StringBuilder(count);

    int k = ActionsHelper.actionsCount(actions) - 1;
    int p = ActionsHelper.ACTIONS_START_INDEX + (k >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    int actionsLeftInThisInt = (k & ActionsHelper.ACTIONS_COUNT_MASK) + 1;
    int buf = actions[p] >>> ActionsHelper.BITS_PER_ACTION * (ActionsHelper.ACTIONS_PER_INT - actionsLeftInThisInt);
    while (k >= 0) {
      if (actionsLeftInThisInt == 0) {
        buf = actions[--p];
        actionsLeftInThisInt = ActionsHelper.ACTIONS_PER_INT;
      }
      final int tmp = buf & SINGLE_ACTION_MASK;
      if (tmp != NOOP) {
        matches.append(TO_STRING_CHARACTERS[tmp]);
      }
      buf >>= ActionsHelper.BITS_PER_ACTION;
      --actionsLeftInThisInt;
      --k;
    }
    return matches.toString();
  }

  /**
   * Parses a string in the for produces by  the to string method above
   * @param actions the string representation of the actions
   * @param zeroBasedStart the zero based template start position
   * @param score the alignment score
   * @return an actions array
   */
  public static int[] build(String actions, int zeroBasedStart, int score) {
    final int[] r = new int[ACTIONS_START_INDEX + (actions.length() + ACTIONS_PER_INT - 1) / ACTIONS_PER_INT];
    r[TEMPLATE_START_INDEX] = zeroBasedStart;
    r[ACTIONS_LENGTH_INDEX] = actions.length();
    r[ALIGNMENT_SCORE_INDEX] = score;
    int k = actions.length() - 1;
    int p = ActionsHelper.ACTIONS_START_INDEX + (k >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    int buf = 0;
    int i = 0;
    int actionsLeftInThisInt = (k & ActionsHelper.ACTIONS_COUNT_MASK) + 1;
    while (k >= 0) {
      --actionsLeftInThisInt;
      buf |= charToAction(actions.charAt(i)) << (BITS_PER_ACTION * (ACTIONS_PER_INT - actionsLeftInThisInt - 1));
      --k;
      ++i;
      if (actionsLeftInThisInt == 0) {
        r[p--] = buf;
        buf = 0;
        actionsLeftInThisInt = ACTIONS_PER_INT;
      }
    }
    assert buf == 0;
    return r;
  }

  private static int charToAction(final char c) {
    switch (c) {
      case SamUtils.CIGAR_SAME:
      return SAME;
      case SamUtils.CIGAR_MISMATCH:
      return MISMATCH;
      case SamUtils.CIGAR_INSERTION_INTO_REF:
        return INSERTION_INTO_REFERENCE;
      case SamUtils.CIGAR_DELETION_FROM_REF:
        return DELETION_FROM_REFERENCE;
      case SamUtils.CIGAR_GAP_IN_READ:
        return CG_GAP_IN_READ;
      case SamUtils.CIGAR_OVERLAP_IN_READ:
        return CG_OVERLAP_IN_READ;
      case SamUtils.CIGAR_UNKNOWN_READ:
        return UNKNOWN_READ;
      case SamUtils.CIGAR_UNKNOWN_TEMPLATE:
      return UNKNOWN_TEMPLATE;
      case SamUtils.CIGAR_SOFT_CLIP:
        return SOFT_CLIP;
      default:
      throw new IllegalArgumentException();
    }
  }

  /**
   * Return the CIGAR character for a given action.
   * @param action action to get CIGAR character for
   * @return CIGAR character
   */
  public static char actionToChar(final int action) {
    return TO_READSIM_CHARACTERS[action];
  }

  /**
   * prepend a new array of actions to the start of the workspace
   *
   * WARNING: if the workspace ends with a deletion and you prepend more deletes
   * the gap penalty won't be correctly accounted for and you will get the wrong
   * alignment score you will need to account for this.
   * TODO: Account for this properly by inspecting the first action in original
   * workspace and adjust gap penalties accordingly
   *
   * @param workspace the workspace to prepend to
   * @param prefix the actions to prepend
   */
  protected static void prepend(int[] workspace, int[] prefix) {
    if (workspace[ALIGNMENT_SCORE_INDEX] == Integer.MAX_VALUE
        || prefix[ALIGNMENT_SCORE_INDEX] == Integer.MAX_VALUE) {
      workspace[ALIGNMENT_SCORE_INDEX] = Integer.MAX_VALUE;
    } else {
      workspace[ALIGNMENT_SCORE_INDEX] += prefix[ALIGNMENT_SCORE_INDEX];
    }
    final int lastIndex = workspace[ACTIONS_LENGTH_INDEX];
    int workspaceEnd = ACTIONS_START_INDEX + (lastIndex >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    int actionsInWorkspaceInt = ACTIONS_PER_INT - (lastIndex & ActionsHelper.ACTIONS_COUNT_MASK);
    int prefixIndex = ACTIONS_START_INDEX;
    int actionsInPrefixInt = ACTIONS_PER_INT;
    int buf;
    if (actionsInWorkspaceInt == ActionsHelper.ACTIONS_PER_INT) {
      buf = 0;
    } else {
      buf = workspace[workspaceEnd];
    }
    for (int i = 0; i < prefix[ACTIONS_LENGTH_INDEX]; ++i) {
      --actionsInWorkspaceInt;
      --actionsInPrefixInt;
      buf |= (prefix[prefixIndex] >> (BITS_PER_ACTION * actionsInPrefixInt) & SINGLE_ACTION_MASK) << (BITS_PER_ACTION * actionsInWorkspaceInt);
      if (actionsInWorkspaceInt == 0) {
        workspace[workspaceEnd] = buf;
        buf = 0;
        ++workspaceEnd;
        actionsInWorkspaceInt = ACTIONS_PER_INT;
      }
      if (actionsInPrefixInt == 0) {
        actionsInPrefixInt = ACTIONS_PER_INT;
        ++prefixIndex;
      }
    }
    workspace[workspaceEnd] = buf;
    workspace[ACTIONS_LENGTH_INDEX] += prefix[ACTIONS_LENGTH_INDEX];
    workspace[TEMPLATE_START_INDEX] = prefix[TEMPLATE_START_INDEX];
  }

  /**
   * Prepend a run of a single type of action.
   *
   * WARNING: if the workspace ends with a deletion and you prepend more deletes
   * the gap penalty won't be correctly accounted for and you will get the wrong
   * alignment score you will need to account for this.
   * TODO: Account for this properly by inspecting the first action in original
   * workspace and adjust gap penalties accordingly
   * @param workspace the workspace to append to
   * @param count the length of the run
   * @param type the type of action
   * @param score alignment score for the run
   */
  public static void prepend(int[] workspace, int count, int type, int score) {
    assert count > 0 : "illegal prepend " + count;
    // TODO: handle new CG commands
    if (workspace[ALIGNMENT_SCORE_INDEX] == Integer.MAX_VALUE
        || score == Integer.MAX_VALUE) {
      workspace[ALIGNMENT_SCORE_INDEX] = Integer.MAX_VALUE;
    } else {
      workspace[ALIGNMENT_SCORE_INDEX] += score;
    }
    final int lastIndex = workspace[ACTIONS_LENGTH_INDEX];
    int workspaceEnd = ActionsHelper.ACTIONS_START_INDEX + (lastIndex >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    int actionsLeftInThisInt = ActionsHelper.ACTIONS_PER_INT - (lastIndex & ActionsHelper.ACTIONS_COUNT_MASK);
    int buf;
    if (actionsLeftInThisInt == ActionsHelper.ACTIONS_PER_INT) {
      buf = 0;
    } else {
      buf = workspace[workspaceEnd];
    }
    for (int i = 0; i < count; ++i) {
      --actionsLeftInThisInt;
      buf |= type << (BITS_PER_ACTION * actionsLeftInThisInt);
      if (actionsLeftInThisInt == 0) {
        workspace[workspaceEnd++] = buf;
        buf = 0;
        actionsLeftInThisInt = ACTIONS_PER_INT;
      }
    }
    workspace[workspaceEnd] = buf;
    workspace[ACTIONS_LENGTH_INDEX] += count;
    if (type != INSERTION_INTO_REFERENCE) {
      workspace[TEMPLATE_START_INDEX] -= count;
    }
  }

  /**
   * Reverses the order of all the actions, in-place in the array.
   * It does not change the score or start position fields.
   * NOTE: this is quite inefficient and allocates temporary memory.
   * @param workspace actions array
   */
  protected static void reverse(int[] workspace) {
    final int actionsCount = actionsCount(workspace);
    final byte[] acts = new byte[actionsCount];
    final CommandIterator iter = ActionsHelper.iterator(workspace);
    int len = 0;
    while (iter.hasNext()) {
      acts[len++] = (byte) iter.next();
    }
    // the actions are now in forward order in 'acts'.
    int k = actionsCount - 1;
    int p = ActionsHelper.ACTIONS_START_INDEX + (k >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    int buf = 0;
    int actionsLeftInThisInt = (k & ActionsHelper.ACTIONS_COUNT_MASK) + 1;
    while (k >= 0) {
      --actionsLeftInThisInt;
      buf |= acts[k] << (BITS_PER_ACTION * (ACTIONS_PER_INT - actionsLeftInThisInt - 1));
      --k;
      if (actionsLeftInThisInt == 0) {
        workspace[p--] = buf;
        buf = 0;
        actionsLeftInThisInt = ACTIONS_PER_INT;
      }
    }
    assert buf == 0;
  }

  /**
   * Modifies <code>numToClip</code> actions from one end of an actions array to soft clip actions.
   * Does NOT modify start position.
   * @param actions the actions array to modify
   * @param front true to modify the end of the actions array that is leftmost on the template
   * @param numToClip number of actions to convert to soft clips
   * @param numToNoop number of actions to convert to <code>noops</code>.
   */
  public static void softClip(int[] actions, boolean front, int numToClip, int numToNoop) {
    int position = front ? ActionsHelper.actionsCount(actions) : 0;
    for (int i = 0; i < numToClip + numToNoop; ++i) {
      if (front) {
        --position;
      }
      final int arrayIndex = ACTIONS_START_INDEX + (position >> ACTIONS_PER_INT_SHIFT); //index of int in actions array to edit
      int buf = actions[arrayIndex];

      final int shiftAmount = BITS_PER_ACTION * (ACTIONS_PER_INT - 1 - (position & ACTIONS_COUNT_MASK));
      final int inverseMask = Integer.MAX_VALUE - (0xF << shiftAmount);
      final int newValBits = (i < numToNoop ? NOOP : SOFT_CLIP) << shiftAmount;

      buf &= inverseMask; //need to wipe all the bits in the mask area first.
      buf |= newValBits;
      actions[arrayIndex] = buf;
      if (!front) {
        ++position;
      }
    }
  }
}
