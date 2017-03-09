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

package com.rtg.reference;

import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.StringUtils.TAB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 */
public class ReferenceParseTest extends TestCase {

  @Override
  protected void tearDown() throws Exception {
    Diagnostic.setLogStream();
  }

  public void testRegion() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 4);
    assertEquals("s1:1-3", ReferenceParse.region(names, "s1:1-3").toString());
    assertEquals(null, ReferenceParse.region(names, "xxx"));
    assertEquals(null, ReferenceParse.region(names, "s1:xx-5"));
    assertEquals(null, ReferenceParse.region(names, "s1:1-5"));
    assertEquals(null, ReferenceParse.region(names, "s2:1-3"));
  }

  public void testGetPloidy() {
    assertEquals(Ploidy.DIPLOID, ReferenceParse.getPloidy("diploid"));
    assertNull(ReferenceParse.getPloidy("xxx"));
  }

  public void testGetSex() {
    assertEquals(Sex.MALE, ReferenceParse.getSex("male"));
    assertNull(ReferenceParse.getSex("xxx"));
  }

  public void testSexMatch() {
    assertEquals(true, ReferenceParse.sexMatch(Sex.EITHER, Sex.EITHER));
    assertEquals(true, ReferenceParse.sexMatch(Sex.MALE, Sex.EITHER));
    assertEquals(false, ReferenceParse.sexMatch(Sex.EITHER, Sex.MALE));
    assertEquals(true, ReferenceParse.sexMatch(Sex.MALE, Sex.MALE));

    assertEquals(false, ReferenceParse.sexMatch(Sex.MALE, Sex.FEMALE));
    assertEquals(false, ReferenceParse.sexMatch(Sex.FEMALE, Sex.MALE));
  }

  public void testLinear() {
    assertEquals((Boolean) true, ReferenceParse.linear("linear"));
    assertEquals((Boolean) false, ReferenceParse.linear("circular"));
    assertEquals(null, ReferenceParse.linear("xxx"));
  }

  public void testSplitLineNull() {
    assertEquals(null, ReferenceParse.splitLine(""));
    assertEquals(null, ReferenceParse.splitLine("# a comment"));
    assertEquals(null, ReferenceParse.splitLine("  \t\t# a comment"));
  }

  public void testSplitLine() {
    assertTrue(Arrays.equals(new String[] {"a"}, ReferenceParse.splitLine("a")));
    assertTrue(Arrays.equals(new String[] {"", "a"}, ReferenceParse.splitLine("\ta")));
    assertTrue(Arrays.equals(new String[] {"a", "b"}, ReferenceParse.splitLine("a\tb #comment")));
  }

  public void testGood() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    Diagnostic.setLogStream(ps.printStream());
    final String refStr = ""
        + "#comment" + LS
        + "version" + TAB + "0" + LS
        + LS
        + "either" + TAB + "def" + TAB + "diploid" + TAB + "linear" + LS
        + "female" + TAB + "seq" + TAB + "s1" + TAB + "diploid" + TAB + "circular" + LS
        + "male" + TAB + "seq" + TAB + "s1" + TAB + "haploid" + TAB + "circular" + LS
        + "male" + TAB + "seq" + TAB + "s2" + TAB + "haploid" + TAB + "circular" + LS
        + LS
        + "male" + TAB + "dup" + TAB + "s1:3-4" + TAB + "s2:1-2"
        ;
    final BufferedReader ref = new BufferedReader(new StringReader(refStr));
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 4);
    names.put("s2", 2);
    final ReferenceParse parse = new ReferenceParse(names, ref, Sex.MALE);
    parse.parse();
    assertFalse(parse.mError);
    assertTrue(parse.mLinearDefault);
    assertEquals(Ploidy.DIPLOID, parse.mPloidyDefault);
    assertEquals(6, parse.mNonblankLines);

    assertEquals(1, parse.mDuplicates.size());
    final Iterator<Pair<RegionRestriction, RegionRestriction>> it = parse.mDuplicates.iterator();
    final Pair<RegionRestriction, RegionRestriction> pair = it.next();
    assertEquals("s1:3-4", pair.getA().toString());
    assertEquals("s2:1-2", pair.getB().toString());

    assertEquals("s1 HAPLOID circular 4" + LS, parse.mReferences.get("s1").toString());
    assertEquals("s2 HAPLOID circular 2" + LS, parse.mReferences.get("s2").toString());

    //System.err.println(ps.toString());
    assertFalse(ps.toString().contains("reference file"));

    ps.close();
    ref.close();
  }

  //error in overall parsing
  public void test1() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    Diagnostic.setLogStream(ps.printStream());
    final String refStr = ""
        + "version" + TAB + "xxx" + LS
        ;
    final BufferedReader ref = new BufferedReader(new StringReader(refStr));
    final Map<String, Integer> names = new HashMap<>();
    final ReferenceParse parse = new ReferenceParse(names, ref, Sex.MALE);
    parse.parse();
    assertTrue(parse.mError);

    //System.err.println(ps.toString());
    assertTrue(ps.toString().contains("Error reading reference file on line:version\txxx"));
    assertTrue(ps.toString().contains("Invalid version line."));

    ps.close();
    ref.close();
  }

  //empty file
  public void test2() throws IOException {
    final MemoryPrintStream ps = new MemoryPrintStream();
    Diagnostic.setLogStream(ps.printStream());
    final BufferedReader ref = new BufferedReader(new StringReader(""));
    final Map<String, Integer> names = new HashMap<>();
    final ReferenceParse parse = new ReferenceParse(names, ref, Sex.MALE);
    parse.parse();
    assertTrue(parse.mError);

    //System.err.println(ps.toString());
    assertTrue(ps.toString().contains("No valid lines found in reference file."));

    ps.close();
    ref.close();
  }

  //error in number of fields for version
  public void testLine1() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    assertEquals("Version line too short", parse.line("version"));
  }

  //error in number of fields for version
  public void testLine2() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    assertEquals("Invalid version line.", parse.line("version\t0\tfoo"));
  }

  //error in version string
  public void testLine3() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    assertEquals("Invalid version line.", parse.line("version\t2"));
  }

  //error in version line type
  public void testLine4() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    assertEquals("Invalid version line.", parse.line("bar\t0"));
  }

  //invalid sex
  public void testLine5() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    parse.mNonblankLines++;
    assertEquals("Invalid sex:xxx", parse.line("xxx\tseq\ts1\tdiploid\tlinear"));
  }

  //invalid line type
  public void testLine6() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    parse.mNonblankLines++;
    assertEquals("Invalid line type (should be one of: def, seq, dup):xxx", parse.line("male\txxx\ts1\tdiploid\tlinear"));
  }

  //valid
  public void testDup0a() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    names.put("s2", 8);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.dup(new String[] {"male", "dup", "s1:1-3", "s2:5-7"}, true));
    assertEquals(1, parse.mDuplicates.size());
  }

  //valid no match
  public void testDup0b() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    names.put("s2", 8);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.dup(new String[] {"male", "dup", "s1:1-3", "s2:5-7"}, false));
    assertEquals(0, parse.mDuplicates.size());
  }

  //too many fields
  public void testDup1() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    assertEquals("Duplicate line has incorrect number of fields.", parse.dup(new String[] {"male", "dup", "s1:1-3", "s2:5-7", "foo"}, true));
  }

  //too few fields
  public void testDup2() {
    final ReferenceParse parse = new ReferenceParse(null, null, Sex.MALE);
    assertEquals("Duplicate line has incorrect number of fields.", parse.dup(new String[] {"male", "dup", "s1:1-3"}, true));
  }

  //invalid region
  public void testDup3() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s2", 8);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid region:xxx", parse.dup(new String[] {"male", "dup", "xxx", "s2:5-7"}, true));
  }

  //invalid region
  public void testDup4() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid region:xxx", parse.dup(new String[] {"male", "dup", "s1:1-3", "xxx"}, true));
  }

  //valid
  public void testSeq0a() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.seq(new String[] {"male", "seq", "s1", "diploid", "linear"}, true));
    assertEquals(1, parse.mReferences.size());
  }

  //valid
  public void testSeq0b() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.seq(new String[] {"male", "seq", "s1", "diploid", "linear"}, false));
    assertEquals(0, parse.mReferences.size());
  }

  //invlaid length
  public void testSeq1() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Sequence line has incorrect number of fields.", parse.seq(new String[] {"male", "seq", "s1", "diploid", "linear", "foo"}, true));
  }

  //invlaid length
  public void testSeq2() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Sequence line has incorrect number of fields.", parse.seq(new String[] {"male", "seq", "s1", "diploid"}, true));
  }

  //invalid name
  public void testSeq3() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid sequence name:", parse.seq(new String[] {"male", "seq", " ", "diploid", "linear"}, true));
  }

  //invalid name
  public void testSeq4() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid sequence name:s 1", parse.seq(new String[] {"male", "seq", "s 1", "diploid", "linear"}, true));
  }

  //invalid ploidy
  public void testSeq5() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid ploidy value:xxx", parse.seq(new String[] {"male", "seq", "s1", "xxx", "linear"}, true));
  }

  //invalid circular
  public void testSeq6() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid linear/circular value:xxx", parse.seq(new String[] {"male", "seq", "s1", "diploid", "xxx"}, true));
  }

  //wrong sequence name
  public void testSeq7() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Sequence in reference file:s2 not found in genome.", parse.seq(new String[] {"male", "seq", "s2", "diploid", "linear"}, true));
  }

  //defined twice
  public void testSeq8() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.seq(new String[] {"male", "seq", "s1", "diploid", "linear"}, true));
    assertEquals("Sequence defined twice:s1", parse.seq(new String[] {"male", "seq", "s1", "diploid", "linear"}, true));
  }

  //valid
  public void testDef0a() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.def(new String[] {"male", "def", "diploid", "linear"}, true));
    assertFalse(parse.mPloidyDefault == null);
  }

  //valid no match
  public void testDef0b() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.def(new String[] {"male", "def", "diploid", "linear"}, false));
    assertTrue(parse.mPloidyDefault == null);
  }

  //invlaid length
  public void testDef1() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Default line has incorrect number of fields.", parse.def(new String[] {"male", "def", "diploid", "linear", "foo"}, true));
  }

  //invlaid length
  public void testDef2() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Default line has incorrect number of fields.", parse.def(new String[] {"male", "seq", "diploid"}, true));
  }


  //invalid ploidy
  public void testDef3() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid ploidy value:xxx", parse.def(new String[] {"male", "def", "xxx", "linear"}, true));
  }

  //invalid circular
  public void testDef4() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals("Invalid linear/circular value:xxx", parse.def(new String[] {"male", "def", "diploid", "xxx"}, true));
  }

  //defined twice
  public void testDef5() {
    final Map<String, Integer> names = new HashMap<>();
    names.put("s1", 3);
    final ReferenceParse parse = new ReferenceParse(names, null, Sex.MALE);
    assertEquals(null, parse.def(new String[] {"male", "def", "diploid", "linear"}, true));
    assertEquals("Duplicate default definition.", parse.def(new String[] {"male", "def", "diploid", "linear"}, true));
  }

}
