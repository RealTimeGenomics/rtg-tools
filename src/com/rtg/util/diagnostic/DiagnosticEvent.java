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

import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Common implementation for event classes.
 *
 * @param <T> diagnostic type
 */
public class DiagnosticEvent<T extends DiagnosticType> {

  private final T mType;
  private final String[] mParams;

  /**
   * Construct a new diagnostic event, checking that the number of parameters
   * matches that required for the specified type.
   *
   * @param type type of diagnostic
   * @param params parameters of the diagnostic
   */
  DiagnosticEvent(final T type, final String... params) {
    if (type == null) {
      throw new NullPointerException();
    }
    if (params.length != type.getNumberOfParameters()) {
      throw new IllegalArgumentException(type + ":" + params.length + ":" + type.getNumberOfParameters());
    }
    mType = type;
    mParams = params;
  }

  /**
   * Get the type of the warning
   *
   * @return warning type
   */
  public T getType() {
    return mType;
  }

  /**
   * Return a copy of the parameters associated with this diagnostic event.
   * The result will not be null.
   *
   * @return diagnostic parameters
   */
  public String[] getParams() {
    return Arrays.copyOf(mParams, mParams.length);
  }

  /** Root name of diagnostics properties bundle. */
  private static final String DIAGNOSTIC_BUNDLE = "com.rtg.util.diagnostic.Diagnostics";
  static final ResourceBundle RESOURCE = ResourceBundle.getBundle(DIAGNOSTIC_BUNDLE, Locale.getDefault());

  /**
   * Return an internationalized text message for this diagnostic.  The generated
   * messages are backed by a <code>PropertyResourceBundle</code> with parameters
   * substituted into appropriate points in the message.  See the package level
   * documentation for further information.
   *
   * @return diagnostic message
   */
  public String getMessage() {
    final String key = getType().toString();
    try {
      String value = RESOURCE.getString(key);
      //
      //
      //

      // Substitute parameters into value string
      final String[] params = getParams();
      for (int k = 0; k < params.length; k++) {
        final int position = value.indexOf("%" + (k + 1));
        // In theory should always use all parameters, but just in case someone does
        // write an error message that does not use all the parameters, we make an
        // extra check here.
        if (position != -1) {
          value = value.substring(0, position) + params[k] + value.substring(position + 2);
        }
      }
      return mType.getMessagePrefix() + value;
    } catch (final MissingResourceException e) {
      Diagnostic.userLog("Missing resource information for diagnostic: " + key);
      return mType.getMessagePrefix() + key;
    }
  }
}

