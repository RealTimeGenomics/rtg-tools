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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

import junit.framework.TestCase;

/**
 */
public class HttpUsageLoggingClientTest extends TestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Diagnostic.setLogStream();
  }

  private static final String URL = "http://localhost:8080/usage";

  public void test() {
    final MyHttpUsageLoggingClient client = new MyHttpUsageLoggingClient(HttpUsageLoggingClientTest.URL, false, 3);
    client.recordBeginning("test mod", UUID.randomUUID());
    assertEquals(3, client.mTries);
  }

  public void testRequired() {
    final MyHttpUsageLoggingClient client = new MyHttpUsageLoggingClient(HttpUsageLoggingClientTest.URL, true, 3);
    try {
      client.recordBeginning("test mod", UUID.randomUUID());
      fail();
    } catch (NoTalkbackSlimException e) {
      //expected
    }
    assertEquals(3, client.mTries);
  }


  public void testMessagesWithSuccess() {
    final MyDiagnosticListener dl = new MyDiagnosticListener(null);
    Diagnostic.addListener(dl);
    try {
      final String username = System.getProperty("user.name");
      final byte[] newname = new byte[UsageMessage.USERNAME_TRIM_LENGTH + 3];
      try {
        System.setProperty("user.name", new String(newname));
        final MyHttpUsageLoggingClient client = new MyHttpUsageLoggingClient(HttpUsageLoggingClientTest.URL, false, 2);
        client.recordBeginning("test mod", UUID.randomUUID());
        assertEquals(3, client.mTries);
        assertEquals(0, dl.mMsgNo);
      } finally {
        System.setProperty("user.name", username);
      }
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testMessagesWithFailure() {
    final MyDiagnosticListener dl = new MyDiagnosticListener(new String[][] {
        {"Failed to send"},
        {"Gave up after 3"}
    });
    Diagnostic.addListener(dl);
    try {
      final String username = System.getProperty("user.name");
      final byte[] newname = new byte[UsageMessage.USERNAME_TRIM_LENGTH + 3];
      try {
        System.setProperty("user.name", new String(newname));
        final MyHttpUsageLoggingClient client = new MyHttpUsageLoggingClient("http://localhost:1000/", false, 3);
        client.recordBeginning("test mod", UUID.randomUUID());
        assertEquals(3, client.mTries);
        assertEquals(2, dl.mMsgNo);
      } finally {
        System.setProperty("user.name", username);
      }
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  private static class MyDiagnosticListener implements DiagnosticListener {
    final String[][] mWarningMsgs;
    int mMsgNo = 0;
    MyDiagnosticListener(String[][] expected) {
      mWarningMsgs = expected;
    }
    @Override
    public void handleDiagnosticEvent(DiagnosticEvent <?> event) {
      if (!event.getMessage().contains("at com.rtg.usage")) { // Ignore debugging stack traces
        for (String msgBit : mWarningMsgs[mMsgNo]) {
          assertTrue("'" + msgBit + "' was not contained in: '" + event.getMessage() + "'", event.getMessage().contains(msgBit));
        }
        mMsgNo++;
      }
    }

    @Override
    public void close() {
    }
  }

  private static class MyHttpUsageLoggingClient extends HttpUsageLoggingClient {
    final String mHost;
    final int mNumFailures;
    int mTries = 0;
    MyHttpUsageLoggingClient(String host, boolean required, int numFailures) {
      super(host, UsageConfigurationTest.createSimpleConfiguration(null, null, null, null, null), required);
      mHost = host;
      mNumFailures = numFailures;
    }

    @Override
    int getRetryTimeInMillis() {
      return 1; //make tests fast
    }

    @Override
    HttpURLConnection openConnection() throws IOException {
      return new HttpURLConnection(new URL(mHost)) {
        @Override
        public OutputStream getOutputStream() throws IOException {
          connect();
          return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
          return new ByteArrayInputStream(UsageServer.RTG_USAGE_ACCEPT.getBytes());
        }

        @Override
        public int getResponseCode() {
          return 200;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
          return false;
        }

        @Override
        public void connect() throws IOException {
          if (mTries++ < mNumFailures) {
            throw new IOException("Woooo");
          }
        }
      };
    }
  }
}
