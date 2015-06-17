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

import java.util.HashSet;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.IntegerOrPercentage;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 * Test the corresponding class.
 */
public class SamFilterOptionsTest extends TestCase {

  public void testConstants() {
    final HashSet<String> names = new HashSet<>();
    names.add(SamFilterOptions.MAX_HITS_FLAG);
    names.add(SamFilterOptions.MAX_AS_MATED_FLAG);
    names.add(SamFilterOptions.MAX_AS_UNMATED_FLAG);
    names.add(SamFilterOptions.EXCLUDE_MATED_FLAG);
    names.add(SamFilterOptions.EXCLUDE_UNMATED_FLAG);
    names.add(CommonFlags.RESTRICTION_FLAG);
    assertEquals(6, names.size());
    assertEquals(0, SamFilterOptions.NO_SINGLE_LETTER);
  }

  public void testIH() {
    final CFlags flags = new CFlags();
    final Flag f = SamFilterOptions.registerMaxHitsFlag(flags, 'x');
    assertNotNull(f);
    assertEquals(f, flags.getFlag(SamFilterOptions.MAX_HITS_FLAG));
    assertEquals(Character.valueOf('x'), f.getChar());
    assertEquals(Integer.class, f.getParameterType());
    assertEquals("INT", f.getParameterDescription());
    assertTrue(f.getDescription().contains("alignment count"));
    final CFlags glags = new CFlags();
    final Flag g = SamFilterOptions.registerMaxHitsFlag(glags, '\0');
    assertNotNull(g);
    assertEquals(g, glags.getFlag(SamFilterOptions.MAX_HITS_FLAG));
    assertNull(g.getChar());
    assertEquals(Integer.class, g.getParameterType());
    assertEquals("INT", g.getParameterDescription());
    assertTrue(g.getDescription().contains("alignment count"));
  }

  public void testASMated() {
    final CFlags flags = new CFlags();
    final Flag f = SamFilterOptions.registerMaxASMatedFlag(flags, 'x');
    assertNotNull(f);
    assertEquals(f, flags.getFlag(SamFilterOptions.MAX_AS_MATED_FLAG));
    assertEquals(Character.valueOf('x'), f.getChar());
    assertEquals(IntegerOrPercentage.class, f.getParameterType());
    assertEquals("INT", f.getParameterDescription());
    assertTrue(f.getDescription().contains("AS"));
    assertTrue(f.getDescription().contains("mated"));
    final CFlags glags = new CFlags();
    final Flag g = SamFilterOptions.registerMaxASMatedFlag(glags, '\0');
    assertNotNull(g);
    assertEquals(g, glags.getFlag(SamFilterOptions.MAX_AS_MATED_FLAG));
    assertNull(g.getChar());
    assertEquals(IntegerOrPercentage.class, g.getParameterType());
    assertEquals("INT", g.getParameterDescription());
    assertTrue(g.getDescription().contains("AS"));
    assertTrue(g.getDescription().contains("mated"));
  }

  public void testASUnmated() {
    final CFlags flags = new CFlags();
    final Flag f = SamFilterOptions.registerMaxASUnmatedFlag(flags, 'x');
    assertNotNull(f);
    assertEquals(f, flags.getFlag(SamFilterOptions.MAX_AS_UNMATED_FLAG));
    assertEquals(Character.valueOf('x'), f.getChar());
    assertEquals(IntegerOrPercentage.class, f.getParameterType());
    assertEquals("INT", f.getParameterDescription());
    assertTrue(f.getDescription().contains("AS"));
    assertTrue(f.getDescription().contains("unmated"));
    final CFlags glags = new CFlags();
    final Flag g = SamFilterOptions.registerMaxASUnmatedFlag(glags, '\0');
    assertNotNull(g);
    assertEquals(g, glags.getFlag(SamFilterOptions.MAX_AS_UNMATED_FLAG));
    assertNull(g.getChar());
    assertEquals(IntegerOrPercentage.class, g.getParameterType());
    assertEquals("INT", g.getParameterDescription());
    assertTrue(g.getDescription().contains("AS"));
    assertTrue(g.getDescription().contains("unmated"));
  }

  public void testExcludeMated() {
    final CFlags flags = new CFlags();
    final Flag f = SamFilterOptions.registerExcludeMatedFlag(flags);
    assertNotNull(f);
    assertEquals(f, flags.getFlag(SamFilterOptions.EXCLUDE_MATED_FLAG));
    assertNull(f.getParameterType());
    assertTrue(f.getDescription().contains("exclude"));
    assertTrue(f.getDescription().contains("mated"));
  }

  public void testExcludeUnmated() {
    final CFlags flags = new CFlags();
    final Flag f = SamFilterOptions.registerExcludeUnmatedFlag(flags);
    assertNotNull(f);
    assertEquals(f, flags.getFlag(SamFilterOptions.EXCLUDE_UNMATED_FLAG));
    assertNull(f.getParameterType());
    assertTrue(f.getDescription().contains("exclude"));
    assertTrue(f.getDescription().contains("unmated"));
  }

  public void testRestriction() {
    final CFlags flags = new CFlags();
    final Flag f = SamFilterOptions.registerRestrictionFlag(flags);
    assertNotNull(f);
    assertEquals(f, flags.getFlag(CommonFlags.RESTRICTION_FLAG));
    assertEquals(String.class, f.getParameterType());
    assertEquals("STRING", f.getParameterDescription());
    assertTrue(f.getDescription().contains("SAM"));
    assertTrue(f.getDescription().contains("range"));
  }

  public void testValidator() {
    final MemoryPrintStream err = new MemoryPrintStream();
    Diagnostic.setLogStream(err.printStream());
    final CFlags flags = new CFlags();
    SamFilterOptions.registerMaxHitsFlag(flags, 'c');
    SamFilterOptions.registerMaxASMatedFlag(flags, 's');
    SamFilterOptions.registerMaxASUnmatedFlag(flags, 'u');
    SamFilterOptions.registerExcludeMatedFlag(flags);
    SamFilterOptions.registerExcludeUnmatedFlag(flags);
    SamFilterOptions.registerRestrictionFlag(flags);
    flags.setFlags("-c", "0");
    assertFalse(SamFilterOptions.validateFilterFlags(flags, false));
    assertTrue(err.toString(), err.toString().contains("The specified flag \"--max-hits\" has invalid value \"0\". It should be greater than or equal to \"1\"."));
    err.reset();
    flags.setFlags("-s", "-1");
    assertFalse(SamFilterOptions.validateFilterFlags(flags, false));
    assertTrue(err.toString(), err.toString().contains("The specified flag \"--max-as-mated\" has invalid value \"-1\". It should be greater than or equal to \"0\"."));
    err.reset();
    flags.setFlags("-u", "-1");
    assertFalse(SamFilterOptions.validateFilterFlags(flags, false));
    assertTrue(err.toString(), err.toString().contains("The specified flag \"--max-as-unmated\" has invalid value \"-1\". It should be greater than or equal to \"0\"."));
    err.reset();
    flags.setFlags("--exclude-mated", "--exclude-unmated");
    assertFalse(SamFilterOptions.validateFilterFlags(flags, false));
    assertTrue(flags.getParseMessage(), flags.getParseMessage().contains("Only one of --exclude-mated or --exclude-unmated can be set"));
    flags.setFlags("--region", "blarhg:-1");
    assertFalse(SamFilterOptions.validateFilterFlags(flags, false));
    assertTrue(flags.getParseMessage(), flags.getParseMessage().contains("The value \"blarhg:-1\" for \"--region\" is malformed."));
    flags.setFlags();
    assertTrue(SamFilterOptions.validateFilterFlags(flags, false));
    Diagnostic.setLogStream();
  }

  public void testMakeParams() {
    Diagnostic.setLogStream();
    final CFlags flags = new CFlags();
    SamFilterOptions.registerMaxHitsFlag(flags, 'c');
    SamFilterOptions.registerMaxASMatedFlag(flags, 's');
    SamFilterOptions.registerMaxASUnmatedFlag(flags, 'u');
    SamFilterOptions.registerExcludeMatedFlag(flags);
    SamFilterOptions.registerExcludeUnmatedFlag(flags);
    flags.setFlags("-c", "5", "-s", "6", "--exclude-unmated", "-u", "7");
    assertEquals("SamFilterParams minMapQ=-1 maxAlignmentCount=5 maxMatedAlignmentScore=6 maxUnmatedAlignmentScore=7 excludeUnmated=true excludeUnplaced=false requireSetFlags=0 requireUnsetFlags=0 regionTemplate=null", SamFilterOptions.makeFilterParamsBuilder(flags).create().toString());
  }

  public void testMakeParams2() {
    final MemoryPrintStream err = new MemoryPrintStream();
    Diagnostic.setLogStream(err.printStream());
    final CFlags flags = new CFlags();
    SamFilterOptions.registerMaxHitsFlag(flags, 'c');
    SamFilterOptions.registerMaxASMatedFlag(flags, 's');
    SamFilterOptions.registerMaxASUnmatedFlag(flags, 'u');
    SamFilterOptions.registerExcludeMatedFlag(flags);
    SamFilterOptions.registerExcludeUnmatedFlag(flags);
    SamFilterOptions.registerRestrictionFlag(flags);
    flags.setFlags("-c", "5", "-s", "6", "--exclude-unmated", "-u", "7", "--region", "chrx");
    assertEquals("SamFilterParams minMapQ=-1 maxAlignmentCount=5 maxMatedAlignmentScore=6 maxUnmatedAlignmentScore=7 excludeUnmated=true excludeUnplaced=false requireSetFlags=0 requireUnsetFlags=0 regionTemplate=chrx", SamFilterOptions.makeFilterParamsBuilder(flags).create().toString());
    assertTrue(err.toString(), err.toString().contains("--max-as-unmated should not be greater than --max-as-mated"));
    Diagnostic.setLogStream();
  }
}
