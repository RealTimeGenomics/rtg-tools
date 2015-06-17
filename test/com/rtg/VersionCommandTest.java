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

import com.rtg.util.Constants;
import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 * Tests for the version command.
 */
public class VersionCommandTest extends TestCase {

  public void ramStringTest(final String string) {
    final Double actualpercentage = Runtime.getRuntime().maxMemory() * 100 / (Environment.getTotalMemory() * 1.0d);
    final double maxMemory = Runtime.getRuntime().maxMemory() * 10.0 / 1024 / 1024 / 1024;
    assertTrue(string.contains("RAM: "));
    assertTrue(string.contains("GB RAM can be used by rtg"));
    assertTrue(string.contains("RAM: " + ((int) maxMemory / 10.0) + "GB of"));

    final String[] lines = string.split("\n");
    for (final String line : lines) {
      if (line.contains("RAM: ")) {
        final String[] chunks = line.trim().split(" ");
        assertTrue(chunks[3].endsWith("GB"));
        final int maxpos = chunks[3].indexOf("GB");
        final String max = chunks[3].substring(0, maxpos);
        final Double maxmem = Double.parseDouble(max);
        assertTrue(maxmem >= 0);
        assertTrue(chunks[1].endsWith("GB"));
        final int gbpos = chunks[1].indexOf("GB");
        final String alloc = chunks[1].substring(0, gbpos);
        final Double allocmem = Double.parseDouble(alloc);
        assertTrue(allocmem >= 0);
        assertTrue(allocmem <= maxmem);


        assertTrue(chunks[10].startsWith("("));
        assertTrue(chunks[10].endsWith("%)"));
        final String percent = chunks[10].substring(1, chunks[10].length() - 2);
        final Double percentdbl = Double.parseDouble(percent);
        assertEquals(actualpercentage.intValue(), percentdbl.intValue());
      }
    }
    String ramString = VersionCommand.getRamString(100, 1000, 1000);
    assertEquals("100.0GB of 100.0GB RAM can be used by rtg (100%)", ramString);
    ramString = VersionCommand.getRamString(9, 90, 1000);
    assertEquals("9.0GB of 100.0GB RAM can be used by rtg (9%)", ramString);
    ramString = VersionCommand.getRamString(80, 80, 100);
    assertEquals("8.0GB of 10.0GB RAM can be used by rtg (80%)", ramString);
  }

  public void testStrings() {
    final MemoryPrintStream bout = new MemoryPrintStream();
    try {
      assertEquals(0, VersionCommand.mainInit(bout.printStream()));
      bout.outputStream().reset();
      assertEquals(0, VersionCommand.mainInit(new String[]{}, bout.outputStream()));
    } finally {
      bout.close();
    }

    final String versionOut = bout.toString();
    assertTrue(versionOut.startsWith("Product: " + Environment.getProductName()));
    ramStringTest(versionOut);

    TestUtils.containsAll(versionOut, "License: " + License.getMessage() + StringUtils.LS,
        "JVM: ",
        "Core Version: " + Environment.getCoreVersion(),
        "Contact: " + Constants.SUPPORT_EMAIL_ADDR + StringUtils.LS,
        "Patents / Patents pending:",
        "Citation:",
        "(c) Real Time Genomics, 201");

  }

}
