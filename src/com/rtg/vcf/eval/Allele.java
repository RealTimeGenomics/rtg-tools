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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.DnaUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.Range;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Hold a variant allele, one allele of a genotype.
 */
@TestClass("com.rtg.vcf.eval.VariantTest")
public class Allele extends SequenceNameLocusSimple {

  private static final byte[] MISSING = {
    0 // "N"
  };

  private final byte[] mNt;

  /**
   * Construct an Allele
   * @param seq chromosome name
   * @param start start position (0-based inclusive)
   * @param end end position (0-based exclusive)
   * @param nt the bases of the allele which substitute for the based in the reference region
   */
  public Allele(String seq, int start, int end, byte[] nt) {
    super(seq, start, end);
    if (nt == null) {
      throw new NullPointerException();
    }
    mNt = nt;
  }

  /**
   * Construct an Allele
   * @param locus the reference region occupied by the Allele
   * @param nt the bases of the allele which substitute for the based in the reference region
   */
  public Allele(SequenceNameLocus locus, byte[] nt) {
    this(locus.getSequenceName(), locus.getStart(), locus.getEnd(), nt);
  }

  /**
   * @return the bases of the allele (may be zero length for a deletion)
   */
  byte[] nt() {
    return mNt;
  }

  @Override
  public String toString() {
    return String.valueOf(getStart() + 1) + ":" + (getEnd() + 1) + "(" + DnaUtils.bytesToSequenceIncCG(mNt) + ")";
  }


  /**
   * Return a full set of alleles, one per REF/ALT declared in the record and one for missing.
   * Each allele has leading/trailing ref bases removed and position adjusted accordingly.
   * So ref is like "skip"
   * @param rec the record
   * @param gtArray if not null, only populate alleles referenced by the GT array
   * @param trim if set, remove all leading and trailing same-as-ref bases
   * @param explicitMissing if set, treat missing allele as an explicit 'N', otherwise treat as skip  @return the set of alleles
   * @return the set of alleles, with the first element corresponding to the missing allele (i.e. index = GT + 1)
   */
  static Allele[] getTrimmedAlleles(VcfRecord rec, int[] gtArray, boolean trim, boolean explicitMissing) {
    final boolean removePaddingBase = !trim && VcfUtils.hasRedundantFirstNucleotide(rec);
    final Allele[] alleles = new Allele[rec.getAltCalls().size() + 2];
    for (int i = -1; i < alleles.length - 1; i++) {
      if (gtArray == null || gtArray[0] == i || (gtArray.length == 2 && gtArray[1] == i)) {
        alleles[i + 1] = getAllele(rec, i, trim, removePaddingBase, explicitMissing);
      }
    }
    return alleles;
  }

  /**
   * Construct a single allele
   * @param rec the record
   * @param allele the index of the allele, 0 is ref, -1 is missing
   * @param trim if set, remove all leading and trailing same-as-ref bases
   * @param removePaddingBase if set (and not otherwise trimming), remove the single leading base
   * @param explicitMissing if set, treat missing allele as an explicit 'N', otherwise treat as skip
   * @return the allele, or null if the allele should not result in a change to the haplotype
   */
  protected static Allele getAllele(VcfRecord rec, int allele, boolean trim, boolean removePaddingBase, boolean explicitMissing) {
    if (allele == -1) {
      if (explicitMissing) {
        return new Allele(rec.getSequenceName(), rec.getStart() + (removePaddingBase ? 1 : 0), rec.getEnd(), MISSING);
      } else {
        return null;
      }
    }
    if (trim) {
      final String ref = rec.getRefCall();
      if (allele == 0) {
        return trim ? null : new Allele(rec, DnaUtils.encodeString(ref));
      }
      final String alt = rec.getAllele(allele);
      final int stripLeading = StringUtils.longestPrefix(ref, alt);
      final int stripTrailing = StringUtils.longestSuffix(stripLeading, ref, alt);
      return new Allele(rec.getSequenceName(), rec.getStart() + stripLeading, rec.getEnd() - stripTrailing,
        DnaUtils.encodeString(StringUtils.clip(alt, stripLeading, stripTrailing)));
    } else {
      final String alt = rec.getAllele(allele);
      return new Allele(rec.getSequenceName(), rec.getStart() + (removePaddingBase ? 1 : 0), rec.getEnd(),
        DnaUtils.encodeString(removePaddingBase ? alt.substring(1) : alt));
    }
  }

  // Determine the bounding reference span of a set of alleles
  static Range getAlleleBounds(Allele[] alleles) {
    int start = Integer.MAX_VALUE;
    int end = Integer.MIN_VALUE;
    for (final Allele allele : alleles) {
      if (allele != null) {
        start = Math.min(start, allele.getStart());
        end = Math.max(end, allele.getEnd());
      }
    }
    return new Range(start, end);
  }


}
