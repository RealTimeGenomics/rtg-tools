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

/**
 */
interface DetailedModel extends ArithCodeModel {


  /**
   * Returns the total count for the current context.
   * @return Total count for the current context.
   */
  int totalCount();

  /**
   * Returns the symbol whose interval of low and high counts
   * contains the given count.
   * @param count The given count.
   * @return The symbol whose interval contains the given count.
   */
  int pointToSymbol(int count);

  /**
   * Calculates <code>{low count, high count, total count}</code> for
   * the given symbol in the current context.
   * The cumulative counts
   * in the return must be such that <code>0 &lt;= low count &lt; high
   * count &lt;= total count</code>.
   * <P>
   * This method will be called exactly once for each symbol being
   * encoded or decoded, and the calls will be made in the order in
   * which they appear in the original file.
   * @param symbol The next symbol to decode.
   * @param result Array into which to write range.
   */
  void interval(int symbol, int[] result);
}
