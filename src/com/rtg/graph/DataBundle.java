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

package com.rtg.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.reeltwo.plot.Point2D;
import com.reeltwo.plot.PointPlot2D;
import com.reeltwo.plot.TextPlot2D;
import com.reeltwo.plot.TextPoint2D;
import com.rtg.util.ContingencyTable;
import com.rtg.util.Pair;
import com.rtg.vcf.eval.RocFilter;
import com.rtg.vcf.eval.RocPoint;

/**
 * Holds a set of graph data for plotting.
 */
final class DataBundle {

  enum GraphType { ROC, PRECISION_RECALL }

  private static final int TOTAL_LABELS = 10;

  private final Point2D[] mRocPoints;
  private final String[] mRocScores;

  private final Point2D[] mPrecisionRecallPoints;
  private final String[] mPrecisionRecallScores;

  private final int mTotalVariants;
  private final float mMinPrecision;

  private String mTitle = "";
  private String mScoreName;
  private boolean mShow;

  private GraphType mGraphType = null;
  private float mRangeMax = 1.0f;
  private Point2D[] mRangedPoints = null;
  private String[] mRangedScores = null;
  private TextPoint2D[] mRangedLabels = null;
  private TextPoint2D mMaxRangedPoint = null;

  DataBundle(int totalVariants, List<RocPoint<String>> points) {
    mTotalVariants = totalVariants;
    mShow = true;

    mRocPoints = points.stream()
      .map(point -> new Point2D((float) point.getFalsePositives(), (float) point.getTruePositives()))
      .toArray(Point2D[]::new);
    mRocScores = points.stream().map(RocPoint::getThreshold).toArray(String[]::new);
    final Pair<List<Point2D>, List<String>> pr = rocToPrecisionRecall(points, totalVariants);
    mPrecisionRecallPoints = pr.getA().toArray(new Point2D[0]);
    mPrecisionRecallScores = pr.getB().toArray(new String[0]);
    mMinPrecision = findMinPrecision();
  }

  private float findMinPrecision() {
    float minPrecision = 100;
    for (int i = 0; i < mPrecisionRecallPoints.length; i++) {
      if (mPrecisionRecallScores[i] != null) {
        minPrecision = Math.min(minPrecision, mPrecisionRecallPoints[i].getY());
      }
    }
    return minPrecision;
  }

  public float computeAuPR() {
    double aupr = 0;
    if (mPrecisionRecallPoints.length > 1) {
      Point2D lastPoint = null;
      for (Point2D p : mPrecisionRecallPoints) {
        if (lastPoint != null) {
          aupr += (p.getX() - lastPoint.getX()) * (p.getY() + lastPoint.getY()) / 2 / 10000;
        }
        lastPoint = p;
      }
    }
    return (float) aupr;
  }


  private static <T> Pair<List<Point2D>, List<T>> rocToPrecisionRecall(List<RocPoint<T>> points, int totalVariants) {
    final List<Point2D> res = new ArrayList<>();
    final List<T> scores = new ArrayList<>();
    if (totalVariants > 0) {
      final boolean hasRaw = points.stream().anyMatch(x -> x.getRawTruePositives() > 0);
      for (int i = 0; i < points.size(); ++i) {
        final RocPoint<T> point = points.get(i);
        if (res.size() == 0 && point.getTruePositives() + point.getFalsePositives() <= 0) {
          continue; // Skip and add these later
        }
        while (res.size() < i) { // Add any missed initial points
          final RocPoint<T> prev = points.get(res.size());
          // The earlier point with TP=0/FP=0 is where precision is technicaly undefined, but we assume
          // (e.g. via ROC interpolation) that precision is constant as we asymptotically approach (TP+FP) = 0
          final Point2D pr = new Point2D(0, getPrecisionRecall(point, totalVariants, hasRaw).getY());
          res.add(pr);
          scores.add(prev.getThreshold());
        }
        final Point2D pr = getPrecisionRecall(point, totalVariants, hasRaw);
        res.add(pr);
        scores.add(point.getThreshold());
      }
      assert res.size() == points.size(); // Required to ensure slider thresholds are in sync when switching between graphs
    }
    return new Pair<>(res, scores);
  }

  private static <T> Point2D getPrecisionRecall(RocPoint<T> point, int totalVariants, boolean hasRaw) {
    final double truePositives = point.getTruePositives();
    final double x = ContingencyTable.recall(truePositives, totalVariants - truePositives) * 100.0;
    final double rawTp = hasRaw ? point.getRawTruePositives() : truePositives;
    final double y = ContingencyTable.precision(rawTp, point.getFalsePositives()) * 100.0;
    return new Point2D((float) x, (float) y);
  }

  boolean show() {
    return mShow;
  }

  void show(boolean flag) {
    mShow = flag;
  }

  void setTitle(String title) {
    mTitle = title;
  }

  // Set an automatic name based on directory name of file and score field if no explicit name was provided
  void setTitle(File f, String name) {
    if (name.length() > 0) {
      setTitle(name);
    } else {
      final StringBuilder autoname = new StringBuilder();
      final String parentDir = f.getAbsoluteFile().getParentFile().getName().replaceFirst("^(vcf)?eval[-_.]", "").replaceFirst("[-_.](vcf)?eval$", "");
      autoname.append(parentDir);

      final String fname = f.getName();
      final int rocIdx = fname.indexOf(RocFilter.ROC_EXT);
      if (rocIdx != -1 && !fname.startsWith(RocFilter.ALL.fileName())) {
        if (autoname.length() > 0) {
          autoname.append(' ');
        }
        autoname.append(fname.substring(0, rocIdx));
      }

      if (getScoreName() != null) {
        if (autoname.length() > 0) {
          autoname.append(' ');
        }
        autoname.append(getScoreName());
      }
      setTitle(autoname.toString());
    }
  }

  void setGraphType(GraphType graphType) {
    if (graphType != mGraphType) {
      mGraphType = graphType;
      updateRangedData();
    }
  }

  void setScoreMax(float max) {
    mRangeMax = max;
    updateRangedData();
  }

  void updateRangedData() {
    // Since the roc graph and PR graph may contain slightly different numbers of points (due to extrapolation at ends)
    // the current slider position may represent a slightly different threshold on each of the graphs.
    // Ideally we should find the point on the PR graph that has the corresponding threshold
    final int rocmax = (int) (mRangeMax * mRocPoints.length);
    final int prmax = (int) (mRangeMax * mPrecisionRecallPoints.length);
    switch (mGraphType) {
      case ROC:
        if (mRangeMax >= 1.0f) {
          mRangedPoints = mRocPoints;
          mRangedScores = mRocScores;
        } else {
          mRangedPoints = Arrays.copyOf(mRocPoints, rocmax);
          mRangedScores = Arrays.copyOf(mRocScores, rocmax);
        }
        break;
      case PRECISION_RECALL:
        if (mRangeMax >= 1.0f) {
          mRangedPoints = mPrecisionRecallPoints;
          mRangedScores = mPrecisionRecallScores;
        } else {
          mRangedPoints = Arrays.copyOf(mPrecisionRecallPoints, prmax);
          mRangedScores = Arrays.copyOf(mPrecisionRecallScores, prmax);
        }
        break;
      default:
        mRangedPoints = null;
        mRangedScores = null;
    }
    updateLabels();

    mMaxRangedPoint = null;
    for (int i = rocmax - 1; i > 0; --i) {
      if (mRocScores[i] == null) {
        continue;
      }
      mMaxRangedPoint = new TextPoint2D(mRocPoints[i].getX(), mRocPoints[i].getY(), mRocScores[i]);
      break;
    }

  }

  private void updateLabels() {
    if (mRangedPoints != null) {
      mRangedLabels = updateLabels(mRangedPoints, mRangedScores);
    }
  }
  private TextPoint2D[] updateLabels(Point2D[] rangedPoints, String[] rangedScores) {
    final ArrayList<Double> counts = new ArrayList<>();
    Point2D pp = null;
    int firstI = rangedPoints.length;
    for (int i = 0; i < rangedPoints.length && pp == null; i++) { // Find first point with a label provided
      if (rangedScores[i] == null) {
        continue;
      }
      firstI = i;
      pp = rangedPoints[i];
    }
    double countTotal = 0;
    for (int i = firstI + 1; i < rangedPoints.length; i++) { // Sum up approx distance between labelled points
      if (rangedScores[i] == null) {
        continue;
      }
      final Point2D p = rangedPoints[i];
      final double c = Math.sqrt(Math.pow(p.getX() - pp.getX(), 2) + Math.pow(p.getY() - pp.getY(), 2));
      counts.add(c);
      countTotal += c;
      pp = p;
    }

    // set up score labels - make TOTAL_LABELS per line
    final ArrayList<TextPoint2D> posPoints = new ArrayList<>();
    if (firstI < rangedPoints.length) {
      posPoints.add(new TextPoint2D(rangedPoints[firstI].getX(), rangedPoints[firstI].getY(), rangedScores[firstI]));
    }
    if (countTotal != 0) {
      final double step = countTotal / (double) TOTAL_LABELS;
      double next = step;
      int pi = firstI;
      int prevI = firstI;
      if (step > 0) {
        double c = 0;
        for (Double count : counts) {
          do {
            pi++;
          } while (rangedScores[pi] == null && pi < rangedScores.length);
          c += count;
          if (c >= next && posPoints.size() <= TOTAL_LABELS - 1) {
            while (next <= c) {
              next += step;
            }
            prevI = pi;
            posPoints.add(new TextPoint2D(rangedPoints[prevI].getX(), rangedPoints[prevI].getY(), rangedScores[prevI]));
          }
        }
      }
      // Always add a final label within range that is after lastLabel
      for (int end = rangedScores.length - 1; end > prevI; end--) {
        if (rangedScores[end] != null) {
          posPoints.add(new TextPoint2D(rangedPoints[end].getX(), rangedPoints[end].getY(), rangedScores[end]));
          break;
        }
      }
    }
    return posPoints.toArray(new TextPoint2D[0]);
  }

  TextPoint2D getMaxRangedLabel() {
    return mMaxRangedPoint; // This is in ROC coordinate space, even if the current graph type is precision-sensitivity
  }

  // Primary line graph
  PointPlot2D getPlot(int lineWidth, int colour) {
    assert mGraphType != null : "graph type has not been set";
    final PointPlot2D lplot = new PointPlot2D();
    lplot.setData(mRangedPoints);
    lplot.setPoints(false);
    lplot.setLines(true);
    lplot.setLineWidth(lineWidth);
    lplot.setColor(colour);
    lplot.setTitle(mTitle);
    return lplot;
  }

  // Text labels to overlay on the graph
  TextPlot2D getScoreLabels() {
    assert mGraphType != null : "graph type has not been set";
    final TextPlot2D tplot = new TextPlot2D();
    tplot.setData(mRangedLabels);
    return tplot;
  }

  // Points corresponding to each text label
  PointPlot2D getScorePoints(int lineWidth, int colour) {
    assert mGraphType != null : "graph type has not been set";
    final PointPlot2D pplot = new PointPlot2D();
    pplot.setData(mRangedLabels);
    pplot.setPoints(true);
    pplot.setColor(colour);
    pplot.setLineWidth(lineWidth);
    return pplot;
  }

  int getTotalVariants() {
    return mTotalVariants;
  }

  float getMinPrecision() {
    return mMinPrecision;
  }

  public String getTitle() {
    return mTitle;
  }

  public void setScoreName(String scoreName) {
    mScoreName = scoreName;
  }

  public String getScoreName() {
    return mScoreName;
  }
}
