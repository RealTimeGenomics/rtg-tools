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
package com.rtg.launcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.ObjectParams;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.GzipAsynchOutputStream;

/**
 * Holds all the parameters needed for producing output to a directory.
 *
 */
public class OutputParams extends ObjectParams implements OutputDirParams {

  private final File mOutputDir;

  /** True iff progress to be output.  */
  private final boolean mProgress;

  /** True iff output files to be zipped. */
  private final boolean mZip;

  /**
   * @param outDir the output directory
   * @param progress true if progress should be output
   * @param zip true if output should be zipped
   */
  public OutputParams(File outDir, boolean progress, boolean zip) {
    if (outDir == null) {
      throw new NullPointerException();
    }
    mOutputDir = outDir;
    mProgress = progress;
    mZip = zip;
    append(new Object[] {mOutputDir, mProgress, mZip});
  }

  /**
   * Get the progress flag.
   *
   * @return the progress flag. (true iff progress is to be output).
   */
  public boolean progress() {
    return mProgress;
  }

  /**
   * Get a stream to an output file in the output directory. This obeys any settings for whether compression should be applied.
   * @param name file name
   * @return the stream.
   * @throws IOException if an I/O error occurs.
   */
  public OutputStream outStream(final String name) throws IOException {
    if (!directory().exists() && !directory().mkdirs()) {
      throw new IOException("Unable to create directory \"" + directory().getPath() + "\"");
    }
    return FileUtils.createOutputStream(outFile(name));
  }


  /**
   * Return the file that will be used for output given a base name. This obeys any settings for whether compression should be applied.
   * @see #isCompressed()
   * @param name base name
   * @return the output file
   */
  public File outFile(final String name) {
    return file(mZip ? name + FileUtils.GZ_SUFFIX : name);
  }

  /**
   * @return true if output is to be compressed
   */
  public boolean isCompressed() {
    return mZip;
  }

  /**
   * @return true if output is to be compressed in blocked compressed format
   */
  public boolean isBlockCompressed() {
    return mZip && GzipAsynchOutputStream.BGZIP;
  }

  /**
   * Get the name of a child file in the output directory where all results are placed, independent of any compression settings.
   * @see #outFile(String)
   * @param child the name of the child.
   * @return the name of the file.
   */
  public File file(final String child) {
    return new File(mOutputDir, child);
  }

  /**
   * Get the output directory.
   * @return the output directory.
   */
  @Override
  public File directory() {
    return mOutputDir;
  }

  @Override
  public void close() {
    // do nothing
  }

  @Override
  public String toString() {
    return "OutputParams"
        + " output directory=" + mOutputDir
        + " progress=" + mProgress
        + " zip=" + mZip;
  }

}

