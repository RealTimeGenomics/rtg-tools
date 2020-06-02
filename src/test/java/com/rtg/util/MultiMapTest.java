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
package com.rtg.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import junit.framework.TestCase;



/**
 * Test class for MultiMap.
 */
public class MultiMapTest extends TestCase {

  private static final String EXPECTED = ""
    + "{" + StringUtils.LS
    + " 10 -> [5, 6, 7, 10]" + StringUtils.LS
    + "}" + StringUtils.LS
    ;

  public void testDefaultCon() {
    final MultiMap<Integer, String> mm = new MultiMap<>();
    checkMap(mm, EXPECTED);
  }

  public void testCon() {
    final MultiMap<Integer, String> mm = new MultiMap<>(new HashMap<Integer, Collection<String>>()
        , new MultiMapFactory<String>() {
      @Override
      public Collection<String> createCollection() {
        return new ArrayList<>();
      }
    });
    checkMap(mm, EXPECTED);
  }

  public void checkMap(final MultiMap<Integer, String> mm, final String expected) {
    assertEquals(0, mm.size());
    assertEquals(0, mm.values().size());
    assertNull(mm.get(5));
    mm.put(3, "1");
    mm.put(3, "2");
    mm.put(3, "3");
    mm.put(7, "1");
    mm.put(9, "1");
    mm.put(7, "2");
    assertEquals(Arrays.asList("1", "2", "3"), mm.get(3));
    assertEquals(Arrays.asList("1", "2"), mm.get(7));
    assertEquals(3, mm.size());
    assertEquals(3, mm.values().size());
    assertEquals(2, mm.remove(7).size());
    assertEquals(2, mm.size());
    assertEquals(2, mm.values().size());
    mm.clear();
    assertEquals(null, mm.get(3));
    assertEquals(0, mm.size());
    assertEquals(0, mm.values().size());
    mm.put(10, "2");
    assertEquals(1, mm.size());
    assertEquals(1, mm.values().size());
    assertEquals(Arrays.asList("2"), mm.get(10));
    assertEquals(1, mm.set(10, Arrays.asList("5", "6", "7")).size());
    assertEquals(1, mm.size());
    assertEquals(Arrays.asList("5", "6", "7"), mm.get(10));
    mm.put(10, "10");
    assertEquals(1, mm.size());
    assertEquals(1, mm.values().size());
    assertEquals(Arrays.asList("5", "6", "7", "10"), mm.get(10));
    assertEquals(expected, mm.toString());
  }
}

