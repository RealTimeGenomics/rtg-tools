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

import java.io.File;
import java.io.IOException;

import com.rtg.mode.SequenceType;
import com.rtg.reader.PrereadArm;
import com.rtg.reader.PrereadType;
import com.rtg.reader.SdfId;
import com.rtg.reader.SdfWriter;
import com.rtg.reader.SourceTemplateReadWriter;
import com.rtg.util.Constants;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.SAMReadGroupRecord;

/**
 * Write simulated reads to SDF.
 */
public class SdfReadWriter implements ReadWriter {

  private final boolean mIsPaired;
  private final SdfWriter mLeft;
  private final SdfWriter mRight;
  private final SdfWriter mSingle;
  private int mTotal = 0;
  private SdfId[] mTemplateSetIds = null;
  private SdfId mOriginalReference = null;
  private boolean mExpectLeft = true;

  /**
   * Constructor
   * @param outputDir directory to create SDF in.
   * @param isPaired whether SDF should be paired
   * @param type SDF type
   * @param names true if we should write read names
   * @param quality true if we should write quality
   */
  public SdfReadWriter(File outputDir, boolean isPaired, PrereadType type, boolean names, boolean quality) {
    mIsPaired = isPaired;
    if (mIsPaired) {
      FileUtils.ensureOutputDirectory(outputDir);
      mSingle = null;
      mLeft = new SdfWriter(new File(outputDir, "left"), Constants.MAX_FILE_SIZE, type, quality, names, true, SequenceType.DNA);
      mLeft.setPrereadArm(PrereadArm.LEFT);
      mLeft.setCommandLine(CommandLine.getCommandLine());
      mRight = new SdfWriter(new File(outputDir, "right"), Constants.MAX_FILE_SIZE, type, quality, names, true, SequenceType.DNA);
      mRight.setPrereadArm(PrereadArm.RIGHT);
      mRight.setCommandLine(CommandLine.getCommandLine());
      //mRight.setOldSdfId(mLeft.getOldSdfId());
      mRight.setSdfId(mLeft.getSdfId());

    } else {
      mLeft = null;
      mRight = null;
      mSingle = new SdfWriter(outputDir, Constants.MAX_FILE_SIZE, type, quality, names, true, SequenceType.DNA);
      mSingle.setPrereadArm(PrereadArm.UNKNOWN);
      mSingle.setCommandLine(CommandLine.getCommandLine());
    }
  }

  @Override
  public void identifyTemplateSet(SdfId... templateIds) {
    mTemplateSetIds = templateIds;
  }

  @Override
  public void identifyOriginalReference(SdfId referenceId) {
    mOriginalReference = referenceId;
  }

  /**
   * Set the SDF comment
   * @param comment the comment text
   */
  public void setComment(String comment) {
    if (mIsPaired) {
      mLeft.setComment(comment);
      mRight.setComment(comment);
    } else {
      mSingle.setComment(comment);
    }
  }

  /**
   * Set the SDF read group
   * @param readGroup the read group information
   */
  public void setReadGroup(SAMReadGroupRecord readGroup) {
    if (readGroup != null) {
      if (mIsPaired) {
        mLeft.setReadGroup(readGroup.getSAMString());
        mRight.setReadGroup(readGroup.getSAMString());
      } else {
        mSingle.setReadGroup(readGroup.getSAMString());
      }
    }
  }

  @Override
  public void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (mIsPaired) {
      throw new IllegalStateException();
    }
    writeSequence(mSingle, mTotal + " " + name, data, qual, length);
    ++mTotal;
  }

  @Override
  public void writeLeftRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (!mIsPaired) {
      throw new IllegalStateException();
    }
    if (!mExpectLeft) {
      throw new IllegalStateException();
    }
    writeSequence(mLeft, mTotal + " " + name, data, qual, length);
    mExpectLeft = !mExpectLeft;
  }

  @Override
  public void writeRightRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (!mIsPaired) {
      throw new IllegalStateException();
    }
    if (mExpectLeft) {
      throw new IllegalStateException();
    }
    writeSequence(mRight, mTotal + " " + name, data, qual, length);
    mExpectLeft = !mExpectLeft;
    ++mTotal;
  }

  private void writeSequence(SdfWriter writer, String name, byte[] data, byte[] qual, int length) throws IOException {
    writer.startSequence(name);
    writer.write(data, qual, length);
    writer.endSequence();
  }


  /**
   * Close method for the writers.
   * @throws IOException whenever
   */
  @Override
  public void close() throws IOException {
    if (mIsPaired) {
      if (!mExpectLeft) {
        throw new IOException("Left and Right arms were not balanced during simulation!");
      }
      if (mLeft != null) {
        mLeft.close();
        SourceTemplateReadWriter.writeTemplateMappingFile(mLeft.directory(), mTemplateSetIds);
        SourceTemplateReadWriter.writeMutationMappingFile(mLeft.directory(), mOriginalReference);
      }
      if (mRight != null) {
        mRight.close();
        SourceTemplateReadWriter.writeTemplateMappingFile(mRight.directory(), mTemplateSetIds);
        SourceTemplateReadWriter.writeMutationMappingFile(mRight.directory(), mOriginalReference);
      }
    } else {
      if (mSingle != null) {
        mSingle.close();
        SourceTemplateReadWriter.writeTemplateMappingFile(mSingle.directory(), mTemplateSetIds);
        SourceTemplateReadWriter.writeMutationMappingFile(mSingle.directory(), mOriginalReference);
      }
    }
  }

  @Override
  public int readsWritten() {
    return mTotal;
  }

}
