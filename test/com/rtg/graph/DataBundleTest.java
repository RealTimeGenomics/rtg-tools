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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.reeltwo.plot.Datum2D;
import com.reeltwo.plot.Graph2D;
import com.reeltwo.plot.PointPlot2D;
import com.reeltwo.plot.TextPoint2D;
import com.rtg.vcf.eval.RocPoint;

import junit.framework.TestCase;

/**
 */
public class DataBundleTest extends TestCase {
  public void test() throws IOException {
    final RocPoint[] points = {new RocPoint(0, 5.0f, 4.0f, 0), new RocPoint(0, 200.0f, 100.0f, 0)};
    final String[] scores = {"5.0", "9.0"};
    final DataBundle db = new DataBundle("Monkey", points, scores, 300);
    db.setScoreName("age");
    assertEquals(300, db.getTotalVariants());
    assertEquals(100.0, db.getPlot(1, 1).getHi(Graph2D.X), 1e-9);
    assertEquals(200.0, db.getPlot(1, 1).getHi(Graph2D.Y), 1e-9);
    assertEquals(4.0, db.getPlot(1, 1).getLo(Graph2D.X), 1e-9);
    assertEquals(5.0, db.getPlot(1, 1).getLo(Graph2D.Y), 1e-9);
    assertEquals("Monkey", db.getTitle());
    db.setTitle(new File("vcfeval-monkey", "weighted_roc.tsv"), "");
    assertEquals("monkey age", db.getTitle());
    db.setTitle(new File("vcfeval_monkey.vcfeval", "weighted_roc.tsv"), "");
    assertEquals("monkey age", db.getTitle());
    db.setTitle(new File("eval.monkey-eval", "weighted_roc.tsv"), "");
    assertEquals("monkey age", db.getTitle());
    db.setTitle(new File("monkey_vcfeval.repeat", "snp_roc.tsv"), "");
    assertEquals("monkey_vcfeval.repeat snp age", db.getTitle());
    assertEquals(2, db.getPlot(1, 1).getData().length);
  }

  public void testScoreLabels() {
    final ArrayList<RocPoint> points = new ArrayList<>();
    final ArrayList<String> scores = new ArrayList<>();
    for (int i = 0; i < 100; ++i) {
      points.add(new RocPoint(0, i, i, 0));
      scores.add(String.format("%.3g", (float) (100 - i)));
    }
    final String[] labels = scores.toArray(new String[scores.size()]);
    labels[labels.length - 1] = "None";
    final DataBundle db = new DataBundle("Monkey", points.toArray(new RocPoint[points.size()]), labels, 300);
    db.setScoreRange(0.0f, 1.0f);
    final PointPlot2D scorePoints = db.getScorePoints(1, 1);
    final String[] exp = {"100", "90.0", "80.0", "70.0", "60.0", "50.0", "40.0", "30.0", "20.0", "10.0", "None"};
    final Datum2D[] data = scorePoints.getData();
    for (int i = 0; i < data.length; ++i) {
      final Datum2D d = data[i];
      assertTrue(d instanceof TextPoint2D);
      final TextPoint2D p = (TextPoint2D) d;
      assertEquals(exp[i], p.getText());
    }
  }
}
