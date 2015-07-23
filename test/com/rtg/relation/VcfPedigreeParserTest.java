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

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.NanoRegression;
import com.rtg.vcf.header.PedigreeField;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfPedigreeParserTest extends TestCase {

  private NanoRegression mNano = null;

  @Override
  public void setUp() {
    mNano = new NanoRegression(this.getClass());
  }
  @Override
  public void tearDown() throws Exception {
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }

  public void testParsing() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File vcfFile = FileHelper.resourceToFile("com/rtg/relation/resources/vcfheader.vcf", new File(dir, "header.vcf"));
      final GenomeRelationships ped2 = VcfPedigreeParser.loadFile(vcfFile);
      mNano.check("pedfromvcf", PedFileParser.toString(ped2));
    }
  }

  public void testPedConversion() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File pedFile = FileHelper.resourceToFile("com/rtg/relation/resources/pop.ped", new File(dir, "pop.ped"));
      final GenomeRelationships ped = PedFileParser.loadFile(pedFile);
      final VcfHeader header = new VcfHeader();
      header.addLine(VcfHeader.VERSION_LINE);
      VcfPedigreeParser.addPedigreeFields(header, ped);
      for (String sample : ped.filterByGenomes(new GenomeRelationships.PrimaryGenomeFilter(ped)).genomes()) {
        header.addSampleName(sample);
      }
      mNano.check("vcffromped.vcf", header.toString());
    }
  }

  public void testFullCircle() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File vcfFile = FileHelper.resourceToFile("com/rtg/relation/resources/vcffromped.vcf", new File(dir, "pop.vcf"));
      final GenomeRelationships ped = VcfPedigreeParser.loadFile(vcfFile);
      final VcfHeader header = new VcfHeader();
      header.addLine(VcfHeader.VERSION_LINE);
      VcfPedigreeParser.addPedigreeFields(header, ped);
      for (String sample : ped.filterByGenomes(new GenomeRelationships.PrimaryGenomeFilter(ped)).genomes()) {
        header.addSampleName(sample);
      }
      mNano.check("vcffromped.vcf", header.toString());
    }
  }

  public void testFullCircleWithCellLine() throws IOException {
    try (final TestDirectory dir = new TestDirectory()) {
      final File vcfFile = FileHelper.resourceToFile("com/rtg/relation/resources/derived.vcf", new File(dir, "pop.vcf"));
      final GenomeRelationships ped = VcfPedigreeParser.loadFile(vcfFile);
      final VcfHeader header = new VcfHeader();
      header.addLine(VcfHeader.VERSION_LINE);
      VcfPedigreeParser.addPedigreeFields(header, ped);
      for (String sample : ped.filterByGenomes(new GenomeRelationships.PrimaryGenomeFilter(ped)).genomes()) {
        header.addSampleName(sample);
      }
      mNano.check("derived.vcf", header.toString());
    }
  }

  public void testNoInformationWarning() throws IOException {
    final GenomeRelationships ped = new GenomeRelationships();
    final PedigreeField f = new PedigreeField("##PEDIGREE=<Sibling=>");
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    VcfPedigreeParser.parsePedLine(ped, f);
    assertTrue(mps.toString().contains("Pedigree line contains no pedigree information: ##PEDIGREE=<Sibling=>"));
    Diagnostic.setLogStream();
  }

}
