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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * test class
 */
public class FalseSeekableStreamTest extends TestCase {

  private InputStream getStream() {
    final byte[] b = "012345678901234567890123456789".getBytes();
    return new ByteArrayInputStream(b);
  }

  public void testGetPosition() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.getPosition();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testLength() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.length();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testSeek() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.seek(5);
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testRead0args() throws Exception {
    try (InputStream basic = getStream()) {
      try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
        assertEquals(basic.read(), fss.read());

      }
    }
  }

  public void testAvailable() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.available();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testClose() throws Exception {
    try (InputStream basic = getStream()) {
      final FalseSeekableStream fss = new FalseSeekableStream(getStream());
      fss.close();
    }
  }

  public void testMark() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.mark(500);
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testMarkSupported() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.markSupported();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testReadbyteArr() throws Exception {
    try (InputStream basic = getStream()) {
      try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
        final byte[] first = new byte[10];
        final byte[] second = new byte[10];
        final int len1 = basic.read(first);
        final int len2 = fss.read(second);
        assertEquals(len1, len2);
        assertTrue(Arrays.equals(first, second));
      }
    }
  }

  public void testRead3args() throws Exception {
    try (InputStream basic = getStream()) {
      try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
        final byte[] first = new byte[10];
        final byte[] second = new byte[10];
        final int len1 = basic.read(first, 2, 8);
        final int len2 = fss.read(second, 2, 8);
        assertEquals(len1, len2);
        assertTrue(Arrays.equals(first, second));
      }
    }
  }

  public void testReset() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      fss.reset();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testSkip() throws IOException {
    try (FalseSeekableStream fss = new FalseSeekableStream(getStream())) {
      final long len = fss.skip(3);
      assertEquals(3, (int) len);
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

}
