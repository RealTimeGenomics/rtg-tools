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

import com.rtg.reader.SdfId;
import com.rtg.util.PortableRandom;

/**
 */
public class UnknownBaseReadWriter implements ReadWriter {
  final ReadWriter mInternal;
  final double mRate;
  byte[] mData;
  final PortableRandom mRandom;
  /**
   * put N in reads at a specified frequency
   * @param internal where to write the N filled reads
   * @param rate the rate to insert N
   * @param random a source of randomness
   */
  public UnknownBaseReadWriter(ReadWriter internal, double rate, PortableRandom random) {
    mInternal = internal;
    mRate = rate;
    mRandom = random;
  }

  @Override
  public void close() throws IOException {
    mInternal.close();

  }

  @Override
  public void identifyTemplateSet(SdfId... templateIds) {
    mInternal.identifyTemplateSet(templateIds);
  }

  @Override
  public void identifyOriginalReference(SdfId referenceId) {
    mInternal.identifyOriginalReference(referenceId);
  }

  private void filter(byte[] data, int length) {
    if (mData == null || length > mData.length) {
      mData = new byte[length];
    }
    for (int i = 0; i < length; ++i) {
      if (mRandom.nextDouble() < mRate) {
        mData[i] = 0;
      } else {
        mData[i] = data[i];
      }
    }
  }

  @Override
  public void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    filter(data, length);
    mInternal.writeRead(name, mData, qual, length);
  }

  @Override
  public void writeLeftRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    filter(data, length);
    mInternal.writeLeftRead(name, mData, qual, length);
  }

  @Override
  public void writeRightRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    filter(data, length);
    mInternal.writeRightRead(name, mData, qual, length);
  }

  @Override
  public int readsWritten() {
    return mInternal.readsWritten();
  }

}
