/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.variant.AbstractMachineErrorParams;
import com.rtg.variant.MachineErrorParamsBuilder;

/**
 * Test class
 */
public class FourFiveFourPairedEndMachineTest extends AbstractMachineTest {

  @Override
  protected AbstractMachineErrorParams getPriors() throws IOException, InvalidParamsException {
    return new MachineErrorParamsBuilder().errors("ls454_pe").create();
  }

  @Override
  protected Machine getMachine(final long seed) throws IOException, InvalidParamsException {
    final FourFiveFourPairedEndMachine m = new FourFiveFourPairedEndMachine(getPriors(), seed);
    m.setMinPairSize(300);
    m.setMaxPairSize(350);
    return m;
  }

  public void test() throws Exception {
    final FourFiveFourPairedEndMachine m = (FourFiveFourPairedEndMachine) getMachine(47);
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaReadWriter w = new FastaReadWriter(out.lineWriter());
    m.setReadWriter(w);
    m.setMinPairSize(3);
    m.setMaxPairSize(7);
    final byte[] frag = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    m.processFragment("name/", 30, frag, frag.length);
    m.processFragment("name/", 30, frag, frag.length);
    m.processFragment("name/", 30, frag, frag.length);

    //System.out.println(out.toString());
    mNano.check("454pe-results.fa", out.toString(), false);
  }


}
