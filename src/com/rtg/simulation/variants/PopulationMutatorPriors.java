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

package com.rtg.simulation.variants;

import java.util.Arrays;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.simulation.SimulationUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.variant.GenomePriorParams;

/**
 * Methods for choosing population variants according to priors stored in a GenomePriorParams
 */
@TestClass("com.rtg.simulation.variants.PriorPopulationVariantGeneratorTest")
public class PopulationMutatorPriors {

  enum VariantType {
    /** SNP mutation */
    SNP,
    /** multiple SNP mutation, same length as ref */
    MNP,
    /** deletion from ref */
    DELETE,
    /** insertion to ref */
    INSERT,
    /** deletion from ref and insertion (like MNP but different length to ref) */
    INSDEL
  }

  private final GenomePriorParams mPriors;

  /** Event rate for all mutations */
  private final double mRate;

  // Cumulative thresholds for selecting a variant type
  private final double mSnpThres;
  private final double mMnpThres;
  private final double mInsertThres;
  private final double mDeleteThres;
  private final double mInsDelThres;

  // Cumulative length distributions
  private final double[] mMnpLengthDist;
  private final double[] mIndelLengthDist;

  /**
   * Array of SNP probability threshold arrays for each reference (A C G T)
   * Each array will consist of the accumulated probability of each possible call.
   * Calls are in the order: A C G T
   */
  private final double[][] mSnpThresholds = new double[4][];

  /**
   * Constructor for mutation rates and threshold from priorities file
   * @param priors with priors probability
   */
  public PopulationMutatorPriors(final GenomePriorParams priors) {
    assert priors != null;
    mPriors = priors;

    double rate = 0;
    final double[] typeRates = new double[5];
    typeRates[0] = mPriors.genomeSnpRate(false) + mPriors.genomeSnpRate(true); // 0 = snp
    rate += typeRates[0];

    typeRates[1] = mPriors.genomeMnpEventRate(false) + mPriors.genomeMnpEventRate(true); // 1 = mnp
    rate += typeRates[1];

    final double indelRatePortion = mPriors.genomeIndelEventRate() / 3; // Assume this is divided evenly between insert, delete, insdel subtypes

    typeRates[2] = indelRatePortion; // 2 = insert
    rate += typeRates[2];

    typeRates[3] = indelRatePortion; // 3 = delete
    rate += typeRates[3];

    typeRates[4] = indelRatePortion;  // 4 = insdel
    rate += typeRates[4];

    //System.err.println("Type event rates: " + Arrays.toString(typeRates));

    mRate = rate; // Overall event rate
    final double[] t = SimulationUtils.cumulativeDistribution(typeRates);
    mSnpThres = t[0];
    mMnpThres = t[1];
    mInsertThres = t[2];
    mDeleteThres = t[3];
    mInsDelThres = t[4];
    mMnpLengthDist = SimulationUtils.cumulativeDistribution(mPriors.genomeMnpDistribution());
    mIndelLengthDist = SimulationUtils.cumulativeDistribution(mPriors.genomeIndelDistribution());

    final double[][] snpProbs = snpProbabilities();
    for (int i = 0; i < mSnpThresholds.length; ++i) {
      mSnpThresholds[i] = SimulationUtils.cumulativeDistribution(snpProbs[i]);
    }

    Diagnostic.developerLog(this.toString());
  }


  private double[][] snpProbabilities() {
    final double[][] probabilities = new double[4][];
    for (int i = 0; i < probabilities.length; ++i) {
      probabilities[i] = new double[4];
    }
    final String[] calls = {"A", "C", "G", "T"}; // Just consider homozygous priors
    for (int i = 0; i < calls.length; ++i) {
      final double[] probs = mPriors.getPriorDistr(calls[i]);
      for (int j = 0; j < probs.length; ++j) {
        if (j != i) { // Do not allow any possibility of a base transitioning to itself
          probabilities[j][i] = probs[j];
        }
      }
    }

    return probabilities;
  }

  /**
   * Choose the alt alleles for a SNP. Currently only biallelic SNPs (i.e. one alt allele)
   * @param r random generator
   * @param refBase the ordinal of the reference nucleotide
   * @return the alt alleles for the SNP.
   */
  byte[] chooseAltSnp(PortableRandom r, byte refBase) {
    return new byte[] {chooseAltBase(r, refBase)};
  }

  /** Choose an alternate base for the given reference base */
  private byte chooseAltBase(PortableRandom r, byte refBase) {
    if (refBase == 0) {
      //Special case, have asked for SNP on N nucleotide
      return 0;
    }

    final double[] thresholds = mSnpThresholds[refBase - 1];
    final double rand = r.nextDouble();
    final byte result;
    if (rand < thresholds[0]) {
      result = 1; // -> A
    } else if (rand < thresholds[1]) {
      result = 2; // -> C
    } else if (rand < thresholds[2]) {
      result = 3; // -> T
    } else {
      result = 4; // -> G
    }
    if (result == refBase) {
      throw new IllegalArgumentException("Invalid snp distribution for rand " + rand + " and refbase " + refBase);
    }
    return result;
  }


  /** @return overall rate of total mutations */
  public double rate() {
    return mRate;
  }

  /**
   * Choose a length for a mutation type
   * @param r random generator
   * @param type mutation type
   * @return next random length
   */
  public int chooseLength(final PortableRandom r, final VariantType type) {
    final double rand = r.nextDouble();

    switch (type) {
      case SNP:
        return 1;
      case MNP:
        return SimulationUtils.chooseFromCumulative(mMnpLengthDist, rand);
      case INSERT:
      case DELETE:
      case INSDEL:
        return SimulationUtils.chooseFromCumulative(mIndelLengthDist, rand) + 1; // the indel distribution starts from length 1
      default:
        throw new IllegalStateException("Unpossible");
    }
  }

  /**
   * Choose a mutation type
   * @param r random generator
   * @return mutation type
   */
  public VariantType chooseType(final PortableRandom r) {
    final double rand = r.nextDouble();
    if (rand < mSnpThres) {
      return VariantType.SNP;
    } else if (rand < mMnpThres) {
      return VariantType.MNP;
    } else if (rand < mInsertThres) {
      return VariantType.INSERT;
    } else if (rand < mDeleteThres) {
      return VariantType.DELETE;
    } else if (rand < mInsDelThres) {
      return VariantType.INSDEL;
    }
    throw new IllegalArgumentException("Invalid variant type distribution");
  }

  protected byte[] chooseAltMnp(PortableRandom random, final byte[] ref) {
    if (ref.length < 2) {
      throw new IllegalArgumentException("Minimum length of a MNP is 2");
    }
    final byte[] result = Arrays.copyOf(ref, ref.length);

    // Ends of the mnp must be different to the ref (otherwise it won't be a MNP) the rest can be random
    result[0] = chooseAltBase(random, ref[0]);
    result[ref.length - 1] = chooseAltBase(random, ref[ref.length - 1]);

    // Fill in the middle of the MNP with randomly selected bases
    for (int i = 1; i < ref.length - 1; ++i) {
      result[i] = (byte) (random.nextInt(4) + 1);
    }
    return result;
  }




  /**
   * Return a detailed human readable representation of this object.
   * It is intended that this show internal details of the
   * object structure that may be relevant to an implementor/debugger but not to
   * a user.
   *
   * @return string with object representation.
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("PopulationMutatorPriors:").append(StringUtils.LS);
    sb.append("Rate ").append(mRate).append(StringUtils.LS);
    sb.append("SnpThres ").append(mSnpThres).append(StringUtils.LS);
    sb.append("MnpThres ").append(mMnpThres).append(StringUtils.LS);
    sb.append("InsertThres ").append(mInsertThres).append(StringUtils.LS);
    sb.append("DeleteThres ").append(mDeleteThres).append(StringUtils.LS);
    sb.append("InsDelThres ").append(mInsDelThres).append(StringUtils.LS);
    sb.append("MnpLengthDist length ").append(mMnpLengthDist.length).append(StringUtils.LS);
    sb.append("MnpLengthDist: ").append(Arrays.toString(mMnpLengthDist)).append(StringUtils.LS);
    sb.append("IndelLengthDist length ").append(mIndelLengthDist.length).append(StringUtils.LS);
    sb.append("IndelLengthDist: ").append(Arrays.toString(mIndelLengthDist)).append(StringUtils.LS);
    sb.append("SnpDists:").append(StringUtils.LS);
    for (int i = 0; i < mSnpThresholds.length; ++i) {
      sb.append("SnpDists[").append(i + 1).append("]:").append(Arrays.toString(mSnpThresholds[i])).append(StringUtils.LS);
    }

    return sb.toString();
  }

}
