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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.MD5Utils;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class FileUsageLoggingClientTest extends TestCase {

  public void test() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final String testData = "oogabooga" + StringUtils.LS + "boogly oogly" + StringUtils.LS + "S=xxxxx1234gogog" + StringUtils.LS;
      try (final RandomAccessFile raf = getRandomAccessFile(dir, testData)) {
        final String result = FileUsageLoggingClient.getLastKey(raf);
        assertEquals("xxxxx1234gogog", result);
      }
    }
  }

  public void test2() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final String testData = "oogabooga" + StringUtils.LS + "boogly oogly" + StringUtils.LS + "S=yyyysadfjkasdfk73251jk15sfad" + StringUtils.LS + "S=xxxxx1234gogog" + StringUtils.LS;
      try (final RandomAccessFile raf = getRandomAccessFile(dir, testData)) {
        final String result = FileUsageLoggingClient.getLastKey(raf);
        assertEquals("xxxxx1234gogog", result);
      }
    }
  }

  public void test3() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final String testData = "oogabooga" + StringUtils.LS + "boogly oogly" + StringUtils.LS + "S=yyyysadfjkasdfk73251jk15sfad" + StringUtils.LS + "S=xxxxx1234gogog" + StringUtils.LS + "more stuff that is irrelevant" + StringUtils.LS + "even more" + StringUtils.LS;
      try (final RandomAccessFile raf = getRandomAccessFile(dir, testData)) {
        final String result = FileUsageLoggingClient.getLastKey(raf);
        assertEquals("xxxxx1234gogog", result);
      }
    }
  }

  public void test4() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final String testData = "oogabooga" + StringUtils.LS + "boogly oogly" + StringUtils.LS + "S=yyyysadfjkasdfk73251jk15sfad" + StringUtils.LS + "S=xxxxx1234gogog";
      try (final RandomAccessFile raf = getRandomAccessFile(dir, testData)) {
        final String result = FileUsageLoggingClient.getLastKey(raf);
        assertEquals("xxxxx1234gogog", result);
      }
    }
  }

  public void testNoSig() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final String testData = "oogabooga" + StringUtils.LS + "boogly oogly" + StringUtils.LS + "SiganaturelyBadSpelling=yyyysadf";
      try (final RandomAccessFile raf = getRandomAccessFile(dir, testData)) {
        final String result = FileUsageLoggingClient.getLastKey(raf);
        assertNull(result);
      }
    }
  }

  public void test3Extreme() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final String testData = "oogabooga" + StringUtils.LS + "boogly oogly" + StringUtils.LS + "S=yyyysadfjkasdfk73251jk15sfad" + StringUtils.LS + "S=xxxxx1234gogog" + StringUtils.LS + "more stuff that is irrelevant" + StringUtils.LS + "even more" + StringUtils.LS;
      try (final RandomAccessFile raf = getRandomAccessFile(dir, testData)) {
        for (int i = 1; i < 11; ++i) {
          final String result = FileUsageLoggingClient.getLastKey(raf, i);
          assertEquals("xxxxx1234gogog", result);
        }
      }
    }
  }

  public void testMessageWriting() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, UsageConfigurationTest.createSimpleConfiguration(null, null, null, null, null), false);
      final UUID runId = UUID.randomUUID();
      client.recordBeginning("unitTest", runId);
      client.recordEnd(42, "unitTest", runId, true);
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      assertEquals(df.format(new Date()) + ".usage", usageOut.getName());
      final String str = FileUtils.fileToString(usageOut);
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "unitTest", "Start", "Success", runId.toString());
      final String[] outputLines = str.split("\r\n|\n");
      final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
      assertEquals(MD5Utils.md5(line1[0]), line1[1]);
      final String[] line2 = outputLines[1].split("\tS=");
      assertEquals(MD5Utils.md5(line1[1] + line2[0]), line2[1]);
    }
  }
  public void testMessageWritingOptionalFields() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      CommandLine.setCommandArgs("FirstArg1", "SecondArg2");
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, UsageConfigurationTest.createSimpleConfiguration(null, null, true, true, true), false);
      final UUID runId = UUID.randomUUID();
      client.recordBeginning("unitTest", runId);
      client.recordEnd(42, "unitTest", runId, true);
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      assertEquals(df.format(new Date()) + ".usage", usageOut.getName());
      final String str = FileUtils.fileToString(usageOut);
      final String hostName = Environment.getHostName();
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "unitTest", "Start", "Success", System.getProperty("user.name"), hostName.substring(0, Math.min(30, hostName.length())), "FirstArg1 SecondArg2",  runId.toString());
      CommandLine.clearCommandArgs();
      final String[] outputLines = str.split("\r\n|\n");
      final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
      assertEquals(MD5Utils.md5(line1[0]), line1[1]);
      final String[] line2 = outputLines[1].split("\tS=");
      assertEquals(MD5Utils.md5(line1[1] + line2[0]), line2[1]);
    }
  }

  public void testMessageWritingWithCorruption() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, new UsageConfiguration(new Properties()), false);
      final UUID runId = UUID.randomUUID();
      final Date date = new Date();
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      final String expectedFilename = df.format(date) + ".usage";
      client.recordBeginning("unitTest", runId);
      FileUtils.appendToFile(new File(dir, expectedFilename), "some stuff" + StringUtils.LS);
      client.recordEnd(42, "unitTest", runId, true);
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];
      assertEquals(expectedFilename, usageOut.getName());
      final String str = FileUtils.fileToString(usageOut);
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "unitTest", "Start", "Success", runId.toString());
      final String[] outputLines = str.split("\r\n|\n");
      final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
      assertEquals(MD5Utils.md5(line1[0]), line1[1]);
      final String[] line2 = outputLines[2].split("\tS="); //notice the subtle difference
      assertEquals(MD5Utils.md5(line1[1] + line2[0]), line2[1]);
    }
  }

  private RandomAccessFile getRandomAccessFile(TestDirectory dir, String testData) throws IOException {
    final File data = FileUtils.stringToFile(testData, new File(dir, "data"));
    return new RandomAccessFile(data, "r");
  }

  public void testPermissions() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, new UsageConfiguration(new Properties()), false);
      final UUID runId = UUID.randomUUID();
      final Date date = new Date();
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      final String expectedFilename = df.format(date) + ".usage";
      client.recordBeginning("unitTest", runId);
      FileUtils.appendToFile(new File(dir, expectedFilename), "some stuff" + StringUtils.LS);
      client.recordEnd(42, "unitTest", runId, true);
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];

      assertTrue(usageOut.canRead());
      assertTrue(usageOut.canWrite());

      try {
        final Path path = FileSystems.getDefault().getPath(usageOut.getCanonicalPath());
        final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);

        //System.err.println(perms);
        assertTrue(perms.contains(PosixFilePermission.GROUP_READ));
        assertTrue(perms.contains(PosixFilePermission.GROUP_WRITE));
        assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
        assertTrue(perms.contains(PosixFilePermission.OTHERS_READ));
        assertTrue(perms.contains(PosixFilePermission.OTHERS_WRITE));
      } catch (UnsupportedOperationException uoe) {
        // OK as file system may not be Posix compliant
      }
    }
  }

  public void testPermissions2() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      if (dir.setWritable(false, false)) { // may not work on Windows machines?
        final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, new UsageConfiguration(new Properties()), true);
        final UUID runId = UUID.randomUUID();
        try {
          client.recordBeginning("unitTest", runId);
        } catch (NoTalkbackSlimException fnfe) {
          // expected
          //System.err.println(fnfe.getMessage());
          assertTrue(fnfe.getMessage().contains("ermission denied") && fnfe.getMessage().contains("Failed to record usage information"));
        }
      }
    }
  }

  public void testWhenExpectedFileIsADirectory() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, new UsageConfiguration(new Properties()), true);
      final UUID runId = UUID.randomUUID();
      final Date date = new Date();
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      final String expectedFilename = df.format(date) + ".usage";
      if (!new File(dir, expectedFilename).mkdir()) {
        fail();
      }
      try {
        client.recordBeginning("unitTest", runId);
      } catch (final NoTalkbackSlimException e) {
        assertTrue(e.getMessage().contains("Failed to record usage information"));
        // expected
      }
    }
  }

  public void testUtterGarbageInFile() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, new UsageConfiguration(new Properties()), false);
      final UUID runId = UUID.randomUUID();
      final Date date = new Date();
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      final String expectedFilename = df.format(date) + ".usage";
      final Random r = new Random();
      try (final FileOutputStream fos = new FileOutputStream(new File(dir, expectedFilename))) {
        for (int k = 0; k < 10; ++k) {
          fos.write('S');
          fos.write('=');
          for (int c = 0; c < 500; ++c) {
            fos.write((char) r.nextInt(65536));
          }
        }
      }
      client.recordBeginning("unitTest", runId);
      client.recordEnd(42, "unitTest", runId, true);
    }
  }

  private static final String[] JUNK = {
    "some stuff",
    "\n\n\r\t \n\r\r",
    "\0\0",
    " ",
    "",
  };

  public void testMultipleMessageWriting() throws Exception {
    // Randomly write a mixture of open and close events and random junk
    try (TestDirectory dir = new TestDirectory()) {
      final FileUsageLoggingClient client = new FileUsageLoggingClient(dir, new UsageConfiguration(new Properties()), false);
      final Date date = new Date();
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      final String expectedFilename = df.format(date) + ".usage";
      final File file = new File(dir, expectedFilename);
      final Random r = new Random();
      final TreeSet<UUID> notYetEnded = new TreeSet<>();
      for (int k = 0; k < 200; ++k) {
        switch (r.nextInt(3)) {
          case 0:
            final UUID code = UUID.randomUUID();
            client.recordBeginning("unitTest", code);
            notYetEnded.add(code);
            break;
          case 1:
            final UUID closeCode = notYetEnded.pollFirst();
            if (closeCode != null) {
              client.recordEnd(r.nextInt(), "unitTest", closeCode, true);
            }
            break;
          default:
            FileUtils.appendToFile(file,  JUNK[r.nextInt(JUNK.length)] + StringUtils.LS);
            break;
        }
      }
      // Some messages will not be ended, but that is ok
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];
      assertEquals(df.format(new Date()) + ".usage", usageOut.getName());
      final String str = FileUtils.fileToString(usageOut);
      //System.out.println(str);
      final String[] outputLines = str.split("\r\n|\n");
      String prevHash = "";
      for (final String line : outputLines) {
        final String[] line1 = line.split("\tS="); //splits into message and md5 sum
        if (line1.length > 1) {
          assertEquals(MD5Utils.md5(prevHash + line1[0]), line1[1]);
          prevHash = line1[1];
        }
      }
    }
  }

}
