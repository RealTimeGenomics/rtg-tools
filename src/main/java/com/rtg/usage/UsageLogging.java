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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Delegates usage logging messages to appropriate recording implementation. All external usage logging
 * calls should go through here.
 */
public final class UsageLogging {

  static final String SEE_MANUAL = "Please consult the user manual section \"Advanced Installation Configuration\".";

  enum UsageDestination {
    NONE,
    FILE_OR_SERVER,
    SERVER_ONLY
  }

  // Name of usage logging properties obtained from license
  static final String REQUIRE_USAGE = "require_usage";
  static final String USAGE_DESTINATION = "usage_destination";

  /** Records messages generated to the usage clients so can test if the calls are being done. */
  private final ArrayList<String> mUsageMessages = new ArrayList<>();

  private final boolean mRequireUsage;
  private final UsageDestination mUsageDestination;
  private final String mModuleName;
  private final UUID mRunId;
  private final UsageLoggingClient mClient;
  private final UsageConfiguration mUsageConfiguration;


  /**
   * Sets up usage logging using default configuration path
   * @param properties contains license configured properties (whether usage is required, and which usage logging modes are allowed)
   * @param modulename name of module being run
   * @param runId unique ID for the run
   * @param suppress true if we should ignore all usage messages
   * @throws IOException when an IO error occurs
   */
  public UsageLogging(Properties properties, String modulename, UUID runId, boolean suppress) throws IOException {
    this(properties, modulename, runId, null, suppress);
  }

  /**
   * Sets up usage logging using a specific configuration path
   * @param licenseProperties contains license configured properties (whether usage is required, and which usage logging modes are allowed)
   * @param modulename name of module being run
   * @param runId unique ID for the run
   * @param configFileOverride path of file to load install specific usage logging configuration from (directory for file based logging, or host for server based logging)
   * @param suppress true if we should use the null reporter (effectively ignoring all usage messages)
   * @throws IOException if an IO error occurs
   */
  UsageLogging(Properties licenseProperties, String modulename, UUID runId, File configFileOverride, boolean suppress) throws IOException {
    final String usageValue = licenseProperties.getProperty(REQUIRE_USAGE);
    mRequireUsage = usageValue != null && Boolean.parseBoolean(usageValue);
    final String destinationValue = licenseProperties.getProperty(USAGE_DESTINATION);
    mUsageDestination = destinationValue == null ? UsageDestination.NONE : UsageDestination.valueOf(destinationValue.toUpperCase(Locale.ROOT));
    mUsageConfiguration = configFileOverride == null ? new UsageConfiguration() : new UsageConfiguration(configFileOverride);
    mModuleName = modulename;
    mRunId = runId;
    if (suppress) {
      mClient = new NullUsageLoggingClient();
    } else {
      if (mUsageConfiguration.isEnabled() && mUsageConfiguration.getUsageHost() != null) {
        mClient = new HttpUsageLoggingClient(mUsageConfiguration.getUsageHost(), mUsageConfiguration, requireUsage());
      } else if (mUsageConfiguration.isEnabled() && allowFileLogging() && mUsageConfiguration.getUsageDir() != null) {
        mClient = new FileUsageLoggingClient(new File(mUsageConfiguration.getUsageDir()), mUsageConfiguration, requireUsage());
      } else if (requireUsage()) {
        throw new NoTalkbackSlimException("Usage logging is required by license, but has not been correctly configured during install. " + SEE_MANUAL);
      } else {
        mClient = new NullUsageLoggingClient();
      }
    }
  }

  /**
   * @return the usage configuration
   */
  public UsageConfiguration getUsageConfiguration() {
    return mUsageConfiguration;
  }

  /**
   * @return a String with a human readable version of all the usage messages so far.
   */
  public String usageLog() {
    return mUsageMessages.toString();
  }

  /**
   * records a start usage logging message to whichever logging endpoint is configured
   */
  public void recordBeginning() {
    final String msg = "Usage beginning module=" + mModuleName + " runId=" + mRunId;
    mUsageMessages.add(msg);
    Diagnostic.developerLog(msg);
    mClient.recordBeginning(mModuleName, mRunId);
  }

  /**
   * records an end usage logging message to whichever logging endpoint is configured
   * @param metric module specific usage metric
   * @param success true if run succeeded
   */
  public void recordEnd(long metric, boolean success) {
    final String msg = "Usage end module=" + mModuleName + " runId=" + mRunId + " metric=" + metric + " success=" + success;
    mUsageMessages.add(msg);
    Diagnostic.developerLog(msg);
    mClient.recordEnd(metric, mModuleName, mRunId, success);
  }

  /**
   * if true the {@link NullUsageLoggingClient} is not allowed
   * @return if a usage logging client must be configured
   */
  boolean requireUsage() {
    return mRequireUsage;
  }

  /**
   * if true the {@link FileUsageLoggingClient} is allowed
   * @return if file based usage logging is allowed
   */
  boolean allowFileLogging() {
    return mUsageDestination == UsageDestination.FILE_OR_SERVER;
  }
}
