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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.relation.Family;
import com.rtg.relation.PedigreeException;
import com.rtg.relation.VcfPedigreeParser;
import com.rtg.tabix.TabixIndexReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.vcf.ArrayVcfIterator;
import com.rtg.vcf.DecomposingVcfIterator;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfSortRefiner;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.ContigField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Only load records when asked for
 */
class TabixVcfRecordSet implements VariantSet {

  private final File mBaselineFile;
  private final File mCallsFile;
  private final Queue<Pair<String, Integer>> mNames = new LinkedList<>();
  private final ReferenceRanges<String> mRanges;
  private final ReferenceRegions mEvalRegions;
  private final VcfHeader mBaseLineHeader;
  private final VcfHeader mCalledHeader;
  private final int mBaselineSampleNo; // Index of baseline sample within header, or -1 if not selecting a sample
  private final int mCalledSampleNo; // Index of call sample within header, or -1 if not selecting a sample
  private final VariantFactory mBaselineFactory;
  private final VariantFactory mCallsFactory;
  private final Map<String, File> mBaselinePreprocessed = new ConcurrentHashMap<>();
  private final Map<String, File> mCallsPreprocessed = new ConcurrentHashMap<>();
  private final boolean mPassOnly;
  private final int mMaxLength;
  private final File mPreprocessDestDir;
  private final boolean mPreprocess;
  private final boolean mRelaxedRef;

  private int mBaselineSkipped;
  private int mCallsSkipped;

  TabixVcfRecordSet(File baselineFile, File calledFile,
                    ReferenceRanges<String> ranges, ReferenceRegions evalRegions,
                    Collection<Pair<String, Integer>> referenceNameOrdering,
                    String baselineSample, String callsSample,
                    boolean passOnly, boolean relaxedRef, int maxLength, File preprocessDestDir) throws IOException {
    if (referenceNameOrdering == null) {
      throw new NullPointerException();
    }
    if (preprocessDestDir != null && !preprocessDestDir.isDirectory()) {
      throw new IllegalArgumentException();
    }
    mBaselineFile = baselineFile;
    mCallsFile = calledFile;
    final VcfHeader baselineHeader = VcfUtils.getHeader(baselineFile);
    final VcfHeader calledHeader = VcfUtils.getHeader(calledFile);
    mRanges = ranges;
    mEvalRegions = evalRegions;
    mPassOnly = passOnly;
    mMaxLength = maxLength;
    mPreprocessDestDir = preprocessDestDir;
    mPreprocess = mPreprocessDestDir != null;
    mRelaxedRef = relaxedRef;

    final Set<String> basenames = new TreeSet<>();
    Collections.addAll(basenames, new TabixIndexReader(TabixIndexer.indexFileName(baselineFile)).sequenceNames());
    final Set<String> callnames = new TreeSet<>();
    Collections.addAll(callnames, new TabixIndexReader(TabixIndexer.indexFileName(calledFile)).sequenceNames());

    final Set<String> basedeclarednames = new TreeSet<>(basenames);
    for (ContigField c : baselineHeader.getContigLines()) {
      basedeclarednames.add(c.getId());
    }
    final Set<String> calldeclarednames = new TreeSet<>(callnames);
    for (ContigField c : calledHeader.getContigLines()) {
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
            Diagnostic.warning("Reference sequence " + name + " is used in baseline but not in calls.");
          }
        } else {
          if (callnames.contains(name)) {
            mNames.add(orderedNameLength);
            Diagnostic.warning("Reference sequence " + name + " is used in calls but not in baseline.");
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
      if (mNames.isEmpty()) {
        throw new NoTalkbackSlimException("After applying regions there were no sequence names in common between the reference and the supplied variant sets."
          + " Check the regions supplied by --" + CommonFlags.RESTRICTION_FLAG + " or --" + CommonFlags.BED_REGIONS_FLAG + " are correct.");
      }
    }

    mBaseLineHeader = mPreprocess ? addDecompositionHeader(baselineHeader) : baselineHeader;
    mCalledHeader = mPreprocess ? addDecompositionHeader(calledHeader) : calledHeader;
    mBaselineFactory = getVariantFactory(VariantSetType.BASELINE, mBaseLineHeader, baselineSample);
    mCallsFactory = getVariantFactory(VariantSetType.CALLS, mCalledHeader, callsSample);
    mBaselineSampleNo = baselineSample != null ? mBaseLineHeader.getSampleIndex(baselineSample) : 0;
    mCalledSampleNo = callsSample != null ? mCalledHeader.getSampleIndex(callsSample) : 0;
  }

  // This is pretty ick, but needed to ensure the main vcfeval output headers contain appropriate declarations created during decomposition
  private VcfHeader addDecompositionHeader(VcfHeader baselineHeader) throws IOException {
    return new DecomposingVcfIterator(new ArrayVcfIterator(baselineHeader), null, false, false).getHeader();
  }

  static VariantFactory getVariantFactory(VariantSetType type, VcfHeader header, String sampleName) {
    final boolean explicitUnknown = GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_EXPLICIT_UNKNOWN_ALLELES);
    final String f = VariantFactory.getFactoryName(type, sampleName);
    switch (f) {
      case VariantFactory.SAMPLE_FACTORY:
        return new VariantFactory.SampleVariants(VcfUtils.getSampleIndexOrDie(header, sampleName, type.label()), explicitUnknown);
      case VariantFactory.ALL_FACTORY:
        return new VariantFactory.AllAlts(explicitUnknown);
      case ParentalVariant.Factory.NAME:
        final Family family;
        try {
          family = Family.getFamily(VcfPedigreeParser.load(header));
        } catch (PedigreeException e) {
          throw new NoTalkbackSlimException(e.getMessage());
        }
        if (type != VariantSetType.BASELINE) {
          throw new RuntimeException("Parents must be specified as the baseline side only");
        }
        return new ParentalVariant.Factory(VcfUtils.getSampleIndexOrDie(header, family.getFather(), type.label()), VcfUtils.getSampleIndexOrDie(header, family.getMother(), type.label()), explicitUnknown);
      default:
        throw new RuntimeException("Could not determine variant factory for " + f);
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
  public Pair<String, Map<VariantSetType, List<Variant>>> nextSet() throws IOException {
    if (mNames.isEmpty()) {
      return null;
    }
    final Pair<String, Integer> nameLength = mNames.remove();
    final String currentName = nameLength.getA();
    final int currentLength = nameLength.getB();
    final Map<VariantSetType, List<Variant>> map = new EnumMap<>(VariantSetType.class);
    final ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      final ReferenceRanges<String> subRanges = mRanges.forSequence(currentName);
      final FutureTask<LoadedVariants> baseFuture = new FutureTask<>(new VcfRecordTabixCallable(mBaselineFile, subRanges, mEvalRegions, currentName, currentLength, VariantSetType.BASELINE, mBaselineFactory, mPassOnly, mMaxLength, mPreprocessDestDir, mRelaxedRef));
      final FutureTask<LoadedVariants> callFuture = new FutureTask<>(new VcfRecordTabixCallable(mCallsFile, subRanges, mEvalRegions, currentName, currentLength, VariantSetType.CALLS, mCallsFactory, mPassOnly, mMaxLength, mPreprocessDestDir, mRelaxedRef));
      executor.execute(baseFuture);
      executor.execute(callFuture);
      final LoadedVariants baseVars = baseFuture.get();
      final LoadedVariants calledVars = callFuture.get();
      map.put(VariantSetType.BASELINE, baseVars.mVariants);
      map.put(VariantSetType.CALLS, calledVars.mVariants);
      if (mPreprocess) {
        Diagnostic.developerLog("Preprocessed VCF at " + baseVars.mPreprocessed);
        Diagnostic.developerLog("Preprocessed VCF at " + calledVars.mPreprocessed);
        mBaselinePreprocessed.put(currentName, baseVars.mPreprocessed);
        mCallsPreprocessed.put(currentName, calledVars.mPreprocessed);
      }
      mBaselineSkipped += baseVars.mSkippedDuringLoading;
      mCallsSkipped += calledVars.mSkippedDuringLoading;
      Diagnostic.userLog("Reference " + currentName + " baseline contains " + map.get(VariantSetType.BASELINE).size() + " variants.");
      Diagnostic.userLog("Reference " + currentName + " calls contains " + map.get(VariantSetType.CALLS).size() + " variants.");
    } catch (final ExecutionException e) {
      IOUtils.rethrow(e.getCause());
    } catch (final InterruptedException e) {
      throw new NoTalkbackSlimException(e, ErrorType.INFO_ERROR, e.getCause().getMessage());
    } finally {
      executor.shutdownNow();
    }
    return new Pair<>(currentName, map);
  }

  @Override
  public VcfHeader baselineHeader() {
    return mBaseLineHeader;
  }

  @Override
  public int baselineSample() {
    return mBaselineSampleNo;
  }

  @Override
  public VcfIterator getBaselineVariants(String sequenceName) throws IOException {
    return new VcfSortRefiner(mPreprocess ? VcfReader.openVcfReader(mBaselinePreprocessed.get(sequenceName)) : VcfReader.openVcfReader(mBaselineFile, mRanges.forSequence(sequenceName)));
  }

  @Override
  public VcfHeader calledHeader() {
    return mCalledHeader;
  }

  @Override
  public int calledSample() {
    return mCalledSampleNo;
  }

  @Override
  public VcfIterator getCalledVariants(String sequenceName) throws IOException {
    return new VcfSortRefiner(mPreprocess ? VcfReader.openVcfReader(mCallsPreprocessed.get(sequenceName)) : VcfReader.openVcfReader(mCallsFile, mRanges.forSequence(sequenceName)));
  }

  @Override
  public int getNumberOfSkippedBaselineVariants() {
    return mBaselineSkipped;
  }

  @Override
  public int getNumberOfSkippedCalledVariants() {
    return mCallsSkipped;
  }

  @Override
  public void close() throws IOException {
    if (mPreprocessDestDir != null) { // Delete all the intermediate files we created
      final Collection<File> files = new HashSet<>();
      Stream.concat(mBaselinePreprocessed.values().stream(), mCallsPreprocessed.values().stream()).forEach(vcf -> {
        files.add(vcf);
        files.add(TabixIndexer.indexFileName(vcf));
      });
      for (File todelete : files) {
        if (todelete.exists() && !todelete.delete()) {
          throw new IOException("Could not delete intermediate file: " + todelete);
        }
      }

      if (FileUtils.isEmptyDir(mPreprocessDestDir)) { // Only do the directory delete if now empty
        if (!mPreprocessDestDir.delete()) {
          throw new IOException("Could not delete intermediate output dir: " + mPreprocessDestDir);
        }
      }
    }
  }

}

