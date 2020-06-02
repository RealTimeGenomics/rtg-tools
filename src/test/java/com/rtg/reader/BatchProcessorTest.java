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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import org.junit.Test;

import com.rtg.util.diagnostic.Diagnostic;

public class BatchProcessorTest {
  @Test
  public void testBatches() {
    Diagnostic.setLogStream();
    final List<Batch<Integer>> accumulator = new ArrayList<>();
    final Function<Batch<Integer>, FutureTask<?>> listRunnableFunction = batch -> new FutureTask<>(() -> accumulator.add(batch), null);
    final BatchProcessor<Integer> processor = new BatchProcessor<>(listRunnableFunction, 1, 3);
    processor.process(Arrays.asList(1, 2, 3, 4, 5, 6, 7).iterator());

    final List<List<Integer>> expected = new ArrayList<>();
    expected.add(Arrays.asList(1, 2, 3));
    expected.add(Arrays.asList(4, 5, 6));
    expected.add(Collections.singletonList(7));
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(i, accumulator.get(i).getBatchNumber());
      assertEquals(expected.get(i), accumulator.get(i).getItems());

    }
  }
}