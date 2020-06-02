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

import com.rtg.util.diagnostic.Diagnostic;

/**
 * <p>Contains static information about program state.
 *
 * <p>Code paths using these mechanisms should be self contained i.e. {@link #setAbort()}
 * and {@link #clearAbort()} should only be called by the main thread and any potential {@link #setAbort()}
 * call should have a <code>finally</code> condition specifying {@link #clearAbort()}
 * once the other threads have safely exited. Note there will be issues for nested structures
 * in this setup.
 *
 */
public final class ProgramState {

  /**
   * Exception thrown when aborting
   */
  public static class SlimAbortException extends RuntimeException {
    /**
     * Constructs the exception
     * @param message error message
     */
    public SlimAbortException(String message) {
      super(message);
    }
  }

  private ProgramState() {

  }

  private static volatile boolean sAbort = false;

  /**
   * Calls to {@link #checkAbort()} after this will throw exception
   */
  public static void setAbort() {
    sAbort = true;
  }

  /**
   * Calls to {@link #checkAbort()} after this will not throw exception
   */
  public static void clearAbort() {
    sAbort = false;
  }

  /**
   * @return true if the abort status has been set by the {@link #setAbort()} method. This method does not throw
   * a {@link SlimAbortException}.
   */
  public static boolean isAbort() {
    return sAbort;
  }

  /**
   * If abort status has been set by the {@link #setAbort()} method then we will throw
   * a {@link SlimAbortException} otherwise we return normally
   */
  public static void checkAbort() {
    if (sAbort) {
      final String message = "Aborting operation in thread: " + Thread.currentThread().getName();
      Diagnostic.userLog(message);
      throw new SlimAbortException(message);
    }
  }
}
