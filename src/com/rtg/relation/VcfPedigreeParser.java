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
import java.util.Set;

import com.rtg.reference.Sex;
import com.rtg.relation.Relationship.RelationshipType;
import com.rtg.relation.Relationship.RelationshipTypeFilter;
import com.rtg.relation.Relationship.SecondInRelationshipFilter;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.PedigreeField;
import com.rtg.vcf.header.SampleField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Reads genome relationships from VCF SAMPLE/PEDIGREE declarations.
 *
 */
public final class VcfPedigreeParser {

  private VcfPedigreeParser() {
  }

  /**
   * Load genome relationships from a VCF header sample and pedigree lines
   * @param vcfFile the VCF file to obtain pedigree information from
   * @return structure containing relationships
   * @throws IOException if the VCF file could not be parsed
   */
  static GenomeRelationships loadFile(File vcfFile) throws IOException {
    try (VcfReader vr = VcfReader.openVcfReader(vcfFile)) {
      return load(vr.getHeader());
    }
  }

  /**
   * Load genome relationships from a VCF header sample and pedigree lines
   * @param header input source
   * @return structure containing relationships
   */
  public static GenomeRelationships load(VcfHeader header) {
    final GenomeRelationships ped = new GenomeRelationships();

    // Include samples from genotype columns
    for (String sample : header.getSampleNames()) {
      ped.addGenome(sample).setProperty(GenomeRelationships.PRIMARY_GENOME_PROPERTY, "true");
    }

    for (String line : header.getGenericMetaInformationLines()) {
      if (line.startsWith(VcfHeader.META_STRING + "diseased=")) {
        for (String sample : StringUtils.split(line.substring((VcfHeader.META_STRING + "diseased=").length()), ',')) {
          ped.addGenome(sample).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
        }
      }
    }

    // Include explicitly specified samples, may contain sexes
    for (SampleField sampleField : header.getSampleLines()) {
      final String sample = sampleField.getId();
      String sex = null;
      final Sex sexField = sampleField.getSex();
      if (sexField != null) {
        sex = sexField.name();
      }
      ped.addGenome(sample, sex);
      // TODO: should we somehow be loading and preserving contamination data for somatic calling?
    }

    // Include pedigree lines, contains relationships and sex information
    for (PedigreeField pedField : header.getPedigreeLines()) {
      parsePedLine(ped, pedField);
    }

    return ped;
  }

  static void parsePedLine(GenomeRelationships ped, PedigreeField pedField) {
    final String individualId = pedField.getChild();
    if (individualId != null) {
      final String paternalId = pedField.getFather();
      final String maternalId = pedField.getMother();

      ped.addGenome(individualId);

      if (paternalId != null) {
        if (ped.getSex(paternalId) == Sex.FEMALE) { // Sex.EITHER is OK though, we'll override it now we know they are a dad
          throw new NoTalkbackSlimException("Conflicting SAMPLE/PEDIGREE definitions of sex for individual " + paternalId);
        }
        ped.addGenome(paternalId, GenomeRelationships.SEX_MALE);
        ped.addParentChild(paternalId, individualId);
      }

      if (maternalId != null) {
        if (ped.getSex(maternalId) == Sex.MALE) {   // Sex.EITHER is OK though, we'll override it now we know they are a mother
          throw new NoTalkbackSlimException("Conflicting SAMPLE/PEDIGREE definitions of sex for individual " + maternalId);
        }
        ped.addGenome(maternalId, GenomeRelationships.SEX_FEMALE);
        ped.addParentChild(maternalId, individualId);
      }
    } else {
      final String derivedId = pedField.getDerived();
      final String originalId = pedField.getOriginal();
      if (derivedId != null && originalId != null) {
        ped.addGenome(derivedId);
        ped.addGenome(originalId);
        ped.addRelationship(RelationshipType.ORIGINAL_DERIVED, originalId, derivedId);
      } else {
        Diagnostic.warning("Pedigree line contains no pedigree information: " + pedField);
      }
    }
  }


  /**
   * Adds appropriate fields to a VCF header to represent the supplied relationships. Determines the set of
   * primary samples from relationship properties
   * @param vcf the VCF header
   * @param pedigree the relationships
   */
  public static void addPedigreeFields(VcfHeader vcf, GenomeRelationships pedigree) {
    addPedigreeFields(vcf, pedigree, pedigree.filterByGenomes(new GenomeRelationships.PrimaryGenomeFilter(pedigree)).genomes());
  }

  /**
   * Adds appropriate fields to a VCF header to represent the supplied relationships
   * @param vcf the VCF header
   * @param gr the relationships
   * @param outputSamples the list of sample names to include in output as primary samples
   */
  public static void addPedigreeFields(VcfHeader vcf, GenomeRelationships gr, String[] outputSamples) {
    final Relationship[] derived = gr.relationships(Relationship.RelationshipType.ORIGINAL_DERIVED);
    if (derived.length > 0 && derived.length == 1 && gr.genomes().length == 2 && derived[0].getProperty("contamination") != null) { // Somatic caller -- should be a better way to handle this
      final Sex sex = gr.getSex(derived[0].first());
      String sexStr = "";
      if (sex == Sex.FEMALE || sex == Sex.MALE) {
        sexStr = ",Sex=" + sex.toString();
      }
      vcf.addLine(VcfHeader.SAMPLE_STRING + "=<ID=" + derived[0].first() + ",Genomes=" + derived[0].first() + ",Mixture=1.0" + sexStr + ",Description=\"Original genome\">");
      final double con = Double.parseDouble(derived[0].getProperty("contamination"));
      final String mixture = Utils.realFormat(con, 2) + VcfUtils.VALUE_SEPARATOR + Utils.realFormat(1 - con, 2);
      vcf.addLine(VcfHeader.SAMPLE_STRING + "=<ID=" + derived[0].second() + ",Genomes=" + derived[0].first() + VcfUtils.VALUE_SEPARATOR + derived[0].second() + ",Mixture=" + mixture + sexStr + ",Description=\"Original genome;Derived genome\">");
      vcf.addLine(VcfHeader.PEDIGREE_STRING + "=<Derived=" + derived[0].second() + ",Original=" + derived[0].first() + ">");
    } else {
      final StringBuilder diseased = new StringBuilder();
      for (final String genome : outputSamples) {
        final Sex sex = gr.getSex(genome);
        if (sex == Sex.FEMALE || sex == Sex.MALE) {
          vcf.addLine(VcfHeader.SAMPLE_STRING + "=<ID=" + genome + ",Sex=" + sex.toString() + ">");
        }
        if (gr.isDiseased(genome)) {
          diseased.append(",").append(genome);
        }
      }
      if (diseased.length() > 0) {
        vcf.addLine(VcfHeader.META_STRING + "diseased=" + diseased.substring(1));
      }
      if (gr.relationships(Relationship.RelationshipType.PARENT_CHILD).length > 0) {
        // Do nuclear families
        final Set<Family> families = Family.getFamilies(gr, true, null); //sloppy parsing to support family disease caller that doesn't enforce parent sex
        for (final Family f : families) {
          for (final String child : f.getChildren()) {
            if (isOutputGenome(outputSamples, child) || isOutputGenome(outputSamples, f.getMother()) || isOutputGenome(outputSamples, f.getFather())) {
              vcf.addLine(VcfHeader.PEDIGREE_STRING + "=<Child=" + child + ",Mother=" + f.getMother() + ",Father=" + f.getFather() + ">");
            }
          }
        }
        // Do non-nuclear for single parent only
        for (String child : gr.genomes()) {
          final Relationship[] relations = gr.relationships(new RelationshipTypeFilter(RelationshipType.PARENT_CHILD), new SecondInRelationshipFilter(child));
          if (relations.length == 1) {
            final String parent = relations[0].first();
            if (isOutputGenome(outputSamples, child) || isOutputGenome(outputSamples, parent)) {
              if (gr.getSex(parent) == Sex.FEMALE) {
                vcf.addLine(VcfHeader.PEDIGREE_STRING + "=<Child=" + child + ",Mother=" + parent + ">");
              } else { // Arbitrarily make father if sex is not known? (Consistent with sloppy get families call)
                vcf.addLine(VcfHeader.PEDIGREE_STRING + "=<Child=" + child + ",Father=" + parent + ">");
              }
            }
          }
        }
      }
      for (Relationship od : derived) {
        if (isOutputGenome(outputSamples, od.first()) || isOutputGenome(outputSamples, od.second())) {
          vcf.addLine(VcfHeader.PEDIGREE_STRING + "=<Derived=" + od.second() + ",Original=" + od.first() + ">");
        }
      }
    }
  }

  private static boolean isOutputGenome(String[] sampleNames, String genome) {
    for (final String name : sampleNames) {
      if (name.equals(genome)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Test out converting RTG to PED file from command line
   * @param args name of RTG style relationships file
   * @throws Exception if there is a problem.
   */
  public static void main(String... args) throws Exception {
    final GenomeRelationships pedigree;
    if (args[0].endsWith(".ped")) {
      pedigree = PedFileParser.loadFile(new java.io.File(args[0]));
    } else if (args[0].endsWith(".vcf") || args[0].endsWith(".vcf.gz")) {
      pedigree = VcfPedigreeParser.loadFile(new java.io.File(args[0]));
    } else {
      pedigree = RelationshipsFileParser.loadFile(new java.io.File(args[0]));
    }

    System.out.print(pedigree);

    // Output the relationships in ped format
    System.out.print(PedFileParser.toString(pedigree));

    // Output list of families identified in the ped file:
    final Set<Family> families = Family.getFamilies(pedigree, false, null);
    for (Family f : families) {
      System.out.println("Family: " + f);
      String familycmd = "";
      familycmd += "--father " + f.getFather();
      familycmd += " --mother " + f.getMother();
      for (String child : f.getChildren()) {
        if (pedigree.getSex(child) == Sex.MALE) {
          familycmd += " --son " + child;
        } else if (pedigree.getSex(child) == Sex.FEMALE) {
          familycmd += " --daughter " + child;
        } else {
          System.err.println("Child has unknown sex: " + child);
        }
      }
      System.out.println("Sample arguments for rtg family: " + familycmd);
    }
  }

}
