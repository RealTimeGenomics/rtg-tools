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
package com.rtg.util.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 */
public class BufferedOutputStreamFixTest extends TestCase {

  private static final String FLUSH_MSG = "I don't like to flush";

  private class BadOutputStream extends OutputStream {

    boolean mBadstate = true;

    @Override
    public void write(int b) {
    }

    @Override
    public void flush() throws IOException {
      if (mBadstate) {
        mBadstate = false;
        throw new IOException(FLUSH_MSG);
      }
    }

    @Override
    public void close() throws IOException {
      throw new IOException("I don't like to close");
    }
  }

  public void test() {
    //Java7 contains this bug, and it appears to have been fixed in Java8.
    //first test if the bug still exists in the java being run
//    try {
//      try (final BufferedOutputStream bos = new BufferedOutputStream(new BadOutputStream())) {
//        bos.write("token".getBytes());
//      }
//    } catch (IOException e) {
//      if (FLUSH_MSG.equals(e.getMessage())) {
//        System.err.println("Seems java has now fixed their bug in FilterOutputStream");
//      }
//    }

    //then check we fixed it
    try {
      try (final BufferedOutputStream bos = new BufferedOutputStreamFix(new BadOutputStream())) {
        bos.write("token".getBytes());
      }
      fail("Should have thrown exception");
    } catch (IOException e) {
      assertEquals(FLUSH_MSG, e.getMessage());
    }
  }
}
