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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 */
public class RocUtilsTest extends TestCase {

  private static final double DELTA = 0.0001;

  public void testInterpolate()  {
    final double tpStep = 3;
    final List<RocPoint<Integer>> points = new ArrayList<>();
    final List<Double> exp = new ArrayList<>();
    points.add(new RocPoint<>(0, 0.0, 0.0, 0.0)); // Will add points at TP: 3
    exp.add(0.0); exp.add(3.0);
    points.add(new RocPoint<>(10, 5.0, 2.0, 0.0)); // Will add points at TP: 8, 11, 14, 17
    exp.add(5.0); exp.add(8.0); exp.add(11.0); exp.add(14.0); exp.add(17.0);
    points.add(new RocPoint<>(20, 20.0, 20.0, 0.0)); // Will add points at TP: 23, 26, 29
    exp.add(20.0); exp.add(23.0); exp.add(26.0); exp.add(29.0);
    points.add(new RocPoint<>(30, 30.0, 50.0, 0.0));
    exp.add(30.0);

    final List<RocPoint<Integer>> interpolated = RocUtils.interpolate(points, tpStep);
    check(interpolated, tpStep);

    assertEquals(0, interpolated.get(0).getTruePositives(), DELTA);
    assertEquals(0, interpolated.get(0).getFalsePositives(), DELTA);
    assertEquals(30, interpolated.get(interpolated.size() - 1).getTruePositives(), DELTA);
    assertEquals(50, interpolated.get(interpolated.size() - 1).getFalsePositives(), DELTA);
    assertEquals(exp.size(), interpolated.size());
    for (int i = 0; i < exp.size(); i++) {
      final RocPoint<Integer> point = interpolated.get(i);
      assertEquals(exp.get(i), point.getTruePositives(), DELTA);
    }
  }

  // Check TP step size increment
  // Check TP non-decreasing
  // Check FP non-decreasing
  private <T> void check(List<RocPoint<T>> points, double tpStep) {
    double lastFp = 0;
    double lastTp = 0;
    for (final RocPoint<T> point : points) {
      assertEquals(0, point.getRawTruePositives(), DELTA);
      assertTrue("" + lastTp + "-" + point.getTruePositives(), point.getTruePositives() - lastTp <= tpStep + DELTA);
      assertTrue(point.getFalsePositives() >= lastFp);
      lastFp = point.getFalsePositives();
      lastTp = point.getTruePositives();
    }

  }
}
