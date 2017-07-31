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
package com.rtg;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

import com.rtg.launcher.AbstractCli;
import com.rtg.util.License;

/**
 * Wraps up a command that can be executed by name from a product entry and has license control, release status etc.
 */
public class Command {

  private final String mCommandName;
  private final String mCommandDescription;
  private final CommandCategory mCategory;
  private final ReleaseLevel mReleaseLevel;
  private final String mLicenceProperty;
  private final String mAltLicenceProperty; // Used for backward compatibility with existing licenses when we change module names. May be null
  private final AbstractCli mCli;

  Command(final AbstractCli cli, final CommandCategory category, final ReleaseLevel level) {
    this(cli, category, level, null);
  }

  Command(final AbstractCli cli, final CommandCategory category, final ReleaseLevel level, final String altKey) {
    this(cli, cli.moduleName(), cli.description(), category, level, License.LICENSE_KEY_PREFIX + cli.moduleName(), altKey);
  }

  Command(final AbstractCli cli, final String commandName, String description, final CommandCategory category, final ReleaseLevel level, final String key, final String altKey) {
    assert commandName != null;
    assert category != null;
    assert level != null;
    assert key != null;
    mCli = cli; // Should only be null if not implemented using the AbstractCli framework, and must therefore override mainInit
    mCommandName = commandName.toUpperCase(Locale.ROOT);
    mCommandDescription = description;
    mCategory = category;
    mReleaseLevel = level;
    mLicenceProperty = key.toLowerCase(Locale.ROOT);
    mAltLicenceProperty = altKey;
  }

  /**
   * Main init for running this module
   * @param args args for the module
   * @param out output stream to write to
   * @param err print stream to write to in case of error
   * @return integer return code
   */
  public int mainInit(final String[] args, final OutputStream out, final PrintStream err) {
    if (mCli == null) {
      throw new RuntimeException("Incorrectly configured module");
    }
    return mCli.mainInit(args, out, err);
  }

  /**
   * @return name of module
   */
  public String getCommandName() {
    return mCommandName;
  }

  /**
   * @return one line description of the command
   */
  public String getCommandDescription() {
    return mCommandDescription;
  }

  @Override
  public String toString() {
    return mCommandName;
  }

  /**
   * @return key value used in license to determine if module is licensed
   */
  public String getLicenceKeyName() {
    return mLicenceProperty;
  }

  /**
   * @return an optional alternative license key name that the module may also be enabled by (for backwards compatibility).
   */
  public String getAltLicenceKeyName() {
    return mAltLicenceProperty;
  }

  /**
   * @return category of module
   */
  public CommandCategory getCategory() {
    return mCategory;
  }

  /**
   * @return release level of module
   */
  public ReleaseLevel getReleaseLevel() {
    return mReleaseLevel;
  }

  /**
   * @return true if should not be visible in help
   */
  public boolean isHidden() {
    return !(mReleaseLevel == ReleaseLevel.BETA || mReleaseLevel == ReleaseLevel.GA);
  }

  /**
   * @return true if current license may run this module
   */
  public boolean isLicensed() {
    return License.isPropertyLicensed(mLicenceProperty)
        || (mAltLicenceProperty != null && License.isPropertyLicensed(mAltLicenceProperty));
  }

  /**
   * @return string representation of license status
   */
  public String licenseStatus() {
    return mAltLicenceProperty != null && License.isPropertyLicensed(mAltLicenceProperty)
        ? License.getExpiryStatus(mAltLicenceProperty)
        : License.getExpiryStatus(mLicenceProperty);
  }

}
