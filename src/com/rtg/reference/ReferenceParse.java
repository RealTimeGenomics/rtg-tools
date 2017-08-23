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

package com.rtg.reference;

import static com.rtg.util.StringUtils.LS;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.RegionRestriction;

/**
 * Parsing for reference file.
 */
class ReferenceParse {

  static RegionRestriction region(final Map<String, Integer> sequenceLengths, final String str) {
    try {
      final RegionRestriction reg = new RegionRestriction(str);
      final String name = reg.getSequenceName();
      final Integer length = sequenceLengths.get(name);

      if (length == null || reg.getStart() == RegionRestriction.MISSING || reg.getEnd() == RegionRestriction.MISSING || reg.getEnd() > length) {
        return null;
      }
      return reg;
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }

  static Ploidy getPloidy(final String ploidyString) {
    try {
      return Ploidy.valueOf(ploidyString.toUpperCase(Locale.getDefault()));
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }

  static Sex getSex(final String sexStr) {
    try {
      return Sex.valueOf(sexStr.toUpperCase(Locale.getDefault()));
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }

  static boolean sexMatch(final Sex actual, final Sex lineSex) {
    return lineSex == Sex.EITHER || actual == lineSex;
  }

  static Boolean linear(final String linearString) {
    switch (linearString) {
      case "linear":
        return Boolean.TRUE;
      case "circular":
        return Boolean.FALSE;
      default:
        return null;
    }
  }

  /**
   * Check for comments, trim white space. Return null if nothing left after that.
   * Otherwise split on tabs and return the array.
   * @param line to be split.
   * @return the split line or null if a problem.
   */
  static String[] splitLine(final String line) {
    final int ix0 = line.indexOf('#');
    final int ix = ix0 == -1 ? line.length() : ix0;
    final String lessComment = line.substring(0, ix);
    if (lessComment.matches("^\\s*$")) {
      return null;
    }
    return lessComment.split("\\s+");
  }

  final Map<String, ReferenceSequence> mReferences = new HashMap<>();
  final Map<String, Integer> mNames;
  final BufferedReader mIn;
  final Sex mSex;
  boolean mError = false;
  final Queue<Pair<RegionRestriction, RegionRestriction>> mDuplicates = new LinkedList<>();
  boolean mLinearDefault;
  Ploidy mPloidyDefault = null;
  int mNonblankLines = 0;

  ReferenceParse(Map<String, Integer> names, BufferedReader in, Sex sex) {
    mNames = names;
    mIn = in;
    this.mSex = sex;
  }

  void error(final String msg) {
    Diagnostic.warning(msg);
    mError = true;
  }

  /**
   * Parse a reference file putting results into fields.
   * @throws IOException if I/O exception reading reference file.
   */
  void parse() throws IOException {
    while (true) {
      final String line = mIn.readLine();
      if (line == null) {
        break;
      }
      final String msg = line(line);
      if (msg != null) {
        error("Error reading reference file on line:" + line + LS + msg);
      }
    }
    end();
  }

  /**
   * Parse one line of reference file.
   * @param line complete non-null line.
   * @return an error message or null if no error.
   */
  String line(final String line) {

    final String[] split = splitLine(line);
    if (split == null) {
      return null;
    }
    ++mNonblankLines;
    if (split.length < 2) {
      return "Version line too short";
    }
    if (mNonblankLines == 1) {
      //version line
      if (split.length != 2 || !split[0].equals("version")
          || !(split[1].equals("0") || split[1].equals("1"))) {
        return "Invalid version line.";
      }
      return null;
    }

    //normal lines
    final Sex lineSex = getSex(split[0]);
    if (lineSex == null) {
      return "Invalid sex:" + split[0];
    }
    final String type = split[1];

    final boolean match = sexMatch(mSex, lineSex);
    switch (type) {
      case "def":
        return def(split, match);
      case "seq":
        return seq(split, match);
      case "dup":
        return dup(split, match);
      default:
        return "Invalid line type (should be one of: def, seq, dup):" + type;
    }
  }

  /**
   * Parse a duplicate line.
   * @param split line after splitting on tabs.
   * @param match if true sexes match and actually carry out operation.
   * @return an error message or null if no errors.
   */
  String dup(final String[] split, final boolean match) {
    if (split.length != 4) {
      return "Duplicate line has incorrect number of fields.";
    }
    final String dup1 = split[2];
    final RegionRestriction r1 = region(mNames, dup1);
    if (r1 == null) {
      return "Invalid region:" + dup1;
    }
    final String dup2 = split[3];
    final RegionRestriction r2 = region(mNames, dup2);
    if (r2 == null) {
      return "Invalid region:" + dup2;
    }
    if (match) {
      mDuplicates.add(new Pair<>(r1, r2));
    }
    return null;
  }

  /**
   * Parse a sequence line.
   * @param split line after splitting on tabs.
   * @param match if true sexes match and actually carry out operation.
   * @return an error message or null if no errors.
   */
  String seq(final String[] split, final boolean match) {
    if (split.length < 5) {
      return "Sequence line has incorrect number of fields.";
    }
    final String name = split[2].trim();
    if ("".equals(name) || name.contains(" ")) {
      return "Invalid sequence name:" + name;
    }
    if (!mNames.containsKey(name)) {
      return "Sequence in reference file:" + name + " not found in genome.";
    }
    final String ploidyString = split[3];
    final Ploidy ploidy = getPloidy(ploidyString);
    if (ploidy == null) {
      return "Invalid ploidy value:" + ploidyString;
    }
    final String linearString = split[4];
    final Boolean linear = linear(linearString);
    if (linear == null) {
      return "Invalid linear/circular value:" + linearString;
    }
    final String hapMate; // haploid complement is optional e.g. X0 sex determination system.
    if ((ploidy == Ploidy.HAPLOID) && (split.length == 6)) {
      hapMate = split[5].trim();
      if ("".equals(hapMate) || hapMate.contains(" ")) {
        return "Invalid haploid mate sequence name:" + hapMate;
      }
      if (!mNames.containsKey(hapMate)) {
        return "Haploid mate sequence in reference file:" + hapMate + " not found in genome.";
      }
    } else if (split.length > 5) {
      return "Sequence line has incorrect number of fields.";
    } else {
      hapMate = null;
    }

    if (match) {
      if (mReferences.containsKey(name)) {
        return "Sequence defined twice:" + name;
      }
      mReferences.put(name, new ReferenceSequence(true, linear, ploidy, name, hapMate, mNames.get(name)));
    }
    return null;
  }

  /**
   * Parse a default line.
   * @param split line after splitting on tabs.
   * @param match if true sexes match and actually carry out operation.
   * @return an error message or null if no errors.
   */
  String def(final String[] split, final boolean match) {
    if (split.length != 4) {
      return "Default line has incorrect number of fields.";
    }
    final String ploidyString = split[2];
    final Ploidy ploidy = getPloidy(ploidyString);
    if (ploidy == null) {
      return "Invalid ploidy value:" + ploidyString;
    }
    final String linearString = split[3];
    final Boolean linear = linear(linearString);
    if (linear == null) {
      return "Invalid linear/circular value:" + linearString;
    }
    if (match) {
      if (mPloidyDefault != null) {
        return "Duplicate default definition.";
      }
      mLinearDefault = linear;
      mPloidyDefault = ploidy;
    }
    return null;
  }

  public void end() {
    if (mNonblankLines == 0) {
      error("No valid lines found in reference file.");
    }
  }
}
