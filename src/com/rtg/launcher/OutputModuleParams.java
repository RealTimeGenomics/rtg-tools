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

package com.rtg.launcher;

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.test.params.ParamsNoField;

/**
 * OutputModuleParams represents a Params for a high-level slim module with an OutputParams controlling its output directory.
 */
public abstract class OutputModuleParams extends ModuleParams {

  /**
   * A builder class for <code>OutputModuleParams</code>.
   * @param <B> builder type
   */
  public abstract static class OutputModuleParamsBuilder<B extends OutputModuleParamsBuilder<B>> extends ModuleParamsBuilder<B> {

    protected OutputParams mOutputParams;

    /**
     * Sets the output parameters.
     *
     * @param params the parameters used for output.
     * @return this builder, so calls can be chained.
     */
    public B outputParams(final OutputParams params) {
      mOutputParams = params;
      return self();
    }

  }

  private final OutputParams mOutputParams;

  /**
   * @param builder the builder object.
   */
  protected OutputModuleParams(OutputModuleParamsBuilder<?> builder) {
    super(builder);
    mOutputParams = builder.mOutputParams;
  }

  /**
   * Get the output params object
   * @return the params
   */
  public OutputParams outputParams() {
    return mOutputParams;
  }

  @Override
  @ParamsNoField
  public File file(final String name) {
    return mOutputParams.file(name);
  }

  @Override
  @ParamsNoField
  public File directory() {
    return mOutputParams == null ? null : mOutputParams.directory();
  }

  /**
   * Return the file that will be used for output given a base name.
   * This obeys any settings for whether compression should be applied.
   * @param name file name
   * @return file to be used for output
   */
  @ParamsNoField
  public File outFile(String name) {
    return mOutputParams.outFile(name);
  }

  /**
   * Get a stream to an output file in the output directory.
   * This obeys any settings for whether compression should be applied.
   * @param name file name
   * @return the stream.
   * @throws IOException if an I/O error occurs.
   */
  @ParamsNoField
  public OutputStream outStream(String name) throws IOException {
    return mOutputParams.outStream(name);
  }

  /**
   * @return true if output is to be compressed in blocked compressed format
   */
  @ParamsNoField
  public boolean blockCompressed() {
    return mOutputParams.isBlockCompressed();
  }

  @Override
  public String toString() {
    return mOutputParams + LS;
  }

}
