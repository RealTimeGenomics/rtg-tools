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

import com.rtg.reader.PrereadType;
import com.rtg.reader.SdfId;
import com.rtg.util.PortableRandom;
import com.rtg.util.machine.MachineType;

/**
 * A read simulation machine which introduces additional errors
 * Specifically PCR amplification and Chimeras. Will not duplicate AND chimera a fragment at the same time.
 */
public class ErrorMachine implements Machine {

  private final Machine mParentMachine;
  private final PortableRandom mErrorRandom;

  private final double mPcrDupRate;
  private final double mChimeraRate;

  private int mChimeraCount = 0;
  private String mChimeraId;
  private byte[] mChimeraData;
  private byte[] mChimeraDataBuf;
  private int mChimeraLength;

  /**
   * @param randomSeed seed for random number generator
   * @param parent the parent machine to wrap
   * @param pcrDuplicationRate the rate of PCR duplication errors
   * @param chimeraRate the rate of chimeric fragment errors
   */
  public ErrorMachine(long randomSeed, Machine parent, double pcrDuplicationRate, double chimeraRate) {
    mErrorRandom = new PortableRandom(randomSeed);
    mParentMachine = parent;

    mPcrDupRate = pcrDuplicationRate;
    mChimeraRate = pcrDuplicationRate + chimeraRate;
  }

  @Override
  public void setQualRange(byte minq, byte maxq) {
    mParentMachine.setQualRange(minq, maxq);
  }

  @Override
  public void setReadWriter(ReadWriter rw) {
    mParentMachine.setReadWriter(rw);
  }

  @Override
  public void identifyTemplateSet(SdfId... templateIds) {
    mParentMachine.identifyTemplateSet(templateIds);
  }

  @Override
  public void identifyOriginalReference(SdfId referenceId) {
    mParentMachine.identifyOriginalReference(referenceId);
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {

    if (mChimeraId != null) {
      final int catLength = mChimeraLength + length;
      if (mChimeraDataBuf == null || mChimeraDataBuf.length < catLength) {
        mChimeraDataBuf = new byte[catLength];
      }
      System.arraycopy(mChimeraData, 0, mChimeraDataBuf, 0, mChimeraLength);
      System.arraycopy(data, 0, mChimeraDataBuf, mChimeraLength, length);

      mParentMachine.processFragment("chimera" + mChimeraCount + '/', Integer.MIN_VALUE, mChimeraDataBuf, catLength);

      ++mChimeraCount;
      mChimeraId = null;
      return;
    }

    final double nextRandom = mErrorRandom.nextDouble();
    if (nextRandom < mPcrDupRate) {
      mParentMachine.processFragment(id, fragmentStart, data, length);
      mParentMachine.processFragment("dupe-" + id, fragmentStart, data, length); //TODO chance to get higher levels of duplication
    } else if (nextRandom < mChimeraRate) {
      //store a fragment to mangle together with the next fragment
      mChimeraId = id;
      mChimeraData = data;
      mChimeraLength = length;
    } else {
      mParentMachine.processFragment(id, fragmentStart, data, length);
    }
  }

  @Override
  public long residues() {
    return mParentMachine.residues();
  }

  @Override
  public boolean isPaired() {
    return mParentMachine.isPaired();
  }

  @Override
  public PrereadType prereadType() {
    return mParentMachine.prereadType();
  }

  @Override
  public MachineType machineType() {
    return mParentMachine.machineType();
  }

  @Override
  public String formatActionsHistogram() {
    return mParentMachine.formatActionsHistogram();
  }
}
