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
package com.rtg.vcf.mendelian;

import java.io.PrintStream;

/**
 * Identifies when the aggregate genotypes of a sample are inconsistent with those of its parents.
 * Stores both three-way concordance and pairwise concordance with each parent.
 */
class TrioConcordance {

  enum Status {
    OK,
    MOTHER,
    FATHER,
    BOTH
  }

  private final PairWiseConcordance mFatherCounts = new PairWiseConcordance();
  private final PairWiseConcordance mMotherCounts = new PairWiseConcordance();

  private final String mChild;
  private final String mFather;
  private final String mMother;
  private int mTrioConsistent = 0;
  private int mTrioInconsistent = 0;

  public TrioConcordance(String child, String father, String mother) {
    mChild = child;
    mFather = father;
    mMother = mother;
  }

  void add(Genotype father, Genotype mother, Genotype child) {
    mFatherCounts.add(father, child);
    mMotherCounts.add(mother, child);
  }

  // Increment the status of a three-way mendelian genotype test
  public void addTrioStatus(boolean nonMendelian) {
    if (nonMendelian) {
      mTrioInconsistent++;
    } else {
      mTrioConsistent++;
    }
  }

  double trioConsistency() {
    return (double) mTrioConsistent / (mTrioInconsistent + mTrioConsistent) * 100;
  }

  boolean isTrioInconsistent(int minVariants, double minConcordance) {
    return ((mTrioInconsistent + mTrioConsistent) >= minVariants) && (trioConsistency() < minConcordance);
  }

  Status getStatus(int minVariants, double minConcordance) {
    final boolean dadInconsistent = mFatherCounts.isInconsistent(minVariants, minConcordance);
    final boolean mumInconsistent = mMotherCounts.isInconsistent(minVariants, minConcordance);
    final boolean trioInconsistent = isTrioInconsistent(minVariants, minConcordance) || (mumInconsistent && dadInconsistent);
    // Be as specific as possible and identify if it is likely just w.r.t mother or father
    if (dadInconsistent && !mumInconsistent) {
      return Status.FATHER;
    } else if (mumInconsistent && !dadInconsistent) {
      return Status.MOTHER;
    } else if (trioInconsistent) {
      return Status.BOTH;
    }
    return Status.OK;
  }

  @Override
  public String toString() {
    return String.format("Concordance %s: F:%s  M:%s  F+M:%d/%d (%.2f%%)", mChild, mFatherCounts.toString(), mMotherCounts.toString(),
      mTrioConsistent, mTrioConsistent + mTrioInconsistent, trioConsistency());
  }

  void check(int minVariants, double minConcordance, PrintStream out) {
    switch (getStatus(minVariants, minConcordance)) {
      case BOTH:
        out.println("Sample " + mChild + " has less than " + minConcordance + " concordance with both parents. Check for incorrect pedigree or sample mislabelling.");
        break;
      case FATHER:
        out.println("Sample " + mChild + " has less than " + minConcordance + " concordance with the father (" + mFather + "). Check for incorrect pedigree or sample mislabelling.");
        break;
      case MOTHER:
        out.println("Sample " + mChild + " has less than " + minConcordance + " concordance with the mother (" + mMother + "). Check for incorrect pedigree or sample mislabelling.");
        break;
      default:
    }
  }


  static class PairWiseConcordance {

    static final int IGNORED = 0;   // Genotype status was not updated due to non-diploid genotypes or missing values
    static final int UNRELATED = 1; // Genotypes appear unrelated
    static final int PARENT = 2;    // Genotypes are consistent with being parent/child
    static final int SAME = 3;      // Genotypes are consistent with being parent/child or even the same sample

    int[] mCounts = new int[4];
    int mInformative = 0;

    // Only consider genotypes where both are diploid with no missing values
    void add(Genotype parent, Genotype child) {
      if (parent.length() != 2 || child.length() != 2 || parent.get(0) < 0 || child.get(0) < 0) {
        mCounts[IGNORED]++;
      } else {
        mInformative++;
        if (parent.equals(child)) {
          mCounts[SAME]++;
        } else {
          if (parent.contains(child.get(0)) || parent.contains(child.get(1))) {
            mCounts[PARENT]++;
          } else {
            mCounts[UNRELATED]++;
          }
        }
      }
    }

    public String toString() {
      return String.format("%d/%d (%.2f%%)", consistent(), size(), (double) consistent() / size() * 100);
    }


    /**
     * @param minVariants the minimum number of variants before a valid test is performed
     * @param minConcordance the concordance below which is considered inconsistent
     * @return true if the pairwise comparison indicates the samples are not concordant
     */
    boolean isInconsistent(int minVariants, double minConcordance) {
      return (size() >= minVariants) && ((double) consistent() / size() * 100 < minConcordance);
    }

    /**
     * @return the total number of non-missing comparisons
     */
    int size() {
      return mInformative;
    }

    /**
     * @return the total number of comparisons that were consistent with parentage
     */
    int consistent() {
      return mCounts[PARENT] + mCounts[SAME];
    }
  }
}
