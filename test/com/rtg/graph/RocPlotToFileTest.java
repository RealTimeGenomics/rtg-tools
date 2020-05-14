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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.imageio.ImageIO;

import com.reeltwo.plot.Box2D;
import com.rtg.AbstractTest;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class RocPlotToFileTest extends AbstractTest {

  public void testPng() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File roc = new File(dir, "roc.tsv");
      FileUtils.copyResource("com/rtg/graph/resources/roc.tsv", roc);
      final File png = new File(dir, "PNG.png");
      final RocPlotToFile r = new RocPlotToFile();
      r.setTitle("a title");
      r.writeRocPlot(png, Collections.singletonList(roc), Collections.singletonList("LINE"));
      final BufferedImage buf = ImageIO.read(png);
      assertEquals(800, buf.getWidth());
      assertEquals(600, buf.getHeight());
    }
  }

  public void testPng2() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File roc = new File(dir, "roc.tsv");
      FileUtils.copyResource("com/rtg/graph/resources/roc2.tsv", roc);
      final File png = new File(dir, "PNG.png");
      final RocPlotToFile r = new RocPlotToFile();
      r.setTitle("a title");
      r.setInitialZoom(new Box2D(10, 10, 100, 100));
      r.writeRocPlot(png, Collections.singletonList(roc), Collections.singletonList("LINE"));
      final BufferedImage buf = ImageIO.read(png);
      assertEquals(800, buf.getWidth());
      assertEquals(600, buf.getHeight());
    }
  }

  private void checkSvg(final File svg, final int lines, final String... expected) throws IOException {
    final String s = FileUtils.fileToString(svg);
    TestUtils.containsAll(s, expected);
    assertEquals(lines, TestUtils.splitLines(s).length);
  }

  public void testSvg() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File roc = new File(dir, "roc.tsv");
      FileUtils.copyResource("com/rtg/graph/resources/roc.tsv", roc);
      final File svg = new File(dir, "example.svg");
      final RocPlotToFile r = new RocPlotToFile();
      r.setTitle("a title");
      r.setImageFormat(SVG);
      r.writeRocPlot(svg, Collections.singletonList(roc), Collections.singletonList("LINE"));
      checkSvg(svg, 95,
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">",
        "<rect height=\"600\" style=\"fill:rgb(255,255,255);stroke:none;\" width=\"800\" x=\"0\" y=\"0\"/>",
        "True Positives",
        "False Positives",
        ">%<",
        "a title (baseline total = 3092754)",
        "</svg>"
      );
    }
  }

  public void testNoTotal() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File roc = new File(dir, "roc.tsv");
      FileUtils.copyResource("com/rtg/graph/resources/roc-nototal.tsv", roc);
      final File svg = new File(dir, "example.svg");
      final RocPlotToFile r = new RocPlotToFile();
      r.setTitle("a title");
      r.setImageFormat(SVG);
      r.writeRocPlot(svg, Collections.singletonList(roc), Collections.singletonList("LINE"));
      checkSvg(svg, 88,
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">",
        "<rect height=\"600\" style=\"fill:rgb(255,255,255);stroke:none;\" width=\"800\" x=\"0\" y=\"0\"/>",
        "True Positives",
        "False Positives",
        "a title<",
        "</svg>"
      );
    }
  }
}
