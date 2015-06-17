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
package com.rtg.util.array.longindex;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.rtg.util.array.IndexType;

/**
 * Contains the only public ways of constructing a LongIndex.
 */
public final class LongCreate {
  private LongCreate() { // private so cant create an instance of this utility class
  }

  /**
   * Create a new LongIndex of the specified length.
   * Chooses an appropriate implementation depending on the length.
   * @param length number of entries in the LongIndex.
   * @return a LongIndex.
   * @exception NegativeArraySizeException if length negative.
   */
  public static LongIndex createIndex(final long length) {
    if (length < 0) {
      throw new NegativeArraySizeException("Negative length=" + length);
    }
    // SAI: It seems it is not always possible to get exactly Integer.MAX_VALUE
    // array entries.  Perhaps the JVM uses some slots for housekeeping.
    if (length <= Integer.MAX_VALUE - 5) {
      return new LongArray(length);
    } else {
      return new LongChunks(length);
    }
  }

  /**
   * Create extensible long array
   * @return the array
   */
  public static LongChunks createExtensibleIndex() {
    return new LongChunks(0, 20); //8MiB per chunk
  }

  /**
   * loads an index saved by {@link LongIndex#save(java.io.ObjectOutputStream)}
   * @param stream stream to load from
   * @return the index stored in the stream
   * @throws IOException if an IO error occurs
   */
  public static LongIndex loadIndex(ObjectInputStream stream) throws IOException {
    final IndexType type = IndexType.values()[stream.readInt()];
    switch (type) {
      case ARRAY:
        return LongArray.loadIndex(stream);
      case CHUNKS:
        return LongChunks.loadIndex(stream);
      default:
        throw new IOException("Unrecognized type");
    }
  }
}

