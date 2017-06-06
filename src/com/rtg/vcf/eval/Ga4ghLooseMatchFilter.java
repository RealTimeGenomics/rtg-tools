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
package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import com.rtg.vcf.DefaultVcfWriter;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.VcfHeader;

/**
 * Assigns loose matches where there is a variant within +/- N bases. Input must be in GA4GH intermediate format.
 *
 * Rules:
 *
 * - Any query variant with BD=FP and BK=. will get BK=lm if there are truth variants within N bases that have BD = TP/FN.
 * - Any truth variant with BD=FN and BK=. will get BK=lm if there are query variants within N bases that have BD = TP/FP.
 * - N-base proximity is based on untrimmed reference spans.
 */
class Ga4ghLooseMatchFilter implements VcfWriter {

  final VcfWriter mInner;
  final int mDistance;
  final Queue<VcfRecord> mBuffer = new ArrayDeque<>(); // Contains all records passing through
  final ArrayDeque<VcfRecord> mTruthRegions = new ArrayDeque<>(); // Contains only truth regions eligible to trigger lm on a query variant
  final ArrayDeque<VcfRecord> mQueryRegions = new ArrayDeque<>(); // Contains only query regions eligible to trigger lm on a truth variant

  Ga4ghLooseMatchFilter(VcfWriter w, int distance) {
    mInner = w;
    mDistance = distance;
  }

  @Override
  public VcfHeader getHeader() {
    return mInner.getHeader();
  }

  @Override
  public void write(VcfRecord record) throws IOException {
    if (!mBuffer.isEmpty()) {
      if (!mBuffer.peek().getSequenceName().equals(record.getSequenceName())) {
        flushBuffer();
      } else {
        while (!mBuffer.isEmpty() && mBuffer.peek().getEnd() < record.getStart() - mDistance) {
          flushFirst();
        }
      }
    }

    final ArrayList<String> mk = record.getFormat(Ga4ghEvalSynchronizer.FORMAT_MATCH_KIND);
    final ArrayList<String> bd = record.getFormat(Ga4ghEvalSynchronizer.FORMAT_DECISION);

    // On entry, possibly set lm status based on proximity to upstream regions
    if (queryWantsLm(mk, bd) && nearbyUpstream(mTruthRegions, record.getStart())) {
      mk.set(Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX, Ga4ghEvalSynchronizer.SUBTYPE_REGIONAL_MATCH);
    }
    if (truthWantsLm(mk, bd) && nearbyUpstream(mQueryRegions, record.getStart())) {
      mk.set(Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX, Ga4ghEvalSynchronizer.SUBTYPE_REGIONAL_MATCH);
    }

    // Add the record to appropriate queues
    mBuffer.add(record);
    if (bd != null) {
      final String bdq = bd.get(Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX);
      if (Ga4ghEvalSynchronizer.DECISION_TP.equals(bdq) || Ga4ghEvalSynchronizer.DECISION_FP.equals(bdq)) {
        mQueryRegions.add(record);
      }
      final String bdt = bd.get(Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX);
      if (Ga4ghEvalSynchronizer.DECISION_TP.equals(bdt) || Ga4ghEvalSynchronizer.DECISION_FN.equals(bdt)) {
        mTruthRegions.add(record);
      }
    }
  }

  protected void flushFirst() throws IOException {
    final VcfRecord record = mBuffer.remove();

    // Clear irrelevant entries from the truth/query region lists
    while (!mTruthRegions.isEmpty() && mTruthRegions.peekFirst().getEnd() <= record.getStart()) {
      mTruthRegions.remove();
    }
    while (!mQueryRegions.isEmpty() && mQueryRegions.peekFirst().getEnd() <= record.getStart()) {
      mQueryRegions.remove();
    }

    final ArrayList<String> mk = record.getFormat(Ga4ghEvalSynchronizer.FORMAT_MATCH_KIND);
    final ArrayList<String> bd = record.getFormat(Ga4ghEvalSynchronizer.FORMAT_DECISION);

    // On exit, possibly set lm status based on proximity to downstream regions
    if (queryWantsLm(mk, bd) && nearbyDownstream(mTruthRegions, record.getEnd())) {
      mk.set(Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX, Ga4ghEvalSynchronizer.SUBTYPE_REGIONAL_MATCH);
    }
    if (truthWantsLm(mk, bd) && nearbyDownstream(mQueryRegions, record.getEnd())) {
      mk.set(Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX, Ga4ghEvalSynchronizer.SUBTYPE_REGIONAL_MATCH);
    }

    mInner.write(record);
  }

  private boolean truthWantsLm(ArrayList<String> mk, ArrayList<String> bd) {
    return mk != null && bd != null
      && Ga4ghEvalSynchronizer.DECISION_FN.equals(bd.get(Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX))
      && Ga4ghEvalSynchronizer.SUBTYPE_MISMATCH.equals(mk.get(Ga4ghEvalSynchronizer.TRUTH_SAMPLE_INDEX));
  }

  private boolean queryWantsLm(ArrayList<String> mk, ArrayList<String> bd) {
    return mk != null && bd != null
      && Ga4ghEvalSynchronizer.DECISION_FP.equals(bd.get(Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX))
      && Ga4ghEvalSynchronizer.SUBTYPE_MISMATCH.equals(mk.get(Ga4ghEvalSynchronizer.QUERY_SAMPLE_INDEX));
  }

  private boolean nearbyUpstream(ArrayDeque<VcfRecord> regions, int start) {
    return !regions.isEmpty() && start < regions.peekLast().getEnd() + mDistance;
  }

  private boolean nearbyDownstream(ArrayDeque<VcfRecord> regions, int end) {
    return !regions.isEmpty() && regions.peekFirst().getStart() - mDistance < end;
  }


  private void flushBuffer() throws IOException {
    while (mBuffer.size() > 0) {
      flushFirst();
    }
    mTruthRegions.clear();
    mQueryRegions.clear();
  }


  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mInner) { // try-with-resources for nice closing side effects
      flushBuffer();
    }
  }

  // Test main first arg must be match distance, second is VCF file name (stdin if omitted)
  public static void main(String[] args) throws IOException {
    int arg = 0;
    final int distance = args.length > 0 ? Integer.parseInt(args[arg++]) : 30;
    final String file = arg < args.length ? args[arg] : "-";
    try (VcfReader reader = VcfReader.openVcfReader(new File(file))) {
      final VcfHeader header = reader.getHeader();
      header.addRunInfo();
      final File vcfFile = null;
      try (VcfWriter writer = new Ga4ghLooseMatchFilter(new DefaultVcfWriter(header, vcfFile, System.out, false, false, true), distance)) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          writer.write(rec);
        }
      }
    }
  }
}
