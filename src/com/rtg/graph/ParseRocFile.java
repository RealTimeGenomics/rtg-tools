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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.reeltwo.plot.Point2D;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 */
public final class ParseRocFile {

  private ParseRocFile() {
  }

  /**
   * loads ROC file into data bundle
   * @param progressBarDelegate called every 100 lines with progress, and at end with file stats
   * @param is input data. this stream is closed by this method.
   * @param shortName name for line
   * @param showProgress true if progress should be sent
   * @return the data bundle
   * @throws IOException if an IO error occurs
   */
  static DataBundle loadStream(ProgressDelegate progressBarDelegate, final BufferedInputStream is, final String shortName, boolean showProgress) throws IOException {
    int lines = 0;
    int totalVariants = -1;
    final ArrayList<Point2D> points = new ArrayList<>();
    final ArrayList<String> scores = new ArrayList<>();

    String line = null;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String prevScore = null;
      float prevX = 0.0f;
      float prevY = 0.0f;
      String score = String.format("%.3g", 0.0f);
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#")) {
          if (line.contains("#total")) {
            final String[] parts = line.split("\\s");
            totalVariants = Integer.parseInt(parts[parts.length - 1]);
          }
          continue;
        }
        final String[] linearr = line.split("\t");
        if (linearr.length < 3) {
          throw new NoTalkbackSlimException("Malformed line: " + line + " in \"" + shortName + "\"");
        }
        final float x = Float.parseFloat(linearr[2]); // False positives
        final float y = Float.parseFloat(linearr[1]); // True positives
        try {
          final float numeric = Float.parseFloat(linearr[0]); // Score
          score = String.format("%.3g", numeric);
        } catch (final NumberFormatException e) {
          score = linearr[0];
        }
        if (prevScore == null || score.compareTo(prevScore) != 0) {
          points.add(new Point2D(prevX, prevY));
          scores.add(score);
        }
        prevX = Math.max(prevX, x);
        prevY = Math.max(prevY, y);
        prevScore = score;

        lines++;
        if (showProgress && lines % 100 == 0) {
          progressBarDelegate.setProgress(lines);
        }
      }
      points.add(new Point2D(prevX, prevY));
      scores.add(score);
    } catch (final NumberFormatException e) {
      throw new NoTalkbackSlimException("Malformed line: " + line + " in \"" + shortName + "\"");
    }
    progressBarDelegate.addFile(lines);
    return new DataBundle(shortName, points.toArray(new Point2D[points.size()]), scores.toArray(new String[scores.size()]), totalVariants);
  }
}
