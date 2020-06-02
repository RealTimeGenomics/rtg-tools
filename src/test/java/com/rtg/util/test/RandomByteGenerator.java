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

import java.util.Random;

/**
 * Utility class to create a repeatable stream of
 * positive random bytes, maximum range 128 (0 - 127).
 */
public final class RandomByteGenerator {

  private final int mRange;
  private final long mSeed;
  private Random mRand;

  /**
   * Constructor for generator with seed from current time.
   * @param range the range of bytes to generate from 0 inclusive to range exclusive.
   */
  public RandomByteGenerator(int range) {
    this(range, System.nanoTime());
    System.err.println("Seed: " + mSeed);
  }

  /**
   * Constructor for generator with seed provided.
   * @param range the range of bytes to generate from 0 inclusive to range exclusive.
   * @param seed the seed for the random number generator.
   */
  public RandomByteGenerator(int range, long seed) {
    assert 0 < range && range <= 128;
    mRange = range;
    mSeed = seed;
    reset();
  }

  /**
   * Method to get the next byte in the sequence.
   * @return the next byte in the sequence.
   */
  public byte nextValue() {
    return (byte) mRand.nextInt(mRange);
  }

  /**
   * Method to reset the generator to produce the same sequence again
   * from the beginning.
   */
  public void reset() {
    mRand = new Random(mSeed);
  }
}
