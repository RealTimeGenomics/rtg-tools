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

package com.rtg.util.intervals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.VcfHeader;

public class JSReferenceRegionsTest {

  @Test
  public void testRegions() throws IOException {
    try (TestDirectory tdir = new TestDirectory("scripted-filters")) {
      File bedfile = new File(tdir, "regions.bed");
      FileUtils.stringToFile("blah\t100\t106", bedfile);

      File vcffile = new File(tdir, "regions.vcf");
      VcfRecord record = new VcfRecord("blah", 100, "ATATAT");
      final VcfHeader header = new VcfHeader();
      header.addCommonHeader();
      FileUtils.stringToFile(header.toString() + record.toString(), vcffile);

      JSReferenceRegions bedRegions = JSReferenceRegions.fromBed(bedfile.toString());
      JSReferenceRegions vcfRegions = JSReferenceRegions.fromVcf(vcffile.toString());

      // Note: direct VcfRecord construction is zero-based, but the JS overlap api is one-based
      VcfRecord[] positives = {
        new VcfRecord("blah", 100, "A"),
        new VcfRecord("blah", 105, "A"),
      };
      for (VcfRecord p : positives) {
        assertTrue(bedRegions.encloses(p.getSequenceName(), p.getOneBasedStart()));
        assertTrue(bedRegions.encloses(p.getSequenceName(), p.getOneBasedStart(), p.getOneBasedStart() + p.getLength()));
        assertTrue(bedRegions.overlaps(p.getSequenceName(), p.getOneBasedStart(), p.getOneBasedStart() + p.getLength()));
        assertTrue(vcfRegions.encloses(p.getSequenceName(), p.getOneBasedStart(), p.getOneBasedStart() + p.getLength()));
      }
      VcfRecord[] negatives = {
        new VcfRecord("blah", 1, "A"),
        new VcfRecord("blah", 99, "A"),
        new VcfRecord("blah", 106, "A"),
      };
      for (VcfRecord p : negatives) {
        assertFalse(bedRegions.encloses(p.getSequenceName(), p.getOneBasedStart(), p.getOneBasedStart() + p.getLength()));
        assertFalse(vcfRegions.encloses(p.getSequenceName(), p.getOneBasedStart(), p.getOneBasedStart() + p.getLength()));
      }
    }
  }

}
