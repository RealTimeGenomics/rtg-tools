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
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.rtg.launcher.GlobalFlags;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;
public class PrereadVerifierTest extends TestCase {

  private static final String LS = System.lineSeparator();

  private File mDir = null;
  @Override
  public void setUp() throws Exception {
    mDir = FileHelper.createTempDirectory();
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    GlobalFlags.resetAccessedStatus();
    FileHelper.deleteAll(mDir);
    mDir = null;
    Diagnostic.deleteLog();
  }

  private InputStream createStream(final String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  private static final String EX1 = ">test" + LS
      + "acgtgtgtgtcttagggctcactggtcatgca" + LS + ">bob the builder" + LS
      + "tagttcagcatcgatca" + LS + ">hobos r us" + LS + "accccaccccacaaacccaa";

  private void createBasePreread() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX1));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(
        al, new DNAFastaSymbolTable());
    // mDir = FileHelper.createTempDirectory();
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
  }

  public void testVerifier() throws IOException {
    createBasePreread();

    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      assertTrue(PrereadVerifier.verify(dsr, new File("data")));
    }
  }

  private static final String EX2 = ">TEST"
      + LS
      + "acatgctgtacgtcgagtcagtcatgcagtcagtcatgcagtcagtcagtcatgcagtcagtcatgcagtcagtcagtcagtcagtcgcatgca"
      + LS
      + ">TEst2"
      + LS
      + "tgactgcatgcatgcatgcatgcatgcatgcatgcatgcagtcagtcgtcgtactgcatgcatgcagtcagtcagtcatgcagtcgctgctagtcgtc"
      + LS
      + ">TeSt3"
      + LS
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + LS
      + ">tEsT4"
      + LS
      + "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
      + LS
      + ">tESt5"
      + LS
      + "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"
      + LS
      + ">teST6"
      + LS
      + "tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt"
      + LS;

  public void testVerifier2() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
        new DNAFastaSymbolTable());
    new SequencesWriter(ds, mDir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      assertTrue(PrereadVerifier.verify(dsr, new File("data")));
    }
  }

  private static class MyFileFilter implements FileFilter {
    @Override
    public boolean accept(final File file) {
      return file != null && !file.getPath().endsWith("log")
          && file.length() > 0;
    }
  }

  public void testVerifierRandomMutation() throws IOException {
    final StringBuilder failures = new StringBuilder();
    final PortableRandom rnd = new PortableRandom();
    try {
      // int count = 0;
      for (int k = 0; k < 20; k++) {
        boolean failed = false;
        try {
          try {
            createBasePreread();
          } catch (final RuntimeException ex) {
            // this is for windows, as sometime windows wont allow
            // reusing mDir
            --k;
            // System.out.println(count++);
            failed = true;
            continue;

          } finally {
            if (failed) {
              FileHelper.deleteAll(mDir);
            }
          }
          // Random mutate base preread
          final File[] targets = mDir.listFiles(new MyFileFilter());
          assertNotNull(targets);
          assertTrue("Targets length > 0, seed was: " + rnd.getSeed(), targets.length > 0);
          final File target = targets[rnd.nextInt(targets.length)];
          // Following loss of precision ok, because of 1GB file size
          // limit we have
          // and because test file is small
          final long size = target.length();
          final int placeToMutate = rnd.nextInt((int) size);
          if (target.getName().equals("seqpointer0") && placeToMutate == 0) {
            //corruption of this byte does not affect sdf
            continue;
          }
          assertTrue("PlaceToMutate fail, seed was: " + rnd.getSeed(), placeToMutate < size && placeToMutate >= 0);
          final String desc;
          try (RandomAccessFile f = new RandomAccessFile(target, "rws")) {
            f.seek(placeToMutate);
            final int current = f.readByte();
            int replace;
            do {
              replace = rnd.nextInt(256);
            } while ((byte) replace == (byte) current);
            f.seek(placeToMutate);
            f.writeByte(replace);
            desc = "Changed " + current + " to " + replace + " at position "
              + placeToMutate + " in " + target.getPath();
          }
          assertEquals("Size > target length, seed was: " + rnd.getSeed(), size, target.length());

          // Now make sure the verifier karks it
          try {
            final SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir);
            try {
              final boolean res = PrereadVerifier.verify(dsr, new File("data"));
              if (res) {
                failures.append("FAILED: ").append(desc).append('\n');
              }
            } finally {
              if (dsr != null) {
                dsr.close();
              }
            }
          } catch (final NoTalkbackSlimException ex) {
            assertTrue("Didn't contain 'newer version', seed was: " + rnd.getSeed(), ex.getMessage().contains("newer version")); // This may happen if we mutate the indexfile version high
          } catch (final CorruptSdfException ex) {
            // if (ex.getMessage() == null) {
            // ex.printStackTrace();
            // }
            //assertEquals("Corrupt index.", ex.getMessage());
          }
        } finally {
          FileHelper.deleteAll(mDir);
        }
      }
      if (failures.length() != 0) {
        fail("Seed: " + rnd.getSeed() + ", " + failures.toString());
      }
    } catch (final IOException ioe) {
      System.err.println("ioe, seed was: " + rnd.getSeed());
      throw ioe;
    }
  }

  public void testVerifierRandomTruncation() throws IOException {
    final StringBuilder failures = new StringBuilder();
    final PortableRandom rnd = new PortableRandom();
    try {
      for (int k = 0; k < 10; k++) {
        boolean failed = false;
        try {
          try {
            createBasePreread();
          } catch (final RuntimeException ex) {
            --k;
            // this is for windows, as sometime windows wont allow
            // creating new dir
            failed = true;
            continue;

          } finally {
            if (failed) {
              FileHelper.deleteAll(mDir);
            }
          }
          // Random mutate base preread
          final File[] targets = mDir.listFiles(new MyFileFilter());
          assertNotNull(targets);
          assertTrue("Targets > 0, seed was: " + rnd.getSeed(), targets.length > 0);

          final int t = rnd.nextInt(targets.length);
          // System.out.println("Files");
          // for (int i = 0; i < targets.length; i++) {
          //   System.out.println((i==t?"*":" ") + targets[i].getPath());
          // }
          final File target = targets[t];
          // Following loss of precision ok, because of 1GB file size
          // limit we have
          // and because test file is small
          final long size = target.length();
          if (size > 1) {
            final int placeToTruncate = size == 32 ? 24 : rnd.nextInt((int) size);
            assertTrue("PlaceToTruncate fail, seed was: " + rnd.getSeed(), placeToTruncate < size && placeToTruncate >= 0);
            final String desc;
            try (RandomAccessFile f = new RandomAccessFile(target, "rws")) {
              f.setLength(placeToTruncate);
              desc = "Truncated target " + t + "/" + targets.length + " " + target.getPath() + " (length " + size + ") to " + placeToTruncate
                + " bytes";
            }
            assertTrue("Size != target length, seed was: " + rnd.getSeed(), size != target.length());

            // Now make sure the verifier karks it
            try {
              final SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir);
              try {
                final boolean res = PrereadVerifier.verify(dsr, new File("data"));
                if (res) {
                  failures.append("FAILED: ").append(desc).append('\n');
                }
              } finally {
                if (dsr != null) {
                  dsr.close();
                }
              }
            } catch (final CorruptSdfException ex) {
              // if (ex.getMessage() == null) {
              // ex.printStackTrace();
              // }
              //assertEquals("Corrupt index.", ex.getMessage());
            }
          }
        } finally {
          FileHelper.deleteAll(mDir);
        }
      }
      if (failures.length() != 0) {
        fail("Seed was: " + rnd.getSeed() + ", " + failures.toString());
      }
    } catch (final IOException ioe) {
      System.err.println("ioe, seed was: " + rnd.getSeed());
      throw ioe;
    }
  }

  private static final String MSG = "Usage: SDFVerify [OPTION]... SDF" + LS + LS
      + "Try '--help' for more information" + LS + "";

  public void testFlags() {
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outstream)) {
      final PrintStream err = new PrintStream(stream);
      try {
        assertEquals(1, PrereadVerifier.mainInit(new String[]{"-x"}, out, err));
      } finally {
        err.close();
      }
    }
    assertEquals("", outstream.toString());
    assertEquals("Error: Unknown flag -x" + LS + LS + MSG, stream.toString());
  }

  /*private static final String HELP_MSG = "Usage: SDFVerify [OPTION]... SDF" + LS
      + LS
      + "Required flags: " + LS
      + "      SDF        the SDF to be verified" + LS
      + LS
      + "Optional flags: " + LS
      + "  -h, --help     print help on command-line flag usage" + LS
      + "  -P, --progress show progress" + LS + LS + "";*/

  public void testHelpMessages() {
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outstream)) {
      final PrintStream err = new PrintStream(stream);
      try {
        assertEquals(1, PrereadVerifier.mainInit(new String[]{"-h"}, out, err));
      } finally {
        err.close();
      }
    }
    assertEquals("", stream.toString());
  }

  public void testRunningFromMainInit() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    new SequencesWriter(ds, mDir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outstream)) {
      final PrintStream err = new PrintStream(stream);
      try {
        final int code = PrereadVerifier.mainInit(new String[]{mDir.getPath()}, out, err);
        assertEquals(stream.toString(), 0, code);
      } finally {
        err.close();
      }
    }
    assertTrue(outstream.toString().contains("erified okay."));
    assertEquals("", stream.toString());
  }

  public void testErrorMessage() throws IOException {
    final File f = FileUtils.createTempDir("prereadverifier", "test");
    try {
      final File mainIndex = new File(f, "mainindex");
      assertTrue(mainIndex.createNewFile());
      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream)) {
        final PrintStream err = new PrintStream(stream);
        try {
          assertEquals(1, PrereadVerifier.mainInit(new String[]{f.getPath()}, out, err));
        } finally {
          err.close();
        }
      }
      assertEquals("", outstream.toString());
      assertEquals("Error: The SDF verification failed." + LS, stream.toString());
    } catch (final RuntimeException e) {
      //okay
    } finally {
      FileHelper.deleteAll(f);
    }
  }

  public void testInvalidFlagMessage() throws IOException {
    final File f = File.createTempFile("preread", "verifier");
    try {
      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream)) {
        final PrintStream err = new PrintStream(stream);
        try {
          assertEquals(1, PrereadVerifier.mainInit(new String[]{f.getPath()}, out, err));
        } finally {
          err.close();
        }
      }
      assertEquals("", outstream.toString());
      assertTrue(stream.toString(), stream.toString().contains("The specified file, \"" + f.getPath() + "\", is not an SDF."));
    } catch (final RuntimeException e) {
      //okay
    } finally {
      FileHelper.deleteAll(f);
    }
  }

  public void testIfChecksumNotPresent() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    new SequencesWriter(ds, mDir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
    final IndexFile f = new IndexFile(mDir);
    f.setDataChecksum(0);
    f.save(mDir);
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outstream)) {
      final PrintStream err = new PrintStream(stream);
      try {
        assertEquals(1, PrereadVerifier.mainInit(new String[]{mDir.getPath()}, out, err));
      } finally {
        err.close();
      }
    }
    assertEquals("", outstream.toString());

    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      assertFalse(PrereadVerifier.verify(dsr, new File("data")));
    }
  }

  public void testIfModifyChecksum() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    new SequencesWriter(ds, mDir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
    final IndexFile f = new IndexFile(mDir);
    f.setDataChecksum(123233);
    f.save(mDir);
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outstream)) {
      final PrintStream err = new PrintStream(stream);
      try {
        assertEquals(1, PrereadVerifier.mainInit(new String[]{mDir.getPath()}, out, err));
      } finally {
        err.close();
      }
    }
    assertEquals("", outstream.toString());
    assertTrue(stream.toString().contains("The SDF verification failed."));

    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      assertFalse(PrereadVerifier.verify(dsr, new File("data")));
    }
  }

  public void testValidator() throws IOException {
    Diagnostic.setLogStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final File tmp = File.createTempFile("prepreadverifier", "test");
    try {
      try (PrintStream diag = new PrintStream(err)) {
        PrereadVerifier.mainInit(new String[]{tmp.getPath()}, null, diag);
      } catch (final RuntimeException ex) {
        fail();
      } finally {
        assertTrue(FileHelper.deleteAll(tmp));

      }
    } finally {
      err.close();
    }
    final String expected = "Error: The specified file, \"" + tmp.getPath() + "\", is not an SDF." + LS
    + MSG;

    assertEquals(expected, err.toString());
  }
}
