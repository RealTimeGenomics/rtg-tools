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

import java.io.IOException;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.PortableRandom;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * 454 paired end machine
 */
public class FourFiveFourPairedEndMachine extends AbstractMachine {

  private static final int NUMBER_TRIES = 1000;
  private static final int MIN_SIDE_LENGTH = 1; // Ensure the smallest side has at least this many bases
  private static final int READ_DIRECTION = GlobalFlags.getIntegerValue(ToolsGlobalFlags.READ_STRAND); // -1 = reverse, 0 = random, 1 = forward

  private int mMinPairSize;
  private int mMaxPairSize;
  protected final PortableRandom mPairSizeRandom;
  protected final PortableRandom mPairPositionRandom;
  protected final PortableRandom mFrameRandom;

  /**
   * Construct with given priors and seed
   * @param params priors
   * @param randomSeed seed for random number generation
   */
  public FourFiveFourPairedEndMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params);
    mPairSizeRandom = new PortableRandom(randomSeed);
    mPairPositionRandom = new PortableRandom();
    mFrameRandom = new PortableRandom();
  }

  protected void reseedErrorRandom() {
    final long seed = mPairSizeRandom.nextLong();
    mPairPositionRandom.setSeed(seed + 1);
    mFrameRandom.setSeed(seed + 2);
    super.reseedErrorRandom(seed + 2);
  }

  /**
   * Set the minimum size of the total lengths of both sides of a read pair
   * @param size the size
   */
  public void setMinPairSize(int size) {
    mMinPairSize = size;
  }

  /**
   * Set the maximum size of the total length of a read pair
   * @param size the size
   */
  public void setMaxPairSize(int size) {
    mMaxPairSize = size;
  }

  @Override
  public boolean isPaired() {
    return true;
  }

  void updateWorkingSpace(int length) {
    if (mReadBytes.length < length) {
      mReadBytes = new byte[length];
      mWorkspace = new int[length];
      mQualityBytes = new byte[length];
    }
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {
    updateWorkingSpace(length);
    reseedErrorRandom();
    final double mid = (mMaxPairSize + mMinPairSize) * 0.5 + 0.5;
    final double width = (mMaxPairSize - mMinPairSize) * 0.25; // 2 std devs per side
    int pairLength = 0;
    for (int x = 0; x < NUMBER_TRIES; ++x) {
      pairLength = (int) (mPairSizeRandom.nextGaussian() * width + mid);
      if ((pairLength >= mMinPairSize) && (pairLength <= mMaxPairSize)) {
        break;
      }
    }

    //this is the position on the subFragment that we cross the adapter
    final int pairPosition = mPairPositionRandom.nextInt(pairLength - (2 * MIN_SIDE_LENGTH)) + MIN_SIDE_LENGTH;

    final boolean forward = READ_DIRECTION == 0 ? mFrameRandom.nextBoolean() : READ_DIRECTION > 0;
    int pos;
    if (forward) {
      pos = process(length - pairLength + pairPosition, data, pairLength - pairPosition, 1, length);
    } else {
      pos = process(pairPosition, data, pairPosition, -1, length);
    }
    String cigar = getCigar(!forward);
    String name = formatReadName(id, forward ? 'F' : 'R', cigar, fragmentStart, pos);
    mReadWriter.writeLeftRead(name, mReadBytes, mQualityBytes, mReadBytesUsed);
    mResidueCount += mReadBytesUsed;
      //System.out.println("length=" + length + " pairLength: " + pairLength + " pp: " + pairPosition + " pos=" + pos + " fs=" + fragmentStart + " name" + name);

    if (forward) {
      pos = process(0, data, pairPosition, 1, length);
    } else {
      pos = process(length - 1, data, pairLength - pairPosition, -1, length);
    }
    cigar = getCigar(!forward);
    name = formatReadName(id, forward ? 'F' : 'R', cigar, fragmentStart, pos);
    mReadWriter.writeRightRead(name, mReadBytes, mQualityBytes, mReadBytesUsed);
    mResidueCount += mReadBytesUsed;
  }

  @Override
  public MachineType machineType() {
    return MachineType.FOURFIVEFOUR_PE;
  }
}
