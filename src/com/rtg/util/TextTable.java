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

import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.util.array.ArrayUtils;
import com.rtg.util.diagnostic.SlimException;

/**
 * A class to format a table of data using aligned columns.
 * Will make human readable tables in CLI output, not necessarily machine readable.
 */
public class TextTable {

  /**
   * Specifies possible column alignment options
   */
  public enum Align {
    /** Left justifies the cell */
    LEFT {
      @Override
      void appendCell(StringBuilder sb, int maxSize, String cell) {
        sb.append(cell);
        appendSpaces(sb, maxSize - cell.length());
      }
    },
    /** Center justifies the cell */
    CENTER {
      @Override
      void appendCell(StringBuilder sb, int maxSize, String cell) {
        final int toAdd = maxSize - cell.length();
        final int left = toAdd / 2;
        appendSpaces(sb, left);
        sb.append(cell);
        appendSpaces(sb, toAdd - left);
      }
    },
    /** Right justifies the cell */
    RIGHT() {
      @Override
      void appendCell(StringBuilder sb, int maxSize, String cell) {
        appendSpaces(sb, maxSize - cell.length());
        sb.append(cell);
      }
    };

    /**
     * Append a cell to the text representation.
     * @param sb accumulating the text representation
     * @param maxSize the output width of the column
     * @param cell the input cell contents
     */
    abstract void appendCell(StringBuilder sb, int maxSize, String cell);

  }

  private static final ArrayList<String> SEPARATOR = new ArrayList<>();

  private final int mTabWidth;
  private final int mInitialIndent;
  private final Align mDefaultAlignment;
  private final ArrayList<ArrayList<String>> mTableContents = new ArrayList<>();
  private int[] mColumnWidths = null;

  private Align[] mAlignment = new Align[0];

  /**
   * Default constructor with tab width set to two.
   */
  public TextTable() {
    this(2, 0, Align.RIGHT);
  }

  /**
   * Constructor
   * @param tabWidth the width of the tab spacing
   * @param initialIndent the number of spaces to have as a base indent at start of each line
   * @param defaultAlignment the alignment used for cells by default
   */
  public TextTable(int tabWidth, int initialIndent, Align defaultAlignment) {
    mTabWidth = tabWidth;
    mInitialIndent = initialIndent;
    mDefaultAlignment = defaultAlignment;
  }

  /**
   * Set the column alignments
   * @param alignment the alignment for each column. If there are more columns than provided alignments, the table default is used.
   */
  public void setAlignment(Align... alignment) {
    if (mTableContents.size() > 0 && mTableContents.get(0).size() < alignment.length) {
      throw new SlimException("Too many alignment values supplied for number of columns in table formatter");
    }
    mAlignment = Arrays.copyOf(alignment, alignment.length);
  }

  /**
   * Add a row of entries to the table
   * Each row must be the same length and contain no nulls
   * @param cells the cells to add
   */
  public void addRow(String... cells) {
    if (mTableContents.size() > 0 && mTableContents.get(0).size() != cells.length) {
      throw new SlimException("Mismatching number of columns in table formatter");
    }
    if (cells.length < mAlignment.length) {
      throw new SlimException("Too many alignment values supplied for number of columns supplied in row");
    }
    if (mColumnWidths == null) {
      mColumnWidths = new int[cells.length];
    }
    final ArrayList<String> cellList = new ArrayList<>(cells.length);
    int col = 0;
    for (final String cell : cells) {
      if (cell == null) {
        throw new SlimException("Null provided as value in table formatters");
      }
      cellList.add(cell);
      mColumnWidths[col] = Math.max(mColumnWidths[col], cell.length());
      col++;
    }
    mTableContents.add(cellList);
  }

  /**
   * Add a separator line to the table.
   */
  public void addSeparator() {
    mTableContents.add(SEPARATOR);
  }

  /**
   * Get the number of rows that have been added to the table.
   * @return the number of rows added.
   */
  public int numRows() {
    return mTableContents.size();
  }

  /**
   * Reset the table so it can be used to format a new set of rows
   */
  public void reset() {
    mTableContents.clear();
  }

  private static void appendSpaces(StringBuilder sb, int number) {
    for (int i = 0; i < number; i++) {
      sb.append(' ');
    }
  }

  // Takes care to handle case of empty columns at end of table
  private long getTotalWidth() {
    long totalWidth =  ArrayUtils.sum(mColumnWidths) + mTabWidth * (mColumnWidths.length - 1L);
    for (int j = mColumnWidths.length - 1; j >= 0 && mColumnWidths[j] == 0; j--) {
      totalWidth -= mTabWidth;
    }
    return totalWidth;
  }

  /**
   * Add formatted table to string builder
   * @param sb string builder to append to
   */
  public void toString(StringBuilder sb) {
    final int rows = mTableContents.size();
    if (rows > 0) {
      final int columns = mTableContents.get(0).size();
      if (columns > 0) {
        final int[] maxLengths = mColumnWidths;
        final long totalWidth =  getTotalWidth();
        for (final ArrayList<String> row : mTableContents) {
          if (row == SEPARATOR) {
            for (long j = 0; j < totalWidth; j++) {
              sb.append('-');
            }
          } else {
            for (int j = 0; j < columns; j++) {
              if (j == 0) {
                appendSpaces(sb, mInitialIndent);
              } else {
                appendSpaces(sb, mTabWidth);
              }
              final Align align = j < mAlignment.length ? mAlignment[j] : mDefaultAlignment;
              align.appendCell(sb, maxLengths[j], row.get(j));
            }
          }
          sb.append(StringUtils.LS);
        }
      }
    }
  }

  /**
   * Output the table as basic tab separated
   * @return the <code>TSV</code> representation
   */
  public String getAsTsv() {
    final StringBuilder sb = new StringBuilder();
    for (ArrayList<String> row : mTableContents) {
      if (row != SEPARATOR) {
        boolean first = true;
        for (String column : row) {
          if (!first) {
            sb.append("\t");
          }
          first = false;
          sb.append(column);
        }
        sb.append(StringUtils.LS);
      }
    }
    return sb.toString();
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }

}
