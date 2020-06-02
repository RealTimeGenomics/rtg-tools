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

package com.rtg.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.util.Resources;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;

/**
 * Helper utils for writing reports
 */
@JumbleIgnore
public final class ReportUtils {
  /** location of resources */
  public static final String TEMPLATE_DIR = "com/rtg/report/resources";
  private ReportUtils() { }


  /**
   * Truncates string at given length, appends <code>"..."</code> if string was shortened
   * @param string string to truncate
   * @param length length to truncate at
   * @return if string length is less than or equal to given length then the given string, otherwise the given string truncated at given length with {@code ...} appended
   */
  public static String truncate(String string, int length) {
    return string.length() > length ? string.substring(0, length) + "..." : string;
  }

  static String[] resourceArray(String... resources) {
    final String[] result = new String[resources.length];
    for (int i = 0; i < resources.length; ++i) {
      final String r = resources[i];
      result[i] = TEMPLATE_DIR + "/" + r;
    }
    return result;
  }

  /**
   * writes given template resource to given output file replacing things in <code>replacements</code> when found
   * @param templateFile name ot resource containing template
   * @param outputFile output file
   * @param replacements mapping between placeholder and final text
   * @throws IOException if an IO error occurs
   */
  public static void writeHtml(String templateFile, File outputFile, Map<String, String> replacements) throws IOException {
    try (final OutputStream os = FileUtils.createOutputStream(outputFile)) {
      writeHtml(templateFile, os, replacements);
    }
  }

  /**
   * writes given template resource to given output stream replacing things in <code>replacements</code> when found
   * @param templateFile name ot resource containing template
   * @param output output stream
   * @param replacements mapping between placeholder and final text
   * @throws IOException if an IO error occurs
   */
  public static void writeHtml(String templateFile, OutputStream output, Map<String, String> replacements) throws IOException {
    final InputStream is = Resources.getResourceAsStream(templateFile);

    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      try (final LineWriter pw = new LineWriter(new OutputStreamWriter(output))) {
        fillTemplate(replacements, br, pw);
      }
    }
  }

  static void fillTemplate(Map<String, String> replacements, BufferedReader br, LineWriter pw) throws IOException {
    String line;
    while ((line = br.readLine()) != null) {
      for (Map.Entry<String, String> entry : replacements.entrySet()) {
        line = line.replace(entry.getKey(), entry.getValue());
      }
      pw.writeln(line);
    }
  }
}
