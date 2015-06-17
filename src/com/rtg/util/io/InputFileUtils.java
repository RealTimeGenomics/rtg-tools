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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for common checks on lists of input files.
 */
public final class InputFileUtils {

  private InputFileUtils() { }

  /**
   * Method to remove redundant canonical paths from a collection of files.
   * @param files collection of input file names to remove redundant canonical paths from.
   * @return collection of unique files, with first occurrence of a canonical path kept only.
   * @throws IOException if an IOException occurs
   */
  public static List<File> removeRedundantPaths(List<File> files) throws IOException {
    final List<File> out = new ArrayList<>();
    final Set<String> paths = new HashSet<>();
    for (File file : files) {
      if (paths.add(file.getCanonicalPath())) {
        out.add(file);
      }
    }
    return out;
  }

  /**
   * Method to check if two file objects represent the same canonical file.
   * @param f1 first file
   * @param f2 second file
   * @return true if the first and second file are the same canonical file.
   * @throws IOException if an IOException occurs
   */
  public static boolean checkIdenticalPaths(File f1, File f2) throws IOException {
    return f1.getCanonicalPath().equals(f2.getCanonicalPath());
  }
}
