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
package com.rtg.util.array.intindex;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.rtg.util.array.IndexType;

/**
 * Contains the only public ways of constructing an IntIndex.
 */
public final class IntCreate {
  private IntCreate() { // private so cant create an instance of this utility class
  }

  /**
   * Create a new IntIndex of the specified length.
   * Chooses an appropriate implementation depending on the length.
   * @param length number of entries in the IntIndex.
   * @return an IntIndex.
   * @exception NegativeArraySizeException if length negative.
   */
  public static IntIndex createIndex(final long length) {
    if (length < 0) {
      throw new NegativeArraySizeException("Negative length=" + length);
    }
    if (length <= IntIndex.MAX_LENGTH) {
      return new IntArray(length);
    } else {
      return new IntChunks(length);
    }
  }

  /**
   * loads an index saved by {@link IntIndex#save(java.io.ObjectOutputStream)}
   * @param stream stream to load from
   * @return the index stored in the stream
   * @throws IOException if an IO error occurs
   */
  public static IntIndex loadIndex(ObjectInputStream stream) throws IOException {
    final IndexType type = IndexType.values()[stream.readInt()];
    switch (type) {
      case ARRAY:
        return IntArray.loadIndex(stream);
      case CHUNKS:
        return IntChunks.loadIndex(stream);
      default:
        throw new IOException("Unrecognized type");
    }
  }
}

