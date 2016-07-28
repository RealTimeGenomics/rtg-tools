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

package com.rtg.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.hamcrest.core.StringEndsWith;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 *
 */
public class ReferenceTextBuilderTest {

  private static final String EXPECTED = ReferenceGenome.REFERENCE_DEFAULT_DIPLOID
    + "either\tseq\tfoo\tdiploid\tlinear\n"
    + "male\tseq\tmoo\thaploid\tcircular\n";

  /** Sometimes an exception is expected */
  @Rule
  public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void test() throws IOException {
    final ReferenceTextBuilder builder = ReferenceTextBuilder.createDiploid();
    builder.addSequence("foo", Sex.EITHER, Ploidy.DIPLOID, true);
    builder.addSequence("moo", Sex.MALE, Ploidy.HAPLOID, false);
    assertEquals(EXPECTED, builder.toString());
    try (TestDirectory dir = new TestDirectory()) {
      final File refText = new File(dir, "someName");
      builder.writeToFile(refText);
      assertEquals(EXPECTED, FileHelper.fileToString(refText));
      final File sdf = ReaderTestUtils.getDNADir(">seq\nacagtacgt\n", new File(dir, "sdf"));
      builder.writeToSdfDir(sdf);
      assertEquals(EXPECTED, FileHelper.fileToString(new File(sdf, ReferenceGenome.REFERENCE_FILE)));
      final File notSdf = new File(dir, "notSdf");
      assertTrue(notSdf.mkdir());
      mExpectedException.expect(IOException.class);
      mExpectedException.expectMessage(StringEndsWith.endsWith("is not an SDF"));
      builder.writeToSdfDir(notSdf);
    }
  }
}