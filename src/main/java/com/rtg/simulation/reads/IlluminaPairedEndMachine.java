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

import static com.rtg.launcher.globals.GlobalFlags.getIntegerValue;

import java.io.IOException;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.mode.DnaUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Illumina paired end read simulator
 */
public class IlluminaPairedEndMachine extends AbstractIlluminaMachine {

  private static final int READ_DIRECTION = getIntegerValue(ToolsGlobalFlags.READ_STRAND); // -1 = reverse, 0 = random, 1 = forward

  // PE Read 2 sequencing primer
  private static final String PE_EXTENSION = "CGGTCTCGGCATTCCTGCTGAACCGCTCTTCCGATCT"  + "NNNNN" + StringUtils.reverse("ACACTCTTTCCCTACACGACGCTCTTCCGATCT");

  protected int mLeftReadLength;
  protected int mRightReadLength;

  {
    mExtension = DnaUtils.encodeString(GlobalFlags.isSet(ToolsGlobalFlags.READ_THROUGH) ? GlobalFlags.getStringValue(ToolsGlobalFlags.READ_THROUGH) : PE_EXTENSION);
  }

  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public IlluminaPairedEndMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params, randomSeed);
  }

  /**
   * Constructs with seed and default Illumina priors
   * @param randomSeed random seed
   * @throws InvalidParamsException if fails to construct priors
   * @throws IOException whenever
   */
  public IlluminaPairedEndMachine(long randomSeed) throws IOException {
    super(randomSeed);
  }

  private void setBuffers() {
    final int len = Math.max(mLeftReadLength, mRightReadLength);
    mQualityBytes = new byte[len];
    mReadBytes = new byte[len];
    mWorkspace = new int[Math.max(20, len)];
  }

  /**
   * Sets length of left arm of generated reads
   * @param val the length
   */
  public void setLeftReadLength(int val) {
    mLeftReadLength = val;
    setBuffers();
  }

  /**
   * Sets length of right arm of generated reads
   * @param val the length
   */
  public void setRightReadLength(int val) {
    mRightReadLength = val;
    setBuffers();
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {
    reseedErrorRandom(mFrameRandom.nextLong());
    final boolean forwardFirst = READ_DIRECTION == 0 ? mFrameRandom.nextBoolean() : READ_DIRECTION > 0;
    final String nameLeft = generateRead(id, fragmentStart, data, length, forwardFirst, mLeftReadLength);
    if (mReadBytesUsed == mLeftReadLength) {
      mReadWriter.writeLeftRead(nameLeft, mReadBytes, mQualityBytes, mLeftReadLength);
      mResidueCount += mLeftReadLength;
    } else {
      throw new FragmentTooSmallException(length, mLeftReadLength);
    }
    final String nameRight = generateRead(id, fragmentStart, data, length, !forwardFirst, mRightReadLength);

    if (mReadBytesUsed == mRightReadLength) {
      mReadWriter.writeRightRead(nameRight, mReadBytes, mQualityBytes, mRightReadLength);
      mResidueCount += mRightReadLength;
    } else {
      throw new FragmentTooSmallException(length, mRightReadLength);
    }
  }

  @Override
  public boolean isPaired() {
    return true;
  }

  @Override
  public MachineType machineType() {
    return MachineType.ILLUMINA_PE;
  }
}
