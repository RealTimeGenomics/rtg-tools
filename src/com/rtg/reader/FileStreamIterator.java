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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;

/**
 * Iterates through <code>FileStreams</code>
 */
class FileStreamIterator implements Iterator<InputStream> {

  private final Iterator<File> mIt;
  private final String mProcessLabel;
  private int mCounter = 0;
  private final int mMaxCount;
  private InputStream mLast;
  private InputStream mNext;

  private File mNextFile;
  private File mLastFile;

  FileStreamIterator(final List<File> files, PrereadArm arm) {
    mIt = files.iterator();
    mMaxCount = files.size();
    mLast = null;
    mNext = null;
    if (arm == null || arm == PrereadArm.UNKNOWN) {
      mProcessLabel = "";
    } else {
      mProcessLabel = (arm == PrereadArm.LEFT ? "left" : "right") + " arm ";
    }
  }
  @Override
  public boolean hasNext() {
    if (mNext != null) {
      return true;
    }
    if (mIt.hasNext()) {
      mNextFile = mIt.next();
      ++mCounter;
      try {
        mNext = FileUtils.createInputStream(mNextFile, true);
        if (mMaxCount == 1) {
          Diagnostic.info("Processing " + mProcessLabel + "\"" + mNextFile.toString() + "\"");
        } else {
          Diagnostic.progress(String.format("Processing \"%s\" (%d of %d)", mNextFile.toString(), mCounter, mMaxCount));
        }
      } catch (final FileNotFoundException fnfe) {
        throw new NoTalkbackSlimException("The file: \"" + mNextFile.getPath() + "\" either could not be found or could not be opened. The underlying error message is: \"" + fnfe.getMessage() + "\"");
      } catch (final IOException ex) {
        throw new NoTalkbackSlimException(ErrorType.IO_ERROR, "The file: \"" + mNextFile.getPath() + "\" had a problem while reading. The underlying error message is: \"" + ex.getMessage() + "\"");
      }
      return true;
    }
    return false;
  }
  @Override
  public InputStream next() {
    if (mLast != null) {
      try {
        mLast.close();
      } catch (final IOException e) {
        //not the end of world, log it
        Diagnostic.userLog("Failed to close inputstream");
      }
    }
    if (mNext == null) {
      hasNext();
    }
    mLast = mNext;
    mLastFile = mNextFile;
    mNext = null;
    return mLast;
  }

  /**
   * Gets the file returned be the last call to <code>next</code>
   * @return current file, or null.
   */
  public File currentFile() {
    return mLastFile;
  }

  /**
   * Not implemented
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
