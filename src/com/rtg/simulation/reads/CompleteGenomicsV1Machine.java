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

package com.rtg.simulation.reads;

import com.rtg.mode.DNA;
import com.rtg.reader.CgUtils;
import com.rtg.simulation.SimulationUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Generator for Complete Genomics paired end reads.
 *
 */
public class CompleteGenomicsV1Machine extends CompleteGenomicsMachine {

  private static final int NUMBER_TRIES = 1000;
  private static final int READ_LENGTH = CgUtils.CG_RAW_READ_LENGTH;

  protected final double[] mOverlapDistribution;
  protected final double[] mGapDistribution;
  protected final double[] mSmallGapDistribution;

  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public CompleteGenomicsV1Machine(AbstractMachineErrorParams params, long randomSeed) {
    super(params, randomSeed);
    mOverlapDistribution = SimulationUtils.cumulativeDistribution(params.overlapDistribution());
    mGapDistribution = SimulationUtils.cumulativeDistribution(params.gapDistribution());
    mSmallGapDistribution = SimulationUtils.cumulativeDistribution(params.smallGapDistribution());
    if (mOverlapDistribution.length != 5) {
      throw new IllegalArgumentException("Missing or incorrect distribution for CG V1 overlap");
    }
    if (mGapDistribution.length != 5) {
      throw new IllegalArgumentException("Missing or incorrect distribution for CG V1 gap");
    }
    if (mSmallGapDistribution.length != 4) {
      throw new IllegalArgumentException("Missing or incorrect distribution for CG V1 small gap");
    }
    mQualityBytes = new byte[READ_LENGTH];
    mReadBytes = new byte[READ_LENGTH];
    mWorkspace = new int[READ_LENGTH];
  }

  private int generateOverlapLength() {
    return SimulationUtils.chooseFromCumulative(mOverlapDistribution, mSegmentRandom.nextDouble()) - 4;
  }

  private int generateGapLength() {
    return SimulationUtils.chooseFromCumulative(mGapDistribution, mSegmentRandom.nextDouble()) + 4;
  }

  private int generateSmallGapLength() {
    return SimulationUtils.chooseFromCumulative(mSmallGapDistribution, mSegmentRandom.nextDouble());
  }

  @Override
  protected String generateRead(String id, int fragmentStart, byte[] data, int length, boolean forward, boolean leftArm) {
    final int startFrom;
    final int direction;
    final char frame;
    if (forward) {
      frame = 'F';
    } else {
      frame = 'R';
    }
    if (leftArm ^ !forward) {
      direction = 1;
      startFrom = 0;
    } else {
      direction = -1;
      startFrom = length - 1;
    }
    for (int x = 0; x < NUMBER_TRIES; ++x) {
      resetCigar();
      int refPos = readBases(startFrom, data, length, direction, 5);
      final int overlap = generateOverlapLength();
      refPos += overlap * direction;
      int tLen = direction == 1 ? length - refPos : length - startFrom + refPos;
      if (refPos < 0 || refPos >= length) {
        continue;
      }
      updateCigarWithPositiveOrNegativeSkip(overlap);
      refPos = readBases(refPos, data, tLen, direction, 10);

      // Currently hardcoded gap of 0
      final int smallgap = generateSmallGapLength();
      refPos += smallgap * direction;
      tLen = direction == 1 ? length - refPos : length - startFrom + refPos;
      if (refPos < 0 || refPos >= length) {
        continue;
      }
      updateCigarWithPositiveOrNegativeSkip(smallgap);
      refPos = readBases(refPos, data, tLen, direction, 10);

      final int gap = generateGapLength();
      refPos += gap * direction;
      tLen = direction == 1 ? length - refPos : length - startFrom + refPos;
      if (refPos < 0 || refPos >= length) {
        continue;
      }
      updateCigarWithPositiveOrNegativeSkip(gap);
      refPos = readBases(refPos, data, tLen, direction, 10);
      //System.out.println("Generated overlap:" + overlap + " gap:" + gap);

      final int newStart = Math.min(startFrom, refPos - direction);
      if (forward ^ direction == 1) {
        reverse();
      }
      if (!forward) {
        DNA.complementInPlace(mReadBytes, 0, mReadBytesUsed);
      }
      final String cigar = getCigar(direction == -1);
      return formatReadName(id, frame, cigar, fragmentStart, newStart);
    }
    //Diagnostic.developerLog(id + " fragmentStart: " + fragmentStart + " length: " + length + " forward: " + forward + " leftArm: " + leftArm);
    throw new NoTalkbackSlimException("Unable to generate a valid read with given priors in " + NUMBER_TRIES + " attempts");
  }

  @Override
  public MachineType machineType() {
    return MachineType.COMPLETE_GENOMICS;
  }
}
