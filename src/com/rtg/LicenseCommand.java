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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.util.License;
import com.rtg.util.StringUtils;

/**
 * License command for RTG, provides summaries of other commands
 */
@JumbleIgnore
public final class LicenseCommand extends Command {

  private CommandLookup mInfo = null;

  LicenseCommand() {
    super(null, "LICENSE", "print license information for all commands", CommandCategory.UTILITY, ReleaseLevel.GA, License.RTG_PROGRAM_KEY, null);
  }

  void setInfo(CommandLookup info) {
    mInfo = info;
  }

  @Override
  public int mainInit(final String[] args, final OutputStream out, final PrintStream err) {
    return mainInit(out, mInfo);
  }

  /**
   * Main function, entry-point for license.
   *
   * @param outStream print stream for output
   * @param info source of module command names
   * @return shell return code 0 for success, anything else for failure
   */
  public static int mainInit(final OutputStream outStream, CommandLookup info) {
    final PrintStream psoutStream = new PrintStream(outStream);
    try {
      printLicense(psoutStream, info);
    } finally {
      psoutStream.flush();
    }
    return 0;
  }

  static String getLicenseSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("License: ").append(License.getMessage()).append(StringUtils.LS);
    final String person = License.getPerson();
    if (person != null) {
      sb.append("Licensed to: ").append(person).append(StringUtils.LS);
    }
    final String keyPath = License.getKeyPath();
    if (keyPath != null) {
      sb.append("License location: ").append(keyPath).append(StringUtils.LS);
    }
    return sb.toString();
  }

  private static void printLicense(final PrintStream out, CommandLookup info) {
    out.println(getLicenseSummary());

    final String commandName = "Command name";
    // Get longest string lengths for use below in pretty-printing.
    final int longestUsageLength = getLongestLengthModule(info.commands(), commandName);
    out.print("\t");
    out.print(commandName);
    for (int i = -1; i < longestUsageLength - commandName.length(); ++i) {
      out.print(" ");
    }
    out.print("\t" + padTo("Licensed?", 17));
    out.print(" Release Level");
    out.println();

    final HashSet<CommandCategory> categories = new LinkedHashSet<>();
    for (Command module : info.commands()) {
      if (showModule(module)) {
        categories.add(module.getCategory());
      }
    }

    for (CommandCategory cat : categories) {
      out.println();
      out.println(cat.getLabel() + ":");
      for (Command module : info.commands()) {
        if (cat == module.getCategory() && showModule(module)) {
          outputModule(module, longestUsageLength, out);
        }
      }
    }
  }

  static void outputModule(Command module, int longestUsageLength, PrintStream out) {
    out.print("\t" + module.getCommandName().toLowerCase(Locale.getDefault()));
    for (int i = 0; i < longestUsageLength - module.getCommandName().length(); ++i) {
      out.print(" ");
    }

    out.print(" \t");
    out.print(padTo(module.licenseStatus(), 17));

    out.print(" ");
    if (module.getReleaseLevel() == ReleaseLevel.GA) {
      out.print(module.getReleaseLevel());
    } else {
      out.print(module.getReleaseLevel().toString().toLowerCase(Locale.getDefault()));
    }

    out.println();
  }

  static String padTo(String str, int length) {
    final StringBuilder sb = new StringBuilder(str);
    for (int i = str.length(); i < length; ++i) {
      sb.append(' ');
    }
    return sb.toString();
  }

  // Show all GA and beta modules modules as they have had some level of polish applied.
  // Developers also get to see warty alpha modules
  static boolean showModule(final Command module) {
    return (module.getReleaseLevel() != ReleaseLevel.ALPHA) || License.isDeveloper();
  }

  static int getLongestLengthModule(Command[] values, String baseString) {
    int longestUsageLength = baseString.length();
    for (Command module : values) {
      if (module.getCommandName().length() > longestUsageLength && showModule(module)) {
        longestUsageLength = module.getCommandName().length();
      }
    }
    return longestUsageLength;
  }
}
