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
package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.OutputParams;
import com.rtg.util.test.params.TestParams;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class VcfEvalParamsTest extends TestCase {

  public void testDefaults() {
    final VcfEvalParams params = VcfEvalParams.builder().create();
    assertEquals(VcfUtils.FORMAT_GENOTYPE_QUALITY, params.scoreField());
    assertEquals(RocSortOrder.DESCENDING, params.sortOrder());
    assertNull(params.outputParams());
    assertNull(params.baselineFile());
    assertNull(params.callsFile());
    assertNull(params.templateFile());
    assertNull(params.baselineSample());
    assertNull(params.callsSample());
    assertFalse(params.useAllRecords());
  }

  public void testBuilder() throws IOException {
    VcfEvalParams.VcfEvalParamsBuilder builder = VcfEvalParams.builder();
    builder = builder.name("blah").outputParams(new OutputParams(new File("out"), false, false)).baseLineFile(new File("mutations")).callsFile(new File("calls")).templateFile(new File("template")).maxLength(199);
    builder = builder.scoreField(VcfUtils.QUAL).sortOrder(RocSortOrder.ASCENDING).baselineSample("name").callsSample("name2");
    builder.rtgStats(true);
    final VcfEvalParams params = builder.create();
    assertEquals("blah", params.name());
    assertEquals(VcfUtils.QUAL, params.scoreField());
    assertEquals(RocSortOrder.ASCENDING, params.sortOrder());
    assertEquals("out", params.outputParams().directory().getName());
    assertEquals("mutations", params.baselineFile().getName());
    assertEquals("calls", params.callsFile().getName());
    assertEquals("template", params.templateFile().getName());
    assertEquals("name", params.baselineSample());
    assertEquals("name2", params.callsSample());
    assertEquals(199, params.maxLength());
    assertEquals(params.outputParams().directory(), params.directory());
    assertEquals(new File(new File("out"), "bbbb"), params.file("bbbb"));
    assertTrue(params.rtgStats());
  }

  public void testOmnes() {
    new TestParams(VcfEvalParams.class, VcfEvalParams.VcfEvalParamsBuilder.class).check();
  }
}
