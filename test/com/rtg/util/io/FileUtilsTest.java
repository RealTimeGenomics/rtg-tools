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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.test.FileHelper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit tests for the FileUtils class.
 *
 */
public class FileUtilsTest extends TestCase {

  protected File mTmp = null;

  /**
   * Constructor (needed for JUnit)
   *
   * @param name A string which names the object.
   */
  public FileUtilsTest(final String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    Diagnostic.setLogStream();
    mTmp = File.createTempFile("FileUtils", "test");
  }

  @Override
  public void tearDown() {
    if (mTmp != null) {
      assertTrue(mTmp.delete());
    }
    mTmp = null;
  }

  public void testPatchURLForUNC() {
    assertEquals("file://\\\\10.65.1.7/share/a.html", FileUtils.patchURLForUNC("file://10.65.1.7/share/a.html"));
    assertEquals("file:/10.65.1.7/share/a.html", FileUtils.patchURLForUNC("file:/10.65.1.7/share/a.html"));
    assertEquals("file:///10.65.1.7/share/a.html", FileUtils.patchURLForUNC("file:///10.65.1.7/share/a.html"));
    assertEquals("http://10.65.1.7/share/a.html", FileUtils.patchURLForUNC("http://10.65.1.7/share/a.html"));
  }

  public void testCheckFile() {
    assertTrue(FileUtils.checkFile(mTmp));
    final String mTmpName = mTmp.getPath();
    assertTrue(FileUtils.checkFile(mTmpName));

    assertTrue(!FileUtils.checkFile(mTmp.getParent()));
    assertTrue(!FileUtils.checkFile(mTmp.getParentFile()));

    assertTrue(!FileUtils.checkFile("Balony"));
    assertTrue(!FileUtils.checkFile(new File("Balony")));
  }

  public void testCheckDir() {
    assertTrue(!FileUtils.checkDir(mTmp));
    assertTrue(!FileUtils.checkDir(mTmp.getPath()));

    assertTrue(FileUtils.checkDir(mTmp.getParent()));
    assertTrue(FileUtils.checkDir(mTmp.getParentFile()));

    assertTrue(!FileUtils.checkDir("Balony"));
    assertTrue(!FileUtils.checkDir(new File("Balony")));
  }

  public void testStreamToFile() throws Exception {
    final byte[] bytes = new byte[100];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (i % 11 + 1);
    }

    File f = FileHelper.createTempFile();
    try {
      f = FileHelper.streamToFile(new ByteArrayInputStream(bytes), f);
      assertNotNull(f);
      assertTrue(f.exists());
      assertTrue(f.isFile());
      assertTrue(f.canRead());
      assertEquals(bytes.length, f.length());
      try (FileInputStream fis = new FileInputStream(f)) {
        final byte[] res = IOUtils.readData(fis);
        assertEquals(bytes.length, res.length);
        for (int i = 0; i < bytes.length; i++) {
          assertEquals("" + i, bytes[i], res[i]);
        }
      }
    } finally {
      assertTrue(f.delete());
    }
  }

  public void testByteArrayToFile() throws Exception {
    final byte[] bytes = new byte[100];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (i % 11 + 1);
    }
    File f = FileHelper.createTempFile();
    try {
      f = FileUtils.byteArrayToFile(bytes, f);

      assertNotNull(f);
      assertTrue(f.exists());
      assertTrue(f.isFile());
      assertTrue(f.canRead());
      assertEquals(bytes.length, f.length());
      try (FileInputStream fis = new FileInputStream(f)) {
        final byte[] res = IOUtils.readData(fis);
        assertEquals(bytes.length, res.length);
        for (int i = 0; i < bytes.length; i++) {
          assertEquals("" + i, bytes[i], res[i]);
        }
      }
    } finally {
      assertTrue(f.delete());
    }
  }

  public void testStringToFile() throws Exception {
    final String test = "A test string...";
    File f = FileHelper.createTempFile();
    try {
      f = FileUtils.stringToFile(test, f);

      assertNotNull(f);
      assertTrue(f.exists());
      assertTrue(f.isFile());
      assertTrue(f.canRead());
      assertEquals(test.length(), f.length());
      try (FileInputStream fis = new FileInputStream(f)) {
        assertEquals(test, IOUtils.readAll(fis));
      }
    } finally {
      assertTrue(f.delete());
    }
  }

  public void testStringToGzFile() throws Exception {
    final String test = "A test string...";
    final File f = FileHelper.createTempFile();
    try {
      assertSame(f, FileHelper.stringToGzFile(test, f));
      assertNotNull(f);
      assertTrue(f.exists());
      assertTrue(f.isFile());
      assertTrue(f.canRead());
      final String result = FileHelper.gzFileToString(f);
      assertEquals(test.length(), result.length());
    } finally {
      assertTrue(f.delete());
    }
  }

  public void testIsNonEmpty() throws Exception {
    File empty = FileHelper.createTempFile();
    File nonempty = FileHelper.createTempFile();
    try {
      empty = FileUtils.stringToFile("", empty);
      nonempty = FileUtils.stringToFile("abc", nonempty);
      assertFalse(FileUtils.isNonEmpty(empty.getAbsolutePath()));
      assertTrue(FileUtils.isNonEmpty(nonempty.getAbsolutePath()));
    } finally {
      assertTrue(empty.delete());
      assertTrue(nonempty.delete());
    }
  }

  private void checkFileToString(final String str) throws IOException {
    File file = null;
    try {
      file = File.createTempFile("test", null);
      FileUtils.stringToFile(str, file);
      final String actual = FileUtils.fileToString(file);
      assertEquals(str, actual);
      final String actual2 = FileUtils.fileToString(file.getAbsoluteFile());
      assertEquals(str, actual2);
      try (FileReader r = new FileReader(file)) {
        final String actual3 = FileHelper.readerToString(r);
        assertEquals(str, actual3);
      }
      final String actual4 = FileUtils.fileToString(file.toString());
      assertEquals(str, actual4);
    } finally {
      FileHelper.deleteAll(file);
    }
  }

  public void testFileToString() throws IOException {
    checkFileToString("");
    checkFileToString(com.rtg.util.StringUtils.LS);
    checkFileToString("fobar baz");
    checkFileToString("fobar baz" + com.rtg.util.StringUtils.LS);
  }

  public void testStreamToString() throws IOException {
    checkStreamToString("");
    checkStreamToString("abc\t\r\n");
  }

  private void checkStreamToString(final String str) throws IOException {
    final byte[] ba = new byte[str.length()];
    for (int i = 0; i < str.length(); i++) {
      ba[i] = (byte) str.charAt(i);
    }
    final InputStream instr = new ByteArrayInputStream(ba);
    final String res = FileUtils.streamToString(instr);
    assertEquals(str, res);
    instr.close();
  }

  public void testStreamToStringBad() throws IOException {
    try {
      FileUtils.streamToString(null);
      fail();
    } catch (final NullPointerException e) {
      // expected
      assertTrue(e.getMessage().contains("null stream given"));
    }
  }

  public void testDelete() throws IOException {
    final File file = File.createTempFile("test", null);
    assertTrue(file.exists());
    FileHelper.deleteAll(file);
    assertFalse(file.exists());
    FileHelper.deleteAll(null);
  }

  public void testIsResultDirectoryValid() throws IOException {
    Diagnostic.setLogStream();
    final File tempDir = FileUtils.createTempDir("junit", "test");
    try {
      final File t = new File(tempDir, "junithi");
      FileUtils.ensureOutputDirectory(t);
      FileUtils.ensureOutputDirectory(t);
      final File l = FileUtils.stringToFile("file test", new File(tempDir, "file"));
      try {
        FileUtils.ensureOutputDirectory(l);
        fail();
      } catch (final NoTalkbackSlimException e) {
      }
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  public void testGetProcessId() {
    final Integer pid = FileUtils.getProcessId();
    //System.err.println("pid=" + pid);
    assertTrue(pid > 0);
  }

  public void testMakeBuffer() {
    final char[] chars = FileUtils.makeBuffer();
    assertTrue(chars != null && chars.length == 8192);
  }

  public void testMakeByteBuffer() {
    final byte[] bytes = FileUtils.makeByteBuffer();
    assertTrue(bytes != null && bytes.length == 8192);
  }

  public void testOutputStream() throws Exception {
    // will NPE if method fails
    FileUtils.getStdoutAsOutputStream().close();
  }

  /*
   //for showing doesn't work in .NET
  public void testBigZip() {
    final long fourMb = 4 * 1024 * 1024;
    final File dir = FileUtils.createTempDir("bigziptest", "big");
    try {
      final File out = new File(dir, "4gb.gz");
      final OutputStream outS = new GZIPOutputStream(new FileOutputStream(out));
      try {
        final byte[] buf = new byte[1024];
        for (int i = 0; i < fourMb; i++) {
          outS.write(buf);
        }
      } finally {
        outS.close();
      }
    } finally {
      FileUtils.deleteAll(dir);
    }
  }
  */

  public static Test suite() {
    return new TestSuite(FileUtilsTest.class);
  }

  public void testCattedGZipFiles() throws IOException {
    final File tmpDir = FileUtils.createTempDir("testGzipCatting", "test");
    try {
      final File zip1 = new File(tmpDir, "1");
      final File zip2 = new File(tmpDir, "2");
      final File zip3 = new File(tmpDir, "3");
      final File joined = new File(tmpDir, "joined");
      FileHelper.stringToGzFile("this is\n", zip1);
      FileHelper.stringToGzFile("a multi part\n", zip2);
      FileHelper.stringToGzFile("gzipped file\n", zip3);
      FileUtils.catInSync(joined, false, zip1, zip2, zip3);
      assertEquals("this is\na multi part\ngzipped file\n", FileHelper.gzFileToString(joined));
    } finally {
      FileHelper.deleteAll(tmpDir);
    }

  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }


  private static class NonSkippingStream extends InputStream {
    private final InputStream mStream;
    public NonSkippingStream(InputStream stream) {
      mStream = stream;
    }

    @Override
    public int read() throws IOException {
      return mStream.read();
    }

    /**
     */
    @Override
    public int read(byte[] a, int b, int c) throws IOException {
      return mStream.read(a, b, c);
    }

    @Override
    public long skip(long count) {
      return 0;
    }
  }
  public void testStreamSkip() throws IOException {
    final InputStream data = new NonSkippingStream(new ByteArrayInputStream("This is some data".getBytes()));
    long remaining = 4;
    long skipped;
    while (remaining > 0 && (skipped = FileUtils.streamSkip(data, remaining)) > 0) {
      remaining -= skipped;
    }
    assertEquals(" is some data", FileHelper.readerToString(new InputStreamReader(data)));
  }

  public void testMassiveStreamSkip() throws IOException {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1111; i++) {
      sb.append('Z');
    }
    sb.append("rest of string");
    final InputStream data = new ByteArrayInputStream(sb.toString().getBytes());
    long remaining = 1111;
    long skipped;
    while (remaining > 0 && (skipped = FileUtils.streamSkip(data, remaining)) > 0) {
      remaining -= skipped;
    }
    assertEquals("rest of string", FileHelper.readerToString(new InputStreamReader(data)));
  }

  public void testTooBigSkip() throws IOException {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 11; i++) {
      sb.append('Z');
    }
    sb.append("rest of string");
    final InputStream data = new ByteArrayInputStream(sb.toString().getBytes());
    long remaining = 1111;
    long skipped;
    while (remaining > 0 && (skipped = FileUtils.streamSkip(data, remaining)) > 0) {
      remaining -= skipped;
    }
    assertTrue(remaining > 0);
    assertEquals("", FileHelper.readerToString(new InputStreamReader(data)));
  }

  public void testSeek() throws IOException {
    final StringBuilder sb = new StringBuilder();
    final String expected = "rest of string";
    for (int i = 0; i < 11; i++) {
      sb.append('Z');
    }
    sb.append(expected);
    final InputStream data = new ByteArrayInputStream(sb.toString().getBytes());
    FileUtils.skip(data, 11);
    assertEquals(expected, FileHelper.readerToString(new InputStreamReader(data)));

  }

  public void testSeekEOF() throws IOException {
    final StringBuilder sb = new StringBuilder();
    final String expected = "rest of string";
    for (int i = 0; i < 11; i++) {
      sb.append('Z');
    }
    sb.append(expected);
    final InputStream data = new ByteArrayInputStream(sb.toString().getBytes());
    try {
      FileUtils.skip(data, 101);
      fail();
    } catch (final EOFException e) {
      //expected
    }
  }

  public void testGetIntFromFile() throws IOException {
    try (DataOutputStream fos = new DataOutputStream(new FileOutputStream(mTmp))) {
      for (int i = 0; i < 20; i++) {
        fos.writeInt(i);
      }
    }
    for (int i = 0; i < 20; i++) {
      assertEquals(i, FileUtils.getIntFromFile(mTmp, i));
    }
    try {
      FileUtils.getIntFromFile(mTmp, 21);
      fail();
    } catch (final EOFException e) {
      // expected
    }
  }

  public void testIsGzipFile() throws IOException {
    final File gzip = File.createTempFile("FileUtilsTest", "testIsGzipFile");
    try {
      try (OutputStream out = FileUtils.createOutputStream(gzip, true)) {
        out.write("I am gzipped".getBytes());
      }
      assertTrue(gzipFile(gzip));
    } finally {
      FileHelper.deleteAll(gzip);
    }
  }

  public void testIsNotGzipFile() throws IOException {
    final File gzip = File.createTempFile("FileUtilsTest", "testIsNotGzipFile");
    try {
      try (FileWriter fw = new FileWriter(gzip)) {
        fw.write("I am not gzipped");
      }
      assertFalse(gzipFile(gzip));
    } finally {
      FileHelper.deleteAll(gzip);
    }
  }

  public boolean gzipFile(File f) throws IOException {
    final boolean ret;
    try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(f))) {
      ret = FileUtils.isGzipFile(is);
    }
    return ret;
  }

  public void testGetZippedFileName() {
    assertEquals("test.gz", FileUtils.getZippedFileName(true, new File("test")).getName());
    assertEquals("test", FileUtils.getZippedFileName(false, new File("test")).getName());
    assertEquals("test.gz", FileUtils.getZippedFileName(true, new File("test.gz")).getName());
    assertEquals("test", FileUtils.getZippedFileName(false, new File("test.gz")).getName());
  }

  public void testCopyResource() throws IOException {
    try (TestDirectory td = new TestDirectory()) {
      final File f = new File(td, "blah");
      FileUtils.copyResource("com/rtg/util/resources/krona.xml", f);
      final String fstr = FileUtils.fileToString(f);
      assertTrue(fstr.startsWith("<!DOCTYPE html PUBLIC"));
      assertTrue(fstr.endsWith("</div></body></html>"));
    }
  }
}
