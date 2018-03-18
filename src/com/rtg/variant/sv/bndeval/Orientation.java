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

import com.rtg.util.machine.MachineOrientation;

import htsjdk.samtools.SAMRecord;


/**
 * Describes the orientation on the genome of the constraints of a breakpoint.
 * U(up) means the breakpoint is forward of a specified position on the genome
 * D(down) means the breakpoint is back from a specified position on the genome.
 * The terms forward and reverse are avoided because they are used for read end orientations.
 * The first constraint is taken from the first of the reads (even if it is not the actual
 * read seen at this point).
 * The first letter refers to the read arm being referred to and the second letter its mate. This means
 * that a read arm described as <code>UD</code> will have a mate described as <code>DU</code>.
 */
public enum Orientation {
  /** Up Up*/
  UU(+1, +1) {
    @Override
    public Orientation flip() {
      return UU;
    }
    @Override
    public int x(int x) {
      return x;
    }
    @Override
    public int y(int y) {
      return y;
    }
    @Override
    public double x(double x) {
      return x;
    }
    @Override
    public double y(double y) {
      return y;
    }
  },
  /** Up Down */
  UD(+1, -1) {
    @Override
    public Orientation flip() {
      return DU;
    }
    @Override
    public int x(int x) {
      return x;
    }
    @Override
    public int y(int y) {
      return -y;
    }
    @Override
    public double x(double x) {
      return x;
    }
    @Override
    public double y(double y) {
      return -y;
    }
  },
  /** Down Up */
  DU(-1, +1) {
    @Override
    public Orientation flip() {
      return UD;
    }
    @Override
    public int x(int x) {
      return -x;
    }
    @Override
    public int y(int y) {
      return y;
    }
    @Override
    public double x(double x) {
      return -x;
    }
    @Override
    public double y(double y) {
      return y;
    }
  },
  /** Down Down */
  DD(-1, -1)  {
    @Override
    public Orientation flip() {
      return DD;
    }
    @Override
    public int x(int x) {
      return -x;
    }
    @Override
    public int y(int y) {
      return -y;
    }
    @Override
    public double x(double x) {
      return -x;
    }
    @Override
    public double y(double y) {
      return -y;
    }
  };

  private final int mX;
  private final int mY;

  /**
   * @param x direction of the first read
   * @param y direction of the second read
   */
  Orientation(int x, int y) {
    mX = x;
    mY = y;
  }

  /**
   * @return the x direction, where +1 means up and -1 means down
   */
  public int xDir() {
    return mX;
  }

  /**
   * @return the y direction, where +1 means up and -1 means down
   */
  public int yDir() {
    return mY;
  }

  /**
   * Compute r coordinate value from x and y
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the r coordinate
   */
  public int r(int x, int y) {
    return x(x) + y(y);
  }

  /**
   * Compute x coordinate value from r and y
   * @param r the r coordinate
   * @param y the y coordinate
   * @return the r coordinate
   */
  public int x(int r, int y) {
    return x(r - y(y));
  }

  /**
   * Compute y coordinate value from r and x
   * @param r the r coordinate
   * @param x the x coordinate
   * @return the r coordinate
   */
  public int y(int r, int x) {
    return y(r - x(x));
  }

  /**
   * @return the orientation with x and y axes interchanged.
   */
  public abstract Orientation flip();

  /**
   * Transform along the x-axis.
   * @param x value to be transformed.
   * @return the transformed value.
   */
  public abstract int x(int x);

  /**
   * Transform along the y-axis.
   * @param y value to be transformed.
   * @return the transformed value.
   */
  public abstract int y(int y);

  /**
   * Transform along the x-axis.
   * @param x value to be transformed.
   * @return the transformed value.
   */
  public abstract double x(double x);

  /**
   * Transform along the y-axis.
   * @param y value to be transformed.
   * @return the transformed value.
   */
  public abstract double y(double y);

  /**
   * Get orientation given differences between x and y co-ordinates.
   * @param xDir usually computed as <code>z-x</code>
   * @param yDir usually computed as <code>w-y</code>
   * @return the orientation.
   */
  public static Orientation orientation(final int xDir, final int yDir) {
    //In real data shouldnt get 0 for either but treat as positive.
    if (xDir < 0) {
      if (yDir < 0) {
        return DD;
      } else {
        return DU;
      }
    } else {
      if (yDir < 0) {
        return UD;
      } else {
        return UU;
      }
    }
  }

  /**
   * Determine the constraint direction represented by this SAM record.
   * @param rec the sam record to determine the orientation of
   * @param mo the expected orientation of read arms for the machine type
   * @return the orientation of the sam record
   */
  public static Orientation orientation(SAMRecord rec, MachineOrientation mo) {
    switch (mo) {
      case FR:
        if (rec.getReadNegativeStrandFlag()) {
          if (rec.getMateNegativeStrandFlag()) {
            return Orientation.DD;
          } else {
            return Orientation.DU;
          }
        } else {
          if (rec.getMateNegativeStrandFlag()) {
            return Orientation.UD;
          } else {
            return Orientation.UU;
          }
        }
      case RF:
        if (rec.getReadNegativeStrandFlag()) {
          if (rec.getMateNegativeStrandFlag()) {
            return Orientation.UU;
          } else {
            return Orientation.UD;
          }
        } else {
          if (rec.getMateNegativeStrandFlag()) {
            return Orientation.DU;
          } else {
            return Orientation.DD;
          }
        }
      case TANDEM:
        if (rec.getFirstOfPairFlag()) {
          if (rec.getReadNegativeStrandFlag()) {
            if (rec.getMateNegativeStrandFlag()) {
              return Orientation.DU;
            } else {
              return Orientation.DD;
            }
          } else {
            if (rec.getMateNegativeStrandFlag()) {
              return Orientation.UU;
            } else {
              return Orientation.UD; // Normal mated orientation on the forward strand
            }
          }
        } else {
          if (rec.getReadNegativeStrandFlag()) {
            if (rec.getMateNegativeStrandFlag()) {
              return Orientation.UD; // Normal mated orientation on the reverse strand
            } else {
              return Orientation.UU;
            }
          } else {
            if (rec.getMateNegativeStrandFlag()) {
              return Orientation.DD;
            } else {
              return Orientation.DU;
            }
          }
        }
      case ANY:
      default:
        break;
    }
    throw new UnsupportedOperationException("Not supported.");
  }

}
