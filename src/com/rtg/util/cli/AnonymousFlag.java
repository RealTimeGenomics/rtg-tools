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

import com.reeltwo.jumble.annotations.TestClass;

/**
 * <code>AnonymousFlag</code> is a flag with no name.
 */
@TestClass(value = {"com.rtg.util.cli.CFlagsTest"})
public class AnonymousFlag extends Flag {

  private static int sAnonCounter = 0;
  private static synchronized int nextAnonCounter() {
    return ++sAnonCounter;
  }

  /** This specifies the ordering. */
  private final int mFlagRank;

  /**
   * Constructor for an anonymous <code>Flag</code>. These flags aren't
   * referred to by name on the command line -- their values are assigned
   * based on their position in the command line.
   *
   * @param flagDescription a name used when printing help messages.
   * @param paramType a <code>Class</code> denoting the type of values to be
   * accepted.
   * @param paramDescription a description of the meaning of the flag.
   */
  public AnonymousFlag(final String flagDescription, final Class<?> paramType,
      final String paramDescription) {
    super(null, null, flagDescription, 1, 1, paramType, paramDescription, null, "");
    mFlagRank = nextAnonCounter();
  }

  @Override
  String getFlagUsage() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getParameterDescription());
    if (getMaxCount() > 1) {
      sb.append('+');
    }
    return sb.toString();
  }

  @Override
  String getCompactFlagUsage() {
    return getFlagUsage();
  }

  @Override
  public boolean equals(final Object other) {
    return super.equals(other);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public int compareTo(final Flag other) {
    if (other instanceof AnonymousFlag) {
      return mFlagRank - ((AnonymousFlag) other).mFlagRank;
    }
    return 1;
  }
}
