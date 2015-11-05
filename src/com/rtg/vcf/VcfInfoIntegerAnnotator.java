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

import com.rtg.vcf.annotation.AbstractDerivedAnnotation;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 */
public class VcfInfoIntegerAnnotator implements VcfAnnotator {

  final AbstractDerivedAnnotation mAnnotation;

  /**
   * Create an INFO annotation that outputs an integer value.
   * @param annotation the annotation to use.
   */
  public VcfInfoIntegerAnnotator(AbstractDerivedAnnotation annotation) {
    assert annotation != null && annotation.getType().getClassType() == Integer.class;
    mAnnotation = annotation;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    header.ensureContains(new InfoField(mAnnotation.getName(), MetaType.INTEGER, VcfNumber.ONE, mAnnotation.getDescription()));
  }

  @Override
  public void annotate(VcfRecord rec) {
    final Integer val = (Integer) mAnnotation.getValue(rec, -1);
    if (val != null) {
      rec.setInfo(mAnnotation.getName(), "" + val);
    }
  }

}
