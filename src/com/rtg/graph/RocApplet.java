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

import java.awt.BorderLayout;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JApplet;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.util.gzip.WorkingGzipInputStream;
import com.rtg.util.io.FileUtils;

/**
 * An applet version of the ROC plot.
 */
@JumbleIgnore
public class RocApplet extends JApplet {

  /* Embed in HTML with something like:

<applet width="100%" height="100%" code="com.rtg.graph.RocApplet" archive="ROC.jar">
<param name="data1" value="snp_pe_snp1_cal.eval/weighted_roc.tsv"> <param name="name1" value="curve number 1">
<param name="data2" value="snp_pe_snp2_cal.eval/weighted_roc.tsv"> <param name="name2" value="funky curve">
<param name="data3" value="snp_pe_snp2_nocal.eval/weighted_roc.tsv"> <param name="name3" value="another curve">
<param name="data4" value="snp_pe_snp1_nocal.eval/weighted_roc.tsv">
</applet>

   */

  static final String PARAM_SIDEPANE = "sidepane";
  static final String PARAM_NAME = "name";
  static final String PARAM_DATA = "data";
  static final String PARAM_TITLE = "title";
  static final String PARAM_SCORES = "scores";
  static final String PARAM_LINEWIDTH = "linewidth";

  RocPlot mRocPlot = null;

  @Override
  public void start() {
    try {
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          mRocPlot.showCurrentGraph();
        }
      });
    } catch (final Exception e) {
      System.err.println("createGUI didn't successfully complete: " + e.getMessage());
    }
  }

  @Override
  public void init() {
    try {
      mRocPlot = new RocPlot();
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          setLayout(new BorderLayout());
          add(mRocPlot.getMainPanel(), BorderLayout.CENTER);
          setGlassPane(mRocPlot.getZoomPlotPanel());
          getGlassPane().setVisible(true);
          mRocPlot.showOpenButton(false);

          final String title = getParameter(PARAM_TITLE);
          if (title != null) {
            System.err.println("Setting title to " + title);
            mRocPlot.setTitle(title);
          }

          final boolean scores = Boolean.parseBoolean(getParameter(PARAM_SCORES));
          System.err.println("Setting show scores to " + scores);
          mRocPlot.showScores(scores);

          final boolean sidepane = Boolean.parseBoolean(getParameter(PARAM_SIDEPANE));
          System.err.println("Setting sidepane visibility to " + sidepane);
          if (sidepane) {
            mRocPlot.setSplitPaneDividerLocation(1.0);
          }

          final String lineWidth = getParameter(PARAM_LINEWIDTH);
          if (lineWidth != null) {
            System.err.println("Setting line width to " + lineWidth);
            mRocPlot.setLineWidth(Integer.parseInt(lineWidth));
          }
}
      });

      final Thread t = new Thread() {
        @Override
        public void run() {
          // Get parameters and set them loading
          for (int streamNum = 1; getParameter(PARAM_DATA + streamNum) != null; streamNum++) {
            final String source = getParameter(PARAM_DATA + streamNum);
            String name = getParameter(PARAM_NAME + streamNum);
            if (name == null) {
              name = source;
            }
            try {
              System.err.println("Attempting to load data source: " + source);
              final URL sourceUrl = new URL(getCodeBase(), source);
              System.err.println("SourceUrl: " + sourceUrl);
              InputStream is = sourceUrl.openStream();
              if (FileUtils.isGzipFilename(source)) {
                is = new WorkingGzipInputStream(is);
              }
              ParseRocFile.loadStream(mRocPlot.getProgressBarDelegate(), new BufferedInputStream(is), name, true);
            } catch (final IOException e) {
              final String message = "Could not load data source: " + source + " " + e.getMessage();
              System.err.println(message);
              // TODO set status in UI
              mRocPlot.setStatus(message);
            }
          }
          // give gui a chance to catch up...
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ie) {
          }
          mRocPlot.updateProgress();
          mRocPlot.showCurrentGraph();
        }
      };
      t.start();
    } catch (Exception e) {
      System.err.println("createGUI didn't successfully complete: " + e.getMessage());
    }
  }


}
