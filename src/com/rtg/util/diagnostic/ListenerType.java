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
package com.rtg.util.diagnostic;

import java.io.Serializable;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 */
public final class ListenerType implements PseudoEnum, Serializable {

  /**
   * Command line listener. Writes messages including progress to stderr.
   */
  public static final ListenerType CLI = new ListenerType(0, "CLI");

  /**
   * File listener. Writes progress messages to the file "progress" in the output directory.
   * The file is rewritten so it contains a single line with the latest progress.
   */
  public static final ListenerType FILE = new ListenerType(1, "FILE");

  /**
   * Specifies that no event listener is to be used.
   */
  public static final ListenerType NULL = new ListenerType(2, "NULL");

  private static final EnumHelper<ListenerType> HELPER = new EnumHelper<>(ListenerType.class, new ListenerType[] {CLI, FILE, NULL});

  /**
   * @return list of the enum values
   */
  public static ListenerType[] values() {
    return HELPER.values();
  }

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str the name of the enum
   * @return the enum value
   */
  public static ListenerType valueOf(final String str) {
    return HELPER.valueOf(str);
  }


  private final String mName;
  private final int mOrdinal;

  private ListenerType(final int ordinal, final String name) {
    mName = name;
    mOrdinal = ordinal;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public String name() {
    return mName;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }
}
