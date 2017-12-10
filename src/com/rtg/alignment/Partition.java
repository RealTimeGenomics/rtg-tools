/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.alignment;

import java.util.ArrayList;
import java.util.List;

/**
 * A partition of a variant call.
 */
public class Partition extends ArrayList<Slice> {
  private static boolean isMnp(final String[] alleles) {
    final int length = alleles[0].length();
    if (length <= 1) {
      return false;
    }
    for (int k = 1; k < alleles.length; ++k) {
      if (alleles[k].length() != length) {
        return false;
      }
    }
    return true;
  }

  /**
   * Decompose MNPs into individual SNPs.
   * @param partition original partition
   * @return partitions with split MNPs
   */
  public static Partition breakMnps(final Partition partition) {
    final Partition retained = new Partition();
    for (final Slice slice : partition) {
      final String[] alleles = slice.getAlleles();
      if (isMnp(alleles)) {
        for (int s = 0; s < alleles[0].length(); ++s) {
          final String[] bases = new String[alleles.length];
          for (int k = 0; k < alleles.length; ++k) {
            bases[k] = alleles[k].substring(s, s + 1);
          }
          retained.add(new Slice(slice.getOffset() + s, bases));
        }
      } else {
        retained.add(slice);
      }
    }
    return retained;
  }

  /**
   * Remove the reference only parts of the partition.
   * @param partition initial partitioning
   * @return partition with reference pieces removed
   */
  public static Partition removeAllRef(final Partition partition) {
    final Partition retained = new Partition();
    for (final Slice slice : partition) {
      boolean allRef = true;
      final String[] alleles = slice.getAlleles();
      for (int k = 1; k < alleles.length; ++k) {
        allRef &= alleles[k].equals(alleles[0]);
      }
      if (!allRef) {
        retained.add(slice);
      }
    }
    return retained;
  }

  /**
   * Remove the reference only parts of each partition in the supplied list.
   * @param partition initial partitionings
   * @return partitions with reference pieces removed
   */
  public static List<Partition> removeAllRefList(final List<Partition> partition) {
    final List<Partition> retained = new ArrayList<>(partition.size());
    for (final Partition part : partition) {
      retained.add(removeAllRef(part));
    }
    return retained;
  }
}
