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

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is the superclass for reading records from a SAM or BAM file.
 * It provides methods for getting the header lines, iterators for
 * iterating through the sequence dictionary and the alignment records,
 * plus lookup methods for the sequence dictionary.
 *
 */
public abstract class SamBamReader implements Iterator<SamBamRecord>, Closeable {

  /** Stores one entry of the sequence dictionary in a SAM/BAM file. */
  public static class SequenceInfo {
    private final String mName;
    private final int mLength;
    /**
     * @param name name of the sequence
     * @param length length of the sequence
     */
    public SequenceInfo(String name, int length) {
      mName = name;
      mLength = length;
    }
    public String getName() {
      return mName;
    }
    public int getLength() {
      return mLength;
    }
  }

  /**
   * Stores all the sequence dictionary, indexed by the reference sequence identifier.
   * Subclasses must set this in their constructor.
   */
  protected List<SequenceInfo> mSeqInfo;

  /**
   * All the non-sequence-dictionary header lines.
   * @return header lines separated by newlines.  Always non-null.
   */
  public abstract String getHeaderLines();

  /**
   * All the header lines.
   * @return header lines separated by newlines.  Always non-null.
   */
  public abstract String getFullHeader();

  /**
   * @return true if the data is coming from a SAM file, false if from BAM.
   */
  public abstract boolean isSam();

  /**
   * @return The number of sequences in the sequence dictionary.
   */
  public int numReferences() {
    return mSeqInfo.size();
  }

  /**
   * Get array containing tags for attributes on current record.
   * @return the array
   */
  public abstract String[] getAttributeTags();

  /**
   * Get the name and length of a reference sequence.
   * Pre: <code>refId</code> is between 0 and <code>numSequences() - 1</code>.
   *
   * @param refId the zero-based number of the sequence.
   * @return an object that contains information about the sequence.
   */
  public SequenceInfo lookupSequence(int refId) {
    return mSeqInfo.get(refId);
  }

  /**
   * @return a read-only list of all the entries in the sequence dictionary.
   */
  public List<SequenceInfo> sequenceDictionary() {
    return Collections.unmodifiableList(mSeqInfo);
  }

  /**
   * True if the current alignment record has an optional tag
   * that starts with <code>tag</code>.
   *
   * Pre: there must be a current alignment record.
   *
   * @param tag The two-character tag of the attribute you are looking for.
   * @return true if the current record has <code>tag</code>.
   */
  public abstract boolean hasAttribute(String tag);

  /**
   * This looks for an optional attribute with the given <code>tag</code>,
   * and returns its type code (one of <code>'AifZH'</code>).
   * If there is no such attribute it returns <code>'?'</code>.
   *
   * Pre: there must be a current alignment record.
   *
   * @param tag The two-character tag of the attribute you are looking for.
   * @return the one character SAM type code.
   */
  public abstract char getAttributeType(String tag);

  /**
   * This looks for an optional attribute with the given <code>tag</code>
   * and returns its value as a string/integer/double object, or null if
   * the alignment record does not contain the requested attribute.
   *
   * Pre: there must be a current alignment record.
   *
   * @param tag The two-character tag of the attribute you are looking for.
   * @return the value associated with that tag, or null.
   */
  public abstract Object getAttributeValue(String tag);

  /**
   * This looks for an optional attribute with the given <code>tag</code>
   * and returns the value part as an integer.
   * It returns <code>Integer.MIN_VALUE</code>
   * if the current alignment record does not contain the requested attribute.
   *
   * Pre: there must be a current alignment record.
   *
   * @param tag The two-character tag of the attribute you are looking for.
   * @return the type:value string associated with that tag, or null.
   */
  public abstract int getIntAttribute(String tag);

  /**
   * Gets the total number of fields in the current record, including
   * all the optional fields.
   *
   * @return the number of fields in the current alignment record
   */
  public abstract int getNumFields();

  /**
   * TODO: for efficiency, add a variant of this that returns/updates a byte[].
   *
   * Get a string version of one field of the current alignment record.
   * NOTE: with <code>fieldNum</code> values 11 or greater, this can be used to get an
   * optional attribute string, or to iterate through all the optional attributes.
   *
   * @param fieldNum <code>0 .. getNumFields()-1</code>.
   * @return a non-null String.
   */
  public abstract String getField(int fieldNum);

  /**
   * Get bytes of a string version of one field of the current alignment record.
   * NOTE: with <code>fieldNum</code> values 11 or greater, this can be used to get an
   * optional attribute string, or to iterate through all the optional attributes.
   *
   * @param fieldNum <code>0 .. getNumFields()-1</code>.
   * @return a non-null String.
   */
  public abstract byte[] getFieldBytes(int fieldNum);

  /**
   * Find the field given a tag
   * @param tag the tag
   * @return the field number, or 0 for not found
   */
  public abstract int getFieldNumFromTag(String tag);

  /**
   * Get an integer version of one of the fixed fields of the current alignment record.
   *
   * @param fieldNum 0..10.
   * @return an integer value, or Integer.MIN_VALUE if that field contains a non-integer.
   */
  public abstract int getIntField(int fieldNum);

  /**
   * This can only be called once per file.
   *
   * @return iterates through all the alignment records.
   */
  public Iterator<SamBamRecord> iterator() {
    return this;
  }

  /**
   * @return true if there is another SAM/BAM alignment record to read.
   */
  @Override
  public abstract boolean hasNext();

  /**
   * The returned record is valid only until <code>next</code> is called again.
   *
   * @return the next SAM/BAM alignment record.
   */
  @Override
  public abstract SamBamRecord next();

//
//  /**
//   * Send copy of record to given importer. Importer is responsible for creating
//   * container object.
//   * @param importer recipient of record
//   * @return the populated record
//   */
//  public abstract SamBamRecord exportCurrentRecord(SamBamRecordImporter importer);
}
