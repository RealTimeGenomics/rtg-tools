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

package com.rtg.vcf.eval;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class RocPointTest {
  @Test
  public void add() throws Exception {
    final RocPoint a = new RocPoint(0.1, 0.3, 0.4, 0.5);
    final RocPoint b = new RocPoint(0.1, 0.2, 0.9, 0.7);
    a.add(b);
    Assert.assertEquals(0.5, a.getTruePositives(), 1e-8);
    Assert.assertEquals(1.3, a.getFalsePositives(), 1e-8);
    Assert.assertEquals(1.2, a.getRawTruePositives(), 1e-8);
  }
  @Test
  public void copyConstructor() throws Exception {
    final RocPoint a = new RocPoint(0.1, 0.3, 0.4, 0.5);
    final RocPoint b = new RocPoint(a);
    Assert.assertEquals(0.1, b.getThreshold(), 1e-8);
    Assert.assertEquals(0.3, b.getTruePositives(), 1e-8);
    Assert.assertEquals(0.4, b.getFalsePositives(), 1e-8);
    Assert.assertEquals(0.5, b.getRawTruePositives(), 1e-8);
  }

}