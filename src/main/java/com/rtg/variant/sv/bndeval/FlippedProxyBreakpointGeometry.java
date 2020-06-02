/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.variant.sv.bndeval;

import com.rtg.util.integrity.Exam;

/**
 * Flip x and y co-ordinates.
 */
public final class FlippedProxyBreakpointGeometry extends AbstractBreakpointGeometry {

  private final AbstractBreakpointGeometry mProxy;

  /**
   * @param br the proxy.
   */
  protected FlippedProxyBreakpointGeometry(AbstractBreakpointGeometry br) {
    mProxy = br;
  }

  @Override
  public AbstractBreakpointGeometry flip() {
    return mProxy;
  }

  @Override
  public Orientation getOrientation() {
    return mProxy.getOrientation().flip();
  }

  @Override
  public int getXLo() {
    return mProxy.getYLo();
  }

  @Override
  public String getXName() {
    return mProxy.getYName();
  }

  @Override
  public int getYLo() {
    return mProxy.getXLo();
  }

  @Override
  public String getYName() {
    return mProxy.getXName();
  }

  @Override
  public int getRLo() {
    return mProxy.getRLo();
  }

  @Override
  public int getRHi() {
    return mProxy.getRHi();
  }

  @Override
  public int getYHi() {
    return mProxy.getXHi();
  }

  @Override
  public int getXHi() {
    return mProxy.getYHi();
  }

  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertNotNull(mProxy);
    mProxy.integrity();
    Exam.assertEquals(this.count(), mProxy.count());
    return true;
  }
}
