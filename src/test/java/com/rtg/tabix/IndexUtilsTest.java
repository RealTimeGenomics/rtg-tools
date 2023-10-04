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
package com.rtg.tabix;

import static com.rtg.util.StringUtils.TAB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.rtg.launcher.MainResult;
import com.rtg.sam.SharedSamConstants;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class IndexUtilsTest extends TestCase {

  private static final String SAM_HEADER_CLIP = ""
    + "@HD" + TAB + SharedSamConstants.OUT_SAM_VERSION + TAB + "SO:coordinate\n"
    + "@SQ" + TAB + "SN:t" + TAB + "LN:84\n"
    ;
  private static final String SAM_CLIP = SAM_HEADER_CLIP
    + "0" + TAB + "0" + TAB + "t" + TAB + "1" + TAB + "255" + TAB + "10S43M2S" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAATCGCTAGGTTCGACTTGGTTAACAACAACGCCTGGGGCTTTTTGG" + TAB + "*\n"
    + "1" + TAB + "0" + TAB + "t" + TAB + "42" + TAB + "255" + TAB + "10S40M2S" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAATTATTCTGGAAAGCAATGCCAGGCAGGGGCAGGTGGCCACGG" + TAB + "*\n";

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  public void testCompress() throws Exception {
    try (final TestDirectory tmpDir = new TestDirectory()) {
      final ArrayList<File> files = new ArrayList<>();
      files.add(FileUtils.stringToFile(SAM_CLIP, new File(tmpDir, "sam.sam")));
      final List<File> bzFiles = IndexUtils.ensureBlockCompressed(files);
      final File samFile = bzFiles.get(0);
      new TabixIndexer(samFile).saveSamIndex();
      assertTrue(samFile.exists());
      assertTrue(new File(samFile.toString() + ".tbi").exists());
      final MainResult res = MainResult.run(new ExtractCli(), samFile.getPath(), "--header");
      assertEquals(res.err(), 0, res.rc());
      assertEquals(SAM_CLIP, res.out());
    }
  }
}
