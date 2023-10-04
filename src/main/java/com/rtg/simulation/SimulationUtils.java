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

package com.rtg.simulation;

import java.io.IOException;
import java.util.Arrays;

import com.rtg.reader.SequencesReader;

/**
 * Utility methods for simulation
 */
public final class SimulationUtils {

  private SimulationUtils() { }

  /**
   * Generates an accumulated distribution from an input distribution. The input distribution need not sum to 1
   * @param dist the non-cumulative distribution
   * @return the cumulative distribution
   */
  public static double[] cumulativeDistribution(final double... dist) {
    double sum = 0;
    for (final double r : dist) {
      sum += r;
    }

    final double[] thres = new double[dist.length];
    double currentThres = 0;
    for (int i = 0; i < dist.length; ++i) {
      currentThres += dist[i];
      thres[i] = currentThres / sum;
    }
    return thres;
  }

  /**
   * Find entry position in a cumulative distribution.
   * @param dist cumulative distribution
   * @param rand a double chosen between 0.0 and 1.0
   * @return a chosen length
   */
  public static int chooseFromCumulative(final double[] dist, final double rand) {
    assert rand <= 1.0 && rand >= 0.0;
    int len = Arrays.binarySearch(dist, rand);
    if (len < 0) {
      len = -len - 1;
    }
    while (len < dist.length && dist[len] == 0.0) {
      ++len;
    }
    return len;
  }

  /**
   * Create a distribution using either the provided distribution or a default one based on sequence lengths
   * @param sr the sequences this distribution should cover
   * @param prob the desired distribution or null if default should be used
   * @return an instantiated distribution
   * @throws IOException if the reader complains
   */
  public static DistributionSampler createDistribution(SequencesReader sr, double... prob) throws IOException {
    if (prob == null) {
      return defaultDistribution(sr);
    } else {
      return new DistributionSampler(prob);
    }
  }

  /**
   * Create a distribution where the odds of selecting a sequence are proportional to it's length
   * @param sr the sequences this distribution should cover
   * @return an instantiated distribution
   * @throws IOException if the reader complains
   */
  public static DistributionSampler defaultDistribution(SequencesReader sr) throws IOException {
    final int[] lengths = sr.sequenceLengths(0, sr.numberSequences());
    long total = 0;
    for (int length : lengths) {
      total += length;
    }
    final double[] nonCumulative = new double[lengths.length];
    for (int i = 0; i < lengths.length; ++i) {
      nonCumulative[i] = (double) lengths[i] / (double) total;
    }
    return new DistributionSampler(nonCumulative);
  }
}
