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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PropertiesUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.ErrorType;

/**
 * A builder class for <code>GenomePriorParams</code>. This is in a separate
 * file because it is so big.
 */
@TestClass(value = { "com.rtg.variant.GenomePriorParamsTest" })
public final class GenomePriorParamsBuilder {

  private static final char COLON = ':';
  private static final String[] BASES = {"A", "C", "G", "T"};

  /** Total SNP rate is <code>mGenomeSnpRateHetero + mGenomeSnpRateHomo</code> */
  double mGenomeSnpRateHetero, mGenomeSnpRateHomo;

  /**
   * Total MNP rate is
   * <code>mGenomeMnpBaseRateHetero + mGenomeMnpBaseRateHomo</code>
   */
  double mGenomeMnpBaseRateHetero, mGenomeMnpBaseRateHomo;

  double[] mGenomeMnpDistribution;

  /** indel rate */
  double mGenomeIndelEventRate;

  /** The proportion of insertions that are homozygous */
  double mGenomeIndelEventFraction;

  double[] mGenomeIndelDistribution;

  double mGenomeIndelLengthDecay;

  HashMap<String, HashMap<String, Double>> mPriorMap = null;
  double[] mAlleleProbabilityLn = new double[6]; //allow for up to 5 alleles in complex calling
  double mDenovoRef;
  double mDenovoNonRef;
  double mContraryProbability = 0.0001;

  /**
   * Creates a builder with initial default values from the
   * <code>human.properties</code> file.
   */
  public GenomePriorParamsBuilder() {
    try {
      genomePriors("human");
    } catch (final Exception e) {
      throw new RuntimeException("Error reading human.properties", e);
    }
  }

  /**
   * Sets the name of the priors resource to use. This reads the resource file
   * and sets all the probabilities.
   *
   * @param prior name of priors resource. Default is human.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if the resource file is invalid.
   * @throws IOException if the resource file cannot be read.
   */
  public GenomePriorParamsBuilder genomePriors(final String prior) throws IOException {
    final Properties pr = PropertiesUtils.getPriorsResource(prior, PropertiesUtils.PropertyType.PRIOR_PROPERTY);
    mGenomeSnpRateHetero = getDouble(prior, pr, "genome_snp_rate_hetero");
    mGenomeSnpRateHomo = getDouble(prior, pr, "genome_snp_rate_homo");
    mGenomeMnpBaseRateHetero = getDouble(prior, pr, "genome_mnp_base_rate_hetero");
    mGenomeMnpBaseRateHomo = getDouble(prior, pr, "genome_mnp_base_rate_homo");
    mGenomeMnpDistribution = parseDistribution(prior, pr, "genome_mnp_distribution", 2);
    mGenomeIndelEventRate = getDouble(prior, pr, "genome_indel_event_rate");
    mGenomeIndelEventFraction = getDouble(prior, pr, "genome_indel_event_fraction");
    mGenomeIndelDistribution = parseDistribution(prior, pr, "genome_indel_distribution", 0);
    mGenomeIndelLengthDecay = getDouble(prior, pr, "error_indel_length_decay");
    mDenovoRef = getDouble(prior, pr, "denovo_reference_rate");
    mDenovoNonRef = getDouble(prior, pr, "denovo_non_reference_rate");
    mPriorMap = load(pr, prior);
    setupAlleleFrequency(mPriorMap);
    return this;
  }

  /**
   * Compute the probability of N alleles appearing in a population at a
   * specific position in the genome
   * @param priorMap the prior distribution for calls, from the priors file.
   */
  private void setupAlleleFrequency(HashMap<String, HashMap<String, Double>> priorMap) {
    final double[] freq = new double[5];
    for (final Entry<String, HashMap<String, Double>> call : priorMap.entrySet()) {
      for (final Entry<String, Double> ref : call.getValue().entrySet()) {
        final Set<String> set = new HashSet<>();
        set.add(ref.getKey());
        Collections.addAll(set, StringUtils.split(call.getKey(), COLON));
        freq[set.size() - 1] += ref.getValue();
      }
    }
    // Hackity Hack TODO do something better for 4 and 5
    freq[3] = freq[2] / 1000;
    freq[4] = freq[3] / 1000;

    //normalize
    double sum = 0;
    for (double aFreq : freq) {
      sum += aFreq;
    }
    mAlleleProbabilityLn[0] = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < freq.length; ++i) {
      mAlleleProbabilityLn[i + 1] = Math.log(freq[i] / sum);
    }
  }

  private void putGenomePrior(HashMap<String, HashMap<String, Double>> priorMap, String ref, String call, double rate) {
    final HashMap<String, Double> ma = priorMap.get(call);
    final HashMap<String, Double> map;
    if (ma == null) {
      map = new HashMap<>();
      priorMap.put(call, map);
    } else {
      map = ma;
    }
    assert map.get(ref) == null : "ref=" + ref + " map=" + map + " call=" + call;
    map.put(ref, rate);
  }

  private HashMap<String, HashMap<String, Double>> load(Properties pr, String prior) {
    final HashMap<String, HashMap<String, Double>> priorMap = new HashMap<>();
    for (String ref : BASES) {
      //haploid
      for (String call : BASES) {
        putGenomePrior(priorMap, ref, call, getDouble(prior, pr, ref.toLowerCase(Locale.ROOT) + "_" + call.toLowerCase(Locale.ROOT)));
      }
      //diploid
      for (String call : BASES) {
        for (String allele2 : BASES) {
          String lookupKey = ref.toLowerCase(Locale.ROOT) + "_" + call.toLowerCase(Locale.ROOT) + "_" + allele2.toLowerCase(Locale.ROOT);
          if (!pr.containsKey(lookupKey)) {
            lookupKey = ref.toLowerCase(Locale.ROOT) + "_" + allele2.toLowerCase(Locale.ROOT) + "_" + call.toLowerCase(Locale.ROOT);
          }
          putGenomePrior(priorMap, ref, call + COLON + allele2, getDouble(prior, pr, lookupKey));
        }
      }
    }
    return priorMap;
  }

  /**
   * Sets heterozygous SNP prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder genomeSnpRateHetero(final double prior) {
    mGenomeSnpRateHetero = prior;
    return this;
  }

  /**
   * Sets heterozygous SNP prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder genomeSnpRateHomo(final double prior) {
    mGenomeSnpRateHomo = prior;
    return this;
  }

  /**
   * Sets the insertion prior. This is the probability of an insertion (of any
   * length) starting at a given nucleotide.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder genomeIndelEventRate(final double prior) {
    mGenomeIndelEventRate = prior;
    return this;
  }

  /**
   * Sets the probability of an insertion being homozygous. The probability of
   * it being heterozygous is <code>1 - prior</code>.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder genomeIndelEventFraction(final double prior) {
    mGenomeIndelEventFraction = prior;
    return this;
  }

  /**
   * Sets the length distribution of inserts and deletions.
   *
   * @param lengths the probability of each length (0..). Must sum to 1.0.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if the distribution is invalid
   */
  public GenomePriorParamsBuilder genomeIndelDistribution(final double[] lengths) {
    checkDistribution(lengths);
    mGenomeIndelDistribution = new double[lengths.length];
    System.arraycopy(lengths, 0, mGenomeIndelDistribution, 0, lengths.length);
    return this;
  }

  /**
   * Set the rate that indel lengths decay beyond the end of the provided
   * distribution P(length) = P(length - 1) * decay
   *
   * @param decay the rate which lengths should decay at
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if distribution is invalid.
   */
  public GenomePriorParamsBuilder genomeIndelLengthDecay(final double decay) {
    mGenomeIndelLengthDecay = decay;
    return this;
  }

  /**
   * Sets heterozygous MNP prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder genomeMnpBaseRateHetero(final double prior) {
    mGenomeMnpBaseRateHetero = prior;
    return this;
  }

  /**
   * Sets heterozygous MNP prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder genomeMnpBaseRateHomo(final double prior) {
    mGenomeMnpBaseRateHomo = prior;
    return this;
  }

  /**
   * Sets the length distribution of MNPs.
   *
   * @param lengths the probability of each length (1..). Must sum to 1.0.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if the distribution is invalid
   */
  public GenomePriorParamsBuilder genomeMnpDistribution(final double[] lengths) {
    checkDistribution(lengths);
    mGenomeMnpDistribution = new double[lengths.length + 2];
    System.arraycopy(lengths, 0, mGenomeMnpDistribution, 2, lengths.length);
    return this;
  }

  /**
   * Set the de Novo mutation prior to use when parents are equal to reference
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder denovoRef(final double prior) {
    mDenovoRef = prior;
    return this;
  }

  /**
   * Set the de Novo mutation prior to use when parents aren't equal to reference
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public GenomePriorParamsBuilder denovoNonRef(final double prior) {
    mDenovoNonRef = prior;
    return this;
  }

  /**
   * The probability applied to contrary evidence for a somatic or de novo call.
   * A value of 1 means that contrary evidence is not further penalized.
   * @param p probability of contrary evidence
   * @return this, for chaining
   */
  public GenomePriorParamsBuilder contraryProbability(final double p) {
    if (p <= 0 || p > 1) {
      throw new IllegalArgumentException();
    }
    mContraryProbability = p;
    return this;
  }

  /**
   * Creates a GenomePriorParams using the current builder.
   *
   * @return the new GenomePriorParams
   * @throws InvalidParamsException if any parameters are invalid.
   */
  public GenomePriorParams create() {
    checkProbability(mGenomeIndelEventRate);
    checkProbability(mGenomeIndelEventFraction);
    checkProbability(mGenomeIndelLengthDecay);
    checkDistribution(mGenomeMnpDistribution);
    checkDistribution(mGenomeIndelDistribution);
    return new GenomePriorParams(this);
  }

  static double[] parseDistribution(final String prior, final Properties pr, final String key, final int start) {
    final String distrib = pr.getProperty(key);
    if (distrib == null) {
      throw new InvalidParamsException(ErrorType.PROPS_KEY_NOT_FOUND, key, prior);
    }
    final String[] words = distrib.split(", *");
    final double[] result = new double[words.length + start];
    try {
      for (int i = 0; i < words.length; ++i) {
        result[i + start] = MachineErrorParamsBuilder.parseDouble(prior, words[i], key);
      }
      MachineErrorParamsBuilder.checkDistribution(result);
    } catch (final IllegalArgumentException e) {
      throw new InvalidParamsException(ErrorType.PRIOR_KEY_VALUE_INVALID, distrib, key, prior);
    }
    return result;
  }

  static double getDouble(final String prior, final Properties pr, final String key) {
    final String val = pr.getProperty(key);
    if (val == null) {
      throw new InvalidParamsException(ErrorType.PROPS_KEY_NOT_FOUND, key, prior);
    }
    return MachineErrorParamsBuilder.parseDouble(prior, val, key);
  }

  private static void checkProbability(final double value) {
    if (value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("rate must be 0.0 .. 1.0, not " + Utils.realFormat(value, 6));
    }
  }

  private static void checkDistribution(final double[] distrib) {
    double sum = 0.0;
    for (final double element : distrib) {
      checkProbability(element);
      sum += element;
    }
    if (Math.abs(sum - 1.0) > 0.0001) {
      throw new IllegalArgumentException("distribution must sum to 1.0, not " + sum);
    }
  }

  /**
   * Calculates the average length, given a probability distribution of lengths.
   * @param dist an array of doubles that sums to 1.0
   * @return the average length.
   */
  public static double averageLength(final double[] dist) {
    double averageLength = 0.0;
    for (int i = 0; i < dist.length; ++i) {
      averageLength += i * dist[i];
    }
    return averageLength;
  }
}
