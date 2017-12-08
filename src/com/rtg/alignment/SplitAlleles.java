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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.rtg.mode.DnaUtils;
import com.rtg.util.MathUtils;

/**
 * Construct allelic primitives for alternates against a reference.
 */
public class SplitAlleles {

  /** The column of the partitioned result corresponding to the reference allele. */
  public static final int REFERENCE_COLUMN_INDEX = 0;

  private final byte[] mRef;
  private final byte[][] mAlts;
  private final UnidirectionalEditDistance mAligner = new GotohEditDistance(1, 1, 1, 10, false); // open, extend, subs, unknowns, stop-template
  // Map from allele to position in partitioned array; order is ref, alt0, alt1, ...
  private final HashMap<String, Integer> mId = new HashMap<>();

  SplitAlleles(final String ref, final String... alts) {
    mRef = DnaUtils.encodeString(ref);
    mId.put(ref, REFERENCE_COLUMN_INDEX);
    mAlts = new byte[alts.length][];
    for (int k = 0; k < alts.length; ++k) {
      mAlts[k] = DnaUtils.encodeString(alts[k]);
      mId.put(alts[k], k + 1); // If multiple alts are the same it will end up with last value, but that's ok
    }
  }

  /**
   * Construct a splitter for the specified reference and alternate alleles.
   * @param ref reference allele
   * @param alts alternate alleles
   */
  public SplitAlleles(final String ref, final Collection<String> alts) {
    this(ref, alts.toArray(new String[alts.size()]));
  }

  /**
   * Return the index within the partition array of given original allele,
   * or -1 if they allele is not present.
   * @param allele allele to get index of
   * @return index for allele
   */
  public int getColumnIndex(final String allele) {
    return MathUtils.unboxNatural(mId.get(allele));
  }

  private int[] align(final byte[] alt) {
    return mAligner.calculateEditDistanceFixedBoth(alt, 0, alt.length, mRef, 0, mRef.length, Integer.MAX_VALUE, Math.max(mRef.length, alt.length));
  }

  private String[] getCigarStrings(final byte[][] sequences) {
    final String[] expandedCigars = new String[sequences.length];
    for (int k = 0; k < sequences.length; ++k) {
      expandedCigars[k] = ActionsHelper.toString(align(sequences[k]));
    }
    return expandedCigars;
  }

  private String[] getCigarStrings(final String[] sequences) {
    final String[] expandedCigars = new String[sequences.length];
    for (int k = 0; k < sequences.length; ++k) {
      expandedCigars[k] = ActionsHelper.toString(align(DnaUtils.encodeString(sequences[k])));
    }
    return expandedCigars;
  }

  private void addSplit(byte[][] sequences, final Partition split, final int[] prevAltPos, final int[] altPos, final int prevRefPos, final int refPos) {
    //System.out.println("Split point at " + refPos);
    final String[] alleles = new String[sequences.length + 1]; // Position 0 is ref
    alleles[0] = getDna(mRef, prevRefPos, refPos);
    for (int k = 0; k < sequences.length; ++k) {
      alleles[k + 1] = getDna(sequences[k], prevAltPos[k], altPos[k]);
      prevAltPos[k] = altPos[k];
    }
    split.add(new Slice(prevRefPos, alleles));
  }

  private String getDna(final byte[] alt, final int start, final int end) {
    return DnaUtils.bytesToSequenceIncCG(alt, start, end - start);
  }

  private boolean updateForInsertion(final String[] expandedCigars, final int[] actPos, final int[] altPos, final int k) {
    boolean sawInsertion = false;
    while (actPos[k] < expandedCigars[k].length() && expandedCigars[k].charAt(actPos[k]) == 'I') {
      ++actPos[k];
      ++altPos[k];
      sawInsertion = true;
    }
    return sawInsertion;
  }

  private Partition getPartition(final byte[][] sequences, final String[] expandedCigars) {
    assert sequences.length == expandedCigars.length;

    final Partition partition = new Partition();

    // Current position in the actions array for each alternate allele
    final int[] actPos = new int[expandedCigars.length];
    // Current position in the allele for each alternate allele
    final int[] altPos = new int[expandedCigars.length];
    // Exclusive end position of the last reported base in each alternate allele
    final int[] prevAltPos = new int[expandedCigars.length];
    // End position on reference of last reported base
    int prevRefPos = 0;
    boolean prevIsRef = true;

    // Special case of a leading insertion
    boolean isLeadingInsertion = false;
    for (int k = 0; k < actPos.length; ++k) {
      isLeadingInsertion |= updateForInsertion(expandedCigars, actPos, altPos, k);
    }
    if (isLeadingInsertion) {
      prevRefPos = 0;
      prevIsRef = false;
    }

    for (int refPos = 0; refPos < mRef.length; ++refPos) {

      // Test if all alleles are reference at refPos
      boolean isRef = true;
      for (int k = 0; k < actPos.length; ++k) {
        isRef &= expandedCigars[k].charAt(actPos[k]) == '=';
      }

      // State change, output current region
      if (isRef != prevIsRef) {
        if (refPos > 0 || !prevIsRef) {
          addSplit(sequences, partition, prevAltPos, altPos, prevRefPos, refPos);
        }
        prevRefPos = refPos;
        prevIsRef = isRef;
      }

      // Handle insertions occurring after an all reference position
      boolean isInsertion = false;
      for (int k = 0; k < actPos.length; ++k) {
        isInsertion |= updateForInsertion(expandedCigars, actPos, altPos, k);
      }
      if (isInsertion) {
        // At least one allele had an insertion, redo this position now we have accumulated it
        refPos--;
        continue;
      }

      // Update the positions in the actions and alleles for the current refPos.
      for (int k = 0; k < actPos.length; ++k) {
        final char a = expandedCigars[k].charAt(actPos[k]++);
        switch (a) {
          case '=':
          case 'X':
            ++altPos[k];
            break;
          case 'D':
            break;
          default:
            throw new RuntimeException("Unexpected: " + a + " in " + expandedCigars[k] + " for allele " + k);
        }
        if (!isRef) {
          // We are already in a situation where at least one allele differs from reference,
          // so go ahead and include any insertions bases at this point
          updateForInsertion(expandedCigars, actPos, altPos, k);
        }
      }
    }

    addSplit(sequences, partition, prevAltPos, altPos, Math.max(0, prevRefPos), mRef.length);
    prevRefPos = mRef.length;

    // Special case of a trailing insertion.
    if (prevIsRef) {
      boolean isTrailingInsertion = false;
      for (int k = 0; k < actPos.length; ++k) {
        isTrailingInsertion |= updateForInsertion(expandedCigars, actPos, altPos, k);
      }
      if (isTrailingInsertion) {
        addSplit(sequences, partition, prevAltPos, altPos, prevRefPos, mRef.length);
      }
    }

    // At the end we should have read every action and use every allele base
    for (int k = 0; k < expandedCigars.length; ++k) {
      assert actPos[k] == expandedCigars[k].length();
      assert altPos[k] == sequences[k].length;
    }

    return partition;
  }

  /**
   * Return a partition of the alleles.
   * @return position and partitioned out bases
   */
  public Partition partition() {
    return getPartition(mAlts, getCigarStrings(mAlts));
  }

  /**
   * Return a list of partitions of the alleles. The first partition will be without
   * any extra sequence, and subsequent partitions with respect to each of the extra
   * sequences in turn.  Thus the total number of partitions is one more than the
   * number of extra sequences.
   * @param extraSequences extra sequences to be used one at a time
   * @return position and partitioned out bases
   */
  public List<Partition> partition(final String[] extraSequences) {
    if (extraSequences.length == 0) {
      return Collections.singletonList(partition());
    }

    final byte[][] extraSeq = new byte[extraSequences.length][];
    for (int k = 0; k < extraSequences.length; ++k) {
      extraSeq[k] = DnaUtils.encodeString(extraSequences[k]);
    }

    final String[] expandedCigars = getCigarStrings(mAlts);
    final String[] extraCigars = getCigarStrings(extraSequences);
    final List<Partition> res = new ArrayList<>(expandedCigars.length + 1);
    res.add(getPartition(mAlts, expandedCigars)); // The partition without extra sequences
    final byte[][] sequences = Arrays.copyOf(mAlts, mAlts.length + 1);
    final String[] cigars = Arrays.copyOf(expandedCigars, expandedCigars.length + 1);
    for (int k = 0; k < extraCigars.length; ++k) {
      cigars[cigars.length - 1] = extraCigars[k];
      sequences[sequences.length - 1] = extraSeq[k];
      res.add(getPartition(sequences, cigars)); // Do the partition with one extra sequence
    }
    return res;
  }

  /**
   * Remove the reference only parts of the partition.
   * @param partition initial partitioning
   * @return partition with reference pieces removed
   */
  public static Partition removeAllRef(final Partition partition) {
    final Partition retained = new Partition();
    for (final Slice pos : partition) {
      boolean allRef = true;
      final String[] alleles = pos.getAlleles();
      for (int k = 1; k < alleles.length; ++k) {
        allRef &= alleles[k].equals(alleles[0]);
      }
      if (!allRef) {
        retained.add(pos);
      }
    }
    return retained;
  }

  /**
   * Remove the reference only parts of each partition in the supplied list.
   * @param partition initial partitionings
   * @return partitions with reference pieces removed
   */
  public static List<Partition> removeAllRefList(final List<Partition> partition) {
    final List<Partition> retained = new ArrayList<>(partition.size());
    for (final Partition part : partition) {
      retained.add(removeAllRef(part));
    }
    return retained;
  }

  // Example command to run through a bunch of examples:
  // zcat snps.vcf.gz | awk '/^[^#]/{print $4,$5}' | tr ',' ' ' | while read; do echo "Testing: ${REPLY}"; java -ea com.rtg.alignment.SplitAlleles ${REPLY}; done

  /**
   * Split alleles given as strings, reference followed by any number of alternates.
   * @param args reference, alternates
   */
  public static void main(final String... args) {
    final String[] alts = new String[args.length - 1];
    System.arraycopy(args, 1, alts, 0, alts.length);
    final SplitAlleles pwa = new SplitAlleles(args[0], alts);
    final Partition split = removeAllRef(pwa.partition());
    for (final Slice s : split) {
      System.out.println(s);
    }
  }
}
