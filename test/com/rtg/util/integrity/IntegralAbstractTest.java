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
package com.rtg.util.integrity;

import static com.rtg.util.StringUtils.LS;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

/**
 * test class
 *
 *
 */
public class IntegralAbstractTest extends TestCase {

  class IntegralAbstractImpl extends IntegralAbstract {

    @Override
    public void toString(final StringBuilder sb) {
      sb.append("IntegralAbstractImpl");
    }

    @Override
    public boolean integrity() {
      return true;
    }

  }

  public void test() {
    final IntegralAbstractImpl imp = new IntegralAbstractImpl();
    assertTrue(imp.globalIntegrity());

    assertEquals("IntegralAbstractImpl", imp.toString());
  }


  protected static class Mock extends IntegralAbstract {
    protected static final int STATIC_INT = 0; //shouldn't appear in output
    protected static final Mock2 STATIC_MOCK_2 = new Mock2(); //shouldn't appear in output
    protected final Integer mIntegerNull = null;
    protected final Mock2 mMock2Null = null;
    protected double mDouble = 42.3;
    protected float mFloat = 21.15f;
    protected boolean mBoolean = true;
    protected long mLong = -123L;
    protected int mInt = 24;
    protected short mShort = 12;
    protected char mChar = 'X';
    protected byte mByte = 10;
    protected final Integer mIInteger = 45;
    protected final Mock1 mMock = new Mock1();
    protected final Mock1 mMock4 = new Mock4();
    protected final int[] mInts = {0, 11, 22, 33};
    protected final double[] mDoubles = {0.1, 11.1, 22.1, 33.1};
    protected final float[] mFloats = {0.1f, 11.1f, 22.1f, 33.1f};
    protected final double[] mLongDoubles = new double[20];
    protected final Integer[] mIIntegers = {1, 2, 3};
    protected final Mock1[] mMocks = new Mock1[] {new Mock1()};
    protected final List<Integer> mListInt = new LinkedList<>();
    protected final List<double[]> mListDblArr = new LinkedList<>();
    protected final List<Integer> mListEmpty = new LinkedList<>();

    public Mock() {
      mListInt.add(42);
      mListInt.add(101);

      mListDblArr.add(new double[] {1.0, 2.0});


    }
    @Override
    public boolean integrity() {
      return true;
    }
  }

  public void test1() {
    final IntegralAbstract sim = new Mock();
    assertTrue(sim.integrity());
    assertTrue(sim.globalIntegrity());
    final String str = sim.toString();
    final String exp = ""
        + "Mock mDouble=42.3000 mFloat=21.1500 mBoolean=" + true + " mLong=-123 mInt=24 mShort=12 mChar='X' mByte=10" + LS
        + "mIntegerNull:" + LS
        + "mMock2Null:" + LS
        + "mIInteger:45" + LS
        + "mMock:Mock1" + LS
        + "mMock4:Mock4" + LS
        + "mInts:[4]" + LS
        + "[0] 0, 11, 22, 33" + LS
        + "mDoubles:[4]" + LS
        + "[0] 0.1000, 11.1000, 22.1000, 33.1000" + LS
        + "mFloats:[4]" + LS
        + "[0] 0.1000, 11.1000, 22.1000, 33.1000" + LS
        + "mLongDoubles:[20]" + LS
        + "[0] 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000" + LS
        + "[10] 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000" + LS
        + "mIIntegers:[3]" + LS
        + "[0] 1" + LS
        + "[1] 2" + LS
        + "[2] 3" + LS
        + "mMocks:[1]" + LS
        + "[0] Mock1" + LS
        + "mListInt:" + LS
        + "{" + LS
        + " 42" + LS
        + " 101" + LS
        + "}" + LS
        + "mListDblArr:" + LS
        + "{" + LS
        + " double[]" + LS
        + "  [2]" + LS
        + "  [0] 1.0000, 2.0000" + LS
        + "}" + LS
        + "mListEmpty:" + LS
        + "{" + LS
        + "}" + LS
        ;
    assertEquals(exp, str);
  }

  public void testInteger() {
    final String str = IntegralAbstract.toString(42);
    assertEquals("42", str);
  }

  protected static class MockSub extends Mock {
    protected final int mSubInt = 144;
    protected final Integer mSubInteger = 143;
  }

  public void test2() {
    final IntegralAbstract sim = new MockSub();
    assertTrue(sim.integrity());
    assertTrue(sim.globalIntegrity());
    final String str = sim.toString();
    final String exp = ""
        + "MockSub mSubInt=144 mDouble=42.3000 mFloat=21.1500 mBoolean=" + true + " mLong=-123 mInt=24 mShort=12 mChar='X' mByte=10" + LS
        + "mSubInteger:143" + LS
        + "mIntegerNull:" + LS
        + "mMock2Null:" + LS
        + "mIInteger:45" + LS
        + "mMock:Mock1" + LS
        + "mMock4:Mock4" + LS
        + "mInts:[4]" + LS
        + "[0] 0, 11, 22, 33" + LS
        + "mDoubles:[4]" + LS
        + "[0] 0.1000, 11.1000, 22.1000, 33.1000" + LS
        + "mFloats:[4]" + LS
        + "[0] 0.1000, 11.1000, 22.1000, 33.1000" + LS
        + "mLongDoubles:[20]" + LS
        + "[0] 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000" + LS
        + "[10] 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000, 0.0000" + LS
        + "mIIntegers:[3]" + LS
        + "[0] 1" + LS
        + "[1] 2" + LS
        + "[2] 3" + LS
        + "mMocks:[1]" + LS
        + "[0] Mock1" + LS
        + "mListInt:" + LS
        + "{" + LS
        + " 42" + LS
        + " 101" + LS
        + "}" + LS
        + "mListDblArr:" + LS
        + "{" + LS
        + " double[]" + LS
        + "  [2]" + LS
        + "  [0] 1.0000, 2.0000" + LS
        + "}" + LS
        + "mListEmpty:" + LS
        + "{" + LS
        + "}" + LS
        ;
    assertEquals(exp, str);
  }

  protected static class Mock1 {
    @Override
    public String toString() {
      return "Mock1";
    }
  }

  static class SinglePrimitive extends IntegralAbstract {
    protected int mInt = 33;
    @Override
    public boolean integrity() {
      return true;
    }
  }

  public void testSinglePrimitive() {
    assertEquals("SinglePrimitive mInt=33" + LS, new SinglePrimitive().toString());
  }

  static class SingleObject extends IntegralAbstract {
    protected final Integer mInt = 33;
    @Override
    public boolean integrity() {
      return true;
    }
  }
  public void testSingleObject() {
    assertEquals("SingleObject" + LS + "mInt:33" + LS, new SingleObject().toString());
  }


  protected static class Mock2 extends Mock1 {
  }

  protected static class Mock3 {
  }

  protected static class Mock4 extends Mock1 {
    @Override
    public String toString() {
      return "Mock4";
    }
  }

  public void testMultiDimensionArrays() {
    final int[][] a = {null, {1, 2, 3}};
    final String str = IntegralAbstract.toString(a);
    //System.err.println(str);
    final String exp = ""
        + "int[][]" + LS
        + "[2]" + LS
        + "[0] null" + LS
        + "[1] int[]" + LS
        + "  [3]" + LS
        + "  [0] 1, 2, 3"
        ;
    assertEquals(exp, str);
  }

  public void testToStringDeclared() {
    assertTrue(IntegralAbstract.toStringDeclared(Integer.class));
    assertTrue(IntegralAbstract.toStringDeclared(Mock1.class));
    assertTrue(IntegralAbstract.toStringDeclared(Mock2.class));
    assertTrue(IntegralAbstract.toStringDeclared(Mock4.class));

    assertFalse(IntegralAbstract.toStringDeclared(Object.class));
    assertFalse(IntegralAbstract.toStringDeclared(Mock3.class));
  }
}
