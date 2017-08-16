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

/**
 * Represents a VCF break-end ALT allele.
 */
public class BreakpointAlt {

  private static final char UP_BRACKET = ']';
  private static final char DOWN_BRACKET = '[';

  private final boolean mLocalUp;
  private final boolean mRemoteUp;
  private final String mRemoteChr;
  private final int mRemotePos;
  private final String mRefSubs;

  /**
   * Construct a break end from an ALT string. The caller should have already determined
   * that the ALT represents a break end rather than a regular allele or symbolic allele.
   * @param alt the VCF ALT string
   */
  public BreakpointAlt(String alt) {
    final int bracketPos;
    final char bracket;
    int tmpPos;
    if ((tmpPos = alt.indexOf(UP_BRACKET)) > -1) {
      bracket = UP_BRACKET;
      bracketPos = tmpPos;
      mRemoteUp = true;
    } else if ((tmpPos = alt.indexOf(DOWN_BRACKET)) > -1) {
      bracket = DOWN_BRACKET;
      bracketPos = tmpPos;
      mRemoteUp = false;
    } else {
      throw new IllegalArgumentException("Invalid break end specification: " + alt);
    }
    mLocalUp = bracketPos != 0;
    final int nextBracketPos = alt.indexOf(bracket, bracketPos + 1);
    if (nextBracketPos == -1) {
      throw new IllegalArgumentException("Invalid break end specification: " + alt);
    }
    final String remoteString = alt.substring(bracketPos + 1, nextBracketPos);
    final int colonPos = remoteString.indexOf(':');
    if (colonPos == -1) {
      throw new IllegalArgumentException("Invalid break end specification: " + alt);
    }
    mRemoteChr = remoteString.substring(0, colonPos);
    if (mRemoteChr.indexOf('<') != -1) {
      throw new IllegalArgumentException("Contig break ends are not supported: " + alt); // e.g. "C[<ctg1>:7["
    }
    mRemotePos = Integer.parseInt(remoteString.substring(colonPos + 1)) - 1; // to 0-based
    mRefSubs = mLocalUp ? alt.substring(0, bracketPos) : alt.substring(nextBracketPos + 1);
  }

  /**
   * Construct a break end directly.
   * @param refSubs the bases of the reference substitution
   * @param localUp true if the local end is up
   * @param remoteChr the name of the chromosome this break end joins to
   * @param remotePos the position within the remote chromosome the breakpoint is at
   * @param remoteUp true if the remote end is up
   */
  public BreakpointAlt(String refSubs, boolean localUp, String remoteChr, int remotePos, boolean remoteUp) {
    mRefSubs = refSubs;
    mLocalUp = localUp;
    mRemoteChr = remoteChr;
    mRemotePos = remotePos;
    mRemoteUp = remoteUp;
  }

  /**
   * @return true if the local part of the breakpoint is orientation "up", that is,
   * the remote side of the adjacency is attached to the right of the current position
   */
  public boolean isLocalUp() {
    return mLocalUp;
  }

  /**
   * @return the name of the chromosome this breakpoint joins to
   */
  public String getRemoteChr() {
    return mRemoteChr;
  }

  /**
   * @return the position within the remote chromosome this breakpoint is at
   */
  public int getRemotePos() {
    return mRemotePos;
  }

  /**
   * @return true if the remote part of the breakpoint is orientation "up", that is,
   * the local side of the adjacency is attached to the right of the remote position.
   */
  public boolean isRemoteUp() {
    return mRemoteUp;
  }

  /**
   * @return the bases that substitute for the reference bases
   */
  public String getRefSubstitution() {
    return mRefSubs;
  }

  @Override
  public String toString() {
    final char bracket = mRemoteUp ? UP_BRACKET : DOWN_BRACKET;
    return mLocalUp
      ? mRefSubs + bracket + mRemoteChr + ":" + (mRemotePos + 1) + bracket
      : bracket + mRemoteChr + ":" + (mRemotePos + 1) + bracket + mRefSubs ;
  }
}
