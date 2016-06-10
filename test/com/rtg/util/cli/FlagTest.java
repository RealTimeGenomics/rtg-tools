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
package com.rtg.util.cli;

import java.util.Arrays;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

import junit.framework.TestCase;

/**
 * Test Flag
 */
public class FlagTest extends TestCase {

  static class DummyEnum implements PseudoEnum {
    public static final DummyEnum WORD = new DummyEnum(0, "WORD");
    public static final DummyEnum SEGMENT = new DummyEnum(1, "SEGMENT");

    private final int mOrdinal;
    private final String mName;

    DummyEnum(final int ordinal, final String name) {
      mOrdinal = ordinal;
      mName = name;
    }
    @Override
    public String name() {
      return mName;
    }
    @Override
    public int ordinal() {
      return mOrdinal;
    }
    @Override
    public String toString() {
      return mName;
    }
    private static final EnumHelper<DummyEnum> HELPER = new EnumHelper<>(DummyEnum.class, new DummyEnum[] {WORD, SEGMENT});

    public static DummyEnum[] values() {
      return HELPER.values();
    }

    public static DummyEnum valueOf(final String str) {
      return HELPER.valueOf(str);
    }
  }

  public void testIsValidEnum() {
    assertTrue(Flag.isValidEnum(DummyEnum.class));

    assertFalse(Flag.isValidEnum(String.class));
    assertFalse(Flag.isValidEnum(null));
  }

  public void testValues2() {
    final String[] values = Flag.values(DummyEnum.class);
    assertNotNull(values);
    assertEquals("[word, segment]", Arrays.toString(values));
    assertEquals(DummyEnum.WORD, Flag.valueOf(DummyEnum.class, "WORD"));
    assertEquals(DummyEnum.WORD, Flag.instanceHelper(DummyEnum.class, "WORD"));
    assertEquals(DummyEnum.WORD, Flag.instanceHelper(DummyEnum.class, "word"));
    assertEquals(DummyEnum.WORD, Flag.instanceHelper(DummyEnum.class, "WorD"));
  }

  public void testValues3() {
    assertNull(Flag.values(String.class));
    assertNull(Flag.values(null));
  }

  public void testHashCode() {
    final Flag anon = new Flag(null, null, "anonymous flag", 1, 1, Integer.class, "int", null, "");
    assertEquals(0, anon.hashCode());
  }

  public void testMinMax() {
    assertEquals("", Flag.minMaxUsage(0, 1, false)); //this will be dealt with by the normal optional usage
    assertEquals("", Flag.minMaxUsage(1, 1, false)); //this will be dealt with by the normal required usage
    assertEquals("May be specified up to 2 times", Flag.minMaxUsage(0, 2, false));
    assertEquals("May be specified up to 3 times, or as a comma separated list", Flag.minMaxUsage(0, 3, true));
    assertEquals("May be specified 0 or more times", Flag.minMaxUsage(0, Integer.MAX_VALUE, false));
    assertEquals("Must be specified 1 or 2 times", Flag.minMaxUsage(1, 2, false));
    assertEquals("Must be specified 1 to 3 times", Flag.minMaxUsage(1, 3, false));
    assertEquals("Must be specified 1 or more times", Flag.minMaxUsage(1, Integer.MAX_VALUE, false));
    assertEquals("Must be specified 2 times", Flag.minMaxUsage(2, 2, false));
    assertEquals("Must be specified 2 or 3 times", Flag.minMaxUsage(2, 3, false));
    assertEquals("Must be specified 2 to 4 times", Flag.minMaxUsage(2, 4, false));
    assertEquals("Must be specified 2 or more times", Flag.minMaxUsage(2, Integer.MAX_VALUE, false));
  }
}
