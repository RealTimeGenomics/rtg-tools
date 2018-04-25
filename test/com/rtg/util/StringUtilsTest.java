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

import static com.rtg.util.StringUtils.LS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class StringUtilsTest extends TestCase {

  public void testExpandPrefix() {
    final TreeSet<String> names = new TreeSet<>();
    final String name = "boo";
    assertEquals(name, StringUtils.expandPrefix(names, name));
    names.add("boolean");
    assertEquals("boolean", StringUtils.expandPrefix(names, name));
    names.add("boolean2");
    assertEquals("boo", StringUtils.expandPrefix(names, name));
  }

  public void testCommas() {
    assertEquals("0", StringUtils.commas(0));
    assertEquals("-1", StringUtils.commas(-1));
    assertEquals("1", StringUtils.commas(+1));
    assertEquals("9,223,372,036,854,775,807", StringUtils.commas(Long.MAX_VALUE));
    assertEquals("-9,223,372,036,854,775,808", StringUtils.commas(Long.MIN_VALUE));
  }

  public void testTrim() {
    assertEquals("", StringUtils.trimSpaces(""));
    assertEquals("", StringUtils.trimSpaces(" "));
    assertEquals("a", StringUtils.trimSpaces("a"));
    assertEquals("a", StringUtils.trimSpaces(" a"));
    assertEquals("a", StringUtils.trimSpaces("a "));
    assertEquals("a b", StringUtils.trimSpaces("a b"));
    assertEquals("a b", StringUtils.trimSpaces(" a b "));
    assertEquals("a b" + (char) 27, StringUtils.trimSpaces(" a b" + (char) 27 + " "));
  }

  public void testTitleCase() {
    assertEquals("T", StringUtils.titleCase("t"));
    assertEquals("Cat", StringUtils.titleCase("cat"));
    assertEquals("Cat", StringUtils.titleCase("Cat"));
    assertEquals("123cat", StringUtils.titleCase("123cat"));
  }

  public void testSentencify() {
    assertEquals("T.", StringUtils.sentencify("t"));
    assertEquals("Cat.", StringUtils.sentencify("cat"));
    assertEquals("Cat.", StringUtils.sentencify("Cat."));
    assertEquals("123 cat1.", StringUtils.sentencify("123 cat1."));
    assertEquals("123 cat1!", StringUtils.sentencify("123 cat1!"));
    assertEquals("123 cat1 (maybe)", StringUtils.sentencify("123 cat1 (maybe)"));
  }

  public void test() {
    final int length = LS.length();
    assertTrue(length == 1 || length == 2);
    assertTrue(LS.equals("\n") || LS.equals("\r\n"));
  }

  public void testSpaces() {
    try {
      StringUtils.spaces(-1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("bad length", e.getMessage());
    }
    assertEquals("", StringUtils.spaces(0));
    assertEquals(" ", StringUtils.spaces(1));
    assertEquals("  ", StringUtils.spaces(2));
    assertEquals("---", StringUtils.repeat('-', 3));
    assertEquals("    ", StringUtils.spaces(4));
  }

  public void testIsJavaIdentifier() {
    assertTrue(StringUtils.isJavaIdentifier("i"));
    assertTrue(StringUtils.isJavaIdentifier("FooBar"));
    assertTrue(StringUtils.isJavaIdentifier("Foo123Bar123"));
    assertTrue(StringUtils.isJavaIdentifier("Foo_123Bar_123"));

    assertFalse(StringUtils.isJavaIdentifier(null));
    assertFalse(StringUtils.isJavaIdentifier(""));
    assertFalse(StringUtils.isJavaIdentifier(" "));
    assertFalse(StringUtils.isJavaIdentifier("  "));
    assertFalse(StringUtils.isJavaIdentifier("1Foo"));
    assertFalse(StringUtils.isJavaIdentifier("Foo%"));

  }

  private static final String LE_1 = "abcde\nhijkl\npnods\n";
  private static final String LE_2 = "abcde\r\nhijkl\r\npnods\r\n";
  private static final String EXP_LE = "abcde" + LS + "hijkl" + LS + "pnods" + LS;
  private static final String LE_3 = "abcde\n\nhijkl\n\npnods\n\n";
  private static final String LE_4 = "abcde\r\n\r\nhijkl\r\n\r\npnods\r\n\r\n";
  private static final String EXP_LE_2 = "abcde" + LS + LS + "hijkl" + LS + LS + "pnods" + LS + LS;

  public void testConvertLineEndings() {
    assertEquals(EXP_LE, StringUtils.convertLineEndings(LE_1));
    assertEquals(EXP_LE, StringUtils.convertLineEndings(LE_2));
    assertEquals(EXP_LE_2, StringUtils.convertLineEndings(LE_3));
    assertEquals(EXP_LE_2, StringUtils.convertLineEndings(LE_4));
  }

  public void testRepeat() {
    assertEquals("", StringUtils.repeat("", 0));
    assertEquals("", StringUtils.repeat("", 1));
    assertEquals("", StringUtils.repeat("AB", 0));
    assertEquals("ABABAB", StringUtils.repeat("AB", 3));
    assertEquals("", StringUtils.repeat("AB", -1));
  }

  public void testDisplay0() {
    assertEquals("\"\"", StringUtils.display(""));
  }

  public void testDisplay1() {
    assertEquals("\"Abcd 12~\"", StringUtils.display("Abcd 12~"));
  }

  public void testDisplay2() {
    assertEquals("\"\\n\"", StringUtils.display("\n"));
    assertEquals("\"\\t\"", StringUtils.display("\t"));
    assertEquals("\"\\b\"", StringUtils.display("\b"));
    assertEquals("\"\\r\"", StringUtils.display("\r"));
    assertEquals("\"\\f\"", StringUtils.display("\f"));
    assertEquals("\"\\\\\"", StringUtils.display("\\"));
    assertEquals("\"\\\'\"", StringUtils.display("\'"));
    assertEquals("\"\\\"\"", StringUtils.display("\""));
  }

  public void testDisplay3() {
    assertEquals("\"\\u0000\"", StringUtils.display("\u0000"));
    assertEquals("\"\\u001F\"", StringUtils.display("\u001F"));
    assertEquals("\" \"", StringUtils.display("\u0020"));
    assertEquals("\"~\"", StringUtils.display("\u007E"));
    assertEquals("\"\\u007F\"", StringUtils.display("\u007F"));
    assertEquals("\"\\uFFFF\"", StringUtils.display("\uFFFF"));
  }

  public void testJoin3() {
    assertEquals("-b blah -f wow omg", StringUtils.join(" ", new String[] {"-b", "blah", "-f", "wow omg"}, false));
    assertEquals("-b blah -f \"wow omg\"", StringUtils.join(" ", new String[] {"-b", "blah", "-f", "wow omg"}, true));
    assertEquals("-b blah -f wow omg", StringUtils.join(" ", new String[] {"-b", "blah", "-f", "wow omg"}));
  }

  public void testJoin2() {
    final ArrayList<String> collection = new ArrayList<>();
    collection.add("-b");
    collection.add("blah");
    collection.add("-f");
    collection.add("wow omg");
    assertEquals("-b blah -f wow omg", StringUtils.join(" ", collection));
  }

  public void testSplit() {
    assertTrue(Arrays.equals(new String[] {"a"}, StringUtils.split("a", '\t')));
    assertTrue(Arrays.equals(new String[] {"a", "b", "c"}, StringUtils.split("a\tb\tc", '\t')));
    assertTrue(Arrays.equals(new String[] {"a", "b", "c", ""}, StringUtils.split("a\tb\tc\t", '\t')));
    assertTrue(Arrays.equals(new String[] {"a", "b", "c", ""}, StringUtils.split("a\tb\tc\t", '\t', 0)));
    assertTrue(Arrays.equals(new String[] {"a\tb\tc\t"}, StringUtils.split("a\tb\tc\t", '\t', 1)));
    assertTrue(Arrays.equals(new String[] {"a", "b\tc\t"}, StringUtils.split("a\tb\tc\t", '\t', 2)));
    assertTrue(Arrays.equals(new String[] {"a", "b", "c\t"}, StringUtils.split("a\tb\tc\t", '\t', 3)));
    assertTrue(Arrays.equals(new String[] {"a", "b", "c", ""}, StringUtils.split("a\tb\tc\t", '\t', 4)));
  }

  public void testProtect() {
    assertEquals(null, StringUtils.xmlProtect(null));
    assertEquals("", StringUtils.xmlProtect(""));
    assertEquals(" ", StringUtils.xmlProtect(" "));
    assertEquals("  ", StringUtils.xmlProtect("  "));
    assertEquals("@", StringUtils.xmlProtect("@"));
    assertEquals("A", StringUtils.xmlProtect("A"));
    assertEquals("&lt;", StringUtils.xmlProtect("<"));
    assertEquals("&gt;", StringUtils.xmlProtect(">"));
    assertEquals("&amp;", StringUtils.xmlProtect("&"));
    assertEquals("&quot;", StringUtils.xmlProtect("\""));
    assertEquals("&amp;lt;", StringUtils.xmlProtect("&lt;"));
    assertEquals("&gt;&gt;", StringUtils.xmlProtect(">>"));
    assertEquals("Hello &gt;", StringUtils.xmlProtect("Hello >"));
    assertEquals("Simon &amp; Garfunkel", StringUtils.xmlProtect("Simon & Garfunkel"));
    assertEquals("&amp;M&lt;&gt;L", StringUtils.xmlProtect("&M<>L"));
    assertEquals(62, StringUtils.xmlProtect("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789").length());
  }

  public void testJoin() {
    final String glue = "glue";
    final Collection<Integer> items = Arrays.asList(1, 2, 3);
    final String expResult = "1glue2glue3";
    final String result = StringUtils.join(glue, items);
    assertEquals(expResult, result);
  }
  public void testJoinEmpty() {
    final String glue = "glue";
    final Collection<Integer> items = Collections.emptyList();
    final String expResult = "";
    final String result = StringUtils.join(glue, items);
    assertEquals(expResult, result);
  }

  public void testPadLeft() {
    assertEquals("", StringUtils.padLeft("", 0));
    assertEquals(" ", StringUtils.padLeft("", 1));
    assertEquals("x", StringUtils.padLeft("x", 1));
    assertEquals(" x", StringUtils.padLeft("x", 2));
  }

  public void testPadBetween() {
    assertEquals("a   c", StringUtils.padBetween("a", 5, "c"));
    assertEquals("abcdef", StringUtils.padBetween("abc", 5, "def"));
  }

  public void testGrep() {
    final String in =
        "asdf one"  + LS
        + "asdf two" + LS
        + "fdsa one" + LS
        + "asdf three" + LS
        + "fdsa two" + LS;
    final String result = "fdsa one" + LS + "fdsa two" + LS;
    assertEquals(result, StringUtils.grep(in, "fdsa"));
    assertEquals(result, StringUtils.grepMinusV(in, "asdf"));
  }

  public void testLeftClip() {
    assertEquals(0, StringUtils.longestPrefix());
    assertEquals(2, StringUtils.longestPrefix("hi"));
    assertEquals(2, StringUtils.longestPrefix("hi", "hi"));
    assertEquals(2, StringUtils.longestPrefix("hi", "his"));
    assertEquals(2, StringUtils.longestPrefix("hit", "his"));
    assertEquals(3, StringUtils.longestPrefix("hit", "hittter", "hits"));
    assertEquals(2, StringUtils.longestPrefix(1, "hit", "hittter", "hits"));
    assertEquals(1, StringUtils.longestPrefix(2, "hit", "hittter", "hits"));
    assertEquals(0, StringUtils.longestPrefix(3, "hit", "hittter", "hits"));
    assertEquals(0, StringUtils.longestPrefix(4, "hit", "hittter", "hits"));
  }

  public void testRightClip() {
    assertEquals(0, StringUtils.longestSuffix());
    assertEquals(2, StringUtils.longestSuffix("hi"));
    assertEquals(2, StringUtils.longestSuffix("hi", "hi"));
    assertEquals(2, StringUtils.longestSuffix("hi", "shi"));
    assertEquals(2, StringUtils.longestSuffix("thi", "shi"));
    assertEquals(3, StringUtils.longestSuffix("rethit", "hit", "rhit"));
    assertEquals(2, StringUtils.longestSuffix(1, "rethit", "hit", "rhit"));
    assertEquals(1, StringUtils.longestSuffix(2, "rethit", "hit", "rhit"));
    assertEquals(0, StringUtils.longestSuffix(3, "rethit", "hit", "rhit"));
    assertEquals(0, StringUtils.longestSuffix(4, "rethit", "hit", "rhit"));

    assertEquals(6, StringUtils.longestSuffix("acacacac", "acacac"));
    assertEquals(0, StringUtils.longestPrefix(6, "acacacac", "acacac"));
  }

  public void testBackslash() {
    assertEquals("", StringUtils.removeBackslashEscapes(""));
    assertEquals("hello\t\n\r#there\\", StringUtils.removeBackslashEscapes("hello\\t\\n\\r\\#there\\\\"));
    assertEquals("foo", StringUtils.removeBackslashEscapes("foo#bar"));
  }

  public void testDeepCopy() {
    final String s = "foo";
    final String res = StringUtils.deepCopy(s);
    assertTrue(s != res);
  }

  public void testQuoteBackslash() {
    assertEquals("\"yo \\\\mama\"", StringUtils.smartQuote("yo \\mama"));
    assertEquals("\"yo \\\"mama\"", StringUtils.smartQuote("yo \"mama"));
    assertEquals("yo\"mama", StringUtils.smartQuote("yo\"mama"));
    assertEquals("\"yo \\\\ma\\\"ma\"", StringUtils.smartQuote("yo \\ma\"ma"));
  }
}

