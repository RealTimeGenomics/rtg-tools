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

package com.rtg.util.machine;

import static com.rtg.sam.SharedSamConstants.OUT_SAM;
import static com.rtg.sam.SharedSamConstants.SAMHEADER1;
import static com.rtg.sam.SharedSamConstants.SAMHEADER2;
import static com.rtg.util.StringUtils.FS;
import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;

import com.rtg.sam.SamUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;
import junit.framework.TestCase;

/**
 */
public class MachineOrientationTest extends TestCase {


  public void test() {
    TestUtils.testEnum(MachineOrientation.class, "[FR, RF, TANDEM, ANY]");
  }
  private static final String TB = "\t";

  private static final String SAM_REC1 = ""
    + SAMHEADER1
    + "0" + TB +  "163" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "=" + TB + "11" + TB + "10" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC2 = ""
    + SAMHEADER1
   + "0" + TB + "83" + TB + "g1" + TB + "11" + TB + "255" + TB + "4M" + TB + "=" + TB + "3" + TB + "-10" + TB + "TTCA" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC3 = "" //
    + SAMHEADER2
    + "0" + TB +  "163" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "g2" + TB + "11" + TB + "10" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC4 = "" // reverse reverse second
    + SAMHEADER2
    + "0" + TB +  "179" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "=" + TB + "11" + TB + "10" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC5 = "" // Single end
    + SAMHEADER2
    + "0" + TB +  "16" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "*" + TB + "0" + TB + "0" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC6 = "" // mate unmapped
    + SAMHEADER1
    + "0" + TB +  "137" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "=" + TB + "11" + TB + "10" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC7 = "" // reverse reverse and different sequence
    + SAMHEADER2
    + "0" + TB +  "179" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "g2" + TB + "11" + TB + "10" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  private static final String SAM_REC8 = "" // reverse reverse
    + SAMHEADER2
    + "0" + TB +  "179" + TB + "g1" + TB +  "3" + TB + "255" + TB + "4M" + TB + "=" + TB + "11" + TB + "10" + TB + "ATCG" + TB + "````" + TB + "AS:i:1" + TB + "IH:i:1" + LS;

  public void testUnjumbleableIllumina() throws IOException {
    final MachineType illumina = MachineType.ILLUMINA_PE;
    final MachineOrientation orientationIllumina = illumina.orientation();
    final SAMRecord rec = readOneSamRecord(SAM_REC1);
    assertTrue(orientationIllumina.firstOnTemplate(rec));
    assertTrue(orientationIllumina.hasValidMate(rec));

    final SAMRecord rec2 = readOneSamRecord(SAM_REC2);
    assertFalse(orientationIllumina.firstOnTemplate(rec2));
    assertTrue(orientationIllumina.hasValidMate(rec2));

    final SAMRecord rec3 = readOneSamRecord(SAM_REC3);
    assertFalse(orientationIllumina.hasValidMate(rec3));

    final SAMRecord rec4 = readOneSamRecord(SAM_REC4);
    assertFalse(orientationIllumina.hasValidMate(rec4));

    final SAMRecord rec5 = readOneSamRecord(SAM_REC5);
    assertFalse(orientationIllumina.hasValidMate(rec5));

    final SAMRecord rec6 = readOneSamRecord(SAM_REC6);
    assertFalse(orientationIllumina.hasValidMate(rec6));

  }

  public void testUnjumbleableCg() throws IOException {
    final MachineType cg = MachineType.COMPLETE_GENOMICS;
    final MachineOrientation orientationCg = cg.orientation();
    final SAMRecord rec = readOneSamRecord(SAM_REC8);
    assertTrue(orientationCg.hasValidMate(rec));

    final SAMRecord rec7 = readOneSamRecord(SAM_REC7);
    assertFalse(orientationCg.hasValidMate(rec7));

    final SAMRecord rec4 = readOneSamRecord(SAM_REC4);
    assertTrue(orientationCg.hasValidMate(rec4));
    assertTrue(orientationCg.firstOnTemplate(rec4));

    final SAMRecord rec5 = readOneSamRecord(SAM_REC5);
    assertFalse(orientationCg.hasValidMate(rec5));

    final SAMRecord rec6 = readOneSamRecord(SAM_REC6);
    assertFalse(orientationCg.hasValidMate(rec6));

  }

  private SAMRecord readOneSamRecord(String samstr) throws IOException {
    final File input = FileUtils.createTempDir("testcheck", "sv_in");
    try {
      FileUtils.stringToFile(samstr, new File(input, OUT_SAM));

      final String inn = input.getPath();

      final SAMRecord rec;
      final File samfile = new File(inn + FS + OUT_SAM);
      try (SamReader reader = SamUtils.makeSamReader(FileUtils.createInputStream(samfile, false))) {
        final CloseableIterator<SAMRecord> iterator = reader.iterator();
        if (iterator.hasNext()) {
          rec = iterator.next();
        } else {
          rec = null;
        }

      }
      return rec;
    } finally {
      FileHelper.deleteAll(input);
    }
  }


  public void testJustReverse() {
    assertTrue(MachineOrientation.FR.orientationOkay(false, true, true, false));
    assertTrue(MachineOrientation.RF.orientationOkay(true, true, false, false));
    assertTrue(MachineOrientation.FR.orientationOkay(false, false, true, true));
    assertTrue(MachineOrientation.RF.orientationOkay(true, false, false, true));
    assertTrue(MachineOrientation.TANDEM.orientationOkay(true, false, true, true));
    assertTrue(MachineOrientation.TANDEM.orientationOkay(false, true, false, false));
    for (final boolean left : new boolean[] {false, true}) {
      for (final boolean right : new boolean[] {false, true}) {
        assertTrue(MachineOrientation.ANY.orientationOkay(left, true, right, false));
      }
    }
    assertFalse(MachineOrientation.FR.orientationOkay(true, true, false, false));
    assertFalse(MachineOrientation.FR.orientationOkay(true, false, true, true));
    assertFalse(MachineOrientation.FR.orientationOkay(false, true, false, false));
    assertFalse(MachineOrientation.RF.orientationOkay(false, false, true, true));
    assertFalse(MachineOrientation.RF.orientationOkay(true, true, true, false));
    assertFalse(MachineOrientation.RF.orientationOkay(false, true, false, false));
    assertFalse(MachineOrientation.TANDEM.orientationOkay(true, true, false, false));
    assertFalse(MachineOrientation.TANDEM.orientationOkay(false, false, true, true));
  }

  public void testPosAndReverse() {
    assertTrue(MachineOrientation.FR.orientationOkay(5, false, 8, true));
    assertTrue(MachineOrientation.RF.orientationOkay(10, true, 10, false));
    assertTrue(MachineOrientation.TANDEM.orientationOkay(10, true, 9, true));
    assertTrue(MachineOrientation.TANDEM.orientationOkay(9, false, 10, false));
    for (final boolean left : new boolean[] {false, true}) {
      for (final boolean right : new boolean[] {false, true}) {
        assertTrue(MachineOrientation.ANY.orientationOkay(0, left, 1, right));
      }
    }
    assertTrue(MachineOrientation.FR.orientationOkay(10, true, 5, false));
    assertFalse(MachineOrientation.FR.orientationOkay(4, true, 5, false));
    assertFalse(MachineOrientation.FR.orientationOkay(9, true, 10, true));
    assertFalse(MachineOrientation.FR.orientationOkay(5, false, 6, false));
    assertTrue(MachineOrientation.RF.orientationOkay(10, false, 5, true));
    assertFalse(MachineOrientation.RF.orientationOkay(10, false, 18, true));
    assertFalse(MachineOrientation.RF.orientationOkay(4, true, 3, true));
    assertFalse(MachineOrientation.RF.orientationOkay(2, false, 1, false));
    assertFalse(MachineOrientation.TANDEM.orientationOkay(1, true, 2, false));
    assertFalse(MachineOrientation.TANDEM.orientationOkay(4, false, 3, true));
    assertFalse(MachineOrientation.TANDEM.orientationOkay(9, true, 10, true));
    assertFalse(MachineOrientation.TANDEM.orientationOkay(10, false, 9, false));
  }
}
