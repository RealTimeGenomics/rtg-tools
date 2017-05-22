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

import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;


/**
 * Used for testing error handling etc. in <code>CLI</code>.
 * Will throw the errors depending on the flags set.
 */
public class MockCliParams extends ModuleParams {


  private static final class MockValidator implements Validator {
    @Override
    public boolean isValid(final CFlags flags) {
      if (flags.isSet("validator")) {
        Diagnostic.error("The specified flag \"validator\" has invalid value \"42\". It should be greater than or equal to 1.");
        return false;
      }
      return true;
    }
  }

  /**
   * Set flags.
   * @param flags to be set.
   */
  public static void makeFlags(final CFlags flags) {
    flags.setValidator(new MockValidator());
    flags.registerOptional('g', "global", "error thrown in globalIntegrity()");
    flags.registerOptional('v', "validator", "error thrown in validator");
    flags.registerOptional('c', "constructor", "InvalidParamsException thrown in params constructor");
    //this happens before the log is switched - it should be left there and include the cause
    flags.registerOptional('x', "constructorx", "RuntimeException thrown in params constructor");
    flags.registerOptional('e', "runtimeErr", "Write to err during task execution");
    flags.registerOptional('r', "runtime", "RuntimeException thrown during execution of task");
    flags.registerOptional('s', "runtimeslim", "SlimException thrown during execution of task");
  }

  private final boolean mGlobalError;

  private final boolean mValidatorError;

  private final boolean mConstructorError;

  final boolean mRuntime;

  final boolean mRuntimeSlim;

  final boolean mRuntimeErr;

  /**
   * @param flags command line flags.
   * @throws InvalidParamsException if there are errors in the command line parameters.
   */
  public MockCliParams(final CFlags flags) {
    super(flags.getName());
    mGlobalError = flags.isSet("global");
    mValidatorError = flags.isSet("validator");
    mConstructorError = flags.isSet("constructor");
    final boolean errorX = flags.isSet("constructorx");
    mRuntime = flags.isSet("runtime");
    mRuntimeSlim = flags.isSet("runtimeslim");
    mRuntimeErr = flags.isSet("runtimeErr");

    if (mConstructorError) {
      throw new InvalidParamsException(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "-c", "42", "41");
    }
    if (errorX) {
      throw new RuntimeException("in parameter constructor");
    }

  }

  @Override
  public String toString() {
    return (mGlobalError ? " global" : "") + (mValidatorError ? " validator" : "") + (mConstructorError ? " constructor" : "");
  }

  @Override
  public File file(final String name) {
    throw new UnsupportedOperationException();
  }

  private File mDirectory = null;

  public void setDirectory(final File dir) {
    mDirectory = dir;
  }

  @Override
  public File directory() {
    if (mDirectory != null) {
      return mDirectory;
    }
    throw new UnsupportedOperationException();
  }

}
