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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.PortableRandom;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * single end machine with random read lengths
 */
@TestClass(value = {"com.rtg.simulation.reads.FourFiveFourSingleEndMachineTest", "com.rtg.simulation.reads.IonTorrentSingleEndMachineTest"})
public abstract class SingleEndRandomLengthMachine extends AbstractMachine {

  private static final int NUMBER_TRIES = 1000;

  private int mMinSize;
  private int mMaxSize;
  protected final PortableRandom mReadSizeRandom;
  protected final PortableRandom mFrameRandom;

  /**
   * Construct with given priors and seed
   * @param params priors
   * @param randomSeed seed for random number generation
   */
  public SingleEndRandomLengthMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params);
    mReadSizeRandom = new PortableRandom(randomSeed);
    mFrameRandom = new PortableRandom();
  }

  protected void reseedErrorRandom() {
    final long seed = mReadSizeRandom.nextLong();
    mFrameRandom.setSeed(seed + 2);
    super.reseedErrorRandom(seed + 2);
  }

  /**
   * Set the minimum size of the total lengths of both sides of a read
   * @param size the size
   */
  public void setMinSize(int size) {
    mMinSize = size;
  }

  /**
   * Set the maximum size of the total length of a read
   * @param size the size
   */
  public void setMaxSize(int size) {
    mMaxSize = size;
  }

  @Override
  public boolean isPaired() {
    return false;
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
    final double mid = (mMaxSize + mMinSize) * 0.5 + 0.5;
    final double width = (mMaxSize - mMinSize) * 0.25; // 2 std devs per side
    int readLength = 0;
    for (int x = 0; x < NUMBER_TRIES; ++x) {
      readLength = (int) (mReadSizeRandom.nextGaussian() * width + mid);
      if ((readLength >= mMinSize) && (readLength <= mMaxSize)) {
        break;
      }
    }
    final boolean forward = mFrameRandom.nextBoolean();
    final int pos;
    if (forward) {
      pos = processBackwards(length - 1, data, length, -1, readLength);
    } else {
      pos = processBackwards(0, data, length, 1, readLength);
    }
    final String cigar = getCigar(forward);  //forward rather than !forward because we used processBackwards
    final String name = formatReadName(id, forward ? 'F' : 'R', cigar, fragmentStart, pos);
    mReadWriter.writeRead(name, mReadBytes, mQualityBytes, mReadBytesUsed);
    mResidueCount += mReadBytesUsed;
  }
}
