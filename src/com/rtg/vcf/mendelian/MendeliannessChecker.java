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
package com.rtg.vcf.mendelian;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.SexMemo;
import com.rtg.relation.Family;
import com.rtg.relation.GenomeRelationships;
import com.rtg.relation.PedigreeException;
import com.rtg.relation.VcfPedigreeParser;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.AsyncVcfWriter;
import com.rtg.vcf.ChildPhasingVcfAnnotator;
import com.rtg.vcf.DefaultVcfWriter;
import com.rtg.vcf.VcfAnnotator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.VcfHeader;

/**
 * Check output obeys rules of Mendelian inheritance and calculate concordance.
 */
public final class MendeliannessChecker extends AbstractCli {


  private static final String OUTPUT_FLAG = "output";
  private static final String OUTPUT_CONSISTENT_FLAG = "output-consistent";
  private static final String OUTPUT_INCONSISTENT_FLAG = "output-inconsistent";
  private static final String OUTPUT_AGGREGATE_FLAG = "Xoutput-aggregate";
  private static final String INPUT_FLAG = "input";
  private static final String ALL_RECORDS_FLAG = "all-records";
  private static final String ALLOW_FLAG = "lenient";
  private static final String PHASE_FLAG = "Xphase";
  private static final String CONCORDANCE_PCT_AGREEMENT = "min-concordance";
  private static final String CONCORDANCE_MIN_VARIANTS = "Xmin-variants";
  private static final String SEGREGATION_PROBABILITY_FLAG = "Xsegregation";
  private static final String STRICT_MISSING_FLAG = "Xstrict-missing";
  private static final String PEDIGREE_FLAG = "pedigree";


  @Override
  public String moduleName() {
    return "mendelian";
  }

  @Override
  public String description() {
    return "check a multisample VCF for Mendelian consistency";
  }

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription("Check a multi-sample VCF for Mendelian consistency.");
    mFlags.registerRequired('i', INPUT_FLAG, File.class, CommonFlags.FILE, "VCF file containing multi-sample variant calls or '-' to read from standard input").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    CommonFlags.initReferenceTemplate(mFlags, true);
    mFlags.registerOptional(OUTPUT_INCONSISTENT_FLAG, File.class, CommonFlags.FILE, "if set, output only non-Mendelian calls to this VCF file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_CONSISTENT_FLAG, File.class, CommonFlags.FILE, "if set, output only consistent calls to this VCF file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_FLAG, File.class, CommonFlags.FILE, "if set, output annotated calls to this VCF file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_AGGREGATE_FLAG, File.class, CommonFlags.FILE, "if set, output aggregate genotype proportions to this file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(ALL_RECORDS_FLAG, "use all records, regardless of filters (Default is to only process records where FILTER is \".\" or \"PASS\")").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    mFlags.registerOptional(PHASE_FLAG, "phase calls based on pedigree").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    mFlags.registerOptional(CONCORDANCE_PCT_AGREEMENT, Double.class, CommonFlags.FLOAT, "percentage concordance required for consistent parentage", 99.0).setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    mFlags.registerOptional(CONCORDANCE_MIN_VARIANTS, Integer.class, CommonFlags.INT, "minimum number of variants needed to check concordance", 2000).setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    mFlags.registerOptional(STRICT_MISSING_FLAG, "do strict checking that missing values contain expected ploidy").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    mFlags.registerOptional(SEGREGATION_PROBABILITY_FLAG, "add segregation probability based on pedigree (only if exactly one family is present)").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    mFlags.registerOptional('l', ALLOW_FLAG, "allow homozygous diploid calls in place of haploid calls and assume missing values are equal to the reference").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    CommonFlags.initIndexFlags(mFlags);
    CommonFlags.initNoGzip(mFlags);
    mFlags.registerOptional(PEDIGREE_FLAG, File.class, CommonFlags.FILE, "genome relationships PED file (Default is to extract pedigree information from VCF header fields)").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
  }


  private Set<Family> getFamilies(VcfHeader header) throws IOException {
    final GenomeRelationships pedigree;
    if (mFlags.isSet(PEDIGREE_FLAG)) {
      pedigree = GenomeRelationships.loadGenomeRelationships((File) mFlags.getValue(PEDIGREE_FLAG));
    } else {
      // Get initial relationships from header samples, PEDIGREE and (if present) SAMPLE lines
      pedigree = VcfPedigreeParser.load(header);
    }
    try {
      return Family.getFamilies(pedigree, true, new HashSet<>(header.getSampleNames())); // Sloppy as we check sex explicitly
    } catch (PedigreeException e) {
      throw new NoTalkbackSlimException(e.getMessage());
    }
  }

  private SexMemo getSexMemo() throws IOException {
    final SexMemo sexMemo;
    try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader((File) mFlags.getValue(CommonFlags.TEMPLATE_FLAG))) {
      sexMemo = new SexMemo(reader, ReferencePloidy.AUTO);
    }
    return sexMemo;
  }


  private void check(final VcfReader vr, final PrintStream out, final PrintStream err) throws IOException {
    final VcfHeader header = vr.getHeader();

    final Set<Family> families = getFamilies(header);
    if (families.size() == 0) {
      err.println("No family information found, no checking done.");
      return;
    }
    for (Family f : families) {
      out.println("Family: [" + f.getFather() + " + " + f.getMother() + "]" + " -> " + Arrays.toString(f.getChildren()));
    }

    final boolean lenient = mFlags.isSet(ALLOW_FLAG);
    final boolean strictMissingPloidy = mFlags.isSet(STRICT_MISSING_FLAG);
    final boolean passOnly = !mFlags.isSet(ALL_RECORDS_FLAG);
    final boolean gzip = !mFlags.isSet(CommonFlags.NO_GZIP);
    final boolean index = !mFlags.isSet(CommonFlags.NO_INDEX);
    final File outputVcfFile = mFlags.isSet(OUTPUT_FLAG) ? VcfUtils.getZippedVcfFileName(gzip, (File) mFlags.getValue(OUTPUT_FLAG)) : null;
    final File inconsistentVcfFile = mFlags.isSet(OUTPUT_INCONSISTENT_FLAG) ? VcfUtils.getZippedVcfFileName(gzip, (File) mFlags.getValue(OUTPUT_INCONSISTENT_FLAG)) : null;
    final File consistentVcfFile = mFlags.isSet(OUTPUT_CONSISTENT_FLAG) ? VcfUtils.getZippedVcfFileName(gzip, (File) mFlags.getValue(OUTPUT_CONSISTENT_FLAG)) : null;
    final boolean annotate = outputVcfFile != null || inconsistentVcfFile != null || consistentVcfFile != null;
    final File aggregateOutputFile;
    final GenotypeProportions aggregate;
    if (mFlags.isSet(OUTPUT_AGGREGATE_FLAG)) {
      aggregateOutputFile = FileUtils.getZippedFileName(gzip, (File) mFlags.getValue(OUTPUT_AGGREGATE_FLAG));
      aggregate = new GenotypeProportions();
    } else {
      aggregateOutputFile = null;
      aggregate = null;
    }

    final List<VcfAnnotator> annotators = new ArrayList<>();

    final MendeliannessAnnotator mendAnnot = new MendeliannessAnnotator(families, getSexMemo(), aggregate, annotate, lenient, strictMissingPloidy);
    annotators.add(mendAnnot);

    if (annotate) { // All these other annotators do is annotate, so don't bother if no VCF files are being output
      if (mFlags.isSet(PHASE_FLAG)) {
        annotators.add(new ChildPhasingVcfAnnotator(families));
      }
// TODO, Tidy up the SegregationVcfAnnotator so it can be moved to tools
//      if (mFlags.isSet(SEGREGATION_PROBABILITY_FLAG) && families.size() == 1) {
//        annotators.add(new SegregationVcfAnnotator(families.iterator().next()));
//      }
    }

    header.addRunInfo(); // Ensure CL in output
    final VcfHeader header2 = header.copy();
    mendAnnot.updateHeader(header2);
    for (final VcfAnnotator annotator: annotators) {
      annotator.updateHeader(header2);
    }

    int skippedRecords = 0;
    try (VcfWriter outputVcf = outputVcfFile != null ? new AsyncVcfWriter(new DefaultVcfWriter(header2, outputVcfFile, null, gzip, index)) : null) {
      try (VcfWriter inconsistentVcf = inconsistentVcfFile != null ? new AsyncVcfWriter(new DefaultVcfWriter(header2, inconsistentVcfFile, null, gzip, index)) : null) {
        try (VcfWriter consistentVcf = consistentVcfFile != null ? new AsyncVcfWriter(new DefaultVcfWriter(header, consistentVcfFile, null, gzip, index)) : null) {
          try (OutputStreamWriter aggregateWriter = aggregateOutputFile != null ? new OutputStreamWriter(FileUtils.createOutputStream(aggregateOutputFile)) : null) {
            while (vr.hasNext()) {
              final VcfRecord rec = vr.next();
              if (passOnly && rec.isFiltered()) {
                ++skippedRecords;
                continue;
              }

              for (final VcfAnnotator annotator : annotators) {
                annotator.annotate(rec);
              }

              final boolean inconsistent = mendAnnot.wasLastInconsistent();
              if (inconsistent && (inconsistentVcf != null)) {
                inconsistentVcf.write(rec);
              }
              if (!inconsistent && (consistentVcf != null)) {
                consistentVcf.write(rec);
              }
              if (outputVcf != null) {
                outputVcf.write(rec);
              }
            }
            if (aggregateWriter != null) {
              aggregate.writeResults(aggregateWriter);
            }
          }
        }
      }
    }

    if (skippedRecords > 0) {
      out.println(skippedRecords + " non-pass records were skipped");
    }

    mendAnnot.printInconsistentSamples(out, (Integer) mFlags.getValue(CONCORDANCE_MIN_VARIANTS), (Double) mFlags.getValue(CONCORDANCE_PCT_AGREEMENT));

    mendAnnot.printSummary(out);
  }

  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    try (PrintStream os = new PrintStream(out)) {
      final Object file = mFlags.getValue(INPUT_FLAG);
      os.println("Checking: " + file);
      try (VcfReader vr = VcfReader.openVcfReader((File) file)) {
        check(vr, os, err);
      }
    }
    return 0;
  }
}
