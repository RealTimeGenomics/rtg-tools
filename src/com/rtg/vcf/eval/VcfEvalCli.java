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
import java.util.EnumSet;
import java.util.Locale;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.NoStatistics;
import com.rtg.launcher.OutputParams;
import com.rtg.launcher.ParamsCli;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.IORunnable;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.annotation.DerivedAnnotations;
import com.rtg.vcf.eval.VcfEvalParams.VcfEvalParamsBuilder;

/**
 * Compare detected SNPs with the generated SNPs
 *
 */
public class VcfEvalCli extends ParamsCli<VcfEvalParams> {

  private static final String MODULE_NAME = "vcfeval";
  private static final String BASELINE = "baseline";
  private static final String CALLS = "calls";
  private static final String SORT_ORDER = "sort-order";
  private static final String ALL_RECORDS = "all-records";
  private static final String SAMPLE = "sample";
  static final String SORT_FIELD = "vcf-score-field";
  private static final String SQUASH_PLOIDY = "squash-ploidy";
  private static final String REF_OVERLAP = "ref-overlap";
  private static final String OUTPUT_MODE = "output-mode";
  private static final String EVAL_REGIONS_FLAG = "evaluation-regions";

  private static final String ROC_SUBSET = "Xroc-subset";
  private static final String SLOPE_FILES = "Xslope-files";
  private static final String MAX_LENGTH = "Xmax-length";
  private static final String RTG_STATS = "Xrtg-stats";
  private static final String TWO_PASS = "Xtwo-pass";
  private static final String OBEY_PHASE = "Xobey-phase";

  @Override
  public String moduleName() {
    return MODULE_NAME;
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
    initFlags(mFlags);
    mFlags.setName(applicationName() + " " + moduleName());
  }

  /**
   * initialize a flags object
   * @param flags the flags object to initialize
   */
  public static void initFlags(final CFlags flags) {
    CommonFlagCategories.setCategories(flags);
    flags.registerExtendedHelp();
    flags.setDescription("Evaluates called variants for genotype agreement with a baseline variant set irrespective of representational differences. Outputs a weighted ROC file which can be viewed with rtg rocplot and VCF files containing false positives (called variants not matched in the baseline), false negatives (baseline variants not matched in the call set), and true positives (variants that match between the baseline and calls).");
    CommonFlags.initOutputDirFlag(flags);
    flags.registerRequired('b', BASELINE, File.class, "file", "VCF file containing baseline variants").setCategory(INPUT_OUTPUT);
    flags.registerRequired('c', CALLS, File.class, "file", "VCF file containing called variants").setCategory(INPUT_OUTPUT);
    flags.registerRequired('t', CommonFlags.TEMPLATE_FLAG, File.class, "SDF", "SDF of the reference genome the variants are called against").setCategory(INPUT_OUTPUT);

    flags.registerOptional(CommonFlags.RESTRICTION_FLAG, String.class, "string", "if set, only read VCF records within the specified range. The format is one of <template_name>, <template_name>:start-end or <template_name>:start+length").setCategory(INPUT_OUTPUT);
    flags.registerOptional(CommonFlags.BED_REGIONS_FLAG, File.class, "File", "if set, only read VCF records that overlap the ranges contained in the specified BED file").setCategory(INPUT_OUTPUT);

    flags.registerOptional('e', EVAL_REGIONS_FLAG, File.class, "File", "if set, evaluate within regions contained in the supplied BED file, allowing transborder matches. To be used for truth-set high-confidence regions or other regions of interest where region boundary effects should be minimized").setCategory(INPUT_OUTPUT);

    flags.registerOptional(SAMPLE, String.class, "STRING", "the name of the sample to select. Use <baseline_sample>,<calls_sample> to select different sample names for baseline and calls. (Required when using multi-sample VCF files)").setCategory(FILTERING);
    flags.registerOptional(ALL_RECORDS, "use all records regardless of FILTER status (Default is to only process records where FILTER is \".\" or \"PASS\")").setCategory(FILTERING);
    flags.registerOptional(SQUASH_PLOIDY, "treat heterozygous genotypes as homozygous ALT in both baseline and calls, to allow matches that ignore zygosity differences").setCategory(FILTERING);
    flags.registerOptional(REF_OVERLAP, "allow alleles to overlap where bases of either allele are same-as-ref (Default is to only allow VCF anchor base overlap)").setCategory(FILTERING);

    flags.registerOptional('f', SORT_FIELD, String.class, "STRING", "the name of the VCF FORMAT field to use as the ROC score. Also valid are \"QUAL\" or \"INFO=<name>\" to select the named VCF INFO field", VcfUtils.FORMAT_GENOTYPE_QUALITY).setCategory(REPORTING);
    flags.registerOptional('O', SORT_ORDER, RocSortOrder.class, "STRING", "the order in which to sort the ROC scores so that \"good\" scores come before \"bad\" scores", RocSortOrder.DESCENDING).setCategory(REPORTING);
    final Flag modeFlag = flags.registerOptional('m', OUTPUT_MODE, String.class, "STRING", "output reporting mode", VcfEvalTask.MODE_SPLIT).setCategory(REPORTING);
    modeFlag.setParameterRange(new String[]{VcfEvalTask.MODE_SPLIT, VcfEvalTask.MODE_ANNOTATE, VcfEvalTask.MODE_COMBINE, VcfEvalTask.MODE_GA4GH, VcfEvalTask.MODE_ROC_ONLY});
    flags.registerOptional('R', ROC_SUBSET, RocFilter.class, "FILTER", "output ROC files corresponding to call subsets").setMaxCount(Integer.MAX_VALUE).enableCsv().setCategory(REPORTING);

    flags.registerOptional(MAX_LENGTH, Integer.class, "INT", "don't attempt to evaluate variant alternatives longer than this", 1000).setCategory(FILTERING);
    flags.registerOptional(TWO_PASS, Boolean.class, "BOOL", "run diploid matching followed by squash-ploidy matching on FP/FN to find common alleles (Default is automatically set by output mode)").setCategory(FILTERING);
    flags.registerOptional(RTG_STATS, "output RTG specific files and statistics").setCategory(REPORTING);
    flags.registerOptional(SLOPE_FILES, "output files for ROC slope analysis").setCategory(REPORTING);
    flags.registerOptional(OBEY_PHASE, String.class, "STRING", "if set, obey global phasing if present in the input VCFs. Use <baseline_phase>,<calls_phase> to select independently for baseline and calls. (Values must be one of [true, false, and invert])", "false").setCategory(FILTERING);

    CommonFlags.initThreadsFlag(flags);
    CommonFlags.initNoGzip(flags);
    flags.setValidator(new VcfEvalFlagsValidator());
  }

  static class VcfEvalFlagsValidator implements Validator {

    @Override
    public boolean isValid(final CFlags flags) {
      if (!CommonFlags.validateOutputDirectory(flags)) {
        return false;
      }

      final File generated = (File) flags.getValue(BASELINE);
      if (!(generated.exists() && generated.isFile())) {
        flags.setParseMessage("baseline VCF file doesn't exist");
        return false;
      }
      final File detected = (File) flags.getValue(CALLS);
      if (!(detected.exists() && detected.isFile())) {
        flags.setParseMessage("calls VCF file doesn't exist");
        return false;
      }
      if (flags.isSet(SORT_FIELD)) {
        final String field = (String) flags.getValue(SORT_FIELD);
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
      if (!CommonFlags.validateThreads(flags)) {
        return false;
      }
      if (!CommonFlags.validateTemplate(flags)) {
        return false;
      }
      if (!CommonFlags.validateRegions(flags)) {
        return false;
      }
      if (!flags.checkNand(SQUASH_PLOIDY, TWO_PASS)) {
        return false;
      }
      if (flags.isSet(SAMPLE)) {
        if (validatePairedFlag(flags, (String) flags.getValue(SAMPLE), "sample name")) {
          return false;
        }
      }
      if (flags.isSet(OBEY_PHASE)) {
        if (validatePairedFlag(flags, (String) flags.getValue(OBEY_PHASE), "phase type")) {
          return false;
        }
      }
      return true;
    }

    private boolean validatePairedFlag(CFlags flags, String flagValue, String label) {
      if (flagValue.length() == 0) {
        flags.setParseMessage("Supplied " + label + " cannot be empty");
        return true;
      }
      final String[] split = StringUtils.split(flagValue, ',');
      if (split.length > 2) {
        flags.setParseMessage("Invalid " + label + " specification " + flagValue + ". At most 1 comma is permitted");
        return true;
      }
      if (split[0].length() == 0) {
        flags.setParseMessage("Invalid " + label + " specification " + flagValue + ". Supplied baseline " + label + " cannot be empty");
        return true;
      }
      final String callsValue = split.length == 2 ? split[1] : split[0];
      if (callsValue.length() == 0) {
        flags.setParseMessage("Invalid sample name specification " + flagValue + ". Supplied calls " + label + " cannot be empty");
        return true;
      }
      return false;
    }
  }


  @Override
  protected IORunnable task(VcfEvalParams params, OutputStream out) {
    return new VcfEvalTask(params, out, new NoStatistics());
  }

  private void checkTabix(File vcfFile) throws IOException {
    final File index = TabixIndexer.indexFileName(vcfFile);
    if (!TabixIndexer.isBlockCompressed(vcfFile)) {
      throw new NoTalkbackSlimException(vcfFile + " is not in bgzip format");
    } else if (!index.exists()) {
      throw new NoTalkbackSlimException("Index not found for file: " + index.getPath() + " expected index called: " + index.getPath());
    }
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
  protected VcfEvalParams makeParams() throws InvalidParamsException, IOException {
    final VcfEvalParamsBuilder builder = VcfEvalParams.builder();
    builder.name(mFlags.getName());
    builder.outputParams(new OutputParams(outputDirectory(), false, !mFlags.isSet(CommonFlags.NO_GZIP)));
    builder.templateFile((File) mFlags.getValue(CommonFlags.TEMPLATE_FLAG));
    final File baseline = (File) mFlags.getValue(BASELINE);
    final File calls = (File) mFlags.getValue(CALLS);
    checkTabix(baseline);
    checkTabix(calls);
    builder.baseLineFile(baseline).callsFile(calls);
    builder.sortOrder((RocSortOrder) mFlags.getValue(SORT_ORDER));
    builder.scoreField((String) mFlags.getValue(SORT_FIELD));
    builder.maxLength((Integer) mFlags.getValue(MAX_LENGTH));
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
    final EnumSet<RocFilter> rocFilters;
    if (!mFlags.isSet(ROC_SUBSET)) {
      rocFilters = EnumSet.of(RocFilter.ALL, RocFilter.SNP, RocFilter.NON_SNP);
    } else {
      rocFilters = EnumSet.noneOf(RocFilter.class);
      for (Object o : mFlags.getValues(ROC_SUBSET)) {
        rocFilters.add((RocFilter) o);
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
    } else if (!mode.equals(VcfEvalTask.MODE_SPLIT)) {
      builder.twoPass(true); // Default to two passes except for split mode (backward compability)
    }
    return builder.create();
  }
}
