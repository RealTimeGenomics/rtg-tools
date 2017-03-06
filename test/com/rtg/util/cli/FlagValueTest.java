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


import junit.framework.TestCase;

/**
 *
 *
 *
 */
public class FlagValueTest extends TestCase {

  /**
   */
  public FlagValueTest(final String name) {
    super(name);
  }

  public void testFlagIntVlaue() {
    final Flag<Integer> f = new Flag<>('I', "integer", "integer Flag", 1, 1, Integer.class, "Integer Value", Integer.MAX_VALUE, "");
    final FlagValue<Integer> v = new FlagValue<>(f, 55);
    assertEquals((Integer) 55, v.getValue());
    assertEquals("integer=55", v.toString());
    final Flag<Integer> n = v.getFlag();
    assertEquals(f.isSet(), n.isSet());
    assertEquals(f.getCount(), n.getCount());
    assertEquals(f.getDescription(), n.getDescription());
    assertEquals(f.getMaxCount(), n.getMaxCount());
    assertEquals(f.getMinCount(), n.getMinCount());
    assertEquals(f.getChar(), n.getChar());
    assertEquals(f.getFlagUsage(), n.getFlagUsage());
    assertEquals(f.getCompactFlagUsage(), n.getCompactFlagUsage());
    assertTrue(f.equals(n));
  }

  public void testFlagDoubleVlaue() {
    final Flag<Double> f = new Flag<>('D', "double", "double Flag", 1, 1, Double.class, "Double Value", Double.MAX_VALUE, "");
    final FlagValue<Double> v = new FlagValue<>(f, 55.000256);
    assertEquals(55.000256, v.getValue());
    assertEquals("double=55.000256", v.toString());
    final Flag<?> n = v.getFlag();
    assertEquals(f.isSet(), n.isSet());
    assertEquals(f.getCount(), n.getCount());
    assertEquals(f.getDescription(), n.getDescription());
    assertEquals(f.getMaxCount(), n.getMaxCount());
    assertEquals(f.getMinCount(), n.getMinCount());
    assertEquals(f.getChar(), n.getChar());
    assertEquals(f.getFlagUsage(), n.getFlagUsage());
    assertEquals(f.getCompactFlagUsage(), n.getCompactFlagUsage());
    assertTrue(f.equals(n));
  }
}
