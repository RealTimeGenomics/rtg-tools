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
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import junit.framework.TestCase;

/**
 */
public class SequenceToTaxonIdsTest extends TestCase {

  /**
   * Test method for {@link com.rtg.taxonomy.SequenceToTaxonIds#sequenceToIds(java.io.File)}.
   * @throws IOException
   */
  public void testSequenceToIds() throws IOException {
    final String example = "1000\tmysequence1\n5454\tmysequence2 has extra stuff going on\n";
    final SequenceToTaxonIds r = new SequenceToTaxonIds();
    final Map<String, Integer> result = r.parse(new BufferedReader(new StringReader(example)));
    assertTrue(result.size() == 2);
    assertTrue(result.containsKey("mysequence1"));
    assertEquals(1000, (int) result.get("mysequence1"));
    assertEquals(5454, (int) result.get("mysequence2"));
  }

  public void testStatics() {
    assertEquals("taxonomy_lookup.tsv", TaxonomyUtils.TAXONOMY_TO_SEQUENCE_FILE);
  }
}
