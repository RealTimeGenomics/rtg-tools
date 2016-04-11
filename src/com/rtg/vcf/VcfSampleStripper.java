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
package com.rtg.vcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.VcfHeader;

/**
 * Removes unwanted samples from a VCF record
 */
public class VcfSampleStripper implements VcfAnnotator {

  private final boolean mRemoveAll;
  private final boolean mKeepMode;
  private final Set<String> mSamples;
  private int[] mSampleIdsToRemove = null;

  /**
   * Remove all samples from header and records
   * @param removeAll false if you don't actually want to do it for some reason.
   */
  VcfSampleStripper(boolean removeAll) {
    mRemoveAll = removeAll;
    mKeepMode = false;
    mSamples = null;
  }

  /**
   * Keep or remove a selected set of samples from header and records
   * @param sampleList the list of sample names
   * @param keep true to keep values in the list, false to remove them
   */
  public VcfSampleStripper(Set<String> sampleList, boolean keep) {
    mRemoveAll = false;
    mKeepMode = keep;
    mSamples = sampleList;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    if (mRemoveAll) {
      header.removeAllSamples();
    } else {
      final HashSet<String> samplesToRemove = new HashSet<>();
      for (String sample : header.getSampleNames()) {
        if (mKeepMode ^ mSamples.contains(sample)) {
          samplesToRemove.add(sample);
        }
      }

      mSampleIdsToRemove = new int[samplesToRemove.size()];
      int i = 0;
      for (String sample : samplesToRemove) {
        final Integer sampleId = header.getSampleIndex(sample);
        if (sampleId == null) {
          throw new NoTalkbackSlimException("Could not find sample name: " + sample + " in VCF header");
        }
        mSampleIdsToRemove[i] = sampleId;
        i++;
      }
      Arrays.sort(mSampleIdsToRemove); //this so that when we come to remove from the format sample lists,

      header.removeSamples(samplesToRemove);
    }
    if (header.getNumberOfSamples() == 0) {
      header.getFormatLines().clear();
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    if (mRemoveAll) {
      rec.getFormatAndSample().clear();
      rec.setNumberOfSamples(0);
      return;
    } else if (mSamples == null || mSamples.size() == 0) {
      return;
    }

    if (mSampleIdsToRemove == null) {
      throw new RuntimeException("Call updateHeader first.");
    }

    int newNumSamples = rec.getNumberOfSamples();
    boolean first = true;
    for (Map.Entry<String, ArrayList<String>> formatValue : rec.getFormatAndSample().entrySet()) {
      //remove each sample from each format
      for (int j = mSampleIdsToRemove.length - 1; j >= 0; j--) { //backwards to avoid changing index values as we remove items
        formatValue.getValue().remove(mSampleIdsToRemove[j]);
      }
      if (first) {
        newNumSamples = formatValue.getValue().size();
        first = false;
      }
    }
    rec.setNumberOfSamples(newNumSamples);
    if (newNumSamples == 0) {
      rec.getFormatAndSample().clear();
    }
  }
}
