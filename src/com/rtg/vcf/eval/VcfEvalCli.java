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
package com.rtg.vcf.eval;

import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.NoStatistics;
import com.rtg.launcher.OutputParams;
import com.rtg.launcher.ParamsCli;
import com.rtg.util.IORunnable;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.annotation.DerivedAnnotations;
import com.rtg.vcf.eval.VcfEvalParams.VcfEvalParamsBuilder;

/**
 * Compare detected SNPs with the generated SNPs
 *
 */
@TestClass(value = {"com.rtg.vcf.eval.VcfEvalCliTest", "com.rtg.vcf.eval.VcfEvalNanoTest"})
public class VcfEvalCli extends ParamsCli<VcfEvalParams> {

  /** Flag used for supplying truth variants */
  public static final String BASELINE = "baseline";
  /** Flag used for supplying query variants */
  public static final String CALLS = "calls";
  /** Flag used to specify whether to load non-pass variants */
  public static final String ALL_RECORDS = "all-records";
  /** Flag used for supplying evaluation regions */
  public static final String EVAL_REGIONS_FLAG = "evaluation-regions";
  /** Flag used for setting the score field */
  public static final String SCORE_FIELD = "vcf-score-field";
  /** Flag used to specify a "good" score is high vs low */
  public static final String SORT_ORDER = "sort-order";

  private static final String SAMPLE = "sample";
  private static final String SQUASH_PLOIDY = "squash-ploidy";
  private static final String REF_OVERLAP = "ref-overlap";
  private static final String OUTPUT_MODE = "output-mode";

  private static final String ROC_SUBSET = "Xroc-subset";
  private static final String SLOPE_FILES = "Xslope-files";
  private static final String MAX_LENGTH = "Xmax-length";
  private static final String RTG_STATS = "Xrtg-stats";
  private static final String TWO_PASS = "Xtwo-pass";
  private static final String OBEY_PHASE = "Xobey-phase";
  private static final String DECOMPOSE = "Xdecompose";
  private static final String LOOSE_MATCH_DISTANCE = "Xloose-match-distance";

  /** Defines the RocFilters that make sense to use with vcfeval */
  public enum VcfEvalRocFilter {
    // Generic filters that should apply to any call set
    /** All variants (required) */
    ALL(RocFilter.ALL),
    /** Homozygous only */
    HOM(RocFilter.HOM),
    /** Heterozygous only */
    HET(RocFilter.HET),
    /** SNPs only */
    SNP(RocFilter.SNP),
    /** Anything not a SNP */
    NON_SNP(RocFilter.NON_SNP),
    /** MNPs only */
    MNP(RocFilter.MNP),
    /** Length-changing only */
    INDEL(RocFilter.INDEL),

    // RTG specific annotations
    /** complex called */
    XRX(RocFilter.XRX),
    /** non-complex called */
    NON_XRX(RocFilter.NON_XRX),
    /** Homozygous complex called */
    HOM_XRX(RocFilter.HOM_XRX),
    /** Homozygous non-complex called */
    HOM_NON_XRX(RocFilter.HOM_NON_XRX),
    /** Heterozygous complex called */
    HET_XRX(RocFilter.HET_XRX),
    /** Heterozygous non-complex called */
    HET_NON_XRX(RocFilter.HET_NON_XRX);

    RocFilter mFilter;
    VcfEvalRocFilter(RocFilter f) {
      mFilter = f;
    }
    RocFilter filter() {
      return mFilter;
    }
  }

  @Override
  public String moduleName() {
    return "vcfeval";
  }

  @Override
  public String description() {
    return "evaluate called variants for agreement with a baseline variant set";
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription("Evaluates called variants for genotype agreement with a baseline variant set irrespective of representational differences. Outputs a weighted ROC file which can be viewed with rtg rocplot and VCF files containing false positives (called variants not matched in the baseline), false negatives (baseline variants not matched in the call set), and true positives (variants that match between the baseline and calls).");
    CommonFlags.initOutputDirFlag(mFlags);
    mFlags.registerRequired('b', BASELINE, File.class, CommonFlags.FILE, "VCF file containing baseline variants").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('c', CALLS, File.class, CommonFlags.FILE, "VCF file containing called variants").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('t', CommonFlags.TEMPLATE_FLAG, File.class, CommonFlags.SDF, "SDF of the reference genome the variants are called against").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional(CommonFlags.RESTRICTION_FLAG, String.class, CommonFlags.STRING, "if set, only read VCF records within the specified range. The format is one of <sequence_name>, <sequence_name>:start-end or <sequence_name>:start+length").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(CommonFlags.BED_REGIONS_FLAG, File.class, "File", "if set, only read VCF records that overlap the ranges contained in the specified BED file").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional('e', EVAL_REGIONS_FLAG, File.class, "File", "if set, evaluate within regions contained in the supplied BED file, allowing transborder matches. To be used for truth-set high-confidence regions or other regions of interest where region boundary effects should be minimized").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional(SAMPLE, String.class, CommonFlags.STRING, "the name of the sample to select. Use <baseline_sample>,<calls_sample> to select different sample names for baseline and calls. (Required when using multi-sample VCF files)").setCategory(FILTERING);
    mFlags.registerOptional(ALL_RECORDS, "use all records regardless of FILTER status (Default is to only process records where FILTER is \".\" or \"PASS\")").setCategory(FILTERING);
    mFlags.registerOptional(SQUASH_PLOIDY, "treat heterozygous genotypes as homozygous ALT in both baseline and calls, to allow matches that ignore zygosity differences").setCategory(FILTERING);
    mFlags.registerOptional(REF_OVERLAP, "allow alleles to overlap where bases of either allele are same-as-ref (Default is to only allow VCF anchor base overlap)").setCategory(FILTERING);

    mFlags.registerOptional('f', SCORE_FIELD, String.class, CommonFlags.STRING, "the name of the VCF FORMAT field to use as the ROC score. Also valid are \"QUAL\" or \"INFO.<name>\" to select the named VCF INFO field", VcfUtils.FORMAT_GENOTYPE_QUALITY).setCategory(REPORTING);
    mFlags.registerOptional('O', SORT_ORDER, RocSortOrder.class, CommonFlags.STRING, "the order in which to sort the ROC scores so that \"good\" scores come before \"bad\" scores", RocSortOrder.DESCENDING).setCategory(REPORTING);
    final Flag<String> modeFlag = mFlags.registerOptional('m', OUTPUT_MODE, String.class, CommonFlags.STRING, "output reporting mode", VcfEvalTask.MODE_SPLIT).setCategory(REPORTING);
    modeFlag.setParameterRange(new String[]{VcfEvalTask.MODE_SPLIT, VcfEvalTask.MODE_ANNOTATE, VcfEvalTask.MODE_COMBINE, VcfEvalTask.MODE_GA4GH, VcfEvalTask.MODE_ROC_ONLY});
    mFlags.registerOptional('R', ROC_SUBSET, VcfEvalRocFilter.class, "FILTER", "output ROC files corresponding to call subsets").setMaxCount(Integer.MAX_VALUE).enableCsv().setCategory(REPORTING);

    mFlags.registerOptional(MAX_LENGTH, Integer.class, CommonFlags.INT, "don't attempt to evaluate variant alternatives longer than this", 1000).setCategory(FILTERING);
    mFlags.registerOptional(TWO_PASS, Boolean.class, "BOOL", "run diploid matching followed by squash-ploidy matching on FP/FN to find common alleles (Default is automatically set by output mode)").setCategory(FILTERING);
    mFlags.registerOptional(RTG_STATS, "output RTG specific files and statistics").setCategory(REPORTING);
    mFlags.registerOptional(SLOPE_FILES, "output files for ROC slope analysis").setCategory(REPORTING);
    mFlags.registerOptional(OBEY_PHASE, String.class, CommonFlags.STRING, "if set, obey global phasing if present in the input VCFs. Use <baseline_phase>,<calls_phase> to select independently for baseline and calls. (Values must be one of [true, false, and invert])", "false").setCategory(FILTERING);
    mFlags.registerOptional(LOOSE_MATCH_DISTANCE, Integer.class, CommonFlags.INT, "if set, GA4GH mode will also apply distance-based loose-matching with the specified distance", 30).setCategory(FILTERING);
    mFlags.registerOptional(DECOMPOSE, Boolean.class, "BOOL", "decompose complex variants into smaller consitituents to allow partial credit", false).setCategory(FILTERING);

    CommonFlags.initThreadsFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);

    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateTabixedInputFile(flags, BASELINE, CALLS)
      && CommonFlags.validateThreads(flags)
      && CommonFlags.validateTemplate(flags)
      && CommonFlags.validateRegions(flags)
      && flags.checkNand(SQUASH_PLOIDY, TWO_PASS)
      && validateScoreField(flags)
      && validatePairedFlag(flags, SAMPLE, "sample name")
      && validatePairedFlag(flags, OBEY_PHASE, "phase type")
      && validateModeSample(flags)
    );
  }

  private static boolean validateModeSample(CFlags flags) {
    final String mode = (String) flags.getValue(OUTPUT_MODE);
    if (flags.isSet(SAMPLE) && ((String) flags.getValue(SAMPLE)).contains(VariantFactory.ALT_SAMPLE) && (VcfEvalTask.MODE_COMBINE.equals(mode) || VcfEvalTask.MODE_GA4GH.equals(mode))) {
      flags.setParseMessage("--" + OUTPUT_MODE + "=combine cannot be used when either sample is " + VariantFactory.ALT_SAMPLE);
      return false;
    }
    return true;
  }

  /**
   * Validates that the VCF score field is well formed
   * @param flags the populated flags
   * @return true if the score field is well formed
   */
  public static boolean validateScoreField(CFlags flags) {
    if (flags.isSet(SCORE_FIELD)) {
      final String field = (String) flags.getValue(SCORE_FIELD);
      final int pIndex = field.indexOf('=');
      if (pIndex != -1) {
        final String fieldTypeName = field.substring(0, pIndex).toUpperCase(Locale.getDefault());
        try {
          final RocScoreField f = RocScoreField.valueOf(fieldTypeName);
          if (f == RocScoreField.DERIVED) {
            try {
              final DerivedAnnotations ann = DerivedAnnotations.valueOf(field.substring(pIndex + 1).toUpperCase(Locale.getDefault()));
              if (!DerivedAnnotations.singleValueNumericAnnotations().contains(ann)) {
                throw new IllegalArgumentException("Non single value numeric annotation");
              }
            } catch (IllegalArgumentException e) {
              flags.setParseMessage("Unrecognized derived annotation \"" + field + "\", must be one of " + Arrays.toString(DerivedAnnotations.singleValueNumericAnnotations().toArray()));
              return false;
            }
          }
        } catch (IllegalArgumentException e) {
          flags.setParseMessage("Unrecognized field type \"" + fieldTypeName + "\", must be one of " + Arrays.toString(RocScoreField.values()));
          return false;
        }
      }
    }
    return true;
  }

  private static boolean validatePairedFlag(CFlags flags, String flag, String label) {
    if (flags.isSet(flag)) {
      final String flagValue = (String) flags.getValue(flag);
      if (flagValue.length() == 0) {
        flags.setParseMessage("Supplied " + label + " cannot be empty");
        return false;
      }
      final String[] split = StringUtils.split(flagValue, ',');
      if (split.length > 2) {
        flags.setParseMessage("Invalid " + label + " specification " + flagValue + ". At most 1 comma is permitted");
        return false;
      }
      if (split[0].length() == 0) {
        flags.setParseMessage("Invalid " + label + " specification " + flagValue + ". Supplied baseline " + label + " cannot be empty");
        return false;
      }
      final String callsValue = split.length == 2 ? split[1] : split[0];
      if (callsValue.length() == 0) {
        flags.setParseMessage("Invalid sample name specification " + flagValue + ". Supplied calls " + label + " cannot be empty");
        return false;
      }
    }
    return true;
  }


  @Override
  protected IORunnable task(VcfEvalParams params, OutputStream out) {
    return new VcfEvalTask(params, out, new NoStatistics());
  }

  private static String[] splitPairedSpec(String flagValue) {
    final String[] split = StringUtils.split(flagValue, ',');
    if (split.length == 2) {
      return split;
    } else {
      return new String[] {split[0], split[0]};
    }
  }

  // Map from name to the subset of Orientors that are relevant to phased GT comparisons
  private static Orientor phaseTypeToOrientor(String name) {
    switch (name) {
      case "true":
        return Orientor.PHASED;
      case "invert":
        return Orientor.PHASE_INVERTED;
      case "false":
        return Orientor.UNPHASED;
      default:
        throw new NoTalkbackSlimException("Unrecognized phase type:" + name);
    }
  }

  @Override
  protected VcfEvalParams makeParams() throws IOException {
    final VcfEvalParamsBuilder builder = VcfEvalParams.builder();
    builder.name(mFlags.getName());
    builder.outputParams(new OutputParams(outputDirectory(), false, !mFlags.isSet(CommonFlags.NO_GZIP)));
    builder.templateFile((File) mFlags.getValue(CommonFlags.TEMPLATE_FLAG));
    final File baseline = (File) mFlags.getValue(BASELINE);
    final File calls = (File) mFlags.getValue(CALLS);
    builder.baseLineFile(baseline).callsFile(calls);
    builder.sortOrder((RocSortOrder) mFlags.getValue(SORT_ORDER));
    builder.scoreField((String) mFlags.getValue(SCORE_FIELD));
    builder.maxLength((Integer) mFlags.getValue(MAX_LENGTH));
    builder.looseMatchDistance((Integer) mFlags.getValue(LOOSE_MATCH_DISTANCE));
    if (mFlags.isSet(CommonFlags.RESTRICTION_FLAG)) {
      builder.restriction(new RegionRestriction((String) mFlags.getValue(CommonFlags.RESTRICTION_FLAG)));
    }
    if (mFlags.isSet(CommonFlags.BED_REGIONS_FLAG)) {
      builder.bedRegionsFile((File) mFlags.getValue(CommonFlags.BED_REGIONS_FLAG));
    }
    if (mFlags.isSet(EVAL_REGIONS_FLAG)) {
      builder.evalRegionsFile((File) mFlags.getValue(EVAL_REGIONS_FLAG));
    }
    if (mFlags.isSet(SAMPLE)) {
      final String[] samples = splitPairedSpec((String) mFlags.getValue(SAMPLE));
      builder.baselineSample(samples[0]);
      builder.callsSample(samples[1]);
    }
    if (mFlags.isSet(OBEY_PHASE)) {
      final String[] phaseTypes = splitPairedSpec((String) mFlags.getValue(OBEY_PHASE));
      builder.baselinePhaseOrientor(phaseTypeToOrientor(phaseTypes[0]));
      builder.callsPhaseOrientor(phaseTypeToOrientor(phaseTypes[1]));
    }
    final Set<RocFilter> rocFilters = new HashSet<>(Collections.singletonList(RocFilter.ALL));  // We require the ALL entry for aggregate stats
    if (!mFlags.isSet(ROC_SUBSET)) {
      rocFilters.addAll(Arrays.asList(RocFilter.SNP, RocFilter.NON_SNP));
    } else {
      final List<?> values = mFlags.getValues(ROC_SUBSET);
      for (Object o : values) {
        rocFilters.add(((VcfEvalRocFilter) o).filter());
      }
    }
    if (mFlags.isSet(RTG_STATS)) {
      rocFilters.add(RocFilter.NON_XRX);
      rocFilters.add(RocFilter.XRX);
      rocFilters.add(RocFilter.HET_NON_XRX);
      rocFilters.add(RocFilter.HET_XRX);
      rocFilters.add(RocFilter.HOM_NON_XRX);
      rocFilters.add(RocFilter.HOM_XRX);
    }
    builder.rocFilters(rocFilters);
    builder.useAllRecords(mFlags.isSet(ALL_RECORDS));
    builder.decompose((Boolean) mFlags.getValue(DECOMPOSE));
    builder.squashPloidy(mFlags.isSet(SQUASH_PLOIDY));
    builder.refOverlap(mFlags.isSet(REF_OVERLAP));
    builder.outputSlopeFiles(mFlags.isSet(SLOPE_FILES));
    builder.numberThreads(CommonFlags.parseThreads((Integer) mFlags.getValue(CommonFlags.THREADS_FLAG)));
    final String mode = ((String) mFlags.getValue(OUTPUT_MODE)).toLowerCase(Locale.ROOT);
    builder.outputMode(mode);
    if (mFlags.isSet(SQUASH_PLOIDY)) {
      builder.twoPass(false); // Makes no sense
    } else if (mFlags.isSet(TWO_PASS)) {
      builder.twoPass((Boolean) mFlags.getValue(TWO_PASS)); // Explicit override
    } else if (!VcfEvalTask.MODE_SPLIT.equals(mode)) {
      builder.twoPass(true); // Default to two passes except for split mode (backward compability)
    }
    return builder.create();
  }
}
