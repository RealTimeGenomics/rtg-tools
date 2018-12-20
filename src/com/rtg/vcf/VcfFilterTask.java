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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.vcf.VcfFilterStatistics.Stat;
import com.rtg.vcf.header.FilterField;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * This is what does the business of filtering for <code>vcffilter</code>. It's been quite brutally pulled out of
 * <code>VcfFilterCli</code> so the initialization is quite hideous.
 */
@TestClass("com.rtg.vcf.VcfFilterCliTest")
class VcfFilterTask {

  protected final VcfFilterStatistics mVcfFilterStatistics = new VcfFilterStatistics();

  // overlap filtering
  protected String mEndRef = "";
  protected int mEndPosition = -1;

  // what to filter on
  protected List<VcfFilter> mFilters = new ArrayList<>();
  protected List<String> mSampleNames = new ArrayList<>();
  protected int[] mSampleIndexes = null;
  protected boolean[] mSampleFailed = null;
  protected boolean mNonSampleSpecificFailed = false;
  protected boolean mRemoveHom;
  protected boolean mRemoveSameAsRef;
  protected boolean mRemoveAllSameAsRef;
  protected boolean mSnpsOnly;
  protected boolean mNonSnpsOnly;
  protected boolean mCheckingSample;
  protected boolean mAllSamples;
  protected ReferenceRegions mIncludeBed = null;
  protected ReferenceRegions mExcludeBed = null;

  protected final Set<String> mKeepInfos = new HashSet<>();
  protected final Set<String> mRemoveInfos = new HashSet<>();
  protected final Set<String> mKeepFilters = new HashSet<>();
  protected final Set<String> mRemoveFilters = new HashSet<>();

  // following used during filtering pipeline
  protected final Set<String> mFilterTags = new TreeSet<>(); // valid filter tags - input file specific
  protected final Set<String> mInfoTags = new TreeSet<>(); // valid info tags - input file specific
  protected Integer mDensityWindow = null;
  protected VcfRecord mPrevRecord = null;
  protected boolean mPrevDense = false;
  protected boolean mRemoveOverlapping;
  protected boolean mResetFailedSampleGts = false;
  protected String mFailFilterName = null;
  protected String mFailSampleFilterName = null;


  void setHeader(VcfHeader header) {
    checkHeaderFieldFilters(header);
    for (final VcfFilter filter : mFilters) {
      if (filter instanceof VcfSampleFilter) {
        ((VcfSampleFilter) filter).setSamples(mSampleIndexes, mSampleFailed);
      }
      filter.setHeader(header);
    }
    if (mFailFilterName != null) {
      header.ensureContains(new FilterField(mFailFilterName, "RTG vcffilter user defined filter"));
    }
    if (mFailSampleFilterName != null) {
      header.ensureContains(new FilterField(mFailSampleFilterName, "RTG vcffilter user defined filter"));
      header.ensureContains(new FormatField(VcfUtils.FORMAT_FILTER, MetaType.STRING, VcfNumber.ONE, "Sample-specific filter status"));
    }
  }

  protected void printStatistics(OutputStream outStream) {
    mVcfFilterStatistics.printStatistics(outStream);
  }

  // check user specified info/filter fields are valid
  protected void checkHeaderFieldFilters(VcfHeader header) {
    if (mCheckingSample || mResetFailedSampleGts || mFailSampleFilterName != null) {
      mSampleFailed = new boolean[header.getNumberOfSamples()];
      if (mAllSamples) {
        mSampleIndexes = new int[header.getNumberOfSamples()];
        for (int i = 0; i < mSampleIndexes.length; ++i) {
          mSampleIndexes[i] = i;
        }
      } else if (mSampleNames.isEmpty()) {
        if (header.getNumberOfSamples() > 0) {
          mSampleIndexes = new int[1];
          mSampleIndexes[0] = VcfUtils.getSampleIndexOrDie(header, null, "input");
        } else {
          mSampleIndexes = new int[0];
        }
      } else {
        mSampleIndexes = new int[mSampleNames.size()];
        int i = 0;
        for (final String sample : mSampleNames) {
          mSampleIndexes[i++] = VcfUtils.getSampleIndexOrDie(header, sample, "input");
        }
      }
    }
    if (!mKeepFilters.isEmpty() || !mKeepInfos.isEmpty() || !mRemoveFilters.isEmpty() || !mRemoveInfos.isEmpty()) {
      mFilterTags.add(VcfUtils.FILTER_PASS);
      mFilterTags.add(VcfUtils.MISSING_FIELD);
      for (final FilterField info : header.getFilterLines()) {
        mFilterTags.add(info.getId());
      }
      final Set<String> userFilterTags = new TreeSet<>();
      userFilterTags.addAll(mKeepFilters);
      userFilterTags.addAll(mRemoveFilters);
      for (final String tag : userFilterTags) {
        if (!mFilterTags.contains(tag)) {
          throw new NoTalkbackSlimException("Invalid FIELD tag: " + tag + " : " + mFilterTags);
        }
      }
      for (final InfoField info : header.getInfoLines()) {
        mInfoTags.add(info.getId());
      }
      final Set<String> userInfoTags = new TreeSet<>();
      userInfoTags.addAll(mKeepInfos);
      userInfoTags.addAll(mRemoveInfos);
      for (final String tag : userInfoTags) {
        if (!mInfoTags.contains(tag)) {
          throw new VcfFormatException("Invalid INFO tag: " + tag + " : " + mInfoTags);
        }
      }
    }
  }


  void process(final VcfReader r, final VcfWriter w) throws IOException {
    mPrevDense = false;
    mPrevRecord = null;
    try {
      while (r.hasNext()) {
        final VcfRecord record = r.next();
        if (accept(record)) {
          write(w, record);
        } else if (mFailFilterName != null) {
          record.addFilter(mFailFilterName);
          write(w, record);
        } else if (mResetFailedSampleGts || mFailSampleFilterName != null) {
          filterSamples(record);
          write(w, record);
        }
      }
    } catch (final VcfFormatException iae) {
      throw new NoTalkbackSlimException(iae.getMessage());
    }
    flush(w);
  }

  // Any samples that failed during the sample-specific filtering (or those failing non-sample-specific filtering)
  // will either have their GT set to missing value or FT field set.
  void filterSamples(VcfRecord record) {
    final List<String> sampleGts = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
    if (mFailSampleFilterName == null && sampleGts == null) {
      throw new VcfFormatException("Record does not contain " + VcfUtils.FORMAT_GENOTYPE + " field:\n" + record);
    }
    if (mNonSampleSpecificFailed) {
      if (mSampleIndexes != null) {
        for (final int sampleIndex : mSampleIndexes) {
          if (mFailSampleFilterName != null) {
            record.addSampleFilter(mFailSampleFilterName, sampleIndex);
          } else {
            sampleGts.set(sampleIndex, VcfRecord.MISSING);
          }
        }
      } else { // When running on a multiple sample VCF we must specify a sample or samples so this is only possible on a single sample VCF
        assert mFailSampleFilterName == null;
        sampleGts.set(0, VcfRecord.MISSING);
      }
    } else if (mCheckingSample) {
      for (final int sampleIndex : mSampleIndexes) {
        if (mSampleFailed[sampleIndex]) {
          if (mFailSampleFilterName != null) {
            record.addSampleFilter(mFailSampleFilterName, sampleIndex);
          } else {
            sampleGts.set(sampleIndex, VcfRecord.MISSING);
          }
        }
      }
    }
  }

  /**
   * Main filtering function...
   * @param record the record to apply filters to
   * @return false if the record failed filters
   */
  boolean accept(VcfRecord record) {
    mVcfFilterStatistics.increment(Stat.TOTAL_COUNT);
    if (mSampleFailed != null) {
      Arrays.fill(mSampleFailed, false);
    }
    mNonSampleSpecificFailed = false;
    //overlap detection
    if (mRemoveOverlapping && record.getSequenceName().equals(mEndRef) && record.getStart() < mEndPosition) {
      mVcfFilterStatistics.increment(Stat.OVERLAP_COUNT);
      mNonSampleSpecificFailed = true;
      return false;
    }
    mEndRef = record.getSequenceName();
    mEndPosition = record.getEnd();
    // check filter/infos to keep - they have precedence
    boolean keep = (mKeepInfos.size() + mKeepFilters.size()) == 0; // only set if no keep flags set
    for (final String tag : record.getInfo().keySet()) {
      if (mKeepInfos.contains(tag)) {
        keep = true;
      }
    }
    if (record.getFilters().isEmpty()) {
      if (mKeepFilters.contains(VcfUtils.MISSING_FIELD)) {
        keep = true;
      }
    } else {
      for (final String tag : record.getFilters()) {
        if (mKeepFilters.contains(tag)) {
          keep = true;
        }
      }
    }
    if (!keep) {
      mVcfFilterStatistics.increment(Stat.FAILED_KEEP_COUNT);
      mNonSampleSpecificFailed = true;
      return false;
    }
    for (final String tag : record.getInfo().keySet()) {
      if (mRemoveInfos.contains(tag)) {
        mVcfFilterStatistics.incrementInfoTag(tag);
        mNonSampleSpecificFailed = true;
        return false;
      }
    }
    if (record.getFilters().isEmpty()) {
      if (mRemoveFilters.contains(VcfUtils.MISSING_FIELD)) {
        mVcfFilterStatistics.incrementFilterTag(VcfUtils.MISSING_FIELD);
        mNonSampleSpecificFailed = true;
        return false;
      }
    } else {
      for (final String tag : record.getFilters()) {
        if (mRemoveFilters.contains(tag)) {
          mVcfFilterStatistics.incrementFilterTag(tag);
          mNonSampleSpecificFailed = true;
          return false;
        }
      }
    }
    if (allSameAsRef(record)) {
      return false;
    }
    // Sample specific
    if (mCheckingSample) {
      boolean acceptGt = true;
      for (final int sampleIndex : mSampleIndexes) {
        if (!acceptGtSpecific(record, sampleIndex)) {
          acceptGt = false;
          if (mSampleFailed != null) {
            mSampleFailed[sampleIndex] = true;
          } else {
            break;
          }
        }
      }
      if (!acceptGt) {
        return false;
      }
    }
    if (mExcludeBed != null && mExcludeBed.enclosed(record)) {
      mVcfFilterStatistics.increment(Stat.EXCLUDE_BED_COUNT);
      mNonSampleSpecificFailed = true;
      return false;
    }
    if (mIncludeBed != null && !mIncludeBed.overlapped(record)) {
      mVcfFilterStatistics.increment(Stat.INCLUDE_BED_COUNT);
      mNonSampleSpecificFailed = true;
      return false;
    }
    for (final VcfFilter filter : mFilters) {
      if (!filter.accept(record)) {
        if (!(filter instanceof VcfSampleFilter)) {
          mNonSampleSpecificFailed = true;
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Writes any remaining SNP lines
   * @param writer the destination
   * @throws java.io.IOException if there is an I/O problem
   */
  void flush(VcfWriter writer) throws IOException {
    if (mPrevRecord != null) {
      if (!mPrevDense) {
        writeCount(writer, mPrevRecord);
      } else {
        mVcfFilterStatistics.increment(Stat.DENSITY_WINDOW_COUNT);
      }
    }
    mPrevRecord = null;
    mPrevDense = false;
  }

  void write(VcfWriter w, VcfRecord record) throws IOException {
    // if all good write the line out
    if (mPrevRecord != null) {
      if (mDensityWindow == null) {
        writeCount(w, mPrevRecord);
      } else {
        final boolean currentDense = record.getSequenceName().equals(mPrevRecord.getSequenceName()) && (getCorrectedPos(record) - getCorrectedPos(mPrevRecord)) <= mDensityWindow;
        if (mPrevDense || currentDense) {  //the previous record was already too dense, or this record makes the previous record too dense.
          mVcfFilterStatistics.increment(Stat.DENSITY_WINDOW_COUNT);
        } else {
          writeCount(w, mPrevRecord);
        }
        mPrevDense = currentDense;
      }
    }
    mPrevRecord = record;
  }

  boolean allSameAsRef(VcfRecord record) {
    if (mRemoveAllSameAsRef) {
      for (int sampleIndex = 0; sampleIndex < record.getNumberOfSamples(); ++sampleIndex) {
        final ArrayList<String> sampleGts = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
        if (sampleGts == null) {
          throw new VcfFormatException("Specified filters require " + VcfUtils.FORMAT_GENOTYPE + " but no such field contained in record:\n" + record);
        }
        final String gt = sampleGts.get(sampleIndex);
        if (VcfUtils.isVariantGt(gt)) {
          return false;
        }
      }
      mVcfFilterStatistics.increment(Stat.ALL_SAME_AS_REF_FILTERED_COUNT);
      return true;
    }
    return false;
  }

  boolean acceptGtSpecific(VcfRecord record, int sampleIndex) {
    // Short circuit this if none of these filters enabled
    if (!mSnpsOnly && !mNonSnpsOnly && !mRemoveSameAsRef && !mRemoveHom) {
      return true;
    }
    final ArrayList<String> sampleGts = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
    if (sampleGts == null) {
      throw new VcfFormatException("Specified filters require " + VcfUtils.FORMAT_GENOTYPE + " but no such field contained in record:\n" + record);
    }
    final int[] altIndexes = VcfUtils.splitGt(sampleGts.get(sampleIndex));
    boolean refAlleleSeen = false;
    boolean allelesSame = true;
    boolean multiNucleotideCall = record.getRefCall().length() > 1;
    for (final int index : altIndexes) {
      if (index <= 0) {
        refAlleleSeen = true;
      } else {
        final List<String> altCalls = record.getAltCalls();
        if (index <= altCalls.size() && altCalls.get(index - 1).length() > 1) {
          multiNucleotideCall = true;
        }
      }
      if (index != altIndexes[0]) {
        allelesSame = false;
      }
    }

    if (allelesSame) {
      if (mRemoveSameAsRef && refAlleleSeen) {
        mVcfFilterStatistics.increment(Stat.SAME_AS_REF_FILTERED_COUNT);
        return false;
      }
      if (mRemoveHom) {
        mVcfFilterStatistics.increment(Stat.HOM_FILTERED_COUNT);
        return false;
      }
    }
    if (multiNucleotideCall) {
      if (mSnpsOnly) {
        mVcfFilterStatistics.increment(Stat.NOT_SNP_COUNT);
        return false;
      }
    } else {
      if (mNonSnpsOnly) {
        mVcfFilterStatistics.increment(Stat.SNP_COUNT);
        return false;
      }
    }
    return true;
  }

  int getCorrectedPos(VcfRecord record) {
    return VcfUtils.hasRedundantFirstNucleotide(record) ? record.getStart() + 1 : record.getStart();
  }

  void writeCount(VcfWriter w, VcfRecord record) throws IOException {
    w.write(record);
    mVcfFilterStatistics.increment(Stat.WRITTEN_COUNT);
  }

}
