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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Test class for corresponding class.
 */
public class Sdf2FastaTest extends AbstractCliTest {


  @Override
  protected AbstractCli getCli() {
    return new Sdf2Fasta();
  }

  private static final String JUNITOUT = ".junitout";

  private void compareToFile(final String str, final File f) throws IOException {
    final String main = FileUtils.fileToString(f);
    assertEquals(str, main);
  }

  private void checkContent1(File x) throws Exception {
    assertTrue(x.exists());
    //final BufferedReader r = new BufferedReader(new FileReader(x));
    try {
      compareToFile(">test" + StringUtils.LS + "ACGT" + StringUtils.LS + ">bob" + StringUtils.LS + "TAGTACCC" + StringUtils.LS + ">cat" + StringUtils.LS + "CAT" + StringUtils.LS + ">dog" + StringUtils.LS + "CCC" + StringUtils.LS, x);
    } finally {
      assertTrue(x.delete());
    }
  }

  private void checkContent2(File x) throws Exception {
    assertTrue(x.exists());
    try {
      compareToFile(">test" + StringUtils.LS + "ACG" + StringUtils.LS + "T" + StringUtils.LS + ">bob" + StringUtils.LS + "TAG" + StringUtils.LS + "TAC" + StringUtils.LS + "CC" + StringUtils.LS + ">cat" + StringUtils.LS + "CAT" + StringUtils.LS + ">dog" + StringUtils.LS + "CCC" + StringUtils.LS, x);
    } finally {
      assertTrue(x.delete());
    }
  }

  public void testWorks() throws Exception {
    final File xd = FileHelper.createTempDirectory();
    final File fastaDirectory = FileHelper.createTempDirectory();
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(new ByteArrayInputStream(">test\nacgt\n>bob\ntagt\naccc\n>cat\ncat\n>dog\nccc".getBytes()));
      final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
      final SequencesWriter sw = new SequencesWriter(ds, xd, 300000, PrereadType.UNKNOWN, false);
      sw.processSequences();
      File x;
      SequencesReader sr = SequencesReaderFactory.createDefaultSequencesReader(xd);
      try {
        x = new File(fastaDirectory, "junitmy.fasta");
        checkMainInitOk("-i", xd.toString(), "-o", x.toString(), "-Z");
        checkContent1(x);
      } finally {
        sr.close();
      }
      sr = SequencesReaderFactory.createDefaultSequencesReader(xd);
      try {
        checkMainInitOk("-i", xd.toString(), "-o", x.toString(), "-l", "3", "-Z");
        checkContent2(x);
      } finally {
        sr.close();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(xd));
      assertTrue(FileHelper.deleteAll(fastaDirectory));
    }
  }

  public void testBadArgs() throws Exception {
    final File xd = FileHelper.createTempDirectory();
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
      final SequencesWriter sw = new SequencesWriter(ds, xd, 300000, PrereadType.UNKNOWN, false);
      sw.processSequences();
      checkHandleFlagsErr("-i", xd.toString(), "-o", "junithi", "-l", "-1");
    } finally {
      //      assertTrue(new File("junithi.fasta").delete());
      assertTrue(FileHelper.deleteAll(xd));
    }
  }

  public void testHelp() {
    checkHelp("SDF containing sequences",
      "output filename (extension added if not present)",
      "maximum number of nucleotides");
  }

  private void checkContent(final String name, final String content) throws Exception {
    final File f = new File(name);
    assertTrue(f.exists());
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      assertEquals(">x", r.readLine());
      assertEquals(content, r.readLine());
      assertNull(r.readLine());
    }
  }

  private void checkString(final String s) {
    if (s.length() != 0) {
      assertEquals("The current environment (operating system, JVM or machine) has not been tested. There is a risk of performance degradation or failure." + StringUtils.LS, s);
    }
  }

  private void runCommandWithNamedOutput(final String name, final String pathpr, final String content) throws Exception {
    final File tempDir = FileUtils.createTempDir("PrereadToFasta", null);
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      final File output = new File(tempDir, name);
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          assertEquals(0, new Sdf2Fasta().mainInit(new String[]{"-o", output.getPath(), "-i", pathpr, "-Z"}, out, err));
          assertEquals(0, out.toString().length());
          if (name.toLowerCase(Locale.getDefault()).endsWith(".fa") || name.toLowerCase(Locale.getDefault()).endsWith(".fasta")) {
            checkContent(new File(tempDir, name).getPath(), content);
          } else {
            checkContent(new File(tempDir, JUNITOUT + ".fasta").getPath(), content);
          }
        }
      } finally {
        bos.close();
      }
      checkString(bos.toString());
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  private void runCommandWithNamedOutput(final String name, final String pathpr, final String contentLeft, String contentRight) throws Exception {
    final File tempDir = FileUtils.createTempDir("PrereadToFasta", null);
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      final File output = new File(tempDir, name);
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          assertEquals(0, new Sdf2Fasta().mainInit(new String[]{"-o", output.getPath(), "-i", pathpr, "-Z"}, out, err));
          assertEquals(0, out.toString().length());
          if (name.toLowerCase(Locale.getDefault()).endsWith(".fa") || name.toLowerCase(Locale.getDefault()).endsWith(".fasta")) {
            final String ext = name.substring(name.lastIndexOf('.'));
            checkContent(new File(tempDir, JUNITOUT + "_1" + ext).getPath(), contentLeft);
            checkContent(new File(tempDir, JUNITOUT + "_2" + ext).getPath(), contentRight);
          } else {
            checkContent(new File(tempDir, JUNITOUT + "_1.fasta").getPath(), contentLeft);
            checkContent(new File(tempDir, JUNITOUT + "_2.fasta").getPath(), contentRight);
          }
        }
      } finally {
        bos.close();
      }
      checkString(bos.toString());
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  private void runCommandLineLength2(final String pathpr) throws Exception {
    final File tempDir = FileUtils.createTempDir("PrereadToFasta", null);
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          assertEquals(0, new Sdf2Fasta().mainInit(new String[]{"-o", new File(tempDir, JUNITOUT).getPath(), "-i", pathpr, "-l", "2", "-Z"}, out, err));
          assertEquals(0, out.toString().length());
          final File f = new File(tempDir, JUNITOUT + ".fasta");
          assertTrue(f.exists());
          compareToFile(">x" + StringUtils.LS + "AC" + StringUtils.LS + "TG" + StringUtils.LS + "N" + StringUtils.LS, f);
        }
      } finally {
        bos.close();
      }
      checkString(bos.toString());
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }
  private void createPreread(final String s, final File dir) throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(new ByteArrayInputStream(s.getBytes()));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    new SequencesWriter(ds, dir, 100000, PrereadType.UNKNOWN, false).processSequences();
  }

  private void createPrereadProtein(final File dir) throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(new ByteArrayInputStream((">x" + StringUtils.LS + "X*ARNDCQEGHILKMFPSTWYV" + StringUtils.LS).getBytes()));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new ProteinFastaSymbolTable());
    new SequencesWriter(ds, dir, 100000, PrereadType.UNKNOWN, false).processSequences();
  }

  public void testValidUse() throws Exception {
    final File dir = FileHelper.createTempDirectory();
    try {
      final File outDir = FileHelper.createTempDirectory();
      try {
        createPreread(">x" + StringUtils.LS + "actgn" + StringUtils.LS, dir);
        final String pathpr = dir.getPath();
        try {
          runCommandWithNamedOutput(JUNITOUT, pathpr, "ACTGN");
          runCommandWithNamedOutput(JUNITOUT + ".FA", pathpr, "ACTGN");
          runCommandWithNamedOutput(JUNITOUT + ".fasta", pathpr, "ACTGN");
          runCommandLineLength2(pathpr);
        } finally {
          FileHelper.deleteAll(dir);
        }
        createPrereadProtein(dir);
        try {
          runCommandWithNamedOutput(JUNITOUT, pathpr, "X*ARNDCQEGHILKMFPSTWYV");
        } finally {
          FileHelper.deleteAll(dir);
        }
      } finally {
      assertTrue(!outDir.exists() || FileHelper.deleteAll(outDir));
      }
    } finally {
      assertTrue(!dir.exists() || FileHelper.deleteAll(dir));
    }
  }

  public void testValidUse2() throws Exception {
    final File dir = FileHelper.createTempDirectory();
    createPreread(">x" + StringUtils.LS + "actgn" + StringUtils.LS, new File(dir, "left"));
    createPreread(">x" + StringUtils.LS + "actgn" + StringUtils.LS, new File(dir, "right"));
    final String pathpr = dir.getPath();
    try {
      runCommandWithNamedOutput(JUNITOUT, pathpr, "ACTGN", "ACTGN");
      runCommandWithNamedOutput(JUNITOUT + ".FA", pathpr, "ACTGN", "ACTGN");
      runCommandWithNamedOutput(JUNITOUT + ".fasta", pathpr, "ACTGN", "ACTGN");
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testLineFlag() throws IOException {
    final String err = checkHandleFlagsErr("-o", "testFile", "-i", "pf2", "-l", "-5");
    assertTrue(err.contains("Error: Expected a nonnegative integer for parameter \"line-length\"."));
  }

  public void testInputAsFile() throws IOException {
    final File que = File.createTempFile("p2f", "flag");
    final String err = checkMainInitBadFlags("-o", "testFile", "-i", que.getPath());
    assertTrue(err.contains("Error: The specified file, \"" + que.getPath() + "\", is not an SDF."));
    assertTrue(FileHelper.deleteAll(que));
  }

  private static final String FULL_NAME_DATA = ""
          + ">name suffix" + StringUtils.LS
          + "ACGTCG" + StringUtils.LS
          + ">second suffix" + StringUtils.LS
          + "ACGGGT" + StringUtils.LS;

  public void testFullName() throws IOException {
    final File dir = FileUtils.createTempDir("testsdf2fasta", "fullname");
    try {
      final File sdf = ReaderTestUtils.getDNADir(FULL_NAME_DATA, new File(dir, "sdf"));
      final File fasta = new File(dir, "fs.fasta.gz");
      checkMainInitOk("-i", sdf.getPath(), "-o", fasta.getPath());
      assertEquals(FULL_NAME_DATA, FileHelper.gzFileToString(fasta));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testTaxonomySDF() throws IOException {
    try (final TestDirectory dir = new TestDirectory("sdfstats")) {

      // One without tax
      final File smallSdf = ReaderTestUtils.getDNADir(dir);
      String err = checkMainInitBadFlags("-i", smallSdf.getPath(), "-o", "-", "--taxons", "41431");
      TestUtils.containsAll(err, "does not contain taxonomy");

      // One with tax
      final File fullSdf = new File(dir, "sdf_full");
      final File faOut = new File(dir, "ex.fasta.gz");
      makeTestTaxonSdf(fullSdf);

      checkMainInitOk("-i", fullSdf.getPath(), "-o", faOut.getPath(), "--taxons", "41431");

      String faout = FileHelper.gzFileToString(faOut);
      TestUtils.containsAll(faout,
        "gi|218169684|gb|CP001289.1|",
        "gi|218165370|gb|CP001287.1|",
        "gi|218169729|gb|CP001290.1|",
        "gi|218169631|gb|CP001288.1|");
    }
  }

  public static void makeTestTaxonSdf(File dest) throws IOException {
    final String sequences = FileHelper.resourceToString("com/rtg/reader/resources/sequences.fasta");
    ReaderTestUtils.getReaderDNA(sequences, dest, null).close();

    // cp taxonomy files to sdf
    final File taxonomy = new File(dest, "taxonomy.tsv");
    FileHelper.resourceToFile("com/rtg/reader/resources/taxonomy.tsv", taxonomy);
    final File taxonomyLookup = new File(dest, "taxonomy_lookup.tsv");
    FileHelper.resourceToFile("com/rtg/reader/resources/taxonomy_lookup.tsv", taxonomyLookup);
  }
}

