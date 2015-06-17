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

package com.rtg.sam;

import static com.rtg.util.StringUtils.TAB;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.CliDiagnosticListener;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import junit.framework.TestCase;

/**
 * Test class
 */
public class SkipInvalidRecordsIteratorTest extends TestCase {


  public void testSomeMethod() {
  }

  static final String SAM_HEAD1 = ""
    + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate\n"
    + "@SQ" + TAB + "SN:gi0" + TAB + "LN:30\n"
    + "@SQ" + TAB + "SN:gi1" + TAB + "LN:30\n"
    ;

  static final String SAM_REC_OK1 = ""
    + "aa" + TAB + "0" + TAB + "gi0" + TAB + "2" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB +  "IB7?*III<I" + TAB + "AS:i:0" + TAB + "IH:i:1" + StringUtils.LS;

  static final String SAM_REC_OK1_FORMAT = ""
    + "aa" + TAB + "0" + TAB + "gi0" + TAB + "2" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB +  "IB7?*III<I" + TAB + "AS:i:0" + TAB + "IH:i:1";

  static final String SAM_REC_OK2 = ""
    + "bb" + TAB + "0" + TAB + "gi1" + TAB + "1" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB +  "IB7?*III<I" + TAB + "AS:i:0" + TAB + "IH:i:1\n";

  static final String SAM_REC_OK3 = ""
    + "cc" + TAB + "0" + TAB + "gi1" + TAB + "4" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB +  "IB7?*III<I" + TAB + "AS:i:0" + TAB + "IH:i:1\n";


  public void test() throws IOException {
    Diagnostic.setLogStream();
    final File dir = FileUtils.createTempDir("testSAMFile", "variant");

    // sam file with no records
    final File samNoRecs = new File(dir, SharedSamConstants.OUT_SAM + "norecs");
    FileUtils.stringToFile(SAM_HEAD1, samNoRecs);

    // sam file with ok records
    final File sam = new File(dir, SharedSamConstants.OUT_SAM);
    FileUtils.stringToFile(SAM_HEAD1 + SAM_REC_OK1, sam);

    try (SkipInvalidRecordsIterator sfr = new SkipInvalidRecordsIterator(sam)) {
      assertTrue(sfr.hashCode() != 0);
      final SAMSequenceDictionary dict = sfr.header().getSequenceDictionary();
      assertEquals("gi0", dict.getSequence(0).getSequenceName());
      final List<SAMSequenceRecord> seqs = dict.getSequences();
      assertEquals(2, seqs.size());
      assertEquals("Iterator: line=1 file=" + sam.getPath() + " record=" + SAM_REC_OK1_FORMAT, sfr.toString());
      try {
        assertTrue(sfr.hasNext());
        assertNotNull(sfr.next());
        assertFalse(sfr.hasNext());
      } catch (final AssertionError ae) {
        fail();
      }
      try {
        sfr.next();
        fail();
      } catch (final NoSuchElementException e) {
        // expected
      }
      try {
        sfr.remove();
        fail();
      } catch (final UnsupportedOperationException e) {
        // expected
      }
    }

    final SkipInvalidRecordsIterator sfr0 = new SkipInvalidRecordsIterator(sam);
    sfr0.close();

    try {
      new SkipInvalidRecordsIterator(new File("badsam"));
      fail();
    } catch (final IOException e) {
      //e.printStackTrace();
    }
    try {
      new SkipInvalidRecordsIterator(new File("xx" + StringUtils.FS + "badsam"));
      fail();
    } catch (final IOException e) {
      //e.printStackTrace();
    }

    FileHelper.deleteAll(dir);
  }

  // bad syntax in field
  static final String SAM_REC_BAD2 = ""
    + "badread2" + TAB + "16" + TAB + "gi" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:0\n";
  // missing field
  static final String SAM_REC_BAD3 = ""
    + "badread3" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB +  TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:i:0\n";
  static final String SAM_REC_BAD3A = ""
    + "badread3a" + TAB + "16" + TAB + "gi0";
  static final String SAM_REC_BAD4 = ""
    + "badread4" + TAB + "-9" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:i:0\n";
  static final String SAM_REC_BAD6 = ""
    + "badread6" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "xxx" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:i:0\n";
  static final String SAM_REC_BAD7 = ""
    + "badread7" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "40" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:i:0\n";
  static final String SAM_REC_BAD8 = ""
    + "badread8" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=" + TAB + "AS:i:0" + TAB + "IH:i:0\n";
  static final String SAM_REC_BAD9 = ""
    + "badread8" + TAB + "flag?" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=" + TAB + "AS:i:0" + TAB + "IH:i:0\n";

  //Expects the input to completely unparseable - exceptions thrown during opening
  public void checkUgly(final String samrecords, int numValid, String... expectedErrs) throws IOException {
    Diagnostic.setLogStream();
    Diagnostic.clearListeners();
    final MemoryPrintStream out = new MemoryPrintStream();
    final File dir = FileUtils.createTempDir("checkugly", "samfileandrecord");
    final CliDiagnosticListener listener = new CliDiagnosticListener(out.printStream());
    final File sam = new File(dir, SharedSamConstants.OUT_SAM);
    final int num = 0;
    try {
      Diagnostic.addListener(listener);
      FileUtils.stringToFile(samrecords, sam);
      assertTrue(expectedErrs.length > 0);
      final SkipInvalidRecordsIterator sfr = new SkipInvalidRecordsIterator(sam);
      fail();
      sfr.close();
    } catch (final NoTalkbackSlimException e) {
      e.printErrorNoLog();
      final String str = out.toString();
      //System.err.println(str);
      assertEquals(numValid, num);
      assertTrue(str.contains("SAM record has an irrecoverable problem in file " + sam.getPath()));
      TestUtils.containsAll(str, expectedErrs);
    } finally {
      Diagnostic.clearListeners();
    }
    FileHelper.deleteAll(dir);
    Diagnostic.clearListeners();
  }

  //Expects the input to contain records that produce warnings
  public void checkWarning(final String samrecords, final int numValid, final String... msgs) throws IOException {
    Diagnostic.setLogStream();
    Diagnostic.clearListeners();
    final MemoryPrintStream out = new MemoryPrintStream();
    final File dir = FileUtils.createTempDir("checkugly", "samfileandrecord");
    final CliDiagnosticListener listener = new CliDiagnosticListener(out.printStream());
    final File sam = new File(dir, SharedSamConstants.OUT_SAM);
    int num = 0;
    try {
      Diagnostic.addListener(listener);
      FileUtils.stringToFile(samrecords, sam);
      try (SkipInvalidRecordsIterator sfr = new SkipInvalidRecordsIterator(sam)) {
        while (sfr.hasNext()) {
          sfr.next();
          num++;
        }
        final String str = out.toString();
        //System.err.println(str);
        TestUtils.containsAll(str, msgs);
        assertEquals(numValid, num);
      } catch (final NoTalkbackSlimException e) {
        fail();
      }
    } catch (final NoTalkbackSlimException e) {
      fail();
    } finally {
      Diagnostic.clearListeners();
    }
    FileHelper.deleteAll(dir);
    Diagnostic.clearListeners();
 }

  //Expects the input to contain no records that cause exceptions to be thrown
  public void checkBadNoError(final String samrecords, int numValid, int total) throws IOException {
    Diagnostic.setLogStream();
    Diagnostic.clearListeners();
    final MemoryPrintStream out = new MemoryPrintStream();
    final File dir = FileUtils.createTempDir("checkbad", "samfileandrecord");
    final CliDiagnosticListener listener = new CliDiagnosticListener(out.printStream());
    final File sam = new File(dir, SharedSamConstants.OUT_SAM);
    int num = 0;
    try {
      Diagnostic.addListener(listener);
      FileUtils.stringToFile(samrecords, sam);
      try (SkipInvalidRecordsIterator sfr = new SkipInvalidRecordsIterator(sam)) {
        while (sfr.hasNext()) {
          final SAMRecord rec = sfr.next();
          if (!rec.getIsValid()) {
            fail();
          }
          num++;
        }
        assertEquals(numValid, num);
        assertEquals(numValid, sfr.getOutputRecordsCount());
        assertEquals(total, sfr.getTotalRecordsCount());
        assertEquals(total - numValid, sfr.getInvalidRecordsCount());
        assertEquals(numValid, sfr.getTotalRecordsCount() - sfr.getFilteredRecordsCount() - sfr.getDuplicateRecordsCount() - sfr.getInvalidRecordsCount());
      } catch (final NoTalkbackSlimException e) {
        fail();
      }
    } catch (final NoTalkbackSlimException e) {
      fail();
    } finally {
      Diagnostic.clearListeners();
    }
    FileHelper.deleteAll(dir);
    Diagnostic.clearListeners();
  }

  //Expects the input to contain records that cause exceptions to be thrown
  public void checkBad(final String samrecords, int numValid, String... expectedErrs) throws IOException {
    Diagnostic.setLogStream();
    Diagnostic.clearListeners();
    final MemoryPrintStream out = new MemoryPrintStream();
    final File dir = FileUtils.createTempDir("checkbad", "samfileandrecord");
    final CliDiagnosticListener listener = new CliDiagnosticListener(out.printStream());
    final File sam = new File(dir, SharedSamConstants.OUT_SAM);
    boolean error = true;
    int num = 0;
    try {
      Diagnostic.addListener(listener);
      FileUtils.stringToFile(samrecords, sam);
      final SkipInvalidRecordsIterator sfr = new SkipInvalidRecordsIterator(sam);
      try {
        while (sfr.hasNext()) {
          sfr.next();
          num++;
        }
        fail("Expected Errors not thrown");
      } catch (final NoTalkbackSlimException e) {
        e.printErrorNoLog();
        error = true;
        final String str = out.toString();
        //System.err.println(str);
        sfr.close();
        assertTrue(str.contains("SAM record has an irrecoverable problem in file " + sam.getPath()));
        for (String expectedErr : expectedErrs) {
          TestUtils.containsAll(str, expectedErr);
        }
      } finally {
        sfr.close();
      }
    } catch (final NoTalkbackSlimException e) {
      fail(e.getMessage());
    } finally {
      Diagnostic.clearListeners();
    }
    assertEquals(Math.max(0, numValid - 1), num); // Currently lookahead prevents last valid record from being obtained in the case of *fatal* records
    assertTrue(error);
    FileHelper.deleteAll(dir);
    Diagnostic.clearListeners();
  }

  static final String SAM_HEAD10 = ""
    + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate\n"
    + "@SQ" + TAB + "SN:gi0" + TAB + "LN:30\n"
    + "@SQ" + TAB + "SN:gi1" + TAB + "LN:30\n"
    ;

  static final String SAM_REC_OK10 = ""
    + "read10" + TAB + "0" + TAB + "gi0" + TAB + "2" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB +  "IB7?*III<I" + TAB + "AS:i:0" + TAB + "IH:i:1" + StringUtils.LS;

  static final String SAM_REC_OK11 = ""
    + "read11" + TAB + "0" + TAB + "gi0" + TAB + "2" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB +  "IB7?*III<I" + TAB + "AS:i:0" + TAB + "IH:i:1" + StringUtils.LS;

  static final String SAM_REC_BAD1 = ""
    + "badread10" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:0\n";

  public void testBad2First1() throws IOException {
    checkWarning(SAM_HEAD10 + SAM_REC_BAD1, 0, "SAM record is invalid in file ", "At data line 1"); // "Line: badread10", "Not enough fields in tag 'IH:0'"
  }

  public void testBad2First2() throws IOException {
    // "Line: badread2", "RNAME 'gi' not found in any SQ record");
    checkWarning(SAM_HEAD10 + SAM_REC_BAD2, 0, "SAM record is invalid in file ",
      "badread2" + TAB + "16" + TAB + "gi" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" /*+ TAB + "IH:0\n" SAM bug*/,
      "[ERROR: Read name badread2, Reference sequence not found in sequence dictionary.]",
      "At data line 1");
  }

  public void testBad2First3() throws IOException {
    checkWarning(SAM_HEAD10 + SAM_REC_BAD3, 0, "SAM record is invalid in file ",
      "badread3" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" /*+ TAB + "IH:0\n" SAM bug*/,
      "[ERROR: Read name badread3, CIGAR should have > zero elements for mapped read.]",
      "At data line 1"); // "Line: badread3", "Empty field at position 5 (zero-based);");
  }

  public void testBad2First4() throws IOException {
    checkUgly(SAM_HEAD10 + SAM_REC_BAD3A, 0, "Line: badread3a", "Error parsing text SAM file. Not enough fields;", "Line 4");
  }

  public void testBad2First5() throws IOException {
    checkWarning(SAM_HEAD10 + SAM_REC_BAD4, 0, "SAM record is invalid in file ",
      "badread4" + TAB + "-9" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=I3IIII" + TAB + "AS:i:0" + TAB + "IH:i:0",
      "[ERROR: Read name badread4, Not primary alignment flag should not be set for unmapped read., ERROR: Read name badread4, Supplementary alignment flag should not be set for unmapped read., ERROR: Read name badread4, MAPQ should be 0 for unmapped read.]",
      "At data line 1"); //"Line: badread4", "MRNM not specified but flags indicate mate mapped;");
  }

  public void testBad2First6() throws IOException {
    checkWarning(SAM_HEAD10 + SAM_REC_BAD6, 0, "Malformed CIGAR string: xxx");
  }

  public void testBad2First7() throws IOException {
  //"Line: badread7", "MRNM specified but flags indicate unpaired
    checkWarning(SAM_HEAD10 + SAM_REC_BAD7, 0, "SAM record is invalid in file ",
      "At data line 1");
  }

  public void testBad2First8() throws IOException {
    checkWarning(SAM_HEAD10 + SAM_REC_BAD8, 0, "SAM record is invalid in file ",
      "badread8" + TAB + "16" + TAB + "gi0" + TAB + "3" + TAB + "255" + TAB + "10M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "AAAAAAAAAA" + TAB + "GC@=" + TAB + "AS:i:0" + TAB + "IH:i:0",
      "[ERROR: Read name badread8, Read length does not match quals length]",
      "At data line 1"); //"Line: badread8", "length(QUAL) != length(SEQ)");
  }

  public void testBad2First9() throws IOException {
    checkUgly(SAM_HEAD10 + SAM_REC_BAD9, 0, "Non-numeric value in FLAG column");
  }

  public void testBad2First10() {
    // no error when negative pos:
    //This is a bug that it isnt found in SAM error checking
    //checkUgly(SAM_HEAD10 + SAM_REC_BAD10, 0); //"Line: badread5");
  }

  public void testBad2Second() throws IOException {
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD1, 1, 2); // "Line: badread10", "Not enough fields in tag 'IH:0'");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD2, 1, 2); // "Line: badread2", "RNAME 'gi' not found in any SQ record");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD3, 1, 2); // "Line: badread3", "Empty field at position 5 (zero-based);");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD4, 1, 2); //, "Line: badread4", "MRNM not specified but flags indicate mate mapped;");
    // no error when negative pos:
    //checkBadLow(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD5, "Line: badread5");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD6, 1, 2); // "Line: something Malformed CIGAR string: xxx
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD7, 1, 2); // "Line: badread7", "MRNM specified but flags indicate unpaired;");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD8, 1, 2); //, "Line: badread8", "length(QUAL) != length(SEQ)");

    checkBad(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD3A, 1, "Line: badread3a", "Error parsing text SAM file. Not enough fields;",  "Line 5");
    checkBad(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_BAD9, 1, "Non-numeric value in FLAG column");
  }

  public void testBad2Third() throws IOException {
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD1, 2, 3); //, "Line: badread10");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD2, 2, 3); //, "Line: badread2", "RNAME 'gi' not found in any SQ record");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD3, 2, 3); //, "Line: badread3", "Empty field at position 5 (zero-based);");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD4, 2, 3); //, "Line: badread4", "MRNM not specified but flags indicate mate mapped;");
    // no error when negative pos:
    //checkBadLow(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD5, "Line: badread5");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD6 + SAM_REC_OK11, 3, 4); //, "Line: blah Malformed CIGAR string: xxx");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD7, 2, 3); //, "Line: badread7", "MRNM specified but flags indicate unpaired;");
    checkBadNoError(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD8, 2, 3); //, "Line: badread8", "length(QUAL) != length(SEQ)");

    checkBad(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD3A, 2, "Line: badread3a", "Error parsing text SAM file. Not enough fields;", "Line 6");
    checkBad(SAM_HEAD10 + SAM_REC_OK10 + SAM_REC_OK11 + SAM_REC_BAD9, 2, "Non-numeric value in FLAG column");
  }

}

