/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.vcf.annotation;

import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 */
public class FormatterTest extends TestCase {

  public void testFormatting() {
    assertEquals("1", Formatter.DEFAULT.toString(1));
    assertEquals("hello", Formatter.DEFAULT.toString("hello"));
    assertEquals("1.11111", Formatter.DEFAULT.toString(1.11111));
    assertEquals("1.111", Formatter.DEFAULT_DOUBLE.toString(1.11111));
  }

  public void testGetFormatter() {
    assertEquals(Formatter.DEFAULT, Formatter.getFormatter(new FormatField("A", MetaType.INTEGER, VcfNumber.ONE, "description")));
    assertEquals(Formatter.DEFAULT, Formatter.getFormatter(new FormatField("A", MetaType.STRING, VcfNumber.ONE, "description")));
    assertEquals(Formatter.DEFAULT, Formatter.getFormatter(new FormatField("A", MetaType.CHARACTER, VcfNumber.ONE, "description")));
    assertEquals(Formatter.DEFAULT_DOUBLE, Formatter.getFormatter(new FormatField("A", MetaType.FLOAT, VcfNumber.ONE, "description")));

    try {
      Formatter.getFormatter(new FormatField("A", MetaType.FLAG, VcfNumber.ONE, "description"));
      fail();
    } catch (IllegalArgumentException ignored) {
    }
    try {
      Formatter.getFormatter(new FormatField("A", MetaType.INTEGER, VcfNumber.ALTS, "description"));
      fail();
    } catch (IllegalArgumentException ignored) {
    }
  }
}
