/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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

package com.rtg.util.machine;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class MachineTypeTest extends TestCase {

  public void test() {
    TestUtils.testPseudoEnum(MachineType.class, "[generic, illumina_se, illumina_pe, complete_genomics, complete_genomics_2, 454_pe, 454_se, iontorrent]");
  }

  public void testCompatible() {
    assertTrue(MachineType.ILLUMINA_SE.compatiblePlatform("illumina"));
    assertTrue(MachineType.ILLUMINA_SE.compatiblePlatform("Illumina"));

    assertTrue(MachineType.ILLUMINA_PE.compatiblePlatform("illumina"));
    assertTrue(MachineType.ILLUMINA_PE.compatiblePlatform("Illumina"));

    assertTrue(MachineType.COMPLETE_GENOMICS.compatiblePlatform("Complete"));
    assertTrue(MachineType.COMPLETE_GENOMICS_2.compatiblePlatform("Completegenomics"));

    assertTrue(MachineType.FOURFIVEFOUR_PE.compatiblePlatform("lS454"));
    assertTrue(MachineType.FOURFIVEFOUR_SE.compatiblePlatform("Ls454"));

    assertTrue(MachineType.IONTORRENT.compatiblePlatform("iontorrent"));
  }
}
