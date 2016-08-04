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
package com.rtg.sam;

import java.io.File;

import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

/**
 * Base file implementation with some extra stuff needed when dealing with the
 * "is this SAM or BAM" question
 */
public class SamBamBaseFile extends BaseFile {

  private static final String[] SAM_BAM_EXTS = {SamUtils.BAM_SUFFIX, SamUtils.SAM_SUFFIX};

  private final boolean mBam;

  /**
   * This is non private solely for testing purposes. Please use {@link #getBaseFile(File, boolean)} instead
   * @param baseFile the base file name
   * @param extension the extension
   * @param gzip whether file should be compressed
   * @param bam if output file is bam
   */
  SamBamBaseFile(File baseFile, String extension, boolean gzip, boolean bam) {
    super(baseFile, extension, gzip);
    mBam = bam;
  }

  /**
   * Gets a base file for dealing with the specifics of a SAM/BAM output option.
   * @param file The file name supplied by the user
   * @param gzip whether the file should be compressed. Ignored if file is determined to be BAM
   * @return the base file
   */
  public static SamBamBaseFile getBaseFile(File file, boolean gzip) {
    final BaseFile initial = FileUtils.getBaseFile(file, gzip, SAM_BAM_EXTS);
    if (initial.getExtension().equalsIgnoreCase(SamUtils.BAM_SUFFIX)) {
      return new SamBamBaseFile(initial.getBaseFile(), initial.getExtension(), false, true);
    } else {
      return new SamBamBaseFile(initial.getBaseFile(), initial.getExtension(), gzip, false);
    }
  }

  /**
   * @return true for BAM, false for SAM
   */
  public boolean isBam() {
    return mBam;
  }

  /**
   * @return whether an index can be produced on output file
   */
  public boolean isIndexable() {
    return isBam() || isGzip();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final SamBamBaseFile that = (SamBamBaseFile) o;
    if (mBam != that.mBam) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mBam ? 1 : 0);
    return result;
  }
}
