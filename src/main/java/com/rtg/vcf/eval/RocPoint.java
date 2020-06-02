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

package com.rtg.vcf.eval;

/**
 * Container for the ROC plot points
 */
public final class RocPoint<T> {
  private T mThreshold;
  private double mTruePositives;
  private double mRawTruePositives;
  private double mFalsePositives;

  /**
   * Construct a point with all 0.0
   */
  RocPoint() {
    this(null, 0, 0, 0);
  }

  /**
   * Construct a new point from an existing one
   * @param other the point to copy
   */
  RocPoint(RocPoint<T> other) {
    this(other.mThreshold, other.mTruePositives, other.mFalsePositives, other.mRawTruePositives);
  }

  /**
   * @param threshold score threshold for the calls that count towards this point
   * @param truePositives number of true positives within the threshold, as measured against the baseline set
   * @param falsePositives number of false positives within the threshold
   * @param tpraw number of true positives within the threshold, as measured against the call set
   */
  public RocPoint(T threshold, double truePositives, double falsePositives, double tpraw) {
    mThreshold = threshold;
    mTruePositives = truePositives;
    mFalsePositives = falsePositives;
    mRawTruePositives = tpraw;
  }

  void add(RocPoint<T> p) {
    mTruePositives += p.mTruePositives;
    mFalsePositives += p.mFalsePositives;
    mRawTruePositives += p.mRawTruePositives;
  }

  public void setThreshold(T threshold) {
    mThreshold = threshold;
  }

  public T getThreshold() {
    return mThreshold;
  }

  public double getTruePositives() {
    return mTruePositives;
  }

  public double getRawTruePositives() {
    return mRawTruePositives;
  }

  public double getFalsePositives() {
    return mFalsePositives;
  }

  @Override
  public String toString() {
    return "" + mThreshold + "\t" + mTruePositives + "\t" + mFalsePositives + "\t" + mRawTruePositives;
  }

}
