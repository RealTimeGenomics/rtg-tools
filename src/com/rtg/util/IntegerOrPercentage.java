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

import java.io.Serializable;

/**
 * Class to hold either an absolute value or a percentage
 *
 */
public class IntegerOrPercentage implements Serializable, Comparable<IntegerOrPercentage> {
  private final int mValue;
  private final boolean mIsPercentage;

  /**
   * Constructs a new IntegerOrPercentage based on the supplied String.
   * If the string ends with "%", it is a percentage, otherwise an absolute value.
   *
   * @param s the string to parse
   * @throws NumberFormatException if not a valid number
   * @return the new instance
   */
  public static IntegerOrPercentage valueOf(final String s) {
    return new IntegerOrPercentage(s);
  }

  /**
   * Constructs a new IntegerOrPercentage based on the supplied int.
   *
   * @param i the int
   * @return the new instance
   */
  public static IntegerOrPercentage valueOf(final int i) {
    return new IntegerOrPercentage(i);
  }

  /**
   * Constructs a new IntegerOrPercentage based on the supplied String.
   * If the string ends with "%", it is a percentage, otherwise an absolute value.
   *
   * @param s the string to parse
   * @throws NumberFormatException if not a valid number
   */
  public IntegerOrPercentage(final String s) {
    final int pos = s.indexOf('%');
    if (pos >= 0) {
      mValue = Integer.parseInt(s.substring(0, pos));
      mIsPercentage = true;
    } else {
      mValue = Integer.parseInt(s);
      mIsPercentage = false;
    }
  }

  /**
   * Constructs a new IntegerOrPercentage based on the supplied int.
   *
   * @param i the int
   */
  public IntegerOrPercentage(final int i) {
    mValue = i;
    mIsPercentage = false;
  }

  /**
   * Computes the value of this IntegerOrPercentage.
   *
   * @param size the 100% value
   * @return the value unchanged if not a percentage, otherwise the percentage of size
   */
  public int getValue(final int size) {
    if (mIsPercentage) {
      return mValue * size / 100;
    } else {
      return mValue;
    }
  }

  /**
   * @return the raw value the user specified, independent of whether it is a percentage.
   */
  public int getRawValue() {
    return mValue;
  }

  /**
   *
   * @return if this is a percentage value
   */
  public boolean isPercentage() {
    return mIsPercentage;
  }

  /**
   * Compares with another IntegerOrPercentage
   * @param o an IntegerOrPercentage
   * @return &lt;0/0/&gt;0 if less than/equal/greater than
   */
  @Override
  public int compareTo(IntegerOrPercentage o) {
    if (mIsPercentage == o.mIsPercentage) {
      return mValue - o.mValue;
    } else {
      return mIsPercentage ? -1 : 1;
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof IntegerOrPercentage && compareTo((IntegerOrPercentage) o) == 0;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this.mValue;
    return 31 * hash + (this.mIsPercentage ? 1 : 0);
  }

  @Override
  public String toString() {
    return mIsPercentage ? Integer.toString(mValue) + "%" : Integer.toString(mValue);
  }
}
