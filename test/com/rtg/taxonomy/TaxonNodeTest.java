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

package com.rtg.taxonomy;

import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

/**
 */
public class TaxonNodeTest extends TestCase {

  public void testSetters() {
    final TaxonNode tn = new TaxonNode(123);
    assertEquals(123, tn.getId());
    assertNull(tn.getName());
    assertNull(tn.getRank());
    assertNull(tn.getParent());
    assertEquals(-1, tn.getParentId());

    tn.setName("name");
    tn.setRank("rank");

    assertEquals(123, tn.getId());
    assertEquals("name", tn.getName());
    assertEquals("rank", tn.getRank());
    assertNull(tn.getParent());
    assertEquals(-1, tn.getParentId());

    final TaxonNode tn2 = new TaxonNode(123, "name", "rank");
    assertTrue(tn.equals(tn2));
    assertEquals(0, tn.compareTo(tn2));

    tn2.setName("name2");
    tn2.setRank("rank\ta bc\n");

    assertTrue(tn.equals(tn2));
    assertEquals(0, tn.compareTo(tn2));

    assertEquals(123, tn2.getId());
    assertEquals("name2", tn2.getName());
    assertEquals("rank a bc ", tn2.getRank());
    assertNull(tn2.getParent());
    assertEquals(-1, tn2.getParentId());
  }

  public void testSimpleTree() {
    final HashMap<Integer, TaxonNode> nodes = new HashMap<>();
    final TaxonNode root = new TaxonNode(1, "root", "root");
    nodes.put(1, root);
    for (int i = 2; i <= 10; i++) {
      final TaxonNode node = new TaxonNode(i, "node" + i, "rank" + i);
      nodes.put(i, node);
      nodes.get((i - 2) / 3 + 1).addChild(node);
    }

    List<TaxonNode> traverse = root.depthFirstTraversal();
    assertEquals(10, traverse.size());

    int i = 0;
    final int[] ids = {1, 2, 5, 6, 7, 3, 8, 9, 10, 4};
    for (TaxonNode tn : traverse) {
      assertEquals(ids[i], tn.getId());
      if (tn.getId() != 1) {
        assertEquals((tn.getId() - 2) / 3 + 1, tn.getParentId());
        assertEquals(tn.getParent().getId(), tn.getParentId());
      } else {
        assertEquals(-1, tn.getParentId());
        assertNull(tn.getParent());
      }
      i++;
    }

    final TaxonNode node = nodes.get(10);
    node.detach();
    traverse = root.depthFirstTraversal();
    assertEquals(9, traverse.size());
  }

  public void testCommon() {
    final TaxonNode tn = new TaxonNode(123, "name", "rank");
    assertEquals("123\t-1\trank\tname", tn.toString());
    assertEquals(123, tn.hashCode());
    assertFalse(tn.equals(null));
    assertTrue(tn.equals(tn));
    assertEquals(0, tn.compareTo(tn));

    final TaxonNode tn2 = new TaxonNode(234, "name2", "rank2");
    assertEquals(111, tn2.compareTo(tn));
    assertEquals(-111, tn.compareTo(tn2));
    assertFalse(tn.equals(tn2));
  }
}
