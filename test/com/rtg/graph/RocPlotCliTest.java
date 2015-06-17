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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class RocPlotCliTest extends AbstractCliTest {
  @Override
  protected AbstractCli getCli() {
    return new RocPlotCli();
  }

  public void test() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      checkHandleFlagsErr();
      final File f = new File(dir, "moo");
      checkHandleFlagsErr(f.getPath());
      assertTrue(f.createNewFile());
      checkHandleFlagsOut(f.getPath());
      checkHandleFlagsOut("--curve", f.getPath() + "=Monkey");
      final File png = new File(dir, "png.png");
      checkHandleFlagsOut(f.getPath(), "--png", png.getPath());
      assertTrue(png.createNewFile());
      checkHandleFlagsErr(f.getPath(), "--png", png.getPath());

      final String html = checkMainInitOk("--curve", f.getPath() + "=Monkey", "--Xhtml");
      TestUtils.containsAll(html,
              String.format("<param name=\"data1\" value=\"%s\">", f.getPath()),
              String.format("<param name=\"name1\" value=\"%s\">", "Monkey")
      );
      final File f2 = new File(dir, "oink");
      assertTrue(f2.createNewFile());
      checkHandleFlagsOut("--curve", f.getPath() + "=Monkey", f2.getPath());

      final String html2 = checkMainInitOk("--curve", f.getPath() + "=Monkey", f2.getPath(), "--Xapplet");
      TestUtils.containsAll(html2,
              String.format("<param name=\"data1\" value=\"%s\">", f.getPath()),
              String.format("<param name=\"name1\" value=\"%s\">", "Monkey"),
              String.format("<param name=\"data2\" value=\"%s\">", f2.getPath()),
              String.format("<param name=\"name2\" value=\"%s\">", f2.getParentFile().getName())
      );
    }
  }
}
