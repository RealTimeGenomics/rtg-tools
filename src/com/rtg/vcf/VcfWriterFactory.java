/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
import java.io.OutputStream;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.CFlags;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Finer control over how VcfWriters are created.
 */
public class VcfWriterFactory {

  private boolean mZip = true;
  private boolean mIndex = true;
  private boolean mWriteHeader = true;
  private boolean mAsync = true;
  private boolean mAddRunInfo = false;

  /**
   * Constructor using defaults to be overridden manually.
   */
  public VcfWriterFactory() { }

  /**
   * Create a writer, determining configuration from a set of command line flags.
   * @param flags contains settings for VCF output
   */
  public VcfWriterFactory(CFlags flags) {
    mZip = !flags.isSet(CommonFlags.NO_GZIP);
    mIndex = !flags.isSet(CommonFlags.NO_INDEX);
    mWriteHeader = !flags.isSet(CommonFlags.NO_HEADER);
  }

  /**
   * Set whether output files should be compressed.
   * @param zip true if output files should be block compressed
   * @return this factory, for call chaining
   */
  public VcfWriterFactory zip(boolean zip) {
    mZip = zip;
    return this;
  }

  /**
   * Set whether output files should be indexed if possible.
   * @param index true if compressed output files should indexed
   * @return this factory, for call chaining
   */
  public VcfWriterFactory index(boolean index) {
    mIndex = index;
    return this;
  }

  /**
   * Set whether output files should have the header written.
   * @param writeHeader true if output files should have their header written
   * @return this factory, for call chaining
   */
  public VcfWriterFactory writeHeader(boolean writeHeader) {
    mWriteHeader = writeHeader;
    return this;
  }

  /**
   * Set whether output files should be written asynchronously.
   * @param async true if compressed output files should written asynchronously
   * @return this factory, for call chaining
   */
  public VcfWriterFactory async(boolean async) {
    mAsync = async;
    return this;
  }

  /**
   * Set whether command line information should be appended to the header.
   * @param addRunInfo true if command line should be added
   * @return this factory, for call chaining
   */
  public VcfWriterFactory addRunInfo(boolean addRunInfo) {
    mAddRunInfo = addRunInfo;
    return this;
  }

  /**
   * Make a writer using current configuration
   * @param header the output VCF header
   * @param output destination file
   * @return the VcfWriter
   * @throws IOException if there was a problem creating the writer
   */
  public VcfWriter make(VcfHeader header, File output) throws IOException {
    assert output != null;
    return make(header, output, null);
  }

  /**
   * Make a writer using current configuration.
   * XXX This should probably be refactored to use the <code>FileUtils.isStdio</code> approach
   * @param header the output VCF header
   * @param output the output file to be written to
   * @param stdout the output stream of stdout (will only be used if output file is null)
   * @return the VcfWriter
   * @throws IOException if there was a problem creating the writer
   */
  public VcfWriter make(VcfHeader header, File output, OutputStream stdout) throws IOException {
    assert stdout != null || output != null;
    assert output == null || !FileUtils.isStdio(output); // If you're using file-as-stdio, you shouldn't also supply an output stream
    final VcfHeader h2 = header.copy();
    if (mAddRunInfo) {
      h2.addRunInfo();
    }
    final VcfWriter w = new DefaultVcfWriter(h2, output, stdout, mZip, mIndex, mWriteHeader);
    return mAsync ? new AsyncVcfWriter(w) : w;
  }
}
