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

import java.util.Arrays;

import com.rtg.util.cli.WrappingStringBuilder;

/**
 * Simple class to output text into an <code>RST</code> formatted table. Need to know max width of columns in advance.
 * Does not do any escaping itself
 */
public class RstTable {

  private final int[] mWidths;
  private final int mSpacing;
  private final StringBuilder mTable;
  private final int mHeaderWidth;

  /**
   * @param spacing amount of spacing between them <code>|</code> column separators and the text
   * @param headerWidth number of characters required for the header
   * @param widths maximum number of characters required in each column
   */
  public RstTable(int spacing, int headerWidth, int... widths) {
    mWidths = Arrays.copyOf(widths, widths.length);
    mSpacing = spacing;
    mTable = new StringBuilder();
    int totColumnWidth = 0;
    for (int width : mWidths) {
      if (width <= 0) {
        throw new IllegalArgumentException("Can't have zero width columns");
      }
      totColumnWidth += width + mSpacing * 2;
    }
    totColumnWidth += mWidths.length - 1;
    if (headerWidth + 2 * mSpacing > totColumnWidth) {
      mHeaderWidth = headerWidth;
    } else {
      mHeaderWidth = totColumnWidth - (2 * mSpacing);
    }
  }

  private void addSeparator() {
    for (int width : mWidths) {
      mTable.append("+").append(StringUtils.repeat("-", width + mSpacing * 2));
    }
    mTable.append("+");
    mTable.append(StringUtils.LS);
  }

  private String widthString(int width) {
    return "%-" + width + "s";
  }

  /**
   * Add a heading to the table
   * @param text the heading text
   */
  public void addHeading(String text) {
    if (text.length() > mHeaderWidth) {
      throw new IllegalArgumentException("Header text too wide, expected: " + mHeaderWidth + " got: " + text.length());
    }
    addSeparator();
    mTable.append(String.format("|" + widthString(mSpacing) + widthString(mHeaderWidth) + widthString(mSpacing) + "|%n", "", text, ""));
    for (int width : mWidths) {
      mTable.append("+").append(StringUtils.repeat("=", width + mSpacing * 2));
    }
    mTable.append("+");
    mTable.append(StringUtils.LS);
  }

  /**
   * Add a row to the table
   * @param text each value for corresponding column in this row
   */
  public void addRow(String... text) {
    if (text.length != mWidths.length) {
      throw new IllegalArgumentException("Expected " + mWidths.length + " columns, got " + text.length);
    }
    final String[][] cols = new String[text.length][];
    int rows = 0;
    for (int col = 0; col < text.length; ++col) {
      cols[col] = splitToWidthWithWrap(mWidths[col], text[col]);
      rows = Math.max(rows, cols[col].length);
    }
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols.length; ++col) {
        final String s = (row < cols[col].length) ? cols[col][row] : "";
        if (s.length() > mWidths[col]) {
          System.err.println("Arrays.toString(mWidths) = " + Arrays.toString(mWidths));
          System.err.println("Arrays.toString(text) = " + Arrays.toString(text));
          throw new IllegalArgumentException("text too wide, expected: " + mWidths[col] + " got: " + s.length() + " " + s);
        }
        final String formatString = "|" + widthString(mSpacing) + widthString(mWidths[col]) + widthString(mSpacing);
        mTable.append(String.format(formatString, "", s, ""));
      }
      mTable.append("|").append(StringUtils.LS);
    }
    addSeparator();
  }

  private static String[] splitToWidthWithWrap(int width, String text) {
    final WrappingStringBuilder sb = new WrappingStringBuilder();
    sb.setWrapWidth(width + StringUtils.LS.length()); // Don't let line breaks count toward width, since they'll get removed by split.
    sb.wrapTextWithNewLines(text);
    return sb.toString().split(StringUtils.LS);
  }

  /**
   * Escape text
   * @param text input text
   * @return <code>text</code> with special characters escaped
   */
  public static String escapeText(String text) {
    return text.replaceAll("[_\\\\]", "\\\\$0");
  }

  /**
   * @return The table in <code>RST</code>
   */
  public String getText() {
    return mTable.toString();
  }

  @Override
  public String toString() {
    return getText();
  }
}
