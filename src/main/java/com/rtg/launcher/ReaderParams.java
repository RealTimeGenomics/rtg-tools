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
package com.rtg.launcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.reader.SequencesReader;
import com.rtg.util.Params;


/**
 * Encapsulates the desired SequencesReader to be opened.
 */
@JumbleIgnore
public abstract class ReaderParams implements Params, Closeable {

  /**
   * Open or return the SequencesReader.
   * @return the SequencesReader. The same reader instance is returned on succesive calls.
   */
  public abstract SequencesReader reader();

  /**
   * Get the lengths of the sequences in the reader.
   * This will result in the reader being opened if not already.
   * @return the lengths.
   * @throws IOException if an I/O Error occurs
   */
  public abstract int[] lengths() throws IOException;

  /**
   * Get the length of the longest sequence.
   * This will result in the reader being opened if not already.
   * @return the length of the longest sequence.
   */
  public abstract long maxLength();

  /**
   * If necessary carefully close the reader.
   * @throws IOException if an IO error occurs
   */
  @Override
  public abstract void close() throws IOException;

  /**
   * Returns the directory corresponding to the SequencesReader source.
   * @return directory, which may be null if the reader is not obtained from disk.
   */
  public abstract File directory();

}

