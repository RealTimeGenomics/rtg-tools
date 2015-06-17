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

import java.util.Locale;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.rtg.util.StringUtils;

/**
 * Allows lookup of all commands that are part of a product and retrieval of a command by name.
 */
public abstract class CommandLookup {

  /**
   * @return list of the enum values for commands
   */
  abstract Command[] commands();

  /**
   * Look a command by name, without prefix expansion to licensed commands.
   * @param name the input command name or prefix
   * @return the matching command, or null if an invalid command name was given
   */
  public Command findModule(final String name) {
    final String norm = name.toUpperCase(Locale.getDefault());
    for (Command module : commands()) {
      if (module.getCommandName().equals(norm)) {
        return module;
      }
    }
    return null;
  }

  /**
   * Look a command by name, allowing prefix expansion to licensed commands.
   * @param name the input command name or prefix
   * @return the matching command, or null if no command name matched
   */
  public Command findModuleWithExpansion(final String name) {
    final String norm = name.toUpperCase(Locale.getDefault());

    final NavigableSet<String> names = new TreeSet<>();
    for (Command module : commands()) {
      if (module.isLicensed()) {
        names.add(module.getCommandName());
      }
    }

    return findModule(StringUtils.expandPrefix(names, norm));
  }
}
