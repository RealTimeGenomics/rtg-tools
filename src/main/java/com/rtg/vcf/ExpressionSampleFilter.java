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

import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * A VCF sample filter constructed from a somewhat general expression supporting simple
 * numerical comparison like <code>OAF&gt;0.03</code>.
 */
public class ExpressionSampleFilter extends VcfSampleFilter {

  private final String mField;
  private final Operation<?> mOp;
  private final Object mValue;

  static Operation<?> selectOp(final String operator, final Object mValue) {
    if (mValue instanceof Double) {
      switch (operator) {
        case "=":
        case "==":
          return OperationDouble.EQ;
        case "!=":
        case "<>":
          return OperationDouble.NE;
        case "<":
          return OperationDouble.LT;
        case ">":
          return OperationDouble.GT;
        case "<=":
          return OperationDouble.LE;
        case ">=":
          return OperationDouble.GE;
        default:
          throw new NoTalkbackSlimException("Invalid operator: " + operator);
      }
    } else {
      switch (operator) {
        case "=":
        case "==":
          return OperationObject.EQ;
        case "!=":
        case "<>":
          return OperationObject.NE;
        default:
          throw new NoTalkbackSlimException("Invalid operator: " + operator);
      }
    }
  }

  ExpressionSampleFilter(final VcfFilterStatistics stats, final String expression) {
    super(stats, VcfFilterStatistics.Stat.USER_EXPRESSION_COUNT);
    final String expr = expression.replace(" ", "");
    int k = 0;
    while (k < expr.length() && "!=<>".indexOf(expr.charAt(k)) == -1) {
      ++k;
    }
    final int opStart = k;
    mField = expr.substring(0, opStart);
    if (mField.isEmpty()) {
      throw new NoTalkbackSlimException("Could not parse field in: " + expression);
    }
    do {
      ++k;
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
    mOp = selectOp(operator, mValue);
  }

  @Override
  boolean acceptSample(final VcfRecord record, final int index) {
    if (mValue instanceof Double) {
      final Double val = record.getSampleDouble(index, mField);
      return val != null && ((OperationDouble) mOp).compare(val, (Double) mValue);
    } else {
      final String val = record.getSampleString(index, mField);
      return val != null && ((OperationObject) mOp).compare(val, mValue);
    }
  }
}
