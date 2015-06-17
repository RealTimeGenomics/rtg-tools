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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.imageio.ImageIO;

import com.rtg.util.StringUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class RocPlotPngTest extends TestCase {

  private static final String ROC = ""
          + "#total baseline variants: 3092754" + StringUtils.LS
          + "#score\ttrue_positives\tfalse_positives" + StringUtils.LS
          + "3.300\t0.000\t15" + StringUtils.LS
          + "2.261\t70000.000\t137" + StringUtils.LS
          + "1.226\t180000.000\t516" + StringUtils.LS
          + "0.700\t406000.000\t11337" + StringUtils.LS
          + "0.533\t1971000.000\t1446920" + StringUtils.LS
          + "0.333\t2071000.000\t1646920" + StringUtils.LS
          + "0.200\t2995295.000\t1864591" + StringUtils.LS;

  public void test() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File roc = FileUtils.stringToFile(ROC, new File(dir, "roc.tsv"));
      final File png = new File(dir, "PNG.png");
      RocPlotPng.rocPngImage(Collections.singletonList(roc), Collections.singletonList("LINE"), "a title", true, 3, png);
      final BufferedImage buf = ImageIO.read(png);
      assertEquals(800, buf.getWidth());
      assertEquals(600, buf.getHeight());
    }
  }
}
