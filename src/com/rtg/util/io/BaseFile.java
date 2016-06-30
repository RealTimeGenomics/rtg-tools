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
package com.rtg.util.io;

import java.io.File;

/**
 * Convenience for dealing with a base file name that may have suffixes before the file extensions
 */
public class BaseFile {
  private final File mBaseFile;
  private final String mExtension;
  private final boolean mGzip;

  /**
   * @param baseFile file name up to point that suffixes and/or extensions are applied
   * @param extension extension that should be appended after suffix
   * @param gzip whether a gzip extension should be applied to end
   */
  public BaseFile(File baseFile, String extension, boolean gzip) {
    mBaseFile = baseFile;
    mExtension = extension;
    mGzip = gzip;
  }

  /**
   * @return File name excluding {@link #getExtension()} and gzip extension
   */
  public File getBaseFile() {
    return mBaseFile;
  }

  /**
   * @return extension that is to used with base file name
   */
  public String getExtension() {
    return mExtension;
  }

  /**
   * @return whether gzip extension should be appended to file name
   */
  public boolean isGzip() {
    return mGzip;
  }

  /**
   * Apply suffix to base file and add all relevant extensions
   * @param suffix suffix for base file
   * @return the fully named file
   */
  public File suffixedFile(String suffix) {
    return new File(mBaseFile.getParentFile(), mBaseFile.getName() + suffix + mExtension + (mGzip ? FileUtils.GZ_SUFFIX : ""));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final BaseFile baseFile = (BaseFile) o;

    if (mGzip != baseFile.mGzip) {
      return false;
    }
    if (!mBaseFile.equals(baseFile.mBaseFile)) {
      return false;
    }
    if (!mExtension.equals(baseFile.mExtension)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = mBaseFile.hashCode();
    result = 31 * result + mExtension.hashCode();
    result = 31 * result + (mGzip ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "BaseFile: " + suffixedFile("[") + "]";
  }
}
