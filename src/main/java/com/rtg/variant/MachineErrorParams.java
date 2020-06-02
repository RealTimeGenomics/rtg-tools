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

import java.io.IOException;

import com.rtg.reader.Arm;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.Utils;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;
import com.rtg.util.machine.MachineType;

/**
 * <pre>
 * The naming convention for these properties is that each name consists of a sequence:
 *  error_M_B_R_H
 *  error_M_distribution_H
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
public final class MachineErrorParams extends AbstractMachineErrorParams implements Integrity {

  static final String QCALIB_KEY = "quality_curve";

  /** How many quality values we expect. */
  static final int QUALITIES = 64;

  /**
   * Creates a GenomePriorParams builder.
   * @return the builder.
   */
  public static MachineErrorParamsBuilder builder() {
    return new MachineErrorParamsBuilder();
  }

  /**
   * Creates a GenomePriorParams builder with given errors.
   * @param errors name of errors resource.
   * @throws InvalidParamsException if the resource file is invalid.
   * @throws IOException if the resource file cannot be read.
   * @return the builder.
   */
  public static MachineErrorParamsBuilder builder(String errors) throws IOException {
    return new MachineErrorParamsBuilder(errors);
  }

  private final double mErrorSnpRate, mErrorMnpEventRate, mErrorInsBaseRate, mErrorInsEventRate, mErrorDelBaseRate, mErrorDelEventRate;

  private final double[] mErrorInsDistribution, mErrorMnpDistribution, mErrorDelDistribution;

  private final int[] mQualityCurve;

  private final double[] mGapDistribution;

  private final double[] mSmallGapDistribution;

  private final double[] mOverlapDistribution;

  private final double[] mOverlapDistribution2;

  private final boolean mCG;

  private final MachineType mMachine;


  /**
   * @param builder the builder object.
   */
  public MachineErrorParams(final MachineErrorParamsBuilder builder) {
    super();
    mErrorSnpRate = builder.mErrorMnpEventRate * GenomePriorParamsBuilder.averageLength(builder.mErrorMnpDistribution);
    mErrorMnpEventRate = builder.mErrorMnpEventRate;
    mErrorInsBaseRate = builder.mErrorInsEventRate * GenomePriorParamsBuilder.averageLength(builder.mErrorInsDistribution);
    mErrorInsEventRate = builder.mErrorInsEventRate;
    mErrorDelBaseRate = builder.mErrorDelEventRate * GenomePriorParamsBuilder.averageLength(builder.mErrorDelDistribution);
    mErrorDelEventRate = builder.mErrorDelEventRate;
    mErrorMnpDistribution = builder.mErrorMnpDistribution;
    mErrorInsDistribution = builder.mErrorInsDistribution;
    mErrorDelDistribution = builder.mErrorDelDistribution;
    mQualityCurve = builder.mQualityCurve;
    mGapDistribution = builder.mGapDistribution;
    mSmallGapDistribution = builder.mSmallGapDistribution;
    mOverlapDistribution = builder.mOverlapDistribution;
    mOverlapDistribution2 = builder.mOverlapDistribution2;
    mMachine = builder.mMachine;
    mCG = mMachine == MachineType.COMPLETE_GENOMICS || mMachine == MachineType.COMPLETE_GENOMICS_2;
  }

  @Override
  public double errorSnpRate() {
    return mErrorSnpRate;
  }

  @Override
  public double errorMnpEventRate() {
    return mErrorMnpEventRate;
  }

  @Override
  public double[] errorMnpDistribution() {
    return mErrorMnpDistribution;
  }

  @Override
  public double errorInsBaseRate() {
    return mErrorInsBaseRate;
  }

  @Override
  public double errorInsEventRate() {
    return mErrorInsEventRate;
  }

  @Override
  public double[] errorInsDistribution() {
    return mErrorInsDistribution;
  }

  @Override
  public double errorDelBaseRate() {
    return mErrorDelBaseRate;
  }

  @Override
  public double errorDelEventRate() {
    return mErrorDelEventRate;
  }

  @Override
  public double[] errorDelDistribution() {
    return mErrorDelDistribution;
  }

  @Override
  protected //Only used for testing - see getScaledPhred
  int[] qualityCurve() {
    return mQualityCurve;
  }

  @Override
  public boolean isCG() {
    return mCG;
  }

  @Override
  public MachineType machineType() {
    return mMachine;
  }

  @Override
  public double[] overlapDistribution() {
    return mOverlapDistribution;
  }

  @Override
  public double[] gapDistribution() {
    return mGapDistribution;
  }

  @Override
  public double[] smallGapDistribution() {
    return mSmallGapDistribution;
  }

  @Override
  public double[] overlapDistribution2() {
    return mOverlapDistribution2;
  }

  @Override
  public int getScaledPhred(final byte rawQuality, int readPos, Arm arm) {
    //TODO support Arm
    if (mQualityCurve != null) {
      if (rawQuality >= mQualityCurve.length) {
        return mQualityCurve[mQualityCurve.length - 1];
      } else {
        return mQualityCurve[rawQuality];
      }
    } else {
      return rawQuality;
    }
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(0 < mErrorMnpEventRate);
    //Assert.assertTrue(mErrorMnpEventRate < mErrorSnpRate); not true if only 1 long SNPs
    //Assert.assertTrue(mErrorSnpRate < 1.0); not true if only one long SNPs
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
    + " insert errors=" + Utils.realFormat(errorInsBaseRate(), 7)
    + " delete errors=" + Utils.realFormat(errorDelBaseRate(), 7)
    + LS;
  }
}
