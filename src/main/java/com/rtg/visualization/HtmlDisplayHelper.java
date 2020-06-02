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

package com.rtg.visualization;



/**
 * This class assists with display of text with <code>HTML</code> color markup
 */
public class HtmlDisplayHelper extends DisplayHelper {

  static final char TAG_START = '<';
  static final char TAG_END = '>';

  static final String END_SPAN = "</span>";
  static final String BOLD_ON = "<span style=\"font-weight:bold\">";
  static final String UNDERLINE_ON = "<span style=\"text-decoration:underline\">";

  static final String[] FGCOLORS = {
    "<span style=\"color:black\">",
    "<span style=\"color:red\">",
    "<span style=\"color:green\">",
    "<span style=\"color:orange\">",
    "<span style=\"color:blue\">",
    "<span style=\"color:magenta\">",
    "<span style=\"color:darkcyan\">",
    "<span style=\"color:white\">",
    "<span style=\"color:whitesmoke\">"
  };

  static final String[] BGCOLORS = {
    "<span style=\"background-color:black\"",
    "<span style=\"background-color:rgb(255, 200, 200)\">",
    "<span style=\"background-color:rgb(200, 255, 200)\">",
    "<span style=\"background-color:orange\">",
    "<span style=\"background-color:dodgerblue\">",
    "<span style=\"background-color:magenta\">",
    "<span style=\"background-color:cyan\">",
    "<span style=\"background-color:white\">",
    "<span style=\"background-color:whitesmoke\">"
  };

  @Override
  protected String header() {
    return "<html><body><pre>";
  }
  @Override
  protected String footer() {
    return "</pre></body></html>";
  }

  @Override
  public boolean supportsNesting() {
    return true;
  }

  @Override
  public String decorateUnderline(String text) {
    return UNDERLINE_ON + text + END_SPAN;
  }

  @Override
  public String decorateBold(String text) {
    return BOLD_ON + text + END_SPAN;
  }

  @Override
  public String decorateForeground(String text, int color) {
    return FGCOLORS[color] + text + END_SPAN;
  }

  @Override
  public String decorateBackground(String text, int color) {
    return BGCOLORS[color] + text + END_SPAN;
  }

  @Override
  protected boolean isMarkupStart(char c) {
    return c == TAG_START;
  }
  @Override
  protected boolean isMarkupEnd(char c) {
    return c == TAG_END;
  }

  @Override
  protected String escape(String text) {
    final StringBuilder sb = new StringBuilder();
    for (int currpos = 0; currpos < text.length(); ++currpos) {
      final char c = text.charAt(currpos);
      if (c == TAG_START) {
        sb.append("&lt;");
      } else if (c == TAG_END) {
        sb.append("&gt;");
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
