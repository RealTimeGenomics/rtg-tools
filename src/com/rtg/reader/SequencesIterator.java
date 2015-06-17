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
 * Contains iterator-style access methods to sequences SequencesReader.
 */
public interface SequencesIterator {

  /**
   * Get the underlying reader.
   * @return the reader.
   */
  SequencesReader reader();

  /**
   * Position the reader at the specified sequence.
   * @param sequenceId the sequence to seek to.
   * @throws java.io.IOException If in I/O error occurs
   */
  void seek(long sequenceId) throws IOException;

  /**
   * Move to the next sequence.
   * @return true if there is a valid next sequence.
   * @throws java.io.IOException If in I/O error occurs
   */
  boolean nextSequence() throws IOException;

  /**
   * Get the identifier for the current sequence.
   * Will initially be set to 0 and is incremented by <code>nextSequence()</code>.
   * @return the identifier for the current sequence ( &gt;= 0).
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   */
  long currentSequenceId() throws IllegalStateException;

  /**
   * Get the length of the current sequence.
   * @return the length of the current sequence (&gt; 0).
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws IOException if an IO error occurs
   */
  int currentLength() throws IllegalStateException, IOException;

  /**
   * Get the name of the current sequence.
   * Will never be null and will always be 1 or more characters in length.
   * The set of characters that can occur in the name will be restricted to the
   * ASCII numbers 32 to 126 inclusive.
   * @return the name of the current sequence.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws java.io.IOException If in I/O error occurs
   */
  String currentName() throws IllegalStateException, IOException;

  /**
   * Get the full name of the current sequence. Formally this is the result of the current suffix appended to the current name
   * Will never be null and will always be 1 or more characters in length.
   * @return the name of the current sequence.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws java.io.IOException If in I/O error occurs
   */
  String currentFullName() throws IllegalStateException, IOException;

  /**
   * Get the suffix of the current name, normally this will be everything after and including the first whitespace character
   * from the source data file.
   * @return the suffix
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws java.io.IOException If in I/O error occurs
   */
  String currentNameSuffix() throws IllegalStateException, IOException;

  /**
   * Reads current sequence  into the supplied array.
   * @param dataOut array to read data into
   * @return length of sequence
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store sequence.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws java.io.IOException If in I/O error occurs
   */
  int readCurrent(byte[] dataOut) throws IllegalArgumentException, IllegalStateException, IOException;

  /**
   * Reads sequence data into the supplied array.
   * @param dataOut array to read data into
   * @param start the start offset within the sequence to read from
   * @param length the number of residues to read
   * @return length of sequence read
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store sequence.
   * @throws java.io.IOException If in I/O error occurs
   */
  int readCurrent(byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException;

  /**
   * Reads current quality into the supplied array.
   * @param dest array to read data into
   * @return length of quality, 0 if <code>hasQualityData()</code> is false
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store quality.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws java.io.IOException If in I/O error occurs
   */
  int readCurrentQuality(byte[] dest) throws IllegalArgumentException, IllegalStateException, IOException;

  /**
   * Reads current quality into the supplied array.
   * @param dest array to read data into
   * @param start the start offset within the sequence to read from
   * @param length the number of residues to read
   * @return length of quality, 0 if <code>hasQualityData()</code> is false
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store quality.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws java.io.IOException If in I/O error occurs
   */
  int readCurrentQuality(byte[] dest, int start, int length) throws IllegalArgumentException, IllegalStateException, IOException;

  /**
   * Reset the Sequences Reader to initial state.
   */
  void reset();
}
