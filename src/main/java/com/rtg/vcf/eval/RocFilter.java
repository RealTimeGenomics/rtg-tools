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

import java.util.Locale;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.vcf.VariantType;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Interface defining ROC filters
 */
public abstract class RocFilter {

  /** accepts everything **/
  public static final RocFilter ALL = new RocFilter("ALL", false) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      return true;
    }

    @Override
    public String fileName() {
      return "weighted_roc.tsv";
    }
    @Override
    public boolean requiresGt() {
      return false;
    }
  };

  /** all homozygous **/
  public static final RocFilter HOM = new RocFilter("HOM", "homozygous", true) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      return VcfUtils.isHomozygousAlt(gt);
    }
  };

  /** all heterozygous **/
  public static final RocFilter HET = new RocFilter("HET", "heterozygous", true) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      return VcfUtils.isHeterozygous(gt);
    }
  };

  /** all SNPs **/
  public static final RocFilter SNP = new RocFilter("SNP", true) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      final VariantType type = VariantType.getType(rec, gt);
      return type == VariantType.SNP;
    }
  };

  /** non-SNPs **/
  public static final RocFilter NON_SNP = new RocFilter("NON_SNP", true) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      final VariantType type = VariantType.getType(rec, gt);
      return type != VariantType.SNP;
    }
  };

  /** all MNPs (non-length changing) **/
  public static final RocFilter MNP = new RocFilter("MNP", true) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      final VariantType type = VariantType.getType(rec, gt);
      return type == VariantType.MNP;
    }
  };

  /** all indels (length changing) **/
  public static final RocFilter INDEL = new RocFilter("INDEL", true) {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      final VariantType type = VariantType.getType(rec, gt);
      return type.isIndelType();
    }
  };

  // RTG simple vs complex breakdowns
  /** all RTG complex calls **/
  public static final RocFilter XRX = new RocFilter("XRX") {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      return VcfUtils.isComplexScored(rec);
    }
    @Override
    public boolean requiresGt() {
      return false;
    }
  };

  /** all RTG simple (non complex) calls **/
  public static final RocFilter NON_XRX = new RocFilter("NON_XRX") {
    @Override
    public boolean accept(VcfRecord rec, int[] gt) {
      return !VcfUtils.isComplexScored(rec);
    }
    @Override
    public boolean requiresGt() {
      return false;
    }
  };


  /** The filename extension used for all ROC files */
  public static final String ROC_EXT = "_roc.tsv";

  protected String mName;
  private final String mBaseFilename;
  private final Boolean mRescale;

  /**
   * Create a RocFilter with default output file name, and using global default for whether to rescale counts to
   * baseline.
   * @param name the name of the filter
   */
  public RocFilter(String name) {
    this(name, null, null);
  }

  /**
   * Create a RocFilter with default output file name
   * @param name the name of the filter
   * @param rescale true if the call counts should be rescaled to baseline, false if the counts should not be rescaled, null to use global default
   */
  public RocFilter(String name, Boolean rescale) {
    this(name, null, rescale);
  }

  /**
   * Create a RocFilter with specified output file name
   * @param name the name of the filter
   * @param baseFilename the base filename used for ROC output files
   * @param rescale true if the call counts should be rescaled to baseline, false if the counts should not be rescaled, null to use global default
   */
  public RocFilter(String name, String baseFilename, Boolean rescale) {
    mName = name.trim();
    mBaseFilename = baseFilename;
    mRescale = rescale;
  }

  /**
   * @return the name of the RocFilter
   */
  public String name() {
    return mName;
  }

  /** @return true if the call counts should be rescaled with respect to baseline total */
  boolean rescale() {
    if (mRescale != null) {
      return mRescale;
    } else {
      return GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_ROC_SUBSET_RESCALE);
    }
  }
  
  @Override
  public String toString() {
    return name();
  }

  /**
   * Set the VCF header to be used with this filter.
   * @param header VCF header
   */
  public void setHeader(VcfHeader header) { }

  /**
   * @return true if the filter requires access to a GT value
   */
  public boolean requiresGt() {
    return true;
  }

  /**
   * Tests specified record
   * @param rec record to be tested
   * @param gt the split GT field of the sample
   * @return if accepted returns true, false otherwise
   */
  public abstract boolean accept(VcfRecord rec, int[] gt);

  /**
   * Get the name of the default output file for this filter
   * @return the output file name
   */
  public String fileName() {
    if (mBaseFilename != null) {
      return mBaseFilename + ROC_EXT;
    } else {
      return name().toLowerCase(Locale.ROOT) + ROC_EXT;
    }
  }
}
