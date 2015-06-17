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
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class SequencesReaderFactoryTest extends TestCase {

  public void testFactory() throws Exception {
    final MemoryPrintStream err = new MemoryPrintStream();
    Diagnostic.setLogStream(err.printStream());
    final File tmpDir = FileUtils.createTempDir("testSeqReaderFact", "blah");
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(new ByteArrayInputStream((">test1\nacgta\n"
                        + ">test2\nagtcatg\n"
                        + ">test3\nacgtttggct\n"
                        + ">test4\natggcttagctacagt\n"
                        + ">test5\nactagattagagtagagatgatgtagatgagtagaaagtt\n"
                        + ">test6\na").getBytes()));
      //0, 5, 12, 22, 38,
      final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
              new DNAFastaSymbolTable());
      final SequencesWriter sw = new SequencesWriter(ds, tmpDir, 20000, PrereadType.UNKNOWN, false);
      sw.processSequences();

      err.reset();
      LongRange region = SequencesReaderFactory.resolveRange(tmpDir, new LongRange(1, 2));
      assertEquals(1, region.getStart());
      assertEquals(2, region.getEnd());
      assertEquals("", err.toString());
      region = SequencesReaderFactory.resolveRange(tmpDir, new LongRange(0, 0));
      assertEquals(0, region.getStart());
      assertEquals(0, region.getEnd());
      assertEquals("", err.toString());
      region = SequencesReaderFactory.resolveRange(tmpDir, new LongRange(-1, 7));
      assertEquals(0, region.getStart());
      assertEquals(6, region.getEnd());
      TestUtils.containsAll(err.toString(), "The end sequence id \"7\" is out of range, it must be from \"1\" to \"6\". Defaulting end to \"6\"");

      SequencesReader sr = SequencesReaderFactory.createDefaultSequencesReader(tmpDir);
      assertNotNull(sr);
      assertTrue(sr instanceof DefaultSequencesReader);

      sr = SequencesReaderFactory.createMemorySequencesReader(tmpDir, true, LongRange.NONE);
      assertNotNull(sr);
      assertTrue(sr instanceof CompressedMemorySequencesReader);

      sr = SequencesReaderFactory.createDefaultSequencesReader(tmpDir);
      assertNotNull(sr);
      assertTrue(sr instanceof DefaultSequencesReader);

      sr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(tmpDir, true, true, LongRange.NONE);
      assertNotNull(sr);
      assertTrue(sr instanceof CompressedMemorySequencesReader);

      sr = SequencesReaderFactory.createDefaultSequencesReaderCheckEmpty(tmpDir, LongRange.NONE);
      assertNotNull(sr);
      assertTrue(sr instanceof DefaultSequencesReader);

      sr = SequencesReaderFactory.createDefaultSequencesReaderCheckEmpty(tmpDir);
      assertNotNull(sr);
      assertTrue(sr instanceof DefaultSequencesReader);

      sr = SequencesReaderFactory.createMemorySequencesReader(null, true, LongRange.NONE);
      assertNull(sr);

    } finally {
      Diagnostic.setLogStream();
      FileHelper.deleteAll(tmpDir);
    }
  }

}
