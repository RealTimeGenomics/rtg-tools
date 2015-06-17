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
package com.rtg.taxonomy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.rtg.util.StringUtils;

/**
 */
public final class SequenceToTaxonIds {

  private SequenceToTaxonIds() { }

  /**
   * Load a map from (short) sequence names to taxon ids supplied as tab separated input
   * @param tsv the input file
   * @return the map
   * @throws IOException if there are problems reading the input
   */
  public static Map<String, Integer> sequenceToIds(File tsv) throws IOException {
    return sequenceToIds(new FileReader(tsv));
  }

  /**
   * Load a map from (short) sequence names to taxon ids supplied as tab separated input
   * @param input input source
   * @return the map
   * @throws IOException if there are problems reading the input
   */
  public static Map<String, Integer> sequenceToIds(Reader input) throws IOException {
    final Map<String, Integer> ret = new HashMap<>();
    try (BufferedReader mergedReader = new BufferedReader(input)) {
      String line;
      while ((line = mergedReader.readLine()) != null) {
        if (line.startsWith("#") || line.length() == 0) {
          continue;
        }
        final String[] parts = StringUtils.split(line, '\t');
        if (parts.length != 2) {
          throw new IOException("Malformed line: " + line);
        }
        final String name;
        final int indexOfSpace = parts[1].indexOf(' ');
        if (indexOfSpace != -1) {
          name = parts[1].substring(0, indexOfSpace);
        } else {
          name = parts[1];
        }

        final int taxId;
        try {
          taxId = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
          throw new IOException("Malformed line: " + line);
        }
        if (ret.put(new String(name.toCharArray()), taxId) != null) {
          throw new IOException("Duplicate name detected: " + line) ;
        }
      }
    }
    return ret;
  }
}
