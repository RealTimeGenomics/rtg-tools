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
package com.rtg.usage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.MD5Utils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class UsageServerTest extends TestCase {

  public void test1() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final UUID runId = runUsageTest1(dir);
      checkUsageTest1(dir, runId, 2);
      final UUID runId2 = runUsageTest1(dir);
      checkUsageTest1(dir, runId2, 4);
    }
  }

  private void checkUsageTest1(TestDirectory dir, UUID runId, int expectedLines) throws IOException {
    final File[] files = dir.listFiles();
    assertNotNull(files);
    assertEquals(1, files.length);
    final File usageOut = files[0];
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
    final Date date = new Date();
    assertEquals(df.format(date) + ".usage", usageOut.getName());
    final String str = FileUtils.fileToString(usageOut);
    TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule", "Start", "Success", runId.toString());
    final String[] outputLines = str.split("\r\n|\n");
    checkIntegrity(expectedLines, outputLines);
  }

  private void checkIntegrity(int expectedLines, String[] outputLines) {
    assertEquals(Arrays.toString(outputLines), expectedLines, outputLines.length);
    String prevMd5 = null;
    for (String outputLine : outputLines) {
      final String[] line = outputLine.split("\tS="); //splits into message and md5 sum
      final String expectedMd5 = prevMd5 == null ? MD5Utils.md5(line[0]) : MD5Utils.md5(prevMd5 + line[0]);
      prevMd5 = expectedMd5;
      assertEquals(expectedMd5, line[1]);
    }
  }

  private UUID runUsageTest1(TestDirectory dir) throws IOException {
    final UsageServer us = new UsageServer(0, dir, 4);
    us.setStopTimer(1);
    us.start();
    final HttpUsageLoggingClient http = new HttpUsageLoggingClient("http://localhost:" + us.getPort() + "/", UsageConfigurationTest.createSimpleConfiguration(null, null, null, null, null), false);
    final UUID runId = UUID.randomUUID();
    http.recordBeginning("testModule", runId);
    http.recordEnd(65, "testModule", runId, true);
    //stupidWorkaround(http);
    us.end();
    return runId;
  }

  public void testWithOptionalFields() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final UsageServer us = new UsageServer(0, dir, 4);
      us.start();
      CommandLine.setCommandArgs("FirstArg1", "SecondArg2");
      final HttpUsageLoggingClient http = new HttpUsageLoggingClient("http://localhost:" + us.getPort() + "/usage", UsageConfigurationTest.createSimpleConfiguration(null, null, true, true, true), false);
      final UUID runId = UUID.randomUUID();
      http.recordBeginning("testModule", runId);
      http.recordEnd(65, "testModule", runId, true);
      stupidWorkaround(http);
      us.end();
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      assertEquals(df.format(new Date()) + ".usage", usageOut.getName());
      final String str = FileUtils.fileToString(usageOut);
      final String hostName = Environment.getHostName();
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule", "Start", "Success", System.getProperty("user.name"), hostName.substring(0, Math.min(30, hostName.length())), "FirstArg1 SecondArg2", runId.toString());
      CommandLine.clearCommandArgs();
      final String[] outputLines = str.split("\r\n|\n");
      final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
      assertEquals(MD5Utils.md5(line1[0]), line1[1]);
      final String[] line2 = outputLines[1].split("\tS=");
      assertEquals(MD5Utils.md5(line1[1] + line2[0]), line2[1]);
    }
  }

  public void testErrors() throws Exception {
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    try {
      try (TestDirectory dir = new TestDirectory()) {
        final UsageServer us = new UsageServer(0, dir, 4);
        us.start();
        final HttpURLConnection http = (HttpURLConnection) new URL("http://localhost:" + us.getPort() + "/usage").openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream os = http.getOutputStream()) {
          os.write(("f%ag=ra%3d&"
                  + "&" + UsageServer.TYPE + "=Start"
                  + "&" + UsageServer.MODULE + "=testError"
                  + "&" + UsageServer.VERSION + "=moo"
                  + "&" + UsageServer.RUN_ID + "=91b80b67-1861-4ab8-a3bc-84bfcab7543c"
                  + "&" + UsageServer.SERIAL + "=599").getBytes());
        }
        final int code = http.getResponseCode();
        assertEquals(200, code);
        final String resp = IOUtils.readAll(http.getInputStream());
        assertEquals("RTG Usage Accept", resp);
        stupidWorkaround(new HttpUsageLoggingClient("http://localhost:" + us.getPort() + "/usage", UsageConfigurationTest.createSimpleConfiguration(null, null, null, null, null), false));
        us.end();
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        final File usageOut = files[0];
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
        assertEquals(df.format(new Date()) + ".usage", usageOut.getName());
        final String str = FileUtils.fileToString(usageOut);
        TestUtils.containsAll(str, "599", "moo", "testError", "Start", "91b80b67-1861-4ab8-a3bc-84bfcab7543c");
        final String[] outputLines = str.split("\r\n|\n");
        final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
        assertEquals(MD5Utils.md5(line1[0]), line1[1]);
        assertTrue(mps.toString().contains("Failed to decode parameter: 'f%ag=ra%3d'"));
        //final HttpUsageTrackingClient http = new HttpUsageTrackingClient("http://localhost:" + us.getPort() + "/usage", new UsageConfiguration(new Properties()), false);
      }
    } finally {
      Diagnostic.setLogStream();
    }
  }

  public void testDateRollover() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final SimpleDateFormat idf = new SimpleDateFormat("yyyy-MM-dd");
      final Date[] dates = {idf.parse("2012-01-15"), idf.parse("2012-02-15"), idf.parse("2012-03-15"), idf.parse("2060-01-01")};
      final UsageServer us = new UsageServer(0, dir, 4) {
        private int mI;
        @Override
        synchronized Date getDate() {
          return dates[mI++];
        }
      };

      us.start();
      final HttpUsageLoggingClient http = new HttpUsageLoggingClient("http://localhost:" + us.getPort() + "/usage", new UsageConfiguration(new Properties()), true);
      final UUID runId = UUID.randomUUID();
      http.recordBeginning("testModule", runId);
      http.recordEnd(65, "testModule", runId, true);
      stupidWorkaround(http);
      us.end();
      final File[] files = dir.listFiles();
      assertNotNull(files);
      Arrays.sort(files);
      assertTrue(3 == files.length || 4 == files.length); //allowing 4 files since the stupidWorkaround may create one
      assertEquals("2012-01.usage", files[0].getName());
      assertEquals("2012-02.usage", files[1].getName());
      assertEquals("2012-03.usage", files[2].getName());
      String str = FileUtils.fileToString(files[0]);
      assertEquals(0, str.length()); //first getDate() is called in constructor
      str = FileUtils.fileToString(files[1]); //second for first message
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule", "Start", runId.toString());
      final String[] outputLines = str.split("\r\n|\n");
      str = FileUtils.fileToString(files[2]); //third for second message
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule", "Success", runId.toString());
      final String[] outputLines2 = str.split("\r\n|\n");
      final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
      assertEquals(MD5Utils.md5(line1[0]), line1[1]);
      final String[] line2 = outputLines2[0].split("\tS=");
      assertEquals(MD5Utils.md5(line2[0]), line2[1]);
    }
  }

  public void testWobblyDateRollover() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final SimpleDateFormat idf = new SimpleDateFormat("yyyy-MM-dd");
      final Date[] dates = {idf.parse("2011-01-01"), idf.parse("2012-01-31"), idf.parse("2012-02-01"), idf.parse("2012-01-31"), idf.parse("2012-02-01"), idf.parse("2060-01-01")};
      final UsageServer us = new UsageServer(0, dir, 4) {
        private int mI;
        @Override
        synchronized Date getDate() {
          return dates[mI++];
        }
      };

      us.start();
      final HttpUsageLoggingClient http = new HttpUsageLoggingClient("http://localhost:" + us.getPort() + "/usage", new UsageConfiguration(new Properties()), true);
      final UUID runId = UUID.randomUUID();
      http.recordBeginning("testModule1", runId);
      http.recordEnd(65, "testModule1", runId, true);
      final UUID runId2 = UUID.randomUUID();
      http.recordBeginning("testModule2", runId2);
      http.recordEnd(77, "testModule2", runId2, false);
      stupidWorkaround(http);
      us.end();
      final File[] files = dir.listFiles();
      assertNotNull(files);
      Arrays.sort(files);
      assertTrue(3 == files.length || 4 == files.length); //allowing 4 files since the stupidWorkaround may create one
      assertEquals("2011-01.usage", files[0].getName());
      assertEquals("2012-01.usage", files[1].getName());
      assertEquals("2012-02.usage", files[2].getName());
      String str = FileUtils.fileToString(files[0]);
      assertEquals(0, str.length()); //first getDate() is called in constructor
      str = FileUtils.fileToString(files[1]); //second for first message and same date for 3rd message
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule1", "testModule2", "Start", runId.toString(), runId2.toString());
      final String[] outputLines1 = str.split("\r\n|\n");
      str = FileUtils.fileToString(files[2]); //messages 2 and 4
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule1", "testModule2", "Success", "Fail", "65", "77", runId.toString(), runId2.toString());
      final String[] outputLines2 = str.split("\r\n|\n");
      checkIntegrity(2, outputLines1);
      checkIntegrity(2, outputLines2);
    }
  }

  private void stupidWorkaround(final HttpUsageLoggingClient client) {
    //this here is because the SUN HttpServer says that its stop(int delay) stops after delay seconds or when all HttpExchanges have been handled
    //whichever is sooner. Unfortunately it only checks if all HttpExchanges have been handled once it receives another connection attempt...
    //so .... yeah, this is the connection being made to prompt it to stop when it can.
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.yield();
          Thread.sleep(200);
        } catch (InterruptedException e) {

        }

        try {
          HttpURLConnection http = client.openConnection();
          http.setConnectTimeout(1000);
          http.setReadTimeout(1000);
          http.setRequestMethod("GET");
          http.setDoOutput(true);
          try (OutputStream os = http.getOutputStream()) {
            os.write("Len says shutdown".getBytes());
          }
          http.getResponseCode();
          http.disconnect();
        } catch (IOException e) {
          // Don't care
        }
      }
    }).start();
  }
}
