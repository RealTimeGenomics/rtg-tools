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
package com.rtg.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import junit.framework.TestCase;

/**
 * Tests for <code>SpawnJvm</code>.
 *
 */
public final class SpawnJvmTest extends TestCase {

  private static final String LS = System.lineSeparator();

  static final class MaxMem {
    private MaxMem() {
    }
    /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
      System.out.println(Runtime.getRuntime().maxMemory());
    }
  }

  static final class ArgsPrinter {
    private ArgsPrinter() {
    }
    /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
      System.out.println(args.length);
    for (String arg : args) {
      System.out.println(arg);
    }
    }
  }

  static final class Rot13 {
    private Rot13() {
    }

    /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) throws IOException {
      final InputStreamReader input = new InputStreamReader(System.in);
      int c;
      while ((c = input.read()) != -1) {
        if (c >= 'A' && c <= 'M' || c >= 'a' && c <= 'm') {
          c += 'N' - 'A';
        } else if (c >= 'N' && c <= 'Z' || c >= 'n' && c <= 'z') {
          c -= 'N' - 'A';
        }
        System.out.print((char) c);
      }
    }
  }

  static final class Interactive {
    private Interactive() {
    }
    /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) throws IOException {
      final BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      String line;
      while ((line = input.readLine()) != null) {
        System.out.println(line.length());
      }
    }
  }

  public void testOutput() throws IOException {
    assertEquals("0" + LS, runClass(null, "com.rtg.util.SpawnJvmTest$ArgsPrinter"));

    assertEquals("3" + LS + "1" + LS + "2" + LS + "3" + LS, runClass(null, "com.rtg.util.SpawnJvmTest$ArgsPrinter", "1", "2", "3"));
  }

  public void testInput() throws IOException {
    assertEquals("nOPQrSTUvWXYZAbCDEFGhIJKlM" + LS, runClass("aBCDeFGHiJKLMNoPQRSTuVWXyZ" + LS, "com.rtg.util.SpawnJvmTest$Rot13"));
  }

  public void testMemory1() throws IOException {
    final int bits = Integer.parseInt(System.getProperty("sun.arch.data.model"));

    ProcessOutput streams = new ProcessOutput(null, 134217728, "com.rtg.util.SpawnJvmTest$MaxMem");
    if (streams.mRetCode == 0) {
      return; //doesn't fail reliably on some platforms (MacOSX succeeds)
    }
    assertTrue(streams.mRetCode != 0); //this was > 0 changed to see if windows 2008 return negative return code

    streams = new ProcessOutput(null, 4096, "com.rtg.util.SpawnJvmTest$MaxMem");
    switch (bits) {
      case 32:
        assertEquals("", streams.mStdOut);
        System.out.println(streams.mStdErr);
        assertTrue(streams.mStdErr.contains("Invalid maximum heap size")
            || streams.mStdErr.contains("Malformed option: '-Xmx4294967296'"));
        break;
      case 64:
        assertEquals("", streams.mStdErr);
        final long mem = Long.parseLong(streams.mStdOut.trim());
        assertTrue(mem > 4096 * 1048576L * 0.87);
        break;
      default:
        throw new RuntimeException("Computer not 32 or 64-bit");
    }
  }

  public void testMemory2() throws IOException {
    for (int mb = 32; mb <= 1024; mb <<= 1) {
      final long mem = Long.parseLong(runClass(null, mb, "com.rtg.util.SpawnJvmTest$MaxMem").trim());
      assertTrue(mem > mb * 1048576L * 0.87);
    }

    final String osWindows = "Windows";
    if (System.getProperty("os.name").startsWith(osWindows) && Environment.is64BitVM()) {
    final long mem = Long.parseLong(runClass(null, 1436, "com.rtg.util.SpawnJvmTest$MaxMem").trim());
    assertTrue(mem > 1436 * 1048576L * 0.87);
    }
  }

  public void testInteraction() throws IOException {
    final Process p = SpawnJvm.spawn("com.rtg.util.SpawnJvmTest$Interactive");
    try {
      try (OutputStreamWriter out = new OutputStreamWriter(p.getOutputStream())) {
        final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
          out.write(LS);
          out.flush();
          assertEquals("0", in.readLine());
          out.write("abcdef" + LS);
          out.flush();
          assertEquals("6", in.readLine());
        } finally {
          in.close();
        }
      }
      try {
        assertEquals(0, p.waitFor());
      } catch (final InterruptedException e) {
        fail(e.toString());
      }
      assertEquals("", readStream(p.getErrorStream()));
    } finally {
      p.getInputStream().close();
      p.getOutputStream().close();
      p.getErrorStream().close();
    }
  }


  private static String readStream(final InputStream is) throws IOException {
    try {
      final InputStreamReader isr = new InputStreamReader(is);

      final StringBuilder sb = new StringBuilder();
      final char[] buffer = new char[4096];
      int length;
      while ((length = isr.read(buffer)) != -1) {
        sb.append(buffer, 0, length);
      }
      return sb.toString();
    } finally {
      is.close();
    }
  }

  public static String runClass(final String input, final String className, final String... args) throws IOException {
    return runClass(input, 64, className, args);
  }

  public static String runClass(final String input, final int maxMem, final String className, final String... args) throws IOException {
    final ProcessOutput streams = new ProcessOutput(input, maxMem, className, args);
    assertEquals("", streams.mStdErr);
    return streams.mStdOut;
  }

  /**
   * Used for holding output of a JVM run.
   */
  static class ProcessOutput {
    String mStdOut;
    private final String mStdErr;
    private int mRetCode;

    public ProcessOutput(final String input, final int maxMem, final String className, final String... args) throws IOException {
      final Process p = SpawnJvm.spawn(maxMem * 1024L * 1024L, className, args);
      try {
        if (input != null) {
          try (OutputStreamWriter out = new OutputStreamWriter(p.getOutputStream())) {
            out.write(input);
          }
        }
        final String[] streams = {readStream(p.getInputStream()), readStream(p.getErrorStream())};
        try {
          mRetCode = p.waitFor();
        } catch (final InterruptedException e) {
          fail(e.toString());
        }
        mStdOut = streams[0];
        mStdErr = streams[1];
      } finally {
        p.getOutputStream().close();
        p.getErrorStream().close();
        p.getInputStream().close();
      }
    }
  }
}


