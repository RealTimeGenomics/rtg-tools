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

package com.rtg.util.array;

import java.util.Random;

import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 * Base class for testing all of the index implementations that are capable of
 * holding more than the maximum integer worth of data that they actually work.
 */
public abstract class AbstractCommonIndexRegression extends TestCase {

  private static final long NUM_ELEMENTS = 2L * Integer.MAX_VALUE + 9000L;

  protected abstract long getRange();
  protected abstract CommonIndex createIndex(long elements);

  protected long getNumElements() {
    return NUM_ELEMENTS;
  }

  /**
   * Test the common index implementation
   */
  public void testIndex() {
    Diagnostic.setLogStream();
    doTest(getRange(), getNumElements());
  }

  private void doTest(long range, long elements) {
    final RandomLongGenerator value = new RandomLongGenerator(range);

    final CommonIndex index = createIndex(elements);
    assertEquals(elements, index.length());

    for (long l = 0; l < elements; ++l) {
      index.set(l, value.nextValue());
    }

    value.reset();

    for (long l = 0; l < elements; ++l) {
      assertEquals(value.nextValue(), index.get(l));
    }
  }

  /**
   * Utility class to create a repeatable stream of
   * positive random longs.
   */
  public static final class RandomLongGenerator {

    private final long mRange;
    private final long mSeed;
    private Random mRand;

    /**
     * Constructor for generator with seed from current time.
     * @param range the range of longs to generate from 0 inclusive to range exclusive.
     */
    public RandomLongGenerator(long range) {
      this(range, System.nanoTime());
      System.err.println("Seed: " + mSeed);
    }

    /**
     * Constructor for generator with seed provided.
     * @param range the range of longs to generate from 0 inclusive to range exclusive.
     * @param seed the seed for the random number generator.
     */
    public RandomLongGenerator(long range, long seed) {
      assert 0 < range && range <= Long.MAX_VALUE;
      mRange = range;
      mSeed = seed;
      reset();
    }

    /**
     * Method to get the next long in the sequence.
     * @return the next long in the sequence.
     */
    public long nextValue() {
      return (long) (mRand.nextDouble() * mRange);
    }

    /**
     * Method to reset the generator to produce the same sequence again
     * from the beginning.
     */
    public void reset() {
      mRand = new Random(mSeed);
    }
  }
}
