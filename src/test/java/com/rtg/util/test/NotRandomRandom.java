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
package com.rtg.util.test;

import com.rtg.util.PortableRandom;

/**
 * Not-random random generator for tests
 */
public class NotRandomRandom extends PortableRandom {

  private double mNextDouble;
  private int mNextInt;
  private boolean mNextBoolean;

  /**
   * Constructor
   */
  public NotRandomRandom() {
    super();
    mNextDouble = 0.0;
    mNextInt = 0;
    mNextBoolean = false;
  }
  @Override
  public double nextDouble() {
    //System.err.println("next double" + mNextDouble );
    final double rand = mNextDouble;
    mNextDouble += 0.1;
    if (mNextDouble >= 1.0) {
      mNextDouble = 0.0;
    }
    //System.err.println("nextDouble: " + rand);
    return rand;
  }
  @Override
  public int nextInt(int max) {
    if (mNextInt >= max) {
      mNextInt = 0;
    }
    final int rand = mNextInt;
    mNextInt += 1;
    //System.err.println("nextInt: " + rand);
    return rand;
  }
  @Override
  public boolean nextBoolean() {
    final boolean rand = mNextBoolean;
    mNextBoolean = !mNextBoolean;
    return rand;
  }
}


