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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rtg.util.MultiMap;

/**
 * Creates an ordering for processing families.
 */
public final class MultiFamilyOrdering {

  private MultiFamilyOrdering() { }

  /**
   * Detects the presence of genetic non-monogamy within an set of families
   * @param families the families to test
   * @return true or false
   */
  public static boolean isMonogamous(Collection<Family> families) {
    final HashSet<String> parents = new HashSet<>();
    for (Family f : families) {
      if (!parents.add(f.getFather())) {
        return false;
      }
      if (!parents.add(f.getMother())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Detects the presence of genetic non-monogamy within an set of families
   * @param families the families to test
   * @return the samples that exist as parents in multiple families
   */
  public static Set<String> nonMonogamousSamples(Collection<Family> families) {
    final LinkedHashSet<String> ret = new LinkedHashSet<>();
    final HashSet<String> parents = new HashSet<>();
    for (Family f : families) {
      if (!parents.add(f.getFather())) {
        ret.add(f.getFather());
      }
      if (!parents.add(f.getMother())) {
        ret.add(f.getMother());
      }
    }
    return ret;
  }

  /**
   * Create an ordering for a set of families such that any family appears before
   * a family that represents the next generation of one of its children members.
   * <br>
   * As a side effect we also set the number of mates parents have in the family object
   * @param families the families to order
   * @return the ordered families
   * @throws PedigreeException if cycles are detected in the pedigree
   */
  public static List<Family> orderFamiliesAndSetMates(Set<Family> families) throws PedigreeException {
    final MultiMap<String, Family> parents = new MultiMap<>(true);
    final HashMap<String, Integer> parentPairCounts = new LinkedHashMap<>();
    final HashMap<String, Family> parentPairToFamily = new HashMap<>();
    for (Family f : families) {
      parents.put(f.getMother(), f);
      parents.put(f.getFather(), f);
      final String lookupName = f.getFather() + " " + f.getMother();
      parentPairCounts.put(lookupName, 0);
      parentPairToFamily.put(lookupName, f);
    }
    //building up edge counts from a family's generation to its next generation
    for (Family genCurrent : families) {
      for (String genCurrentChild : genCurrent.getChildren()) {
        final Collection<Family> genNextFamilies = parents.get(genCurrentChild);
        if (genNextFamilies != null) {
          for (Family genNextFamily : genNextFamilies) {
            final String lookupName = genNextFamily.getFather() + " " + genNextFamily.getMother();
            parentPairToFamily.put(lookupName, genNextFamily);
            int count = parentPairCounts.get(lookupName);
            ++count;
            parentPairCounts.put(lookupName, count);
          }
        }
      }
    }
    //find "first" generation families
    final ArrayDeque<Family> zeroAncestors = new ArrayDeque<>();
    final Iterator<Map.Entry<String, Integer>> it = parentPairCounts.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry<String, Integer> me = it.next();
      if (me.getValue() == 0) {
        zeroAncestors.add(parentPairToFamily.get(me.getKey()));
        it.remove();
      }
    }
    //Kahn!!!!
    final ArrayList<Family> ret = new ArrayList<>();
    while (!zeroAncestors.isEmpty()) {
      final Family n = zeroAncestors.removeFirst();
      ret.add(n);
      for (String child : n.getChildren()) {
        final Collection<Family> genNextFamilies = parents.get(child);
        if (genNextFamilies != null) {
          for (Family genNextFamily : genNextFamilies) {
            final String lookupName = genNextFamily.getFather() + " " + genNextFamily.getMother();
            int count = parentPairCounts.get(lookupName);
            --count;
            if (count == 0) {
              zeroAncestors.add(genNextFamily);
              parentPairCounts.remove(lookupName);
            } else {
              parentPairCounts.put(lookupName, count);
            }
          }
        }
      }
    }
    if (parentPairCounts.size() > 0) {
      throw new PedigreeException("Cycles in pedigree detected, check pedigree structure");
    }
    //extra stuff
    for (Family f : families) {
      f.setFatherDistinctMates(parents.get(f.getFather()).size());
      f.setMotherDistinctMates(parents.get(f.getMother()).size());
    }
    for (Map.Entry<String, Collection<Family>> entry : parents.entrySet()) {
      final Collection<Family> fams = entry.getValue();
      int id = 0;
      Boolean father = null;
      for (Family f : fams) {
        if (father == null) {
          father = f.getFather().equals(entry.getKey());
        }
        if (father) {
          f.setFatherFamilyId(id++);
        } else {
          f.setMotherFamilyId(id++);
        }
      }
    }
    return ret;
  }
}
