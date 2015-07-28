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

package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.rtg.launcher.CommonFlags;
import com.rtg.tabix.TabixIndexReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.ContigField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Only load records when asked for
 */
class TabixVcfRecordSet implements VariantSet {

  private final File mBaselineFile;
  private final File mCallsFile;
  private final String mBaselineSampleName;
  private final String mCallsSampleName;
  private final Collection<Pair<String, Integer>> mNames = new ArrayList<>();
  private final ReferenceRanges<String> mRanges;
  private final VcfHeader mBaseLineHeader;
  private final VcfHeader mCalledHeader;
  private final boolean mPassOnly;
  private final boolean mSquashPloidy;
  private final int mMaxLength;
  private final RocSortValueExtractor mExtractor;

  private int mBaselineSkipped;
  private int mCallsSkipped;

  TabixVcfRecordSet(File baselineFile, File calledFile,
                    ReferenceRanges<String> ranges, Collection<Pair<String, Integer>> referenceNameOrdering,
                    String baselineSample, String callsSample,
                    RocSortValueExtractor extractor, boolean passOnly, boolean squashPloidy, int maxLength) throws IOException {
    if (referenceNameOrdering == null) {
      throw new NullPointerException();
    }
    mBaselineFile = baselineFile;
    mCallsFile = calledFile;
    mRanges = ranges;
    mBaselineSampleName = baselineSample;
    mCallsSampleName = callsSample;
    mBaseLineHeader = VcfUtils.getHeader(baselineFile);
    mCalledHeader = VcfUtils.getHeader(calledFile);
    mPassOnly = passOnly;
    mSquashPloidy = squashPloidy;
    mExtractor = extractor;
    mMaxLength = maxLength;

    final Set<String> basenames = new TreeSet<>();
    Collections.addAll(basenames, new TabixIndexReader(TabixIndexer.indexFileName(baselineFile)).sequenceNames());
    final Set<String> callnames = new TreeSet<>();
    Collections.addAll(callnames, new TabixIndexReader(TabixIndexer.indexFileName(calledFile)).sequenceNames());

    final Set<String> basedeclarednames = new TreeSet<>(basenames);
    for (ContigField c : mBaseLineHeader.getContigLines()) {
      basedeclarednames.add(c.getId());
    }
    final Set<String> calldeclarednames = new TreeSet<>(callnames);
    for (ContigField c : mCalledHeader.getContigLines()) {
      calldeclarednames.add(c.getId());
    }
    if (Collections.disjoint(basedeclarednames, calldeclarednames)) {
      throw new NoTalkbackSlimException("There were no sequence names in common between the supplied baseline and called variant sets. Check they use the same reference and are non-empty.");
    }

    final Set<String> union = new HashSet<>(basenames);
    union.addAll(callnames);
    if (disjoint(union, referenceNameOrdering)) {
      throw new NoTalkbackSlimException("There were no sequence names in common between the reference and the supplied variant sets."
        + " Check the correct reference is being used.");
    }
    final Set<String> referenceNames = new HashSet<>();
    for (Pair<String, Integer> orderedNameLength : referenceNameOrdering) {
      final String name = orderedNameLength.getA();
      if (ranges.allAvailable() || ranges.containsSequence(name)) {
        referenceNames.add(name);
        if (basenames.contains(name)) {
          if (callnames.contains(name)) {
            mNames.add(orderedNameLength);
          } else {
            mNames.add(orderedNameLength);
            Diagnostic.warning("Reference sequence " + name + " is declared in baseline but not declared in calls (variants will be treated as FN).");
          }
        } else {
          if (callnames.contains(name)) {
            mNames.add(orderedNameLength);
            Diagnostic.warning("Reference sequence " + name + " is declared in calls but not declared in baseline (variants will be treated as FP).");
          } else {
            Diagnostic.userLog("Skipping reference sequence " + name + " that is used by neither baseline or calls.");
          }
        }
      }
    }
    if (ranges.allAvailable()) {
      for (String name : basenames) {
        if (!referenceNames.contains(name)) {
          Diagnostic.warning("Baseline variants for sequence " + name + " will be ignored as this sequence is not contained in the reference.");
        }
      }
      for (String name : callnames) {
        if (!referenceNames.contains(name)) {
          Diagnostic.warning("Call set variants for sequence " + name + " will be ignored as this sequence is not contained in the reference.");
        }
      }
    } else {
      if (mNames.size() == 0) {
        throw new NoTalkbackSlimException("After applying regions there were no sequence names in common between the reference and the supplied variant sets."
          + " Check the regions supplied by --" + CommonFlags.RESTRICTION_FLAG + " or --" + CommonFlags.BED_REGIONS_FLAG + " are correct.");
      }
    }
  }

  private boolean disjoint(Set<String> names, Collection<Pair<String, Integer>> referenceNameOrdering) {
    for (Pair<String, Integer> pair : referenceNameOrdering) {
      if (names.contains(pair.getA())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Pair<String, Map<VariantSetType, List<DetectedVariant>>> nextSet() {
    final Map<VariantSetType, List<DetectedVariant>> map = new HashMap<>();
    final Iterator<Pair<String, Integer>> iterator = mNames.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    final Pair<String, Integer> nameLength = iterator.next();
    mNames.remove(nameLength);
    final String currentName = nameLength.getA();
    final int currentLength = nameLength.getB();
    final ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      final ReferenceRanges<String> subRanges = mRanges.forSequence(currentName);
      final FutureTask<LoadedVariants> baseFuture = new FutureTask<>(new VcfRecordTabixCallable(mBaselineFile, subRanges, currentName, currentLength, VariantSetType.BASELINE, mBaselineSampleName, mExtractor, mPassOnly, mSquashPloidy, mMaxLength));
      final FutureTask<LoadedVariants> callFuture = new FutureTask<>(new VcfRecordTabixCallable(mCallsFile, subRanges, currentName, currentLength, VariantSetType.CALLS, mCallsSampleName, mExtractor, mPassOnly, mSquashPloidy, mMaxLength));
      executor.execute(baseFuture);
      executor.execute(callFuture);
      final LoadedVariants baseVars = baseFuture.get();
      final LoadedVariants calledVars = callFuture.get();
      map.put(VariantSetType.BASELINE, baseVars.mVariants);
      map.put(VariantSetType.CALLS, calledVars.mVariants);
      mBaselineSkipped += baseVars.mSkippedDuringLoading;
      mCallsSkipped += calledVars.mSkippedDuringLoading;
      Diagnostic.userLog("Reference " + currentName + " baseline contains " + map.get(VariantSetType.BASELINE).size() + " variants.");
      Diagnostic.userLog("Reference " + currentName + " calls contains " + map.get(VariantSetType.CALLS).size() + " variants.");
    } catch (final ExecutionException e) {
      throw new NoTalkbackSlimException(e.getCause(), ErrorType.INFO_ERROR, e.getCause().getMessage());
    } catch (final InterruptedException e) {
      throw new NoTalkbackSlimException(e, ErrorType.INFO_ERROR, e.getCause().getMessage());
    } finally {
      executor.shutdownNow();
    }
    return new Pair<>(currentName, map);
  }

  @Override
  public VcfHeader baseLineHeader() {
    return mBaseLineHeader;
  }

  @Override
  public VcfHeader calledHeader() {
    return mCalledHeader;
  }

  @Override
  public int getNumberOfSkippedBaselineVariants() {
    return mBaselineSkipped;
  }

  @Override
  public int getNumberOfSkippedCalledVariants() {
    return mCallsSkipped;
  }

}

