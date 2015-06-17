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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests corresponding class
 */
public class Cg2SdfTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new Cg2Sdf();
  }

  public void testHelp() {
    checkHelp("rtg cg2sdf"
        , "Converts Complete Genomics sequencing system reads to RTG SDF format"
        , "-I,", "--input-list-file", "file containing a list of Complete Genomics files (1 per line)"
        , "-o,", "name of output SDF"
        , "FILE+", "file in Complete Genomics format. May be specified 0 or more times"
        , "--max-unknowns=INT", "maximum number of Ns allowed in either side for a read (Default is 5)"
        , "--no-quality", "does not include quality data in the resulting SDF"
    );
    checkExtendedHelp("rtg cg2sdf"
        , "--Xcompress=BOOL", "compress sdf (Default is " + true + ")"
        , "--Xkeep-names", "add name data to the resulting SDF"
        );
  }

  public void testCFlagsA() throws Exception {
    Diagnostic.setLogStream();

    final File dir = FileUtils.createTempDir("cg2sdftest", "badfiles");
    final File notexist = new File(dir, "no");
    final File in = new File(dir, "fin");
    try {
      assertTrue(in.createNewFile());
      checkHandleFlagsOut("-o", notexist.getPath(), in.getPath());
      final CFlags flags = getCFlags();
      final Iterator<Object> it = flags.getAnonymousValues(0).iterator();
      assertTrue(it.hasNext());
      assertEquals(in.getPath(), ((File) it.next()).getPath());
      assertEquals(notexist.getPath(), ((File) flags.getValue(Cg2Sdf.OUTPUT_FLAG)).getPath());
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private static final String[] ERROR_FOOTER = {"",
    "Usage: rtg cg2sdf [OPTION]... -o SDF FILE+",
    "                  [OPTION]... -o SDF -I FILE",
    "Try '--help' for more information"
  };

  public void testCFlagsB() throws Exception {
    Diagnostic.setLogStream();

    final File dir = FileUtils.createTempDir("cg2sdftest", "badfiles");
    final File in = new File(dir, "in");
    final File outdir = new File(dir, "out");
    try {
      TestUtils.containsAll(checkHandleFlagsErr(in.getPath()),
                            "You must provide a value for -o SDF");
      TestUtils.containsAll(checkHandleFlagsErr(in.getPath()),
                            ERROR_FOOTER);
      TestUtils.containsAll(checkHandleFlagsErr("-o", outdir.getPath()),
                            "No input files specified.");
      assertTrue(in.createNewFile());
      assertTrue(outdir.mkdirs());
      TestUtils.containsAll(checkHandleFlagsErr("-o", outdir.getPath(), in.getPath()),
                            "The directory",
                            "already exists. Please remove it first or choose a different directory.");

      assertTrue(FileHelper.deleteAll(outdir));

      TestUtils.containsAll(checkHandleFlagsErr("-o", outdir.getPath(), in.getPath(), "--max-unknowns", "-1"),
                            "The specified flag \"--max-unknowns\" has invalid value \"-1\". It should be greater than or equal to \"0\".");

      final String err = checkHandleFlagsErr("-o", outdir.getPath(), "-I", "x");
      TestUtils.containsAll(err,
                            "An error occurred reading " + getCFlags().getValue(CommonFlags.INPUT_LIST_FLAG));

    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private void resetCounts(int[] counts) {
    for (int i = 0; i < counts.length; i++) {
      counts[i] = 0;
    }
  }

  public void testCg2SdfWithBadFiles() throws Exception {

    final int[] fileNotFoundCount = new int[3];

    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        //System.err.println("++" + event.getMessage() + "++");
        if (ErrorType.FILE_NOT_FOUND == event.getType()) {
          if (event.getMessage().contains("in.calls")) {
            fileNotFoundCount[0]++;
          } else if (event.getMessage().contains("in.scores")) {
            fileNotFoundCount[1]++;
          } else if (event.getMessage().contains("in")) {
            fileNotFoundCount[2]++;
          }
        } else if (ErrorType.INFO_ERROR == event.getType()) {
          //Expected
        } else if (ErrorType.FILE_READ_ERROR != event.getType()) {
          fail("Unexpected error: " + event.getType());
        }
      }

      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      final PrintStream err = new PrintStream(bos);
      Diagnostic.setLogStream();

      final File dir = FileUtils.createTempDir("cg2sdftest", "badfiles");
      final File in = new File(dir, "in");
      final File out = new File(dir, "out");
      try {
        assertEquals(1, new Cg2Sdf().mainInit(new String[] {in.getPath(), "-o", out.getPath()}, bout, err));
        assertEquals(0, fileNotFoundCount[0]);
        assertEquals(0, fileNotFoundCount[1]);
        assertEquals(1, fileNotFoundCount[2]);
        resetCounts(fileNotFoundCount);
        //        System.err.println(bout.toString());
        //        System.err.println(bos.toString());
        Diagnostic.setLogStream();
        //make the input file
        err.close();
      } finally {
        FileHelper.deleteAll(dir);
      }
    } finally {
      Diagnostic.removeListener(dl);
      Diagnostic.deleteLog();
      bos.close();
      bout.close();
    }
  }

  private static final String[] EXPECTED_MSGS = {
                            StringUtils.LS
                          + "Input Data" + StringUtils.LS
                          + "Files              : in.tsv" + StringUtils.LS
                          + "Format             : CG" + StringUtils.LS
                          + "Type               : DNA" + StringUtils.LS
                          + "Number of pairs    : 2" + StringUtils.LS
                          + "Number of sequences: 4" + StringUtils.LS
                          + "Total residues     : 40" + StringUtils.LS
                          + "Minimum length     : 10" + StringUtils.LS
                          + "Maximum length     : 10" + StringUtils.LS
                          + StringUtils.LS
                          + "Output Data" + StringUtils.LS,
                            "SDF-ID             : ",
                            "Number of pairs    : 1" + StringUtils.LS
                          + "Number of sequences: 2" + StringUtils.LS
                          + "Total residues     : 20" + StringUtils.LS
                          + "Minimum length     : 10" + StringUtils.LS
                          + "Maximum length     : 10" + StringUtils.LS,
                            "There were 1 pairs skipped due to filters"
  };

  public void testNs() throws Exception {
    final File tempDir = FileUtils.createTempDir("cg2sdftest", "ns");
    try {
      final File out = new File(tempDir, "out");
      final File in = new File(tempDir, "in.tsv");
      FileUtils.stringToFile(">flags\treads\tscores\n1\tANAAANAAAACCCCCCCCCC\tABCDEFGHIJJIHGFEDCBA\n2\tANAAANNAAACCCCCNCCCC\tABCDEFGHIJJIHGFEDCBA\n", in);

      final MemoryPrintStream mps = new MemoryPrintStream();
      new Cg2Sdf().mainInit(new String[] {"-o", out.getPath(), "--max-unknowns", "2", in.getPath()}, mps.outputStream(), mps.printStream());

      TestUtils.containsAll(mps.toString(), EXPECTED_MSGS);

      final File summary = new File(out, "summary.txt");
      assertTrue(summary.exists());
      final String sum = FileUtils.fileToString(summary);
      TestUtils.containsAll(sum, EXPECTED_MSGS);

    } finally {
      FileHelper.deleteAll(tempDir);
    }
  }

  public void testGetBaseInputPath() {
    assertEquals("test", Cg2Sdf.getBaseInputPath(new File("test.tsv")));
    assertEquals("", Cg2Sdf.getBaseInputPath(new File(".tsv")));
  }

  public void testCg2SdfWithVersion2() throws Exception {
    Diagnostic.setLogStream();
    final File tempDir = FileUtils.createTempDir("cg2sdftest", "version2");
    try {
      final File dir = new File(tempDir, "output");
      final ByteArrayOutputStream berr = new ByteArrayOutputStream();
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      final ByteArrayOutputStream boutleft = new ByteArrayOutputStream();
      final ByteArrayOutputStream boutright = new ByteArrayOutputStream();
      try {
        final MemoryPrintStream out = new MemoryPrintStream();
        final MemoryPrintStream err = new MemoryPrintStream();
        Diagnostic.setLogStream();
        final File sample = new File(tempDir, "sample.tsv.gz");
        try {
          FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample.tsv"), sample);
          final int res = new Cg2Sdf().mainInit(new String[] {sample.getAbsolutePath(), "-o", dir.getAbsolutePath()}, out.outputStream(), err.printStream());
          assertEquals(err.toString(), 0, res);
          TestUtils.containsAll(out.toString(), "");
          final int res2 = new SdfStatistics().mainInit(new String[] {dir.getAbsolutePath() + "/left"}, boutleft, err.printStream());
          assertEquals(err.toString(), 0, res2);
          TestUtils.containsAll(boutleft.toString(), "",
            "DNA",
            "CG",
            "LEFT",
            "Number of sequences: 3",
            "Maximum length     : 10",
            "Minimum length     : 10",
            "N                  : 0",
            "A                  : 15",
            "C                  : 0",
            "G                  : 10",
            "T                  : 5",
            "Total residues     : 30",
            "Residue qualities  : yes");
          final int res3 = new SdfStatistics().mainInit(new String[] {dir.getAbsolutePath() + "/right"}, boutright, err.printStream());
          assertEquals(0, res3);
          TestUtils.containsAll(boutright.toString(), "",
            "DNA",
            "CG",
            "RIGHT",
            "Number of sequences: 3",
            "Maximum length     : 10",
            "Minimum length     : 10",
            "N                  : 0",
            "A                  : 5",
            "C                  : 10",
            "G                  : 0",
            "T                  : 15",
            "Total residues     : 30",
            "Residue qualities  : yes");
        } finally {
          assertTrue(sample.delete());
        }
      } finally {
        berr.close();
        bout.close();
        boutleft.close();
        boutright.close();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  public void testCg2SdfNoQuality() throws Exception {
    Diagnostic.setLogStream();
    final File tempDir = FileUtils.createTempDir("cg2sdftest", "noquality");
    try {
      final File dir = new File(tempDir, "output");
      final ByteArrayOutputStream berr = new ByteArrayOutputStream();
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      final ByteArrayOutputStream boutleft = new ByteArrayOutputStream();
      final ByteArrayOutputStream boutright = new ByteArrayOutputStream();
      try {
        final MemoryPrintStream err = new MemoryPrintStream();
        Diagnostic.setLogStream();
        final File sample = new File(tempDir, "sample.tsv.gz");
        try {
          FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample.tsv"), sample);
          final int res = new Cg2Sdf().mainInit(new String[] {sample.getAbsolutePath(), "-o", dir.getAbsolutePath(), "--no-quality"}, bout, err.printStream());
          assertEquals(err.toString(), 0, res);
          final int res2 = new SdfStatistics().mainInit(new String[] {dir.getAbsolutePath() + "/left"}, boutleft, err.printStream());
          assertEquals(err.toString(), 0, res2);
          assertTrue(!boutleft.toString().contains("Residue qualities  : yes"));
          final int res3 = new SdfStatistics().mainInit(new String[] {dir.getAbsolutePath() + "/right"}, boutright, err.printStream());
          assertEquals(err.toString(), 0, res3);
          assertTrue(boutright.toString().contains("Residue qualities  : no"));
        } finally {
          assertTrue(sample.delete());
        }
      } finally {
        berr.close();
        bout.close();
        boutleft.close();
        boutright.close();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  public void testSamRGFormatCustom() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.tsv.gz");
      final File output = new File(dir, "output");
      final String expected = "@RG\tID:rg999\tPL:COMPLETE\tSM:sample";
      final String custom = expected.replaceAll("\t", "\\\\t");
      final String[] args = {input.getPath(), "-o", output.getPath(), "--sam-rg", custom};
      FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample.tsv"), input);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      final int errorCode = new Cg2Sdf().mainInit(args, out.outputStream(), err.printStream());
      assertEquals(err.toString(), 0, errorCode);
      final DefaultSequencesReader leftReader = new DefaultSequencesReader(new File(output, "left"), LongRange.NONE);
      assertEquals(expected, leftReader.samReadGroup());
      final DefaultSequencesReader rightReader = new DefaultSequencesReader(new File(output, "right"), LongRange.NONE);
      assertEquals(expected, rightReader.samReadGroup());
    }
  }

  public static Test suite() {
    return new TestSuite(Cg2SdfTest.class);
  }
  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}

