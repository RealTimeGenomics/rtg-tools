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
import java.util.Map;
import java.util.NoSuchElementException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.io.ClosedFileInputStream;
import com.rtg.util.io.IOIterator;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.RuntimeIOException;

/**
 * A class to wrap the Tabix line reading capability.
 */
public class TabixLineReader implements LineReader {

  private static LineReader getDelegate(File input, File tabix, RegionRestriction region) throws IOException {
    final LineReader reader;
    if (region == null) {
      reader = new SingleRestrictionLineReader(input, new TabixIndexReader(tabix));
    } else {
      reader = new SingleRestrictionLineReader(input, new TabixIndexReader(tabix), region);
    }
    return reader;
  }

  private static LineReader getDelegate(File input, File tabix, ReferenceRanges<String> ranges) throws IOException {
    final LineReader reader;
    if (ranges == null || ranges.allAvailable()) {
      reader = new SingleRestrictionLineReader(input, new TabixIndexReader(tabix));
    } else {
      reader = new MultiRestrictionLineReader(input, new TabixIndexReader(tabix), ranges);
    }
    return reader;
  }


  private final LineReader mDelegate;

  /**
   *
   * @param input input text file
   * @param tabix index for input file
   * @param region the region to extract, may be null for no restriction
   * @throws IOException if an IO error occurs
   */
  public TabixLineReader(File input, File tabix, RegionRestriction region) throws IOException {
    this(getDelegate(input, tabix, region));
  }

  /**
   *
   * @param input input text file
   * @param tabix index for input file
   * @param ranges the ranges to extract, may be null for no restrictions
   * @throws IOException if an IO error occurs
   */
  public TabixLineReader(File input, File tabix, ReferenceRanges<String> ranges) throws IOException {
    this(getDelegate(input, tabix, ranges));
  }

  /**
   * Used only by <code>VCFMerge</code>.
   * @param input input text file
   * @param tir tabix line reader
   * @param region the region restriction, may be null for no restriction
   * @throws IOException if an IO error occurs
   */
  public TabixLineReader(File input, TabixIndexReader tir, RegionRestriction region) throws IOException {
    this(new SingleRestrictionLineReader(input, tir, region));
  }

  private TabixLineReader(LineReader delegate) {
    mDelegate = delegate;
  }

  @Override
  public void close() throws IOException {
    mDelegate.close();
  }

  @Override
  public String readLine() throws IOException {
    return mDelegate.readLine();
  }

  private static class SingleRestrictionLineReader implements LineReader, IOIterator<String> {
    private final BlockCompressedPositionReader mBCPositionReader;
    private final VirtualOffsets mRange;
    private final String mSequence;
    private final int mBeg;
    private final int mEnd;
    private String mCurrent = null;
    SingleRestrictionLineReader(File input, TabixIndexReader tir) throws IOException {
      mSequence = null;
      mBeg = -1;
      mEnd = -1;
      final BlockCompressedLineReader bclr = new BlockCompressedLineReader(new BlockCompressedInputStream(input));
      mBCPositionReader = tir.getOptions().mFormat == TabixIndexer.TabixOptions.FORMAT_VCF ? new VcfPositionReader(bclr, tir.getOptions().mSkip) : new GenericPositionReader(bclr, tir.getOptions());
      mRange = new VirtualOffsets(0, 0xFFFFFFFFFFFFFFFFL, null);
    }
    SingleRestrictionLineReader(File input, TabixIndexReader tir, RegionRestriction region) throws IOException {
      if (region == null) {
        throw new NullPointerException();
      }
      mSequence = region.getSequenceName();
      mBeg = region.getStart();
      mEnd = region.getEnd();
      final BlockCompressedLineReader bclr = new BlockCompressedLineReader(new BlockCompressedInputStream(input));
      mBCPositionReader = tir.getOptions().mFormat == TabixIndexer.TabixOptions.FORMAT_VCF ? new VcfPositionReader(bclr, tir.getOptions().mSkip) : new GenericPositionReader(bclr, tir.getOptions());
      mRange = tir.getFilePointers(region);
      if (mRange != null) {
        mBCPositionReader.seek(mRange.start(0));
      }
    }

    @Override
    public String readLine() throws IOException {
      return hasNext() ? next() : null;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (mRange == null) {
        return false;
      }
      if (mCurrent != null) {
        return true;
      }
      while (mBCPositionReader.hasNext() && AbstractIndexReader.isLessThanUnsigned(mBCPositionReader.getNextVirtualOffset(), mRange.end(0))) {
        mBCPositionReader.next();
        if ((mSequence == null || mSequence.equals(mBCPositionReader.getReferenceName()))
          && (mEnd == -1 || mBCPositionReader.getStartPosition() < mEnd)
          && (mBeg <= -1 || mBCPositionReader.getStartPosition() + mBCPositionReader.getLengthOnReference() > mBeg)) {
          mCurrent = mBCPositionReader.getRecord();
          return true;
        } else if (mEnd != -1 && mBCPositionReader.getStartPosition() >= mEnd) {
          break;
        }
      }
      return false;
    }

    @Override
    public String next() throws IOException {
      if (hasNext()) {
        final String ret = mCurrent;
        mCurrent = null;
        return ret;
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void close() throws IOException {
      mBCPositionReader.close();
    }
  }

  
  private static class MultiRestrictionLineReader implements LineReader {
    private final VirtualOffsets mOffsets;
    private final BlockCompressedPositionReader mReader;
    private final Map<String, Integer> mSequenceLookup;

    private int mCurrentOffset = 0;
    private SequenceNameLocus mCurrentRegion;
    private int mCurrentTemplate;
    private int mPreviousAlignmentStart = Integer.MIN_VALUE;
    private long mPreviousVirtualOffsetStart = Long.MIN_VALUE;

    private int mDoubleFetched = 0;

    private String mNextRecord;
    private int mNextAlignmentStart;
    private int mNextTemplateId;

    MultiRestrictionLineReader(File input, TabixIndexReader tir, ReferenceRanges<String> ranges) throws IOException {
      if (ranges == null) {
        throw new NullPointerException();
      }
      //Diagnostic.developerLog("Creating MultiRestrictionLineReader");
      final BlockCompressedLineReader bclr = new BlockCompressedLineReader(new BlockCompressedInputStream(new ClosedFileInputStream(input)));
      mReader = tir.getOptions().mFormat == TabixIndexer.TabixOptions.FORMAT_VCF ? new VcfPositionReader(bclr, tir.getOptions().mSkip) : new GenericPositionReader(bclr, tir.getOptions());
      final VirtualOffsets offsets = tir.getFilePointers(ranges);
      mOffsets = offsets == null ? new VirtualOffsets() : offsets;
      mSequenceLookup = tir.mSequenceLookup;
      populateNext(true);
    }

    // Support the ability to "push-back" a record when we get the first one past the bounds of the current region,
    // Since if we end up re-using the underlying iterator, we need to evaluate that record w.r.t the new region.
    private boolean mBuffered = false;

    private void populateNext(boolean force) throws IOException {
      final int previousStart = mNextAlignmentStart;
      final int previousTemplateId = mNextTemplateId;
      mNextRecord = null;
      if (force) {
        advanceSubIterator();
      }
      while (mCurrentOffset <= mOffsets.size()) {
        if (!mBuffered && !mReader.hasNext()) {  // Only happens when stream is exhausted, so effectively just closes things out.
          advanceSubIterator();
        } else {
          if (mBuffered) {
            mBuffered = false;
          } else {
            mReader.next();
          }
          final String refName = mReader.getReferenceName();
          final Integer refId = mSequenceLookup.get(refName); // Note that we cannot rely on mReader.getReferenceId in this scenario, as that is built up incrementally
          if (refId == null) {
            throw new IOException("Tabixed input contained a sequence name not found in the corresponding index: " + refName);
          }

          if (refId > mCurrentTemplate) { // current offset has exceeded region and block overlapped next template
            recordPreviousAndAdvance(previousStart, previousTemplateId);
          } else {

            if (refId < mCurrentTemplate) { // Current block may occasionally return records from the previous template if the block overlaps
              //Diagnostic.developerLog("Ignoring record from earlier template at " + mReader.getReferenceName() + ":" + (mReader.getStartPosition() + 1) + " (" + refId + "<" + mCurrentTemplate + ")");
              continue;
            }

            final int alignmentStart = mReader.getStartPosition();
            final int alignmentEnd = alignmentStart + mReader.getLengthOnReference();

            if (alignmentEnd <= mCurrentRegion.getStart()) {  // before region
              //Diagnostic.developerLog("Ignoring record from earlier than start at " + mReader.getReferenceName() + ":" + (mReader.getStartPosition() + 1));
              continue;
            }

            if (alignmentStart <= mPreviousAlignmentStart) { // this record would have been already returned by an earlier region
              //Diagnostic.developerLog("Ignoring record from earlier block at " + mReader.getReferenceName() + ":" + (alignmentStart + 1));
              ++mDoubleFetched;
              if (mDoubleFetched % 100000 == 0) {
                Diagnostic.developerLog("Many double-fetched records noticed at " + mReader.getReferenceName() + ":" + (alignmentStart + 1) + " in region " + mCurrentRegion + " (skipping through to " + mPreviousAlignmentStart + ")");
              }
              continue;
            }

            if (alignmentStart >= mCurrentRegion.getEnd()) {  // past current region, advance the iterator and record the furtherest we got
              recordPreviousAndAdvance(previousStart, previousTemplateId);
              continue;
            }

            mNextRecord = mReader.getRecord();
            mNextTemplateId = mCurrentTemplate;
            mNextAlignmentStart = alignmentStart;
            break;
          }
        }
      }
    }

    private void recordPreviousAndAdvance(int previousStart, int previousTemplateId) {
      if (previousStart != Integer.MIN_VALUE && previousTemplateId == mCurrentTemplate) {
        mPreviousAlignmentStart = previousStart;
      } else {
        mPreviousAlignmentStart = Integer.MIN_VALUE;
      }
      mBuffered = true;
      advanceSubIterator();
    }

    protected void advanceSubIterator() {
      try {
        if (mCurrentOffset < mOffsets.size()) {
          if (mOffsets.start(mCurrentOffset) != mPreviousVirtualOffsetStart) {
            //Diagnostic.developerLog("After region " + mCurrentOffset + " " + mCurrentRegion + ", opening new reader at offset " + VirtualOffsets.offsetToString(mOffsets.start(mCurrentOffset)) + " for region " + mOffsets.region(mCurrentOffset));
            mReader.seek(mOffsets.start(mCurrentOffset));
            mBuffered = false;
          //} else {
          //  Diagnostic.developerLog("After region " + mCurrentOffset + " " + mCurrentRegion + ", re-using existing reader for region " + mOffsets.region(mCurrentOffset));
          }
          mCurrentRegion = mOffsets.region(mCurrentOffset);
          final int newTemplate = mSequenceLookup.get(mCurrentRegion.getSequenceName());
          if (newTemplate != mCurrentTemplate) {
            mPreviousAlignmentStart = Integer.MIN_VALUE;
          }
          mPreviousVirtualOffsetStart = mOffsets.start(mCurrentOffset);
          mCurrentTemplate = newTemplate;

        }
        ++mCurrentOffset;
      } catch (IOException e) {
        throw new RuntimeIOException(e.getMessage(), e);
      }
    }

    @Override
    public String readLine() throws IOException {
      final String result = mNextRecord;
      if (result != null) {
        populateNext(false);
      }
      return result;
    }

    @Override
    public void close() throws IOException {
      mReader.close();
      if (mDoubleFetched > 0) {
        Diagnostic.developerLog("There were " + mDoubleFetched + " tabixed records double-fetched due to overlapping blocks");
      }
    }
  }
}
