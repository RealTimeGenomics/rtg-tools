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

import java.io.IOException;

import com.rtg.reader.SequencesReader;

/**
 * Encapsulate the random selection of sequences
 */
public class SequenceDistribution {
  private final double[] mCumulative;

  /**
   * create a sequence distribution based on the ratios provided in <code>nonCumulative</code>
   * @param nonCumulative the ratios between sequences
   */
  public SequenceDistribution(double[] nonCumulative) {
    mCumulative = new double[nonCumulative.length];
    double sum = 0;
    for (int i = 0; i < nonCumulative.length; ++i) {
      sum += nonCumulative[i];
      mCumulative[i] = sum;
    }
  }

  /**
   * Pick a sequence based on where in the distribution the provided double falls
   * @param rand double between 0 and 1
   * @return the sequence id
   */
  public int selectSequence(double rand) {
    int seqId = 0;
    while (mCumulative[seqId] < rand) {
      ++seqId;
    }
    return seqId;
  }

  /**
   * Create a distribution using either the provided distribution or a default one based on sequence lengths
   * @param sr the sequences this distribution should cover
   * @param prob the desired distribution or null if default should be used
   * @return an instantiated SequenceDistribution
   * @throws IOException if the reader complains
   */
  public static SequenceDistribution createDistribution(SequencesReader sr, double[] prob) throws IOException {
    if (prob == null) {
      return defaultDistribution(sr);
    } else {
      return new SequenceDistribution(prob);
    }
  }

  /**
   * Create a distribution where the odds of selecting a sequence are proportional to it's length
   * @param sr the sequences this distribution should cover
   * @return an instantiated SequenceDistribution
   * @throws IOException if the reader complains
   */
  public static SequenceDistribution defaultDistribution(SequencesReader sr) throws IOException {
    final int[] lengths = sr.sequenceLengths(0, sr.numberSequences());
    long total = 0;
    for (int length : lengths) {
      total += length;
    }
    final double[] nonCumulative = new double[lengths.length];
    for (int i = 0; i < lengths.length; ++i) {
      nonCumulative[i] = (double) lengths[i] / (double) total;
    }
    return new SequenceDistribution(nonCumulative);
  }
}
