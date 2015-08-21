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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import com.rtg.launcher.AbstractCli;
import com.rtg.reference.Sex;
import com.rtg.util.TextTable;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.io.LineWriter;

/**
 */
public class PedStatsCli extends AbstractCli {

  private static final String PRIMARY_IDS = "primary-ids";
  private static final String MALE_IDS = "male-ids";
  private static final String FEMALE_IDS = "female-ids";
  private static final String PATERNAL_IDS = "paternal-ids";
  private static final String MATERNAL_IDS = "maternal-ids";
  private static final String FOUNDER_IDS = "founder-ids";
  private static final String FAMILIES_OUT = "families";
  private static final String DOT_OUT = "dot";
  private static final String DUMP = "Xdump";
  private static final String FAMILY_FLAGS = "Xfamily-flags";
  private static final String ORDERING = "Xordering";

  @Override
  public String moduleName() {
    return "pedstats";
  }

  @Override
  public String description() {
    return "print information about a pedigree file";
  }

  @Override
  protected void initFlags() {
    mFlags.registerExtendedHelp();
    mFlags.setDescription("Output information from pedigree files of various formats. For quick pedigree visualization using graphviz, try:\n\n"
        + "  dot -Tpng <(rtg pedstats --dot \"A Title\" PEDFILE) | display -\n"
        + "\nor for a larger pedigree:\n\n"
        + "  dot -Tpdf -o mypedigree.pdf <(rtg pedstats --dot \"A Title\" PEDFILE)\n"
    );
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired(File.class, "FILE", "the pedigree file to process, may be PED or VCF, use '-' to read from stdin").setCategory(CommonFlagCategories.INPUT_OUTPUT);

    mFlags.registerOptional(PRIMARY_IDS, "output ids of all primary individuals").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(MALE_IDS, "output ids of all males").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(FEMALE_IDS, "output ids of all females").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(PATERNAL_IDS, "output ids of paternal individuals").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(MATERNAL_IDS, "output ids of maternal individuals").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(FOUNDER_IDS, "output ids of all founders").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(FAMILIES_OUT, "output information about family structures").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(DOT_OUT, String.class, "STRING", "output pedigree in GraphViz format, using the supplied text as a title").setCategory(CommonFlagCategories.REPORTING);

    mFlags.registerOptional(DUMP, "dump full relationships structure").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(FAMILY_FLAGS, "output command-line flags for family caller").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(ORDERING, "output family processing order for use during forward backward algorithm").setCategory(CommonFlagCategories.REPORTING);

    mFlags.checkAtMostOne(PRIMARY_IDS, MALE_IDS, FEMALE_IDS, PATERNAL_IDS, MATERNAL_IDS, FAMILIES_OUT, DOT_OUT, DUMP, ORDERING, FAMILY_FLAGS);
  }

  private static void writeIds(LineWriter w, String... genomes) throws IOException {
    for (String genome : genomes) {
      w.writeln(genome);
    }
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File pedFile = (File) mFlags.getAnonymousValue(0);
    final GenomeRelationships pedigree = GenomeRelationships.loadGenomeRelationships(pedFile);

    try (LineWriter w = new LineWriter(new OutputStreamWriter(out))) {

      if (mFlags.isSet(DOT_OUT)) {      // Output dotty stuff
        w.writeln(pedigree.toGraphViz((String) mFlags.getValue(DOT_OUT)));

      } else if (mFlags.isSet(PRIMARY_IDS)) {
        writeIds(w, pedigree.genomes(new GenomeRelationships.PrimaryGenomeFilter(pedigree)));
      } else if (mFlags.isSet(MALE_IDS)) {
        writeIds(w, pedigree.genomes(new GenomeRelationships.GenomeSexFilter(pedigree, Sex.MALE)));
      } else if (mFlags.isSet(FEMALE_IDS)) {
        writeIds(w, pedigree.genomes(new GenomeRelationships.GenomeSexFilter(pedigree, Sex.FEMALE)));
      } else if (mFlags.isSet(MATERNAL_IDS)) {
        final String[] genomes = pedigree.genomes(new GenomeRelationships.HasRelationshipGenomeFilter(pedigree, Relationship.RelationshipType.PARENT_CHILD, true),
          new GenomeRelationships.GenomeSexFilter(pedigree, Sex.FEMALE));
        writeIds(w, genomes);
      } else if (mFlags.isSet(PATERNAL_IDS)) {
        final String[] genomes = pedigree.genomes(new GenomeRelationships.HasRelationshipGenomeFilter(pedigree, Relationship.RelationshipType.PARENT_CHILD, true),
          new GenomeRelationships.GenomeSexFilter(pedigree, Sex.MALE));
        writeIds(w, genomes);
      } else if (mFlags.isSet(FOUNDER_IDS)) {
        final String[] genomes = pedigree.genomes(new GenomeRelationships.FounderGenomeFilter(pedigree, false));
        writeIds(w, genomes);
      } else if (mFlags.isSet(FAMILIES_OUT)) { // Output list of families identified in the ped file:
        final Set<Family> families = Family.getFamilies(pedigree, false, null);
        for (final Family f : families) {
          w.writeln(f.toString());
        }

      } else if (mFlags.isSet(DUMP)) {
        w.writeln(pedigree.toString());

      } else if (mFlags.isSet(ORDERING)) {      // ordering stuff
        final List<Family> families = MultiFamilyOrdering.orderFamiliesAndSetMates(Family.getFamilies(pedigree, false, null));
        w.writeln("Families in processing order:");
        for (final Family f : families) {
          w.writeln(f.toString());
        }
        final Set<String> nonMonog = MultiFamilyOrdering.nonMonogamousSamples(families);
        if (nonMonog.size() > 0) {
          w.writeln("The following individuals are not monogamous:");
          for (final String s : nonMonog) {
            w.writeln(s);
          }
        } else {
          w.writeln("Set of families is monogamous");
        }

      } else if (mFlags.isSet(FAMILY_FLAGS)) {
        final Set<Family> families = Family.getFamilies(pedigree, false, null);
        for (final Family f : families) {
          String familycmd = "";
          familycmd += "--father " + f.getFather();
          familycmd += " --mother " + f.getMother();
          for (final String child : f.getChildren()) {
            if (pedigree.getSex(child) == Sex.MALE) {
              familycmd += " --son " + child;
            } else if (pedigree.getSex(child) == Sex.FEMALE) {
              familycmd += " --daughter " + child;
            } else {
              System.err.println("Child has unknown sex: " + child);
            }
          }
          w.writeln("rtg family " + familycmd);
        }
      } else { // Output summary information
        final TextTable table = new TextTable();
        table.setAlignment(TextTable.Align.LEFT);
        w.writeln("Pedigree file: " + pedFile.toString());
        w.writeln();
        table.addRow("Total samples:", "" + pedigree.genomes().length);
        table.addRow("Primary samples:", "" + pedigree.genomes(new GenomeRelationships.PrimaryGenomeFilter(pedigree)).length);
        table.addRow("Male samples:", "" + pedigree.genomes(new GenomeRelationships.GenomeSexFilter(pedigree, Sex.MALE)).length);
        table.addRow("Female samples:", "" + pedigree.genomes(new GenomeRelationships.GenomeSexFilter(pedigree, Sex.FEMALE)).length);
        table.addRow("Afflicted samples:", "" + pedigree.genomes(new GenomeRelationships.DiseasedGenomeFilter(pedigree)).length);
        table.addRow("Founder samples:", "" + pedigree.genomes(new GenomeRelationships.FounderGenomeFilter(pedigree, false)).length);
        table.addRow("Parent-child relationships:", "" + pedigree.relationships(new Relationship.RelationshipTypeFilter(Relationship.RelationshipType.PARENT_CHILD)).length);
        table.addRow("Other relationships:", "" + pedigree.relationships(new Relationship.NotFilter(new Relationship.RelationshipTypeFilter(Relationship.RelationshipType.PARENT_CHILD))).length);
        table.addRow("Families:", "" + Family.getFamilies(pedigree, false, null).size());
        w.writeln(table.toString());
      }
    }
    return 0;
  }
}
