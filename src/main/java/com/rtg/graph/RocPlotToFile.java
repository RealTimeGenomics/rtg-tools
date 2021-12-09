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

import static com.rtg.graph.RocPlotCli.PNG_FLAG;
import static com.rtg.graph.RocPlotCli.SVG_FLAG;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.reeltwo.plot.renderer.GraphicsRenderer;
import com.reeltwo.plot.ui.ImageWriter;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;

/**
 */
public final class RocPlotToFile extends RocPlotSettings {

  // Metadata key used to store plot command settings
  static final String ROCPLOT_META_KEY = "rocplot_cmd";

  private ImageWriter.ImageFormat mImageFormat = ImageWriter.ImageFormat.PNG;

  RocPlotToFile() {
  }

  RocPlotToFile setImageFormat(ImageWriter.ImageFormat format) {
    mImageFormat = format;
    return this;
  }

  String getOutputImageAsFlags(File outFile) {
    final StringBuilder sb = new StringBuilder(" --");
    switch (mImageFormat) {
      case PNG:
        sb.append(PNG_FLAG);
        break;
      case SVG:
        sb.append(SVG_FLAG);
        break;
      default:
        return "";
    }
    sb.append(" ").append(StringUtils.smartQuote(outFile.toString()));
    return sb.toString();
  }

  void writeRocPlot(File outFile, List<File> fileList, List<String> nameList) throws IOException {
    final Map<String, DataBundle> data = new LinkedHashMap<>(fileList.size());
    final StringBuilder curves = new StringBuilder();
    for (int i = 0; i < fileList.size(); ++i) {
      final File f = fileList.get(i);
      final String name = nameList.get(i);
      final DataBundle db = ParseRocFile.loadStream(new ParseRocFile.NullProgressDelegate(), FileUtils.createInputStream(f, false), f.getAbsolutePath(), mInterpolate);
      db.setWeighted(mWeighted);
      db.setTitle(f, name);
      data.put(db.getTitle(), db);
      curves.append(" --").append(RocPlotCli.CURVE_FLAG).append(" ").append(StringUtils.dumbQuote(f.getPath() + "=" + db.getTitle()));
    }
    final GraphicsRenderer gr = new GraphicsRenderer();
    gr.setColors(RocPlotPalettes.SINGLETON.getPalette(mPaletteName));
    gr.setTextAntialiasing(true);
    if (!mPlain) {
      gr.setGraphBGColor(new Color(0.8f, 0.9f, 1.0f), Color.WHITE);
      gr.setGraphShadowWidth(4);
    }
    final ImageWriter iw = new ImageWriter(gr);
    final String m = ImageWriter.isImageWritingEnabled();
    if (m != null) {
      throw new NoTalkbackSlimException("Host OS is not correctly configured for image output: " + m);
    }
    final ArrayList<String> ordering = new ArrayList<>(data.keySet());
    final ArrayList<Pair<DataBundle, Integer>> toShow = new ArrayList<>();
    for (int i = 0; i < ordering.size(); ++i) {
      final DataBundle db = data.get(ordering.get(i));
      if (db.show()) {
        toShow.add(new Pair<>(db, i));
      }
    }
    final RocPlot.ExternalZoomGraph2D graph;
    if (mPrecisionRecall) {
      graph = new RocPlot.PrecisionRecallGraph2D(toShow, mLineWidth, mShowScores, getTitle(), mShowPoints);
    } else {
      graph = new RocPlot.RocGraph2D(toShow, mLineWidth, mShowScores, getTitle(), mShowPoints);
    }
    if (mInitialZoom != null) {
      graph.setZoom(mInitialZoom);
    }

    final StringBuilder sb = new StringBuilder(getSettingsAsFlags())
      .append(getOutputImageAsFlags(outFile))
      .append(curves);
    iw.setMetaData(ROCPLOT_META_KEY, sb.toString());
    iw.toImage(mImageFormat, outFile, graph, 800, 600, null);
  }
}
