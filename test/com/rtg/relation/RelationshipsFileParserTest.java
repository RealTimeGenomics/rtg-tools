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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.rtg.relation.Relationship.RelationshipType;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class RelationshipsFileParserTest extends TestCase {

  private static final String RELATIONS_ODD = ""
    + "original-derived genomea genomeb randprop_mcprop=hoho=yoyo_foobar" + StringUtils.LS;

  public void testCases() throws IOException {
    final File dir = FileUtils.createTempDir("test", "relationshipfile");
    try {
      final File relationFile = new File(dir, "relationshipfile.txt");
      FileUtils.stringToFile(RELATIONS_ODD, relationFile);
      final GenomeRelationships gnf = RelationshipsFileParser.loadFile(relationFile);
      final String[] res = gnf.genomes();
      Arrays.sort(res);
      final Relationship[] expected = {new Relationship("genomea", "genomeb", RelationshipType.ORIGINAL_DERIVED)};
      expected[0].setProperty("randprop_mcprop", "hoho=yoyo_foobar");
      assertTrue(Arrays.toString(gnf.genomes()), Arrays.equals(new String[] {"genomea", "genomeb"}, res));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("genomea")), Arrays.equals(expected, gnf.relationships("genomea")));
      assertTrue("Actual: " + Arrays.toString(gnf.relationships("genomeb")), Arrays.equals(expected, gnf.relationships("genomeb")));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
  private static final String RELATIONS_BAD_3 = "" + "original-de genomea genomeb" + StringUtils.LS;
  private static final String RELATIONS_BAD_5 = "" + "original-derived genomeb" + StringUtils.LS;

  public void testBad() throws IOException {
    Diagnostic.setLogStream();
    final File dir = FileUtils.createTempDir("test", "relationshipfile");
    try {
      final File relationFile = new File(dir, "relationshipfile.txt");
      FileUtils.stringToFile(RELATIONS_BAD_3, relationFile);
      try {
        RelationshipsFileParser.loadFile(relationFile);
        fail("should throw exception");
      } catch (final NoTalkbackSlimException e) {
        assertEquals("unrecognized relationship type: 'original-de'", e.getMessage());
      }
      FileUtils.stringToFile(RELATIONS_BAD_5, relationFile);
      try {
        RelationshipsFileParser.loadFile(relationFile);
        fail("should throw exception");
      } catch (final NoTalkbackSlimException e) {
        assertEquals("unrecognized line in relationships: 'original-derived genomeb'", e.getMessage());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }


}
