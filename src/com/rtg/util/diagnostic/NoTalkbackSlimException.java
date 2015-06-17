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
package com.rtg.util.diagnostic;

/**
 * Exception for reporting errors within SLIM which are a result of local user
 * or machine error.  The exceptions are logged but are not reported by the talkback mechanism.
 * Being a subclass of
 * {@link SlimException} they allow the program to terminate with the
 * proper error code.
 *
 */
public class NoTalkbackSlimException extends SlimException {

  /**
   * A new exception for an error where intelligent information can be
   * returned to the user via the diagnostics system.
   * A stack trace will not be logged.
   *
   * @param type error type
   * @param args parameters of the error type
   */
  public NoTalkbackSlimException(final ErrorType type, final String... args) {
    this(null, type, args);
  }

  /**
   * A new exception for an error where intelligent information can be
   * returned to the user via the diagnostics system.
   * A stack trace will not be logged.
   *
   * @param t ignored
   * @param type error type
   * @param args parameters of the error type
   */
  public NoTalkbackSlimException(final Throwable t, final ErrorType type, final String... args) {
    super(false, t, type, args);
  }

  /**
   * Report program failure to user
   * @param message message for error
   */
  public NoTalkbackSlimException(String message) {
    this(null, ErrorType.INFO_ERROR, message);
  }
}
