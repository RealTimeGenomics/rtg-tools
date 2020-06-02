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
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reference.Sex;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.io.LineWriter;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class PedFilterCli extends AbstractCli {

  private static final String KEEP_PRIMARY = "keep-primary";
  private static final String KEEP_FAMILY = "keep-family";
  private static final String KEEP_IDS = "keep-ids";
  private static final String KEEP_SEX = "Xkeep-sex"; // TODO - behaviour of 'either' is counterintuitive
  private static final String REMOVE_PARENTAGE = "remove-parentage";
  private static final String VCF_OUT = "vcf";
  private static final String IDS_OUT = "Xprint-ids"; // Handy to use with vcffilter --keep-sample/--remove-sample, but overlaps with pedstats *-ids flags

  @Override
  public String moduleName() {
    return "pedfilter";
  }

  @Override
  public String description() {
    return "filter and convert a pedigree file";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Filter and convert a pedigree file.");
    CommonFlagCategories.setCategories(mFlags);

    mFlags.registerRequired(File.class, CommonFlags.FILE, "the pedigree file to process, may be PED or VCF, use '-' to read from stdin").setCategory(CommonFlagCategories.INPUT_OUTPUT);

    mFlags.registerOptional(KEEP_PRIMARY, "keep only primary individuals (those with a PED individual line / VCF sample column)").setCategory(CommonFlagCategories.FILTERING);
    mFlags.registerOptional(KEEP_FAMILY, String.class, "STRING", "keep only individuals with the specified family ID").setCategory(CommonFlagCategories.FILTERING).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_IDS, String.class, "STRING", "keep only individuals with the specified ID").setCategory(CommonFlagCategories.FILTERING).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_SEX, Sex.class, "STRING", "keep only individuals with the specified sex").setCategory(CommonFlagCategories.FILTERING).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(REMOVE_PARENTAGE, "remove all parent-child relationship information").setCategory(CommonFlagCategories.FILTERING);

    mFlags.registerOptional(VCF_OUT, "output pedigree in the form of a VCF header").setCategory(CommonFlagCategories.REPORTING);
    mFlags.registerOptional(IDS_OUT, "output as a comma separated list of individual IDs").setCategory(CommonFlagCategories.REPORTING);
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File pedFile = (File) mFlags.getAnonymousValue(0);
    GenomeRelationships pedigree = GenomeRelationships.loadGenomeRelationships(pedFile);

    final Collection<GenomeRelationships.GenomeFilter> filters = new ArrayList<>();
    if (mFlags.isSet(KEEP_PRIMARY)) {
      filters.add(new GenomeRelationships.PrimaryGenomeFilter(pedigree));
    }
    if (mFlags.isSet(KEEP_FAMILY)) {
      filters.add(new GenomeRelationships.FamilyIdFilter(pedigree, mFlags.getValues(KEEP_FAMILY).stream().map(String.class::cast).collect(Collectors.toSet())));
    }
    if (mFlags.isSet(KEEP_IDS)) {
      filters.add(new GenomeRelationships.IdFilter(pedigree, mFlags.getValues(KEEP_IDS).stream().map(String.class::cast).collect(Collectors.toSet())));
    }
    if (mFlags.isSet(KEEP_SEX)) {
      filters.add(new GenomeRelationships.GenomeSexFilter(pedigree, mFlags.getValues(KEEP_SEX).stream().map(Sex.class::cast).collect(Collectors.toSet())));
    }

    pedigree = pedigree.filterByGenomes(filters.toArray(new GenomeRelationships.GenomeFilter[0]));

    if (mFlags.isSet(REMOVE_PARENTAGE)) {
      pedigree = pedigree.filterByRelationships(new Relationship.NotFilter(new Relationship.RelationshipTypeFilter(Relationship.RelationshipType.PARENT_CHILD)));
    }

    try (LineWriter w = new LineWriter(new OutputStreamWriter(out))) {
      if (mFlags.isSet(VCF_OUT)) {      // Output the relationships as a VCF header
        final VcfHeader header = new VcfHeader();
        header.addCommonHeader();
        VcfPedigreeParser.addPedigreeFields(header, pedigree);
        for (String sample : pedigree.filterByGenomes(new GenomeRelationships.PrimaryGenomeFilter(pedigree)).genomes()) {
          header.addSampleName(sample);
        }
        w.write(header.toString());
      } else if (mFlags.isSet(IDS_OUT)) {      // Output only individual IDs
        w.writeln(StringUtils.join(",", pedigree.genomes()));
      } else {             // Output the relationships in ped format
        w.write(PedFileParser.toString(pedigree));

      }
    }
    return 0;
  }

}
