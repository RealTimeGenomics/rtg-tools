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
import java.io.OutputStream;

import com.rtg.util.array.byteindex.ByteChunks;
import com.rtg.util.array.longindex.LongChunks;

/**
 * Simple implementation of {@link PrereadNames}
 */
public class SimplePrereadNames implements PrereadNamesInterface {

  private final ByteChunks mNameBytes = new ByteChunks(0);
  private final LongChunks mPointers = new LongChunks(1);
  private long mTotalNameSize = 0;

  /**
   * Default constructor
   */
  public SimplePrereadNames() {
    //This is here to make the first pointer explicit
    mPointers.set(0, 0);
  }

  @Override
  public long length() {
    return mPointers.length() - 1;
  }

  @Override
  public String name(long id) {
    return new String(getNameBytes(id));
  }

  byte[] getNameBytes(long id) {
    final long start = mPointers.get(id);
    final long end = mPointers.get(id + 1);
    final int len = (int) (end - start);
    final byte[] out = new byte[len];
    mNameBytes.getBytes(out, 0, start, len);
    return out;
  }

  /**
   * Add a name after the given id
   * @param id please make this the current length of this
   * @param name the name to add
   */
  public void setName(long id, String name) {
    assert id == length();
    final byte[] nameBytes = name.getBytes();
    final int length = nameBytes.length;
    mNameBytes.extendBy(length);
    mNameBytes.copyBytes(nameBytes, 0, mTotalNameSize, length);
    mTotalNameSize += length;
    mPointers.extendBy(1);
    mPointers.set(id + 1, mTotalNameSize);
  }

  /**
   * Calculate the checksum of the names in a manner compatible with
   * how the checksum is calculated in the SDF.
   *
   * @return the checksum of the names.
   */
  @Override
  public long calcChecksum() {
    final PrereadHashFunction namef = new PrereadHashFunction();
    for (long k = 0; k < length(); ++k) {
      final byte[] name = getNameBytes(k);
      namef.irvineHash(name);
      namef.irvineHash(name.length);
    }
    return namef.getHash();
  }

  /**
   * Returns size of object in bytes
   * @return size of object in no of bytes
   */
  @Override
  public long bytes() {
    //low balling estimate at 2 pointers and a long per entry. TODO make this more reasonable
    return mNameBytes.bytes() + mPointers.bytes();
  }

  @Override
  public void writeName(Appendable a, long id) throws IOException {
    a.append(name(id));
  }

  @Override
  public void writeName(OutputStream stream, long id) throws IOException {
    stream.write(getNameBytes(id));
  }

}
