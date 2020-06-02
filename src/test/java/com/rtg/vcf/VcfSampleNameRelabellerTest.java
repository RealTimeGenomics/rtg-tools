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

package com.rtg.vcf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class VcfSampleNameRelabellerTest extends TestCase {

  public void check(final VcfSampleNameRelabeller r) {
    final VcfHeader header = new VcfHeader();
    header.addSampleName("foo1");
    header.addSampleName("fooX");
    header.addSampleName("foo2");
    header.addMetaInformationLine("##PEDIGREE=<Child=fooX,Mother=foo1,Father=foo2>");
    // note description should not change
    header.addMetaInformationLine("##SAMPLE=<ID=foo1,Description=\"foo1\">");
    r.updateHeader(header);
    //System.out.println(header.toString());
    assertEquals(0, header.getSampleIndex("bar1"));
    assertEquals(1, header.getSampleIndex("fooX"));
    assertEquals(2, header.getSampleIndex("bar2"));
    assertEquals(-1, header.getSampleIndex("foo2"));
    TestUtils.containsAll(header.toString(),
      "##SAMPLE=<ID=bar1,Description=\"foo1\">",
      "##PEDIGREE=<Child=fooX,Mother=bar1,Father=bar2>"
    );
  }

  public void testDirectConstructor() {
    final HashMap<String, String> sampleNameMap = new HashMap<>();
    sampleNameMap.put("foo1", "bar1");
    sampleNameMap.put("foo2", "bar2");
    sampleNameMap.put("foo3", "bar3"); // not actually present in header
    check(new VcfSampleNameRelabeller(sampleNameMap));
  }

  public void testFileConstructor() throws IOException {
    final File f = FileHelper.createTempFile();
    try {
      check(VcfSampleNameRelabeller.create(FileUtils.stringToFile("foo1 bar1\n foo2 \t\tbar2 \nfoo3 bar3\n\n", f)));
    } finally {
      assertTrue(f.delete());
    }
  }

  public void testFileConstructorBad() throws IOException {
    final File f = FileHelper.createTempFile();
    try {
      VcfSampleNameRelabeller.create(FileUtils.stringToFile("old-name new-name what-name", f));
    } catch (final NoTalkbackSlimException e) {
      assertTrue(e.getMessage().startsWith("Expected: old-name new-name on line 1 of"));
    } finally {
      assertTrue(f.delete());
    }
  }
}
