/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import java.io.IOException;

import com.rtg.util.io.MemoryPrintStream;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import junit.framework.TestCase;

/**
 *
 */
public class SmartSamWriterTest extends TestCase {

  static SAMRecord createRecord(int position) {
    final String ref = "A";
    final SAMRecord rec = createRecord(position, ref);
    return rec;
  }

  private static SAMRecord createRecord(int position, String ref) {
    final SAMFileHeader header = new SAMFileHeader();
    header.getSequenceDictionary().addSequence(new SAMSequenceRecord("A", 100000));
    header.getSequenceDictionary().addSequence(new SAMSequenceRecord("B", 100000));
    final SAMRecord rec = new SAMRecord(header);
    rec.setReferenceName(ref);
    rec.setReadName("read");
    rec.setReadString("ACGT");
    rec.setBaseQualityString("####");
    if (ref.equals(SAMRecord.NO_ALIGNMENT_REFERENCE_NAME)) {
      rec.setReadUnmappedFlag(true);
    } else {
      rec.setCigarString("4=");
      rec.setAlignmentStart(position);
    }
    return rec;
  }

  public void test() throws IOException {
    final SAMRecord[] records = {
      createRecord(1300),
      createRecord(1400),
      createRecord(1500),
      createRecord(1600),
      createRecord(2000),
      createRecord(70, "B"),
      createRecord(71, "B"),
      createRecord(0, SAMRecord.NO_ALIGNMENT_REFERENCE_NAME),
      createRecord(0, SAMRecord.NO_ALIGNMENT_REFERENCE_NAME),
      createRecord(0, SAMRecord.NO_ALIGNMENT_REFERENCE_NAME)
    };
    final MemoryPrintStream mps = new MemoryPrintStream();
    final SmartSamWriter smartSamWriter = new SmartSamWriter(new SAMFileWriterFactory().makeSAMWriter(records[0].getHeader(), true, mps.outputStream()));

    smartSamWriter.addRecord(records[2]);
    smartSamWriter.addRecord(records[0]);
    smartSamWriter.addRecord(records[4]);
    smartSamWriter.addRecord(records[3]);
    smartSamWriter.addRecord(records[1]);
    smartSamWriter.addRecord(records[5]);
    smartSamWriter.addRecord(records[6]);
    smartSamWriter.addRecord(records[7]);
    smartSamWriter.addRecord(records[8]);
    smartSamWriter.addRecord(records[9]);
    smartSamWriter.close();
    final StringBuilder sb = new StringBuilder();
    sb.append(SamUtils.getHeaderAsString(records[0].getHeader()));
    for (SAMRecord r : records) {
      sb.append(r.getSAMString());
    }
    assertEquals(sb.toString(), mps.toString());
  }

}