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
 * Exception for reporting errors within SLIM.  In every case the exception
 * is logged via the diagnostics system.
 *
 */
public class SlimException extends RuntimeException {

  private final boolean mTalkback;
  private final ErrorType mErrorType;
  private final String[] mErrorArguments;

  /**
   * A new exception for an error where intelligent information can be
   * returned to the user via the diagnostics system and where an
   * underlying exception may contain further information useful for debuggers.
   *
   * @param talkback if true then invoke talkback mechanism.
   * @param t cause
   * @param type error type
   * @param args parameters of the error type
   */
  protected SlimException(final boolean talkback, final Throwable t, final ErrorType type, final String... args) {
    super(t != null ? t.toString() : args.length > 0 ? args[0] : "", t);  //TODO this is heinous and should be dealt to with extreme prejudice
    mTalkback = talkback;
    mErrorType = type;
    mErrorArguments = args.clone();
  }


  /**
   * A new exception for an error where intelligent information can be
   * returned to the user via the diagnostics system and where an
   * underlying exception may contain further information useful for debuggers.
   *
   * @param t cause
   * @param type error type
   * @param args parameters of the error type
   */
  public SlimException(final Throwable t, final ErrorType type, final String... args) {
    this(true, t, type, args);
  }

  /**
   * A new exception for an error where intelligent information can be
   * returned to the user via the diagnostics system.
   * A stack trace will be logged.
   *
   * @param type error type
   * @param args parameters of the error type
   */
  public SlimException(final ErrorType type, final String... args) {
    this(null, type, args);
  }

  /**
   * A new exception wrapping another throwable.
   * A stack trace will be logged.
   *
   * @param t cause
   */
  public SlimException(final Throwable t) {
    this(t, ErrorType.SLIM_ERROR);
  }

  /**
   * A new exception where no additional useful information is available.
   * A stack trace will be logged.
   */
  public SlimException() {
    this(ErrorType.SLIM_ERROR);
  }

  /**
   * An exception where we have an error message for the user.
   * @param message message for the error.
   */
  public SlimException(final String message) {
    this(ErrorType.INFO_ERROR, message);
  }

  /**
   * The error that caused this exception
   * @return the type
   */
  public ErrorType getErrorType() {
    return mErrorType;
  }

  /**
   * Log the error to the diagnostic log file.
   */
  public void logException() {
    Diagnostic.userLog(getCause() == null ? this : getCause());
    Diagnostic.errorLogOnly(mErrorType, mErrorArguments);
  }

  /**
   * Print the user viewable error without logging.
   */
  public void printErrorNoLog() {
    Diagnostic.errorNoLog(mErrorType, mErrorArguments);
  }

  /**
   * Invoke the talkback mechanism if this was a talkback exception.
   */
  public void invokeTalkback() {
    if (mTalkback) {
      Talkback.postTalkback(getCause() == null ? this : getCause());
    }
  }
}

