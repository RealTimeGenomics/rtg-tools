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

import java.io.IOException;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.vcf.header.VcfHeader;

import htsjdk.samtools.util.AbstractAsyncWriter;
import htsjdk.samtools.util.RuntimeIOException;

/**
 * Adaptor that allows asynchronous writing of our VcfRecord objects.
 */
public class AsyncVcfWriter extends AbstractAsyncWriter<VcfRecord> implements VcfWriter {

  private static final int BUFFER_SIZE = GlobalFlags.getIntegerValue(ToolsGlobalFlags.VCF_ASYNC_BUFFER_SIZE);

  private final VcfWriter mWriter;

  /**
   * Constructor
   * @param w the inner VcfWriter
   */
  public AsyncVcfWriter(VcfWriter w) {
    super(BUFFER_SIZE);
    mWriter = w;
  }

  @Override
  public VcfHeader getHeader() {
    return mWriter.getHeader();
  }

  @Override
  protected String getThreadNamePrefix() {
    return "VcfWriter";
  }

  @Override
  protected void synchronouslyWrite(VcfRecord record) {
    try {
      mWriter.write(record);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  @Override
  protected void synchronouslyClose() {
    try {
      mWriter.close();
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }
}
