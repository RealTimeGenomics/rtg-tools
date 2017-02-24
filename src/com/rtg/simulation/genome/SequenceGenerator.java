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

package com.rtg.simulation.genome;

import java.io.File;
import java.io.IOException;

import com.rtg.mode.DNA;
import com.rtg.mode.Residue;
import com.rtg.mode.SequenceType;
import com.rtg.reader.PrereadType;
import com.rtg.reader.SequenceDataSource;
import com.rtg.reader.SequencesWriter;
import com.rtg.util.Constants;
import com.rtg.util.PortableRandom;

/**
 * Generates a randomized DNA sequence
 *
 *
 */
public class SequenceGenerator {

  private final SequencesWriter mWriter;

  private static class RandomDataSource implements SequenceDataSource {

    private int mSequenceNumber = 0;
    private final int[] mLengths;
    private final PortableRandom mSource;
    private final RandomDistribution mDistribution;
    private long mMaxLength = Long.MIN_VALUE;
    private long mMinLength = Long.MAX_VALUE;
    private final Residue[] mResidues = DNA.values();
    private final String mPrefix;

    private byte[] mSequenceData;

    RandomDataSource(final int[] lengths, final PortableRandom source, final RandomDistribution distribution, String namePrefix) {
      mLengths = lengths;
      mSource = source;
      mDistribution = distribution;
      mPrefix = namePrefix;
    }

    @Override
    public void close() {
    }

    @Override
    public String name() {
      return mPrefix + mSequenceNumber;
    }

    @Override
    public SequenceType type() {
      return SequenceType.DNA;
    }

    @Override
    public long getWarningCount() {
      return 0;
    }

    @Override
    public boolean hasQualityData() {
      return false;
    }

    @Override
    public boolean nextSequence() {
      if (mSequenceNumber >= mLengths.length) {
        return false;
      }
      mSequenceData = new byte[mLengths[mSequenceNumber]];
      for (int i = 0; i < mSequenceData.length; ++i) {
        mSequenceData[i] = (byte) getRandomResidue().ordinal();
      }

      ++mSequenceNumber;
      mMinLength = Math.min(mMinLength, currentLength());
      mMaxLength = Math.max(mMaxLength, currentLength());
      return true;
    }

    @Override
    public byte[] sequenceData() {
      return mSequenceData;
    }


    private Residue getRandomResidue() {
      if (mDistribution == null) {
        return mResidues[1 + mSource.nextInt(mResidues.length - 1)];
      }
      return mResidues[1 + mDistribution.nextValue()];
    }

    @Override
    public byte[] qualityData() {
      return null;
    }

    @Override
    public void setDusting(final boolean val) { }

    @Override
    public int currentLength() {
      return mSequenceData.length;
    }

    @Override
    public long getDusted() {
      return 0;
    }

    @Override
    public long getMaxLength() {
      return mMaxLength;
    }

    @Override
    public long getMinLength() {
      return mMinLength;
    }
  }

  public long getSizeLimit() {
    return mWriter.getSizeLimit();
  }

  /**
   * Create a sequence generator with specified seed and nucleotide frequency
   * corresponding to the supplied distribution
   *  @param generator random number generator
   * @param distribution relative frequency of a/t/g/c
   * @param lengths array of lengths of the sequences to write
   * @param outDirectory output directory
   * @param namePrefix prefix to use for generated sequence names
   */
  public SequenceGenerator(final PortableRandom generator, final RandomDistribution distribution, final int[] lengths, final File outDirectory, String namePrefix) {
    final RandomDataSource dataSource = new RandomDataSource(lengths, generator, distribution, namePrefix);
    mWriter = new SequencesWriter(dataSource, outDirectory, Constants.MAX_FILE_SIZE, PrereadType.UNKNOWN, true);
  }

  /**
   * @param comment the comment for the generated SDF file(s)
   */
  public void setComment(final String comment) {
    mWriter.setComment(comment);
  }

  /**
   * Write a sequences
   *
    * @throws IOException when IO errors occur
   */
  public void createSequences() throws IOException {
    mWriter.processSequences();
  }

}
