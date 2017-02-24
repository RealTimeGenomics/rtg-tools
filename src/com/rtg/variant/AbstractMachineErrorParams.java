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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.reader.Arm;
import com.rtg.reader.FastaUtils;
import com.rtg.util.Params;
import com.rtg.util.machine.MachineType;

/**
 * Abstract machine error parameters class
 */
@TestClass("com.rtg.variant.MachineErrorParamsTest")
public abstract class AbstractMachineErrorParams implements Params, PhredScaler {

  /**
   * Get a phred score from an ASCII quality character optionally
   * correcting it.
   * @param qualChar  original quality character.
   * @param readPos position on read of <code>qualChar</code>
   * @param arm which arm of paired end reads is this. Use {@code LEFT} if single end.
   * @return the possibly corrected phred score.
   */
  public final int getScaledPhredFromAscii(final char qualChar, int readPos, Arm arm) {
    assert qualChar >= FastaUtils.PHRED_LOWER_LIMIT_CHAR;
    final byte rawQuality = (byte) (qualChar - FastaUtils.PHRED_LOWER_LIMIT_CHAR);
    return getScaledPhred(rawQuality, readPos, arm);
  }

  /**
   * Get a phred score from a binary quality value optionally
   * correcting it.
   * @param quality original quality value.
   * @param readPos position on read of <code>qualChar</code>
   * @param arm which arm of paired end reads is this. Use {@code LEFT} if single end.
   * @return the possibly corrected phred score.
   */
  public abstract int getScaledPhred(final byte quality, int readPos, Arm arm);

  /**
   * Get the CG v1 small gap distribution for 0,1,2,3.
   * @return the gap distribution, always non-null.
   */
  public abstract double[] smallGapDistribution();

  /**
   * Get the CG v1 large gap distribution for 4,5,6,7,8.
   * @return the gap distribution, always non-null.
   */
  public abstract double[] gapDistribution();

  /**
   * Get the CG v1 overlap probability distribution for -4,-3,-2,-1,0.
   *
   * @return the overlap distribution, always non-null.
   */
  public abstract double[] overlapDistribution();

  /**
   * Get the CG v2 overlap probability distribution for -7,-6,-5,-4,-3,-2,-1,0.
   *
   * @return the overlap distribution, always non-null.
   */
  public abstract double[] overlapDistribution2();

  /**
   * Return the machine type specified in the priors
   *
   * @return the expected machine type
   */
  public abstract MachineType machineType();

  /**
   * True if the CG priors (in particular the overlap distribution) has been explicitly set.
   *
   * @return true for CG.
   */
  public abstract boolean isCG();

  /**
   * Get the optional quality calibration curve.
   *
   * @return null, or an array with 64 entries, each in the range 0..63.
   */
  protected abstract int[] qualityCurve();

  /**
   * Get the length distribution of sequencing machine deletion errors.
   * This does not include the underlying mutation distribution.
   * So the total deletion distribution should be the weighted sum
   * of this one and <code>deletionDistribution()</code>.
   *
   * @return an array that sums to 1.0.  Entry 0 will be 0.0.
   */
  public abstract double[] errorDelDistribution();

  /**
   * Get the sequencing machine error event rate for deletion events.
   * @return a probability between 0 and 1.
   */
  public abstract double errorDelEventRate();

  /**
   * Get the sequencing machine error rate for bases deleted.
   * @return a probability between 0 and 1.
   */
  public abstract double errorDelBaseRate();

  /**
   * Get the length distribution of sequencing machine insertion errors.
   * This does not include the underlying mutation distribution.
   * So the total insertion distribution should be the weighted sum
   * of this one and <code>insertionDistribution()</code>.
   *
   * @return an array that sums to 1.0.  Entry 0 will be 0.0.
   */
  public abstract double[] errorInsDistribution();

  /**
   * Get the sequencing machine error rate for insert events.
   * @return a probability between 0 and 1.
   */
  public abstract double errorInsEventRate();

  /**
   * Get the sequencing machine error rate for bases inserted.
   * @return a probability between 0 and 1.
   */
  public abstract double errorInsBaseRate();

  /**
   * Get the length distribution of sequencing machine MNP errors.
   * This includes SNP errors (when length equals 1).
   * This does not include the underlying mutation distribution.
   * So the total MNP distribution should be the weighted sum
   * of this one and <code>insertionDistribution()</code>.
   *
   * @return an array that sums to 1.0.  Entry 0 will be 0.0.
   */
  public abstract double[] errorMnpDistribution();

  /**
   * Get the sequencing machine error rate for an MNP event starting.
   * Since an MNP of length 1 is a SNP, this includes the probability
   * of a SNP error.  In fact, the SNP rate is roughly equal to this
   * MNP event rate times the average length of an MNP.
   *
   * @return a probability between 0 and 1.
   */
  public abstract double errorMnpEventRate();

  /**
   * Get the sequencing machine error rate for single bases changed.
   * This is calculated as the MNP event rate * the average length of a MNP.
   * @return a probability between 0 and 1.
   */
  public abstract double errorSnpRate();

  @Override
  public void close() throws IOException { }

}
