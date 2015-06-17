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
package com.rtg.relation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 */
public class MultiFamilyOrderingTest extends TestCase {

  public void testSuperDuperUltraSimple() {
    final Set<Family> fams = new HashSet<>();
    final Family fam = new Family("father", "mother", "child1", "child2", "child3");
    fams.add(fam);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(1, famsO.size());
    assertTrue(fams.contains(famsO.get(0)));
    assertEquals(1, fams.size());
    assertTrue(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(1, fam.getFatherDistinctMates());
    assertEquals(1, fam.getMotherDistinctMates());
    //System.err.println(famsO);
  }

  public void testMultiGen() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen2 = new Family("child1", "dInLaw1", "gchild1", "gchild2");
    fams.add(gen2);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(2, famsO.size());
    assertEquals(gen1, famsO.get(0));
    assertEquals(gen2, famsO.get(1));
    fams.clear();
    fams.add(gen1);
    fams.add(gen2);
    assertEquals(2, famsO.size());
    assertEquals(gen1, famsO.get(0));
    assertEquals(gen2, famsO.get(1));
    assertTrue(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(1, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
  }

  public void testMultiGenExtra() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen2 = new Family("child1", "dInLaw1", "gchild1", "gchild2");
    final Family gen2b = new Family("sInLaw1", "child3", "gchild4", "gchild5");
    fams.add(gen2);
    fams.add(gen2b);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(3, famsO.size());
    assertEquals(gen1, famsO.get(0));
    assertTrue(famsO.contains(gen2));
    assertTrue(famsO.contains(gen2b));
    assertTrue(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(1, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(1, gen2b.getFatherDistinctMates());
    assertEquals(1, gen2b.getMotherDistinctMates());
  }

  public void testMultiGenDifferentFamilies() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen2 = new Family("child1", "dInLaw1", "gchild1", "gchild2");
    final Family gen1b = new Family("barney", "betty", "bambam");
    final Family gen2b = new Family("bambam", "pebbles", "gchild4");
    fams.add(gen1);
    fams.add(gen2);
    fams.add(gen1b);
    fams.add(gen2b);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(4, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2, gen2b)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2b));
    assertTrue(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(1, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(1, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(1, gen2b.getFatherDistinctMates());
    assertEquals(1, gen2b.getMotherDistinctMates());
  }

  public void testGG() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen2 = new Family("child1", "dInLaw1", "gchild1", "gchild2");
    final Family gen1b = new Family("barney", "betty", "bambam");
    final Family gen2b = new Family("bambam", "pebbles", "gchild4");
    final Family gen3 = new Family("gchild4", "gchild2", "ggchild0");
    fams.add(gen1);
    fams.add(gen2);
    fams.add(gen3);
    fams.add(gen1b);
    fams.add(gen2b);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(5, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2, gen2b, gen3)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2b));
    assertTrue(famsO.indexOf(gen2b) < famsO.indexOf(gen3));
    assertTrue(famsO.indexOf(gen2) < famsO.indexOf(gen3));
    assertTrue(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(1, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(1, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(1, gen2b.getFatherDistinctMates());
    assertEquals(1, gen2b.getMotherDistinctMates());
    assertEquals(1, gen3.getFatherDistinctMates());
    assertEquals(1, gen3.getMotherDistinctMates());
  }

  public void testUnfaithful() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen1b = new Family("father", "newWife", "child4", "child5");
    fams.add(gen1b);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(2, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b)));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(2, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(2, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
  }
  public void testUnfaithfulWithGKids() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen1b = new Family("father", "newWife", "child4", "child5");
    final Family gen2 = new Family("child1", "dInLaw1", "gchild1");
    final Family gen2b = new Family("sInLaw", "child4", "gchild2");
    fams.add(gen1b);
    fams.add(gen2);
    fams.add(gen2b);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(4, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2, gen2b)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2b));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2b));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(2, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(2, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(1, gen2b.getFatherDistinctMates());
    assertEquals(1, gen2b.getMotherDistinctMates());
  }

  public void testUnfaithfulWithIncestuousHalfSibs() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen1b = new Family("father", "newWife", "child4", "child5");
    final Family gen2 = new Family("child1", "child4", "gchild1");
    fams.add(gen1b);
    fams.add(gen2);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(3, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));

    assertEquals(2, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(2, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
  }

  public void testBigDaddy() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen2 = new Family("father", "child2", "gchild1", "gchild2");
    final Family gen3 = new Family("father", "gchild1", "ggchild1", "ggchild2");
    final Family gen4 = new Family("father", "ggchild2", "gggchild1", "gggchild2");
    fams.add(gen2);
    fams.add(gen3);
    fams.add(gen4);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(4, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen2, gen3, gen4)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen2) < famsO.indexOf(gen3));
    assertTrue(famsO.indexOf(gen3) < famsO.indexOf(gen4));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));
    assertEquals(4, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(4, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(4, gen3.getFatherDistinctMates());
    assertEquals(1, gen3.getMotherDistinctMates());
    assertEquals(4, gen4.getFatherDistinctMates());
    assertEquals(1, gen4.getMotherDistinctMates());
  }

  public void testBigDaddyNewWife() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen1b = new Family("father", "newWife", "child4", "child5");
    final Family gen2 = new Family("father", "child3", "gchild1");
    final Family gen2b = new Family("father", "child4", "gchild2");
    fams.add(gen2b);
    fams.add(gen1b);
    fams.add(gen2);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(4, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2, gen2b)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2b));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));

    assertEquals(4, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(4, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(4, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(4, gen2b.getFatherDistinctMates());
    assertEquals(1, gen2b.getMotherDistinctMates());
  }

  public void testBigDaddyGchild() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen1b = new Family("father", "newWife", "child4", "child5");
    final Family gen2 = new Family("child1", "child4", "gchild1");
    final Family gen3 = new Family("father", "gchild1", "ggchild2");
    fams.add(gen3);
    fams.add(gen1b);
    fams.add(gen2);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(4, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2, gen3)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen2) < famsO.indexOf(gen3));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));

    assertEquals(3, gen1.getFatherDistinctMates());
    assertEquals(1, gen1.getMotherDistinctMates());
    assertEquals(3, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(1, gen2.getMotherDistinctMates());
    assertEquals(3, gen3.getFatherDistinctMates());
    assertEquals(1, gen3.getMotherDistinctMates());
  }

  public void testBigDaddyAndBigMommaGchild() {
    final Set<Family> fams = new LinkedHashSet<>();
    final Family gen1 = new Family("father", "mother", "child1", "child2", "child3");
    final Family gen1b = new Family("father", "newWife", "child4", "child5");
    final Family gen2 = new Family("child1", "mother", "gchild1");
    final Family gen3 = new Family("father", "gchild1", "ggchild2");
    fams.add(gen3);
    fams.add(gen1b);
    fams.add(gen2);
    fams.add(gen1);
    final List<Family> famsO = MultiFamilyOrdering.orderFamiliesAndSetMates(fams);
    assertEquals(4, famsO.size());
    assertTrue(famsO.containsAll(Arrays.asList(gen1, gen1b, gen2, gen3)));
    assertTrue(famsO.indexOf(gen1) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen1b) < famsO.indexOf(gen2));
    assertTrue(famsO.indexOf(gen2) < famsO.indexOf(gen3));
    assertFalse(MultiFamilyOrdering.isMonogamous(famsO));

    assertEquals(3, gen1.getFatherDistinctMates());
    assertEquals(2, gen1.getMotherDistinctMates());
    assertEquals(3, gen1b.getFatherDistinctMates());
    assertEquals(1, gen1b.getMotherDistinctMates());
    assertEquals(1, gen2.getFatherDistinctMates());
    assertEquals(2, gen2.getMotherDistinctMates());
    assertEquals(3, gen3.getFatherDistinctMates());
    assertEquals(1, gen3.getMotherDistinctMates());
  }
}
