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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import com.rtg.util.StringUtils;

/**
 * Adds methods that add a platform-specific line separator after the
 * write call, like <code>PrintStream.println()</code> but with proper
 * exceptions and no automatic flushing.
 */
public class LineWriter extends FilterWriter {

  /**
   * @param out the writer to wrap around
   */
  public LineWriter(Writer out) {
    super(out);
  }

  /**
   * Output a platform specific new line.
   * @throws IOException if the underlying writer exception
   */
  public void newLine() throws IOException {
    super.write(StringUtils.LS);
  }

  /**
   * Like the matching write method but adds a new line.
   * @param c the char
   * @throws IOException if the underlying writer exception
   */
  public void writeln(int c) throws IOException {
    super.write(c);
    newLine();
  }

  /**
   * Like the matching write method but adds a new line.
   * @param str the string
   * @throws IOException if the underlying writer exception
   */
  public void writeln(String str) throws IOException {
    super.write(str);
    newLine();
  }

  /**
   * Like the matching write method but adds a new line.
   * @param cbuf the buffer
   * @throws IOException if the underlying writer exception
   */
  public void writeln(char[] cbuf) throws IOException {
    super.write(cbuf);
    newLine();
  }

  /**
   * Like the matching write method but adds a new line.
   * @param cbuf the buffer
   * @param off offset
   * @param len length
   * @throws IOException if the underlying writer exception
   */
  public void writeln(char[] cbuf, int off, int len) throws IOException {
    super.write(cbuf, off, len);
    newLine();
  }

  /**
   * Like the matching write method but adds a new line.
   * @param str the string
   * @param off offset
   * @param len length
   * @throws IOException if the underlying writer exception
   */
  public void writeln(String str, int off, int len) throws IOException {
    super.write(str, off, len);
    newLine();
  }

  /**
   * Write an empty new line.
   * @throws IOException if the underlying writer exception
   */
  public void writeln() throws IOException {
    newLine();
  }

}
