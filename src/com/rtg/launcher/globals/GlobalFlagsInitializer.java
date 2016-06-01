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

import static com.rtg.launcher.globals.GlobalFlags.CATEGORY;

import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.cli.Flag;

/**
 * Base class for registering a bundle of experimental flags
 */
@TestClass("com.rtg.launcher.globals.GlobalFlagsTest")
public abstract class GlobalFlagsInitializer {

  private final List<Flag> mFlags;

  GlobalFlagsInitializer(List<Flag> flags) {
    mFlags = flags;
  }

  /**
   * Called to register the flags for this bundle. Should contain one or more {@link #registerFlag(String)}.
   */
  public abstract void registerFlags();

  protected void registerFlag(String name) {
    registerFlag(name, null, null);
  }

  protected <T> void registerFlag(String name, Class<T> type, T def) {
    if (type != null && def == null) {
      throw new IllegalArgumentException("Default value must be non-null for experimental flags with a type");
    }
    mFlags.add(new Flag(null, "XX" + name, "", 0, 1, type, type == null ? "" : type.getSimpleName(), def, CATEGORY));
  }
}
