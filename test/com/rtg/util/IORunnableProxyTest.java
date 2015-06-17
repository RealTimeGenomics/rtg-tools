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

import java.io.EOFException;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Test class
 */
public class IORunnableProxyTest extends TestCase {

  public void testSomeMethod() throws Exception {
    final IORunnableProxy proxIO = new IORunnableProxy(new IORunnable() {
      @Override
      public void run() throws IOException {
        throw new EOFException("generic IO exception");
      }
    });
    Thread t = new Thread(proxIO);
    t.start();
    t.join();
    try {
      proxIO.checkError();
      fail();
    } catch (EOFException e) {
    }

    final IORunnableProxy proxRuntime = new IORunnableProxy(new IORunnable() {
      @Override
      public void run() {
        throw new IllegalArgumentException("gotcha");
      }
    });
    t = new Thread(proxRuntime);
    t.start();
    t.join();
    try {
      proxRuntime.checkError();
      fail();
    } catch (IllegalArgumentException e) {
    }

    final IORunnableProxy proxError = new IORunnableProxy(new IORunnable() {
      @Override
      public void run() {
        throw new OutOfMemoryError("not really");
      }
    });
    t = new Thread(proxError);
    t.start();
    t.join();
    try {
      proxError.checkError();
      fail();
    } catch (OutOfMemoryError e) {
    }

    final IORunnableProxy proxOk = new IORunnableProxy(new IORunnable() {
      @Override
      public void run() {
      }
    });
    t = new Thread(proxOk);
    t.start();
    t.join();
    proxOk.checkError();
  }

}
