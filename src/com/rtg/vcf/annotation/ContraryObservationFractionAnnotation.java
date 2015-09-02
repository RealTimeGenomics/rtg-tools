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

import java.util.Arrays;
import java.util.List;

import com.rtg.util.StringUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.PedigreeField;
import com.rtg.vcf.header.VcfHeader;

/**
 * The fraction of evidence that is considered contrary to the call made for this sample.
 * For example, in a normal&ndash;cancer somatic call of 0/0 -&gt; 1/0, the <code>COF</code>
 * value is the fraction of the somatic (1) allele in the normal sample.  Usually a high
 * <code>COF</code> value indicates an unrealiable call.
 */
public class ContraryObservationFractionAnnotation extends AbstractDerivedFormatAnnotation {

  private VcfHeader mHeader = null;
  private int[] mDerivedToOriginal = null;

  protected ContraryObservationFractionAnnotation(final String field, final String description, final AnnotationDataType type) {
    super(field, description, type);
  }

  /**
   * Construct a new contrary observation fraction format annotation.
   */
  public ContraryObservationFractionAnnotation() {
    this("COF", "Contrary observation fraction", AnnotationDataType.DOUBLE);
  }

  private void initSampleInfo(final VcfHeader header) {
    mHeader = header;
    mDerivedToOriginal = new int[header.getNumberOfSamples()];
    Arrays.fill(mDerivedToOriginal, -1);
    final List<PedigreeField> pedigreeLines = mHeader.getPedigreeLines();
    for (final PedigreeField pedLine : pedigreeLines) {
      final String orig = pedLine.getOriginal();
      final String derived = pedLine.getDerived();
      mDerivedToOriginal[header.getSampleIndex(derived)] = header.getSampleIndex(orig);
    }
  }

  protected Object getValue(final long ref, final long alt) {
    final long total = alt + ref;
    if (total == 0) {
      return null; // Undefined (no coverage in the normal)
    }
    return alt / (double) total;
  }

  @Override
  public Object getValue(final VcfRecord record, final int sampleNumber) {
    assert mHeader != null; // i.e. checkHeader method must be called before this
    if (sampleNumber >= mDerivedToOriginal.length) {
      return null; // No such sample
    }
    final int originalSample = mDerivedToOriginal[sampleNumber];
    if (originalSample < 0) {
      return null; // Not a derived sample
    }
    final Integer ss = record.getSampleInteger(sampleNumber, VcfUtils.FORMAT_SOMATIC_STATUS);
    if (ss == null || ss != 2) {
      return null; // Not a somatic call
    }
    final int[] originalGt = VcfUtils.getValidGt(record, originalSample);
    final String ad;
    if (VcfUtils.isHomozygousRef(originalGt)) {
      // Somatic call
      assert !VcfUtils.isHomozygousRef(VcfUtils.getValidGt(record, sampleNumber));
      ad = record.getSampleString(originalSample, VcfUtils.FORMAT_ALLELIC_DEPTH);
    } else {
      // Gain of reference, flip the logic, should be little ALT in the derived sample
      assert VcfUtils.isHomozygousRef(VcfUtils.getValidGt(record, sampleNumber));
      ad = record.getSampleString(sampleNumber, VcfUtils.FORMAT_ALLELIC_DEPTH);
    }
    if (ad == null) {
      return null; // No AD information for the original
    }
    final String[] adSplit = StringUtils.split(ad, ',');
    if (adSplit.length != 2) {
      return null; // Not biallelic, these cases too hard for now
    }
    final long ref = Long.parseLong(adSplit[0]);
    final long alt = Long.parseLong(adSplit[1]);
    return getValue(ref, alt);
  }

  @Override
  public String checkHeader(VcfHeader header) {
    initSampleInfo(header);
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.FORMAT_ALLELIC_DEPTH, VcfUtils.FORMAT_GENOTYPE});
  }

}
