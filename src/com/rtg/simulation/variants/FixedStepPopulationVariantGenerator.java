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

package com.rtg.simulation.variants;

import java.io.IOException;
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.reader.SequencesReader;
import com.rtg.util.PortableRandom;
import com.rtg.util.QuickSort;
import com.rtg.util.intervals.SequenceIdLocus;
import com.rtg.util.intervals.SequenceIdLocusSimple;

/**
 * Generate specific population variants at given interval
 */
public class FixedStepPopulationVariantGenerator extends PopulationVariantGenerator {

  private static final class AlleleSortProxy implements QuickSort.SortProxy {
    private final PopulationVariant mVar;

    AlleleSortProxy(PopulationVariant var) {
      mVar = var;
    }

    @Override
    public int compare(long index1, long index2) {
      int cmp = mVar.mAlleles[(int) index1].length - mVar.mAlleles[(int) index2].length;
      if (cmp != 0) {
        return cmp;
      }
      for (int i = 0; i < mVar.mAlleles[(int) index1].length; ++i) {
        cmp = mVar.mAlleles[(int) index1][i] - mVar.mAlleles[(int) index2][i];
        if (cmp != 0) {
          return cmp;
        }
      }
      return 0;
    }

    @Override
    public void swap(long index1, long index2) {
      final byte[] t = mVar.mAlleles[(int) index1];
      mVar.mAlleles[(int) index1] = mVar.mAlleles[(int) index2];
      mVar.mAlleles[(int) index2] = t;
      final double e = mVar.mDistribution[(int) index1];
      mVar.mDistribution[(int) index1] = mVar.mDistribution[(int) index2];
      mVar.mDistribution[(int) index2] = e;
    }

    @Override
    public long length() {
      return mVar.mAlleles.length;
    }
  }

  // Effectively turns the population variant into a canonical version
  static void collapsePopulationVariant(PopulationVariant popVar) {
    QuickSort.sort(new AlleleSortProxy(popVar)); // Make it easier to collapse alleles
    // Remove duplicate alleles
    int endColl = popVar.mAlleles.length - 1; // Inclusive index bounds for identical alleles
    int startColl = endColl;
    int length = popVar.mAlleles.length;
    for (int i = endColl - 1; i >= 0; --i) {
      if (Arrays.equals(popVar.mAlleles[endColl], popVar.mAlleles[i])) {
        startColl = i;
      } else {
        length -= collapsePart(popVar, endColl, startColl, length);
        endColl = i;
        startColl = i;
      }
    }
    length -= collapsePart(popVar, endColl, startColl, length);
    // Remove == ref alleles
    for (int i = length - 1; i >= 0; --i) {
      if (Arrays.equals(popVar.mRef, popVar.mAlleles[i])) {
        System.arraycopy(popVar.mAlleles, i + 1, popVar.mAlleles, i, length - i - 1);
        // Don't need to adjust mDist numbers since they are absorbed into the implicit ref probability once removed
        System.arraycopy(popVar.mDistribution, i + 1, popVar.mDistribution, i, length - i - 1);
        --length;
      }
    }
    // Truncate arrays
    popVar.mAlleles = Arrays.copyOf(popVar.mAlleles, length);
    popVar.mDistribution = Arrays.copyOf(popVar.mDistribution, length);
  }

  private static int collapsePart(PopulationVariant popVar, int endColl, int startColl, int length) {
    if (startColl != endColl) {
      System.arraycopy(popVar.mAlleles, endColl + 1, popVar.mAlleles, startColl + 1, length - endColl - 1);
      for (int j = startColl + 1; j <= endColl; ++j) {
        popVar.mDistribution[startColl] += popVar.mDistribution[j]; // Collapse dist probabilities
      }
      System.arraycopy(popVar.mDistribution, endColl + 1, popVar.mDistribution, startColl + 1, length - endColl - 1);
    }
    return endColl - startColl;
  }

  private final Mutator mMutor;
  private final SequencesReader mReader;
  private final PortableRandom mRandom;
  private final byte[] mTemplate;
  private final int mPosAdj;
  private final double[] mAltDist;

  /**
   * @param reader reference sequences
   * @param distance distance apart for each variant
   * @param mutor mutator to use
   * @param random source of randomness
   * @param alleleFrequency desired allele frequency
   */
  public FixedStepPopulationVariantGenerator(SequencesReader reader, int distance, Mutator mutor, PortableRandom random, double alleleFrequency) {
    super(new FixedStepPositionGenerator(reader, distance));
    mMutor = mutor;
    mReader = reader;
    mPosAdj = mutor.isIndel() ? 1 : 0;
    mTemplate = new byte[mutor.getReferenceLength() + mPosAdj];
    mRandom = random;
    mAltDist = new double[] {alleleFrequency * 0.5, alleleFrequency * 0.5};
  }

  private static final class FixedStepPositionGenerator implements VariantPositionGenerator {
    private final SequencesReader mReader;
    private final int mDistance;
    private int mSeq;
    private int mPos;
    private FixedStepPositionGenerator(SequencesReader reader, int distance) {
      mReader = reader;
      mDistance = distance;
    }

    @Override
    public SequenceIdLocus nextVariantPosition() throws IOException {
      while (mSeq < mReader.numberSequences()) {
        if (mPos >= mReader.length(mSeq)) {
          ++mSeq;
          mPos = 0;
          continue;
        }
        final SequenceIdLocusSimple ret = new SequenceIdLocusSimple(mSeq, mPos);
        mPos += mDistance;
        return ret;
      }
      return null;
    }
  }

  private byte[] prependAnchorToHaplotype(final byte[] haplotype) {
    final byte[] anchored = new byte[haplotype.length + 1];
    anchored[0] = mTemplate[0];
    System.arraycopy(haplotype, 0, anchored, 1, haplotype.length);
    return anchored;
  }

  @Override
  PopulationVariant nextPopulationVariant() throws IOException {
    SequenceIdLocus vp;
    boolean validNs;
    int position;
    do {
      validNs = false;
      vp = mVariantPositionGenerator.nextVariantPosition();
      if (vp == null) {
        return null;
      }
      position = vp.getStart() - mPosAdj;
      if (position + mTemplate.length >= mReader.length(vp.getSequenceId())) {
        return null;
      }
//      System.err.println("length: " + mReader.length(vp.getReferenceId()));
//      System.err.println("position: " + position + " arrLength: " + mTemplate.length);
      if (position >= 0) {
        mReader.read(vp.getSequenceId(), mTemplate, position, mTemplate.length);
        for (byte b : mTemplate) {
          if (b != DNA.N.ordinal()) {
            validNs = true;
            break;
          }
        }
      }
    } while (position < 0 || !validNs);
    final MutatorResult result = mMutor.generateMutation(mTemplate, mPosAdj, mRandom);
    final PopulationVariant popVar = new PopulationVariant(new SequenceIdLocusSimple(vp.getSequenceId(), position));
    popVar.mAlleles = new byte[2][];
    popVar.mRef = Arrays.copyOf(mTemplate, mTemplate.length);
    if (mMutor.isIndel()) {
      // Alleviate VCF output by prepending extra base, already present in mTemplate array
      popVar.mAlleles[0] = prependAnchorToHaplotype(result.getFirstHaplotype());
      popVar.mAlleles[1] = prependAnchorToHaplotype(result.getSecondHaplotype());
    } else {
      popVar.mAlleles[0] = result.getFirstHaplotype();
      popVar.mAlleles[1] = result.getSecondHaplotype();
    }
    popVar.mDistribution = Arrays.copyOf(mAltDist, mAltDist.length);
    collapsePopulationVariant(popVar);
    return popVar;
  }

}
