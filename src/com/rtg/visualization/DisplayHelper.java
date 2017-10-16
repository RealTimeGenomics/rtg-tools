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


import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.Environment;
import com.rtg.util.StringUtils;

/**
 * Assists with display of potentially colored text.
 * This base class displays without any markup, and subclasses add markup capability.
 */
public class DisplayHelper {

  /** Possible markup implementations */
  public enum MarkupType {
    /** Perform no markup */
    NONE,
    /** Markup with <code>ANSI</code> escape sequences */
    ANSI,
    /** Markup with <code>HTML</code> sequences */
    HTML,
    /** Try and auto-detect whether to use ANSI or NONE */
    AUTO,
  }

  /** A default instance that can be used for text display */
  public static final DisplayHelper DEFAULT = getDefault();

  static DisplayHelper getDefault() {
      switch ((MarkupType) GlobalFlags.getFlag(ToolsGlobalFlags.DEFAULT_MARKUP).getValue()) {
        case ANSI:
          return new AnsiDisplayHelper();
        case HTML:
          return new HtmlDisplayHelper();
        case AUTO:
          return System.console() != null && !Environment.OS_WINDOWS ? new AnsiDisplayHelper() : new DisplayHelper();
        case NONE:
        default:
          return new DisplayHelper();
      }
  }

  static final int LABEL_LENGTH = 6;

  static final char SPACE_CHAR = ' ';
  static final char INSERT_CHAR = '_';

  //Define color identifiers.
  /** Color code for black */
  public static final int BLACK = 0;
  /** Color code for red */
  public static final int RED = 1;
  /** Color code for green */
  public static final int GREEN = 2;
  /** Color code for yellow */
  public static final int YELLOW = 3;
  /** Color code for blue */
  public static final int BLUE = 4;
  /** Color code for magenta */
  public static final int MAGENTA = 5;
  /** Color code for cyan */
  public static final int CYAN = 6;
  /** Color code for white */
  public static final int WHITE = 7;

  /** Color code for off-white */
  public static final int WHITE_PLUS = 8;

  // Colors used for consistent "Theme"
  /** Section headers etc */
  public static final int THEME_SECTION_COLOR = CYAN;
  /** Used for text the user types literally */
  public static final int THEME_LITERAL_COLOR = GREEN;
  /** General entry types, where the user will enter a specific value of the type */
  public static final int THEME_TYPE_COLOR = YELLOW;
  /** For error messages */
  public static final int THEME_ERROR_COLOR = RED;

  String getSpaces(final int diff) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < diff; ++i) {
      sb.append(SPACE_CHAR);
    }
    return sb.toString();
  }

  String getInserts(final int diff) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < diff; ++i) {
      sb.append(INSERT_CHAR);
    }
    return sb.toString();
  }

  int getBaseColor(char readChar) {
    switch (readChar) {
      case 'a':
      case 'A':
        return DisplayHelper.GREEN;
      case 't':
      case 'T':
        return DisplayHelper.RED;
      case 'c':
      case 'C':
        return DisplayHelper.BLUE;
      case 'g':
      case 'G':
        return DisplayHelper.MAGENTA;
      default:
        return -1;
    }
  }

  protected String escape(String text) {
    return text;
  }
  protected String header() {
    return null;
  }
  protected String footer() {
    return null;
  }

  /**
   * @return true if display properties can nest
   */
  public boolean supportsNesting() {
    return false;
  }

  /**
   * Mark up the text with underlining
   * @param text the text
   * @return the marked up text
   */
  public String decorateUnderline(String text) {
    return text;
  }

  /**
   * Mark up the text with boldness
   * @param text the text
   * @return the marked up text
   */
  public String decorateBold(String text) {
    return text;
  }

  /**
   * Mark up the text with a foreground color
   * @param text the text
   * @param color the foreground color
   * @return the marked up text
   */
  public String decorateForeground(String text, int color) {
    return text;
  }

  /**
   * Mark up the text with a background color
   * @param text the text
   * @param color the background color
   * @return the marked up text
   */
  public String decorateBackground(String text, int color) {
    return text;
  }

  /**
   * Mark up the text with foreground and background colors
   * @param text the text
   * @param fgcolor the foreground color
   * @param bgcolor the background color
   * @return the marked up text
   */
  public String decorate(final String text, int fgcolor, int bgcolor) {
    return decorateForeground(decorateBackground(text, bgcolor), fgcolor);
  }

  protected String decorateLabel(final String label) {
    String shortLabel = (label.length() >= LABEL_LENGTH ? label.substring(label.length() - LABEL_LENGTH) : label) + ":";
    if (shortLabel.length() < LABEL_LENGTH) {
      shortLabel = getSpaces(LABEL_LENGTH - shortLabel.length()) + shortLabel;
    }
    return decorateForeground(shortLabel, DisplayHelper.CYAN) + " ";
  }

  protected boolean isMarkupStart(char c) {
    return false;
  }
  protected boolean isMarkupEnd(char c) {
    return false;
  }

  /**
   * Compute the length of a string, excluding markup
   * @param text the text
   * @return the string length
   */
  public int length(final String text) {
    int length = 0;
    boolean inMarkup = false;
    for (int currpos = 0; currpos < text.length(); ++currpos) {
      final char c = text.charAt(currpos);
      if (inMarkup) {
        if (isMarkupEnd(c)) {
          inMarkup = false;
        }
      } else {
        if (isMarkupStart(c)) {
          inMarkup = true;
        } else {
          ++length;
        }
      }
    }
    return length;
  }

  /**
   * Mark up a section of text that contains DNA bases, coloring each base accordingly
   * @param text the text
   * @return the marked up text
   */
  public String decorateBases(final String text) {
    return decorateWithHighlight(text, null, BLACK, true);
  }

  // Decorates a section of DNA with highlight colors, not marking up space characters at the ends
  protected String decorateWithHighlight(final String str, boolean[] highlightMask, int bgcolor, boolean colorBases) {
    final StringBuilder output = new StringBuilder();
    int coord = 0; // coordinate ignoring markup
    final StringBuilder toHighlight = new StringBuilder();
    boolean highlight = false;
    boolean inMarkup = false;
    for (int i = 0; i < str.length(); ++i) {
      final char c = str.charAt(i);
      if (inMarkup) {
        if (isMarkupEnd(c)) {
          inMarkup = false;
        }
        output.append(c);
      } else {
        if (isMarkupStart(c)) {
          inMarkup = true;
          output.append(c);
        } else {
          final boolean hl = highlightMask != null && coord < highlightMask.length && highlightMask[coord];
          if (hl != highlight) {
            if (highlight) {
              output.append(trimHighlighting(toHighlight.toString(), bgcolor));
              toHighlight.setLength(0);
            }
            highlight = highlightMask[coord];
          }
          final StringBuilder dest = highlight ? toHighlight : output;
          final int col = getBaseColor(c);
          if (colorBases && col >= 0) {
            dest.append(decorateForeground(String.valueOf(c), col));
          } else {
            dest.append(c);
          }
          ++coord;
        }
      }
    }
    if (highlight) {
      output.append(trimHighlighting(toHighlight.toString(), bgcolor));
    }
    return output.toString();
  }

  private String trimHighlighting(final String dna, int bgcolor) {
    final String trimmed = StringUtils.trimSpaces(dna);
    if (trimmed.length() == 0) { // All whitespace, no markup needed
      return dna;
    }
    if (trimmed.length() == dna.length()) { // No trimming needed
      return decorateBackground(dna, bgcolor);
    } else {
      if (dna.charAt(0) == ' ') { // Some amount of non-marked up prefix needed
        int prefixEnd = 0;
        while (dna.charAt(prefixEnd) == ' ') {
          ++prefixEnd;
        }
        return dna.substring(0, prefixEnd) + decorateBackground(trimmed, bgcolor) + dna.substring(prefixEnd + trimmed.length());
      } else { // Trimming was only at the end
        return decorateBackground(trimmed, bgcolor) + dna.substring(trimmed.length());
      }
    }
  }

  /**
   * Trims a sequence for display.
   * @param sequence the string to clip. May contain markup
   * @param clipStart first position in non-markup coordinates to output
   * @param clipEnd end position (exclusive) in non-markup coordinates
   * @return the clipped sequence.
   */
  protected String clipSequence(String sequence, final int clipStart, final int clipEnd) {
    final StringBuilder sb = new StringBuilder();
    int coord = 0; // coordinate ignoring markup
    boolean inMarkup = false;
    for (int currpos = 0; currpos < sequence.length(); ++currpos) {
      final char c = sequence.charAt(currpos);
      if (inMarkup) {
        if (isMarkupEnd(c)) {
          inMarkup = false;
        }
        sb.append(c);
      } else {
        if (isMarkupStart(c)) {
          inMarkup = true;
          sb.append(c);
        } else {
          if ((coord >= clipStart) && (coord < clipEnd)) {
            sb.append(c);
          }
          ++coord;
        }
      }
    }
    return sb.toString();
  }
}
