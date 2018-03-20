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

import java.util.ArrayList;
import java.util.List;

import com.rtg.util.Utils;

/**
 * Utilities for dealing with ROC curves
 */
public final class RocUtils {

  private RocUtils() { }

  /**
   * Responsible for describing the threshold that applies to an interpolation between two points
   * @param <T> the type of threshold values
   */
  public interface ThresholdInterpolator<T> {
    /**
     * @param a first value
     * @param b second value
     * @param frac proportion between a and b
     * @return threshold corresponding to the interpolated value, or null if no threshold is appropriate
     */
    T interpolate(T a, T b, double frac);
  }


  /**
   * Outputs an null threshold at any interpolated points
   * @param <T> the type of threshold values
   */
  public static final class NullThresholdInterpolator<T> implements ThresholdInterpolator<T> {
    @Override
    public T interpolate(T a, T b, double frac) {
      return null;
    }
  }

  // Displays selection criteria at interpolated points.
  // This notation may be confusing for most users, so it isn't on by default
  static final class DetailedThresholdInterpolator implements ThresholdInterpolator<String> {
    private static final String TOP = "\u22a4";
    private static final String BOTTOM = "\u22a5";

    @Override
    public String interpolate(String a, String b, double frac) {
      if (frac <= 0) {
        return getLabel(a, BOTTOM);
      } else if (frac >= 1) {
        return getLabel(b, TOP);
      } else {
        return "p>" + Utils.realFormat(frac, 2) + "?" + getLabel(a, BOTTOM) + ":" + getLabel(b, TOP);
      }
    }
    private static String getLabel(Object o, String fixed) {
      return o == null ? fixed : String.valueOf(o);
    }
  }

  /**
   * Interpolates an ROC curve to ensure that there are no true positive jumps greater than the specified increment
   * between points. Interpolated points have no threshold value.
   *
   * Accuracy corresponding to this interpolated point is obtainable in practise by employing
   * <code>point.getThreshold()</code> with probability f (and <code>lastPoint.getThreshold()</code> with probability 1 - f).
   *
   * @param points input ROC data points. These must be sorted by increasing true positives
   * @param tpStep the minimum desired true positive increment between points
   * @param labelInterpolated if true, add labels to interpolated points.
   * @return ROC curve containing additional interpolated points where needed.
   */
  public static List<RocPoint<String>> interpolate(List<RocPoint<String>> points, double tpStep, boolean labelInterpolated) {
    final ThresholdInterpolator<String> interp = labelInterpolated ? new DetailedThresholdInterpolator() : new NullThresholdInterpolator<>();
    return interpolate(points, tpStep, interp);
  }

  /**
   * Interpolates an ROC curve to ensure that there are no true positive jumps greater than the specified increment
   * between points. Interpolated points have no threshold value.
   *
   * Accuracy corresponding to this interpolated point is obtainable in practise by employing
   * <code>point.getThreshold()</code> with probability f (and <code>lastPoint.getThreshold()</code> with probability 1 - f).
   *
   * @param points input ROC data points. These must be sorted by increasing true positives
   * @param tpStep the minimum desired true positive increment between points
   * @param <T> the type of threshold value
   * @return ROC curve containing additional interpolated points where needed.
   */
  public static <T> List<RocPoint<T>> interpolate(List<RocPoint<T>> points, double tpStep) {
    return interpolate(points, tpStep, new NullThresholdInterpolator<>());
  }

  /**
   * Interpolates an ROC curve to ensure that there are no true positive jumps greater than the specified increment
   * between points. Interpolated points may be given a threshold value.
   *
   * Accuracy corresponding to this interpolated point is obtainable in practise by employing
   * <code>point.getThreshold()</code> with probability f (and <code>lastPoint.getThreshold()</code> with probability 1 - f).
   *
   * @param points input ROC data points. These must be sorted by increasing true positives
   * @param tpStep the minimum desired true positive increment between points
   * @param interp object responsible for producing appropriate thresholds for interpolated points
   * @param <T> the type of threshold value
   * @return ROC curve containing additional interpolated points where needed.
   */
  public static <T> List<RocPoint<T>> interpolate(List<RocPoint<T>> points, double tpStep, ThresholdInterpolator<T> interp) {
    if (tpStep > 1 && points.size() > 0) {
      final boolean hasRaw = points.stream().anyMatch(x -> x.getRawTruePositives() > 0);
      final List<RocPoint<T>> res = new ArrayList<>();
      RocPoint<T> lastPoint = points.get(0);
      res.add(lastPoint);
      for (int i = 1; i < points.size(); ++i) {
        final RocPoint<T> point = points.get(i);
        assert point.getTruePositives() >= lastPoint.getTruePositives() : "ROC Points are not sorted by increasing TP";
        if (point.getTruePositives() - lastPoint.getTruePositives() > tpStep) {
          final double fStep = tpStep / (point.getTruePositives() - lastPoint.getTruePositives());
          for (double f = fStep; f < 1.0; f += fStep) {
            final double iTp = lastPoint.getTruePositives() + f * (point.getTruePositives() - lastPoint.getTruePositives());
            final double iFp = lastPoint.getFalsePositives() + f * (point.getFalsePositives() - lastPoint.getFalsePositives());
            final double iRawTp = hasRaw ? (lastPoint.getRawTruePositives() + f * (point.getRawTruePositives() - lastPoint.getRawTruePositives())) : 0;
            final RocPoint<T> interpolated = new RocPoint<>(interp.interpolate(lastPoint.getThreshold(), point.getThreshold(), f), iTp, iFp, iRawTp);
            res.add(interpolated);
          }
        }
        lastPoint = point;
        res.add(point);
      }
      return res;
    } else {
      return points;
    }
  }

}
