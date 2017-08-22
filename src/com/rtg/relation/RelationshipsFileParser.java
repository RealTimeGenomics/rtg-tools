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

package com.rtg.relation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import com.rtg.relation.Relationship.RelationshipType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Reads RTG format relationships file.
 *
 */
public final class RelationshipsFileParser {

  private static final HashMap<String, RelationshipType> TYPES = new HashMap<>();
  static {
    TYPES.put("parent-child", RelationshipType.PARENT_CHILD);
    TYPES.put("original-derived", RelationshipType.ORIGINAL_DERIVED);
  }

  private RelationshipsFileParser() { }

  /**
   * Load a genome relationships file
   * @param file file to load
   * @return structure containing relationships and link to mapping files.
   * @throws IOException if an IO error occurs
   */
  static GenomeRelationships loadFile(File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      return RelationshipsFileParser.load(reader);
    }
  }

  /**
   * Load a genome relationships file
   * @param reader input source
   * @return structure containing relationships and link to mapping files.
   * @throws IOException if an IO error occurs
   */
  public static GenomeRelationships load(BufferedReader reader) throws IOException {
    final GenomeRelationships ped = new GenomeRelationships();

    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim(); // So we don't get thrown off by leading/trailing whitespace
      //ignore comments
      if (line.startsWith("#") || line.matches("^\\s*$")) {
        continue;
      }
      //genome lines
      if (line.matches("^genome\\s++[-a-zA-Z0-9_]++(\\s++[-a-zA-Z0-9_]++=[^\\s]*+)*+$")) {
        parseGenomeLine(ped, line);
        continue;
      }
      //relationship lines
      if (line.matches("^[a-z]++-[a-z]++\\s++[-a-zA-Z0-9_]++\\s++[-a-zA-Z0-9_]++(\\s++[-a-zA-Z0-9_]++=[^\\s]*+)*+$")) {
        parseRelationshipLine(ped, line);
        continue;
      }
      throw new NoTalkbackSlimException("unrecognized line in relationships: '" + line + "'");
    }

    return ped;
  }

  private static void parseGenomeLine(GenomeRelationships ped, String line) {
    final String[] spaceSplit = line.split("\\s+");
    if (spaceSplit.length < 2) {
      throw new NoTalkbackSlimException("genome definition: '" + line + "' should contain at least 2 columns but only contained: " + spaceSplit.length);
    }
    final String genome = spaceSplit[1];
    ped.addGenome(genome);
    final Properties p = ped.getProperties(genome);
    for (int i = 2; i < spaceSplit.length; ++i) {
      final String[] splitProp = spaceSplit[i].split("=", 2);
      p.setProperty(splitProp[0], splitProp[1]);
    }
    p.put(GenomeRelationships.PRIMARY_GENOME_PROPERTY, "true");
  }

  private static void parseRelationshipLine(GenomeRelationships ped, String line) {
    final String[] spaceSplit = line.split("\\s+");
    if (spaceSplit.length < 3) {
      throw new NoTalkbackSlimException("relationship definition: '" + line + "' should contain at least 3 columns but only contained: " + spaceSplit.length);
    }
    final RelationshipType t = TYPES.get(spaceSplit[0]);
    if (t == null) {
      throw new NoTalkbackSlimException("unrecognized relationship type: '" + spaceSplit[0] + "'");
    }
    final String genome1 = spaceSplit[1];
    ped.addGenome(genome1);
    final String genome2 = spaceSplit[2];
    ped.addGenome(genome2);

    final Relationship r = ped.addRelationship(t, genome1, genome2);
    for (int i = 3; i < spaceSplit.length; ++i) {
      final String[] splitProp = spaceSplit[i].split("=", 2);
      r.setProperty(splitProp[0], splitProp[1]);
    }
  }

}
