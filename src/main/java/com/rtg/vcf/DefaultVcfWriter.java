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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.tabix.IndexingStreamCreator;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.ByteUtils;
import com.rtg.vcf.header.VcfHeader;


/**
 * Writer to write VCF records out into a <code>.vcf</code> output stream.
 *
 */
public class DefaultVcfWriter implements VcfWriter {

  private final IndexingStreamCreator mIndexer;
  private final OutputStream mOut;
  private final VcfHeader mHeader;
  private boolean mHeaderWritten = false;

  /**
   * Creates a new VCF writer, using on-the-fly indexing, and can optionally skip writing the header
   * @param header header for the file
   * @param outputFile the output file to be written to
   * @param stdout the output stream of stdout (will only be used if output file is null)
   * @param compress true if the output should be gzip compressed (also true if bam is true)
   * @param createIndexIfPossible true if an index should be created
   * @param writeHeader true if the header should be written
   * @throws java.io.IOException if there is a problem during writing.
   */
  public DefaultVcfWriter(VcfHeader header, File outputFile, OutputStream stdout, boolean compress, boolean createIndexIfPossible, boolean writeHeader) throws IOException {
    if (header == null) {
      throw new NullPointerException("header cannot be null");
    }
    mIndexer = new IndexingStreamCreator(outputFile, stdout, compress, new TabixIndexer.VcfIndexerFactory(), createIndexIfPossible);
    mOut = mIndexer.createStreamsAndStartThreads();
    mHeader = header;
    mHeaderWritten = !writeHeader;
  }

  /**
   * create a new VCF writer, no frills
   *
   * @param header header for the file
   * @param out stream to write to
   */
  public DefaultVcfWriter(VcfHeader header, OutputStream out) {
    if (out == null) {
      throw new NullPointerException("output stream cannot be null");
    }
    if (header == null) {
      throw new NullPointerException("header cannot be null");
    }
    mIndexer = null;
    mOut = out;
    mHeader = header;
  }

  /**
   * write current header to output stream
   * @throws java.io.IOException if there is an I/O problem
   */
  protected void writeHeader() throws IOException {
    mOut.write(mHeader.toString().getBytes());
  }

  private void writeToStream(VcfRecord record) throws IOException {
    //System.err.println(record.toString());
    mOut.write(record.toString().getBytes());
    ByteUtils.writeNewline(mOut);
  }

  @Override
  public VcfHeader getHeader() {
    return mHeader;
  }

  @Override
  public void write(VcfRecord record) throws IOException {
    if (!mHeaderWritten) {
      mHeaderWritten = true;
      writeHeader();
    }
    writeToStream(record);
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    if (!mHeaderWritten) {
      mHeaderWritten = true;
      writeHeader();
    }
    try (Closeable ignored = mOut) {
      // Just using nice try-with-resources side effects.
    } finally {
      // We're explicitly allowing an exception thrown by mIndexer.close to shadow one thrown by mOut.close
      // Since it was probably the mIndexer exception that caused mOut to have a subsequent boggle
      try (Closeable ignored2 = mIndexer) {
        // Just using nice try-with-resources side effects.
      }
    }
  }


}
