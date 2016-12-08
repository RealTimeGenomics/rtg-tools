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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.GZIPOutputStream;

import com.rtg.util.Resources;
import com.rtg.util.gzip.GzipUtils;
import com.rtg.util.io.FileUtils;

/**
 * File utility functions for use in unit tests. Temporary files and directories
 * are not automatically deleted, must be done by caller.
 *
 */
public final class FileHelper {
  /** Temporary file prefix */
  private static final String PREFIX = "unit";

  /** Temporary file suffix */
  private static final String SUFFIX = "test";

  /**
   * Private to prevent instantiation.
   */
  private FileHelper() {
  }

  /**
   * Creates an empty file in the unit test temporary-file directory,
   * automatically generating its name.
   *
   * @return an empty temporary <code>File</code>
   * @exception IOException if an error occurs.
   */
  public static File createTempFile() throws IOException {
    return File.createTempFile(PREFIX, SUFFIX);
  }

  /**
   * Creates an empty directory in the unit test temporary-file directory,
   * automatically generating its name.
   *
   * @return an empty temporary directory
   * @exception IOException if a file could not be created
   */
  public static File createTempDirectory() throws IOException {
    return FileUtils.createTempDir(PREFIX, SUFFIX);
  }

  /**
   * Creates an empty file in the given directory,
   * automatically generating its name.
   *
   * @param directory the directory to create the file in
   * @return an empty temporary <code>File</code>
   * @exception IOException if an error occurs.
   */
  public static File createTempFile(File directory) throws IOException {
    return File.createTempFile(PREFIX, SUFFIX, directory);
  }

  /**
   * Creates an empty directory in the given directory,
   * automatically generating its name.
   *
   * @param directory the directory to create the directory in
   * @return an empty temporary directory
   * @exception IOException if a file could not be created
   */
  public static File createTempDirectory(File directory) throws IOException {
    return FileUtils.createTempDir(PREFIX, SUFFIX, directory);
  }

  /**
   * Deletes the given <code>file</code>. If <code>file</code> is a directory then
   * that directory and its contents are all deleted.
   * Intended to be a last resort way of cleaning things up.
   * To that end it allows file to be null.
   *
   * @param file a file/directory to delete
   * @return true if operation entirely successful
   */
  public static boolean deleteAll(final File file) {
    boolean ok = true;
    if (file != null) {
      if (file.isDirectory()) {
        final File[] files = file.listFiles();
        if (files != null) {
          for (final File x : files) {
            ok &= deleteAll(x);
          }
        }
      }
      ok &= !file.exists() || file.delete();
    }
    return ok;
  }

  /**
   * Read the contents of a gzipped file and turn it into a string.
   * @param file gzipped file
   * @return string of contents
   * @throws IOException if an I/O error occurs
   */
  public static String gzFileToString(final File file) throws IOException {
    try (Reader reader = new InputStreamReader(GzipUtils.createGzipInputStream(new FileInputStream(file)))) {
      return FileHelper.readerToString(reader);
    }
  }

  /**
   * Gets the contents of the given resource as a string.
   *
   * @param resource name (classpath) of the resource.
   * @return a String containing the contents of the stream
   * @exception IOException if an error occurs.
   */
  public static String resourceToString(final String resource) throws IOException {
    final InputStream str = Resources.getResourceAsStream(resource);
    if (str == null) {
      throw new RuntimeException("Unable to find resource:" + resource);
    }
    final String res = FileUtils.streamToString(str);
    str.close();
    return res;
  }

  /**
   * Read the contents of a zip file and turn it into a string
   * @param file to be read
   * @return contents of file
   * @throws java.io.IOException if an IO error occurs
   */
  public static String zipFileToString(final File file) throws IOException {
    try (Reader reader = new InputStreamReader(GzipUtils.createGzipInputStream(new FileInputStream(file)))) {
      return FileHelper.readerToString(reader);
    }
  }

  /**
   * Read the contents of a file and turn it into a string.
   * @param file where to get the string.
   * @return the contents of the file.
   * @throws IOException if an I/O error occurs.
   */
  public static String fileToString(final File file) throws IOException {
    return readerToString(new FileReader(file));
  }

  /**
   * Read the contents of a reader and turn it into a string.
   * @param fileReader where to get the string.
   * @return the contents of the file.
   * @throws IOException if an I/O error occurs.
   */
  public static String readerToString(final Reader fileReader) throws IOException {
    final StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(fileReader)) {
      final char[] buffer = FileUtils.makeBuffer();
      final int eof = -1;
      for (int len = br.read(buffer); len > eof; len = br.read(buffer)) {
        for (int i = 0; i < len; ++i) {
          sb.append(buffer[i]);
        }
      }
    }
    return sb.toString();
  }

  /**
   * Creates a GZIP file copying the string <code>content</code> to it.
   *
   * @param content a <code>String</code>
   * @param file a non-null <code>File</code> to write to
   * @return a <code>File</code> containing the string content (same as <code>file</code>).
   * @exception IOException if an error occurs.
   * @exception NullPointerException if the content is null
   */
  public static File stringToGzFile(final String content, final File file) throws IOException {
    if (content == null) {
      throw new NullPointerException("null string given");
    }
    try (OutputStream out = FileUtils.createOutputStream(file, true, false, true)) {
      out.write(content.getBytes());
    }
    return file;
  }

  /**
   * Convenience method for turning a resource into a GZIP file that can be used as input.
   * @param resource name (classpath) of the resource.
   * @param file a <code>File</code> to write to
   * @return a <code>File</code> containing the string content (same as <code>file</code>).
   * @throws IOException if an error occurs
   */
  public static File resourceToGzFile(final String resource, final File file) throws IOException {
    try (InputStream stream = Resources.getResourceAsStream(resource)) {
      return FileHelper.streamToGzFile(stream, file);
    }
  }

  /**
   * Convenience method for turning a resource into a file that can be used as input.
   * @param resource name (classpath) of the resource.
   * @param file a <code>File</code> to write to
   * @return a <code>File</code> containing the string content (same as <code>file</code>).
   * @throws IOException if an error occurs
   */
  public static File resourceToFile(final String resource, final File file) throws IOException {
    try (InputStream stream = Resources.getResourceAsStream(resource)) {
      return FileHelper.streamToFile(stream, file);
    }
  }

  /**
   * Writes the contents of the given input <code>stream</code> to the given
   * gzipped file.
   *
   * @param stream an <code>InputStream</code>
   * @param file a <code>File</code> to write to
   * @return a <code>File</code> containing the contents of the stream
   * @exception IOException if an error occurs.
   * @exception NullPointerException if the stream is null
   */
  public static File streamToGzFile(final InputStream stream, final File file) throws IOException {
    if (stream == null) {
      throw new NullPointerException("null stream given");
    }
    if (file == null) {
      throw new NullPointerException("null file given");
    }
    try (OutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
      final byte[] b = new byte[FileUtils.BUFFER_SIZE];
      int len = stream.read(b);
      while (len > 0) {
        out.write(b, 0, len);
        len = stream.read(b);
      }
    }
    return file;
  }

  /**
   * Writes the contents of the given input <code>stream</code> to the given
   * file.
   *
   * @param stream an <code>InputStream</code>
   * @param file a <code>File</code> to write to
   * @return a <code>File</code> containing the contents of the stream
   * @exception IOException if an error occurs.
   * @exception NullPointerException if the stream is null
   */
  public static File streamToFile(final InputStream stream, final File file) throws IOException {
    if (stream == null) {
      throw new NullPointerException("null stream given");
    }
    if (file == null) {
      throw new NullPointerException("null file given");
    }
    try (FileOutputStream out = new FileOutputStream(file)) {
      final byte[] b = new byte[FileUtils.BUFFER_SIZE];
      int len = stream.read(b);
      while (len != -1) {
        out.write(b, 0, len);
        len = stream.read(b);
      }
    }
    return file;
  }

}
