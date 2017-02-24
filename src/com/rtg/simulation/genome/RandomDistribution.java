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

package com.rtg.simulation.genome;

import com.rtg.util.PortableRandom;

/**
 * Produces random values with specified relative frequency values will be
 * between 0 (inclusive) and the size number of frequencies provided
 * (exclusive).
 *
 *
 */
public class RandomDistribution {

  private final int[] mDistribution;
  private final int mTotal;
  private final PortableRandom mGenerator;

  /**
   *
   * @param distribution relative distribution of the values (0, array length -
   *        1]
   * @param generator random number generator
   */
  public RandomDistribution(int[] distribution, PortableRandom generator) {
    mGenerator = generator;
    if (distribution != null) {
      mDistribution =  distribution.clone();
      mTotal = arraySum(distribution);
    } else {
      mDistribution = null;
      mTotal = 0;
    }
  }

  private int arraySum(int[] source) {
    int sum = 0;
    for (final int val : source) {
      sum += val;
    }
    return sum;
  }

  /**
   * @return a the next random value
   */
  public int nextValue() {
    if (mDistribution == null) {
      return 0;
    }
    int random = mGenerator.nextInt(mTotal);
    int i = 0;
    while (i < mDistribution.length && random >= 0) {
      random -= mDistribution[i];
      ++i;
    }
    return i - 1;
  }

  /**
   * get the number of values in the distribution
   *
   * @return number of values in the distribution
   */
  public int valueCount() {
    return mDistribution.length;
  }

}
