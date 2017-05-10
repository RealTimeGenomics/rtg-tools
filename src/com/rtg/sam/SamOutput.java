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

import com.rtg.reader.SequencesReader;
import com.rtg.tabix.IndexingStreamCreator;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.io.AdjustableGZIPOutputStream;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.CRAMFileWriter;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.util.BlockCompressedOutputStream;

/**
 * Handles managing the various things we want when outputting SAM or BAM with indexing
 */
public final class SamOutput implements Closeable {
  private final File mOutFile;
  private final Closeable mStreamCreator;
  private final SAMFileWriter mWriter;

  private SamOutput(File outFile, Closeable streamCreator, SAMFileWriter writer) {
    mOutFile = outFile;
    mStreamCreator = streamCreator;
    mWriter = writer;
  }

  /**
   * Creates a SAM or BAM writer as appropriate and generates an index for this output if possible. Also writes to standard output if filename is "-".
   * This method does not support CRAM output.
   * @param filename filename given by user
   * @param stdio output stream to use if filename is "-" (standard out)
   * @param header header for output SAM/BAM file
   * @param gzipIfPossible whether we should attempt to compress output file
   * @param presorted if input if in correct sort order
   * @param reference the reference used to resolve CRAM, or null if no CRAM support is required
   * @return wrapper containing writer and other relevant things
   * @throws IOException if an IO Error occurs
   */
  public static SamOutput getSamOutput(File filename, OutputStream stdio, SAMFileHeader header, boolean gzipIfPossible, boolean presorted, SequencesReader reference) throws IOException {
    return getSamOutput(filename, stdio, header, gzipIfPossible, presorted, true, true, true, reference);
  }

  /**
   * Creates a SAM or BAM writer as appropriate and generates an index for this output if possible. Also writes to standard output if filename is "-".
   * @param filename filename given by user
   * @param stdio output stream to use if filename is "-" (standard out)
   * @param header header for output SAM/BAM file
   * @param gzipIfPossible whether we should attempt to compress output file
   * @param presorted if input if in correct sort order
   * @param writeHeader true if the header should be written, false otherwise
   * @param terminateBlockGzip true if the output stream should contain a termination block (may be false if doing indexing of chunks)
   * @param indexIfPossible true if the output should be indexed if possible
   * @param reference the reference used to resolve CRAM, or null if no CRAM support is required
   * @return wrapper containing writer and other relevant things
   * @throws IOException if an IO Error occurs
   */
  public static SamOutput getSamOutput(File filename, OutputStream stdio, SAMFileHeader header, boolean gzipIfPossible, boolean presorted, boolean writeHeader, boolean terminateBlockGzip, boolean indexIfPossible, SequencesReader reference) throws IOException {
    final SamBamBaseFile baseFile = SamBamBaseFile.getBaseFile(filename, gzipIfPossible);
    final File outputFile;
    final OutputStream outputStream;
    final SamBamBaseFile.SamFormat type;
    final boolean compress;
    if (FileUtils.isStdio(filename)) { // Use uncompressed SAM for stdout
      outputFile = null;
      outputStream = stdio;
      type = SamBamBaseFile.SamFormat.SAM;
      compress = false;
    } else {
      outputFile = baseFile.file();
      outputStream = null;
      type = baseFile.format();
      compress = type != SamBamBaseFile.SamFormat.SAM || baseFile.isGzip();
    }
    if (type == SamBamBaseFile.SamFormat.CRAM) {
      if (!writeHeader || !terminateBlockGzip) {
        throw new UnsupportedOperationException("Piecewise CRAM output is not supported");
      }
      final OutputStream indexOut = indexIfPossible ? FileUtils.createOutputStream(BamIndexer.indexFileName(outputFile)) : null;
      try {
        final SAMFileWriter writer = new CRAMFileWriter(FileUtils.createOutputStream(outputFile), indexOut, presorted, reference == null ? SamUtils.NO_CRAM_REFERENCE_SOURCE : reference.referenceSource(), header, outputFile.getName());
        return new SamOutput(outputFile, indexOut, writer);
      } catch (Throwable t) {
        indexOut.close();
        throw t;
      }
    } else {
      final TabixIndexer.IndexerFactory indexerFactory = type == SamBamBaseFile.SamFormat.SAM ? new TabixIndexer.SamIndexerFactory() : null;
      final IndexingStreamCreator streamCreator = new IndexingStreamCreator(outputFile, outputStream, compress, indexerFactory, indexIfPossible);
      try {
        final OutputStream samOutputstream = streamCreator.createStreamsAndStartThreads(header.getSequenceDictionary().size(), writeHeader, terminateBlockGzip);
        try {
          final SAMFileWriterFactory fact = new SAMFileWriterFactory().setTempDirectory(filename.getAbsoluteFile().getParentFile()).setUseAsyncIo(true);
          final SAMFileWriter writer;
          switch (type) {
            case BAM:
              writer = fact.makeBAMWriter(header, presorted, new BlockCompressedOutputStream(samOutputstream, null, AdjustableGZIPOutputStream.DEFAULT_GZIP_LEVEL, terminateBlockGzip), writeHeader, false /* ignored */, true);
              break;
            case SAM:
              writer = fact.makeSAMWriter(header, presorted, samOutputstream, writeHeader);
              break;
            default:
              throw new UnsupportedOperationException();
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
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (Closeable ignored = mStreamCreator;
         SAMFileWriter ignored2 = mWriter
    ) {

    }
  }

  public File getOutFile() {
    return mOutFile;
  }

  public SAMFileWriter getWriter() {
    return mWriter;
  }
}
