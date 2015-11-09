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
package com.rtg.mode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.Resources;
import com.rtg.util.diagnostic.ErrorType;

/**
 * Holds constants from the protein scoring matrix using gapped scoring.
 * These are collected together here for convenience and to ensure consistent values are being used.
 * <pre>
 * E = K * m * n * exp (- LAMBDA * raw score)
 * </pre>
 */
public final class ProteinScoringMatrix extends ScoringMatrix {

  private static final String DEFAULT = "BLOSUM62";

  /** K in expect equation. */
  private final double mK;
  private final double mLogK;

  /** H is the relative entropy of the scoring matrix. */
  private final double mH;

  /** LAMBDA in expect equation. */
  private final double mLambda;

  /** Average score for two identical amino acids. */
  private final double mHit;

  /** Average score for two different amino acids. */
  private final double mMiss;

  /** Score on opening a gap. */
  private final double mGap;

  /** Score on extending a gap. */
  private final double mExtend;

  /** Expected value for score. */
  private final double mExpected;

  /** Smallest value in matrix. */
  private final int mMax;

  /**
   * Default <code>BLOSUM62</code> matrix for protein distance
   * @throws InvalidParamsException exception when invalid params
   * @throws IOException exception when io goes wrong
   */
  public ProteinScoringMatrix() throws InvalidParamsException, IOException {
    this(DEFAULT);
  }

  /**
   * Matrix for protein distance
   * @param matrixName "<code>BLOSUM45</code>", "<code>BLOSUM62</code>", "<code>BLOSUM80</code>" etc.
   * @throws InvalidParamsException exception when invalid parameters.
   * @throws IOException exception when IO goes wrong
   */
  public ProteinScoringMatrix(String matrixName) throws InvalidParamsException, IOException {
    final String mat = matrixName.toUpperCase(Locale.getDefault());
    final int len = Protein.values().length;
    mScores = new int[len][len];
    final String res = "com/rtg/mode/" + mat;
    try (InputStream in = Resources.getResourceAsStream(res)) {
      if (in == null) {
        throw new MissingResourceException("Could not find:" + res, ProteinScoringMatrix.class.getName(), res);
      }
      try (BufferedReader re = new BufferedReader(new InputStreamReader(in))) {
        try {
          parse(re);
        } catch (final IOException | NumberFormatException e) {
          throw new MissingResourceException("Malformed resource: " + res + " message: " + e.getMessage(), ProteinScoringMatrix.class.getName(), res);
        }
        final String resProps = "com/rtg/mode/" + mat + ".properties";
        try (final InputStream inProps = Resources.getResourceAsStream(resProps)) {
          if (inProps == null) {
            throw new MissingResourceException("Could not find:" + resProps, ProteinScoringMatrix.class.getName(), resProps);
          }

          final Properties pr = new Properties();
          try {
            pr.load(inProps);
          } catch (final IOException e) {
            throw new InvalidParamsException(ErrorType.PROPS_LOAD_FAILED, "Matrix", resProps, e.getMessage());
          } catch (final IllegalArgumentException e) {
            throw new InvalidParamsException(ErrorType.PROPS_INVALID, "Matrix", resProps);
          }
          mK = getDouble(mat, pr, "K");
          mLogK = Math.log(mK);
          mH = getDouble(mat, pr, "H");
          mLambda = getDouble(mat, pr, "LAMBDA");
          mHit = getDouble(mat, pr, "HIT");
          mMiss = getDouble(mat, pr, "MISS");
          mGap = getDouble(mat, pr, "GAP");
          mExtend = getDouble(mat, pr, "EXTEND");
          mExpected = getDouble(mat, pr, "EXPECTED");
          mMax = findMax();
        }
      }
    }
    assert integrity();
  }

  private int findMax() {
    int max = Integer.MIN_VALUE;
    for (int[] mScore : mScores) {
      for (final int v : mScore) {
        if (v > max) {
          max = v;
        }
      }
    }
    return max;
  }

  public int getMaxScore() {
    return mMax;
  }

  /**
   * Returns the K value
   * @return K
   */
  public double getK() {
    return mK;
  }

  /**
   * Returns the K value
   * @return K
   */
  public double getLogK() {
    return mLogK;
  }

  /**
   * Returns the H value.
   * This is the relative entropy of the scoring matrix.
   * @return H
   */
  public double getH() {
    return mH;
  }

  /**
   * Returns the lambda value
   * @return lambda
   */
  public double getLambda() {
    return mLambda;
  }

  /**
   * Returns the hit value
   * @return hit
   */
  public double getHit() {
    return mHit;
  }

  /**
   * Returns the miss value
   * @return miss
   */
  public double getMiss() {
    return mMiss;
  }

  /**
   * Returns the gap opening value
   * @return gap
   */
  public double getGap() {
    return mGap;
  }

  /**
   * Returns the gap extend value
   * @return extend
   */
  public double getExtend() {
    return mExtend;
  }

  /**
   * Returns the expected value
   * @return expected
   */
  public double getExpected() {
    return mExpected;
  }

  private static double getDouble(String props, Properties pr, String key) {
    final String val = pr.getProperty(key);
    //System.err.println("key=" + key + " val=" + val);
    if (val == null) {
      throw new InvalidParamsException(ErrorType.PROPS_KEY_NOT_FOUND, key, props);
    }
    return Double.parseDouble(val);
  }

}
