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


import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.SlimException;

import htsjdk.samtools.util.RuntimeIOException;

/**
 * Access to io functionality.
 *
 */
public final class IOUtils {

  private IOUtils() { }

  /** Length of buffer to use during reading */
  private static final int BUFFER_LENGTH = 16384;

  private static final int EOF = -1;

  /**
   * Reads all the data from the supplied URL into a byte array.
   *
   * @param url the URL
   * @return a byte array containing the stream data.
   * @exception IOException if an error occurs during IO.
   */
  public static byte[] readData(final URL url) throws IOException {
    try (InputStream input = url.openStream()) {
      return readData(input);
    }
  }

  /**
   * Reads all the data from the supplied file into a byte array.
   *
   * @param input the file
   * @return a byte array containing the stream data.
   * @exception IOException if an error occurs during IO.
   */
  public static byte[] readData(File input) throws IOException {
    try (InputStream is = new FileInputStream(input)) {
      return readData(is);
    }
  }

  /**
   * Reads all the data from the supplied InputStream into a byte array.
   *
   * @param input the InputStream
   * @return a byte array containing the stream data.
   * @exception IOException if an error occurs during IO.
   */
  public static byte[] readData(final InputStream input) throws IOException {
    final byte[] inputBuffer = new byte[BUFFER_LENGTH];
    final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(BUFFER_LENGTH);
    int bytesRead;
    while ((bytesRead = input.read(inputBuffer)) != -1) {
      byteOutput.write(inputBuffer, 0, bytesRead);
    }
    final byte[] r = byteOutput.toByteArray();
    byteOutput.close();
    return r;
  }

  /**
   * Reads specified amount of data from stream or amount left in stream.
   * @param is stream to read from
   * @param buf buffer to read into
   * @param offset start position in buffer to read into
   * @param length amount of data to read in bytes
   * @return amount of data read
   * @throws IOException if an IO error occurs
   */
  public static int readAmount(InputStream is, byte[] buf, int offset, int length) throws IOException {
    int read = 0;
    int len;
    while (read < length && (len = is.read(buf, offset + read, length - read)) > 0) {
      read += len;
    }
    return read;
  }

  /**
   * Reads specified amount of data from stream.
   * @param is stream to read from
   * @param buf buffer to read into
   * @param offset start position in buffer to read into
   * @param length amount of data to read in bytes
   * @throws IOException If an IO error occurs
   */
  public static void readFully(InputStream is, byte[] buf, int offset, int length) throws IOException {
    final int read = readAmount(is, buf, offset, length);
    if (read < length) {
      throw new EOFException();
    }
  }

  /**
   * Read all of a URL into a String.
   *
   * @param url the URL to read.
   * @return a String containing the contents of the URL
   * @exception IOException If there is a problem during reading.
   */
  public static String readAll(final URL url) throws IOException {
    try (InputStream input = url.openStream()) {
      return readAll(input);
    }
  }


  /**
   * Read all of a File into a String.
   *
   * @param file the File to read.
   * @return a String containing the contents of the File
   * @exception IOException If there is a problem during reading.
   */
  public static String readAll(final File file) throws IOException {
    try (InputStream input = new FileInputStream(file)) {
      return readAll(input);
    }
  }


  /**
   * Read all of an input stream into a String.
   *
   * @param input input stream being read.
   * @return a String containing the contents of the input stream.
   * @exception IOException If there is a problem during reading.
   */
  public static String readAll(final InputStream input) throws IOException {
    return readAll(new InputStreamReader(input));
  }


  /**
   * Read all of an input stream into a String with the specified character encoding.
   *
   * @param input input stream being read.
   * @param encoding the character encoding string.
   * @return a String containing the contents of the input stream.
   * @exception IOException If there is a problem during reading.
   */
  public static String readAll(final InputStream input, final String encoding) throws IOException {
    return readAll(new InputStreamReader(input, encoding));
  }

  /**
   * Read all of a Reader into a String.
   *
   * @param input Reader being read.
   * @return a String containing the contents of the input stream.
   * @exception IOException If there is a problem during reading.
   */
  public static String readAll(final Reader input) throws IOException {
    final char[] b = new char[BUFFER_LENGTH];
    try (StringWriter str = new StringWriter(BUFFER_LENGTH)) {
      while (true) {
        final int length = input.read(b);
        if (length == EOF) {
          break;
        } else if (length == 0) {
          throw new RuntimeException();
        } else {
          str.write(b, 0, length);
        }
      }
      return str.toString();
    }
  }

  /**
   * Re-throw the underlying cause throwable obtained during execution in another thread.
   * @param cause the inner Throwable, typically an <code>ExecutionException.getCause()</code>
   * @throws IOException if the cause was an IOException
   */
  public static void rethrow(Throwable cause) throws IOException {
    if (cause instanceof IOException) {
      throw (IOException) cause;
    } else {
      rethrowWrapIO(cause);
    }
  }

  /**
   * Re-throw the underlying cause throwable obtained during execution in another thread.
   * This method wraps any underlying <code>IOException</code> in a <code>RuntimeIOException</code>
   * @param cause the inner Throwable, typically an <code>ExecutionException.getCause()</code>
   */
  public static void rethrowWrapIO(Throwable cause) {
    if (cause instanceof Error) {
      throw (Error) cause;
    } else if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    } else if (cause instanceof IOException) {
      throw new RuntimeIOException(cause);
    } else {
      throw new SlimException(cause, ErrorType.INFO_ERROR, cause.getMessage());
    }
  }
}
