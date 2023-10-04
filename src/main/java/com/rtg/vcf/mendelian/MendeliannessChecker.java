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

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.FLOAT;
import static com.rtg.launcher.CommonFlags.INT;
import static com.rtg.launcher.CommonFlags.PEDIGREE_FLAG;
import static com.rtg.launcher.CommonFlags.TEMPLATE_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.SENSITIVITY_TUNING;

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
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.ChildPhasingVcfAnnotator;
import com.rtg.vcf.VcfAnnotator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.VcfHeader;

/**
 * Check output obeys rules of Mendelian inheritance and calculate concordance.
 */
public final class MendeliannessChecker extends AbstractCli {


  private static final String OUTPUT_FLAG = "output"; // Slightly different semantics to normal output flag
  private static final String OUTPUT_CONSISTENT_FLAG = "output-consistent";
  private static final String OUTPUT_INCONSISTENT_FLAG = "output-inconsistent";
  private static final String OUTPUT_AGGREGATE_FLAG = "Xoutput-aggregate";
  private static final String INPUT_FLAG = "input";
  private static final String ALL_RECORDS_FLAG = "all-records";
  private static final String LENIENT_FLAG = "lenient";
  private static final String PHASE_FLAG = "Xphase";
  private static final String CONCORDANCE_PCT_AGREEMENT = "min-concordance";
  private static final String CONCORDANCE_MIN_VARIANTS = "Xmin-variants";
  private static final String SEGREGATION_PROBABILITY_FLAG = "Xsegregation";
  private static final String STRICT_MISSING_FLAG = "Xstrict-missing";


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
    mFlags.registerRequired('i', INPUT_FLAG, File.class, FILE, "VCF file containing multi-sample variant calls. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    CommonFlags.initReferenceTemplate(mFlags, true);
    mFlags.registerOptional('o', OUTPUT_FLAG, File.class, FILE, "if set, output annotated calls to this VCF file. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_INCONSISTENT_FLAG, File.class, FILE, "if set, output only non-Mendelian calls to this VCF file").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_CONSISTENT_FLAG, File.class, FILE, "if set, output only consistent calls to this VCF file").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_AGGREGATE_FLAG, File.class, FILE, "if set, output aggregate genotype proportions to this file").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(ALL_RECORDS_FLAG, "use all records, regardless of filters (Default is to only process records where FILTER is \".\" or \"PASS\")").setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional(PHASE_FLAG, "phase calls based on pedigree").setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional(CONCORDANCE_PCT_AGREEMENT, Double.class, FLOAT, "percentage concordance required for consistent parentage", 99.0).setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional(CONCORDANCE_MIN_VARIANTS, Integer.class, INT, "minimum number of variants needed to check concordance", 2000).setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional(STRICT_MISSING_FLAG, "do strict checking that missing values contain expected ploidy").setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional(SEGREGATION_PROBABILITY_FLAG, "add segregation probability based on pedigree (only if exactly one family is present)").setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional('l', LENIENT_FLAG, "allow homozygous diploid calls in place of haploid calls and assume missing values are equal to the reference").setCategory(SENSITIVITY_TUNING);
    CommonFlags.initIndexFlags(mFlags);
    CommonFlags.initNoGzip(mFlags);
    mFlags.registerOptional(PEDIGREE_FLAG, File.class, FILE, "genome relationships PED file (Default is to extract pedigree information from VCF header fields)").setCategory(SENSITIVITY_TUNING);
    mFlags.setValidator(new Validator() {
      @Override
      public boolean isValid(CFlags flags) {
        return CommonFlags.validateInputFile(flags, INPUT_FLAG)
          && flags.checkInRange(CONCORDANCE_PCT_AGREEMENT, 0.0, false, 100.0, true)
          && flags.checkInRange(CONCORDANCE_MIN_VARIANTS, 0, Integer.MAX_VALUE)
          && validateNotStdio(flags, OUTPUT_AGGREGATE_FLAG, OUTPUT_CONSISTENT_FLAG, OUTPUT_INCONSISTENT_FLAG);
      }

      private boolean validateNotStdio(CFlags cflags, String... flags) {
        for (String f : flags) {
          if (cflags.isSet(f) && FileUtils.isStdio((File) cflags.getValue(f))) {
            cflags.setParseMessage("stdout is not supported for --" + f);
            return false;
          }
        }
        return true;
      }
    });
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
    try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader((File) mFlags.getValue(TEMPLATE_FLAG))) {
      sexMemo = new SexMemo(reader, ReferencePloidy.AUTO);
    }
    return sexMemo;
  }


  private void check(final PrintStream out, final PrintStream err) throws IOException {
    final boolean lenient = mFlags.isSet(LENIENT_FLAG);
    final boolean strictMissingPloidy = mFlags.isSet(STRICT_MISSING_FLAG);
    final boolean passOnly = !mFlags.isSet(ALL_RECORDS_FLAG);
    final boolean gzip = !mFlags.isSet(CommonFlags.NO_GZIP);
    final File outputVcfFile = mFlags.isSet(OUTPUT_FLAG) ? VcfUtils.getZippedVcfFileName(gzip, (File) mFlags.getValue(OUTPUT_FLAG)) : null;
    final boolean stdout = FileUtils.isStdio(outputVcfFile);
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
    final Object file = mFlags.getValue(INPUT_FLAG);
    try (VcfReader vr = VcfReader.openVcfReader((File) file)) {

      final VcfHeader header = vr.getHeader();
      final Set<Family> families = getFamilies(header);
      if (families.isEmpty()) {
        err.println("No family information found, no checking done.");
        return;
      }
      if (!stdout) {
        out.println("Checking: " + file);
        for (Family f : families) {
          out.println("Family: [" + f.getFather() + " + " + f.getMother() + "]" + " -> " + Arrays.toString(f.getChildren()));
        }
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

      final VcfHeader header2 = header.copy();
      mendAnnot.updateHeader(header2);
      for (final VcfAnnotator annotator : annotators) {
        annotator.updateHeader(header2);
      }

      int skippedRecords = 0;
      final VcfWriterFactory f = new VcfWriterFactory(mFlags).addRunInfo(true);
      try (VcfWriter outputVcf = outputVcfFile != null ? f.make(header2, outputVcfFile) : null) {
        try (VcfWriter inconsistentVcf = inconsistentVcfFile != null ? f.make(header2, inconsistentVcfFile) : null) {
          try (VcfWriter consistentVcf = consistentVcfFile != null ? f.make(header, consistentVcfFile) : null) {
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

                final MendeliannessAnnotator.Consistency status = mendAnnot.lastConsistency();
                if (status == MendeliannessAnnotator.Consistency.INCONSISTENT && (inconsistentVcf != null)) {
                  inconsistentVcf.write(rec);
                }
                if (status == MendeliannessAnnotator.Consistency.CONSISTENT && (consistentVcf != null)) {
                  consistentVcf.write(rec);
                }
                if (outputVcf != null) {
                  outputVcf.write(rec);
                }
              }
              if (aggregateWriter != null) {
                aggregateWriter.append("## Canonicalized, Exclude multiallelic").append(StringUtils.LS);
                aggregate.canonicalParents().filterMultiallelic().writeResults(aggregateWriter);

                aggregateWriter.append("## Canonicalized, Exclude multiallelic, Diploid Only").append(StringUtils.LS);
                aggregate.canonicalParents().filterMultiallelic().filterNonDiploid().writeResults(aggregateWriter);

                aggregateWriter.append("## Full Genotype Aggregates").append(StringUtils.LS);
                aggregate.writeResults(aggregateWriter);
              }
            }
          }
        }
      }

      if (!stdout) {
        if (skippedRecords > 0) {
          out.println(skippedRecords + " non-pass records were skipped");
        }
        mendAnnot.printInconsistentSamples(out, (Integer) mFlags.getValue(CONCORDANCE_MIN_VARIANTS), (Double) mFlags.getValue(CONCORDANCE_PCT_AGREEMENT));
        mendAnnot.printSummary(out);
      }
    }
  }

  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    try (PrintStream os = new PrintStream(out)) {
      check(os, err);
    }
    return 0;
  }
}
