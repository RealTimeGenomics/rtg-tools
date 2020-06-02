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

import java.io.Closeable;
import java.io.IOException;

import com.rtg.mode.SequenceType;

/**
 * Provides access to a set of sequences.
 *
 */
public interface SequenceDataSource extends Closeable {

  /**
   * Get the type of the sequences (all sequences have the same type (DNA/Protein)).
   * @return the type of the sequences (non-null).
   */
  SequenceType type();

  /**
   * @return If reader contains quality data <code>true</code>, if not <code>false</code>
   */
  boolean hasQualityData();

  /**
   * Move to the next sequence.
   * @return true if there is a valid next sequence.
   * @throws IOException If in I/O error occurs
   */
  boolean nextSequence() throws IOException;

  /**
   * Get the length of the current sequence.
   * @return the length of the current sequence (&gt; 0).
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws IOException if an IO error occurs
   */
  int currentLength() throws IOException;

  /**
   * Get the name of the current sequence.
   * Will never be null and will always be 1 or more characters in length.
   * The set of characters that can occur in the name will be restricted to the
   * ASCII numbers 32 to 126 inclusive.
   * @return the name of the current sequence.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws IOException If in I/O error occurs
   */
  String name() throws IOException;

  /**
   * Returns the sequence data for the current sequence. This returns the internal byte array of the implementor. The array will only be filled up to <code>currentLength</code>.
   * @return the current sequence data.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws IOException If in I/O error occurs
   */
  byte[] sequenceData() throws IOException;

  /**
   * Returns the quality data for the current sequence. This returns the internal byte array of the implementor. The array will only be filled up to <code>currentLength</code>.
   * @return the current quality data.
   * @throws IllegalStateException if <code>nextSequence()</code> returned false on its last call.
   * @throws IOException If in I/O error occurs
   */
  byte[] qualityData() throws IOException;

  /**
   * Closes the data source.
   * @throws IOException If an I/O error occurs
   */
  @Override
  void close() throws IOException;

  /**
   * Enables dusting on the output sequence.
   * @param val True - enables dusting, False - disables it
   */
  void setDusting(boolean val);

  /**
   * Gets the number of dusted input residues.
   * @return the number of dusted input residues.
   */
  long getDusted();

  /**
   * Get the maximum sequence length.
   * @return the maximum sequence length.
   */
  long getMaxLength();

  /**
   * Get the minimum sequence length.
   * @return the minimum sequence length.
   */
  long getMinLength();

  /**
   * Return the total number of warnings that occurred
   * @return warning count
   */
  long getWarningCount();

}
