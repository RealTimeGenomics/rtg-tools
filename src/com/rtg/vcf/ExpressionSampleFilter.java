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
 * numerical comparision like <code>OAF&gt;0.03</code>.
 */
public class ExpressionSampleFilter extends VcfSampleFilter {

  private enum Operation {
    EQ {
      @Override
      public boolean compare(double a, double b) {
        return a == b;
      }
    },
    NE {
      @Override
      public boolean compare(double a, double b) {
        return a != b;
      }
    },
    LT {
      @Override
      public boolean compare(double a, double b) {
        return a < b;
      }
    },
    GT {
      @Override
      public boolean compare(double a, double b) {
        return a > b;
      }
    },
    LE {
      @Override
      public boolean compare(double a, double b) {
        return a <= b;
      }
    },
    GE {
      @Override
      public boolean compare(double a, double b) {
        return a >= b;
      }
    };

    protected abstract boolean compare(final double a, final double b);
  }

  private final String mField;
  private final Operation mOp;
  private final double mValue;

  ExpressionSampleFilter(final VcfFilterStatistics stats, final String expression) {
    super(stats, VcfFilterStatistics.Stat.USER_EXPRESSION_COUNT);
    final String expr = expression.replace(" ", "");
    int k = 0;
    while ("!=<>".indexOf(expr.charAt(k)) == -1) {
      k++;
    }
    final int opStart = k;
    mField = expr.substring(0, opStart);
    do {
      k++;
    } while ("!=<>".indexOf(expr.charAt(k)) >= 0);
    final String operator = expr.substring(opStart, k);
    switch (operator) {
      case "=":
      case "==":
        mOp = Operation.EQ;
        break;
      case "!=":
      case "<>":
        mOp = Operation.NE;
        break;
      case "<":
        mOp = Operation.LT;
        break;
      case ">":
        mOp = Operation.GT;
        break;
      case "<=":
        mOp = Operation.LE;
        break;
      case ">=":
        mOp = Operation.GE;
        break;
      default:
        throw new NoTalkbackSlimException("Invalid operator: " + operator);
    }
    mValue = Double.parseDouble(expr.substring(k));
  }

  @Override
  boolean acceptSample(final VcfRecord record, final int index) {
    final Double val = record.getSampleDouble(index, mField);
    return val != null && mOp.compare(val, mValue);
  }
}
