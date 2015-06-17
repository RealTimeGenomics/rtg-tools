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
package com.rtg.util.diagnostic;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import com.rtg.util.License;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogFile;
import com.rtg.util.io.LogStream;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.HttpServer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for the Talkback class.
 *
 */
public class TalkbackTest extends TestCase {

  /**
   */
  public TalkbackTest(final String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(TalkbackTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  @Override
  public void setUp() {
    CommandLine.setCommandArgs();
    Talkback.setModuleName("TalkbackTest");
    Talkback.setTalkback(true);
  }
  @Override
  public void tearDown() {
    Talkback.setModuleName(null);
    Talkback.setTalkback(false);
    Diagnostic.setLogStream();
  }

  public void testPostTalkback1() throws IOException {
    if (License.checkLicense()) {
      final File tmpDir = FileUtils.createTempDir("talkback", "pt1");
      try {

        final PrintStream olderr = System.err;
        final MemoryPrintStream mps = new MemoryPrintStream();
        System.setErr(mps.printStream());
        try {
          final File logFile = new File(tmpDir, "log");
          Diagnostic.setLogStream(new LogFile(logFile));
          final Map<String, String> got = new HashMap<>();
          final HttpServer h = createHttpServer(got, null);
          h.start();
          Talkback.setTalkbackURL("http://localhost:" + h.getPort() + "/talkback");
          try {
            assertTrue(Talkback.postTalkback(new RuntimeException(), true));
          } finally {
            h.stop();
          }
          assertTrue("expiry not in " + got.toString(), got.containsKey("expiry"));
          assertTrue("machine not in " + got.toString(), got.containsKey("machine"));
          assertTrue("subject not in " + got.toString(), got.containsKey("subject"));
          assertTrue("user not in " + got.toString(), got.containsKey("user"));
          assertTrue("not developer user in " + got.toString(), "1".equals(got.get("d")));
          TestUtils.containsAll(mps.toString(), "Sending talkback to Real Time Genomics (log", "Talkback successfully sent.");
          assertEquals(2, mps.toString().split(StringUtils.LS).length);
        } finally {
          System.setErr(olderr);
        }

      } finally {
        Diagnostic.setLogStream();
        FileHelper.deleteAll(tmpDir);
      }
    } else {
      System.err.println("WARNING: No key, talkback posting not tested");
    }
  }

  public void testPostTalkbackLogHandling() throws IOException {
    final Map<String, String> got = new HashMap<>();
    final Map<String, String> posted = new HashMap<>();

    final PrintStream olderr = System.err;
    final MemoryPrintStream mps = new MemoryPrintStream();
    System.setErr(mps.printStream());
    try {

      HttpServer h = createHttpServer(got, posted);
      h.start();
      Talkback.setTalkbackURL("http://localhost:" + h.getPort() + "/talkback");
      try {
        assertTrue(Talkback.postTalkback(new RuntimeException(), true));
      } finally {
        h.stop();
      }
      assertEquals("<no log stream set>", posted.get("log"));
      TestUtils.containsAll(mps.toString(), "Sending talkback to Real Time Genomics...", "Talkback successfully sent.");
      assertEquals(2, mps.toString().split(StringUtils.LS).length);
      mps.reset();

      final File logFile = File.createTempFile("talkback_test", "log");
      final PrintStream logStream = new PrintStream(logFile);
      Diagnostic.setLogStream(new LogStream() {
        @Override
        public PrintStream stream() {
          return logStream;
        }

        @Override
        public void close() {
          logStream.close();
        }

        @Override
        public void removeLog() {
        }

        @Override
        public File file() {
          return logFile;
        }
      });

      try {
        got.clear();
        posted.clear();
        h = createHttpServer(got, posted);
        h.start();
        Talkback.setTalkbackURL("http://localhost:" + h.getPort() + "/talkback");
        Diagnostic.userLog("hello world");
        try {
          assertTrue(Talkback.postTalkback(new RuntimeException(), true));
        } finally {
          h.stop();
        }

        assertTrue(posted.get("log").endsWith("hello world" + StringUtils.LS));
        assertEquals(31 + StringUtils.LS.length(), posted.get("log").length());

        TestUtils.containsAll(mps.toString(), "Sending talkback to Real Time Genomics (log", "Talkback successfully sent.");
        assertEquals(2, mps.toString().split(StringUtils.LS).length);
        mps.reset();

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 25000; i++) {
          sb.append("A");
        }

        got.clear();
        posted.clear();
        h = createHttpServer(got, posted);
        h.start();
        Talkback.setTalkbackURL("http://localhost:" + h.getPort() + "/talkback");
        Diagnostic.userLog(sb.toString());
        try {
          assertTrue(Talkback.postTalkback(new RuntimeException(), true));
        } finally {
          h.stop();
        }
        assertEquals(25051 + 2 * StringUtils.LS.length(), posted.get("log").length());
        TestUtils.containsAll(mps.toString(), "Sending talkback to Real Time Genomics (log", "Talkback successfully sent.");
        assertEquals(2, mps.toString().split(StringUtils.LS).length);
        mps.reset();

        got.clear();
        posted.clear();
        h = createHttpServer(got, posted);
        h.start();
        Talkback.setTalkbackURL("http://localhost:" + h.getPort() + "/talkback");
        Diagnostic.userLog(sb.toString());
        try {
          assertTrue(Talkback.postTalkback(new RuntimeException(), true));
        } finally {
          h.stop();
        }
        assertEquals(40056, posted.get("log").length());
        TestUtils.containsAll(mps.toString(), "Sending talkback to Real Time Genomics (log", "The log file is oversized, sending truncated log.  Please send the full log file", "Talkback successfully sent.");
        assertEquals(3, mps.toString().split(StringUtils.LS).length);
        mps.reset();
      } finally {
        logStream.close();
        assertTrue(logFile.delete());
      }

    } finally {
      System.setErr(olderr);
    }

  }

  private static HttpServer createHttpServer(final Map<String, String> got, final Map<String, String> posted) throws IOException {
    final HttpServer h = new HttpServer();
    h.setHandler(new HttpServer.Handler() {
      @Override
      public void doPage(PrintStream out, String request, Map<String, String> headers, Map<String, String> get, Map<String, String> post) {
        out.println("HTTP/1.0 " + HttpURLConnection.HTTP_OK);
        out.println("Content-Type: text/plain");
        out.println();
        out.println();
        if (got != null) {
          for (final Map.Entry<String, String> entry : get.entrySet()) {
            got.put(entry.getKey(), entry.getValue());
          }
        }
        if (posted != null) {
          for (final Map.Entry<String, String> entry : post.entrySet()) {
            posted.put(entry.getKey(), entry.getValue());
          }
        }
      }
    });
    return h;
  }

}


