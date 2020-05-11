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

import static com.rtg.graph.RocPlotToFile.ImageFormat.SVG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.reeltwo.plot.Box2D;
import com.reeltwo.plot.renderer.GraphicsRenderer;
import com.reeltwo.plot.ui.ImageWriter;
import com.rtg.util.io.FileUtils;

/**
 */
public final class RocPlotToFile {

  /**
   * Supported image formats.
   */
  public enum ImageFormat {
    /** SVG file type Scalable vector graphics */
    SVG,
    /** Portable network graphics */
    PNG
  }

  private ImageFormat mImageFormat = ImageFormat.PNG;
  private boolean mShowScores = true;
  private int mLineWidth = 2;
  private boolean mPrecisionRecall = false;
  private Box2D mInitialZoom = null;
  private boolean mInterpolate = false;
  private String mPaletteName = RocPlotPalettes.SINGLETON.defaultName();

  RocPlotToFile() {
  }

  RocPlotToFile setShowScores(boolean show) {
    mShowScores = show;
    return this;
  }
  RocPlotToFile setLineWidth(int width) {
    mLineWidth = width;
    return this;
  }
  RocPlotToFile setInterpolate(boolean interpolate) {
    mInterpolate = interpolate;
    return this;
  }
  RocPlotToFile setPrecisionRecall(boolean precisionRecall) {
    mPrecisionRecall = precisionRecall;
    return this;
  }
  RocPlotToFile setImageFormat(ImageFormat format) {
    mImageFormat = format;
    return this;
  }
  RocPlotToFile setInitialZoom(Box2D initialZoom) {
    mInitialZoom = initialZoom;
    return this;
  }
  RocPlotToFile setPaletteName(String palette) {
    mPaletteName = palette;
    return this;
  }
  void writeRocPlot(File outFile, List<File> fileList, List<String> nameList, String title) throws IOException {
    final Map<String, DataBundle> data = new LinkedHashMap<>(fileList.size());
    for (int i = 0; i < fileList.size(); ++i) {
      final File f = fileList.get(i);
      final String name = nameList.get(i);
      final DataBundle db = ParseRocFile.loadStream(new ParseRocFile.NullProgressDelegate(), FileUtils.createInputStream(f, false), f.getAbsolutePath(), mInterpolate);
      db.setTitle(f, name);
      data.put(db.getTitle(), db);
    }
    final GraphicsRenderer gr = new GraphicsRenderer();
    gr.setColors(RocPlotPalettes.SINGLETON.getPalette(mPaletteName));
    gr.setTextAntialiasing(true);
    final ImageWriter iw = new ImageWriter(gr);

    final ArrayList<String> paths = new ArrayList<>(data.keySet());

    final RocPlot.ExternalZoomGraph2D graph;
    if (mPrecisionRecall) {
      graph = new RocPlot.PrecisionRecallGraph2D(paths, mLineWidth, mShowScores, data, title != null ? title : "Precision/Recall");
    } else {
      graph = new RocPlot.RocGraph2D(paths, mLineWidth, mShowScores, data, title != null ? title : "ROC");
    }
    if (mInitialZoom != null) {
      graph.setZoom(mInitialZoom);
    }
    if (mImageFormat == SVG) {
      try (final FileOutputStream os = new FileOutputStream(outFile)) {
        iw.toSVG(os, graph, 800, 600, null);
      }
    } else {
      iw.toPNG(outFile, graph, 800, 600, null);
    }
  }
}
