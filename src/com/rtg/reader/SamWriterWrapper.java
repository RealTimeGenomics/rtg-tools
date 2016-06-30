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
import java.io.OutputStream;
import java.util.Arrays;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.CommonFlags;
import com.rtg.mode.DnaUtils;
import com.rtg.sam.ReadGroupUtils;
import com.rtg.sam.SamBamConstants;
import com.rtg.sam.SamCommandHelper;
import com.rtg.sam.SamUtils;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

/**
 * Wrapper for writing single or paired-end sequences as SAM/BAM
 */
@TestClass("com.rtg.reader.Sdf2SamTest")
public class SamWriterWrapper implements WriterWrapper {

  private static final String[] EXTS = {SamUtils.BAM_SUFFIX, SamUtils.SAM_SUFFIX};

  private final boolean mHasNames;
  private final SdfReaderWrapper mReader;
  private final boolean mIsPaired;
  private final int mFlags;
  private final SAMReadGroupRecord mReadGroupRecord;
  private final SAMFileWriter mWriter;

  /**
   * Convenience wrapper for writing.
   * @param baseOutput base output file name.
   * @param reader the reader that this writer is writing from.
   * @param gzip if true, compress the output (SAM only).
   * @throws IOException if there is a problem constructing the writer.
   */
  public SamWriterWrapper(File baseOutput, SdfReaderWrapper reader, boolean gzip) throws IOException {
    this(baseOutput, reader, gzip, EXTS);
  }

  private SamWriterWrapper(File baseOutput, SdfReaderWrapper reader, boolean gzip, String[] extensions) throws IOException {
    assert reader != null;
    assert extensions.length > 0;
    mReader = reader;
    mIsPaired = reader.isPaired();
    mHasNames = reader.hasNames();
    final long maxLength = reader.maxLength();
    if (maxLength > Integer.MAX_VALUE) {
      throw new NoTalkbackSlimException(ErrorType.SEQUENCE_LENGTH_ERROR);
    }

    final BaseFile baseFile = FileUtils.getBaseFile(baseOutput, gzip, extensions);

    final SAMFileHeader header = new SAMFileHeader();
    header.setSortOrder(SAMFileHeader.SortOrder.unsorted);

    if (mIsPaired) {
      mFlags = SamBamConstants.SAM_READ_IS_PAIRED | SamBamConstants.SAM_READ_IS_UNMAPPED | SamBamConstants.SAM_MATE_IS_UNMAPPED;
    } else {
      mFlags = SamBamConstants.SAM_READ_IS_UNMAPPED;
    }
    final SequencesReader r = mIsPaired ? mReader.left() : mReader.single();
    if (r instanceof AnnotatedSequencesReader) {
      final AnnotatedSequencesReader r2 = (AnnotatedSequencesReader) r;
      final String readGroupString = r2.samReadGroup();
      if (readGroupString != null) {
        mReadGroupRecord = SamCommandHelper.validateAndCreateSamRG(readGroupString.replaceAll("\t", "\\\\t"), SamCommandHelper.ReadGroupStrictness.REQUIRED);
        header.addReadGroup(mReadGroupRecord);
      } else {
        mReadGroupRecord = null;
      }
      final String comment = r2.comment();
      if (comment != null) {
        header.addComment(comment);
      }
    } else {
      mReadGroupRecord = null;
    }

    SamUtils.addProgramRecord(header);

    final boolean bam = !CommonFlags.isStdio(baseOutput) && baseFile.getExtension().endsWith(SamUtils.BAM_SUFFIX);

    final OutputStream os;
    if (CommonFlags.isStdio(baseOutput)) {
      os = FileUtils.getStdoutAsOutputStream();
    } else {
      os = FileUtils.createOutputStream(baseFile.suffixedFile("", gzip && !bam), gzip && !bam);
    }

    final SAMFileWriterFactory fact = new SAMFileWriterFactory();
    mWriter = bam
      ? fact.makeBAMWriter(header, true, os, false)
      : fact.makeSAMWriter(header, true, os);
  }

  @Override
  public void writeSequence(long seqId, byte[] dataBuffer, byte[] qualityBuffer) throws IllegalStateException, IOException {
    if (mIsPaired) {
      writeSequence(mReader.left(), seqId, dataBuffer, qualityBuffer, mFlags | SamBamConstants.SAM_READ_IS_FIRST_IN_PAIR);
      writeSequence(mReader.right(), seqId, dataBuffer, qualityBuffer, mFlags | SamBamConstants.SAM_READ_IS_SECOND_IN_PAIR);
    } else {
      writeSequence(mReader.single(), seqId, dataBuffer, qualityBuffer, mFlags);
    }
  }

  void writeSequence(SequencesReader reader, long seqId, byte[] dataBuffer, byte[] qualityBuffer, int flags) throws IllegalArgumentException, IllegalStateException, IOException {
    final SAMRecord rec = new SAMRecord(mWriter.getFileHeader());

    final int length = reader.read(seqId, dataBuffer);
    rec.setReadName(mHasNames ? reader.name(seqId) : ("" + seqId));
    rec.setReferenceName("*");
    rec.setFlags(flags);
    rec.setReadBases(DnaUtils.bytesToSequenceIncCG(dataBuffer, 0, length).getBytes());
    if (mReader.hasQualityData()) {
      reader.readQuality(seqId, qualityBuffer);
      rec.setBaseQualities(Arrays.copyOf(qualityBuffer, length));
    }
    rec.setInferredInsertSize(0);
    rec.setMateAlignmentStart(0);
    rec.setMateReferenceName("*");
    if (mReadGroupRecord != null) {
      rec.setAttribute(ReadGroupUtils.RG_ATTRIBUTE, mReadGroupRecord.getReadGroupId());
    }

    mWriter.addAlignment(rec);
  }

  @Override
  public void close() throws IOException {
    if (mWriter != null) {
      mWriter.close();
    }
  }
}

