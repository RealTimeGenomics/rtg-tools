/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.MathUtils;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.TypedField;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import htsjdk.samtools.util.StringUtil;

/**
 * Mechanism for adjusting fields in VCF records in accordance with a change in allele mapping.
 */
public class Adjuster {

  private static final String DEFAULT_CONFIG = "com/rtg/vcf/field_adjustment.properties";

  /** Type of action to take for a particular attribute. */
  public enum Policy {
    /** Attribute should be retained without adjustment. */
    RETAIN,
    /** Attribute should be dropped. */
    DROP,
    /** Corresponding values should be summed (reference + ALTs). */
    SUM,
    ;
  }

  private final Map<String, Policy> mInfoPolicies = new HashMap<>();
  private final Map<String, Policy> mFormatPolicies = new HashMap<>();

  private final VcfHeader mHeader;

  static Properties getConfig() throws IOException {
    final Properties props = new Properties();
    try (Reader r = GlobalFlags.isSet(ToolsGlobalFlags.VCF_FIELD_ADJUSTMENT_CONFIG)
      ? new FileReader(GlobalFlags.getStringValue(ToolsGlobalFlags.VCF_FIELD_ADJUSTMENT_CONFIG))
      : new InputStreamReader(Resources.getResourceAsStream(DEFAULT_CONFIG))) {
      props.load(r);
    }
    return props;
  }

  /**
   * Construct an adjuster.
   * @param header corresponding to input VCF records.
   * @throws IOException if the adjustment configuration is invalid or cannot be read
   */
  public Adjuster(VcfHeader header) throws IOException {
    this(header, getConfig());
  }

  /**
   * Construct an adjuster.
   * @param header corresponding to input VCF records.
   * @param config contains adjustment policy configuration.
   * @throws IOException if the adjustment configuration is invalid or cannot be read
   */
  private Adjuster(VcfHeader header, Properties config) throws IOException {
    mHeader = header;
    for (String annot : config.stringPropertyNames()) {
      final String pname = config.getProperty(annot);
      final Policy p;
      try {
        p = Policy.valueOf(pname);
      } catch (RuntimeException e) {
        throw new IOException("Adjustment policy for " + annot + " uses invalid policy name: " + pname);
      }
      setPolicy(annot, p);
    }
  }

  /**
   * Set the policy for handling a particular format field.
   * @param fieldName name of format field
   * @param policy policy (null removes handling)
   */
  void setPolicy(final String fieldName, final Policy policy) {
    final Map<String, Policy> m;
    final String f;
    if (fieldName.startsWith("INFO.")) {
      m = mInfoPolicies;
      f = fieldName.substring("INFO.".length());
    } else if (fieldName.startsWith("FORMAT.")) {
      m = mFormatPolicies;
      f = fieldName.substring("FORMAT.".length());
    } else {
      throw new IllegalArgumentException("Invalid annotation specifier: " + fieldName);
    }
    if (policy == null) {
      m.remove(f);
    } else {
      m.put(f, policy);
    }
  }

  boolean hasPolicy(FormatField f) {
    return mFormatPolicies.containsKey(f.getId());
  }

  boolean hasPolicy(InfoField f) {
    return mInfoPolicies.containsKey(f.getId());
  }

  /**
   * Potentially change, delete, or insert VCF FORMAT and INFO fields in accordance with
   * a change of alleles from the original record to the replacement.  For example, if
   * alleles have been combined, then it is appropriate to add the AD elements for the
   * corresponding alleles.
   * @param original original VCF record
   * @param replacement replacement VCF record (possibly already populated)
   * @param alleleMap mapping from alleles in original record into replacement record
   */
  public void adjust(final VcfRecord original, final VcfRecord replacement, final int[] alleleMap) {
    try {
      final int numAlleles = MathUtils.max(alleleMap) + 1;
      assert numAlleles == replacement.getAltCalls().size() + 1;

      for (String field : original.getInfo().keySet()) {
        if (mInfoPolicies.containsKey(field)) {
          adjustInfoField(original, replacement, alleleMap, numAlleles, field, mInfoPolicies.get(field));
        }
      }
      for (String field : original.getFormats()) {
        if (mFormatPolicies.containsKey(field)) {
          adjustFormatField(original, replacement, alleleMap, numAlleles, field, mFormatPolicies.get(field));
        }
      }
    } catch (final VcfFormatException e) {
      throw e;
    } catch (final RuntimeException e) {
      // Log any records that lead to exceptions during adjustment
      Diagnostic.userLog("Problem adjusting: " + original.toString());
      throw e;
    }
  }

  private void validateTypedField(TypedField<?> f) {
    final VcfNumber n = f.getNumber();
    if (n != VcfNumber.REF_ALTS && n != VcfNumber.DOT) {
      throw new VcfFormatException("Cannot derive new values for " + f.fieldName() + " " + f.getId() + " declared as Number=" + n);
    }
    final MetaType type = f.getType();
    if (type != MetaType.FLOAT && type != MetaType.INTEGER) {
      throw new VcfFormatException("Cannot derive new values for " + f.fieldName() + " " + f.getId() + " of non-numeric type " + type);
    }
  }

  private void adjustInfoField(VcfRecord original, VcfRecord replacement, int[] alleleMap, int numAlleles, String field, Policy policy) {
    if (original.getInfo().containsKey(field)) {
      switch (policy) {
        case RETAIN:
          replacement.setInfo(field, original.getInfo(field));
          break;
        case DROP:
          replacement.removeInfo(field);
          break;
        case SUM:
          final InfoField f = mHeader.getInfoField(field);
          validateTypedField(f);
          final String[] parts = original.getInfoSplit(field);
          if (parts.length != alleleMap.length) {
            throw new VcfFormatException("INFO field " + field + " was expected to contain " + alleleMap.length + " values");
          }
          try {
            replacement.setInfo(field, sumAdjustField(alleleMap, numAlleles, f.getType(), parts));
          } catch (NumberFormatException e) {
            throw new VcfFormatException(e.getMessage());
          }
          break;
        default:
          throw new RuntimeException();
      }
    }
  }

  private void adjustFormatField(VcfRecord original, VcfRecord replacement, int[] alleleMap, int numAlleles, String field, Policy policy) {
    if (original.hasFormat(field)) {
      switch (policy) {
        case RETAIN:
          replacement.setFormat(field, original.getFormat(field));
          break;
        case DROP:
          replacement.removeFormat(field);
          break;
        case SUM:
          final FormatField f = mHeader.getFormatField(field);
          validateTypedField(f);
          final List<String> replacementFieldValues = sumAdjustFormatSamples(original.getFormat(field), alleleMap, numAlleles, f.getType());
          replacement.setFormat(field, replacementFieldValues);
          break;
        default:
          throw new RuntimeException();
      }
    }
  }


  private List<String> sumAdjustFormatSamples(final List<String> values, final int[] alleleMap, int numAlleles, MetaType type) {
    // Implicit assumption is that values are numeric and "R" type
    if (values == null || values.isEmpty()) {
      return values;
    }
    try {
      return values.stream()
        .map(f -> sumAdjustFormatField(f, alleleMap, numAlleles, type))
        .collect(Collectors.toCollection(ArrayList::new));
    } catch (NumberFormatException e) {
      throw new VcfFormatException(e.getMessage());
    }
  }

  private String sumAdjustFormatField(String fieldValue, int[] alleleMap, int numAlleles, MetaType type) {
    final String newFieldForsample;
    if (VcfUtils.MISSING_FIELD.equals(fieldValue)) {
      newFieldForsample = VcfUtils.MISSING_FIELD;
    } else {
      final String[] parts = StringUtils.split(fieldValue, ',');
      if (parts.length != alleleMap.length) {
        throw new VcfFormatException("FORMAT field value " + fieldValue + " was expected to contain " + alleleMap.length + " values");
      }
      try {
        newFieldForsample = StringUtil.join(",", sumAdjustField(alleleMap, numAlleles, type, parts));
      } catch (NumberFormatException e) {
        throw new VcfFormatException(e.getMessage());
      }
    }
    return newFieldForsample;
  }

  private String[] sumAdjustField(int[] alleleMap, int numAlleles, MetaType type, String[] parts) {
    final String[] adjusted;
    if (type == MetaType.INTEGER) {
      final long[] sums = new long[numAlleles];
      for (int k = 0; k < parts.length; ++k) {
        final int newAllele = alleleMap[k];
        if (newAllele != VcfUtils.MISSING_GT) {
          sums[newAllele] += Long.parseLong(parts[k]);
        }
      }
      adjusted = VcfUtils.formatIntArray(sums);
    } else if (type == MetaType.FLOAT) {
      final double[] sums = new double[numAlleles];
      for (int k = 0; k < parts.length; ++k) {
        final int newAllele = alleleMap[k];
        if (newAllele != VcfUtils.MISSING_GT) {
          sums[newAllele] += Double.parseDouble(parts[k]);
        }
      }
      // We don't currently have a way to specify number of DP.
      adjusted = VcfUtils.formatFloatArray(sums);
    } else {
      throw new RuntimeException();
    }
    return adjusted;
  }
}
