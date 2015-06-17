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
package com.rtg.mode;

import static com.rtg.mode.SequenceMode.BIDIRECTIONAL;
import static com.rtg.mode.SequenceMode.PROTEIN;
import static com.rtg.mode.SequenceMode.TRANSLATED;
import static com.rtg.mode.SequenceMode.UNIDIRECTIONAL;

import java.io.ObjectStreamException;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class SequenceModeTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.SequenceMode()}.
   */
  public final void test() {
    TestUtils.testPseudoEnum(SequenceMode.class, "[PROTEIN, BIDIRECTIONAL, UNIDIRECTIONAL, TRANSLATED]");
  }

  public void testReadResolve() throws ObjectStreamException {
    for (SequenceMode t : SequenceMode.values()) {
      assertEquals(t, t.readResolve());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode()}.
   */
  public final void testHashEquals() {
    TestUtils.equalsTest(SequenceMode.values());
    final SequenceMode[][] vg = new SequenceMode[SequenceMode.values().length][];
    for (int i = 0; i < vg.length; i++) {
      vg[i] = new SequenceMode[1];
    }
    for (final Object sm : SequenceMode.values()) {
      vg[((SequenceMode) sm).ordinal()][0] = (SequenceMode) sm;
    }
    TestUtils.equalsHashTest(vg);
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode()}.
   */
  public final void testHash() {
    for (final Object sm : SequenceMode.values()) {
      assertEquals(((SequenceMode) sm).ordinal() + 1, sm.hashCode());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode#type()}.
   */
  public final void testType() {
    assertEquals(SequenceType.PROTEIN, PROTEIN.type());
    assertEquals(SequenceType.DNA, BIDIRECTIONAL.type());
    assertEquals(SequenceType.DNA, UNIDIRECTIONAL.type());
    assertEquals(SequenceType.DNA, TRANSLATED.type());
    assertEquals("PROTEIN", SequenceMode.PROTEIN.name());
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode#type()}.
   */
  public final void testCodeType() {
    assertEquals(SequenceType.PROTEIN, PROTEIN.codeType());
    assertEquals(SequenceType.DNA, BIDIRECTIONAL.codeType());
    assertEquals(SequenceType.DNA, UNIDIRECTIONAL.codeType());
    assertEquals(SequenceType.PROTEIN, TRANSLATED.codeType());
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode#numberFrames()}.
   */
  public final void testNumberFrames() {
    assertEquals(1, PROTEIN.numberFrames());
    assertEquals(2, BIDIRECTIONAL.numberFrames());
    assertEquals(1, UNIDIRECTIONAL.numberFrames());
    assertEquals(6, TRANSLATED.numberFrames());
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode#numberFrames()}.
   */
  public final void testCodeIncrement() {
    assertEquals(1, PROTEIN.codeIncrement());
    assertEquals(1, BIDIRECTIONAL.codeIncrement());
    assertEquals(1, UNIDIRECTIONAL.codeIncrement());
    assertEquals(3, TRANSLATED.codeIncrement());
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode#frameFromCode(int)}.
   */
  public final void testFrameFromCode() {
    assertEquals(ProteinFrame.PROTEIN, PROTEIN.frameFromCode(0));
    assertEquals(UnidirectionalFrame.FORWARD, UNIDIRECTIONAL.frameFromCode(0));
    assertEquals(BidirectionalFrame.FORWARD, BIDIRECTIONAL.frameFromCode(0));
    assertEquals(BidirectionalFrame.REVERSE, BIDIRECTIONAL.frameFromCode(1));
    assertEquals(TranslatedFrame.FORWARD1, TRANSLATED.frameFromCode(0));
    assertEquals(TranslatedFrame.FORWARD2, TRANSLATED.frameFromCode(1));
    assertEquals(TranslatedFrame.FORWARD3, TRANSLATED.frameFromCode(2));
    assertEquals(TranslatedFrame.REVERSE1, TRANSLATED.frameFromCode(3));
    assertEquals(TranslatedFrame.REVERSE2, TRANSLATED.frameFromCode(4));
    assertEquals(TranslatedFrame.REVERSE3, TRANSLATED.frameFromCode(5));

    try {
      assertEquals(TranslatedFrame.REVERSE3, TRANSLATED.frameFromCode(6));
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      // expected
    }

  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode#valueOf(String)}.
   */
  public final void testValueOf() {
    try {
      SequenceMode.valueOf("XX");
      fail("IllegalArgumentException expected");
    } catch (final IllegalArgumentException e) {
      assertEquals("XX", e.getMessage());

    }

    try {
      SequenceMode.valueOf("");
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }

  }

  /**
   * Test method for {@link com.rtg.mode.SequenceMode}}.
   * Checks the methods that deal with converting internal/external ids.
   */
  public final void testIntExt() {
    for (final Object sm : SequenceMode.values()) {
      final Frame[] frames = ((SequenceMode) sm).allFrames();
      for (final Frame fr : frames) {
        checkIntExt((SequenceMode) sm, fr, 0, 0);
        checkIntExt((SequenceMode) sm, fr, 25, 24);
        checkIntExt((SequenceMode) sm, fr, (Integer.MAX_VALUE / 6) - 1, 0);
      }
    }
    try {
      checkIntExt(SequenceMode.TRANSLATED, TranslatedFrame.FORWARD3, Integer.MAX_VALUE / 6, 0);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      assertEquals("Internal id out of range. id=357913941 frame=FORWARD3 frame ord=2 internal id=2147483648", e.getMessage());
    }
  }

  private void checkIntExt(final SequenceMode sm, final Frame fr, final long externalId, final long offset) {
    final int internal = sm.internalId(externalId, offset, fr);
    final Frame fra = sm.frame(internal);
    assertEquals(fr, fra);
    final long exta = sm.sequenceId(internal, offset);
    assertEquals(externalId, exta);
  }


  public void testRE() {
    try {
      SequenceMode.BIDIRECTIONAL.internalId(1, 5, BidirectionalFrame.FORWARD);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("Internal id out of range. id=1 frame=FORWARD internal id=-8", e.getMessage());
    }
  }
}

