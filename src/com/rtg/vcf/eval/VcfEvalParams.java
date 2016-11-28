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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.rtg.launcher.OutputModuleParams;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.vcf.VcfUtils;

/**
 * Params object for VcfEvalTask
 */
public final class VcfEvalParams extends OutputModuleParams {

  /**
   * Creates a <code>VcfEvalParamsBuilder</code> builder.
   * @return the builder.
   */
  public static VcfEvalParamsBuilder builder() {
    return new VcfEvalParamsBuilder();
  }

  /**
   * A builder class for <code>VcfEvalParams</code>.
   */
  public static final class VcfEvalParamsBuilder extends OutputModuleParamsBuilder<VcfEvalParamsBuilder> {
    File mBaselineFile;
    File mCallsFile;
    File mTemplateFile;
    RocSortOrder mSortOrder = RocSortOrder.DESCENDING;
    String mScoreField = VcfUtils.FORMAT_GENOTYPE_QUALITY;
    String mBaselineSample;
    String mCallsSample;
    String mOutputMode = VcfEvalTask.MODE_SPLIT;
    boolean mUseAllRecords = false;
    int mNumberThreads = 1;
    private boolean mTwoPass = false;
    private boolean mSquashPloidy = false;
    private boolean mRefOverlap = false;
    int mMaxLength = -1;
    boolean mOutputSlopeFiles = false;
    private RegionRestriction mRestriction = null;
    private File mBedRegionsFile = null;
    private File mEvalRegionsFile = null;
    private Set<RocFilter> mRocFilters = new HashSet<>(Arrays.asList(RocFilter.ALL, RocFilter.HET, RocFilter.HOM));
    private Orientor mBaselinePhaseOrientor = Orientor.UNPHASED;
    private Orientor mCallsPhaseOrientor = Orientor.UNPHASED;

    @Override
    protected VcfEvalParamsBuilder self() {
      return this;
    }

    /**
     * Sets the baseline file.
     *
     * @param baseline the baseline file.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder baseLineFile(final File baseline) {
      mBaselineFile = baseline;
      return self();
    }

    /**
     * Sets the calls file.
     *
     * @param calls the calls file.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder callsFile(final File calls) {
      mCallsFile = calls;
      return self();
    }

    /**
     * Sets the template file.
     *
     * @param template the template file.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder templateFile(final File template) {
      mTemplateFile = template;
      return self();
    }

    /**
     * Set to use all records from VCF, by default we only use PASS records.
     *
     * @param useAllRecords set to <code>true</code> to use all records for ROC.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder useAllRecords(final boolean useAllRecords) {
      mUseAllRecords = useAllRecords;
      return self();
    }

    /**
     * Sets the baseline sample name to use in multiple sample VCF input.
     *
     * @param sampleName the sample name to use in multiple sample VCF input.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder baselineSample(final String sampleName) {
      mBaselineSample = sampleName;
      return self();
    }

    /**
     * Sets the calls sample name to use in multiple sample VCF input.
     *
     * @param sampleName the sample name to use in multiple sample VCF input.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder callsSample(final String sampleName) {
      mCallsSample = sampleName;
      return self();
    }

    /**
     * Sets the run output mode.
     *
     * @param modeName the name of the output mode.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder outputMode(final String modeName) {
      mOutputMode = modeName;
      return self();
    }

    /**
     * Sets the VCF ROC scoring field.
     *
     * @param scoreField the VCF format field to use as the ROC score.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder scoreField(final String scoreField) {
      mScoreField = scoreField;
      return self();
    }

    /**
     * Sets the sort order for the ROC score.
     *
     * @param sortOrder the sort order for the ROC score.
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder sortOrder(final RocSortOrder sortOrder) {
      mSortOrder = sortOrder;
      return self();
    }

    /**
     * @param value number of threads to use when evaluating variants
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder numberThreads(int value) {
      mNumberThreads = value;
      return self();
    }

    /**
     * @param twoPass true if we should run diploid followed by squash-ploidy evaluation
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder twoPass(boolean twoPass) {
      mTwoPass = twoPass;
      return self();
    }

    /**
     * @param squashPloidy true if heterozygous diploid calls should be squashed to homozygous alternative
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder squashPloidy(boolean squashPloidy) {
      mSquashPloidy = squashPloidy;
      return self();
    }

    /**
     * @param refOverlap true if overlaps may occur where either side is same-as-ref.
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder refOverlap(boolean refOverlap) {
      mRefOverlap = refOverlap;
      return self();
    }

    /**
     * @param maxLength the maximum length variant to consider
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder maxLength(int maxLength) {
      mMaxLength = maxLength;
      return self();
    }

    /**
     * @param filters the set of ROC outputs to produce
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder rocFilters(Set<RocFilter> filters) {
      mRocFilters = filters;
      return self();
    }

    /**
     * @param outputSlopeFiles if set, output the files for ROC slope analysis
     * @return this builder, so calls can be chained
     */
    public VcfEvalParamsBuilder outputSlopeFiles(boolean outputSlopeFiles) {
      mOutputSlopeFiles = outputSlopeFiles;
      return self();
    }

    /**
     * Sets a restriction on the records that will be processed. The format is a reference sequence name,
     * followed by an optional range specification. Only records that match the reference name and start
     * within the range (if specified) will be processed. A name of null indicates no filtering.
     * @param restriction a reference sequence name
     * @return this builder, so calls can be chained.
     */
    public VcfEvalParamsBuilder restriction(final RegionRestriction restriction) {
      mRestriction = restriction;
      return this;
    }

    /**
     * Set the bed file to use which specifies regions from which variants will be loaded
     * @param bedFile the bed file which specifies regions
     * @return this builder
     */
    public VcfEvalParamsBuilder bedRegionsFile(File bedFile) {
      mBedRegionsFile = bedFile;
      return this;
    }

    /**
     * Set the bed file to use which specifies regions for transborder matches
     * @param bedFile the bed file which specifies regions
     * @return this builder
     */
    public VcfEvalParamsBuilder evalRegionsFile(File bedFile) {
      mEvalRegionsFile = bedFile;
      return this;
    }

    /**
     * Set the Orientor to use for the baseline during diploid GT comparisons.
     * @param orientor the Orientor to use
     * @return this builder
     */
    public VcfEvalParamsBuilder baselinePhaseOrientor(Orientor orientor) {
      mBaselinePhaseOrientor = orientor;
      return this;
    }

    /**
     * Set the Orientor to use for the calls during diploid GT comparisons.
     * @param orientor the Orientor to use
     * @return this builder
     */
    public VcfEvalParamsBuilder callsPhaseOrientor(Orientor orientor) {
      mCallsPhaseOrientor = orientor;
      return this;
    }

    /**
     * Creates a <code>VcfEvalParams</code> using the current builder
     * configuration.
     * @return the new <code>VcfEvalParams</code>
     */
    public VcfEvalParams create() {
      return new VcfEvalParams(this);
    }
  }

  private final File mBaselineFile;
  private final File mCallsFile;
  private final File mTemplateFile;
  private final RegionRestriction mRestriction;
  private final File mBedRegionsFile;
  private final File mEvalRegionsFile;
  private final String mScoreField;
  private final String mOutputMode;
  private final RocSortOrder mSortOrder;
  private final String mBaselineSample;
  private final String mCallsSample;
  private final int mNumberThreads;
  private final boolean mUseAllRecords;
  private final boolean mTwoPass;
  private final boolean mSquashPloidy;
  private final boolean mRefOverlap;
  private final int mMaxLength;
  private final Set<RocFilter> mRocFilters;
  private final boolean mOutputSlopeFiles;
  private final Orientor mBaselinePhaseOrientor;
  private final Orientor mCallsPhaseOrientor;


  /**
   * @param builder the builder object.
   */
  protected VcfEvalParams(VcfEvalParamsBuilder builder) {
    super(builder);
    mBaselineFile = builder.mBaselineFile;
    mCallsFile = builder.mCallsFile;
    mTemplateFile = builder.mTemplateFile;
    mRestriction = builder.mRestriction;
    mBedRegionsFile = builder.mBedRegionsFile;
    mEvalRegionsFile = builder.mEvalRegionsFile;
    mSortOrder = builder.mSortOrder;
    mScoreField = builder.mScoreField;
    mOutputMode = builder.mOutputMode;
    mBaselineSample = builder.mBaselineSample;
    mCallsSample = builder.mCallsSample;
    mNumberThreads = builder.mNumberThreads;
    mUseAllRecords = builder.mUseAllRecords;
    mTwoPass = builder.mTwoPass;
    mSquashPloidy = builder.mSquashPloidy;
    mRefOverlap = builder.mRefOverlap;
    mMaxLength = builder.mMaxLength;
    mRocFilters = builder.mRocFilters;
    mOutputSlopeFiles = builder.mOutputSlopeFiles;
    mBaselinePhaseOrientor = builder.mBaselinePhaseOrientor;
    mCallsPhaseOrientor = builder.mCallsPhaseOrientor;
  }

  /**
   * Get the baseline file.
   * @return the baseline file.
   */
  public File baselineFile() {
    return mBaselineFile;
  }

  /**
   * Get the calls file.
   * @return the calls file.
   */
  public File callsFile() {
    return mCallsFile;
  }

  /**
   * Get the template file.
   * @return the template file.
   */
  public File templateFile() {
    return mTemplateFile;
  }


  /**
   * @return The region to restrict iteration of the VCF records to.
   */
  public RegionRestriction restriction() {
    return mRestriction;
  }

  /**
   * @return a bed file containing the regions to load variants from, or null for no bed region based filtering.
   */
  public File bedRegionsFile() {
    return mBedRegionsFile;
  }

  /**
   * @return a bed file containing the evaluation regions, or null for no transborder region evaluation.
   */
  public File evalRegionsFile() {
    return mEvalRegionsFile;
  }

  /**
   * Get the sort field from VCF to use for ROC curves.
   * @return the sort field from VCF format field to use for ROC curves.
   */
  public String scoreField() {
    return mScoreField;
  }

  /**
   * Get the sort order to use for ROC curves.
   * @return the sort order to use in ROC curves.
   */
  public RocSortOrder sortOrder() {
    return mSortOrder;
  }

  /**
   * Get the baseline sample name for multiple sample VCF input.
   * @return the baseline sample name to use for multiple sample VCF input.
   */
  public String baselineSample() {
    return mBaselineSample;
  }
  /**
   * Get the calls sample name for multiple sample VCF input.
   * @return the calls sample name to use for multiple sample VCF input.
   */
  public String callsSample() {
    return mCallsSample;
  }

  /**
   * Get the run mode name.
   * @return the run mode name.
   */
  public String outputMode() {
    return mOutputMode;
  }

  /**
   * Get whether to use all VCF records for ROC or not
   * @return <code>true</code> if true all VCF records are used, if <code>false</code>, only PASS VCF records will be used.
   */
  public boolean useAllRecords() {
    return mUseAllRecords;
  }

  /**
   * Get whether to run two-pass evaluation, diploid followed by squash on the false positives and false negatives.
   * @return true if two evaluation passes should be made.
   */
  public boolean twoPass() {
    return mTwoPass;
  }

  /**
   * @return true if heterozygous diploid calls should be squashed to homozygous alternative
   */
  public boolean squashPloidy() {
    return mSquashPloidy;
  }

  /**
   * @return true if overlaps are permitted at same-as-ref bases
   */
  public boolean refOverlap() {
    return mRefOverlap;
  }

  /**
   * @return the number of threads to run in
   */
  public int numberThreads() {
    return mNumberThreads;
  }

  /**
   * @return the limit on variant length
   */
  public int maxLength() {
    return mMaxLength;
  }

  /**
   * @return a set of the ROC outputs to produce
   */
  public Set<RocFilter> rocFilters() {
    return mRocFilters;
  }

  /**
   * @return the Orientor to use for the baseline during diploid GT comparisons.
   */
  public Orientor baselinePhaseOrientor() {
    return mBaselinePhaseOrientor;
  }

  /**
   * @return the Orientor to use for the calls during diploid GT comparisons.
   */
  public Orientor callsPhaseOrientor() {
    return mCallsPhaseOrientor;
  }

  /**
   * @return true if slope files should be written
   */
  public boolean outputSlopeFiles() {
    return mOutputSlopeFiles;
  }

  @Override
  public String toString() {
    return "Baseline file=" + mBaselineFile.getPath() + ", Calls file=" + mCallsFile.getPath() + ", Template file=" + mTemplateFile.getPath() + ", score field=" + mScoreField + ", sort order=" + mSortOrder + ", baseline sample name=" + mBaselineSample + ", calls sample name=" + mCallsSample + ", num threads=" + mNumberThreads + ", use all records=" + mUseAllRecords + ", squash ploidy=" + mSquashPloidy + ", two pass=" + mTwoPass + ", max length=" + mMaxLength + ", roc filters=" + mRocFilters + ", output mode=" + mOutputMode + ", output params=" + super.toString();
  }

}
