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
package com.rtg.sam;

import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.StringUtils.TAB;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.TestUtils;

/**
 */
public final class SharedSamConstants {
  private SharedSamConstants() { }

  /** Output Sam File Name **/
  public static final String OUT_SAM = "alignments.sam";

  /**
   * The comment line for the dummy SDF ID produced by test SDF creation utilities
   */
  public static final String TEMPLATE_SDF_ID = "@CO" + TAB + SamUtils.TEMPLATE_SDF_ATTRIBUTE + ReaderTestUtils.DUMMY_TEST_ID.toString();

  /** Body of reference sequence **/
  public static final String REF_BODY = "aa" + "atcg" + "actg" + "gtca" + "gcta" + "gg";
  /** Reference sequence **/
  public static final String REF_SEQS = ""
      + ">g1" + LS
      +  REF_BODY + LS
      + ">gempty" + LS
      + LS
      ;

  /** Reference file - everything defaults to diploid. */
  public static final String REF_DIPLOID = ""
      + "version 1" + LS
      + "either\tdef\tdiploid\tlinear" + LS
      ;

  /** Reference file - everything defaults to haploid. */
  public static final String REF_HAPLOID = ""
      + "version 1" + LS
      + "either\tdef\thaploid\tlinear" + LS
      ;

  // 12 3456 7890 1234 5678 90
  // aa atcg actg gtca gcta gg
  //    atcg actg
  //    atcg actg
  //      cg actg tt
  //       g actg ctc
  //       g actg ctc
  //              ttca gcta
  /** A typical header for SAM files, which defines sequence g1. **/
  public static final String SAMHEADER1 = ""
      + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + LS
      + TEMPLATE_SDF_ID + LS
      + "@SQ" + TAB + "SN:g1" + TAB + "LN:20" + LS
      + "@SQ" + TAB + "SN:gempty" + TAB + "LN:0" + LS
      + "@RG" + TAB + "ID:RG1" + TAB + "SM:TEST" + TAB + "PL:ILLUMINA" + LS
      ;
  /** A SAM header that defines g1 and g2 **/
  public static final String SAMHEADER2 = ""
      + SAMHEADER1
      + "@SQ" + TAB + "SN:g2" + TAB + "LN:20" + LS
      ;

  /** A typical header for SAM files, for cancer calling. **/
  public static final String SAMHEADER_CANCER = ""
      + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + LS
      + "@SQ" + TAB + "SN:g1" + TAB + "LN:20" + LS
      + TEMPLATE_SDF_ID + LS
      + "@RG" + TAB + "ID:RG1" + TAB + "SM:TEST" + TAB + "PL:ILLUMINA" + LS
      + "@RG" + TAB + "ID:RG2" + TAB + "SM:cancer" + TAB + "PL:ILLUMINA" + LS
      ;

  /** A typical header for SAM files for a 1 child family. **/
  public static final String SAMHEADER_FAMILY = ""
      + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + LS
      + "@SQ" + TAB + "SN:g1" + TAB + "LN:20" + LS
      + TEMPLATE_SDF_ID + LS
      + "@RG" + TAB + "ID:RG1" + TAB + "SM:father" + TAB + "PL:ILLUMINA" + LS
      + "@RG" + TAB + "ID:RG2" + TAB + "SM:mother" + TAB + "PL:ILLUMINA" + LS
      + "@RG" + TAB + "ID:RG3" + TAB + "SM:child" + TAB + "PL:ILLUMINA" + LS
      ;

  /** A typical header for SAM files for a cell lineage. **/
  public static final String SAMHEADER_LINEAGE = ""
                                                + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + LS
                                                + TEMPLATE_SDF_ID + LS
                                                + "@SQ" + TAB + "SN:g1" + TAB + "LN:20" + LS
                                                + "@RG" + TAB + "ID:RG1" + TAB + "SM:original" + TAB + "PL:ILLUMINA" + LS
                                                + "@RG" + TAB + "ID:RG2" + TAB + "SM:left" + TAB + "PL:ILLUMINA" + LS
                                                + "@RG" + TAB + "ID:RG3" + TAB + "SM:right" + TAB + "PL:ILLUMINA" + LS
      ;

  /** SAM format file after mapping and alignment **/
  public static final String SAM_BODY = ""
      + "0" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "RG:Z:RG1" + LS
      + "1" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "RG:Z:RG1" + LS
      + "2" + TAB + "0" + TAB + "g1" + TAB +  "5" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CGACTGTT" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + LS
      + "3" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + LS
      + "4" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + LS
      + "5" + TAB + "0" + TAB + "g1" + TAB + "11" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "TTCAGCTA" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + LS
      ;

  /** length. */
  public static final int SAM_LENGTH = 6 * 8;

  /** SAM File Contents **/
  public static final String SAM1 = ""
      + SAMHEADER1
      + SAM_BODY
      ;

  /** SAM File Contents **/
  public static final String SAM_CANCER = ""
      + SAMHEADER_CANCER
      + SAM_BODY
      ;

  /** SAM File Contents **/
  public static final String SAM_FAMILY = ""
      + SAMHEADER_FAMILY
      + SAM_BODY
      ;

  /** SAM File Contents of a lineage */
  public static final String SAM_LINEAGE = ""
                                          + SAMHEADER_LINEAGE
                                          + SAM_BODY
      ;

  /** SAM format file after mapping and alignment **/
  public static final String SAM_BODY_AMB = ""
      + "0" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "RG:Z:RG1" + TAB + "NH:i:2" + LS
      + "1" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "RG:Z:RG1" + TAB + "NH:i:2" + LS
      + "2" + TAB + "0" + TAB + "g1" + TAB +  "5" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CGACTGTT" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + TAB + "NH:i:2" + LS
      + "3" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + TAB + "NH:i:2" + LS
      + "4" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + TAB + "NH:i:2" + LS
      + "5" + TAB + "0" + TAB + "g1" + TAB + "11" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "TTCAGCTA" + TAB + "````````" + TAB + "AS:i:1" + TAB + "RG:Z:RG1" + TAB + "NH:i:2" + LS
      ;

  /** SAM File Contents - same as SAM1 but made ambiguous**/
  public static final String SAM1_AMB = ""
      + SAMHEADER1
      + SAM_BODY_AMB
      ;

  /** Paired-end and reverse complement variant of SAM1 **/
  public static final String SAM3 = ""
      + SAMHEADER1
      //left paired end
      + "0" + TAB +  "115" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "=" + TAB + "11" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + LS
      + "1" + TAB +   "16" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + LS
      + "2" + TAB +   "16" + TAB + "g1" + TAB +  "5" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CGACTGTT" + TAB + "````````" + TAB + "AS:i:1" + LS
      //reverse complement
      + "3" + TAB +  "16" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + LS
      + "4" + TAB +   "16" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + LS
      //right paired end
      + "5" + TAB + "179" + TAB + "g1" + TAB + "11" + TAB + "255" + TAB + "8M" + TAB + "=" + TAB + "3" + TAB + "0" + TAB + "TTCAGCTA" + TAB + "````````" + TAB + "AS:i:1" + LS
      ;
  /** length. */
  public static final int SAM3_LENGTH = 6 * 8;

  /** Same as SAM1 except that IH values added **/
  public static final String SAM9 = ""
      + SAMHEADER1
      + "0" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "IH:i:1" + LS
      + "1" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "IH:i:2" + LS
      + "2" + TAB + "0" + TAB + "g1" + TAB +  "5" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CGACTGTT" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:3" + LS
      + "3" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:3" + LS
      + "4" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:2" + LS
      + "5" + TAB + "0" + TAB + "g1" + TAB + "11" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "TTCAGCTA" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:1" + LS
      ;
  /** length. */
  public static final int SAM9_LENGTH = 6 * 8;

  /** Same as SAM9 except that an IH value has 0 **/
  public static final String SAM10 = ""
      + SAMHEADER1
      + "0" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "IH:i:2" + LS
      + "1" + TAB + "0" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + TAB + "IH:i:1" + LS
      + "2" + TAB + "0" + TAB + "g1" + TAB +  "5" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CGACTGTT" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:3" + LS
      + "3" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:3" + LS
      + "4" + TAB + "0" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:0" + LS
      + "5" + TAB + "0" + TAB + "g1" + TAB + "11" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "TTCAGCTA" + TAB + "````````" + TAB + "AS:i:1" + TAB + "IH:i:1" + LS
      ;

  /** Unmapped records. */
  public static final String SAM_UNMAPPED = ""
      + "238" + TAB + "77" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "*" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CCATCAATAGAATCCGTTCGCCAGCATTCTCTAAATGATCAGGAGCCCATTAGCGACGGCAGAAGTTGGCTC" + TAB + "555555555555555555555555555555555555555555555555555555555555555555555555" + TAB + "XC:A:d" + LS
      + "1245" + TAB + "141" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "*" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CAGAGCTACTTGTCATCGGTTCCATCCCAGCTGGACTCGTGCATGGCCACGCGGTTATGCTAAAAGGTAGAC" + TAB + "555555555555555555555555555555555555555555555555555555555555555555555555" + LS
      ;

  /** Multiple sequence reference */
  public static final String REF_SEQS_M = ""
      + ">g1" + LS
      + "aa" + "atcg" + "actg" + "gtca" + "gcta" + "gg" + LS
      + ">gempty" + LS
      + LS
      + ">g2" + LS
      + "aa" + "atcg" + "actg" + "gtca" + "gcta" + "gg" + LS
      ;

  /** Multiple sequence SAM records */
  public static final String SAM_M = ""
      + SAMHEADER2
      + SAM_BODY
      + SAM_BODY.replace("g1", "g2")
      ;
  /** length. */
  public static final int SAM_M_LENGTH = 2 * SAM_LENGTH;

  /** Unsorted SAM */
  public static final String SAM_UNSORTED;
  static {
    final String[] lines = TestUtils.splitLines(SAM_BODY);
    final String t = lines[2];
    lines[2] = lines[1];
    lines[1] = t;
    SAM_UNSORTED = ""
        + "@HD" + TAB + "VN:1.0" + LS
        + "@SQ" + TAB + "SN:g1" + TAB + "LN:20" + LS
        + "@RG" + TAB + "ID:RG1" + TAB + "SM:TEST" + TAB + "PL:ILLUMINA" + LS
        + TestUtils.cat(lines);
  }

  /** Header for 3 File SAM **/
  public static final String IN3SAM3HEADER = SAMHEADER1;
  /** Part 0 of 3 File SAM **/
  public static final String OK0 = ""
      //left paired end
      + "0" + TAB +  "115" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "=" + TAB + "11" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + LS;
  /** Part 1 of 3 File SAM **/
  public static final String OK1 = ""
      + "1" + TAB +   "16" + TAB + "g1" + TAB +  "3" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ATCGACTG" + TAB + "````````" + TAB + "AS:i:0" + LS;
  /** Part 2 of 3 File SAM **/
  public static final String OK2 = ""
      + "2" + TAB +   "16" + TAB + "g1" + TAB +  "5" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "CGACTGTT" + TAB + "````````" + TAB + "AS:i:1" + LS;
  /** Part 3 of 3 File SAM **/
  public static final String OK3 = ""
      + "3" + TAB +  "16" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + LS;
  /** Part 4 of 3 File SAM **/
  public static final String OK4 = ""
      + "4" + TAB +   "16" + TAB + "g1" + TAB +  "6" + TAB + "255" + TAB + "8M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "GACTGCTC" + TAB + "````````" + TAB + "AS:i:1" + LS;
  /** Part 5 of 3 File SAM **/
  public static final String OK5 = ""
      + "5" + TAB + "147" + TAB + "g1" + TAB + "11" + TAB + "255" + TAB + "8M" + TAB + "=" + TAB + "3" + TAB + "0" + TAB + "TTCAGCTA" + TAB + "````````" + TAB + "AS:i:1" + LS;

  /**
   * Check multiple sequences in the same file. Used in a number of different
   * tests.
   */
  public static final String REF_SEQS11 = ""
      + ">g1" + LS + "aa" + "atcg" + "actg" + "gtca" + "gcta" + "gg" + LS
      + ">g2" + LS + "TT" + "TAGC" + "TGAC" + "CAGT" + "CGAT" + "CC" + LS;

  // 12 3456 7890 1234 5678 90
  // aa atcg actg gtca gcta gg
  // atcg actg
  // atcg actg
  // cg actg tt
  // g actg ctc
  // g actg ctc
  // ttca gcta
  // SAM format file after mapping and alignment
  static final String SAM_BODY11 = ""
      + SAM_BODY
      + "0" + TAB + "0" + TAB + "g2" + TAB + "3"  + TAB + "255" + TAB + "8M"     + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "tagctgac" + TAB + "````````" + TAB + "AS:i:0" + LS
      + "1" + TAB + "0" + TAB + "g2" + TAB + "3"  + TAB + "255" + TAB + "8="     + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "tagctgac" + TAB + "````````" + TAB + "AS:i:0" + LS
      + "2" + TAB + "0" + TAB + "g2" + TAB + "5"  + TAB + "255" + TAB + "6=1X1M" + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "gctgacaa" + TAB + "````````" + TAB + "AS:i:1" + LS
      + "3" + TAB + "0" + TAB + "g2" + TAB + "6"  + TAB + "255" + TAB + "8M"     + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ctgacgag" + TAB + "````````" + TAB + "AS:i:1" + LS
      + "4" + TAB + "0" + TAB + "g2" + TAB + "6"  + TAB + "255" + TAB + "8M"     + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "ctgacgag" + TAB + "````````" + TAB + "AS:i:1" + LS
      + "5" + TAB + "0" + TAB + "g2" + TAB + "11" + TAB + "255" + TAB + "1X7M"   + TAB + "*" + TAB + "0" + TAB + "0" + TAB + "aagtcgat" + TAB + "````````" + TAB + "AS:i:1" + LS;
  /** length. */
  public static final int SAM_LENGTH11 = SAM_LENGTH + 6 * 8;
  /**
   * SAM header. Used in a number of different tests.
   */
  public static final String SAM11 = ""
      + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + LS
      + TEMPLATE_SDF_ID + LS
      + "@SQ" + TAB + "SN:g1" + TAB + "LN:20" + LS
      + "@SQ" + TAB + "SN:g2" + TAB + "LN:20" + LS
      + "@RG" + TAB + "ID:RG1" + TAB + "SM:TEST" + TAB + "PL:ILLUMINA" + LS
      + SAM_BODY11;
}
