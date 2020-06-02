/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.variant.sv.bndeval;

import com.rtg.util.integrity.Exam;

/**
 * Describes a six-sided region in two dimensional space with six explicit values.
 *
 * This class is used to represent individual break-point constraints determined by
 * paired-end reads and unions and intersections of these.
 */
public final class BreakpointGeometry extends AbstractBreakpointGeometry {

  /**
   * Create a break point given the real co-ordinates.
   * Useful for re-creating data from text files etc.
   * @param xName name for x co-ordinate.
   * @param x co-ordinate.
   * @param z co-ordinate.
   * @param yName name for y co-ordinate.
   * @param y co-ordinate.
   * @param w co-ordinate.
   * @param r co-ordinate.
   * @param s co-ordinate.
   * @return the break point.
   */
  public static BreakpointGeometry makeGeometry(final String xName, final int x, final int z, final String yName, final int y, final int w, final int r, final int s) {
    final Orientation orientation = Orientation.orientation(z - x, w - y);
    final BreakpointGeometry bg = new BreakpointGeometry(orientation, xName, yName, x, z, y, w, r, s);
    bg.globalIntegrity();
    return bg;
  }

  private final Orientation mOrientation;
  private final String mXName;
  private final String mYName;
  private final int mXLo;
  private final int mXHi;
  private final int mYLo;
  private final int mYHi;
  private final int mRLo;
  private final int mRHi;

  private final AbstractBreakpointGeometry mFlip;

  /**
   * @param orientation along the two axes.
   * @param xName name of x sequence.
   * @param yName name of y sequence.
   * @param xLo first x co-ordinate value
   * @param xHi second x co-ordinate value
   * @param yLo first y co-ordinate value
   * @param yHi second y co-ordinate value
   * @param rLo first diagonal value
   * @param rHi second diagonal value
   */
  public BreakpointGeometry(Orientation orientation, String xName, String yName, int xLo, int xHi, int yLo, int yHi, int rLo, int rHi) {
    super();
    mOrientation = orientation;
    mXName = xName;
    mYName = yName;
    mXLo = xLo;
    mXHi = xHi;
    mYLo = yLo;
    mYHi = yHi;
    mRLo = rLo;
    mRHi = rHi;
    mFlip = new FlippedProxyBreakpointGeometry(this);
    //assert globalIntegrity();
  }

  /**
   * Get orientation.
   * @return Returns the orientation.
   */
  @Override
  public Orientation getOrientation() {
    return mOrientation;
  }

  /**
   * Get xName.
   * @return Returns the xName.
   */
  @Override
  public String getXName() {
    return mXName;
  }

  /**
   * Get yName.
   * @return Returns the yName.
   */
  @Override
  public String getYName() {
    return mYName;
  }

  /**
   * Get x.
   * @return Returns the x.
   */
  @Override
  public int getXLo() {
    return mXLo;
  }

  /**
   * Get z.
   * @return Returns the z.
   */
  @Override
  public int getXHi() {
    return mXHi;
  }

  /**
   * Get y.
   * @return Returns the y.
   */
  @Override
  public int getYLo() {
    return mYLo;
  }

  /**
   * Get w.
   * @return Returns the w.
   */
  @Override
  public int getYHi() {
    return mYHi;
  }

  /**
   * Get r.
   * @return Returns the r.
   */
  @Override
  public int getRLo() {
    return mRLo;
  }

  /**
   * Get s.
   * @return Returns the s.
   */
  @Override
  public int getRHi() {
    return mRHi;
  }

  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertNotNull(mOrientation);
    Exam.assertNotNull(mXName);
    Exam.assertTrue(0 <= mXLo);
    Exam.assertNotNull(mYName);
    Exam.assertTrue(0 <= mYLo);
    return true;
  }

  @Override
  public AbstractBreakpointGeometry flip() {
    return mFlip;
  }
}
