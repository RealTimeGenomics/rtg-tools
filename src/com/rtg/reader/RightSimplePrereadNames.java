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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Read names list for right arm that uses left arm names for storage.
 */
public class RightSimplePrereadNames extends SimplePrereadNames {
  private final SimplePrereadNames mLeftNames;

  /**
   * Construct a right arm names holder, using the left arm's names.
   * @param leftNames a {@link SimplePrereadNames} for the left arm
   */
  public RightSimplePrereadNames(SimplePrereadNames leftNames) {
    mLeftNames = leftNames;
  }

  @Override
  public long length() {
    return mLeftNames.length();
  }

  @Override
  public String name(long id) {
    return mLeftNames.name(id);
  }

  @Override
  public void setName(long id, String name) {
    // do nothing - using left read name as storage
  }

  @Override
  public long calcChecksum() {
    return mLeftNames.calcChecksum();
  }

  @Override
  public long bytes() {
    //low balling estimate at 2 pointers and a long per entry. TODO make this more reasonable
    return 0L;
  }

  @Override
  public void writeName(Appendable a, long id) throws IOException {
    a.append(name(id));
  }

  @Override
  public void writeName(OutputStream stream, long id) throws IOException {
    stream.write(mLeftNames.getNameBytes(id));
  }

}
