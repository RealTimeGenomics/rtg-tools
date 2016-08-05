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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.launcher.CommonFlags;
import com.rtg.tabix.IndexingStreamCreator;
import com.rtg.tabix.TabixIndexer;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;

/**
 * Handles managing the various things we want when outputting SAM or BAM with indexing
 */
public final class SamOutput implements Closeable {
  private final File mOutFile;
  private final IndexingStreamCreator mStreamCreator;
  private final SAMFileWriter mWriter;

  private SamOutput(File outFile, IndexingStreamCreator streamCreator, SAMFileWriter writer) {
    mOutFile = outFile;
    mStreamCreator = streamCreator;
    mWriter = writer;
  }

  /**
   * Creates a SAM or BAM writer as appropriate and generates an index for this output if possible. Also writes to standard output if filename is "-".
   * @param filename filename given by user
   * @param stdio output stream to use if filename is "-" (standard out)
   * @param header header for output SAM/BAM file
   * @param gzipIfPossible whether we should attempt to compress output file
   * @return wrapper containing writer and other relevant things
   * @throws IOException if an IO Error occurs
   */
  public static SamOutput getSamOutput(File filename, OutputStream stdio, SAMFileHeader header, boolean gzipIfPossible) throws IOException {
    final SamBamBaseFile baseFile = SamBamBaseFile.getBaseFile(filename, gzipIfPossible);
    final File outputFile;
    final OutputStream outputStream;
    final boolean bam;
    final boolean compress;
    if (CommonFlags.isStdio(filename)) {
      outputFile = null;
      outputStream = stdio;
      bam = false;
      compress = false;
    } else {
      outputFile = baseFile.suffixedFile("");
      outputStream = null;
      bam = baseFile.isBam();
      compress = bam || baseFile.isGzip();
    }
    final TabixIndexer.IndexerFactory indexerFactory = bam ? null : new TabixIndexer.SamIndexerFactory();
    final IndexingStreamCreator streamCreator = new IndexingStreamCreator(outputFile, outputStream, compress, indexerFactory, true);
    try {
      final OutputStream samOutputstream = streamCreator.createStreamsAndStartThreads(header.getSequenceDictionary().size(), true, true);
      try {
        final SAMFileWriter writer;
        if (bam) {
          writer = new SAMFileWriterFactory().makeBAMWriter(header, true, samOutputstream);
        } else {
          writer = new SAMFileWriterFactory().makeSAMWriter(header, true, samOutputstream);
        }
        return new SamOutput(outputFile, streamCreator, writer);
      } catch (Throwable t) {
        samOutputstream.close();
        throw t;
      }
    } catch (Throwable t) {
      streamCreator.close();
      throw t;
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (IndexingStreamCreator ignored = mStreamCreator;
         SAMFileWriter ignored2 = mWriter
    ) {

    }
  }

  public File getOutFile() {
    return mOutFile;
  }

  public IndexingStreamCreator getStreamCreator() {
    return mStreamCreator;
  }

  public SAMFileWriter getWriter() {
    return mWriter;
  }
}
