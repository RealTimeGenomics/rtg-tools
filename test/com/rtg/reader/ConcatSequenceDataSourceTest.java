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

import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.mode.Residue;
import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.InformationEvent;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class ConcatSequenceDataSourceTest extends TestCase {
  private static final Residue A = DNA.A;
  private static final Residue C = DNA.C;
  private static final Residue G = DNA.G;
  private static final Residue T = DNA.T;
  private static final Residue N = DNA.N;

  public void testEmpty() {
    Diagnostic.setLogStream();
    try {
      assertNotNull(new ConcatSequenceDataSource<>(null, null));
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("Cannot concatenate 0 sources", e.getMessage());
    }

    try {
      assertNotNull(new ConcatSequenceDataSource<>(new ArrayList<>(), null));
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("Cannot concatenate 0 sources", e.getMessage());
    }
  }

  public void testSingle() throws Exception {
    final byte[][] data = {{(byte) A.ordinal(), (byte) C.ordinal(), (byte) G.ordinal(), (byte) T.ordinal()}, {(byte) A.ordinal(), (byte) A.ordinal(), (byte) N.ordinal()}};
    final String[] labels = {"acgt", "aan"};
    final ArrayList<SequenceDataSource> list = new ArrayList<>();
    list.add(new ArraySequenceDataSource(data, null, labels, SequenceType.DNA));
    final String[] names = {"blah", "rah", "grah" };

    final int[] infoEvents = {0};

    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void close() { }

      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        assertTrue(event instanceof InformationEvent);
        assertEquals("Processing \"blah\" (1 of 1)", event.getMessage());
        infoEvents[0]++;
      }
    };
    Diagnostic.addListener(dl);
    try {
      final ConcatSequenceDataSource<SequenceDataSource> ds = new ConcatSequenceDataSource<>(list, Arrays.asList(names));
      assertEquals(1, infoEvents[0]);
      assertEquals(0, ds.getWarningCount());
      assertTrue(ds.nextSequence());
      assertEquals(SequenceType.DNA, ds.type());

      final byte[] b = ds.sequenceData();
      assertEquals(A.ordinal(), b[0]);

      assertFalse(ds.nextSequence());

      new ConcatSequenceDataSource<>(list, null);
      assertEquals(1, infoEvents[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }

  }

  private class ArraySequenceDataSource implements SequenceDataSource {
    final byte[][] mData;
    final byte[][] mQuality;
    final String[] mLabels;
    final SequenceType mType;
    int mSequenceIndex;
    long mMinLength = Long.MAX_VALUE;
    long mMaxLength = Long.MIN_VALUE;

    ArraySequenceDataSource(byte[][] data, byte[][] quality, String[] labels, SequenceType type) {
      mData = Arrays.copyOf(data, data.length);
      mQuality = quality == null ? null : Arrays.copyOf(quality, quality.length);
      mLabels = Arrays.copyOf(labels, labels.length);
      mType = type;

      mSequenceIndex = 0;
    }

    @Override
    public SequenceType type() {
      return mType;
    }

    @Override
    public boolean nextSequence() {
      ++mSequenceIndex;
      if (mSequenceIndex < mData.length) {
        mMinLength = Math.min(mMinLength, currentLength());
        mMaxLength = Math.max(mMaxLength, currentLength());
      }
      return mSequenceIndex < mData.length;
    }

    @Override
    public String name() {
      return mLabels[mSequenceIndex];
    }

    @Override
    public byte[] sequenceData() {
      return mData[mSequenceIndex];
    }

    @Override
    public byte[] qualityData() {
      return mQuality == null ? null : mQuality[mSequenceIndex];
    }

    @Override
    public boolean hasQualityData() {
      return mQuality != null;
    }

    @Override
    public void close() {
      // nothing to do
    }

    @Override
    public void setDusting(final boolean val) {
      // ignored
    }

    @Override
    public long getWarningCount() {
      return 0;
    }

    @Override
    public int currentLength() {
      return mData[mSequenceIndex].length;
    }

    @Override
    public long getDusted() {
      return 0;
    }

    @Override
    public long getMaxLength() {
      return mMaxLength;
    }

    @Override
    public long getMinLength() {
      return mMinLength;
    }
  }
}
