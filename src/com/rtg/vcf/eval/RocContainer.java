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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rtg.util.ContingencyTable;
import com.rtg.util.MathUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.TextTable;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;

/**
 */
public class RocContainer {

  private static final String SLOPE_EXT = "_slope.tsv";
  private final String mFieldLabel;
  private final List<SortedMap<Double, RocPoint>> mRocs = new ArrayList<>();
  private final List<RocFilter> mFilters = new ArrayList<>();
  private final Comparator<Double> mComparator;
  private int mNoScoreVariants = 0;

  /**
   * Constructor
   * @param sortOrder the sort order that ensures that "good" scores are sorted before "bad" scores
   * @param fieldLabel the label for the field being used as the scoring attribute
   */
  public RocContainer(RocSortOrder sortOrder, String fieldLabel) {
    switch (sortOrder) {
      case ASCENDING:
        mComparator = new AscendingDoubleComparator();
        break;
      case DESCENDING:
      default:
        mComparator = new DescendingDoubleComparator();
        break;
    }
    mFieldLabel = fieldLabel;
  }

  /**
   * @return the number of variants not included in ROC curves due to invalid score values
   */
  public int getNumberOfIgnoredVariants() {
    return mNoScoreVariants;
  }

  /**
   * Add an ROC output file
   * @param filter filter to apply to calls, or <code>null</code> for all calls
   */
  public void addFilter(RocFilter filter) {
    mRocs.add(new TreeMap<Double, RocPoint>(mComparator));
    mFilters.add(filter);
  }

  void addStandardFilters() {
    addFilter(RocFilter.ALL);
    addFilter(RocFilter.HETEROZYGOUS);
    addFilter(RocFilter.HOMOZYGOUS);
  }

  void addExtraFilters() {
    addFilter(RocFilter.SIMPLE);
    addFilter(RocFilter.COMPLEX);
    addFilter(RocFilter.HETEROZYGOUS_SIMPLE);
    addFilter(RocFilter.HETEROZYGOUS_COMPLEX);
    addFilter(RocFilter.HOMOZYGOUS_SIMPLE);
    addFilter(RocFilter.HOMOZYGOUS_COMPLEX);
  }

  /**
   * add single result to ROC
   * @param primarySortValue normally the posterior score
   * @param weight weight of the call, 0.0 indicates a false positive with weight of 1
   * @param filters specifies which roc lines the point will be added to
   */
  public void addRocLine(double primarySortValue, double weight, EnumSet<RocFilter> filters) {
    if (Double.isNaN(primarySortValue) || Double.isInfinite(primarySortValue)) {
      mNoScoreVariants++;
    } else {
      for (int i = 0; i < mFilters.size(); i++) {
        final SortedMap<Double, RocPoint> map = mRocs.get(i);
        final RocFilter filter = mFilters.get(i);

        if (filters.contains(filter)) {
          final RocPoint point = new RocPoint(primarySortValue, weight, weight > 0.0 ? 0 : 1);
          final RocPoint old = map.put(primarySortValue, point);
          if (old != null) {
            point.mTp += old.mTp;
            point.mFp += old.mFp;
          }
        }
      }
    }
  }

  private static final String HEADER = "#total baseline variants: ";
  private static final String HEADER2 = "#score true_positives false_positives".replaceAll(" ", "\t");
  private static final String HEADER3 = " false_negatives precision sensitivity f_measure".replaceAll(" ", "\t");
  private void rocHeader(LineWriter out, int totalVariants, boolean extraMetrics) throws IOException {
    out.writeln(HEADER + totalVariants);
    if (mFieldLabel != null) {
      out.writeln("#score field: " + mFieldLabel);
    }
    out.write(HEADER2);
    if (extraMetrics) {
      out.write(HEADER3);
    }
    out.newLine();
  }

  /**
   * output ROC data to files
   * @param outDir directory into which ROC files are written
   * @param truePositives total number of baseline true positives
   * @param falsePositives total number of false positives
   * @param falseNegatives total number of false negatives
   * @param zip whether output should be compressed
   * @param slope if true, write ROC slope file
   * @throws IOException if an IO error occurs
   */
  public void writeRocs(File outDir, int truePositives, int falsePositives, int falseNegatives, boolean zip, boolean slope) throws IOException {
    Diagnostic.developerLog("Writing ROC");
    if (getNumberOfIgnoredVariants() > 0) {
      Diagnostic.warning("There were " + getNumberOfIgnoredVariants() + " variants not thresholded in ROC data files due to missing or invalid " + mFieldLabel + " values.");
    }
    final int totalBaselineVariants = truePositives + falseNegatives;
    for (int i = 0; i < mFilters.size(); i++) {
      final RocFilter filter = mFilters.get(i);
      final File rocFile = FileUtils.getZippedFileName(zip, new File(outDir, filter.filename()));
      try (LineWriter os = new LineWriter(new OutputStreamWriter(FileUtils.createOutputStream(rocFile, zip)))) {
        double tp = 0.0;
        double fp = 0.0;
        final boolean extraMetrics = filter == RocFilter.ALL && totalBaselineVariants > 0;
        rocHeader(os, totalBaselineVariants, extraMetrics);
        for (final Map.Entry<Double, RocPoint> me : mRocs.get(i).entrySet()) {
          final RocPoint p = me.getValue();
          tp += p.mTp;
          fp += p.mFp;
          final String score = Utils.realFormat(me.getKey(), 3);
          writeRocLine(os, score, totalBaselineVariants, tp, fp, extraMetrics);
        }
        if (extraMetrics && (Math.abs(tp - truePositives) > 0.001 || Math.abs(fp - falsePositives) > 0.001)) {
          writeRocLine(os, "None", totalBaselineVariants, truePositives, falsePositives, extraMetrics);
        }
      }
      if (slope) {
        produceSlopeFile(rocFile, zip);
      }
    }
  }

  private void writeRocLine(LineWriter os, String score, int totalPositives, double truePositives, double falsePositives, boolean extraMetrics) throws IOException {
    os.write(score + "\t" + Utils.realFormat(truePositives, 2) + "\t" + Utils.realFormat(falsePositives, 2));
    if (extraMetrics) {
      final double fn = totalPositives - truePositives;
      final double precision = ContingencyTable.precision(truePositives, falsePositives);
      final double recall = ContingencyTable.recall(truePositives, fn);
      final double fMeasure = ContingencyTable.fMeasure(precision, recall);
      os.write("\t" + Utils.realFormat(fn, 2)
        + "\t" + Utils.realFormat(precision, 4)
        + "\t" + Utils.realFormat(recall, 4)
        + "\t" + Utils.realFormat(fMeasure, 4));
    }
    os.newLine();
  }

  private void produceSlopeFile(File input, boolean zip) throws IOException {
    if (input.exists() && input.length() > 0) {
      final File output = new File(input.getParentFile(), input.getName().replaceAll(RocFilter.ROC_EXT, SLOPE_EXT));
      try (final PrintStream printOut = new PrintStream(FileUtils.createOutputStream(output, zip));
           final InputStream in = zip ? FileUtils.createGzipInputStream(input, false) : FileUtils.createFileInputStream(input, false)) {
        RocSlope.writeSlope(in, printOut);
      }
    }
  }

  void writeSummary(File summaryFile, int truePositives, int falsePositives, int falseNegatives) throws IOException {
    final String summary;
    final int totalPositives = truePositives + falseNegatives;
    if (totalPositives > 0) {
      final TextTable table = new TextTable();
      table.addRow("Threshold", "True-pos", "False-pos", "False-neg", "Precision", "Sensitivity", "F-measure");
      table.addSeparator();

      final RocPoint best = bestFMeasure(totalPositives);
      if (best == null) {
        Diagnostic.warning("Not enough ROC data to maximize F-measure, only un-thresholded statistics will be computed. Consider selecting a different scoring attribute with --" + VcfEvalCli.SORT_FIELD);
      } else {
        addRow(table, Utils.realFormat(best.mThreshold, 3), best.mTp, best.mFp, totalPositives - best.mTp);
      }
      addRow(table, "None", truePositives, falsePositives, falseNegatives);
      summary = table.toString();
    } else {
      summary = "0 total baseline variants, no summary statistics available" + StringUtils.LS;
    }
    Diagnostic.info(summary);
    try (OutputStream os = FileUtils.createOutputStream(summaryFile, false)) {
      os.write(summary.getBytes());
    }
  }

  private static void addRow(final TextTable table, String threshold, double truePositive, double falsePositive, double falseNegative) {
    final double precision = ContingencyTable.precision(truePositive, falsePositive);
    final double recall = ContingencyTable.recall(truePositive, falseNegative);
    final double fMeasure = ContingencyTable.fMeasure(precision, recall);
    table.addRow(threshold,
      Long.toString(MathUtils.round(truePositive)),
      Long.toString(MathUtils.round(falsePositive)),
      Long.toString(MathUtils.round(falseNegative)),
      Utils.realFormat(precision, 4),
      Utils.realFormat(recall, 4),
      Utils.realFormat(fMeasure, 4));
  }

  // Find the threshold entry (from the ALL filter) that maximises f-measure, as an (arbitrary but) fair comparison point
  RocPoint bestFMeasure(int totalBaselineVariants) {
    for (int i = 0; i < mFilters.size(); i++) {
      final RocFilter filter = mFilters.get(i);
      if (filter == RocFilter.ALL) {
        double tp = 0.0;
        double fp = 0.0;
        double best = -1;
        RocPoint bestPoint = null;
        for (final Map.Entry<Double, RocPoint> me : mRocs.get(i).entrySet()) {
          final RocPoint p = me.getValue();
          tp += p.mTp;
          fp += p.mFp;
          final double precision = ContingencyTable.precision(tp, fp);
          final double recall = ContingencyTable.recall(tp, totalBaselineVariants - tp);
          final double fMeasure = ContingencyTable.fMeasure(precision, recall);
          if (fMeasure >= best) {
            best = fMeasure;
            bestPoint = new RocPoint(p.mThreshold, tp, fp);
          }
        }
        return bestPoint;
      }
    }
    return null;
  }

  private static final class RocPoint {
    double mThreshold;
    double mTp;
    double mFp;

    private RocPoint(double threshold, double tp, double fp) {
      mThreshold = threshold;
      mTp = tp;
      mFp = fp;
    }
  }

  private static class DescendingDoubleComparator implements Comparator<Double>, Serializable {
    @Override
    public int compare(Double o1, Double o2) {
      return o2.compareTo(o1);
    }
  }

  private static class AscendingDoubleComparator implements Comparator<Double>, Serializable {
    @Override
    public int compare(Double o1, Double o2) {
      return o1.compareTo(o2);
    }
  }
}
