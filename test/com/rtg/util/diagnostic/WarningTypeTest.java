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

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class WarningTypeTest extends TestCase {

  public void testEnum() {
    TestUtils.testPseudoEnum(WarningType.class, ""
        + "[BAD_TIDE, NUMBER_OF_BAD_TIDE, NO_NAME, SEQUENCE_TOO_LONG, "
        + "BAD_PATH, NO_SEQUENCE, SEQUENCE_LABEL_TOO_LONG, SEQUENCE_LABEL_MISMATCH, "
        + "INCORRECT_LENGTH, "
        + "POSSIBLY_NOT_PROTEIN, NUMBER_OF_INCORRECT_LENGTH, NOT_FASTA_FILE, "
        + "POSSIBLY_SOLEXA, BAD_CHAR_WARNINGS, "
        + "SAM_IGNORED_RECORDS, SAM_INCOMPATIBLE_HEADERS, SAM_BAD_FORMAT_WARNING1, SAM_BAD_FORMAT_WARNING, "
        + "INFO_WARNING, NOT_FASTQ_FILE]"
    );
    assertEquals(3, WarningType.BAD_TIDE.getNumberOfParameters());
    assertEquals(1, WarningType.NUMBER_OF_BAD_TIDE.getNumberOfParameters());
    assertEquals(1, WarningType.NO_NAME.getNumberOfParameters());
    assertEquals(1, WarningType.SEQUENCE_TOO_LONG.getNumberOfParameters());
    assertEquals(1, WarningType.BAD_PATH.getNumberOfParameters());
    assertEquals(1, WarningType.NO_SEQUENCE.getNumberOfParameters());
    assertEquals(1, WarningType.SEQUENCE_LABEL_TOO_LONG.getNumberOfParameters());
    assertEquals(2, WarningType.SEQUENCE_LABEL_MISMATCH.getNumberOfParameters());
    assertEquals(3, WarningType.INCORRECT_LENGTH.getNumberOfParameters());
    assertEquals(2, WarningType.POSSIBLY_NOT_PROTEIN.getNumberOfParameters());
    assertEquals(1, WarningType.NUMBER_OF_INCORRECT_LENGTH.getNumberOfParameters());
    assertEquals(1, WarningType.NOT_FASTA_FILE.getNumberOfParameters());
    assertEquals(0, WarningType.POSSIBLY_SOLEXA.getNumberOfParameters());
    assertEquals(1, WarningType.BAD_CHAR_WARNINGS.getNumberOfParameters());
    assertEquals(2, WarningType.SAM_IGNORED_RECORDS.getNumberOfParameters());
    assertEquals(2, WarningType.SAM_INCOMPATIBLE_HEADERS.getNumberOfParameters());
    assertEquals(1, WarningType.SAM_BAD_FORMAT_WARNING1.getNumberOfParameters());
    assertEquals(2, WarningType.SAM_BAD_FORMAT_WARNING.getNumberOfParameters());
    assertEquals(1, WarningType.INFO_WARNING.getNumberOfParameters());
    assertEquals(1, WarningType.NOT_FASTQ_FILE.getNumberOfParameters());
  }

  public void testReadResolve() {
    for (WarningType t : WarningType.values()) {
      assertEquals(t, t.readResolve());
    }
  }

  public void testPrefix() {
    assertEquals("", WarningType.INFO_WARNING.getMessagePrefix());
  }
}

