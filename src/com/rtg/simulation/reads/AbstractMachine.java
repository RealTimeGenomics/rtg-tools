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

package com.rtg.simulation.reads;

import java.util.Arrays;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.alignment.ActionsHelper;
import com.rtg.mode.DNA;
import com.rtg.reader.PrereadType;
import com.rtg.reader.SdfId;
import com.rtg.simulation.SimulationUtils;
import com.rtg.util.MathUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Loads and applies priors
 */
@TestClass(value = {"com.rtg.simulation.reads.DummyMachineTest", "com.rtg.simulation.reads.IlluminaSingleEndMachineTest"})
public abstract class AbstractMachine implements Machine {

  protected ReadWriter mReadWriter;

  protected final AbstractMachineErrorParams mParams;
  protected final double mInsertThreshold;
  protected final double mDeleteThreshold;
  protected final double mMnpThreshold;
  protected double[] mInsertLengthDistribution;
  protected double[] mDeleteLengthDistribution;
  protected double[] mMnpLengthDistribution;

  protected int[] mWorkspace;
  private int mCigarCurrentLength;
  private int mCigarCurrentOp;

  protected final PortableRandom mErrorTypeRandom;
  protected final PortableRandom mErrorLengthRandom;
  protected final PortableRandom mBaseRandom;
  protected final PortableRandom mQualityRandom;

  protected byte[] mQualityBytes;
  protected byte[] mReadBytes;
  protected int mReadBytesUsed;
  protected long mResidueCount;

  // Optional sequence data used during fragment read-through.
  // This could be split into separate forward and reverse sequences but this seems fine for now.
  protected byte[] mExtension = null;

  protected byte mMaxQ = 63;
  protected byte mMinQ = 0;
  protected byte mQRange = (byte) (mMaxQ - mMinQ);

  long[] mActionsHistogram;


  /**
   * Constructor
   * @param params priors to use
   */
  public AbstractMachine(AbstractMachineErrorParams params) {
    mParams = params;
    mInsertThreshold = mParams.errorInsEventRate();
    mDeleteThreshold = mInsertThreshold + mParams.errorDelEventRate();
    mMnpThreshold = mDeleteThreshold + mParams.errorMnpEventRate();

    mInsertLengthDistribution = SimulationUtils.cumulativeDistribution(MathUtils.deconvolve(mParams.errorInsDistribution(), mParams.errorInsEventRate()));
    mDeleteLengthDistribution = SimulationUtils.cumulativeDistribution(MathUtils.deconvolve(mParams.errorDelDistribution(), mParams.errorDelEventRate()));
    mMnpLengthDistribution = SimulationUtils.cumulativeDistribution(MathUtils.deconvolve(mParams.errorMnpDistribution(), mParams.errorMnpEventRate()));
    mErrorTypeRandom = new PortableRandom();
    mErrorLengthRandom = new PortableRandom();
    mBaseRandom = new PortableRandom();
    mQualityRandom = new PortableRandom();
    mReadBytes = new byte[0];
    mQualityBytes = new byte[0];

    final double p = mParams.errorSnpRate() + mParams.errorInsBaseRate() + mParams.errorDelBaseRate();
    final byte fixedQual = getPhred(p);
    setQualRange(fixedQual, fixedQual);

    mActionsHistogram = new long[ActionsHelper.getNumActions()];
  }

  @Override
  public PrereadType prereadType() {
    return PrereadType.UNKNOWN;
  }

  /**
   * Sets the minimum and maximum quality values that will be output for bases.
   *
   * @param minq the minimum quality value permitted.
   * @param maxq the maximum quality value permitted.
   */
  @Override
  public final void setQualRange(byte minq, byte maxq) {
    if (minq < 0 || minq > maxq) {
      throw new IllegalArgumentException("Invalid min q value: " + minq);
    }
    if (maxq > 63) {
      throw new IllegalArgumentException("Invalid max q value: " + maxq);
    }
    mMinQ = minq;
    mMaxQ = maxq;
    mQRange = (byte) (maxq - minq);
  }

  // Returns a phred score scaled/trimmed between 0 and mQRange
  private byte getPhred(double p) {
    assert p >= 0 && p <= 1;
    final int phred = p == 0 ? mQRange : (int) MathUtils.phred(p);
    return (byte) Math.min(mQRange, phred);
  }

  // Distribute quality values for an erroneously called base. Most should have poor quality.
  byte getMissCallQuality() {
    final double p = mQualityRandom.nextDouble();
    return (byte) (mMinQ + getPhred(p));
  }

  // Distribute quality values for a correctly called base. Most should have high quality. N's get poor quality
  byte getCorrectCallQuality(byte base) {
    final double p = mQualityRandom.nextDouble();
    return base == 0 ? 0 : (byte) (mMaxQ - getPhred(p));
  }

  protected void reseedErrorRandom(long seed) {
    mErrorTypeRandom.setSeed(seed * 3);
    mErrorLengthRandom.setSeed(seed * 5);
    mBaseRandom.setSeed(seed * 7);
    mQualityRandom.setSeed(seed * 9);
  }

  enum SimErrorType {
    /** SNP mutation */
    //SNP,
    /** multiple SNP mutation */
    MNP,
    /** insertion  */
    INSERT,
    /** deletion */
    DELETE,
    /** deletion */
    SUBSTITUTE_N,
    /** no error */
    NOERROR
  }

  SimErrorType getErrorType(double thres) {
    if (thres < mInsertThreshold) {
      return SimErrorType.INSERT;
    } else if (thres < mDeleteThreshold) {
      return SimErrorType.DELETE;
    } else if (thres < mMnpThreshold) {
      return SimErrorType.MNP;
    }
    return SimErrorType.NOERROR;
  }

  // Choose a non-N, non-ref DNA base at random. To allow any the four
  // bases, pass a ref of 0.
  byte chooseBase(byte ref) {
    byte ret = (byte) (1 + mBaseRandom.nextInt(DNA.values().length - (ref == 0 ? 1 : 2)));
    if (ref != 0 && ret >= ref) {
      ++ret;
    }
    return ret;
  }

  protected void addCigarState(int length, int op) {
    if (mCigarCurrentLength == 0) {
      mCigarCurrentOp = op;
      mCigarCurrentLength = length;
    } else if (op != mCigarCurrentOp) {
      ActionsHelper.prepend(mWorkspace, mCigarCurrentLength, mCigarCurrentOp, 0);
      mCigarCurrentOp = op;
      mCigarCurrentLength = length;
    } else {
      mCigarCurrentLength += length;
    }
  }

  protected void resetCigar() {
    mCigarCurrentOp = -1;
    mCigarCurrentLength = 0;
    mWorkspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
    mReadBytesUsed = 0;
    Arrays.fill(mReadBytes, (byte) 6);
  }

  private void addToCigar(StringBuilder sb, int count, int action, boolean prepend) {
    final char c = ActionsHelper.actionToChar(action);
    mActionsHistogram[action] += count;
    if (prepend) {
      sb.insert(0, c) ;
      sb.insert(0, count);
    } else {
      sb.append(count);
      sb.append(c);
    }
  }

  private String actionsToCigarSpecial(int[] workspace, boolean reverse) {
    final StringBuilder sb = new StringBuilder();
    final int start = ActionsHelper.ACTIONS_START_INDEX;
    final int length = workspace[ActionsHelper.ACTIONS_LENGTH_INDEX];
    final int intLength = length >> ActionsHelper.ACTIONS_PER_INT_SHIFT;
    final int lastLength = length & ActionsHelper.ACTIONS_COUNT_MASK;
    int actionCount = 0;
    int action = -1;
    for (int i = start; i <= start + intLength; ++i) {
      final int l;
      if (i == start + intLength) {
        l = lastLength;
      } else {
        l = ActionsHelper.ACTIONS_PER_INT;
      }
      final int localW = workspace[i];
      for (int index = 0; index < l; ++index) {
        final int lAction = (localW >>> ((ActionsHelper.ACTIONS_PER_INT - index - 1) * ActionsHelper.BITS_PER_ACTION)) & ActionsHelper.SINGLE_ACTION_MASK;
        if (lAction != action) {
          if (action != -1) {
            addToCigar(sb, actionCount, action, reverse);
          }
          action = lAction;
          actionCount = 0;
        }
        ++actionCount;
      }
    }
    if (action != -1) {
      addToCigar(sb, actionCount, action, reverse);
    }
    return sb.toString();
  }

  protected String getCigar(boolean reverse) {
    if (mCigarCurrentLength != 0) {
      ActionsHelper.prepend(mWorkspace, mCigarCurrentLength, mCigarCurrentOp, 0);
    }
    return actionsToCigarSpecial(mWorkspace, reverse);
  }

  // Simulate a simple whole read or arm read, drawing readLength bases from template.
  protected int process(int startPos, byte[] data, int templateLength, int direction, int readLength) {
    resetCigar();
    final int refPos = readBases(startPos, data, templateLength, direction, readLength);
    if (direction < 0) {
      DNA.complementInPlace(mReadBytes, 0, mReadBytesUsed);
    }
    return Math.min(startPos, refPos - direction);
  }

  //works like process, except we don't complement -1 direction and we do complement +1 direction
  //this is useful when we want to simulate forward reads anchored at the end of a read or reverse reads
  //anchored at the start. Note that the cigar will also need to be adjusted
  protected int processBackwards(int startPos, byte[] data, int templateLength, int direction, int readLength) {
    resetCigar();
    final int refPos = readBases(startPos, data, templateLength, direction, readLength, readLength - 1, -1);
    if (direction > 0) {
      DNA.complementInPlace(mReadBytes, 0, mReadBytesUsed);
    }
    return Math.min(startPos, refPos - direction);
  }

  protected int readBases(int startPos, byte[] data, int templateLength, int direction, int readLength) {
    return readBases(startPos, data, templateLength, direction, readLength, 0, 1);
  }

  /**
   * Draw bases from template into read buffer. Might be called
   * multiple times per read for complex read structures such as
   * complete genomics. Incrementally updates cigar and read buffer.
   *
   * @param startPos start position
   * @param data template sequence data
   * @param templateLength amount reference can travel from <code>startPos</code> in <code>direction</code>. i.e. amount of template read should cover
   * @param direction 1 for forwards, -1 for reverse
   * @param readLength max read length
   * @param readStartPos where the 0 position on the read is (e.g. 0 for read direction 1, <code>readLength - 1</code> for read direction -1)
   * @param readDirection direction read is being place in array (1 for forward, -1 for backwards)
   * @return position
   */
  protected int readBases(int startPos, byte[] data, int templateLength, int direction, int readLength, int readStartPos, int readDirection) {
    int templateUsed = 0;
    int readBases = 0;
    while (readBases < readLength && (templateUsed < templateLength || mExtension != null)) {
      final SimErrorType e = getErrorType(mErrorTypeRandom.nextDouble());
      final int templateAvail = mExtension == null ? templateLength - templateUsed : Integer.MAX_VALUE;
      switch (e) {
        case MNP:
          final int mnpLength = Math.min(templateAvail, Math.min(readLength - readBases, SimulationUtils.chooseFromCumulative(mMnpLengthDistribution, mErrorLengthRandom.nextDouble())));
          assert mnpLength > 0;
          for (int i = 0; i < mnpLength; ++i) {
            final int templatePos = startPos + templateUsed * direction;
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = chooseBase(getBase(data, templatePos, templateLength));
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getMissCallQuality();
            ++readBases;
            ++templateUsed;
          }
          addCigarState(mnpLength, ActionsHelper.MISMATCH);
          break;
        case INSERT:
          final int insLength = Math.min(readLength - readBases, SimulationUtils.chooseFromCumulative(mInsertLengthDistribution, mErrorLengthRandom.nextDouble()));
          assert insLength > 0;
          for (int k = 0; k < insLength; ++k) {
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = chooseBase((byte) 0);
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getMissCallQuality();
            ++readBases;
          }
          addCigarState(insLength, ActionsHelper.INSERTION_INTO_REFERENCE);
          break;
        case DELETE:
          final int delLength = Math.min(templateAvail, SimulationUtils.chooseFromCumulative(mDeleteLengthDistribution, mErrorLengthRandom.nextDouble()));
          templateUsed += delLength;
          addCigarState(delLength, ActionsHelper.DELETION_FROM_REFERENCE);
          // Deletion
          break;
        case NOERROR:
          final int refPos = startPos + templateUsed * direction;
          final byte base = getBase(data, refPos, templateLength);
          mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = base;
          mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getCorrectCallQuality(base);
          ++readBases;
          ++templateUsed;
          addCigarState(1, ActionsHelper.SAME);
          break;
        default:
          throw new IllegalStateException();
      }
    }
    mReadBytesUsed += readBases;
    return startPos + templateUsed * direction;
  }

  private byte getBase(byte[] template, int templatePos, int templateLength) {
    if (mExtension != null) {
      if (templatePos < 0) {
        return mExtension[(templatePos + 1) % mExtension.length + mExtension.length - 1];
      } else if (templatePos >= templateLength) {
        return mExtension[(templatePos - templateLength) % mExtension.length];
      }
    }
    return template[templatePos];
  }

  /**
   * Specifies the list of template sets used during generation.
   * @param templateIds an array containing an ID for each template set
   */
  @Override
  public void identifyTemplateSet(SdfId... templateIds) {
    mReadWriter.identifyTemplateSet(templateIds);
  }

  @Override
  public void identifyOriginalReference(SdfId referenceId) {
    mReadWriter.identifyOriginalReference(referenceId);
  }

  @Override
  public long residues() {
    return mResidueCount;
  }

  /**
   * Set <code>ReadWriter</code> to use for output of simulated reads
   * @param rw the <code>ReadWriter</code>
   */
  @Override
  public void setReadWriter(ReadWriter rw) {
    mReadWriter = rw;
  }

  /**
   * @return a textual representation summary of the actions histogram
   */
  @Override
  public String formatActionsHistogram() {
    final StringBuilder sb = new StringBuilder();

    final long totalError = mActionsHistogram[ActionsHelper.MISMATCH] + mActionsHistogram[ActionsHelper.DELETION_FROM_REFERENCE] + mActionsHistogram[ActionsHelper.INSERTION_INTO_REFERENCE];
    final long total = mActionsHistogram[ActionsHelper.SAME] + totalError;
    if (total == 0) {
      return "";
    } else if (total < 0) {
      sb.append("Overflow detected summing total.");
    }
    sb.append("Total action count:\t").append(total).append(StringUtils.LS);

    if (mActionsHistogram[ActionsHelper.SAME] > 0) {
      sb.append("Match count:\t").append(mActionsHistogram[ActionsHelper.SAME]).append('\t').append(Utils.realFormat(mActionsHistogram[ActionsHelper.SAME] / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    if (mActionsHistogram[ActionsHelper.MISMATCH] > 0) {
      sb.append("Mismatch count:\t").append(mActionsHistogram[ActionsHelper.MISMATCH]).append('\t').append(Utils.realFormat(mActionsHistogram[ActionsHelper.MISMATCH] / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    if (mActionsHistogram[ActionsHelper.DELETION_FROM_REFERENCE] > 0) {
      sb.append("Deletion count:\t").append(mActionsHistogram[ActionsHelper.DELETION_FROM_REFERENCE]).append('\t').append(Utils.realFormat(mActionsHistogram[ActionsHelper.DELETION_FROM_REFERENCE] / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    if (mActionsHistogram[ActionsHelper.INSERTION_INTO_REFERENCE] > 0) {
      sb.append("Insertion count:\t").append(mActionsHistogram[ActionsHelper.INSERTION_INTO_REFERENCE]).append('\t').append(Utils.realFormat(mActionsHistogram[ActionsHelper.INSERTION_INTO_REFERENCE] / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    if (totalError > 0) {
      sb.append("Total error count:\t").append(totalError).append('\t').append(Utils.realFormat(totalError / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    return sb.toString();
  }

  static String formatReadName(String id, char frame, String cigar, int fragmentStart, int newStart) {
    assert id.endsWith("/");
    final String idStr;
    if (fragmentStart < 0) {
      idStr = id;
    } else {
      final int startPos = fragmentStart + newStart;
      idStr = id + (startPos + 1) + "/";
    }
    return idStr + frame + "/" + cigar;
  }
}
