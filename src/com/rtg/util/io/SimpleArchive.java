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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple archive format, doesn't support directories
 */
public final class SimpleArchive {

  private static final int VERSION = 1;
  private SimpleArchive() {
  }

  private static class FileHeader {
    final int mNameSize;
    final long mFileSize;

    FileHeader(int nameSize, long fileSize) {
      this.mNameSize = nameSize;
      this.mFileSize = fileSize;
    }
  }

  /**
   * extracts archive
   * @param archive archive file
   * @param outputDir destination dir
   * @throws IOException if an IO error occurs
   */
  public static void unpackArchive(File archive, File outputDir) throws IOException {
    try (DataInputStream input = new DataInputStream(new FileInputStream(archive))) {
      unpackArchivePrivate(input, outputDir);
    }
  }

  /**
   * extracts archive
   * @param input stream from archive file
   * @param outputDir destination dir
   * @throws IOException if an IO error occurs
   */
  public static void unpackArchive(InputStream input, File outputDir) throws IOException {
    final DataInputStream dInput = new DataInputStream(input);
    unpackArchivePrivate(dInput, outputDir);
  }

  /**
   * extracts archive
   * @param input stream from archive file
   * @param outputDir destination dir
   * @throws IOException if an IO error occurs
   */
  private static void unpackArchivePrivate(DataInputStream input, File outputDir) throws IOException {
    if (!outputDir.exists()) {
      if (!outputDir.mkdirs()) {
        throw new IllegalArgumentException("Could not create directory: " + outputDir.getPath());
      }
    }
    input.readInt(); //version
    final byte[] buf = new byte[4096];
    final int numFiles = input.readInt();
    for (int i = 0; i < numFiles; ++i) {
      final FileHeader header = new FileHeader(input.readInt(), input.readLong());
      final byte[] name = new byte[header.mNameSize];
      input.readFully(name);
      final File outFile = new File(outputDir, new String(name));
      try (FileOutputStream output = new FileOutputStream(outFile)) {
        long remaining = header.mFileSize;
        int toRead = buf.length < remaining ? buf.length : (int) remaining;
        int len;
        while (toRead > 0 && (len = input.read(buf, 0, toRead)) > 0) {
          output.write(buf, 0, len);
          remaining -= len;
          toRead = buf.length < remaining ? buf.length : (int) remaining;
        }
      }
    }
  }

  /**
   * Creates archive
   * @param dest file to contain other files
   * @param input input files
   * @throws IOException if an IO error occurs
   */
  public static void writeArchive(File dest, File... input) throws IOException {
    try (DataOutputStream output = new DataOutputStream(new FileOutputStream(dest))) {
      output.writeInt(VERSION);
      output.writeInt(input.length);
      final byte[] buf = new byte[4096];
      for (File f : input) {
        final byte[] nameBytes = f.getName().getBytes();
        final int nameSize = nameBytes.length;
        final FileHeader header = new FileHeader(nameSize, f.length());

        output.writeInt(header.mNameSize);
        output.writeLong(header.mFileSize);
        output.write(nameBytes);

        try (FileInputStream fis = new FileInputStream(f)) {
          int len;
          while ((len = fis.read(buf)) > 0) {
            output.write(buf, 0, len);
          }
        }
      }
    }
  }

  private static void printUsage() {
    System.out.println("Usage: ");
    System.out.println("SimpleArchive c archive files...");
    System.out.println("SimpleArchive d archive outputDir");
  }

  /**
   * Creates or unpacks archive
   * @param args arguments
   * @throws IOException if an io error occurs
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      printUsage();
      return;
    }
    if (args[0].equals("c")) {
      final File archive = new File(args[1]);
      final File[] input = new File[args.length - 2];
      for (int i = 0; i < input.length; ++i) {
        input[i] = new File(args[i + 2]);
      }
      writeArchive(archive, input);
    } else if (args[0].equals("d")) {
      final File archive = new File(args[1]);
      final File output = new File(args[2]);
      unpackArchive(archive, output);
    } else {
     printUsage();
    }
  }
}
