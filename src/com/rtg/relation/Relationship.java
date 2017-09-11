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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Utils;

/**
 * Defines a single relationship between two genomes
 */
@TestClass("com.rtg.relation.GenomeRelationshipsTest")
public class Relationship {

  /** Key for contamination. */
  public static final String CONTAMINATION = "contamination";
  /** Key for reverse contamination. */
  public static final String REVERSE_CONTAMINATION = "reverse-contamination";

  /**
   * Enumeration for relationship types. Add extras as needed.
   */
  public enum RelationshipType {
    /** The first genome is a parent of the second genome. */
    PARENT_CHILD,
    /** The second genome is derived from the first genome (e.g. normal / tumor, or cell line). */
    ORIGINAL_DERIVED,
  }

  private final String mGenome1;
  private final String mGenome2;
  private final RelationshipType mType;
  private final Map<String, String> mProperties;

  /**
   * @param genome1 first genome in relationship
   * @param genome2 second genome in relationship
   * @param type type of relationship
   */
  public Relationship(String genome1, String genome2, RelationshipType type) {
    mGenome1 = genome1;
    mGenome2 = genome2;
    mType = type;
    mProperties = new TreeMap<>();
  }

  /**
   * Get genome 1.
   * @return Returns the first genome in the relationship.
   */
  public String first() {
    return mGenome1;
  }

  /**
   * Get genome 2.
   * @return Returns the second genome in the relationship.
   */
  public String second() {
    return mGenome2;
  }

  /**
   * @return Returns type of relationship
   */
  public RelationshipType type() {
    return mType;
  }

  /**
   * Sets an arbitrary property for this relationship
   * @param propname name of property
   * @param val value for property
   */
  public void setProperty(String propname, String val) {
    mProperties.put(propname, val);
  }

  /**
   * query for a property
   * @param propname name of property
   * @return value of property
   */
  public String getProperty(String propname) {
    return mProperties.get(propname);
  }

  private Double getPropertyAsDouble(final String propName) {
    final String value = getProperty(propName);
    return value == null ? null : Double.valueOf(value);
  }

  /**
   * Get the contamination level for this relationship or null if no contamination
   * is specified.
   *
   * @return contamination level
   */
  public Double getContamination() {
    return getPropertyAsDouble(CONTAMINATION);
  }

  /**
   * Get the reverse contamination level for this relationship or null if no reverse contamination
   * is specified.
   *
   * @return contamination level
   */
  public Double getReverseContamination() {
    return getPropertyAsDouble(REVERSE_CONTAMINATION);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Relationship)) {
      return false;
    }
    final Relationship p = (Relationship) o;
    return mType.ordinal() == p.mType.ordinal() && mProperties.equals(p.mProperties) && mGenome1.equals(p.mGenome1) && mGenome2.equals(p.mGenome2);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mProperties.hashCode(), Utils.pairHash(mType.ordinal(), Utils.pairHash(mGenome1.hashCode(), mGenome2.hashCode())));
  }

  @Override
  public String toString() {
    return mType + " (" + mGenome1 + "-" + mGenome2 + ")" + (!mProperties.isEmpty() ? (" :: " + mProperties) : "");
  }


  /**
   * Things which may accept or reject a Relationship
   */
  public interface RelationshipFilter {
    /**
     * Returns true if the relationship is accepted
     * @param relationship the relationship to test
     * @return true if the relationship is accepted
     */
    boolean accept(Relationship relationship);
  }

  /** Accepts relationships that are rejected by a sub-filter */
  public static class NotFilter implements RelationshipFilter {
    private final RelationshipFilter mFilter;

    /**
     * Accepts relationships of a certain type
     * @param filter the type to match
     */
    public NotFilter(RelationshipFilter filter) {
      mFilter = filter;
    }

    @Override
    public boolean accept(Relationship relationship) {
      return !mFilter.accept(relationship);
    }
  }

  /** Accepts relationships of a certain type */
  public static class RelationshipTypeFilter implements RelationshipFilter {
    private final RelationshipType mType;

    /**
     * Accepts relationships of a certain type
     * @param type the type to match
     */
    public RelationshipTypeFilter(RelationshipType type) {
      mType = type;
    }

    @Override
    public boolean accept(Relationship relationship) {
      return mType == relationship.type();
    }
  }

  /** Accepts relationships where a specified genome is in the first position */
  public static class FirstInRelationshipFilter implements RelationshipFilter {
    private final String mGenome;

    /**
     * Accepts relationships with a genome in the first position
     * @param genome the genome that must be in the first position
     */
    public FirstInRelationshipFilter(String genome) {
      mGenome = genome;
    }

    @Override
    public boolean accept(Relationship relationship) {
      return mGenome.equals(relationship.first());
    }
  }

  /** Accepts relationships where a specified genome is in the second position */
  public static class SecondInRelationshipFilter implements RelationshipFilter {
    private final String mGenome;

    /**
     * Accepts relationships with a genome in the second position
     * @param genome the genome that must be in the second position
     */
    public SecondInRelationshipFilter(String genome) {
      mGenome = genome;
    }

    @Override
    public boolean accept(Relationship relationship) {
      return mGenome.equals(relationship.second());
    }
  }

  /**
   * Filters relationships based on given samples. Only relationships where both genomes match are accepted.
   */
  public static class SampleRelationshipFilter implements RelationshipFilter {

    private final Collection<String> mSamples;

    /**
     * @param samples samples to keep relationships for
     */
    public SampleRelationshipFilter(String... samples) {
      mSamples = Arrays.asList(samples);
    }

    /**
     * @param samples samples to keep relationships for
     */
    public SampleRelationshipFilter(Set<String> samples) {
      mSamples = samples;
    }

    @Override
    public boolean accept(Relationship relationship) {
      return mSamples.contains(relationship.first()) && mSamples.contains(relationship.second());
    }
  }
}
