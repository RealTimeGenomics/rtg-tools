/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.TypedField;

/**
 * Simple single-value annotation formatters.
 */
class Formatter {

  static final Formatter DEFAULT = new Formatter();
  static final Formatter DEFAULT_DOUBLE = new Formatter() {
    @Override
    String toString(Object val) {
      return Utils.realFormat((Double) val, 3);
    }
  };
  static final Formatter DEFAULT_DOUBLE_ARR = new Formatter() {
    @Override
    String toString(Object val) {
      return StringUtils.join(",", VcfUtils.formatFloatArray((double[]) val));
    }
  };

  static Formatter getFormatter(TypedField<?> field) {
    if (field.getNumber().getNumber() != 1) {
      throw new IllegalArgumentException("Value formatting of multi-valued field " + field.getId() + " currently not supported.");
    }
    switch (field.getType()) {
      case FLOAT:
        return DEFAULT_DOUBLE;
      case INTEGER:
      case STRING:
      case CHARACTER:
        return DEFAULT;
      default:
        throw new IllegalArgumentException("Value formatting of type " + field.getType() + " not supported for field " + field.getId());
    }
  }

  /**
   * Convert the supplied object to a string representation for VCF
   * @param val the value to format
   * @return the value as a String
   */
  String toString(Object val) {
    return val.toString();
  }
}
