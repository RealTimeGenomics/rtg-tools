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

import com.reeltwo.jumble.annotations.TestClass;


/**
 * Stream manager for reading labels
 * should only be used by DefaultSequencesReader
 */
@TestClass("com.rtg.reader.DefaultSequencesReaderTest")
class LabelStreamManager extends AbstractStreamManager {

  LabelStreamManager(final File dir, final long numberSequences, final String indexFile, final String dataPrefix, final String pointerPrefix, long dataIndexVersion, DataFileOpenerFactory openerFactory) throws IOException {
    super(dir, numberSequences, indexFile, dataPrefix, pointerPrefix, dataIndexVersion, openerFactory.getLabelOpener());
  }

  static LabelStreamManager getNameStreamManager(File dir, long numberSequences, long dataIndexVersion, DataFileOpenerFactory openerFactory) throws IOException {
    return new LabelStreamManager(dir, numberSequences, SdfFileUtils.LABEL_INDEX_FILENAME, SdfFileUtils.LABEL_DATA_FILENAME, SdfFileUtils.LABEL_POINTER_FILENAME, dataIndexVersion, openerFactory);
  }

  static LabelStreamManager getSuffixStreamManager(File dir, long numberSequences, long dataIndexVersion, DataFileOpenerFactory openerFactory) throws IOException {
    return new LabelStreamManager(dir, numberSequences, SdfFileUtils.LABEL_SUFFIX_INDEX_FILENAME, SdfFileUtils.LABEL_SUFFIX_DATA_FILENAME, SdfFileUtils.LABEL_SUFFIX_POINTER_FILENAME, dataIndexVersion, openerFactory);
  }

  @Override
  protected void seekImpl(final long seqNum) throws IOException {
    final long pointerpos = seqNum - mCurrentLower;
    mPointers.randomAccessFile().seek(pointerpos * 4L);

    final long seqpos = readInt(mPointers.randomAccessFile()); //.readInt();
    final long length;
    if (mPointers.randomAccessFile().length() - mPointers.randomAccessFile().getPosition() >= 4L) {
      final int nextseqpos;
      nextseqpos = readInt(mPointers.randomAccessFile()); //.readInt();
      length = nextseqpos - seqpos;
    } else {
      length = mIndex.dataSize(mIndexedSequenceFileNumber) - seqpos; //mData.randomAccessFile().length() - seqpos;
    }
    if (length - 1 > Integer.MAX_VALUE || length < 0) {
      //we cast to int in various places
      throw new CorruptSdfException();
    }
    mDataLength = length - 1;
    mData.randomAccessFile().seek(seqpos);
  }
}

