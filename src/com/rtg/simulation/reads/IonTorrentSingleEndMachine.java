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

import java.io.IOException;

import com.rtg.alignment.ActionsHelper;
import com.rtg.simulation.SimulationUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;
import com.rtg.variant.MachineErrorParamsBuilder;

/**
 * Ion Torrent single end machine
 */
public class IonTorrentSingleEndMachine extends SingleEndRandomLengthMachine {

  private final Double mHomopolyDeleteProbability = 0.2; //Double.parseDouble(System.getProperty("it_mach_homopoly_del_prob", "0.20"));
  private final Double mHomopolyInsertProbability = 0.03; //Double.parseDouble(System.getProperty("it_mach_homopoly_ins_prob", "0.03"));

  protected long[] mHomopolyErrorCounts;

  /**
   * Construct with given priors and seed
   * @param params priors
   * @param randomSeed seed for random number generation
   */
  public IonTorrentSingleEndMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params, randomSeed);
    mHomopolyErrorCounts = new long[2];
  }

  /**
   * Construct with given random seed and default 454 priors
   * @param randomSeed seed for random number generation
   * @throws InvalidParamsException If priors fail to load
   * @throws IOException whenever
   */
  public IonTorrentSingleEndMachine(long randomSeed) throws InvalidParamsException, IOException {
    this(new MachineErrorParamsBuilder().errors("iontorrent").create(), randomSeed);
  }

  static int homopolymerLength(int pos, byte[] data, int templateLength) {
    int homopolymerLength = 1;
    final byte thisByte = data[pos];
    for (int i = pos; i > 1 && data[i - 1] == thisByte; --i) {
      ++homopolymerLength;
    }
    for (int i = pos; i < templateLength && data[i + 1] == thisByte; ++i) {
      ++homopolymerLength;
    }
    return homopolymerLength;
  }

  @Override
  protected int readBases(int startPos, byte[] data, int templateLength, int direction, int readLength, int readStartPos, int readDirection) {
    int refUsed = 0;
    int readBases = 0;
    while (readBases < readLength && refUsed < templateLength) {
      final int homoPolymerLength = homopolymerLength(startPos + refUsed * direction, data, templateLength);
      if (homoPolymerLength > 2) {
        final Double p = mErrorTypeRandom.nextDouble();
        if (p < mHomopolyDeleteProbability) { //20% chance of shortening
          for (int i = 0; i < homoPolymerLength - 1 && readBases < readLength && refUsed < templateLength; ++i) {
            final byte base = data[startPos + refUsed * direction];
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = base;
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getCorrectCallQuality(base);
            ++readBases;
            ++refUsed;
            addCigarState(1, ActionsHelper.SAME);
          }

          if (readBases < readLength && refUsed < templateLength) {
            refUsed += 1;
            addCigarState(1, ActionsHelper.DELETION_FROM_REFERENCE);
            mHomopolyErrorCounts[0]++;
            // Deletion
          }
        } else if (p < mHomopolyDeleteProbability + mHomopolyInsertProbability) { //3% chance of elongating homopoly by 1
          for (int i = 0; i < homoPolymerLength && readBases < readLength && refUsed < templateLength; ++i) {
            final byte base = data[startPos + refUsed * direction];
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = base;
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getCorrectCallQuality(base);
            ++readBases;
            ++refUsed;
            addCigarState(1, ActionsHelper.SAME);
          }
          if (readBases < readLength) {
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = data[startPos + refUsed * direction];
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getMissCallQuality();
            ++readBases;
            addCigarState(1, ActionsHelper.INSERTION_INTO_REFERENCE);
            mHomopolyErrorCounts[1]++;
          }
        } else {
          for (int i = 0; i < homoPolymerLength && readBases < readLength && refUsed < templateLength; ++i) {
            final byte base = data[startPos + refUsed * direction];
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = base;
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getCorrectCallQuality(base);
            ++readBases;
            ++refUsed;
            addCigarState(1, ActionsHelper.SAME);
          }
        }
      } else {
        final SimErrorType e = getErrorType(mErrorTypeRandom.nextDouble());
        switch (e) {
          case MNP:
            final int mnpLength = Math.min(templateLength - refUsed, Math.min(readLength - readBases, SimulationUtils.chooseFromCumulative(mMnpLengthDistribution, mErrorLengthRandom.nextDouble())));
            assert mnpLength > 0;
            for (int i = 0; i < mnpLength; ++i) {
              mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = chooseBase(data[startPos + refUsed * direction]);
              mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getMissCallQuality();
              ++readBases;
              ++refUsed;
            }
            addCigarState(mnpLength, ActionsHelper.MISMATCH);
            break;
          case NOERROR:
            final byte base = data[startPos + refUsed * direction];
            mReadBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = base;
            mQualityBytes[readStartPos + (mReadBytesUsed + readBases) * readDirection] = getCorrectCallQuality(base);
            ++readBases;
            ++refUsed;
            addCigarState(1, ActionsHelper.SAME);
            break;
          case DELETE:
            final int delLength = Math.min(templateLength - refUsed, SimulationUtils.chooseFromCumulative(mDeleteLengthDistribution, mErrorLengthRandom.nextDouble()));
            refUsed += delLength;
            addCigarState(delLength, ActionsHelper.DELETION_FROM_REFERENCE);
            // Deletion
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
          default:
            throw new IllegalStateException();
        }
      }
    }
    mReadBytesUsed += readBases;
    return startPos + refUsed * direction;
  }

  @Override
  public String formatActionsHistogram() {
    final StringBuilder sb = new StringBuilder(super.formatActionsHistogram());
    final long total = mActionsHistogram[ActionsHelper.SAME] + mActionsHistogram[ActionsHelper.MISMATCH] + mActionsHistogram[ActionsHelper.INSERTION_INTO_REFERENCE] + mActionsHistogram[ActionsHelper.DELETION_FROM_REFERENCE];
    if (mHomopolyErrorCounts[0] > 0) {
      sb.append("Of deletions, due to homopolymer:\t").append(mHomopolyErrorCounts[0]).append('\t').append(Utils.realFormat(mHomopolyErrorCounts[0] / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    if (mHomopolyErrorCounts[1] > 0) {
      sb.append("Of insertions, due to homopolymer:\t").append(mHomopolyErrorCounts[1]).append('\t').append(Utils.realFormat(mHomopolyErrorCounts[1] / (double) total * 100, 2)).append('%').append(StringUtils.LS);
    }
    return sb.toString();
  }

  @Override
  public MachineType machineType() {
    return MachineType.IONTORRENT;
  }
}
