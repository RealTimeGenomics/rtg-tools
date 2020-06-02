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

import com.rtg.util.ObjectParams;
import com.rtg.util.ParamsBuilder;
import com.rtg.util.test.params.ParamsNoField;

/**
 * ModuleParams represents a Params for a high-level slim module. Currently these modules also need to have some notion of a main output directory (this should be abstracted out).
 */
public abstract class ModuleParams extends ObjectParams implements OutputDirParams {

  /**
   * @param <B> the builder type
   */
  public abstract static class ModuleParamsBuilder<B extends ModuleParamsBuilder<B>> extends ParamsBuilder<B> {

    protected String mName = "ModuleParams";

    /**
     * Sets the application name.
     * @param name the application name.
     * @return this builder, so calls can be chained.
     */
    public B name(final String name) {
      mName = name;
      return self();
    }

  }

  private final String mName;

  protected ModuleParams(final String name) {
    if (name == null) {
      throw new NullPointerException();
    }
    mName = name;
    mObjects = new Object[] {mName};
  }

  protected ModuleParams(final ModuleParamsBuilder<?> builder) {
    mName = builder.mName;
    mObjects = new Object[] {mName};
  }

  /**
   * Generate a child file in the output directory.
   * @param name of the child.
   * @return a child file in the output directory.
   */
  @ParamsNoField
  public abstract File file(String name);

  /**
   * @return the external name of the application.
   */
  public String name() {
    return mName;
  }

  @Override
  public String toString() {
    return name() + LS;
  }
}
