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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.ToolsEntry;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.DnaUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

/**
 */
public class DefaultSequencesReaderTest extends AbstractSequencesReaderTest {

  @Override
  protected SequencesReader createSequencesReader(final File dir, LongRange region) throws IOException {
    return SequencesReaderFactory.createDefaultSequencesReader(dir, region);
  }

  private File mOutDir = null;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mOutDir = FileUtils.createTempDir("sequencegenerator", "main");
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    FileHelper.deleteAll(mOutDir);
    mOutDir = null;
  }

  private static class TestRaf extends RandomAccessFile {

    private final int mMaxDiff;
    private int mCurrent = 1;
    private int mAdj;

    public TestRaf(final File file, final String mode, final int maxDiff) throws IOException {
      super(file, mode);
      mMaxDiff = maxDiff;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      final int len2;
      if (mCurrent < len) {
        len2 = mCurrent;
      } else {
        len2 = len;
      }
      if (mCurrent + mAdj > mMaxDiff) {
        mAdj = -1;
      } else if (mCurrent + mAdj < 1) {
        mAdj = 1;
      }
      mCurrent += mAdj;
      return super.read(b, off, len2);
    }

  }

  public void testLengthsHelper() throws IOException {
    final byte[] buffer = new byte[7];
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test1\nacgta\n"
                      + ">test2\nagtcatg\n"
                      + ">test3\nacgtttggct\n"
                      + ">test4\natggcttagctacagt\n"
                      + ">test5\nactagattagagtagagatgatgtagatgagtagaaagtt\n"
                      + ">test6\na"));
    //0, 5, 12, 22, 38,
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
            new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20000, PrereadType.UNKNOWN, false);
    sw.processSequences();
    final File file = SdfFileUtils.sequencePointerFile(mDir, 0);
    final int[] exp = {5, 7, 10, 16, 40, 78};
    try (TestRaf raf = new TestRaf(file, "r", 73)) {
      checkRaf(raf, exp, buffer);
    }
    try (RandomAccessFile raf2 = new RandomAccessFile(file, "r")) {
      checkRaf(raf2, exp, buffer);
    }
  }

  private void checkRaf(final RandomAccessFile raf, final int[] expected, final byte[] buffer) throws IOException  {
    final int[] lengths = new int[6];
    DefaultSequencesReader.sequenceLengthsHelper(raf, buffer, lengths, 0, 0, raf.length(), 5);
    assertTrue(Arrays.equals(expected, lengths));
  }

  public void testCopy() throws IOException {
    //set a command line.
    new ToolsEntry().intMain(new String[]{"feh", "-f", "super feh"}, TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream());

    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test1\nacgta\n"
                      + ">test2\nagtcatg\n"
                      + ">test3\nacgtttggct\n"
                      + ">test4\natggcttagctacagt\n"
                      + ">test5\nactagattagagtagagatgatgtagatgagtagaaagtt\n"
                      + ">test6\na"));
    //0, 5, 12, 22, 38,
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
            new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20000, PrereadType.UNKNOWN, false);
    sw.setComment("blah rag");
    sw.processSequences();
    try (SequencesReader r = createSequencesReader(mDir)) {
      try (SequencesReader rCopy = r.copy()) {
        assertEquals(r.dataChecksum(), rCopy.dataChecksum());
        assertEquals(r.qualityChecksum(), rCopy.qualityChecksum());
        assertEquals(r.nameChecksum(), rCopy.nameChecksum());
        assertEquals(r.maxLength(), rCopy.maxLength());
        assertEquals(r.minLength(), rCopy.minLength());
        assertEquals(r.numberSequences(), rCopy.numberSequences());
        assertEquals(r.sdfVersion(), rCopy.sdfVersion());
        if (r instanceof AnnotatedSequencesReader) {
          final AnnotatedSequencesReader rCast = (AnnotatedSequencesReader) r;
          final AnnotatedSequencesReader rCopyCast = (AnnotatedSequencesReader) rCopy;
          assertEquals(rCast.comment(), rCopyCast.comment());
          assertEquals("feh -f \"super feh\"", rCopyCast.commandLine());
        }
      }
    }

  }

  static final String FASTA = ">r0" + LS + "ACGTACG" + LS
   + ">r1" + LS + "GCGTA" + LS
   + ">r2" + LS + "cCGTAC" + LS
   + ">r3" + LS + "TCGTACGTAC" + LS
   + ">r4" + LS + "GGGTACGTACGT" + LS;
  public void testRegion() throws IOException {
    ReaderTestUtils.getReaderDNA(FASTA, mOutDir, new SdfId(0L)).close();
    try (DefaultSequencesReader reader = new DefaultSequencesReader(mOutDir, new LongRange(1, 3))) {
      assertEquals(2, reader.numberSequences());
      byte[] data = reader.read(0);
      assertEquals("GCGTA", DnaUtils.bytesToSequenceIncCG(data));
      data = reader.read(1);
      assertEquals("CCGTAC", DnaUtils.bytesToSequenceIncCG(data));

      final int[] lengths = reader.sequenceLengths(0, 2);
      assertEquals(2, lengths.length);
      assertEquals(5, lengths[0]);
      assertEquals(6, lengths[1]);
    }


  }


  public void testReadMe() throws IOException {
    ReaderTestUtils.getReaderDNA(FASTA, mOutDir, new SdfId(0L)).close();
    try (DefaultSequencesReader reader = new DefaultSequencesReader(mOutDir, new LongRange(1, 3))) {
      assertNull(reader.getReadMe());
      FileUtils.stringToFile("This is some example content\nBlah", new File(mOutDir, "readme.txt"));
      assertEquals("This is some example content\nBlah", reader.getReadMe());
    }
  }

  public void testSeekNegative() throws IOException {
    ReaderTestUtils.getReaderDNA(FASTA, mOutDir, new SdfId(0L)).close();
    try (final DefaultSequencesReader reader = new DefaultSequencesReader(mOutDir, new LongRange(1, 3))) {
      try {
        reader.iterator().seek(Integer.MIN_VALUE);
        fail();
      } catch (final IllegalArgumentException e) {
        // ok
      }
    }
  }

}

