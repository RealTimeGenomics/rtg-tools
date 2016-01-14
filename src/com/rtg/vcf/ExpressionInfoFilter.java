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

package com.rtg.vcf;

import java.util.List;

import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * A VCF info filter constructed from a somewhat general expression.
 */
public class ExpressionInfoFilter extends VcfInfoFilter {

  private final String mField;
  private final Operation<?> mOp;
  private final Object mValue;

  ExpressionInfoFilter(final VcfFilterStatistics stats, final String expression) {
    super(stats, VcfFilterStatistics.Stat.USER_EXPRESSION_COUNT);
    final String expr = expression.replace(" ", "");
    int k = 0;
    while (k < expr.length() && "!=<>".indexOf(expr.charAt(k)) == -1) {
      k++;
    }
    final int opStart = k;
    mField = expr.substring(0, opStart);
    if (mField.isEmpty()) {
      throw new NoTalkbackSlimException("Could not parse field in: " + expression);
    }
    do {
      k++;
    } while (k < expr.length() && "!=<>".indexOf(expr.charAt(k)) >= 0);
    if (opStart >= expr.length()) {
      throw new NoTalkbackSlimException("No operator found in: " + expression);
    }
    final String operator = expr.substring(opStart, k);
    final String value = expr.substring(k);
    Object tempValue;
    try {
      tempValue = Double.valueOf(value);
    } catch (final NumberFormatException e) {
      tempValue = value;
    }
    mValue = tempValue;
    mOp = ExpressionSampleFilter.selectOp(operator, mValue);
  }

  @Override
  boolean acceptCondition(VcfRecord record) {
    final List<String> vals = record.getInfo().get(mField);
    if (vals != null) {
      for (final String val : vals) {
        if (mValue instanceof Double) {
          final Double v = Double.valueOf(val);
          if (((OperationDouble) mOp).compare(v, (Double) mValue)) {
            return true;
          }
        } else if (((OperationObject) mOp).compare(val, mValue)) {
          return true;
        }
      }
    }
    return false;
  }
}
