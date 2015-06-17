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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.reader.FastqSequenceDataSource.FastQScoreType;
import com.rtg.util.Constants;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.cli.Flag;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.SimpleArchive;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Tests for <code>PrereadStatistics</code> class.
 *
 */
public class SdfStatisticsTest extends AbstractCliTest {

  public void testHelp() {
    checkHelp("rtg sdfstats"
        , "Print statistics that describe a directory of SDF formatted data."
        , "SDF+", "SDF directories. Must be specified 1 or more times"
        , "--lengths", "print out the name and length of each sequence. (Not recommended for read sets)"
        , "-p,", "--position", "only display info about unknown bases (Ns) by read position"
        , "-q,", "--quality", "display mean of quality"
        , "-n,", "--unknowns", "display info about unknown bases (Ns)"
        , "--taxonomy", "display information about taxonomy"
        , "--sex=SEX", "display reference sequence list for the given sex, if defined (Must be one of [male, female, either]). May be specified 0 or more times"
        );
  }

  public void testCFlags() {
    final Appendable err = new StringWriter();
    final Appendable out = new StringWriter();
    final SdfStatistics stats = new SdfStatistics();
    final CFlags flags = new CFlags("", out, err);
    stats.initFlags(flags);
    assertNotNull(flags);

    assertNotNull(flags.getFlag("unknowns"));

    final Flag inFlag = flags.getAnonymousFlag(0);
    assertEquals(1, inFlag.getMinCount());
    assertEquals(Integer.MAX_VALUE, inFlag.getMaxCount());

    final File nsd = new File("no-such-dir");
    nsd.deleteOnExit();
    flags.setFlags("-n", "no-such-dir");
    assertTrue(flags.isSet("unknowns"));
    assertEquals(nsd, inFlag.getValue());
  }

  public void testMain() throws Exception {
    final String fasta = ">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa";

    final File preread = ReaderTestUtils.getDNADir(fasta);
    preread.deleteOnExit();

    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          try {
            assertEquals(0, new SdfStatistics().mainInit(new String[]{"-n", preread.getPath()}, out, err));
          } finally {
            out.close();
          }
          final String outString = out.toString();
          //System.err.println(outString);
          final String[] expected = {
            "Location           : " + preread.getPath(),
            "Type               : DNA",
            "Number of sequences: 3",
            "Maximum length     : 32",
            "Minimum length     : 17",
            "Sequence names     : yes",
            StringUtils.LS + "N                  : 0",
            StringUtils.LS + "A                  : 18",
            StringUtils.LS + "C                  : 23",
            StringUtils.LS + "G                  : 13",
            StringUtils.LS + "T                  : 15",
            "Total residues     : 69",
            "Residue qualities  : no",
            "SDF-ID             : ",
            "Blocks of Ns       : 0",
            "Longest block of Ns: 0"

          };
          TestUtils.containsAll(outString, expected);
          // TestUtils.containsAll(expected, outString);
        }
      } finally {
        bos.close();
      }
      final String s = bos.toString();
      assertEquals(0, s.length());
    } finally {
      assertTrue(FileHelper.deleteAll(preread));
    }
  }

  public void testReadMe() throws Exception {
    final String fasta = ">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa";
    try (final TestDirectory dir = new TestDirectory()) {
      final File preread = ReaderTestUtils.getDNADir(fasta, dir);
      final String readme = "This is a readme test.";
      FileUtils.stringToFile(readme, new File(dir, "readme.txt"));
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        final PrintStream err = new PrintStream(bos);
        try {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          try {
            assertEquals(0, new SdfStatistics().mainInit(new String[]{preread.getPath()}, out, err));
          } finally {
            out.close();
          }
          final String outString = out.toString();
          final String[] expected = {
            "Location           : " + preread.getPath(),
            "Type               : DNA",
            "Number of sequences: 3",
            "Maximum length     : 32",
            "Minimum length     : 17",
            "Sequence names     : yes",
            StringUtils.LS + "N                  : 0",
            StringUtils.LS + "A                  : 18",
            StringUtils.LS + "C                  : 23",
            StringUtils.LS + "G                  : 13",
            StringUtils.LS + "T                  : 15",
            "Total residues     : 69",
            "Residue qualities  : no",
            "SDF-ID             : ",
            "Additional Info:",
            readme

          };
          TestUtils.containsAll(outString, expected);
        } finally {
          err.close();
        }
      }
    }
  }


  public void testLengths() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    final String fasta = ">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\nnaccccaccccacaaacccaann";

    final File prereadDir = ReaderTestUtils.getDNADir(fasta);
    try {
      try (final AnnotatedSequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(prereadDir)) {
        SdfStatistics.printSequenceNameAndLength(reader, ps.printStream());
        TestUtils.containsAll(ps.toString(), "123456789012345678901\t32", "bob\t17", "hobos\t23");
      }
    } finally {
      assertTrue(FileHelper.deleteAll(prereadDir));
    }
  }

  public void testMainQS() throws Exception {
    final String fastq = ""
      + "@12345" + StringUtils.LS
      + "acgtgt" + StringUtils.LS
      + "+12345" + StringUtils.LS
      + "IIIIII" + StringUtils.LS;

    final File preread = ReaderTestUtils.getDNAFastqDir(fastq, false);
    preread.deleteOnExit();

    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          try {
            assertEquals(0, new SdfStatistics().mainInit(new String[]{"-q", preread.getPath()}, out, err));
          } finally {
            out.close();
          }
          final String outString = out.toString();
          //System.err.println(outString);
          final String value = Double.toString(40.0);
          final String[] expected = {"Location           : " + preread.getPath(),
            "Maximum length     : 6",
            "Minimum length     : 6",
            "N                  : 0",
            "A                  : 1",
            "C                  : 1",
            "G                  : 2",
            "T                  : 2",
            "Total residues     : 6",
            "Residue qualities  : yes",
            "Average quality    : " + value,
            "Average qual / pos : ",
            "                 1 : " + value,
            "                 2 : " + value,
            "                 3 : " + value,
            "                 4 : " + value,
            "                 5 : " + value,
            "                 6 : " + value,
          };
          TestUtils.containsAll(outString, expected);
        }
      } finally {
        bos.close();
      }
      final String s = bos.toString();
      assertEquals(0, s.length());
    } finally {
      assertTrue(FileHelper.deleteAll(preread));
    }
  }

  public void testMainWithNs() throws Exception {
    final String fasta = ">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\nnaccccaccccacaaacccaann";

    final File preread = FileUtils.createTempDir("testfasta", "dna");
    preread.deleteOnExit();
    final ArrayList<InputStream> inputStreams = new ArrayList<>();
    inputStreams.add(new ByteArrayInputStream(fasta.getBytes()));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(inputStreams, new DNAFastaSymbolTable());
    final SequencesWriter sequenceWriter = new SequencesWriter(ds, preread, Constants.MAX_FILE_SIZE, PrereadType.UNKNOWN, true);
    CommandLine.setCommandArgs("blahrg");
    sequenceWriter.setComment("blooo");
    sequenceWriter.processSequences(false, false);
    CommandLine.clearCommandArgs();

    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          try {
            assertEquals(0, new SdfStatistics().mainInit(new String[]{"-n", preread.getPath()}, out, err));

          } finally {
            out.close();
          }
          final String outString = out.toString();
          //System.err.println(outString);
          TestUtils.containsAll(outString,
            "Location           : " + preread.getPath(),
            "Parameters         : blahrg",
            "Comment            : blooo",
            "Type               : DNA",
            "Number of sequences: 3",
            "Maximum length     : 32",
            "Minimum length     : 17",
            "Sequence names     : no",
            StringUtils.LS + "N                  : 3",
            StringUtils.LS + "A                  : 18",
            StringUtils.LS + "C                  : 23",
            StringUtils.LS + "G                  : 13",
            StringUtils.LS + "T                  : 15",
            "Total residues     : 72",
            "Residue qualities  : no",
            "Blocks of Ns       : 2",
            "Longest block of Ns: 2",
            "Source             : UNKNOWN",
            "Paired arm         : UNKNOWN");
          assertTrue(outString.contains("Histogram of N frequencies"));
          assertTrue(outString.contains(" 0 : 2"));
          assertTrue(outString.contains(" 3 : 1"));

        }
      } finally {
        bos.close();
      }
      final String s = bos.toString();
      assertEquals(0, s.length());
    } finally {
      assertTrue(FileHelper.deleteAll(preread));
    }
  }

  public void testMainWithPos() throws Exception {
    final String fasta = ">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\nnaccccaccccacaaacccaann";

    final File preread = ReaderTestUtils.getDNADir(fasta);
    preread.deleteOnExit();

    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream err = new PrintStream(bos)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          try {
            assertEquals(0, new SdfStatistics().mainInit(new String[]{"-p", preread.getPath()}, out, err));

          } finally {
            out.close();
          }
          final String outString = out.toString();
          //System.err.println(outString);
          assertTrue(outString.contains("Histogram of N position frequencies"));
          assertTrue(outString.contains(" 1 : 1"));
          assertTrue(outString.contains("22 : 1"));
          assertTrue(outString.contains("23 : 1"));
        }
      } finally {
        bos.close();
      }
      final String s = bos.toString();
      assertEquals(0, s.length());
    } finally {
      assertTrue(FileHelper.deleteAll(preread));
    }

  }

  private void checkSdfVersion(final long version, final File dir, final String resource) throws IOException {
    final InputStream archive = Resources.getResourceAsStream(resource);
    if (archive == null) {
      throw new IOException("Unable to find archive: " + resource + " if you have updated the SDF version be sure to create one for compatibility testing. You can create one with the 'add-sdf-test' ant task");
    }
    try {
      SimpleArchive.unpackArchive(archive, dir);
    } finally {
      archive.close();
    }
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream err = new PrintStream(bos)) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
          int code = new SdfStatistics().mainInit(new String[]{"-n", "-q", dir.getPath()}, out, err);
          assertEquals(bos.toString(), 0, code);
        } finally {
          out.close();
        }
        final String outString = out.toString();
        assertTrue("Expected to contain 'SDF Version        : " + version + "' in: " + StringUtils.LS + outString,
          outString.contains("SDF Version        : " + version + StringUtils.LS));
      }
    } finally {
      bos.close();
    }
    final String s = bos.toString();
    assertEquals(0, s.length());
  }

  private static byte[][] readAllData(final SequencesReader sr) throws IOException {
    final byte[][] ret = new byte[(int) sr.numberSequences()][];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = sr.read(i);
    }
    return ret;
  }

  private static byte[][] readAllQuality(final SequencesReader sr) throws IOException {
    final byte[][] ret = new byte[(int) sr.numberSequences()][];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new byte[sr.length(i)];
      sr.readQuality(i, ret[i]);
    }
    return ret;
  }

  private void checkSdfData(final File archDir, final String oldRes, final File newDir, final String fastqRes) throws IOException {
    final InputStream archive = Resources.getResourceAsStream(oldRes);
    if (archive == null) {
      throw new IOException("Unable to find archive: " + oldRes + " if you have updated the SDF version be sure to create one for compatibility testing.");
    }
    try {
      SimpleArchive.unpackArchive(archive, archDir);
    } finally {
      archive.close();
    }
    final FastqSequenceDataSource fastq = new FastqSequenceDataSource(Collections.singletonList(Resources.getResourceAsStream(fastqRes)), FastQScoreType.PHRED);
    final SequencesWriter sw = new SequencesWriter(fastq, newDir, 1000000, PrereadType.UNKNOWN, false);
    sw.processSequences();
    byte[][] newData;
    byte[][] newQuality;
    try (SequencesReader dsrNew = SequencesReaderFactory.createDefaultSequencesReader(newDir)) {
      newData = readAllData(dsrNew);
      newQuality = readAllQuality(dsrNew);
    }
    final byte[][] oldData;
    final byte[][] oldQuality;
    try (SequencesReader dsrOld = SequencesReaderFactory.createDefaultSequencesReader(archDir)) {
      oldData = readAllData(dsrOld);
      oldQuality = readAllQuality(dsrOld);
    }
    assertTrue(Arrays.deepEquals(newData, oldData));
    assertTrue(Arrays.deepEquals(newQuality, oldQuality));
  }




  public void checkSdfVersionX(long x) throws IOException {
    final File testDir = FileUtils.createTempDir("sdfstats", "ver" + x);
    try {
      checkSdfVersion(x, testDir, "com/rtg/reader/resources/sdfver" + x + ".arch");
    } finally {
      assertTrue(FileHelper.deleteAll(testDir));
    }
  }

  public void checkSdfVersionXData(long x) throws IOException {
    final File testDir = FileUtils.createTempDir("sdfdata", "ver" + x);
    final File archDir = new File(testDir, "oldSdf");
    final File newDir = new File(testDir, "newSdf");
    try {
      checkSdfData(archDir, "com/rtg/reader/resources/sdfver" + x + ".arch", newDir, "com/rtg/reader/resources/sdfsrc.fastq");
    } finally {
      assertTrue(FileHelper.deleteAll(testDir));
    }
  }

  private static final long MIN_VER_TEST = 4;
  public void testSdfVersionX() throws IOException {
    for (long i = MIN_VER_TEST; i <= IndexFile.VERSION; i++) {
      checkSdfVersionX(i);
      checkSdfVersionXData(i);
    }
  }

  private static void createCurrentVersionFiles(File destDir) throws IOException {
    final File dir = FileUtils.createTempDir("sdfver", "version" + IndexFile.VERSION);
    try {
      final File tempDir = new File(dir, "sdf");
      final File sdfIn = FileHelper.resourceToFile("com/rtg/reader/resources/sdfsrc.fastq", new File(dir, "sdfsrc.fastq"));
      final int code = new FormatCli().mainInit(new String[] {"-o", tempDir.getPath(), "-f", "fastq", "-q", "sanger", sdfIn.getPath()}, FileUtils.getStdoutAsOutputStream(), System.err);
      assertEquals(0, code);
      SimpleArchive.writeArchive(new File(destDir, "sdfver" + IndexFile.VERSION + ".arch"), tempDir.listFiles());
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testTaxonomySDF() throws IOException {
    try (final TestDirectory dir = new TestDirectory("sdfstats")) {
      // create sdf from sequences.fasta
      final File fullSdf = new File(dir, "sdf_full");
      final File sequences = new File(dir, "sequences.fasta");
      FileHelper.resourceToFile("com/rtg/reader/resources/sequences.fasta", sequences);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MemoryPrintStream err = new MemoryPrintStream();
      final FormatCli format = new FormatCli();
      int rc = format.mainInit(new String[] {"-o", fullSdf.getAbsolutePath(), sequences.getAbsolutePath()}, out, err.printStream());
      assertEquals("Error: " + err.toString(), "", err.toString());
      assertEquals(0, rc);
      //System.err.println(out.toString());

      err.close();
      out.close();

      out = new ByteArrayOutputStream();
      err = new MemoryPrintStream();
      rc = new SdfStatistics().mainInit(new String[] {"-n", fullSdf.getPath()}, out, err.printStream());
      assertEquals(0, rc);
      final String outString = out.toString();
      //System.err.println(outString);
      final String[] expected = {
          "Location           : " + fullSdf.getPath(),
          "Type               : DNA",
          "Number of sequences: 44",
          "Maximum length     : 200",
          "Minimum length     : 200",
          "Sequence names     : yes",
          StringUtils.LS + "N                  : 0",
          StringUtils.LS + "A                  : 2498",
          StringUtils.LS + "C                  : 1791",
          StringUtils.LS + "G                  : 1824",
          StringUtils.LS + "T                  : 2687",
          "Total residues     : 8800",
          "Residue qualities  : no",
          "SDF-ID             : ",
          "Blocks of Ns       : 0",
          "Longest block of Ns: 0"
      };
      TestUtils.containsAll(outString, expected);
      err.close();
      out.close();

      assertEquals(0, err.toString().length());
      out = new ByteArrayOutputStream();
      err = new MemoryPrintStream();
      rc = new SdfStatistics().mainInit(new String[] {"-n", fullSdf.getPath(), "--taxonomy"}, out, err.printStream());
      assertEquals(1, rc);
      assertTrue(err.toString().contains("--taxonomy was specified but"));
      assertTrue(err.toString().contains("is missing a 'taxonomy.tsv'"));
      err.close();
      out.close();

      // cp taxonomy files to sdf
      final File taxonomy = new File(fullSdf, "taxonomy.tsv");
      FileHelper.resourceToFile("com/rtg/reader/resources/taxonomy.tsv", taxonomy);
      final File taxonomyLookup = new File(fullSdf, "taxonomy_lookup.tsv");
      FileHelper.resourceToFile("com/rtg/reader/resources/taxonomy_lookup.tsv", taxonomyLookup);

      out = new ByteArrayOutputStream();
      err = new MemoryPrintStream();
      rc = new SdfStatistics().mainInit(new String[] {"-n", fullSdf.getPath(), "--taxonomy"}, out, err.printStream());
      assertEquals(0, rc);

      final String outString2 = out.toString();
      //System.err.println(outString2);
      final String[] expected2 = {
          "Location           : " + fullSdf.getPath(),
          "Type               : DNA",
          "Number of sequences: 44",
          "Maximum length     : 200",
          "Minimum length     : 200",
          "Sequence names     : yes",
          StringUtils.LS + "N                  : 0",
          StringUtils.LS + "A                  : 2498",
          StringUtils.LS + "C                  : 1791",
          StringUtils.LS + "G                  : 1824",
          StringUtils.LS + "T                  : 2687",
          "Total residues     : 8800",
          "Residue qualities  : no",
          "SDF-ID             : ",
          "Blocks of Ns       : 0",
          "Longest block of Ns: 0",
          "Taxonomy nodes     : 20",
          "Sequence nodes     : 12",
          "Other nodes        : 8"
      };
      TestUtils.containsAll(outString2, expected2);
      err.close();
      out.close();

      assertEquals(0, err.toString().length());
    }
  }

  /**
   * Point at <code>test/com/rtg/reader/resources</code> directory
   * @param args directory to put archive in
   */
  public static void main(String[] args) throws IOException {
    createCurrentVersionFiles(new File(args[0]));
  }

  @Override
  protected AbstractCli getCli() {
    return new SdfStatistics();
  }
}
