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
package com.rtg.sam;

import java.io.File;

import com.rtg.util.IntegerOrPercentage;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;

/**
 * Parameters for selection of SAM records.
 */
public class SamFilterParams {

  /**
   * Creates a builder.
   * @return the builder.
   */
  public static SamFilterParamsBuilder builder() {
    return new SamFilterParamsBuilder();
  }

  /** The builder. */
  public static class SamFilterParamsBuilder {

    protected int mMaxAlignmentCount = -1;
    protected int mMinMapQ = -1;
    protected IntegerOrPercentage mMaxASMatedValue = null;
    protected IntegerOrPercentage mMaxASUnmatedValue = null;
    protected boolean mExcludeMated = false;
    protected boolean mExcludeUnmated = false;
    protected boolean mExcludeUnmapped = false;
    protected boolean mExcludeDuplicates = false;
    protected boolean mExcludeUnplaced = false;
    protected boolean mExcludeVariantInvalid = false;
    protected int mRequireUnsetFlags = 0;
    protected int mRequireSetFlags = 0;
    protected boolean mFindAndRemoveDuplicates = false; //this is for the detect and remove, rather than looking at the sam flag
    protected Double mSubsampleFraction = null;
    protected Double mSubsampleRampFraction = null;
    protected long mSubsampleSeed = 42;
    protected boolean mInvertFilters = false;

    protected SamRegionRestriction mRestriction = null;
    protected File mBedRegionsFile = null;

    /**
     * SAM records having MAPQ less than this value will be filtered.
     * @param val the minimum MAPQ permitted or -1 for no filtering
     * @return this builder, so calls can be chained
     */
    public SamFilterParamsBuilder minMapQ(final int val) {
      mMinMapQ = val;
      return this;
    }

    /**
     * Invert flag and attribute based filter criteria
     * @param invert true if criteria should be inverted.
     * @return this builder, so calls can be chained
     */
    public SamFilterParamsBuilder invertFilters(boolean invert) {
      mInvertFilters = invert;
      return this;
    }

    /**
     * Set the proportion of alignments to retain during subsampling.
     * @param fraction the proportion of alignments to retain (or null to disable subsampling)
     * @return this builder, so calls can be chained
     */
    public SamFilterParamsBuilder subsampleFraction(final Double fraction) {
      mSubsampleFraction = fraction;
      return this;
    }

    /**
     * Set the proportion of alignments to retain during subsampling.
     * @param fraction the proportion of alignments to retain (or null to disable subsampling)
     * @return this builder, so calls can be chained
     */
    public SamFilterParamsBuilder subsampleRampFraction(final Double fraction) {
      mSubsampleRampFraction = fraction;
      return this;
    }

    /**
     * Set the seed/salt used during subsampling.
     * @param seed the seed
     * @return this builder, so calls can be chained
     */
    public SamFilterParamsBuilder subsampleSeed(final long seed) {
      mSubsampleSeed = new PortableRandom(seed).nextLong(); // Add some PRNG variability to the user-supplied seed.
      return this;
    }

    /**
     * SAM records having alignment count over this value will be filtered.
     * @param val the maximum count or -1 for no filtering
     * @return this builder, so calls can be chained
     */
    public SamFilterParamsBuilder maxAlignmentCount(final int val) {
      mMaxAlignmentCount = val;
      return this;
    }

    /**
     * Mated SAM records having alignment score over this value will be filtered
     * @param val the maximum score as integer or percentage, or null for no filtering
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder maxMatedAlignmentScore(final IntegerOrPercentage val) {
      mMaxASMatedValue = val;
      return this;
    }

    /**
     * Unmated SAM records having alignment score over this value will be filtered
     * @param val the maximum score as integer or percentage, or null for no filtering
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder maxUnmatedAlignmentScore(final IntegerOrPercentage val) {
      mMaxASUnmatedValue = val;
      return this;
    }

    /**
     * Should unmated records be excluded. Default is false.
     * @param val true to exclude unmated records
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder excludeUnmated(final boolean val) {
      mExcludeUnmated = val;
      return this;
    }

    /**
     * Should mated records be excluded. Default is false.
     * @param val true to exclude mated records
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder excludeMated(final boolean val) {
      mExcludeMated = val;
      return this;
    }

    /**
     * Should unmapped records be excluded. Default is false.
     * @param val true to exclude unmapped records
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder excludeUnmapped(final boolean val) {
      mExcludeUnmapped = val;
      return this;
    }

    /**
     * Should records without an alignment start position be excluded. Default is false.
     * @param val true to exclude unmapped records
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder excludeUnplaced(final boolean val) {
      mExcludeUnplaced = val;
      return this;
    }

    /**
     * Should PCR and optical duplicate records be excluded. Default is false.
     * @param val true to exclude unmapped records
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder excludeDuplicates(final boolean val) {
      mExcludeDuplicates = val;
      return this;
    }

    /**
     * Exclude records which are invalid for the variant caller (e.g. records with NH=0).
     * @param val true to exclude records invalid in the variant caller
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder excludeVariantInvalid(final boolean val) {
      mExcludeVariantInvalid = val;
      return this;
    }

    /**
     * Specify mask indicating SAM flags that must be unset. Any record with any
     * of these flags set will be excluded. Default is not checking any of these flags.
     * @param flags mask indicating flags that must be unset.
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder requireUnsetFlags(final int flags) {
      mRequireUnsetFlags = flags;
      return this;
    }

    /**
     * Specify mask indicating SAM flags that must be set. Any record with any
     * of these flags unset will be excluded. Default is not checking any of these flags.
     * @param flags mask indicating flags that must be set.
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder requireSetFlags(final int flags) {
      mRequireSetFlags = flags;
      return this;
    }

    /**
     * Should duplicates be detected and removed. Default is true.
     * @param val true to exclude detected duplicates
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder findAndRemoveDuplicates(final boolean val) {
      mFindAndRemoveDuplicates = val;
      return this;
    }

    /**
     * Sets a restriction on the records that will be processed. The format is a reference sequence name,
     * followed by an optional range specification. Only records that match the reference name and start
     * within the range (if specified) will be processed. A name of null indicates no filtering.
     * @param restriction a reference sequence name
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder restriction(final String restriction) {
      if (restriction == null) {
        mRestriction = null;
      } else  {
        mRestriction = new SamRegionRestriction(restriction);
      }
      return this;
    }

    /**
     * Sets a restriction on the records that will be processed. The format is a reference sequence name,
     * followed by an optional range specification. Only records that match the reference name and start
     * within the range (if specified) will be processed. A name of null indicates no filtering.
     * @param restriction a reference sequence name
     * @return this builder, so calls can be chained.
     */
    public SamFilterParamsBuilder restriction(final SamRegionRestriction restriction) {
      mRestriction = restriction;
      return this;
    }

    /**
     * Set the bed file to use which specifies regions
     * @param bedRegionsFile the bed file which specifies regions
     * @return this builder
     */
    public SamFilterParamsBuilder bedRegionsFile(File bedRegionsFile) {
      mBedRegionsFile = bedRegionsFile;
      return this;
    }

    /**
     * Create the immutable version.
     * @return a <code>SamFilterParams</code> value
     */
    public SamFilterParams create() {
      return new SamFilterParams(this);
    }
  }

  private final int mMaxAlignmentCount;
  private final int mMinMapQ;
  private final IntegerOrPercentage mMaxASMatedValue;
  private final IntegerOrPercentage mMaxASUnmatedValue;
  private final int mRequireUnsetFlags;
  private final int mRequireSetFlags;
  private final boolean mExcludeUnmated;
  private final boolean mExcludeUnplaced;
  private final boolean mFindAndRemoveDuplicates; //detected version
  private final boolean mExcludeVariantInvalid;
  private final Double mSubsampleFraction;
  private final Double mSubsampleRampFraction;
  private final long mSubsampleSeed;
  private final boolean mInvertFilters;

  private final SamRegionRestriction mRestriction;
  private final File mBedRegionsFile;

  /**
   * @param builder the builder object.
   */
  public SamFilterParams(SamFilterParamsBuilder builder) {
    mMinMapQ = builder.mMinMapQ;
    mSubsampleFraction = builder.mSubsampleFraction;
    mSubsampleRampFraction = builder.mSubsampleRampFraction;
    mSubsampleSeed = builder.mSubsampleSeed;
    mMaxAlignmentCount = builder.mMaxAlignmentCount;
    mMaxASMatedValue = builder.mMaxASMatedValue;
    mMaxASUnmatedValue = builder.mMaxASUnmatedValue;
    mExcludeUnplaced = builder.mExcludeUnplaced;
    mExcludeUnmated = builder.mExcludeUnmated; // Can't be done via requireUnset/requireSet. Needs to select reads that are !unmapped and !properpair
    mExcludeVariantInvalid = builder.mExcludeVariantInvalid;

    int requireUnsetFlags = builder.mRequireUnsetFlags;
    if (builder.mExcludeDuplicates) {
      requireUnsetFlags |= SamBamConstants.SAM_PCR_OR_OPTICAL_DUPLICATE;
    }
    if (builder.mExcludeMated) {
      requireUnsetFlags |= SamBamConstants.SAM_READ_IS_MAPPED_IN_PROPER_PAIR;
    }
    if (builder.mExcludeUnmapped) {
      requireUnsetFlags |= SamBamConstants.SAM_READ_IS_UNMAPPED;
    }
    mRequireUnsetFlags = requireUnsetFlags;
    mRequireSetFlags = builder.mRequireSetFlags;

    final int badFlags = mRequireUnsetFlags & mRequireSetFlags;
    if (badFlags != 0) {
      throw new InvalidParamsException("Conflicting SAM FLAG criteria that no record can meet: " + badFlags);
    }

    mInvertFilters = builder.mInvertFilters;

    mRestriction = builder.mRestriction;
    mBedRegionsFile = builder.mBedRegionsFile;
    mFindAndRemoveDuplicates = builder.mFindAndRemoveDuplicates;
  }

  /**
   * @return true if current configuration results in exclusion of records
   */
  public boolean isFiltering() {
    return mMinMapQ != -1
      || mSubsampleFraction != null
      || mSubsampleRampFraction != null
      || mMaxAlignmentCount != -1
      || mMaxASMatedValue != null
      || mMaxASUnmatedValue != null
      || mExcludeUnplaced
      || mExcludeUnmated
      || mExcludeVariantInvalid
      || mRequireUnsetFlags != 0
      || mRequireSetFlags != 0
      || mInvertFilters
      || mRestriction != null
      || mBedRegionsFile != null
      || mFindAndRemoveDuplicates;
  }


  /**
   * @return true if final filter status should be inverted
   */
  public boolean invertFilters() {
    return mInvertFilters;
  }

  /**
   * Gets the proportion of alignments that should be retained after subsampling.
   * Null indicates no subsampling should be performed.
   * @return the fraction of alignments to retain. E.g. 0.33 will subsample alignments to retain 1/3
   */
  public Double subsampleFraction() {
    return mSubsampleFraction;
  }

  /**
   * Gets the proportion of alignments that should be retained after subsampling, at the end of the subsample ramp.
   * Null indicates no subsample ramping should be performed.
   * @return the fraction of alignments to retain. E.g. 0.33 will subsample alignments to retain 1/3
   */
  public Double subsampleRampFraction() {
    return mSubsampleRampFraction;
  }

  /**
   * @return the seed used during subsampling (actually a salt)
   */
  public long subsampleSeed() {
    return mSubsampleSeed;
  }


  /**
   * Mated SAM records having alignment score over this value will be filtered.
   * @return the maximum score or null for no filtering
   */
  public IntegerOrPercentage maxMatedAlignmentScore() {
    return mMaxASMatedValue;
  }

  /**
   * Unmated SAM records having alignment score over this value will be filtered.
   * @return the maximum score or null for no filtering
   */
  public IntegerOrPercentage maxUnmatedAlignmentScore() {
    return mMaxASUnmatedValue;
  }

  /**
   * SAM records having MAPQ less than this value will be filtered.
   * @return the minimum MAPQ permitted or -1 for no filtering
   */
  public int minMapQ() {
    return mMinMapQ;
  }

  /**
   * SAM records having alignment count over this value will be filtered.
   * @return the maximum count or -1 for no filtering
   */
  public int maxAlignmentCount() {
    return mMaxAlignmentCount;
  }

  /**
   * True if unmated SAM records should be excluded.
   * @return exclusion status
   */
  public boolean excludeUnmated() {
    return mExcludeUnmated;
  }

  /**
   * True if NH=0 and other records invalid for the variant caller should be excluded.
   * @return exclusion status
   */
  public boolean excludeVariantInvalid() {
    return mExcludeVariantInvalid;
  }

  /**
   * True if SAM records without an alignment start position should be excluded.
   * @return exclusion status
   */
  public boolean excludeUnplaced() {
    return mExcludeUnplaced;
  }

  /**
   * A mask indicating SAM flags that must be unset. Any record with any
   * of these flags set will be excluded. Default is not checking any of these flags.
   * @return mask indicating flags that must be unset.
   */
  public int requireUnsetFlags() {
    return mRequireUnsetFlags;
  }

  /**
   * A mask indicating SAM flags that must be set. Any record with any
   * of these flags unset will be excluded. Default is not checking any of these flags.
   * @return mask indicating flags that must be set.
   */
  public int requireSetFlags() {
    return mRequireSetFlags;
  }

  /**
   * Should duplicates be detected and removed.
   * @return true to exclude detected duplicates
   */
  public boolean findAndRemoveDuplicates() {
    return mFindAndRemoveDuplicates;
  }

  /**
   * @return The region to restrict iteration of the SAM record to.
   */
  public SamRegionRestriction restriction() {
    return mRestriction;
  }

  /**
   * Returns reference sequence name to filter on.  Null means no filtering.
   * @return reference sequence name to use
   */
  public String restrictionTemplate() {
    return mRestriction == null ? null : mRestriction.getSequenceName();
  }

  /**
   * Gets the zero based start position of restricted calling. -1 means no position filtering.
   *
   * @return the zero based restriction start position inclusive.
   */
  public int restrictionStart() {
    return mRestriction == null ? -1 : mRestriction.getStart();
  }

  /**
   * Gets the zero based exclusive end position of restricted calling.
   *
   * @return the zero based exclusive restriction end position.
   */
  public int restrictionEnd() {
    return mRestriction == null ? -1 : mRestriction.getEnd();
  }


  /**
   * @return a bed file containing the regions to process, or null for no bed region based filtering.
   */
  public File bedRegionsFile() {
    return mBedRegionsFile;
  }

  @Override
  public String toString() {
    return "SamFilterParams"
      + " minMapQ=" + minMapQ()
      + " maxAlignmentCount=" + maxAlignmentCount()
      + " maxMatedAlignmentScore=" + maxMatedAlignmentScore()
      + " maxUnmatedAlignmentScore=" + maxUnmatedAlignmentScore()
      + " excludeUnmated=" + excludeUnmated()
      + " excludeUnplaced=" + excludeUnplaced()
      + " excludeVariantInvalid=" + excludeVariantInvalid()
      + " requireSetFlags=" + requireSetFlags()
      + " requireUnsetFlags=" + requireUnsetFlags()
      + (subsampleFraction() == null ? "" : " subsampleFraction=" + subsampleFraction() + " subsampleSeed=" + subsampleSeed())
      + " regionTemplate=" + restrictionTemplate();
  }

}
