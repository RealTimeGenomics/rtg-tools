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

import com.rtg.util.Utils;
import com.rtg.vcf.annotation.AbstractDerivedAnnotation;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 */
public class VcfInfoDoubleAnnotator implements VcfAnnotator {

  final AbstractDerivedAnnotation mAnnotation;
  final int mDecimalPlaces;

  /**
   * Create an INFO annotation that outputs a double value.
   * Uses a default of 3 for number of decimal places.
   * @param annotation the annotation to use.
   */
  public VcfInfoDoubleAnnotator(AbstractDerivedAnnotation annotation) {
    this(annotation, 3);
  }

  /**
   * Create an INFO annotation that outputs a double value.
   * @param annotation the annotation to use.
   * @param decimalPlaces the number of decimal places to output.
   */
  public VcfInfoDoubleAnnotator(AbstractDerivedAnnotation annotation, int decimalPlaces) {
    assert annotation != null && annotation.getType().getClassType() == Double.class;
    mAnnotation = annotation;
    mDecimalPlaces = decimalPlaces;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    header.ensureContains(new InfoField(mAnnotation.getName(), MetaType.FLOAT, VcfNumber.ONE, mAnnotation.getDescription()));
  }

  @Override
  public void annotate(VcfRecord rec) {
    final Double val = (Double) mAnnotation.getValue(rec, -1);
    if (val != null) {
      rec.setInfo(mAnnotation.getName(), Utils.realFormat(val, mDecimalPlaces));
    }
  }

}
