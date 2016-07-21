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

package com.rtg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.MockCli;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class DummyCliEntryTest extends TestCase {
  private static final class MockCliEntry extends AbstractCliEntry {
    final File mOutput;

    private MockCliEntry(File output) {
      mOutput = output;
    }

    @Override
    protected Command getSlimModule(String arg) {
      return new Command(new MockCli(new File(mOutput, "dir")) {
        @Override
        public String description() {
          return "Mocking";
        }

        @Override
        protected int mainExec(OutputStream out, PrintStream err) throws IOException {
          return 0;
        }
      }, CommandCategory.ASSEMBLY, ReleaseLevel.ALPHA);
    }
  }

  public void test() throws IOException {
    try (TestDirectory t = new TestDirectory()) {
      final ByteArrayOutputStream err = new ByteArrayOutputStream();
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (PrintStream print = new PrintStream(err)) {
        final int main = new MockCliEntry(t).intMain(new String[]{"--help"}, out, print);
        print.flush();
        assertEquals("", err.toString());
        assertEquals(err.toString(), 0, main);
        assertEquals("", out.toString());
      }
    }
  }

}