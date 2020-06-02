/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 */
public class AnsiDisplayHelperTest extends DisplayHelperTest {

  @Test
  public void testAnsiColors() {
    assertEquals((char) 27 + "[48;5;17m", AnsiDisplayHelper.ansiBackground(DisplayHelper.BLUE));
    assertEquals((char) 27 + "[31m", AnsiDisplayHelper.ansiForeground(DisplayHelper.RED));
  }

  /** Expected exception handler */
  @Rule
  public ExpectedException mExpectedException = ExpectedException.none();
  @Test
  public void testColorExceptionRed() {
    mExpectedException.expect(IllegalArgumentException.class);
    AnsiDisplayHelper.extendedColor(6, 5, 5);
  }
  @Test
  public void testColorExceptionGreen() {
    mExpectedException.expect(IllegalArgumentException.class);
    AnsiDisplayHelper.extendedColor(5, 6, 5);
  }
  @Test
  public void testColorExceptionBlue() {
    mExpectedException.expect(IllegalArgumentException.class);
    AnsiDisplayHelper.extendedColor(5, 5, 6);
  }
  @Test
  public void testExtendedColorWhite() {
    assertEquals(231, AnsiDisplayHelper.extendedColor(5, 5, 5));
  }

  @Test
  public void testMarkupStart() {
    final AnsiDisplayHelper helper = new AnsiDisplayHelper();
    assertTrue(helper.isMarkupStart((char) 0x1b));
    assertFalse(helper.isMarkupStart('f'));
  }

  @Test
  public void testMarkupEnd() {
    final AnsiDisplayHelper helper = new AnsiDisplayHelper();
    assertTrue(helper.isMarkupEnd('m'));
    assertFalse(helper.isMarkupEnd((char) 0x1b));
    assertFalse(helper.isMarkupEnd((char) 0x00));
  }
}
