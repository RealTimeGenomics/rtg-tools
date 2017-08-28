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
package com.rtg.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Helps with implementing PseudoEnums.
 * Given the list of values this class will deal with the values and <code>valueOf</code> implementation.
 * @param <T> Class of enum
 */
public class EnumHelper<T extends PseudoEnum> {

  private final Class<T> mClass;

  private final T[] mValues;

  private final String[] mNames;

  private final Map<String, T> mValueOf = new HashMap<>();

  /**
   * Constructs the helper
   * @param c the class of enum this is to help
   * @param values all the enum values in ordinal order.
   */
  public EnumHelper(final Class<T> c, final T[] values) {
    mValues = values.clone();
    mNames = new String[mValues.length];
    int i = 0;
    for (final T t : mValues) {
      mValueOf.put(t.name(), t);
      mNames[i++] = t.name();
    }
    mClass = c;
  }

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str name of enum
   * @return the enum value
   */
  public T valueOf(final String str) {
    final T ret = mValueOf.get(str);
    if (ret == null) {
      throw new IllegalArgumentException(str + " is not a valid enum value of type: " + mClass);
    }
    return ret;
  }

  /**
   * Like an enum's values() method.
   * @return list of enum values
   */
  public T[] values() {
    return mValues.clone();
  }

  /**
   * Return an array containing the names of all enum members.
   * @return list of enum names
   */
  public String[] names() {
    return mNames.clone();
  }
}
