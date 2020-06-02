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

import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;
import com.rtg.variant.AbstractMachineErrorParams;
import com.rtg.variant.MachineErrorParamsBuilder;

/**
 * super class for both single and paired end Illumina machines
 */
public abstract class AbstractIlluminaMachine extends AbstractMachine {

  protected final PortableRandom mFrameRandom;


  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public AbstractIlluminaMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params);
    mFrameRandom = new PortableRandom(randomSeed);
  }

  /**
   * Constructs with seed and default Illumina priors
   * @param randomSeed random seed
   * @throws InvalidParamsException if fails to construct priors
   * @throws IOException whenever
   */
  public AbstractIlluminaMachine(long randomSeed) throws IOException {
    this(new MachineErrorParamsBuilder().errors("illumina").create(), randomSeed);
  }

  protected String generateRead(String id, int fragmentStart, byte[] data, int length, boolean forward, int readLength) {
    final int startFrom;
    final int direction;
    if (forward) {
      startFrom = 0;
      direction = 1;
    } else {
      startFrom = length - 1;
      direction = -1;
    }
    final int localStart = process(startFrom, data, length, direction, readLength);
    final String cigar = getCigar(!forward);
    return formatReadName(id, forward ? 'F' : 'R', cigar, fragmentStart, localStart);
  }

  @Override
  public abstract void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException;


  @Override
  public abstract boolean isPaired();

}
