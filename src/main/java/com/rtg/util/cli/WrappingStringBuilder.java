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
package com.rtg.util.cli;

import com.rtg.util.StringUtils;
import com.rtg.visualization.DisplayHelper;

/**
 * <code>WrappingStringBuilder</code> is sort of like a StringBuilder,
 * that can perform word wrapping.
 *
 */
public final class WrappingStringBuilder {

  private final StringBuilder mSB = new StringBuilder();
  private String mSuffix = "";
  private String mPrefix = "";
  private int mWrapWidth = 0;
  private int mLineStart = 0;
  private DisplayHelper mDisplayHelper = new DisplayHelper();

  /**
   * A new wrapping buffer.
   *
   */
  public WrappingStringBuilder() {
    this("");
  }

  /**
   * A new wrapping buffer with display helper.
   * @param dh the display helper for string markup
   */
  public WrappingStringBuilder(DisplayHelper dh) {
    this("");
    mDisplayHelper = dh;
  }

  /**
   * A new wrapping buffer with initial content.
   *
   * @param initial initial content
   */
  public WrappingStringBuilder(final String initial) {
    append(initial);
  }

  /**
   * @return the helper used to mark-up text and determine lengths for wrapping
   */
  public DisplayHelper displayHelper() {
    return mDisplayHelper;
  }

  /**
   * Sets the number of characters that text will be wrapped at when
   * using the wrapping methods.
   *
   * @param width the horizontal wrap width. Default value is 0.
   */
  public void setWrapWidth(final int width) {
    if (width < 0) {
      throw new IllegalArgumentException("Wrap width must be positive.");
    }
    mWrapWidth = width;
  }

  /** Sets the wrap indent to be the length of the current line. */
  public void setWrapIndent() {
    setWrapIndent(lineLength());
  }

  /**
   * Sets the wrap indent to be the specified string.
   *
   * @param prefix wrap indent
   */
  public void setWrapIndent(final String prefix) {
    if (prefix == null) {
      throw new NullPointerException();
    }
    mPrefix = prefix;
  }

  /**
   * Sets the wrap indent to be the specified number of space characters.
   *
   * @param indent indent amount
   */
  public void setWrapIndent(final int indent) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indent; ++i) {
      sb.append(' ');
    }
    mPrefix = sb.toString();
  }

  /**
   * Sets a particular string to be used to terminate any wrapped lines
   * @param suffix the wrap suffix
   */
  public void setWrapSuffix(final String suffix) {
    mSuffix = suffix;
  }

  /**
   * Appends a character without implicit wrapping.
   *
   * @param c char to append
   * @return the buffer
   */
  public WrappingStringBuilder append(final char c) {
    mSB.append(c);
    if (c == '\n') {
      mLineStart = mSB.length();
    }
    return this;
  }

  /**
   * Appends a string without implicit wrapping.
   *
   * @param s text to wrap.
   * @return the buffer
   */
  public WrappingStringBuilder append(final String s) {
    final int end = s.length();
    for (int i = 0; i < end; ++i) {
      append(s.charAt(i));
    }
    return this;
  }

  /**
   * Appends a string, but skipping any leading space characters.
   *
   * @param s text to wrap.
   */
  private void appendTrimmed(final String s) {
    boolean skip = true;
    for (int i = 0; i < s.length(); ++i) {
      final char c = s.charAt(i);
      if (c != ' ') {
        skip = false;
      }
      if (!skip) {
        append(c);
      }
    }
  }

  @Override
  public String toString() {
    return mSB.toString();
  }

  /**
   * Add a newline character and then prefix spacing
   */
  public void wrap() {
    append(mSuffix);
    append(StringUtils.LS);
    append(mPrefix);
  }

  private int lineLength() {
    return mDisplayHelper == null ? (mSB.length() - mLineStart) : mDisplayHelper.length(mSB.substring(mLineStart));
  }

  /**
   * Append a word without breaking it, wrapping first if necessary
   *
   * @param s text to wrap
   * @return the buffer
   */
  public WrappingStringBuilder wrapWord(final String s) {
    if (mWrapWidth - mPrefix.length() < 20) {
      // Skip wrapping if there isn't enough width
      append(s);
      return this;
    }
    final int available = mWrapWidth - lineLength() - mSuffix.length();
    if (s.length() >= available) {
      if (lineLength() != mPrefix.length()) {
        wrap();
      }
      appendTrimmed(s);
    } else {
      append(s);
    }
    return this;
  }


  /**
   * Same as <code>wrapText</code> except it allows newline characters.
   * @param s string to append
   * @return this <code>WrappingStringBuilder</code>
   */
  public WrappingStringBuilder wrapTextWithNewLines(final String s) {
    final String[] arr = s.split("\r?\n");
    for (final String i : arr) {
      wrapText(i);
      append(StringUtils.LS);
    }
    return this;
  }

  /**
   * Wraps text.
   *
   * @param s text to wrap
   * @return the buffer
   * @throws IllegalArgumentException if the input string contains <code>\n</code>
   */
  public WrappingStringBuilder wrapText(final String s) {
    if (s.indexOf('\n') != -1) {
      throw new IllegalArgumentException("Input string cannot contain line breaks.");
    }
    int start = 0;
    int end = 0;
    while (end < s.length()) {
      boolean leader = true;
      for (end = start; end < s.length(); ++end) {
        final char c = s.charAt(end);
        if (Character.isWhitespace(c)) {
          if (!leader) {
            break;
          }
        } else {
          leader = false;
        }
      }
      wrapWord(s.substring(start, end));
      start = end;
    }
    return this;
  }
}


