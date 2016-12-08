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
package com.rtg.util;


/**
 * Container for posterior conversion methods. All our posteriors are represented as natural log scaled odds.
 *
 */
public final class PosteriorUtils {

  private static final double TEN_LOG_TEN = 10.0 / Math.log(10.0);

  private static final double INV_TEN_LOG_TEN = 1.0 / TEN_LOG_TEN;

  /** For values greater than this 1 + e^x == e^x for doubles. */
  private static final double MAX_EXP = 36.9;

  /** For values greater than this 10^(x / 10) == 10^(x / 10) - 1 for doubles. */
  private static final double MAX_PWR = 161.4;

  private PosteriorUtils() { }

  /**
   * @param posterior the posterior score (natural log scaled odds)
   * @return the posterior score in phred scale
   */
  public static double phredIfy(double posterior) {
    if (posterior > MAX_EXP) {
      return posterior * TEN_LOG_TEN;
    }
    return TEN_LOG_TEN * Math.log(1 + Math.exp(posterior));
  }

  /**
   * @param phredPosterior the posterior score in phred scale
   * @return the posterior score (natural log scaled odds)
   */
  public static double unphredIfy(double phredPosterior) {
    //from typing <code>y= -10*log10(1/(1+e^x)) solve for x</code> into wolfram alpha
    if (phredPosterior > MAX_PWR) {
      return INV_TEN_LOG_TEN * phredPosterior;
    }
    return Math.log(Math.pow(10, phredPosterior / 10.0) - 1);
  }

  /**
   * @param nonIdentityPosterior the non-identity posterior score (natural log scaled)
   * @return the non-identity posterior score in phred scale
   */
  public static double nonIdentityPhredIfy(double nonIdentityPosterior) {
    return phredIfy(-nonIdentityPosterior);
  }

  /**
   * @param identityPhred the phred scale non-identity posterior score
   * @return the non-identity posterior score (natural log scaled)
   */
  public static double nonIdentityUnPhredIfy(double identityPhred) {
    return -unphredIfy(identityPhred);
  }

  //  public static void main(final String[] args) {
  //    for (int i = 1580; i < 1620; ++i) {
  //      final double x = i / 10.0;
  //      final double y = Math.pow(10, x / 10.0);
  //      final boolean same = y + 1 == y;
  //      System.err.println(x + " " + (same ? "-" : "+"));
  //    }
  //  }
}

