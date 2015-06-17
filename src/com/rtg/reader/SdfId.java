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

import java.util.UUID;

import com.rtg.util.StringUtils;

/**
 * Encapsulate <code>SDF-ID</code>'s in its various incarnations
 */
public class SdfId {

  private final long mSdfId;
  private final UUID mSdfUuid;
  private final boolean mHasUuid;

  private final int mLongHash;

  /**
   * Create a new random UUID style SDF-ID
   */
  public SdfId() {
    this(UUID.randomUUID());
  }

  /**
   * Create a UUID style SDF-ID
   * @param id the id
   */
  public SdfId(UUID id) {
    mSdfUuid = id;
    mSdfId = 0;
    mHasUuid = true;
    mLongHash = Long.valueOf(mSdfId).hashCode();
  }

  /**
   * Create an old long based SDF-ID
   * @param id the id
   */
  public SdfId(long id) {
    mSdfUuid = new UUID(0, 0);
    mSdfId = id;
    mHasUuid = false;
    mLongHash = Long.valueOf(mSdfId).hashCode();
  }

  /**
   * <p>Construct an SDF-ID from given string, the type is determined by the String itself
   * <p> i.e. <code>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</code> gives a UUID style SDF-ID
   * <p> i.e. <code>xxxxxxxxxxxxxxxx</code> gives a long based SDF-ID
   * <p><code>x</code> means a hexadecimal digit
   * @param value value to decode
   */
  public SdfId(String value) {
    if (value.contains("-")) {
      mHasUuid = true;
      mSdfUuid = UUID.fromString(value);
      mSdfId = 0;
    } else {
      mHasUuid = false;
      mSdfUuid = new UUID(0, 0);
      mSdfId = StringUtils.fromLongHexString(value);
    }
    mLongHash = Long.valueOf(mSdfId).hashCode();
  }

  /**
   * Checks if this <code>SDF-ID</code> is the same as the supplied one. Except when
   * one or the other has no id (i.e. long constructor with 0 as argument), this implies
   * that no id was available and we cannot check for equality, either because the SDF has
   * no id or that version of the output files did not record the id.
   * @param other other id to check
   * @return true if both <code>SdfId</code>'s are the same or one is not available. false otherwise.
   */
  public boolean check(SdfId other) {
    if (mHasUuid) {
      if (other.mHasUuid) {
        return mSdfUuid.equals(other.mSdfUuid);
      } else {
        return other.mSdfId == 0;
      }
    }
    //mHasUuid == false
    if (other.mHasUuid) {
      return mSdfId == 0;
    }
    //mHasUuid == false && other.mHasUuid == false
    return mSdfId == other.mSdfId || mSdfId == 0 || other.mSdfId == 0;
  }

  @Override
  public String toString() {
    if (mHasUuid) {
      return mSdfUuid.toString();
    } else {
      return Long.toHexString(mSdfId);
    }
  }

  long getLowBits() {
    if (mHasUuid) {
      return mSdfUuid.getLeastSignificantBits();
    }
    return mSdfId;
  }

  long getHighBits() {
    if (mHasUuid) {
      return mSdfUuid.getMostSignificantBits();
    }
    //for testing only
    return 0L;
    //throw new UnsupportedOperationException("High bits only available from UUID type SDF ID");
  }

  /**
   * Reports if a valid SDF-ID in contained within, non-valid SDF-IDs are constructed
   * by calling the {@link SdfId#SdfId(long)} constructor with 0 as the argument. This is used to indicate
   * an SDF-ID was not available from whatever source.
   * @return true if this is a valid SDF-ID
   */
  public boolean available() {
    return mHasUuid || mSdfId != 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SdfId) {
      final SdfId other = (SdfId) obj;
      if (mHasUuid != other.mHasUuid) {
        return false;
      }
      if (mHasUuid) {
        return mSdfUuid.equals(other.mSdfUuid);
      }
      return mSdfId == other.mSdfId;
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (mHasUuid) {
      return mSdfUuid.hashCode();
    }
    return mLongHash;
  }
}
