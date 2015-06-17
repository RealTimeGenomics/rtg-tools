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

package com.rtg.reader;

import static com.rtg.util.StringUtils.TAB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.rtg.mode.DNA;
import com.rtg.mode.DnaUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 */
public class MappedSamBamSequenceDataSourceTest extends SamBamSequenceDataSourceTest {

  protected SamBamSequenceDataSource getSourceFromFiles(List<File> files, boolean paired) {
    return MappedSamBamSequenceDataSource.fromInputFiles(files, paired, false, null);
  }

  @Override
  public void testErrors() throws IOException {
    try (TestDirectory td = new TestDirectory()) {
      File f = new File(td, "blah.sam");
      Diagnostic.setLogStream();
      // No Qualities
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_SINGLE, SAM_NL, "n", "ACGT", "*"), f);
      checkNoTalkbackError(f, false, "SAM or BAM input must have qualities.");
      // Paired flag in single end
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_LEFT, SAM_NL, "n", "ACGT", "!!!!"), f);
      checkNoTalkbackError(f, false, "SAM flags for read n indicate it is paired end.");
      // Uneven paired records
      //checkNoTalkbackError(SAM_HEADER + String.format(SAM_LINE_LEFT, SAM_NL, "n", "ACGT", "!!!!"), true, "1 reads missing a pair when processing paired end SAM input.");
      // Unpaired record in paired input
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_SINGLE, SAM_NL, "n", "ACGT", "!!!!"), f);
      checkNoTalkbackError(f, true, "SAM flags for read n indicate it is single end.");
      // Two left reads
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_LEFT, SAM_NL, "n", "ACGT", "!!!!") + String.format(SAM_LINE_LEFT, SAM_NL, "n", "ACGT", "!!!!"), f);
      checkNoTalkbackError(f, true, "Conflicting paired end flags detected in SAM input at read n.");
      // Two right reads
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_RIGHT, SAM_NL, "n", "ACGT", "!!!!") + String.format(SAM_LINE_RIGHT, SAM_NL, "n", "ACGT", "!!!!"), f);
      checkNoTalkbackError(f, true, "Conflicting paired end flags detected in SAM input at read n.");
    }
  }

  /**
   *  Template SAM line for a SAM / BAM sequences data source
   *  Use <code>String.format(SAM_LINE,"name", "flags", "Cigar", "Bases","Qualities");</code> to get a line.
   */
  private static final String SAM_LINE_SINGLE_REVERSE = String.format(SAM_LINE, "%1$s", "%2$s", "16", "test1", "1", "%3$s", "%4$s", "%5$s");

  private static final String SAM_SEQ_DICT_HEADER = "@SQ" + TAB + "SN:test1" + TAB + "LN:100" + SAM_NL;

  public void testReverseStrand() throws IOException {
    Diagnostic.setLogStream();
    final File tmpDir = FileUtils.createTempDir("test", "mappedsambamdatasource");
    try {
      final File input = FileUtils.stringToFile(SAM_HEADER + SAM_SEQ_DICT_HEADER + String.format(SAM_LINE_SINGLE_REVERSE, SAM_NL, READS[4][0], READS[4][1].length() + "=", READS[4][1], READS[4][2]), new File(tmpDir, "input.sam"));
      final List<File> inputs = new ArrayList<>();
      inputs.add(input);
      final SamBamSequenceDataSource source = getSourceFromFiles(inputs, false);
      assertTrue(source.nextSequence());
      assertEquals(READS[4][1].length(), source.currentLength());
      assertEquals(READS[4][0], source.name());
      for (int j = 0; j < READS[4][1].length(); j++) {
          assertEquals(DNA.valueOf(READS[4][1].charAt(READS[4][1].length() - 1 - j)).complement().ordinal(), source.sequenceData()[j]);
      }
      for (int j = 0; j < READS[4][2].length(); j++) {
        assertEquals(READS[4][2].charAt(READS[4][2].length() - 1 - j) - 33, source.qualityData()[j]);
      }
      assertFalse(source.nextSequence());
      assertFalse(source.nextSequence());
    } finally {
      assertTrue(FileHelper.deleteAll(tmpDir));
    }
  }

  public void testNonPrimaryAlignment() throws IOException {
    final StringBuilder sb = new StringBuilder();
    sb.append(SAM_HEADER);

    for (int i = 0; i < READS.length; i++) {
      if (i % 2 == 0) {
        sb.append(String.format(SAM_LINE_LEFT, SAM_NL, READS[i][0], READS[i][1], READS[i][2]));
        sb.append(String.format(SAM_LINE_RIGHT, SAM_NL, READS[i][0], DnaUtils.reverseComplement(READS[i][1]), READS[i][2]));
      } else {
        sb.append(String.format(SAM_LINE_RIGHT, SAM_NL, READS[i][0], DnaUtils.reverseComplement(READS[i][1]), READS[i][2]));
        sb.append(String.format(SAM_LINE_LEFT, SAM_NL, READS[i][0], READS[i][1], READS[i][2]));
      }
      // this read should be filtered out with a MappedSamBamSequenceDataSource
      if (i == 2) {
        sb.append(String.format(SAM_LINE_LEFT_NOT_PRIMARY, SAM_NL, "notprimary", READS[3][1], READS[3][2]));
      }
    }

    try (TestDirectory td = new TestDirectory()) {
      final File nonef = new File(td, "none.sam");
      final File somef = new File(td, "some.sam");
      assertTrue(sb.toString().contains("notprimary"));
      FileUtils.stringToFile(sb.toString(), somef);
      FileUtils.stringToFile(SAM_HEADER, nonef);
      final List<File> inputs = new ArrayList<>();
      inputs.add(nonef);
      inputs.add(somef);
      final SamBamSequenceDataSource source = getSourceFromFiles(inputs, true);

      checkBasicPaired(source);
    }
  }

}
