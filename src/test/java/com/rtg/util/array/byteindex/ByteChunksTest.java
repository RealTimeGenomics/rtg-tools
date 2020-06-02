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
package com.rtg.util.array.byteindex;



/**
 * Test Chunks
 */
public class ByteChunksTest extends AbstractByteIndexTest {

  private static final int CHUNK_BITS = 29;

  @Override
  protected ByteIndex create(final long length) {
    return new ByteChunks(length);
  }

  @Override
  protected ByteIndex create(final long length, final int bits) {
    return new ByteChunks(length, bits);
  }

  public void testChunkSize() {
    final ByteChunks dc = new ByteChunks(100L);
    dc.integrity();
    assertEquals(100, dc.length());
    assertEquals(1L << 30, dc.chunkSize());
  }

  public void testTooLong() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new ByteChunks((1L << CHUNK_BITS) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
        fail("RuntimeException expected");
      } catch (final RuntimeException e) {
        //expected
      }
      try {
        new ByteChunks((1L << CHUNK_BITS - 1) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
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
    final ByteIndex ix0 = new ByteChunks(size - 1, bits);
    ix0.integrity();
    final ByteIndex ix1 = new ByteChunks(size, bits);
    ix1.integrity();
    final ByteIndex ix2 = new ByteChunks(size + 1, bits);
    ix2.integrity();
  }

  public void testArrayCopy() {
    final ByteChunks bc = new ByteChunks(32, 3);
    final byte[] bytes = new byte[32];
    for (int i = 0; i < bytes.length; ++i) {
      bytes[i] = (byte) i;
    }
    bc.copyBytes(bytes, 0, 0, bytes.length);
    for (int i = 0; i < bytes.length; ++i) {
      assertEquals(bytes[i], bc.getByte(i));
    }
    final byte[] out = new byte[32];
    bc.getBytes(out, 0, 0, 32);
    for (int i = 0; i < out.length; ++i) {
      assertEquals(i, out[i]);
    }

    bc.getBytes(out, 10, 5, 15);
    for (int i = 0; i < out.length; ++i) {
      if (i < 10 || i > 24) {
        assertEquals(i, out[i]);
      } else {
        assertEquals(i - 5, out[i]);
      }
    }
  }

  public void testArrayCopy2() {
    final ByteChunks bc = new ByteChunks(32, 3);
    final byte[] bytes = new byte[32];
    for (int i = 0; i < bytes.length; ++i) {
      bytes[i] = (byte) i;
    }
    bc.copyBytes(bytes, 20, 5, 12);
    for (int i = 0; i < 32; ++i) {
      if (i < 5 || i > 16) {
        assertEquals(0, bc.getByte(i));
      } else {
        assertEquals(bytes[i + 15], bc.getByte(i));
      }
    }
  }

  private void set(final int start, final ByteChunks bc) {
    for (int i = start; i < bc.length(); ++i) {
      bc.setSigned(i, -128 + (i + 1) % 255);
    }
  }

  private void check(final ByteChunks bc) {
    for (int i = 0; i < bc.length(); ++i) {
      assertEquals(-128 + (i + 1) % 255, bc.getSigned(i));
    }
  }

  private void setSigned(final int start, final ByteChunks lc) {
    for (int i = start; i < lc.length(); ++i) {
      lc.setSigned(i, -(i + 1) + 10);
    }
  }

  private void checkSigned(final ByteChunks lc) {
    for (int i = 0; i < lc.length(); ++i) {
      assertEquals(-(i + 1) + 10, lc.getSigned(i));
    }
  }

  public void test() {
    final ByteChunks ic = new ByteChunks(42);
    set(0, ic);
    check(ic);

    setSigned(0, ic);
    checkSigned(ic);
  }

  //When extend less than one chunks worth
  public void testExtendShortTotal1() {
    final ByteChunks ic = new ByteChunks(20, 7);
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
    final ByteChunks ic = new ByteChunks(20, 7);
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
    final ByteChunks bc = new ByteChunks(200, 7);
    bc.integrity();
    assertEquals(2, bc.arrayLength());
    assertEquals(200, bc.length());
    bc.set(0, 42);
    assertEquals(42, bc.get(0));
    bc.set(199, 43);
    assertEquals(43, bc.get(199));
    set(0, bc);
    check(bc);

    assertEquals(200, bc.extendBy(10));
    assertEquals(210, bc.length());
    assertEquals(2, bc.arrayLength());
    set(200, bc);
    check(bc);

    assertEquals(210, bc.extendBy(10));
    assertEquals(220, bc.length());
    assertEquals(2, bc.arrayLength());

    assertEquals(220, bc.extendBy(0));
    assertEquals(220, bc.length());
    assertEquals(2, bc.arrayLength());

    assertEquals(220, bc.extendBy(100));
    assertEquals(320, bc.length());
    assertEquals(6, bc.arrayLength());
    set(210, bc);
    check(bc);

    assertEquals(320, bc.extendBy(1000));
    assertEquals(1320, bc.length());
    assertEquals(14, bc.arrayLength());
    set(320, bc);
    check(bc);

    try {
      bc.extendBy(-1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("length=-1", e.getMessage());
    }
  }

  public void testTrim() {
    checkTrim(20, 12);
    checkTrim(20, 20);
    checkTrim(10, 0);
    checkTrim(0, 0);
  }

  private void checkTrim(final int initLength, final int trimLength) {
    final ByteChunks ic = new ByteChunks(initLength, 3);
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

  public void testTooLong3() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new ByteChunks(((long) Integer.MAX_VALUE) << 8, 7);
      } catch (final RuntimeException e) {
        assertEquals("length requested too long length=549755813632 mChunkSize=128", e.getMessage());
      }
    }
  }

  public void testTooLong4() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new ByteChunks(Integer.MAX_VALUE + 1L, 0);
      } catch (final RuntimeException e) {
        assertEquals("length requested too long length=2147483648 mChunkSize=1", e.getMessage());
      }
    }
  }

}


