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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import com.rtg.reader.AbstractSdfWriter;
import com.rtg.reader.ReaderUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.MultiMap;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Utilities to help with loading taxonomy information from an SDF
 */
public final class TaxonomyUtils {

  /** Default name for taxonomy file. */
  public static final String TAXONOMY_FILE = "taxonomy.tsv";

  /** Default name of taxonomy to sequence name lookup file. */
  public static final String TAXONOMY_TO_SEQUENCE_FILE = "taxonomy_lookup.tsv";

  private TaxonomyUtils() { }

  /**
   * Returns true if the supplied reader contains taxonomy information
   * @param reader the input SDF
   * @return true if taxonomy information is present
   * @throws NoTalkbackSlimException if only one of the required taxonomy files is present
   */
  public static boolean hasTaxonomyInfo(SequencesReader reader) {
    final File taxonFile = new File(reader.path(), TAXONOMY_FILE);
    final File mappingFile = new File(reader.path(), TAXONOMY_TO_SEQUENCE_FILE);
    if (taxonFile.exists() && mappingFile.exists()) {
      return true;
    } else if (taxonFile.exists() || mappingFile.exists()) {
      throw new NoTalkbackSlimException("Reference SDF does not contain both taxonomy and sequences lookup");
    } else {
      return false;
    }
  }

  /**
   * Load a taxonomy from the supplied reader.
   * @param reader the input SDF
   * @return the taxonomy
   * @throws IOException if there are problems reading the input
   */
  public static Taxonomy loadTaxonomy(SequencesReader reader) throws IOException {
    final Taxonomy tax = new Taxonomy();
    try (FileInputStream fis = new FileInputStream(new File(reader.path(), TaxonomyUtils.TAXONOMY_FILE))) {
      tax.read(fis);
    }
    return tax;
  }

  /**
   * Load a map from (short) sequence names to taxon ids from the supplied reader
   * @param reader the input SDF
   * @return the map
   * @throws IOException if there are problems reading the input
   */
  public static Map<String, Integer> loadTaxonomyMapping(SequencesReader reader) throws IOException {
    return SequenceToTaxonIds.sequenceToIds(new File(reader.path(), TAXONOMY_TO_SEQUENCE_FILE));
  }

  /**
   * Load a map from taxon id to sequence ids from the supplied reader. Note that this implementation is
   * not efficient, in that it has to load reader names and invert the regular name to taxon id map.
   * @param reader the input SDF
   * @return the map
   * @throws IOException if there are problems reading the input
   */
  public static MultiMap<Integer, Long> loadTaxonomyIdMapping(SequencesReader reader) throws IOException {
    final AbstractSdfWriter.SequenceNameHandler handler = new AbstractSdfWriter.SequenceNameHandler();
    final Map<String, Long> names = ReaderUtils.getSequenceNameMap(reader);
    final Map<String, Integer> sequenceLookupMap = loadTaxonomyMapping(reader);
    // Invert the map and convert target to IDs
    final MultiMap<Integer, Long> result = new MultiMap<>();
    for (Map.Entry<String, Integer> entry : sequenceLookupMap.entrySet()) {
      final Long id = names.get(handler.handleSequenceName(entry.getKey()).label());
      if (id != null) {
        result.put(entry.getValue(), id);
      }
    }
    return result;
  }
}
