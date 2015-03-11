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
package com.rtg.sam;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.rtg.reader.SequencesReader;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;

import htsjdk.samtools.SAMFileHeader;

/**
 * When we are about to read sam files, this object just stores the file list, the filter params and the uber header
 */
public class SamReadingContext {

  private final Collection<File> mFiles;
  private final SamFilterParams mParams;
  private final SAMFileHeader mHeader;
  private final int mNumThreads;

  private final SequencesReader mReference;
  private final ReferenceRanges<String> mReferenceRanges;

  /**
   * Create the reading context, using information in the filter params to resolve restriction regions.
   * @param files the files to be read
   * @param numThreads the number of threads to use for reading
   * @param filterParams supplies settings involving individual record filtering and region restrictions
   * @param header the already obtained merged SAM header.
   * @param reference the SequencesReader to be used as the reference (only required for CRAM files).
   * @throws IOException if the restriction involved reading a BED file that could not be read
   */
  public SamReadingContext(Collection<File> files, int numThreads, SamFilterParams filterParams, SAMFileHeader header, SequencesReader reference) throws IOException {
    this(files, numThreads, filterParams, header, reference, SamRangeUtils.createReferenceRanges(header, filterParams));
  }

  /**
   * Create the reading context, using information in the filter params to resolve restriction regions.
   * @param files the files to be read
   * @param numThreads the number of threads to use for reading
   * @param filterParams supplies settings involving individual record filtering and region restrictions
   * @param header the already obtained merged SAM header.
   * @param reference the SequencesReader to be used as the reference (only required for CRAM files).
   * @param referenceRanges the already resolved reference ranges to read over.
   */
  public SamReadingContext(Collection<File> files, int numThreads, SamFilterParams filterParams, SAMFileHeader header, SequencesReader reference, ReferenceRanges<String> referenceRanges) {
    if (header == null) {
      throw new NullPointerException();
    }
    mFiles = files;
    mParams = filterParams;
    mHeader = header;
    mNumThreads = numThreads;
    mReference = reference;
    mReferenceRanges = referenceRanges;
  }

  /**
   * @return the SAM header common to all the files being read (i.e. with all read groups merged etc)
   */
  public SAMFileHeader header() {
    return mHeader;
  }

  /**
   * @return the set of SAM files to be read
   */
  public Collection<File> files() {
    return mFiles;
  }

  /**
   * @return the number of threads to use for reading
   */
  public int numThreads() {
    return mNumThreads;
  }

  /**
   * @return the SamFilterParams used for individual record filtering. Fileds involving region restriction
   * (either explicit or via BED files) should not be used, as they have been loaded and resolved in the context already.
   */
  public SamFilterParams filterParams() {
    return mParams;
  }

  /**
   * @return true if the context has range restrictions, false if unrestricted
   */
  public boolean hasRegions() {
    return mReferenceRanges != null;
  }


  /**
   * @return the SequencesReader supplying the template that the SAM files were mapped against. Only required for CRAM reading.
   */
  public SequencesReader reference() {
    return mReference;
  }

  /**
   * @return the resolved ranges that the reading should operate over, or null if no ranges are being applied.
   */
  public ReferenceRanges<String> referenceRanges() {
    return mReferenceRanges;
  }

  /**
   * @param sequenceId the SAM sequence id of interest.
   * @return the resolved ranges for the specified template sequence
   * @throws java.lang.NullPointerException if no ranges are being applied
   */
  public RangeList<String> rangeList(int sequenceId) {
    return mReferenceRanges.get(sequenceId);
  }

//  /**
//   * @param sequenceName the sequence name of interest.
//   * @return the resolved ranges for the specified template sequence, or null if no ranges are being applied
//   * @throws java.lang.NullPointerException if no ranges are being applied
//   */
//  public RangeList<String> rangeList(String sequenceName) {
//    return mReferenceRanges.get(sequenceName);
//  }
}
