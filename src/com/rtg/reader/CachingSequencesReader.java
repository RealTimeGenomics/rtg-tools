/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This wrapper class caches retrieved sequence and quality data in memory using <code>SoftReference</code> objects.
 */
public final class CachingSequencesReader extends AbstractSequencesReader {

  private abstract class Cache<T> extends HashMap<Long, SoftReference<T>> {
    abstract T readInternal(long sequenceIndex) throws IOException;
    protected T cachedRead(long sequenceIndex) throws IOException {
      final SoftReference<T> softReference = get(sequenceIndex);
      final T cached = (softReference != null) ? softReference.get() : null;
      if (cached != null) {
        return cached;
      }
      final T data = readInternal(sequenceIndex);
      put(sequenceIndex, new SoftReference<>(data));
      return data;
    }
  }

  private abstract class NameCache extends Cache<String> {
    String read(long sequenceIndex) throws IOException {
      return cachedRead(sequenceIndex);
    }
  }

  private abstract class SequenceCache extends CachingSequencesReader.Cache<byte[]> {
    byte[] read(long sequenceIndex) throws IOException {
      final byte[] cached = cachedRead(sequenceIndex);
      return Arrays.copyOf(cached, cached.length);
    }
    int read(long sequenceIndex, byte[] dataOut) throws IOException {
      final byte[] cached = cachedRead(sequenceIndex);
      if (cached.length > dataOut.length) {
        throw new IllegalArgumentException("Array too small got: " + dataOut.length + " required: " + cached.length);
      }
      System.arraycopy(cached, 0, dataOut, 0, cached.length);
      return cached.length;
    }
    int read(long sequenceIndex, byte[] dataOut, int start, int length) throws IOException {
      if (length > dataOut.length) {
        throw new IllegalArgumentException("Array too small got: " + dataOut.length + " required: " + length);
      }
      final byte[] cached = cachedRead(sequenceIndex);
      if (start + length > cached.length) {
        throw new IllegalArgumentException("Requested data not a subset of sequence data.");
      }
      System.arraycopy(cached, start, dataOut, 0, length);
      return cached.length;
    }
  }

  private final SequencesReader mInner;
  private final NameCache mNameCache = new NameCache() {
    @Override
    String readInternal(long sequenceIndex) throws IOException {
      return mInner.name(sequenceIndex);
    }
  };
  private final NameCache mNameSuffixCache = new NameCache() {
    @Override
    String readInternal(long sequenceIndex) throws IOException {
      return mInner.nameSuffix(sequenceIndex);
    }
  };
  private final SequenceCache mDataCache = new SequenceCache() {
    @Override
    byte[] readInternal(long sequenceIndex) throws IOException {
      return mInner.read(sequenceIndex);
    }
  };
  private final SequenceCache mQualityCache = new SequenceCache() {
    @Override
    byte[] readInternal(long sequenceIndex) throws IOException {
      return mInner.readQuality(sequenceIndex);
    }
  };


  /**
   * Constructor
   * @param reader delegate reader
   */
  public CachingSequencesReader(SequencesReader reader) {
    mInner = reader;
  }

  @Override
  public IndexFile index() {
    return mInner.index();
  }

  @Override
  public long numberSequences() {
    return mInner.numberSequences();
  }

  @Override
  public File path() {
    return mInner.path();
  }

  @Override
  public int length(long sequenceIndex) throws IOException {
    return mInner.length(sequenceIndex);
  }

  @Override
  public byte sequenceDataChecksum(long sequenceIndex) throws IOException {
    return mInner.sequenceDataChecksum(sequenceIndex);
  }

  @Override
  public String name(long sequenceIndex) throws IOException {
    return mNameCache.read(sequenceIndex);
  }

  @Override
  public String nameSuffix(long sequenceIndex) throws IOException {
    return mNameSuffixCache.read(sequenceIndex);
  }

  @Override
  public byte[] read(long sequenceIndex) throws IOException {
    return mDataCache.read(sequenceIndex);
  }

  @Override
  public int read(long sequenceIndex, byte[] dataOut) throws IOException {
    return mDataCache.read(sequenceIndex, dataOut);
  }

  @Override
  public int read(long sequenceIndex, byte[] dataOut, int start, int length) throws IOException {
    return mDataCache.read(sequenceIndex, dataOut, start, length);
  }

  @Override
  public byte[] readQuality(long sequenceIndex) throws IOException {
    return mQualityCache.read(sequenceIndex);
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest) throws IOException {
    return mQualityCache.read(sequenceIndex, dest);
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IOException {
    return mQualityCache.read(sequenceIndex, dest, start, length);
  }

  @Override
  public void close() throws IOException {
    mInner.close();
  }

  @Override
  public NamesInterface names() throws IOException {
    return mInner.names();
  }

  @Override
  public long lengthBetween(long start, long end) throws IOException {
    return mInner.lengthBetween(start, end);
  }

  @Override
  public int[] sequenceLengths(long start, long end) throws IOException {
    return mInner.sequenceLengths(start, end);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || !o.getClass().equals(CachingSequencesReader.class)) {
      return false;
    }
    final CachingSequencesReader other = (CachingSequencesReader) o;
    return mInner.equals(other.mInner);
  }

  @Override
  public int hashCode() {
    return mInner.hashCode();
  }

  @Override
  public SequencesReader copy() {
    throw new UnsupportedOperationException();
  }
}
