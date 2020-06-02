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
import java.io.FileNotFoundException;
import java.io.IOException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;

/**
 * Constructs <code>SequencesReader</code>s.
 *
 */
public final class SequencesReaderFactory {

  private SequencesReaderFactory() {
  }

  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   *
   * @param dir the SDF directory
   * @param region the sequences of the SDF to include
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReader(final File dir, LongRange region) throws IOException {
    try {
      final DefaultSequencesReader r = new DefaultSequencesReader(dir, region);
      logSDF(r);
      return r;
    } catch (final FileNotFoundException e) {
      // Slightly better I/O reporting than the default provided by AbstractCli
      if (dir.isDirectory()) {
        throw new IOException("The specified SDF, \"" + dir.getPath() + "\", does not seem to contain a valid SDF index", e);
      } else if (dir.exists()) {
        throw new IOException("The specified file, \"" + dir.getPath() + "\", is not an SDF.", e);
      } else {
        throw new IOException("The specified SDF, \"" + dir.getPath() + "\", does not exist.", e);
      }
    }
  }

  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   *
   * @param dir the SDF directory
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReader(final File dir) throws IOException {
    return createDefaultSequencesReader(dir, LongRange.NONE);
  }

  /**
   * Constructs a <code>MemorySequencesReader</code>.
   *
   * @param dir the SDF directory
   * @param loadNames whether to load names from disk or not
   * @param region range of sequences to load
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static SequencesReader createMemorySequencesReader(final File dir, final boolean loadNames, LongRange region) throws IOException {
    return createMemorySequencesReader(dir, loadNames, false, region);
  }


  /**
   * Constructs a <code>MemorySequencesReader</code>.
   *
   * @param dir the SDF directory
   * @param loadNames whether to load names from disk or not
   * @param loadFullNames whether to load full names from disk or not
   * @param region range of sequences to load
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static SequencesReader createMemorySequencesReader(final File dir, final boolean loadNames, boolean loadFullNames, LongRange region) throws IOException {
    //new Throwable("With dir " + dir).printStackTrace();
    if (dir == null) {
      return null;
    }
    final SequencesReader r = CompressedMemorySequencesReader.createSequencesReader(dir, loadNames, loadFullNames, region);
    logSDF(r);
    return r;
  }

  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   * Checks if the resulting reader has no sequences.
   * @param dir the SDF directory
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   * @throws NoTalkbackSlimException if the reader has not sequences.
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReaderCheckEmpty(final File dir) throws IOException {
    return createDefaultSequencesReaderCheckEmpty(dir, LongRange.NONE);
  }


  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   * Checks if the resulting reader has no sequences.
   * @param dir the SDF directory
   * @param region the sequences of the SDF to include
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   * @throws NoTalkbackSlimException if the reader has not sequences.
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReaderCheckEmpty(final File dir, LongRange region) throws IOException {
    final AnnotatedSequencesReader result = createDefaultSequencesReader(dir, region);
    ReaderUtils.validateNotEmpty(result);
    return result;
  }

  /**
   * Constructs a <code>MemorySequencesReader</code>.
   * Checks if the resulting reader has no sequences.
   * @param dir the SDF directory
   * @param loadNames whether to load names from disk or not
   * @param loadFullNames whether to load full names from disk or not
   * @param region range of sequences to load
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   * @throws NoTalkbackSlimException if the reader has not sequences.
   */
  public static SequencesReader createMemorySequencesReaderCheckEmpty(final File dir, final boolean loadNames, boolean loadFullNames, LongRange region) throws IOException {
    final SequencesReader result = createMemorySequencesReader(dir, loadNames, loadFullNames, region);
    ReaderUtils.validateNotEmpty(result);
    return result;
  }

  /**
   * Resolves an inital range (supplied by the user, and may have unbounded ends) to the available sequences.
   * If end is greater than number of sequences it sets end to number of sequences.
   * @param dir SDF directory
   * @param range the range
   * @return resolved range
   * @throws IOException if an IO error occurs
   * @throws NoTalkbackSlimException if the start is out of range.
   */
  public static LongRange resolveRange(final File dir, LongRange range) throws IOException {
    return resolveRange(new IndexFile(dir), range);
  }

  /**
   * Resolves an inital range (supplied by the user, and may have unbounded ends) to the available sequences.
   * If end is greater than number of sequences it sets end to number of sequences.
   * @param index the SDF index for the reader
   * @param range the range
   * @return the resolved range.
   * @throws NoTalkbackSlimException if the start is out of range.
   */
  public static LongRange resolveRange(IndexFile index, LongRange range) {
    return resolveRange(range, index.getNumberSequences());
  }

  /**
   * Resolves an inital range (supplied by the user, and may have unbounded ends) to the available sequences.
   * If end is greater than number of sequences it sets end to number of sequences.
   * @param numberSequences the number of sequences in the SDF
   * @param range the range
   * @return the resolved range.
   * @throws NoTalkbackSlimException if the start is out of range.
   */
  public static LongRange resolveRange(LongRange range, long numberSequences) {
    final long start = range.getStart() == LongRange.MISSING ? 0 : range.getStart();
    if (start < 0) {
      throw new IllegalArgumentException();
    }
    if (start > numberSequences || (numberSequences != 0 && start == numberSequences)) {  // Allow start == 0 if empty SDF
      throw new NoTalkbackSlimException("The start sequence id \"" + start + "\" must be less than than the number of available sequences \"" + numberSequences + "\".");
    }
    long end = range.getEnd() == LongRange.MISSING ? numberSequences : range.getEnd();
    if (end > numberSequences) {
      Diagnostic.warning("The end sequence id \"" + range.getEnd() + "\" is out of range, it"
        + " must be from \"" + (start + 1) + "\" to \"" + numberSequences + "\". Defaulting end to \"" + numberSequences + "\"");
      end = numberSequences;
    }
    return new LongRange(start, end);
  }

  private static void logSDF(SequencesReader r) {
    final String sdfID = r.index().getSdfId().toString();
    Diagnostic.userLog("Referenced SDF-ID: " + sdfID + " Type: " + r.type() + " Sequences: " + r.numberSequences() + " Max-Length: " + r.maxLength() + " Min-Length: " + r.minLength() + " Arm: " + r.getArm());
  }


}
