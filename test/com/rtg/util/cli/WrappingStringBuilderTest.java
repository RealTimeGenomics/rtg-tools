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

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class WrappingStringBuilderTest extends TestCase {

  private static final String LS = System.lineSeparator();

  public void test() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    assertEquals("", b.toString());
    assertEquals(b, b.append("hi"));
    assertEquals("hi", b.toString());
    assertEquals(b, b.append(" there peasant"));
    assertEquals("hi there peasant", b.toString());
    try {
      b.setWrapWidth(-1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("Wrap width must be positive.", e.getMessage());
    }
    b.setWrapWidth(10);
    assertEquals("hi there peasant", b.toString());
    assertEquals("hi there peasant a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  ", b.wrapText(" a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  ").toString());
    assertEquals(b, b.append('z'));
    assertEquals("hi there peasant a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  z", b.toString());
  }

  public void testInitial() {
    assertEquals("hi", new WrappingStringBuilder("hi").toString());
    assertEquals(" a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  ", new WrappingStringBuilder(" a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  ").toString());
  }

  public void testWrapText() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    b.setWrapWidth(70);
    try {
      b.wrapText("hello\nthere");
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("Input string cannot contain line breaks.", e.getMessage());
    }
    assertEquals(b, b.wrapText("hello"));
    final String s = b.wrapText(" a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  ").toString();
    assertEquals(s, "hello a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a" + LS + "a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a" + LS + "aa a a a a a a a aa  ", s);
  }

  public void testWrapText2() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    b.setWrapWidth(70);
    b.setWrapIndent("POX");
    try {
      b.wrapText("hello\nthere");
      fail();
    } catch (final IllegalArgumentException e) {
      // ok
    }
    assertEquals(b, b.wrapText("hello"));
    assertEquals(b, b.wrapText(" a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  "));
    final String s = b.toString();
    assertEquals(s, "hello a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a" + LS + "POXa a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a" + LS + "POXa a aa a a a a a a a aa  ", s);
  }

  public void testWrapText3() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    b.setWrapWidth(70);
    b.setWrapIndent(5);
    try {
      b.wrapText("hello\nthere");
      fail();
    } catch (final IllegalArgumentException e) {
      // ok
    }
    assertEquals(b, b.wrapText("hello"));
    final String s = b.wrapText(" a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a a a a aa a a a a a a a aa  ").toString();
    assertEquals(s, "hello a a a a a a a a  a a aa a a a a a a a a a a a a a a a aa a a a" + LS + "     a a a aa a a a a a a a a a a a a a a a a a aa  aa a a a a aaa a" + LS + "     a a a aa a a a a a a a aa  ", s);
  }

  public void testBorderlinePrefix() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    assertEquals(b, b.wrapText(""));
    assertEquals(b, b.wrapWord(""));
    b.setWrapWidth(40);
    b.setWrapIndent(20);
    assertEquals(b, b.wrapText("hello there my friend, squeamish ossifrage"));
    assertEquals("hello there my friend, squeamish" + LS + "                    ossifrage", b.toString());
    assertEquals(b, b.wrapText(""));
    assertEquals(b, b.wrapWord(""));
    assertEquals("hello there my friend, squeamish" + LS + "                    ossifrage", b.toString());
  }

  public void testBorderlinePrefix2() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    assertEquals(b, b.wrapText(""));
    assertEquals(b, b.wrapWord(""));
    b.setWrapWidth(40);
    b.setWrapIndent(21);
    assertEquals(b, b.wrapText("hello there my friend, squeamish ossifrage"));
    assertEquals(b, b.wrapText(""));
    assertEquals("hello there my friend, squeamish ossifrage", b.toString());
  }

  public void testBorderlinePrefix3() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    assertEquals(b, b.wrapText(""));
    assertEquals(b, b.wrapWord(""));
    b.setWrapWidth(40);
    b.setWrapIndent(21);
    assertEquals(b, b.wrapText("hello there my friend, squeamish"));
    assertEquals(b, b.wrapWord(" ossifrage"));
    assertEquals("hello there my friend, squeamish ossifrage", b.toString());
    assertEquals(b, b.wrapText(""));
    assertEquals("hello there my friend, squeamish ossifrage", b.toString());
  }

  public void testBorderlinePrefix4() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    assertEquals(b, b.wrapText(""));
    assertEquals(b, b.wrapWord(""));
    b.setWrapWidth(40);
    b.setWrapIndent(20);
    assertEquals(b, b.wrapText("hello there my friend, squeamish"));
    assertEquals(b, b.wrapWord(" ossifrage"));
    assertEquals("hello there my friend, squeamish" + LS + "                    ossifrage", b.toString());
  }

  public void testBorderlinePrefix5() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    assertEquals(b, b.wrapText(""));
    assertEquals(b, b.wrapWord(""));
    b.setWrapWidth(40);
    b.setWrapIndent(20);
    assertEquals(b, b.wrapText("hello there my friend, squeamish"));
    assertEquals(b, b.wrapWord("ossifrage"));
    assertEquals("hello there my friend, squeamish" + LS + "                    ossifrage", b.toString());
  }

  public void testException() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    try {
      b.wrapText("hello\nthere");
      fail();
    } catch (final IllegalArgumentException e) {
      // ok
    }
  }

  public void testEnd0() {
    final WrappingStringBuilder b = new WrappingStringBuilder();
    b.setWrapWidth(40);
    b.wrapText("0123456789012345678901234567890123456789");
    b.wrapText("a");
    assertEquals("0123456789012345678901234567890123456789" + LS + "a", b.toString());
  }

}
