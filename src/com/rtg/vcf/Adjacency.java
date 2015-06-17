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

package com.rtg.vcf;

import java.io.IOException;

/**
 * Represents a single VCF 'adjacency', which ties together 2 'breakends'.
 * Each break end has a chromosome name, a position and a forward/reverse direction.
 *
 */
public class Adjacency implements Comparable<Adjacency> {
  private String mThisChromosome;
  private int mThisEnd;
  private boolean mThisForward;
  private String mThisBases;

  private String mMateChromosome;
  private int mMateStart;
  private boolean mMateForward;

  /**
   * @param chr name of the current chromosome.
   * @param pos one-based position on the current chromosome where the breakpoint starts.
   * @param isForward true if this breakpoint is going forward along the current chromosome.
   * @param bases the bases across the breakpoint, which start at <code>chr:pos</code>.
   * @param mateChr name of the mate chromosome.
   * @param matePos one-based position where we start on the mate chromosome.
   * @param mateIsForward true if we will go forward along the mate chromosome.
   */
  public Adjacency(String chr, int pos, boolean isForward, String bases, String mateChr, int matePos, boolean mateIsForward) {
    mThisChromosome = chr;
    mThisEnd = pos;
    mThisForward = isForward;
    mThisBases = bases;

    mMateChromosome = mateChr;
    mMateStart = matePos;
    mMateForward = mateIsForward;
  }

  /**
   * create a dummy version so we can compare with real objects
   * @param chr name of the current chromosome.
   * @param pos one-based position on the current chromosome where the breakpoint starts.
   * @param isForward true if this breakpoint is going forward along the current chromosome.
   */
  public Adjacency(String chr, int pos, boolean isForward) {
    this(chr, pos, isForward, "", "", 0, true);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mMateChromosome == null) ? 0 : mMateChromosome.hashCode());
    result = prime * result + (mMateForward ? 1231 : 1237);
    result = prime * result + mMateStart;
    result = prime * result + ((mThisBases == null) ? 0 : mThisBases.hashCode());
    result = prime * result + ((mThisChromosome == null) ? 0 : mThisChromosome.hashCode());
    result = prime * result + mThisEnd;
    result = prime * result + (mThisForward ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Adjacency)) {
      return false;
    }
    final Adjacency other = (Adjacency) obj;
    return compareTo(other) == 0;
  }

  /**
   * @return name of the current chromosome.
   */
  public String thisChromosome() {
    return mThisChromosome;
  }

  /**
   * @return one-based position on the current chromosome where the breakpoint starts.
   */
  public int thisEndPos() {
    return mThisEnd;
  }

  /**
   * @return true if this breakpoint is going forward along the current chromosome.
   */
  public boolean thisIsForward() {
    return mThisForward;
  }

  /**
   * @return the bases across the breakpoint, which start at <code>chr:pos</code>.
   */
  public String thisBases() {
    return mThisBases;
  }

  /**
   * @return name of the mate chromosome.
   */
  public String mateChromosome() {
    return mMateChromosome;
  }

  /**
   * @return one-based position where we start on the mate chromosome.
   */
  public int mateStartPos() {
    return mMateStart;
  }

  /**
   * @return true if we will go forward along the mate chromosome.
   */
  public boolean mateIsForward() {
    return mMateForward;
  }

  /**
   * Try to parse one part of a VCF ALT field as an adjacency string.
   *
   * @param chr name of the starting chromosome
   * @param pos the position where the starting chromosome ends
   * @param altCall VCF adjacency string.  For example <code>"ACGT[chr2:123["</code>.
   * @return null if <code>altCall</code> is not an adjacency specification.
   * @throws IOException if <code>altCall</code> is an illegal adjacency specification.
   */
  public static Adjacency parseAdjacency(String chr, int pos, String altCall) throws IOException {
    int br1 = altCall.indexOf('[');
    final boolean mateIsForward;
    final int br2;
    if (br1 >= 0) {
      mateIsForward = true;
      br2 = altCall.indexOf('[', br1 + 1);
    } else {
      mateIsForward = false;
      br1 = altCall.indexOf(']');
      br2 = altCall.indexOf(']', br1 + 1);
    }
    if (br1 < 0) {
      return null;
    }
    final String errmsg = "Invalid .vcf adjacency call: ";
    if (br2 < 0) {
      throw new IOException(errmsg + altCall);
    }
    assert 0 <= br1 && br1 < br2 && br2 < altCall.length();
    final String matepos;
    final String bases;
    final boolean isForward;
    if (br1 == 0) {
      isForward = false;
      matepos = altCall.substring(1, br2);
      bases = altCall.substring(br2 + 1);
    } else {
      if (br2 != altCall.length() - 1) {
        throw new IOException(errmsg + altCall);
      }
      isForward = true;
      bases = altCall.substring(0, br1);
      matepos = altCall.substring(br1 + 1, br2);
    }
    // TODO: check that first base in bases is equal to the reference base.
    // now parse matepos
    final String[] mate = matepos.split(":");
    if (mate.length != 2) {
      throw new IOException(errmsg + altCall);
    }
    return new Adjacency(chr, pos, isForward, bases, mate[0], Integer.parseInt(mate[1]), mateIsForward);
  }

  @Override
  public int compareTo(Adjacency that) {
    if (that == null) {
      throw new NullPointerException("o is null");
    }
    int res = this.mThisChromosome.compareTo(that.mThisChromosome);
    if (res == 0) {
      if (this.mThisEnd < that.mThisEnd) {
        res = -1;
      } else if (this.mThisEnd > that.mThisEnd) {
        res = 1;
      }
    }
    if (res == 0 && this.mThisForward != that.mThisForward) {
      res = this.mThisForward ? -1 : 1; // forward ends come before backward ends.
    }
    return res;
  }

}
