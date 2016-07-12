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

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.reader.AnnotatedSequencesReader;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.Resources;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 *
 */
public class ReferenceDetectorTest extends AbstractNanoTest {

  public void test() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final String dna = mNano.loadReference("testref.fasta");
      final File sdfDir = ReaderTestUtils.getDNADir(dna, new File(dir, "sdf"));
      final ReferenceDetector detector = ReferenceDetector.loadManifest(Resources.getResourceAsStream("com/rtg/reference/resources/testref.manifest"));
      try (final AnnotatedSequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(sdfDir)) {
        assertTrue(detector.checkReference(reader));
        detector.installReferenceConfiguration(reader);
        final File refTxt = new File(sdfDir, "reference.txt");
        assertTrue(refTxt.exists());
        mNano.check("testref-reference.txt", FileHelper.fileToString(refTxt), false);
      }
    }
  }
}