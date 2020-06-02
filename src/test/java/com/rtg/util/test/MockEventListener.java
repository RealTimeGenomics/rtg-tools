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

package com.rtg.util.test;

import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorEvent;

import org.junit.Assert;

/**
 * Make it easy to test Diagnostic messages.
 *
 */
public final class MockEventListener implements DiagnosticListener {

  DiagnosticEvent<?> mEvent = null;

  @Override
  public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
    mEvent = event;
  }

  /**
   * Check that an error event was recieved with the supplied message.
   * @param expected the expected message
   * @return true if the message was recieved and contained the message.
   */
  public boolean compareErrorMessage(final String expected) {
    Assert.assertNotNull("Event is null", mEvent);
    boolean res;
    res = mEvent instanceof ErrorEvent;
    //System.err.println(mEvent.getMessage());
    res &= expected.equals(mEvent.getMessage());
    return res;
  }

  /**
   * @return the event recieved.
   */
  public DiagnosticEvent<?> getEvent() {
    return mEvent;
  }
  @Override
  public void close() {
  }
}
