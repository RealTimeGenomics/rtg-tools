/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import com.rtg.util.MathUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Assigns density attribute.  Annotates variants with the number of variants within
 * a specfied distance either side of the record.  Counting and annotation is done
 * per sample.
 */
class DensityAnnotator implements VcfWriter {

  private static final String DENSITY_ATTRIBUTE = "DENS";
  static final int DEFAULT_DISTANCE = 5;
  final VcfWriter mInner;
  final int mDistance;
  final Queue<VcfRecord> mBuffer = new ArrayDeque<>();
  final Queue<int[]> mCounts = new ArrayDeque<>();

  DensityAnnotator(final VcfWriter w, final int distance) {
    mInner = w;
    mDistance = distance;
  }

  DensityAnnotator(final VcfWriter w) {
    this(w, DEFAULT_DISTANCE);
  }

  static VcfHeader updateHeader(final VcfHeader header, final int distance) {
    header.ensureContains(new FormatField(DENSITY_ATTRIBUTE, MetaType.INTEGER, VcfNumber.ONE, "Number of variants within " + distance + " bases"));
    return header;
  }

  @Override
  public VcfHeader getHeader() {
    return mInner.getHeader();
  }

  private void updateCounts(VcfRecord record, int[] counts) {
    for (final VcfRecord rec : mBuffer) {
      for (int sample = 0; sample < counts.length; ++sample) {
        if (VcfUtils.isVariantGt(record.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sample))
          && VcfUtils.isVariantGt(rec.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sample))) {
          ++counts[sample];
        }
      }
    }
  }

  @Override
  public void write(final VcfRecord record) throws IOException {
    if (!mBuffer.isEmpty()) {
      if (!mBuffer.peek().getSequenceName().equals(record.getSequenceName())) {
        flushBuffer();
      } else {
        while (!mBuffer.isEmpty() && mBuffer.peek().getEnd() < record.getStart() - mDistance) {
          flushFirst();
        }
      }
    }

    // Update counts for records lying to the left
    final int[] counts = new int[record.getNumberOfSamples()];
    updateCounts(record, counts);
    mCounts.add(counts);
    mBuffer.add(record);
  }

  protected void flushFirst() throws IOException {
    assert mBuffer.size() == mCounts.size();
    final VcfRecord record = mBuffer.remove();
    final int[] counts = mCounts.remove();
    // Update counts for records lying to the right
    updateCounts(record, counts);
    if (!MathUtils.isZero(counts)) {
      // Only annotate the record if there is a non-zero count for some sample
      record.addFormat(DENSITY_ATTRIBUTE);
      for (final int count : counts) {
        record.addFormatAndSample(DENSITY_ATTRIBUTE, String.valueOf(count));
      }
    }
    mInner.write(record);
  }

  private void flushBuffer() throws IOException {
    while (!mBuffer.isEmpty()) {
      flushFirst();
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (final VcfWriter ignored = mInner) {
      flushBuffer();
    }
  }

  /**
   * Direct entry point.
   * @param args distance input.vcf
   * @throws IOException if an I/O error occurs.
   */
  public static void main(String[] args) throws IOException {
    final int distance = Integer.parseInt(args[0]);
    try (VcfReader reader = VcfReader.openVcfReader(new File(args[1]))) {
      try (VcfWriter writer = new DensityAnnotator(new VcfWriterFactory().addRunInfo(true).make(updateHeader(reader.getHeader(), distance), null, System.out), distance)) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          writer.write(rec);
        }
      }
    }
  }
}
