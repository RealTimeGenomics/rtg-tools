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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.alignment.Partition;
import com.rtg.alignment.Slice;
import com.rtg.alignment.SplitAlleles;
import com.rtg.mode.DnaUtils;
import com.rtg.reader.ReaderUtils;
import com.rtg.reader.SequencesReader;

/**
 * Decomposes VCF records into smaller constituents based on alignments. This does not perform additional
 * normalization (such as ensuring indels are left shifted).
 */
@TestClass(value = "com.rtg.vcf.VcfDecomposerCliTest")
class Decomposer {

  private static final String ORP = "ORP";
  private static final String ORL = "ORL";

  private final SequencesReader mTemplate;
  private final Map<String, Long> mNameMap;

  private long mCurrentSequenceId = -1;
  private byte[] mCurrentSequence = null;
  protected long mTotalCallsSplit = 0;
  protected long mTotalPieces = 0;
  protected long mTotalRecords = 0;

  /**
   * Construct the decomposer. If a reference sequence is supplied, any anchor bases required will consistently be added to
   * the left of decomposed records. If no reference sequence is supplied, memory consumption is lower, but occasionally a
   * decomposed indel must be right-anchored (using the reference base provided in the input record). Either way the output
   * records are valid.
   * @param template supplies reference bases, optional.
   * @throws IOException if there is a problem reading from the reference
   */
  Decomposer(SequencesReader template) throws IOException {
    mTemplate = template;
    mNameMap = template == null ? null : ReaderUtils.getSequenceNameMap(mTemplate);
  }

  /**
   * Return true if this record is a candidate for decomposition. Currently this requires that the REF or ALT
   * alleles only contain DNA symbols
   * @param record the record to examine
   * @return true if decomposition can be attempted
   */
  protected boolean canDecompose(VcfRecord record) {
    return DnaUtils.isValidDna(record.getRefCall()) && record.getAltCalls().stream().allMatch(DnaUtils::isValidDna);
  }

  private void updateTemplate(final String sequenceName) throws IOException {
    if (mTemplate != null) {
      final long sequenceId = mNameMap.get(sequenceName);
      if (mCurrentSequenceId != sequenceId) {
        mCurrentSequenceId = sequenceId;
        mCurrentSequence = mTemplate.read(sequenceId);
      }
    }
  }

  private boolean needsAnchorBase(final String[] alleles) {
    for (final String a : alleles) {
      if (a.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private String getAnchoredAllele(boolean needAnchor, char anchor, boolean left, String allele) {
    return !needAnchor ? allele : left ? anchor + allele : allele + anchor;
  }

  private int[] updateAlts(final List<String> altCalls, final String[] alleles, final boolean needAnchor, char anchor, boolean left) {
    final int[] altRemap = new int[alleles.length]; // Map old allele number to new allele number
    final String refAllele = alleles[0];
    altCalls.clear();
    for (int k = 1; k < alleles.length; ++k) {
      final String allele = alleles[k];
      if (allele.equals(refAllele)) {
        altRemap[k] = 0; // This allele is now reference
      } else {
        final String newAllele = getAnchoredAllele(needAnchor, anchor, left, allele);
        final int existing = altCalls.indexOf(newAllele);
        if (existing >= 0) {
          // We already have this alternative
          altRemap[k] = existing + 1;
        } else {
          // This is a new alternative
          altCalls.add(newAllele);
          altRemap[k] = altCalls.size();
        }
      }
    }
    return altRemap;
  }

  protected List<VcfRecord> decompose(final VcfRecord rec) throws IOException {
    updateTemplate(rec.getSequenceName());
    ++mTotalRecords;
    final SplitAlleles result;
    final int pad;
    if (VcfUtils.hasRedundantFirstNucleotide(rec)) {
      pad = 1;
      result = new SplitAlleles(rec.getRefCall().substring(1), rec.getAltCalls().stream().map(s -> s.substring(1)).collect(Collectors.toList()));
    } else {
      pad = 0;
      result = new SplitAlleles(rec.getRefCall(), rec.getAltCalls());
    }
    final Partition partition = result.partition();
    assert partition.size() > 0;
    if (partition.size() == 0   // Not sure this can happen
      || (pad == 0 && partition.size() == 1) // Variant without padding, already in smallest form
      || (pad == 1 && partition.size() == 1 && partition.get(0).getOffset() == 0 && needsAnchorBase(partition.get(0).getAlleles())) // Variant with anchor, already in smallest form
      ) {
      return Collections.singletonList(rec); // Efficiency, no change in record
    }
    final Partition split = SplitAlleles.removeAllRef(partition);
    ++mTotalCallsSplit;
    final ArrayList<VcfRecord> res = new ArrayList<>(split.size());
    for (final Slice s : split) {
      final VcfRecord splitRecord = new VcfRecord(rec);
      splitRecord.addInfo(ORP, String.valueOf(rec.getStart() + 1)); // 1-based for output
      splitRecord.addInfo(ORL, String.valueOf(rec.getRefCall().length()));
      final String[] alleles = s.getAlleles();
      final boolean needAnchor = needsAnchorBase(alleles);
      final int offset = s.getOffset() + pad;
      final int start = splitRecord.getStart() + offset;

      final boolean left;
      final char anchor;
      if (mTemplate != null) {
        left = true;
        anchor = DnaUtils.base(mCurrentSequence, start - 1);
      } else {
        left = offset != 0;
        anchor = !needAnchor ? '^' : rec.getRefCall().charAt(left ? offset - 1 : offset + alleles[0].length() - pad);
      }

      if (needAnchor && left) {
        splitRecord.setStart(start - 1);
      } else {
        splitRecord.setStart(start);
      }
      splitRecord.setRefCall(getAnchoredAllele(needAnchor, anchor, left, alleles[0]));
      final int[] alleleMap = updateAlts(splitRecord.getAltCalls(), alleles, needAnchor, anchor, left);
      // Go through samples and update genotypes
      final List<String> oldGenotypes = splitRecord.getFormat(VcfUtils.FORMAT_GENOTYPE);
      for (int sample = 0; sample < splitRecord.getNumberOfSamples(); ++sample) {
        final String oldGt = oldGenotypes.get(sample);
        final int[] gt = VcfUtils.splitGt(oldGt);
        for (int k = 0; k < gt.length; ++k) {
          gt[k] = gt[k] == VcfUtils.MISSING_GT ? VcfUtils.MISSING_GT : alleleMap[gt[k]];
        }
        splitRecord.setFormatAndSample(VcfUtils.FORMAT_GENOTYPE, VcfUtils.joinGt(VcfUtils.isPhasedGt(oldGt), gt), sample);
      }
      ++mTotalPieces;
      res.add(splitRecord);
    }
    return res;
  }

  void printStatistics(OutputStream out) {
    final PrintStream output = new PrintStream(out);
    output.println("Total records : " + mTotalRecords);
    output.println("Number of records decomposed : " + mTotalCallsSplit);
    output.println("Remaining records : " + (mTotalRecords + (mTotalPieces - mTotalCallsSplit)));
  }
}
