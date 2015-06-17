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
package com.rtg.util.array;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Function for swapping two array elements
 */
@TestClass(value = {"com.rtg.util.QuickSortTest"})
public class Swapper {

  /**
   * Swapper for two index arrays.
   * @param primary primary index array.
   * @param secondary secondary index array.
   */
  public Swapper(final CommonIndex primary, final CommonIndex secondary) {
    mPrimary = primary;
    mSecondary = secondary;
  }

  private final CommonIndex mPrimary;
    private final CommonIndex mSecondary;

  /**
   * Swap the two elements a and b.
   * @param a first element.
   * @param b second element.
   */
  public void swap(final long a, final long b) {
//    System.err.println("CI swap: " + a + " " + b);
    //System.err.println(a + ":" + b);
    final long x = mPrimary.get(a);
    mPrimary.set(a, mPrimary.get(b));
    mPrimary.set(b, x);
    final long y = mSecondary.get(a);
    mSecondary.set(a, mSecondary.get(b));
    mSecondary.set(b, y);
  }
}
