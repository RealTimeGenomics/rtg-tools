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

package com.rtg.util.arithcode;

import java.util.Random;

import com.rtg.util.array.byteindex.ByteChunks;

import junit.framework.TestCase;

/**
 * Test of <code>ArithEncoder</code> and <code>ArithDecoder</code>.
 */
public class ArithTest extends TestCase {

  //simple test just one block
  public void testSimple() {
    final ByteChunks bc = new ByteChunks(0);
    final OutputBytes ob = new OutputBytes(bc);
    final ArithEncoder en = new ArithEncoder(ob);

    assertEquals(0, en.endBlock());
    en.encode(0, 1, 5);
    en.encode(3, 5, 5);
    en.encode(0, 6, 7);
    final long eb1 = en.endBlock();
    assertTrue(0 < eb1);
    en.close();
    assertEquals(eb1, en.endBlock());

    final ArithDecoder de = new ArithDecoder(new InputBytes(bc, 0, eb1));

    final int c1 = de.getCurrentSymbolCount(5);
    assertTrue(0 <= c1 && c1 < 1);
    de.removeSymbolFromStream(0, 1, 5);

    final int c2 = de.getCurrentSymbolCount(5);
    assertTrue(3 <= c2 && c2 < 5);
    de.removeSymbolFromStream(3, 5, 5);

    final int c3 = de.getCurrentSymbolCount(7);
    assertTrue(0 <= c3 && c2 < 6);
    de.removeSymbolFromStream(0, 6, 7);
  }

  //multiple variable length blocks
  public void testBig() {
    randomTest(3, 1, 142, 1132, new StaticModel(new int[] {3, 2, 7}));
    randomTest(13, 10, 42, 132, new UniformModel(13));
    randomTest(3, 3, 142, 1132, new StaticModel(new int[] {3, 2, 7}));
  }

  /**
   *
   * @param range of individual symbols.
   * @param blocks number of different blocks.
   * @param seedi for lengths of blocks.
   * @param seedj for symbols being generated.
   * @param am model capable of handling range.
   */
  private void randomTest(final int range, final int blocks, final int seedi, final int seedj, final ArithCodeModel am) {
    final Random randi = new Random(seedi);
    final Random randj = new Random(seedj);
    final int[][] ra = new int[blocks][];
    for (int i = 0; i < ra.length; ++i) {
      ra[i] = new int[randi.nextInt(50)];
      for (int j = 0; j < ra[i].length; ++j) {
        ra[i][j] = randj.nextInt(range);
      }
    }

    final ByteChunks bc = new ByteChunks(0);
    final ArithEncoder en = new ArithEncoder(new OutputBytes(bc));
    final long[] positions = new long[ra.length + 1];
    positions[0] = en.endBlock();
    for (int i = 0; i < ra.length; ++i) {
      for (int j = 0; j < ra[i].length; ++j) {
        am.encode(en, ra[i][j]);
      }
      positions[i + 1] = en.endBlock();
    }
    //    for (int i = 0; i < ra.length; ++i) {
    //      System.err.println("[" + i + "] " + positions[i] + "  " + ra[i].length);
    //    }
    //    System.err.println("[" + ra.length + "] " + positions[ra.length]);

    en.close();
    for (int i = 0; i < ra.length; ++i) {
      final InputBytes ib = new InputBytes(bc, positions[i], positions[i + 1]);
      final ArithDecoder de = new ArithDecoder(ib);
      for (int j = 0; j < ra[i].length; ++j) {
        final int sym = am.decode(de);
        assertEquals("i=" + i + " j=" + j, ra[i][j], sym);
      }
    }
  }

}
