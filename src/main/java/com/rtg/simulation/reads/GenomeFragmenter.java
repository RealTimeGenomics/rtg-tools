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
import java.util.Locale;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.mode.DNA;
import com.rtg.reader.SdfId;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SourceTemplateReadWriter;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.Sex;
import com.rtg.simulation.DistributionSampler;
import com.rtg.simulation.IntSampler;
import com.rtg.simulation.SimulationUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Produces fragments for read simulation from provided genomes.
 */
public class GenomeFragmenter {

  private static final int NUMBER_TRIES = 1000;
  private static final int MAX_WARNINGS = 5;
  private static final boolean OS_SEQ = GlobalFlags.isSet(ToolsGlobalFlags.OS_SEQ_FRAGMENTS);
  static final int OS_SEQ_MIN = GlobalFlags.getIntegerValue(ToolsGlobalFlags.OS_SEQ_FRAGMENTS);

  private Machine mMachine;
  private int mCounter;
  private final SequencesReader[] mReaders;
  private final ReferenceGenome[] mRefGenome;
  private final int[][] mSequenceLengths;
  private boolean mAllowNs;
  private IntSampler mLengthChooser = null;
  private final PortableRandom mPositionRandom;
  private final PortableRandom mLengthRandom;
  private byte[] mByteBuffer;
  private byte[] mWorkspace;
  private int mWarnCount = 0;
  private final DistributionSampler[] mSelectionDistributions;
  private boolean mHasIdentified = false;
  private final int[][] mSequenceCounts;

  GenomeFragmenter(long randomSeed, DistributionSampler[] selectionProb, SequencesReader[] sdfs) throws IOException {
    mLengthRandom = new PortableRandom(randomSeed);
    mPositionRandom = new PortableRandom(mLengthRandom.nextLong() * 11);
    mReaders = sdfs;
    mSelectionDistributions = selectionProb;
    for (DistributionSampler s : selectionProb) {
      s.setRandom(mPositionRandom);
    }
    mSequenceLengths = new int[sdfs.length][];
    mSequenceCounts = new int[sdfs.length][];
    for (int i = 0; i < mReaders.length; ++i) {
      mSequenceLengths[i] = mReaders[i].sequenceLengths(0, (int) mReaders[i].numberSequences());
      mSequenceCounts[i] = new int[mSequenceLengths[i].length];
    }
    mByteBuffer = new byte[1000];
    mWorkspace = new byte[1000];
    mMachine = null;
    mRefGenome = new ReferenceGenome[sdfs.length];
    for (int k = 0; k < mRefGenome.length; ++k) {
      // Used to determine circular status when constructing fragments -- hopefully sex and ploidy independent
      mRefGenome[k] = new ReferenceGenome(sdfs[k], Sex.EITHER, ReferencePloidy.AUTO);
    }
  }

  GenomeFragmenter(long randomSeed, SequencesReader... sdfs) throws IOException {
    this(randomSeed, defaultDistributions(sdfs), sdfs);
  }

  static DistributionSampler[] defaultDistributions(SequencesReader[] sdfs) throws IOException {
    final DistributionSampler[] result = new DistributionSampler[sdfs.length];
    for (int i = 0; i < result.length; ++i) {
      result[i] = SimulationUtils.defaultDistribution(sdfs[i]);
    }
    return result;
  }

  /**
   * Machine to pass fragments to
   * @param m the machine
   */
  public void setMachine(Machine m) {
    mMachine = m;
  }

  /**
   * Allow N's to be present in fragments
   * @param v true or false
   */
  public void allowNs(boolean v) {
    mAllowNs = v;
  }

  /**
   * Set the object responsible for selecting the length of each subsequent fragment
   * @param chooser the length chooser
   */
  public void setLengthChooser(IntSampler chooser) {
    mLengthChooser = chooser;
    mLengthChooser.setRandom(mLengthRandom);
  }

  private boolean checkNs(int fragLength) {
    final int maxNsAllowed = mAllowNs ? fragLength / 10 : 0;
    int numNs = 0;
    for (int k = 0; k < fragLength; ++k) {
      if (mByteBuffer[k] == (byte) DNA.N.ordinal()) {
        ++numNs;
      }
    }
    return numNs <= maxNsAllowed;
  }

  /**
   * Chooses a fragment and passes to machine set by {@link GenomeFragmenter#setMachine(Machine)}
   * @throws IOException whenever
   * @throws IllegalStateException if sizes are impossible.
   * @throws NullPointerException if the machine is null.
   */
  public void makeFragment() throws IOException {
    int fragLength = mLengthChooser.next();
    if (fragLength == Integer.MIN_VALUE) {
      makeWarning("Could not generate fragment length within specified min and max length. Fragment skipped.");
    } else {
      if (OS_SEQ && OS_SEQ_MIN > 0) { // Virtual truncation of off-probe end
        if (fragLength <= OS_SEQ_MIN) {
          Diagnostic.developerLog("Length chooser returned fragment length smaller than OS_SEQ_MIN: " + fragLength + " <= " + OS_SEQ_MIN);
          return;
        }
        fragLength = mLengthRandom.nextInt(fragLength - OS_SEQ_MIN) + OS_SEQ_MIN;
      }
      if (mByteBuffer.length < fragLength) {
        mByteBuffer = new byte[fragLength];
        mWorkspace = new byte[fragLength];
      }
      for (int x = 0; x < NUMBER_TRIES; ++x) {
        // Randomly pick a reader
        final int readerId = mPositionRandom.nextInt(mSequenceLengths.length);

        // Choose sequence
        final int seqId = mSelectionDistributions[readerId].next();
        final String seqName = mReaders[readerId].name(seqId);
        // Choose position on sequence
        final int sequenceLength = mSequenceLengths[readerId][seqId];
        final int fragStart = OS_SEQ ? 0 : (int) (mPositionRandom.nextDouble() * sequenceLength);
        if (fragLength <= sequenceLength - fragStart) {
          mReaders[readerId].read(seqId, mByteBuffer, fragStart, fragLength);
          if (checkNs(fragLength) && emitFragment(fragLength, seqId, readerId, seqName, fragStart)) {
            return;
          }
        } else if (!mRefGenome[readerId].sequence(seqName).isLinear()) {
          int remaining = fragLength;
          int soFar = 0;
          int cPos = fragStart;
          // sequence is circular and can even be shorter than the fragment size, hence ...
          while (remaining > 0) {
            final int readLen = Math.min(remaining, sequenceLength - cPos);
            assert readLen > 0;
            mReaders[readerId].read(seqId, mWorkspace, cPos, readLen);
            System.arraycopy(mWorkspace, 0, mByteBuffer, soFar, readLen);
            soFar += readLen;
            remaining -= readLen;
            cPos += readLen;
            cPos %= sequenceLength;
          }
          assert soFar == fragLength;
          if (checkNs(fragLength) && emitFragment(fragLength, seqId, readerId, seqName, fragStart)) {
            return;
          }
        }
      }
      makeWarning("Could not generate fragment with length " + fragLength + ". Fragment skipped.");
    }
  }

  boolean emitFragment(int fragLength, int seqId, int readerId, String seqName, int fragStart) throws IOException {
    identifyTemplateIds();
    mMachine.processFragment("frag" + mCounter++ + "/" + readerId + "/" + seqId + "/" + seqName + "/", fragStart, mByteBuffer, fragLength);
    mSequenceCounts[readerId][seqId]++;
    return true;
  }

  private void identifyTemplateIds() throws IOException {
    if (!mHasIdentified) {
      final SdfId[] ids = new SdfId[mReaders.length];
      int idx = 0;
      for (SequencesReader reader : mReaders) {
        ids[idx++] = reader.getSdfId();
      }
      mMachine.identifyTemplateSet(ids);
      if (mReaders.length == 1) {
        final SdfId originalRef = SourceTemplateReadWriter.readMutationMap(mReaders[0].path());
        mMachine.identifyOriginalReference(originalRef);
      }
      mHasIdentified = true;
    }
  }

  private void makeWarning(String message) {
    if (mWarnCount < MAX_WARNINGS) {
      Diagnostic.warning(message);
      ++mWarnCount;
      if (mWarnCount == MAX_WARNINGS) {
        Diagnostic.warning("Subsequent warnings of this type will not be shown.");
      }
    }
  }

  /**
   * @return information about actual created species relevant information
   * @throws IOException if an IO error occurs
   */
  public String fractionStatistics() throws IOException {
    final StringBuilder sb = new StringBuilder();
    //final double[][] fractions = new double[mSequenceCounts.length][];
    int totalFrags = 0;
    for (int i = 0; i < mSequenceCounts.length; ++i) {
      //fractions[i] = new double[mSequenceCounts[i].length];
      for (int j = 0; j < mSequenceCounts[i].length; ++j) {
        //fractions[i][j] = (double) mSequenceCounts[i][j] / mSequenceLengths[i][j];
        totalFrags += mSequenceCounts[i][j];
      }
    }
    sb.append("#name").append("\t").append("fragment_count").append("\t").append("fraction_dna").append("\t").append("seq_length").append(StringUtils.LS);
    for (int i = 0; i < mSequenceCounts.length; ++i) {
      for (int j = 0; j < mSequenceCounts[i].length; ++j) {
        final double fractionDna = (double) mSequenceCounts[i][j] / totalFrags;
        sb.append(mReaders[i].name(j)).append("\t").append(mSequenceCounts[i][j]).append("\t").append(String.format(Locale.ROOT, "%1.8g", fractionDna))
                .append("\t").append(String.format("%d", mSequenceLengths[i][j])).append(StringUtils.LS);
      }
    }
    return sb.toString();
  }
}
