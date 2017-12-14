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

package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Support output files with INFO field annotations containing variant status.
 */
@TestClass("com.rtg.vcf.eval.CombinedEvalSynchronizerTest")
abstract class WithInfoEvalSynchronizer extends WithRocsEvalSynchronizer {

  static final String INFO_BASE = "BASE";
  static final String INFO_CALL = "CALL";
  static final String INFO_SYNCPOS = "SYNC";
  static final String INFO_CALL_WEIGHT = "CALL_WEIGHT";
  static final String STATUS_OUTSIDE = "OUT";
  static final String STATUS_IGNORED = "IGN";
  static final String STATUS_HARD = "HARD";
  static final String STATUS_TP = "TP";
  static final String STATUS_FN = "FN";
  static final String STATUS_FP = "FP";
  static final String STATUS_FN_CA = "FN_CA";
  static final String STATUS_FP_CA = "FP_CA";
  static final String INFO_BASE_ALTERNATE = "BASE_ALTERNATE";
  static final String INFO_CALL_ALTERNATE = "CALL_ALTERNATE";

  /**
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param dualRocs true to output additional ROC curves for allele-matches found in two-pass mode
   * @param rocFilters which ROC curves to output
   * @throws IOException if there is a problem opening output files
   */
  WithInfoEvalSynchronizer(VariantSet variants, ReferenceRanges<String> ranges,
                           RocSortValueExtractor extractor,
                           File outdir, boolean zip, boolean slope, boolean dualRocs, Set<RocFilter> rocFilters) throws IOException {
    super(variants, ranges, extractor, outdir, zip, slope, dualRocs, rocFilters);
  }

  @Override
  protected final void resetBaselineRecordFields(VcfRecord rec) {
    resetOurAnnotations(rec);
  }

  @Override
  protected final void resetCallRecordFields(VcfRecord rec) {
    resetOurAnnotations(rec);
  }

  // Remove any pre-existing annotations for VCFs that have already been through vcfeval
  private void resetOurAnnotations(VcfRecord rec) {
    final Map<String, ArrayList<String>> info = rec.getInfo();
    info.remove(INFO_SYNCPOS);
    info.remove(INFO_BASE);
    info.remove(INFO_CALL);
    info.remove(INFO_CALL_WEIGHT);
  }

  static void addInfoHeaders(VcfHeader header, VariantSetType type) {
    header.ensureContains(new InfoField(INFO_SYNCPOS, MetaType.INTEGER, VcfNumber.DOT, "Chromosome-unique sync region ID. When IDs differ for baseline/call, both will be listed."));
    if (type == null || type == VariantSetType.BASELINE) {
      header.ensureContains(new InfoField(INFO_BASE, MetaType.STRING, VcfNumber.ONE, "Baseline genotype status"));
      header.ensureContains(new InfoField(INFO_BASE_ALTERNATE, MetaType.FLAG, VcfNumber.ONE, "Baseline variant could be matched in an alternate path"));
    }
    if (type == null || type == VariantSetType.CALLS) {
      header.ensureContains(new InfoField(INFO_CALL, MetaType.STRING, VcfNumber.ONE, "Call genotype status"));
      header.ensureContains(new InfoField(INFO_CALL_WEIGHT, MetaType.FLOAT, VcfNumber.ONE, "Call weight (equivalent number of baseline variants). When unspecified, assume 1.0"));
      header.ensureContains(new InfoField(INFO_CALL_ALTERNATE, MetaType.FLAG, VcfNumber.ONE, "Call variant could be matched in an alternate path"));
    }
  }

  protected Map<String, String> updateForCall(boolean unknown, Map<String, String> newInfo) {
    final String status;
    boolean altMatch = false;
    if (unknown) {
      status = STATUS_IGNORED;
    } else {
      final String sync;
      String weight = null;
      final double w;
      if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) {
        status = STATUS_HARD;
        sync = null;
      } else if (mCv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        w = ((OrientedVariant) mCv).getWeight();
        if (w > 0) {
          if (Math.abs(w - 1.0) > 0.001) {
            weight = String.format("%.3g", w);
          }
          addToROCContainer(w, 0, 1, false);
          status = STATUS_TP;
        } else {
          status = STATUS_OUTSIDE;
        }
        sync = Integer.toString(mCSyncStart + 1);
      } else if (mCv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        w = ((OrientedVariant) mCv).getWeight();
        if (w > 0) {
          if (Math.abs(w - 1.0) > 0.001) {
            weight = String.format("%.3g", w);
          }
          addToROCContainer(w, 0, 1, true);
          status = STATUS_FP_CA;
        } else {
          status = STATUS_OUTSIDE;
        }
        sync = Integer.toString(mCSyncStart2 + 1);
      } else if (mCv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        ++mCallOutside;
        status = STATUS_OUTSIDE;
        sync = null;
      } else if (mCv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        if (mCv.hasStatus(VariantId.STATUS_ANY_MATCH)) {
          altMatch = true;
        }
        addToROCContainer(0, 1, 0, false);
        status = STATUS_FP;
        sync = mCSyncStart2 > 0 ? Integer.toString(mCSyncStart2 + 1) : Integer.toString(mCSyncStart + 1);
      } else {
        throw new RuntimeException("Unhandle variant status during postprocessing: " + mCv);
      }
      if (sync != null) {
        final String oldSync = newInfo.get(INFO_SYNCPOS);
        newInfo.put(INFO_SYNCPOS, oldSync == null || oldSync.equals(sync) ? sync : (oldSync + VcfRecord.ALT_CALL_INFO_SEPARATOR + sync));
      }
      if (weight != null) {
        newInfo.put(INFO_CALL_WEIGHT, weight);
      }
    }
    newInfo.put(INFO_CALL, status);
    if (altMatch) {
      newInfo.put(INFO_CALL_ALTERNATE, null);
    }
    return newInfo;
  }

  protected Map<String, String> updateForBaseline(boolean unknown, Map<String, String> newInfo) {
    final String status;
    boolean altMatch = false;
    if (unknown) {
      status = STATUS_IGNORED;
    } else {
      final String sync;
      if (mBv.hasStatus(VariantId.STATUS_SKIPPED)) {
        status = STATUS_HARD;
        sync = null;
      } else if (mBv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        status = STATUS_OUTSIDE;
        sync = null;
      } else if (mBv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        incrementBaselineCounts(true, false, false);
        status = STATUS_TP;
        sync = Integer.toString(mBSyncStart + 1);
      } else if (mBv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        incrementBaselineCounts(false, true, false);
        status = STATUS_FN_CA;
        sync = Integer.toString(mBSyncStart2 + 1);
      } else if (mBv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        if (mBv.hasStatus(VariantId.STATUS_ANY_MATCH)) {
          altMatch = true;
        }
        incrementBaselineCounts(false, false, true);
        status = STATUS_FN;
        sync = mBSyncStart2 > 0 ? Integer.toString(mBSyncStart2 + 1) : Integer.toString(mBSyncStart + 1);
      } else {
        throw new RuntimeException("Unhandle variant status during postprocessing: " + mBv);
      }
      if (sync != null) {
        final String oldSync = newInfo.get(INFO_SYNCPOS);
        newInfo.put(INFO_SYNCPOS, oldSync == null || oldSync.equals(sync) ? sync : (sync + VcfRecord.ALT_CALL_INFO_SEPARATOR + oldSync));
      }
    }
    newInfo.put(INFO_BASE, status);
    if (altMatch) {
      newInfo.put(INFO_BASE_ALTERNATE, null);
    }
    return newInfo;
  }

  protected void setNewInfoFields(VcfRecord rec, Map<String, String> newInfo) {
    for (Map.Entry<String, String> e : newInfo.entrySet()) {
      if (e.getValue() == null) {
        rec.addInfo(e.getKey());
      } else {
        rec.addInfo(e.getKey(), e.getValue());
      }
    }
  }
}
