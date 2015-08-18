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
package com.rtg.util.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;

import com.rtg.util.TestUtils;

import junit.framework.Assert;

/**
 * Test a <code>CFlags</code> object for various metrics used in SLIM.
 *
 */
public final class TestCFlags {

  private TestCFlags() { }

  private static final String[] OUTLAWS = {
    "preread",    // should be SDF
    "pre-read",   // should be SDF
    "specifies",  // probably redundant
    "don't",      // colloquial
    "CG",         // should be Complete Genomics
    "classname",  // users don't know about Java
    "class",      // users don't know about Java
    "licence",    // we are an American company
    "colour",     // we are an American company
  };

  private static final HashSet<String> PDESC = new HashSet<>();
  static {
    PDESC.add(null);
    PDESC.add("BOOL");
    PDESC.add("DIR");
    PDESC.add("EXPRESSION");
    PDESC.add("FILE");
    PDESC.add("FLOAT");
    PDESC.add("FORMAT");
    PDESC.add("INT");
    PDESC.add("MODEL");
    PDESC.add("NAME");
    PDESC.add("STRING");
    PDESC.add("PERCENTAGE");
    PDESC.add("SDF");
    PDESC.add("SDF|FILE");
    PDESC.add("SEX");
    PDESC.add("STRING|FILE");
  }

  private static void checkDescriptionConstraints(final Flag f) {
    // Most of these conventions are based on what is seen in standard Unix commands
    // and man pages
    final String desc = f.getDescription();
    //System.err.println("Description: " + desc);
    final String name = f.getName();
    Assert.assertNotNull("Null description: --" + name, desc);
    if ((name != null) && (name.charAt(0) == 'X')) {
      return;
    }
    Assert.assertTrue("Description is too short: --" + name + " desc: " + desc, desc.length() > 8);
    Assert.assertTrue("Description should start with lowercase: --" + name + " desc: " + desc, Character.isLowerCase(desc.charAt(0)) || Character.isUpperCase(desc.charAt(1)));
    Assert.assertTrue("Description should start with lowercase: --" + name + " desc: " + desc, Character.isLetterOrDigit(desc.charAt(0)));
    Assert.assertFalse("Description should not end with \".\": --" + name + " desc: " + desc, desc.endsWith("."));
    final String[] parts = desc.split("\\s.,;:\"!?");
    for (final String o : OUTLAWS) {
      for (final String p : parts) {
        Assert.assertFalse("Outlawed \"" + o + "\" occurs in flag --" + name + " usage: " + desc, o.equals(p));
      }
    }
    CheckSpelling.check(name, desc);
    final String lcDesc = desc.toLowerCase(Locale.getDefault());

    // Should not mention the word default twice
    final int d = lcDesc.indexOf("default");
    if (d != -1) {
      Assert.assertEquals(d, lcDesc.lastIndexOf("default"));
    }
    if (name != null) {
      if (name.length() < 3) {
        Assert.fail("Long flag name is too short: --" + name);
      }
      for (int k = 0; k < name.length(); k++) {
        if (Character.isWhitespace(name.charAt(k))) {
          Assert.fail("Name of flag contains whitespace: --" + name);
        }
      }
    }
    final String pd = f.getParameterDescription();
    Assert.assertTrue(pd + " is not an allowed parameter description", PDESC.contains(pd));
  }

  /**
   * Check various syntactic properties of a <code>CFlags</code> description.
   *
   * @param flags the flags
   * @param contains strings required to be present
   */
  public static void check(final CFlags flags, final String... contains) {
    Assert.assertNotNull(flags);
    final StringBuilder problems = new StringBuilder();
    try {
      CheckSpelling.setSpelling(problems);
      for (final Flag f : flags.getRequired()) {
        checkDescriptionConstraints(f);
      }
      for (final Flag f : flags.getOptional()) {
        checkDescriptionConstraints(f);
      }
      if (problems.length() > 0) {
        Assert.fail(problems.toString());
      }
    } catch (final IOException e) {
      Assert.fail(e.getMessage());
    }
    //System.err.println("Usage TestCFlags.java\n" + flags.getUsageString());
    final String usage = flags.getUsageString().replaceAll("    --", "\\\\0, --").replaceAll("\\s+", " ");
    Assert.assertNotNull(usage);
    if (contains != null) {
      TestUtils.containsAll(usage, contains);
    }
  }

  /**
   * Check various syntactic properties of a <code>CFlags</code> description.
   *
   * @param flags the flags
   * @param contains strings required to be present
   */
  public static void checkExtendedUsage(final CFlags flags, final String... contains) {
    Assert.assertNotNull(flags);
    //System.err.println("Usage TestCFlags.java\n" + flags.getUsageString());
    final String usage = flags.getExtendedUsageString(Flag.Level.EXTENDED).replaceAll("\\s+", " ");
    Assert.assertNotNull(usage);
    if (contains != null) {
      TestUtils.containsAll(usage, contains);
    }
  }

}

