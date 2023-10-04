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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.reader.FormatCli.BadFormatCombinationException;
import com.rtg.reader.FormatCli.PrereadExecutor;
import com.rtg.reader.FormatCli.PrereadExecutor.SequenceProcessor;
import com.rtg.util.Constants;
import com.rtg.util.License;
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

/**
 * Test class for corresponding class.
 */
public class FormatCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new FormatCli();
  }

  public void testHelp() {
    checkHelp("rtg format"
        , "Converts the contents of sequence data files (FASTA/FASTQ/SAM/BAM) into the RTG Sequence Data File (SDF) format."
        , "-f,", "--format=FORMAT", "format of input. Allowed values are [fasta, fastq"
        , "-q", "--quality-format=FORMAT", "quality data encoding", "Allowed values are [sanger, solexa, illumina] (Default is sanger)"
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
      checkHandleFlagsOut("-p", "-o", nsd.getPath(), "-f", "fastq", "-q", "sanger", xx.getPath());
      final CFlags flags = getCFlags();
      assertTrue(flags.isSet("protein"));
      assertEquals("fastq", flags.getValue("format"));
      assertEquals(nsd.getPath(), ((File) flags.getValue("output")).getPath());
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
    + "Mean length        : 5" + StringUtils.LS
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
      testValidUse(new String[]{"-o", outputDir.getPath(), "-I", list.getPath(), "--duster", "--exclude", "y"}, outputDir);
    }
  }

  private void testValidUse(String[] args, File outputDir) throws IOException {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final String outStr = checkMainInitOk(args);
      TestUtils.containsAll(outStr, EXPECTED_MSG
        , "Formatting FASTA data"
        , "Input Data"
        , "Files              : "
        , StringUtils.LS + StringUtils.LS + "Output Data"
        , "SDF-ID             : "
        , "Number of sequences: 1" + StringUtils.LS
        + "Total residues     : 5" + StringUtils.LS
        + "Minimum length     : 5" + StringUtils.LS
        + "Mean length        : 5" + StringUtils.LS
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
      final File outf = new File(tempDir, "out.fasta");
      MainResult.run(new Sdf2Fasta(), "-o", outf.getPath(), "-i", outputDir.getPath(), "-Z");
      assertTrue(outf.exists());
      assertFalse(outf.isDirectory());
      try (BufferedReader r = new BufferedReader(new FileReader(outf))) {
        assertEquals(">x", r.readLine());
        assertEquals("NCTGN", r.readLine());
        assertNull(r.readLine());
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

  public void testValidUseQ() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "raw");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw);
      final File outputDir = new File(tempDir, JUNITOUT);
      final String bout = checkMainInitOk("-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", raw.getPath());
      mNano.check("format-useq.txt", StringUtils.grepMinusV(bout, "SDF-ID|Processing|Formatting FASTQ"));
      assertTrue(outputDir.isDirectory());
      assertTrue(new File(outputDir, "mainIndex").exists());
      final File summary = new File(outputDir, "summary.txt");
      assertTrue(summary.exists());
      final String sum = FileUtils.fileToString(summary);
      mNano.check("format-useq.txt", StringUtils.grepMinusV(sum, "SDF-ID"));

      final File outf = new File(tempDir, "out.fasta");
      //System.err.println("bos 1 ' " + bos.toString());
      MainResult res = MainResult.run(new SdfStatistics(), outputDir.getAbsolutePath());
      assertEquals(res.err(), 0, res.rc());
      assertTrue(res.out().contains("Sequence names     : yes"));
      assertTrue(res.out().contains("Residue qualities  : yes"));

      res = MainResult.run(new Sdf2Fasta(), "-o", outf.getPath(), "-i", outputDir.getPath(), "-Z");
      assertEquals(res.err(), 0, res.rc());
      assertEquals(0, res.out().length());
      assertTrue(outf.exists());
      assertFalse(outf.isDirectory());
      try (BufferedReader r = new BufferedReader(new FileReader(outf))) {
        assertEquals(">x", r.readLine());
        assertEquals("ACTGN", r.readLine());
        assertNull(r.readLine());
      }
    }
  }

  public void testNoNames() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "raw");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw);
      final File outputDir = new File(tempDir, JUNITOUT);
      checkMainInitOk("-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", raw.getPath(), "--no-names");
      final MainResult res = MainResult.run(new SdfStatistics(), outputDir.getAbsolutePath());
      assertEquals(res.err(), 0, res.rc());
      assertTrue(res.out(), res.out().contains("Sequence names     : no"));
    }
  }

  public void testNoQuality() throws Exception  {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "raw");
      FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw);
      final File outputDir = new File(tempDir, JUNITOUT);
      checkMainInitOk("-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", raw.getPath(), "--no-quality");
      final MainResult res = MainResult.run(new SdfStatistics(), outputDir.getAbsolutePath());
      assertEquals(res.err(), 0, res.rc());
      assertTrue(res.out(), res.out().contains("Residue qualities  : no"));
    }
  }

  public void testValidUsePaired() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File raw1 = new File(tempDir, "raw1");
      final File raw2 = new File(tempDir, "raw2");
      FileUtils.stringToFile(">x\n" + "aCTGN\n>y\nACTGN\n", raw1);
      FileUtils.stringToFile(">x\n" + "ACTGN\n>y\nACTGN\n", raw2);

      final File outputDir = new File(tempDir, JUNITOUT);
      final MainResult res = runFormat(outputDir, "@RG\tID:id\tSM:sm\tPL:ILLUMINA",
        "-o",  outputDir.getPath(), "-f", "fasta", "-l" , raw1.getPath(), "-r", raw2.getPath(), "--duster", "--exclude", "y", "--sam-rg", "@RG\\tID:id\\tSM:sm\\tPL:ILLUMINA");
      TestUtils.containsAll(res.out()
        , "Formatting paired-end FASTA data"
        , "Input Data" + StringUtils.LS
          + "Files              : raw1 raw2" + StringUtils.LS
          + "Format             : paired-end FASTA" + StringUtils.LS
          + "Type               : DNA" + StringUtils.LS
          + "Number of pairs    : 2" + StringUtils.LS
          + "Number of sequences: 4" + StringUtils.LS
          + "Total residues     : 20" + StringUtils.LS
          + "Minimum length     : 5" + StringUtils.LS
          + "Mean length        : 5" + StringUtils.LS
          + "Maximum length     : 5" + StringUtils.LS
          + StringUtils.LS
          + "Output Data" + StringUtils.LS
          + "SDF-ID             : "
        , "Number of pairs    : 1" + StringUtils.LS
          + "Number of sequences: 2" + StringUtils.LS
          + "Total residues     : 10" + StringUtils.LS
          + "Minimum length     : 5" + StringUtils.LS
          + "Mean length        : 5" + StringUtils.LS
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
      TestUtils.containsAll(prog, "Formatting paired-end FASTA data", "paired-end SDF");
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
      final String s = checkMainInitBadFlags("-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", "-l", rawLeft.getPath(), "-r", rawRight.getPath());

      assertTrue(outputDir.isDirectory());
      assertTrue(new File(outputDir, "left").isDirectory());
      assertTrue(new File(outputDir, "right").isDirectory());
      assertTrue(new File(new File(outputDir, "left"), "mainIndex").exists());
      assertTrue(new File(new File(outputDir, "right"), "mainIndex").exists());
      assertTrue(s, s.contains("Invalid input, paired end data must have same number of sequences. Left had: 2 Right had: 1"));
      out.close();
      err.close();
    }
  }


  public void testValidUseCG() throws Exception {
    if (License.isDeveloper()) {
      try (final TestDirectory tempDir = new TestDirectory("format")) {
        final File raw1 = new File(tempDir, "raw1.fq");
        final File raw2 = new File(tempDir, "raw2.fq");
        FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw1);
        FileUtils.stringToFile("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n", raw2);

        final File outputDir = new File(tempDir, JUNITOUT);
        final String out = checkMainInitOk("-o", outputDir.getPath(), "-f", "fastq-cg", "-l", raw1.getPath(), "-r", raw2.getPath());
        assertTrue(out.contains("Format             : paired-end FASTQ-CG"));
        assertTrue(out.contains("Type               : DNA"));
        assertTrue(out.contains("SDF-ID"));
        assertTrue(out.contains("Number of sequences: 2"));
        assertTrue(out.contains("Total residues     : 10"));
        assertTrue(out.contains("Minimum length     : 5"));
        assertTrue(out.contains("Maximum length     : 5"));

        assertTrue(outputDir.isDirectory());
        assertTrue(new File(outputDir, "left").isDirectory());
        assertTrue(new File(outputDir, "right").isDirectory());
        assertTrue(new File(new File(outputDir, "left"), "mainIndex").exists());
        assertTrue(new File(new File(outputDir, "right"), "mainIndex").exists());
      }
    }
  }

  public void testValidUseProtein() throws Exception {
    try (TestDirectory tempDir = new TestDirectory("format")) {
      final File raw = new File(tempDir, "in.fasta");
      FileUtils.stringToFile(">x\nX*ARNDCQEGHILKMFPSTWYV\n", raw);
      final File outputDir = new File(tempDir, JUNITOUT);
      final String outStr = checkMainInitOk("-o", outputDir.getPath(), "-p", raw.getPath());
      mNano.check("format-useprotein.txt", StringUtils.grepMinusV(outStr, "SDF-ID|Processing|Formatting FASTA"));
      assertTrue(outputDir.isDirectory());
      assertTrue(new File(outputDir, "mainIndex").exists());
      final File summary = new File(outputDir, "summary.txt");
      assertTrue(summary.exists());
      final String sum = FileUtils.fileToString(summary);
      mNano.check("format-useprotein.txt", StringUtils.grepMinusV(sum, "SDF-ID"));
      final File outf = new File(tempDir, "out.fasta");
      final MainResult res = MainResult.run(new Sdf2Fasta(), "-o", outf.getPath(), "-i", outputDir.getPath(), "-Z");
      assertEquals(0, res.out().length());
      assertTrue(outf.exists());
      assertFalse(outf.isDirectory());
      try (BufferedReader r = new BufferedReader(new FileReader(outf))) {
        assertEquals(">x", r.readLine());
        assertEquals("X*ARNDCQEGHILKMFPSTWYV", r.readLine());
        assertNull(r.readLine());
      }
    }
  }

  public void testBadUse() {
    final String e = checkMainInitBadFlags("--no-such-option");
    assertTrue(e.contains("Error: Unknown flag --no-such-option"));
  }

  private static final String EXPECTED_ERROR2 = "Error: File not found: \"" + File.separator + "in_a_small_hole_lived_a_hobbit\"" + StringUtils.LS
                                            + "Error: There were 1 invalid input file paths" + StringUtils.LS;

  public void testBadFiles() throws Exception {
    try (TestDirectory dir = new TestDirectory("format")) {
      final File out = new File(dir, "prereadertestshouldnthappen");
      final String e = checkMainInitBadFlags("-o", out.getPath(), File.separator + "in_a_small_hole_lived_a_hobbit");
      assertEquals(e, EXPECTED_ERROR2, e);
    }
  }

  public void testInputTestingFlags() throws IOException {
    Diagnostic.setLogStream();
    try (TestDirectory dir = new TestDirectory("format")) {
      final File input = new File(dir, "in");
      FileUtils.stringToFile("", input);
      final File outDir = new File(dir, "out");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", outDir.getPath()), "No input files specified.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", outDir.getPath(), "-l", input.getPath()), "Both left and right reads must be specified.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", outDir.getPath(), "-r", input.getPath()), "Both left and right reads must be specified.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", outDir.getPath(), "-l", input.getPath(), "-r", input.getPath(), "-p"), "Cannot set protein flag when left and right files are specified.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", outDir.getPath(), "-l", input.getPath(), input.getPath()), "Either specify individual input files or left and right files, not both.");
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", outDir.getPath(), "-r", input.getPath(), input.getPath()), "Either specify individual input files or left and right files, not both.");
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
      assertTrue(err, err.contains("Invalid value \"embl\" for flag -f"));
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
    TestUtils.containsAllUnwrapped(result, "The directory \"" + que.getPath() + "\" already exists. Please remove it first or choose a different directory.");
    FileHelper.deleteAll(que);
  }

  public void testOutputAsFileflag() throws IOException {
    Diagnostic.setLogStream();
    try (TestDirectory dir = new TestDirectory("format")) {
      final File que = new File(dir, "query");
      FileUtils.stringToFile("", que);
      final String[] args = {
        "-o", que.getPath(),
        que.getPath(),
      };
      final String result = checkHandleFlagsErr(args);
      TestUtils.containsAllUnwrapped(result, "The directory \"" + que.getPath() + "\" already exists. Please remove it first or choose a different directory.");
    }
  }

  public void testSolexa13Format() throws Exception {
    try (TestDirectory tempDir = new TestDirectory("format")) {
      final File outputDir = new File(tempDir, JUNITOUT);
      final File input = new File(tempDir, "in");
      FileUtils.stringToFile("^", input);
      String err = checkMainInitBadFlags("-o", outputDir.getPath(), "-f", "fastq", "-q", "solexa1.2", input.getPath());
      assertTrue("error was: " + err, err.contains("Invalid value \"solexa1.2\" for flag -q."));

      err = checkMainInitBadFlags("-o", outputDir.getPath(), "-f", "fastq", "-q", "illumina", input.getPath());
      assertTrue("error was: " + err, err.contains("Unrecognized symbols appeared before label symbol. Last sequence read was: \"<none>\""));
    }
  }

  public void testErrorHandling() throws IOException {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File outputDir = new File(tempDir, JUNITOUT);
      final File raw = new File(tempDir, "test");
      FileUtils.stringToFile(">x\nX*ARNDCQEGHILKMFPSTWYV\n", raw);
      String err = checkMainInitBadFlags("-o", outputDir.getPath(), "-f", "fastq", "-q", "sanger", "-p", raw.getPath());
      TestUtils.containsAll(err, "Incompatible sequence type and file format. format=FASTQ protein=" + true);
      final File outputDir2 = new File(tempDir, "blah");
      err = checkMainInitBadFlags("-o", outputDir2.getPath(), "-f", "fasta", "--trim-threshold", "15", "-p",  raw.getPath());
      TestUtils.containsAll(err, "Input must contain qualities to perform quality-based read trimming.");
    }
  }

  public void testGetFormat() {
    assertEquals(SourceFormat.FASTA, FormatCli.getFormat("fasta", null, true, false).getSourceFormat());
    assertEquals(SourceFormat.FASTQ, FormatCli.getFormat("fastq", "sanger", true, false).getSourceFormat());
    assertEquals(QualityFormat.SOLEXA, FormatCli.getFormat("fastq", "solexa", true, false).getQualityFormat());
    assertEquals(QualityFormat.SOLEXA1_3, FormatCli.getFormat("fastq", "illumina", true, false).getQualityFormat());
    try {
      FormatCli.getFormat("blah", null, true, false);
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

  private static final DataSourceDescription FASTQ_DS = new DataSourceDescription(SourceFormat.FASTQ, QualityFormat.SANGER, false, false, false);

  public void testBadFormatCombinationException() throws IOException {
    Diagnostic.setLogStream();
    final PrereadExecutor ex = new PrereadExecutor(true, false, FASTQ_DS, null, null, null, false, false, false, true, null, null, false);
    try {
      ex.performPreread(null);
      fail();
    } catch (final BadFormatCombinationException e) {
      assertEquals("Incompatible sequence type and file format. format=FASTQ protein=" + true, e.getMessage());
    }
  }

  public void testFormattingMessage() {
    testMessage("FASTQ-CG", new DataSourceDescription(SourceFormat.FASTQ, QualityFormat.UNKNOWN, true, false, true));
    testMessage("FASTA", new DataSourceDescription(SourceFormat.FASTA, QualityFormat.UNKNOWN, false, false, false));
    testMessage("FASTQ", FASTQ_DS);
    testMessage("FASTQ-SOLEXA", new DataSourceDescription(SourceFormat.FASTQ, QualityFormat.SOLEXA, false, false, false));
    testMessage("FASTQ-SOLEXA1_3", new DataSourceDescription(SourceFormat.FASTQ, QualityFormat.SOLEXA1_3, false, false, false));
    testMessage("SAM/BAM", new DataSourceDescription(SourceFormat.SAM, QualityFormat.SANGER, true, false, false));
    testMessage("SAM/BAM", new DataSourceDescription(SourceFormat.SAM, QualityFormat.SANGER, false, false, false));
  }

  private void testMessage(String formatString, DataSourceDescription desc) {
    if (desc.isPairedEnd()) {
      assertEquals("Formatting paired-end " + formatString + " data", FormatCli.PrereadExecutor.formattingMessage(desc));
    } else {
      assertEquals("Formatting " + formatString + " data", FormatCli.PrereadExecutor.formattingMessage(desc));
    }
  }


  public void testSamRGFormat() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input);
      runFormat(output, "@RG\tID:rg1\tSM:sm1\tPL:ILLUMINA",
        input.getPath(), "-o", output.getPath(), "-f", "sam-pe");
    }
  }

  public void testSamRGFormatCustom() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      final String custom = "@RG\tID:rg999\tPL:NOT_ILLUMINA\tSM:sample".replaceAll("\t", "\\\\t");
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input);
      runFormat(output, "@RG\tID:rg999\tPL:NOT_ILLUMINA\tSM:sample",
        input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--sam-rg", custom);
    }
  }

  private MainResult runFormat(File output, String expected, String... args) throws IOException {
    final MainResult res = checkMainInit(args);
    final DefaultSequencesReader leftReader = new DefaultSequencesReader(new File(output, "left"), LongRange.NONE);
    assertEquals(expected, leftReader.samReadGroup());
    final DefaultSequencesReader rightReader = new DefaultSequencesReader(new File(output, "right"), LongRange.NONE);
    assertEquals(expected, rightReader.samReadGroup());
    return res;
  }

  public void testSamRGFormatMultiple() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      runFormat(output, "@RG\tID:rg2\tSM:doggy\tPL:Sturdy",
        input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg2");
    }
  }

  public void testDedupSecondary() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      final String err = checkMainInitWarn(input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg1", "--Xdedup-secondary-alignments");
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
      final String err = checkMainInitWarn(input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg1", "--Xdedup-secondary-alignments");
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
      final String err = checkMainInitBadFlags(input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "I am not here");
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
      runFormat(output, "@RG\tID:rg1\tSM:sm1\tPL:ILLUMINA",
        input.getPath(), input2.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg1");
    }
  }
  public void testMultifileMissingFromSecond() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File input2 = new File(dir, "input2.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      FileHelper.resourceToFile("com/rtg/sam/resources/mated.sam", input2);
      final String err = checkMainInitBadFlags(input2.getPath(), input.getPath(), "-o", output.getPath(), "-f", "sam-pe", "--select-read-group", "rg2");
      TestUtils.containsAll(err, "No read group information matching \"rg2\" present in the input file ");
    }
  }

  public void testSamRGFormatMultipleFail() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File input = new File(dir, "input.sam");
      final File output = new File(dir, "output");
      FileHelper.resourceToFile("com/rtg/reader/resources/mated.sam", input);
      final String err = checkMainInitBadFlags(input.getPath(), "-o", output.getPath(), "-f", "sam-pe");
      TestUtils.containsAll(err, "Multiple read groups present in the input file");
    }
  }

  public void testInterleavedFastq() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("format")) {
      final File input = new File(tempDir, "input.fastq");
      FileHelper.resourceToFile("com/rtg/reader/resources/interleaved.fastq", input);
      final File output = new File(tempDir, "output");
      final MainResult res = checkMainInit("-o",  output.getPath(), "-f", "fastq-interleaved", input.getPath());
      mNano.check("format-interleaved.txt", StringUtils.grepMinusV(res.out(), "SDF-ID|Processing"));

      assertTrue(output.isDirectory());
      assertTrue(new File(output, "left").isDirectory());
      assertTrue(new File(output, "right").isDirectory());
      assertTrue(new File(new File(output, "left"), "mainIndex").exists());
      assertTrue(new File(new File(output, "right"), "mainIndex").exists());
      final File progress = new File(output, "progress");
      assertTrue(progress.exists());
      final String prog = FileUtils.fileToString(progress);
      TestUtils.containsAll(prog, "Formatting interleaved paired-end FASTQ data", "Finished successfully");
    }
  }

}
