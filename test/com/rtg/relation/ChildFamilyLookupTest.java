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

import junit.framework.TestCase;

/**
 */
public class ChildFamilyLookupTest extends TestCase {

  public void test() {
    final Family f1 = new Family("father1", "mother", "child1");
    final Family f2 = new Family("father2", "mother", "child2");
    f2.setSampleId(1, 1);
    f2.setSampleId(0, 3);
    f2.setSampleId(2, 4);
    final Family[] fams = {f1, f2};
    assertEquals(2, fams.length);
    final ChildFamilyLookup lookup = new ChildFamilyLookup(5, fams);
    for (final Family f : fams) {
      final int[] sampleIds = f.getSampleIds();
      assertNull(lookup.getFamily(sampleIds[Family.FATHER_INDEX]));
      assertNull(lookup.getFamily(sampleIds[Family.MOTHER_INDEX]));
      for (int i = Family.FIRST_CHILD_INDEX; i < sampleIds.length; ++i) {
        assertEquals(f, lookup.getFamily(sampleIds[i]));
      }
    }
  }
}
