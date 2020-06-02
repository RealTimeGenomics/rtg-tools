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
package com.rtg.reader;

import java.io.IOException;

/**
 * internal type used by <code>SdfWriter</code> to manage writing sequence data
 */
interface SequenceFilePair {

  /**
   * Declares the beginning of a sequence.
   * @return false if operation was not performed due to not being enough space.
   * @throws IOException if an IO error occurs.
   */
  boolean markNextSequence() throws IOException;

  /**
   * Writes sequence bytes to disk.
   * @param data byte array to write
   * @param offset position in the array to start from (zero based)
   * @param length amount of data to write
   * @return true if succeeded, false if space limit hit.
   * @throws IOException if an I/O error occurs.
   */
  boolean write(byte[] data, int offset, int length) throws IOException;

  /**
   * Writes quality bytes to disk.
   * @param qual byte array to write
   * @param offset position in the array to start from (zero based)
   * @param length amount of data to write
   * @return true if succeeded, false if space limit hit.
   * @throws IOException if an I/O error occurs.
   */
  boolean writeQuality(byte[] qual, int offset, int length) throws IOException;

  /**
   * @return original length of data written
   */
  long valuesWritten();

  /**
   * @return the number of bytes available to write
   */
  long bytesFree();

  /**
   * Called to indicate have finished writing the final sequence.
   * @throws IOException if an I/O error occurs
   */
  void lastSequence() throws IOException;

  /**
   * Closes OutputStreams
   * @throws IOException if an I/O error occurs.
   */
  void close() throws IOException;

}
