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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.rtg.util.intervals.Range;

/**
 * Holds a breakend geometry for evaluation along with match status.
 */
class BndVariant extends Range {

  private final BreakpointGeometry mBreakpoint;
  private final int mId;
  private final List<BndVariant> mMatches = new ArrayList<>();
  private boolean mCorrect;
  private double mWeight;

  BndVariant(BreakpointGeometry breakpoint, int id) {
    super(Math.min(breakpoint.getXLo(), breakpoint.getXHi()), Math.max(breakpoint.getXLo(), breakpoint.getXHi()));
    mBreakpoint = breakpoint;
    mId = id;
  }

  public BreakpointGeometry getBreakpoint() {
    return mBreakpoint;
  }

  public int getId() {
    return mId;
  }

  public void setCorrect(boolean correct) {
    mCorrect = correct;
  }

  public boolean isCorrect() {
    return mCorrect;
  }

  public Collection<BndVariant> matches() {
    return mMatches;
  }

  public void addWeight(double weight) {
    mWeight += weight;
  }

  public double weight() {
    return mWeight;
  }
}
