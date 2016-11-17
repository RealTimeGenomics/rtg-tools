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

package com.rtg.vcf.annotation;

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public abstract class AbstractDerivedFormatAnnotation extends AbstractDerivedAnnotation<FormatField> {

  private final Formatter mFormatter;

  /**
   * Constructor for simple single-valued annotations
   * @param field the field declaration
   */
  protected AbstractDerivedFormatAnnotation(FormatField field) {
    this(field, Formatter.getFormatter(field));
  }

  /**
   * @param field the field declaration
   * @param formatter to use (or null if the subclass will be doing its own formatting)
   */
  protected AbstractDerivedFormatAnnotation(FormatField field, Formatter formatter) {
    super(field);
    mFormatter = formatter;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    checkHeader(header);
    header.ensureContains(getField());
  }

  @Override
  public void annotate(VcfRecord rec) {
    for (int i = 0; i < rec.getNumberOfSamples(); i++) {
      final Object val = getValue(rec, i);
      if (val != null) {
        rec.setFormatAndSample(getName(), mFormatter.toString(val), i);
      }
    }
    rec.padFormatAndSample(getName());
  }

}
