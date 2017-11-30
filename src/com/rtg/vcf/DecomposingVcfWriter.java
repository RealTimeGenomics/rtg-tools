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
import com.rtg.alignment.SplitAlleles;
import com.rtg.mode.DnaUtils;
import com.rtg.reader.ReaderUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.Pair;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Decomposes variants during writing
 */
@TestClass(value="com.rtg.vcf.VcfDecomposerCliTest")
class DecomposingVcfWriter implements VcfWriter {

  private static final String ORP = "ORP";
  private static final String ORL = "ORL";

  private final VcfWriter mOut;
  private final SequencesReader mTemplate;
  private final Map<String, Long> mNameMap;

  private long mCurrentSequenceId = -1;
  private byte[] mCurrentSequence = null;
  protected long mTotalCallsSplit = 0;
  protected long mTotalPieces = 0;
  protected long mTotalRecords = 0;

  DecomposingVcfWriter(VcfWriter dest, SequencesReader template) throws IOException {
    mOut = new ReorderingVcfWriter(dest); // Needed to ensure appropriate ordering of output variants
    mOut.getHeader().ensureContains(new InfoField(ORP, MetaType.STRING, VcfNumber.ONE, "Original variant position"));
    mOut.getHeader().ensureContains(new InfoField(ORL, MetaType.STRING, VcfNumber.ONE, "Original reference length"));
    mTemplate = template;
    mNameMap = ReaderUtils.getSequenceNameMap(mTemplate);
  }

  @Override
  public VcfHeader getHeader() {
    return mOut.getHeader();
  }

  @Override
  public void write(VcfRecord record) throws IOException {
    final String ref = record.getRefCall();
    if (record.getAltCalls().stream().anyMatch(alt -> VariantType.getType(ref, alt).isSvType())) {
      mOut.write(record); // Just pass straight through
    } else {
      updateTemplate(mNameMap.get(record.getSequenceName()));
      for (final VcfRecord res : decompose(record)) {
        mOut.write(res);
      }
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mOut) {
      // Use try with resources on existing writer for nice closing
    }
  }

  private void updateTemplate(final long sequenceId) throws IOException {
    if (mCurrentSequenceId != sequenceId) {
      mCurrentSequenceId = sequenceId;
      mCurrentSequence = mTemplate.read(sequenceId);
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

  private String getAnchoredAllele(final String allele, final int start, final boolean needAnchor) {
    return needAnchor ? DnaUtils.base(mCurrentSequence, start - 1) + allele : allele;
  }

  private int[] updateAlts(final List<String> altCalls, final String[] alleles, final int start, final boolean needAnchor) {
    final int[] altRemap = new int[alleles.length]; // Map old allele number to new allele number
    final String refAllele = getAnchoredAllele(alleles[0], start, needAnchor);
    altCalls.clear();
    for (int k = 1; k < alleles.length; ++k) {
      final String allele = alleles[k];
      if (allele.equals(refAllele)) {
        altRemap[k] = 0; // This allele is now reference
      } else {
        final String newAllele = getAnchoredAllele(allele, start, needAnchor);
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

  private List<VcfRecord> decompose(final VcfRecord rec) {
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
    final List<Pair<Integer, String[]>> partition = result.partition();
    if (partition.size() <= 1 && pad == 0) {
      return Collections.singletonList(rec); // Efficiency, no change in record
    }
    final List<Pair<Integer, String[]>> split = SplitAlleles.removeAllRef(partition);
    ++mTotalCallsSplit;
    final ArrayList<VcfRecord> res = new ArrayList<>(split.size());
    for (final Pair<Integer, String[]> s : split) {
      final VcfRecord splitRecord = new VcfRecord(rec);
      splitRecord.addInfo(ORP, String.valueOf(rec.getStart() + 1)); // 1-based for output
      splitRecord.addInfo(ORL, String.valueOf(rec.getRefCall().length()));
      final String[] alleles = s.getB();
      final boolean needAnchor = needsAnchorBase(alleles);
      final Integer offset = s.getA() + pad;
      final int start = splitRecord.getStart() + offset;
      if (needAnchor) {
        splitRecord.setStart(start - 1);
        splitRecord.setRefCall(getAnchoredAllele(alleles[0], start, true));
      } else {
        splitRecord.setStart(start);
        splitRecord.setRefCall(alleles[0]);
      }
      final int[] alleleMap = updateAlts(splitRecord.getAltCalls(), alleles, start, needAnchor);
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
