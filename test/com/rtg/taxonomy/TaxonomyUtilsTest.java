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
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import com.rtg.reader.AnnotatedSequencesReader;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.Sdf2FastaTest;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.MultiMap;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class TaxonomyUtilsTest extends TestCase {

  public void testStatics() throws IOException {
    try (final TestDirectory dir = new TestDirectory("sdfstats")) {
      assertFalse(TaxonomyUtils.hasTaxonomyInfo(SequencesReaderFactory.createDefaultSequencesReader(ReaderTestUtils.getDNADir(dir))));

      final File fullSdf = new File(dir, "sdf_full");
      Sdf2FastaTest.makeTestTaxonSdf(fullSdf);
      try (final AnnotatedSequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(fullSdf)) {
        assertTrue(TaxonomyUtils.hasTaxonomyInfo(reader));

        Taxonomy tax = TaxonomyUtils.loadTaxonomy(reader);
        assertEquals(20, tax.size());

        Map<String, Integer> nameLookup = TaxonomyUtils.loadTaxonomyMapping(reader);
        assertEquals(44, nameLookup.size());
        assertEquals(12, new HashSet<>(nameLookup.values()).size());

        MultiMap<Integer, Long> idLookup = TaxonomyUtils.loadTaxonomyIdMapping(reader);
        assertEquals(12, idLookup.size());
        assertEquals(44, idLookup.allValues().size());
      }
    }
  }
}
