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

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.MockReaderParams;
import com.rtg.launcher.OutputParams;
import com.rtg.mode.SequenceMode;
import com.rtg.sam.MappedParams.MappedParamsBuilder;
import com.rtg.sam.MappedParamsTest.MockMappedParams.MockMappedParamsBuilder;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.params.TestParams;

import junit.framework.TestCase;

/**
 */
public class MappedParamsTest extends TestCase {

  static final class MockMappedParams extends MappedParams {

    static MockMappedParamsBuilder builder() {
      return new MockMappedParamsBuilder();
    }

    static final class MockMappedParamsBuilder extends MappedParamsBuilder<MockMappedParamsBuilder> {
      @Override
      protected MockMappedParamsBuilder self() {
        return this;
      }

      public MockMappedParams create() {
        return new MockMappedParams(this);
      }
    }

    MockMappedParams(MockMappedParamsBuilder builder) {
      super(builder);
    }
  }

  public void testOmnes() {
    new TestParams(MappedParams.class, MappedParamsBuilder.class).check();
  }

  public void testDefaultMappedParams() throws IOException {
    Diagnostic.setLogStream();
    final File tempDir = FileUtils.createTempDir("mappedparams", "test");
    try {
      MockMappedParams def = MockMappedParams.builder().outputParams(new OutputParams(tempDir, true)).create();
      assertNotNull(def.filterParams());
      assertNull(def.genome());
      def.close();
      def = MockMappedParams.builder().outputParams(new OutputParams(tempDir, true)).genome(new MockReaderParams(1, 1, SequenceMode.BIDIRECTIONAL.codeType())).create();
      assertNotNull(def.genome());
      def.close();
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  public void testMappedParamsBuilder() {
    final MockMappedParamsBuilder builder = MockMappedParams.builder();
    assertEquals(builder, builder.genome(null));
    assertEquals(builder, builder.filterParams(null));
  }
}
