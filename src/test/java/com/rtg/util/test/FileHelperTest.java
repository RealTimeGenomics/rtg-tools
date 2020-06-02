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
package com.rtg.util.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

/**
 */
public class FileHelperTest extends TestCase {

  public void testCreateTempFile() throws Exception {
    final File f = FileHelper.createTempFile();
    try {
      assertNotNull(f);
      assertTrue(f.exists());
      assertTrue(f.isFile());
      assertTrue(f.canRead());
      assertEquals(0, f.length());
      assertTrue(f.getName().startsWith("unit"));
      assertTrue(f.getName().endsWith("test"));
    } finally {
      assertTrue(f.delete());
    }
    assertFalse(f.exists());

    final File f2 = FileHelper.createTempFile();
    try {
      assertNotNull(f2);
      assertTrue(f2.exists());
      assertTrue(f2.isFile());
      assertTrue(f2.canRead());
      assertEquals(0, f2.length());

      assertTrue(!f2.equals(f));
      assertTrue(f2.getName().startsWith("unit"));
      assertTrue(f2.getName().endsWith("test"));
    } finally {
      assertTrue(f2.delete());
    }
    assertFalse(f2.exists());
  }

  public void testCreateTempDirectory() throws Exception {
    final File f = FileHelper.createTempDirectory();
    try {
      assertNotNull(f);
      assertTrue(f.exists());
      assertTrue(f.isDirectory());
      assertTrue(f.canRead());
      assertTrue(f.getName().startsWith("unit"));
      assertTrue(f.getName().endsWith("test"));
    } finally {
      assertTrue(FileHelper.deleteAll(f));
    }
    assertFalse(f.exists());

    final File f2 = FileHelper.createTempDirectory();
    try {
      assertNotNull(f2);
      assertTrue(f2.exists());
      assertTrue(f2.isDirectory());
      assertTrue(f2.canRead());

      assertTrue(!f2.equals(f));
      assertTrue(f2.getName().startsWith("unit"));
      assertTrue(f2.getName().endsWith("test"));
    } finally {
      assertTrue(FileHelper.deleteAll(f2));
    }
    assertFalse(f2.exists());
  }

  public void testSubDirMethods() throws IOException {
    final File parent = FileHelper.createTempDirectory();
    try {
      final File f = FileHelper.createTempFile(parent);
      assertTrue(f.getName().startsWith("unit"));
      assertTrue(f.getName().endsWith("test"));
      assertEquals(f.getParentFile(), parent);
      final File f2 = FileHelper.createTempDirectory(parent);
      assertTrue(f2.getName().startsWith("unit"));
      assertTrue(f2.getName().endsWith("test"));
      assertEquals(f2.getParentFile(), parent);
    } finally {
      assertTrue(FileHelper.deleteAll(parent));
    }
  }

  public void testReaderToString() throws IOException {
    assertEquals("", FileHelper.readerToString(new StringReader("")));
    assertEquals(" ", FileHelper.readerToString(new StringReader(" ")));
  }

  public void testResourceToString() throws IOException {
    final String str = FileHelper.resourceToString("com/rtg/util/resources/test.properties");
    assertTrue(str.contains("this as a resource"));
  }

  public void testResourceToStringBad() throws IOException {
    try {
      FileHelper.resourceToString("bad");
      fail();
    } catch (final RuntimeException e) {
      // expected
      assertEquals("Unable to find resource:bad", e.getMessage());
    }
  }

  public void testStreamToFile() throws IOException {
    final File f = FileHelper.createTempFile();
    try {
      assertEquals(f, FileHelper.streamToFile(new ByteArrayInputStream(new byte[0]), f));
      assertEquals(0, f.length());
      assertEquals(f, FileHelper.streamToFile(new ByteArrayInputStream(new byte[42]), f));
      assertEquals(42, f.length());
      try {
        FileHelper.streamToFile(null, f);
        fail();
      } catch (final NullPointerException e) {
        assertEquals("null stream given", e.getMessage());
      }
      try {
        FileHelper.streamToFile(new ByteArrayInputStream(new byte[0]), null);
        fail();
      } catch (final NullPointerException e) {
        assertEquals("null file given", e.getMessage());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(f));
    }
  }

  public void testStreamToGzFile() throws IOException {
    final File f = FileHelper.createTempFile();
    try {
      assertEquals(f, FileHelper.streamToGzFile(new ByteArrayInputStream(new byte[0]), f));
      assertTrue(f.exists());
      assertEquals(f, FileHelper.streamToGzFile(new ByteArrayInputStream(new byte[42]), f));
      assertTrue(f.length() > 0);
      assertEquals(42, FileHelper.gzFileToString(f).length());
      try {
        FileHelper.streamToFile(null, f);
        fail();
      } catch (final NullPointerException e) {
        assertEquals("null stream given", e.getMessage());
      }
      try {
        FileHelper.streamToFile(new ByteArrayInputStream(new byte[0]), null);
        fail();
      } catch (final NullPointerException e) {
        assertEquals("null file given", e.getMessage());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(f));
    }
  }

  public void testStringToGzFile() throws IOException {
    final File f = FileHelper.createTempFile();
    try {
      assertEquals(f, FileHelper.stringToGzFile("", f));
      assertEquals("", FileHelper.gzFileToString(f));
      assertEquals(f, FileHelper.stringToGzFile("hi", f));
      assertEquals("hi", FileHelper.gzFileToString(f));
      try {
        FileHelper.stringToGzFile(null, f);
        fail();
      } catch (final NullPointerException e) {
        assertEquals("null string given", e.getMessage());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(f));
    }
  }

  public void testDeleteAllBad() {
    assertTrue(FileHelper.deleteAll(new File("there-is-no-file-called-this-i-hope")));
  }

}
