/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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
package com.rtg.launcher.globals;

import java.util.List;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.reference.Ploidy;
import com.rtg.util.cli.Flag;

/**
 * Experimental flags for tools release
 */
@JumbleIgnore
public class ToolsGlobalFlags extends GlobalFlagsInitializer {

  /** Allow SAM file loading when sam header is not coordinate sorted */
  public static final String SAM_IGNORE_SORT_ORDER_FLAG = "com.rtg.sam.ignore-header-sortorder";

  /** Allow definite SDF-ID mismatches to be warning rather than exit */
  public static final String LENIENT_SDF_ID_MISMATCH_FLAG = "com.rtg.sam.lenient-sdf-id";

  /** Level of gzip compression to use. */
  public static final String GZIP_LEVEL = "com.rtg.utils.gzip-level";

  /** When writing VCFs asynchronously, the maximum number of records to buffer (per VCF) */
  public static final String VCF_ASYNC_BUFFER_SIZE = "com.rtg.vcf.async-buffer-size";

  /** When looking at chromosomes declared as polyploid, treat as though they were actually the given ploidy */
  public static final String TREAT_POLYPLOID_AS = "com.rtg.reference.polyploid-as";

  /** When formatting SDF from mapped paired-end SAM, should we drop or keep alignments without a mate */
  public static final String FORMAT_SAMPE_KEEP_SINGLETONS = "com.rtg.format.sampe-keep-singletons";

  /** Optional directory where childsim should look for genetic maps */
  public static final String CHILDSIM_GENETIC_MAP_DIR = "com.rtg.simulation.variants.genetic-map-dir";

  /** Which strand simulated reads are sequenced from: 0 = random, -1 = reverse, 1 = forward */
  public static final String READ_STRAND = "com.rtg.simulation.reads.read-strand";
  /** Supply explicit sequence used for fragment read-through */
  public static final String READ_THROUGH = "com.rtg.simulation.reads.read-through";
  /** If set, assume supplied fragments are from OS-Seq sequencing, and if this size is greater than 0, simulate fragment truncation */
  public static final String OS_SEQ_FRAGMENTS = "com.rtg.simulation.reads.os-seq-fragments";

  /** Output the best path found along with the haplotypes */
  public static final String VCFEVAL_DUMP_BEST_PATH = "com.rtg.vcf.eval.dump-path";
  /** Trace the path finding algorithm -- produces tons of output */
  public static final String VCFEVAL_TRACE = "com.rtg.vcf.eval.trace";
  /** When comparing consistent paths, whether to maximize included calls, baseline, or sum of both */
  public static final String VCFEVAL_MAXIMIZE_MODE = "com.rtg.vcf.eval.maximize";
  /** Custom variant path result processor */
  public static final String VCFEVAL_PATH_PROCESSOR = "com.rtg.vcf.eval.custom-path-processor";
  /** Custom variant factories */
  public static final String VCFEVAL_VARIANT_FACTORY = "com.rtg.vcf.eval.custom-variant-factory";
  /** Allele-matching (to ignore call zygosity differences) is normally haploid for speed */
  public static final String VCFEVAL_HAPLOID_ALLELE_MATCHING = "com.rtg.vcf.eval.haploid-allele-matching";
  /** Specify the maximum number of simultaneous paths before vcfeval skips a region */
  public static final String VCFEVAL_MAX_PATHS = "com.rtg.vcf.eval.max-paths";
  /** Specify the maximum number of iterations since last sync point before vcfeval skips a region */
  public static final String VCFEVAL_MAX_ITERATIONS = "com.rtg.vcf.eval.max-iterations";
  /** Specify whether to treat the alleles of unknown sequence (e.g. missing side of a half call, spanning dels, etc) as an explicit token requiring a match, or just ignore */
  public static final String VCFEVAL_EXPLICIT_UNKNOWN_ALLELES = "com.rtg.vcf.eval.explicit-unknown-alleles";
  /** Turn on alternate ROC slope calculation */
  public static final String VCFEVAL_ALT_ROC_SLOPE_CALCULATION = "com.rtg.vcf.eval.alt-roc-slope";
  /** Mark variants matched in alternative paths */
  public static final String VCFEVAL_FLAG_ALTERNATES = "com.rtg.vcf.eval.flag-alternates";
  /** Prune paths that contain obvious no-ops (where variants cancel out) */
  public static final String VCFEVAL_PRUNE_NO_OPS = "com.rtg.vcf.eval.prune-no-ops";
  /** During vcfeval decomposition also break MNPs into individual SNPs */
  public static final String VCFEVAL_DECOMPOSE_MNPS = "com.rtg.vcf.eval.decompose.break-mnps";
  /** During vcfeval decomposition also separate remaining indels into individual SNPs plus an insertion/deletion */
  public static final String VCFEVAL_DECOMPOSE_INDELS = "com.rtg.vcf.eval.decompose.break-indels";
  /** Whether to apply ROC sub-category baseline rescaling to adjust for representation bias */
  public static final String VCFEVAL_ROC_SUBSET_RESCALE = "com.rtg.vcf.eval.roc-subset-rescale";

  /** Add labels to interpolated points */
  public static final String ROCPLOT_INTERPOLATE_LABEL = "com.rtg.vcf.eval.rocplot-interpolate-label";
  /** Minimum sensitivity gap between points after interpolation */
  public static final String ROCPLOT_INTERPOLATION_GAP = "com.rtg.vcf.eval.rocplot-interpolate-gap";


  ToolsGlobalFlags(List<Flag<?>> flags) {
    super(flags);
  }


  @Override
  public void registerFlags() {
    registerFlag(SAM_IGNORE_SORT_ORDER_FLAG);
    registerFlag(LENIENT_SDF_ID_MISMATCH_FLAG, Boolean.class, Boolean.TRUE);
    registerFlag(FORMAT_SAMPE_KEEP_SINGLETONS, Boolean.class, Boolean.FALSE);
    registerFlag(GZIP_LEVEL, Integer.class, 2);

    registerFlag(TREAT_POLYPLOID_AS, Ploidy.class, Ploidy.HAPLOID);

    registerFlag(VCF_ASYNC_BUFFER_SIZE, Integer.class, 2000);

    // Simulation
    registerFlag(CHILDSIM_GENETIC_MAP_DIR, String.class, "");
    registerFlag(READ_THROUGH, String.class, "default");
    registerFlag(READ_STRAND, Integer.class, 0);
    registerFlag(OS_SEQ_FRAGMENTS, Integer.class, 0);

    // vcfeval
    registerFlag(VCFEVAL_DUMP_BEST_PATH);
    registerFlag(VCFEVAL_TRACE);
    registerFlag(VCFEVAL_MAXIMIZE_MODE, String.class, "default");
    registerFlag(VCFEVAL_PATH_PROCESSOR, String.class, "");
    registerFlag(VCFEVAL_VARIANT_FACTORY, String.class, "");
    registerFlag(VCFEVAL_HAPLOID_ALLELE_MATCHING, Boolean.class, Boolean.TRUE);
    registerFlag(VCFEVAL_MAX_PATHS, Integer.class, 50000);
    registerFlag(VCFEVAL_MAX_ITERATIONS, Integer.class, 10000000);
    registerFlag(VCFEVAL_ALT_ROC_SLOPE_CALCULATION);
    registerFlag(VCFEVAL_EXPLICIT_UNKNOWN_ALLELES, Boolean.class, Boolean.FALSE);
    registerFlag(VCFEVAL_FLAG_ALTERNATES, Boolean.class, Boolean.FALSE);
    registerFlag(VCFEVAL_PRUNE_NO_OPS, Boolean.class, Boolean.TRUE);
    registerFlag(VCFEVAL_DECOMPOSE_MNPS, Boolean.class, Boolean.FALSE);
    registerFlag(VCFEVAL_DECOMPOSE_INDELS, Boolean.class, Boolean.FALSE);
    registerFlag(VCFEVAL_ROC_SUBSET_RESCALE, Boolean.class, Boolean.TRUE);

    registerFlag(ROCPLOT_INTERPOLATE_LABEL, Boolean.class, Boolean.FALSE);
    registerFlag(ROCPLOT_INTERPOLATION_GAP, Integer.class, 1);
  }
}
