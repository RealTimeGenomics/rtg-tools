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
package com.rtg.reader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.rtg.mode.FastaSymbolTable;
import com.rtg.mode.Residue;
import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.io.FileUtils;


/**
 * FASTA sequence reader. Reads an entire sequence at a time into a byte array.
 */
public class FastaSequenceDataSource implements SequenceDataSource {

  /**
   * Number of TIDE warnings that will be printed
   */
  protected static final int NUMBER_OF_TIDE_WARNINGS = 10;

  private byte[] mBuffer;
  int mBufferPosition = -1;
  final byte[] mInputBuffer = new byte[FileUtils.BUFFERED_STREAM_SIZE];
  int mInputBufferPosition = -1;
  private int mBufferLength = -1;
  int mInputBufferLength = -1;

  private final FastaSymbolTable mTable;
  private final byte[] mFastaSymbolLookupTable;

  final Iterator<InputStream> mSourceIt;

  InputStream mSource = null;
  boolean mSourceClosed = true;

  String mCurrentSequenceName = null;

  private int mWarningCount = 0;

  private long mMaxLength = 0;
  private long mMinLength = Long.MAX_VALUE;
  private long mDusted = 0;

  private boolean mDustSequence;

  /**
   * FASTA extension
   */
  public static final String FASTA_EXTENSION = ".fasta";

  /**
   * Read FASTA sequences from given InputStreams. Primarily used for testing.
   * @param iss list of InputStreams
   * @param table Symbol table for type of input.
   */
  public FastaSequenceDataSource(List<InputStream> iss, FastaSymbolTable table) {
    if (table == null) {
      throw new NullPointerException();
    }
    mTable = table;
    mFastaSymbolLookupTable = table.getAsciiToOrdinalTable();
    mSourceIt = iss.iterator();
  }

  /**
   * Read FASTA sequences from given files.
   * @param files List of files
   * @param table Symbol table for type of input.
   * @param arm the arm that this source belongs to
   */
  public FastaSequenceDataSource(List<File> files, FastaSymbolTable table, PrereadArm arm) {
    if (files == null || arm == null) {
      throw new NullPointerException();
    }
    mTable = table;
    mFastaSymbolLookupTable = table.getAsciiToOrdinalTable();
    mSourceIt = new FileStreamIterator(files, arm);
  }

  @Override
  public void close() throws IOException {
    while (nextSource()) {
      //intentional
    }
  }

  @Override
  public int currentLength() {
    return mBufferLength;
  }

  @Override
  public long getWarningCount() {
    return mWarningCount;
  }

  @Override
  public boolean hasQualityData() {
    return false;
  }

  @Override
  public String name() throws IllegalStateException, IOException {
    return mCurrentSequenceName;
  }

  /**
   * Step to the next input stream.
   * @return true if next source has been opened, false for no more sources.
   * @throws IOException If an I/O error occurs.
   */
  boolean nextSource() throws IOException {
    if (mSourceIt.hasNext()) {
      final InputStream is = mSourceIt.next();
      if (is == null) {
        mSource = null;
        return false;
      } else {
        mSource = new BufferedInputStream(is, FileUtils.BUFFERED_STREAM_SIZE);
      }
      mSourceClosed = false;
      mInputBufferPosition = 0;
      mInputBufferLength = 0;
      mBufferLength = 0;
      mBufferPosition = 0;
      return true;
    }
    if (mSource != null) {
      mSource.close();
    }
    mSource = null;
    return false;
  }

  void readInputBuffer() throws IOException {
    if (!mSourceClosed && mInputBufferPosition == mInputBufferLength) {
      //read in input buffer
      mInputBufferLength = mSource.read(mInputBuffer);
      mInputBufferPosition = 0;
      if (mInputBufferLength <= 0) {   //at end of file...
        mSource.close();
        mSourceClosed = true;
        mSource = null;
      }
    }
  }

  private void ensureBuffer() {
    if (mBufferLength == -1 || mBuffer == null) {
      mBuffer = new byte[mInputBufferLength * 10];
      mBufferPosition = 0;
    } else if (mBuffer.length == Integer.MAX_VALUE) {
      throw new NoTalkbackSlimException("Sequence: \"" + mCurrentSequenceName + "\" is too large, individual sequence length must be less than or equal to " + Integer.MAX_VALUE);
    } else {
      mBuffer = Arrays.copyOf(mBuffer, (int) Math.min(mBuffer.length * 1.62, Integer.MAX_VALUE));
    }
  }

  /**
   * Used to loop through sources with no sequences
   * @return true if a sequence with a label is found
   * @throws IOException if an io exception occurs
   */
  private boolean searchSequenceLabel() throws IOException {
    if (!seekAndReadNextLabel()) {
      if (!(nextSource() && searchSequenceLabel())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean nextSequence() throws IOException {
    if (mSource == null || mSourceClosed) {
      if (!nextSource()) {
        return false;
      }
    }
    mCurrentSequenceName = null;

    if (!searchSequenceLabel()) {
      return false;
    }

    mBufferPosition = 0;
    //we're after the \n, so now is nucleotides
    readData('>');
    return true;
  }

  void readData(char labelChar) throws IOException {
    readInputBuffer();
    if (mInputBufferLength == -1) {
      return;
    }
    if (mBuffer == null) {
      ensureBuffer();
    }
    byte inputByte;
    final Residue unknownResidue = mTable.unknownResidue();
    byte residueByte;
    while (mInputBufferPosition < mInputBufferLength) {
      if (mInputBufferLength <= 0) {
        return;
      }
      inputByte = mInputBuffer[mInputBufferPosition];
      if (inputByte == labelChar) {
        setBufferLength(mBufferPosition);
        return;
      } else if (inputByte <= 32) {     //whitespace
        ++mInputBufferPosition;
        readInputBuffer();
        continue;
      } else if (mDustSequence && Character.isLowerCase((char) inputByte)) {
        residueByte = (byte) unknownResidue.ordinal();
        ++mDusted;
      } else {
        residueByte = mFastaSymbolLookupTable[inputByte];
        if (residueByte == (byte) 255) {
        //unrecognized character, print warning, shove in unknown
          if (mWarningCount < NUMBER_OF_TIDE_WARNINGS) {
            Diagnostic.warning(WarningType.BAD_TIDE, mCurrentSequenceName, Character.toString((char) inputByte), unknownResidue.toString());
          }
          ++mWarningCount;
          if (mWarningCount == NUMBER_OF_TIDE_WARNINGS) {
            Diagnostic.warning("Subsequent warnings of this type will not be shown.");
          }
          residueByte = (byte) unknownResidue.ordinal();
        }
      }
      if (mBufferPosition == mBuffer.length) {
        ensureBuffer();
      }
      mBuffer[mBufferPosition] = residueByte;
      ++mBufferPosition;
      ++mInputBufferPosition;
      readInputBuffer();
    }
    setBufferLength(mBufferPosition);
  }

  private void setBufferLength(int length) {
    mBufferLength = length;
    if (mBufferLength > mMaxLength) {
      mMaxLength = mBufferLength;
    }
    if (mBufferLength < mMinLength) {
      mMinLength = mBufferLength;
    }
  }

  /**
   * Will continue iterating through input buffer values until the previous
   * character is the FASTA label indicator (&gt;).
   * Will then read the label.
   * @return true If previous character is &gt; and the label can be successfully read, otherwise false
   * @throws IOException if an I/O error occurs.
   */
  private boolean seekAndReadNextLabel() throws IOException {
    readInputBuffer();
    while (mInputBufferLength > 0 && mInputBufferPosition < mInputBufferLength) {
      final byte b = mInputBuffer[mInputBufferPosition];
      if (b == '>') {
        ++mInputBufferPosition; //move one past the >
        return readLabel();
      } else {
        if (b == '@') {
          throw new NoTalkbackSlimException(ErrorType.FASTQ);
        } else if (!Character.isWhitespace((char) b)) {
          throw new NoTalkbackSlimException(ErrorType.BAD_FASTA_LABEL, name() != null ? name() : "<none>");
        }
      }
      ++mInputBufferPosition;
      readInputBuffer();
    }
    final String filename = mSourceIt instanceof FileStreamIterator ? ((FileStreamIterator) mSourceIt).currentFile().getPath() : "<Not known>";
    Diagnostic.warning(WarningType.NOT_FASTA_FILE, filename);
    return false;
  }

  boolean readLabel() throws IOException {
    final StringBuilder name = new StringBuilder();
    readInputBuffer();
    while (mInputBufferLength > 0 &&  mInputBufferPosition < mInputBufferLength) {
      final byte b = mInputBuffer[mInputBufferPosition];
      if (b == '\r' || b == '\n') {
        mCurrentSequenceName = name.toString();
        ++mInputBufferPosition; //skip past the new line
        return true;
      }
      if (name.length() >= 1000 * 1000) {
        Diagnostic.warning(""); //Ugly way to separate the warning.
        Diagnostic.warning("Fasta sequence label is longer than 1,000,000 characters, this might affect the output file size.");
      }
      name.append((char) b);
      ++mInputBufferPosition;
      readInputBuffer();
    }
    return false;
  }


  @Override
  public byte[] qualityData() throws IllegalStateException, IOException {
    return null;
  }

  @Override
  public byte[] sequenceData() throws IllegalStateException, IOException {
    return mBuffer;
  }

  @Override
  public void setDusting(boolean val) {
    mDustSequence = val;
  }

  @Override
  public SequenceType type() {
    return mTable.getSequenceType();
  }

  /**
   * @return the number of residues that were dusted
   */
  @Override
  public long getDusted() {
    return mDusted;
  }

  /**
   * @return the maximum read length
   */
  @Override
  public long getMaxLength() {
    return mMaxLength;
  }

  /**
   * @return the minimum read length
   */
  @Override
  public long getMinLength() {
    return mMinLength;
  }

}
