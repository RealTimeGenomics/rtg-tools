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

import com.rtg.AbstractTest;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.util.PortableRandom;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

public class SdfVerifierTest extends AbstractTest {

  private static final String LS = System.lineSeparator();

  private InputStream createStream(final String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  private static final String EX1 = ">test" + LS
      + "acgtgtgtgtcttagggctcactggtcatgca" + LS + ">bob the builder" + LS
      + "tagttcagcatcgatca" + LS + ">hobos r us" + LS + "accccaccccacaaacccaa";

  private void createBasePreread(File dir) throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX1));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, dir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
  }

  public void testVerifier() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      createBasePreread(dir);
      try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(dir)) {
        assertTrue(SdfVerifier.verify(dsr, new File("data")));
      }
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
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    try (TestDirectory dir = new TestDirectory()) {
      new SequencesWriter(ds, dir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
      try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(dir)) {
        assertTrue(SdfVerifier.verify(dsr, new File("data")));
      }
    }
  }

  private static class MyFileFilter implements FileFilter {
    @Override
    public boolean accept(final File file) {
      return file != null && !file.getPath().endsWith("log") && file.length() > 0;
    }
  }

  public void testVerifierRandomMutation() throws IOException {
    final StringBuilder failures = new StringBuilder();
    final PortableRandom rnd = new PortableRandom();
    try (TestDirectory topDir = new TestDirectory("testVerifierRandomMutation")) {
      for (int k = 0; k < 20; ++k) {
        final File dir = new File(topDir, "sub-" + k);
        createBasePreread(dir);
        // Random mutate base preread
        final File[] targets = dir.listFiles(new MyFileFilter());
        assertNotNull(targets);
        assertTrue("Targets length > 0, seed was: " + rnd.getSeed(), targets.length > 0);
        final File target = targets[rnd.nextInt(targets.length)];
        // Following loss of precision ok, because of 1GB file size
        // limit we have and because test file is small
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
          desc = "Changed " + current + " to " + replace + " at position " + placeToMutate + " in " + target.getPath();
        }
        assertEquals("Size > target length, seed was: " + rnd.getSeed(), size, target.length());

        // Now make sure the verifier karks it
        try (final SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(dir)) {
          final boolean res = SdfVerifier.verify(dsr, new File("data"));
          if (res) {
            failures.append("FAILED: ").append(desc).append('\n');
          }
        } catch (final NoTalkbackSlimException ex) {
          assertTrue("Didn't contain 'newer version', seed was: " + rnd.getSeed(), ex.getMessage().contains("newer version")); // This may happen if we mutate the indexfile version high
        } catch (final CorruptSdfException ex) {
          // if (ex.getMessage() == null) {
          // ex.printStackTrace();
          // }
          //assertEquals("Corrupt index.", ex.getMessage());
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
    try (TestDirectory topDir = new TestDirectory()) {
      for (int k = 0; k < 10; ++k) {
        final File dir = new File(topDir, "sub-" + k);
        createBasePreread(dir);
        // Random mutate base preread
        final File[] targets = dir.listFiles(new MyFileFilter());
        assertNotNull(targets);
        assertTrue("Targets > 0, seed was: " + rnd.getSeed(), targets.length > 0);

        final int t = rnd.nextInt(targets.length);
        final File target = targets[t];
        // Following loss of precision ok, because of 1GB file size
        // limit we have and because test file is small
        final long size = target.length();
        if (size > 1) {
          final int placeToTruncate = size == 32 ? 24 : rnd.nextInt((int) size);
          assertTrue("PlaceToTruncate fail, seed was: " + rnd.getSeed(), placeToTruncate < size && placeToTruncate >= 0);
          final String desc;
          try (RandomAccessFile f = new RandomAccessFile(target, "rws")) {
            f.setLength(placeToTruncate);
            desc = "Truncated target " + t + "/" + targets.length + " " + target.getPath() + " (length " + size + ") to " + placeToTruncate + " bytes";
          }
          assertTrue("Size != target length, seed was: " + rnd.getSeed(), size != target.length());

          // Now make sure the verifier karks it
          try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(dir)) {
            final boolean res = SdfVerifier.verify(dsr, new File("data"));
            if (res) {
              failures.append("FAILED: ").append(desc).append('\n');
            }
          } catch (final CorruptSdfException ex) {
            // if (ex.getMessage() == null) {
            // ex.printStackTrace();
            // }
            //assertEquals("Corrupt index.", ex.getMessage());
          }
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

  private static final String MSG = "Usage: SDFVerify [OPTION]... SDF";

  public void testFlags() {
    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outstream);
         PrintStream err = new PrintStream(stream)) {
      assertEquals(1, SdfVerifier.mainInit(new String[]{"-x"}, out, err));
    }
    assertEquals("", outstream.toString());
    TestUtils.containsAllUnwrapped(stream.toString(), "Error: Unknown flag -x", MSG);
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
    try (PrintStream out = new PrintStream(outstream);
         PrintStream err = new PrintStream(stream)) {
      assertEquals(1, SdfVerifier.mainInit(new String[]{"-h"}, out, err));
    }
    assertEquals("", stream.toString());
  }

  public void testRunningFromMainInit() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    try (TestDirectory dir = new TestDirectory()) {
      new SequencesWriter(ds, dir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream)) {
        try (final PrintStream err = new PrintStream(stream)) {
          final int code = SdfVerifier.mainInit(new String[]{dir.getPath()}, out, err);
          assertEquals(stream.toString(), 0, code);
        }
      }
      assertTrue(outstream.toString().contains("erified okay."));
      assertEquals("", stream.toString());
    }
  }

  public void testErrorMessage() throws IOException {
    try (final TestDirectory f = new TestDirectory("prepreadverifier")) {
      final File mainIndex = new File(f, "mainindex");
      assertTrue(mainIndex.createNewFile());
      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream);
           PrintStream err = new PrintStream(stream)) {
        assertEquals(1, SdfVerifier.mainInit(new String[]{f.getPath()}, out, err));
      }
      assertEquals("", outstream.toString());
      assertEquals("Error: The SDF verification failed." + LS, stream.toString());
    } catch (final RuntimeException e) {
      //okay
    }
  }

  public void testInvalidFlagMessage() throws IOException {
    try (final TestDirectory tmpDir = new TestDirectory("prepreadverifier")) {
      final File f = FileHelper.createTempFile(tmpDir);
      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream);
           PrintStream err = new PrintStream(stream)) {
        assertEquals(1, SdfVerifier.mainInit(new String[]{f.getPath()}, out, err));
      }
      assertEquals("", outstream.toString());
      assertTrue(stream.toString(), stream.toString().contains("The specified file, \"" + f.getPath() + "\", is not an SDF."));
    } catch (final RuntimeException e) {
      //okay
    }
  }

  public void testIfChecksumNotPresent() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    try (TestDirectory dir = new TestDirectory()) {
      new SequencesWriter(ds, dir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
      final IndexFile f = new IndexFile(dir);
      f.setDataChecksum(0);
      f.save(dir);
      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream);
           PrintStream err = new PrintStream(stream)) {
        assertEquals(1, SdfVerifier.mainInit(new String[]{dir.getPath()}, out, err));
      }
      assertEquals("", outstream.toString());

      try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(dir)) {
        assertFalse(SdfVerifier.verify(dsr, new File("data")));
      }
    }
  }

  public void testIfModifyChecksum() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(EX2));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    try (TestDirectory dir = new TestDirectory()) {
      new SequencesWriter(ds, dir, 1000000000, PrereadType.UNKNOWN, false).processSequences();
      final IndexFile f = new IndexFile(dir);
      f.setDataChecksum(123233);
      f.save(dir);

      final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outstream);
           PrintStream err = new PrintStream(stream)) {
        assertEquals(1, SdfVerifier.mainInit(new String[]{dir.getPath()}, out, err));
      }
      assertEquals("", outstream.toString());
      assertTrue(stream.toString().contains("The SDF verification failed."));

      try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(dir)) {
        assertFalse(SdfVerifier.verify(dsr, new File("data")));
      }
    }
  }

  public void testValidator() throws IOException {
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    try (final TestDirectory tmp = new TestDirectory("prepreadverifier")) {
      final File f = FileHelper.createTempFile(tmp);
      try (PrintStream diag = new PrintStream(err)) {
        SdfVerifier.mainInit(new String[]{f.getPath()}, null, diag);
      } catch (final RuntimeException ex) {
        fail();
      }
    } finally {
      err.close();
    }
    TestUtils.containsAllUnwrapped(err.toString(), "Error: The specified file, \"", "\", is not an SDF.", MSG);

  }
}
