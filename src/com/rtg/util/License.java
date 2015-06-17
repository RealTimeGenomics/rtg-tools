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
package com.rtg.util;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.usage.UsageLogging;


/**
 * Class for getting functionality enablement and expiry information.
 */
@JumbleIgnore
public final class License {

  /**
   * Prevent instantiation.
   */
  private License() { }

  /** Prefix used to denote property names that enable a feature */
  public static final String LICENSE_KEY_PREFIX = "enable_";

  /** Licence key entry for enabling the program */
  public static final String RTG_PROGRAM_KEY = LICENSE_KEY_PREFIX + "rtg";

  private static final boolean IS_DEVELOPER = Boolean.getBoolean("rtg.developer");

  /**
   * Returns true if license is valid.
   *
   * @return Whether or not license is valid.
   */
  public static boolean checkLicense() {
    return true;
  }

  /***
   * String representing expiry date of license in format YYYY-MM-DD
   * @return the expiry date, or "never" if there is none.
   */
  public static String getExpirationDate() {
    return "never";
  }

  /***
   * Returns true if the we are running in developer mode.
   * @return true if the user is a developer.
   */
  public static boolean isDeveloper() {
    return IS_DEVELOPER;
  }

  /**
   * @return serial number for current license
   */
  public static String getSerialNumber() {
    return "27182818284";
  }

  /**
   * @return person email
   */
  public static String getPersonEmail() {
    return "anon";
  }

  /**
   * If <code>checkLicense()</code> returns false this returns the reason.
   *
   * @return Description of problem.
   */
  public static String invalidMessage() {
    throw new UnsupportedOperationException();
  }

  /**
   * Return a text message describing the expiration status.
   *
   * @return key message
   */
  public static String getMessage() {
    return "No license file required";
  }


  /**
   * Checks if the named property exists in the license key and is set
   * to a date that has not expired.
   *
   * @param name the property name
   * @return if the property exists in they license key and is set to true or a date that has not expired
   */
  public static boolean isPropertyLicensed(final String name) {
    return true;
  }

  /**
   * Returns an expiry string for the named property, which may either
   * be a boolean for permanently enabled, or an expiry date.
   *
   * @param name the property name
   * @return licensed, unlicensed, or the expiry date.
   */
  public static String getExpiryStatus(final String name) {
    return "Licensed";
  }

  public static String getPerson() {
    return "RTG Core Non-commercial Use";
  }

  public static String getKeyPath() {
    return null;
  }

  public static String getOrganisation() {
    return null;
  }

  /**
   * Obtain a usage logging message handler for this run
   * @param moduleName name of module being run
   * @param runId unique id for run
   * @param suppress true if we should ignore all usage messages
   * @return usage logging handler
   * @throws IOException if an IO error occurs
   */
  public static UsageLogging usageLogging(String moduleName, UUID runId, boolean suppress) throws IOException {
    return new UsageLogging(new Properties(), moduleName, runId, suppress);
  }

}

