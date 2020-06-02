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

import java.io.IOException;

import com.rtg.mode.SequenceType;

import junit.framework.TestCase;

/**
 */
public class AlternatingSequencesReaderTest extends TestCase {

  public void testExceptions1() throws IllegalStateException, IOException {
    final SequencesReader first = new MockSequencesReader(SequenceType.DNA, 2L, 10L);
    final SequencesReader second = new MockSequencesReader(SequenceType.DNA, 2L, 10L);
    final SequencesIterator reader = new AlternatingSequencesReader(first, second).iterator();
    try {
      reader.currentLength();
      fail();
    } catch (IllegalStateException e) {
      //expected
    }
    try {
      reader.currentSequenceId();
      fail();
    } catch (IllegalStateException e) {
      //expected
    }
  }

  public void testExceptions3() throws IllegalStateException, IOException {
    final SequencesReader first = new MockSequencesReader(SequenceType.DNA, 2L, 10L);
    final SequencesReader second = new MockSequencesReader(SequenceType.DNA, 2L, 10L);
    final AlternatingSequencesReader reader = new AlternatingSequencesReader(first, second);
    try {
      reader.dataChecksum();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.qualityChecksum();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.nameChecksum();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.copy();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.path();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.getArm();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.getPrereadType();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.getSdfId();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.hasHistogram();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.hasQualityData();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.histogram();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.lengthBetween(0, 0);
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.longestNBlock();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.maxLength();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.minLength();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported yet.", e.getMessage());
    }
    reader.close();
  }

  public void testExceptions2() throws IllegalStateException, IOException {
    final SequencesReader first = new MockSequencesReader(SequenceType.DNA, 2L, 10L);
    final SequencesReader second = new MockSequencesReader(SequenceType.DNA, 2L, 10L);
    final AlternatingSequencesReader reader = new AlternatingSequencesReader(first, second);
    try {
      reader.nBlockCount();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.names();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.posHistogram();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.positionQualityAverage();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.residueCounts();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.sdfVersion();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.sequenceLengths(0, 0);
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    try {
      reader.type();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
      assertEquals("Not supported yet.", e.getMessage());
    }
    reader.close();
  }

  private static class FakeReader extends MockSequencesReader {
    private final int mReadLength;

    FakeReader(SequenceType sequenceType, long numberSquences, long length, int readLength) {
      super(sequenceType, numberSquences, length);
      mReadLength = readLength;
    }
    @Override
    public int length(long index) {
      return mReadLength + (int) index;
    }
  }

  public void test() throws IllegalStateException, IOException {
    final SequencesReader first = new FakeReader(SequenceType.DNA, 2L, 13L, 6);
    final SequencesReader second = new FakeReader(SequenceType.DNA, 2L, 11L, 5);
    final AlternatingSequencesReader reader = new AlternatingSequencesReader(first, second);
    assertEquals(4, reader.numberSequences());
    assertEquals(24, reader.totalLength());
    final SequencesIterator it = reader.iterator();
    assertTrue(it.nextSequence());
    assertEquals(0, it.currentSequenceId());
    assertEquals(6, it.currentLength());
    assertTrue(it.nextSequence());
    assertEquals(1, it.currentSequenceId());
    assertEquals(5, it.currentLength());
    assertTrue(it.nextSequence());
    assertEquals(2, it.currentSequenceId());
    assertEquals(7, it.currentLength());
    assertTrue(it.nextSequence());
    assertEquals(3, it.currentSequenceId());
    assertEquals(6, it.currentLength());
    assertFalse(it.nextSequence());
    reader.close();
  }
}
