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

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.reeltwo.jumble.annotations.JumbleIgnore;

/**
 * Implementation that sends progress events to a progress bar
 */
@JumbleIgnore
public class ProgressBarDelegate implements ProgressDelegate {
  private final JProgressBar mProgressBar;

  private int mTotalLines;
  private int mTotalFiles;

  /**
   * @param prog progress bar to display progress on
   */
  public ProgressBarDelegate(JProgressBar prog) {
    mProgressBar = prog;
    mProgressBar.setString("Loading...");
  }

  @Override
  public void setProgress(final int progress) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (progress < 0) {
          mProgressBar.setIndeterminate(false);
        } else {
          mProgressBar.setIndeterminate(true);
        }
        mProgressBar.setString("" + progress);
      }
    });
  }

  @Override
  public void addFile(int numberLines) {
    ++mTotalFiles;
    mTotalLines += numberLines;
  }

  @Override
  public void done() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mProgressBar.setIndeterminate(false);
        mProgressBar.setString("Loaded " + mTotalLines + " points from " + mTotalFiles + " files.");
      }
    });
  }
}
