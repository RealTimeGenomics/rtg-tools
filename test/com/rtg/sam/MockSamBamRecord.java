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

import java.util.Arrays;

/**
 * A mock SAM/BAM record.
 * Usage: call setField or addAttribute for each field your test needs to read.
 *   You can also reuse a record, by overwriting just the fields that change.
 *
 */
public class MockSamBamRecord implements SamBamRecord {

  // stores all fields, by their position.
  private String[] mField = new String[SamBamConstants.ATTRIBUTES_FIELD];

  /**
   * @param attr a string like "NM:i:32".
   */
  public void addAttribute(String attr) {
    assert attr.matches("[A-Z][A-Z]:[AifZH]:.+");
    mField = Arrays.copyOf(mField, mField.length + 1);
    mField[mField.length - 1] = attr;
  }

  @Override
  public String getReadName() {
    return mField[SamBamConstants.RNAME_FIELD];
  }

  @Override
  public int getReadNumber() {
    return Integer.parseInt(mField[SamBamConstants.RNAME_FIELD]);
  }

  @Override
  public int getFlags() {
    return Integer.parseInt(mField[SamBamConstants.FLAG_FIELD]);
  }

  @Override
  public boolean hasAttribute(String tag) {
    return getFieldNumFromTag(tag) >= 0;
  }

  @Override
  public char getAttributeType(String tag) {
    final int pos = getFieldNumFromTag(tag);
    return pos < 0 ? 0 : mField[pos].charAt(3);
  }

  @Override
  public int getFieldNumFromTag(String tag) {
    for (int i = SamBamConstants.ATTRIBUTES_FIELD; i < mField.length; i++) {
      if (mField[i] != null && mField[i].startsWith(tag)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public String[] getAttributeTags() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAttributeValue(String tag) {
    final int pos = getFieldNumFromTag(tag);
    return pos < 0 ? null : mField[pos].substring(5);
  }

  @Override
  public int getIntAttribute(String tag) {
    final int pos = getFieldNumFromTag(tag);
    return pos < 0 ? 0 : Integer.parseInt(mField[pos].substring(5));
  }

  @Override
  public int getNumFields() {
    return mField.length;
  }

  @Override
  public String getField(int fieldNum) {
    return mField[fieldNum];
  }

  @Override
  public int getIntField(int fieldNum) {
    return Integer.parseInt(mField[fieldNum]);
  }

  @Override
  public byte[] getFieldBytes(int fieldNum) {
    return mField[fieldNum].getBytes();
  }

}
