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
import java.util.Locale;

import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

/**
 * Base file implementation with some extra stuff needed when dealing with the
 * "is this SAM or BAM" question
 */
public class SamBamBaseFile extends BaseFile {

  enum SamFormat { SAM, BAM, CRAM }

  private static final String[] SAM_BAM_EXTS = {SamUtils.BAM_SUFFIX, SamUtils.SAM_SUFFIX, SamUtils.CRAM_SUFFIX};

  private final SamFormat mFormat;

  /**
   * This is non private solely for testing purposes. Please use {@link #getBaseFile(File, boolean)} instead
   * @param baseFile the base file name
   * @param extension the extension
   * @param gzip whether file should have a gzip extension
   * @param format output file type
   */
  SamBamBaseFile(File baseFile, String extension, boolean gzip, SamFormat format) {
    super(baseFile, extension, gzip);
    mFormat = format;
  }

  /**
   * Gets a base file for dealing with the specifics of a SAM/BAM output option.
   * @param file The file name supplied by the user
   * @param gzip whether the file should be compressed. Ignored if file is determined to be BAM
   * @return the base file
   */
  public static SamBamBaseFile getBaseFile(File file, boolean gzip) {
    final BaseFile initial = FileUtils.getBaseFile(file, gzip, SAM_BAM_EXTS);
    switch (initial.getExtension().toLowerCase(Locale.ROOT)) {
      case SamUtils.CRAM_SUFFIX:
        return new SamBamBaseFile(initial.getBaseFile(), initial.getExtension(), false, SamFormat.CRAM);
      case SamUtils.SAM_SUFFIX:
        return new SamBamBaseFile(initial.getBaseFile(), initial.getExtension(), gzip, SamFormat.SAM);
      //case SamUtils.BAM_SUFFIX:
      default:
        return new SamBamBaseFile(initial.getBaseFile(), initial.getExtension(), false, SamFormat.BAM);
    }
  }

  /**
   * @return the primary output format type
   */
  SamFormat format() {
    return mFormat;
  }

  /**
   * @return true for BAM, false for SAM
   */
  public boolean isBam() {
    return mFormat == SamFormat.BAM;
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
    if (mFormat != that.mFormat) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + mFormat.ordinal();
  }
}
