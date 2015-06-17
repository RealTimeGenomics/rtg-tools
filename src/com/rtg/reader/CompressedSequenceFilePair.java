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
package com.rtg.reader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.io.FileUtils;

/**
 * File pair implementation for writing compressed data
 */
@TestClass("com.rtg.reader.SequencesWriterTest")
class CompressedSequenceFilePair extends NormalSequenceFilePair {

  /**
   * Constructor
   * @param dir directory to write files to
   * @param fileNum file number to write
   * @param quality whether to write quality data
   * @param limit largest number of values allowed
   * @param seqValues range of possible values
   * @param checksumSeq checksum tracker for sequence data
   * @param checksumQual checksum tracker for quality data
   * @throws IOException if an IO error occurs
   */
  public CompressedSequenceFilePair(File dir, int fileNum, boolean quality, final long limit, final int seqValues, CRC32 checksumSeq, CRC32 checksumQual) throws IOException {
    super(new FileBitwiseOutputStream(SdfFileUtils.sequenceDataFile(dir, fileNum), CompressedByteArray.minBits(seqValues)),
          quality ? new FileCompressedOutputStream(SdfFileUtils.qualityDataFile(dir, fileNum), SdfWriter.MAX_QUALITY_VALUE) : null,
          new DataOutputStream(FileUtils.createOutputStream(SdfFileUtils.sequencePointerFile(dir, fileNum), false)),
          quality, limit, checksumSeq, checksumQual);

  }
}
