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

import junit.framework.TestCase;

public class SamBamBaseFileTest extends TestCase {

  public void testGetBaseFile() {
    assertEquals(new SamBamBaseFile(new File("foo.bar.bang"), ".bam", false, SamBamBaseFile.SamFormat.BAM), SamBamBaseFile.getBaseFile(new File("foo.bar.bang"), true));
    assertEquals(new SamBamBaseFile(new File("foo.bar.bang"), ".bam", false, SamBamBaseFile.SamFormat.BAM), SamBamBaseFile.getBaseFile(new File("foo.bar.bang.gz"), true));
    assertEquals(new SamBamBaseFile(new File("foo.bar"), ".sam", true, SamBamBaseFile.SamFormat.SAM), SamBamBaseFile.getBaseFile(new File("foo.bar.sam"), true));
    assertEquals(new SamBamBaseFile(new File("foo.bar"), ".sam", true, SamBamBaseFile.SamFormat.SAM), SamBamBaseFile.getBaseFile(new File("foo.bar.sam.gz"), true));
    assertEquals(new SamBamBaseFile(new File("foo.bar"), ".sam", false, SamBamBaseFile.SamFormat.SAM), SamBamBaseFile.getBaseFile(new File("foo.bar.sam.gz"), false));
    assertEquals(new SamBamBaseFile(new File("foo.bar"), ".bam", false, SamBamBaseFile.SamFormat.BAM), SamBamBaseFile.getBaseFile(new File("foo.bar.bam"), true));
    assertEquals(new SamBamBaseFile(new File("foo.bar"), ".bam", false, SamBamBaseFile.SamFormat.BAM), SamBamBaseFile.getBaseFile(new File("foo.bar.bam.gz"), true));
    assertEquals(new SamBamBaseFile(new File("foo.bar"), ".bam", false, SamBamBaseFile.SamFormat.BAM), SamBamBaseFile.getBaseFile(new File("foo.bar.bam.gz"), false));
  }

  public void test() {
    final SamBamBaseFile file1 = new SamBamBaseFile(new File("filename"), ".bam", false, SamBamBaseFile.SamFormat.BAM);
    assertEquals(new File("filename"), file1.getBaseFile());
    assertEquals(".bam", file1.getExtension());
    assertFalse(file1.isGzip());
    assertEquals(new File("filename.bam"), file1.suffixedFile(""));
    assertEquals(new File("filename_suffix.bam"), file1.suffixedFile("_suffix"));
    final SamBamBaseFile file2 = new SamBamBaseFile(new File("filename"), ".sam", true, SamBamBaseFile.SamFormat.SAM);
    assertEquals(new File("filename.sam.gz"), file2.suffixedFile(""));
    assertEquals(new File("filename_suffix.sam.gz"), file2.suffixedFile("_suffix"));
  }
}