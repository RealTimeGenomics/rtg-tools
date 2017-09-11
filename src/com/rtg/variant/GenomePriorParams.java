/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.variant;

import static com.rtg.util.StringUtils.LS;

import java.util.HashMap;
import java.util.Map;

import com.rtg.mode.DNA;
import com.rtg.util.ObjectParams;
import com.rtg.util.Utils;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 * <pre>
 * The naming convention for these properties is that each name consists of a sequence:
 *  genome_M_B_R_H
 *  genome_M_distribution_H
 *
 *  M(utation):
 *     snp - single nucleotide polymorphism
 *     mnp - multi-nucleotide polymorphism
 *     ins - insertion
 *     del - deletion
 *     indel - a single measure which is equal for both insertions and deletions
 *     com - complex
 *  B(asis):
 *     base - measure per base position
 *     event - measure per event (eg adjacent deletions counted as a single event)
 *  R(ate):
 *     rate - frequency wrt the whole genome
 *     fraction - proportion of measure that is homozygous (the heterozygous case can be computed by subtracting from 1)
 *  H
 *          - sum of heterozygous and homozygous cases
 *     homo - homozygous
 *     hetero - heterozygous
 *
 * distribution - a distribution of frequencies vs length (these always sum to 1.0)
 * </pre>
 */
public class GenomePriorParams extends ObjectParams implements Integrity {

  static final double AVERAGE_HOMO_MNP_LENGTH = 2.1;
  static final double AVERAGE_HETERO_MNP_LENGTH = 5.8;

  /**
   * Creates a GenomePriorParams builder.
   * @return the builder.
   */
  public static GenomePriorParamsBuilder builder() {
    return new GenomePriorParamsBuilder();
  }

  private final double mGenomeSnpRateHetero;
  private final double mGenomeSnpRateHomo;
  private final double mGenomeIndelEventRate;
  private final double mGenomeIndelEventFraction;
  private final double mGenomeMnpBaseRateHetero;
  private final double mGenomeMnpBaseRateHomo;
  private final double mGenomeIndelLengthDecay;
  private final double mDenovoRef;
  private final double mLogDenovoRef;
  private final double mDenovoNonRef;
  private final double mLogDenovoNonRef;
  private final double mContraryProbability;

  private final HashMap<String, HashMap<String, Double>> mPriorMap;

  private final double[] mGenomeMnpDistribution, mGenomeIndelDistribution;
  private final double[] mAlleleProbabilityLn;

  /**
   * @param builder the builder object.
   */
  public GenomePriorParams(final GenomePriorParamsBuilder builder) {
    super();
    mGenomeSnpRateHomo = builder.mGenomeSnpRateHomo;
    mGenomeSnpRateHetero = builder.mGenomeSnpRateHetero;
    mGenomeMnpBaseRateHetero = builder.mGenomeMnpBaseRateHetero;
    mGenomeMnpBaseRateHomo = builder.mGenomeMnpBaseRateHomo;
    mGenomeMnpDistribution = builder.mGenomeMnpDistribution;
    mGenomeIndelEventRate = builder.mGenomeIndelEventRate;
    mGenomeIndelEventFraction = builder.mGenomeIndelEventFraction;
    mGenomeIndelDistribution = builder.mGenomeIndelDistribution;
    mGenomeIndelLengthDecay = builder.mGenomeIndelLengthDecay;
    mAlleleProbabilityLn = builder.mAlleleProbabilityLn;
    mDenovoRef = builder.mDenovoRef;
    mDenovoNonRef = builder.mDenovoNonRef;
    mLogDenovoRef = Math.log(mDenovoRef);
    mLogDenovoNonRef = Math.log(mDenovoNonRef);
    mPriorMap = builder.mPriorMap;
    mContraryProbability = builder.mContraryProbability;
  }

  /**
   * Get the rate of SNPs per base pair.
   * @param hetero true for heterozygous SNPs.
   * @return a probability between 0 and 1.
   */
  public double genomeSnpRate(final boolean hetero) {
    if (hetero) {
      return mGenomeSnpRateHetero;
    } else {
      return mGenomeSnpRateHomo;
    }
  }

  /**
   * Get the proportion of base pairs that are affected by MNPs.
   * @param hetero true for heterozygous MNPs.
   * @return a probability between 0 and 1.
   */
  public double genomeMnpBaseRate(final boolean hetero) {
    if (hetero) {
      return mGenomeMnpBaseRateHetero;
    } else {
      return mGenomeMnpBaseRateHomo;
    }
  }

  /**
   * Get the rate of MNP events.
   * @param hetero true for heterozygous MNPs.
   * @return a probability between 0 and 1.
   */
  public double genomeMnpEventRate(final boolean hetero) {
    if (hetero) {
      return genomeMnpBaseRate(true) / AVERAGE_HETERO_MNP_LENGTH;
    } else {
      return genomeMnpBaseRate(false) / AVERAGE_HOMO_MNP_LENGTH;
    }
  }

  /**
   * Get the length distribution for MNPs.
   * Entry i is the probability of the MNP having length i.
   * So entries 0 and 1 are both 0.0.
   *
   * @return a (read-only) array of doubles that sums to 1.0.
   */
  public double[] genomeMnpDistribution() {
    return mGenomeMnpDistribution;
  }

  /**
   * Get the average rate of indel events for heterozygous or homozygous indels.
   * @param hetero true for heterozygous indels.
   * @return a probability between 0 and 1.
   */

  public double genomeIndelEventRate(final boolean hetero) {
    if (hetero) {
      return mGenomeIndelEventRate * (1 - mGenomeIndelEventFraction);
    } else {
      return mGenomeIndelEventRate * mGenomeIndelEventFraction;
    }
  }

  /**
   * Get the total rate of indel events in the genome.
   * @return a probability between 0 and 1.
   */
  public double genomeIndelEventRate() {
    return mGenomeIndelEventRate;
  }

  /**
   * Get the length distribution for indels.
   * Entry i is the probability of the indels having length i + 1.
   *
   * @return an array of doubles that sums to 1.0.
   */
  public double[] genomeIndelDistribution() {
    return mGenomeIndelDistribution;
  }

  /**
   * Get the fraction of indels that are homozygous.
   * Subtract this from 1.0 to get the fraction that are heterozygous.
   *
   * @return a probability between 0 and 1.
   */
  public double genomeIndelEventFraction() {
    return mGenomeIndelEventFraction;
  }

  /**
   * Get the rate that indel lengths decay beyond the end of the provided distribution
   * P(length) = P(length - 1) * decay
   *
   * @return the rate lengths should decay beyond the end of provided distributions
   */
  public double genomeIndelLengthDecay() {
    return mGenomeIndelLengthDecay;
  }
  /**
   * @return the de Novo prior for parents equal to reference
   */
  public double denovoRef() {
    return mDenovoRef;
  }
  /**
   * @return the de Novo prior for parents equal to reference in log scale
   */
  public double logDenovoRef() {
    return mLogDenovoRef;
  }
  /**
   * @return the de Novo prior for parents that are not equal to reference
   */
  public double denovoNonRef() {
    return mDenovoNonRef;
  }

  /**
   * @return the de Novo prior for parents that are not equal to reference in log scale
   */
  public double logDenovoNonRef() {
    return mLogDenovoNonRef;
  }

  /**
   * Return prior distribution for the given call
   * @param call input call, homozygous and heterozygous
   * @return prior distribution
   */
  public double[] getPriorDistr(final String call) {
    final Map<String, Double> map = mPriorMap.get(call);
    assert map != null : "No prior distribution for call " + call;
    final double[] prior = new double[4];
    int cnt = 0;
    for (final Map.Entry<String, Double> re : map.entrySet()) {
      ++cnt;
      final int rei = DNA.valueOf(re.getKey()).ordinal() - 1;
      final double ra = re.getValue();
      prior[rei] = ra;
    }
    for (final double r : prior) {
      assert r > 0.0 && r < 1.0 && !Double.isNaN(r);
    }
    assert cnt == 4 : "cnt=" + cnt + " call=" + call + " map=" + map;
    return prior;
  }
  /**
   * @param alleleCount the number of alleles you would like a probability for
   * @return the probability of the given number of alleles occurring in the population
   */
  public double getAlleleFrequencyLnProbability(int alleleCount) {
    return mAlleleProbabilityLn[alleleCount];
  }

  /**
   * @return probability that a piece of evidence if contrary to a somatic or de novo call.
   */
  public double contraryProbability() {
    return mContraryProbability;
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mGenomeSnpRateHetero >= 0.0 && mGenomeSnpRateHetero < 1.0 && !Double.isNaN(mGenomeSnpRateHetero));
    Exam.assertTrue(mGenomeSnpRateHomo >= 0.0 && mGenomeSnpRateHomo < 1.0 && !Double.isNaN(mGenomeSnpRateHomo));
    Exam.assertTrue(mGenomeSnpRateHetero + mGenomeSnpRateHomo < 1.0);
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    Exam.assertTrue(integrity());
    return true;
  }

  @Override
  public String toString() {
    return "    "
    + "heterozygous prior=" + Utils.realFormat(genomeSnpRate(true), 7)
    + " homozygous prior=" + Utils.realFormat(genomeSnpRate(false), 7)
    + LS;
  }
}
