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
import java.util.Properties;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.MathUtils;
import com.rtg.util.PropertiesUtils;
import com.rtg.util.Utils;
import com.rtg.util.array.ArrayUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.machine.MachineType;

/**
 * A builder class for <code>MachineErrorParams</code>.
 */
@TestClass(value = { "com.rtg.variant.MachineErrorParamsTest" })
public class MachineErrorParamsBuilder {

  static final double[] CG_DEFAULT_OVERLAP_DIST = MathUtils.renormalize(new int[]{0, 8, 84, 8, 0});
  static final double[] CG_DEFAULT_GAP_DIST = MathUtils.renormalize(new int[]{0, 27, 64, 9, 0});
  static final double[] CG_DEFAULT_SMALL_GAP_DIST = MathUtils.renormalize(new int[]{90, 7, 3, 0});
  static final double[] CG_DEFAULT_OVERLAP2_DIST = MathUtils.renormalize(new int[]{25, 47, 104, 306, 445, 70, 3, 0});

  /** The proportion of insertions that are homozygous */
  protected double mErrorMnpEventRate, mErrorInsEventRate, mErrorDelEventRate;

  protected double[] mErrorMnpDistribution, mErrorInsDistribution, mErrorDelDistribution;

  protected int[] mQualityCurve;

  // CG v1
  protected double[] mGapDistribution;
  protected double[] mSmallGapDistribution;
  protected double[] mOverlapDistribution;

  // CG v2
  protected double[] mOverlapDistribution2;

  protected MachineType mMachine;


  /**
   * Creates a builder with initial default values from the
   * <code>human.properties</code> file.
   */
  public MachineErrorParamsBuilder() {
    try {
      errors("default");
    } catch (final Exception e) {
      throw new RuntimeException("Error reading default.properties", e);
    }
  }

  /**
   * Sets the name of the machine errors resource file to use. This reads the
   * resource file and sets all the probabilities.
   *
   * @param errors name of errors resource.
   * @throws InvalidParamsException if the resource file is invalid.
   * @throws IOException if the resource file cannot be read.
   */
  public MachineErrorParamsBuilder(String errors) throws IOException, InvalidParamsException {
    errors(errors);
  }

  /**
   * Construct builder from existing errors.
   *
   * @param errors name of errors resource.
   */
  public MachineErrorParamsBuilder(final AbstractMachineErrorParams errors) {
    mErrorMnpEventRate = errors.errorMnpEventRate();
    mErrorInsEventRate = errors.errorInsEventRate();
    mErrorDelEventRate = errors.errorDelEventRate();
    mErrorMnpDistribution = errors.errorMnpDistribution();
    mErrorInsDistribution = errors.errorInsDistribution();
    mErrorDelDistribution = errors.errorDelDistribution();
    mQualityCurve = errors.qualityCurve();
    mGapDistribution = errors.gapDistribution();
    mSmallGapDistribution = errors.smallGapDistribution();
    mOverlapDistribution = errors.overlapDistribution();
    mOverlapDistribution2 = errors.overlapDistribution2();
    mMachine = errors.machineType();
  }

  /**
   * Sets the name of the machine errors resource file to use. This reads the
   * resource file and sets all the probabilities.
   *
   * @param errors name of errors resource. Default is default.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if the resource file is invalid.
   * @throws IOException if the resource file cannot be read.
   */
  public MachineErrorParamsBuilder errors(final String errors) throws InvalidParamsException, IOException {
    Diagnostic.developerLog("Loading machine errors for: " + errors);
    //System.out.println("Loading machine errors for: " + errors);
    final Properties pr = PropertiesUtils.getPriorsResource(errors, PropertiesUtils.PropertyType.ERROR_PROPERTY);
    mErrorMnpDistribution = GenomePriorParamsBuilder.parseDistribution(errors, pr, "error_mnp_distribution", 1);
    mErrorInsDistribution = GenomePriorParamsBuilder.parseDistribution(errors, pr, "error_ins_distribution", 1);
    mErrorDelDistribution = GenomePriorParamsBuilder.parseDistribution(errors, pr, "error_del_distribution", 1);
    mErrorMnpEventRate = GenomePriorParamsBuilder.getDouble(errors, pr, "error_mnp_event_rate");
    mErrorInsEventRate = GenomePriorParamsBuilder.getDouble(errors, pr, "error_ins_event_rate");
    mErrorDelEventRate = GenomePriorParamsBuilder.getDouble(errors, pr, "error_del_event_rate");

    // parse the optional quality calibration curve
    if (pr.containsKey(MachineErrorParams.QCALIB_KEY)) {
      final String curve = pr.getProperty(MachineErrorParams.QCALIB_KEY);
      final String[] nums = curve.split(", *");
      if (nums.length != MachineErrorParams.QUALITIES) {
        throw new InvalidParamsException(ErrorType.PRIOR_KEY_VALUE_INVALID, curve, MachineErrorParams.QCALIB_KEY, errors);
      }
      mQualityCurve = new int[nums.length];
      for (int q = 0; q < nums.length; ++q) {
        mQualityCurve[q] = Integer.parseInt(nums[q]);
      }
      checkQualityCurve(mQualityCurve); // check the values
    }
    if (pr.containsKey("machine_type")) {
      try {
        mMachine = MachineType.valueOf(pr.getProperty("machine_type"));
      } catch (final IllegalArgumentException e) {
        mMachine = null;
      }
    }
    mOverlapDistribution = getDistribution(pr, "overlap", CG_DEFAULT_OVERLAP_DIST, errors);
    mSmallGapDistribution = getDistribution(pr, "smallgap", CG_DEFAULT_SMALL_GAP_DIST, errors);
    mGapDistribution = getDistribution(pr, "gap", CG_DEFAULT_GAP_DIST, errors);
    mOverlapDistribution2 = getDistribution(pr, "overlap_2", CG_DEFAULT_OVERLAP2_DIST, errors);

    return this;
  }

  private double[] getDistribution(Properties pr, String key, double[] defaultDist, String errors) {
    final double[] dist;
    if (pr.containsKey(key)) {
      final String distStr = pr.getProperty(key);
      try {
        dist = MathUtils.renormalize(ArrayUtils.parseIntArray(distStr));
        if (dist.length < defaultDist.length) {
          throw new InvalidParamsException(ErrorType.PRIOR_KEY_VALUE_INVALID, distStr, key, errors);
        }
      } catch (final NumberFormatException e) {
        throw new InvalidParamsException(ErrorType.PRIOR_KEY_VALUE_INVALID, distStr, key, errors);
      }
    } else {
      dist = defaultDist;
    }
    return dist;
  }

  static double parseDouble(final String prior, final String val, final String key) {
    final double ret;
    try {
      ret = Double.valueOf(val);
    } catch (final NumberFormatException e) {
      throw new InvalidParamsException(ErrorType.PRIOR_KEY_VALUE_INVALID, val, key, prior);
    }
    if (ret < 0.0 || ret > 1.0) {
      throw new InvalidParamsException(ErrorType.PRIOR_KEY_VALUE_INVALID, val, key, prior);
    }
    return ret;
  }

  /**
   * Sets MNP event rate error prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public MachineErrorParamsBuilder errorMnpEventRate(final double prior) {
    mErrorMnpEventRate = prior;
    return this;
  }

  /**
   * Sets insert error prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public MachineErrorParamsBuilder errorInsEventRate(final double prior) {
    mErrorInsEventRate = prior;
    return this;
  }

  /**
   * Sets deletion error event prior.
   *
   * @param prior value.
   * @return this builder, so calls can be chained.
   */
  public MachineErrorParamsBuilder errorDelEventRate(final double prior) {
    mErrorDelEventRate = prior;
    return this;
  }

  /**
   * Set the length distribution of machine MNP errors. Note that
   * <code>lengths[0]</code> refers to inserts of length 1.
   *
   * @param lengths the probability of each length (1..). Must sum to 1.0.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if distribution is invalid.
   */
  public MachineErrorParamsBuilder errorMnpDistribution(final double[] lengths) {
    checkDistribution(lengths);
    mErrorMnpDistribution = new double[lengths.length + 1];
    System.arraycopy(lengths, 0, mErrorMnpDistribution, 1, lengths.length);
    return this;
  }

  /**
   * Set the length distribution of machine insertion errors. Note that
   * <code>lengths[0]</code> refers to inserts of length 1.
   *
   * @param lengths the probability of each length (1..). Must sum to 1.0.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if distribution is invalid.
   */
  public MachineErrorParamsBuilder errorInsDistribution(final double[] lengths) {
    checkDistribution(lengths);
    mErrorInsDistribution = new double[lengths.length + 1];
    System.arraycopy(lengths, 0, mErrorInsDistribution, 1, lengths.length);
    return this;
  }

  /**
   * Set the length distribution of machine deletion errors. Note that
   * <code>lengths[0]</code> refers to deletes of length 1.
   *
   * @param lengths the probability of each length (1..). Must sum to 1.0.
   * @return this builder, so calls can be chained.
   * @throws InvalidParamsException if distribution is invalid.
   */
  public MachineErrorParamsBuilder errorDelDistribution(final double[] lengths) {
    checkDistribution(lengths);
    mErrorDelDistribution = new double[lengths.length + 1];
    System.arraycopy(lengths, 0, mErrorDelDistribution, 1, lengths.length);
    return this;
  }

  /**
   * Set the machine type.
   *
   * @param machine the machine type.
   * @return this builder, so calls can be chained.
   */
  public MachineErrorParamsBuilder machine(final MachineType machine) {
    mMachine = machine;
    return this;
  }

  /**
   * Creates a MachineErrorParams using the current builder.
   *
   * @return the new parameters object.
   * @throws InvalidParamsException if any parameters are invalid.
   */
  public MachineErrorParams create() {
    checkProbability(mErrorInsEventRate);
    checkProbability(mErrorDelEventRate);
    checkDistribution(mErrorMnpDistribution);
    checkDistribution(mErrorInsDistribution);
    checkDistribution(mErrorDelDistribution);
    if (mQualityCurve != null) {
      checkQualityCurve(mQualityCurve);
    }
    checkDistribution(mGapDistribution);
    checkDistribution(mOverlapDistribution);
    checkDistribution(mSmallGapDistribution);
    checkDistribution(mOverlapDistribution2);
    return new MachineErrorParams(this);
  }

  private static void checkProbability(final double value) {
    if (value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("rate must be 0.0 .. 1.0, not " + Utils.realFormat(value, 6));
    }
  }

  static void checkDistribution(final double[] distrib) {
    double sum = 0.0;
    for (final double element : distrib) {
      checkProbability(element);
      sum += element;
    }
    if (Math.abs(sum - 1.0) > 0.0001) {
      throw new IllegalArgumentException("distribution must sum to 1.0, not " + sum);
    }
  }

  private static void checkQualityCurve(final int[] qualities) {
    if (qualities.length != MachineErrorParams.QUALITIES) {
      throw new InvalidParamsException(ErrorType.INVALID_QUALITY_LENGTH, MachineErrorParams.QCALIB_KEY);
    }
    for (int quality : qualities) {
      if (quality < 0 || quality >= MachineErrorParams.QUALITIES) {
        throw new InvalidParamsException(ErrorType.INVALID_QUALITY);
      }
    }
  }


}
