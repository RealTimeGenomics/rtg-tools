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
package com.rtg.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * General parser for text files with:
 * -ignore lines beginning with #
 * -detect lines beginning with <code>@someKey</code> and pass forward this info
 * -treat file as tab separated
 */
@TestClass("com.rtg.reference.ReferenceDetectorTest")
public abstract class GeneralParser implements Closeable {

  private final BufferedReader mReader;

  protected GeneralParser(InputStream stream) {
    mReader = new BufferedReader(new InputStreamReader(stream));
  }

  /**
   * Parses all lines of the input file. Calling {@link #parseLine(String)} for each line
   * @throws IOException if an IO error occurs
   */
  public void parse() throws IOException {
    String line;
    while ((line = mReader.readLine()) != null) {
      parseLine(line);
    }
  }

  /**
   * parses a line, forward's call on as appropriate
   * @param line the line being parsed
   */
  protected void parseLine(String line) {
    if (line.length() > 0) {
      final String[] split = StringUtils.split(line, '\t');
      final char firstChar = line.charAt(0);
      switch (firstChar) {
        case '#':
          parseHashLine(line.substring(1));
          break;
        case '@':
          parseAtLine(split[0].substring(1), Arrays.copyOfRange(split, 1, split.length));
          break;
        default:
          parseRegularLine(split);
          break;
      }
    }
  }

  /**
   * Gets the first element, or throws an exception if none exist
   * @param key the key for the line
   * @param elements the set of elements for the line
   * @return the first element
   */
  protected String getSingleValue(String key, String... elements) {
    if (elements.length > 0) {
      return elements[0];
    } else {
      throw new IllegalArgumentException("Expected value for: @" + key);
    }
  }

  protected abstract void parseHashLine(String comment);

  protected abstract void parseAtLine(String key, String... elements);

  protected abstract void parseRegularLine(String... elements);

  @Override
  public void close() throws IOException {
    mReader.close();
  }
}
