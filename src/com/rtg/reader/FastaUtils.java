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
package com.rtg.reader;


import java.io.File;
import java.util.Arrays;

import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

/**
 * Utility functions for manipulating FASTA/FASTQ encoded data, e.g. between binary and ascii phred.
 */
public final class FastaUtils {

  private static final String[] EXTS = {".fasta", ".fa"};

  /** ASCII PHRED highest allowed value */
  public static final int PHRED_UPPER_LIMIT_CHAR = '~';
  /** ASCII PHRED lowest allowed value */
  public static final int PHRED_LOWER_LIMIT_CHAR = '!';

  private FastaUtils() {
  }

  /**
   * @return array of extensions that we recognize for FASTA files
   */
  public static String[] extensions() {
    return Arrays.copyOf(EXTS, EXTS.length);
  }

  // Convert between raw and ascii phred quality values

  /**
   * @param quality the raw quality value
   * @return the ASCII phred value corresponding to raw quality value
   */
  public static char rawToAsciiQuality(byte quality) {
    return (char) (quality + PHRED_LOWER_LIMIT_CHAR);
  }

  /**
   * @param quality the ASCII quality value
   * @return the raw quality value corresponding to the ASCII quality value
   */
  public static byte asciiToRawQuality(char quality) {
    return (byte) (quality - PHRED_LOWER_LIMIT_CHAR);
  }

  /**
   * @param qualities the raw quality values
   * @return the ASCII phred values corresponding to raw quality values
   */
  public static char[] rawToAsciiQuality(byte[] qualities) {
    if (qualities == null) {
      return null;
    }
    final char[] result = new char[qualities.length];
    for (int i = 0; i < qualities.length; ++i) {
      result[i] = rawToAsciiQuality(qualities[i]);
    }
    return result;
  }

  /**
   * Converts an array of bytes into a sanger-encoded quality string
   *
   * @param quality buffer containing input qualities
   * @return the quality string
   */
  public static String rawToAsciiString(byte[] quality) {
    return new String(rawToAsciiQuality(quality));
  }

  /**
   * Converts an array of bytes into a sanger-encoded quality string
   *
   * @param quality buffer containing input qualities
   * @param length the number of bytes from the input buffer to convert
   * @return the quality string
   */
  public static String rawToAsciiString(byte[] quality, int length) {
    return rawToAsciiString(quality, 0, length);
  }

  /**
   * Converts an array of bytes into a sanger-encoded quality string
   *
   * @param quality buffer containing input qualities
   * @param offset Where in buffer to start conversion.
   * @param length the number of bytes from the input buffer to convert
   * @return the quality string
   */
  public static String rawToAsciiString(byte[] quality, int offset, int length) {
    final StringBuilder b = new StringBuilder();
    final int end = offset + length;
    for (int i = offset; i < end; ++i) {
      b.append(rawToAsciiQuality(quality[i]));
    }
    return b.toString();
  }

  /**
   * @param qualities the ASCII quality values
   * @return the raw quality values corresponding to the ASCII quality values
   */
  public static byte[] asciiToRawQuality(CharSequence qualities) {
    if (qualities == null) {
      return null;
    }
    return asciiToRawQuality(qualities, 0, qualities.length());
  }

  /**
   * @param qualities the ASCII quality values
   * @param offset Where in buffer to start conversion.
   * @param length the number of bytes from the input buffer to convert
   * @return the raw quality values corresponding to the ASCII quality values
   */
  public static byte[] asciiToRawQuality(CharSequence qualities, int offset, int length) {
    if (qualities == null) {
      return null;
    }
    final byte[] result = new byte[length];
    for (int j = offset, i = 0; i < result.length; ++i, ++j) {
      result[i] = asciiToRawQuality(qualities.charAt(j));
    }
    return result;
  }

  /**
   * @param qualities the ASCII quality values
   * @return the raw quality values corresponding to the ASCII quality values
   */
  public static byte[] asciiToRawQuality(char[] qualities) {
    if (qualities == null) {
      return null;
    }
    final byte[] result = new byte[qualities.length];
    for (int i = 0; i < qualities.length; ++i) {
      result[i] = asciiToRawQuality(qualities[i]);
    }
    return result;
  }

  /**
   * @param qualities the ASCII quality values
   * @return the raw quality values corresponding to the ASCII quality values
   */
  public static byte[] asciiToRawQuality(byte[] qualities) {
    if (qualities == null) {
      return null;
    }
    final byte[] result = new byte[qualities.length];
    for (int i = 0; i < qualities.length; ++i) {
      result[i] = asciiToRawQuality((char) qualities[i]);
    }
    return result;
  }

  /**
   * Takes a file and returns a FASTA base file, removing any gzip extension and storing a FASTQ extension if found
   * @param file the source file
   * @param gzip whether output is intended to be gzipped
   * @return the base file
   */
  public static BaseFile baseFile(File file, boolean gzip) {
    return FileUtils.getBaseFile(file, gzip, EXTS);
  }

}
