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
package com.rtg.util.test;

import com.rtg.util.PortableRandom;

/**
 * Generate random DNA nucleotides according to the uniform distribution over
 * A, G, C, and T.
 */
public final class RandomDna {

  private RandomDna() { }

  private static final PortableRandom RANDOM = new PortableRandom();

  /**
   * Random nucleotide string.
   *
   * @param length length of string
   * @param seed seed for random number generator
   * @return random string
   */
  public static String random(final int length, final long seed) {
    return random(length, new PortableRandom(seed));
  }

  /**
   * Random nucleotide string.
   *
   * @param len length of string
   * @param r random number generator
   * @return random string
   */
  public static String random(final int len, final PortableRandom r) {
    final StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; ++i) {
      switch (r.nextInt(4)) {
      case 0:
        sb.append('A');
        break;
      case 1:
        sb.append('C');
        break;
      case 2:
        sb.append('G');
        break;
      default:
        sb.append('T');
        break;
      }
    }
    return sb.toString();
  }

  /**
   * Random nucleotide string.
   *
   * @param len length of string
   * @return random string
   */
  public static String random(final int len) {
    return random(len, RANDOM);
  }

  /**
   * Main program.
   *
   * @param args size of sequence
   */
  public static void main(final String[] args) {
    if (args.length < 1 || args.length > 2) {
      System.err.println("USAGE: total-size sequence-size");
      return;
    }
    int seqNum = 0;
    if ("-random".equals(args[0])) {
      while (true) {
        System.out.println(">d" + seqNum++);
        System.out.println(random(RANDOM.nextInt(0x000FFFFF)));
      }
    } else {
      long size = Long.parseLong(args[0]);
      long chunk = 1000;
      if (args.length > 1) {
        chunk = Long.parseLong(args[1]);
      }

      while (size > 0) {
        System.out.println(">d" + seqNum++);
        System.out.println(random((int) Math.min(chunk, size)));
        size -= chunk;
      }
    }
  }

}

