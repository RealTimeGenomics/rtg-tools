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
package com.rtg.vcf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.launcher.MainResult;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class VariantStatisticsTest extends AbstractNanoTest {

  public void testGetRatio() {
    assertEquals("- (1/0)", VariantStatistics.divide(1L, 0L));
    assertEquals("0.00 (0/1)", VariantStatistics.divide(0L, 1L));
    assertEquals("0.33 (1/3)", VariantStatistics.divide(1L, 3L));
  }

  private static final String[] BASE_OUTPUT = {
    "Passed Filters               : 0" + StringUtils.LS
    , "Failed Filters               : 0" + StringUtils.LS
  };

  public void testGetStatistics() {
    final VariantStatistics stats = new VariantStatistics(null);
    TestUtils.containsAll(stats.getStatistics(), BASE_OUTPUT);
  }

  public void testPrintStatistics() throws IOException {
    try (final MemoryPrintStream ps = new MemoryPrintStream()) {
      final VariantStatistics stats = new VariantStatistics(null);
      stats.printStatistics(ps.outputStream());
      TestUtils.containsAll(ps.toString(), BASE_OUTPUT);
    }
  }

  private static final String[] VARIANTS = {
    "seq\t0\t.\tA\tC" + "\t.\tPASS\t.\tGT\t0/0"
    , "seq\t0\t.\tA\tC" + "\t.\ta10.0\t.\tGT\t0/0"
    , "seq\t0\t.\tA\tC" + "\t.\tOC\t.\tGT\t0/0"
    , "seq\t0\t.\tA\t." + "\t.\tRC\t.\tGT\t0/0"
    , "seq\t0\t.\tA\t." + "\t.\tRC\t.\tGT\t0/0"
    , "seq\t0\t.\tACGT\t." + "\t.\tOTHER\t.\tGT\t0/0"
    , "seq\t0\t.\tG\tGA" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tG\tGA,GT" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tA\tC" + "\t.\tPASS\t.\tGT\t0/1"
    , "seq\t0\t.\tA\tT" + "\t.\tPASS\t.\tGT\t1/0"
    , "seq\t0\t.\tG\tGAAT" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tG\tGAAT,GT" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tG\tGT" + "\t.\tPASS\t.\tGT\t0/1"
    , "seq\t0\t.\tG\tGAAT" + "\t.\tPASS\t.\tGT\t0/1"
    , "seq\t0\t.\tA\tG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tG\tA" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tA\tG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tA\tG" + "\t.\tPASS\t.\tGT\t1/0"
    , "seq\t0\t.\tA\tG,C" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tT\tC" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tA\tG,T" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tT\tG,C" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tAG\tGA" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tAG\tGA" + "\t.\tPASS\t.\tGT\t0/1"
    , "seq\t0\t.\tAG\tGT" + "\t.\tPASS\t.\tGT\t1/0"
    , "seq\t0\t.\tGA\tG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGA\tG" + "\t.\tPASS\t.\tGT\t0/1"
    , "seq\t0\t.\tGA\tG" + "\t.\tPASS\t.\tGT\t1/0"
    , "seq\t0\t.\tGACT\tG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGACT\tG" + "\t.\tPASS\t.\tGT\t0/1"
    , "seq\t0\t.\tGACT\tG,GA" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tGACT\tGC" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGACT\tGC,GT" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tGACT\tG,GC" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tGA\tGGC" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGA\tG,GAA" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tGA\tG,GG" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tGACT\tGGACT,GA" + "\t.\tPASS\t.\tGT\t1/2"
    , "seq\t0\t.\tGA\tGGG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGGG\tGA" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGGG\tG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGGAG\tGG" + "\t.\tPASS\t.\tGT\t1/1"
    , "seq\t0\t.\tGGG\tGGAG" + "\t.\tPASS\t.\tGT\t1/1"
  };

  public void testVariantStatistics() {
    final VcfHeader h = new VcfHeader();
    h.addCommonHeader();
    h.addSampleName("SAMPLE");
    final VariantStatistics stats = new VariantStatistics(null);
    for (final String variant : VARIANTS) {
      final VcfRecord r = VcfReaderTest.vcfLineToRecord(variant);
      r.addFilter("RX");
      stats.tallyVariant(h, r);
    }
    final VcfRecord missingGT = VcfReaderTest.vcfLineToRecord("seq\t0\t.\tGGG\tGGAG" + "\t.\tPASS\t.\tGT\t.");
    stats.tallyVariant(h, missingGT);
    for (final String variant : VARIANTS) {
      final VcfRecord r = VcfReaderTest.vcfLineToRecord(variant);
      stats.tallyVariant(h, r);
      stats.tallyVariant(h, r);
      stats.tallyVariant(h, r);
    }
    TestUtils.containsAll(stats.getStatistics()
        , "Passed Filters               : 115" + StringUtils.LS
        , "Failed Filters               : 58" + StringUtils.LS
        , "SNPs                         : 30" + StringUtils.LS
        , "MNPs                         : 9" + StringUtils.LS
        , "Insertions                   : 21" + StringUtils.LS
        , "Deletions                    : 27" + StringUtils.LS
        , "Indels                       : 24" + StringUtils.LS
        , "Same as reference            : 3" + StringUtils.LS
        , "Missing Genotype             : 1" + StringUtils.LS
        , "SNP Transitions/Transversions: 2.40 (36/15)" + StringUtils.LS
        , "Total Het/Hom ratio          : 1.31 (63/48)" + StringUtils.LS
        , "SNP Het/Hom ratio            : 1.50 (18/12)" + StringUtils.LS
        , "MNP Het/Hom ratio            : 2.00 (6/3)" + StringUtils.LS
        , "Insertion Het/Hom ratio      : 1.33 (12/9)" + StringUtils.LS
        , "Deletion Het/Hom ratio       : 1.25 (15/12)" + StringUtils.LS
        , "Indel Het/Hom ratio          : 1.00 (12/12)" + StringUtils.LS
        , "Insertion/Deletion ratio     : 0.78 (21/27)" + StringUtils.LS
        , "Indel/SNP+MNP ratio          : 1.85 (72/39)" + StringUtils.LS
        );
  }

  public void testLengthHistograms() {
    final VariantStatistics stats = new VariantStatistics(null);
    stats.showLengthHistograms(true);
    stats.showAlleleCountHistograms(true);

    final VcfHeader h = new VcfHeader();
    h.addCommonHeader();
    h.addSampleName("SAMPLE");
    for (final String variant : VARIANTS) {
      final VcfRecord r = VcfReaderTest.vcfLineToRecord(variant);
      r.addFilter("RX");
      stats.tallyVariant(h, r);
    }
    final VcfRecord missingGT = VcfReaderTest.vcfLineToRecord("seq\t0\t.\tGGG\tGGAG" + "\t.\tPASS\t.\tGT\t.");
    stats.tallyVariant(h, missingGT);
    for (final String variant : VARIANTS) {
      final VcfRecord r = VcfReaderTest.vcfLineToRecord(variant);
      stats.tallyVariant(h, r);
      stats.tallyVariant(h, r);
      stats.tallyVariant(h, r);
    }

    //System.err.println(stats.getStatistics());
    TestUtils.containsAll(stats.getStatistics()
        , "Passed Filters               : 115" + StringUtils.LS
        , "Failed Filters               : 58" + StringUtils.LS
        , "Number of Alleles            : 0\t1" + StringUtils.LS
        , "                               1\t51" + StringUtils.LS
        , "                               2\t63" + StringUtils.LS
        , "SNPs                         : 30" + StringUtils.LS
        , "MNPs                         : 9" + StringUtils.LS
        , "Insertions                   : 21" + StringUtils.LS
        , "Deletions                    : 27" + StringUtils.LS
        , "Indels                       : 24" + StringUtils.LS
        , "Same as reference            : 3" + StringUtils.LS
        , "Missing Genotype             : 1" + StringUtils.LS
        , "SNP Transitions/Transversions: 2.40 (36/15)" + StringUtils.LS
        , "Total Het/Hom ratio          : 1.31 (63/48)" + StringUtils.LS
        , "SNP Het/Hom ratio            : 1.50 (18/12)" + StringUtils.LS
        , "MNP Het/Hom ratio            : 2.00 (6/3)" + StringUtils.LS
        , "Insertion Het/Hom ratio      : 1.33 (12/9)" + StringUtils.LS
        , "Deletion Het/Hom ratio       : 1.25 (15/12)" + StringUtils.LS
        , "Indel Het/Hom ratio          : 1.00 (12/12)" + StringUtils.LS
        , "Insertion/Deletion ratio     : 0.78 (21/27)" + StringUtils.LS
        , "Indel/SNP+MNP ratio          : 1.85 (72/39)" + StringUtils.LS
        , "Variant Allele Lengths :" + StringUtils.LS
        , "length SNP MNP Delete Insert Indel".replaceAll(" ", "\t") + StringUtils.LS
        , "1 54 0 18 30 18".replaceAll(" ", "\t") + StringUtils.LS
        , "2 0 12 21 0 12".replaceAll(" ", "\t") + StringUtils.LS
        , "3 0 0 15 12 0".replaceAll(" ", "\t") + StringUtils.LS
        );
  }


  public void testMultiVariantStatistics() {
    final VariantStatistics stats = new VariantStatistics(null);
    final String[] sampleNames = {"sam1", "sam2", "sam3"};
    final VcfRecord rec = VcfReaderTest.vcfLineToRecord("test\t5\t.\tA\tG,CT,T\t.\tPASS\tDP=12\tGT:DP:RE:GQ:RS\t1/1:4:0.020:13:G,4,0.020\t2:4:0.020:13:CT,4,0.020\t3/1:4:0.020:13:T,4,0.020,G,4,0.020");
    stats.tallyVariant(rec, Arrays.asList(sampleNames));
    assertEquals(""
    + "Failed Filters               : 0" + StringUtils.LS
    + "Passed Filters               : 1" + StringUtils.LS
    + StringUtils.LS
    + "Sample Name: sam1" + StringUtils.LS
    + "SNPs                         : 1" + StringUtils.LS
    + "MNPs                         : 0" + StringUtils.LS
    + "Insertions                   : 0" + StringUtils.LS
    + "Deletions                    : 0" + StringUtils.LS
    + "Indels                       : 0" + StringUtils.LS
    + "Same as reference            : 0" + StringUtils.LS
    + "SNP Transitions/Transversions: - (2/0)" + StringUtils.LS
    + "Total Het/Hom ratio          : 0.00 (0/1)" + StringUtils.LS
    + "SNP Het/Hom ratio            : 0.00 (0/1)" + StringUtils.LS
    + "MNP Het/Hom ratio            : - (0/0)" + StringUtils.LS
    + "Insertion Het/Hom ratio      : - (0/0)" + StringUtils.LS
    + "Deletion Het/Hom ratio       : - (0/0)" + StringUtils.LS
    + "Indel Het/Hom ratio          : - (0/0)" + StringUtils.LS
    + "Insertion/Deletion ratio     : - (0/0)" + StringUtils.LS
    + "Indel/SNP+MNP ratio          : 0.00 (0/1)" + StringUtils.LS
    + StringUtils.LS
    + "Sample Name: sam2" + StringUtils.LS
    + "SNPs                         : 0" + StringUtils.LS
    + "MNPs                         : 0" + StringUtils.LS
    + "Insertions                   : 0" + StringUtils.LS
    + "Deletions                    : 0" + StringUtils.LS
    + "Indels                       : 1" + StringUtils.LS
    + "Same as reference            : 0" + StringUtils.LS
    + "Total Haploid                : 1" + StringUtils.LS
    + "Haploid SNPs                 : 0" + StringUtils.LS
    + "Haploid MNPs                 : 0" + StringUtils.LS
    + "Haploid Insertions           : 0" + StringUtils.LS
    + "Haploid Deletions            : 0" + StringUtils.LS
    + "Haploid Indels               : 1" + StringUtils.LS
    + "SNP Transitions/Transversions: - (0/0)" + StringUtils.LS
    + "Insertion/Deletion ratio     : - (0/0)" + StringUtils.LS
    + "Indel/SNP+MNP ratio          : - (1/0)" + StringUtils.LS
    + StringUtils.LS
    + "Sample Name: sam3" + StringUtils.LS
    + "SNPs                         : 1" + StringUtils.LS
    + "MNPs                         : 0" + StringUtils.LS
    + "Insertions                   : 0" + StringUtils.LS
    + "Deletions                    : 0" + StringUtils.LS
    + "Indels                       : 0" + StringUtils.LS
    + "Same as reference            : 0" + StringUtils.LS
    + "SNP Transitions/Transversions: 1.00 (1/1)" + StringUtils.LS
    + "Total Het/Hom ratio          : - (1/0)" + StringUtils.LS
    + "SNP Het/Hom ratio            : - (1/0)" + StringUtils.LS
    + "MNP Het/Hom ratio            : - (0/0)" + StringUtils.LS
    + "Insertion Het/Hom ratio      : - (0/0)" + StringUtils.LS
    + "Deletion Het/Hom ratio       : - (0/0)" + StringUtils.LS
    + "Indel Het/Hom ratio          : - (0/0)" + StringUtils.LS
    + "Insertion/Deletion ratio     : - (0/0)" + StringUtils.LS
    + "Indel/SNP+MNP ratio          : 0.00 (0/1)" + StringUtils.LS, stats.getStatistics());
  }

  public void testMultiVariantStatisticsNulls() {
    final VariantStatistics stats = new VariantStatistics(null);
    final String[] sampleNames = {"sam1", "sam2", "sam3"};
    final VcfRecord rec = VcfReaderTest.vcfLineToRecord("test\t5\t.\tA\tG\t.\tPASS\tDP=4\tGT:DP:RE:GQ:RS\t1:4:0.020:13:G,4,0.020\t.\t.");
    stats.tallyVariant(rec, Arrays.asList(sampleNames));
    assertEquals(""
    + "Failed Filters               : 0" + StringUtils.LS
    + "Passed Filters               : 1" + StringUtils.LS
    + StringUtils.LS
    + "Sample Name: sam1" + StringUtils.LS
    + "SNPs                         : 1" + StringUtils.LS
    + "MNPs                         : 0" + StringUtils.LS
    + "Insertions                   : 0" + StringUtils.LS
    + "Deletions                    : 0" + StringUtils.LS
    + "Indels                       : 0" + StringUtils.LS
    + "Same as reference            : 0" + StringUtils.LS
    + "Total Haploid                : 1" + StringUtils.LS
    + "Haploid SNPs                 : 1" + StringUtils.LS
    + "Haploid MNPs                 : 0" + StringUtils.LS
    + "Haploid Insertions           : 0" + StringUtils.LS
    + "Haploid Deletions            : 0" + StringUtils.LS
    + "Haploid Indels               : 0" + StringUtils.LS
    + "SNP Transitions/Transversions: - (1/0)" + StringUtils.LS
    + "Insertion/Deletion ratio     : - (0/0)" + StringUtils.LS
    + "Indel/SNP+MNP ratio          : 0.00 (0/1)" + StringUtils.LS
    + StringUtils.LS
    + "Sample Name: sam2" + StringUtils.LS
    + "SNPs                         : 0" + StringUtils.LS
    + "MNPs                         : 0" + StringUtils.LS
    + "Insertions                   : 0" + StringUtils.LS
    + "Deletions                    : 0" + StringUtils.LS
    + "Indels                       : 0" + StringUtils.LS
    + "Same as reference            : 0" + StringUtils.LS
    + "Missing Genotype             : 1" + StringUtils.LS
    + "SNP Transitions/Transversions: - (0/0)" + StringUtils.LS
    + "Insertion/Deletion ratio     : - (0/0)" + StringUtils.LS
    + "Indel/SNP+MNP ratio          : - (0/0)" + StringUtils.LS
    + StringUtils.LS
    + "Sample Name: sam3" + StringUtils.LS
    + "SNPs                         : 0" + StringUtils.LS
    + "MNPs                         : 0" + StringUtils.LS
    + "Insertions                   : 0" + StringUtils.LS
    + "Deletions                    : 0" + StringUtils.LS
    + "Indels                       : 0" + StringUtils.LS
    + "Same as reference            : 0" + StringUtils.LS
    + "Missing Genotype             : 1" + StringUtils.LS
    + "SNP Transitions/Transversions: - (0/0)" + StringUtils.LS
    + "Insertion/Deletion ratio     : - (0/0)" + StringUtils.LS
    + "Indel/SNP+MNP ratio          : - (0/0)" + StringUtils.LS, stats.getStatistics());
  }

  private static final String VCF = ""
      + "##fileformat=VCFv4.1\n"
      + "##fileDate=20120210\n"
      + "##source=RTGvPOST-2.4 build <not found> (<not found>)\n"
      + "##CL=copy pasta\n"
      + "##RUN-ID=bce4adcc-dc18-4e92-bb05-e487aa60e1bc\n"
      + "##TEMPLATE-SDF-ID=4a99be67-0ae0-48f2-ad13-bed3a66a2643\n"
      + "##reference=/rtgshare/data/human/ref/sdf/hg18\n"
      + "##contig=<ID=chr1,length=247249719>\n"
      + "##INFO=<ID=LOH,Number=1,Type=String,Description=\"Indicates whether or not variant is a potential loss of heterozygosity\">\n"
      + "##INFO=<ID=RSS,Number=1,Type=Float,Description=\"RTG somatic call score\">\n"
      + "##INFO=<ID=XRX,Number=0,Type=Flag,Description=\"RTG variant was called using complex caller\">\n"
      + "##INFO=<ID=RCE,Number=0,Type=Flag,Description=\"RTG variant is equivalent to the previous variant\">\n"
      + "##INFO=<ID=CT,Number=1,Type=Integer,Description=\"Coverage threshold that was applied\">\n"
      + "##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Combined read depth for variant over all samples\">\n"
      + "##FILTER=<ID=OC,Description=\"Coverage threshold exceeded\">\n"
      + "##FILTER=<ID=a10.0,Description=\"Ambiguity exceeded 10.0\">\n"
      + "##FILTER=<ID=RC,Description=\"RTG variant is a complex region\">\n"
      + "##FILTER=<ID=RX,Description=\"RTG variant contained within hypercomplex region\">\n"
      + "##FILTER=<ID=RCEQUIV,Description=\"RTG variant is equivalent to the previous variant\">\n"
      + "##FILTER=<ID=OTHER,Description=\"Variant is invalid for unknown reasons\">\n"
      + "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">\n"
      + "##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Read Depth\">\n"
      + "##FORMAT=<ID=GQ,Number=1,Type=Float,Description=\"Genotype Quality\">\n"
      + "##FORMAT=<ID=RE,Number=1,Type=Float,Description=\"RTG Total Error\">\n"
      + "##FORMAT=<ID=AR,Number=1,Type=Float,Description=\"Ambiguity Ratio\">\n"
      + "##FORMAT=<ID=AB,Number=1,Type=Float,Description=\"Allele Balance\">\n"
      + "##FORMAT=<ID=RQ,Number=1,Type=Float,Description=\"RTG sample quality\">\n"
      + "##FORMAT=<ID=RS,Number=.,Type=String,Description=\"RTG Support Statistics\">\n"
      + "##SAMPLE=<ID=EM0231,Genomes=EM0231,Mixture=1.0,Description=\"Original genome\">\n"
      + "##SAMPLE=<ID=EM0231_T1,Genomes=EM0231;EM0231_T1,Mixture=0.15;0.85,Description=\"Original genome;Derived genome\">\n"
      + "##PEDIGREE=<Derived=EM0231_T1,Original=EM0231>\n"
      + "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tEM0231\tEM0231_T1\n"
      + "chr1\t799550\t.\tG\tC\t6.0\tPASS\tLOH=1.0;RSS=0.5;DP=96\tGT:DP:RE:AR:AB:GQ:RS\t1/0:28:4.396:0.036:0.714:6.0:C,6,0.600,G,20,3.194,T,2,0.601\t0:68:12.244:0.074:0.779:24.5:A,1,0.251,C,12,1.216,G,53,10.577,T,2,0.200\n"
      + "chr1\t805742\trs1\tG\tA\t8.6\ta10.0\tLOH=1.0;RSS=0.8;DP=95\tGT:DP:RE:AR:AB:GQ:RS\t1/0:44:10.800:0.159:0.659:9.0:A,13,4.902,C,2,2.000,G,29,3.898\t0:51:10.055:0.078:0.745:18.9:A,12,3.928,G,38,5.729,T,1,0.398\n"
      + "chr1\t805870\trs2\tG\tT\t44.7\tOC\tLOH=1.0;RSS=4.5;DP=722;CT=156\tGT:DP:RE:AR:AB:GQ:RS\t0/1:284:167.524:0.542:0.803:44.7:C,1,1.000,G,228,134.836,T,55,31.688\t0:438:241.295:0.498:0.801:147.3:C,1,1.000,G,351,184.773,T,86,55.522\n"
      + "chr1\t1255017\trs3\tT\tC\t4.0\tPASS\tLOH=1.0;RSS=0.2;DP=52\tGT:DP:RE:AR:AB:GQ:RS\t1/0:15:2.253:0.000:0.333:4.0:A,1,0.100,C,9,1.199,T,5,0.954\t1:37:4.497:0.000:0.027:60.8:A,4,0.833,C,31,3.313,G,1,0.251,T,1,0.100\n"
      + "chr1\t1256603\trs4\tT\tA\t19.5\tPASS\tLOH=-1.0;RSS=1.9;DP=41\tGT:DP:RE:AR:AB:GQ:RS\t0/0:8:1.017:0.000:0.875:19.5:C,1,0.316,T,7,0.701\t1/0:33:3.612:0.000:0.545:62.1:A,15,1.810,T,18,1.801\n"
      + "chr1\t1257249\trs5\tG\tGC\t16.3\tPASS\tLOH=1.0;RSS=1.6;DP=43;XRX\tGT:DP:RE:GQ\t0/1:8:0.001:44.1\t0:35:0.304:16.3\n"
      + "chr1\t1257257\trs6\tG\tGGG\t18.7\tPASS\tLOH=1.0;RSS=1.9;DP=38;XRX\tGT:DP:RE:GQ\t0/1:10:0.431:54.3\t0:28:0.104:18.7\n"
      + "chr1\t92998003\t.\tCT\tCC,C\t5.1\tPASS\tLOH=-1.0;RSS=0.2;DP=26;XRX\tGT:DP:RE:GQ\t1/1:10:1.217:11.9\t2/1:16:1.343:4.9\n"
      + "chr1\t181263907\t.\tTTCCT\tTTC,TTCCC\t5.3\tPASS\tLOH=-1.0;RSS=0.1;DP=66;XRX\tGT:DP:RE:GQ:SS\t1/1:26:3.559:15.8\t1/2:40:4.499:3.6:2\n"
      + "chr1\t240295482\t.\tTC\tT,TT\t31.8\tPASS\tLOH=-1.0;RSS=2.9;DP=67;XRX\tGT:DP:RE:GQ\t1/1:30:2.627:29.4\t1/2:37:5.703:30.5\n"
      + "chr1\t240295492\t.\tTC\tT,TT\t31.8\tPASS\tLOH=-1.0;RSS=2.9;DP=67;XRX\tGT:DP:RE:GQ\t1/1/2:30:2.627:29.4\t./2:37:5.703:30.5\n"
      + "chr1\t240430567\t.\tAT\tA,AA\t10.6\tPASS\tLOH=-1.0;RSS=1.0;DP=88;XRX\tGT:DP:RE:GQ:DN\t1/1:38:2.961:14.8\t1/2:50:4.304:11.3:Y\n"
      + "chr1\t240430569\t.\tAT\tA,AA\t10.6\tPASS\tLOH=-1.0;RSS=1.0;DP=88;XRX\tGT:DP:RE:GQ\t2/1/0:38:2.961:14.8\t2/2:50:4.304:11.3\n"
      + "chr1\t240430571\t.\tAT\tA,AA\t10.6\tPASS\tLOH=-1.0;RSS=1.0;DP=88;XRX\tGT:DP:RE:GQ\t0:38:2.961:14.8\t1:50:4.304:11.3\n"
    ;


  public void testBasic() throws IOException {
    try (final TestDirectory tdir = new TestDirectory("vcfstats")) {
      final File vcfFile = File.createTempFile("example", ".vcf", tdir);
      FileUtils.stringToFile(VCF, vcfFile);
      MainResult res = MainResult.run(new VcfStatsCli(), vcfFile.getPath(), "--Xvariant");
      assertEquals(res.err(), 0, res.rc());
      final String outVariant = res.out();
      mNano.check("variantstatistics.txt", outVariant.substring(outVariant.indexOf(StringUtils.LS) + StringUtils.LS.length()), true);

      res = MainResult.run(new VcfStatsCli(), vcfFile.getPath());
      assertEquals(res.err(), 0, res.rc());
      final String outVcfRecord = res.out();
      assertEquals(outVariant, outVcfRecord);
      mNano.check("variantstatistics.txt", outVcfRecord.substring(outVcfRecord.indexOf(StringUtils.LS) + StringUtils.LS.length()), true);

      res = MainResult.run(new VcfStatsCli(), vcfFile.getPath(), "--known");
      assertEquals(res.err(), 0, res.rc());
      String outStats = res.out();
      mNano.check("variantstatistics-known.txt", outStats.substring(outStats.indexOf(StringUtils.LS) + StringUtils.LS.length()), true);

      res = MainResult.run(new VcfStatsCli(), vcfFile.getPath(), "--novel");
      assertEquals(res.err(), 0, res.rc());
      outStats = res.out();
      mNano.check("variantstatistics-novel.txt", outStats.substring(outStats.indexOf(StringUtils.LS) + StringUtils.LS.length()), true);
    }
  }

  public void testBasicLengthHistogram() throws IOException {
    final File vcfFile = FileHelper.createTempFile();
    try {
      FileUtils.stringToFile(VCF, vcfFile);
      final MainResult res = MainResult.run(new VcfStatsCli(), vcfFile.getPath(), "--allele-lengths");
      assertEquals(res.err(), 0, res.rc());
      final String outVariant = res.out();
      mNano.check("variantstatistics2.txt", outVariant.substring(outVariant.indexOf(StringUtils.LS) + StringUtils.LS.length()), true);
    } finally {
      assertTrue(vcfFile.delete());
    }
  }

}
