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
package com.rtg.mode;

/**
 * The possible framing for sequences.
 *
 */
public interface Frame {

  /**
   * Get a unique integer code for the frame.
   * @return a unique integer code.
   */
  int ordinal();

  /**
   * Get a code with respect to the frame.
   * @param codes the underlying array of codes.
   * @param length the number of valid entries in codes.
   * @param index the position to access with respect to this frame.
   *              note that it is in the units of codes not of the translated
   *              results - for example in translated frames index will typically
   *              be incremented by 3 to get succesive translated codes.
   * @return the code at the position specified by index.
   */
  byte code(byte[] codes, int length, int index);

  /**
   * Get a code with respect to the frame.
   * @param codes the underlying array of codes.
   * @param length the number of valid entries in codes.
   * @param index the position to access with respect to this frame.
   *              note that it is in the units of codes not of the translated
   *              results - for example in translated frames index will typically
   *              be incremented by 3 to get succesive translated codes.
   * @param firstValid start position of first valid code in code array material (inclusive).
   * @param lastValid last valid code in code array material (exclusive). for a translated frame this will be the
   * position of the first code that cannot be included in a valid translated result.
   * @return the code at the position specified by index.
   */
  byte code(byte[] codes, int length, int index, int firstValid, int lastValid);

  /**
   * Calculate the first valid code. (inclusive)
   * @param offset position into source material that array starts
   * @param length length of data in array
   * @param fullLength length of source material
   * @return the first valid code to pass to code method
   */
  int calculateFirstValid(int offset, int length, int fullLength);

  /**
   * Calculate the last valid code. (exclusive)
   * @param offset position into source material that array starts
   * @param length length of data in array
   * @param fullLength length of source material
   * @return the last valid code to pass to code method
   */
  int calculateLastValid(int offset, int length, int fullLength);


  /**
   * Get the string to be used in output formats for describing the frame.
   * @return a string to be used in output formats.
   */
  String display();

  /**
   * Get the offset of the frame with respect to the underlying sequence.
   * Is &gt;= 0 (0 except for translated frames).
   * @return the offset of the frame with respect to the underlying sequence.
   */
  int phase();

  /**
   * Check if the frame is forward (or untranslated).
   * @return true iff frame is forward.
   */
  boolean isForward();

  /**
   * Get the reverse of this frame
   * @return a frame
   */
  Frame getReverse();
}

