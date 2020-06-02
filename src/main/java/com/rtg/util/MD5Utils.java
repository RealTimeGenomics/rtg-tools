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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 */
public final class MD5Utils {

  private MD5Utils() {
  }

  /**
   * Read all of the input stream and return it's MD5 digest.
   * @param in input stream to be read
   * @return MD5 digest.
   * @throws IOException when reading input stream.
   */
  public static String md5(final InputStream in) throws IOException {
    return md5(in, 1024);
  }

  /**
   * Read all of the input stream and return it's MD5 digest.
   * @param in input stream to be read
   * @param bufferLength length of buffer to read at a time
   * @return MD5 digest.
   * @throws IOException when reading input stream.
   */
  static String md5(final InputStream in, final int bufferLength) throws IOException {
    final byte[] buffer = new byte[bufferLength];
    try {
      final MessageDigest md = MessageDigest.getInstance("MD5");
      final DigestInputStream dis = new DigestInputStream(in, md);
      while (dis.read(buffer) != -1) { }
      final byte[] digest = md.digest();
      return digestToString(digest);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param digest MD5 digest as a byte array.
   * @return MD5 digest as a string.
   */
  public static String digestToString(final byte[] digest) {
    final StringBuilder sb = new StringBuilder();
    for (byte aDigest : digest) {
      sb.append(Integer.toHexString(0x100 + (aDigest & 0xFF)).substring(1));
    }
    return sb.toString();
  }

  /**
   * Returns the MD5 for the given <code>text</code>.
   *
   * @param text a <code>String</code>
   * @return MD5
   */
  public static String md5(final String text) {
    if (text == null) {
      throw new IllegalArgumentException("String was null");
    } else if (text.length() == 0) {
      throw new IllegalArgumentException("String was 0 length");
    }
    try {
      return md5(new ByteArrayInputStream(text.getBytes()));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);  //should never be able to happen, though
    }
  }
}
