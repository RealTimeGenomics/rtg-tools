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

import java.io.File;
import java.io.IOException;

import com.rtg.mode.SequenceType;

/**
 * Provides access to a set of sequences.
 *
 */
public interface SequencesReader extends AutoCloseable {

  /**
   * Get an iterator-style accessor
   * @return the SequencesIterator
   */
  SequencesIterator iterator();

  /**
   * Get the type of the sequences (all sequences have the same type (DNA/Protein)).
   * @return the type of the sequences (non-null).
   */
  SequenceType type();

  /**
   * Get the sum of the lengths of all sequences.
   * @return the sum of the lengths of all sequences (&gt; 0).
   */
  long totalLength();

  /**
   * Get the length of the longest sequence.
   * @return the length of the longest sequence (&gt; 0).
   */
  long maxLength();

  /**
   * Get the length of the shortest sequence
   * @return the length of the shortest sequence. (&gt; 0)
   */
  long minLength();

  /**
   * Get the number of sequences.
   * @return the number of sequences (&gt; 0).
   */
  long numberSequences();

  /**
   * Returns the checksum for the sequence data
   * @return checksum
   */
  long dataChecksum();

  /**
   * Returns the checksum for the sequence qualities
   * @return checksum
   */
  long qualityChecksum();

  /**
   * Returns the checksum for the sequence names
   * @return checksum
   */
  long nameChecksum();

  /**
   * Returns the checksum for the sequence name suffixes
   * @return checksum
   */
  long suffixChecksum();

  /**
   * Return the residue counts for current preread
   * residues are stored on the basis of ordinal
   * @return residue counts array,
   */
  long[] residueCounts();

  /**
   * Return histogram of N's
   * @return array containing counts
   */
  long[] histogram();

  /**
   * Return position histogram of N's
   * @return array containing counts
   */
  long[] posHistogram();

  /**
   * Return quality scores average histogram
   * @return array containing counts
   */
  double globalQualityAverage();

  /**
   * Return the average quality per position
   * @return array containing counts
   */
  double[] positionQualityAverage();
  /**
   * Number of blocks of N's
   * @return the number
   */
  long nBlockCount();

  /**
   * Longest single N block
   * @return the length
   */
  long longestNBlock();

  /**
   * returns true if result from <code>histogram()</code> is valid
   * @return true if histogram available
   */
  boolean hasHistogram();

  /**
   * @return left and right
   */
  PrereadArm getArm();

  /**
   * @return sequencing technology
   */
  PrereadType getPrereadType();

  /**
   * Get a unique id which is the same on left and right arms of Complete Genomics
   * data. For use with older data only.
   * @return GUID
   */
  SdfId getSdfId();


  /**
   * Returns the SDF version of the underlying source if appropriate
   * @return the version of the SDF store, or -1 if it is not an SDF store
   */
  long sdfVersion();

  /**
   * Return the the path to any source file/directory relating to this reader.  If
   * this reader is not backed by files then it is permissible to return
   * null.
   *
   * @return directory
   */
  File path();

  /**
   * @return If reader contains quality data <code>true</code>, if not <code>false</code>
   */
  boolean hasQualityData();

  /**
   * @return If reader contains sequence names <code>true</code>, if not <code>false</code>
   */
  boolean hasNames();

  // Direct accessor methods

  /**
   * Returns the length of the requested sequence
   * @param sequenceIndex the sequence id
   * @return the length of the requested sequence
   * @throws IOException if an IO error occurs
   */
  int length(long sequenceIndex) throws IOException;

  /**
   * Returns the checksum associated with the requested sequence
   * @param sequenceIndex index of sequence
   * @return first byte of CRC32 checksum produced from sequence data
   * @throws IOException if an IO error occurs
   */
  byte sequenceDataChecksum(long sequenceIndex) throws IOException;

  /**
   * Get the name of the specified sequence.
   * Will never be null and will always be 1 or more characters in length.
   * The set of characters that can occur in the name will be restricted to the
   * ASCII numbers 32 to 126 inclusive.
   * @param sequenceIndex Sequence to read.
   * @return the name of the current sequence.
   * @throws IOException If in I/O error occurs
   */
  String name(long sequenceIndex) throws IOException;

  /**
   * Get the name suffix of the specified sequence.
   * @param sequenceIndex Sequence to read.
   * @return the name of the current sequence.
   * @throws IOException If in I/O error occurs
   */
  String nameSuffix(long sequenceIndex) throws IOException;

  /**
   * Get the full name of the specified sequence.
   * Will never be null and will always be 1 or more characters in length.
   * The set of characters that can occur in the name will be restricted to the
   * ASCII numbers 32 to 126 inclusive.
   * @param sequenceIndex Sequence to read.
   * @return the name of the current sequence.
   * @throws IOException If in I/O error occurs
   */
  String fullName(long sequenceIndex) throws IOException;

  /**
   * Reads sequence data into the a newly allocated array.
   * @param sequenceIndex Sequence to read.
   * @return array containing read data
   * @throws IOException If in I/O error occurs
   */
  byte[] read(long sequenceIndex) throws IOException;

  /**
   * Reads sequence data into the supplied array.
   * @param sequenceIndex Sequence to read.
   * @param dataOut array to read data into
   * @return length of sequence
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store sequence.
   * @throws IOException If in I/O error occurs
   */
  int read(long sequenceIndex, byte[] dataOut) throws IllegalArgumentException, IOException;

  /**
   * Reads sequence data into the supplied array.
   * @param sequenceIndex Sequence to read.
   * @param dataOut array to read data into
   * @param start the start offset within the sequence to read from
   * @param length the number of residues to read
   * @return length of sequence read
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store sequence.
   * @throws IOException If in I/O error occurs
   */
  int read(long sequenceIndex, byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException;

  /**
   * Reads quality data into a newly allocated array.
   * @param sequenceIndex Sequence to read quality for.
   * @return array that the quality data was read into
   * @throws IOException If in I/O error occurs
   */
  byte[] readQuality(long sequenceIndex) throws IOException;

  /**
   * Reads quality data into the supplied array.
   * @param sequenceIndex Sequence to read quality for.
   * @param dest array to read data into
   * @return length of quality, 0 if <code>hasQualityData()</code> is false
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store quality.
   * @throws IOException If in I/O error occurs
   */
  int readQuality(long sequenceIndex, byte[] dest) throws IllegalArgumentException, IOException;

  /**
   * Reads quality data into the supplied array.
   * @param sequenceIndex Sequence to read quality for.
   * @param dest array to read data into
   * @param start the start offset within the sequence to read from
   * @param length the number of quality values to read
   * @return length of quality, 0 if <code>hasQualityData()</code> is false
   * @throws IllegalArgumentException If <code>dataOut</code> does not have enough length to store quality.
   * @throws IOException If in I/O error occurs
   */
  int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IllegalArgumentException, IOException;

  /**
   * If appropriate ensures any backing file is closed.
   *
   * @throws IOException If in I/O error occurs
   */
  @Override
  void close() throws IOException;

  /**
   * Return an object which can be used to get names for sequences.
   * @return names
   * @throws IOException If in I/O error occurs
   */
  PrereadNamesInterface names() throws IOException;

  /**
   * count the number of residue in the sequences between <code>start</code>(inclusive) and <code>end</code>(exclusive).
   * @param start sequence id of first sequence.
   * @param end  sequence id of last sequence + 1
   * @return the length
   * @throws IOException If in I/O error occurs
   */
  long lengthBetween(long start, long end) throws IOException;

  /**
   * Return all the sequence lengths
   *
   * @param start starting sequence id
   * @param end ending sequence id (excl)
   * @return array of sequence lengths
   * @throws IOException If in I/O error occurs
   */
  int[] sequenceLengths(long start, long end) throws IOException;

  /**
   * @return another copy of this reader which can be used independently
   */
  SequencesReader copy();

  /**
   * Whether input was compressed, generally only required for filtering purposes
   * @return true if compressed
   */
  boolean compressed();

  /**
   * Get the contents of the read-me file as a string if this reader has a directory.
   * @return the contents of the read-me, or null if it does not exist.
   * @throws IOException if there is an error reading the file.
   */
  String getReadMe() throws IOException;

  /**
   * @return the index containing reader meta data
   */
  IndexFile index();

}
