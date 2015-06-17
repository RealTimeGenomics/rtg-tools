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
package com.rtg.util.arithcode;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * A generic arithmetic coding subclass containing elements common to
 * both arithmetic decoding and arithmetic coding.
 *
 * @version 1.1
 */
@TestClass("com.rtg.util.arithcode.ArithTest")
class ArithCoder {

  /**
   * The low bound on the current interval for coding.  Initialized
   * to zero.
   */
  protected long mLow; // implied = 0;

  /**
   * The high bound on the current interval for coding.
   * Initialized to top value possible.
   */
  protected long mHigh = TOP_VALUE;

  /**
   * Construct a generic arithmetic coder.
   */
  protected ArithCoder() { }

  /**
   * Precision of coding, expressed in number of bits used for
   * arithmetic before shifting out partial results.
   */
  protected static final int CODE_VALUE_BITS = 27;

  /**
   * The largest possible interval value. All <code>1</code>s.
   */
  protected static final long TOP_VALUE = ((long) 1 << CODE_VALUE_BITS) - 1;

  /**
   * 1/4 of the largest possible value plus one.
   */
  protected static final long FIRST_QUARTER = TOP_VALUE / 4 + 1;

  /**
   * 1/2 of the largest possible value; <code>2 * FIRST_QUARTER</code>
   */
  protected static final long HALF = 2 * FIRST_QUARTER;

  /**
   * 3/4 of the largest possible value; <code>3 * FIRST_QUARTER</code>
   */
  protected static final long THIRD_QUARTER = 3 * FIRST_QUARTER;

}
