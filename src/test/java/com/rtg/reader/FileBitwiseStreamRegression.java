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
import java.io.InputStream;
import java.io.OutputStream;

import com.rtg.util.bytecompression.CompressedByteArray;

/**
 * Unit test to try using the <code>FileBitwiseInputStream</code> and
 * the <code>FileBitwiseOutputStream</code> classes with enough data
 * to break the internal integer casting.
 */
public class FileBitwiseStreamRegression extends AbstractFileStreamRegression {

  @Override
  protected long calcLength(int range, long elements) {
    final int bitsPerElement = CompressedByteArray.minBits(range);
    final int bytesPerLong = 8;
    final int bitsPerLong = 64;
    final int chunkSize = bytesPerLong * bitsPerElement;
    return (elements / bitsPerLong) * chunkSize + (elements % bitsPerLong > 0 ? chunkSize : 0);
  }

  @Override
  protected OutputStream createOutputStream(File file, int range) throws IOException {
    return new FileBitwiseOutputStream(file, CompressedByteArray.minBits(range));
  }

  @Override
  protected InputStream createInputStream(File file, int range, long elements, boolean seekable) throws IOException {
    return new FileBitwiseInputStream(file, CompressedByteArray.minBits(range), elements, seekable);
  }

}
