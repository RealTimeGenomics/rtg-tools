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
import com.rtg.mode.DnaUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Illumina read generator machine implementation
 */
public class IlluminaSingleEndMachine extends AbstractIlluminaMachine {

  private static final String SE_EXTENSION = "ACACTCTTTCCCTACACGACGCTCTTCCGATCT" + "NNNNN" + StringUtils.reverse("ACACTCTTTCCCTACACGACGCTCTTCCGATCT");

  protected int mReadLength;

  {
    mExtension = DnaUtils.encodeString(GlobalFlags.isSet(ToolsGlobalFlags.READ_THROUGH) ? GlobalFlags.getStringValue(ToolsGlobalFlags.READ_THROUGH) : SE_EXTENSION);
  }

  /**
   * Constructs with seed and default Illumina priors
   * @param randomSeed random seed
   * @throws InvalidParamsException if fails to construct priors
   * @throws IOException whenever
   */
  public IlluminaSingleEndMachine(long randomSeed) throws InvalidParamsException, IOException {
    super(randomSeed);
  }

  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public IlluminaSingleEndMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params, randomSeed);
  }

  /**
   * Sets length of generated reads
   * @param val the length
   */
  public void setReadLength(int val) {
    mReadLength = val;
    mQualityBytes = new byte[mReadLength];
    mReadBytes = new byte[mReadLength];
    mWorkspace = new int[Math.max(4, mReadLength)];     //TODO perhaps should be 4+readlength?
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {
    reseedErrorRandom(mFrameRandom.nextLong());
    final String name = generateRead(id, fragmentStart, data, length, mFrameRandom.nextBoolean(), mReadLength);
    if (mReadBytesUsed == mReadLength) {
      mReadWriter.writeRead(name, mReadBytes, mQualityBytes, mReadLength);
      mResidueCount += mReadLength;
    } else {
      throw new FragmentTooSmallException(length, mReadLength);
    }
  }

  @Override
  public boolean isPaired() {
    return false;
  }

  @Override
  public MachineType machineType() {
    return MachineType.ILLUMINA_SE;
  }
}
