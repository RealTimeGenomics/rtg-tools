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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import com.rtg.mode.DnaUtils;
import com.rtg.util.io.BaseFile;

import junit.framework.TestCase;

/**
 *
 */
public class FastqUtilsTest extends TestCase {

  public void testBaseFile() {
    checkBaseFile("input", ".fastq", true, FastqUtils.baseFile(new File("input"), true));
    checkBaseFile("input", ".fastq", true, FastqUtils.baseFile(new File("input.fastq"), true));
    checkBaseFile("input", ".fastq", true, FastqUtils.baseFile(new File("input.fastq.gz"), true));
    checkBaseFile("input", ".fq", true, FastqUtils.baseFile(new File("input.fq"), true));
    checkBaseFile("input", ".fq", true, FastqUtils.baseFile(new File("input.fq.gz"), true));
    checkBaseFile("input.fast", ".fastq", true, FastqUtils.baseFile(new File("input.fast.gz"), true));

    final BaseFile bf = FastqUtils.baseFile(new File("input.fastq.gz"), true);
    assertEquals("input.fastq.gz", bf.file().getName());
    assertEquals("input_moo.fastq.gz", bf.suffixedFile("_moo").getName());
    final BaseFile bf2 = FastqUtils.baseFile(new File("input.fastq.gz"), false);
    assertEquals("input.fastq", bf2.file().getName());
    assertEquals("input_moo.fastq", bf2.suffixedFile("_moo").getName());
  }

  public void testWriteSequence() throws IOException {
    try (final StringWriter sw = new StringWriter()) {
      final String seqString = "GATCAGGTAGTT";
      final byte[] seqData = DnaUtils.encodeArray(seqString.getBytes());
      final String qualString = "%^@%#^@#*HDA";
      final byte[] qualData = FastaUtils.asciiToRawQuality(qualString);
      FastqUtils.writeFastqSequence(sw, "thefirstsequence", seqData, qualData);
      assertEquals("@thefirstsequence\n" + seqString + "\n+\n" + qualString + "\n", sw.toString());
    }
  }

  private void checkBaseFile(String expectedBase, String expExtension, boolean gz, BaseFile res) {
    checkBaseFile(new File(expectedBase), expExtension, gz, res);
  }
  private void checkBaseFile(File expectedBase, String expExtension, boolean gz, BaseFile res) {
    assertEquals(expectedBase, res.getBaseFile());
    assertEquals(expExtension, res.getExtension());
    assertEquals(gz, res.isGzip());
  }
}