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

//import com.rtg.util.PortableRandom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;


/**
 * Class contains hash function used in preread verification process.
 *
 */
public final class PrereadHashFunction {

  /** Randomly generated arrays used to compute <code>irvineHash</code> codes */
  static final long[] HASH_BLOCKS;
  static {
    HASH_BLOCKS = new long[256];
    final Random r = new Random(1L); // use same seed for deterministic behavior
    for (int i = 0; i < 256; i++) {
      HASH_BLOCKS[i] = r.nextLong();
    }
  }

  /**
   * Create a new <Code>PrereadHashFunction</Code>
   */
  public PrereadHashFunction() {
    reset();
  }

  /**
   * Reset the function
   */
  public void reset() {
    mHash = 0L;
    mCount = 0;
  }

  /**
   * Adds to hash the given int. This hash function
   * exhibits better statistical behavior than String <code>hashCode()</code> and
   * has speed comparable to CRC32.
   * @param in integer to checksum (should not be null)
   */
  public void irvineHash(final int in) {
      mHash = Long.rotateLeft(mHash, 1) ^ HASH_BLOCKS[(in + mCount++) & 0xFF];
  }

  /**
   * Hash a long explicitly, but hashing each of its bytes sequentially.
   *
   * @param in0 integer to checksum
   */
  public void irvineHash(final long in0) {
    long in = in0;
    for (int k = 0; k < 8; k++) {
      irvineHash((int) (in & 0xFF));
      in = in >>> 8;
    }
  }

  /**
   * Incrementally hashes stream
   * @param in data to hash
   * @throws java.io.IOException if a problem occurs in stream
   */
  public void irvineHash(final InputStream in) throws IOException {
    final byte[] buff = new byte[4096];
    int len;
    while ((len = in.read(buff, 0, buff.length)) > 0) {
      irvineHash(buff, 0, len);
    }
  }

  /**
   * Hashes bytes in an array
   * @param buff data to hash
   */
  public void irvineHash(byte[] buff) {
    irvineHash(buff, 0, buff.length);
  }

  /**
   * Hashes bytes in an array
   * @param buff data to hash
   * @param length number of entries in the array to hash
   */
  public void irvineHash(byte[] buff, int length) {
    irvineHash(buff, 0, length);
  }

  /**
   * Hashes bytes in an array
   * @param buff data to hash
   * @param offset start position
   * @param length number of entries in the array to hash
   */
  public void irvineHash(byte[] buff, int offset, int length) {
    for (int i = offset; i < offset + length; i++) {
      irvineHash(buff[i]);
    }
  }

  /**
   * Returns a 64 bit hash of the given stream. This hash function
   * exhibits better statistical behavior than String <code>hashCode()</code> and
   * has speed comparable to CRC32.
   * @param s string to hash
   */
  public void irvineHash(final String s) {
      for (int i = 0; i < s.length(); i++) {
        irvineHash(s.charAt(i));
      }
  }

  /**
   * Return the hash
   * @return hash
   */
  public long getHash() {
    return mHash;
  }

  private long mHash;
  private int mCount;

}
