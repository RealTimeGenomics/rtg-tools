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

package com.rtg.vcf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.rtg.tabix.BrLineReader;
import com.rtg.tabix.LineReader;
import com.rtg.util.StringUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Handles low-level parsing of VCF headers and individual records.
 */
public class VcfParser {

  private static final int CHROM_FIELD = 0;
  private static final int POS_FIELD = 1;
  private static final int ID_FIELD = 2;
  private static final int REF_FIELD = 3;
  private static final int ALT_FIELD = 4;
  private static final int QUAL_FIELD = 5;
  private static final int FILTER_FIELD = 6;
  private static final int INFO_FIELD = 7;


  /**
   * Parse the header from a file.
   * @param f the file
   * @return the header
   * @throws IOException if there is a problem reading the source
   */
  public VcfHeader parseHeader(File f) throws IOException {
    try (Reader r = new InputStreamReader(FileUtils.createInputStream(f, false))) {
      return parseHeader(r);
    }
  }

  // Convenience method
  VcfHeader parseHeader(Reader r) throws IOException {
    try (BrLineReader br = new BrLineReader(new BufferedReader(r))) {
      return parseHeader(br);
    }
  }

  /**
   * Parse a VCF header.
   * @param in input source to read the header from
   * @return the parsed header
   * @throws IOException if there is a problem reading the source
   */
  public VcfHeader parseHeader(LineReader in) throws IOException {
    try {
      final VcfHeader header = new VcfHeader();
      String line;
      while ((line = in.readLine()) != null) {
        try {
          if (line.startsWith("##")) {
            header.addMetaInformationLine(line);
          } else if (line.startsWith("#")) {
            //should always be last header line
            header.addColumnHeaderLine(line);
            break;
          }
        } catch (final VcfFormatException e) {
          throw new VcfFormatException("Invalid VCF header. " + e.getMessage() + " on line:" + line); // Add context information
        }
      }
      if (header.getVersionLine() == null) {
        throw new VcfFormatException("No VCF file format version header line found");
      }
      return header;
    } catch (IOException | RuntimeException e) {
      in.close();
      throw e;
    }
  }

  /**
   * Turn a line of <code>VCF</code> output into a {@link VcfRecord}
   * @param line line of file
   * @return the corresponding record
   */
  public VcfRecord parseLine(String line) {
    return parseFields(StringUtils.split(line, '\t'));
  }

  /**
   * Turn a line of <code>VCF</code> output into a {@link VcfRecord}
   * @param field the fields of the record after splitting on tab
   * @return the corresponding record
   */
  public VcfRecord parseFields(String... field) {
    if (field.length < 8) {
      throw new VcfFormatException("Expected at least 8 fields");
    }
    for (int i = 0; i < field.length; i++) {
      if (field[i].trim().length() == 0) {
        throw new VcfFormatException("Field in column " + (i + 1) + " is empty");
      }
    }

    final int pos;
    try {
      pos = Integer.parseInt(field[POS_FIELD]) - 1;
    } catch (NumberFormatException e) {
      throw new VcfFormatException(e.getMessage());
    }
    final String ref = field[REF_FIELD];
    if (ref.length() == 0) {  /// VCF spec implies (but is not specific) that we could also reject if VcfRecord.MISSING.equals(ref)
      throw new VcfFormatException("REF field cannot be missing");
    }
    final VcfRecord rec = new VcfRecord(field[CHROM_FIELD], pos, ref);
    rec.setId(field[ID_FIELD]);
    if (!VcfRecord.MISSING.equals(field[ALT_FIELD])) {
      final String[] altSplit = StringUtils.split(field[ALT_FIELD], ',');
      for (final String anAltSplit : altSplit) {
        if (anAltSplit.length() == 0) {
          throw new VcfFormatException("An empty ALT allele is not permitted");
        }
        rec.addAltCall(anAltSplit);
      }
    }
    rec.setQuality(field[QUAL_FIELD]);  // "." or float.
    final String[] filterSplit = StringUtils.split(field[FILTER_FIELD], ';');
    for (final String aFilterSplit : filterSplit) {
      if (!VcfRecord.MISSING.equals(aFilterSplit)) {
        rec.addFilter(aFilterSplit);
      }
    }
    if (!VcfRecord.MISSING.equals(field[INFO_FIELD])) {
      final String[] infoSplit = StringUtils.split(field[INFO_FIELD], ';');
      for (final String anInfoSplit : infoSplit) {
        final String[] singleInfoSplit = StringUtils.split(anInfoSplit, '=', 2);
        final String key = singleInfoSplit[0];
        if (rec.getInfo().containsKey(key)) {
          throw new VcfFormatException("Duplicate INFO field: " + key);
        }
        if (singleInfoSplit.length == 1) {
          rec.setInfo(key);
        } else {
          final String[] vals = StringUtils.split(singleInfoSplit[1], ',');
          rec.setInfo(key, vals);
        }
      }
    }
    rec.setNumberOfSamples(0);
    // now parse each sample field.
    if (field.length > 8) {
      if (field.length == 9) {
        throw new VcfFormatException("Format field exists without sample fields");
      }
      final String[] formatFields = StringUtils.split(field[8], ':');
      rec.setNumberOfSamples(field.length - 9);
      for (final String key : formatFields) {
        if (rec.hasFormat(key)) {
          throw new VcfFormatException("Duplicate FORMAT field: " + key);
        }
        rec.addFormat(key);
      }
      for (int sample = 9; sample < field.length; ++sample) {
        final String[] formatValues = StringUtils.split(field[sample], ':');
        if (formatValues.length > formatFields.length) {
          throw new VcfFormatException("Column " + (sample + 1) + " does not have the same number of values as specified in the format column. Field=" + field[sample]);
        }
        for (int i = 0; i < formatValues.length; ++i) {
          rec.addFormatAndSample(formatFields[i], formatValues[i]);
        }
        for (int i = formatValues.length; i < formatFields.length; ++i) {
          rec.addFormatAndSample(formatFields[i], VcfRecord.MISSING);
        }
      }
    }
    return rec;
  }
}
