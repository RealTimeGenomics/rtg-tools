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

import java.util.Iterator;

import junit.framework.TestCase;

/**
 */
public class BasicLinkedListNodeTest extends TestCase {

  private BasicLinkedListNode<String> getList(String[] values) {
    BasicLinkedListNode<String> head = null;
    for (int i = values.length - 1; i >= 0; --i) {
      head = new BasicLinkedListNode<>(values[i], head);
    }
    return head;
  }

  public void test() {
    final String[] values = {"a", "b", "c", "d"};
    final BasicLinkedListNode<String> f = getList(values);
    assertEquals("a", f.getValue());
    assertEquals(4, f.size());
    Iterator<String> it = f.iterator();
    for (String s : values) {
      assertTrue(it.hasNext());
      assertEquals(s, it.next());
    }
    assertFalse(it.hasNext());

    final BasicLinkedListNode<String> firstSide = new BasicLinkedListNode<>("first", f);
    final BasicLinkedListNode<String> secondSide = new BasicLinkedListNode<>("second", f);
    assertEquals(5, firstSide.size());
    assertEquals(5, secondSide.size());

    it = firstSide.iterator();
    assertTrue(it.hasNext());
    assertEquals("first", it.next());
    for (String s : values) {
      assertTrue(it.hasNext());
      assertEquals(s, it.next());
    }
    assertFalse(it.hasNext());

    it = secondSide.iterator();
    assertTrue(it.hasNext());
    assertEquals("second", it.next());
    for (String s : values) {
      assertTrue(it.hasNext());
      assertEquals(s, it.next());
    }
    assertFalse(it.hasNext());
  }

  public void testNotSupported() {
    final BasicLinkedListNode<String> list = new BasicLinkedListNode<>("second", null);
    final Iterator<String> it = list.iterator();
    try {
      it.remove();
      fail("Expected exception not thrown");
    } catch (UnsupportedOperationException e) {
      assertEquals("Remove not supported", e.getMessage());
    }
  }

}
