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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.rtg.util.MathUtils;

/**
 * Mechanism for adjusting fields in VCF records in accordance with a change in allele mapping.
 */
public class Adjuster {

  public enum Policy {
    DROP, SUM
  }

  private final Map<String, Policy> mPolicyMap = new HashMap<>();

  public Adjuster() {
    // todo temporary defaults
    mPolicyMap.put(VcfUtils.FORMAT_ALLELIC_DEPTH, Policy.SUM);
    mPolicyMap.put("ABP", Policy.DROP); // todo xxx temporary just testing functionality
  }

  /**
   * Set the policy for handling a particular format field.
   * @param formatField name of format field
   * @param policy policy (null removes handling)
   */
  public void setPolicy(final String formatField, final Policy policy) {
    if (policy == null) {
      mPolicyMap.remove(formatField);
    } else {
      mPolicyMap.put(formatField, policy);
    }
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
      for (final Map.Entry<String, Policy> entry : mPolicyMap.entrySet()) {
        final String field = entry.getKey();
        final Policy policy = entry.getValue();

        switch (policy) {
          case DROP:
            replacement.removeFormat(field);
            break;
          case SUM:
            final ArrayList<String> originalFieldValues = original.getFormat(field);
            final ArrayList<String> replacementFieldValues = sum(originalFieldValues, alleleMap);
            // If possible use existing field to hold the result, this helps ensure attributes come
            // out in the order the are defined in the replacement (and hence probably the original).
            final ArrayList<String> existing = replacement.getFormat(field);
            if (existing == null) {
              replacement.addFormat(field);
              replacement.getFormat(field).addAll(replacementFieldValues);
            } else {
              existing.clear();
              existing.addAll(replacementFieldValues);
            }
            break;
          default:
            throw new RuntimeException();
        }
      }
    } catch (final RuntimeException e) {
      // todo remove this catch -- only here for testing bogosity
      // todo or perhaps retain but log failing records
      System.err.println(original.toString());
      throw e;
    }
  }

  // todo is the following needed or useful?
  /**
   * Potentially change, delete, or insert VCF FORMAT and INFO fields in accordance with
   * a change of alleles from the original record to the replacement.  This version does
   * in situ replacement.
   * @param record original VCF record
   * @param alleleMap mapping from alleles in original record into replacement record
   */
  public void adjust(final VcfRecord record, final int[] alleleMap) {
    adjust(record, record, alleleMap);
  }


  private ArrayList<String> sum(final ArrayList<String> values, final int[] alleleMap) {
    // Implicit assumption is that values are numeric and "R" type
    // todo "A" type might also be needed
    if (values == null || values.isEmpty()) {
      return values;
    }
    final int newMaxAllele = MathUtils.max(alleleMap);
    final ArrayList<String> res = new ArrayList<>(values.size());
    for (final String fieldForsample : values) {
      if (VcfUtils.MISSING_FIELD.equals(fieldForsample)) {
        res.add(VcfUtils.MISSING_FIELD);
      } else {
        final String[] parts = fieldForsample.split(",");
        final long[] sums = new long[newMaxAllele + 1];
        for (int k = 0; k < parts.length; ++k) {
          final int newAllele = alleleMap[k];
          if (newAllele != VcfUtils.MISSING_GT) {
            sums[newAllele] += Long.parseLong(parts[k]);
          }
        }
        final StringBuilder sb = new StringBuilder();
        for (final long v : sums) {
          if (sb.length() > 0) {
            sb.append(',');
          }
          sb.append(v);
        }
        res.add(sb.toString());
      }
    }
    return res;
  }
}
