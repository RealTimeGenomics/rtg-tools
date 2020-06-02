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

import java.io.File;
import java.io.IOException;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class SamCommandHelperTest extends TestCase {

  private CFlags mFlags = null;

  @Override
  public void setUp() {
    mFlags = new CFlags();
    SamCommandHelper.initSamRg(mFlags);
  }

  @Override
  public void tearDown() {
    mFlags = null;
  }


  public void testRG() throws IOException {
    final File tmpDir = FileUtils.createTempDir("tmp", "sdjf");
    try {
      mFlags.setFlags("--sam-rg", tmpDir.getAbsolutePath());
      assertFalse(SamCommandHelper.validateSamRg(mFlags));
      assertTrue(mFlags.getParseMessage().contains("for --sam-rg is a directory, must be a file"));

      final File f = new File(tmpDir, "samrgfile");
      FileUtils.stringToFile("", f);

      mFlags.setFlags("--sam-rg", f.getAbsolutePath());
      assertTrue(SamCommandHelper.validateSamRg(mFlags));
    } finally {
      FileHelper.deleteAll(tmpDir);
    }
  }

  public void testSamRGErrors() throws IOException, InvalidParamsException {
    final File outer = FileUtils.createTempDir("rammap", "end2end");
    try {

      final File header = new File(outer, "header");
      FileUtils.stringToFile("", header);

      final MemoryPrintStream mps = new MemoryPrintStream();
      Diagnostic.setLogStream(mps.printStream());
      try {
        try {
          SamCommandHelper.validateAndCreateSamRG(header.toString(), SamCommandHelper.ReadGroupStrictness.REQUIRED);
          fail();
        } catch (final InvalidParamsException ipe) {
          assertTrue(ipe.getMessage().contains("file \"" + header.getPath()));
//           assertTrue(mps.toString().contains("No read group information present in the input file \"" + header.getPath() + "\", please provide file with single read group line"));
        }
        assertNull(SamCommandHelper.validateAndCreateSamRG(header.toString(), SamCommandHelper.ReadGroupStrictness.OPTIONAL));

        final File header2 = new File(outer, "header2");
        FileUtils.stringToFile("@RG\tID:L23\tSM:NA123" + "\n" + "@RG\tID:L43\tSM:NA123", header2);

        final MemoryPrintStream mps2 = new MemoryPrintStream();
        Diagnostic.setLogStream(mps2.printStream());
        try {
          SamCommandHelper.validateAndCreateSamRG(header2.toString(), SamCommandHelper.ReadGroupStrictness.REQUIRED);
          fail();
        } catch (final InvalidParamsException ipe) {
          assertTrue(ipe.getMessage().contains("file \"" + header2.getPath()));
//           assertTrue(mps2.toString().contains("Multiple read group information present in the input file \"" + header2.getPath() + "\", please provide file with single read group line"));
        }
        try {
          SamCommandHelper.validateAndCreateSamRG(header2.toString(), SamCommandHelper.ReadGroupStrictness.AT_MOST_ONE);
          fail();
        } catch (final InvalidParamsException ipe) {
          assertTrue(ipe.getMessage().contains("file \"" + header2.getPath()));
//           assertTrue(mps2.toString().contains("Multiple read group information present in the input file \"" + header2.getPath() + "\", please provide file with single read group line"));
        }
        assertNull(SamCommandHelper.validateAndCreateSamRG(header2.toString(), SamCommandHelper.ReadGroupStrictness.OPTIONAL));
      } finally {
        Diagnostic.setLogStream();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(outer));
    }
  }

  public void testStrictnessEnum() {
    TestUtils.testEnum(SamCommandHelper.ReadGroupStrictness.class, "[REQUIRED, OPTIONAL, AT_MOST_ONE]");
  }

}
