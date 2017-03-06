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

package com.rtg.launcher.globals;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.ClassPathScanner;

/**
 * A global store of command line flags to use for highly experimental options
 * <p>
 *   NOTE care should be taken to ensure flags have been set by the time your code accessing the values is initialised
 *   (i.e. they should not get values during the static initialisation of a <code>CLI</code> class, since this could
 *   cause them to access the values before the <code>CFlags.setFlags(args)</code> call is made. However other things
 *   can also cause a classes static initialization to start)
 * </p>
 */
public final class GlobalFlags {


  private GlobalFlags() { }
  //NOTE flag definitions should be placed here, to prevent circular dependencies
  //NOTE read class note

  //Map

  //Species

  //Edit distance factory

  //Assembler

  // AVR

  //vcfeval

  private static final String GLOBAL_FLAG_PACKAGE = "com.rtg.launcher.globals";
  private static CFlags sFlags;
  private static final Set<String> ACCESSED_FLAGS = new HashSet<>();
  /** Category for global flags */
  public static final String CATEGORY = "Highly Experimental";
  private static final ArrayList<Flag<?>> FLAGS = new ArrayList<>();

  private static final CFlags DEFAULT_FLAGS = new CFlags();
  static { //this ensures default values are available in tests and classes which don't use <code>CLI</code> framework
    final List<Class<?>> initializers = new ClassPathScanner(GLOBAL_FLAG_PACKAGE).getClasses(clazz -> GlobalFlagsInitializer.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()));
    for (Class<?> clazz : initializers) {
      try {
        final GlobalFlagsInitializer initializer = (GlobalFlagsInitializer) clazz.getDeclaredConstructor(List.class).newInstance(FLAGS);
        initializer.registerFlags();
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {

        throw new RuntimeException(e);
      }
    }
    registerExperimentalFlags(DEFAULT_FLAGS);
  }

  /**
   * Add experimental global flags to given flags instance
   * @param flags flags to augment
   */
  public static void registerExperimentalFlags(CFlags flags) {
    resetAccessedStatus();
    final String[] cat = flags.getCategories();
    if (cat != null) {
      final String[] copy = Arrays.copyOf(cat, cat.length + 1);
      copy[copy.length - 1] = CATEGORY;
      flags.setCategories(flags.getHelpCategory(), copy);
    }
    for (final Flag<?> flag : FLAGS) {
      flags.register(flag);
    }
    sFlags = flags;
  }

  /**
   * @param name name of flag (without <code>XX</code> prefix)
   * @return flag object
   */
  public static Flag<?> getFlag(String name) {
    final String innerName = "XX" + name;
    ACCESSED_FLAGS.add(innerName);
    return sFlags.getFlag(innerName);
  }

  /**
   * @param name name of flag (without <code>XX</code> prefix)
   * @return true if flag is set
   */
  public static boolean isSet(String name) {
    return getFlag(name).isSet();
  }

  /**
   * @param name name of flag (without <code>XX</code> prefix)
   * @return string representation of flag value
   */
  public static String getStringValue(String name) {
    return (String) getFlag(name).getValue();
  }

  /**
   * @param name name of flag (without <code>XX</code> prefix)
   * @return file representation of flag value
   */
  public static File getFileValue(String name) {
    return (File) getFlag(name).getValue();
  }

  /**
   * @param name name of flag (without <code>XX</code> prefix)
   * @return boolean representation of flag value
   */
  public static boolean getBooleanValue(String name) {
    return (Boolean) getFlag(name).getValue();
  }

  /**
   * @param flagName name of flag (without <code>xx</code> prefix)
   * @return int representation of flag value
   */
  public static int getIntegerValue(String flagName) {
    return (Integer) getFlag(flagName).getValue();
  }

  /**
   * @param flagName name of flag (without <code>xx</code> prefix)
   * @return double representation of flag value
   */
  public static double getDoubleValue(String flagName) {
    return (Double) getFlag(flagName).getValue();
  }

  /**
   * checks flags haven't been accessed yet, prints a warning if they have
   * @return true if no flags have been accessed
   */
  public static boolean initialAccessCheck() {
    boolean bad = false;
    for (final String flag : ACCESSED_FLAGS) {
      Diagnostic.warning("Flag: --" + flag + " is accessed before flag registration");
      bad = true;
    }
    resetAccessedStatus();
    return !bad;
  }

  /**
   * @return true iff all set flags were also accessed since registration
   */
  public static boolean finalAccessCheck() {
    for (final Flag<?> f : FLAGS) {
      if (f.isSet() && !ACCESSED_FLAGS.contains(f.getName())) {
        Diagnostic.warning("Flag: --" + f.getName() + " is set but never accessed.");
        resetAccessedStatus();
        return false;
      }
    }
    resetAccessedStatus();
    return true;
  }

  /**
   * Unsets list of flags which have been accessed/set.
   * If your test is failing due to another test using a global flag, find THAT test
   * and put a call to this into the tear down method.
   */
  public static void resetAccessedStatus() {
    ACCESSED_FLAGS.clear();
    DEFAULT_FLAGS.reset();
    sFlags = DEFAULT_FLAGS;
  }
}
