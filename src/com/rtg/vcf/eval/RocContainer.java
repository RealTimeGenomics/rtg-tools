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
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.ContingencyTable;
import com.rtg.util.MathUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.TextTable;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 */
public class RocContainer {

  private static final String SLOPE_EXT = "_slope.tsv";
  private final String mFieldLabel;
  private final Map<RocFilter, SortedMap<Double, RocPoint>> mRocs = new HashMap<>();
  private final Comparator<Double> mComparator;
  private int mNoScoreVariants = 0;
  private final RocSortValueExtractor mRocExtractor;
  private final String mFilePrefix;
  private boolean mRequiresGt = false;
  private RocPoint mBest = null;
  private double mBestFMeasure = 0;

  /**
   * Constructor
   * @param extractor responsible for extracting ROC scores from VCF records.
   */
  public RocContainer(RocSortValueExtractor extractor) {
    this(extractor, "");
  }

  /**
   * Constructor
   * @param extractor responsible for extracting ROC scores from VCF records.
   * @param filePrefix prefix attached to ROC output file names
   */
  public RocContainer(RocSortValueExtractor extractor, String filePrefix) {

    switch (extractor.getSortOrder()) {
      case ASCENDING:
        mComparator = new AscendingDoubleComparator();
        break;
      case DESCENDING:
      default:
        mComparator = new DescendingDoubleComparator();
        break;
    }
    mFieldLabel = extractor.toString();
    mFilePrefix = filePrefix;
    mRocExtractor = extractor;
  }

  /**
   * @return the number of variants not included in ROC curves due to invalid score values
   */
  public int getNumberOfIgnoredVariants() {
    return mNoScoreVariants;
  }

  /**
   * Add an ROC output file corresponding to a filtered view of the calls
   * @param filter filtered curve to generate
   */
  public void addFilter(RocFilter filter) {
    mRocs.put(filter, new TreeMap<Double, RocPoint>(mComparator));
    mRequiresGt |= filter.requiresGt();
  }

  /**
   * Add ROC outputs for all the supplied filters to the container
   * @param filters filtered curves to generate
   */
  void addFilters(EnumSet<RocFilter> filters) {
    if (filters != null) {
      for (RocFilter f : filters) {
        addFilter(f);
      }
    }
  }

  Collection<RocFilter> filters() {
    return mRocs.keySet();
  }

  /**
   * add single result to ROC
   * @param rec the VCF record to incorporate into the ROC curves
   * @param sampleId the index of the sample column to use for score extraction
   * @param tpWeight true positive weight of the call (baseline weighting).
   * @param fpWeight false positive weight of the call.
   * @param tpRaw true positive weight of the call (unweighted).
   */
  public void addRocLine(VcfRecord rec, int sampleId, double tpWeight, double fpWeight, double tpRaw) {
    double score = Double.NaN;
    try {
      score = mRocExtractor.getSortValue(rec, sampleId);
    } catch (IndexOutOfBoundsException ignored) {
    }
    if (Double.isNaN(score) || Double.isInfinite(score)) {
      mNoScoreVariants++;
    } else {
      final RocPoint point = new RocPoint(score, tpWeight, fpWeight, tpRaw);
      final int[] gt = mRequiresGt ? VcfUtils.getValidGt(rec, sampleId) : null;
      for (final RocFilter filter : filters()) {
        if (filter.accept(rec, gt)) {
          addRocLine(point, filter);
        }
      }
    }
  }

  /**
   * add single result to ROC
   * @param point the ROC point
   * @param filter specifies which roc line the point will be added to
   */
  void addRocLine(RocPoint point, RocFilter filter) {
    final SortedMap<Double, RocPoint> points = mRocs.get(filter);
    if (points.containsKey(point.mThreshold)) {
      points.get(point.mThreshold).add(point);
    } else {
      points.put(point.mThreshold, new RocPoint(point));
    }
  }

  /**
   * Output ROC data to files. While scanning ROC data also keeps a record to the point with highest f-Measure.
   * @param outDir directory into which ROC files are written
   * @param truePositives total number of baseline true positives
   * @param falsePositives total number of false positives
   * @param falseNegatives total number of false negatives
   * @param truePositivesRaw total number of call true positives
   * @param zip whether output should be compressed
   * @param slope if true, write ROC slope file
   * @throws IOException if an IO error occurs
   */
  public void writeRocs(File outDir, int truePositives, int falsePositives, int falseNegatives, int truePositivesRaw, boolean zip, boolean slope) throws IOException {
    Diagnostic.developerLog("Writing ROC");
    mBestFMeasure = 0;
    mBest = null;
    final RocPoint unfiltered = new RocPoint(Double.NaN, truePositives, falsePositives, truePositivesRaw);
    final int totalBaselineVariants = truePositives + falseNegatives;
    final int totalCallVariants = truePositivesRaw + falsePositives;
    for (Map.Entry<RocFilter, SortedMap<Double, RocPoint>> entry : mRocs.entrySet()) {
      final RocFilter filter = entry.getKey();
      final SortedMap<Double, RocPoint> points = entry.getValue();
      final File rocFile = FileUtils.getZippedFileName(zip, new File(outDir, mFilePrefix + filter.fileName()));
      try (LineWriter os = new LineWriter(new OutputStreamWriter(FileUtils.createOutputStream(rocFile, zip)))) {
        final RocPoint current = new RocPoint();
        final boolean extraMetrics = filter == RocFilter.ALL && totalBaselineVariants > 0;
        rocHeader(os, totalBaselineVariants, totalCallVariants, extraMetrics);
        for (final Map.Entry<Double, RocPoint> me : points.entrySet()) {
          final RocPoint point = me.getValue();
          current.add(point);
          current.mThreshold = point.mThreshold;
          final String score = Utils.realFormat(point.mThreshold, 3);
          writeRocLine(os, score, totalBaselineVariants, current, extraMetrics, filter == RocFilter.ALL);
        }
        if (extraMetrics && (Math.abs(current.mTp - unfiltered.mTp) > 0.001 || Math.abs(current.mFp - unfiltered.mFp) > 0.001)) {
          writeRocLine(os, "None", totalBaselineVariants, unfiltered, extraMetrics, false);
        }
      }
      if (slope) {
        produceSlopeFile(rocFile, zip);
      }
    }
  }

  private void rocHeader(LineWriter out, int totalBaselineVariants, int totalCallVariants, boolean extraMetrics) throws IOException {
    out.writeln("#total baseline variants: " + totalBaselineVariants);
    out.writeln("#total call variants: " + totalCallVariants);
    if (mFieldLabel != null) {
      out.writeln("#score field: " + mFieldLabel);
    }
    out.write("#score true_positives_baseline false_positives true_positives_call".replaceAll(" ", "\t"));
    if (extraMetrics) {
      out.write(" false_negatives precision sensitivity f_measure".replaceAll(" ", "\t"));
    }
    out.newLine();
  }

  private void writeRocLine(LineWriter os, String score, int totalPositives, RocPoint point, boolean extraMetrics, boolean updateBest) throws IOException {
    final double truePositives = point.mTp;
    final double falsePositives = point.mFp;
    final double truePositivesRaw = point.mTpRaw;
    os.write(score
      + "\t" + Utils.realFormat(truePositives, 2)
      + "\t" + Utils.realFormat(falsePositives, 2)
      + "\t" + Utils.realFormat(truePositivesRaw, 2));
    if (extraMetrics) {
      final double fn = totalPositives - truePositives;
      final double precision = ContingencyTable.precision(truePositivesRaw, falsePositives);
      final double recall = ContingencyTable.recall(truePositives, fn);
      final double fMeasure = ContingencyTable.fMeasure(precision, recall);
      os.write("\t" + Utils.realFormat(fn, 2)
        + "\t" + Utils.realFormat(precision, 4)
        + "\t" + Utils.realFormat(recall, 4)
        + "\t" + Utils.realFormat(fMeasure, 4));
      if (updateBest && (mBest == null || fMeasure >= mBestFMeasure)) {
        mBestFMeasure = fMeasure;
        mBest = new RocPoint(point);
      }
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

  void missingScoreWarning() {
    if (getNumberOfIgnoredVariants() > 0) {
      Diagnostic.warning("There were " + getNumberOfIgnoredVariants() + " variants not thresholded in ROC data files due to missing or invalid " + mFieldLabel + " values.");
    }
  }

  void writeSummary(File outDir, int truePositives, int falseNegatives, int truePositivesRaw, int falsePositives) throws IOException {
    final File summaryFile = new File(outDir, mFilePrefix + CommonFlags.SUMMARY_FILE);
    final String summary;
    final int totalPositives = truePositives + falseNegatives;
    if (totalPositives > 0) {
      final TextTable table = new TextTable();
      table.addRow("Threshold", "True-pos-baseline", "True-pos-call", "False-pos", "False-neg", "Precision", "Sensitivity", "F-measure");
      table.addSeparator();

      final RocPoint best = mBest;
      if (best == null) {
        Diagnostic.warning("Not enough ROC data to maximize F-measure, only un-thresholded statistics will be computed. Consider selecting a different scoring attribute with --" + VcfEvalCli.SORT_FIELD);
      } else {
        addRow(table, Utils.realFormat(best.mThreshold, 3), best.mTp, totalPositives - best.mTp, best.mTpRaw, best.mFp);
      }
      addRow(table, "None", truePositives, falseNegatives, truePositivesRaw, falsePositives);
      summary = table.toString();
    } else {
      summary = "0 total baseline variants, no summary statistics available" + StringUtils.LS;
    }
    Diagnostic.info(summary);
    try (OutputStream os = FileUtils.createOutputStream(summaryFile, false)) {
      os.write(summary.getBytes());
    }
  }

  private static void addRow(final TextTable table, String threshold, double truePositive, double falseNegative, double truePositiveRaw, double falsePositive) {
    final double precision = ContingencyTable.precision(truePositiveRaw, falsePositive);
    final double recall = ContingencyTable.recall(truePositive, falseNegative);
    final double fMeasure = ContingencyTable.fMeasure(precision, recall);
    table.addRow(threshold,
      Long.toString(MathUtils.round(truePositive)),
      Long.toString(MathUtils.round(truePositiveRaw)),
      Long.toString(MathUtils.round(falsePositive)),
      Long.toString(MathUtils.round(falseNegative)),
      Utils.realFormat(precision, 4),
      Utils.realFormat(recall, 4),
      Utils.realFormat(fMeasure, 4));
  }


  static final class RocPoint {
    double mThreshold;
    double mTp;
    double mTpRaw;
    double mFp;

    RocPoint() {
      this(Double.NaN, 0, 0, 0);
    }
    RocPoint(RocPoint other) {
      this(other.mThreshold, other.mTp, other.mFp, other.mTpRaw);
    }
    RocPoint(double threshold, double tp, double fp, double tpraw) {
      mThreshold = threshold;
      mTp = tp;
      mFp = fp;
      mTpRaw = tpraw;
    }
    void add(RocPoint p) {
      mTp += p.mTp;
      mFp += p.mFp;
      mTpRaw += p.mTpRaw;
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
