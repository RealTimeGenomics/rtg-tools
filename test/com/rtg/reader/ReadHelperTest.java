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
import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.mode.SequenceType;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class ReadHelperTest extends TestCase {

  /**
   */
  public ReadHelperTest(String name) {
    super(name);
  }

  public void testHandling() {
    final String seqString = "acgtcacgtcacgtcacgtcacgtcacgtcacgtc";
    Diagnostic.setLogStream();
    assertNull(ReadHelper.getRead(null, 0));
    assertNull(ReadHelper.getQual(null, 0));
    CompressedMemorySequencesReader msr = new CompressedMemorySequencesReader(new byte[][] {DnaUtils.encodeArray(seqString.getBytes())}, new String[] {"seq1"}, new long[] {35}, 35, 35, SequenceType.DNA) {
      private boolean mIsLeft = true;

      @Override
      public PrereadArm getArm() {
        try {
          return mIsLeft ? PrereadArm.LEFT : PrereadArm.RIGHT;
        } finally {
          mIsLeft = false;
        }
      }

      @Override
      public PrereadType getPrereadType() {
        return PrereadType.UNKNOWN;
      }

      @Override
      public boolean equals(Object o) {
        return o == this;
      }

      @Override
      public int hashCode() {
        return 1;
      }



    };
    try {
      assertTrue(Arrays.equals(DnaUtils.encodeArray("acgtcacgtcacgtcacgtcacgtcacgtcacgtc".getBytes()), ReadHelper.getRead(msr, 0)));
      assertTrue(Arrays.equals(DnaUtils.encodeArray("acgtcacgtcacgtcacgtcacgtcacgtcacgtc".getBytes()), ReadHelper.getRead(msr, 0)));
      assertNull(ReadHelper.getQual(msr, 0));
    } finally {
      msr.close();
    }
    msr = new CompressedMemorySequencesReader(new byte[][] {DnaUtils.encodeArray(seqString.getBytes())}, new String[] {"seq1"}, new long[] {35}, 35, 35, SequenceType.DNA) {
      private boolean mIsLeft = true;

      @Override
      public PrereadArm getArm() {
        try {
          return mIsLeft ? PrereadArm.LEFT : PrereadArm.RIGHT;
        } finally {
          mIsLeft = false;
        }
      }

      @Override
      public PrereadType getPrereadType() {
        return PrereadType.UNKNOWN;
      }

      @Override
      public boolean equals(Object o) {
        return o == this;
      }

      @Override
      public int hashCode() {
        return 1;
      }
    };
    try {
      assertTrue(Arrays.equals(DnaUtils.encodeArray("acgtcacgtcacgtcacgtcacgtcacgtcacgtc".getBytes()), ReadHelper.getRead(msr, 0)));
    } finally {
      msr.close();
    }
  }

  public void testCGQuality() throws Exception {
    Diagnostic.setLogStream();
    final File temp = FileUtils.createTempDir("cgblah", "qual");
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(new ByteArrayInputStream(("@testQuality\n"
          + "actgcatc\n"
          + "+\n"
          + "!<><##!<").getBytes()));
      final FastqSequenceDataSource fq = new FastqSequenceDataSource(al, QualityFormat.SANGER);
      final SequencesWriter sw = new SequencesWriter(fq, temp, 20, PrereadType.CG, false);
      sw.processSequences();
      try (SequencesReader msr = CompressedMemorySequencesReader.createSequencesReader(temp, true, false, LongRange.NONE)) {
        assertTrue(Arrays.equals(DnaUtils.encodeArray("actgcatc".getBytes()), ReadHelper.getRead(msr, 0)));
        assertTrue(Arrays.equals(new byte[]{0, 27, 29, 27, 2, 2, 0, 27}, ReadHelper.getQual(msr, 0)));
      }
    } finally {
      FileHelper.deleteAll(temp);
    }
  }
}
