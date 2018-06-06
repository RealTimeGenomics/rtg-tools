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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Partition files into groups based on file length.
 */
public final class Partition {

  private Partition() { }

  private static class FileLengthComparator implements Comparator<File>, Serializable {
    @Override
    public int compare(final File f1, final File f2) {
      if (f1 == f2) {
        return 0;
      }
      return Long.compare(f2.length(), f1.length());
    }
  }

  private static final FileLengthComparator FILE_COMPARATOR = new FileLengthComparator();

  private static List<List<File>> binThem(final int binCount, final File... files) {
    // Greedy algorithm - sort by file length, start with biggest, put in emptiest bin
    Arrays.sort(files, FILE_COMPARATOR);
    final ArrayList<List<File>> bins = new ArrayList<>();
    for (int k = 0; k < binCount; ++k) {
      bins.add(new ArrayList<>());
    }
    final long[] usage = new long[binCount];

    // This could be made faster if we kept track of most empty bin some other way
    for (final File f : files) {
      int b = 0;
      long u = usage[0];
      for (int k = 1; k < binCount; ++k) {
        if (usage[k] < u) { //this results in 0 length files all getting put into the first empty bin, which MAY result in further empty bins remaining empty.
//        if (usage[k] <= u && bins.get(k).size() < bins.get(b).size()) { // <- as per above, this kind of thing would spread the empty files across empty bins.
          u = usage[k];
          b = k;
        }
      }
      usage[b] += f.length();
      bins.get(b).add(f);
    }

    return bins;
  }

  /**
   * Partition the given files into the specified bins trying to approximately
   * balance the size of the bins according to the lengths of the files.
   * Note that some bins may be empty, in the presence of 0 length files.
   *
   * @param binCount number of bins
   * @param files files to bin
   * @return <code>binCount</code> bins collectively containing all the files
   */
  static List<List<File>> partition(final int binCount, final File... files) {
    // Use our own sorted copy to avoid corrupting someone elses array
    final File[] sort = Arrays.copyOf(files, files.length);
    return binThem(binCount, sort);
  }

  /**
   * Partition the given files into the specified bins trying to approximately
   * balance the size of the bins according to the lengths of the files.
   * Note that some bins may be empty, in the presence of 0 length files.
   *
   * @param binCount number of bins
   * @param files files to bin
   * @return <code>binCount</code> bins collectively containing all the files
   */
  public static List<List<File>> partition(final int binCount, final Collection<File> files) {
    return binThem(binCount, files.toArray(new File[files.size()]));
  }
}
