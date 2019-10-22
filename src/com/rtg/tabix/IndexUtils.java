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
package com.rtg.tabix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.util.BlockCompressedOutputStream;

/**
 * Utility methods for indexes
 */
public final class IndexUtils {

  private IndexUtils() { }


  /**
   * Turns a file into a block compressed file. This is a hack, do not use in production.
   * @param f file to ensure block compressed.
   * @return file that is block compressed
   * @throws java.io.IOException if an IO error occurs
   */
  public static File ensureBlockCompressed(File f) throws IOException {
    File ret = f;
    if (!TabixIndexer.isBlockCompressed(f)) {
      Diagnostic.info("Block compressing file: " + f.getPath());
      final File outFile = File.createTempFile(f.getName(), "", f.getAbsoluteFile().getParentFile());
      try (InputStream is = FileUtils.createInputStream(f, false)) {
        try (BlockCompressedOutputStream bcos = new BlockCompressedOutputStream(outFile)) {
          FileUtils.streamToStream(is, bcos, 2048);
        }
      }
      final File mvFile;
      if (!FileUtils.isGzipFilename(f)) {
        mvFile = new File(f.getParentFile(), f.getName() + FileUtils.GZ_SUFFIX);
        if (!f.delete()) {
          Diagnostic.warning("Failed to remove: " + f.getPath());
        }
        ret = mvFile;
      } else {
        mvFile = f;
      }
      if ((!mvFile.exists() || mvFile.delete()) && !outFile.renameTo(mvFile)) {
        Diagnostic.warning("Failed to rename temporary file: " + outFile.getPath() + " to: " + mvFile.getPath());
      }
    }
    return ret;
  }

  /**
   * Turns a list of files into a list of block compressed files. This is a hack, do not use in production.
   * @param files collection of files to ensure block compressed.
   * @return list of files that is block compressed
   * @throws java.io.IOException if an IO error occurs
   */
  public static List<File> ensureBlockCompressed(Collection<File> files) throws IOException {
    final ArrayList<File> toAdd = new ArrayList<>();
    for (File f : files) {
      toAdd.add(ensureBlockCompressed(f));
    }
    return toAdd;
  }
}
