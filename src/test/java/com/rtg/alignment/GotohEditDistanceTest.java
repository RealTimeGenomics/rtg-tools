/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.alignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

import com.rtg.mode.DnaUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;

/**
 */
public class GotohEditDistanceTest extends AbstractUnidirectionalEditDistanceTest {

  /**
   * Get an instance of <code>UnidirectionalEditDistance</code>.
   *
   * @param gapOpen the gap open penalty.
   * @return the edit distance object.
   */
  @Override
  protected UnidirectionalEditDistance getEditDistanceInstance(int gapOpen, int gapExtend, int substitutionPenalty, int unknownsPenalty) {
    return new GotohEditDistance(gapOpen, gapExtend, substitutionPenalty, unknownsPenalty, false);
  }
  /**
   * Get an instance of <code>UnidirectionalEditDistance</code>.
   *
   * @param gapOpen the gap open penalty.
   * @return the edit distance object.
   */
  protected UnidirectionalEditDistance getEditDistanceInstance(int gapOpen, int gapExtend, int substitutionPenalty, int unknownsPenalty, boolean stopWhenTemplateRunsOut) {
    return new GotohEditDistance(gapOpen, gapExtend, substitutionPenalty, unknownsPenalty, stopWhenTemplateRunsOut);
  }

  /**
   * This is the read1 example from the article
   * "The Sequence Alignment/Map (SAM) Format and
   * SAMtools" by Li and others, in Bioinformatics
   * Advance Access published June 8, 2009.
   */
  public void testSamDocs() {
    check(1, 5,
        "   TTAGATAAAGGATA CTG".replaceAll(" ", ""), 3,
        "ATGTTAGATAA  GATAGCTGTGCT".replaceAll(" ", ""), false, 0
        );
  }

  public void testSamDocsReverse() {
    check(1, 5,
        "    CAG TATCCTTTATCTAA".replaceAll(" ", ""), 4,
        "AGCACAGCTATC  TTATCTAACAT".replaceAll(" ", ""), false, 0
        );
  }

  public void testStuart1() {
    check(5,
            //  3       10         20         30         40         50          60         70         80         90
            "AAAACTCAAG TGTCCCCGAG TGCGGCTGCT CTCGTTTGGG GTTCGTTGCT CGTGGACT AT AGCGGCTTTC TTTGCAGAAC CTCAAGACTG TTACATTACG",
            //   4750                                                   4800
            "AAACCTCAAG TGTCCCCGAG TGCGGCTGCT CTCGTTTGGG GTTCGTTGCT CGTGGACTGGT AGCGGCTTTC TTTGCAGAAC CTCAAGACTG TTACATTAC ", false, 1);
    }

  public void testStuart2() {
    check(1, 5,
        //  3       10         20         30         40         50          60
        "AAAACTCAAG TGTCCCCGAG TGCGGCTGCT CTCGTTTGGG GTTCGTTGCT CGTGGACT AT AGCGGCTTTC TTTGCAGAAC CTCAAGACTG TTACATTACG", -1,
        //   4750       4760                                        4800
         "AACCTCAAG TGTCCCCGAG TGCGGCTGCT CTCGTTTGGG GTTCGTTGCT CGTGGACTGGT AGCGGCTTTC TTTGCAGAAC CTCAAGACTG TTACATTACG", false, 1);
    }

  public void testEquals1() {
    check(0,
        "a",
        "att", false, 1);
    }

  public void testEqualsN() {
    check(0,
        "acgt",
        "acgt", false, 1);
    }
  public void testEqualsNAsN() {
    check(0,
        "acnt",
        "angt", false, 0);
  }
  public void testEqualsNAsNMismatches() {
    check(1, 2,
        "acnt",
        0, "angt", false, 1);
  }

  public void testSnpsInRead() {
    check(2,
        "agct",
        "acgt", false, 1);
    }

  /**
   * Test a deletion from the read.
   */
  public void testDelete() {
    check(1, 6,
        "acgacgtttcgcgcgc", -1,
        " cgacg   cgcgcg", false, 1);
    }

  /**
   * Two deletes (total of 9... over maxShift!) from the read
   */
  public void testDelete2() {
    check(11,
        "tactcgaaccccttctatggggtactgcat",
        "tactcga     ttctat    tactgcat", true, 1);
  }

  public void testGreyRegion() {
    check(11,
        "tactcgaacccttctatggggtactcat",
        "tactcga    ttctat    tactgcat", true, 1);
  }

  /**
   * Test a single insertion into the read.
   */
  public void testInsert() {
    check(4,
        "gatacgaac   tcgtacgcact",
        "gatacgaacccctcgtacgcactcg", false, 1);
  }

  /**
   * This is an example where CachedMatrixEditDistance gives an alignment
   * with score 5, and misses the <code>8=3D11=</code> alignment with score 4.
   */
  public void testBadAlignment() {
    check(4,
        "gatacgaa   ctcgtcgcact",
        "gatacgaaaccctcgtcgcactcg", false, 1);
  }

  /* this alignment has *15* deletes, which exceeds our maxShift threshold, hence returns maxint */
  public void testBigDiff() {
    check(2, 38,   // CachedMatrixEditDistance gives 44.
        "gggttttacgtctgcattttttttgttttagagacgtacgtacgtagttttttttaccctgcag",
        "aaccaacgtaaggacgtttttaagaacgtacgtttttgggactgcaacgt", true);
  }

  /* entire read is shifted too far left */
  public void testSmallDiff() {
    check(2, 13,
        "gggacgtacgtgggctgca",
        "ctgcactgcagag", true);
  }
  /* this is constructed so the above is JUST inside allowable shifting */
  public void testSmallDiff2() {
    check(2, 10,
        "gggacgtacgtgggctgca", -7,
        "       acctgcactgcagag", false, 1);
  }

  public void test1000Long() {
//    27=1X63=1X41=1X6=1X33=1X20=1I60=1I52=4I28=1X6=1X80=1X14=1X22=1X4=2I11=1D69=1D43=1X33=4I38=1X24=1X25=1X4=1I4=1D34=1X9=1X28=1X73=1X10=1I61=1I45=
    final String read = "acatatcatgcatcaacaacacttaggtctgccaccgacgctgtcatgaccactgctcaaacgcacagtcggccgccgtttatccagccggttacggaaacctaagaacaccagggtatccttagcgttctgtttttctatttacatagatgaagtgaaaccaaacagggatctatcagtattctgcggcactgtgtggggcgtattttgcattctgcgtgtggccaaggggagccaatttttaggggaatggattaggcgcgtcgtcagcaagcccacaccggtgtctgcctgcgccggtcgccctgcagtactaaggacggtttcttactaatacctgagttatcctacagctcaattattggcgaaacgccgaatcccatgacgagggtgggcaggaagctgcgacacgatgcgtggttattggcgaaataggcggcgcgatctacattatacgtgtcaggataataaggtcgcagtgcaagatttcgaggtaggaagtgggccattatgaacgagacgaggctccggacgactgacgcaactaagagtgtttgctcgtatattgaggagtaagttgaaagcatacataatacccaagatggttcgcaagtgtggaagtgaggtcctcttgatagtcctttccgtgacctacaatgtcctagcccggatccctttttgacaaactaacacagttaaaattgtccgaactgattgaaccttgtggtgctatctcgctctggcgtcgaacgcccccgcttcgcgagttctcgcgtaccgtacacagcgcctatgagaggacataagtagaagcaagtttgttgtccaccctatccatatatgccgttttacagccaccgattcagttcgctcgccgtttaactctagatcaggactaaccatggccttcatgaatagaacccctctaacactgttagatcgattcaacgggggtgacaacatcctgtatacggcgattgagcgtatctgtatggatttt";
//    *       AS:i:47 NM:i:36 MQ:i:255        IH:i:1
//    Gotoh alignment score: 55 7532, cigar:
//    27=1X63=1X41=1X6=1X33=1X20=1I60=1I52=4I28=1X6=1X80=1X14=1X22=1X4=2I12=1D68=1D43=1X34=4I37=1X24=1X25=1X4=1I4=1D34=1X9=1X28=1X73=1X11=1I60=1I44=6D1X
//    27=1X63=1X41=1X6=1X33=1X20=1I60=1I52=4I28=1X6=1X80=1X14=1X22=1X4=2I11=1D69=1D43=1X33=4I38=1X24=1X25=1X4=1I4=1D34=1X9=1X28=1X73=1X10=1I61=1I45=

//    Template:
    final String template = "acatatcatgcatcaacaacacttaggcctgccaccgacgctgtcatgaccactgctcaaacgcacagtcggccgccgtttatccagccggatacggaaacctaagaacaccagggtatccttagcgttctgtgtttctagttacatagatgaagtgaaaccaaacagggatctctcagtattctgcggcactgttggggcgtattttgcattctgcgtgtggccaaggggagccaatttttaggggaatggattggcgcgtcgtcagcaagcccacaccggtgtctgcctgcgccggtcgccctgcctaaggacggtttcttactaatacctgaattatcccacagctcaattattggcgaaacgccgaatcccatgacgagggtgggcaggaagctgcgacacgatgcgtggttattggcgtaataggcggcgcgaactacattatacgtgtcaggatactaagcgcagtgcaagaatttcgaggtaggaagtgggccattatgaacgagacgaggctccggacgactgacgcaactaagagtgtattgctcgtatattgaggagtaagttgaaagcatacataataccgaagatggttcgcaagtgtggaagtgaggtcctctagtcctttccgtgacctacaatgtcctagcccggatcgctttttgacaaactaacacagttacaattgtccgaactgattgaaccttgcggtgtatcatcgctctggcgtcgaacgcccccgcttcgcgagtcctcgcgtacggtacacagcgcctatgagaggacataagcagaagcaagtttgttgtccaccctatccatatatgccgttttacagccaccgattcagttcgctcgccgtttatctctagatcagactaaccatggccttcatgaatagaacccctctaacactgttagatcgattcaacggggggacaacatcctgtatacggcgattgagcgtatctgtatggattttatattagggcttcttaacacggcggatctccgatgcagccgcgtgggtggtgtacaagagct";
    check(47, read, template, false, 1);
  }

  public void test1000Score30() {
    check(1, 30,
            "aaggtattacaattgactccagaaaacaaaagggttgaagcacagggtggagggggctgaagccatgtggctcaatggccatgtg aagaaacaggccaacccttagtcccagttcactcagtgggggcctgatacagacggccccacagtgaaagacgggttaacaaaagtcaagagctcaggataagattcttctctggcgtaccaagatttttgttcttggtgccacacctcacccgatactgacaaaagcaaagcaggcaatccgagagaagtgggaaggctcaaacctatgtattaagaagatctggaaagattgggaattttaggcatgaagaagaaagagtcagaaaggacaagtcccacataaactgccatgtgggaggacatggaacttgtattctatggccactgagattagg tgtaggactaaaggagagaagctatagcaaagggattagggcttaatgagggaggactttcttctgcatgtactgcagcaagtaggagggagggggatagatgaccacttttcagggagttggaagagattcattcttccaagggatggtcgggcaagacagcctcaaggtccttccctacccccagaatctctgagtctctgacctaagcaagggatagcaatatggtgagcagttggtagggtttgaagcttaaaatctttaaatgcaaaattgaatttaaatattttaagcttcaaactacaaataccaatttcttttggattcaaggggatcttcatgcagtagacctggttttaagatcagatgatgttgagttattatttcctcattgggagagtagttctaagtaggaggcaggatagcatagctcacggtaccaaaccgatctttagttttttgtttttgtttttcttttgagatgaagtcttactctgtca ccaggctggagtggaatggcgtgcatctcagctcattgcaacctcctcctcccgggttcaagcgattctcccacctca".replaceAll(" ", ""),
            2,
          "ccaaggtattacaattgactccagaaaac aaagggttgaagcacagggtggagggggctgaagccatgtggctcaatggccatgtgaaagaaacaggccaaccAttagtcccagttcactcagtgggAgcctgatacagacggccccacagtgaaagacgggttaacaaaagtcaagagctcaggataagattcttctctggcgtaccaagatttttgttcttggtgccacacctcaccGgatactgac aaagcaaagcaggcaatcc agagaagtgggaaggctcaaacctatgtattaagaagatctggaaagattgggaattttaggcatgaagaagaaagaCtcagaaaggacaagtcccacataaactgccatgtgggaggacatggaacttgtattctatggccactgagattaggttgtaggactaaaggagagaagctatagcaaagggattagggcttaatgagggaggactttcttctgcatgtactgcagcaagAATgagggagggggatagatgaccacttttcagggagttggaagagattcattcttccaagggatggtcgggcaagacagcctcaaggtcctt cctacccccagaatctctgagtctctgacctaaAcaagggatagcaaCatggtgagcagttggta  gtttgaagcttaaaatctttaaatgcaaaattgaatttaaatattttaagcttcaaactacaaataccaatttctttAggattcaaggggatcttcatgcagtagacctggttttaagatcagatgatgttgagttattatttcctcattgggagagtagttctaagtaggaggcaggatagcatagctca ggtaccaaaccgatctttagttttttgtttttgtttttcttttgagatgaagtcttactctgtcacccaggctggagtggaatggcgtg atctcagctcattgcaacctcctcctcccgggttcaagcgattctcccacctcag".replaceAll(" ", "")
          // ===========================I=========================================================D================X=======================X===============================================================================================================X=========I===================I=============================================================================X============================================================================D==================================================================================XXX==========================================================================================I=================================X=============X=================II=============================================================================X=================================================================================================================I=================================================================D=======================I====================================================== score=31
          // Note: 7 inserts (one of length 2), 3 deletes, 9 XNPs = alignment score 7*2+1 + 3*2 + 9 = 30.
, false, 0
         );
  }

  public void test1000BigOffset() {
    // HopStepLong gave: ============D==========================================DX==============================X======X==============================X=I===================================================================================================================================================================================================XX=====XX===================================================================XXXXXX=======================================================D====D====================X================XXX====================================================X=======================X====================X==================DDD=====D=====================================I==========================================D=================================================================DDD================================================I=======================================================X=========================XX=X=====================================I=========================================I===X=======
    final String read = "            ttattttatttatttttgagacggaatttcgttcttgtcgcctaggctggagtgccggtgttgcgacctcagcccactgccgccttctcctcccccattcaagcgattctcctgcctcagcctccaggtagatgggattacaggcatgcgccaccacgcctggctaatttctgtattttcagtggacatggggtttcaccatgttggccaggctgatcttgaactgctgacctcgtgatccacccaccttggcgtcccagagtgctgggatggcaggcgtgagtcactgcgcctggccaagcctttcgttctgtccaatcagagcttcctatgattggatacggcccacccacgttgggaaaggcaatctactttgctccgtgcatggattcaaatgccgtaaacatccagagaccctcccacacaccttgaatcgtgtctgaccacacatctgggcactcccgttggcccagtcaactggacacttaagtacccaacacatgtcctcccttgcttttgcctctagactagatcattcccatctgcagagagacacatagccatttttttcaggtaaaacagttttttcgagtacattccccaaagccttcaccccatctcctctttgcttccttttcctagcatagcaggacgtgggtttagatgggcttgagtttctccagcatcgtcttcatgctccccttttctcgtcacttccttcactgcacacacctgggtctcaccaagatccctactaacccccagtgataagtgcaagaccactgggtcacctttcctgcaccgactagcatggtggactcagctacacccctttctccttgactcactcggcttctagagttccacaccctctctgtctttctcctacccctctggcaacttctctgaaattgcgttagtctattctcacaccgctaataaagacattctgaagccgggtgcggtggctcacgtctgtcatcccagcatttaggaggcc";
    final String tmpl = "tttattttatt ttattttatttattttgagacggaatttcgttcttgtcgcctaggctggagtgcagtgttgcgacctcagcccactgccgccttcacctcccacattcaagcgattctcctgcctcagcctcccgagtagatgggattacaggcatgcgccaccacgcctggctaatttctgtattttcagtggacatggggtttcaccatgttggccaggctgatcttgaactgctgacctcgtgatccacccaccttggcgtcccagagtgctgggatggcaggcgtgagtcactgcgcctggccaagcctttcgttctgtccaatcagtccttccactgattggatacggcccacccacgttgggaaaggcaatctactttgctccgtgcatggattcaaatgctcctctcatccagagaccctcccacacaccttgaatcgtgtctgaccacacatctgggcaccccgtggcccagtcaactggacacataagtacccaacacattctctcccttgcttttgcctctagactagatcattcccatctgcagagagacacacagccatttttttcaggtaaaacaattttttcgagtacattcccccaagccttcaccccatctctttgctccttttcctagcatagcaggacgtgggtttagatggcgcttgagtttctccagcatcgtcttcatgctccccttttctctcacttccttcactgcacacacctgggtctcaccaagatccctactaacccccagtgataagtgcaccactgggtcacctttcctgcaccgactagcatggtggactcagctatcacccctttctccttgactcactcggcttctagagttccacaccctctctgtcttcctcctacccctctggcaacttctctccagttgcgttagtctattctcacaccgctaataaagacatatctgaagccgggtgcggtggctcacgtctgtcatcccagcactttgggaggccgaggcgg";
    check(1, 56, read, 11, tmpl, false, 0);
  }

  private void check(int gapOpen, int score, String readString, String templateString, boolean expectMaxInt) {
    check(gapOpen, score, readString, 0, templateString, expectMaxInt, 0);
  }

  private void check(int score, String readString, String templateString, boolean expectMaxInt, int unknownsPenalty) {
    check(1, score, readString, 0, templateString, expectMaxInt, unknownsPenalty);
  }

  public void check(int gapOpen, int score, String readString, int zeroBasedStart, String templateString, boolean expectMaxInt, int unknownsPenalty) {
    final int maxShift = (int) (readString.length() * 0.01 + 7);
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(gapOpen, 1, 1, unknownsPenalty);
    final ActionsValidator validator = new ActionsValidator(gapOpen, 1, 1, unknownsPenalty);
    // encode as DNA
    final byte[] read = readString.replaceAll(" ", "").getBytes();
    final byte[] temp = templateString.replaceAll(" ", "").getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    final int[] tmp = editDist.calculateEditDistance(read, read.length, temp, zeroBasedStart, Integer.MAX_VALUE, maxShift, true);
    final int[] a0 = new int[tmp.length];
    System.arraycopy(tmp, 0, a0, 0, tmp.length);

    //Note: Only print alignment results if DNA.
    //final AlignmentResult ares = new AlignmentResult(read, a0, 0, temp);
    //System.out.println(ares.toString());
    //System.out.println("actions:" + ActionsHelper.toString(a0));
    if (expectMaxInt) {
      assertEquals(Integer.MAX_VALUE, ActionsHelper.alignmentScore(a0));
      return;
    }
    assertTrue(validator.mErrorMsg, validator.isValid(a0, read, read.length, temp, false, Integer.MAX_VALUE));
    assertEquals(score, ActionsHelper.alignmentScore(a0));

    // now check that moving the read start position along gives the same alignment.
    final String[] prefixes = {"a", "cc", "cat", "gagag"};
    final String prefix = prefixes[readString.length() % prefixes.length];
    final byte[] read2 = (prefix + readString).replaceAll(" ", "").getBytes();
    DnaUtils.encodeArray(read2);
    final int[] a2 = editDist.calculateEditDistanceFixedStart(read2, prefix.length(), read2.length, temp, zeroBasedStart, Integer.MAX_VALUE, maxShift);
    for (int i = 0; i <= ActionsHelper.ACTIONS_START_INDEX; ++i) {
      assertEquals(a0[i], a2[i]);
    }

    // now check that we get roughly the same alignment if we flip read+template onto the opposite arm.
    // (this is not always true, but the scores should always be pretty close)
    final byte[] s1R = DnaUtils.encodeString(reverse(readString).replaceAll(" ", "").toLowerCase(Locale.ROOT));
    final byte[] s2R = DnaUtils.encodeString(reverse(templateString).replaceAll(" ", "").toLowerCase(Locale.ROOT));
    assertEquals(read.length, s1R.length);
    assertEquals(temp.length, s2R.length);
    final UnidirectionalEditDistance fR = getEditDistanceInstance(gapOpen, 1, 1, unknownsPenalty); // because f.logStats() kills it!
    final int startR = s2R.length - (zeroBasedStart + s1R.length);
    final int[] vR = fR.calculateEditDistance(s1R, s1R.length, s2R, startR, Integer.MAX_VALUE, maxShift, false);

    assertEquals(score, ActionsHelper.alignmentScore(vR));
    // the actions strings should be nearly the same, but can have a short reversed segment due to an ambiguous insert/delete position.
    final String actsLeft = ActionsHelper.toString(a0);
    final String actsLeftRev = reverse(actsLeft);
    final String acts = ActionsHelper.toString(vR);
    assertEquals(actsLeftRev.length(), acts.length());
    assertEquals(ActionsHelper.deletionFromReadAndOverlapCount(a0), ActionsHelper.deletionFromReadAndOverlapCount(vR));
    assertEquals(ActionsHelper.insertionIntoReadAndGapCount(a0), ActionsHelper.insertionIntoReadAndGapCount(vR));
    //assertEquals(actsLeftRev, acts);
  }

  private String reverse(String str) {
    final StringBuilder sb = new StringBuilder();
    for (int i = str.length() - 1; i >= 0; --i) {
      sb.append(str.charAt(i));
    }
    return sb.toString();
  }

  public void testMaxScore() {
    final int maxScore = 11;
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 1);    //many indels in this example, allow extra shifting
    final ActionsValidator validator = new ActionsValidator(1, 1, 1, 1);
    // this has a score of 11.
    // encode as DNA
    final byte[] read = "tactcgaaccccttctatggggtactgcat".getBytes();
    final byte[] temp = "tactcga     ttctat    tactgcat".replaceAll(" ", "").getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    int[] a0 = editDist.calculateEditDistance(read, read.length, temp, 0, maxScore, 9, true);
    //Note: Only print alignment results if DNA.
    //final AlignmentResult ares = new AlignmentResult(read, a0, 0, temp);
    //System.out.println(ares.toString());
    assertTrue(validator.isValid(a0, read, read.length, temp, false, maxScore));
    assertEquals(11, ActionsHelper.alignmentScore(a0));
    //System.err.println(com.rtg.sam.CigarFormatter.actionsToCigar(a0, false, 1, read.length, temp.length, read));
    a0 = editDist.calculateEditDistance(read, read.length, temp, 0, 10, 9, true);
    assertEquals(0, ActionsHelper.actionsCount(a0)); // means no acceptable alignment found
    assertEquals(Integer.MAX_VALUE, ActionsHelper.alignmentScore(a0));
  }

  public void testFixedAndFloatingStart() {
    final int maxScore = 10;
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 1);
    final ActionsValidator validator = new ActionsValidator(1, 1, 1, 1);
    // this has a score of 11.
    // encode as DNA
    final byte[] read = "acgacgtttcgcgcgc".getBytes();
    final byte[] temp = " cgacg   cgcgcg".replaceAll(" ", "").getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    final int[] a0 = editDist.calculateEditDistance(read, read.length, temp, 0, maxScore, 7, true);
//    final AlignmentResult ares = new AlignmentResult(read, a0, 0, temp);
//    System.out.println(ares.toString());
    assertTrue(validator.isValid(a0, read, read.length, temp, false, maxScore));
    assertEquals(6, ActionsHelper.alignmentScore(a0));
    assertEquals(-1, ActionsHelper.zeroBasedTemplateStart(a0));

    final int[] a1 = editDist.calculateEditDistanceFixedStart(read, 0, read.length, temp, 0, maxScore, 10);
//    final AlignmentResult ares1 = new AlignmentResult(read, a1, 0, temp);
//    System.out.println(ares1.toString());
    assertTrue(validator.isValid(a1, read, read.length, temp, false, maxScore));
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(a1));
    assertEquals(7, ActionsHelper.alignmentScore(a1));
    //((GotohEditDistance) editDist).dump(read, temp);

    final int[] a2 = editDist.calculateEditDistanceFixedEnd(read, 0, read.length, temp, 0, 10, 10, 7);
//    final AlignmentResult ares2 = new AlignmentResult(read, a2, 0, temp);
//    System.out.println(ares2.toString());
    assertTrue(validator.isValid(a2, read, read.length, temp, false, maxScore));
    assertEquals(-1, ActionsHelper.zeroBasedTemplateStart(a2));
    assertEquals(7, ActionsHelper.alignmentScore(a2));
    //((GotohEditDistance) editDist).dump(read, temp);

    final int[] a3 = editDist.calculateEditDistanceFixedBoth(read, 0, read.length, temp, 0, 10, 10, 10);
//    final AlignmentResult ares3 = new AlignmentResult(read, a3, 0, temp);
//    System.out.println(ares3.toString());
    assertTrue(validator.isValid(a3, read, read.length, temp, false, maxScore));
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(a3));
    assertEquals(8, ActionsHelper.alignmentScore(a3));
    //((GotohEditDistance) editDist).dump(read, temp);
  }

  // lots of tests of the small pathological cases when both ends are fixed.
  public void testNothing() {
    checkBothFixed("", "", "");
  }
  public void testBothFixedE() {
    checkBothFixed("a", "a", "=");
  }
  public void testBothFixedX() {
    checkBothFixed("a", "c", "X");
  }
  public void testBothFixedD() {
    checkBothFixed("",  "c", "D");
  }
  public void testBothFixedI() {
    checkBothFixed("a", "",  "I");
  }
  public void testBothFixedDE() {
    checkBothFixed("a", "ga", "D=");
  }
  public void testBothFixedED() {
    checkBothFixed("a", "ag", "=D");
  }
  public void testBothFixedIE() {
    checkBothFixed("ga", "a", "I=");
  }
  public void testBothFixedEI() {
    checkBothFixed("ag", "a", "=I");
  }
  public void testBothFixedDEE() {
    checkBothFixed("cg", "acg", "D==");
  }
  public void testBothFixedEDE() {
    checkBothFixed("ag", "acg", "=D=");
  }
  public void testBothFixedEED() {
    checkBothFixed("ac", "acg", "==D");
  }
  public void testBothFixedIEE() {
    checkBothFixed("acg", "cg", "I==");
  }
  public void testBothFixedEIE() {
    checkBothFixed("acg", "ag", "=I=");
  }
  public void testBothFixedEEI() {
    checkBothFixed("acg", "ac", "==I");
  }
  public void testBothFixedDDDEE() {
    checkBothFixed("tg", "ctgtg", "DDD==");
  }
  public void testBothFixedIIIEE() {
    checkBothFixed("ctgtg", "tg", "III==");
  }
  public void testBothFixedEDDD()  {
    checkBothFixed("a", "atag", "=DDD");
  }
  public void testBothFixedEIII()  {
    checkBothFixed("atag", "a", "=III");
  }

  protected void checkBothFixed(final String readString, final String templateString, final String actions) {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);
    final ActionsValidator validator = new ActionsValidator(1, 1, 1, 0);
    // encode as DNA
    final byte[] read = readString.getBytes();
    final byte[] temp = templateString.getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    final int[] a0 = editDist.calculateEditDistanceFixedBoth(read, 0, read.length, temp, 0, temp.length, 10, 10);
    assertEquals(actions, ActionsHelper.toString(a0));
    assertTrue(validator.isValid(a0, read, read.length, temp, Integer.MAX_VALUE));
  }

  /**
   * Test method for {@link com.rtg.alignment.GotohEditDistance#dump}.
   * As well as testing that the dump output looks right, this also checks
   * that the penalties for shifting too far are all correct.
   *
   * @throws IOException
   */
  public void testDump() throws IOException {
    final GotohEditDistance editDist = (GotohEditDistance) getEditDistanceInstance(1, 1, 1, 1);
    // encode as DNA
    final byte[] read =      "ttacgacgtttcgcgcgc".replaceAll(" ", "").getBytes();
    final byte[] temp = "ccttttttcgacg   cgcgcg".replaceAll(" ", "").getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    final PrintStream oldOut = System.out;
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream out = new PrintStream(bos)) {
        System.setOut(out);
        final int[] a0 = editDist.calculateEditDistance(read, read.length, temp, 0, Integer.MAX_VALUE, 7, true);
        assertEquals(6, ActionsHelper.alignmentScore(a0));
        editDist.dump(read, temp, 7, 7 / 2 + 1);
      } finally {
        System.setOut(oldOut);

      }
    } finally {
      bos.close();
    }
    mNano.check("GotohEditDistanceDump.txt", bos.toString().trim(), true);
  }

  /**
   * Test method for {@link com.rtg.alignment.GotohEditDistance#diagonalCost(int, byte[], int)}.
   */
  public void testDiagonalCost() {
    final GotohEditDistance editDist = (GotohEditDistance) getEditDistanceInstance(1, 1, 1, 0);
    final byte[] read = {4, 3, 2, 1};
    assertEquals(0, editDist.diagonalCost(4, read, 1));
    assertEquals(1, editDist.diagonalCost(3, read, 1));
  }

  /**
   * Test method for {@link com.rtg.alignment.GotohEditDistance#isSame(int, int)}.
   */
  public void testIsSame() {
    final GotohEditDistance editDist = (GotohEditDistance) getEditDistanceInstance(1, 1, 1, 0);
    assertFalse(editDist.isSame(0, 0));
    assertFalse(editDist.isSame(1, 0));
    assertFalse(editDist.isSame(0, 1));
    assertTrue(editDist.isSame(3, 3));
    assertFalse(editDist.isSame(2, 3));
    assertFalse(editDist.isSame(3, 2));
  }

  private int[] calculate(final String readStr, final String tempStr, final UnidirectionalEditDistance ed) {
    // encode as DNA
    final byte[] read = readStr.getBytes();
    final byte[] temp = tempStr.getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    return ed.calculateEditDistance(read, read.length, temp, 0, Integer.MAX_VALUE, 7, true);
  }

  /** This tests the log output, and that the matrix growing is working okay. */
  public void testStats() {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final PrintStream pos = new PrintStream(bos);
    Diagnostic.setLogStream(pos);
    try {
      final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 1);
      editDist.logStats();  // should be empty
      // read length 2
      assertEquals(0, ActionsHelper.alignmentScore(calculate("tc", "atc", editDist)));
      editDist.logStats();
      // read length 1
      assertEquals(0, ActionsHelper.alignmentScore(calculate("a", "atc", editDist)));
      editDist.logStats();
      // read length 26
      assertEquals(5, ActionsHelper.alignmentScore(calculate("atcgatgcccagcagcagcatttttg", "atcgattttgcccagcagcagcattttt", editDist)));
      editDist.logStats();
      // read length 25
      assertEquals(4, ActionsHelper.alignmentScore(calculate("atcgatgcccagcagcagcattttt", "atcgattttgcccagcagcagcattttt", editDist)));
      editDist.logStats();
      //case which creeps into grey region
      assertEquals(Integer.MAX_VALUE, ActionsHelper.alignmentScore(calculate("tactcgaacccttctatggggtactcat", "tactcgattctattactgcat", editDist)));
      editDist.logStats();

      // restore the usual log stream
    } finally {
      Diagnostic.setLogStream();
    }
    pos.close();
    //System.out.println(bos.toString());
    final String[] lines = bos.toString().split(StringUtils.LS);
    assertEquals(35, lines.length);
    assertTrue(lines[0].contains("GotohEditDistance maxbytes=0, currsize=0x0"));
    assertTrue(lines[4].contains("GotohEditDistance maxbytes=120, currsize=3x5"));
    assertTrue(lines[9].contains("GotohEditDistance maxbytes=120, currsize=2x2"));
    assertTrue(lines[15].contains("GotohEditDistance maxbytes=10584, currsize=27x49"));
    assertTrue(lines[22].contains("GotohEditDistance maxbytes=10584, currsize=26x48"));
    assertTrue(lines[23].contains("Maximum offset Histogram"));
    assertTrue(lines[24].contains("4 = 2"));
    assertTrue(lines[25].contains("1 = 1"));
    assertTrue(lines[26].contains("0 = 1"));
    assertTrue(lines[27].contains("Exceeded maxShift offset: 0"));
    assertTrue(lines[29].contains("GotohEditDistance maxbytes=11832, currsize=29x51"));
    assertTrue(lines[31].contains("4 = 2"));
    assertTrue(lines[32].contains("1 = 1"));
    assertTrue(lines[33].contains("0 = 1"));
    assertTrue(lines[34].contains("Exceeded maxShift offset: 1"));

  }

  public void testSomethingFromHopStep() {
    final byte[] template = "ctgaccatt".getBytes();
    final byte[] read =       "gcccatc".getBytes();

    DnaUtils.encodeArray(template);
    DnaUtils.encodeArray(read);

    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    final int[] actions = ed.calculateEditDistanceFixedStart(read, 1, read.length, template, 3, 10, 10);
    assertNotNull(actions);
    assertEquals(2, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
    assertEquals(3, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals("X====X", ActionsHelper.toString(actions));
  }

  // really, 6? Show me how... :)
  // it looks like 7?
  public void testStuExample() {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);

    // gap of 3 = 4, plus 3 subs = 7??
    //                       v                             v                                          v
    // "TTTTCTTTTTTTATTATT   TTACTTTAAGTTTTAGGGTACATGTGCACAGTGTGCAGGTTAGTTACATATGTATACATGTGCCATGCTGGTGCGCTGCACC".getBytes();
    // "TTTTCTTTTTTTATTATTATTATACTTTAAGTTTTAGGGTACATGTGCACAATGTGCAGGTTAGTTACATATGTATACATGTGCCATGCTGGTGTGCTGCACC".getBytes();

    final byte[] read =    "TTTTCTTTTTTTATTATTTTACTTTAAGTTTTAGGGTACATGTGCACAGTGTGCAGGTTAGTTACATATGTATACATGTGCCATGCTGGTGCGCTGCACC".getBytes();
    final byte[] tmpl = "TTTTCTTTTTTTATTATTATTATACTTTAAGTTTTAGGGTACATGTGCACAATGTGCAGGTTAGTTACATATGTATACATGTGCCATGCTGGTGTGCTGCACC".getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(tmpl);
    final int[] actions = editDist.calculateEditDistanceFixedBoth(read, 0, read.length, tmpl, 0, tmpl.length, Integer.MAX_VALUE, 10);
    assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals(7, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
  }

  // really, 22? Show me how... :)
  // it looks like 23?
  public void testStuExample3() {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] read = "AGTCTCAGATACAAAATAAATGTGCAAAAATCACAAGCATTCTTATACACCAATTAACAGAAGCCAAATCATGAGTGAACTCCCATTCACAATTGCTTC".getBytes();
    final byte[] tmpl = "GATACAAAATCAATGTACAAAAATCACAAGCATTCTTATACACCAACAACAGACAAACAGAGAGCAAAATCATGAGTGAACTCCCATTCACAATTGCTTC".getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(tmpl);
    final int[] actions = editDist.calculateEditDistanceFixedBoth(read, 0, read.length, tmpl, 0, tmpl.length, Integer.MAX_VALUE, 10);
    assertEquals(23, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
    assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
  }


  // really, 16? Show me how... :)
  // it looks like 17?
  public void testStuExample2() {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);

//    final String s1 = removeSpaces("CATTCACAATTGCTTCAAA     TAA AATACCTAGGAATCCAACTTACAAGGGATGTGAAGGACCTCTTCAAG GAGAA CTACAAACCACTGCTCAGGGAAATAG");
  //  final String s2 = removeSpaces(     "ACAATTGCTTCAAAGAGAATAA AATACCTAGGAATCCAACTTACAAGGGATGTGAAGGACCTCTTCAAG CAGAG CTACAAACCACTGCTGAAGGAAATAA");

    final byte[] read = "CATTCACAATTGCTTCAAATAAAATACCTAGGAATCCAACTTACAAGGGATGTGAAGGACCTCTTCAAGGAGAACTACAAACCACTGCTCAGGGAAATAG".getBytes();
    final byte[] tmpl = "ACAATTGCTTCAAAGAGAATAAAATACCTAGGAATCCAACTTACAAGGGATGTGAAGGACCTCTTCAAGCAGAGCTACAAACCACTGCTGAAGGAAATAA".getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(tmpl);
    final int[] actions = editDist.calculateEditDistanceFixedBoth(read, 0, read.length, tmpl, 0, tmpl.length, Integer.MAX_VALUE, 10);
    assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals(17, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
  }


  public void testFixedAlanStuff() {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] read = "aaacgcaagatccaggccctgcagcagcaggcggac".getBytes();
    final byte[] tmpl = "aaacgcaagatccaggcccgcagcagcaggcggac".getBytes();

    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(tmpl);

    int[] actions = editDist.calculateEditDistanceFixedBoth(read, 1, 4, tmpl, 1, 4, 10, 10);
    assertEquals("===", ActionsHelper.toString(actions));
    assertEquals(0, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
    assertEquals(1, actions[ActionsHelper.TEMPLATE_START_INDEX]);

    // acgcaagatccaggccctgcagcagcaggcgga
    // acgcaagatccaggcccgcagcagcaggcgga
    actions = editDist.calculateEditDistanceFixedBoth(read, 2, read.length - 1, tmpl, 2, tmpl.length - 1, 10, 10);
    assertEquals("=================I===============", ActionsHelper.toString(actions));
    assertEquals(2, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(2, ActionsHelper.alignmentScore(actions));

    final byte[] read2 =  "aaaacacttctccttc".getBytes();
    final byte[] tmpl2 = "aaaacactgtctccttcaaaaaaaaaaaa".getBytes();

    DnaUtils.encodeArray(read2);
    DnaUtils.encodeArray(tmpl2);

    actions = editDist.calculateEditDistanceFixedBoth(read2, 0, read2.length, tmpl2, 1, 17, 15, 10);
    assertEquals("I=======D========", ActionsHelper.toString(actions));
    assertEquals(1, ActionsHelper.zeroBasedTemplateStart(actions));  //the insertion doesn't have an effect on the start pos
    assertEquals(4, ActionsHelper.alignmentScore(actions));

    final String read3 = "CGTTGGCCAGGC";
    final String tmpl3 = "TGTTGGCCAGGCTGGGGAAT";

    actions = editDist.calculateEditDistanceFixedBoth(DnaUtils.encodeString(read3), 0, read3.length(), DnaUtils.encodeString(tmpl3), 0, tmpl3.length(), 20, 7);
    assertEquals(Integer.MAX_VALUE, ActionsHelper.alignmentScore(actions));
    actions = editDist.calculateEditDistanceFixedBoth(DnaUtils.encodeString(read3), 0, read3.length(), DnaUtils.encodeString(tmpl3), 0, tmpl3.length(), 20, 8);
    assertEquals(10, ActionsHelper.alignmentScore(actions));
  }

  public void testFixedEndAlanStuff() {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] read = "gggggaaaacatcttctccttctacctgaacagggacccagttttt".getBytes();
    final byte[] tmpl = "cccccctaaaacagcttctctagcatcttgacagggcactcagagggggg".getBytes();

    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(tmpl);

    final int maxShift = (int) (read.length * 0.01 + 7);

    int[] actions = editDist.calculateEditDistanceFixedEnd(read, 5, 7, tmpl, 7, 9, 15, maxShift);
    assertEquals("==", ActionsHelper.toString(actions));
    assertEquals(7, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(0, ActionsHelper.alignmentScore(actions));

    //  aaaacatcttctccttctacctgaacagggacccag
    //  aaaacagcttctctagcatcttgacagggcactcag
    actions = editDist.calculateEditDistanceFixedEnd(read, 5, read.length - 5, tmpl, 6, tmpl.length - 7, 15, maxShift);
    assertEquals("======X======XXX=XX=X===XXX==X==X===", ActionsHelper.toString(actions));
    assertEquals(7, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(12, ActionsHelper.alignmentScore(actions));

    final byte[] read2 =  "aaaacacttctccttc".getBytes();
    final byte[] tmpl2 = "aaaacactgtctccttcaaaaaaaaaaaa".getBytes();

    DnaUtils.encodeArray(read2);
    DnaUtils.encodeArray(tmpl2);

    actions = editDist.calculateEditDistanceFixedEnd(read2, 0, read2.length, tmpl2, 1, 17, 15, maxShift);
    assertEquals("========D========", ActionsHelper.toString(actions));
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(actions));  //the deletion shifts the start position to 0
    assertEquals(2, ActionsHelper.alignmentScore(actions));
  }

  public void testFixedStartAlanStuff() {
    final UnidirectionalEditDistance editDist = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] read = "gggggaaaacatcttctccttctacctgaacagggacccagttttt".getBytes();
    final byte[] tmpl = "cccccctaaaacagcttctctagcatcttgacagggcactcagagggggg".getBytes();

    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(tmpl);

    int[] actions = editDist.calculateEditDistanceFixedStart(read, 5, read.length - 5, tmpl, 7, 15, 10);
    assertEquals("======X======XXX=XX=X===XXX==X==X===", ActionsHelper.toString(actions));
    assertEquals(7, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(12, ActionsHelper.alignmentScore(actions));

    final byte[] read2 = "aaaacatcttctccttc".getBytes();
    final byte[] tmpl2 = "aaaacacttctccttcaaaaaaaaaaaa".getBytes();

    DnaUtils.encodeArray(read2);
    DnaUtils.encodeArray(tmpl2);

    actions = editDist.calculateEditDistanceFixedStart(read2, 0, read2.length, tmpl2, 0, 15, 10);
    assertEquals("======I==========", ActionsHelper.toString(actions)); //extra action due to indel
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(2, ActionsHelper.alignmentScore(actions));

    final byte[] read3 = "aaaacacttctccttc".getBytes();
    final byte[] tmpl3 = "aaaacactgtctccttcaaaaaaaaaaaa".getBytes();

    DnaUtils.encodeArray(read3);
    DnaUtils.encodeArray(tmpl3);

    actions = editDist.calculateEditDistanceFixedStart(read3, 0, read3.length, tmpl3, 0, 15, 10);
    assertEquals("========D========", ActionsHelper.toString(actions)); //extra action due to indel
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(2, ActionsHelper.alignmentScore(actions));

    final String read4 =         "CTGCGGGAGCCAGAGAG";
    final String tmpl4 = "AGTGCTAACTGCGGGAGCCAGAGAGTGCGGAGGGGAGTCGGGTCGGAGAGAGGCGGCAGG";
    actions = editDist.calculateEditDistanceFixedStart(DnaUtils.encodeString(read4), 0, read4.length(), DnaUtils.encodeString(tmpl4), 0, 20, 7);
    assertEquals(Integer.MAX_VALUE, ActionsHelper.alignmentScore(actions));
    actions = editDist.calculateEditDistanceFixedStart(DnaUtils.encodeString(read4), 0, read4.length(), DnaUtils.encodeString(tmpl4), 0, 20, 8);
    assertEquals(9, ActionsHelper.alignmentScore(actions));
  }

  public void testDontAlignWholeReadOffTemplate() {
    final byte[] s1 = DnaUtils.encodeString("ctaga");
    final byte[] s2 = DnaUtils.encodeString("nnnnnctaca");

    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    final int[] actions = ed.calculateEditDistance(s1, s1.length, s2, 5, 10, 5, true);
    assertNotNull(actions);
    assertEquals(1, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
    assertEquals(5, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals("===X=", ActionsHelper.toString(actions));
  }

  @Override
  public void testSoftClip() {

    final byte[] s1 = DnaUtils.encodeString("tttaaaaaaaaaaaa");
    final byte[] s2 = DnaUtils.encodeString("tttaaaaaaa");

    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 1);

    final int[] actions = ed.calculateEditDistance(s1, s1.length, s2, 0, 10, 5, false);
    assertNotNull(actions);
    assertEquals(5, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
    assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals("==========XXXXX", ActionsHelper.toString(actions));
  }

  public void testSoftClipPenalty() {

    final byte[] s1 = DnaUtils.encodeString("tttaaaaaaaaaaaa");
    final byte[] s2 = DnaUtils.encodeString("tttaaaaaaa");

    final UnidirectionalEditDistance ed = getEditDistanceInstance(5, 1, 1, 2);

    final int[] actions = ed.calculateEditDistance(s1, s1.length, s2, 0, 10, 5, false);
    assertNotNull(actions);
    assertEquals(10, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
    assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals("==========XXXXX", ActionsHelper.toString(actions));
  }

  public void testRegression() {
    final String t = "GTAGACGGATGGATGGATGGACAGGTAGGCAGGTAGACGGATGGTTGGACGGACGGATAGGTAGATGGGTGGGTGGATGGGCGGGTGGATGGGTGGATGGATGGACAGGT";
    final String r =   "GGATGGATGGATGGTGGGACAGGTAGGCAGGGAGACGGATGGTTGGACGGCGGATAGGTACGATGGGTGGGTGGATGGGCGGGTGGATGGGTGGATGGAT";

    check(1, 9, r, 2, t, false, 0);
  }

  public void testGapOpen() {
    //one insert, one substitution, 2 gap open penalty
    check(2, 4,
        "atcggctagtcattatgag tacgattgagatactatgttta",
        "atcggctagtcattatgagttacgattgagatgctatgttta", false);

    //one delete, one substitution, 2 gap open penalty
    check(2, 4,
        "atcggctagtcattatgagtttacgattgagatactatgttta",
        "atcggctagtcattatgagtt acgattgagatgctatgttta", false);

    //one insert, one substitution, 0 gap open penalty
    check(0, 2,
        "atcggctagtcattatgag tacgattgagatactatgttta",
        "atcggctagtcattatgagttacgattgagatgctatgttta", false);
  }

  public void testDIPenalties() {
    final UnidirectionalEditDistance ged = getEditDistanceInstance(40, 6, 30, 0);

    final byte[] tmpl = DnaUtils.encodeStringWithHyphen("CCAAATATTAGTCCTGTATGTTGTACTCCAATGTGCTCACAAG-GGGGGG-GAGGAGGGGC-TGTTGCTCAAGAGTGAATTGAAATGGCTACTACATTGTGTGAGTCCAGAGGG");
    final byte[] read = DnaUtils.encodeStringWithHyphen("CCAAATATTAGTCCTGTATGTTGTACTCCAATGTGCTCACAAG-CATCTC-TGTTGCTCAAGAGTGAATTGAAATGGCTACTACATTGTGTGAGTCCAGAGGG");

    final int[] actions = ged.calculateEditDistanceFixedBoth(read, 0, read.length, tmpl, 0, tmpl.length, Integer.MAX_VALUE, 15);
    assertNotNull(actions);
    assertEquals("===========================================IIIIIDDDDDDDDDDDDDDD=====================================================", ActionsHelper.toString(actions));
    assertEquals(40 + (6 * 5) + 40 + (6 * 15), actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
  }

  public void testLargeRead() {
    final byte[] tmpl = DnaUtils.encodeString("CCAAATATTAGTCCTGTATGTTGTACTCCAATGTGCTCACAAG GGGGGG GAGGAGGGGC TGTTGCTCAAGAGTGAATTGAAATGGCTACTACATTGTGTGAGTCCAGAGGG".replaceAll(" ", ""));
    final byte[] read = DnaUtils.encodeString("CCAAATATTAGTCCTGTATGTTGTACTCCAATGTGCTCACAAG CATCTC TGTTGCTCAAGAGTGAATTGAAATGGCTACTACATTGTGTGAGTCCAGAGGG".replaceAll(" ", ""));
    final UnidirectionalEditDistance ged = getEditDistanceInstance(40, 6, 30, 0);

    try (MemoryPrintStream mps = new MemoryPrintStream()) {
      Diagnostic.setLogStream(mps.printStream());
      try {
        final int[] actions = ged.calculateEditDistance(read, Integer.MAX_VALUE, tmpl, 0, Integer.MAX_VALUE, 15, false);
        assertEquals(Integer.MAX_VALUE, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
        assertEquals(0, actions[ActionsHelper.ACTIONS_LENGTH_INDEX]);

      } finally {
        assertTrue(mps.toString().contains("Can not create DPM for parameters rlen"));
      }
    }
    //assertNotNull(actions);
  }

  public void testPacBioFloatingEnds() {
    final byte[] tmpl = DnaUtils.encodeStringWithHyphen("TTAGCGAAGTGACGGCTTGCATCAAAACGAAGACGAACCAGATGAAGACGAACGCAACGACAACAAACTAGACGGTGTCAAACGCGAACGAAGTTACGAAGTGACATCTGAAAGGTTGACGAACGCCACATCAGAAAAAGAACCACA----------------------------");
    final byte[] read = DnaUtils.encodeStringWithHyphen("TTAGCGAAGTGACG-CT-GCATCAAA-CGAAGACGAACCAGATGAAGACGAACGCAACGACAACAA-CTAGACGGTGTCAA-CGCGAACGAAGTTA-GAAGTGACATCTGAA-GGTTGACGAACGC-ACATCAGAAAAAGA-CCACAAAGAGCAAAGAAACTACTAAAAGTTGTT");
    final UnidirectionalEditDistance ged = getEditDistanceInstance(1, 1, 3, 0, true);
    final int[] a1 = ged.calculateEditDistanceFixedStart(read, 0, read.length, tmpl, 0, Integer.MAX_VALUE, 50);
    final String actions1 = ActionsHelper.toString(a1);
    assertEquals("=============D==D======D========================================D==============D================D=============D==============D==============D======", actions1);
    final int[] a2 = ged.calculateEditDistanceFixedStart(tmpl, 0, tmpl.length, read, 0, Integer.MAX_VALUE, 50);
    final String actions2 = ActionsHelper.toString(a2);
    assertEquals(actions1.replace("D", "I"), actions2);
    assertEquals(ActionsHelper.alignmentScore(a1), ActionsHelper.alignmentScore(a2));
  }

  // reverse of previous test
  public void testPacBioFloatingStarts() {
    final byte[] tmpl = DnaUtils.encodeStringWithHyphen("TTGTTGAAAATCATCAAAGAAACGAGAAACACC-AGAAAAAGACTACA-CGCAAGCAGTTGG-AAGTCTACAGTGAAG-ATTGAAGCAAGCGC-AACTGTGGCAGATC-AACAACAGCAACGCAAGCAGAAGTAGACCAAGCAGAAGC-AAACTACG-TC-GCAGTGAAGCGATT");
    final byte[] read = DnaUtils.encodeStringWithHyphen("----------------------------ACACCAAGAAAAAGACTACACCGCAAGCAGTTGGAAAGTCTACAGTGAAGCATTGAAGCAAGCGCAAACTGTGGCAGATCAAACAACAGCAACGCAAGCAGAAGTAGACCAAGCAGAAGCAAAACTACGTTCGGCAGTGAAGCGATT");
    final UnidirectionalEditDistance ged = getEditDistanceInstance(1, 1, 3, 0, true);
    final int[] a1 = ged.calculateEditDistanceFixedEnd(read, 0, read.length, tmpl, tmpl.length - read.length, tmpl.length, Integer.MAX_VALUE, 50);
    final String actions1 = ActionsHelper.toString(a1);
    assertEquals("=====I==============I=============I===============I==============I==============I=======================================I========I==I==============", actions1);
    final int[] a2 = ged.calculateEditDistanceFixedEnd(tmpl, 0, tmpl.length, read, read.length - tmpl.length, read.length, Integer.MAX_VALUE, 50);
    final String actions2 = ActionsHelper.toString(a2);
    assertEquals(actions1.replace("I", "D"), actions2);
    assertEquals(ActionsHelper.alignmentScore(a1), ActionsHelper.alignmentScore(a2));
  }

  public void testPacBioFloatingEndsNotAtOrigin() {
    final byte[] tmpl = DnaUtils.encodeStringWithHyphen("TTAGCGAAGTGACGGCTTGCATCAAAACGAAGACGAACCAGATGAAGACGAACGCAACGACAACAAACTAGACGGTGTCAAACGCGAACGAAGTTACGAAGTGACATCTGAAAGGTTGACGAACGCCACATCAGAAAAAGAACCACA----------------------------");
    final byte[] read = DnaUtils.encodeStringWithHyphen("TTAGCGAAGTGACG-CT-GCATCAAA-CGAAGACGAACCAGATGAAGACGAACGCAACGACAACAA-CTAGACGGTGTCAA-CGCGAACGAAGTTA-GAAGTGACATCTGAA-GGTTGACGAACGC-ACATCAGAAAAAGA-CCACAAAGAGCAAAGAAACTACTAAAAGTTGTT");
    final UnidirectionalEditDistance ged = getEditDistanceInstance(1, 1, 3, 0, true);
    final int[] a1 = ged.calculateEditDistanceFixedStart(read, 25, read.length, tmpl, 28, Integer.MAX_VALUE, 50);
    final String actions1 = ActionsHelper.toString(a1);
    assertEquals("====================================D==============D================D=============D==============D==============D======", actions1);
    final int[] a2 = ged.calculateEditDistanceFixedStart(tmpl, 28, tmpl.length, read, 25, Integer.MAX_VALUE, 50);
    final String actions2 = ActionsHelper.toString(a2);
    assertEquals(actions1.replace("D", "I"), actions2);
    assertEquals(ActionsHelper.alignmentScore(a1), ActionsHelper.alignmentScore(a2));
  }

  public void testPacBioFloatingEndsLongTmp() {
    final byte[] tmpl = DnaUtils.encodeStringWithHyphen("TTAGCGAAGTGACGGCTTGCATCAAAACGAAGACGAACCAGATGAAGACGAACGCAACGACAACAAACTAGACGGTGTCAAACGCGAACGAAGTTACGAAGTGACATCTGAAAGGTTGACGAACGCCACATCAGAAAAAGAACCACA----------------------------");
    final byte[] read = DnaUtils.encodeStringWithHyphen("TTAGCGAAGTGACG-CT-GCATCAAA-CGAAGACGAAC");
    final UnidirectionalEditDistance ged = getEditDistanceInstance(1, 1, 3, 0, true);
    final int[] a1 = ged.calculateEditDistanceFixedStart(read, 0, read.length, tmpl, 0, Integer.MAX_VALUE, 50);
    final String actions1 = ActionsHelper.toString(a1);
    assertEquals("=============D==D======D==============", actions1);
  }


//  private static final String READ_OFF_EDGE = "GGGGGTTTCCCCCCCCTCCCCCCCCCCCCCCCCCCCCTCCCCCCCGGGGG";
//  private static final String TEMPLATE_OFF_EDGE = "GGGGGTTTCCCCGCCCTCCCCCCCCCTCCCCCCCCACTCCCCACCGAGGGGGAGGTTTCC";
//  public void testOffEdgeWeird() {
//    final byte[] read = DnaUtils.encodeString(READ_OFF_EDGE);
//    final byte[] template = DnaUtils.encodeString(TEMPLATE_OFF_EDGE);
//    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, false);
//    final int[] actions = ed.calculateEditDistance(read, read.length, template, 2, 589824, 50, false);
//    //This test shows weird behaviour:
//    //1: shifts start position to -33, despite max shift being 50 (-45 would be a better position, but isn't allowed due to rlen * 0.7 clause)
//    //2: As a result in the off template region we have both inserts and mismatches (see note 3), this historically causes problems with soft clipping
//    //3: Said mismatches should actually be matches due to treatNsAsMismatches being false, and they are being afforded a score of 0, however for some reason
//    //   they are being recorded as mismatches
//  }

  private static final String READ_OFF_EDGE_ION = "GTGATTCTTTCGACCAATTTAGTGCAGAAAGAAGAAATTCAATCCTAACTGAGACCTTACACCGTTTCTCATTAGAAGGAGACAGGAGCATC";
  private static final String TEMPLATE_OFF_EDGE_ION = "CTCAGTCTGTTAACATGCAACTTTTCAATATAGCATGTTATTTCATGCTATCAGAATTCACAAGGTACCAATTTAATTACTACAGAGTACTTATAGAATCATTTAAAATATAATAA"
          + "AATTGTATGATAGAGATTATATGCAATAAAACATTAACAAAATGCTAAAATACGAGACATATTCCGATTAAAGTATTTATAAAATTGATATTTATCTGTTTTTATATCTTAAAGCTGTGTCTGTAAACTGATGGCTAACAAAACTAGGATTTTGGTCACT"
          + "TCTAAAATGGAACATTTAAAGCGAAAGCTGACAAAATATTAATTTTGCATGAAGGTAGCAGCTATTTTTATGGGACATTTTCAGAACTCCAAAATCTACAGCCAGACTAGCTCAAAACTCATGGGATGTGATTCTTTCGACTAATTTAGTGCAGAAAGAA"
          + "GAAATTCAATCCTAACTGAGACCTTACACCGTTTCTCATTAGAAGGAGATGCTCCTGTCTCCTGG";

  public void testFixedStartAllInserts() {
    final byte[] read = DnaUtils.encodeString(READ_OFF_EDGE_ION);
    final byte[] template = DnaUtils.encodeString(TEMPLATE_OFF_EDGE_ION);
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, 0);
    //final int[] actions = ed.calculateEditDistance(read, read.length, template, 403, 589824, 28, false);
    final int[] actions = ed.calculateEditDistanceFixedStart(read, 82, 92, template, 485, 589824, 28);
    assertEquals("IIIIIIIIII", ActionsHelper.toString(actions));
    assertEquals(485, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(29, ActionsHelper.alignmentScore(actions));
  }

  private static final String READ_OFF_EDGE_ION_FLIP = "CTACGAGGACAGAGGAAGATTACTCTTTGCCACATTCCAGAGTCAATCCTAACTTAAAGAAGAAAGACGTGATTTAACCAGCTTTCTTAGTG";
  private static final String TEMPLATE_OFF_EDGE_ION_FLIP = "GGTCCTCTGTCCTCGTAGAGGAAGATTACTCTTTGCCACATTCCAGAGTCAATCCTAACTTAAAGAAGAAAGACGTGATTTAATCAGC"
          + "TTTCTTAGTGTAGGGTACTCAAAACTCGATCAGACCGACATCTAAAACCTCAAGACTTTTACAGGGTATTTTTATCGACGATGGAAGTACGTTTTAATTATAAAACAGTCGAAAGCGAAATTTACAAGGTAAAATCT"
          + "TCACTGGTTTTAGGATCAAAACAATCGGTAGTCAAATGTCTGTGTCGAAATTCTATATTTTTGTCTATTTATAGTTAAAATATTTATGAAATTAGCCTTATACAGAGCATAAAATCGTAAAACAATTACAAAATAAC"
          + "GTATATTAGAGATAGTATGTTAAAATAATATAAAATTTACTAAGATATTCATGAGACATCATTAATTTAACCATGGAACACTTAAGACTATCGTACTTTATTGTACGATATAACTTTTCAACGTACAATTGTCTGACTC";

  public void testFixedEndAllInserts() {
    final byte[] read = DnaUtils.encodeString(READ_OFF_EDGE_ION_FLIP);
    final byte[] template = DnaUtils.encodeString(TEMPLATE_OFF_EDGE_ION_FLIP);
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, 0);
    //final int[] actions = ed.calculateEditDistance(read, read.length, template, 403, 589824, 28, false);
    final int[] actions = ed.calculateEditDistanceFixedEnd(read, 0, 10, template, 0, 16, 589824, 28);
    assertEquals("IIIIIIIIII", ActionsHelper.toString(actions));
    assertEquals(16, ActionsHelper.zeroBasedTemplateStart(actions));
    assertEquals(29, ActionsHelper.alignmentScore(actions));
  }
}
