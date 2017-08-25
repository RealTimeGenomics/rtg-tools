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
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.mode.SequenceType;

/**
 * A wrapper for a sequences reader which reverse complements bases and reverse
 * quality information.
 */
public final class ReverseComplementingReader extends AbstractSequencesReader {

  private final SequencesReader mUnderlyingReader;

  /**
   * Construct a new reverse complementing reader wrapping another reader.
   *
   * @param reader a reader
   * @exception NullPointerException if <code>reader</code> is null
   * @exception IllegalArgumentException if the underlying type is not nucleotides
   */
  public ReverseComplementingReader(final SequencesReader reader) {
    if (reader == null) {
      throw new NullPointerException();
    }
    if (reader.type() != SequenceType.DNA) {
      throw new IllegalArgumentException();
    }
    mUnderlyingReader = reader;
  }


  static void reverse(final byte[] x, final int start, final int length) {
    int l = start;
    int r = start + length - 1;
    while (l < r) {
      final byte b = x[l];
      x[l] = x[r];
      x[r] = b;
      ++l;
      --r;
    }
  }

  @Override
  public IndexFile index() {
    return mUnderlyingReader.index();
  }


  // Direct accessor methods
  @Override
  public int readQuality(final long sequenceIndex, final byte[] dest) throws IllegalArgumentException, IOException {
    final int r = mUnderlyingReader.readQuality(sequenceIndex, dest);
    reverse(dest, 0, r);
    return r;
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IllegalArgumentException, IOException {
    final int r = mUnderlyingReader.readQuality(sequenceIndex, dest, start, length);
    reverse(dest, 0, r);
    return r;
  }

  @Override
  public int read(final long sequenceIndex, final byte[] dataOut) throws IllegalArgumentException, IOException {
    final int r = mUnderlyingReader.read(sequenceIndex, dataOut);
    DNA.reverseComplementInPlace(dataOut, 0, r);
    return r;
  }

  @Override
  public int read(final long sequenceIndex, final byte[] dataOut, int start, int length) throws IllegalArgumentException, IOException {
    final int r = mUnderlyingReader.read(sequenceIndex, dataOut, start, length);
    DNA.reverseComplementInPlace(dataOut, start, start + r);
    return r;
  }

  @Override
  public int length(final long sequenceIndex) throws IOException {
    return mUnderlyingReader.length(sequenceIndex);
  }

  @Override
  public byte sequenceDataChecksum(long sequenceIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String name(final long sequenceIndex) throws IOException {
    return mUnderlyingReader.name(sequenceIndex);
  }

  @Override
  public String fullName(final long sequenceIndex) throws IOException {
    return name(sequenceIndex);
  }

  @Override
  public File path() {
    return mUnderlyingReader.path();
  }

  @Override
  public void close() throws IOException {
    mUnderlyingReader.close();
  }

  @Override
  public NamesInterface names() throws IOException {
    return mUnderlyingReader.names();
  }

  private void swap(final long[] x, final int a, final int b) {
    final long t = x[a];
    x[a] = x[b];
    x[b] = t;
  }

  @Override
  public long numberSequences() {
    return mUnderlyingReader.numberSequences();
  }

  @Override
  public long[] residueCounts() {
    final long[] q = mUnderlyingReader.residueCounts();
    final long[] c = Arrays.copyOf(q, q.length);
    swap(c, DNA.A.ordinal(), DNA.T.ordinal());
    swap(c, DNA.C.ordinal(), DNA.G.ordinal());
    return c;
  }

  @Override
  public long lengthBetween(final long start, final long end) throws IOException {
    return mUnderlyingReader.lengthBetween(start, end);
  }

  /**
   * Puts lengths of sequences in an array and returns it.
   * Lightly tested
   *
   * @param start starting sequence
   * @param end ending sequence (excl)
   * @return array of lengths
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int[] sequenceLengths(final long start, final long end) throws IOException {
    return mUnderlyingReader.sequenceLengths(start, end);
  }

  @Override
  public double[] positionQualityAverage() {
    final double[] av = mUnderlyingReader.positionQualityAverage();
    int l = 0;
    int r = av.length - 1;
    while (l < r) {
      final double b = av[l];
      av[l] = av[r];
      av[r] = b;
      ++l;
      --r;
    }
    return av;
  }

  @Override
  public SequencesReader copy() {
    return new ReverseComplementingReader(mUnderlyingReader.copy());
  }

  @Override
  public String nameSuffix(long sequenceIndex) throws IOException {
    return mUnderlyingReader.nameSuffix(sequenceIndex);
  }

  @Override
  public String getReadMe() throws IOException {
    return mUnderlyingReader.getReadMe();
  }

}

