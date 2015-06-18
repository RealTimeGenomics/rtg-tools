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
 * Contain stats for a two-class contingency table, allowing
 * calculation of various accuracy metrics.
 */
public class ContingencyTable {

  protected static final int NEG = 0;
  protected static final int POS = 1;

  /** First dimension is actual, second dimension is predicted */
  protected final double[][] mContingencyTable = new double[2][2];

  /**
   * Default constructor with all counts set to zero
   */
  public ContingencyTable() {
    this(0, 0, 0, 0);
  }

  /**
   * Construct explicit contingency table
   * @param tp the number of true positives
   * @param fp the number of false positives
   * @param tn the number of true negatives
   * @param fn the number of false negatives
   */
  public ContingencyTable(double tp, double fp, double tn, double fn) {
    mContingencyTable[POS][POS] = tp;
    mContingencyTable[NEG][NEG] = tn;
    mContingencyTable[NEG][POS] = fp;
    mContingencyTable[POS][NEG] = fn;
  }

  /**
   * Add an entry to the contingency table
   * @param actual actual class
   * @param pred predicted class
   * @param weight instance weight
   */
  public void add(int actual, int pred, double weight) {
    mContingencyTable[actual][pred] += weight;
  }

  /**
   * Add in the results from the supplied evaluation
   * @param eval another evaluation
   */
  public void add(ContingencyTable eval) {
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        add(i, j, eval.mContingencyTable[i][j]);
      }
    }
  }

  /** @return the number of true positives. */
  public double truePositives() {
    return mContingencyTable[POS][POS];
  }

  /** @return the number of true positives. */
  public double trueNegatives() {
    return mContingencyTable[NEG][NEG];
  }

  /** @return the number of true positives. */
  public double falsePositives() {
    return  mContingencyTable[NEG][POS];
  }

  /** @return the number of true positives. */
  public double falseNegatives() {
    return mContingencyTable[POS][NEG];
  }

  /** @return the number of correctly classified instances. */
  public double correct() {
    return truePositives() + trueNegatives();
  }

  /** @return the number of incorrectly classified instances. */
  public double incorrect() {
    return falsePositives() + falseNegatives();
  }

  /** @return the total number of classified instances. */
  public double total() {
    return truePositives() + trueNegatives() + falsePositives() + falseNegatives();
  }

  /** @return the fraction of incorrectly classified instances. */
  public double errorRate() {
    return incorrect() / total();
  }

  /** @return the fraction of correctly classified instances. */
  public double accuracy() {
    return correct() / total();
  }

  /**
   * Compute the fraction of positive instances that were correctly classified.
   * @param truePositive true positives
   * @param falseNegative false negatives
   * @return the fraction of positive instances correctly classified
   * @throws IllegalArgumentException if total positives is 0
   */
  public static double recall(double truePositive, double falseNegative) {
    final double totalPositive = truePositive + falseNegative;
    if (totalPositive == 0) {
      throw new IllegalArgumentException();
    }
    return truePositive / totalPositive;
  }

  /**
   * Compute the fraction of predicted positives that were correct.
   * @param truePositive true positives
   * @param falsePositive false positives
   * @return the fraction of predicted positives that were correct
   */
  public static double precision(double truePositive, double falsePositive) {
    return truePositive / (falsePositive + truePositive);
  }

  /**
   * Compute the F-measure score, the harmonic mean of precision and recall
   * @param precision input precision
   * @param recall input recall
   * @return f-measure score
   */
  public static double fMeasure(double precision, double recall) {
    final double sum = precision + recall;
    return sum == 0 ? 0 : 2 * precision * recall / sum;
  }

}
