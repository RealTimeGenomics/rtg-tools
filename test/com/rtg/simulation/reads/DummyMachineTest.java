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
import java.util.Arrays;

import com.rtg.reader.PrereadType;
import com.rtg.reader.SdfId;
import com.rtg.simulation.reads.AbstractMachine.SimErrorType;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.TestUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.MachineErrorParams;
import com.rtg.variant.MachineErrorParamsBuilder;

import junit.framework.TestCase;

/**
 * Test class
 */
public class DummyMachineTest extends TestCase {

  abstract static class MockMachine implements Machine {
    @Override
    public void setQualRange(byte minq, byte maxq) { }
    @Override
    public void setReadWriter(ReadWriter rw) { }
    @Override
    public void identifyTemplateSet(SdfId... templateIds) { }
    @Override
    public void identifyOriginalReference(SdfId referenceId) { }
    @Override
    public long residues() {
      return 0;
    }
    @Override
    public boolean isPaired() {
      return false;
    }
    @Override
    public PrereadType prereadType() {
      return PrereadType.UNKNOWN;
    }
    @Override
    public MachineType machineType() {
      return MachineType.ILLUMINA_PE;
    }
    @Override
    public String formatActionsHistogram() {
      return null;
    }
  }

  AbstractMachine getMachine(MachineErrorParams priors) {
    return new AbstractMachine(priors) {
      @Override
      public void setReadWriter(ReadWriter rw) {
      }
      @Override
      public void processFragment(String id, int fragmentStart, byte[] data, int length) {
      }
      @Override
      public boolean isPaired() {
        return false;
      }
      @Override
      public PrereadType prereadType() {
        return null;
      }
      @Override
      public MachineType machineType() {
        return null;
      }
    };
  }

  public AbstractMachine getMachine() throws IOException, InvalidParamsException {
    return getMachine(new MachineErrorParamsBuilder().errors("illumina").create());
  }

  public void test() throws Exception {
    final AbstractMachine m = getMachine();
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaReadWriter w = new FastaReadWriter(out.lineWriter());
    m.setReadWriter(w);
    m.mWorkspace = new int[10];
    m.mReadBytes = new byte[10];
    m.mQualityBytes = new byte[10];
    final byte[] frag = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    m.reseedErrorRandom(21);
    assertEquals(0, m.process(0, frag, frag.length, 1, 10));
    assertTrue(Arrays.equals(new byte[] {1, 1, 1, 1, 1, 1, 3, 1, 1, 1}, m.mReadBytes));

    assertEquals("6.1X3.", m.getCigar(false));
    assertEquals("6.1X6.", m.getCigar(true));

    assertEquals(0, m.mResidueCount);   //this is never incremented by process...
    assertEquals(0, m.residues());

    assertEquals(0, m.process(0, frag, frag.length, 1, 10));
    assertEquals("10.", m.getCigar(false));
    assertEquals(0, m.process(0, frag, frag.length, 1, 10));
    assertEquals("1X9.", m.getCigar(false));
    for (int c = 0; c < 1551; ++c) {
      m.process(0, frag, frag.length, 1, 10);
    }
    //    System.out.println(m.getCigar(false, 0, 10, 10));
    assertEquals("9.1D1.", m.getCigar(false));
    for (int c = 0; c < 1134; ++c) {
      m.process(0, frag, frag.length, 1, 10);
    }
    assertEquals("3.1I6.", m.getCigar(false));
  }

  public void testNameFormat() {
    assertEquals("34/9/F/33.", AbstractMachine.formatReadName("34/", 'F', "33.", 5, 3));
    assertEquals("blah/F/33.", AbstractMachine.formatReadName("blah/", 'F', "33.", -1, 3));
  }

  public void testErrorType() {
    TestUtils.testEnum(SimErrorType.class, "[MNP, INSERT, DELETE, SUBSTITUTE_N, NOERROR]");
    final AbstractMachine am = getMachine(new MachineErrorParamsBuilder().errorInsEventRate(0.2).errorDelEventRate(0.15).errorMnpEventRate(0.3).create());
    assertEquals(SimErrorType.INSERT, am.getErrorType(0.0));
    assertEquals(SimErrorType.INSERT, am.getErrorType(0.1999));
    assertEquals(SimErrorType.DELETE, am.getErrorType(0.20));
    assertEquals(SimErrorType.DELETE, am.getErrorType(0.3499));
    assertEquals(SimErrorType.MNP, am.getErrorType(0.35));
    assertEquals(SimErrorType.MNP, am.getErrorType(0.6499));
    assertEquals(SimErrorType.NOERROR, am.getErrorType(0.65));
    assertEquals(SimErrorType.NOERROR, am.getErrorType(0.99));

  }
}
