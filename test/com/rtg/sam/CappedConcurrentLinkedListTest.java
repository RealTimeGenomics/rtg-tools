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
package com.rtg.sam;

import java.util.ArrayList;

import junit.framework.TestCase;


/**
 */
public class CappedConcurrentLinkedListTest extends TestCase {

  public void test() {
    final CappedConcurrentLinkedList<Integer> ll = new CappedConcurrentLinkedList<>(1, 0);
    final Integer one = 1;
    ll.add(one);
    assertEquals(one, ll.peek());
    assertEquals(one, ll.poll());
    ll.close();
    assertNull(ll.poll());
    assertNull(ll.peek());
    assertEquals("CappedConcurrentLinkedList id: 0 size: 0 hasNext: " + true + " closed: " + true, ll.toString());
    //TODO test all the threading stuff....
  }

  /**
   */
  public void testUnsupported() {
    final CappedConcurrentLinkedList<Integer> ll = new CappedConcurrentLinkedList<>(1, 0);
    try {
      ll.addAll(null);
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.element();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.iterator();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.offer(null);
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.remove();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.remove(null);
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.removeAll(null);
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    try {
      ll.retainAll(null);
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    //just ignored
    assertFalse(ll.contains(null));
    assertTrue(ll.containsAll(new ArrayList<Integer>()));
    ll.add(1);
    assertEquals(1, ll.size());
    try {
      ll.clear();
      fail();
    } catch (UnsupportedOperationException e) {
      //expected
    }
    assertFalse(ll.isEmpty());
    assertNotNull(ll.toArray());
    assertNotNull(ll.toArray(new Integer[ll.size()]));
  }
}
