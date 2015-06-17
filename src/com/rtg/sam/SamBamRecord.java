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


/**
 *
 */
public interface SamBamRecord {

  /**
   * Returns the read name as a string.
   * @return the read name
   */
  String getReadName();

  /**
   * This is equivalent to getting the read name and parsing it as an
   * integer, but much more efficient.
   * It should be called only when you are sure the read name
   * will be a valid integer.
   *
   * @return The read name, converted to an integer.
   */
  int getReadNumber();

  /**
   * This returns all the flags as bits within an int.
   * The SAM_READ_IS_... constants can be used to test the individual
   * flags.
   *
   * @return All the flags.
   */
  int getFlags();

  /**
   * Same as in {@link SamBamReader#hasAttribute(String) SamBamReader}.
   *
   * @param tag The two-character tag of the attribute you are looking for.
   * @return true if the current record has that attribute.
   */
  boolean hasAttribute(String tag);

  /**
   * @see SamBamReader#getAttributeType(String)
   *
   * @param tag The two-character tag you are looking for.
   * @return the type character associated with that tag, else 0.
   */
  char getAttributeType(String tag);

  /**
   * @see SamBamReader#getFieldNumFromTag(java.lang.String)
   *
   * @param tag the tag
   * @return the field num
   */
  int getFieldNumFromTag(String tag);

  /**
   * Get array containing tags for attributes on current record.
   * @return the array
   */
  String[] getAttributeTags();

  /**
   * This looks for an optional attribute with the given <code>tag</code>
   * and returns its value as a string/integer/double object, or null if
   * the alignment record does not contain the requested attribute.
   *
   * Pre: there must be a current alignment record.
   *
   * @param tag The two-character tag you are looking for.
   * @return the value associated with that tag, or null.
   */
  Object getAttributeValue(String tag);

  /**
   * This looks for an optional attribute with the given <code>tag</code>
   * and returns the value part as an integer.
   * It returns <code>Integer.MIN_VALUE</code>
   * if the current alignment record does not contain the requested attribute.
   *
   * Pre: there must be a current alignment record.
   *
   * @param tag The two-character tag you are looking for.
   * @return the integer value associated with that tag, else <code>Integer.MIN_VALUE</code>.
   */
  int getIntAttribute(String tag);

  /**
   * @return the number of fields in the current alignment record
   */
  int getNumFields();

  /**
   * Get a string version of one field of the current alignment record.
   * Pre: <code>fieldNum</code> is at least 0 and is less than <code>getNumFields()</code>.
   *
   * @param fieldNum the zero-based number of a field in the current record
   * @return a non-null String.
   */
  String getField(int fieldNum);

  /**
   * Get the length of a field in the current alignment record.
   * Pre: <code>fieldNum</code> is at least 0 and is less than <code>getNumFields()</code>.
   *
   * @param fieldNum the zero-based number of a field in the current record
   * @return number of characters in the field, including the tag characters and colons of attributes.
   */
  // int getFieldLength(int fieldNum);   TODO: add this so MatedSamResultsFilter can get read length efficiently

  /**
   * Get bytes of string version of field of the current alignment record.
   * Pre: <code>fieldNum</code> is at least 0 and is less than <code>getNumFields()</code>.
   * @param fieldNum the zero-based number of a field in the current record
   * @return a non-null byte[].
   */
  byte[] getFieldBytes(int fieldNum);

  /**
   * Get an integer version of one field of the current alignment record.
   * Pre: <code>fieldNum</code> is at least 0 and is less than <code>getNumFields()</code>.
   *
   * @param fieldNum the zero-based number of a field in the current record
   * @return an integer value, or -1 if that field contains a non-integer.
   */
  int getIntField(int fieldNum);

}
