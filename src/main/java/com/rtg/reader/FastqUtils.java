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
import java.io.Writer;
import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

/**
 * Functions for working with FASTQ files
 */
public final class FastqUtils {

  private FastqUtils() { }


  private static final String[] EXTS = {".fastq", ".fq"};

  /**
   * @return array of extensions that we recognize for FASTQ files
   */
  public static String[] extensions() {
    return Arrays.copyOf(EXTS, EXTS.length);
  }

  /**
   * Takes a file and returns a FASTQ base file, removing any gzip extension and storing a FASTQ extension if found
   * @param file the source file
   * @param gzip whether output is intended to be gzipped
   * @return the base file
   */
  public static BaseFile baseFile(File file, boolean gzip) {
    return FileUtils.getBaseFile(file, gzip, EXTS);
  }

  /**
   * Check if the supplied file is has an extension indicative of FASTQ.
   * @param f file to test
   * @return true if file has FASTQ extension
   */
  public static boolean isFastqExtension(File f) {
    final String name = FileUtils.isGzipFilename(f) ? FileUtils.removeExtension(f.getName()) : f.getName();
    return Arrays.stream(EXTS).anyMatch(name::endsWith);
  }

  /**
   * Write a FASTQ sequence
   * @param w writer to output FASTQ to
   * @param seqName the name of the sequence
   * @param seqData the sequence data (encoded as per {@link com.rtg.mode.DNA})
   * @param qualityData the quality in raw phred values
   * @param length how long the sequence data is
   * @throws IOException if an IO error occurs
   */
  public static void writeFastqSequence(Writer w, String seqName, byte[] seqData, byte[] qualityData, int length) throws IOException {
    w.write("@");
    w.write(seqName);
    w.write("\n");
    w.write(DnaUtils.bytesToSequenceIncCG(seqData, 0, length));
    w.write("\n");
    w.write("+");
    //mAppend.append(name);
    w.write("\n");
    w.write(FastaUtils.rawToAsciiString(qualityData, 0, length));
    w.write("\n");
  }

  /**
   * Write a FASTQ sequence
   * @param w writer to output FASTQ to
   * @param seqName the name of the sequence
   * @param seqData the sequence data (encoded as per {@link com.rtg.mode.DNA})
   * @param qualityData the quality in raw phred values
   * @throws IOException if an IO error occurs
   */
  public static void writeFastqSequence(Writer w, String seqName, byte[] seqData, byte[] qualityData) throws IOException {
    writeFastqSequence(w, seqName, seqData, qualityData, seqData.length);
  }
}
