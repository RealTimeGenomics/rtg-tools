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
import java.io.FileNotFoundException;
import java.io.IOException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Utility class for SDF stuff.
 */
public final class SdfUtils {

  private SdfUtils() { }

  static final int MAX_NO_DUP_SEQUENCE_COUNT = 100000;

  /**
   * Validate that an SDF directory has names.
   * @param sdf the SDF directory
   */
  public static void validateHasNames(final File sdf) {
    try {
      final boolean hasNames;
      if (ReaderUtils.isPairedEndDirectory(sdf)) {
        hasNames = hasNames(ReaderUtils.getLeftEnd(sdf)) || hasNames(ReaderUtils.getRightEnd(sdf));
      } else {
        hasNames = hasNames(sdf);
      }
      if (!hasNames) {
        throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "SDF: " + sdf + " has no name data");
      }
    } catch (final FileNotFoundException e) {
      throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Unable to find file: " + e.getMessage() + " part of SDF: " + sdf);
    } catch (final IOException e) {
      throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Unable to read SDF: " + sdf + " (" + e.getMessage() + ")");
    }
  }

  private static boolean hasNames(File sdf) throws IOException {
    final IndexFile id = new IndexFile(sdf);
    return id.hasNames();
  }


  /**
   * Validate that a given SDF input has no duplicate sequence names
   * @param reader the reader for the SDF
   * @param noMax set to true to disregard maximum sequence count limit
   */
  public static void validateNoDuplicates(final SequencesReader reader, boolean noMax) {
    if (!reader.hasNames()) {
      throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "SDF: " + reader.path() + " has no name data");
    }
    if (!noMax && reader.numberSequences() > MAX_NO_DUP_SEQUENCE_COUNT) {
      Diagnostic.warning("Too many sequences to check for duplicate sequence names.");
      return;
    }
    try {
      if (NameDuplicateDetector.checkSequence(reader, null)) {
        throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Duplicate sequence names detected in SDF: " + reader.path());
      }
    } catch (final IOException e) {
      throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Unable to read SDF: " + reader.path() + " (" + e.getMessage() + ")");
    }
  }
}
