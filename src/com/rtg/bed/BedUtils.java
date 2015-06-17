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
package com.rtg.bed;

import java.io.File;
import java.io.IOException;

import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;

/**
 */
public final class BedUtils {

  /** BED file suffix */
  public static final String BED_SUFFIX = ".bed";

  private BedUtils() { }

  /**
   * @param f file
   * @return true if file has <code>bed</code> or gzipped <code>bed</code> extension
   */
  public static boolean isBedExtension(File f) {
    return f.getName().endsWith(BED_SUFFIX) || f.getName().endsWith(BED_SUFFIX + FileUtils.GZ_SUFFIX);
  }

  /**
   * Create a tabix index for a BED file
   * @param fileToIndex the BED file
   * @throws IOException if there is a problem
   */
  public static void createBedTabixIndex(File fileToIndex) throws IOException {
    try {
      new TabixIndexer(fileToIndex).saveBedIndex();
    } catch (final IllegalArgumentException e) {
      Diagnostic.warning("Cannot produce TABIX index for: " + fileToIndex + ": " + e.getMessage());
      throw e;
    } catch (final UnindexableDataException e) {
      Diagnostic.warning("Cannot produce TABIX index for: " + fileToIndex + ": " + e.getMessage());
    }
  }

  /**
   * Create a ReferenceRegions from the specified BED file
   * @param f a pointer to the bed file
   * @return a new <code>ReferenceRegions</code> or null if the argument is null
   * @throws java.io.IOException when reading the file fails
   */
  public static ReferenceRegions regions(File f) throws IOException {
    if (f != null) {
      try (BedReader reader = BedReader.openBedReader(null, f, 0)) {
        return regions(reader);
      }
    } else {
      return null;
    }
  }

  /**
   * Create a new instance from the specified BED file
   * @param reader the BED reader
   * @return a new <code>ReferenceRegions</code>
   * @throws java.io.IOException when reading the file fails
   */
  public static ReferenceRegions regions(BedReader reader) throws IOException {
    final ReferenceRegions regions = new ReferenceRegions();
    while (reader.hasNext()) {
      regions.add(reader.next());
    }
    return regions;
  }
}
