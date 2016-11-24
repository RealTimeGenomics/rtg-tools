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
package com.rtg.util.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rtg.util.RstTable;
import com.rtg.util.StringUtils;

/**
 * This class is a handy utility for dealing with command line flags. It allows
 * syntactic and type-checking of flags. Here is some example usage:
 *
 * <pre>
 * public static void main(final String[] args) {
 *   // Create the CFlags
 *   CFlags flags = new CFlags(&quot;java Example&quot;);
 *
 *   // Simple register, a boolean option, no description
 *   flags.registerOptional(&quot;verbose&quot;);
 *   // Register a required integer flag with usage and long description.
 *   flags.registerRequired(&quot;port&quot;, Integer.class, &quot;NUMBER&quot;, &quot;The port to connect to&quot;); // does type-checking!
 *   flags.setRemainderUsage(&quot; &lt; FILE&quot;);
 *
 *   // Set the user-supplied flags with main's String[] args
 *   // This will attempt to parse the flags.
 *   // If it cannot it will print out an appropriate message and call System.exit.
 *   // To override this behaviour, see setInvalidFlagHandler()
 *   flags.setFlags(args);
 *
 *   // Read the flags
 *   if (flags.isSet(&quot;port&quot;)) {
 *     Integer port = (Integer) flags.getValue(&quot;port&quot;);
 *   }
 *
 *   // Rest of your code follows here.
 *
 * }
 * </pre>
 *
 */
public final class CFlags {

  private static final String TABLE_USAGE = "cflags.table";

  static final String LS = System.lineSeparator();

  /**
   * Default width to which help is printed. This is semi-intelligent in that it
   * attempts to look at environment variables to determine the terminal width.
   */
  public static final int DEFAULT_WIDTH;

  static {
    // Try a couple of approaches to working out a larger default value, 80 is fairly cramped
    int defaultWidth = getTermWidth("printenv TERMCAP", ":CO#([0-9]+):", 1);
    if (defaultWidth == -1) {
      defaultWidth = getTermWidth("stty -F /dev/tty size", "([0-9]+) ([0-9]+)", 2);
    }
    if (defaultWidth == -1) {
      defaultWidth = getTermWidth("stty -f /dev/tty size", "([0-9]+) ([0-9]+)", 2);
    }
    DEFAULT_WIDTH = Math.max(80, Math.min(defaultWidth, 160));
  }

  private static int getTermWidth(String command, String regex, int group) {
    try {
      final Process p = Runtime.getRuntime().exec(command);
      try {
        p.waitFor();
        final InputStream is = p.getInputStream();
        final byte[] b = new byte[is.available()];
        if (is.read(b) == b.length) {
          final String commandOut = new String(b).toUpperCase(Locale.getDefault());
          final Matcher m = Pattern.compile(regex).matcher(commandOut);
          if (m.find()) {
            return Integer.parseInt(m.group(group));
          }
        }
      } finally {
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();
      }
    } catch (final Throwable tt) {
      // We really don't care
    }
    return -1;
  }

  /** The default invalid flag handler. */
  public static final InvalidFlagHandler DEFAULT_INVALID_FLAG_HANDLER = new InvalidFlagHandler() {
    @Override
    public void handleInvalidFlags(final CFlags flags) {
      final boolean help = flags.isSet(HELP_FLAG);
      final boolean xhelp = flags.isSet(EXTENDED_HELP_FLAG);
      final boolean xxhelp = flags.isSet(EXPERIMENTAL_HELP_FLAG);
      if (help || xhelp || xxhelp) {
        if (xxhelp) {
          flags.printExtendedUsage(Flag.Level.EXPERIMENTAL);
        } else if (xhelp) {
          flags.printExtendedUsage(Flag.Level.EXTENDED);
        } else {
          flags.printUsage();
        }
      } else {
        flags.error(flags.getInvalidFlagMsg());
      }
    }
  };

  static final String SHORT_FLAG_PREFIX = "-";
  static final String LONG_FLAG_PREFIX = "--";
  /** The built-in flag that signals wants help about flag usage. */
  public static final String HELP_FLAG = "help";
  /** The built-in flag that signals wants help about extended flag usage. */
  public static final String EXTENDED_HELP_FLAG = "Xhelp";
  /** The built-in flag that signals wants help about experimental flag usage. */
  public static final String EXPERIMENTAL_HELP_FLAG = "XXhelp";
  private static final String USAGE_SUMMARY_PREFIX = "Usage: ";
  private static final String PARSE_ERROR_PREFIX = "Error: ";
  static final String REQUIRED_FLAG_USAGE_PREFIX = "Required flags: ";
  static final String OPTIONAL_FLAG_USAGE_PREFIX = "Optional flags: ";

  private final SortedSet<Flag> mRegisteredFlags;

  private final List<SortedSet<Flag>> mRequiredSets;

  private final List<Flag> mAnonymousFlags;
  /** Maps from long names to all registered flags. */
  private final NavigableMap<String, Flag> mLongNames;
  /** Maps from short char names to flags (only for those that have short names). */
  private final Map<Character, Flag> mShortNames;
  /** Where error messages are written. */
  private final Appendable mErr;
  /** Where output is written */
  private final Appendable mOut;
  /** Custom text to tack on to the usage header. */
  private String mRemainderHeaderString = "";
  /** Typically a description of what the program does. */
  private String mProgramDescription = "";
  /** The name of the program accepting flags. */
  private String mProgramName;
  /** Optional validator for overall consistency between flags. */
  private Validator mValidator;
  /** Optional handler to deal with invalid flags. */
  private InvalidFlagHandler mInvalidFlagHandler = DEFAULT_INVALID_FLAG_HANDLER;

  private boolean mUseCategories = false;
  private String[] mCategories = null;
  private String mHelpCategory = null;

  // Set during setFlags()

  /** The original command line. */
  private String[] mArguments = new String[0];
  /** Stores all the read flags and their values, in the order they were seen. */
  private List<FlagValue> mReceivedFlags;
  private String mParseMessageString = "";

  /**
   * Creates a new <code>CFlags</code> instance.
   * @param programName the name of the program.
   * @param programDescription a description of what the program does.
   * @param out output stream
   * @param err error
   */
  public CFlags(final String programName, final String programDescription, final Appendable out, final Appendable err) {
    this(programName, out, err);
    setDescription(programDescription);
  }

  /**
   * Creates a new <code>CFlags</code> instance.
   * @param programName the name of the program.
   * @param out output stream, usually standard out
   * @param err where error messages are written.
   */
  public CFlags(final String programName, final Appendable out, final Appendable err) {
    mOut = out;
    mErr = err;
    mAnonymousFlags = new ArrayList<>();
    mRegisteredFlags = new TreeSet<>();
    mRequiredSets = new ArrayList<>();
    mLongNames = new TreeMap<>();
    mShortNames = new TreeMap<>();
    registerOptional('h', HELP_FLAG, "print help on command-line flag usage");
    registerOptional(EXPERIMENTAL_HELP_FLAG, "");
    setName(programName);
  }

  /** Registers the <code>--Xhelp</code> flag */
  public void registerExtendedHelp() {
    final Flag f = registerOptional(EXTENDED_HELP_FLAG, "print help on extended command-line flag usage");
    if (mHelpCategory != null) {
      f.setCategory(mHelpCategory);
    }
  }

  /** Creates a new <code>CFlags</code> instance. */
  public CFlags() {
    this(null, System.out, System.err);
  }

  // Switch flags -- those that have a name but don't take a parameter
  // These can only be optional

  /**
   * Registers an option. This option is not required to be specified, and has
   * no usage info and no type associated with it. This method is a shortcut for
   * simple boolean flags.
   * @param name the option name (without the prefix string).
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerOptional(final String name, final String description) {
    return registerOptional(name, null, "", description);
  }

  /**
   * Registers an option. This option is not required to be specified, and has
   * no usage info and no type associated with it. This method is a shortcut for
   * simple boolean flags.
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerOptional(final char nameChar, final String name, final String description) {
    return registerOptional(nameChar, name, null, "", description, null);
  }

  // Required flags

  /**
   * Register an anonymous flag. An anonymous flag has no name. Any anonymous
   * flags are processed in the order they are encountered.
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter.
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerRequired(final Class<?> type, final String usage, final String description) {
    final Flag flag = new AnonymousFlag(description, type, usage);
    register(flag);
    return flag;
  }

  /**
   * Registers a required flag. This flag requires a parameter of a specified type.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerRequired(final String name, final Class<?> type, final String usage,
      final String description) {
    return registerRequired(null, name, type, usage, description);
  }

  /**
   * Registers a required flag. This flag requires a parameter of a specified type.
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerRequired(final char nameChar, final String name, final Class<?> type, final String usage, final String description) {
    return registerRequired(Character.valueOf(nameChar), name, type, usage, description);
  }

  private Flag registerRequired(final Character nameChar, final String name, final Class<?> type, final String usage, final String description) {
    return register(new Flag(nameChar, name, description, 1, 1, type, usage, null, ""));
  }

  // Optional flags

  /**
   * Registers an option. When provided, this option requires a parameter of a
   * specified type.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerOptional(final String name, final Class<?> type, final String usage, final String description) {
    return registerOptional(name, type, usage, description, null);
  }

  /**
   * Registers an option. This option requires a parameter of a specified type.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @param defaultValue value the result will take on if not explicitly specified on command line.
   * @param <T> flag value type
   * @return the flag.
   */
  public <T> Flag registerOptional(final String name, final Class<T> type, final String usage, final String description, final T defaultValue) {
    return registerOptional(null, name, type, usage, description, defaultValue);
  }

  /**
   * Registers an option. This option requires a parameter of a specified type.
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerOptional(final char nameChar, final String name, final Class<?> type, final String usage, final String description) {
    return registerOptional(nameChar, name, type, usage, description, null);
  }

  /**
   * Registers an option. This option requires a parameter of a specified type.
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @param defaultValue default value.
   * @param <T> Flags value type
   * @return the flag.
   */
  public <T> Flag registerOptional(final Character nameChar, final String name, final Class<T> type, final String usage, final String description, final T defaultValue) {
    return register(new Flag(nameChar, name, description, 0, 1, type, usage, defaultValue, ""));
  }

  /**
   * Register a flag.
   * @param flag flag to register
   * @return registered instance
   */
  public Flag register(final Flag flag) {
    if (flag instanceof AnonymousFlag) {
      mAnonymousFlags.add(flag);
      mRegisteredFlags.add(flag);
    } else {
      if (mLongNames.containsKey(flag.getName())) {
        throw new IllegalArgumentException("A flag named " + flag.getName() + " already exists.");
      }
      if (flag.getChar() != null) {
        if (mShortNames.containsKey(flag.getChar())) {
          throw new IllegalArgumentException("A flag with short name " + flag.getChar() + " already exists.");
        }
        mShortNames.put(flag.getChar(), flag);
      }
      mRegisteredFlags.add(flag);
      mLongNames.put(flag.getName(), flag);
    }
    return flag;
  }

  /**
   * Un-register a flag.
   *
   * @param name the name of the flag
   * @return the flag that was removed, or <code>null</code> if it wasn't registered already
   */
  public Flag unregister(final String name) {
    if (!mLongNames.containsKey(name)) {
      return null;
    }
    final Flag flag = mLongNames.get(name);
    mLongNames.remove(flag.getName());
    if (flag.getChar() != null) {
      mShortNames.remove(flag.getChar());
    }
    mRegisteredFlags.remove(flag);
    return flag;
  }

  /**
   * Returns a set of the required flags that have not been fully set during <code>setFlags</code>.
   * @return a set of flags.
   */
  private List<Flag> getPendingRequired() {
    final List<Flag> results = new ArrayList<>();
    for (final Flag f : mRegisteredFlags) {
      if (f.getCount() < f.getMinCount()) {
        results.add(f);
      }
    }
    return results;
  }

  /**
   * Returns a collection of the required flags.
   * @return a collection of flags.
   */
  public List<Flag> getRequired() {
    final List<Flag> results = new ArrayList<>();
    for (final Flag f : mRegisteredFlags) {
      if (f.getMinCount() > 0) {
        results.add(f);
      }
    }
    return results;
  }

  /**
   * Returns a collection of the optional flags.
   * @return a collection of flags.
   */
  public List<Flag> getOptional() {
    final List<Flag> results = new ArrayList<>();
    for (final Flag f : mRegisteredFlags) {
      if (f.getMinCount() == 0) {
        results.add(f);
      }
    }
    return results;
  }

  public String getParseMessage() {
    return mParseMessageString;
  }

  public void setParseMessage(final String parseString) {
    mParseMessageString = parseString;
  }

  public String[] getArguments() {
    return mArguments.clone();
  }

  /**
   * Return what we think the command line was (assumes the
   * <code>getName</code> method returns the part of the command line
   * needed to invoke the main.  It only makes sense to call this
   * after <code>setFlags</code> has been called.
   * @return something resembling the command line.
   */
  public String getCommandLine() {
    final StringBuilder sb = new StringBuilder(getName() == null ? "" : getName());
    for (final String a : getArguments()) {
      sb.append(" ");
      if (a.indexOf(' ') != -1) {
        sb.append('"').append(a).append('"');
      } else {
        sb.append(a);
      }
    }
    return sb.toString().trim();
  }

  private void setRemainingParseMessage(final Collection<String> remaining) {
    final StringBuilder usage = new StringBuilder();
    if (remaining != null) {
      if (remaining.size() == 1) {
        usage.append("Unexpected argument");
      } else {
        usage.append("Unexpected arguments");
      }
      for (String s : remaining) {
        usage.append(' ').append('\"').append(s).append('\"');
      }
    }
    setParseMessage(usage.toString());
  }

  private void setPendingParseMessage(final Collection<Flag> pendingRequired) {
    final StringBuilder usage = new StringBuilder();
    if ((pendingRequired != null) && !pendingRequired.isEmpty()) {
      if (pendingRequired.size() == 1) {
        usage.append("You must provide a value for");
      } else {
        usage.append("You must provide values for");
      }
      for (Flag f : pendingRequired) {
        usage.append(' ').append(f.getCompactFlagUsage());
        if (f.getMinCount() > 1) {
          final int count = f.getMinCount() - f.getCount();
          usage.append(" (").append(count).append((count == 1) ? " more time)" : " more times)");
        }
      }
    }
    setParseMessage(usage.toString());
  }

  /**
   * Sets the header text giving usage regarding standard input and output.
   * @param usageString a short description to append to the header text.
   */
  public void setRemainderHeader(final String usageString) {
    mRemainderHeaderString = usageString;
  }

  /**
   * Adds a set of optional flags to the usage header. Should be called for each set of optional required flags
   * i.e. flag <code>-f</code> is a required flag and also one of optional flags <code>-i</code> and
   * <code>-I</code> are required to have valid arguments so you would call this method once with <code>-i</code>
   * and once with <code>-I</code>.
   * @param set flags to be added to usage header
   */
  public void addRequiredSet(Flag... set) {
    final TreeSet<Flag> tset = new TreeSet<>();
    Collections.addAll(tset, set);
    mRequiredSets.add(tset);
  }

  /**
   * Sets the name of the program reading the arguments. Used when printing usage.
   * @param progName the name of the program.
   */
  public void setName(final String progName) {
    mProgramName = progName;
  }

  /**
   * Gets the name of the program reading the arguments. Used when printing usage.
   * @return the name of the program reading the arguments.
   */
  public String getName() {
    return mProgramName;
  }

  /**
   * Sets the description of the program reading the arguments. Used when printing usage.
   * @param description the description.
   */
  public void setDescription(final String description) {
    mProgramDescription = description;
  }

  private void setFlag(final Flag flag, final String strValue) {
//    if (strValue != null && strValue.contains("\n")) {
//      throw new IllegalArgumentException("Value cannot contain new line characters.");
//    }
    mReceivedFlags.add(flag.setValue(strValue));
  }

  public void setInvalidFlagHandler(final InvalidFlagHandler handler) {
    mInvalidFlagHandler = handler;
  }

  public void setValidator(final Validator validator) {
    mValidator = validator;
  }

  /** Resets the list of flags received and their values. */
  public void reset() {
    mReceivedFlags = new ArrayList<>();
    for (final Flag f : mRegisteredFlags) {
      f.reset();
    }
    for (final Flag f : mAnonymousFlags) {
      f.reset();
    }
    setParseMessage("");
  }

  /**
   * Parses the command line flags for later use by the
   * <code>getValue(final String flagname)</code> method.
   * @param args The new flags value.
   * @return True iff all required flags were seen and all seen flags were of
   * set with expected type.
   */
  public boolean setFlags(final String... args) {
    reset();
    mArguments = args.clone();
    boolean success = true;

    // Quickly scan args to see if it looks like they tried to ask for help,
    // even if it's in a position where a flag value was expected
    boolean checkHelp = getFlag(HELP_FLAG) != null;
    boolean checkExHelp = getFlag(EXTENDED_HELP_FLAG) != null;
    boolean checkExexHelp = getFlag(EXPERIMENTAL_HELP_FLAG) != null;
    for (final String arg : args) {
      if (checkHelp && ((LONG_FLAG_PREFIX + HELP_FLAG).equals(arg) || (SHORT_FLAG_PREFIX + "h").equals(arg))) {
        setFlag(getFlag(HELP_FLAG), null);
        checkHelp = false;
      }
      if (checkExHelp && (LONG_FLAG_PREFIX + EXTENDED_HELP_FLAG).equals(arg)) {
        setFlag(getFlag(EXTENDED_HELP_FLAG), null);
        checkExHelp = false;
      }
      if (checkExexHelp && (LONG_FLAG_PREFIX + EXPERIMENTAL_HELP_FLAG).equals(arg)) {
        setFlag(getFlag(EXPERIMENTAL_HELP_FLAG), null);
        checkExexHelp = false;
      }
    }
    if (isSet(HELP_FLAG) || isSet(EXTENDED_HELP_FLAG) || isSet(EXPERIMENTAL_HELP_FLAG)) {
      success = false;
    }

    final List<String> remaining = new ArrayList<>();
    int anonymousCount = 0;
    boolean restAnonymous = false;
    for (int i = 0; i < args.length && success; i++) {
      final String nameArg = args[i];
      Flag flag = null;
      String value = null;
      if (!restAnonymous && nameArg.equals(LONG_FLAG_PREFIX)) {
        restAnonymous = true;
        continue;
      } else if (!restAnonymous && nameArg.startsWith(SHORT_FLAG_PREFIX) && nameArg.length() > 1) {
        if (nameArg.startsWith(LONG_FLAG_PREFIX)) {
          String name = nameArg.substring(LONG_FLAG_PREFIX.length());
          final int splitpos = name.indexOf('=');
          if (splitpos != -1) {
            value = name.substring(splitpos + 1);
            name = name.substring(0, splitpos);
          }
          flag = getFlagWithExpansion(name);
        } else if (nameArg.length() == SHORT_FLAG_PREFIX.length() + 1) {
          final Character nameChar = nameArg.charAt(SHORT_FLAG_PREFIX.length());
          flag = mShortNames.get(nameChar);
        }
        if (flag == null) {
          setParseMessage("Unknown flag " + nameArg);
          success = false;
        }
      }
      if (flag != null) {
        if ((flag.getParameterType() != null) && (value == null)) {
          if (++i == args.length) {
            setParseMessage("Expecting value for flag " + nameArg);
            success = false;
            break;
          } else {
            value = args[i];
          }
        }
        try {
          setFlag(flag, value);
        } catch (final FlagCountException ie) {
          setParseMessage("Attempt to set flag " + nameArg + " too many times.");
          success = false;
        } catch (final IllegalArgumentException e) {
          setParseMessage("Invalid value \"" + value + "\" for flag " + nameArg + ". " + e.getMessage());
          success = false;
        }
      } else if (anonymousCount < mAnonymousFlags.size()) {
        flag = getAnonymousFlag(anonymousCount);
        try {
          setFlag(flag, args[i]);
          if (flag.getCount() == flag.getMaxCount()) {
            anonymousCount++;
          }
        } catch (final IllegalArgumentException e) {
          setParseMessage("Invalid value \"" + args[i] + "\". " + e.getMessage());
          success = false;
        }
      } else {
        remaining.add(args[i]);
      }
    }

    if (success && !remaining.isEmpty()) {
      setRemainingParseMessage(remaining);
      success = false;
    }
    final List<Flag> pendingRequired = getPendingRequired();
    if (success && !pendingRequired.isEmpty()) {
      setPendingParseMessage(pendingRequired);
      success = false;
    }
    if (success && (mValidator != null) && !mValidator.isValid(this)) {
      success = false;
    }
    if (!success && (mInvalidFlagHandler != null)) {
      mInvalidFlagHandler.handleInvalidFlags(this);
    }
    return success;
  }

  /**
   * Get an iterator over anonymous flags.
   * @return iterator over anonymous flags.
   */
  public Iterator<Flag> getAnonymousFlags() {
    return mAnonymousFlags.iterator();
  }

  /**
   * Get an anonymous flag by index.
   * @param index the index
   * @return the flag
   */
  public Flag getAnonymousFlag(final int index) {
    return mAnonymousFlags.get(index);
  }

  /**
   * Get a flag from its name. This method will also return a flag if
   * there is a single unambiguous flag for which the supplied name is a prefix
   * @param flag flag name
   * @return the flag, or null if no matching flag
   */
  Flag getFlagWithExpansion(final String flag) {
    return mLongNames.get(StringUtils.expandPrefix(mLongNames.navigableKeySet(), flag));
  }

  /**
   * Get a flag from its name, using exact matching.
   * @param flag flag name
   * @return the flag, or null if no matching flag
   */
  public Flag getFlag(final String flag) {
    return mLongNames.get(flag);
  }

  /**
   * Gets the value supplied with a flag.
   * @param flag the name of the flag (without the prefix).
   * @return an <code>Object</code> value. This object will be of type
   * configured during the option registering. You can also get the value of
   * no-type flags as a boolean, which indicates whether the no-type flag
   * occurred.
   */
  public Object getValue(final String flag) {
    return getFlag(flag).getValue();
  }

  /**
   * Get the values of a flag.
   * @param flag the flag
   * @return values of the flag
   */
  public List<Object> getValues(final String flag) {
    return getFlag(flag).getValues();
  }

  /**
   * Get an anonymous value.
   * @param index the index
   * @return the anonymous value
   */
  public Object getAnonymousValue(final int index) {
    return getAnonymousFlag(index).getValue();
  }

  /**
   * Get the anonymous values.
   * @param index the index
   * @return anonymous values
   */
  public List<Object> getAnonymousValues(final int index) {
    return getAnonymousFlag(index).getValues();
  }

  /**
   * Returns a list of values that the user supplied, in the order
   * that they were supplied. Each element of the Iterator is a FlagValue.
   * @return an <code>Iterator</code> of <code>FlagValue</code>s.
   */
  public List<FlagValue> getReceivedValues() {
    return mReceivedFlags;
  }

  /**
   * Returns true if a particular flag was provided in the arguments.
   * @param flag the name of the option.
   * @return true if the option was provided in the arguments.
   */
  public boolean isSet(final String flag) {
    final Flag aFlag = getFlag(flag);
    return (aFlag != null) && aFlag.isSet();
  }

  /**
   * Checks whether all the supplied flags have been set, and sets the parse message if a flag has not been set.
   * @param flags the name of the required options.
   * @return true if all the options were provided in the arguments.
   */
  public boolean checkRequired(String... flags) {
    for (final String flag : flags) {
      final Flag aFlag = getFlag(flag);
      if ((aFlag == null) || !aFlag.isSet()) {
        setParseMessage("The flag " + LONG_FLAG_PREFIX + flag + " is required");
        return false;
      }
    }
    return true;
  }

  /**
   * Checks that at most one of the supplied flags is set (zero is also allowed). Sets the parse message on failure.
   * @param flags the name of the options.
   * @return true if passes.
   */
  public boolean checkAtMostOne(String... flags) {
    if (flags.length < 2) {
      throw new IllegalArgumentException("checkAtMostOne requires at least two parameters");
    }
    String firstFlag = null;
    boolean isset = false;
    for (final String flag : flags) {
      final Flag aFlag = getFlag(flag);
      if ((aFlag != null) && aFlag.isSet()) {
        if (!isset) {
          isset = true;
        } else {
          setParseMessage("Cannot set both " + firstFlag + " and " + LONG_FLAG_PREFIX + flag);
          return false;
        }
        firstFlag = LONG_FLAG_PREFIX + flag;
      }
    }
    return true;
  }

  /**
   * Checks that exactly one of the supplied flags is set. Sets the parse message on failure.
   * @param flags the name of the options.
   * @return true if passes.
   */
  public boolean checkXor(String... flags) {
    if (flags.length < 2) {
      throw new IllegalArgumentException("checkXor requires at least two parameters");
    }
    final StringBuilder sb = new StringBuilder();
    boolean isset = false;
    boolean toomany = false;
    for (final String flag : flags) {
      final Flag aFlag = getFlag(flag);
      if ((aFlag != null) && aFlag.isSet()) {
        if (!isset) {
          isset = true;
        } else {
          toomany = true;
        }
      }
      if (sb.length() > 0) {
        sb.append(" or ");
      }
      sb.append(LONG_FLAG_PREFIX).append(flag);
    }
    if (isset && !toomany) {
      return true;
    } else {
      setParseMessage("One of " + sb.toString() + " must be set");
      return false;
    }
  }

  /**
   * Checks that at least one of the supplied flags is set (multiple are allowed). Sets the parse message
   * on failure.
   * @param flags the name of the options.
   * @return true if passes.
   */
  public boolean checkOr(String... flags) {
    if (flags.length < 2) {
      throw new IllegalArgumentException("checkOr requires at least two parameters");
    }
    final StringBuilder sb = new StringBuilder();
    for (final String flag : flags) {
      final Flag aFlag = getFlag(flag);
      if ((aFlag != null) && aFlag.isSet()) {
        return true;
      }
      if (sb.length() > 0) {
        sb.append(" or ");
      }
      sb.append(LONG_FLAG_PREFIX).append(flag);
    }
    setParseMessage("At least one " + sb.toString() + " must be set");
    return false;
  }

  /**
   * Checks neither flag is set, or both flags are set. Sets the parse message
   * on failure.
   * @param flag1 first flag
   * @param flag2 second flag
   * @return true if passes.
   */
  public boolean checkIff(String flag1, String flag2) {
    if (isSet(flag1) != isSet(flag2)) {
      setParseMessage("Flags " + LONG_FLAG_PREFIX + flag1 + " and " + LONG_FLAG_PREFIX + flag2 + " must be set together");
      return false;
    }
    return true;
  }

  /**
   * Checks neither flag is set, or only one flag or the other is set, but not both. Sets the parse message
   * on failure.
   * @param flag1 first flag
   * @param flag2 second flag
   * @return true if passes.
   */
  public boolean checkNand(String flag1, String flag2) {
    return checkAtMostOne(flag1, flag2);
  }

  /**
   * Checks whether any of the supplied "banned" flags have been set, and sets the parse message if any have been set.
   * @param flags the name of the option.
   * @return true none of the banned options were provided in the arguments.
   */
  public boolean checkBanned(String... flags) {
    for (final String flag : flags) {
      final Flag aFlag = getFlag(flag);
      if ((aFlag != null) && aFlag.isSet()) {
        setParseMessage("The flag " + LONG_FLAG_PREFIX + flag + " is not permitted for this set of arguments");
        return false;
      }
    }
    return true;
  }

  /**
   * Checks an integer flag has a value between two values, both ends inclusive
   * @param flagName the flag name to check
   * @param lowValue the low value, or Integer.MIN_VALUE if unbounded
   * @param highValue the high value, or Integer.MAX_VALUE if unbounded
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkInRange(String flagName, int lowValue, int highValue) {
    return checkInRange(flagName, lowValue, true, highValue, true);
  }

  /**
   * Checks an integer flag has a value between two values
   * @param flagName the flag name to check
   * @param lowValue the low value, or Integer.MIN_VALUE if unbounded
   * @param lowInclusive true if the low value itself is permitted
   * @param highValue the high value, or Integer.MAX_VALUE if unbounded
   * @param highInclusive true if the high value itself is permitted
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkInRange(String flagName, int lowValue, boolean lowInclusive, int highValue, boolean highInclusive) {
    if (isSet(flagName)) {
      final int value = (Integer) getValue(flagName);
      if ((value < lowValue) || (value <= lowValue && !lowInclusive)
        || (value > highValue) || (value >= highValue && !highInclusive)) {
        if (highValue == Integer.MAX_VALUE) {
          setParseMessage("The value for " + LONG_FLAG_PREFIX + flagName + " must be " + (lowInclusive ? "at least " : "greater than ") + lowValue);
        } else if (lowValue == Integer.MIN_VALUE) {
          setParseMessage("The value for " + LONG_FLAG_PREFIX + flagName + " must be " + (highInclusive ? "at most " : "less than ") + highValue);
        } else {
          setParseMessage("The value for " + LONG_FLAG_PREFIX + flagName + " must be in the range "
            + ((lowInclusive ? "[" : "(") + lowValue + ", " + highValue + (highInclusive ? "]" : ")")));
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Checks a double flag has a value between two values, both ends inclusive
   * @param flagName the flag name to check
   * @param lowValue the low value, or -Double.MAX_VALUE if unbounded
   * @param highValue the high value, or Double.MAX_VALUE if unbounded
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkInRange(String flagName, double lowValue, double highValue) {
    return checkInRange(flagName, lowValue, true, highValue, true);
  }

  /**
   * Checks a double flag has a value between two values.
   * @param flagName the flag name to check
   * @param lowValue the low value, or -Double.MAX_VALUE if unbounded
   * @param lowInclusive true if the low value itself is permitted
   * @param highValue the high value, or Double.MAX_VALUE if unbounded
   * @param highInclusive true if the high value itself is permitted
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkInRange(String flagName, double lowValue, boolean lowInclusive, double highValue, boolean highInclusive) {
    if (isSet(flagName)) {
      final double value = (Double) getValue(flagName);
      if ((value < lowValue) || (value <= lowValue && !lowInclusive)
        || (value > highValue) || (value >= highValue && !highInclusive)) {
        if (highValue == Double.MAX_VALUE) {
          setParseMessage("The value for " + LONG_FLAG_PREFIX + flagName + " must be " + (lowInclusive ? "at least " : "greater than ") + lowValue);
        } else if (lowValue == -Double.MAX_VALUE) {
          setParseMessage("The value for " + LONG_FLAG_PREFIX + flagName + " must be " + (highInclusive ? "at most " : "less than ") + highValue);
        } else {
          setParseMessage("The value for " + LONG_FLAG_PREFIX + flagName + " must be in the range "
            + ((lowInclusive ? "[" : "(") + lowValue + ", " + highValue + (highInclusive ? "]" : ")")));
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Checks min/max pair of integer flags has values between two values, both ends inclusive
   * @param minFlag the flag name for the minimum value
   * @param maxFlag the flag name for the minimum value
   * @param min the low value, or -Double.MAX_VALUE if unbounded
   * @param max the high value, or Double.MAX_VALUE if unbounded
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkMinMaxInRange(String minFlag, String maxFlag, int min, int max) {
    return checkMinMaxInRange(minFlag, maxFlag, min, true, max, true);
  }

  /**
   * Checks min/max pair of integer flags has values between two values, both ends inclusive
   * @param minFlag the flag name for the minimum value
   * @param maxFlag the flag name for the minimum value
   * @param min the low value, or -Double.MAX_VALUE if unbounded
   * @param lowInclusive true if the low value itself is permitted
   * @param max the high value, or Double.MAX_VALUE if unbounded
   * @param highInclusive true if the max value itself is permitted
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkMinMaxInRange(String minFlag, String maxFlag, int min, boolean lowInclusive, int max, boolean highInclusive) {
    if (!checkInRange(minFlag, min, lowInclusive, max, highInclusive)) {
      return false;
    }
    if (!checkInRange(maxFlag, min, lowInclusive, max, highInclusive)) {
      return false;
    }
    if (isSet(minFlag) && isSet(maxFlag) && (Integer) getValue(minFlag) > (Integer) getValue(maxFlag)) {
      setParseMessage("The value for --" + minFlag + " cannot be greater than the value for --" + maxFlag);
      return false;
    }
    return true;
  }

  /**
   * Checks min/max pair of double flags has values between two values, both ends inclusive
   * @param minFlag the flag name for the minimum value
   * @param maxFlag the flag name for the minimum value
   * @param min the low value, or -Double.MAX_VALUE if unbounded
   * @param max the high value, or Double.MAX_VALUE if unbounded
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public boolean checkMinMaxInRange(String minFlag, String maxFlag, double min, double max) {
    if (!checkInRange(minFlag, min, max)) {
      return false;
    }
    if (!checkInRange(maxFlag, min, max)) {
      return false;
    }
    if (isSet(minFlag) && isSet(maxFlag) && (Double) getValue(minFlag) > (Double) getValue(maxFlag)) {
      setParseMessage("The value for --" + minFlag + " cannot be greater than the value for --" + maxFlag);
      return false;
    }
    return true;
  }

  /**
   * Gets a compact description of the required and optional flags. This
   * contains only the names of the options with their usage parameters (i.e.:
   * not their individual descriptions).
   * @return a short <code>String</code> listing the options.
   */
  public String getCompactFlagUsage() {
    final WrappingStringBuilder sb = new WrappingStringBuilder();
    appendCompactFlagUsage(sb);
    return sb.toString().trim();
  }

  /**
   * Get the usage header used by the invalid flag message
   * @return as above
   */
  public String getUsageHeader() {
    if (mProgramName != null) {
      final WrappingStringBuilder ret = new WrappingStringBuilder();
      ret.append(USAGE_SUMMARY_PREFIX);
      ret.append(mProgramName);
      ret.append(' ');
      ret.setWrapIndent();
      appendCompactFlagUsage(ret);
      if (!mRemainderHeaderString.equals("")) {
        ret.append(' ');
        final String[] splitRemainderHeaderString = mRemainderHeaderString.split(LS);
        for (int i = 0; i < splitRemainderHeaderString.length; i++) {
          ret.wrapText(splitRemainderHeaderString[i]);
          if (i != splitRemainderHeaderString.length - 1) {
            ret.wrap();
          }
        }
      }
      ret.append(LS);
      return ret.toString();
    }
    return "";
  }

  private int getWrapIndent() {
    return USAGE_SUMMARY_PREFIX.length() + mProgramName.length() + 1;
  }

  void appendUsageHeader(final WrappingStringBuilder wb) {
    if (mProgramName != null) {
      wb.append(getUsageHeader());
      wb.setWrapIndent(getWrapIndent());
    }
  }

  private void appendCompactFlagUsage(final WrappingStringBuilder wb) {
    if (mRequiredSets.size() == 0) {
      mRequiredSets.add(new TreeSet<Flag>());
    }
    boolean first = true;
    for (final SortedSet<Flag> ops : mRequiredSets) {
      if (!first) {
        wb.wrap();
      }
      first = false;
      appendCompactFlagUsage(wb, ops);
    }
  }

  /**
   * Adds compact flag usage information to the given WrappingStringBuilder,
   * wrapping at appropriate places.
   */
  private void appendCompactFlagUsage(final WrappingStringBuilder wb, final SortedSet<Flag> optionals) {
    boolean first = true;
    if (getOptional().size() > 0) {
      wb.wrapWord("[OPTION]...");
      first = false;
    }
    for (Flag flag : getRequired()) {
      wb.wrapWord((first ? "" : " ") + flag.getCompactFlagUsage());
      first = false;
    }
    for (final Flag f : optionals) {
      wb.wrapWord((first ? "" : " ") + f.getCompactFlagUsage());
      first = false;
    }
  }

  void appendParseMessage(final WrappingStringBuilder wb) {
    if (!mParseMessageString.equals("")) {
      wb.append(PARSE_ERROR_PREFIX);
      wb.setWrapIndent(PARSE_ERROR_PREFIX.length());
      wb.wrapTextWithNewLines(mParseMessageString).append(LS);
    }
  }

  /**
   * @return a <code>String</code> containing the full usage information. This
   * contains the usage header, usage for each flag and usage footer.
   * Program description is omitted.
   */
  public String getUsageString() {
    return getUsageString(DEFAULT_WIDTH);
  }

  /**
   * @return a <code>String</code> containing the full usage information. This
   * contains the usage header, usage for each flag and usage footer.
   * Program description is omitted.
   * @param level the level of extended help that should be output
   */
  public String getExtendedUsageString(Flag.Level level) {
    return getExtendedUsageString(DEFAULT_WIDTH, level);
  }

  /**
   * Get the usage string.
   * @param width width of output
   * @return usage wrapped to given width
   */
  public String getUsageString(final int width) {
    final WrappingStringBuilder usage = new WrappingStringBuilder();
    usage.setWrapWidth(width);
    appendUsageHeader(usage);
    appendProgramDescription(usage);
    if (mUseCategories) {
      if (mCategories == null) {
        throw new IllegalStateException("Please set categories first");
      }
      appendCategoryFlagUsage(usage, Flag.Level.DEFAULT);
    } else {
      appendLongFlagUsage(usage, Flag.Level.DEFAULT);
    }
    return usage.toString();
  }

  /**
   * Get the usage string.
   * @param width width of output
   * @param level the level of extended help that should be output
   * @return usage wrapped to given width
   */
  public String getExtendedUsageString(final int width, Flag.Level level) {
    final WrappingStringBuilder usage = new WrappingStringBuilder();
    usage.setWrapWidth(width);
    appendUsageHeader(usage);
    usage.append(LS);
    usage.setWrapIndent(6);
    usage.wrapText("Note: Extended command line options are often experimental and untested. These options may come and go between releases. Use them with caution!");
    usage.append(LS);
    appendLongFlagUsage(usage, level);
    return usage.toString();
  }

  List<Flag> getFlagFromType(final String type) {
    final List<Flag> results = new ArrayList<>();
    for (final Flag f : mRegisteredFlags) {
      if (f.getCategory().equals(type)) {
        results.add(f);
      }
    }
    return results;
  }

  private void appendCategoryFlagUsage(final WrappingStringBuilder wb, final Flag.Level level) {
    if (Boolean.getBoolean(TABLE_USAGE)) {
      wb.setWrapWidth(0);
      wb.append(getTableUsage(level));
      return;
    }
    // Get longest string lengths for use below in pretty-printing.
    //final int[] counts = new int[mCategories.length];
    final int longestUsageLength = getUsageLength(level);

    // We do all the required flags first
    for (final String mCategorie : mCategories) {
      final List<Flag> flags = getFlagFromType(mCategorie);
      final Iterator<Flag> flagItr = flags.iterator();
      int flagsCount = 0;
      while (flagItr.hasNext()) {
        final Flag flag = flagItr.next();
        if (displayFlag(flag, level)) {
          flagsCount++;
        }
      }
      if (flagsCount > 0) {
        wb.append(LS);
        wb.append(mCategorie).append(LS);
        wb.setWrapIndent(longestUsageLength + 7);
        for (final Flag flag : flags) {
          flag.appendLongFlagUsage(wb, longestUsageLength, level);
        }
      }
    }
  }

  private String getTableUsage(Flag.Level level) {
    final int usageLength = getUsageLength(level);

    //layout:
    //+--------+-----------------+--------------------+
    //| Category                                      |
    //+========+=================+====================+
    //| ``-x`` | ``--usage=VAL`` | longer description |
    //+--------+-----------------+--------------------+
    final StringBuilder sb = new StringBuilder();
    for (final String category : mCategories) {
      final int descriptionLength = getUsageDescriptionLength(level, category);

      if (usageLength > 0 && descriptionLength > 0) {
        final RstTable table = new RstTable(1, category.length(), 6, usageLength + 4, descriptionLength);
        table.addHeading(category);

        final List<Flag> flags = getFlagFromType(category);
        for (Flag flag : flags) {
          if (displayFlag(flag, level)) {
            final String shortFlag = flag.getChar() != null ? "``" + SHORT_FLAG_PREFIX + flag.getChar() + "``" : "";
            table.addRow(
              shortFlag,
              "``" + flag.getFlagUsage() + "``",
              flag.getUsageDescription()
            );
          }
        }
        sb.append(StringUtils.LS);
        sb.append(table.getText());
      }
    }
    return sb.toString();
  }

  private int getUsageLength(Flag.Level level) {
    int longestUsageLength = 0;
    // Get longest string lengths for use below in pretty-printing.
    //final int[] counts = new int[mCategories.length];
    for (Flag flag : mRegisteredFlags) {
      //counts[getCategoryLocation(flag.getCategory())]++;
      if (!displayFlag(flag, level)) {
        continue;
      }
      final String usageStr = flag.getFlagUsage();
      if (usageStr.length() > longestUsageLength) {
        longestUsageLength = usageStr.length();
      }
    }
    return longestUsageLength;
  }

  private int getUsageDescriptionLength(Flag.Level level, String category) {
    final List<Flag> flags = getFlagFromType(category);
    int usageLength = 0;
    for (Flag flag : flags) {
      if (displayFlag(flag, level)) {
        final String usageDescription = flag.getUsageDescription();
        if (usageDescription.length() > usageLength) {
          usageLength = usageDescription.length();
        }
      }
    }
    return usageLength;
  }

  static boolean displayFlag(Flag flag, Flag.Level level) {
    return flag.level() == level;
  }

  /**
   * Append the description text for this program, if one exists.
   * @param wb wrapping string builder
   */
  void appendProgramDescription(final WrappingStringBuilder wb) {
    if (!mProgramDescription.equals("")) {
      wb.append(LS);
      wb.setWrapIndent(0);
      wb.wrapTextWithNewLines(mProgramDescription);
    }
  }

  /** Help argument message displayed for invalid flags */
  public static final String TRYHELPSTRING = "Try '" + LONG_FLAG_PREFIX + HELP_FLAG + "' for more information";

  /**
   * Get the complete message used when there is an invalid flag.
   * @return the complete message used when there is an invalid flag.
   */
  public String getInvalidFlagMsg() {
    final WrappingStringBuilder wb = new WrappingStringBuilder();
    wb.setWrapWidth(DEFAULT_WIDTH);
    appendParseMessage(wb);
    appendUsageHeader(wb);
    wb.append(LS + TRYHELPSTRING);
    return wb.toString();
  }

  private void appendLongFlagUsage(final WrappingStringBuilder wb, final Flag.Level level) {
    int longestUsageLength = 0;
    // Get longest string lengths for use below in pretty-printing.
    Iterator<Flag> flagItr = mRegisteredFlags.iterator();
    while (flagItr.hasNext()) {
      final Flag flag = flagItr.next();
      if (displayFlag(flag, level)) {
        final String usageStr = flag.getFlagUsage();
        if (usageStr.length() > longestUsageLength) {
          longestUsageLength = usageStr.length();
        }
      }
    }
    // We do all the required flags first
    final List<Flag> required = getRequired();
    flagItr = required.iterator();
    int requiredCount = 0;
    while (flagItr.hasNext()) {
      final Flag flag = flagItr.next();
      if (displayFlag(flag, level)) {
        requiredCount++;
      }
    }
    if (requiredCount > 0) {
      wb.append(LS);
      wb.append(REQUIRED_FLAG_USAGE_PREFIX).append(LS);
      wb.setWrapIndent(longestUsageLength + 7);
      for (final Flag flag : required) {
        flag.appendLongFlagUsage(wb, longestUsageLength, level);
      }
    }
    // Then all the optional flags
    final List<Flag> optional = getOptional();
    flagItr = optional.iterator();
    int optionalCount = 0;
    while (flagItr.hasNext()) {
      final Flag flag = flagItr.next();
      if (displayFlag(flag, level)) {
        optionalCount++;
      }
    }
    if (optionalCount > 0) {
      wb.setWrapIndent(0);
      wb.append(LS);
      wb.append(OPTIONAL_FLAG_USAGE_PREFIX).append(LS);
      wb.setWrapIndent(longestUsageLength + 7);
      for (final Flag flag : optional) {
        flag.appendLongFlagUsage(wb, longestUsageLength, level);
      }
    }
  }

  /**
   * Prints the full usage information.
   */
  public void printUsage() {
    out(getUsageString());
  }

  /**
   * Prints the usage information for extended flags.
   * @param level the level of extended help that should be output
   */
  public void printExtendedUsage(Flag.Level level) {
    out(getExtendedUsageString(level));
  }

  /**
   * Prints message to the specified error output.
   * @param msg message to be written to error output.
   */
  public void error(final String msg) {
    try {
      mErr.append(msg);
      mErr.append(LS);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Prints message to the specified output.
   * @param msg message to be written to output.
   */
  public void out(final String msg) {
    try {
      mOut.append(msg);
      mOut.append(LS);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Just used for testing purposes
   * @param args The command line arguments.
   */
  public static void main(final String[] args) {
    final CFlags cli = new CFlags("CFlags", System.out, System.err);

    // cli.setRemainderHeader("ARGS");
    // Maybe support remaining args like this:
    cli.registerRequired(String.class, "ARGS", "these are some extra required args");
    cli.registerRequired(String.class, "BARGS", "these are some extra required args");

    final Flag intFlag = cli.registerRequired('i', "int", Integer.class, "my_int",
    "This sets an int value.");
    intFlag.setMaxCount(5);
    // intFlag.setMaxCount(Integer.MAX_VALUE);
    intFlag.setMinCount(2);

    cli.registerOptional('s', "switch", "this is a toggle flag.");
    cli.registerOptional('b', "boolean", Boolean.class, "true/false", "this sets a boolean value.",
        Boolean.TRUE);

    final Flag f = cli.registerOptional('f', "float", Float.class, null, "this sets a float value.", (float) 20);
    f.setParameterRange(new String[] {"0.2", "0.4", "0.6" });
    cli.registerOptional("string", String.class, null, "this sets a string value. and for this one I'm going to have quite a long description. Possibly long enough to need wrapping.",
    "myDefault");
    cli.setFlags(args);

    System.out.println("--switch: " + cli.getValue("switch"));
    System.out.println("--boolean: " + cli.getValue("boolean"));
    System.out.println("--int: " + cli.getValue("int"));
    System.out.println("--float: " + cli.getValue("float"));
    System.out.println("--string: " + cli.getValue("string"));

    System.out.println(LS + "Multi-occurrence flag:");
    System.out.println("--int: " + cli.getValues("int"));

    System.out.println(LS + "Received values in order:");
    for (FlagValue fv : cli.getReceivedValues()) {
      System.out.println(fv);
    }
  }

  /**
   * Uses categories to display help messages
   * @param helpCategory the category that help flags will appear under
   * @param categories the categories to use
   */
  public void setCategories(String helpCategory, String[] categories) {
    boolean helpOK = false;
    for (final String cat : categories) {
      if (cat.equals(helpCategory)) {
        helpOK = true;
      }
    }
    if (!helpOK) {
      throw new IllegalArgumentException("Help category " + helpCategory + " not found in categories.");
    }
    mUseCategories = true;
    setCategories(categories);
    setHelpCategory(helpCategory);
  }

  /**
   * @return the categories
   */
  public String[] getCategories() {
    if (mCategories != null) {
      return Arrays.copyOf(mCategories, mCategories.length);
    }
    return null;
  }

  /**
   * @return the help category
   */
  public String getHelpCategory() {
    return mHelpCategory;
  }

  private void setCategories(String[] categories) {
    mCategories = Arrays.copyOf(categories, categories.length);
  }

  private void setHelpCategory(String helpCategory) {
    mHelpCategory = helpCategory;
    getFlag(HELP_FLAG).setCategory(helpCategory);
    final Flag f = getFlag(EXTENDED_HELP_FLAG);
    if (f != null) {
      f.setCategory(helpCategory);
    }
    final Flag f2 = getFlag(EXPERIMENTAL_HELP_FLAG);
    if (f2 != null) {
      f2.setCategory(helpCategory);
    }
  }
}
