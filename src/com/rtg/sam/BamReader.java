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

import static com.rtg.sam.SamBamConstants.CIGAR_FIELD;
import static com.rtg.sam.SamBamConstants.FLAG_FIELD;
import static com.rtg.sam.SamBamConstants.ISIZE_FIELD;
import static com.rtg.sam.SamBamConstants.MAPQ_FIELD;
import static com.rtg.sam.SamBamConstants.MPOS_FIELD;
import static com.rtg.sam.SamBamConstants.MRNM_FIELD;
import static com.rtg.sam.SamBamConstants.POS_FIELD;
import static com.rtg.sam.SamBamConstants.QNAME_FIELD;
import static com.rtg.sam.SamBamConstants.QUAL_FIELD;
import static com.rtg.sam.SamBamConstants.RNAME_FIELD;
import static com.rtg.sam.SamBamConstants.SEQ_FIELD;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.util.io.ByteArrayIOUtils;

import htsjdk.samtools.util.RuntimeIOException;

/**
 * Reads BAM data
 */
public class BamReader extends SamBamReader {

  private final SamBamRecordImpl mCurrentRecord;

  //constants
  //BAM ONLY FIELDS
  /** index bin field */
  public static final int BIN_FIELD = -1;

  //Offsets
  private static final int REF_ID_OFFSET = 0;
  private static final int POS_OFFSET = 4;
  private static final int READ_NAME_LEN_OFFSET = 8;
  private static final int MAPQ_OFFSET = 9;
  private static final int BIN_OFFSET = 10;
  private static final int CIGAR_LEN_OFFSET = 12;
  private static final int FLAG_OFFSET = 14;
  private static final int READ_LEN_OFFSET = 16;
  private static final int MATE_REF_ID_OFFSET = 20;
  private static final int MATE_POS_OFFSET = 24;
  private static final int INSERT_SIZE_OFFSET = 28;
  private static final int READ_NAME_OFFSET = 32;

  private static final char[] CIGAR_CODES = SamUtils.getCigarCodes();

  private static final char[] BASE_CODES = {'=', //0
                                                        'A', //1
                                                        'C', //2
                                                        '\0', //3
                                                        'G', //4
                                                        '\0', //5
                                                        '\0', //6
                                                        '\0', //7
                                                        'T', //8
                                                        '\0', //9
                                                        '\0', //10
                                                        '\0', //11
                                                        '\0', //12
                                                        '\0', //13
                                                        '\0', //14
                                                        'N'}; //15

  private boolean mState = false;
  private byte[] mIOBuf;
  private final BgzfInputStream mInput;

  private String mHeader;
  private int[] mReferenceLengths;
  private String[] mReferenceNames;

  private byte[] mCurrentAlignment;
  private int mCurrentAlignmentLength;
  private boolean mHasNext;
  private int mNextAlignmentLength;
  private long mVirtualOffset;
  private long mNextVirtualOffset;

  private int mReadNameLength;
  private int mCigarLen;
  private int mReadLen;
  private int mReadSeqLen;
  private int mCigarOffset;
  private int mReadSeqOffset;
  private int mReadQualOffset;
  private int mAttributeOffset;

  private boolean mAttributeCheck;
  private int[] mAttributeOffsets;
  private int mAttributeOffsetsLength;

  /**
   * Construct the reader from file
   * @param input BAM file
   * @throws IOException If an IO error occurs
   */
  public BamReader(final File input) throws IOException {
    this(new BgzfInputStream(input), true);
  }

  /**
   * Construct the reader from stream
   * @param input BAM stream
   * @throws IOException If an IO error occurs
   */
  public BamReader(final InputStream input) throws IOException {
    this(new BgzfInputStream(input), true);
  }

  /**
   * Construct the reader from <code>BGZF</code> stream
   * @param input <code>BGZF</code> stream
   * @param readHeader true if file has a BAM header
   * @throws IOException If an IO error occurs
   */
  public BamReader(final BgzfInputStream input, boolean readHeader) throws IOException {
    mInput = input;
    mIOBuf = new byte[4096];
    if (readHeader) {
      final int numRefs = readHeader(input);
      readReferences(input, numRefs);
    }
    mCurrentAlignment = new byte[8192];
    mCurrentRecord = new SamBamRecordImpl(this);
    populateHasNext();
  }

  /**
   * Number of references in BAM header
   * @return number
   */
  @Override
  public int numReferences() {
    return mReferenceLengths.length;
  }

  /**
   * return length of reference from sequence id
   * @param refId sequence id
   * @return length
   */
  public int referenceLength(final int refId) {
    return mReferenceLengths[refId];
  }

  /**
   * Get reference name for given id
   * @param refId the id
   * @return the name
   */
  public String referenceName(final int refId) {
    return mReferenceNames[refId];
  }

  private void updateVariableLengths() {
    mReadNameLength = mCurrentAlignment[READ_NAME_LEN_OFFSET] & 0xFF;
    mCigarLen = ByteArrayIOUtils.bytesToShortLittleEndian(mCurrentAlignment, CIGAR_LEN_OFFSET);
    mReadLen = ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, READ_LEN_OFFSET);
    mReadSeqLen = (mReadLen + 1) / 2;
    mCigarOffset = READ_NAME_OFFSET + mReadNameLength;
    mReadSeqOffset = mCigarOffset + mCigarLen * 4;
    mReadQualOffset = mReadSeqOffset + mReadSeqLen;
    mAttributeOffset = mReadQualOffset + mReadLen;
  }

  /**
   * Returns value of given field in current alignment
   * @param fieldId as described by constant fields above.
   * @return the value
   */
  @Override
  public int getIntField(final int fieldId) {
    state();
    switch (fieldId) {
      case RNAME_FIELD:
        return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, REF_ID_OFFSET);
      case QNAME_FIELD:
        return Integer.parseInt(zeroPaddedString(mCurrentAlignment, READ_NAME_OFFSET, mReadNameLength));
      case POS_FIELD:
        return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, POS_OFFSET) + 1;
      case MAPQ_FIELD:
        return mCurrentAlignment[MAPQ_OFFSET] & 0xFF;
      case BIN_FIELD:
        return ByteArrayIOUtils.bytesToShortLittleEndian(mCurrentAlignment, BIN_OFFSET);
      case FLAG_FIELD:
        return ByteArrayIOUtils.bytesToShortLittleEndian(mCurrentAlignment, FLAG_OFFSET);
      case MRNM_FIELD:
        return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, MATE_REF_ID_OFFSET);
      case MPOS_FIELD:
        return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, MATE_POS_OFFSET) + 1;
      case ISIZE_FIELD:
        return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, INSERT_SIZE_OFFSET);
      default:
        throw new IllegalArgumentException("Invalid int field: " + fieldId);
    }
  }

  /**
   * Returns value of given field in current alignment
   * @param fieldId as described by constant fields above.
   * @return the value
   */
  @Override
  public String getField(final int fieldId) {
    state();
    switch (fieldId) {
      case RNAME_FIELD:
        final int refId = ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, REF_ID_OFFSET);
        if (refId != -1) {
          return mReferenceNames[refId];
        }
        return "*";
      case MRNM_FIELD:
        final int matedRefId = ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, MATE_REF_ID_OFFSET);
        if (matedRefId != -1) {
          return mReferenceNames[matedRefId];
        }
        return "*";
      case QNAME_FIELD:
        return zeroPaddedString(mCurrentAlignment, READ_NAME_OFFSET, mReadNameLength);
      case CIGAR_FIELD:
        return bytesToCigar(mCurrentAlignment, mCigarOffset, mCigarLen);
      case SEQ_FIELD:
        return bytesToSeq(mCurrentAlignment, mReadSeqOffset, mReadSeqLen, mReadLen);
      case QUAL_FIELD:
        return bytesToQual(mCurrentAlignment, mReadQualOffset, mReadLen);
      default:
        throw new IllegalArgumentException("Invalid String field: " + fieldId);
    }
  }

  @Override
  public byte[] getFieldBytes(int fieldNum) {
    return getField(fieldNum).getBytes();
  }

  /**
   * Gives the virtual offset used by BAM index for current alignment
   * @return the value
   */
  public long virtualOffset() {
    return mVirtualOffset;
  }

  /**
   * Gives the virtual offset used by BAM index for current alignment
   * @return the value
   */
  public long nextVirtualOffset() {
    return mNextVirtualOffset;
  }


  private void populateHasNext() throws IOException {
    mNextVirtualOffset = encodeVirtualOffset(mInput.blockStart(), mInput.dataOffset());
    final int len = mInput.read(mIOBuf, 0, 4);
    if (len <= 0) {
      mHasNext = false;
      return;
    }
    if (len < 4) {
      readDataFully(mIOBuf, len, 4 - len, mInput);
    }
    mNextAlignmentLength = ByteArrayIOUtils.bytesToIntLittleEndian(mIOBuf, 0);
    mHasNext = true;
  }

  @Override
  public boolean hasNext() {
    return mHasNext;
  }

  @Override
  public SamBamRecord next() {
    mAttributeCheck = false;
    mVirtualOffset = mNextVirtualOffset;
    mCurrentAlignmentLength = mNextAlignmentLength;
    initAlignmentBlock(mCurrentAlignmentLength);
    try {
      readDataFully(mCurrentAlignment, 0, mCurrentAlignmentLength, mInput);
      updateVariableLengths();
      mState = true;
      populateHasNext();
    } catch (final IOException e) {
      throw new RuntimeIOException(e.getMessage(), e);
    }
    return mCurrentRecord;
  }

  private void initAlignmentBlock(final int size) {
    if (mCurrentAlignment.length < size) {
      mCurrentAlignment = new byte[size];
    }
  }

  private static long encodeVirtualOffset(final long blockOffset, final int alignmentOffset) {
    return blockOffset << 16 | (long) alignmentOffset;
  }

  //return number of references
  private int readHeader(final InputStream input) throws IOException {
    readDataFully(mIOBuf, 0, 8, input);
    final int headerLength = ByteArrayIOUtils.bytesToIntLittleEndian(mIOBuf, 4);
    if (mIOBuf.length < headerLength) {
      mIOBuf = new byte[headerLength];
    }
    readDataFully(mIOBuf, 0, headerLength, input);
    mHeader = new String(mIOBuf, 0, headerLength);
    //ignore it
    readDataFully(mIOBuf, 0, 4, input);
    return ByteArrayIOUtils.bytesToIntLittleEndian(mIOBuf, 0);
  }

  private void readReferences(final InputStream input, final int num) throws IOException {
    mSeqInfo = new ArrayList<>(num);
    mReferenceLengths = new int[num];
    mReferenceNames = new String[num];
    for (int i = 0; i < mReferenceLengths.length; i++) {
      readDataFully(mIOBuf, 0, 4, input);
      final int nameLen = ByteArrayIOUtils.bytesToIntLittleEndian(mIOBuf, 0);
      if (mIOBuf.length < nameLen) {
        mIOBuf = new byte[nameLen];
      }
      readDataFully(mIOBuf, 0, nameLen, input);
      mReferenceNames[i] = new String(mIOBuf, 0, nameLen - 1);
      readDataFully(mIOBuf, 0, 4, input);
      mReferenceLengths[i] = ByteArrayIOUtils.bytesToIntLittleEndian(mIOBuf, 0);
      mSeqInfo.add(new SequenceInfo(mReferenceNames[i], mReferenceLengths[i]));
    }
  }

  private static void readDataFully(final byte[] buf, final int offset, final int length, final InputStream stream) throws IOException {
    int amountRead = 0;
    while (amountRead < length) {
      final int len = stream.read(buf, offset + amountRead, length - amountRead);
      if (len <= 0) {
        throw new EOFException("Unexpected end of file while reading BAM file");
      }
      amountRead += len;
    }
  }

  @Override
  public void close() throws IOException {
    mInput.close();
  }

  private void state() {
    if (!mState) {
      throw new IllegalStateException();
    }
  }


  private static String zeroPaddedString(final byte[] buf, final int offset, final int length) {
    return new String(buf, offset, length - 1);
  }

  private static String bytesToCigar(final byte[] buf, final int offset, final int length) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      final int c = ByteArrayIOUtils.bytesToIntLittleEndian(buf, offset + (i * 4));
      sb.append((c >> 4) & 0x0FFFFFFF);
      sb.append(CIGAR_CODES[c & 0xF]);
    }
    return sb.toString();
  }

  private static String bytesToSeq(final byte[] buf, final int offset, final int length, final int readLen) {
    final StringBuilder sb = new StringBuilder();
    final int len = length * 2 == readLen ? length : length - 1;
    for (int i = 0; i < len; i++) {
      final byte b = buf[offset + i];
      sb.append(BASE_CODES[(b & 0xFF) >> 4]);
      sb.append(BASE_CODES[b & 0xF]);
    }
    if (len != length) {
      final byte b = buf[offset + len];
      sb.append(BASE_CODES[(b & 0xFF) >> 4]);
    }
    return sb.toString();
  }

  private static String bytesToQual(final byte[] buf, final int offset, final int length) {
    if (buf[offset] == (byte) 0xFF) {
      return "*";
    }
    final byte[] qual = new byte[length];
    for (int i = 0; i < length; i++) {
      qual[i] = (byte) (((byte) '!') + buf[offset + i]);
    }
    return new String(qual, 0, length);
  }

  private void prepAttributes() {
    if (!mAttributeCheck) {
      mAttributeOffsetsLength = 0;
      if (mAttributeOffsets == null) {
        mAttributeOffsets = new int[10];
      }
      int pos = mAttributeOffset;
      while (pos < mCurrentAlignmentLength) {
        if (mAttributeOffsetsLength == mAttributeOffsets.length) {
          mAttributeOffsets = Arrays.copyOf(mAttributeOffsets, mAttributeOffsets.length * 2);
        }
        mAttributeOffsets[mAttributeOffsetsLength++] = pos;
        pos += 2;
        final char type = (char) mCurrentAlignment[pos++];
        switch (type) {
          case 'A':
          case 'c':
          case 'C':
            pos++;
            break;
          case 's':
          case 'S':
            pos += 2;
            break;
          case 'i':
          case 'I':
          case 'f':
            pos += 4;
            break;
          case 'H':
          case 'Z':
            while (mCurrentAlignment[pos++] != 0) {
              if (pos == mCurrentAlignmentLength) {
                throw new SamRecordException("Invalid BAM attribute string, number: " + (mAttributeOffsetsLength - 1));
              }
            }
            break;
          default:
            throw new SamRecordException("Invalid BAM attribute field type: " + type + ", number: " + (mAttributeOffsetsLength - 1));
        }
      }
      mAttributeCheck = true;
    }
  }

  private int findAttribute(final String tag) {
    final int tag1 = (int) tag.charAt(0); //I changed these from codePoints for .net, hopefully will be ok
    final int tag2 = (int) tag.charAt(1);
    for (int i = 0; i < mAttributeOffsetsLength; i++) {
      final int pos = mAttributeOffsets[i];
      if ((int) mCurrentAlignment[pos] == tag1 && (int) mCurrentAlignment[pos + 1] == tag2) {
        return pos;
      }
    }
    return 0;
  }

  @Override
  public String[] getAttributeTags() {
    final String[] ret = new String[mAttributeOffsetsLength];
    for (int i = 0; i < mAttributeOffsetsLength; i++) {
      final int pos = mAttributeOffsets[i];
      ret[i] = new String(mCurrentAlignment, pos, 2);
    }
    return ret;
  }



  /**
   *
   * @param tag irrelevant
   * @return nothing
   */
  @Override
  public int getFieldNumFromTag(final String tag) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public char getAttributeType(final String tag) {
    prepAttributes();
    final int pos = findAttribute(tag);
    if (pos != 0) {
      return (char) mCurrentAlignment[pos + 2];
    }
    return '?';
  }

  @Override
  public Object getAttributeValue(final String tag) {
    prepAttributes();
    final int pos = findAttribute(tag);
    if (pos != 0) {
      final char type = (char) mCurrentAlignment[pos + 2];
      switch (type) {
        case 'A':
          return (char) mCurrentAlignment[pos + 3];
        case 'f':
          return Float.intBitsToFloat(ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, pos + 3));
        case 'i':
          return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, pos + 3);
        case 'C':
          return mCurrentAlignment[pos + 3] & 0xff;
        case 'c':
          return (int) mCurrentAlignment[pos + 3];
        case 'Z':
        case 'H':
          return zeroTerminatedString(mCurrentAlignment, pos + 3, mCurrentAlignmentLength);
        default:
          throw new IllegalArgumentException();
      }
    }
    return null;
  }

  private String zeroTerminatedString(final byte[] buf, final int pos, final int maxPos) {
    int len = 0;
    while (pos + len < maxPos) {
      if (buf[pos + len++] == 0) {
        return new String(buf, pos, len - 1);
      }
    }
    throw new SamRecordException("Invalid attribute field");
  }

  @Override
  public String getHeaderLines() {
    return mHeader;
  }

  @Override
  public String getFullHeader() {
    return mHeader;
  }

  @Override
  public int getIntAttribute(final String tag) {
    prepAttributes();
    final int pos = findAttribute(tag);
    if (pos != 0) {
      switch ((char) mCurrentAlignment[pos + 2]) {
        case 'i':
          return ByteArrayIOUtils.bytesToIntLittleEndian(mCurrentAlignment, pos + 3);
        case 'C':
          return mCurrentAlignment[pos + 3] & 0xff;
        case 'c':
          return (int) mCurrentAlignment[pos + 3];
        case 's':
          return (short) ByteArrayIOUtils.bytesToShortLittleEndian(mCurrentAlignment, pos + 3);
        case 'S':
          return ByteArrayIOUtils.bytesToShortLittleEndian(mCurrentAlignment, pos + 3);
        default:
          throw new IllegalArgumentException("Not an implemented int type: " + ((char) mCurrentAlignment[pos + 2]));
      }
    } else {
      return Integer.MIN_VALUE;
    }
  }

  @Override
  public int getNumFields() {
    prepAttributes();
    return mAttributeOffsetsLength;
  }

  @Override
  public boolean hasAttribute(final String tag) {
    prepAttributes();
    return findAttribute(tag) != 0;
  }

  /**
   * BAM
   * @return false
   */
  @Override
  public boolean isSam() {
    return false;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
