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
package com.rtg.vcf.eval;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.BasicLinkedListNode;

/**
 * Prefer paths that maximise total number of included variants (baseline + called).
 * Includes some other tie-breaking heuristics that prefer more aesthetically pleasing matches.
 */
@TestClass("com.rtg.vcf.eval.PathTest")
final class MaxSumBoth implements PathPreference {

  @Override
  public Path better(Path first, Path second) {
    // Prefer paths that maximise total number of included variants (baseline + called)
    BasicLinkedListNode<OrientedVariant> firstIncluded = first.mCalledPath.getIncluded();
    BasicLinkedListNode<OrientedVariant> secondIncluded = second.mCalledPath.getIncluded();
    int firstSize = firstIncluded == null ? 0 : firstIncluded.size();
    int secondSize = secondIncluded == null ? 0 : secondIncluded.size();
    firstIncluded = first.mBaselinePath.getIncluded();
    secondIncluded = second.mBaselinePath.getIncluded();
    firstSize += firstIncluded == null ? 0 : firstIncluded.size();
    secondSize += secondIncluded == null ? 0 : secondIncluded.size();
    if (firstSize == secondSize) {
      // Tie break equivalently scoring paths for greater aesthetics
      if (firstIncluded != null && secondIncluded != null) {

        // Prefer solutions that minimize discrepencies between baseline and call counts since last sync point
        final int fDelta = Math.abs(first.mBSinceSync - first.mCSinceSync);
        final int sDelta = Math.abs(second.mBSinceSync - second.mCSinceSync);
        if (fDelta != sDelta) {
          return fDelta < sDelta ? first : second;
        }

        // Prefer solutions that sync more regularly (more likely to be "simpler")
        final int syncDelta = (first.mSyncPointList == null ? 0 : first.mSyncPointList.getValue()) - (second.mSyncPointList == null ? 0 : second.mSyncPointList.getValue());
        if (syncDelta != 0) {
          return syncDelta > 0 ? first : second;
        }

      /*
      Diagnostic.developerLog("Sum: Remaining break at: " + first.mBaselinePath.getPosition() + " sum = " + firstSize
          + " first = (" + first.mBSinceSync + "," + first.mCSinceSync + ")"
          + " second = (" + second.mBSinceSync + "," + second.mCSinceSync + ")"
          + " " + first.inSync() + "," + second.inSync() + " syncdelta=" + syncDelta
      );
      */

        // At this point break ties arbitrarily based on allele ordering
        return (firstIncluded.getValue().alleleId() < secondIncluded.getValue().alleleId()) ? first : second;
      }
    }
    return firstSize > secondSize ? first : second;
  }
}
