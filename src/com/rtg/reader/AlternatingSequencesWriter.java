/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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
package com.rtg.reader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.intervals.LongRange;


/**
 * Writes sequences into two SDFs, alternating between left and right.
 * Used in CG data formatting.
 */
public class AlternatingSequencesWriter extends SequencesWriter {

  /**
   * Creates an alternating writer for processing sequences from provided data source.
   * @param source Source of sequences
   * @param outputDir Destination of output files
   * @param sizeLimit Maximum size for output files
   * @param type preread type
   * @param compressed whether <code>SDF</code> should be compressed
   * @param trimQualityThreshold quality threshold to trim reads after, or null for no trimming
   */
  public AlternatingSequencesWriter(SequenceDataSource source, File outputDir, long sizeLimit, PrereadType type, boolean compressed, Integer trimQualityThreshold) {
    super(source, outputDir, sizeLimit, null, type, compressed, trimQualityThreshold);
  }
  /**
   * Creates an alternating writer for processing sequences from provided data source.
   * @param source Source of sequences
   * @param outputDir Destination of output files
   * @param sizeLimit Maximum size for output files
   * @param namesToExclude Names to be excluded from written result (may be null)
   * @param type preread type
   * @param compressed whether <code>SDF</code> should be compressed
   * @param trimQualityThreshold quality threshold to trim reads after, or null for no trimming
   */
  public AlternatingSequencesWriter(SequenceDataSource source, File outputDir, long sizeLimit, final Collection<String> namesToExclude, PrereadType type, boolean compressed, Integer trimQualityThreshold) {
    super(source, outputDir, sizeLimit, namesToExclude, type, compressed, trimQualityThreshold);
  }

  /**
   * Constructor for use with in memory sequence sinks
   * @param source Source of sequences
   * @param namesToExclude Names to be excluded from written result (may be null)
   * @param type preread type
   * @param compressed whether <code>SDF</code> should be compressed
   */
  public AlternatingSequencesWriter(final SequenceDataSource source, final Collection<String> namesToExclude, final PrereadType type, boolean compressed) {
    super(source, namesToExclude, type, compressed);
  }

  private void processSequences(AbstractSdfWriter inSdfWriterLeft, AbstractSdfWriter inSdfWriterRight, LongRange region) throws IOException {
    try (SequenceDataSource dataSource = mDataSource;
    AbstractSdfWriter sdfWriterLeft = inSdfWriterLeft;
    AbstractSdfWriter sdfWriterRight = inSdfWriterRight) {
      final boolean checkLimit = region != null && region != LongRange.NONE && region.getEnd() != LongRange.MISSING;
      final long seqLimit;
      if (checkLimit) {
        seqLimit = region.getEnd();
      } else {
        seqLimit = -1;
      }
      sdfWriterLeft.setPrereadArm(PrereadArm.LEFT);
      sdfWriterLeft.setSdfId(mSdfId);
      sdfWriterLeft.setComment(mComment);
      sdfWriterLeft.setReadGroup(mReadGroup);
      sdfWriterLeft.setCommandLine(CommandLine.getCommandLine());
      sdfWriterRight.setPrereadArm(PrereadArm.RIGHT);
      sdfWriterRight.setSdfId(mSdfId);
      sdfWriterRight.setComment(mComment);
      sdfWriterRight.setReadGroup(mReadGroup);
      sdfWriterRight.setCommandLine(CommandLine.getCommandLine());

      String name = "";
      while (dataSource.nextSequence()) {
        //this is to short circuit processing
        if (checkLimit && sdfWriterLeft.getNumberOfSequences() >= seqLimit) {
          break;
        }
        if (mCheckDuplicateNames) {
          if (name.equals(dataSource.name())) {
            throw new NoTalkbackSlimException("More than one read-pair with the name " + name + " in input.");
          }
          name = dataSource.name();
        }
        processSingleSequence(sdfWriterLeft);
        if (!dataSource.nextSequence()) {
          throw new NoTalkbackSlimException("Input source had uneven number of records.");
        }
        processSingleSequence(sdfWriterRight);
      }

      if (dataSource.getWarningCount() > FastaSequenceDataSource.NUMBER_OF_TIDE_WARNINGS) {
        Diagnostic.warning(""); //Ugly way to separate the warning.
        Diagnostic.warning(WarningType.NUMBER_OF_BAD_TIDE, Long.toString(dataSource.getWarningCount()));
      }
    } finally {
      calculateStats(inSdfWriterLeft, inSdfWriterRight);
    }
  }

  /**
   * Processes alternating paired input sequences into two in memory readers
   * @param sourceFile file that was the source of this data
   * @param includeQuality true if including quality data in output, false otherwise
   * @param names storage for read names
   * @param suffixes storage for read name suffixes
   * @param region restriction returned reader to given range of reads
   * @return the reader from reading the sequence data
   * @throws IOException If an I/O error occurs
   */
  public CompressedMemorySequencesReader[] processSequencesInMemoryPaired(File sourceFile, boolean includeQuality, SimplePrereadNames names, SimplePrereadNames suffixes, LongRange region) throws IOException {
    final CompressedMemorySequencesWriter sdfWriterLeft = new CompressedMemorySequencesWriter(sourceFile, mPrereadType, mDataSource.hasQualityData() && includeQuality, names, suffixes, true, mDataSource.type(), region);
    final RightSimplePrereadNames rNames = names == null ? null : new RightSimplePrereadNames(names);
    final RightSimplePrereadNames rSuffixes = names == null ? null : new RightSimplePrereadNames(suffixes);
    final CompressedMemorySequencesWriter sdfWriterRight = new CompressedMemorySequencesWriter(sourceFile, mPrereadType, mDataSource.hasQualityData() && includeQuality, rNames, rSuffixes, true, mDataSource.type(), region);
    processSequences(sdfWriterLeft, sdfWriterRight, region);
    return new CompressedMemorySequencesReader[] {sdfWriterLeft.getReader(), sdfWriterRight.getReader()};
  }


  @Override
  public void processSequences(boolean includeQuality, boolean includeNames) throws IOException {
    final File outputDirLeft = new File(mOutputDir, "left");
    final File outputDirRight = new File(mOutputDir, "right");
    final SdfWriter sdfWriterLeft = new SdfWriter(outputDirLeft, mSizeLimit, mPrereadType, mDataSource.hasQualityData() && includeQuality, includeNames, mCompressed, mDataSource.type());
    final SdfWriter sdfWriterRight = new SdfWriter(outputDirRight, mSizeLimit, mPrereadType, mDataSource.hasQualityData() && includeQuality, includeNames, mCompressed, mDataSource.type());
    processSequences(sdfWriterLeft, sdfWriterRight, null);
  }

  private void calculateStats(AbstractSdfWriter left, AbstractSdfWriter right) {
    mTotalLength += left.getTotalLength() + right.getTotalLength();
    mNumberOfSequences += left.getNumberOfSequences() + right.getNumberOfSequences();
    if (left.getMaxLength() > mMaxLength || right.getMaxLength() > mMaxLength) {
      mMaxLength = Math.max(left.getMaxLength(), right.getMaxLength());
    }
    if (left.getMinLength() < mMinLength || right.getMinLength() < mMinLength) {
      mMinLength = Math.min(left.getMinLength(), right.getMinLength());
    }
  }

}
