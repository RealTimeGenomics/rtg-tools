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

/**
 * Holds sequence names as two parts, an initial short name and a suffix.
 */
public class Label {
  private final String mLabel;
  private final String mSuffix;

  /**
   * Creates a label loading the fields.
   *
   * @param label  the short name for the label
   * @param suffix the suffix for the label
   */
  public Label(String label, String suffix) {
    mLabel = label == null ? "" : label;
    mSuffix = suffix == null ? "" : suffix;
  }

  /**
   * Returns the short name for the label.
   *
   * @return short name
   */
  public String label() {
    return mLabel;
  }

  /**
   * Returns the suffix for the label.
   *
   * @return suffix
   */
  public String suffix() {
    return mSuffix;
  }

  /**
   * Returns the full name for the label.
   *
   * @return full name
   */
  public String fullLabel() {
    return mLabel + mSuffix;
  }

  /**
   * Returns length of label.
   *
   * @return length of label
   */
  public int length() {
    return mLabel.length() + mSuffix.length();
  }

  @Override
  public String toString() {
    return fullLabel();
  }
}
