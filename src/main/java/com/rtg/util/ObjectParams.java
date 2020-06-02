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
package com.rtg.util;

import java.io.IOException;

import com.reeltwo.jumble.annotations.JumbleIgnore;


/**
 * Provides simple (but not incredibly efficient) way of doing hash and equals
 * by using an array of the objects in the class.
 */
@JumbleIgnore
public abstract class ObjectParams implements Params {

  /** This is really final but needs to be set late in the constructors for the sub-classes so can't be declared final. */
  protected Object[] mObjects = new Object[0];

  /**
   * Append objects.
   * Useful in subclasses with their own state.
   * @param objs to be added.
   */
  protected void append(final Object[] objs) {
    mObjects = Utils.append(mObjects, objs);
  }

  protected final Object[] objects() {
    return mObjects;
  }

  @Override
  public int hashCode() {
    return Utils.hash(objects());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    assert obj instanceof ObjectParams : obj.getClass().getName();
    final ObjectParams that = (ObjectParams) obj;
    return Utils.equals(this.objects(), that.objects());
  }

  @Override
  public void close() throws IOException {
    // default - do nothing
  }

}
