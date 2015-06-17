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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.mode.SequenceType;
import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.io.BufferedRandomAccessFile;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.RandomAccessFileStream;
import com.rtg.util.io.SeekableStream;

/**
 * Class for opening variety of SDF data files
 */
public class DataFileOpenerFactory {

  private final DataFileOpener mLabel;
  private final DataFileOpener mSequence;
  private final DataFileOpener mQuality;

  /**
   * Create a factory for providing {@link DataFileOpener}'s for given parameters
   * @param sequenceEncoding the sequence encoding (obtained from {@link com.rtg.reader.IndexFile})
   * @param qualityEncoding the quality encoding (obtained from {@link com.rtg.reader.IndexFile})
   * @param type the sequence type
   */
  public DataFileOpenerFactory(byte sequenceEncoding, byte qualityEncoding, SequenceType type) {
    mLabel = new NormalOpener();
    mSequence = IndexFile.SEQUENCE_ENCODING_COMPRESSED == sequenceEncoding ? new BitwiseOpener(CompressedByteArray.minBits(type.numberCodes())) : new NormalOpener();
    mQuality = IndexFile.QUALITY_ENCODING_COMPRESSED == qualityEncoding ? new CompressedOpener(CompressedMemorySequencesReader.MAX_QUAL_VALUE) : new NormalOpener();
  }

  /**
   * @return opener for label data
   */
  public DataFileOpener getLabelOpener() {
    return mLabel;
  }

  /**
   * @return opener for sequence data
   */
  public DataFileOpener getSequenceOpener() {
    return mSequence;
  }

  /**
   * @return opener for quality data
   */
  public DataFileOpener getQualityOpener() {
    return mQuality;
  }

  private static class NormalOpener implements DataFileOpener {
    @Override
    public InputStream open(File filename, long length) throws IOException {
      return FileUtils.createFileInputStream(filename, false);
    }
    @Override
    public SeekableStream openRandomAccess(File filename, long length) throws IOException {
      return new RandomAccessFileStream(new BufferedRandomAccessFile(filename, "r"));
    }
  }
  private static class BitwiseOpener implements DataFileOpener {
    private final int mBits;
    public BitwiseOpener(int bits) {
      mBits = bits;
    }
    @Override
    public InputStream open(File filename, long length) throws IOException {
      return new FileBitwiseInputStream(filename, mBits, length, false);
    }
    @Override
    public SeekableStream openRandomAccess(File filename, long length) throws IOException {
      return new FileBitwiseInputStream(filename, mBits, length, true);
    }
  }

  private static class CompressedOpener implements DataFileOpener {
    private final int mRange;
    public CompressedOpener(int range) {
      mRange = range;
    }
    @Override
    public InputStream open(File filename, long length) throws IOException {
      return new FileCompressedInputStream(filename, mRange, length, false);
    }
    @Override
    public SeekableStream openRandomAccess(File filename, long length) throws IOException {
      return new FileCompressedInputStream(filename, mRange, length, true);
    }
  }
}
