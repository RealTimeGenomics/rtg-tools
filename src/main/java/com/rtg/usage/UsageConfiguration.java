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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import com.rtg.util.Environment;
import com.rtg.util.io.FileUtils;

/**
 */
public class UsageConfiguration {


  static final String DEFAULT_USAGE_HOST = "https://api.realtimegenomics.com/usage/submit.php";

  static final String ENABLE_USAGE = "usage";
  static final String USAGE_DIR = "usage.dir";
  static final String USAGE_HOST = "usage.host";
  static final String USAGE_LOG_USERNAME = "usage.log.username";
  static final String USAGE_LOG_HOSTNAME = "usage.log.hostname";
  static final String USAGE_LOG_COMMANDLINE = "usage.log.commandline";
  private static final String[] USAGE_TYPES = {USAGE_DIR, USAGE_HOST, USAGE_LOG_USERNAME, USAGE_LOG_HOSTNAME, USAGE_LOG_COMMANDLINE};

  private final Properties mProp;

  /**
   * Construct default usage configuration from environment-supplied settings
   */
  UsageConfiguration() {
    mProp = new Properties();
    final Map<String, String> env = Environment.getEnvironmentMap();
    final Boolean enabled = Boolean.valueOf(env.get(ENABLE_USAGE));
    mProp.setProperty(ENABLE_USAGE, enabled.toString());
    if (enabled) {
      // Pull the properties we care about out of system properties
      for (String property : USAGE_TYPES) {
        if (env.get(property) != null) {
          mProp.setProperty(property, env.get(property));
        }
      }

      // Set default usage host if other destinations have not been set
      if (env.get(USAGE_DIR) == null && env.get(USAGE_HOST) == null) {
        mProp.setProperty(USAGE_HOST, DEFAULT_USAGE_HOST);
      }
    }
  }

  /**
   * Load usage configuration from an external properties file
   * @param configFile contains configuration settings
   * @throws IOException if the configuration file could not be read
   */
  UsageConfiguration(File configFile) throws IOException {
    mProp = new Properties();
    try (InputStream is = FileUtils.createInputStream(configFile, false)) {
      mProp.load(is);
    }
    mProp.setProperty(ENABLE_USAGE, "true"); // If you're bothering to use an explicit configuration file, it's enabled
  }

  UsageConfiguration(Properties prop) {
    mProp = prop;
    mProp.setProperty(ENABLE_USAGE, "true"); // If you're bothering to use an explicit configuration file, it's enabled
  }

  static File createSimpleConfigurationFile(File propFile, String usageDir, String usageHost) throws IOException {
    final Properties prop = new Properties();
    if (usageDir != null) {
      prop.setProperty(USAGE_DIR, usageDir);
    }
    if (usageHost != null) {
      prop.setProperty(USAGE_HOST, usageHost);
    }
    try (OutputStream os = FileUtils.createOutputStream(propFile)) {
      prop.store(os, "");
    }
    return propFile;
  }

  public boolean isEnabled() {
    return Boolean.parseBoolean(mProp.getProperty(ENABLE_USAGE));
  }

  public String getUsageDir() {
    return mProp.getProperty(USAGE_DIR);
  }

  public String getUsageHost() {
    return mProp.getProperty(USAGE_HOST);
  }

  /**
   * @return true if usage messages should include the current user
   */
  public boolean logUsername() {
    return Boolean.parseBoolean(mProp.getProperty(USAGE_LOG_USERNAME, "false"));
  }

  /**
   * @return true if usage messages should include the current machine name
   */
  public boolean logHostname() {
    return Boolean.parseBoolean(mProp.getProperty(USAGE_LOG_HOSTNAME, "false"));
  }

  /**
   * @return true if usage messages should include the current command line
   */
  public boolean logCommandLine() {
    return Boolean.parseBoolean(mProp.getProperty(USAGE_LOG_COMMANDLINE, "false"));
  }
}
