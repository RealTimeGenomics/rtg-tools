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

import java.io.File;
import java.util.Collection;

/**
 * Encapsulates a single collection of mapping output files (typically when
 * used as input for another command).
 *
 */
public abstract class SingleMappedParams extends MappedParams {

  /**
   * Builder class for SingleMappedParams
   * @param <B> the builder type
   */
  public abstract static class SingleMappedParamsBuilder<B extends SingleMappedParamsBuilder<B>> extends MappedParamsBuilder<B> {

    protected Collection<File> mMapped;
    protected int mIoThreads = 1;
    protected int mExecThreads = 1;

    /**
     * Sets the mapping input files.
     * @param mapped the files containing mappings.
     * @return this builder, so calls can be chained.
     */
    public B mapped(final Collection<File> mapped) {
      mMapped = mapped;
      return self();
    }

    /**
     * The number of additional threads to use for Sam file reading and parsing.
     * @param ioThreads number of additional threads to run for reading Sam files.
     *          Default is 0.
     * @return this builder, so calls can be chained.
     */
    public B ioThreads(final int ioThreads) {
      mIoThreads = ioThreads;
      return self();
    }

    /**
     * The number of additional threads to use for Sam file reading and parsing.
     * @param execThreads number of additional threads to run for reading Sam files.
     *          Default is 0.
     * @return this builder, so calls can be chained.
     */
    public B execThreads(final int execThreads) {
      mExecThreads = execThreads;
      return self();
    }

  }

  private final Collection<File> mMapped;
  private final int mIoThreads;
  private final int mExecThreads;

  protected SingleMappedParams(final SingleMappedParamsBuilder<?> builder) {
    super(builder);
    mMapped = builder.mMapped;
    mIoThreads = builder.mIoThreads;
    mExecThreads = builder.mExecThreads;
  }

  /**
   * Get the mapped reads (in SAM/BAM format).
   * @return the mapped reads (in SAM/BAM format).
   */
  public Collection<File> mapped() {
    return mMapped;
  }

  /**
   * Get the number of extra threads to use in the Sam File Reading Pool
   * (Default 0)
   * @return the number of threads to use
   */
  public int ioThreads() {
    return mIoThreads;
  }

  /**
   * Get the number of threads to use for execution.
   * @return the number of threads to use for execution (default 1).
   */
  public int execThreads() {
    return mExecThreads;
  }
}
