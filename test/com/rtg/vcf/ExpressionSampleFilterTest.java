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

package com.rtg.vcf;

import com.rtg.util.diagnostic.NoTalkbackSlimException;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class ExpressionSampleFilterTest extends TestCase {

  public void testEq() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final ExpressionSampleFilter f = new ExpressionSampleFilter(stats, "ATTR=1");
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("ATTR", "1");
    rec.addFormatAndSample("ATTR", "2");
    assertTrue(f.acceptSample(rec, 0));
    assertFalse(f.acceptSample(rec, 1));
  }

  public void testNe() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final ExpressionSampleFilter f = new ExpressionSampleFilter(stats, "ATTR!=1");
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("ATTR", "1");
    rec.addFormatAndSample("ATTR", "2");
    assertFalse(f.acceptSample(rec, 0));
    assertTrue(f.acceptSample(rec, 1));
  }

  public void testGt() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final ExpressionSampleFilter f = new ExpressionSampleFilter(stats, "ATTR>1");
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("ATTR", "1");
    rec.addFormatAndSample("ATTR", "2");
    assertFalse(f.acceptSample(rec, 0));
    assertTrue(f.acceptSample(rec, 1));
  }

  public void testLt() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final ExpressionSampleFilter f = new ExpressionSampleFilter(stats, "ATTR<2");
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("ATTR", "1");
    rec.addFormatAndSample("ATTR", "2");
    assertTrue(f.acceptSample(rec, 0));
    assertFalse(f.acceptSample(rec, 1));
  }

  public void testGe() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final ExpressionSampleFilter f = new ExpressionSampleFilter(stats, "ATTR  >= 2");
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("ATTR", "1");
    rec.addFormatAndSample("ATTR", "2");
    assertFalse(f.acceptSample(rec, 0));
    assertTrue(f.acceptSample(rec, 1));
  }

  public void testLe() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    final ExpressionSampleFilter f = new ExpressionSampleFilter(stats, "ATTR<=1");
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("ATTR", "1");
    rec.addFormatAndSample("ATTR", "2");
    assertTrue(f.acceptSample(rec, 0));
    assertFalse(f.acceptSample(rec, 1));
  }

  public void testBadExpressions() {
    final VcfFilterStatistics stats = new VcfFilterStatistics();
    try {
      new ExpressionSampleFilter(stats, "<=1");
      fail();
    } catch (final NoTalkbackSlimException e) {
      // expected
    }
    try {
      new ExpressionSampleFilter(stats, "ATTR<!=1");
      fail();
    } catch (final NoTalkbackSlimException e) {
      // expected
    }
    try {
      new ExpressionSampleFilter(stats, "ATTR<=1.0.0");
      fail();
    } catch (final NoTalkbackSlimException e) {
      // expected
    }
    try {
      new ExpressionSampleFilter(stats, "ATTR!=");
      fail();
    } catch (final NoTalkbackSlimException e) {
      // expected
    }
    try {
      new ExpressionSampleFilter(stats, "ATTR");
      fail();
    } catch (final NoTalkbackSlimException e) {
      // expected
    }
    try {
      new ExpressionSampleFilter(stats, "");
      fail();
    } catch (final NoTalkbackSlimException e) {
      // expected
    }
  }
}
