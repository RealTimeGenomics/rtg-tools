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


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.IORunnable;


/**
 */
public class MockCli extends ParamsCli<MockCliParams> {

  private final File mOutDir;

  /**
   * Create a mock command line with specified output directory.
   * @param outDir output directory
   */
  public MockCli(final File outDir) {
    mOutDir = outDir;
  }

  /**
   * Create a mock command line with no output directory.
   */
  public MockCli() {
    this(null);
  }

  @Override
  public String applicationName() {
    return "MockCli";
  }

  @Override
  protected void initFlags() {
    MockCliParams.makeFlags(mFlags);
  }

  @Override
  protected MockCliParams makeParams() {
    return new MockCliParams(mFlags);
  }

  @Override
  protected IORunnable task(MockCliParams params, final OutputStream out) {
    try {
      out.write("Mock task did something".getBytes());
    } catch (final IOException e) {
      // do nothing
    }
    return null;
  }

  @Override
  protected File outputDirectory() {
    if (mOutDir != null) {
      return mOutDir;
    }
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String moduleName() {
    return "";
  }

}
