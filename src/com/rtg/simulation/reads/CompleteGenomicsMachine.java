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
import com.rtg.alignment.ActionsHelper;
import com.rtg.reader.PrereadType;
import com.rtg.util.PortableRandom;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Generator for Complete Genomics paired end reads.
 *
 */
@TestClass("com.rtg.simulation.reads.CompleteGenomicsV1MachineTest")
public abstract class CompleteGenomicsMachine extends AbstractMachine {

  protected final PortableRandom mFrameRandom;
  protected final PortableRandom mSegmentRandom;

  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public CompleteGenomicsMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params);
    mFrameRandom = new PortableRandom(randomSeed);
    mSegmentRandom = new PortableRandom(randomSeed * 3);
  }

  protected void reverse() {
    for (int left = 0, right = mReadBytesUsed - 1; left < right; ++left, --right) {
      // exchange the first and last
      final byte temp = mReadBytes[left];
      mReadBytes[left] = mReadBytes[right];
      mReadBytes[right] = temp;
    }
  }

  protected void updateCigarWithPositiveOrNegativeSkip(final int skip) {
    if (skip > 0) {
      // N operation
      addCigarState(skip, ActionsHelper.CG_GAP_IN_READ);
    } else if (skip < 0) {
      // B operation
      addCigarState(-skip, ActionsHelper.CG_OVERLAP_IN_READ);
    }
  }

  @Override
  public boolean isPaired() {
    return true;
  }

  @Override
  public PrereadType prereadType() {
    return PrereadType.CG;
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {
    reseedErrorRandom(mFrameRandom.nextLong());
    final boolean forwardFrame = mFrameRandom.nextBoolean();

    final String nameLeft = generateRead(id, fragmentStart, data, length, forwardFrame, true);

    if (mReadBytesUsed == mReadBytes.length) {
      mReadWriter.writeLeftRead(nameLeft, mReadBytes, mQualityBytes, mReadBytes.length);
      mResidueCount += mReadBytes.length;
    } else {
      throw new FragmentTooSmallException(length, mReadBytes.length);
    }

    final String nameRight = generateRead(id, fragmentStart, data, length, forwardFrame, false);
    if (mReadBytesUsed == mReadBytes.length) {
      mReadWriter.writeRightRead(nameRight, mReadBytes, mQualityBytes, mReadBytes.length);
      mResidueCount += mReadBytes.length;
    } else {
      throw new FragmentTooSmallException(length, mReadBytes.length);
    }
  }

  protected abstract String generateRead(String id, int fragmentStart, byte[] data, int length, boolean forwardFrame, boolean leftArm);

}
