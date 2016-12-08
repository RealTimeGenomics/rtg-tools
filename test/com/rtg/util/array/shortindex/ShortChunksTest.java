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
package com.rtg.util.array.shortindex;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Test Chunks
 */
public class ShortChunksTest extends AbstractShortIndexTest {

  private static final int CHUNK_BITS = 29;

  @Override
  protected ShortIndex create(final long length) {
    return new ShortChunks(length);
  }

  @Override
  protected ShortIndex create(final long length, final int bits) {
    return new ShortChunks(length, bits);
  }


  public void testChunkSize() {
    final ShortChunks dc = new ShortChunks(100L);
    dc.integrity();
    assertEquals(100, dc.length());
    assertEquals(1L << 29, dc.chunkSize());
  }

  public void testTooLong() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new ShortChunks((1L << CHUNK_BITS) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
        fail("RuntimeException expected");
      } catch (final RuntimeException e) {
        //expected
      }
      try {
        new ShortChunks((1L << CHUNK_BITS - 1) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
        fail("OutOfMemoryError expected");
      } catch (final OutOfMemoryError e) {
        //expected
      }
    }
  }

  /** Tests allocations of chunks and fomula for computing how many chunks there are. */
  public void testEdgeCase() {
    final int bits = 3;
    final int size = 1 << bits;
    final ShortIndex ix0 = new ShortChunks(size - 1, bits);
    ix0.integrity();
    final ShortIndex ix1 = new ShortChunks(size, bits);
    ix1.integrity();
    final ShortIndex ix2 = new ShortChunks(size + 1, bits);
    ix2.integrity();
  }

  private void set(final int start, final ShortChunks bc) {
    for (int i = start; i < bc.length(); ++i) {
      bc.setSigned(i, -128 + (i + 1) % 255);
    }
  }

  private void check(final ShortChunks bc) {
    for (int i = 0; i < bc.length(); ++i) {
      assertEquals(-128 + (i + 1) % 255, bc.getSigned(i));
    }
  }

  private void setSigned(final int start, final ShortChunks lc) {
    for (int i = start; i < lc.length(); ++i) {
      lc.setSigned(i, -(i + 1) + 10);
    }
  }

  private void checkSigned(final ShortChunks lc) {
    for (int i = 0; i < lc.length(); ++i) {
      assertEquals(-(i + 1) + 10, lc.getSigned(i));
    }
  }

  public void test() {
    final ShortChunks ic = new ShortChunks(42);
    set(0, ic);
    check(ic);

    setSigned(0, ic);
    checkSigned(ic);
  }

  //When extend less than one chunks worth
  public void testExtendShortTotal1() {
    final ShortChunks ic = new ShortChunks(20, 7);
    ic.integrity();
    assertEquals(1, ic.arrayLength());
    assertEquals(20, ic.length());
    assertEquals(20, ic.totalLength());
    ic.extendBy(20);
    assertEquals(1, ic.arrayLength());
    assertEquals(40, ic.length());
    assertEquals(40, ic.totalLength());
    ic.integrity();
  }

  //When extend less than one chunks worth
  public void testExtendShortTotal2() {
    final ShortChunks ic = new ShortChunks(20, 7);
    ic.integrity();
    assertEquals(1, ic.arrayLength());
    assertEquals(20, ic.length());
    assertEquals(20, ic.totalLength());
    ic.extendBy(5);
    assertEquals(1, ic.arrayLength());
    assertEquals(25, ic.length());
    assertEquals(30, ic.totalLength());
    ic.integrity();
  }

  public void testTrim() {
    checkTrim(20, 12);
    checkTrim(20, 20);
    checkTrim(10, 0);
    checkTrim(0, 0);
  }

  private void checkTrim(final int initLength, final int trimLength) {
    final ShortChunks ic = new ShortChunks(initLength, 3);
    for (int i = 0; i < initLength; ++i) {
      ic.set(i, i + 1);
    }
    ic.integrity();
    ic.trim(trimLength);
    ic.integrity();
    assertEquals(trimLength, ic.length());
    for (int i = 0; i < trimLength; ++i) {
      assertEquals(i + 1, ic.get(i));
    }
    try {
      ic.get(trimLength);
      fail();
    } catch (final Exception e) {
      //e.printStackTrace();
    }
  }

  /** Test extension. */
  public void testExtensionb() {
    final ShortChunks sc = new ShortChunks(200, 7);
    sc.integrity();
    assertEquals(2, sc.arrayLength());
    assertEquals(200, sc.length());
    sc.set(0, 42);
    assertEquals(42, sc.get(0));
    sc.set(199, 43);
    assertEquals(43, sc.get(199));
    set(0, sc);
    check(sc);

    assertEquals(200, sc.extendBy(10));
    assertEquals(210, sc.length());
    assertEquals(2, sc.arrayLength());
    set(200, sc);
    check(sc);

    assertEquals(210, sc.extendBy(10));
    assertEquals(220, sc.length());
    assertEquals(2, sc.arrayLength());

    assertEquals(220, sc.extendBy(0));
    assertEquals(220, sc.length());
    assertEquals(2, sc.arrayLength());

    assertEquals(220, sc.extendBy(100));
    assertEquals(320, sc.length());
    assertEquals(6, sc.arrayLength());
    set(210, sc);
    check(sc);

    assertEquals(320, sc.extendBy(1000));
    assertEquals(1320, sc.length());
    assertEquals(14, sc.arrayLength());
    set(320, sc);
    check(sc);

    try {
      sc.extendBy(-1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("-1", e.getMessage());
    }
  }


  public void testSerial() throws IOException {
    final ShortChunks la = new ShortChunks(10);
    for (int i = 0; i < 10; ++i) {
      la.set(i, i * 4 + 7);
    }
    final ByteArrayOutputStream out =  new ByteArrayOutputStream();
    la.save(new ObjectOutputStream(out));
    final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    final ShortIndex index2 = ShortCreate.loadIndex(new ObjectInputStream(in));
    assertTrue(index2 instanceof ShortChunks);
    assertEquals(la.length(), index2.length());
    for (int i = 0; i < 10; ++i) {
      assertEquals(la.get(i), index2.get(i));
    }
  }
}
