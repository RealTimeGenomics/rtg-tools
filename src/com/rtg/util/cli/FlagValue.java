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

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a flag and value pairing. This is used when retrieving the set
 * of flags in the order they were set.
 * @param <T> Flags value type
 */
public class FlagValue<T> {
  private final Flag<T> mFlag;

  private final List<T> mValue = new ArrayList<>();

  FlagValue(final Flag<T> flag, final T value) {
    mFlag = flag;
    mValue.add(value);
  }

  FlagValue(Flag<T> flag, List<T> values) {
    mFlag = flag;
    mValue.addAll(values);
  }

  /**
   * Gets the Flag that this value was supplied to.
   *
   * @return the Flag that this value was supplied to.
   */
  public Flag<T> getFlag() {
    return mFlag;
  }

  /**
   * Gets the value supplied to the flag.
   *
   * @return the value supplied to the flag.
   */
  public T getValue() {
    return mValue.get(0);
  }

  /**
   * Gets the values supplied to the flag.
   *
   * @return the value supplied to the flag.
   */
  public List<T> getValues() {
    return mValue;
  }

  /**
   * Gets a human-readable description of the flag value.
   *
   * @return a human-readable description of the flag value.
   */
  public String toString() {
    String name = mFlag.getName();
    if (name == null) {
      name = mFlag.getParameterDescription();
    }
    return name + "=" + (mValue.size() == 1 ? mValue.get(0).toString() : mValue.toString());
  }
}
