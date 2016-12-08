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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.MemoryPrintStream;

/**
 * Client for talking to <code>http</code> based usage logging server
 */
@TestClass(value = {"com.rtg.usage.UsageServerTest", "com.rtg.usage.HttpUsageLoggingClientTest"})
public class HttpUsageLoggingClient implements UsageLoggingClient {

  private static final int DEFAULT_RETRY_WAIT = 1000;
  private static final int NUM_TRIES = 3;
  private final String mHost;
  private final UsageConfiguration mUsageConfiguration;
  private final boolean mRequireUsage;
  private boolean mStartSent = false;

  /**
   * @param host <code>url</code> for posting usage to
   * @param conf the usage configuration (for user and host name logging options)
   * @param requireUsage true if a failed message should be treated as an error
   */
  public HttpUsageLoggingClient(String host, UsageConfiguration conf, boolean requireUsage) {
    mHost = host;
    mUsageConfiguration = conf;
    mRequireUsage = requireUsage;
  }

  @Override
  public void recordBeginning(String module, UUID runId) {
    final HashMap<String, String> values = new HashMap<>();
    values.put(UsageServer.RUN_ID, runId.toString());
    values.put(UsageServer.TYPE, "Start");
    values.put(UsageServer.MODULE, module);
    //values.put(UsageServer.METRIC, "N/A");
    setCommon(values);
    mStartSent = sendMessage(values);
  }

  @Override
  public void recordEnd(long metric, String module, UUID runId, boolean success) {
    if (mStartSent) {
      final HashMap<String, String> values = new HashMap<>();
      values.put(UsageServer.RUN_ID, runId.toString());
      values.put(UsageServer.MODULE, module);
      values.put(UsageServer.TYPE, success ? "Success" : "Fail");
      values.put(UsageServer.METRIC, Long.toString(metric));
      setCommon(values);
      sendMessage(values);
    }
  }

  private void setCommon(Map<String, String> map) {
    if (mUsageConfiguration.logUsername()) {
      map.put(UsageServer.USERNAME, UsageMessage.trimField(System.getProperty("user.name"), UsageMessage.USERNAME_TRIM_LENGTH));
    }
    if (mUsageConfiguration.logHostname()) {
      map.put(UsageServer.HOSTNAME, UsageMessage.trimField(Environment.getHostName(), UsageMessage.HOSTNAME_TRIM_LENGTH));
    }
    if (mUsageConfiguration.logCommandLine()) {
      map.put(UsageServer.COMMANDLINE, UsageMessage.trimField(CommandLine.getCommandLine(), UsageMessage.COMMANDLINE_TRIM_LENGTH));
    }
    map.put(UsageServer.SERIAL, License.getSerialNumber());
    map.put(UsageServer.VERSION, Environment.getVersion());
  }

  HttpURLConnection openConnection() throws IOException {
    return (HttpURLConnection) new URL(mHost).openConnection();
  }

  int getRetryTimeInMillis() {
    return DEFAULT_RETRY_WAIT;
  }

  private boolean sendMessage(Map<String, String> values) {
    boolean success = false;
    String failureMessage = "";
    for (int t = 0; t < NUM_TRIES && !success; ++t) {
      if (t > 0) {
        try {
          Thread.sleep(getRetryTimeInMillis());
        } catch (InterruptedException e) {
          if (mRequireUsage) {
            throw new NoTalkbackSlimException("Failed to send usage information. Aborting.");
          } else {
            return success;
          }
        }
        //Diagnostic.warning("Retrying... (Attempt " + (t + 1) + " of " + NUM_TRIES + ")");
      }
      try {
        final HttpURLConnection http = openConnection();
        http.setConnectTimeout(30000);
        http.setReadTimeout(30000);
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        boolean first = true;
        final StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
          if (entry.getValue() == null) {
            continue;
          }
          if (!first) {
            body.append("&");
          }
          first = false;
          body.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
          body.append("=");
          body.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        final int code;
        final String response;
        try (OutputStream os = http.getOutputStream()) {
          os.write(body.toString().getBytes());
        }
        code = http.getResponseCode();
        try (InputStream responseIn = http.getInputStream()) {
          response = IOUtils.readAll(responseIn);
        }
        if (code != 200 || !UsageServer.RTG_USAGE_ACCEPT.equals(response)) {
          failureMessage = "Failed to send usage information to " + mHost + " (" + code + " " + http.getResponseMessage() + ")";
          if (License.isDeveloper() || "Regression User".equals(License.getPerson())) {
            // For debugging failures during development / regression testing
            Diagnostic.warning("DEV:" + failureMessage);
          }
        } else {
          success = true; //all good!
        }
      } catch (IOException ioe) {
        failureMessage = "Failed to send usage information to " + mHost + " (" + ioe.getMessage() + ")";
        if (License.isDeveloper() || "Regression User".equals(License.getPerson())) {
          // For debugging failures during development / regression testing
          try (MemoryPrintStream mps = new MemoryPrintStream()) {
            mps.printStream().println(failureMessage);
            ioe.printStackTrace(mps.printStream());
            Diagnostic.warning("DEV:" + mps.toString());
          }
        }
      }
    }
    if (!success) {
      Diagnostic.warning(failureMessage);
      if (mRequireUsage) {
        Diagnostic.warning("(Gave up after " + NUM_TRIES + " attempts)");
        throw new NoTalkbackSlimException("Failed to send usage information. Aborting.");
      } else {
        Diagnostic.warning("(Gave up after " + NUM_TRIES + " attempts. If this persists, you may wish to disable usage logging in the RTG installation configuration)");
      }
    }
    return success;
  }
}
