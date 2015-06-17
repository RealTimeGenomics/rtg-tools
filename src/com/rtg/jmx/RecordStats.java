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
package com.rtg.jmx;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Output stats from management beans.
 */
public class RecordStats implements Runnable {

  private static final String LS = System.lineSeparator();

  private ArrayList<MonStats> mStats = new ArrayList<>();

  private Appendable mOut;
  private int mDelay;
  private static final int REPEAT_HEADER = 30;
  private boolean mRun = true;


  RecordStats(Appendable out, int delay) {
    if (delay < 100) {
      throw new IllegalArgumentException();
    }
    mOut = out;
    mDelay = delay;
  }

  void addStats(MonStats... stats) {
    Collections.addAll(mStats, stats);
  }

  void terminate() {
    mRun = false;
  }

  void addHeader() throws IOException {
    mOut.append("# Monitoring started.").append(LS);
    for (MonStats s : mStats) {
      s.addHeader(mOut);
    }
  }

  void addColumnLabels() throws IOException {
    mOut.append("#");
    for (MonStats s : mStats) {
      mOut.append(" ");
      s.addColumnLabelsTop(mOut);
    }
    mOut.append(LS);
    mOut.append("#");
    for (MonStats s : mStats) {
      mOut.append(" ");
      s.addColumnLabelsBottom(mOut);
    }
    mOut.append(LS);
    mOut.append("#=============================================================================================================================").append(LS);
  }

  void addColumnData() throws IOException {
    mOut.append(" ");
    for (MonStats s : mStats) {
      mOut.append(" ");
      s.addColumnData(mOut);
    }
    mOut.append(LS);
  }

  @Override
  public void run() {
    try {
      addHeader();
      int count = 0;
      while (mRun) {
        final long time = System.currentTimeMillis();
        if (count == 0) {
          addColumnLabels();
        }
        addColumnData();

        count++;
        if (count == REPEAT_HEADER) {
          count = 0;
        }

        try {
          if (mRun) {
            final long wait = Math.max(100, mDelay - (System.currentTimeMillis() - time));
            Thread.sleep(wait);
          }
        } catch (InterruptedException e) {
          mRun = false;
        }
      }
      mOut.append("# Monitoring finished.").append(LS);
    } catch (IOException e) {
      System.err.println("Monitoring disabled: " + e.getMessage());
    } finally {
      if (mOut instanceof Closeable) {
        try {
          ((Closeable) mOut).close();
        } catch (IOException e) {
          // Do nothing
        }
      }
    }
  }

}
