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
import com.rtg.vcf.header.TypedField;
import com.rtg.vcf.header.VcfHeader;

/**
 * VCF annotation specification and extraction interface.
 */
public interface VcfAnnotation<T extends TypedField<T>> {

  /**
   * Gets the VCF header field where this annotation should be stored
   * @return the header field
   */
  T getField();

  /**
   * Gets the name of the annotation.
   * @return name of annotation
   */
  String getName();

  /**
   * Returns the value for an annotation from a given {@link VcfRecord}.
   * For sample specific annotations, the value in the first sample is returned.
   * @param record record to extract value from.
   * @param sampleNumber the number of the sample to extract the value from.
   * @return value for this annotation from the given record, or null if no value could be calculated.
   */
  Object getValue(VcfRecord record, int sampleNumber);

  /**
   * Ensures any required annotations are declared in the given header.
   * @param header a VCF header
   * @return null if all good, other wise an error message.
   */
  String checkHeader(VcfHeader header);

}
