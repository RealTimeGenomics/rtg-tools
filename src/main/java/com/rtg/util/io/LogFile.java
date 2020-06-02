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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * A log backed by a file which can be deleted.
 */
public class LogFile implements LogStream {


  private final PrintStream mStream;

  private final File mFile;

  /**
   * @param file which is used to back the stream.
   */
  public LogFile(final File file) {
    if (file == null) {
      throw new NullPointerException();
    }
    mFile = file;
    FileOutputStream fileOutputStream;
    try {
      fileOutputStream = new FileOutputStream(file, true);
    } catch (final FileNotFoundException e) {
      fileOutputStream = null;
    }
    if (fileOutputStream == null) {
      mStream = null;
    } else {
      mStream = new PrintStream(fileOutputStream);
    }
  }

  @Override
  public void close() {
    if (mStream != null) {
      mStream.close();
    }
  }

  @Override
  public File file() {
    return mFile;
  }

  @Override
  public void removeLog() {
    close();
    if (mStream != null) {
      if (mFile.exists() && !mFile.delete()) {
        throw new RuntimeException("Unable to delete log file: " + mFile.getPath());
      }
    }
  }

  @Override
  public PrintStream stream() {
    return mStream;
  }

  @Override
  public String toString() {
    return "LogFile " + mFile;
  }

}

