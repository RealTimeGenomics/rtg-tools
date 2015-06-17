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
import com.rtg.mode.SequenceType;
import com.rtg.sam.SamFilter;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class SamBamSequenceDataSourceTest extends TestCase {

  /** SAM new line character */
  public static final String SAM_NL = "\n";  // SAM files always use \n.

  /** SAM header for a SAM / BAM sequences data source */
  public static final String SAM_HEADER = ""
      + "@HD" + TAB + "VN:1.0" + TAB + "SO:queryname" + SAM_NL
      + "@RG" + TAB + "ID:A" + TAB + "SM:SAMPLE" + TAB + "PL:ILLUMINA" + SAM_NL
      ;
  /** Additional sam read group header */
  public static final String SECOND_READ_GROUP = ""
      + "@RG" + TAB + "ID:B" + TAB + "SM:SAMPLE" + TAB + "PL:ILLUMINA" + SAM_NL
      ;

  static final String[][] READS = {new String[]{"name0", "ATCGACG", "```````"},
                                                          new String[]{"name1", "ATCGACG", "```````"},
                                                          new String[]{"name2", "GACGCTC", "```````"},
                                                          new String[]{"name3", "GACGCTC", "```````"},
                                                          new String[]{"name4", "TTCAGCTA", "`IJ`````"},
                                                          new String[]{"name5", "TTCNGCTA", "````````"}};

  /**
   *  Template SAM line for a SAM / BAM sequences data source
   *  Use <code>String.format(SAM_LINE,"name", "flags", "RName", "Pos", "Cigar", "Bases","Qualities");</code> to get a line.
   */
  public static final String SAM_LINE = "%2$s" + TAB + "%3$s" + TAB + "%4$s" + TAB + "%5$s" + TAB + "0" + TAB + "%6$s" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "%7$s" + TAB + "%8$s" + TAB + "RG:Z:A" + "%1$s";

  /**
   *  Template SAM line for a SAM / BAM sequences data source
   *  Use <code>String.format(SAM_LINE_SINGLE,"name","Bases","Qualities");</code> to get a line.
   */
  public static final String SAM_LINE_SINGLE = String.format(SAM_LINE, "%1$s", "%2$s", "4", "*", "0", "*", "%3$s", "%4$s");

  /**
   *  Template left SAM line for a SAM / BAM sequences data source
   *  Use <code>String.format(SAM_LINE_LEFT,"name","Bases","Qualities");</code> to get a line.
   */
  public static final String SAM_LINE_LEFT = String.format(SAM_LINE, "%1$s", "%2$s", "77", "*", "0", "*", "%3$s", "%4$s");

  /**
   * Template left SAM line for a SAM / BAM sequences data source for a NON-PRIMARY-ALIGNMENT
   */
  public static final String SAM_LINE_LEFT_NOT_PRIMARY = String.format(SAM_LINE, "%1$s", "%2$s", "321", "*", "0", "*", "%3$s", "%4$s");

  /**
   *  Template right SAM line for a SAM / BAM sequences data source
   *  Use <code>String.format(SAM_LINE_RIGHT,"name","Bases","Qualities");</code> to get a line.
   */
  public static final String SAM_LINE_RIGHT = String.format(SAM_LINE, "%1$s", "%2$s", "141", "*", "0", "*", "%3$s", "%4$s");

  protected static final String BAD_HEADER = ""
      + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + SAM_NL
      + "@RG" + TAB + "ID:A" + TAB + "SM:SAMPLE" + TAB + "PL:ILLUMINA" + SAM_NL
      ;

  protected SamBamSequenceDataSource getSourceFromFiles(List<File> files, boolean paired, SamFilter filter) {
    return SamBamSequenceDataSource.fromInputFiles(files, paired, false, filter);
  }

  protected void checkNoTalkbackError(File inputFile, boolean paired, String expectedError) throws IOException {
    final List<File> inputs = new ArrayList<>();
    inputs.add(inputFile);
    final SamBamSequenceDataSource source = getSourceFromFiles(inputs, paired, null);
    try {
      if (source.nextSequence()) {
        source.qualityData();
      }
      fail();
    } catch (NoTalkbackSlimException e) {
      assertEquals(expectedError, e.getMessage());
    }
  }

  public void testErrors() throws IOException {
    Diagnostic.setLogStream();
    try (TestDirectory td = new TestDirectory()) {
      final File f = new File(td, "blah.sam");
      // Bad Header
      FileUtils.stringToFile(BAD_HEADER, f);
      checkNoTalkbackError(f, false, "SAM or BAM input must be sorted by queryname.");
      // No Qualities
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_SINGLE, SAM_NL, "n", "ACGT", "*"), f);
      checkNoTalkbackError(f, false, "SAM or BAM input must have qualities.");
      // Paired flag in single end
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_LEFT, SAM_NL, "n", "ACGT", "!!!!"), f);
      checkNoTalkbackError(f, false, "SAM flags for read n indicate it is paired end.");
      // Uneven paired records
      FileUtils.stringToFile(SAM_HEADER + String.format(SAM_LINE_LEFT, SAM_NL, "n", "ACGT", "!!!!"), f);
      checkNoTalkbackError(f, true, "Unbalanced read arms detected when processing paired end SAM input.");
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

  public void testFileInput() throws IOException {
    Diagnostic.setLogStream();
    final File tmpDir = FileUtils.createTempDir("test", "sambamdatasource");
    try {
      final StringBuilder sb = new StringBuilder();
      sb.append(SAM_HEADER);
      for (final String[] read : READS) {
        sb.append(String.format(SAM_LINE_SINGLE, SAM_NL, read[0], read[1], read[2]));
      }
      final File input = FileUtils.stringToFile(sb.toString(), new File(tmpDir, "input.sam"));
      final List<File> inputs = new ArrayList<>();
      inputs.add(input);
      final SamBamSequenceDataSource source = getSourceFromFiles(inputs, false, null);
      checkBasic(source);
    } finally {
      assertTrue(FileHelper.deleteAll(tmpDir));
    }
  }

  public void checkBasic(SamBamSequenceDataSource source) throws IOException {
    assertEquals(Long.MAX_VALUE, source.getMinLength());
    assertEquals(Long.MIN_VALUE, source.getMaxLength());
    assertTrue(source.hasQualityData());
    assertEquals(0, source.getWarningCount());
    assertEquals(0, source.getDusted());
    assertEquals(SequenceType.DNA, source.type());
    for (final String[] read : READS) {
      assertTrue(source.nextSequence());
      assertEquals(read[1].length(), source.currentLength());
      assertEquals(read[0], source.name());
      for (int j = 0; j < read[1].length(); j++) {
        assertEquals(DNA.valueOf(read[1].charAt(j)).ordinal(), source.sequenceData()[j]);
      }
      for (int j = 0; j < read[2].length(); j++) {
        assertEquals(read[2].charAt(j) - 33, source.qualityData()[j]);
      }
    }
    assertFalse(source.nextSequence());
    assertFalse(source.nextSequence());
    assertEquals(7, source.getMinLength());
    assertEquals(8, source.getMaxLength());
    source.close();
  }

  public void checkBasicPaired(SamBamSequenceDataSource source) throws IOException {
    assertTrue(source.hasQualityData());
    assertEquals(0, source.getWarningCount());
    assertEquals(0, source.getDusted());
    assertEquals(SequenceType.DNA, source.type());
    for (final String[] read : READS) {
      assertTrue(source.nextSequence());
      assertEquals(read[1].length(), source.currentLength());
      assertEquals(read[0], source.name());
      for (int j = 0; j < read[1].length(); j++) {
        assertEquals(DNA.valueOf(read[1].charAt(j)).ordinal(), source.sequenceData()[j]);
      }
      for (int j = 0; j < read[2].length(); j++) {
        assertEquals(read[2].charAt(j) - 33, source.qualityData()[j]);
      }

      assertTrue(source.nextSequence());
      assertEquals(read[1].length(), source.currentLength());
      assertEquals(read[0], source.name());
      for (int j = 0; j < read[1].length(); j++) {
        assertEquals(DNA.complement((byte) DNA.valueOf(read[1].charAt(j)).ordinal()), source.sequenceData()[read[1].length() - j - 1]);
      }
      for (int j = 0; j < read[2].length(); j++) {
        assertEquals(read[2].charAt(j) - 33, source.qualityData()[j]);
      }
    }
    assertFalse(source.nextSequence());
    assertFalse(source.nextSequence());
    assertEquals(7, source.getMinLength());
    assertEquals(8, source.getMaxLength());
    source.close();
  }
  public void testFilter() throws IOException {
    final StringBuilder sb = new StringBuilder();
    sb.append(SAM_HEADER);
    sb.append(SECOND_READ_GROUP);
    final int numReads = 5;
    for (int i = 0; i < READS.length; i++) {
      sb.append(String.format(SAM_LINE_LEFT, SAM_NL, READS[i][0], READS[i][1], READS[i][2]));
      sb.append(String.format(SAM_LINE_RIGHT, SAM_NL, READS[i][0], DnaUtils.reverseComplement(READS[i][1]), READS[i][2]));
      if (i < numReads) {
        sb.append(String.format(SAM_LINE_LEFT, SAM_NL, READS[i][0], READS[i][1], READS[i][2]).replaceAll("RG:Z:A", "RG:Z:B"));
        sb.append(String.format(SAM_LINE_RIGHT, SAM_NL, READS[i][0], DnaUtils.reverseComplement(READS[i][1]), READS[i][2]).replaceAll("RG:Z:A", "RG:Z:B"));
      }
    }
    checkFilter(sb, numReads, "B");
    checkFilter(sb, READS.length, "A");
  }

  private void checkFilter(StringBuilder sb, int numReads, String readGroup) throws IOException {
    try (TestDirectory td = new TestDirectory()) {
      final File f = new File(td, "blah.sam");
      FileUtils.stringToFile(sb.toString(), f);
      final List<File> inputs = new ArrayList<>();
      inputs.add(f);
      final SamBamSequenceDataSource source = getSourceFromFiles(inputs, true, new SamBamSequenceDataSource.FilterReadGroups(readGroup));
      int recordCount = 0;
      while (source.nextSequence()) {
        recordCount++;
      }
      assertEquals(numReads * 2, recordCount);
    }
  }
}
