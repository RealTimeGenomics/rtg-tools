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

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.util.Constants;
import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.WrappingStringBuilder;

/**
 * Help module for Slim, provides summaries of other modules
 *
 */
@JumbleIgnore
public final class HelpCommand extends Command {

  private CommandLookup mInfo = null;

  HelpCommand() {
    super(null, "HELP", "print this screen or help for specified command", CommandCategory.UTILITY, ReleaseLevel.GA, License.RTG_PROGRAM_KEY, null);
  }

  void setInfo(CommandLookup info) {
    mInfo = info;
  }

  @Override
  public int mainInit(final String[] args, final OutputStream out, final PrintStream err) {
    return mainInit(args, out, err, mInfo);
  }

  private static final String APPLICATION_NAME = Constants.APPLICATION_NAME;

  static final String MEM_STR = "       " + APPLICATION_NAME + " RTG_MEM=16G COMMAND [OPTION]...  (e.g. to set maximum memory use to 16 GB)" + StringUtils.LS + StringUtils.LS;

  static final String USAGE_STR = "Usage: " + APPLICATION_NAME + " COMMAND [OPTION]..." + StringUtils.LS
                                + (Environment.OS_LINUX || Environment.OS_MAC_OS_X ? MEM_STR : StringUtils.LS)
                                + "Type '" + APPLICATION_NAME + " help COMMAND' for help on a specific command." + StringUtils.LS
                                + "The following commands are available:" + StringUtils.LS;

  /**
   * Main function, entry-point for help.
   * @param args currently unused, will take module name in future
   * @param outStream print stream for output
   * @param errStream print stream for error
   * @param info source of module command names
   * @return shell return code 0 for success, anything else for failure
   */
  public static int mainInit(final String[] args, final OutputStream outStream, final PrintStream errStream, CommandLookup info) {
    //final PrintStream outPrintStream = new PrintStream(outStream);
    return printHelp(args, outStream, errStream, info);
  }

  private static int printHelp(final String[] args, final OutputStream outStream, final PrintStream errStream, CommandLookup info) {
    if (args != null && args.length > 0) {
      final String args0 = args[0].toUpperCase(Locale.getDefault());
      if ("--XHIDDEN".equals(args0) || "--XHELP".equals(args0)) {
        final PrintStream psOutStream =  new PrintStream(outStream);
        try {
          printUsage(psOutStream, true, info);
        } finally {
          psOutStream.flush();
        }
        return 0;
      } else {
        final Command mod = info.findModule(args0);
        if (mod == null) {
          final PrintStream psOutStream =  new PrintStream(outStream);
          try {
            printUsage(psOutStream, false, info);
          } finally {
            psOutStream.flush();
          }
        } else {
          if (!mod.isLicensed()) {
            outputUnlicensedModule(mod);
          } else {
            mod.mainInit(new String[] {"-h"}, outStream, errStream);
          }
        }
        return 0; //successfully determined help for this module
      }
    } else {  //args
      final PrintStream psOutStream = new PrintStream(outStream);
      try {
        printUsage(args == null ? errStream : psOutStream, false, info);
      } finally {
        psOutStream.flush();
      }
      return args == null ? 1 : 0; //if no args given, return error
    }
  }

  protected static void outputUnlicensedModule(Command mod) {
    final String message = "The " + mod.getCommandName()
      + " command has not been enabled by your current license." + StringUtils.LS + "Please contact "
      + Constants.SUPPORT_EMAIL_ADDR + " to have this command licensed.";
    System.err.println(message);
    throw new RuntimeException();
  }

  /**
   * Usage information for the SLIM runtime. Includes information about modules.
   * @param printHidden print hidden modules
   * @param info source of module command names
   * @return string representation of usage information
   */
  public static String getUsage(boolean printHidden, CommandLookup info) {
    return getUsage(printHidden, CFlags.DEFAULT_WIDTH, info);
  }

  protected static String getUsage(boolean printHidden, int width, CommandLookup info) {
    String moduleTypeName;
    moduleTypeName = null;

    // Get longest string lengths for use below in pretty-printing.
    final int longestUsageLength = Math.min(getLongestLengthModule(info.commands()), 13);

    final StringBuilder sb = new StringBuilder();

    sb.append(USAGE_STR).append(StringUtils.LS);
    for (Command module : info.commands()) {

      // Show only licensed, non-hidden modules.
      if (!module.isLicensed()) {
        continue;
      }
      if (!printHidden && module.isHidden())  {
        continue;
      }

      if (moduleTypeName == null || !moduleTypeName.equals(module.getCategory().toString())) {
        if (moduleTypeName != null) {
          sb.append(StringUtils.LS);
        }
        sb.append(module.getCategory().getLabel()).append(":").append(StringUtils.LS);
        moduleTypeName = module.getCategory().toString();
      }
      sb.append("\t").append(module.getCommandName().toLowerCase(Locale.getDefault()));
      if (module.getCommandName().length() > longestUsageLength) {
        sb.append(StringUtils.LS).append("\t");
        for (int i = 0; i < longestUsageLength; ++i) {
          sb.append(" ");
        }
      } else {
        for (int i = 0; i < longestUsageLength - module.getCommandName().length(); ++i) {
          sb.append(" ");
        }
      }
      sb.append(" \t");
      sb.append(module.getCommandDescription()).append(StringUtils.LS);
    }
    final WrappingStringBuilder wb = new WrappingStringBuilder();
    wb.setWrapWidth(width);
    final StringBuilder spaces = new StringBuilder();
    for (int i = 0; i < longestUsageLength; ++i) {
      spaces.append(" ");
    }
    wb.setWrapIndent("\t" + spaces.toString() + " \t");
    wb.wrapTextWithNewLines(sb.toString());
    return wb.toString();
  }

  /**
   * Print the usage information for the SLIM runtime to the given print stream
   * @param printStream the print stream to print usage information to
   * @param printHidden print hidden modules
   * @param info source of module command names
   */
  public static void printUsage(final PrintStream printStream, final boolean printHidden, CommandLookup info) {
    printStream.print(getUsage(printHidden, info));
  }

  static int getLongestLengthModule(Command[] values) {
    int longestUsageLength = 0;
    for (Command module : values) {
      if (module.getCommandName().length() > longestUsageLength && !module.isHidden()) {
        longestUsageLength = module.getCommandName().length();
      }
    }
    return longestUsageLength;
  }
}
