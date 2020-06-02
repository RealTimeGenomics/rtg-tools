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

import static com.rtg.util.StringUtils.LS;

import java.io.IOException;

import com.rtg.launcher.OutputModuleParams;
import com.rtg.launcher.ReaderParams;
import com.rtg.util.test.params.ParamsNoField;

/**
 * MappedParams represents a Params for a high-level slim module with
 * a genome which the input SAM records were mapped against and
 * a SamFilterParams controlling its filtering of SAM input records.
 */
public abstract class MappedParams extends OutputModuleParams {

  /**
   * A builder class for <code>SamFilteredModuleParams</code>.
   * @param <B> builder type
   */
  public abstract static class MappedParamsBuilder<B extends MappedParamsBuilder<B>> extends OutputModuleParamsBuilder<B> {

    protected SamFilterParams mFilterParams = SamFilterParams.builder().create();
    protected ReaderParams mGenome;

    boolean mIgnoreIncompatibleSamHeaders = false;

    /**
     * SAM filtering parameters.
     * @param val filtering parameters
     * @return this builder, so calls can be chained.
     */
    public B filterParams(final SamFilterParams val) {
      mFilterParams = val;
      return self();
    }

    /**
     * Sets the genome reader.
     * @param params the parameters used for output.
     * @return this builder, so calls can be chained.
     */
    public B genome(final ReaderParams params) {
      mGenome = params;
      return self();
    }

    /**
     * true to ignore incompatible headers when merging SAM records
     * @param val the value
     * @return this builder
     */
    public B ignoreIncompatibleSamHeaders(boolean val) {
      mIgnoreIncompatibleSamHeaders = val;
      return self();
    }
  }

  private final SamFilterParams mFilterParams;
  private final ReaderParams mGenome;
  private final boolean mIgnoreIncompatibleSamHeaders;

  /**
   * @param builder the builder object.
   */
  protected MappedParams(MappedParamsBuilder<?> builder) {
    super(builder);
    mFilterParams = builder.mFilterParams;
    mGenome = builder.mGenome;
    mIgnoreIncompatibleSamHeaders = builder.mIgnoreIncompatibleSamHeaders;
  }

  /**
   * Get the filter params object
   * @return the params
   */
  public SamFilterParams filterParams() {
    return mFilterParams;
  }

  /**
   *  Get parameters for the reference genome.
   * @return parameters for the reference genome.
   */
  public ReaderParams genome() {
    return mGenome;
  }

  /**
   * @return true to ignore incompatible headers when merging SAM records
   */
  public boolean ignoreIncompatibleSamHeaders() {
    return mIgnoreIncompatibleSamHeaders;
  }

  @Override
  @ParamsNoField
  //params should be invariant.
  public void close() throws IOException {
    if (mGenome != null) {
      mGenome.close();
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(mFilterParams).append(LS);
    if (mGenome != null) {
      sb.append("    ").append(mGenome);
      sb.append(LS);
    }
    sb.append(super.toString());
    return sb.toString();
  }
}
