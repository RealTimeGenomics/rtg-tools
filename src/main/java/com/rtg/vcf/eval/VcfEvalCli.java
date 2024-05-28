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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  /** Flag used to indicate that ROCs are not required */
  public static final String NO_ROC = "no-roc";

  private static final String SAMPLE = "sample";
  private static final String SQUASH_PLOIDY = "squash-ploidy";
  private static final String REF_OVERLAP = "ref-overlap";
  private static final String OUTPUT_MODE = "output-mode";
  protected static final String CRITERIA_SENSITIVITY = "at-sensitivity";
  protected static final String CRITERIA_PRECISION = "at-precision";
  protected static final String CRITERIA_SCORE = "at-score";

  protected static final String ROC_SUBSET = "roc-subset";
  protected static final String ROC_EXPR = "roc-expr";
  protected static final String ROC_REGIONS = "roc-regions";
  protected static final String ROC_CROSS_JOIN = "roc-cross-join";
  private static final String SLOPE_FILES = "Xslope-files";
  private static final String MAX_LENGTH = "Xmax-length";
  private static final String TWO_PASS = "Xtwo-pass";
  private static final String OBEY_PHASE = "Xobey-phase";
  private static final String SAMPLE_PLOIDY = "sample-ploidy";
  private static final String DECOMPOSE = "decompose";
  private static final String LOOSE_MATCH_DISTANCE = "Xloose-match-distance";

  /** Defines the preset RocFilters that make sense to use with vcfeval */
  public enum VcfEvalRocFilter implements RocFilterProxy {
    // Generic filters that should apply to any call set
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
    INDEL(RocFilter.INDEL);

    RocFilter mFilter;
    VcfEvalRocFilter(RocFilter f) {
      mFilter = f;
    }
    @Override
    public RocFilter filter() {
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
    CommonFlags.initRegionOrBedRegionsFlags(mFlags);

    mFlags.registerOptional('e', EVAL_REGIONS_FLAG, File.class, "File", "if set, evaluate within regions contained in the supplied BED file, allowing transborder matches. To be used for truth-set high-confidence regions or other regions of interest where region boundary effects should be minimized").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional(SAMPLE, String.class, CommonFlags.STRING, "the name of the sample to select. Use <baseline_sample>,<calls_sample> to select different sample names for baseline and calls. (Required when using multi-sample VCF files)").setCategory(FILTERING);
    mFlags.registerOptional(ALL_RECORDS, "use all records regardless of filter status (Default is to only process variants passing filters)").setCategory(FILTERING);
    mFlags.registerOptional(SQUASH_PLOIDY, "treat heterozygous genotypes as homozygous ALT in both baseline and calls, to allow matches that ignore zygosity differences").setCategory(FILTERING);
    mFlags.registerOptional(REF_OVERLAP, "allow alleles to overlap where bases of either allele are same-as-ref (Default is to only allow VCF anchor base overlap)").setCategory(FILTERING);

    mFlags.registerOptional(NO_ROC, "do not produce ROCs").setCategory(REPORTING);
    final Flag<String> modeFlag = mFlags.registerOptional('m', OUTPUT_MODE, String.class, CommonFlags.STRING, "output reporting mode", VcfEvalTask.MODE_SPLIT).setCategory(REPORTING);
    modeFlag.setParameterRange(new String[]{VcfEvalTask.MODE_SPLIT, VcfEvalTask.MODE_ANNOTATE, VcfEvalTask.MODE_COMBINE, VcfEvalTask.MODE_GA4GH, VcfEvalTask.MODE_ROC_ONLY});

    mFlags.registerOptional(MAX_LENGTH, Integer.class, CommonFlags.INT, "don't attempt to evaluate variant alternatives longer than this", 1000).setCategory(FILTERING);
    mFlags.registerOptional(SAMPLE_PLOIDY, Integer.class, CommonFlags.INT, "expected ploidy of samples", 2).setCategory(FILTERING);
    mFlags.registerOptional(TWO_PASS, Boolean.class, "BOOL", "run diploid matching followed by squash-ploidy matching on FP/FN to find common alleles (Default is automatically set by output mode)").setCategory(FILTERING);

    registerVcfRocFlags(mFlags, VcfUtils.FORMAT_GENOTYPE_QUALITY, VcfEvalRocFilter.class);

    mFlags.registerOptional(OBEY_PHASE, String.class, CommonFlags.STRING, "if set, obey global phasing if present in the input VCFs. Use <baseline_phase>,<calls_phase> to select independently for baseline and calls. (Values must be one of [true, false, and invert])", "false").setCategory(FILTERING);
    mFlags.registerOptional(LOOSE_MATCH_DISTANCE, Integer.class, CommonFlags.INT, "if set, GA4GH mode will also apply distance-based loose-matching with the specified distance", 30).setCategory(FILTERING);
    mFlags.registerOptional(DECOMPOSE, "decompose complex variants into smaller constituents to allow partial credit").setCategory(FILTERING);

    CommonFlags.initThreadsFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);

    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateTabixedInputFile(flags, BASELINE, CALLS)
      && CommonFlags.validateThreads(flags)
      && CommonFlags.validateTemplate(flags)
      && CommonFlags.validateRegions(flags)
      && flags.checkNand(SCORE_FIELD, NO_ROC)
      && flags.checkNand(SQUASH_PLOIDY, TWO_PASS)
      && validatePairedFlag(flags, SAMPLE, "sample name")
      && validatePairedFlag(flags, OBEY_PHASE, "phase type")
      && validateModeSample(flags)
      && validateVcfRocFlags(flags)
      && flags.checkInRange(SAMPLE_PLOIDY, 1, Integer.MAX_VALUE)
    );
  }

  /**
   * @param flags object to register flags into
   * @return true if the standard ROC flags validate successfully 
   */
  public static boolean validateVcfRocFlags(CFlags flags) {
      return validateScoreField(flags)
      && validateVcfRocFlag(flags, ROC_EXPR)
      && validateVcfRocFlag(flags, ROC_REGIONS)
      && flags.checkAtMostOne(CRITERIA_PRECISION, CRITERIA_SENSITIVITY, CRITERIA_SCORE)
      && flags.checkInRange(CRITERIA_PRECISION, 0.0, 1.0)
      && flags.checkInRange(CRITERIA_SENSITIVITY, 0.0, 1.0);
  }

  /**
   * @param flags object to register flags into
   * @param defaultScoreField default value for the score field flag
   * @param rocfilterclass supplier of preset roc subsets
   * @param <T> type of class providing roc filter subsets 
   */
  public static <T extends RocFilterProxy> void registerVcfRocFlags(CFlags flags, String defaultScoreField, Class<T> rocfilterclass) {
    flags.registerOptional('f', SCORE_FIELD, String.class, CommonFlags.STRING, "the name of the VCF FORMAT field to use as the ROC score. Also valid are \"QUAL\", \"INFO.<name>\" or \"FORMAT.<name>\" to select the named VCF FORMAT or INFO field", defaultScoreField).setCategory(REPORTING);
    flags.registerOptional('O', SORT_ORDER, RocSortOrder.class, CommonFlags.STRING, "the order in which to sort the ROC scores so that \"good\" scores come before \"bad\" scores", RocSortOrder.DESCENDING).setCategory(REPORTING);
    flags.registerOptional(ROC_SUBSET, rocfilterclass, CommonFlags.STRING, "output ROC file for preset variant subset").setMaxCount(Integer.MAX_VALUE).enableCsv().setCategory(REPORTING);
    flags.registerOptional(ROC_EXPR, String.class, CommonFlags.STRING, "output ROC file for variants matching custom JavaScript expression. Use the form <LABEL>=<EXPRESSION>").setMaxCount(Integer.MAX_VALUE).setCategory(REPORTING);
    flags.registerOptional(ROC_REGIONS, String.class, CommonFlags.STRING, "output ROC file for variants overlapping custom regions supplied in BED file. Use the form <LABEL>=<FILENAME>").setMaxCount(Integer.MAX_VALUE).setCategory(REPORTING);
    flags.registerOptional(ROC_CROSS_JOIN, "output ROC files for each intersection of --" + ROC_SUBSET + ", --" + ROC_EXPR + ", and --" + ROC_REGIONS + " values provided").setCategory(REPORTING);
    flags.registerOptional(CRITERIA_PRECISION, Double.class, CommonFlags.FLOAT, "output summary statistics where precision >= supplied value (Default is to summarize at maximum F-measure)").setCategory(REPORTING);
    flags.registerOptional(CRITERIA_SENSITIVITY, Double.class, CommonFlags.FLOAT, "output summary statistics where sensitivity >= supplied value (Default is to summarize at maximum F-measure)").setCategory(REPORTING);
    flags.registerOptional(CRITERIA_SCORE, Double.class, CommonFlags.FLOAT, "output summary statistics where score meets supplied value (Default is to summarize at maximum F-measure)").setCategory(REPORTING);
    flags.registerOptional(SLOPE_FILES, "output files for ROC slope analysis").setCategory(REPORTING);
  }

  static boolean validateVcfRocFlag(CFlags flags, String rocFlag) {
    if (flags.isSet(rocFlag)) {
      final List<?> values = flags.getValues(rocFlag);
      for (Object o : values) {
        final String spec = (String) o;
        final String[] split = StringUtils.split(spec, '=', 2);
        if (split.length != 2) {
          flags.setParseMessage("--" + rocFlag + " invalid specification " + spec);
          return false;
        }
      }
    }
    return true;
  }

  private static boolean validateModeSample(CFlags flags) {
    final String mode = (String) flags.getValue(OUTPUT_MODE);
    if (flags.isSet(SAMPLE) && ((String) flags.getValue(SAMPLE)).contains(VariantFactory.ALT_SAMPLE) && (VcfEvalTask.MODE_COMBINE.equals(mode) || VcfEvalTask.MODE_GA4GH.equals(mode))) {
      flags.setParseMessage("--" + OUTPUT_MODE + "=" + mode + " cannot be used when either sample is " + VariantFactory.ALT_SAMPLE);
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
      final int pIndex = field.indexOf('.');
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
  private static Orientor phaseTypeToOrientor(String name, int haplotypes) {
    switch (name) {
      case "true":
        return new Orientor.PhasedOrientor(false, haplotypes);
      case "invert":
        return new Orientor.PhasedOrientor(true, haplotypes);
      case "false":
        return new Orientor.UnphasedOrientor(haplotypes);
      default:
        throw new NoTalkbackSlimException("Unrecognized phase type:" + name);
    }
  }

  @Override
  protected VcfEvalParams makeParams() throws IOException {
    final VcfEvalParamsBuilder builder = VcfEvalParams.builder();
    builder.name(mFlags.getName());
    builder.outputParams(new OutputParams(outputDirectory(), !mFlags.isSet(CommonFlags.NO_GZIP)));
    builder.templateFile((File) mFlags.getValue(CommonFlags.TEMPLATE_FLAG));
    final File baseline = (File) mFlags.getValue(BASELINE);
    final File calls = (File) mFlags.getValue(CALLS);
    builder.baseLineFile(baseline).callsFile(calls);
    final RocSortOrder sortOrder = (RocSortOrder) mFlags.getValue(SORT_ORDER);
    builder.sortOrder(sortOrder);
    final String scoreField = mFlags.isSet(VcfEvalCli.NO_ROC) ? null : (String) mFlags.getValue(VcfEvalCli.SCORE_FIELD);
    builder.scoreField(scoreField);
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
    final int haplotypes = (Integer) mFlags.getValue(SAMPLE_PLOIDY);
    final String[] phaseTypes = splitPairedSpec((String) mFlags.getValue(OBEY_PHASE));
    builder.baselinePhaseOrientor(phaseTypeToOrientor(phaseTypes[0], haplotypes));
    builder.callsPhaseOrientor(phaseTypeToOrientor(phaseTypes[1], haplotypes));
    final Set<String> genotypeSubsets = new HashSet<>(Arrays.asList(RocFilter.HET.name(), RocFilter.HOM.name()));
    final Set<String> variantSubsets = new HashSet<>(
              Arrays.asList(RocFilter.SNP.name(), RocFilter.NON_SNP.name(), RocFilter.INDEL.name(), RocFilter.MNP.name())
    );
    final Set<RocFilter> rocFilters = getRocFilters(mFlags, Arrays.asList(RocFilter.SNP, RocFilter.NON_SNP), genotypeSubsets, variantSubsets);
    builder.rocFilters(rocFilters);
    if (mFlags.isSet(CRITERIA_PRECISION)) {
      builder.rocCriteria(new PrecisionThreshold((Double) mFlags.getValue(CRITERIA_PRECISION)));
    } else if (mFlags.isSet(CRITERIA_SENSITIVITY)) {
      builder.rocCriteria(new SensitivityThreshold((Double) mFlags.getValue(CRITERIA_SENSITIVITY)));
    } else if (mFlags.isSet(CRITERIA_SCORE)) {
      builder.rocCriteria(new FixedScoreThreshold((Double) mFlags.getValue(CRITERIA_SCORE), sortOrder.comparator()));
    }
    builder.useAllRecords(mFlags.isSet(ALL_RECORDS));
    builder.decompose(mFlags.isSet(DECOMPOSE));
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

  /**
   * Helper method to construct roc filters from flags
   * @param flags the flags with options set
   * @param fallbackRocFilters the default filters to configure if no flags were provided
   * @param rocSubsetGroups defines how to group together the various pre-defined roc subsets when determining
   * cross-join combinations
   * @return the set of roc filters to use
   * @throws IOException if a region based filter is specified but cannot be loaded
   */
  @SafeVarargs
  public static Set<RocFilter> getRocFilters(CFlags flags, List<RocFilter> fallbackRocFilters, Set<String>... rocSubsetGroups) throws IOException {
    final LinkedHashMap<Object, Set<RocFilter>> rocFiltersTypeMap = new LinkedHashMap<>();

    final Set<RocFilter> rocExprFilters = new LinkedHashSet<>();
    if (flags.isSet(ROC_EXPR)) {
      final List<?> values = flags.getValues(VcfEvalCli.ROC_EXPR);
      for (Object o : values) {
        final String[] e = StringUtils.split((String) o, '=', 2);
        assert e.length == 2;
        rocExprFilters.add(new ExpressionRocFilter(e[0], e[1]));
      }
    }
    rocFiltersTypeMap.put(ROC_EXPR, rocExprFilters);

    final Set<RocFilter> rocRegionsFilters = new LinkedHashSet<>();
    if (flags.isSet(ROC_REGIONS)) {
      final List<?> values = flags.getValues(VcfEvalCli.ROC_REGIONS);
      for (Object o : values) {
        final String[] e = StringUtils.split((String) o, '=', 2);
        assert e.length == 2;
        rocRegionsFilters.add(new RegionsRocFilter(e[0], new File(e[1])));
      }
    }
    rocFiltersTypeMap.put(ROC_REGIONS, rocRegionsFilters);

    // Split subset filters into flavors for intersecting in ROC_CROSS_JOIN
    for (Set<String> group : rocSubsetGroups) {
      rocFiltersTypeMap.put(group, new LinkedHashSet<>());
    }
    if (flags.isSet(VcfEvalCli.ROC_SUBSET)) {
      final List<?> values = flags.getValues(VcfEvalCli.ROC_SUBSET);
      for (Object o : values) {
        boolean invalid = true;
        final RocFilter rocFilter = ((RocFilterProxy) o).filter();
        for (Set<String> group : rocSubsetGroups) {
          if (group.contains(rocFilter.name())) {
            rocFiltersTypeMap.get(group).add(rocFilter);
            invalid = false;
          }
        }
        if (invalid) {
          throw new NoTalkbackSlimException("Cannot use roc-subset filter: " + rocFilter.name() + " with this tool.");
        }
      }
    }

    final Set<RocFilter> rocFilters = new LinkedHashSet<>();
    if (flags.isSet(ROC_CROSS_JOIN)) {
      final List<Object> keys = new ArrayList<>();
      for (Map.Entry<Object, Set<RocFilter>> e : rocFiltersTypeMap.entrySet()) {
        if (!e.getValue().isEmpty()) {
          keys.add(e.getKey());
        }
      }
      if (keys.size() > 0) {
        buildCombinedRocFilters(rocFilters, rocFiltersTypeMap, keys, 0, new RocFilter[keys.size()]);
      }
      rocFilters.add(RocFilter.ALL);  // Make sure this is added at end for downstream

    } else {
      rocFilters.add(RocFilter.ALL); // We require the ALL entry for aggregate stats
      for (Set<RocFilter> filters : rocFiltersTypeMap.values()) {
        rocFilters.addAll(filters);
      }
    }

    if (!flags.isAnySet(ROC_SUBSET, ROC_EXPR, ROC_REGIONS)) {
      rocFilters.addAll(fallbackRocFilters);
    }

    return rocFilters;
  }

  private static void buildCombinedRocFilters(Set<RocFilter> filters, LinkedHashMap<Object, Set<RocFilter>> rocFiltersTypeMap, List<Object> keys, int pos, RocFilter[] path) {
    if (pos < keys.size()) {
      final Set<RocFilter> group = rocFiltersTypeMap.get(keys.get(pos));
      for (RocFilter f : group) {
        path[pos] = f;
        buildCombinedRocFilters(filters, rocFiltersTypeMap, keys, pos + 1, path);
      }
    } else {
      final List<RocFilter> delegates = Arrays.asList(path);
      final boolean rescale = delegates.stream().allMatch(RocFilter::rescale);
      filters.add(new CombinedRocFilter(delegates, rescale));
    }
  }

}
