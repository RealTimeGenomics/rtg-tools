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

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.launcher.MainResult;
import com.rtg.tabix.ExtractCli;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;

/**
 *
 */
public class SamOutputTest extends AbstractNanoTest {

  public void test() throws IOException {
    final SAMFileHeader header = new SAMFileHeader();
    header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
    header.getSequenceDictionary().addSequence(new SAMSequenceRecord("seq", 1000000));
    final SAMRecord rec = new SAMRecord(header);
    rec.setAlignmentStart(500000);
    rec.setReferenceName("seq");
    rec.setReadName("read");
    rec.setReadString("acgt");
    rec.setBaseQualityString("####");
    rec.setCigarString("4=");
    try (TestDirectory dir = new TestDirectory()) {
      final MemoryPrintStream mps = new MemoryPrintStream();
      try (SamOutput out = SamOutput.getSamOutput(new File(dir, "foo"), mps.outputStream(), header, true)) {
        out.getWriter().addAlignment(rec);
      }
      mNano.check("samoutput_expected_1.sam", MainResult.run(new ExtractCli(), "--header", new File(dir, "foo.bam").getPath(), "seq:500000+1").out());
      assertTrue(new File(dir, "foo.bam.bai").exists());
      assertEquals("", mps.toString());

      try (SamOutput out = SamOutput.getSamOutput(new File(dir, "foo.sam"), mps.outputStream(), header, true)) {
        out.getWriter().addAlignment(rec);
      }
      mNano.check("samoutput_expected_1.sam", MainResult.run(new ExtractCli(), "--header", new File(dir, "foo.sam.gz").getPath(), "seq:500000+1").out());
      assertTrue(new File(dir, "foo.sam.gz.tbi").exists());
      assertEquals("", mps.toString());

      try (SamOutput out = SamOutput.getSamOutput(new File(dir, "foo.sam"), mps.outputStream(), header, false)) {
        out.getWriter().addAlignment(rec);
      }
      mNano.check("samoutput_expected_1.sam", FileHelper.fileToString(new File(dir, "foo.sam")));
      assertFalse(new File(dir, "foo.sam.tbi").exists());
      assertEquals("", mps.toString());

      try (SamOutput out = SamOutput.getSamOutput(new File("-"), mps.outputStream(), header, true)) {
        out.getWriter().addAlignment(rec);
      }
      mNano.check("samoutput_expected_1.sam", mps.toString());
    }
  }
}