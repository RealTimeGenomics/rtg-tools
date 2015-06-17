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
import java.io.OutputStream;

/**
 * Container for sequence names accessed by sequence id
 */
public interface PrereadNamesInterface {

  /**
   * Returns number of names
   * @return length of names
   */
  long length();

  /**
   * Returns the sequence name for a specified id.
   * @param id sequence id
   * @return sequence name
   */
  String name(final long id);

  /**
   * Calculate the checksum of the names in a manner compatible with
   * how the checksum is calculated in the SDF.
   *
   * @return the checksum of the names.
   */
  long calcChecksum();

  /**
   * @return size of object in bytes
   */
  long bytes();

  /**
   * Convenience method to append a name to an Appendable.  This avoid
   * the string creation of the <code>name()</code> method.
   *
   * @param a an <code>Appendable</code> value
   * @param id a <code>long</code> value
   * @throws IOException when writing to the appendable.
   */
  void writeName(final Appendable a, final long id) throws IOException;

  /**
   * Convenience method to append a name to an Appendable.  This avoid
   * the string creation of the <code>name()</code> method.
   *
   * @param os an <code>OutputStream</code> value
   * @param id a <code>long</code> value
   * @throws IOException when writing to the stream.
   */
  void writeName(OutputStream os, long id) throws IOException;

}
