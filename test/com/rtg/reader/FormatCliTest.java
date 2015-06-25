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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.reader.FormatCli.BadFormatCombinationException;
import com.rtg.reader.FormatCli.PrereadExecutor;
import com.rtg.reader.FormatCli.PrereadExecutor.SequenceProcessor;
import com.rtg.util.Constants;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for corresponding class.
 *
 */
public class FormatCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new FormatCli();
  }

  public static Test suite() {
    return new TestSuite(FormatCliTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public void testHelp() {
    checkHelp("rtg format"
        , "Converts the contents of sequence data files (FASTA/FASTQ/SAM/BAM) into the RTG Sequence Data File (SDF) format."
        , "-f,", "--format=FORMAT", "format of input (Must be one of [fasta, fastq, cgfastq, sam-se, sam-pe]) (Default is fasta)"
        , "-q,", "--quality-format=FORMAT", "format of quality data for fastq files (use sanger for Illumina 1.8+) (Must be one of [sanger, solexa, illumina])"
        , "--input-list-file=FILE", "file containing a list of input read files (1 per line)"
        , "-l,", "--left=FILE", "left input file for FASTA/FASTQ paired end data"
        , "-o,", "--output=SDF", "name of output SDF"
        , "-p,", "--protein", "input is protein. If this option is not specified, then the input is assumed to consist of nucleotides"
        , "-r,", "--right=FILE", "right input file for FASTA/FASTQ paired end data"
        , "FILE+", "input sequence files. May be specified 0 or more times"
        , "--duster", "treat lower case residues as unknowns"
        , "--exclude=STRING", "exclude input sequences based on their name. If the input sequence contains the specified string then that sequence is excluded from the SDF. May be specified 0 or more times"
        , "--no-names", "do not include name data in the SDF output"
        , "--no-quality", "do not include quality data in the SDF output"
        , "--allow-duplicate-names"
        , "--sam-rg"
        , "--select-read-group"
        );
    checkExtendedHelp("rtg format"
        , "--Xcompress=BOOL", "compress sdf (Default is " + true + ")"
        );
  }

  public void testCFlagsQ() throws Exception {
    Diagnostic.setLogStream();
    final File dir = FileUtils.createTempDir("formattest", "badfiles");
    try {
      final File xx = new File(dir, "xx");
      FileUtils.stringToFile("", xx);
      final File nsd = new File(dir, "no-such-dir");
      final ResourceBundle resource = ResourceBundle.getBundle(FormatCli.PREREAD_RESOURCE_BUNDLE, Locale.getDefault());
      checkHandleFlagsOut("-p", "-o", nsd.getPath(), "-f", resource.getString("FORMAT_FASTQ"), "-q", "sanger", xx.getPath());
      final CFlags flags = getCFlags();
      assertTrue(flags.isSet(resource.getString("PROTEIN_FLAG")));
      assertEquals(resource.getString("FORMAT_FASTQ"), flags.getValue(resource.getString("FORMAT_FLAG")));
      assertEquals(nsd.getPath(), ((File) flags.getValue(resource.getString("OUTPUT_FLAG"))).getPath());
      assertEquals(xx, flags.getAnonymousFlag(0).getValue());
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private static final String JUNITOUT = ".junitout";
  private static final String EXPECTED_MSG = "" + StringUtils.LS
    + "Format             : FASTA" + StringUtils.LS
    + "Type               : DNA" + StringUtils.LS
    + "Number of sequences: 2" + StringUtils.LS
    + "Total residues     : 10" + StringUtils.LS
    + "Minimum length     : 5" + StringUtils.LS
    + "Maximum length     : 5" + StringUtils.LS;

  public void testValidUseA() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "test");
      final File outputDir = new File(tempDir, JUNITOUT);
      FileUtils.stringToFile(">x\naCTGN\n>y\nACTGN\n", raw);
      testValidUse(new String[] {"-o", outputDir.getPath(), raw.getPath(), "--duster", "--exclude", "y"}, outputDir);
    }
  }

  public void testValidUseAList() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "test");
      final File outputDir = new File(tempDir, JUNITOUT);
      FileUtils.stringToFile(">x\naCTGN\n>y\nACTGN\n", raw);
      final File list = new File(tempDir, "list");
      FileUtils.stringToFile(raw.getPath(), list);
      testValidUse(new String[] {"-o", outputDir.getPath(), "-I", list.getPath(), "--duster", "--exclude", "y"}, outputDir);
    }
  }

  private void testValidUse(String[] args, File outputDir) throws IOException {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      try (PrintStream err = new PrintStream(bos)) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(0, new FormatCli().mainInit(args, bout, err));

        final String outStr = bout.toString();
        TestUtils.containsAll(outStr, EXPECTED_MSG
          , "Formatting FASTA data"
          , "Input Data"
          , "Files              : "
          , StringUtils.LS + StringUtils.LS + "Output Data"
          , "SDF-ID             : "
          , "Number of sequences: 1" + StringUtils.LS
          + "Total residues     : 5" + StringUtils.LS
          + "Minimum length     : 5" + StringUtils.LS
          + "Maximum length     : 5" + StringUtils.LS
          + StringUtils.LS
          + "There were 1 sequences skipped due to filters" + StringUtils.LS
          + "There were 1 residues converted from lower case to unknowns");

        assertTrue(outputDir.isDirectory());
        assertTrue(new File(outputDir, "mainIndex").exists());
        final File summary = new File(outputDir, "summary.txt");
        assertTrue(summary.exists());
        final String sum = FileUtils.fileToString(summary);
        assertTrue(sum, sum.contains(EXPECTED_MSG));
        final File progress = new File(outputDir, "progress");
        assertTrue(progress.exists());
        final String prog = FileUtils.fileToString(progress);
        TestUtils.containsAll(prog, "Formatting FASTA data");
        final File outf = File.createTempFile("junit", ".fasta");
        outf.deleteOnExit();
        try {
          assertEquals(0, new Sdf2Fasta().mainInit(new String[]{"-o", outf.getPath(), "-i", outputDir.getPath(), "-Z"}, out, err));
          assertEquals(0, out.toString().length());
          assertTrue(outf.exists());
          assertFalse(outf.isDirectory());
          try (BufferedReader r = new BufferedReader(new FileReader(outf))) {
            assertEquals(">x", r.readLine());
            assertEquals("NCTGN", r.readLine());
            assertNull(r.readLine());
          }
        } finally {
          assertTrue(outf.delete());
        }
      }
    }

  }

  public void testDuplicateError() throws Exception {
    Diagnostic.setLogStream();
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File inputA = new File(tempDir, "inputA.fasta");
      final File inputB = new File(tempDir, "inputB.fasta");
      FileUtils.stringToFile(">x\nactgn" + StringUtils.LS, inputA);
      FileUtils.stringToFile(">y\nactgn" + StringUtils.LS + ">x\nactgn" + StringUtils.LS, inputB);
      final File outputDir = new File(tempDir, "output");
      final File dupFile = new File(outputDir.getAbsolutePath(), "duplicate-names.txt");
      final String err = checkMainInitWarn("-o", outputDir.getAbsolutePath(), inputA.getAbsolutePath(), inputB.getAbsolutePath());
      assertTrue(outputDir.exists());
      assertTrue(dupFile.exists());
      assertTrue(err.contains("Duplicate Sequence Names in Input"));
      assertEquals("x" + StringUtils.LS, FileUtils.fileToString(dupFile));
    }
  }

  private static final String EXPECTED_FASTQ_MSG = "" + StringUtils.LS
    + "Format             : FASTQ" + StringUtils.LS
    + "Type               : DNA" + StringUtils.LS
    + "Number of sequences: 1" + StringUtils.LS
    + "Total residues     : 5" + StringUtils.LS
    + "Minimum length     : 5" + StringUtils.LS
    + "Maximum length     : 5" + StringUtils.LS;

  public void testValidUseQ() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "raw");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw);
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        try (PrintStream err = new PrintStream(bos)) {
          final File outputDir = new File(tempDir, JUNITOUT);
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          assertEquals(0, new FormatCli().mainInit(new String[]{"-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", raw.getPath()}, bout, err));
          TestUtils.containsAll(bout.toString(), EXPECTED_FASTQ_MSG);
          assertTrue(outputDir.isDirectory());
          assertTrue(new File(outputDir, "mainIndex").exists());
          final File summary = new File(outputDir, "summary.txt");
          assertTrue(summary.exists());
          final String sum = FileUtils.fileToString(summary);
          assertTrue(sum, sum.contains(EXPECTED_FASTQ_MSG));

          final File outf = new File(tempDir, "out.fasta");
          //System.err.println("bos 1 ' " + bos.toString());
          try (ByteArrayOutputStream bos1 = new ByteArrayOutputStream()) {
            assertEquals(0, new SdfStatistics().mainInit(new String[]{outputDir.getAbsolutePath()}, bos1, TestUtils.getNullPrintStream()));
            assertTrue(bos1.toString().contains("Sequence names     : yes"));
            assertTrue(bos1.toString().contains("Residue qualities  : yes"));
          }

          final int flag = new Sdf2Fasta().mainInit(new String[]{"-o", outf.getPath(), "-i", outputDir.getPath(), "-Z"}, out, err);
          assertEquals(0, out.toString().length());
          assertEquals(0, flag);
          assertTrue(outf.exists());
          assertFalse(outf.isDirectory());
          try (BufferedReader r = new BufferedReader(new FileReader(outf))) {
            assertEquals(">x", r.readLine());
            assertEquals("ACTGN", r.readLine());
            assertNull(r.readLine());
          }
        }
        bos.flush();
        //System.err.println(bos.toString());
      }
    }
  }

  public void testNoNames() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "raw");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw);
      final File outputDir = new File(tempDir, JUNITOUT);
      assertEquals(0, new FormatCli().mainInit(new String[] {"-o",  outputDir.getPath(), "-f", "fastq", "-q", "sanger", raw.getPath(), "--no-names"}, TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream()));
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        assertEquals(0, new SdfStatistics().mainInit(new String[]{outputDir.getAbsolutePath()}, bos, TestUtils.getNullPrintStream()));
        assertTrue(bos.toString(), bos.toString().contains("Sequence names     : no"));
      }
    }
  }

  public void testNoQuality() throws Exception  {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "raw");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw);
      final File outputDir = new File(tempDir, JUNITOUT);
      assertEquals(0, new FormatCli().mainInit(new String[] {"-o",  outputDir.getPath(), "-f", "fastq", "-q", "sanger", raw.getPath(), "--no-quality"}, TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream()));
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        assertEquals(0, new SdfStatistics().mainInit(new String[]{outputDir.getAbsolutePath()}, bos, TestUtils.getNullPrintStream()));
        assertTrue(bos.toString(), bos.toString().contains("Residue qualities  : no"));
      }
    }
  }

  public void testValidUsePaired() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw1 = new File(tempDir, "raw1");
      final File raw2 = new File(tempDir, "raw2");
      FileUtils.stringToFile(">x\n" + "aCTGN\n>y\nACTGN\n", raw1);
      FileUtils.stringToFile(">x\n" + "ACTGN\n>y\nACTGN\n", raw2);
      final MemoryPrintStream mps = new MemoryPrintStream();
      final MemoryPrintStream mpsout = new MemoryPrintStream();

          final File outputDir = new File(tempDir, JUNITOUT);
          runFormat(outputDir, new String[] {"-o",  outputDir.getPath(), "-f", "fasta", "-l" , raw1.getPath(), "-r", raw2.getPath(), "--duster", "--exclude", "y", "--sam-rg", "@RG\\tID:id\\tSM:sm\\tPL:ILLUMINA"}, "@RG\tID:id\tSM:sm\tPL:ILLUMINA", mpsout, mps);
          TestUtils.containsAll(mpsout.toString()
              , "Formatting paired-end FASTA data"
              , "Input Data" + StringUtils.LS
              + "Files              : raw1 raw2" + StringUtils.LS
              + "Format             : FASTA" + StringUtils.LS
              + "Type               : DNA" + StringUtils.LS
              + "Number of pairs    : 2" + StringUtils.LS
              + "Number of sequences: 4" + StringUtils.LS
              + "Total residues     : 20" + StringUtils.LS
              + "Minimum length     : 5" + StringUtils.LS
              + "Maximum length     : 5" + StringUtils.LS
              + StringUtils.LS
              + "Output Data" + StringUtils.LS
              + "SDF-ID             : "
              , "Number of pairs    : 1" + StringUtils.LS
              + "Number of sequences: 2" + StringUtils.LS
              + "Total residues     : 10" + StringUtils.LS
              + "Minimum length     : 5" + StringUtils.LS
              + "Maximum length     : 5" + StringUtils.LS
              + StringUtils.LS
              + "There were 1 pairs skipped due to filters" + StringUtils.LS
              + "There were 1 residues converted from lower case to unknowns" + StringUtils.LS
              );

          assertTrue(outputDir.isDirectory());
          assertTrue(new File(outputDir, "left").isDirectory());
          assertTrue(new File(outputDir, "right").isDirectory());
          assertTrue(new File(new File(outputDir, "left"), "mainIndex").exists());
          assertTrue(new File(new File(outputDir, "right"), "mainIndex").exists());
          final File progress = new File(outputDir, "progress");
          assertTrue(progress.exists());
          final String prog = FileUtils.fileToString(progress);

          TestUtils.containsAll(prog, "Formatting paired-end FASTA data", "paired-end prereader");

      final String s = mps.toString();
      if (s.length() != 0) {
        assertEquals("The current environment (operating system, JVM or machine) has not been tested. There is a risk of performance degradation or failure." + StringUtils.LS, mps.toString());
      }
    }
  }

  public void testUnevenPaired() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File rawLeft = new File(tempDir, "left.fasta");
      final File rawRight = new File(tempDir, "right.fasta");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n@x2\n" + "actgn\n" + "+x2\n" + "ACTGN\n", rawLeft);
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", rawRight);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      final File outputDir = new File(tempDir, JUNITOUT);
      assertEquals(1, new FormatCli().mainInit(new String[] {"-o",  outputDir.getPath(), "-f", "fastq", "-q", "sanger", "-l" , rawLeft.getPath(), "-r", rawRight.getPath()}, out.outputStream(), err.printStream()));

      assertTrue(outputDir.isDirectory());
      assertTrue(new File(outputDir, "left").isDirectory());
      assertTrue(new File(outputDir, "right").isDirectory());
      assertTrue(new File(new File(outputDir, "left"), "mainIndex").exists());
      assertTrue(new File(new File(outputDir, "right"), "mainIndex").exists());
      final String s = err.toString();
      assertTrue(s, s.contains("Invalid input, paired end data must have same number of sequences. Left had: 2 Right had: 1"));
      out.close();
      err.close();
    }
  }


  public void testValidUseCG() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw1 = new File(tempDir, "raw1.fq");
      final File raw2 = new File(tempDir, "raw2.fq");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw1);
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw2);
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final File outputDir = new File(tempDir, JUNITOUT);
          assertEquals(bos.toString(), 0, new FormatCli().mainInit(new String[]{"-o", outputDir.getPath(), "-f", "cgfastq", "-l", raw1.getPath(), "-r", raw2.getPath()}, bout, err));

          assertTrue(bout.toString().contains("Format             : CG"));
          assertTrue(bout.toString().contains("Type               : DNA"));
          assertTrue(bout.toString().contains("SDF-ID"));
          assertTrue(bout.toString().contains("Number of sequences: 2"));
          assertTrue(bout.toString().contains("Total residues     : 10"));
          assertTrue(bout.toString().contains("Minimum length     : 5"));
          assertTrue(bout.toString().contains("Maximum length     : 5"));

          assertTrue(outputDir.isDirectory());
          assertTrue(new File(outputDir, "left").isDirectory());
          assertTrue(new File(outputDir, "right").isDirectory());
          assertTrue(new File(new File(outputDir, "left"), "mainIndex").exists());
          assertTrue(new File(new File(outputDir, "right"), "mainIndex").exists());
        }
      } finally {
        bos.close();
      }
      final String s = bos.toString();
      if (s.length() != 0) {
        assertEquals("The current environment (operating system, JVM or machine) has not been tested. There is a risk of performance degradation or failure." + StringUtils.LS, bos.toString());
      }
    }
  }

  private static final String EXPECTED_PRT_MSG = "" + StringUtils.LS
    + "Format             : FASTA" + StringUtils.LS
    + "Type               : PROTEIN" + StringUtils.LS
    + "Number of sequences: 1" + StringUtils.LS
    + "Total residues     : 22" + StringUtils.LS
    + "Minimum length     : 22" + StringUtils.LS
    + "Maximum length     : 22" + StringUtils.LS;

  public void testValidUseProtein() throws Exception {
    final File raw = File.createTempFile("junit", "test");
    try {
      FileUtils.stringToFile(">x\nX*ARNDCQEGHILKMFPSTWYV\n", raw);
      final File tempDir = FileUtils.createTempDir("junit", "test");
      try {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
          try (PrintStream err = new PrintStream(bos)) {
            final File outputDir = new File(tempDir, JUNITOUT);
            assertEquals(0, new FormatCli().mainInit(new String[]{"-o", outputDir.getPath(), "-p", raw.getPath()}, bout, err));
            final String outStr = bout.toString();
            assertTrue(outStr.contains(EXPECTED_PRT_MSG));
            assertTrue(outputDir.isDirectory());
            assertTrue(new File(outputDir, "mainIndex").exists());
            final File summary = new File(outputDir, "summary.txt");
            assertTrue(summary.exists());
            final String sum = FileUtils.fileToString(summary);
            assertTrue(sum, sum.contains(EXPECTED_PRT_MSG));
            final File outf = File.createTempFile("junit", ".fasta");
            outf.deleteOnExit();
            try {
              final ByteArrayOutputStream out = new ByteArrayOutputStream();
              assertEquals(0, new Sdf2Fasta().mainInit(new String[]{"-o", outf.getPath(), "-i", outputDir.getPath(), "-Z"}, out, err));
              assertEquals(0, out.toString().length());
              assertTrue(outf.exists());
              assertFalse(outf.isDirectory());
              try (BufferedReader r = new BufferedReader(new FileReader(outf))) {
                assertEquals(">x", r.readLine());
                assertEquals("X*ARNDCQEGHILKMFPSTWYV", r.readLine());
                assertNull(r.readLine());
              }
            } finally {
              assertTrue(outf.delete());
            }
          }
        } finally {
          bos.close();
        }
        final String s = bos.toString();
        if (s.length() != 0) {
          assertEquals("The current environment (operating system, JVM or machine) has not been tested. There is a risk of performance degradation or failure." + StringUtils.LS, bos.toString());
        }
      } finally {
        assertTrue(FileHelper.deleteAll(tempDir));
      }
    } finally {
      assertTrue(raw.delete());
    }
  }

  public void testBadUse() throws Exception {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream err = new PrintStream(bos)) {
        assertEquals(1, new FormatCli().mainInit(new String[]{"--no-such-option"}, new ByteArrayOutputStream(), err));
      }
    } finally {
      bos.close();
    }
    final String e = bos.toString();
    assertTrue(e.contains("Error: Unknown flag --no-such-option"));
  }

  private static final String EXPECTED_ERROR2 = "Error: File not found: \"" + File.separator + "in_a_small_hole_lived_a_hobbit\"" + StringUtils.LS
                                            + "Error: There were 1 invalid input file paths" + StringUtils.LS;

  public void testBadFiles() throws Exception {
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          assertEquals(1, new FormatCli().mainInit(new String[]{"-o", "prereadertestshouldnthappen", File.separator + "in_a_small_hole_lived_a_hobbit"}, new ByteArrayOutputStream(), err));
        }
      } finally {
        bos.close();
      }
      final String e = bos.toString();
      assertEquals(e, EXPECTED_ERROR2, e);
    } finally {
      FileHelper.deleteAll(new File("prereadertestshouldnthappen"));
    }
  }

  public void testInputTestingFlags() throws IOException {
    Diagnostic.setLogStream();
    try (TestDirectory dir = new TestDirectory("format")) {
      final File input = new File(dir, "in");
      FileUtils.stringToFile("", input);
      final File outDir = new File(dir, "out");
      TestUtils.containsAll(checkHandleFlagsErr("-o", outDir.getPath()),
          "No input files specified.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", outDir.getPath(), "-l", input.getPath()),
          "Both left and right reads must be specified.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", outDir.getPath(), "-r", input.getPath()),
          "Both left and right reads must be specified.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", outDir.getPath(), "-l", input.getPath(), "-r", input.getPath(), "-p"),
          "Cannot set protein flag when left and right files are specified.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", outDir.getPath(), "-l", input.getPath(), input.getPath()),
          "Either specify individual input files or left and right files, not both.");
      TestUtils.containsAll(checkHandleFlagsErr("-o", outDir.getPath(), "-r", input.getPath(), input.getPath()),
          "Either specify individual input files or left and right files, not both.");
    }
  }

  public void testFormatflag() throws IOException {
    Diagnostic.setLogStream();
    try (TestDirectory que = new TestDirectory("format")) {
      final File f = new File(que, "in");
      FileUtils.stringToFile("", f);
      final String[] args = {
          "-o", "res",
           f.getPath(),
           "-f", "embl"
      };
      final String err = checkHandleFlagsErr(args);
      assertTrue(err, err.contains("Invalid value \"embl\" for \"-f\""));
    }
  }

  public void testOutputflag() throws IOException {
    Diagnostic.setLogStream();
    final File que = FileUtils.createTempDir("format", "pre");
    final File i = new File(que, "test");
    assertTrue(i.createNewFile());
    final String[] args = {
        "-o", que.getPath(),
         i.getPath(),
    };
    final String result = checkHandleFlagsErr(args);
    assertTrue(result, result.replaceAll("\\s+", " ").contains("The directory \"" + que.getPath() + "\" already exists. Please remove it first or choose a different directory."));
    FileHelper.deleteAll(que);
  }

  public void testOutputAsFileflag() throws IOException {
    Diagnostic.setLogStream();
    try (TestDirectory dir = new TestDirectory("format")) {
      File que = new File(dir, "query");
      FileUtils.stringToFile("", que);
      final String[] args = {
        "-o", que.getPath(),
        que.getPath(),
      };
      final String result = checkHandleFlagsErr(args);
      assertTrue(result, result.replaceAll("\\s+", " ").contains("The directory \"" + que.getPath() + "\" already exists. Please remove it first or choose a different directory."));
    }
  }

  public void testSolexa13Format() throws Exception {
    try (TestDirectory tempDir = new TestDirectory("format")) {
      final File outputDir = new File(tempDir, JUNITOUT);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ByteArrayOutputStream err = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(err);
      final File input = new File(tempDir, "in");
      FileUtils.stringToFile("^", input);
      assertEquals(1, new FormatCli().mainInit(new String[] {"-o", outputDir.getPath(), "-f", "fastq", "-q", "solexa1.2", input.getPath()}, out, ps));
      out.flush();
      ps.flush();
      assertTrue("error was: " + err.toString(), err.toString().contains("Invalid value \"solexa1.2\" for \"-q\"."));
      out = new ByteArrayOutputStream();
      err = new ByteArrayOutputStream();
      ps = new PrintStream(err);
      assertEquals(1, new FormatCli().mainInit(new String[] {"-o", outputDir.getPath(), "-f", "fastq", "-q", "illumina", input.getPath()}, out, ps));
      out.flush();
      ps.flush();
      assertTrue("error was: " + err.toString(), err.toString().contains("Unrecognized symbols appeared before label symbol. Last sequence read was: \"<none>\""));
    }
  }

  public void testErrorHandling() throws IOException {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File outputDir = new File(tempDir, JUNITOUT);
      final File raw = new File(tempDir, "test");
      FileUtils.stringToFile(">x\nX*ARNDCQEGHILKMFPSTWYV\n", raw);
      final MemoryPrintStream ps = new MemoryPrintStream();
      assertEquals(1, new FormatCli().mainInit(new String[] {"-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", "-p", raw.getPath()}, ps.outputStream(), ps.printStream()));
      TestUtils.containsAll(ps.toString(), "Incompatible sequence type and file format. format=FASTQ protein=" + true);
      ps.reset();
      final File outputDir2 = new File(tempDir, "blah");
      assertEquals(1, new FormatCli().mainInit(new String[] {"-o", outputDir2.getPath(), "-f", "fasta", "--trim-threshold", "15", "-p",  raw.getPath()}, ps.outputStream(), ps.printStream()));
      TestUtils.containsAll(ps.toString(), "Input must contain qualities to perform quality-based read trimming.");
    }
  }

  public void testGetFormat() {
    assertEquals(InputFormat.FASTA, FormatCli.getFormat("fasta", null, true));
    assertEquals(InputFormat.FASTQ, FormatCli.getFormat("fastq", "sanger", true));
    assertEquals(InputFormat.CG, FormatCli.getFormat("cgfastq", null, true));
    assertEquals(InputFormat.SOLEXA, FormatCli.getFormat("fastq", "solexa", true));
    assertEquals(InputFormat.SOLEXA1_3, FormatCli.getFormat("fastq", "illumina", true));
    try {
      FormatCli.getFormat("blah", null, true);
    } catch (final NoTalkbackSlimException e) {
      assertEquals("Invalid file format=blah", e.getMessage());
    }
  }

  public void testSequenceProcessor() throws IOException {
    final File dir = FileUtils.createTempDir("test", "number");
    try {
      final List<InputStream> list = new ArrayList<>();
      final SequenceProcessor p = new SequenceProcessor(new FastaSequenceDataSource(list, new DNAFastaSymbolTable()), PrereadType.UNKNOWN, PrereadArm.UNKNOWN, dir, null, null, false, null);
      assertEquals(Constants.MAX_FILE_SIZE, p.mWriter.getSizeLimit());
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void testBadFormatCombinationException() throws IOException {
    Diagnostic.setLogStream();
    final PrereadExecutor ex = new PrereadExecutor(true, false, InputFormat.FASTQ, null, null, null, false, false, false, true, null, null, false);
    try {
      ex.performPreread(null);
      fail();
    } catch (final BadFormatCombinationException e) {
      assertEquals("Incompatible sequence type and file format. format=FASTQ protein=" + true, e.getMessage());
    }
  }

  public void testFormattingMessage() {
    testMessage("CGFASTQ", InputFormat.CG);
    testMessage("FASTA", InputFormat.FASTA);
    testMessage("FASTQ", InputFormat.FASTQ, InputFormat.SOLEXA, InputFormat.SOLEXA1_3);
    testMessage("SAM/BAM", InputFormat.SAM_PE, InputFormat.SAM_SE);
  }

  private void testMessage(String formatString, InputFormat... types) {
    for (InputFormat format : types) {
      assertEquals("Formatting paired-end " + formatString + " data", FormatCli.PrereadExecutor.formattingMessage(true, format));
      assertEquals("Formatting " + formatString + " data", FormatCli.PrereadExecutor.formattingMessage(false, format));
    }
  }


  public void testSamRGFormat() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      final String[] args = {input.getPath(), "-o", output.getPath(), "-f", "sam-pe"};
      final String expected = "@RG\tID:rg1\tSM:sm1\tPL:ILLUMINA";
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      runFormat(output, args, expected, out, err);
    }
  }

  public void testSamRGFormatCustom() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      final String expected = "@RG\tID:rg999\tPL:NOT_ILLUMINA\tSM:sample";
      final String custom = expected.replaceAll("\t", "\\\\t");
      final String[] args = {input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--sam-rg", custom};
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      runFormat(output, args, expected, out, err);
    }
  }

  private void runFormat(File output, String[] args, String expected, MemoryPrintStream out, MemoryPrintStream err) throws IOException {
    final int errorCode = new FormatCli().mainInit(args, out.outputStream(), err.printStream());
    assertEquals(err.toString(), 0, errorCode);
    final DefaultSequencesReader leftReader = new DefaultSequencesReader(new File(output, "left"), LongRange.NONE);
    assertEquals(expected, leftReader.samReadGroup());
    final DefaultSequencesReader rightReader = new DefaultSequencesReader(new File(output, "right"), LongRange.NONE);
    assertEquals(expected, rightReader.samReadGroup());
  }

  public void testSamRGFormatMultiple() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      final String expected = "@RG\tID:rg2\tSM:doggy\tPL:Sturdy";
      final String[] args = {input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg2"};
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      runFormat(output, args, expected, out, err);
    }
  }

  public void testDedupSecondary() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      String err = checkMainInitWarn(input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg1", "--Xdedup-secondary-alignments");
      TestUtils.containsAll(err, "1 reads missing a pair");
      try (final DefaultSequencesReader rightReader = new DefaultSequencesReader(new File(output, "right"), LongRange.NONE)) {
        assertEquals(2, rightReader.numberSequences());

        final StringBuilder sb = new StringBuilder();
        final SequencesIterator it = rightReader.iterator();
        while (it.nextSequence()) {
          sb.append(it.currentName());
        }
        TestUtils.containsAll(sb.toString(), "48218590", "48851323");
      }
    }
  }
  public void testHandleDupAndSupplemental() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated-dups.sam", input);
      String err = checkMainInitWarn(input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg1", "--Xdedup-secondary-alignments");
      TestUtils.containsAll(err, "Read 48851323 is duplicated in SAM input", "1 reads missing a pair", "1 records ignored as duplicates in input");

      try (final DefaultSequencesReader rightReader = new DefaultSequencesReader(new File(output, "right"), LongRange.NONE)) {
        assertEquals(2, rightReader.numberSequences());

        final StringBuilder sb = new StringBuilder();
        final SequencesIterator it = rightReader.iterator();
        while (it.nextSequence()) {
          sb.append(it.currentName());
        }
        TestUtils.containsAll(sb.toString(), "48218590", "48851323");
      }
    }
  }

  public void testUnmatchableReadGroup() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      String err = checkMainInitBadFlags(input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "I am not here");
      TestUtils.containsAll(err, "No read group information matching \"I am not here\" present in the input file");
    }
  }

  public void testMultifile() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File input2 = new File(dir, "input2.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input2);
      final String[] args = {input.getPath(), input2.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg1"};
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      final String expected = "@RG\tID:rg1\tSM:sm1\tPL:ILLUMINA";
      runFormat(output, args, expected, out, err);
    }
  }
  public void testMultifileMissingFromSecond() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File input2 = new File(dir, "input2.sam");
      final File output = new File(dir, "output");
      final String[] args = {input2.getPath(), input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg2"};
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input2);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      final int errorCode = new FormatCli().mainInit(args, out.outputStream(), err.printStream());
      assertFalse(err.toString(), errorCode == 0);
      TestUtils.containsAll(err.toString(), "No read group information matching \"rg2\" present in the input file ");
    }
  }

  public void testSamRGFormatMultipleFail() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      final String[] args = {input.getPath(), "-o", output.getPath(), "-f", "sam-pe"};
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream err = new MemoryPrintStream();
      final int errorCode = new FormatCli().mainInit(args, out.outputStream(), err.printStream());
      assertFalse(err.toString(), errorCode == 0);
      TestUtils.containsAll(err.toString(), "Multiple read group information present in the input file");
    }
  }
}
