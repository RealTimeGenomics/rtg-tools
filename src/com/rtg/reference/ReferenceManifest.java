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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.rtg.util.Resources;

/**
 * General utility for reference manifest files
 */
public final class ReferenceManifest {

  private ReferenceManifest() { }

  private static final String RESOURCE_PATH = "com/rtg/reference/resources";

  private static final String MANIFEST_LIST = RESOURCE_PATH + "/manifest.list";

  private static final String NAME_PATH = RESOURCE_PATH + "/namelookup";

  private static final String NAME_NUMBERS_LIST = NAME_PATH + "/numbers.list";

  private static final String NAME_SEX_LIST = NAME_PATH + "/sex.list";

  /** Number of sequences an SDF can have before it is excluded from manifest checking */
  public static final int MAX_SEQUENCE_NAMES = 200000;

  /**
   * Types of checks that may be in a reference manifest
   */
  public enum CheckTypes {
    /** Checks the sequence name is present */
    NAME,
    /** Checks the sequence length matches */
    LENGTH,
    /** Checks a single byte checksum matches */
    CRC32
  }

  /**
   * Get the list of auto install reference manifest detectors
   * @return a list of reference detectors
   * @throws IOException if an IO error occurs
   */
  public static List<ReferenceDetector> getReferenceDetectors() throws IOException {
    return getReferenceDetectors(RESOURCE_PATH, MANIFEST_LIST);
  }

  /**
   * Get the list of chromosome name reference manifest detectors
   * @return a list of reference detectors
   * @throws IOException if an IO error occurs
   */
  public static List<ReferenceDetector> getChromosomeNumberDetectors() throws IOException {
    return getReferenceDetectors(NAME_PATH, NAME_NUMBERS_LIST);
  }

  /**
   * Get the list of chromosome sex reference manifest detectors
   * @return a list of reference detectors
   * @throws IOException if an IO error occurs
   */
  public static List<ReferenceDetector> getChromosomeSexDetectors() throws IOException {
    return getReferenceDetectors(NAME_PATH, NAME_SEX_LIST);
  }

  private static List<ReferenceDetector> getReferenceDetectors(String path, String list) throws IOException {
    final List<ReferenceDetector> ret = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(Resources.getResourceAsStream(list)))) {
      String line;
      while ((line = reader.readLine()) != null) {
        try (InputStream stream = Resources.getResourceAsStream(path + "/" + line)) {
          ret.add(ReferenceDetector.loadManifest(stream));
        }
      }
    }
    return ret;
  }

}
