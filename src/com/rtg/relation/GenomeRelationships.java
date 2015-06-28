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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import com.rtg.launcher.CommonFlags;
import com.rtg.reference.Sex;
import com.rtg.relation.Relationship.RelationshipFilter;
import com.rtg.relation.Relationship.RelationshipType;
import com.rtg.relation.Relationship.RelationshipTypeFilter;
import com.rtg.relation.Relationship.SecondInRelationshipFilter;
import com.rtg.util.MultiMap;
import com.rtg.util.StringUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfUtils;

/**
 * Class for managing the storage of genome relationship information.
 */
public class GenomeRelationships {


  /** Used as a return value for empty relationship queries */
  private static final Relationship[] EMPTY_REL = new Relationship[0];

  /** Property name used to store genome sex. */
  public static final String SEX_PROPERTY = "sex";

  //TODO these should be an enumeration
  /** Genome sex property value for males. */
  public static final String SEX_MALE = "male";

  /** Genome sex property value for females. */
  public static final String SEX_FEMALE = "female";

  /** Property name used to store genome disease status. */
  public static final String DISEASE_PROPERTY = "disease";

  /**
   * Property name used to indicate that a genome is primary.
   * For PED files this means it a line of its own in the file.
   * For VCF files this means it is one of the samples that appears in the SAMPLE list.
   * If this property is not set, it indicates that the presence
   * of the genome has been inferred via relationships.
   */
  public static final String PRIMARY_GENOME_PROPERTY = "primary-genome";


  private final Collection<String> mGenomes;
  private final Map<String, Properties> mGenomeProperties;
  private final MultiMap<String, Relationship> mRelationships;

  /**
   * Make an empty set of relationships.
   */
  public GenomeRelationships() {
    mGenomes = new TreeSet<>();
    mGenomeProperties = new TreeMap<>();
    mRelationships = new MultiMap<>(true);
  }

  /**
   * Loads genome relationships from one of the supported formats.
   * @param pedFile the file to read pedigree information from
   * @return the relationships
   * @throws java.io.IOException if there is a problem reading the file
   */
  public static GenomeRelationships loadGenomeRelationships(File pedFile) throws IOException {
    final GenomeRelationships pedigree;

    if (VcfUtils.isVcfExtension(pedFile)) {
      pedigree = VcfPedigreeParser.loadFile(pedFile);
    } else if (pedFile.getName().endsWith(".relations")) { // internal legacy format
      pedigree = RelationshipsFileParser.loadFile(pedFile);
    } else if (pedFile.getName().endsWith(".ped")) {
      pedigree = PedFileParser.loadFile(pedFile);
    } else { // Guess based on content
      pedigree = loadGenomeRelationships(new InputStreamReader(CommonFlags.isStdio(pedFile) ? System.in : FileUtils.createInputStream(pedFile, false)));
    }
    return pedigree;
  }

  /**
   * Loads genome relationships from one of the supported formats.
   * @param input the reader supplying pedigree information
   * @return the relationships
   * @throws java.io.IOException if there is a problem reading the file
   */
  public static GenomeRelationships loadGenomeRelationships(Reader input) throws IOException {
    final String pedTxt = FileUtils.readerToString(input);
    if (pedTxt.startsWith("##fileformat=VCF")) {
      try (VcfReader r = new VcfReader(new BufferedReader(new StringReader(pedTxt)))) {
        return VcfPedigreeParser.load(r.getHeader());
      }
    } else {
      try (BufferedReader r = new BufferedReader(new StringReader(pedTxt))) {
        return PedFileParser.load(r);
      }
    }
  }

  /**
   * @return array of genomes declared in file
   */
  public String[] genomes() {
    return mGenomes.toArray(new String[mGenomes.size()]);
  }

  /**
   * Return true if the genome is contained in this pedigree
   * @param genome the genome name
   * @return true if the genome is present in this pedigree
   */
  public boolean hasGenome(String genome) {
    return mGenomes.contains(genome);
  }

  /**
   * @param genome genome to get properties for
   * @return properties for the genome
   */
  public Properties getProperties(final String genome) {
    return mGenomeProperties.get(genome);
  }

  /**
   * Convenience method to check if a specified genome is marked as exhibiting disease.
   *
   * @param genome genome to check
   * @return true if genome is marked as exhibiting disease
   */
  public boolean isDiseased(final String genome) {
    final Properties p = getProperties(genome);
    if (p == null) {
      return false;
    }
    return Boolean.valueOf(p.getProperty(DISEASE_PROPERTY, "false"));
  }

  /**
   * Get the sex of the specified genome. If the genome could not be found or no sex is
   * specified for the genome then <code>EITHER</code> is returned.
   *
   * @param genome genome to get sex for
   * @return sex of genome
   */
  public Sex getSex(final String genome) {
    final Properties p = getProperties(genome);
    if (p == null) {
      return Sex.EITHER;
    }
    return Sex.valueOf(p.getProperty(SEX_PROPERTY, "either").toUpperCase(Locale.getDefault()));
  }

  /**
   * Get all relationships involving a genome.
   * @param genome genome to find relationships for.
   * @return array of all relationships involving given genome.
   */
  public Relationship[] relationships(String genome) {
    final Collection<Relationship> col = mRelationships.get(genome);
    return col == null ? EMPTY_REL : col.toArray(new Relationship[col.size()]);
  }

  /**
   * Get all relationships of a given type.
   * @param type type of relation
   * @return array of all relationships of the given type.
   */
  public Relationship[] relationships(final RelationshipType type) {
    return relationships(new RelationshipTypeFilter(type));
  }

  /**
   * Get all relationships of a given type involving the specified genome at
   * either end of the relationship.
   * @param genome genome to find relationships for.
   * @param filters a list of filters that the resulting relationships must be accepted by
   * @return array of matching relationships involving given genome.
   */
  public Relationship[] relationships(String genome, RelationshipFilter... filters) {
    final Collection<Relationship> col = mRelationships.get(genome);
    if (col == null) {
      return EMPTY_REL;
    }
    final HashSet<Relationship> derivatives = new LinkedHashSet<>();
    for (final Relationship r : col) {
      if (allAccepted(r, filters)) {
        derivatives.add(r);
      }
    }
    return derivatives.toArray(new Relationship[derivatives.size()]);
  }

  /**
   * Get all relationships matching the supplied filters.
   * @param filters a list of filters that the resulting relationships must be accepted by.
   * @return array of matching relationships.
   */
  public Relationship[] relationships(RelationshipFilter... filters) {
    final HashSet<Relationship> collected = new LinkedHashSet<>();
    // We will see the same relationship multiple times, but it gets collapsed in the Set
    for (final Collection<Relationship> rs : mRelationships.values()) {
      for (final Relationship r : rs) {
        if (allAccepted(r, filters)) {
          collected.add(r);
        }
      }
    }
    return collected.toArray(new Relationship[collected.size()]);
  }

  /**
   * Get all genomes matching the supplied filters.
   * @param filters a list of filters that the resulting genomes must be accepted by.
   * @return array of matching genome names.
   */
  public String[] genomes(GenomeFilter... filters) {
    final HashSet<String> collected = new LinkedHashSet<>();
    for (final String genome : genomes()) {
      if (allAccepted(genome, filters)) {
        collected.add(genome);
      }
    }
    return collected.toArray(new String[collected.size()]);
  }

  /**
   * Create a filtered version of this GenomeRelationships. All genomes are retained, but only relationships
   * that are accepted by the supplied filters are retained.
   * @param filters the filter criterion
   * @return a filtered version of this GenomeRelationships
   */
  public GenomeRelationships filterByRelationships(RelationshipFilter... filters) {
    final GenomeRelationships result = new GenomeRelationships();
    for (final String genome : genomes()) {
      result.addGenome(genome).putAll(getProperties(genome));
    }
    for (final Relationship r : relationships(filters)) {
      addRelationship(r);
    }
    return result;
  }

  /**
   * Create a filtered version of this GenomeRelationships. Only genomes accepted by the supplied filter will be
   * contained in the output. Any relationships involving genomes not present in the output are also removed.
   * @param filters the filter criterion
   * @return a filtered version of this GenomeRelationships
   */
  public GenomeRelationships filterByGenomes(GenomeFilter... filters) {
    final GenomeRelationships result = new GenomeRelationships();
    for (final String genome : genomes(filters)) {
      result.addGenome(genome).putAll(getProperties(genome));
    }
    for (final Relationship r : relationships(new Relationship.SampleRelationshipFilter(result.genomes()))) {
      result.addRelationship(r);
    }
    return result;
  }

  static boolean allAccepted(String genome, GenomeFilter... filters) {
    for (final GenomeFilter f : filters) {
      if (!f.accept(genome)) {
        return false;
      }
    }
    return true;
  }

  static boolean allAccepted(Relationship r, RelationshipFilter... filters) {
    for (final RelationshipFilter f : filters) {
      if (!f.accept(r)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "Genomes: " + mGenomes + StringUtils.LS + "GenomeProperties: " + mGenomeProperties + StringUtils.LS + "Relationships: " + mRelationships + StringUtils.LS;
  }

  // Make a graphviz-safe ID for a node given a non-safe name
  private static String nodeId(Map<String, String> nodeIds, String name) {
    String id = nodeIds.get(name);
    if (id == null) {
      id = "node" + nodeIds.size();
      nodeIds.put(name, id);
    }
    return id;
  }

  static final Map<String, String> REL_LABELS = new HashMap<>();
  static {
    REL_LABELS.put(RelationshipType.PARENT_CHILD.name(), "Child");
    REL_LABELS.put(RelationshipType.ORIGINAL_DERIVED.name(), "Derived");
  }

  /**
   * Creates a <code>graphviz</code> compatible output to display the pedigree. Only genomes that are included
   * in some kind of relationship are shown. Families where both parents are known are denoted with
   * a node representing the marriage, to better group offspring from the same parents. Diseased
   * status is indicated with afflicted individuals show in grey.
   * @param title the text to use as the graph title
   * @return the <code>graphviz</code> output
   */
  String toGraphViz(final String title) {
    final StringBuilder sb = new StringBuilder();
    sb.append("digraph Ped {\n" + "  ratio =\"auto\";\n" + "  mincross = 2.0;\n" + "  labelloc = \"t\";\n" + "  label=\"").append(title).append("\";\n").append("\n");

    final HashSet<Relationship> seen = new HashSet<>();
    final HashSet<String> seenGenomes = new LinkedHashSet<>();
    final Map<String, String> nodeIds = new HashMap<>();

    // Output family specific stuff (i.e. node representing a marriage, with children coming off that)
    for (final Family family : Family.getFamilies(this, false, null)) {
      seenGenomes.add(family.getFather());
      seenGenomes.add(family.getMother());
      final String fatherId = nodeId(nodeIds, family.getFather());
      final String motherId = nodeId(nodeIds, family.getMother());
      final String marriageId = nodeId(nodeIds, "m" + family.getFather() + "x" + family.getMother());
      sb.append("  {\n");
      //sb.append("    rank = same;\n");
      sb.append("    ").append(fatherId).append(" -> ").append(marriageId).append(" [dir=none];\n");
      sb.append("    ").append(motherId).append(" -> ").append(marriageId).append(" [dir=none];\n");
      sb.append("    ").append(marriageId).append(" [shape=diamond,style=filled,label=\"\",height=.1,width=.1];\n");
      sb.append("  }\n");
      for (final String child : family.getChildren()) {
        sb.append("  ").append(marriageId).append(" -> ").append(nodeId(nodeIds, child)).append(" [];\n");
        for (final Relationship r : relationships(child, new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new SecondInRelationshipFilter(child))) {
          seen.add(r);
          seenGenomes.add(child);
        }
      }
    }

    // Output remaining relationships
    for (final Collection<Relationship> rs : mRelationships.values()) {
      for (final Relationship r : rs) {
        if (!seen.contains(r)) {
          final String relname = REL_LABELS.containsKey(r.type().name()) ? REL_LABELS.get(r.type().name()) : r.type().name();
          sb.append("  ").append(nodeId(nodeIds, r.first())).append(" -> ").append(nodeId(nodeIds, r.second())).append(" [label=\"").append(relname).append("\", fontsize=10];\n");
          seen.add(r);
          seenGenomes.add(r.first());
          seenGenomes.add(r.second());
        }
      }
    }

    // Output genome nodes
    for (final String genome : seenGenomes) {
      final Sex s = getSex(genome);
      String style = "";
      final String shape;
      if (s == Sex.MALE) {
        shape = ", shape=box";
        style = ", style=filled, fillcolor=skyblue";
      } else if (s == Sex.FEMALE) {
        shape = ", shape=oval";
        style = ", style=filled, fillcolor=pink";
      } else {
        shape = ", shape=diamond";
      }
      if (isDiseased(genome)) {
        style = ", style=filled, fillcolor=grey";
      }
      sb.append("  ").append(nodeId(nodeIds, genome)).append(" [label=\"").append(genome).append("\"").append(shape).append(style).append("];\n");
    }

    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Helper method to add a genome with unspecified sex.
   * @param genome genome identifier
   * @return the properties (for convenience of adding properties to the genome)
   */
  public Properties addGenome(String genome) {
    if (mGenomes.add(genome)) {
      mGenomeProperties.put(genome, new Properties());
    }
    return mGenomeProperties.get(genome);
  }

  /**
   * Helper method to add a genome with known sex.
   * @param genome genome identifier
   * @param sex sex of the individual (null if unknown)
   * @return the properties (for convenience of adding properties to the genome)
   */
  public Properties addGenome(String genome, Sex sex) {
    String ssex = null;
    if (sex == Sex.MALE) {
      ssex = SEX_MALE;
    } else if (sex == Sex.FEMALE) {
      ssex = SEX_FEMALE;
    }
    return addGenome(genome, ssex);
  }

  /**
   * Helper method to add a genome with known sex.
   * @param genome genome identifier
   * @param sex sex of the individual (null if unknown)
   * @return the properties (for convenience of adding properties to the genome)
   */
  public Properties addGenome(String genome, String sex) {
    final Properties props = addGenome(genome);
    if (sex != null) {
      props.put(SEX_PROPERTY, sex);
    }
    return props;
  }

  /**
   * Helper method to add a parent child relationship between two genomes.
   * @param parent parent genome identifier
   * @param child child genome identifier
   * @return the relationship that was added (for convenience of adding properties)
   */
  public Relationship addParentChild(String parent, String child) {
    return addRelationship(RelationshipType.PARENT_CHILD, parent, child);
  }

  /**
   * Helper method to add a relationship between two genomes.
   * @param type the type of relationship
   * @param first the first genome in the relationship
   * @param second the second genome in the relationship
   * @return the relationship that was added (for convenience of adding properties)
   */
  public Relationship addRelationship(RelationshipType type, String first, String second) {
    return addRelationship(new Relationship(first, second, type));
  }

  /**
   * Helper method to add a relationship between two genomes.
   * @param rel the relationship
   * @return the relationship that was added (for convenience of adding properties)
   */
  public Relationship addRelationship(Relationship rel) {
    mRelationships.put(rel.first(), rel);
    mRelationships.put(rel.second(), rel);
    return rel;
  }

  /**
   * Things which may accept or reject a Genome
   */
  public interface GenomeFilter {
    /**
     * Returns true if the genome is accepted
     * @param genome the genome to test
     * @return true if the genome is accepted
     */
    boolean accept(String genome);
  }

  /**
   * Accepts genomes that have a relationship where they are in the specified end of the relationship
   */
  public static class HasRelationshipGenomeFilter implements GenomeFilter {
    final GenomeRelationships mPed;
    final RelationshipType mType;
    final boolean mIsFirst;
    final int mNumRequired;

    /**
     * Accepts genomes that have a matching relationship
     * @param ped the pedigree indicating whether the genome is primary
     * @param type the type of the relationship the genome must be a member of
     * @param first true if the genome must be first in the relationship, false if the genome must be second
     */
    public HasRelationshipGenomeFilter(GenomeRelationships ped, RelationshipType type, boolean first) {
      this(ped, type, first, 1);
    }

    /**
     * Accepts genomes that have a specified number of matching relationships
     * @param ped the pedigree indicating whether the genome is primary
     * @param type the type of the relationship the genome must be a member of
     * @param first true if the genome must be first in the relationship, false if the genome must be second
     * @param numRequired the minimum number of relationship matches in order to be accepted
     */
    public HasRelationshipGenomeFilter(GenomeRelationships ped, RelationshipType type, boolean first, int numRequired) {
      mPed = ped;
      mType = type;
      mIsFirst = first;
      mNumRequired = numRequired;
      assert mNumRequired > 0;
    }

    @Override
    public boolean accept(String genome) {
      if (mPed.hasGenome(genome)) {
        int matches = 0;
        for (Relationship rel : mPed.relationships(genome)) {
          if (rel.type() == mType && (mIsFirst ^ genome.equals(rel.second()))) {
            matches++;
            if (matches == mNumRequired) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  /**
   * Accepts genomes that are marked as primary
   */
  public static class PrimaryGenomeFilter implements GenomeFilter {
    final GenomeRelationships mPed;
    /**
     * Accepts genomes that are marked as primary
     * @param ped the pedigree indicating whether the genome is primary
     */
    public PrimaryGenomeFilter(GenomeRelationships ped) {
      mPed = ped;
    }
    @Override
    public boolean accept(String genome) {
      return mPed.hasGenome(genome) && Boolean.valueOf(mPed.getProperties(genome).getProperty(PRIMARY_GENOME_PROPERTY));
    }
  }

  /**
   * Accepts genomes that are marked as diseased
   */
  public static class DiseasedGenomeFilter implements GenomeFilter {
    final GenomeRelationships mPed;
    /**
     * Accepts genomes that are marked as primary
     * @param ped the pedigree indicating whether each genome is diseased
     */
    public DiseasedGenomeFilter(GenomeRelationships ped) {
      mPed = ped;
    }
    @Override
    public boolean accept(String genome) {
      return mPed.isDiseased(genome);
    }
  }

  /**
   * Accepts genomes that are match the specified sex
   */
  public static class GenomeSexFilter implements GenomeFilter {
    final GenomeRelationships mPed;
    final Sex mSex;
    /**
     * Accepts genomes that are marked as primary
     * @param ped the pedigree indicating the sex of each genome
     * @param sex the sex to accept by this filter
     */
    public GenomeSexFilter(GenomeRelationships ped, Sex sex) {
      mPed = ped;
      mSex = sex;
    }
    @Override
    public boolean accept(String genome) {
      return mSex == mPed.getSex(genome);
    }
  }

  /** Accepts genomes that are accepted by any one of multiple delegates */
  public static class OrGenomeFilter implements GenomeFilter {
    final GenomeFilter[] mDelegates;
    /**
     * Accepts genomes that are accepted by any of the delegate filters
     * @param filters the delegate filters
     */
    public OrGenomeFilter(GenomeFilter... filters) {
      mDelegates = filters;
    }
    @Override
    public boolean accept(String genome) {
      for (GenomeFilter filter : mDelegates) {
        if (filter.accept(genome)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Accepts genomes that are rejected by the delegate
   */
  public static class InvertGenomeFilter implements GenomeFilter {
    final GenomeFilter mDelegate;
    /**
     * Accepts genomes that are rejected by the delegate filter
     * @param filter the delegate filter
     */
    public InvertGenomeFilter(GenomeFilter filter) {
      mDelegate = filter;
    }
    @Override
    public boolean accept(String genome) {
      return !mDelegate.accept(genome);
    }
  }

  /**
   * Accepts genomes that are founders (i.e. have no parents) or half-founders (have only 1 parent)
   */
  public static class FounderGenomeFilter extends InvertGenomeFilter {
    /**
     * Accepts genomes that are founders (i.e. have no parents)
     * @param ped the ped containing genome relationships
     * @param includeHalfs if true, include those individuals that have only 1 parent
     */
    public FounderGenomeFilter(GenomeRelationships ped, boolean includeHalfs) {
      super(new GenomeRelationships.OrGenomeFilter(
        new GenomeRelationships.HasRelationshipGenomeFilter(ped, Relationship.RelationshipType.PARENT_CHILD, false, includeHalfs ? 2 : 1),
        new GenomeRelationships.HasRelationshipGenomeFilter(ped, RelationshipType.ORIGINAL_DERIVED, false, 1)));
    }
  }

  /**
   * Returns a count of the distinct groups that the given genomes represent within the current pedigree.
   *
   * @param genomes A list of genomes to group
   * @return number of groups detected
   */
  public int numberOfDisconnectedGroups(Collection<String> genomes) {
    final String[] genomes2 = genomes.toArray(new String[genomes.size()]);
    final int[] connectionsMatrix = new int[genomes.size() * genomes.size()];

    final HashSet<Integer> groupIds = new HashSet<>();
    // populate connections & minimize groups
    for (int j = 0; j < genomes2.length; j++) {
      int min = j + 1;
      final String g1 = genomes2[j];
      for (int i = 0; i < genomes2.length; i++) {
        final String g2 = genomes2[i];
        if (areRelated(g1, g2)) {
          connectionsMatrix[j * genomes2.length + i] = j + 1;  // set a default id
          // find any other connections and lower ids
          for (int k = 0; k < j; k++) {
            final int value = connectionsMatrix[k * genomes2.length + i];
            if (value != 0 && value < min) {
              min = value;
            }
          }
        }
      }
      // assign lower ids to this row
      for (int i = 0; i < genomes2.length; i++) {
        if (connectionsMatrix[j * genomes2.length + i] != 0) {
          connectionsMatrix[j * genomes2.length + i] = min;
          groupIds.add(min);  // add to set of unique ids
        }
      }
    }

    return groupIds.size();
  }

  private boolean areRelated(String genome1, String genome2) {
    if (genome1.equals(genome2)) {
      return true;
    }
    if (hasGenome(genome1) && hasGenome(genome2)) {
      final Relationship[] relationships = relationships(genome1);
      for (Relationship rel : relationships) {
        if (genome2.equals(rel.first()) || genome2.equals(rel.second())) {
          return true;
        }
      }
    }
    return false;
  }

}
