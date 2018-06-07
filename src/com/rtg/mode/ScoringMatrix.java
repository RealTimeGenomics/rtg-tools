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
package com.rtg.mode;

import static com.rtg.util.StringUtils.LS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import com.rtg.util.TsvParser;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 * An abstract class to keep common components of protein scoring matrices
 */
public abstract class ScoringMatrix implements Serializable, Integrity {

  protected int[][] mScores;

  /**
   * Get the integer value of the score.
   * @param i first ordinal value of an amino acid.
   * @param j second ordinal value of an amino acid.
   * @return score.
   */
  public int score(final int i, final int j) {
    return mScores[i][j];
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (final int[] mScore : mScores) {
      sb.append(Arrays.toString(mScore)).append(LS);
    }
    return sb.toString();
  }

  @Override
  public boolean integrity() {
    for (int i = 0; i < mScores.length; ++i) {
      Exam.assertEquals(mScores[i].length, mScores.length);
      for (int j = 0; j < mScores[i].length; ++j) {
        final int sc = mScores[i][j];
        Exam.assertTrue(-10 < sc && sc < 20);
        Exam.assertEquals(sc, mScores[j][i]);
      }
    }
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

  protected int codeIndex(final ProteinFastaSymbolTable table, final String x) {
    final Protein pr = (Protein) table.scanResidue(x.charAt(0));
    return pr.ordinal();
  }

  protected void parse(final BufferedReader in) throws IOException {
    new TsvParser<Void>() {
      private final ProteinFastaSymbolTable mTable = new ProteinFastaSymbolTable();
      private int[] mColIds = null;
      @Override
      protected String[] split() {
        return line().split("\\ +");
      }
      @Override
      protected void parseLine(String... splits) {
        if (mColIds == null) {
          //get the ids for the columns
          mColIds = new int[splits.length];
          for (int i = 1; i < splits.length; ++i) {
            final String x = splits[i];
            mColIds[i] = codeIndex(mTable, x);
          }
        } else {
          final int row = codeIndex(mTable, splits[0]);
          if (row > -1) {
            //System.err.println("row=" + row);
            for (int i = 1; i < splits.length; ++i) {
              final int col = mColIds[i];
              if (col == -1) {
                continue;
              }
              final int sc = Integer.parseInt(splits[i]);
              mScores[row][col] = sc;
              //System.err.println("[" + i + "]" + splits[i].length() + ":" + splits[i]);
            }
          }
        }
      }
    }.parse(in);
  }
}
