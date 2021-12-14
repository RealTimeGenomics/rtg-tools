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
package com.rtg.vcf.eval;

import java.util.Collections;

import com.rtg.vcf.ScriptedVcfFilter;
import com.rtg.vcf.VcfFilter;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.VcfHeader;

/**
 * A ROC filter for variants that match a user-supplied expression
 */
public class ExpressionRocFilter extends RocFilter {

  private final String mExpression;
  private VcfFilter mFilter = null;

  /**
   * Constructor
   * @param name the filter label (used to determine the output file name)
   * @param expression JavaScript expression applied to each VcfRecord
   */
  public ExpressionRocFilter(String name, String expression) {
    super(name);
    mExpression = expression;
  }

  @Override
  public boolean requiresGt() {
    return false;
  }

  @Override
  public void setHeader(VcfHeader header) {
    mFilter = createExpressionFilter(mExpression);
    mFilter.setHeader(header);
  }

  @Override
  public boolean accept(VcfRecord rec, int[] gt) {
    return mFilter.accept(rec);
  }

  private static ScriptedVcfFilter createExpressionFilter(String expression) {
    return new ScriptedVcfFilter(expression, Collections.emptyList(), System.out, System.err);
  }
}
