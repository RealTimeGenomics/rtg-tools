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
package com.rtg.util.io;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Tests the IOUtils class. Run from the command line with:<p>
 *
 * java junit.swingui.TestRunner com.reeltwo.util.net.IOUtilsTest java
 * com.reeltwo.util.net.IOUtilsTest
 *
 */
public class IOUtilsTest extends TestCase {

  private static final String STRING = "yollywock";

  private static final byte[] BYTES = STRING.getBytes();

  private static final byte[] EMPTY = new byte[0];


  public void testEmpty() throws IOException {
    testReadAll(EMPTY);
  }


  public void testString() throws IOException {
    testReadAll(BYTES);
  }


  private void checkBytes(final byte[] s, final byte[] bres) {
    assertEquals(s.length, bres.length);
    for (int i = 0; i < s.length; i++) {
      assertEquals(s[i], bres[i]);
    }
  }

  public void testReadAll(final byte[] s) throws IOException {
    InputStream in = new ByteArrayInputStream(s);
    checkBytes(s, IOUtils.readAll(in).getBytes());

    in = new ByteArrayInputStream(s);
    checkBytes(s, IOUtils.readAll(in, "UTF-8").getBytes());
  }

  public void testReadAllFile() throws IOException {
    final File a = FileHelper.createTempFile();
    try {
      final PrintStream fw = new PrintStream(a);
      fw.print(STRING);
      fw.close();

      assertEquals(STRING, IOUtils.readAll(a));
      assertEquals(STRING, IOUtils.readAll(new URL(a.toURI().toString())));
      checkBytes(BYTES, IOUtils.readData(new URL(a.toURI().toString())));
    } finally {
      assertTrue(!a.exists() || FileHelper.deleteAll(a));
    }
  }

  public void testReadDataBogusIn() {
    try {
      IOUtils.readData((URL) null);
      fail("Accepted null");
    } catch (final IOException e) {
      fail("IO");
    } catch (final NullPointerException e) {
      // okay
    }

    try {
      IOUtils.readData((InputStream) null);
      fail("Accepted null");
    } catch (final IOException e) {
      fail("IO");
    } catch (final NullPointerException e) {
      // okay
    }
  }


  public void testReadData() {
    final String s = "Hobbits live in small holes in the ground";
    try {
      final byte[] r = IOUtils.readData(new ByteArrayInputStream(s.getBytes()));
      assertTrue(r != null);
      assertEquals(s, new String(r));
    } catch (final IOException e) {
      fail("IO: " + e.getMessage());
    }
  }


  public void testReadDataEmpty() {
    final String s = "";
    try {
      final byte[] r = IOUtils.readData(new ByteArrayInputStream(s.getBytes()));
      assertTrue(r != null);
      assertEquals(s, new String(r));
    } catch (final IOException e) {
      fail("IO: " + e.getMessage());
    }
  }


  public void testReadDataZeroLength() {
    try {
      final byte[] r = IOUtils.readData(new ByteArrayInputStream(new byte[0]));
      assertTrue(r != null);
      assertEquals(0, r.length);
    } catch (final IOException e) {
      fail("IO: " + e.getMessage());
    }
  }

}
