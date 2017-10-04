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

import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.SENSITIVITY_TUNING;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.IntegerOrPercentage;
import com.rtg.util.PortableRandom;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Constants and utility methods for command line flags for filtering SAM records.
 */
public final class SamFilterOptions {

  private SamFilterOptions() { }

  /** Flag name for filter on <code>IH</code> or <code>NH</code> attribute of SAM record. */
  public static final String MAX_HITS_FLAG = "max-hits";

  /** Used for a flag not having a single letter command. */
  public static final char NO_SINGLE_LETTER = '\0';

  private static final String HITS_DESC = "if set, ignore SAM records with an alignment count that exceeds this value";

  /**
   * Register flag for filtering on the number of hits.
   * @param flags flags to add into
   * @param singleLetter single letter code for option, or 0 for no single letter option
   * @return the flag
   */
  public static Flag<Integer> registerMaxHitsFlag(final CFlags flags, final char singleLetter) {
    if (singleLetter != NO_SINGLE_LETTER) {
      return flags.registerOptional(singleLetter, MAX_HITS_FLAG, Integer.class, CommonFlags.INT, HITS_DESC).setCategory(SENSITIVITY_TUNING);
    } else {
      return flags.registerOptional(MAX_HITS_FLAG, Integer.class, CommonFlags.INT, HITS_DESC).setCategory(SENSITIVITY_TUNING);
    }
  }
  
  /** Flag name for enabling subsampling records. */
  public static final String SUBSAMPLE_FLAG = "subsample";

  /** Flag name for specifying the seed when subsampling. */
  public static final String SUBSAMPLE_SEED_FLAG = "seed";

  /**
   * Register flags for subsampling
   *
   * @param flags flags to add into
   */
  public static void registerSubsampleFlags(final CFlags flags) {
    flags.registerOptional(SUBSAMPLE_FLAG, Double.class, CommonFlags.FLOAT, "if set, subsample the input to retain this fraction of reads").setCategory(UTILITY);
    flags.registerOptional(SUBSAMPLE_SEED_FLAG, Integer.class, CommonFlags.INT, "seed used during subsampling").setCategory(UTILITY);
  }

  /** Flag name for inverting flag and attribute filter criteria. */
  public static final String INVERT_FLAG = "invert";

  /**
   * Register flag for inverting filtering criteria
   * @param flags flags to add into
   */
  public static void registerInvertCriteriaFlag(final CFlags flags) {
    flags.registerOptional(INVERT_FLAG, "if set, invert the result of flag and attribute based filter criteria").setCategory(SENSITIVITY_TUNING);
  }

  /** Flag name for filter on <code>MAPQ</code> field of SAM record. */
  public static final String MIN_MAPQ_FLAG = "min-mapq";

  private static final String MAPQ_DESC = "if set, ignore SAM records with MAPQ less than this value";

  /**
   * Register flag for filtering on the MAPQ
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Integer> registerMinMapQFlag(final CFlags flags) {
    return flags.registerOptional(MIN_MAPQ_FLAG, Integer.class, CommonFlags.INT, MAPQ_DESC).setCategory(SENSITIVITY_TUNING);
  }

  /** Flag name for filter of <code>AS</code> attribute of mated SAM records. */
  public static final String MAX_AS_MATED_FLAG = "max-as-mated";

  private static final String AS_MATED_DESC = "if set, ignore mated SAM records with an alignment score (AS attribute) that exceeds this value";

  /**
   * Register flag for mated <code>AS</code> filtering.
   * @param flags flags to add into
   * @param singleLetter single letter code for option, or 0 for no single letter option
   * @return the flag
   */
  public static Flag<IntegerOrPercentage> registerMaxASMatedFlag(final CFlags flags, final char singleLetter) {
    if (singleLetter != NO_SINGLE_LETTER) {
      return flags.registerOptional(singleLetter, MAX_AS_MATED_FLAG, IntegerOrPercentage.class, CommonFlags.INT, AS_MATED_DESC).setCategory(SENSITIVITY_TUNING);
    } else {
      return flags.registerOptional(MAX_AS_MATED_FLAG, IntegerOrPercentage.class, CommonFlags.INT, AS_MATED_DESC).setCategory(SENSITIVITY_TUNING);
    }
  }

  /** Flag name for filter of <code>AS</code> attribute of unmated SAM records. */
  public static final String MAX_AS_UNMATED_FLAG = "max-as-unmated";

  private static final String AS_UNMATED_DESC = "if set, ignore unmated SAM records with an alignment score (AS attribute) that exceeds this value";

  /**
   * Register flag for unmated <code>AS</code> filtering.
   * @param flags flags to add into
   * @param singleLetter single letter code for option, or 0 for no single letter option
   * @return the flag
   */
  public static Flag<IntegerOrPercentage> registerMaxASUnmatedFlag(final CFlags flags, final char singleLetter) {
    if (singleLetter != NO_SINGLE_LETTER) {
      return flags.registerOptional(singleLetter, MAX_AS_UNMATED_FLAG, IntegerOrPercentage.class, CommonFlags.INT, AS_UNMATED_DESC).setCategory(SENSITIVITY_TUNING);
    } else {
      return flags.registerOptional(MAX_AS_UNMATED_FLAG, IntegerOrPercentage.class, CommonFlags.INT, AS_UNMATED_DESC).setCategory(SENSITIVITY_TUNING);
    }
  }

  /** Flag name for filtering out mated results. */
  public static final String EXCLUDE_MATED_FLAG = "exclude-mated";

  private static final String EXCLUDE_MATED_DESC = "exclude all mated SAM records";

  /**
   * Register flag for excluding mated results.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Boolean> registerExcludeMatedFlag(final CFlags flags) {
    return flags.registerOptional(EXCLUDE_MATED_FLAG, EXCLUDE_MATED_DESC).setCategory(SENSITIVITY_TUNING);
  }

  /** Flag name for filtering out unmated results. */
  public static final String EXCLUDE_UNMATED_FLAG = "exclude-unmated";

  private static final String EXCLUDE_UNMATED_DESC = "exclude all unmated SAM records";

  /**
   * Register flag for excluding unmated results.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Boolean> registerExcludeUnmatedFlag(final CFlags flags) {
    return flags.registerOptional(EXCLUDE_UNMATED_FLAG, EXCLUDE_UNMATED_DESC).setCategory(SENSITIVITY_TUNING);
  }

  /** Flag name to enable filtering out unmapped results. */
  public static final String EXCLUDE_UNMAPPED_FLAG = "exclude-unmapped";

  private static final String EXCLUDE_UNMAPPED_DESC = "exclude all unmapped SAM records";

  /**
   * Register flag for excluding unmapped results.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Boolean> registerExcludeUnmappedFlag(final CFlags flags) {
    return flags.registerOptional(EXCLUDE_UNMAPPED_FLAG, EXCLUDE_UNMAPPED_DESC).setCategory(SENSITIVITY_TUNING);
  }

  /** Flag name to enable filtering out unplaced results. */
  public static final String EXCLUDE_UNPLACED_FLAG = "exclude-unplaced";

  private static final String EXCLUDE_UNPLACED_DESC = "exclude all SAM records with no alignment position";

  /**
   * Register flag for excluding unmapped results.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Boolean> registerExcludeUnplacedFlag(final CFlags flags) {
    return flags.registerOptional(EXCLUDE_UNPLACED_FLAG, EXCLUDE_UNPLACED_DESC).setCategory(SENSITIVITY_TUNING);
  }

  /** Flag name to enable filtering out duplicate results. */
  public static final String EXCLUDE_DUPLICATES_FLAG = "exclude-duplicates";

  private static final String EXCLUDE_DUPLICATES_DESC = "exclude all SAM records flagged as a PCR or optical duplicate";

  /**
   * Register flag for excluding duplicate results.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Boolean> registerExcludeDuplicatesFlag(final CFlags flags) {
    return flags.registerOptional(EXCLUDE_DUPLICATES_FLAG, EXCLUDE_DUPLICATES_DESC).setCategory(SENSITIVITY_TUNING);
  }

  // this options differs from the above as it refers to the mechanism of detecting and removing duplicates on the fly, as well as looking at the SAM records FLAG field
  /** keep duplicates flag constant */
  public static final String KEEP_DUPLICATES_FLAG = "keep-duplicates";

  private static final String KEEP_DUPLICATES_DESC = "don't detect and filter duplicate reads based on mapping position";

  /**
   * Register flag for keeping duplicate results.
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<Boolean> registerKeepDuplicatesFlag(final CFlags flags) {
    return flags.registerOptional(KEEP_DUPLICATES_FLAG, KEEP_DUPLICATES_DESC).setCategory(SENSITIVITY_TUNING);
  }

  private static final String RESTRICTION_DESC = "if set, only process SAM records within the specified range. The format is one of <sequence_name>, <sequence_name>:start-end or <sequence_name>:start+length";

  /**
   * Register flag for restricting records to be processed.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<String> registerRestrictionFlag(final CFlags flags) {
    return flags.registerOptional(CommonFlags.RESTRICTION_FLAG, String.class, CommonFlags.STRING, RESTRICTION_DESC).setCategory(INPUT_OUTPUT);
  }

  /**
   * Register flag for restricting records to be processed.
   *
   * @param flags flags to add into
   * @return the flag
   */
  public static Flag<File> registerBedRestrictionFlag(final CFlags flags) {
    return flags.registerOptional(CommonFlags.BED_REGIONS_FLAG, File.class, CommonFlags.FILE, "if set, only read SAM records that overlap the ranges contained in the specified BED file").setCategory(INPUT_OUTPUT);
  }


  private static final String REQUIRE_FLAGS = "require-flags";
  private static final String FILTER_FLAGS = "filter-flags";

  /**
   * Register flags for restricting directly based on flags.
   * @param flags the flags to add in to
   */
  public static void registerMaskFlags(CFlags flags) {
    flags.registerOptional('f', REQUIRE_FLAGS, Integer.class, CommonFlags.INT, "decimal mask indicating SAM FLAG bits that must be set for the record").setCategory(SENSITIVITY_TUNING);
    flags.registerOptional('F', FILTER_FLAGS, Integer.class, CommonFlags.INT, "decimal mask indicating SAM FLAG bits that must not be set for the record").setCategory(SENSITIVITY_TUNING);
  }

  /**
   * Validate the filter flag input, will produce the appropriate
   * diagnostic error when validation fails.
   *
   * @param flags the flags object to check
   * @param allowUnmappedOnly true if the user is allowed to exclude both mated and unmated alignments
   * @return true if the provided flags are valid, false otherwise
   */
  public static boolean validateFilterFlags(final CFlags flags, boolean allowUnmappedOnly) {
    if (!flags.checkInRange(MAX_HITS_FLAG, 1, Integer.MAX_VALUE)
      || !flags.checkInRange(SUBSAMPLE_FLAG, 0.0, 1.0)
      || !flags.checkInRange(MIN_MAPQ_FLAG, 1, Integer.MAX_VALUE)) {
      return false;
    }
    if (flags.isSet(MAX_AS_MATED_FLAG)) {
      final IntegerOrPercentage maxMated = (IntegerOrPercentage) flags.getValue(MAX_AS_MATED_FLAG);
      if (maxMated.getValue(100) < 0) {
        flags.setParseMessage("The value for --" + MAX_AS_MATED_FLAG + " must be at least 0");
        return false;
      }
    }
    if (flags.isSet(MAX_AS_UNMATED_FLAG)) {
      final IntegerOrPercentage maxUnmated = (IntegerOrPercentage) flags.getValue(MAX_AS_UNMATED_FLAG);
      if (maxUnmated.getValue(100) < 0) {
        flags.setParseMessage("The value for --" + MAX_AS_UNMATED_FLAG + " must be at least 0");
        return false;
      }
    }
    if (!(allowUnmappedOnly || flags.checkNand(EXCLUDE_MATED_FLAG, EXCLUDE_UNMATED_FLAG))) {
      return false;
    }

    if (flags.isSet(FILTER_FLAGS) && flags.isSet(REQUIRE_FLAGS)) {
      final int unset = (Integer) flags.getValue(FILTER_FLAGS);
      final int set = (Integer) flags.getValue(REQUIRE_FLAGS);
      final int badFlags = unset & set;
      if (badFlags != 0) {
        flags.setParseMessage("--" + FILTER_FLAGS + " and --" + REQUIRE_FLAGS + " have conflicting values. Flags in common: " + badFlags);
      }
    }

    if (!CommonFlags.validateRegions(flags)) {
      return false;
    }

    if (flags.getFlag(KEEP_DUPLICATES_DESC) != null
        && flags.getFlag(EXCLUDE_DUPLICATES_FLAG) != null) {
      throw new RuntimeException("Cannot have registered flags for both include and exclude duplicates");
    }
    return true;
  }

  /**
   * Build parameters from flags.
   * @param flags command line flags
   * @return parameters
   */
  public static SamFilterParams.SamFilterParamsBuilder makeFilterParamsBuilder(final CFlags flags) {
    final SamFilterParams.SamFilterParamsBuilder builder = SamFilterParams.builder();
    builder.invertFilters(flags.isSet(INVERT_FLAG));
    if (flags.isSet(MAX_HITS_FLAG)) {
      builder.maxAlignmentCount((Integer) flags.getValue(MAX_HITS_FLAG));
    }
    if (flags.isSet(MIN_MAPQ_FLAG)) {
      builder.minMapQ((Integer) flags.getValue(MIN_MAPQ_FLAG));
    }
    if (flags.isSet(SUBSAMPLE_FLAG)) {
      builder.subsampleFraction((Double) flags.getValue(SUBSAMPLE_FLAG));
    }
    if (flags.isSet(SUBSAMPLE_SEED_FLAG)) {
      builder.subsampleSeed((Integer) flags.getValue(SUBSAMPLE_SEED_FLAG));
    } else {
      builder.subsampleSeed(new PortableRandom().nextLong());
    }
    if (flags.isSet(MAX_AS_MATED_FLAG)) {
      final IntegerOrPercentage matedAS = (IntegerOrPercentage) flags.getValue(MAX_AS_MATED_FLAG);
      builder.maxMatedAlignmentScore(matedAS);
      if (flags.isSet(MAX_AS_UNMATED_FLAG)) {
        final IntegerOrPercentage unmatedAS = (IntegerOrPercentage) flags.getValue(MAX_AS_UNMATED_FLAG);
        if (unmatedAS.compareTo(matedAS) > 0) {
          Diagnostic.warning("--" + MAX_AS_UNMATED_FLAG + " should not be greater than --" + MAX_AS_MATED_FLAG);
        }
      }
    }
    if (flags.isSet(MAX_AS_UNMATED_FLAG)) {
      builder.maxUnmatedAlignmentScore((IntegerOrPercentage) flags.getValue(MAX_AS_UNMATED_FLAG));
    }
    builder.excludeMated(flags.isSet(EXCLUDE_MATED_FLAG));
    builder.excludeUnmated(flags.isSet(EXCLUDE_UNMATED_FLAG));
    builder.excludeUnmapped(flags.isSet(SamFilterOptions.EXCLUDE_UNMAPPED_FLAG));
    builder.excludeUnplaced(flags.isSet(SamFilterOptions.EXCLUDE_UNPLACED_FLAG));
    if (flags.isSet(FILTER_FLAGS)) {
      builder.requireUnsetFlags((Integer) flags.getValue(FILTER_FLAGS));
    }
    if (flags.isSet(REQUIRE_FLAGS)) {
      builder.requireSetFlags((Integer) flags.getValue(REQUIRE_FLAGS));
    }

    // Some tools want inclusion by default and some want exclusion by default
    if (flags.getFlag(KEEP_DUPLICATES_FLAG) != null) {
      builder.findAndRemoveDuplicates(!flags.isSet(KEEP_DUPLICATES_FLAG));
      builder.excludeDuplicates(!flags.isSet(KEEP_DUPLICATES_FLAG));
    } else if (flags.getFlag(EXCLUDE_DUPLICATES_FLAG) != null) {
      builder.excludeDuplicates(flags.isSet(EXCLUDE_DUPLICATES_FLAG));
    }

    if (flags.isSet(CommonFlags.RESTRICTION_FLAG)) {
      builder.restriction((String) flags.getValue(CommonFlags.RESTRICTION_FLAG));
    }
    if (flags.isSet(CommonFlags.BED_REGIONS_FLAG)) {
      builder.bedRegionsFile((File) flags.getValue(CommonFlags.BED_REGIONS_FLAG));
    }

    return builder;
  }
}
