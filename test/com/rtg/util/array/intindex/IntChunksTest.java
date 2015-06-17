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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Test Chunks
 */
public class IntChunksTest extends AbstractIntIndexTest {

  private static final int CHUNK_BITS = 29;

  @Override
  protected IntIndex create(final long length) {
    return new IntChunks(length);
  }

  @Override
  protected IntIndex create(final long length, final int bits) {
    return new IntChunks(length, bits);
  }

  public void testChunkSize() {
    final IntChunks dc = new IntChunks(100L);
    dc.integrity();
    assertEquals(100, dc.length());
    assertEquals(1L << 28, dc.chunkSize());
  }

  public void testTooLong() {
    // Only try this if there is not too much memory available ...
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new IntChunks((1L << CHUNK_BITS) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
        fail();
      } catch (final RuntimeException e) {
        //expected
      }
      try {
        new IntChunks((1L << CHUNK_BITS - 1) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
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
    final IntIndex ix0 = new IntChunks(size - 1, bits);
    ix0.integrity();
    final IntIndex ix1 = new IntChunks(size, bits);
    ix1.integrity();
    final IntIndex ix2 = new IntChunks(size + 1, bits);
    ix2.integrity();
  }


  /** Test extension. */
  public void testExtensiona() {
    final IntChunks lc = new IntChunks(5, 200, 7);
    lc.integrity();
    assertEquals(200, lc.length());
    lc.set(0, 42);
    assertEquals(42, lc.get(0));
    lc.set(255, 43);
    assertEquals(43, lc.get(255));

    assertEquals(200, lc.extendBy(10));
    assertEquals(210, lc.length());

    assertEquals(210, lc.extendBy(10));
    assertEquals(220, lc.length());

    assertEquals(220, lc.extendBy(100));
    assertEquals(320, lc.length());

    assertEquals(320, lc.extendBy(1000));
    assertEquals(1320, lc.length());

    lc.set(1407, 44);
    assertEquals(44, lc.get(1407));

    try {
      lc.extendBy(-1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("-1", e.getMessage());
    }
  }

  private void set(final int start, final IntChunks lc) {
    for (int i = start; i < lc.length(); i++) {
      lc.set(i, i + 1);
    }
  }

  private void check(final IntChunks lc) {
    for (int i = 0; i < lc.length(); i++) {
      assertEquals(i + 1, lc.get(i));
    }
  }

  private void setSigned(final int start, final IntChunks lc) {
    for (int i = start; i < lc.length(); i++) {
      lc.setSigned(i, -(i + 1) + 10);
    }
  }

  private void checkSigned(final IntChunks lc) {
    for (int i = 0; i < lc.length(); i++) {
      assertEquals(-(i + 1) + 10, lc.getSigned(i));
    }
  }

  public void test() {
    final IntChunks ic = new IntChunks(42);
    set(0, ic);
    check(ic);

    setSigned(0, ic);
    checkSigned(ic);
  }

  //When extend less than one chunks worth
  public void testExtendShortTotal1() {
    final IntChunks ic = new IntChunks(20, 7);
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
    final IntChunks ic = new IntChunks(20, 7);
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

  /** Test extension. */
  public void testExtensionb() {
    final IntChunks ic = new IntChunks(200, 7);
    ic.integrity();
    assertEquals(2, ic.arrayLength());
    assertEquals(200, ic.length());
    ic.set(0, 42);
    assertEquals(42, ic.get(0));
    ic.set(199, 43);
    assertEquals(43, ic.get(199));
    set(0, ic);
    check(ic);

    assertEquals(200, ic.extendBy(10));
    assertEquals(210, ic.length());
    assertEquals(2, ic.arrayLength());

    assertEquals(210, ic.extendBy(10));
    assertEquals(220, ic.length());
    assertEquals(2, ic.arrayLength());

    assertEquals(220, ic.extendBy(0));
    assertEquals(220, ic.length());
    assertEquals(2, ic.arrayLength());

    assertEquals(220, ic.extendBy(100));
    assertEquals(320, ic.length());
    assertEquals(6, ic.arrayLength());
    set(200, ic);
    check(ic);

    assertEquals(320, ic.extendBy(1000));
    assertEquals(1320, ic.length());
    assertEquals(14, ic.arrayLength());
    set(320, ic);
    check(ic);

    try {
      ic.extendBy(-1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("-1", e.getMessage());
    }
  }

  public void testTrim() {
    checkTrim(20, 12);
    checkTrim(20, 20);
    checkTrim(10, 0);
    checkTrim(0, 0);
  }

  private void checkTrim(final int initLength, final int trimLength) {
    final IntChunks ic = new IntChunks(initLength, 3);
    for (int i = 0; i < initLength; i++) {
      ic.set(i, i + 1);
    }
    ic.integrity();
    ic.trim(trimLength);
    ic.integrity();
    assertEquals(trimLength, ic.length());
    for (int i = 0; i < trimLength; i++) {
      assertEquals(i + 1, ic.get(i));
    }
    try {
      ic.get(trimLength);
      fail();
    } catch (final Exception e) {
      //e.printStackTrace();
    }
  }

  public void testTooLong3() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new IntChunks(((long) Integer.MAX_VALUE) << 8, 7);
      } catch (final RuntimeException e) {
        assertEquals("length requested too long length=549755813632 mChunkSize=128", e.getMessage());
      }
    }
  }

  public void testTooLong4() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new IntChunks(Integer.MAX_VALUE + 1L, 0);
      } catch (final RuntimeException e) {
        assertEquals("length requested too long length=2147483648 mChunkSize=1", e.getMessage());
      }
    }
  }

  public void testBug() {
    try {
      new IntChunks(2, 100, 4);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("too few chunks for length=100 chunks=2 chunkBits=4", e.getMessage());
    }
  }


  public void testSerial() throws IOException {
    final IntChunks la = new IntChunks(10);
    for (int i = 0; i < 10; i++) {
      la.set(i, i * 4 + 7);
    }
    final ByteArrayOutputStream out =  new ByteArrayOutputStream();
    la.save(new ObjectOutputStream(out));
    final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    final IntIndex index2 = IntCreate.loadIndex(new ObjectInputStream(in));
    assertTrue(index2 instanceof IntChunks);
    assertEquals(la.length(), index2.length());
    for (int i = 0; i < 10; i++) {
      assertEquals(la.get(i), index2.get(i));
    }
  }}


