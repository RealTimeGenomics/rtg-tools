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
 * This class is concerned with display of text with <code>ANSI</code> color markup
 */
public class AnsiDisplayHelper extends DisplayHelper {

  //  See ANSI color codes, see http://en.wikipedia.org/wiki/ANSI_escape_code#CSI_Codes

  static final char ESC = (char) 27;
  static final String RESET_ATTRIBUTES = ESC + "[0m";
  static final String BOLD_ON = ESC + "[1m";
  static final String BOLD_OFF = ESC + "[22m";
  static final String DIM_ON = ESC + "[2m"; /// Not widely supported
  static final String DIM_OFF = ESC + "[22m";
  static final String ITALICS_ON = ESC + "[3m"; /// Not widely supported, sometimes shown as inverse
  static final String ITALICS_OFF = ESC + "[23m"; /// Not widely supported, sometimes shown as inverse
  static final String UNDERLINE_ON = ESC + "[4m";
  static final String UNDERLINE_OFF = ESC + "[24m";
  static final String INVERSE_ON = ESC + "[7m";
  static final String INVERSE_OFF = ESC + "[27m";

  static final String[] FGCOLORS = {
    ESC + "[30m",
    ESC + "[31m",
    ESC + "[32m",
    ESC + "[33m",
    ESC + "[34m",
    ESC + "[35m",
    ESC + "[36m",
    ESC + "[37m",
    ESC + "[39m"
  };

  // These background colors require 256 color support
  static final String[] BGCOLORS = {
    ESC + "[48;5;" + extendedColor(0, 0, 0) + "m",
    ESC + "[48;5;" + extendedColor(1, 0, 0) + "m",
    ESC + "[48;5;" + extendedColor(0, 1, 0) + "m",
    ESC + "[48;5;" + extendedColor(1, 1, 0) + "m",
    ESC + "[48;5;" + extendedColor(0, 0, 1) + "m",
    ESC + "[48;5;" + extendedColor(1, 0, 1) + "m",
    ESC + "[48;5;" + extendedColor(0, 1, 1) + "m",
    ESC + "[48;5;" + extendedColor(1, 1, 1) + "m",
    ESC + "[48;5;237m",
    //ESC + "[49m"
  };


  // Map from 0-6 RGB values into ANSI 256 color extended range
  static int extendedColor(int r, int g, int b) {
    if ((r >= 6) || (g >= 6) || (b >= 6)) {
      throw new IllegalArgumentException();
    }
    return 16 + r * 36 + g * 6 + b;
  }

  static String defaultForeground() {
    return ESC + "[39m";
  }
  static String defaultBackground() {
    return ESC + "[49m";
  }

  static String ansiForeground(int color) {
    return FGCOLORS[color];
  }

  static String ansiBackground(int color) {
    return BGCOLORS[color];
  }

  @Override
  public String decorateUnderline(String text) {
    return UNDERLINE_ON + text + UNDERLINE_OFF;
  }

  @Override
  public String decorateBold(String text) {
    return BOLD_ON + text + BOLD_OFF;
  }

  @Override
  public String decorateForeground(String text, int color) {
    return ansiForeground(color) + text + defaultForeground();
  }

  @Override
  public String decorateBackground(String text, int color) {
    return ansiBackground(color) + text + defaultBackground();
  }

  @Override
  protected boolean isMarkupStart(char c) {
    return c == ESC;
  }
  @Override
  protected boolean isMarkupEnd(char c) {
    return c == 'm';
  }
}
