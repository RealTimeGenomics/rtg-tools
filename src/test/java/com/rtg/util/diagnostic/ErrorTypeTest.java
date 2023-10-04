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
public class ErrorTypeTest extends TestCase {

  public void testEnum() {
    for (final ErrorType et : ErrorType.values()) {
      check(et);
    }
    TestUtils.testPseudoEnum(ErrorType.class, "[SLIM_ERROR, INFO_ERROR,"
        + " DIRECTORY_EXISTS, DIRECTORY_NOT_EXISTS, DIRECTORY_NOT_CREATED,"
        + " NOT_A_DIRECTORY, INVALID_MAX_FREQUENCY, EXPECTED_POSITIVE,"
        + " EXPECTED_NONNEGATIVE, DISK_SPACE, SEQUENCE_TOO_LONG, INVALID_LICENSE, FILE_EXISTS,"
        + " FILE_NOT_CREATED, READING_ERROR, WRITING_ERROR,"
        + " NO_VALID_INPUTS, SEQUENCE_LENGTH_ERROR,"
        + " INVALID_MASK, WORD_SIZE_TOO_LARGE, INVALID_WORD_SIZE, INVALID_STEP_SIZE,"
        + " SDF_INDEX_NOT_VALID, SDF_FILETYPE_ERROR,"
        + " STEP_SIZE_GREATER_THAN_WORD,"
        + " SDF_VERIFICATION_FAILED, SDF_VERSION_INVALID, SDF_NOT_FOUND, NOT_SDF,"
        + " INVALID_MAX_INTEGER_FLAG_VALUE,"
        + " INVALID_MIN_INTEGER_FLAG_VALUE, WORD_NOT_LESS_READ,"
        + " NOT_ENOUGH_MEMORY, BAD_FASTQ_QUALITY, BAD_FASTA_LABEL,"
        + " BAD_CHARS_NAME,"
        + " NO_QUALITY_LABEL, NOT_ENOUGH_QUALITY, PRIORS_NOT_FOUND,"
        + " PROPS_KEY_NOT_FOUND, PRIOR_KEY_VALUE_INVALID, INVALID_PARAMETER, PROPS_INVALID, PROPS_LOAD_FAILED, FASTQ, FASTA, INVALID_INTEGER,"
        + " INVALID_LONG_READ_PARAMS, FILE_NOT_FOUND, CG_LENGTH_ERROR,"
        + " FILE_READ_ERROR, SAM_NOT_SORTED, SAM_BAD_FORMAT, SAM_BAD_FORMAT_NO_FILE, NOT_A_FILE, NOT_A_CG_SDF,"
        + " IS_A_CG_SDF, SAM_INCOMPATIBLE_HEADER_ERROR,"
        + " FILES_NOT_FOUND, IO_ERROR, INVALID_QUALITY_LENGTH,"
        + " NOT_A_CG_INPUT, NOT_A_PAIRED_END_SDF, INVALID_QUALITY, INVALID_MASK_PARAMS,"
        + " BLACKLIST_LONG_READ_ONLY]");
    assertEquals(0, ErrorType.SLIM_ERROR.getNumberOfParameters());
    assertEquals(1, ErrorType.INFO_ERROR.getNumberOfParameters());
    assertEquals(1, ErrorType.DIRECTORY_EXISTS.getNumberOfParameters());
    assertEquals(1, ErrorType.DIRECTORY_NOT_CREATED.getNumberOfParameters());
    assertEquals(1, ErrorType.DIRECTORY_NOT_EXISTS.getNumberOfParameters());
    assertEquals(1, ErrorType.INVALID_MAX_FREQUENCY.getNumberOfParameters());
    assertEquals(1, ErrorType.SEQUENCE_TOO_LONG.getNumberOfParameters());
    assertEquals(0, ErrorType.INVALID_LICENSE.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_A_DIRECTORY.getNumberOfParameters());
    assertEquals(1, ErrorType.EXPECTED_POSITIVE.getNumberOfParameters());
    assertEquals(1, ErrorType.EXPECTED_NONNEGATIVE.getNumberOfParameters());
    assertEquals(1, ErrorType.DISK_SPACE.getNumberOfParameters());
    assertEquals(1, ErrorType.FILE_EXISTS.getNumberOfParameters());
    assertEquals(1, ErrorType.FILE_NOT_CREATED.getNumberOfParameters());
    assertEquals(1, ErrorType.READING_ERROR.getNumberOfParameters());
    assertEquals(1, ErrorType.WRITING_ERROR.getNumberOfParameters());
    assertEquals(0, ErrorType.SEQUENCE_LENGTH_ERROR.getNumberOfParameters());
    assertEquals(0, ErrorType.NO_VALID_INPUTS.getNumberOfParameters());
    assertEquals(1, ErrorType.INVALID_MASK.getNumberOfParameters());
    assertEquals(2, ErrorType.WORD_SIZE_TOO_LARGE.getNumberOfParameters());
    assertEquals(3, ErrorType.INVALID_WORD_SIZE.getNumberOfParameters());
    assertEquals(1, ErrorType.INVALID_STEP_SIZE.getNumberOfParameters());
    assertEquals(1, ErrorType.SDF_INDEX_NOT_VALID.getNumberOfParameters());
    assertEquals(2, ErrorType.SDF_FILETYPE_ERROR.getNumberOfParameters());
    assertEquals(2, ErrorType.STEP_SIZE_GREATER_THAN_WORD.getNumberOfParameters());
    assertEquals(0, ErrorType.SDF_VERIFICATION_FAILED.getNumberOfParameters());
    assertEquals(1, ErrorType.SDF_VERSION_INVALID.getNumberOfParameters());
    assertEquals(1, ErrorType.SDF_NOT_FOUND.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_SDF.getNumberOfParameters());
    assertEquals(3, ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE.getNumberOfParameters());
    assertEquals(3, ErrorType.INVALID_MIN_INTEGER_FLAG_VALUE.getNumberOfParameters());
    assertEquals(2, ErrorType.WORD_NOT_LESS_READ.getNumberOfParameters());
    assertEquals(0, ErrorType.NOT_ENOUGH_MEMORY.getNumberOfParameters());
    assertEquals(1, ErrorType.BAD_FASTQ_QUALITY.getNumberOfParameters());
    assertEquals(1, ErrorType.BAD_FASTA_LABEL.getNumberOfParameters());
    assertEquals(1, ErrorType.NO_QUALITY_LABEL.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_ENOUGH_QUALITY.getNumberOfParameters());
    assertEquals(1, ErrorType.PRIORS_NOT_FOUND.getNumberOfParameters());
    assertEquals(2, ErrorType.PROPS_KEY_NOT_FOUND.getNumberOfParameters());
    assertEquals(3, ErrorType.PRIOR_KEY_VALUE_INVALID.getNumberOfParameters());
    assertEquals(1, ErrorType.INVALID_PARAMETER.getNumberOfParameters());
    assertEquals(2, ErrorType.PROPS_INVALID.getNumberOfParameters());
    assertEquals(0, ErrorType.FASTQ.getNumberOfParameters());
    assertEquals(0, ErrorType.FASTA.getNumberOfParameters());
    assertEquals(4, ErrorType.INVALID_INTEGER.getNumberOfParameters());
    assertEquals(2, ErrorType.INVALID_LONG_READ_PARAMS.getNumberOfParameters());
    assertEquals(1, ErrorType.FILE_NOT_FOUND.getNumberOfParameters());
    assertEquals(1, ErrorType.CG_LENGTH_ERROR.getNumberOfParameters());
    assertEquals(1, ErrorType.FILE_READ_ERROR.getNumberOfParameters());
    assertEquals(0, ErrorType.SAM_NOT_SORTED.getNumberOfParameters());
    assertEquals(2, ErrorType.SAM_BAD_FORMAT.getNumberOfParameters());
    assertEquals(1, ErrorType.SAM_BAD_FORMAT_NO_FILE.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_A_FILE.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_A_CG_SDF.getNumberOfParameters());
    assertEquals(1, ErrorType.IS_A_CG_SDF.getNumberOfParameters());
    assertEquals(1, ErrorType.SAM_INCOMPATIBLE_HEADER_ERROR.getNumberOfParameters());
    assertEquals(1, ErrorType.FILES_NOT_FOUND.getNumberOfParameters());
    assertEquals(1, ErrorType.IO_ERROR.getNumberOfParameters());
    assertEquals(1, ErrorType.INVALID_QUALITY_LENGTH.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_A_CG_INPUT.getNumberOfParameters());
    assertEquals(1, ErrorType.NOT_A_PAIRED_END_SDF.getNumberOfParameters());
    assertEquals(0, ErrorType.INVALID_QUALITY.getNumberOfParameters());
    assertEquals(0, ErrorType.INVALID_MASK_PARAMS.getNumberOfParameters());
  }

  public void testReadResolve() {
    for (ErrorType et : ErrorType.values()) {
      assertEquals(et, et.readResolve());
    }
  }

  public void testPrefix() {
    assertEquals("Error: ", ErrorType.INFO_ERROR.getMessagePrefix());
  }

  public void check(final ErrorType et) {
    final int nArgs = et.getNumberOfParameters();
    final String[] args = new String[nArgs];
    for (int i = 0; i < nArgs; ++i) {
      args[i] = "xxxx" + i + "yyyy";
    }
    final ErrorEvent event = new ErrorEvent(et, args);
    final String msg = event.getMessage();
    for (int i = 0; i < nArgs; ++i) {
      assertTrue(msg.contains(args[i]));
    }
  }
}

