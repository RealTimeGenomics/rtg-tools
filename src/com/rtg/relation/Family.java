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

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.rtg.reference.Sex;
import com.rtg.relation.Relationship.RelationshipType;
import com.rtg.relation.Relationship.RelationshipTypeFilter;
import com.rtg.relation.Relationship.SecondInRelationshipFilter;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Represents a nuclear family (father, mother, and at least one child).
 */
public class Family {

  /** Position of the first child in model array. */
  public static final int FIRST_CHILD_INDEX = 2;
  /** Position of the mother in model array. */
  public static final int MOTHER_INDEX = 1;
  /** Position of the father in model array. */
  public static final int FATHER_INDEX = 0;

  final String mFather;
  final String mMother;
  final Set<String> mChildren = new TreeSet<>();
  final GenomeRelationships mPedigree;
  private final String[] mMembers;
  private final boolean[] mIsDiseased;
  private final int[] mSampleIds;
  private int mFatherFamilyId;
  private int mFatherDistinctMates = 1;
  private int mMotherFamilyId;
  private int mMotherDistinctMates = 1;



  /**
   * Constructs a family from the provided pedigree.
   * The file must contain only parent-child relationships
   * Each child must share the same two parents.
   * @param pedigree the GenomeRelationships which describes the family
   * @return the identified family
   * @throws PedigreeException if the genome file doesn't describe a simple family
   */
  public static Family getFamily(GenomeRelationships pedigree) throws PedigreeException {
    String firstParent = null;
    String secondParent = null;
    final String father;
    final String mother;
    final Set<String> children = new TreeSet<>();
    for (final String name : pedigree.genomes()) {
      boolean found = false;
      for (final Relationship r : pedigree.relationships(name, new RelationshipTypeFilter(RelationshipType.PARENT_CHILD))) {
        if (r.first().equals(name)) {
          if (!found) {
            if (firstParent != null) {
              throw new PedigreeException("There are more than two parents specified");
            }
            firstParent = secondParent;
            secondParent = name;
          }
          found = true;
        } else {
          children.add(name);
        }
      }
    }
    if (firstParent == null) {
      throw new PedigreeException("There are fewer than two parents specified");
    }
    if ((pedigree.getSex(firstParent) == Sex.FEMALE) || (pedigree.getSex(secondParent) == Sex.MALE)) {
      mother = firstParent;
      father = secondParent;
    } else if ((pedigree.getSex(firstParent) == Sex.MALE) || (pedigree.getSex(secondParent) == Sex.FEMALE)) {
      father = firstParent;
      mother = secondParent;
    } else {
      Diagnostic.warning("Cannot determine sex of either parent ('" + firstParent + "' and '" + secondParent + "'), assuming father is '" + firstParent + "'");
      father = firstParent;
      mother = secondParent;
    }
    return new Family(pedigree, father, mother, children.toArray(new String[0]));
  }

  /**
   * Extracts all complete nuclear families from the provided relationships.
   * @param pedigree the GenomeRelationships which describes the family
   * @param sloppy if true, allow families where the sex of both parents are unknown
   * @param samplesToKeep allow only these samples in returned families. null to keep everything.
   * @return a set containing all identified families
   * @throws PedigreeException if the genome file doesn't describe a simple family
   */
  public static Set<Family> getFamilies(GenomeRelationships pedigree, boolean sloppy, Set<String> samplesToKeep) throws PedigreeException {
    final Relationship.SampleRelationshipFilter sampleFilter = samplesToKeep != null ? new Relationship.SampleRelationshipFilter(samplesToKeep) : null;
    final Map<Pair<String, String>, Set<String>> partials = new LinkedHashMap<>();
    for (final String child : pedigree.genomes()) {
      final Relationship.RelationshipFilter[] filters = sampleFilter != null ? new Relationship.RelationshipFilter[] {new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new SecondInRelationshipFilter(child), sampleFilter} : new Relationship.RelationshipFilter[] {new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new SecondInRelationshipFilter(child)};
      final Relationship[] parents = pedigree.relationships(child, filters);
      //System.err.println("Child " + child + " has " + parents.length + " parents");
      if (parents.length == 2) {
        // A child with two parents is a candidate for creating or entering a family
        final String p1 = parents[0].first();
        final String p2 = parents[1].first();
        final Pair<String, String> parentkey = (p1.compareTo(p2) < 0) ? new Pair<>(p1, p2) : new Pair<>(p2, p1);
        final Set<String> chidlins = partials.computeIfAbsent(parentkey, k -> new HashSet<>());
        chidlins.add(child);
      }
    }

    // Convert each of the partial families into real families
    final Comparator<Family> parentComparator = new Comparator<Family>() {
      @Override
      public int compare(Family o1, Family o2) {
        final int q = o1.getFather().compareTo(o2.getFather());
        if (q != 0) {
          return q;
        }
        return o1.getMother().compareTo(o2.getMother());
      }
    };
//    final Map<String, Integer> parentFams = new HashMap<>();
    final Set<Family> families = new TreeSet<>(parentComparator);
    for (Map.Entry<Pair<String, String>, Set<String>> entry : partials.entrySet()) {
      final Pair<String, String> parentkey = entry.getKey();
      final String mother;
      final String father;
      if ((pedigree.getSex(parentkey.getA()) == Sex.FEMALE) || (pedigree.getSex(parentkey.getB()) == Sex.MALE)) {
        mother = parentkey.getA();
        father = parentkey.getB();
      } else if ((pedigree.getSex(parentkey.getA()) == Sex.MALE) || (pedigree.getSex(parentkey.getB()) == Sex.FEMALE)) {
        father = parentkey.getA();
        mother = parentkey.getB();
      } else {
        if (sloppy) { // Arbitrarily take A as father and B as mother
          father = parentkey.getA();
          mother = parentkey.getB();
        } else {
          continue; // Can't work out sex of parents, skip this as a family
        }
      }

      // Require every child to have sex specified, and both parents must have correct sex
      boolean accepted = true;
      if (!sloppy) {
        for (String child : entry.getValue()) {
          if (pedigree.getSex(child) == Sex.EITHER) {
            // Child with unknown sex, skip as not well formed family
            accepted = false;
            break;
          }
        }
        if (pedigree.getSex(father) != Sex.MALE) { // Check sex of father
          accepted = false;
        }
        if (pedigree.getSex(mother) != Sex.FEMALE) { // Check sex of mother
          accepted = false;
        }
      }
      if (accepted) {
        final Family family = new Family(pedigree, father, mother, entry.getValue().toArray(new String[0]));
//        int fatherId = 0;
//        if (parentFams.containsKey(father)) {
//          fatherId = parentFams.get(father) + 1;
//        }
//        int motherId = 0;
//        if (parentFams.containsKey(mother)) {
//          motherId = parentFams.get(mother) + 1;
//        }
//        family.setFatherFamilyId(fatherId);
//        family.setMotherFamilyId(motherId);
//        parentFams.put(father, fatherId);
//        parentFams.put(mother, motherId);
        families.add(family);
      }
    }

    return families;
  }

  /**
   * Constructs a family from the provided pedigree.
   * Each child must share the same two parents.
   * @param pedigree the GenomeRelationships which contains the family relationships
   * @param father the name of the father
   * @param mother the name of the mother
   * @param children the names of the children
   * @throws PedigreeException if the pedigree and sample names do not form a simple family
   */
  public Family(GenomeRelationships pedigree, String father, String mother, String... children) throws PedigreeException {
    mPedigree = pedigree;
    mFather = father;
    mMother = mother;
    if (father.equals(mother)) {
      throw new PedigreeException("Mother and father cannot be the same sample: '" + father + "'");
    }


    // Default IDs are assigned corresponding to FATHER_INDEX, MOTHER_INDEX, FIRST_CHILD_INDEX...
    // These may be reassigned if need be
    mSampleIds = new int[children.length + 2];
    for (int i = 0; i < mSampleIds.length; ++i) {
      mSampleIds[i] = i;
    }

    // Various sanity checks on the relationships
    for (final String c : children) {
      final Relationship[] rel = pedigree.relationships(c, new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new SecondInRelationshipFilter(c));
      if (rel.length != 2) {
        throw new PedigreeException("Child sample: '" + c + "' has " + rel.length + " parents");
      }
      if (rel[0].first().equals(rel[1].first())) { // This can probably not occur due to querying collapsing duplicates
        throw new PedigreeException("Child sample: '" + c + "' had the same parent '" + rel[0].first() + "' specified twice");
      }
      if (c.equals(mother) || c.equals(father)) {
        throw new PedigreeException("The sample: '" + c + "' cannot be both a parent and a child in the family");
      }
      for (String parent : new String[] {rel[0].first(), rel[1].first()}) {
        if (!(parent.equals(mother) || parent.equals(father))) {
          throw new PedigreeException("The sample: '" + c + "' had non-family parent '" + parent + "'");
        }
      }
      mChildren.add(c);
    }

    mMembers = new String[mChildren.size() + FIRST_CHILD_INDEX];
    mIsDiseased = new boolean[mChildren.size() + FIRST_CHILD_INDEX];
    mMembers[FATHER_INDEX] = mFather;
    mIsDiseased[FATHER_INDEX] = isDiseased(mFather);
    mMembers[MOTHER_INDEX] = mMother;
    mIsDiseased[MOTHER_INDEX] = isDiseased(mMother);
    int i = FIRST_CHILD_INDEX;
    for (final String child : mChildren) {
      mMembers[i] = child;
      mIsDiseased[i] = isDiseased(child);
      ++i;
    }
  }

  /**
   * Construct family without pedigree
   * @param father father sample
   * @param mother mother sample
   * @param children children samples
   */
  public Family(String father, String mother, String... children) {
    mPedigree = new GenomeRelationships();
    mIsDiseased = new boolean[children.length + FIRST_CHILD_INDEX];
    mMembers = new String[children.length + FIRST_CHILD_INDEX];
    for (int i = 0; i < children.length; ++i) {
      mPedigree.addParentChild(father, children[i]);
      mPedigree.addParentChild(mother, children[i]);
      mChildren.add(children[i]);
      mMembers[FIRST_CHILD_INDEX + i] = children[i];
    }
    mSampleIds = new int[children.length + 2];
    for (int i = 0; i < mSampleIds.length; ++i) {
      mSampleIds[i] = i;
    }
    mFather = father;
    mMother = mother;
    mMembers[FATHER_INDEX] = father;
    mMembers[MOTHER_INDEX] = mother;
  }

  /**
   * Get father.
   * @return Returns the parent that is our best guess at the father
   */
  public String getFather() {
    return mFather;
  }

  /**
   * Get mother.
   * @return Returns the parent that is our best guess at the mother
   */
  public String getMother() {
    return mMother;
  }

  /**
   * Get an array containing the genome IDs. The first element is the
   * father ID, second is mother ID, followed by the children IDs.
   * @return Returns an array of the genome IDs.
   */
  public int[] getSampleIds() {
    return mSampleIds;
  }

  /**
   * Set the genome id associated with a family member.
   * @param index the family member index within this family (FATHER_INDEX, MOTHER_INDEX, FIRST_CHILD_INDEX...)
   * @param id the genome id. This corresponds to the output order in a VCF
   */
  public void setSampleId(int index, int id) {
    mSampleIds[index] = id;
  }

  /**
   * Set the genome ids associated with family members.
   * @param sampleIds a list containing the genome names. The index of the name within the list is the genome id.
   */
  public void setSampleIds(List<String> sampleIds) {
    for (int i = 0; i < mMembers.length; ++i) {
      mSampleIds[i] = sampleIds.indexOf(mMembers[i]);
    }
  }

  /**
   * Used to uniquely identify a family a father is a parent in
   * @return the id
   */
  public int getFatherFamilyId() {
    return mFatherFamilyId;
  }

  public void setFatherFamilyId(int id) {
    mFatherFamilyId = id;
  }

  /**
   * Used to uniquely identify a family a mother is a parent in
   * @return the id
   */
  public int getMotherFamilyId() {
    return mMotherFamilyId;
  }

  public void setMotherFamilyId(int id) {
    mMotherFamilyId = id;
  }

  /**
   * @return the number of families the father is a parent in.
   */
  public int getFatherDistinctMates() {
    return mFatherDistinctMates;
  }

  /**
   * @param fatherDistinctMates the number of families the father is a parent in.
   */
  public void setFatherDistinctMates(int fatherDistinctMates) {
    mFatherDistinctMates = fatherDistinctMates;
  }

  /**
   * @return the number of families the mother is a parent in.
   */
  public int getMotherDistinctMates() {
    return mMotherDistinctMates;
  }

  /**
   * @param motherDistinctMates the number of families the mother is a parent in.
   */
  public void setMotherDistinctMates(int motherDistinctMates) {
    mMotherDistinctMates = motherDistinctMates;
  }


  /**
   * Get children.
   * @return Returns a list of the children.
   */
  public String[] getChildren() {
    return mChildren.toArray(new String[0]);
  }

  /**
   * Get the number of children.
   * @return Returns the number of children.
   */
  public int numChildren() {
    return mChildren.size();
  }

  /**
   * Get all family members. Father is always first, Mother is second, followed by children.
   * @return Returns a list of all family members.
   */
  public String[] getMembers() {
    return mMembers;
  }

  /**
   * Get the size of the family, including the parents.
   * @return Returns the number of family members.
   */
  public int size() {
    return mSampleIds.length;
  }

  /**
   * Convenience method to check if a specified genome is marked as exhibiting disease.
   * @param genome genome to check
   * @return true if genome is marked as exhibiting disease
   */
  public final boolean isDiseased(final String genome) {
    return mPedigree.isDiseased(genome);
  }

  /**
   * Convenience method to check if a specified genome is marked as exhibiting disease.
   * @param genome family ID of the genome to check
   * @return true if genome is marked as exhibiting disease
   */
  public boolean isDiseased(final int genome) {
    return mIsDiseased[genome];
  }

  /**
   * Get the pedigree from which this family was obtained
   * @return the pedigree
   */
  public GenomeRelationships pedigree() {
    return mPedigree;
  }

  /**
   * Check if this family is consistent with exactly one parent having disease.
   * @return true if consistent
   */
  public boolean isOneParentDiseased() {
    return isDiseased(MOTHER_INDEX) ^ isDiseased(FATHER_INDEX);
  }

  @Override
  public String toString() {
    //return "Family: [" + mFather + " + " + mMother + "]" + " -> " + mChildren;
    final StringBuilder sb = new StringBuilder();
    sb.append("Father: ").append(mFather).append(" sex: ").append(mPedigree.getSex(mFather)).append(" id:").append(mSampleIds[FATHER_INDEX]).append(StringUtils.LS);
    sb.append("Mother: ").append(mMother).append(" sex: ").append(mPedigree.getSex(mMother)).append(" id:").append(mSampleIds[MOTHER_INDEX]).append(StringUtils.LS);
    int i = 0;
    for (String childName : getChildren()) {
        sb.append("Child ").append(i + 1).append(": ").append(childName).append(" sex: ").append(mPedigree.getSex(childName)).append(" id:").append(mSampleIds[FIRST_CHILD_INDEX + i]).append(StringUtils.LS);
        ++i;
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Family)) {
      return false;
    }
    final Family f = (Family) o;
    return (mPedigree == f.mPedigree) && mFather.equals(f.mFather) && mMother.equals(f.mMother);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mFather.hashCode(), mMother.hashCode());
  }
}
