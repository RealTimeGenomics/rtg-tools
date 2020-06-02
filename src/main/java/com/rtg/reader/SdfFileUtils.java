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

/**
 * Manages files used for a binary sequences directory
 */
public final class SdfFileUtils {

  private SdfFileUtils() {
  }

  /**  main index file name  */
  public static final String INDEX_FILENAME = "mainIndex";
  static final String SEQUENCE_DATA_FILENAME = "seqdata";
  static final String SEQUENCE_QUALITY_DATA_FILENAME = "qualitydata";
  static final String LABEL_DATA_FILENAME = "namedata";
  static final String LABEL_SUFFIX_DATA_FILENAME = "suffixdata";
  static final String SEQUENCE_POINTER_FILENAME = "seqpointer";
  static final String LABEL_POINTER_FILENAME = "namepointer";
  static final String LABEL_SUFFIX_POINTER_FILENAME = "suffixpointer";

  //We currently don't roll index files
  static final String SEQUENCE_INDEX_FILENAME = "sequenceIndex0";
  static final String LABEL_INDEX_FILENAME = "nameIndex0";
  static final String LABEL_SUFFIX_INDEX_FILENAME = "suffixIndex0";

  static File sequenceDataFile(final File dir, final int fileNo) {
    return new File(dir, SEQUENCE_DATA_FILENAME + fileNo);
  }

  static File qualityDataFile(final File dir, final int fileNo) {
    return new File(dir, SEQUENCE_QUALITY_DATA_FILENAME + fileNo);
  }

  static File sequencePointerFile(final File dir, final int fileNo) {
    return new File(dir, SEQUENCE_POINTER_FILENAME + fileNo);
  }

  /**
   * Returns the sequence index file for the given directory, these don't roll.
   * @param dir Directory containing binary sequences
   * @return file for index
   */
  static File sequenceIndexFile(final File dir) {
    return new File(dir, SEQUENCE_INDEX_FILENAME);
  }

  static File labelDataFile(final File dir, final int fileNo) {
    return new File(dir, LABEL_DATA_FILENAME + fileNo);
  }
  static File labelPointerFile(final File dir, final int fileNo) {
    return new File(dir, LABEL_POINTER_FILENAME + fileNo);
  }
  static File labelIndexFile(final File dir) {
    return new File(dir, LABEL_INDEX_FILENAME);
  }

  static File labelSuffixDataFile(final File dir, final int fileNo) {
    return new File(dir, LABEL_SUFFIX_DATA_FILENAME + fileNo);
  }
  static File labelSuffixPointerFile(final File dir, final int fileNo) {
    return new File(dir, LABEL_SUFFIX_POINTER_FILENAME + fileNo);
  }
  static File labelSuffixIndexFile(final File dir) {
    return new File(dir, LABEL_SUFFIX_INDEX_FILENAME);
  }
}
