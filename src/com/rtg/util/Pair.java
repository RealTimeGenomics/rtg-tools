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

/**
 * Immutable pair of non-null objects.
 * Care taken with equals and hashcode.
 * @param <A> type of first element in pair.
 * @param <B> type of second element in pair.
 */
public class Pair<A, B> {

  private final A mA;
  private final B mB;

  /**
   * Utility method to make creating pairs easier by skipping typing out the types
   * @param one the first item
   * @param two the second item
   * @param <T1> type of the first item
   * @param <T2> type of the second item
   * @return a pair containing both items
   */
  public static<T1, T2> Pair<T1, T2> create(T1 one, T2 two) {
    return new Pair<>(one, two);
  }

  /**
   * Represents a pair of any type of object.
   * @param a first item
   * @param b second item
   */
  public Pair(final A a, final B b) {
    if (a == null) {
      throw new NullPointerException();
    }
    if (b == null) {
      throw new NullPointerException();
    }
    mA = a;
    mB = b;
  }

  /**
   * Get a.
   * @return Returns the a.
   */
  public A getA() {
    return mA;
  }

  /**
   * Get b.
   * @return Returns the b.
   */
  public B getB() {
    return mB;
  }

  @Override
  public boolean equals(final Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (arg0 == this) {
      return true;
    }
    if (!(arg0 instanceof Pair)) {
      return false;
    }
    final Pair<?, ?> that = (Pair<?, ?>) arg0;
    return this.mA.equals(that.mA) && this.mB.equals(that.mB);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mA.hashCode(), mB.hashCode());
  }

  @Override
  public String toString() {
    return mA + ":" + mB;
  }

}

