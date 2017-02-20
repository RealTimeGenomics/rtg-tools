/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.reader;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class FastqTrimProcessorTest {

  @Test
  public void test3Bases() {
    final StringWriter writer = new StringWriter();
    try (final AsyncFastqSequenceWriter asyncWriter = getAsyncWriter(writer)) {
      new FastqTrimProcessor(new Batch<>(0, getSequences()), false, new LastBasesReadTrimmer(3), new BatchReorderingWriter<>(asyncWriter)).run();
    }
    final FastqSequence[] expectedSequences = {
      FastqSequenceTest.getFastq("foo", "ACCCC"),
      FastqSequenceTest.getFastq("bar", "AC"),
      FastqSequenceTest.getFastq("baz", "A")

    };
    final String expectedFastq = getFastq(expectedSequences);
    Assert.assertEquals(expectedFastq, writer.toString());
  }

  @Test
  public void test3BasesMinLength() {
    final StringWriter writer = new StringWriter();
    final ReadTrimmer trimmer = new MultiReadTrimmer(new LastBasesReadTrimmer(3), new MinLengthReadTrimmer(5));
    try (final AsyncFastqSequenceWriter asyncWriter = getAsyncWriter(writer)) {
      new FastqTrimProcessor(new Batch<>(0, getSequences()), false, trimmer, new BatchReorderingWriter<>(asyncWriter)).run();
    }
    final FastqSequence[] expectedSequences = {
      FastqSequenceTest.getFastq("foo", "ACCCC"),
      FastqSequenceTest.getFastq("bar", ""),
      FastqSequenceTest.getFastq("baz", "")
    };
    final String expectedFastq = getFastq(expectedSequences);
    Assert.assertEquals(expectedFastq, writer.toString());
  }

  private List<FastqSequence> getSequences() {
    return Arrays.asList(
      FastqSequenceTest.getFastq("foo", "ACCCCACA"),
      FastqSequenceTest.getFastq("bar", "ACCCC"),
      FastqSequenceTest.getFastq("baz", "ACCC")

    );
  }

  private AsyncFastqSequenceWriter getAsyncWriter(StringWriter writer) {
    return new AsyncFastqSequenceWriter(new FastqWriter(writer));
  }

  @Test
  public void test3BasesMinLengthFiltered() {
    final StringWriter writer = new StringWriter();
    final ReadTrimmer trimmer = new MultiReadTrimmer(new LastBasesReadTrimmer(3), new MinLengthReadTrimmer(5));
    try (final AsyncFastqSequenceWriter asyncWriter = getAsyncWriter(writer)) {
      new FastqTrimProcessor(new Batch<>(0, getSequences()), true, trimmer, new BatchReorderingWriter<>(asyncWriter)).run();
    }
    final FastqSequence[] expectedSequences = {
      FastqSequenceTest.getFastq("foo", "ACCCC"),
    };
    final String expectedFastq = getFastq(expectedSequences);
    Assert.assertEquals(expectedFastq, writer.toString());
  }

  private String getFastq(FastqSequence[] expectedSequences) {
    return Arrays.stream(expectedSequences)
        .map(FastqSequence::toFastq)
        .reduce((a, b) -> a + b)
        .orElse("");
  }

}