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

import static com.rtg.vcf.eval.RocContainer.RocColumns.FALSE_NEGATIVES;
import static com.rtg.vcf.eval.RocContainer.RocColumns.FALSE_POSITIVES;
import static com.rtg.vcf.eval.RocContainer.RocColumns.F_MEASURE;
import static com.rtg.vcf.eval.RocContainer.RocColumns.PRECISION;
import static com.rtg.vcf.eval.RocContainer.RocColumns.SCORE;
import static com.rtg.vcf.eval.RocContainer.RocColumns.SENSITIVITY;
import static com.rtg.vcf.eval.RocContainer.RocColumns.TRUE_POSITIVES_BASELINE;
import static com.rtg.vcf.eval.RocContainer.RocColumns.TRUE_POSITIVES_CALL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.ContingencyTable;
import com.rtg.util.Environment;
import com.rtg.util.MathUtils;
import com.rtg.util.MultiSet;
import com.rtg.util.StringUtils;
import com.rtg.util.TextTable;
import com.rtg.util.Utils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class RocContainer {

  /**
   * List of ROC file column headings
   */
  public static class RocColumns {
    /** Header for the score column */
    public static final String SCORE = "score";
    /** Header for the true positives when there is only one column */
    public static final String TRUE_POSITIVES = "true_positives";
    /** Header for the true positives counted in baseline space */
    public static final String TRUE_POSITIVES_BASELINE = "true_positives_baseline";
    /** Header for the false positives */
    public static final String FALSE_POSITIVES = "false_positives";
    /** Header for the true positives counted in call space */
    public static final String TRUE_POSITIVES_CALL = "true_positives_call";
    /** Header for the false negative column */
    public static final String FALSE_NEGATIVES = "false_negatives";
    /** Header for the precision column */
    public static final String PRECISION = "precision";
    /** Header for the sensitivity column */
    public static final String SENSITIVITY = "sensitivity";
    /** Header for the F measure column */
    public static final String F_MEASURE = "f_measure";
  }

  private static final int SCORE_DP = 3; // Decimal points for score field
  private static final int COUNT_DP = 2; // Decimal points for weighted variant counts
  private static final int METRICS_DP = 4; // Decimal points for metrics such as precision/recall

  private static final String SLOPE_EXT = "_slope.tsv";

  private final String mFieldLabel;
  private final Map<RocFilter, SortedMap<Double, RocPoint<Double>>> mRocs = new LinkedHashMap<>();
  private final Comparator<Double> mComparator;
  private final RocSortValueExtractor mRocExtractor;
  private final String mFilePrefix;
  private final RocFilter mDefaultRocFilter;
  private RocPointCriteria mBestCutpoint = new FMeasureThreshold();
  private int mNoScoreVariants = 0;
  private boolean mRequiresGt = false;

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
    this(extractor, filePrefix, RocFilter.ALL);
  }

  /**
   * Constructor
   * @param extractor responsible for extracting ROC scores from VCF records.
   * @param filePrefix prefix attached to ROC output file names
   * @param filter the RocFilter to regard as the default for summary reporting
   */
  public RocContainer(RocSortValueExtractor extractor, String filePrefix, RocFilter filter) {
    mComparator = extractor.getSortOrder().comparator();
    mFieldLabel = extractor.toString();
    mFilePrefix = filePrefix;
    mRocExtractor = extractor;
    mDefaultRocFilter = filter;
  }

  /**
   * Sets the criteria for selecting a particular point on the ROC curve
   * @param criteria criteria implementation
   */
  public void setRocPointCriteria(RocPointCriteria criteria) {
    mBestCutpoint = criteria;
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
    mRocs.put(filter, new TreeMap<>(mComparator));
    mRequiresGt |= filter.requiresGt();
  }

  /**
   * Add ROC outputs for all the supplied filters to the container
   * @param filters filtered curves to generate
   */
  public void addFilters(Set<RocFilter> filters) {
    assert filters != null;
    for (RocFilter f : filters) {
      addFilter(f);
    }
  }

  /**
   * @return the current set of filters.
   */
  public Collection<RocFilter> filters() {
    return mRocs.keySet();
  }

  MultiSet<RocFilter> mBaselineTotals = new MultiSet<>();
  MultiSet<RocFilter> mBaselineTpTotal = new MultiSet<>();

  /**
   * Add an assessed baseline variant to the total baseline count
   * @param rec the VCF record containing the baseline variant
   * @param sampleId index of the sample column for identifying the variant classification
   * @param correct if the baseline variant was correct
   */
  public void incrementBaselineCount(VcfRecord rec, int sampleId, boolean correct) {
    incrementBaselineCount(rec, sampleId, correct, 1);
  }

  /**
   * Add an assessed baseline variant to the total baseline count
   * @param rec the VCF record containing the baseline variant
   * @param sampleId index of the sample column for identifying the variant classification
   * @param correct if the baseline variant was correct
   * @param weight weight of variant
   */
  public void incrementBaselineCount(VcfRecord rec, int sampleId, boolean correct, int weight) {
    final int[] gt = mRequiresGt ? VcfUtils.getValidGt(rec, sampleId) : null;
    for (final RocFilter filter : filters()) {
      if (filter.accept(rec, gt)) {
        mBaselineTotals.add(filter, weight);
        if (correct) {
          mBaselineTpTotal.add(filter, weight);
        }
      }
    }
  }


  /**
   * Prepare the container for accepting roc points from VCF records.
   * @param header the header corresponding to subsequent records.
   */
  public void setHeader(VcfHeader header) {
    filters().forEach(f -> f.setHeader(header));
    mRocExtractor.setHeader(header);
  }

  /**
   * Add single called variant result to the ROC
   * @param rec the VCF record to incorporate into the ROC curves
   * @param sampleId the index of the sample column containing the variant (for score extraction and variant classification)
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
    final RocPoint<Double> point;
    if (Double.isNaN(score) || Double.isInfinite(score)) {
      ++mNoScoreVariants;
      point = new RocPoint<>(Double.NaN, tpWeight, fpWeight, tpRaw);
    } else {
      point = new RocPoint<>(score, tpWeight, fpWeight, tpRaw);
    }
    final int[] gt = mRequiresGt ? VcfUtils.getValidGt(rec, sampleId) : null;
    for (final RocFilter filter : filters()) {
      if (filter.accept(rec, gt)) {
        addRocLine(point, filter);
      }
    }
  }

  /**
   * add single result to ROC
   * @param point the ROC point
   * @param filter specifies which roc line the point will be added to
   */
  void addRocLine(RocPoint<Double> point, RocFilter filter) {
    final SortedMap<Double, RocPoint<Double>> points = mRocs.get(filter);
    if (points.containsKey(point.getThreshold())) {
      points.get(point.getThreshold()).add(point);
    } else {
      points.put(point.getThreshold(), new RocPoint<>(point));
    }
  }

  /**
   * Output ROC data to files. While scanning ROC data also keeps a record to the point with highest f-Measure.
   * @param outDir directory into which ROC files are written
   * @param zip whether output should be compressed
   * @param slope if true, write ROC slope file
   * @throws IOException if an IO error occurs
   */
  public void writeRocs(File outDir, boolean zip, boolean slope) throws IOException {
    Diagnostic.developerLog("Writing ROC");
    mBestCutpoint.init();
    for (final Map.Entry<RocFilter, SortedMap<Double, RocPoint<Double>>> entry : mRocs.entrySet()) {
      final RocFilter filter = entry.getKey();
      // Compute adjustment factor for sub-categorized calls when call representation is systematically
      // different to baseline representation.
      // For example, if the baseline atomizes all variants to SNPs but the called variants keep MNPs and complex variants as a
      // unit, many of the TP baseline SNPs end up getting their correctness associated with the non_snp ROC. To
      // adjust for this, we scale each categorized baseline TP metric so that the endpoints are the same.
      final boolean rescale = filter.rescale();
      final int totalBaselineVariants = mBaselineTotals.get(rescale ? filter : mDefaultRocFilter);
      final RocPoint<Double> total = getTotal(rescale ? filter : mDefaultRocFilter); // Get totals according to call-representation categorization
      final int totalCallVariants = (int) Math.round(total.getRawTruePositives() + total.getFalsePositives());

      final double scale;
      final boolean extraMetrics;
      if (rescale) {
        extraMetrics = totalBaselineVariants > 0;
        final int totalBaselineTp = mBaselineTpTotal.get(filter); // Get totals according to baseline-representation categorization
        scale = total.getTruePositives() > 0 ? totalBaselineTp / total.getTruePositives() : 1;
        Diagnostic.userLog("Representation bias correction factor for " + filter + " " + totalBaselineTp + "/" + total.getTruePositives() + " = " + scale);
      } else {
        extraMetrics = filter == mDefaultRocFilter && totalBaselineVariants > 0;
        scale = 1.0;
      }

      final SortedMap<Double, RocPoint<Double>> points = entry.getValue();
      final File rocFile = FileUtils.getZippedFileName(zip, new File(outDir, mFilePrefix + filter.fileName()));
      try (LineWriter os = new LineWriter(new OutputStreamWriter(FileUtils.createOutputStream(rocFile)))) {
        rocHeader(os, filter, totalBaselineVariants, totalCallVariants, extraMetrics, rescale);
        String prevScore = null;
        String score = "None";
        final RocPoint<Double> cumulative = new RocPoint<>();
        for (final Map.Entry<Double, RocPoint<Double>> me : points.entrySet()) {
          final RocPoint<Double> point = me.getValue();
          score = Double.isNaN(point.getThreshold()) ? "None" : Utils.realFormat(point.getThreshold(), SCORE_DP);
          if (prevScore != null && score.compareTo(prevScore) != 0) {
            writeRocLine(os, filter, prevScore, totalBaselineVariants, cumulative, extraMetrics, scale);
          }
          prevScore = score;
          cumulative.add(point);
          cumulative.setThreshold(point.getThreshold());
        }
        if (prevScore != null || (totalBaselineVariants > 0)) {
          writeRocLine(os, filter, score, totalBaselineVariants, cumulative, extraMetrics, scale);
        }
      }
      if (slope) {
        produceSlopeFile(rocFile);
      }
    }
  }

  private void rocHeader(LineWriter out, RocFilter filter, int totalBaselineVariants, int totalCallVariants, boolean extraMetrics, boolean rescaled) throws IOException {
    out.writeln("#Version " + Environment.getVersion() + ", ROC output 1.2");
    if (CommandLine.getCommandLine() != null) {
      out.writeln("#CL " + CommandLine.getCommandLine());
    }
    out.writeln("#selection: " + filter.name() + (rescaled ? " (baseline rescaled)" : ""));
    out.writeln("#total baseline variants: " + totalBaselineVariants);
    out.writeln("#total call variants: " + totalCallVariants);
    out.writeln("#score field: " + mFieldLabel);
    final List<String> baseRocColumns = Arrays.asList(SCORE, TRUE_POSITIVES_BASELINE, FALSE_POSITIVES, TRUE_POSITIVES_CALL);
    out.write("#" + String.join("\t", baseRocColumns));

    if (extraMetrics) {
      final List<String> extraRocColumns = Arrays.asList(FALSE_NEGATIVES, PRECISION, SENSITIVITY, F_MEASURE);
      out.write("\t" + String.join("\t", extraRocColumns));
    }
    out.newLine();
  }

  private void writeRocLine(LineWriter os, RocFilter filter, String score, int totalPositives, RocPoint<Double> point, boolean extraMetrics, double tpScaleFactor) throws IOException {
    final double truePositives = point.getTruePositives() * tpScaleFactor;
    final double falsePositives = point.getFalsePositives();
    final double truePositivesRaw = point.getRawTruePositives();
    os.write(score
      + "\t" + Utils.realFormat(truePositives, COUNT_DP)
      + "\t" + Utils.realFormat(falsePositives, COUNT_DP)
      + "\t" + Utils.realFormat(truePositivesRaw, COUNT_DP));
    if (extraMetrics) {
      final double fn = totalPositives - truePositives;
      final double precision = ContingencyTable.precision(truePositivesRaw, falsePositives);
      final double recall = ContingencyTable.recall(truePositives, fn);
      final double fMeasure = ContingencyTable.fMeasure(precision, recall);
      os.write("\t" + Utils.realFormat(fn, COUNT_DP)
        + "\t" + Utils.realFormat(precision, METRICS_DP)
        + "\t" + Utils.realFormat(recall, METRICS_DP)
        + "\t" + Utils.realFormat(fMeasure, METRICS_DP));
      if ((filter == mDefaultRocFilter) && point.getThreshold() != null && !Double.isNaN(point.getThreshold())) {
        mBestCutpoint.considerRocPoint(point, precision, recall, fMeasure);
      }
    }
    os.newLine();
  }

  private static void addSummaryRow(final TextTable table, String threshold, double truePositive, double falseNegative, double truePositiveRaw, double falsePositive) {
    final double precision = ContingencyTable.precision(truePositiveRaw, falsePositive);
    final double recall = ContingencyTable.recall(truePositive, falseNegative);
    final double fMeasure = ContingencyTable.fMeasure(precision, recall);
    table.addRow(threshold,
      Long.toString(MathUtils.round(truePositive)),
      Long.toString(MathUtils.round(truePositiveRaw)),
      Long.toString(MathUtils.round(falsePositive)),
      Long.toString(MathUtils.round(falseNegative)),
      Utils.realFormat(precision, METRICS_DP),
      Utils.realFormat(recall, METRICS_DP),
      Utils.realFormat(fMeasure, METRICS_DP));
  }

  /**
   * Write the summary file for the ROC data
   * @param outDir directory into which ROC files are written
   * @throws IOException if an IO error occurs
   */
  public void writeSummary(File outDir) throws IOException {
    final File summaryFile = new File(outDir, mFilePrefix + CommonFlags.SUMMARY_FILE);
    final String summary;
    final int totalPositives = mBaselineTotals.get(mDefaultRocFilter);
    if (totalPositives > 0) {
      final TextTable table = new TextTable();
      table.addRow("Threshold", "True-pos-baseline", "True-pos-call", "False-pos", "False-neg", "Precision", "Sensitivity", "F-measure");
      table.addSeparator();

      if (isRocEnabled()) {
        final RocPoint<Double> best = mBestCutpoint.cutpoint();
        if (best == null) {
          Diagnostic.warning("Could not select " + mBestCutpoint.name() + " threshold from ROC data, only un-thresholded statistics will be shown. Consider selecting a different scoring attribute with --" + VcfEvalCli.SCORE_FIELD);
        } else {
          Diagnostic.info("Selected score threshold using: " + mBestCutpoint.name());
          addSummaryRow(table, Utils.realFormat(best.getThreshold(), SCORE_DP), best.getTruePositives(), totalPositives - best.getTruePositives(), best.getRawTruePositives(), best.getFalsePositives());
        }
      }
      final RocPoint<Double> total = getTotal(mDefaultRocFilter);
      addSummaryRow(table, "None", total.getTruePositives(), totalPositives - total.getTruePositives(), total.getRawTruePositives(), total.getFalsePositives());
      summary = table.toString();
    } else {
      summary = "0 total baseline variants, no summary statistics available" + StringUtils.LS;
    }
    Diagnostic.info(summary);
    try (OutputStream os = FileUtils.createOutputStream(summaryFile)) {
      os.write(summary.getBytes());
    }
  }

  private void produceSlopeFile(File input) throws IOException {
    if (input.exists() && input.length() > 0) {
      final File output = new File(input.getParentFile(), input.getName().replaceAll(RocFilter.ROC_EXT, SLOPE_EXT));
      try (final PrintStream printOut = new PrintStream(FileUtils.createOutputStream(output));
           final InputStream in = FileUtils.createInputStream(input, false)) {
        RocSlope.writeSlope(in, printOut);
      }
    }
  }

  /**
   * @return true if we are configured for generating ROC data files (rather than just aggregate statistics)
   */
  public boolean isRocEnabled() {
    return mRocExtractor != RocSortValueExtractor.NULL_EXTRACTOR;
  }

  /**
   * Issue a warning if there were variants that did not contain the score field.
   */
  public void missingScoreWarning() {
    if (isRocEnabled() && getNumberOfIgnoredVariants() > 0) {
      Diagnostic.warning("There were " + getNumberOfIgnoredVariants() + " variants not thresholded in ROC data files due to missing or invalid " + mFieldLabel + " values.");
    }
  }

  RocPoint<Double> getTotal(RocFilter filter) {
    final RocPoint<Double> total = new RocPoint<>();
    for (final RocPoint<Double> point : mRocs.get(filter).values()) {
      total.add(point);
    }
    return total;
  }
}
