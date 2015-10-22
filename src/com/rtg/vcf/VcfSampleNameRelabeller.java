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
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * An annotator allowing for sample names to be changed.
 */
@TestClass("com.rtg.vcf.VcfAnnotatorCliTest")
public class VcfSampleNameRelabeller implements VcfAnnotator {

  private final Map<String, String> mSampleNameMap;

  /**
   * Construct a new annotator for relabelling samples.
   * @param sampleNameMap mapping of old sample names to new sample names
   */
  public VcfSampleNameRelabeller(final Map<String, String> sampleNameMap) {
    mSampleNameMap = sampleNameMap;
  }

  @Override
  public void updateHeader(final VcfHeader header) {
    for (final Map.Entry<String, String> e : mSampleNameMap.entrySet()) {
      header.relabelSample(e.getKey(), e.getValue());
    }
  }

  @Override
  public void annotate(final VcfRecord rec) {
    // Actual records do not change as a result of sample name relabelling
  }

  /**
   * Create a sample relabeller backed by a file.
   * @param relabelFile file containing pairs of old names and new names
   * @return relabeller
   * @throws IOException if an I/O error occurs.
   */
  public static VcfSampleNameRelabeller create(final File relabelFile) throws IOException {
    final HashMap<String, String> map = new HashMap<>();
    try (final LineNumberReader r = new LineNumberReader(FileUtils.createReader(relabelFile, false))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.length() > 0) {
          final String[] parts = line.trim().split("\\s+");
          if (parts.length != 2) {
            throw new NoTalkbackSlimException("Expected: old-name new-name on line " + r.getLineNumber() + " of " + relabelFile.getPath() + "\nSaw: " + line);
          }
          map.put(parts[0], parts[1]);
        }
      }
    }
    return new VcfSampleNameRelabeller(map);
  }
}
