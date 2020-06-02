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

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.reeltwo.plot.Box2D;
import com.rtg.util.StringUtils;

/**
 * Stores common settings for ROC plot drawing style
 */
@JumbleIgnore
class RocPlotSettings {

  private static final int LINE_WIDTH_DEFAULT = 2;

  protected String mTitle = null;
  protected boolean mShowScores = true;
  protected boolean mPlain = false;
  protected int mLineWidth = LINE_WIDTH_DEFAULT;
  protected boolean mPrecisionRecall = false;
  protected Box2D mInitialZoom = null;
  protected boolean mInterpolate = false;
  protected boolean mWeighted = true;
  protected String mPaletteName = RocPlotPalettes.SINGLETON.defaultName();

  RocPlotSettings setTitle(String title) {
    mTitle = title;
    return this;
  }
  RocPlotSettings setShowScores(boolean show) {
    mShowScores = show;
    return this;
  }
  RocPlotSettings setPlain(boolean plain) {
    mPlain = plain;
    return this;
  }
  RocPlotSettings setLineWidth(int width) {
    mLineWidth = width;
    return this;
  }
  RocPlotSettings setInterpolate(boolean interpolate) {
    mInterpolate = interpolate;
    return this;
  }
  RocPlotSettings setWeighted(boolean weighted) {
    mWeighted = weighted;
    return this;
  }
  RocPlotSettings setPrecisionRecall(boolean precisionRecall) {
    mPrecisionRecall = precisionRecall;
    return this;
  }
  RocPlotSettings setInitialZoom(Box2D initialZoom) {
    mInitialZoom = initialZoom;
    return this;
  }
  RocPlotSettings setPaletteName(String palette) {
    mPaletteName = palette;
    return this;
  }

  String getTitle() {
    return mTitle != null ? mTitle : mPrecisionRecall ? "Precision/Sensitivity" : "ROC";
  }

  private String getZoomString(Box2D zoom) {
    final int xlo = Math.round(zoom.getXLo());
    final int ylo = Math.round(zoom.getYLo());
    final int xhi = Math.round(zoom.getXHi());
    final int yhi = Math.round(zoom.getYHi());
    if (xlo == 0 && ylo == 0) {
      return String.format("%d,%d", xhi, yhi);
    } else {
      return String.format("%d,%d,%d,%d", xlo, ylo, xhi, yhi);
    }
  }

  String getSettingsAsFlags() {
    final StringBuilder sb = new StringBuilder("rtg rocplot");
    if (mTitle != null) {
      sb.append(" --").append(RocPlotCli.TITLE_FLAG).append(' ').append(StringUtils.smartQuote(mTitle));
    }
    if (mLineWidth != LINE_WIDTH_DEFAULT) {
      sb.append(" --").append(RocPlotCli.LINE_WIDTH_FLAG).append(' ').append(mLineWidth);
    }
    if (mShowScores) {
      sb.append(" --").append(RocPlotCli.SCORES_FLAG);
    }
    if (mPlain) {
      sb.append(" --").append(RocPlotCli.PLAIN_FLAG);
    }
    if (mPrecisionRecall) {
      sb.append(" --").append(RocPlotCli.PRECISION_SENSITIVITY_FLAG);
    }
    if (mInitialZoom != null) {
      sb.append(" --").append(RocPlotCli.ZOOM_FLAG).append(' ').append(getZoomString(mInitialZoom));
    }
    if (!mPaletteName.equals(RocPlotPalettes.SINGLETON.defaultName())) {
      sb.append(" --").append(RocPlotCli.PALETTE).append(' ').append(StringUtils.smartQuote(mPaletteName));
    }
    return sb.toString();
  }
}
