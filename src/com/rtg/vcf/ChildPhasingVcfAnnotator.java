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
package com.rtg.vcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.rtg.relation.Family;
import com.rtg.vcf.header.VcfHeader;

/**
 * Phases child genotype calls in VCF records according to pedigree. When a child genotype can be
 * unambiguously phased according to Mendelian inheritance, the genotype will be ordered such
 * that the allele inherited from the father is first, and the mothers is second.
 *
 */
public class ChildPhasingVcfAnnotator implements VcfAnnotator {

  private VcfHeader mHeader = null;

  private final Collection<Family> mFamilies;

  /**
   * Constructor. All children within the supplied families will be phased with respect to their parents.
   * @param families the families of interest
   */
  public ChildPhasingVcfAnnotator(Family... families) {
    this(Arrays.asList(families));
  }

  /**
   * Constructor. All children within the supplied families will be phased with respect to their parents.
   * @param families the families of interest
   */
  public ChildPhasingVcfAnnotator(Collection<Family> families) {
    mFamilies = families;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    mHeader = header;
  }

  @Override
  public void annotate(VcfRecord rec) {
    // Phase the calls
    final List<String> calls = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
    final String[] phased = new String[calls.size()];
    for (Family f : mFamilies) { // The families should have at least two members with generated calls, but there may be other members missing
      final Integer fatherIndex = mHeader.getSampleIndex(f.getFather());
      final String fatherCall = (fatherIndex == null) ? VcfRecord.MISSING : calls.get(fatherIndex);
      final Integer motherIndex = mHeader.getSampleIndex(f.getMother());
      final String motherCall = (motherIndex == null) ? VcfRecord.MISSING : calls.get(motherIndex);
      for (String child : f.getChildren()) {
        final Integer childIndex = mHeader.getSampleIndex(child);
        if (childIndex != null) {
          phased[childIndex] = phaseDiploidCall(fatherCall, motherCall, calls.get(childIndex));
        }
      }
    }
    final ArrayList<String> newCalls = new ArrayList<>(calls.size());
    for (int i = 0; i < calls.size(); i++) {
      if (phased[i] == null) { // All not-yet phased calls get copied through unaltered
        phased[i] = calls.get(i);

        // If we wanted, we could phase homozygous remaining calls, but there is no information to be gained
      /*
      final int[] p = VcfUtils.splitGT(phased[i]);
      if (p.length == 2 && p[0] != -1 && p[0].equals(p[1])) {
        phased[i] = "" + p[0] + VcfUtils.PHASED_SEPARATOR + p[1];
      }
      */
      }
      newCalls.add(phased[i]);
    }
    // Plug phased calls into the record
    calls.clear();
    calls.addAll(newCalls);
  }

  /**
   * Attempt to phase the child with respect to the parents if it is non-missing and diploid
   * Assumes input obeys mendelian constraints.
   * If the call cannot be phased, returns null.
   * If the call can be phased, it is ordered such that the father allele comes first (this is for consistency with our child sim tools).
   * @param fatherCall the genotype call of the father
   * @param motherCall the genotype call of the mother
   * @param childCall the genotype call of the child
   * @return the child genotype call, or null if it could not be phased
   */
  static String phaseDiploidCall(String fatherCall, String motherCall, String childCall) {
    final int[] childAlleles = VcfUtils.splitGt(childCall);
    if ((childAlleles.length == 2)
        && (childAlleles[0] != -1) && (childAlleles[1] != -1)) {

      if (childAlleles[0] == childAlleles[1]) {  // Homozygous always phased
        return "" + childAlleles[0] + VcfUtils.PHASED_SEPARATOR + childAlleles[1];
      } else {
        final int[] fatherAlleles = VcfUtils.splitGt(fatherCall);
        final int[] motherAlleles = VcfUtils.splitGt(motherCall);

        final boolean firstInFather = contains(fatherAlleles, childAlleles[0]);
        final boolean firstInMother = contains(motherAlleles, childAlleles[0]);
        final boolean secondInFather = contains(fatherAlleles, childAlleles[1]);
        final boolean secondInMother = contains(motherAlleles, childAlleles[1]);

        final boolean firstOnlyInFather = firstInFather && !firstInMother;
        final boolean firstOnlyInMother = firstInMother && !firstInFather;
        final boolean secondOnlyInFather = secondInFather && !secondInMother;
        final boolean secondOnlyInMother = secondInMother && !secondInFather;

        if ((firstOnlyInMother || secondOnlyInFather) && !(firstOnlyInFather || secondOnlyInMother)) {
          return "" + childAlleles[1] + VcfUtils.PHASED_SEPARATOR + childAlleles[0];
        } else if ((firstOnlyInFather || secondOnlyInMother) && !(firstOnlyInMother || secondOnlyInFather)) {
          return "" + childAlleles[0] + VcfUtils.PHASED_SEPARATOR + childAlleles[1];
        }
      }
    }
    return null;
  }

  private static boolean contains(int[] gt, int allele) {
    for (int current : gt) {
      if (allele == current) {
        return true;
      }
    }
    return false;
  }

}
