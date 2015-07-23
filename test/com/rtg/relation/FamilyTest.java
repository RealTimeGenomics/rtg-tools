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

import static com.rtg.util.StringUtils.LS;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Set;

import junit.framework.TestCase;

/**
 */
public class FamilyTest extends TestCase {

  /** Relations for testing. */
  public static final String RELATIONS = ""
      + "genome father sex=male disease=" + false + LS
      + "genome mother disease=" + true + LS
      + "genome childa disease=" + false + LS
      + "genome childb disease=" + true + LS
      + "parent-child father childa" + LS
      + "parent-child father childb" + LS
      + "parent-child mother childa" + LS
      + "parent-child mother childb" + LS
      ;


  public void testIndexes() {
    assertTrue(Family.FATHER_INDEX != Family.MOTHER_INDEX);
    assertTrue(Family.FATHER_INDEX != Family.FIRST_CHILD_INDEX);
    assertTrue(Family.MOTHER_INDEX != Family.FIRST_CHILD_INDEX);
  }

  public void testClassic() throws Exception {
    final Family f = Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(RELATIONS))));
    assertTrue(f.getMother().equals("mother"));
    assertFalse(f.isDiseased(0));
    assertTrue(f.isDiseased(1));

    assertFalse(f.isDiseased("father"));
    assertTrue(f.isDiseased("mother"));
    assertEquals(2, f.getChildren().length);
    assertFalse(f.isDiseased("no-such-person"));
    assertTrue(f.isOneParentDiseased());

    assertFalse(f.isDiseased(2));
    assertTrue(f.isDiseased(3));
  }



  public void testMissingParent() throws Exception {
    final String input = ""
        + "parent-child mother childa" + LS
        + "parent-child mother childb" + LS
        ;
    try {
      Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(input))));
      fail();
    } catch (final IllegalArgumentException e) {
      // Expected
      assertEquals("There are fewer than two parents specified", e.getMessage());
    }
  }

  public void testChildHasOnlyOneParent() throws Exception {
    final String input = ""
        + "parent-child mother childa" + LS
        + "parent-child father childa" + LS
        + "parent-child father childb" + LS
        ;
    try {
      Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(input))));
      fail();
    } catch (final IllegalArgumentException e) {
      // Expected
      assertEquals("Child sample: 'childb' has 1 parents", e.getMessage());
    }
  }

  public void testScrewedUp() throws Exception {
    final String input = ""
        + "parent-child mother childa" + LS
        + "parent-child father childa" + LS
        + "parent-child father mother" + LS
        ;
    try {
      Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(input))));
      fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  public void testScrewedUp2() throws Exception {
    final String input = ""
        + "parent-child mother childa" + LS
        + "parent-child mother father" + LS
        + "parent-child father childa" + LS
        ;
    try {
      Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(input))));
      fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  public void testDoubleMother() throws Exception {
    final String input = ""
        + "parent-child mother childa" + LS
        + "parent-child mother childa" + LS
        + "parent-child father childb" + LS
        + "parent-child mother childb" + LS
        ;
    try {
      Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(input))));
      fail();
    } catch (final IllegalArgumentException e) {
      // Expected
      assertEquals("Child sample: 'childa' has 1 parents", e.getMessage());
    }
  }
  public void testTripleParents() throws Exception {
    final String input = ""
        + "parent-child mother childa" + LS
        + "parent-child mother childa" + LS
        + "parent-child father childb" + LS
        + "parent-child mother childb" + LS
        + "parent-child stepmother childb" + LS
        ;
    try {
      Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(input))));
      fail();
    } catch (final IllegalArgumentException e) {
      // Expected
      assertEquals("There are more than two parents specified", e.getMessage());
    }
  }

  public void testNormal() throws Exception {
    final String rel = "genome male sex=male" + LS
        + "genome female sex=female" + LS
        + "genome son sex=male" + LS
        + "parent-child male son" + LS
        + "parent-child female son" + LS
        + "genome daughter sex=female" + LS
        + "parent-child male daughter" + LS
        + "parent-child female daughter" + LS;
    final Family fam = Family.getFamily(RelationshipsFileParser.load(new BufferedReader(new StringReader(rel))));
    assertEquals("male", fam.getFather());
    assertEquals("female", fam.getMother());
  }

  public void testMultiFamily() {
    final GenomeRelationships pedigree = new GenomeRelationships();
    pedigree.addGenome("father", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    pedigree.addGenome("mother", GenomeRelationships.SEX_FEMALE);
    pedigree.addGenome("child", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    pedigree.addParentChild("father", "child");
    pedigree.addParentChild("mother", "child");
    pedigree.addGenome("mother2", GenomeRelationships.SEX_FEMALE);
    pedigree.addGenome("child2", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    pedigree.addGenome("child3", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    pedigree.addParentChild("father", "child2");
    pedigree.addParentChild("mother2", "child2");
    pedigree.addParentChild("father", "child3");
    pedigree.addParentChild("mother2", "child3");
    pedigree.addGenome("unrelated1", GenomeRelationships.SEX_FEMALE);
    pedigree.addGenome("fatherfather", GenomeRelationships.SEX_MALE);
    pedigree.addGenome("fathermother", GenomeRelationships.SEX_FEMALE);
    pedigree.addParentChild("fatherfather", "father");
    pedigree.addParentChild("fathermother", "father");

    final Set<Family> families = Family.getFamilies(pedigree, false, null);
    assertEquals(3, families.size());
    final Family[] afamilies = families.toArray(new Family[families.size()]);
    assertEquals("father", afamilies[0].getFather());
    assertEquals("mother", afamilies[0].getMother());
    assertEquals("child", afamilies[0].getChildren()[0]);

    assertEquals("father", afamilies[1].getFather());
    assertEquals("mother2", afamilies[1].getMother());
    assertEquals("child2", afamilies[1].getChildren()[0]);
    assertEquals("child3", afamilies[1].getChildren()[1]);

    assertEquals("fatherfather", afamilies[2].getFather());
    assertEquals("fathermother", afamilies[2].getMother());
    assertEquals("father", afamilies[2].getChildren()[0]);
  }
}
