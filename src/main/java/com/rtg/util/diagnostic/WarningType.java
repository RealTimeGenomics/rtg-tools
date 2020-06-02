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
package com.rtg.util.diagnostic;

import java.io.Serializable;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 * Enumeration of SLIM warnings.
 * See <code>src.com.reeltwo.cartesian.util.diagnostic.Diagnostics.properties</code>
 * for the localised messages.
 */
public final class WarningType implements DiagnosticType, PseudoEnum, Serializable {

  private static int sOrdinal = -1;

  /**
   * Warning for an unexpected nucleotide or amino acid in a sequence. Parameters
   * are sequence name and the unexpected residue.
   */
  public static final WarningType BAD_TIDE = new WarningType(++sOrdinal, "BAD_TIDE", 3);

  /**
   * Number of bad tides
   */
  public static final WarningType NUMBER_OF_BAD_TIDE = new WarningType(++sOrdinal, "NUMBER_OF_BAD_TIDE", 1);

  /**
   * Warning for a sequence with no name.  Parameter is the name assigned to the
   * sequence.
   */
  public static final WarningType NO_NAME = new WarningType(++sOrdinal, "NO_NAME", 1);



  /**
   * Warning used for a sequence with too many residues. Parameter is the name of the
   * sequence.
   */
  public static final WarningType SEQUENCE_TOO_LONG = new WarningType(++sOrdinal, "SEQUENCE_TOO_LONG", 1);

  /**
   * A file or directory could not be converted to a canonical path (this may be
   * because the file does not exist).  The parameter is the supplied path.
   */
  public static final WarningType BAD_PATH = new WarningType(++sOrdinal, "BAD_PATH", 1);

  /**
   * A sequence label was present but not sequence data followed.
   */
  public static final WarningType NO_SEQUENCE = new WarningType(++sOrdinal, "NO_SEQUENCE", 1);

  /**
   * A sequence label was too large to write fully because of file size limits,
   * or label size limits.
   * The parameter is the truncated label.
   * (Unlikely to ever happen in real world).
   */
  public static final WarningType SEQUENCE_LABEL_TOO_LONG = new WarningType(++sOrdinal, "SEQUENCE_LABEL_TOO_LONG", 1);

  /**
   * A sequence name and quality name in FASTQ file differs.
   */
  public static final WarningType SEQUENCE_LABEL_MISMATCH = new WarningType(++sOrdinal, "SEQUENCE_LABEL_MISMATCH", 2);

  /**
   * A read length does not agree with the length expected and will be ignored (in Ngs).
   */
  public static final WarningType INCORRECT_LENGTH = new WarningType(++sOrdinal, "INCORRECT_LENGTH", 3);

  /**
   * %1 out of %2 Symbols corresponded to A, C, G, T, or N.
   */
  public static final WarningType POSSIBLY_NOT_PROTEIN = new WarningType(++sOrdinal, "POSSIBLY_NOT_PROTEIN", 2);

  /**
   * Number of reads of wrong length.
   */
  public static final WarningType NUMBER_OF_INCORRECT_LENGTH = new WarningType(++sOrdinal, "NUMBER_OF_INCORRECT_LENGTH", 1);

  /**
   * The supplied file \"%1\" is not a FASTA file or has no sequences.
   */
  public static final WarningType NOT_FASTA_FILE = new WarningType(++sOrdinal, "NOT_FASTA_FILE", 1);

  /**
   * A quality outside the likely range was found, if the input is Solexa/Illumina it should be specified
   */
  public static final WarningType POSSIBLY_SOLEXA = new WarningType(++sOrdinal, "POSSIBLY_SOLEXA", 0);

  /** Total bad character warnings. */
  public static final WarningType BAD_CHAR_WARNINGS = new WarningType(++sOrdinal, "BAD_CHAR_WARNINGS", 1);

  /** Warning that sam records were ignored because of corrupted record content. */
  public static final WarningType SAM_IGNORED_RECORDS = new WarningType(++sOrdinal, "SAM_IGNORED_RECORDS", 2);

  /** Warning that two sam files cannot be merged because their headers are different. */
  public static final WarningType SAM_INCOMPATIBLE_HEADERS = new WarningType(++sOrdinal, "SAM_INCOMPATIBLE_HEADERS", 2);

  /** SAM record has invalid format. */
  public static final WarningType SAM_BAD_FORMAT_WARNING1 = new WarningType(++sOrdinal, "SAM_BAD_FORMAT_WARNING1", 1);

  /** SAM record has invalid format. */
  public static final WarningType SAM_BAD_FORMAT_WARNING = new WarningType(++sOrdinal, "SAM_BAD_FORMAT_WARNING", 2);

  /** %1 */
  public static final WarningType INFO_WARNING = new WarningType(++sOrdinal, "INFO_WARNING", 1);

  /**
   * The supplied file \"%1\" is not a FASTQ file or has no sequences.
   */
  public static final WarningType NOT_FASTQ_FILE = new WarningType(++sOrdinal, "NOT_FASTQ_FILE", 1);


  private static final EnumHelper<WarningType> HELPER = new EnumHelper<>(WarningType.class, new WarningType[] {
    BAD_TIDE,
    NUMBER_OF_BAD_TIDE,
    NO_NAME,
    SEQUENCE_TOO_LONG,
    BAD_PATH,
    NO_SEQUENCE,
    SEQUENCE_LABEL_TOO_LONG,
    SEQUENCE_LABEL_MISMATCH,
    INCORRECT_LENGTH,
    POSSIBLY_NOT_PROTEIN,
    NUMBER_OF_INCORRECT_LENGTH,
    NOT_FASTA_FILE,
    POSSIBLY_SOLEXA,
    BAD_CHAR_WARNINGS,
    SAM_IGNORED_RECORDS,
    SAM_INCOMPATIBLE_HEADERS,
    SAM_BAD_FORMAT_WARNING1,
    SAM_BAD_FORMAT_WARNING,
    INFO_WARNING,
    NOT_FASTQ_FILE
  });

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str name of value
   * @return the enum value
   */
  public static WarningType valueOf(final String str) {
    return HELPER.valueOf(str);
  }

  /**
   * @return list of enum values
   */
  public static WarningType[] values() {
    return HELPER.values();
  }

  /** Number of parameters that must occur in conjunction with this warning. */
  private final int mParams;

  private final int mOrdinal;

  private final String mName;

  private WarningType(final int ordinal, final String name, final int params) {
    mParams = params;
    mOrdinal = ordinal;
    mName = name;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  @Override
  public String name() {
    return mName;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public int getNumberOfParameters() {
    return mParams;
  }

  Object readResolve() {
    return values()[this.ordinal()];
  }

  @Override
  public String getMessagePrefix() {
    return "";
  }
}

