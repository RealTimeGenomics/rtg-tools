/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.rtg.util.io.FileUtils;

/**
 * Makes parsing TSV files a little easier
 */
public abstract class TsvParser<T> {

  private int mLineNumber;
  private String mLine;

  /**
   * Parse the supplied file
   * @param file the input file
   * @throws IOException if there was a problem parsing the file.
   * @return the result of parsing
   */
  public T parse(File file) throws IOException {
    try (final BufferedInputStream is = FileUtils.createInputStream(file, false)) {
      return parse(is);
    }
  }

  /**
   * Parse the supplied input stream
   * @param is the input source
   * @throws IOException if there was a problem parsing the file.
   * @return the result of parsing
   */
  public T parse(InputStream is) throws IOException {
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      return parse(reader);
    }
  }

  /**
   * Parse the supplied reader
   * @param reader the input source
   * @throws IOException if there was a problem parsing the file.
   * @return the result of parsing
   */
  public T parse(BufferedReader reader) throws IOException {
    mLineNumber = 0;
    while ((mLine = reader.readLine()) != null) {
      ++mLineNumber;
      if (mLine.length() != 0) {
        if (mLine.startsWith("#")) {
          parseHeader(mLine);
        } else {
          parseLine(split());
        }
      }
    }
    return result();
  }

  protected int lineNumber() {
    return mLineNumber;
  }

  protected String line() {
    return mLine;
  }

  protected String[] split() {
    return StringUtils.split(line(), '\t');
  }

  // Override some of the below methods

  /**
   * Process a header line (that is, starts with '#' character)
   * @param line the header line
   * @throws IOException if there was a problem parsing the header line.
   */
  protected void parseHeader(String line) throws IOException { }

  protected void parseLine(String... columns) throws IOException { }

  protected T result() {
    return null;
  }

}
