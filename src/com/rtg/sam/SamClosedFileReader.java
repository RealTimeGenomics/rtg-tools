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

import com.rtg.reader.SequencesReader;
import com.rtg.tabix.LocusIndex;
import com.rtg.tabix.TabixIndexReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.VirtualOffsets;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.ClosedFileInputStream;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.CloseableIterator;

/**
 * Utility methods for getting a <code>SAMRecord</code> iterator that is backed by a {@link ClosedFileInputStream}. Note there is no
 * way to reset the reader. If you want to create a new one you must instantiate a new instance of this class.
 */
public final class SamClosedFileReader extends AbstractSamRecordIterator {

  private static final boolean MULTI_CHUNKS = true;

  private final File mFile;
  private final SamReader.Type mType;
  private final ReferenceRanges<String> mRegions;
  private final SequencesReader mReference;
  private final CloseableIterator<SAMRecord> mIterator;
  private final ClosedFileInputStream mStream;
  private boolean mIsClosed;

  /**
   * @param file SAM or BAM file, if region is specified an index must also be present with the appropriate relative file path
   * @param regions regions the file should select, null for whole file
   * @param reference the SequencesReader to be used as the reference (only required for CRAM files).
   * @param header header that should be used for SAM records may not be null  @throws IOException if an IO error occurs
   * @throws IOException if an IO error occurs
   */
  public SamClosedFileReader(File file, ReferenceRanges<String> regions, SequencesReader reference, SAMFileHeader header) throws IOException {
    super(header);
    SamUtils.logRunId(header);
    final SamReader.Type t = SamUtils.getSamType(file);
    mType = t == null ? SamReader.Type.SAM_TYPE : t; // Treat unknown (e.g. piped or /dev/fd63) as SAM
    mFile = file;
    mStream = new ClosedFileInputStream(mFile);
    mReference = reference;
    mRegions = regions;
    mIterator = obtainIteratorInternal();
  }

  @Override
  public boolean hasNext() {
    return mIterator.hasNext();
  }

  @Override
  public SAMRecord next() {
    return mIterator.next();
  }

  @Override
  public void close() throws IOException {
    if (!mIsClosed) {
      mIsClosed = true;
      mIterator.close();
    }
  }


  /**
   * Creates a closed file input stream backed {@link SAMRecord} iterator over the given region.
   * @return the iterator
   * @throws IOException if an IO error occurs
   */
  private CloseableIterator<SAMRecord> obtainIteratorInternal() throws IOException {

    // Handle easy case of no restriction
    if (mRegions == null || mRegions.allAvailable()) {
      if (mType == SamReader.Type.BAM_TYPE) {
        // Currently the below makeSamReader doesn't support header override.
        // Needed when merging map intermediate files which have a minimal header in order to work with large metagenomic references
        return SamUtils.makeSamReader(new BlockCompressedInputStream(mStream), mReference, mHeader, mType).iterator();
      } else {
        return SamUtils.makeSamReader(mStream, mReference, mHeader).iterator(); // htsjdk will decide whether decompression is required
      }
    }

    final LocusIndex index;
    if (mType == SamReader.Type.SAM_TYPE) {
      final File indexFileName = TabixIndexer.indexFileName(mFile);
      if (!TabixIndexer.isBlockCompressed(mFile) || !indexFileName.exists()) {
        throw new NoTalkbackSlimException("File " + mFile.getPath() + " is not tabix indexed");
      }
      index = new TabixIndexReader(indexFileName);
    } else if (mType == SamReader.Type.BAM_TYPE) {
      File indexFileName = BamIndexer.indexFileName(mFile);
      if (!indexFileName.exists()) {
        indexFileName = BamIndexer.secondaryIndexFileName(mFile);
        if (!indexFileName.exists()) {
          throw new NoTalkbackSlimException("File " + mFile.getPath() + " is not indexed for " + mType.fileExtension());
        }
      }
      index = new BamIndexReader(indexFileName, mHeader.getSequenceDictionary());
    } else {
      throw new NoTalkbackSlimException("Indexed extraction from " + mType.fileExtension() + " " + mFile.getPath());
    }

    final VirtualOffsets filePointers = index.getFilePointers(mRegions);
    if (filePointers == null) {
      return new EmptySAMRecordIterator();
    }

    //Diagnostic.developerLog("Using virtual offsets for file: " + mFile.toString() + "\t" + filePointers);

    /*
     * NOTE: now we make sure we handle skipping records that are within the indexed region but not within given restriction,
     * by wrapping in a restricting iterator.
     */
    final BlockCompressedInputStream cfis = new BlockCompressedInputStream(mStream);
    if (MULTI_CHUNKS) {
      return new SamMultiRestrictingIterator(cfis, filePointers, mReference, mHeader, mType, mFile.toString());
    } else {
      // This should be correct but will read all data from start of first region through to end of last region
      cfis.seek(filePointers.start(0));
      return new SamRestrictingIterator(SamUtils.makeSamReader(cfis, mReference, mHeader, mType).iterator(), mRegions);
    }
  }

  private static class EmptySAMRecordIterator implements SAMRecordIterator {
    @Override
    public SAMRecordIterator assertSorted(SAMFileHeader.SortOrder sortOrder) {
      return this;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public SAMRecord next() {
      return null;
    }
  }
}
