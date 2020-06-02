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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.reference.Sex;
import com.rtg.relation.Relationship.FirstInRelationshipFilter;
import com.rtg.relation.Relationship.RelationshipType;
import com.rtg.relation.Relationship.RelationshipTypeFilter;
import com.rtg.relation.Relationship.SecondInRelationshipFilter;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class GenomeRelationshipsTest extends TestCase {

  /** Relations for testing. */
  public static final String RELATIONS = ""
    + "#parents" + StringUtils.LS
    + "#twins" + StringUtils.LS
    + "#cancer" + StringUtils.LS
    + "" + StringUtils.LS
    + "genome father disease=" + true + " sex=male" + StringUtils.LS
    + "genome twina\t\tdisease=" + false + " sex=female" + StringUtils.LS
    + "genome twinb\t\tdisease=" + true + StringUtils.LS
    + "parent-child\tfather\ttwina" + StringUtils.LS
    + "parent-child\t father\ttwinb" + StringUtils.LS
    + "parent-child  mother twina" + StringUtils.LS
    + "parent-child mother twinb" + StringUtils.LS
    + "original-derived father fathercancer contamination=0.03" + StringUtils.LS;

  private static final Relationship[] EXPECTED_FATHER = {
    new Relationship("father", "twina", RelationshipType.PARENT_CHILD),
    new Relationship("father", "twinb", RelationshipType.PARENT_CHILD),
    new Relationship("father", "fathercancer", RelationshipType.ORIGINAL_DERIVED),
  };
  private static final Relationship[] EXPECTED_MOTHER = {
    new Relationship("mother", "twina", RelationshipType.PARENT_CHILD),
    new Relationship("mother", "twinb", RelationshipType.PARENT_CHILD),
  };
  private static final Relationship[] EXPECTED_TWINA = {
    new Relationship("father", "twina", RelationshipType.PARENT_CHILD),
    new Relationship("mother", "twina", RelationshipType.PARENT_CHILD),
  };
  private static final Relationship[] EXPECTED_TWINB = {
    new Relationship("father", "twinb", RelationshipType.PARENT_CHILD),
    new Relationship("mother", "twinb", RelationshipType.PARENT_CHILD),
  };
  private static final Relationship[] EXPECTED_DERIVED = {
    new Relationship("father", "fathercancer", RelationshipType.ORIGINAL_DERIVED)
  };
  static {
    EXPECTED_FATHER[2].setProperty("contamination", "0.03");
    EXPECTED_DERIVED[0].setProperty("contamination", "0.03");
  }

  public void testRelationshipType() {
    TestUtils.testEnum(RelationshipType.class, "[PARENT_CHILD, ORIGINAL_DERIVED]");
  }

  public void testFile() throws IOException {
    final File dir = FileUtils.createTempDir("test", "relationshipfile");
    try {
      final File relationFile = new File(dir, "relationshipfile.txt");
      FileUtils.stringToFile(RELATIONS, relationFile);
      final GenomeRelationships gnf = RelationshipsFileParser.loadFile(relationFile);
      final String[] res = gnf.genomes();
      Arrays.sort(res);
      assertTrue(Arrays.toString(gnf.genomes()), Arrays.equals(new String[] {"father", "fathercancer", "mother", "twina", "twinb"}, res));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("father")), Arrays.equals(EXPECTED_FATHER, gnf.relationships("father")));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("mother")), Arrays.equals(EXPECTED_MOTHER, gnf.relationships("mother")));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("twina")), Arrays.equals(EXPECTED_TWINA, gnf.relationships("twina")));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("twinb")), Arrays.equals(EXPECTED_TWINB, gnf.relationships("twinb")));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("fathercancer")), Arrays.equals(EXPECTED_DERIVED, gnf.relationships("fathercancer")));
      for (final Relationship r : gnf.relationships("father")) {
        assertEquals("father", r.first());
      }
      for (final Relationship r : gnf.relationships("fathercancer")) {
        assertEquals("fathercancer", r.second());
      }
      assertTrue(gnf.isDiseased("father"));
      assertFalse(gnf.isDiseased("mother"));
      assertFalse(gnf.isDiseased("twina"));
      assertEquals(Sex.MALE, gnf.getSex("father"));
      assertEquals(Sex.FEMALE, gnf.getSex("twina"));
      assertEquals(Sex.EITHER, gnf.getSex("mother"));
      assertEquals(Sex.EITHER, gnf.getSex("no-such-genome"));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private GenomeRelationships makeFamily() {
    final GenomeRelationships genomeRelationships = new GenomeRelationships();
    genomeRelationships.addGenome("fatherfather", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    genomeRelationships.addGenome("fathermother", GenomeRelationships.SEX_FEMALE);
    genomeRelationships.addGenome("father", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    genomeRelationships.addGenome("mother", GenomeRelationships.SEX_FEMALE);
    genomeRelationships.addGenome("child", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    genomeRelationships.addParentChild("father", "child");
    genomeRelationships.addParentChild("mother", "child");
    genomeRelationships.addParentChild("fatherfather", "father");
    genomeRelationships.addParentChild("fathermother", "father");
    return genomeRelationships;
  }

  public void testQueries() {
    final GenomeRelationships genomeRelationships = makeFamily();
    assertEquals(3, genomeRelationships.relationships("father").length);
    assertEquals(3, genomeRelationships.relationships("father", new RelationshipTypeFilter(RelationshipType.PARENT_CHILD)).length);
    assertEquals(1, genomeRelationships.relationships("father", new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new FirstInRelationshipFilter("father")).length);
    assertEquals(2, genomeRelationships.relationships("father", new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new SecondInRelationshipFilter("father")).length);
    assertEquals(4, genomeRelationships.relationships(RelationshipType.PARENT_CHILD).length);
    assertEquals(2, genomeRelationships.relationships("child").length);

    assertEquals(4, genomeRelationships.genomes(new GenomeRelationships.HasRelationshipGenomeFilter(genomeRelationships, RelationshipType.PARENT_CHILD, true)).length);  // ff, fm, f, m
    assertEquals(2, genomeRelationships.genomes(new GenomeRelationships.HasRelationshipGenomeFilter(genomeRelationships, RelationshipType.PARENT_CHILD, false)).length); // f, c
    assertEquals(3, genomeRelationships.genomes(new GenomeRelationships.FounderGenomeFilter(genomeRelationships, false)).length);  // ff, fm, m

    genomeRelationships.addGenome("child-tumor", Sex.MALE);
    genomeRelationships.addRelationship(RelationshipType.ORIGINAL_DERIVED, "child", "child-tumor");
    assertEquals(3, genomeRelationships.genomes(new GenomeRelationships.FounderGenomeFilter(genomeRelationships, false)).length);  // ff, fm, m
  }

  public void testFilter() {
    final GenomeRelationships genomeRelationships = makeFamily();
    assertEquals(5, genomeRelationships.genomes().length);

    final GenomeRelationships individuals = genomeRelationships.filterByRelationships(new Relationship.NotFilter(new RelationshipTypeFilter(RelationshipType.PARENT_CHILD)));
    assertEquals(5, individuals.genomes().length);
    assertEquals(0, individuals.relationships(RelationshipType.PARENT_CHILD).length);
  }

  public void testNumberOfConnectedGroups() {
    final GenomeRelationships gr = new GenomeRelationships();
    gr.addGenome("father");
    gr.addGenome("mother");
    gr.addGenome("son");
    gr.addGenome("cousin");
    gr.addParentChild("father", "son");
    gr.addParentChild("mother", "son");

    final ArrayList<String> genomes = new ArrayList<>();
    genomes.add("father");
    genomes.add("mother");
    genomes.add("son");

    assertEquals(1, gr.numberOfDisconnectedGroups(genomes));

    genomes.add("cousin");
    assertEquals(2, gr.numberOfDisconnectedGroups(genomes));

    gr.addGenome("cousin2");
    genomes.add("cousin2");

    assertEquals(3, gr.numberOfDisconnectedGroups(genomes));

    genomes.remove("cousin2");

    gr.addGenome("father2");
    gr.addGenome("mother2");
    gr.addGenome("son2");
    gr.addParentChild("father2", "son2");
    gr.addParentChild("mother2", "son2");
    genomes.add("father2");
    genomes.add("mother2");
    genomes.add("son2");

    assertEquals(3, gr.numberOfDisconnectedGroups(genomes));

    gr.addGenome("grandmother");
    gr.addParentChild("grandmother", "father2");
    gr.addParentChild("grandmother", "mother");
    genomes.add("grandmother");

    assertEquals(2, gr.numberOfDisconnectedGroups(genomes));
  }

}
