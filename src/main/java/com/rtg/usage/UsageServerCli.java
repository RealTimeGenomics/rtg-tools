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
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * module entrance for usage server
 */
public class UsageServerCli extends AbstractCli {

  private static final String PORT = "port";
  private static final String THREADS = "threads";

  // these can be used to check if the server has been started if we're called programmatically
  protected final Object mSync;
  private boolean mStarted = false;

  /**
   * Constructor
   */
  public UsageServerCli() {
    mSync = new Object();
    mSuppressUsage = true;
  }

  @Override
  public String moduleName() {
    return "usageserver";
  }

  @Override
  public String description() {
    return "run a local server for collecting RTG command usage information";
  }

  boolean getStarted() {
    synchronized (mSync) {
      return mStarted;
    }
  }

  @Override
  protected void initFlags() {
    final String host = mUsageLogger.getUsageConfiguration().getUsageHost();
    int defaultPort = 8080;
    if (host != null) {
      try {
        final URI uri = new URI(host);
        if (uri.getScheme() != null && "http".equalsIgnoreCase(uri.getScheme())) {
          final int hostPort = uri.getPort();
          defaultPort = hostPort == -1 ? 80 : hostPort;
        }
      } catch (URISyntaxException e) {
        throw new NoTalkbackSlimException("Malformed usage host specified in usage configuration: " + host);
      }
    }
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription("Start usage tracking server.");
    mFlags.registerOptional('p', PORT, Integer.class, CommonFlags.INT, "port on which to listen for usage logging connections.", defaultPort).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional('T', THREADS, Integer.class, CommonFlags.INT, "number of worker threads to handle incoming connections.", 4).setCategory(CommonFlagCategories.UTILITY);
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    Diagnostic.info("Checking usage configuration.");
    if (!mUsageLogger.getUsageConfiguration().isEnabled()) {
      throw new NoTalkbackSlimException("Cannot start usage server without RTG_USAGE configuration option set. " + UsageLogging.SEE_MANUAL);
    } else if (mUsageLogger.getUsageConfiguration().getUsageDir() == null) {
      throw new NoTalkbackSlimException("Cannot start usage server without RTG_USAGE_DIR configuration option set. " + UsageLogging.SEE_MANUAL);
    }
    final Integer port = (Integer) mFlags.getValue(PORT);
    final String configHost = mUsageLogger.getUsageConfiguration().getUsageHost();
    // Output some warnings if the port doesn't correspond with where the clients will be pointing
    if (configHost != null) {
      try {
        final URI uri = new URI(configHost);
        if (uri.getScheme() != null && "http".equalsIgnoreCase(uri.getScheme())) {
          final int configPort = uri.getPort() == -1 ? 80 : uri.getPort();
          if (configPort != port) {
            Diagnostic.warning("Specified port " + port + " does not correspond with port from usage configuration: " + configPort);
          }
        } else {
          Diagnostic.warning("This server (HTTP on port " + port + ") does not correspond to current usage configuration: " + configHost);
        }
      } catch (URISyntaxException e) {
        throw new NoTalkbackSlimException("Malformed usage host URI specified in usage configuration: " + configHost);
      }
    } else {
      Diagnostic.warning("Clients will not be able to connect until RTG_USAGE_HOST has been set. " + UsageLogging.SEE_MANUAL);
    }
    final UsageServer us = new UsageServer(port, new File(mUsageLogger.getUsageConfiguration().getUsageDir()), (Integer) mFlags.getValue(THREADS));
    synchronized (mSync) {
      us.start();
      out.write(("Usage server listening on port " + us.getPort() + StringUtils.LS).getBytes());
      mStarted = true;
      try {
        boolean cont = true;
        while (cont) {
          try {
            mSync.wait(1000);
          } catch (InterruptedException e) {
            cont = false;
          }
        }
      } finally {
        us.end();
      }
    }
    return 0;
  }

}
