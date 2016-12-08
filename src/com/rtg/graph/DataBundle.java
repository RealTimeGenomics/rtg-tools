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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.reeltwo.plot.Point2D;
import com.reeltwo.plot.PointPlot2D;
import com.reeltwo.plot.TextPlot2D;
import com.reeltwo.plot.TextPoint2D;
import com.rtg.util.ContingencyTable;
import com.rtg.vcf.eval.RocPoint;

/**
 * Holds counts.
 */
final class DataBundle {
  private static final int TOTAL_LABELS = 10;

  private final Point2D[] mPoints;
  private final String[] mScores;

  private final Point2D[] mPrecisionRecall;

  private String mTitle;
  private final int mTotalVariants;
  private final float mMinPrecision;
  private boolean mShow;

  private Point2D[] mRangedPoints = null;
  private String[] mRangedScores = null;
  private TextPoint2D[] mRangedPosPoints = null;
  private TextPoint2D[] mRangedPrecisionRecallPosPoints = null;
  private Point2D[] mRangedPrecisionRecall;
  private String mScoreName;

  DataBundle(String title, RocPoint[] points, String[] labels, int totalVariants) {
    final List<Point2D> asPoints = Arrays.stream(points)
      .map(point -> new Point2D((float) point.getFalsePositives(), (float) point.getTruePositives()))
      .collect(Collectors.toList());
    mPoints = asPoints.toArray(new Point2D[asPoints.size()]);
    mScores = labels;
    mTitle = title;
    mTotalVariants = totalVariants;
    mShow = true;
    mPrecisionRecall = new Point2D[totalVariants > 0 ? points.length - 1 : 0];
    final boolean hasRaw = Arrays.stream(points).anyMatch(x -> x.getRawTruePositives() > 0);
    float minPrecision = 100;
    for (int i = 0; i < mPrecisionRecall.length; ++i) {
      final RocPoint point = points[i + 1];
      final double truePositives = point.getTruePositives();
      final float rawTp = (float) (hasRaw ? point.getRawTruePositives() : truePositives);
      final float x = (float) (ContingencyTable.recall(truePositives, mTotalVariants - truePositives) * 100.0);
      final float y = (float) (ContingencyTable.precision(rawTp, point.getFalsePositives()) * 100.0);
      mPrecisionRecall[i] = new Point2D(x, y);
      minPrecision = Math.min(minPrecision, y);
    }
    mMinPrecision = minPrecision;

    resetRange();
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

  void setScoreRange(float min, float max) {
    final int smin = (int) (min * mScores.length);
    final int smax = (int) (max * mScores.length);

    final ArrayList<String> scores = new ArrayList<>();
    final ArrayList<Point2D> points = new ArrayList<>();
    final ArrayList<Point2D> precisionRecallPoints = new ArrayList<>();

    for (int i = smin; i < smax; ++i) {
      scores.add(mScores[i]);
      points.add(mPoints[i]);
    }
    for (int i = smin; i < Math.min(smax, mPrecisionRecall.length - 1); ++i) {
      precisionRecallPoints.add(mPrecisionRecall[i]);
    }

    mRangedPoints = points.toArray(new Point2D[points.size()]);
    mRangedPrecisionRecall = precisionRecallPoints.toArray(new Point2D[precisionRecallPoints.size()]);
    mRangedScores = new String[scores.size()];
    for (int i = 0; i < scores.size(); ++i) {
      mRangedScores[i] = scores.get(i);
    }

    updateLabels();
  }

  void resetRange() {
    mRangedPoints = mPoints;
    mRangedScores = mScores;
    mRangedPrecisionRecall = mPrecisionRecall;
    updateLabels();
  }

  private void updateLabels() {
    mRangedPosPoints = updateLabels(mRangedPoints, mRangedScores);
    mRangedPrecisionRecallPosPoints = updateLabels(mRangedPrecisionRecall, mRangedScores);
  }
  private TextPoint2D[] updateLabels(Point2D[] rangedPoints, String[] rangedScores) {
    final ArrayList<Integer> counts = new ArrayList<>();
    float px = 0;
    float py = 0;
    if (rangedPoints.length != 0) {
      px = rangedPoints[0].getX();
      py = rangedPoints[0].getY();
    }
    int countTotal = 0;
    for (Point2D p : rangedPoints) {
      final int c = (int) (p.getX() - px + p.getY() - py);
      counts.add(c);
      countTotal += c;
      px = p.getX();
      py = p.getY();
      //System.err.println(c + " px: " + p.getX() + " py: " + p.getY());
    }

    final ArrayList<TextPoint2D> posPoints = new ArrayList<>();
    // set up score labels - make TOTAL_LABELS per line
    if (countTotal != 0) {
      final double step = countTotal / (double) TOTAL_LABELS;
      double next = step;
      Point2D p = rangedPoints[0];
      posPoints.add(new TextPoint2D(p.getX(), p.getY(), rangedScores[0]));
      if (step > 0) {
        int c = 0;
        for (int i = 0; i < counts.size(); ++i) {
          for (int j = 0; j < counts.get(i); ++j) {
            ++c;
            if (c >= next && posPoints.size() <= TOTAL_LABELS - 1) {
              while (c >= next) {
                next += step;
              }
              p = rangedPoints[i];
              posPoints.add(new TextPoint2D(p.getX(), p.getY(), rangedScores[i]));
            }
          }
        }
      }
      final int end = rangedScores.length - 1;
      p = rangedPoints[Math.min(end, rangedPoints.length - 1)];
      posPoints.add(new TextPoint2D(p.getX(), p.getY(), rangedScores[end]));
    }
    return posPoints.toArray(new TextPoint2D[posPoints.size()]);
  }

  TextPoint2D getMaxRangedPoint() {
    return mRangedPosPoints.length == 0 ? null : mRangedPosPoints[mRangedPosPoints.length - 1];
  }

  PointPlot2D getPlot(int lineWidth, int colour) {
    final PointPlot2D lplot = makePlot(lineWidth, colour, mRangedPoints);
    return lplot;
  }

  PointPlot2D getPrecisionRecallPlot(int lineWidth, int colour) {
    return makePlot(lineWidth, colour, mRangedPrecisionRecall);
  }

  private PointPlot2D makePlot(int lineWidth, int colour, Point2D[] rangedPrecisionRecall) {
    final PointPlot2D lplot = new PointPlot2D();
    lplot.setData(rangedPrecisionRecall);
    lplot.setPoints(false);
    lplot.setLines(true);
    lplot.setLineWidth(lineWidth);
    lplot.setColor(colour);
    lplot.setTitle(mTitle);
    return lplot;
  }

  TextPlot2D getScoreLabels() {
    return makeScoreLabels(mRangedPosPoints);
  }

  TextPlot2D getPrecisionRecallScoreLabels() {
    return makeScoreLabels(mRangedPrecisionRecallPosPoints);
  }

  private TextPlot2D makeScoreLabels(Point2D[] rangedPrecisionRecall) {
    final TextPlot2D tplot = new TextPlot2D();
    tplot.setData(rangedPrecisionRecall);
    return tplot;
  }

  PointPlot2D getScorePoints(int lineWidth, int colour) {
    return makeScorePoints(lineWidth, colour, mRangedPosPoints);
  }

  private PointPlot2D makeScorePoints(int lineWidth, int colour, TextPoint2D[] rangedPosPoints) {
    final PointPlot2D pplot = new PointPlot2D();
    pplot.setData(rangedPosPoints);
    pplot.setPoints(true);
    pplot.setColor(colour);
    pplot.setLineWidth(lineWidth);
    return pplot;
  }

  PointPlot2D getPrecisionRecallScorePoints(int lineWidth, int colour) {
    return makeScorePoints(lineWidth, colour, mRangedPrecisionRecallPosPoints);
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
