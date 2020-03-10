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

import static com.rtg.util.StringUtils.LS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import com.rtg.bed.BedReader;
import com.rtg.bed.BedUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfReaderFactory;

import junit.framework.TestCase;

/**
 */
public class ReferenceRegionsTest extends TestCase {
  private static final String BED = (""
                                    + "monkey 25 30 bar" + LS
                                    + "monkey 20 26 foo" + LS
                                    + "wombat 40 60 foo" + LS
  ).replaceAll(" ", "\t");


  private static final String VCF = (""
                                     + "##fileformat=VCFv4.1" + LS
                                     + "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" + LS
                                     + "#CHROM POS ID REF ALT QUAL FILTER INFO FORMAT sample" + LS
                                     + "monkey 26 . AAAAA G . PASS . GT 1/1" + LS
                                     + "monkey 21 . AAAAAA G . PASS . GT 1/1" + LS
                                     + "wombat 41 . AAAAAAAAAAAAAAAAAAAA G . PASS . GT 1/1" + LS
  ).replaceAll(" ", "\t");

  public void testBedRegions() throws IOException {
    final BedReader reader = new BedReader(new BufferedReader(new StringReader(BED)));
    final ReferenceRegions regions = ReferenceRegions.regions(reader);
    checkRegions(regions);
  }
  public void testVcfRegions() throws IOException {
    final VcfReader reader = new VcfReaderFactory().make(new BufferedReader(new StringReader(VCF)));
    final ReferenceRegions regions = ReferenceRegions.regions(reader);
    checkRegions(regions);
  }

  private void checkRegions(ReferenceRegions regions) {
    assertFalse(regions.enclosed("monkey", 19));
    assertTrue(regions.enclosed("monkey", 20));
    assertTrue(regions.enclosed("monkey", 29));
    assertFalse(regions.enclosed("monkey", 30));
    assertFalse(regions.enclosed("monkey", 45));
    assertTrue(regions.enclosed("wombat", 45));

    // Test ranges of inclusion
    assertTrue(regions.enclosed("wombat", 40, 60));
    assertFalse(regions.enclosed("wombat", 39, 60));
    assertFalse(regions.enclosed("wombat", 40, 61));
    assertFalse(regions.enclosed("wombat", 39, 61));
    assertFalse(regions.enclosed("wombat", 10, 20));

    assertTrue(regions.overlapped("wombat", 35, 41));
    assertFalse(regions.overlapped("wombat", 35, 40));
    assertTrue(regions.overlapped("wombat", 59, 70));
    assertFalse(regions.overlapped("wombat", 60, 70));
    assertTrue(regions.overlapped("wombat", 30, 70));
    assertFalse(regions.overlapped("wombat", 10, 20));
  }

  public void testFromFile() throws IOException {
    assertNull(BedUtils.regions(null));
    try (TestDirectory tmp = new TestDirectory()) {
      final File f = new File(tmp, "bed");
      FileUtils.stringToFile(BED, f);
      final ReferenceRegions bed = BedUtils.regions(f);
      assertEquals(10, bed.coveredLength("monkey"));
      assertEquals(20, bed.coveredLength("wombat"));
      assertTrue(bed.overlapped("wombat", 35, 41));
    }

  }
  public void testEntirelyEnclosedRegion() throws IOException {
    final String bed = (""
        + "monkey 20 26 foo" + LS
        + "monkey 10 40 bar" + LS
    ).replaceAll(" ", "\t");
    final BedReader reader = new BedReader(new BufferedReader(new StringReader(bed)));
    final ReferenceRegions regions = ReferenceRegions.regions(reader);
    assertFalse(regions.enclosed("monkey", 9));
    for (int i = 10; i < 40; ++i) {
      assertTrue(regions.enclosed("monkey", i));
    }
    assertFalse(regions.enclosed("monkey", 40));
  }
  public void testOverlappedEndLater() throws IOException {
    final String bed = (""
        + "monkey 20 26 foo" + LS
        + "monkey 23 40 bar" + LS
    ).replaceAll(" ", "\t");
    final BedReader reader = new BedReader(new BufferedReader(new StringReader(bed)));
    final ReferenceRegions regions = ReferenceRegions.regions(reader);
    assertFalse(regions.enclosed("monkey", 19));
    for (int i = 20; i < 40; ++i) {
      assertTrue(regions.enclosed("monkey", i));
    }
    assertFalse(regions.enclosed("monkey", 40));
  }

  public void testNotOverlapped() throws IOException {
    final String bed = (""
        + "monkey 20 26 foo" + LS
        + "monkey 30 40 bar" + LS
    ).replaceAll(" ", "\t");
    final BedReader reader = new BedReader(new BufferedReader(new StringReader(bed)));
    final ReferenceRegions regions = ReferenceRegions.regions(reader);
    assertTrue(regions.enclosed("monkey", 25));
    assertFalse(regions.enclosed("monkey", 26));
    assertFalse(regions.enclosed("monkey", 29));
    assertTrue(regions.enclosed("monkey", 30));
    assertFalse(regions.enclosed("alligator", 10));
  }

  private static final String EXPECTED_INTERSECT_1 = ""
    + String.format("%s\t%d\t%d%n", "monkey", 20, 50)
    + String.format("%s\t%d\t%d%n", "monkey", 100, 200);

   private static final String EXPECTED_INTERSECT_2 = ""
    + String.format("%s\t%d\t%d%n", "monkey", 20, 25)
    + String.format("%s\t%d\t%d%n", "monkey", 30, 40)
     + String.format("%s\t%d\t%d%n", "monkey", 45, 50);

  public void testIntersect() throws IOException {
    final String bed = (""
      + "monkey 20 50 foo" + LS
      + "monkey 100 200 bar" + LS
    ).replaceAll(" ", "\t");

    final BedReader reader = new BedReader(new BufferedReader(new StringReader(bed)));
    final ReferenceRegions regions = ReferenceRegions.regions(reader);
    regions.intersect(regions);
    assertEquals(EXPECTED_INTERSECT_1, regions.toString());
    final ReferenceRegions other = new ReferenceRegions();
    other.add("monkey", 15, 25);
    other.add("monkey", 30, 40);
    other.add("monkey", 45, 55);
    regions.intersect(other);
    assertEquals(EXPECTED_INTERSECT_2, regions.toString());
  }

  public void testBin() {
    final ReferenceRegions regions = new ReferenceRegions();
    regions.add("monkey", 50, 700);
    regions.add("hobo", 0, 700);
    assertEquals(2, regions.size());
    assertTrue(regions.enclosed("monkey", 500, 600));
    assertFalse(regions.enclosed("monkey", 0, 50));
    assertFalse(regions.enclosed("monkey", 750, 800));
  }

  public void testEmpty() {
    final ReferenceRegions regions = new ReferenceRegions();
    regions.add("monkey", 60, 80);
    assertTrue(regions.overlapped("monkey", 50, 70));
  }

  public void testCoveredLength() {
    final ReferenceRegions regions = new ReferenceRegions();
    regions.add("monkey", 60, 80);
    regions.add("wombat", 63, 68);
    assertEquals(20, regions.coveredLength("monkey"));
    assertEquals(5, regions.coveredLength("wombat"));
    assertEquals(0, regions.coveredLength("hyena"));
    regions.add("monkey", 78, 88);
    assertEquals(28, regions.coveredLength("monkey"));
    final Map<String, Integer> covered = regions.coveredLengths();
    assertEquals(28, covered.get("monkey").intValue());
    assertEquals(5, covered.get("wombat").intValue());
    assertNull(covered.get("hyena"));
  }

}
