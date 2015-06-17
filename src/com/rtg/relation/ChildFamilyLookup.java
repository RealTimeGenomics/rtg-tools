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
package com.rtg.relation;

/**
 * Creates a lookup from a sample id to the family in which that sample is a child
 */
public class ChildFamilyLookup {
  private final Family[] mChildToFamily;

  /**
   * @param numSamples total number of samples
   * @param families the set of families, with sample ids set appropriately
   */
  public ChildFamilyLookup(int numSamples, Family... families) {
    final Family[] childToFamily = new Family[numSamples];
    if (families != null) {
      for (Family f : families) {
        final int[] sampleIds = f.getSampleIds();
        for (int i = Family.FIRST_CHILD_INDEX; i < sampleIds.length; i++) {
          childToFamily[sampleIds[i]] = f;
        }
      }
    }
    mChildToFamily = childToFamily;
  }

  /**
   * Get the family (if any) in which given sample is a child.
   * @param sampleId id of sample
   * @return the family in which the sample is a child, or null if given sample is missing one or both parents from the genome relationships input
   */
  public Family getFamily(int sampleId) {
    return mChildToFamily[sampleId];
  }
}
