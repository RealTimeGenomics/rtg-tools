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
package com.rtg.util;

import java.io.IOException;

/**
 * Wrapper for running an {@link IORunnable} inside a framework that only accepts
 * {@link Runnable}
 */
public class IORunnableProxy implements Runnable {

  private final IORunnable mWrapped;

  private IOException mIOException;
  private RuntimeException mRuntimeException;
  private Error mError;

  /**
   * @param wrapped task to wrap
   */
  public IORunnableProxy(IORunnable wrapped) {
    mWrapped = wrapped;
  }


  @Override
  public void run() {
    try {
      mWrapped.run();
    } catch (IOException e) {
      mIOException = e;
    } catch (RuntimeException e) {
      mRuntimeException = e;
    } catch (Error e) {
      mError = e;
    }
  }

  /**
   * Throws any exception or error encountered during the run, wrapped in the appropriate wrap. If there were
   * no problems nothing happens
   * @throws IOException if an <code>IOException</code> was encountered during run
   * @throws RuntimeException if a <code>RuntimeException</code> was encountered during run.
   * @throws Error if a <code>Error</code> was encountered during run.
   */
  public void checkError() throws IOException {
    if (mIOException != null) {
      throw mIOException;
    } else if (mRuntimeException != null) {
      throw mRuntimeException;
    } else if (mError != null) {
      throw mError;
    }
  }
}
