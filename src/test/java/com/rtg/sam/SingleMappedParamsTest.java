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
package com.rtg.sam;

import java.util.ArrayList;

import com.rtg.sam.SingleMappedParams.SingleMappedParamsBuilder;
import com.rtg.util.test.params.TestParams;

import junit.framework.TestCase;

/**
 */
public class SingleMappedParamsTest extends TestCase {

  /**
   * Mock SingleMappedParamsBuilder class
   */
  public static final class MockSingleMappedParamsBuilder extends SingleMappedParamsBuilder<MockSingleMappedParamsBuilder> {
    @Override
    protected MockSingleMappedParamsBuilder self() {
      return this;
    }
  }

  static final class MockSingleMappedParams extends SingleMappedParams {

    MockSingleMappedParams(MockSingleMappedParamsBuilder builder) {
      super(builder);
    }
  }

  public void testSingleMappedParamsBuilder() {
    final MockSingleMappedParams dummy = new MockSingleMappedParams(new MockSingleMappedParamsBuilder().mapped(new ArrayList<>()).ioThreads(2).execThreads(3));
    assertEquals(2, dummy.ioThreads());
    assertEquals(3, dummy.execThreads());
    assertEquals(0, dummy.mapped().size());
  }

  public void testSingleMappedParamsDefaults() {
    final MockSingleMappedParams dummy = new MockSingleMappedParams(new MockSingleMappedParamsBuilder());
    assertEquals(1, dummy.ioThreads());
    assertEquals(1, dummy.execThreads());
    assertNull(dummy.mapped());
  }

  public void testOmnes() {
    new TestParams(SingleMappedParams.class, SingleMappedParamsBuilder.class).check();
  }
}
