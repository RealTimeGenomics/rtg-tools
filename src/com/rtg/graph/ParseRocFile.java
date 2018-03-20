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

import static com.rtg.vcf.eval.RocContainer.RocColumns.FALSE_POSITIVES;
import static com.rtg.vcf.eval.RocContainer.RocColumns.SCORE;
import static com.rtg.vcf.eval.RocContainer.RocColumns.TRUE_POSITIVES;
import static com.rtg.vcf.eval.RocContainer.RocColumns.TRUE_POSITIVES_BASELINE;
import static com.rtg.vcf.eval.RocContainer.RocColumns.TRUE_POSITIVES_CALL;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.eval.RocPoint;
import com.rtg.vcf.eval.RocUtils;

/**
 */
public final class ParseRocFile {

  private static final float MIN_SENSITIVITY_INTERVAL = GlobalFlags.getIntegerValue(ToolsGlobalFlags.ROCPLOT_INTERPOLATION_GAP);
  private static final boolean LABEL_INTERPOLATED = GlobalFlags.getBooleanValue(ToolsGlobalFlags.ROCPLOT_INTERPOLATE_LABEL);

  /** These headings are used when there might be an additional call space true positives column */
  private static final List<String> WITH_RAW_HEADINGS = Arrays.asList(SCORE, TRUE_POSITIVES_BASELINE, FALSE_POSITIVES);
  /** Legacy headings */
  private static final List<String> SIMPLE_HEADINGS = Arrays.asList(SCORE, TRUE_POSITIVES, FALSE_POSITIVES);

  // Can be used when no load progress updates are required
  static class NullProgressDelegate implements ProgressDelegate {
    @Override
    public void setProgress(int progress) {
    }

    @Override
    public void addFile(int numberLines) {
    }

    @Override
    public void done() {
    }
  }

  private ParseRocFile() {
  }

  /**
   * Loads ROC file into data bundle.
   * @param progressBarDelegate called every 100 lines with progress, and at end with file stats
   * @param is input data. this stream is closed by this method.
   * @param shortName name for line
   * @param interpolate true if curve should be interpolated at regular sensitivity intervals
   * @return the data bundle
   * @throws IOException if an IO error occurs
   */
  static DataBundle loadStream(ProgressDelegate progressBarDelegate, final BufferedInputStream is, final String shortName, boolean interpolate) throws IOException {
    int lines = 0;
    int totalVariants = -1;
    final ArrayList<RocPoint<String>> points = new ArrayList<>();

    String line = null;
    String scoreName = null;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String prevScore = null;
      float prevFp = 0.0f;
      float prevTp = 0.0f;
      float prevRawTp = 0.0f;
      // These defaults may be overridden if we find a header row
      int scoreCol = 0;
      int tpCol = 1;
      int fpCol = 2;
      int tpRawCol = -1;
      // We create an artificial initial point corresponding to 0 TP and 0 FP.
      points.add(new RocPoint<>(null, prevTp, prevFp, prevRawTp));
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#")) {
          if (line.startsWith("#total")) {
            final String[] parts = line.split("\\s");
            if (parts.length > 3) {
              switch (parts[1]) {
                case "baseline":
                  totalVariants = Integer.parseInt(parts[parts.length - 1]);
                  break;
                default:
                  break;
              }
            } else {
              totalVariants = Integer.parseInt(parts[parts.length - 1]);
            }
          } else if (line.startsWith("#score field:")) {
            final String[] parts = line.split("\\s");
            if (parts.length >= 3) {
              scoreName = parts[2];
            }
          } else {
            final List<String> header = Arrays.asList(StringUtils.split(line.substring(1), '\t'));
            if (header.containsAll(WITH_RAW_HEADINGS)) {
              scoreCol = header.indexOf(SCORE);
              tpCol = header.indexOf(TRUE_POSITIVES_BASELINE);
              fpCol = header.indexOf(FALSE_POSITIVES);
              tpRawCol = header.indexOf(TRUE_POSITIVES_CALL);
            } else if (header.containsAll(SIMPLE_HEADINGS)) {
              scoreCol = header.indexOf(SCORE);
              tpCol = header.indexOf(TRUE_POSITIVES);
              fpCol = header.indexOf(FALSE_POSITIVES);
            }
          }
          continue;
        }
        final String[] linearr = line.split("\t");
        if (linearr.length < 3) {
          throw new NoTalkbackSlimException("Malformed line: " + line + " in \"" + shortName + "\"");
        }
        final float fp = Float.parseFloat(linearr[fpCol]); // False positives
        final float tp = Float.parseFloat(linearr[tpCol]); // True positives
        final float rawTp;
        if (tpRawCol > -1 && linearr.length > tpRawCol) {
          rawTp = Float.parseFloat(linearr[tpRawCol]);
        } else {
          rawTp = 0.0f;
        }
        String score;
        try {
          final float numeric = Float.parseFloat(linearr[scoreCol]); // Score
          score = String.format("%.3g", numeric);
        } catch (final NumberFormatException e) {
          score = linearr[0];
        }
        if (prevScore != null && score.compareTo(prevScore) != 0) {
          points.add(new RocPoint<>(prevScore, prevTp, prevFp, rawTp));
        }
        prevFp = Math.max(prevFp, fp);
        prevTp = Math.max(prevTp, tp);
        prevRawTp = Math.max(prevRawTp, rawTp);
        prevScore = score;

        ++lines;
        if (lines % 100 == 0) {
          progressBarDelegate.setProgress(lines);
        }
      }
      if (prevScore != null) {
        points.add(new RocPoint<>(prevScore, prevTp, prevFp, prevRawTp));
      }
    } catch (final NumberFormatException e) {
      throw new NoTalkbackSlimException("Malformed line: " + line + " in \"" + shortName + "\"");
    }
    progressBarDelegate.addFile(lines);
    final List<RocPoint<String>> finalPoints = interpolate
      ? RocUtils.interpolate(points, (double) (totalVariants * MIN_SENSITIVITY_INTERVAL / 100), LABEL_INTERPOLATED)
      : points;
    final DataBundle dataBundle = new DataBundle(totalVariants, finalPoints);
    dataBundle.setTitle(shortName);
    dataBundle.setScoreName(scoreName);
    return dataBundle;
  }
}
