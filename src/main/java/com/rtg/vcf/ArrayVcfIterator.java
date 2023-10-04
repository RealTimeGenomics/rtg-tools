/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.vcf;

import java.io.IOException;
import java.util.function.Consumer;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.io.IOIterator;
import com.rtg.vcf.header.VcfHeader;

/**
 * An array-backed VcfIterator
 */
@TestClass("com.rtg.vcf.VcfFilterIteratorTest")
public class ArrayVcfIterator implements VcfIterator {
  private final VcfHeader mHeader;
  private final VcfRecord[] mRecords;
  private int mPos;

  /**
   * Constructor
   * @param header the header
   * @param records array of records
   */
  public ArrayVcfIterator(VcfHeader header, VcfRecord... records) {
    mHeader = header;
    mRecords = records;
    mPos = 0;
  }

  @Override
  public VcfHeader getHeader() {
    return mHeader;
  }

  @Override
  public boolean hasNext() {
    return mPos < mRecords.length;
  }

  @Override
  public VcfRecord next() {
    return mRecords[mPos++];
  }

  @Override
  public void forEach(Consumer<? super VcfRecord> action) throws IOException {
    IOIterator.forEach(this, action);
  }

  @Override
  public void close() {
  }
}
