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

package com.rtg.bed;

import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.StringUtils.TAB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class BedWriterTest extends TestCase {

  private static final String BED_CONTENTS = ""
      + "# COMMENT" + LS
      + "track adsfasdf" + LS
      + "browser adsfasdf" + LS
      + "chr1" + TAB + "2" + TAB + "80" + TAB + "annotation" + LS
      + "chr2" + TAB + "3" + TAB + "40" + TAB + "annotation1" + TAB + "annotation2" + LS
      + "chr3" + TAB + "7" + TAB + "90" + TAB + "annotation" + TAB + "annotation3" + LS;

  public void testBedWriter() throws IOException {
    try (TestDirectory testDir = new TestDirectory()) {
      final File bedFile = new File(testDir, "test.bed");
      try (BedWriter writer = new BedWriter(new FileOutputStream(bedFile))) {
        writer.writeComment(" COMMENT");
        writer.writeln("track adsfasdf");
        writer.writeln("browser adsfasdf");
        writer.write(new BedRecord("chr1", 2, 80, "annotation"));
        writer.write(new BedRecord("chr2", 3, 40, "annotation1", "annotation2"));
        writer.write(new BedRecord("chr3", 7, 90, "annotation", "annotation3"));
      }
      final String written = FileUtils.fileToString(bedFile);
      assertEquals(BED_CONTENTS, written);
    }
  }
}
