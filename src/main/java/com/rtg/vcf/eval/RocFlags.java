/*
 * Copyright (c) 2025. Real Time Genomics Limited.
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

import static com.rtg.util.cli.CommonFlagCategories.REPORTING;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.annotation.DerivedAnnotations;

/**
 * Utilities for dealing with ROC file related command line flags
 */
public final class RocFlags {

  /** Flag used for setting the score field */
  public static final String SCORE_FIELD = "vcf-score-field";
  /** Flag used to specify a "good" score is high vs low */
  public static final String SORT_ORDER = "sort-order";
  /** Flag used to indicate that ROCs are not required */
  public static final String NO_ROC = "no-roc";

  /** Flag used to select best metrics by sensitivity */
  public static final String CRITERIA_SENSITIVITY = "at-sensitivity";
  /** Flag used to select best metrics by precision */
  public static final String CRITERIA_PRECISION = "at-precision";
  /** Flag used to select best metrics by score */
  public static final String CRITERIA_SCORE = "at-score";

  /** Flag used to select preset ROC subsets */
  public static final String ROC_SUBSET = "roc-subset";
  /** Flag used to create an expression-based ROC subset */
  public static final String ROC_EXPR = "roc-expr";
  /** Flag used to create an regions-file-based ROC subset */
  public static final String ROC_REGIONS = "roc-regions";
  /** Flag used to enable generating rocs based on cross-join of subsets */
  public static final String ROC_CROSS_JOIN = "roc-cross-join";

  static final String SLOPE_FILES = "Xslope-files";

  private RocFlags() { }

  /**
   * @param flags object to register flags into
   * @param defaultScoreField default value for the score field flag
   * @param rocfilterclass supplier of preset roc subsets
   * @param <T> type of class providing roc filter subsets
   */
  public static <T extends RocFilterProxy> void registerVcfRocFlags(CFlags flags, String defaultScoreField, Class<T> rocfilterclass) {
    flags.registerOptional('f', SCORE_FIELD, String.class, CommonFlags.STRING, "the name of the VCF FORMAT field to use as the ROC score. Also valid are \"QUAL\", \"INFO.<name>\" or \"FORMAT.<name>\" to select the named VCF FORMAT or INFO field", defaultScoreField).setCategory(REPORTING);
    flags.registerOptional('O', SORT_ORDER, RocSortOrder.class, CommonFlags.STRING, "the order in which to sort the ROC scores so that \"good\" scores come before \"bad\" scores", RocSortOrder.DESCENDING).setCategory(REPORTING);
    if (rocfilterclass != null) {
      flags.registerOptional(ROC_SUBSET, rocfilterclass, CommonFlags.STRING, "output ROC file for preset variant subset").setMaxCount(Integer.MAX_VALUE).enableCsv().setCategory(REPORTING);
    }
    flags.registerOptional(ROC_EXPR, String.class, CommonFlags.STRING, "output ROC file for variants matching custom JavaScript expression. Use the form <LABEL>=<EXPRESSION>").setMaxCount(Integer.MAX_VALUE).setCategory(REPORTING);
    flags.registerOptional(ROC_REGIONS, String.class, CommonFlags.STRING, "output ROC file for variants overlapping custom regions supplied in BED file. Use the form <LABEL>=<FILENAME>").setMaxCount(Integer.MAX_VALUE).setCategory(REPORTING);
    flags.registerOptional(ROC_CROSS_JOIN, "output ROC files for each intersection of --" + ROC_SUBSET + ", --" + ROC_EXPR + ", and --" + ROC_REGIONS + " values provided").setCategory(REPORTING);
    flags.registerOptional(CRITERIA_PRECISION, Double.class, CommonFlags.FLOAT, "output summary statistics where precision >= supplied value (Default is to summarize at maximum F-measure)").setCategory(REPORTING);
    flags.registerOptional(CRITERIA_SENSITIVITY, Double.class, CommonFlags.FLOAT, "output summary statistics where sensitivity >= supplied value (Default is to summarize at maximum F-measure)").setCategory(REPORTING);
    flags.registerOptional(CRITERIA_SCORE, Double.class, CommonFlags.FLOAT, "output summary statistics where score meets supplied value (Default is to summarize at maximum F-measure)").setCategory(REPORTING);
    flags.registerOptional(SLOPE_FILES, "output files for ROC slope analysis").setCategory(REPORTING);
  }

  /**
   * @param flags object to register flags into
   * @return true if the standard ROC flags validate successfully
   */
  public static boolean validateVcfRocFlags(CFlags flags) {
      return validateScoreField(flags)
      && flags.checkNand(SCORE_FIELD, NO_ROC)
      && validateVcfRocFlag(flags, ROC_EXPR)
      && validateVcfRocFlag(flags, ROC_REGIONS)
      && flags.checkAtMostOne(CRITERIA_PRECISION, CRITERIA_SENSITIVITY, CRITERIA_SCORE)
      && flags.checkInRange(CRITERIA_PRECISION, 0.0, 1.0)
      && flags.checkInRange(CRITERIA_SENSITIVITY, 0.0, 1.0);
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

  /**
   * Helper method to construct roc point criteria from flags
   * @param flags the flags with options set
   * @param sortOrder the sort order used to order points on the ROC curve
   * @return the roc point criteria implementation
   */
  public static RocPointCriteria getRocPointCriteria(CFlags flags, RocSortOrder sortOrder) {
    final RocPointCriteria criteria;
    if (flags.isSet(CRITERIA_PRECISION)) {
      criteria = new PrecisionThreshold((Double) flags.getValue(CRITERIA_PRECISION));
    } else if (flags.isSet(CRITERIA_SENSITIVITY)) {
      criteria = new SensitivityThreshold((Double) flags.getValue(CRITERIA_SENSITIVITY));
    } else if (flags.isSet(CRITERIA_SCORE)) {
      criteria = new FixedScoreThreshold((Double) flags.getValue(CRITERIA_SCORE), sortOrder.comparator());
    } else {
      criteria = new FMeasureThreshold();
    }
    return criteria;
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
      final List<?> values = flags.getValues(ROC_EXPR);
      for (Object o : values) {
        final String[] e = StringUtils.split((String) o, '=', 2);
        assert e.length == 2;
        rocExprFilters.add(new ExpressionRocFilter(e[0], e[1]));
      }
    }
    rocFiltersTypeMap.put(ROC_EXPR, rocExprFilters);

    final Set<RocFilter> rocRegionsFilters = new LinkedHashSet<>();
    if (flags.isSet(ROC_REGIONS)) {
      final List<?> values = flags.getValues(ROC_REGIONS);
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
    if (flags.isSet(ROC_SUBSET)) {
      final List<?> values = flags.getValues(ROC_SUBSET);
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
