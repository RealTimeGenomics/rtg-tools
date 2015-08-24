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

import java.util.ArrayList;

import com.reeltwo.plot.Datum2D;
import com.reeltwo.plot.Graph2D;
import com.reeltwo.plot.Point2D;
import com.reeltwo.plot.PointPlot2D;
import com.reeltwo.plot.TextPoint2D;

import junit.framework.TestCase;

/**
 */
public class DataBundleTest extends TestCase {
  public void test() {
    final Point2D[] points = {new Point2D(4.0f, 5.0f), new Point2D(100.0f, 200.0f)};
    final String[] scores = {"5.0", "9.0"};
    final DataBundle db = new DataBundle("Monkey", points, scores, 300);
    assertEquals(300, db.getTotalVariants());
    assertEquals(100.0, db.getPlot(1, 1).getHi(Graph2D.X), 1e-9);
    assertEquals(200.0, db.getPlot(1, 1).getHi(Graph2D.Y), 1e-9);
    assertEquals(4.0, db.getPlot(1, 1).getLo(Graph2D.X), 1e-9);
    assertEquals(5.0, db.getPlot(1, 1).getLo(Graph2D.Y), 1e-9);
    assertEquals("Monkey", db.getTitle());
    assertEquals(2, db.getPlot(1, 1).getData().length);
  }

  public void testScoreLabels() {
    final ArrayList<Point2D> points = new ArrayList<>();
    final ArrayList<String> scores = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      points.add(new Point2D(i, i));
      scores.add(String.format("%.3g", (float) (100 - i)));
    }
    final String[] labels = scores.toArray(new String[scores.size()]);
    labels[labels.length - 1] = "None";
    final DataBundle db = new DataBundle("Monkey", points.toArray(new Point2D[points.size()]), labels, 300);
    db.setScoreRange(0.0f, 1.0f);
    final PointPlot2D scorePoints = db.getScorePoints(1, 1);
    final String[] exp = {"100", "90.0", "81.0", "71.0", "62.0", "52.0", "43.0", "33.0", "24.0", "14.0", "5.00", "None"};
    final Datum2D[] data = scorePoints.getData();
    for (int i = 0; i < data.length; i++) {
      final Datum2D d = data[i];
      assertTrue(d instanceof TextPoint2D);
      final TextPoint2D p = (TextPoint2D) d;
      assertEquals(exp[i], p.getText());
    }
  }
}
