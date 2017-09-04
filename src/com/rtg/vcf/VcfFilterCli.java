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
package com.rtg.vcf;

import static com.rtg.launcher.CommonFlags.BED_REGIONS_FLAG;
import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.FLOAT;
import static com.rtg.launcher.CommonFlags.INPUT_FLAG;
import static com.rtg.launcher.CommonFlags.INT;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.NO_HEADER;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.RESTRICTION_FLAG;
import static com.rtg.launcher.CommonFlags.STRING;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.bed.BedUtils;
import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.sam.SamRangeUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfFilterStatistics.Stat;
import com.rtg.vcf.header.VcfHeader;

/**
 * Class inputs the variant results and filters them based on various criteria.
 *
 */
public final class VcfFilterCli extends AbstractCli {

  private static final String REMOVE_INFO = "remove-info";
  private static final String KEEP_INFO = "keep-info";

  private static final String REMOVE_FILTER = "remove-filter";
  private static final String KEEP_FILTER = "keep-filter";
  private static final String INCLUDE_BED = "include-bed";
  private static final String EXCLUDE_BED = "exclude-bed";
  private static final String INCLUDE_VCF = "include-vcf";
  private static final String EXCLUDE_VCF = "exclude-vcf";

  private static final String REMOVE_SAME_AS_REF = "remove-same-as-ref";
  private static final String MIN_ALLELES = "min-alleles";
  private static final String MAX_ALLELES = "max-alleles";
  private static final String REMOVE_HOM = "remove-hom";
  private static final String REMOVE_ALL_SAME_AS_REF = "remove-all-same-as-ref";

  private static final String SNPS_ONLY = "snps-only";
  private static final String NON_SNPS_ONLY = "non-snps-only";

  private static final String MIN_DEPTH = "min-read-depth";
  private static final String MAX_DEPTH = "max-read-depth";
  private static final String MIN_COMBINED_DEPTH = "min-combined-read-depth";
  private static final String MAX_COMBINED_DEPTH = "max-combined-read-depth";
  private static final String MIN_GENOTYPE_QUALITY = "min-genotype-quality";
  private static final String MAX_GENOTYPE_QUALITY = "max-genotype-quality";
  private static final String MIN_QUALITY = "min-quality";
  private static final String MAX_QUALITY = "max-quality";
  private static final String MAX_AMBIGUITY_RATIO = "max-ambiguity-ratio";
  private static final String MIN_AVR_SCORE = "min-avr-score";
  private static final String MAX_AVR_SCORE = "max-avr-score";
  private static final String MIN_DENOVO_SCORE = "min-denovo-score";
  private static final String MAX_DENOVO_SCORE = "max-denovo-score";

  private static final String FAIL_FLAG = "fail";
  private static final String FAIL_SAMPLES_FLAG = "fail-samples";
  private static final String CLEAR_FAILED_SAMPLES = "clear-failed-samples";

  private static final String SAMPLE = "sample";
  private static final String ALL_SAMPLES = "all-samples";

  private static final String DENSITY_WINDOW = "density-window";
  private static final String REMOVE_OVERLAPPING = "remove-overlapping";

  // old filter flags
  private static final String MIN_POSTERIOR_SCORE = "Xmin-posterior-score";
  private static final String MAX_POSTERIOR_SCORE = "Xmax-posterior-score";

  private static final String EXPR_FLAG = "Xexpr";
  private static final String KEEP_EXPRESSION_FLAG = "keep-expr";
  private static final String JAVASCRIPT_FLAG = "javascript";


  private final VcfFilterTask mVcfFilterTask = new VcfFilterTask();
  private static final String[] SAMPLE_FLAGS = new String[]{
    REMOVE_HOM,
    REMOVE_SAME_AS_REF,
    SNPS_ONLY, NON_SNPS_ONLY,
    MIN_DEPTH, MAX_DEPTH,
    MIN_GENOTYPE_QUALITY, MAX_GENOTYPE_QUALITY,
    MIN_POSTERIOR_SCORE, MAX_POSTERIOR_SCORE,
    MAX_AMBIGUITY_RATIO,
    MIN_AVR_SCORE, MAX_AVR_SCORE,
    MIN_DENOVO_SCORE, MAX_DENOVO_SCORE,
  };

  @Override
  public String moduleName() {
    return "vcffilter";
  }

  @Override
  public String description() {
    return "filter records within a VCF file";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Filters VCF records based on various criteria. When filtering on multiple samples, if any of the specified samples fail the criteria, the record will be filtered. By default filtered records are removed, but see the --" + FAIL_FLAG + ", --" + CLEAR_FAILED_SAMPLES + ", and --" + FAIL_SAMPLES_FLAG + " options for alternatives.");
    CommonFlagCategories.setCategories(mFlags);

    mFlags.registerRequired('i', INPUT_FLAG, File.class, FILE, "VCF file containing variants to be filtered. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    final Flag<File> output = mFlags.registerOptional('o', OUTPUT_FLAG, File.class, FILE, "output VCF file. Use '-' to write to standard output. This option is required, unless \"--javascript\" is being used").setCategory(INPUT_OUTPUT);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    CommonFlags.initForce(mFlags);

    mFlags.registerOptional(RESTRICTION_FLAG, String.class, STRING, "if set, only read VCF records within the specified range. The format is one of <sequence_name>, <sequence_name>:start-end or <sequence_name>:start+length").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(BED_REGIONS_FLAG, File.class, FILE, "if set, only read VCF records that overlap the ranges contained in the specified BED file").setCategory(INPUT_OUTPUT);

    VcfMerge.initAddHeaderFlag(mFlags);

    // What to apply to, what to do with the results of filtering
    mFlags.registerOptional(CLEAR_FAILED_SAMPLES, "retain failed records, set the sample GT field to missing").setCategory(REPORTING);
    mFlags.registerOptional('f', FAIL_FLAG, String.class, STRING, "retain failed records, add the provided label to the FILTER field").setCategory(REPORTING);
    mFlags.registerOptional('F', FAIL_SAMPLES_FLAG, String.class, STRING, "retain failed records, add the provided label to the sample FT field").setCategory(REPORTING);

    // Variant position
    mFlags.registerOptional(INCLUDE_BED, File.class, FILE, "only keep variants within the regions in this BED file").setCategory(FILTERING);
    mFlags.registerOptional(EXCLUDE_BED, File.class, FILE, "discard all variants within the regions in this BED file").setCategory(FILTERING);
    mFlags.registerOptional(INCLUDE_VCF, File.class, FILE, "only keep variants that overlap with the ones in this file").setCategory(FILTERING);
    mFlags.registerOptional(EXCLUDE_VCF, File.class, FILE, "discard all variants that overlap with the ones in this file").setCategory(FILTERING);
    mFlags.registerOptional('w', DENSITY_WINDOW, Integer.class, INT, "window within which multiple variants are discarded").setCategory(FILTERING);
    mFlags.registerOptional(REMOVE_OVERLAPPING, "remove records that overlap with previous records").setCategory(FILTERING);

    // REF/ALT contents
    mFlags.registerOptional(MIN_ALLELES, Integer.class, INT, "minimum number of alleles (REF included)").setCategory(FILTERING);
    mFlags.registerOptional(MAX_ALLELES, Integer.class, INT, "maximum number of alleles (REF included)").setCategory(FILTERING);

    // Contents of FILTER
    mFlags.registerOptional('r', REMOVE_FILTER, String.class, STRING, "remove variants with this FILTER tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional('k', KEEP_FILTER, String.class, STRING, "only keep variants with this FILTER tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();

    // Contents of INFO
    mFlags.registerOptional('R', REMOVE_INFO, String.class, STRING, "remove variants with this INFO tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional('K', KEEP_INFO, String.class, STRING, "only keep variants with this INFO tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();

    // Which FORMAT columns do we look at
    mFlags.registerOptional(SAMPLE, String.class, STRING, "apply sample-specific criteria to the named sample contained in the input VCF").setCategory(INPUT_OUTPUT).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(ALL_SAMPLES, "apply sample-specific criteria to all samples contained in the input VCF").setCategory(INPUT_OUTPUT);

    // FORMAT/GT
    mFlags.registerOptional(SNPS_ONLY, "only keep where sample variant is a simple SNP").setCategory(FILTERING);
    mFlags.registerOptional(NON_SNPS_ONLY, "only keep where sample variant is MNP or INDEL").setCategory(FILTERING);
    mFlags.registerOptional(REMOVE_HOM, "remove where sample is homozygous").setCategory(FILTERING);
    mFlags.registerOptional(REMOVE_SAME_AS_REF, "remove where sample is same as reference").setCategory(FILTERING);
    mFlags.registerOptional(REMOVE_ALL_SAME_AS_REF, "remove where all samples are same as reference").setCategory(FILTERING);

    // Other INFO fields
    mFlags.registerOptional('c', MIN_COMBINED_DEPTH, Integer.class, INT, "minimum allowed combined read depth").setCategory(FILTERING);
    mFlags.registerOptional('C', MAX_COMBINED_DEPTH, Integer.class, INT, "maximum allowed combined read depth").setCategory(FILTERING);

    // Other FORMAT fields
    mFlags.registerOptional('d', MIN_DEPTH, Integer.class, INT, "minimum allowed sample read depth").setCategory(FILTERING);
    mFlags.registerOptional('D', MAX_DEPTH, Integer.class, INT, "maximum allowed sample read depth").setCategory(FILTERING);
    mFlags.registerOptional('g', MIN_GENOTYPE_QUALITY, Double.class, FLOAT, "minimum allowed genotype quality").setCategory(FILTERING);
    mFlags.registerOptional('G', MAX_GENOTYPE_QUALITY, Double.class, FLOAT, "maximum allowed genotype quality").setCategory(FILTERING);
    mFlags.registerOptional('q', MIN_QUALITY, Double.class, FLOAT, "minimum allowed quality").setCategory(FILTERING);
    mFlags.registerOptional('Q', MAX_QUALITY, Double.class, FLOAT, "maximum allowed quality").setCategory(FILTERING);
    mFlags.registerOptional('A', MAX_AMBIGUITY_RATIO, Double.class, FLOAT, "maximum allowed ambiguity ratio").setCategory(FILTERING);
    mFlags.registerOptional(MIN_AVR_SCORE, Double.class, FLOAT, "minimum allowed AVR score").setCategory(FILTERING);
    mFlags.registerOptional(MAX_AVR_SCORE, Double.class, FLOAT, "maximum allowed AVR score").setCategory(FILTERING);
    mFlags.registerOptional(MIN_DENOVO_SCORE, Double.class, FLOAT, "minimum de novo score threshold").setCategory(FILTERING);
    mFlags.registerOptional(MAX_DENOVO_SCORE, Double.class, FLOAT, "maximum de novo score threshold").setCategory(FILTERING);

    // Xflags
    mFlags.registerOptional('p', MIN_POSTERIOR_SCORE, Double.class, FLOAT, "minimum allowed posterior score").setCategory(FILTERING);
    mFlags.registerOptional('P', MAX_POSTERIOR_SCORE, Double.class, FLOAT, "maximum allowed posterior score").setCategory(FILTERING);
    final Flag<String> expr = mFlags.registerOptional(EXPR_FLAG, String.class, STRING, "keep variants where this sample expression is true (e.g. GQ>0.5)").setCategory(FILTERING);
    expr.setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional('e', KEEP_EXPRESSION_FLAG, String.class, STRING, "records for which this expression evaluates to true will be retained").setCategory(FILTERING);
    final Flag<String> javascript = mFlags.registerOptional('j', JAVASCRIPT_FLAG, String.class, STRING, "javascript filtering functions for determining whether to keep record. May be either an expression or a file name");
    javascript.setCategory(FILTERING);
    javascript.setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(NO_HEADER, "prevent VCF header from being written").setCategory(UTILITY);
    mFlags.addRequiredSet(output);
    mFlags.addRequiredSet(javascript);
    mFlags.setValidator(new VcfFilterValidator());
  }

  @TestClass("com.rtg.vcf.VcfFilterCliValidatorTest")
  private static class VcfFilterValidator implements Validator {
    @Override
    public boolean isValid(final CFlags flags) {
      if (flags.isSet(OUTPUT_FLAG) && !CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_FLAG)))) {
        return false;
      }
      if (!CommonFlags.validateInputFile(flags, INPUT_FLAG)
        || !CommonFlags.validateInputFile(flags, EXCLUDE_BED, EXCLUDE_VCF, INCLUDE_BED, INCLUDE_VCF)
        || !flags.checkOr(OUTPUT_FLAG, JAVASCRIPT_FLAG)
        || !flags.checkMinMaxInRange(MIN_GENOTYPE_QUALITY, MAX_GENOTYPE_QUALITY, 0.0, Double.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_POSTERIOR_SCORE, MAX_POSTERIOR_SCORE, 0.0, Double.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_QUALITY, MAX_QUALITY, 0.0, Double.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_AVR_SCORE, MAX_AVR_SCORE, 0.0, Double.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_DENOVO_SCORE, MAX_DENOVO_SCORE, 0.0, Double.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_DEPTH, MAX_DEPTH, 0, Integer.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_COMBINED_DEPTH, MAX_COMBINED_DEPTH, 0, Integer.MAX_VALUE)
        || !flags.checkMinMaxInRange(MIN_ALLELES, MAX_ALLELES, 1, Integer.MAX_VALUE)
        || !flags.checkInRange(MAX_AMBIGUITY_RATIO, 0.0, 1.0)
        || !flags.checkInRange(DENSITY_WINDOW, 1, Integer.MAX_VALUE)
        || !CommonFlags.validateRegions(flags)
        || !flags.checkNand(EXCLUDE_BED, EXCLUDE_VCF)
        || !flags.checkNand(INCLUDE_BED, INCLUDE_VCF)
        || !flags.checkNand(SNPS_ONLY, NON_SNPS_ONLY)
        || !flags.checkNand(SAMPLE, ALL_SAMPLES)
        || !flags.checkNand(FAIL_FLAG, CLEAR_FAILED_SAMPLES)) {
        return false;
      }
      if ((flags.isSet(MIN_GENOTYPE_QUALITY) || flags.isSet(MAX_GENOTYPE_QUALITY)) && (flags.isSet(MIN_POSTERIOR_SCORE) || flags.isSet(MAX_POSTERIOR_SCORE))) {
        //Only possible if someone is monkeying with X-flags
        flags.setParseMessage("Use genotype-quality or posterior filters, not both.");
        return false;
      }
      if (flags.isSet(MIN_DENOVO_SCORE) || flags.isSet(MAX_DENOVO_SCORE)) {
        if (flags.isSet(ALL_SAMPLES) || flags.getValues(SAMPLE).size() != 1) {
          flags.setParseMessage("De Novo filtering requires a single sample to be specified");
          return false;
        }
      }
      return true;
    }
  }

  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    try {
      process(out);
      return 0;
    } finally {
      out.flush();
    }
  }

  private void process(final OutputStream output) throws IOException {
    if (mFlags.isSet(SAMPLE)) {
      mVcfFilterTask.mSampleNames.addAll(mFlags.getValues(SAMPLE).stream().map(o -> (String) o).collect(Collectors.toList()));
    }

    mVcfFilterTask.mResetFailedSampleGts = mFlags.isSet(CLEAR_FAILED_SAMPLES);
    mVcfFilterTask.mFailFilterName = mFlags.isSet(FAIL_FLAG) ? (String) mFlags.getValue(FAIL_FLAG) : null;
    mVcfFilterTask.mFailSampleFilterName = mFlags.isSet(FAIL_SAMPLES_FLAG) ? (String) mFlags.getValue(FAIL_SAMPLES_FLAG) : null;

    // These involve checking the specified sample field
    mVcfFilterTask.mRemoveHom = mFlags.isSet(REMOVE_HOM);
    mVcfFilterTask.mRemoveSameAsRef = mFlags.isSet(REMOVE_SAME_AS_REF);
    mVcfFilterTask.mSnpsOnly = mFlags.isSet(SNPS_ONLY);
    mVcfFilterTask.mNonSnpsOnly = mFlags.isSet(NON_SNPS_ONLY);

    if (mFlags.isSet(MIN_ALLELES) || mFlags.isSet(MAX_ALLELES)) {
      final int minAlleles = mFlags.isSet(MIN_ALLELES) ? (Integer) mFlags.getValue(MIN_ALLELES) : 1;
      final int maxAlleles = mFlags.isSet(MAX_ALLELES) ? (Integer) mFlags.getValue(MAX_ALLELES) : Integer.MAX_VALUE;
      mVcfFilterTask.mFilters.add(new AbstractVcfFilter(mVcfFilterTask.mVcfFilterStatistics, Stat.ALLELE_COUNT) {
        @Override
        boolean acceptCondition(VcfRecord record) {
          final int numAlleles = record.getAltCalls().size() + 1;
          return numAlleles >= minAlleles && numAlleles <= maxAlleles;
        }
      });
    }
    if (mFlags.isSet(MIN_QUALITY) || mFlags.isSet(MAX_QUALITY)) {
      final double minQuality = mFlags.isSet(MIN_QUALITY) ? (Double) mFlags.getValue(MIN_QUALITY) : 0.0;
      final double maxQuality = mFlags.isSet(MAX_QUALITY) ? (Double) mFlags.getValue(MAX_QUALITY) : Double.MAX_VALUE;
      mVcfFilterTask.mFilters.add(new AbstractVcfFilter.QualFilter(mVcfFilterTask.mVcfFilterStatistics, minQuality, maxQuality));
    }
    if (mFlags.isSet(MIN_GENOTYPE_QUALITY) || mFlags.isSet(MAX_GENOTYPE_QUALITY)
        || mFlags.isSet(MIN_POSTERIOR_SCORE) || mFlags.isSet(MAX_POSTERIOR_SCORE)) {
      final double minGq = mFlags.isSet(MIN_GENOTYPE_QUALITY) ? (Double) mFlags.getValue(MIN_GENOTYPE_QUALITY) : mFlags.isSet(MIN_POSTERIOR_SCORE) ? (Double) mFlags.getValue(MIN_POSTERIOR_SCORE) : 0.0;
      final double maxGq = mFlags.isSet(MAX_GENOTYPE_QUALITY) ? (Double) mFlags.getValue(MAX_GENOTYPE_QUALITY) : mFlags.isSet(MAX_POSTERIOR_SCORE) ? (Double) mFlags.getValue(MAX_POSTERIOR_SCORE) : Double.MAX_VALUE;
      final boolean posteriorFiltering = mFlags.isSet(MIN_POSTERIOR_SCORE) || mFlags.isSet(MAX_POSTERIOR_SCORE);
      mVcfFilterTask.mVcfFilterStatistics.setPosteriorFiltering(posteriorFiltering);
      mVcfFilterTask.mFilters.add(new VcfSampleFilter.GqFilter(mVcfFilterTask.mVcfFilterStatistics, minGq, maxGq, posteriorFiltering));
    }
    if (mFlags.isSet(MIN_DEPTH) || mFlags.isSet(MAX_DEPTH)) {
      final int minReadDepth = mFlags.isSet(MIN_DEPTH) ? (Integer) mFlags.getValue(MIN_DEPTH) : 0;
      final int maxReadDepth = mFlags.isSet(MAX_DEPTH) ? (Integer) mFlags.getValue(MAX_DEPTH) : Integer.MAX_VALUE;
      mVcfFilterTask.mFilters.add(new VcfSampleFilter.MinMaxIntFilter(mVcfFilterTask.mVcfFilterStatistics, Stat.READ_DEPTH_FILTERED_COUNT, minReadDepth, maxReadDepth, VcfUtils.FORMAT_SAMPLE_DEPTH));
    }
    if (mFlags.isSet(MIN_COMBINED_DEPTH) || mFlags.isSet(MAX_COMBINED_DEPTH)) {
      final int minCombinedReadDepth = mFlags.isSet(MIN_COMBINED_DEPTH) ? (Integer) mFlags.getValue(MIN_COMBINED_DEPTH) : 0;
      final int maxCombinedReadDepth = mFlags.isSet(MAX_COMBINED_DEPTH) ? (Integer) mFlags.getValue(MAX_COMBINED_DEPTH) : Integer.MAX_VALUE;
      mVcfFilterTask.mFilters.add(new VcfInfoFilter.MinMaxIntFilter(mVcfFilterTask.mVcfFilterStatistics, Stat.COMBINED_READ_DEPTH_FILTERED_COUNT, minCombinedReadDepth, maxCombinedReadDepth, VcfUtils.INFO_COMBINED_DEPTH));
    }
    if (mFlags.isSet(MIN_AVR_SCORE) || mFlags.isSet(MAX_AVR_SCORE)) {
      final double minAvrScore = mFlags.isSet(MIN_AVR_SCORE) ? (Double) mFlags.getValue(MIN_AVR_SCORE) : Double.NEGATIVE_INFINITY;
      final double maxAvrScore = mFlags.isSet(MAX_AVR_SCORE) ? (Double) mFlags.getValue(MAX_AVR_SCORE) : Double.POSITIVE_INFINITY;
      mVcfFilterTask.mFilters.add(new VcfSampleFilter.MinMaxDoubleFilter(mVcfFilterTask.mVcfFilterStatistics, Stat.AVR_SCORE_FILTERED_COUNT, minAvrScore, maxAvrScore, VcfUtils.FORMAT_AVR));
    }
    if (mFlags.isSet(MAX_AMBIGUITY_RATIO)) {
      final double maxAmbiguityRatio = mFlags.isSet(MAX_AMBIGUITY_RATIO) ? (Double) mFlags.getValue(MAX_AMBIGUITY_RATIO) : Double.POSITIVE_INFINITY;
      mVcfFilterTask.mFilters.add(new VcfSampleFilter.MinMaxDoubleFilter(mVcfFilterTask.mVcfFilterStatistics, Stat.AMBIGOUS_FILTERED_COUNT, Double.NEGATIVE_INFINITY, maxAmbiguityRatio, VcfUtils.FORMAT_AMBIGUITY_RATIO));
    }
    if (mFlags.isSet(MAX_DENOVO_SCORE) || mFlags.isSet(MIN_DENOVO_SCORE)) {
      mVcfFilterTask.mFilters.add(new VcfSampleFilter.DenovoFilter(mVcfFilterTask.mVcfFilterStatistics,
          mFlags.isSet(MIN_DENOVO_SCORE) ? (Double) mFlags.getValue(MIN_DENOVO_SCORE) : 0,
          mFlags.isSet(MAX_DENOVO_SCORE) ? (Double) mFlags.getValue(MAX_DENOVO_SCORE) : Double.MAX_VALUE));
    }

    mVcfFilterTask.mCheckingSample = false;
    for (final String flag : SAMPLE_FLAGS) {
      mVcfFilterTask.mCheckingSample |= mFlags.isSet(flag);
    }
    mFlags.getValues(EXPR_FLAG).stream().filter(expr -> !((String) expr).startsWith("INFO.")).forEach(expr -> mVcfFilterTask.mCheckingSample = true);
    mVcfFilterTask.mRemoveOverlapping = mFlags.isSet(REMOVE_OVERLAPPING);
    mVcfFilterTask.mRemoveAllSameAsRef = mFlags.isSet(REMOVE_ALL_SAME_AS_REF);
    mVcfFilterTask.mDensityWindow = mFlags.isSet(DENSITY_WINDOW) ? (Integer) mFlags.getValue(DENSITY_WINDOW) : null;
    if (mFlags.isSet(KEEP_FILTER)) {
      mVcfFilterTask.mKeepFilters.addAll(mFlags.getValues(KEEP_FILTER).stream().map(tag -> (String) tag).collect(Collectors.toList()));
    }
    if (mFlags.isSet(REMOVE_FILTER)) {
      mVcfFilterTask.mRemoveFilters.addAll(mFlags.getValues(REMOVE_FILTER).stream().map(tag -> (String) tag).collect(Collectors.toList()));
    }
    if (mFlags.isSet(KEEP_INFO)) {
      mVcfFilterTask.mKeepInfos.addAll(mFlags.getValues(KEEP_INFO).stream().map(tag -> (String) tag).collect(Collectors.toList()));
    }
    if (mFlags.isSet(REMOVE_INFO)) {
      mVcfFilterTask.mRemoveInfos.addAll(mFlags.getValues(REMOVE_INFO).stream().map(tag -> (String) tag).collect(Collectors.toList()));
    }
    if (mFlags.isSet(INCLUDE_BED)) {
      mVcfFilterTask.mIncludeBed = BedUtils.regions((File) mFlags.getValue(INCLUDE_BED));
    } else if (mFlags.isSet(INCLUDE_VCF)) {
      mVcfFilterTask.mIncludeBed = VcfUtils.regionsVcf((File) mFlags.getValue(INCLUDE_VCF));
    }
    if (mFlags.isSet(EXCLUDE_BED)) {
      mVcfFilterTask.mExcludeBed = BedUtils.regions((File) mFlags.getValue(EXCLUDE_BED));
    } else if (mFlags.isSet(EXCLUDE_VCF)) {
      mVcfFilterTask.mExcludeBed = VcfUtils.regionsVcf((File) mFlags.getValue(EXCLUDE_VCF));
    }
    final RegionRestriction region;
    if (mFlags.isSet(RESTRICTION_FLAG)) {
      region = new RegionRestriction((String) mFlags.getValue(RESTRICTION_FLAG));
    } else {
      region = null;
    }
    final ReferenceRanges<String> ranges;
    if (mFlags.isSet(BED_REGIONS_FLAG)) {
      Diagnostic.developerLog("Loading BED regions");
      ranges = SamRangeUtils.createBedReferenceRanges((File) mFlags.getValue(BED_REGIONS_FLAG));
    } else {
      ranges = null;
    }
    mVcfFilterTask.mAllSamples = mFlags.isSet(ALL_SAMPLES);
    for (final Object expr : mFlags.getValues(EXPR_FLAG)) {
      final String e = (String) expr;
      if (e.startsWith("INFO.")) {
        mVcfFilterTask.mFilters.add(new ExpressionInfoFilter(mVcfFilterTask.mVcfFilterStatistics, e.substring("INFO.".length())));
      } else {
        mVcfFilterTask.mFilters.add(new ExpressionSampleFilter(mVcfFilterTask.mVcfFilterStatistics, e));
      }
    }

    final Optional<ScriptedVcfFilter> scriptFilter = buildScriptFilter(mFlags, output);
    scriptFilter.ifPresent(mVcfFilterTask.mFilters::add);

    final Collection<String> extraHeaderLines = VcfMerge.getHeaderLines(mFlags);
    final File in = (File) mFlags.getValue(INPUT_FLAG);
    final File out = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean stdout = out != null && FileUtils.isStdio(out);
    Diagnostic.developerLog("Starting filter");
    try (VcfReader r = ranges != null ? VcfReader.openVcfReader(in, ranges) : VcfReader.openVcfReader(in, region)) {
      final VcfHeader header = r.getHeader();
      VcfUtils.addHeaderLines(header, extraHeaderLines);
      mVcfFilterTask.setHeader(header);
      try (VcfWriter w = getVcfWriter(header, output, out)) {
        mVcfFilterTask.process(r, w);
      }
      if (!stdout && out != null) {
        mVcfFilterTask.printStatistics(output);
      }
      scriptFilter.ifPresent(ScriptedVcfFilter::end);
    }
  }

  private VcfWriter getVcfWriter(VcfHeader header, OutputStream output, File out) throws IOException {
    if (out == null) {
      return new NullVcfWriter(header);
    }
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final File outputFile = FileUtils.isStdio(out) ? null : VcfUtils.getZippedVcfFileName(gzip, out);
    return new VcfWriterFactory(mFlags).make(header, outputFile, output);
  }

  private Optional<ScriptedVcfFilter> buildScriptFilter(CFlags flags, OutputStream output) throws IOException {
    final String expressionString = fileOrString(flags, KEEP_EXPRESSION_FLAG);
    final List<?> values = flags.getValues(JAVASCRIPT_FLAG);
    final List<String> beginnings = new ArrayList<>(values.size());
    for (Object begin : values) {
      beginnings.add(fileIfExists((String) begin));
    }
    if (expressionString != null || beginnings.size() > 0) {
      final ScriptedVcfFilter filter = new ScriptedVcfFilter(expressionString, beginnings, output);
      return Optional.of(filter);
    }
    return Optional.empty();
  }

  private String fileOrString(CFlags flags, String flag) throws IOException {
    if (!flags.isSet(flag)) {
      return null;
    }
    final String flagValue = (String) flags.getValue(flag);
    return fileIfExists(flagValue);
  }

  private String fileIfExists(String flagValue) throws IOException {
    final File file = new File(flagValue);
    return file.exists() ? FileUtils.fileToString(file) : flagValue;
  }

}
