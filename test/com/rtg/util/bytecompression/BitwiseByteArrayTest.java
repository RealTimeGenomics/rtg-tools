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
package com.rtg.util.bytecompression;

/**
 */
public class BitwiseByteArrayTest extends ByteArrayTest {

  @Override
  protected ByteArray getByteArray(final long size, final int bits) {
    return new BitwiseByteArray(size, bits);
  }


  public void testTrivial() {
    final BitwiseByteArray a = new BitwiseByteArray(4, 3, 3, false);
    a.set(0, new byte[] {(byte) 0, (byte) 1, (byte) 2, (byte) 3}, 4);
    assertEquals(0, a.get(0));
    assertEquals(1, a.get(1));
    assertEquals(2, a.get(2));
    assertEquals(3, a.get(3));
  }

  public void testGrowMultiArray() {
    final BitwiseByteArray a = new BitwiseByteArray(0, 3, 5, true); // Array size not a multiple of bits
    for (int i = 0; i < 100; i += 4) {
      a.set(i, new byte[] {(byte) 0, (byte) 1, (byte) 2, (byte) 3}, 4);
      assertEquals(0, a.get(i));
      assertEquals(1, a.get(i + 1));
      assertEquals(2, a.get(i + 2));
      assertEquals(3, a.get(i + 3));
    }

    final BitwiseByteArray b = new BitwiseByteArray(0, 3, 6, true); // Array size a multiple of bits
    for (int i = 0; i < 100; i += 4) {
      b.set(i, new byte[] {(byte) 0, (byte) 1, (byte) 2, (byte) 3}, 4);
      assertEquals(0, b.get(i));
      assertEquals(1, b.get(i + 1));
      assertEquals(2, b.get(i + 2));
      assertEquals(3, b.get(i + 3));
    }
  }

  public void testSizes() {
    final int bits = 3;
    final ByteArray array = new BitwiseByteArray(128L, bits);
    assertEquals(128 / 64 * 8 * bits, array.bytes());
    assertEquals(128, array.length());
  }

  public void testEqualsBig() {
    final byte[] data = new byte[1000];
    for (int i = 0; i < data.length; ++i) {
      data[i] = (byte) (i & 7);
    }
    final BitwiseByteArray big1 = new BitwiseByteArray(data.length, 3, 20, false);
    final BitwiseByteArray big2 = new BitwiseByteArray(data.length, 3, 40, false);
    big1.set(0L, data, data.length);
    big2.set(0L, data, data.length);

    for (int i = 0; i < data.length; ++i) {
      assertEquals(data[i], big1.get(i));
      assertEquals(data[i], big2.get(i));
    }
  }

  public void testUnsupportedOp() {
    final BitwiseByteArray bba = new BitwiseByteArray(5, 3, 20, false);
    try {
      bba.set(3L, (byte) 2);
      fail(".set should not be supported???");
    } catch (UnsupportedOperationException uoe) {
      assertEquals("not supported", uoe.getMessage());
    }
  }

  public void testGrow() {
    final BitwiseByteArray bba = new BitwiseByteArray(0, 3, 5, true);
    final byte[] bs = new byte[1];
    for (long i = 0; i < 1234; ++i) {
      bs[0] = (byte) (i % 8);
      //System.err.println("tSetting: " + i + " : " + bs[0]);
      bba.set(i, bs, 1);
    }
    for (long i = 0; i < 1234; ++i) {
      assertEquals((byte) (i % 8), bba.get(i));
    }
  }

  /**
   * A benchmark method.
   * @param args "bitwise", "compressed" or "normal".
   */
  /*
  public static void main(final String[] args) {
    final long length = 100 * 1024 * 1024;
    final int bits = 3;
    final String which = args.length < 1 ? "bitwise" : args[0];
    // do this several times, to warm up the hot-spot compiler.
    for (int run = 0; run < 4; ++run) {
      final ByteArray array =
        "bitwise".equals(which) ? new BitwiseByteArray(length, bits)
        : "compressed".equals(which) ? new CompressedByteArray(length, 1 << (bits - 1))
        : "bitfield".equals(which) ? new BitfieldByteArray(length, bits)
        : ByteArray.allocate(length);
        System.out.println("============= run #" + run + "============= Speed testing " + array);
        long time0 = System.nanoTime();
        final byte[] tmp = new byte[35];
        for (long pos = 0; pos < length - tmp.length; ++pos) {
          tmp[(int) (pos % tmp.length)] = (byte) ((pos + 3) % (1 << bits));
          if (pos % tmp.length == tmp.length - 1) {
            array.set(pos, tmp, tmp.length);
          }
        }
        long time1 = System.nanoTime();
        final float loadTime = (time1 - time0) / 1.0e9f;
        System.out.println("set time: " + loadTime + " secs");
        System.out.println("        = " + (length / 1e6f) / loadTime + " MB/sec");

        time0 = System.nanoTime();
        final int repeats = 10;
        for (int trial = 0; trial < repeats * length / tmp.length; ++trial) {
          array.get(tmp, (trial * 10231L) % (length - tmp.length), tmp.length);
        }
        time1 = System.nanoTime();
        final float getTime = (time1 - time0) / 1.0e9f;
        System.out.println("get time: " + getTime + " secs / " + repeats);
        System.out.println("        = " + (repeats * length / 1e6f) / getTime + " MB/sec");

        if (array instanceof BitwiseByteArray) {
          final BitwiseByteArray barray = (BitwiseByteArray) array;
          time0 = System.nanoTime();
          long equal = 0L;
          for (int trial = 0; trial < repeats * length / tmp.length; ++trial) {
            equal += barray.countEquals((trial * 10243L) % (length - tmp.length), tmp.length, barray, 0L);
          }
          time1 = System.nanoTime();
          final float countTime = (time1 - time0) / 1.0e9f;
          System.out.println(equal + " equalities found in " + repeats * length + " bytes");
          System.out.println("count time: " + countTime + " secs / " + repeats);
          System.out.println("          = " + (repeats * length / 1e6f) / countTime + " MB/sec");
        }
      }
  }
 */
}
