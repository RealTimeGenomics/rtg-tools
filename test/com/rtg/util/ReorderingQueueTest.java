/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test class
 */
public class ReorderingQueueTest extends TestCase {

  static class SimpleRecord {
    final String mRef;
    final int mPos;
    SimpleRecord(String ref, int pos) {
      mRef = ref;
      mPos = pos;
    }
  }

  static class SimpleComparator implements Comparator<SimpleRecord>, Serializable {
    @Override
    public int compare(SimpleRecord o1, SimpleRecord o2) {
      final int r1 = o1.mRef.compareTo(o2.mRef);
      return (r1 != 0) ? r1 : o1.mPos - o2.mPos;
    }
  }

  static class SimpleReorderingQueue extends ReorderingQueue<SimpleRecord> {
    List<SimpleRecord> mOutput = new ArrayList<>();
    SimpleReorderingQueue() {
      super(5, new SimpleComparator());
    }

    @Override
    protected String getReferenceName(SimpleRecord record) {
      return record.mRef;
    }

    @Override
    protected int getPosition(SimpleRecord record) {
      return record.mPos;
    }

    @Override
    protected void flushRecord(SimpleRecord rec) {
      mOutput.add(rec);
    }

    @Override
    protected void reportReorderingFailure(SimpleRecord rec) {
    }
  }

  public void test1() throws IOException {
    final SimpleReorderingQueue q = new SimpleReorderingQueue();
    q.addRecord(new SimpleRecord("a", 4));
    q.addRecord(new SimpleRecord("a", 3));
    q.addRecord(new SimpleRecord("a", 2));
    q.addRecord(new SimpleRecord("a", 2));
    q.addRecord(new SimpleRecord("a", 1));
    q.addRecord(new SimpleRecord("a", 0));
    q.close();
    assertEquals(5, q.mOutput.size());
    for (int i = 0; i < 5; ++i) {
      assertEquals(i, q.mOutput.get(i).mPos);
    }
  }

  public void test2() throws IOException {
    final SimpleReorderingQueue q = new SimpleReorderingQueue();
    q.addRecord(new SimpleRecord("a", 4));
    q.addRecord(new SimpleRecord("a", 3));
    q.addRecord(new SimpleRecord("a", 2));
    q.addRecord(new SimpleRecord("b", 1));
    q.addRecord(new SimpleRecord("b", 0));
    q.close();
    assertEquals(5, q.mOutput.size());
    for (int i = 0; i < 3; ++i) {
      final SimpleRecord r = q.mOutput.get(i);
      assertEquals("a", r.mRef);
      assertEquals(i + 2, r.mPos);
    }
    for (int i = 0; i < 2; ++i) {
      final SimpleRecord r = q.mOutput.get(i + 3);
      assertEquals("b", r.mRef);
      assertEquals(i, r.mPos);
    }
  }
}
