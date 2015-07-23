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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;

import com.rtg.launcher.GlobalFlags;
import com.rtg.util.Utils;

/**
 * Class converts <code>ROC</code> files and calculates slope
 * output is "posterior" "slope"
 */
public final class RocSlope {

  private static final boolean ALT_SLOPE = GlobalFlags.isSet(GlobalFlags.ALTERNATE_ROC_SLOPE_CALCULATION);

  /**
   * Calculate the slope along an roc curve
   * @param in the roc curve
   * @param out the destination for the slope graph
   * @throws IOException some of the time
   */
  public static void writeSlope(InputStream in, PrintStream out) throws IOException {
    new RocSlope(in, out);
  }
  /**
   * Calculate the slope along an roc curve
   * @param in the roc curve
   * @param out the destination for the slope graph
   * @throws IOException some of the time
   */
  private RocSlope(InputStream in, PrintStream out) throws IOException {
    out.println("#posterior\tslope\tlog-slope");
    try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
      String s;
      final ArrayList<Container> stack = new ArrayList<>();
      stack.add(new Container(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0, 0, 0));
      while ((s = r.readLine()) != null) {
        if (s.startsWith("#")) {
          continue;
        }
        final String[] split = s.split("\t");
        assert split.length >= 3;
        final double pos = Double.parseDouble(split[0]);
        final int y = (int) Double.parseDouble(split[1]);
        final int x = (int) Double.parseDouble(split[2]);
        //System.err.println("pos=" + pos + " x=" + x + " y=" + y);
        final int size = stack.size();
        if (size == 0) {
          stack.add(new Container(pos, pos, 0, 0, x, y));
        } else {
          final Container top = stack.get(size - 1);
          final double p0 = top.mLastPosterior;
          final double posterior = Double.isInfinite(p0) ? pos : top.mLastPosterior;
          stack.add(new Container(posterior, pos, top.endX(), top.endY(), x - top.endX(), y - top.endY()));
        }
        while (true) {
          if (stack.size() <= 2) {
            break;
          }
          final int size1 = stack.size() - 1;
          final Container top1 = stack.get(size1);
          final Container top2 = stack.get(size1 - 1);

          if (top1.slope() >= top2.slope()) {
            final Container c = new Container(top2.mFirstPosterior, top1.mLastPosterior, top2.mStartx, top2.mStarty, top1.mDeltax + top2.mDeltax, top1.mDeltay + top2.mDeltay);
            stack.remove(size1);
            stack.remove(size1 - 1);
            stack.add(c);
          } else {
            break;
          }
        }
      }
      for (int i = 1; i < stack.size(); i++) {
        final Container c1 = stack.get(i);
        final double slope = c1.slope();
        final double posterior = c1.mFirstPosterior;
        if (slope == 0.0 || Double.isInfinite(slope)) {
          continue;
        }
        final double logSlope = ALT_SLOPE ? (10 * Math.log10(1 + slope)) : Math.log10(slope);
        out.println(Utils.realFormat(posterior, 2) + "\t" + Utils.realFormat(slope, 2) + "\t" + Utils.realFormat(logSlope, 3));
        final double lastPosterior = c1.mLastPosterior;
        if (lastPosterior < posterior) {
          out.println(Utils.realFormat(lastPosterior, 2) + "\t" + Utils.realFormat(slope, 2) + "\t" + Utils.realFormat(logSlope, 3));
        }
      }
      //       out.println();
      //       for (int i = 1; i < stack.size(); i++) {
      //         final Container c1 = stack.get(i);
      //         out.println(Utils.realFormat(c1.mLastPosterior, 2) + "\t" + Utils.realFormat(c1.slope(), 2));
      //       }
    }
  }

  private static final class Container {
    private final double mFirstPosterior;
    private final double mLastPosterior;
    private final int mStartx;
    private final int mStarty;
    private final int mDeltax;
    private final int mDeltay;

    private Container(final double firstPos, final double lastPos, final int startx, final int starty, final int deltax, final int deltay) {
      //System.err.println("firstPos=" + firstPos + " lastPos=" + lastPos + " startx=" + startx + " starty=" + starty + " deltax=" + deltax + " deltay=" + deltay);
      mFirstPosterior = firstPos;
      mLastPosterior = lastPos;
      mStartx = startx;
      mStarty = starty;
      mDeltax = deltax;
      mDeltay = deltay;
    }

    private double slope() {
      if (mDeltay == 0) {
        return 0.0;
      }
      return (double) mDeltay / mDeltax;
    }

    private int endX() {
      return mStartx + mDeltax;
    }

    private int endY() {
      return mStarty + mDeltay;
    }
  }
}
