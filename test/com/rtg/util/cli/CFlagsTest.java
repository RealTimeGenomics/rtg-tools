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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;
import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 * Tests the CFlags class.
 *
 * @since 1.0
 */
public class CFlagsTest extends TestCase {

  protected CFlags mFlags;

  /** Used by JUnit (called after each test method) */
  @Override
  public void setUp() {
    mFlags = new CFlags();
    mFlags.setInvalidFlagHandler(null);
  }

  /** Used by JUnit (called before each test method) */
  @Override
  public void tearDown() {
    mFlags = null;
  }

  public void testRemaining() {
    mFlags.registerRequired("boolean", Boolean.class, "", "");
    assertTrue(!mFlags.setFlags("s1", "--boolean", "true", "s2", "s3"));
  }

  public void testRequired() {
    mFlags.registerRequired("boolean", Boolean.class, "a boolean value", "");
    assertTrue(!mFlags.setFlags("s1", "s2", "s3"));
  }

  public void testRepeated() {
    mFlags.registerRequired('b', "boolean", Boolean.class, "a boolean value", "");
    try {
      mFlags.registerRequired("boolean", Boolean.class, "a boolean value", "");
      fail("Should not accept registering a flag with preexisting long name");
    } catch (final IllegalArgumentException iae) {
      // Expected
    }

    try {
      mFlags.registerRequired('b', "foolean", Boolean.class, "a boolean value", "");
      fail("Should not accept registering a flag with preexisting short name");
    } catch (final IllegalArgumentException iae) {
      // Expected
    }
  }

  public void testTooMany() {
    mFlags.registerOptional("boolean", "a boolean value");
    assertTrue(mFlags.setFlags("--boolean"));
    assertTrue(!mFlags.setFlags("--boolean", "--boolean"));
    assertTrue(mFlags.setFlags("--boolean"));

    mFlags.registerRequired("int", Integer.class, "number", "an integer value");
    assertTrue(mFlags.setFlags("--int", "10"));
    assertTrue(mFlags.setFlags("--int", "10"));
    assertTrue(!mFlags.setFlags("--int", "10", "--boolean", "--int", "10"));
    assertTrue(mFlags.setFlags("--int", "10", "--boolean"));
    mFlags.registerOptional("stringy", String.class, "string", "some kind of string");
    assertTrue(mFlags.setFlags("--int", "10", "--boolean", "--stringy", "a value"));
    assertEquals("--int 10 --boolean --stringy \"a value\"", mFlags.getCommandLine());
  }

  public void testCli() {
    mFlags.registerOptional("q", "Suppresses printing error messages and prompts.");
    mFlags.registerOptional("log", "Same as q, but allow logging.");
    mFlags.registerOptional("in", String.class, "<file>",
        "Takes input from a file instead of the keyboard.");
    mFlags.registerOptional("out", String.class, "<file>",
        "Sends output to a file instead of the screen.", "default");
    mFlags
        .setDescription("For help on interactive use or script commands, type 'help' at program startup.");
    String[] args = {"--q", "--in", "a" };
    if (!mFlags.setFlags(args)) {
      fail("Shouldn't fail");
    }
    assertTrue(mFlags.isSet("q"));
    assertTrue(mFlags.isSet("in"));

    assertTrue(!mFlags.isSet("out"));
    assertEquals("default", mFlags.getValue("out"));

    assertEquals(Boolean.FALSE, mFlags.getValue("log"));
    assertEquals(Boolean.TRUE, mFlags.getValue("q"));

    args = new String[] {"--q", "--in" };
    if (mFlags.setFlags(args)) {
      fail("Should have failed.");
    }
  }

  public void testPrefixRetrieval() {
    mFlags.registerRequired("boolean", Boolean.class, "", "");
    assertNull(mFlags.getFlag("foo"));
    assertNull(mFlags.getFlag("boo"));
    assertNotNull(mFlags.getFlag("boolean"));

    assertNull(mFlags.getFlagWithExpansion("foo"));
    assertNotNull(mFlags.getFlagWithExpansion("boo"));
    assertNotNull(mFlags.getFlagWithExpansion("boolean"));

    assertTrue(mFlags.setFlags("--boo", "true"));

    mFlags.registerRequired("boolean2", Boolean.class, "", "");

    assertNull(mFlags.getFlag("boo"));
    assertNotNull(mFlags.getFlag("boolean"));

    assertNull(mFlags.getFlagWithExpansion("boo"));
    assertNotNull(mFlags.getFlagWithExpansion("boolean"));

    assertFalse(mFlags.setFlags("--boo", "true"));
  }

  public void testSetArgsWithStrings() {
    mFlags.registerRequired("boolean", Boolean.class, "", "");
    mFlags.registerRequired("byte", Byte.class, "", "");
    mFlags.registerRequired("short", Short.class, "", "");
    mFlags.registerRequired("char", Character.class, "", "");

    mFlags.registerRequired("int", Integer.class, "", "");
    mFlags.registerRequired("float", Float.class, "", "");
    mFlags.registerOptional("long", Long.class, "", "");
    mFlags.registerRequired("double", Double.class, "", "");

    mFlags.registerOptional("file", File.class, "", "");

    assertTrue(mFlags.setFlags("--boolean", "true", "--byte", "10", "--short", "127",
        "--char", "c", "--int", "3", "--float", "4.6", "--long", "64234", "--file", "afilename",
        "--double", "64324.234"));

    assertTrue(mFlags.isSet("boolean"));
    assertTrue(mFlags.getValue("boolean").equals(Boolean.TRUE));

    assertTrue(mFlags.isSet("byte"));
    assertTrue(mFlags.getValue("byte").equals(Byte.valueOf("10")));

    assertTrue(mFlags.isSet("short"));
    assertTrue(mFlags.getValue("short").equals(Short.valueOf("127")));

    assertTrue(mFlags.isSet("char"));
    assertTrue(mFlags.getValue("char").equals('c'));

    assertTrue(mFlags.isSet("int"));
    assertTrue(mFlags.getValue("int").equals(Integer.valueOf("3")));

    assertTrue(mFlags.isSet("float"));
    assertTrue(mFlags.getValue("float").equals(Float.valueOf("4.6")));

    assertTrue(mFlags.isSet("long"));
    assertTrue(mFlags.getValue("long").equals(Long.valueOf("64234")));

    assertTrue(mFlags.isSet("double"));
    assertTrue(mFlags.getValue("double").equals(Double.valueOf("64324.234")));

    assertTrue(mFlags.isSet("file"));
    assertTrue(mFlags.getValue("file").equals(new File("afilename")));

    // getOptional and getRequired
    final Collection<Flag> optional = mFlags.getOptional();
    assertNotNull(optional);
    assertTrue(4 == optional.size()); // always has help / XXhelp
    assertFalse(optional.contains(mFlags.getFlag("boolean")));
    assertTrue(optional.contains(mFlags.getFlag("help"))); // always has help
    assertTrue(optional.contains(mFlags.getFlag("long")));
    assertTrue(optional.contains(mFlags.getFlag("file")));

    final Collection<Flag> required = mFlags.getRequired();
    assertNotNull(required);
    assertTrue(7 == required.size());
    assertFalse(required.contains(mFlags.getFlag("long")));
    assertTrue(required.contains(mFlags.getFlag("boolean")));
    assertTrue(required.contains(mFlags.getFlag("byte")));
    assertTrue(required.contains(mFlags.getFlag("short")));
    assertTrue(required.contains(mFlags.getFlag("char")));
    assertTrue(required.contains(mFlags.getFlag("int")));
    assertTrue(required.contains(mFlags.getFlag("float")));
    assertTrue(required.contains(mFlags.getFlag("double")));

    // getType
    assertEquals(Boolean.class, mFlags.getFlag("boolean").getParameterType());
    assertEquals(Byte.class, mFlags.getFlag("byte").getParameterType());
    assertEquals(Short.class, mFlags.getFlag("short").getParameterType());
    assertEquals(Character.class, mFlags.getFlag("char").getParameterType());
    assertEquals(Integer.class, mFlags.getFlag("int").getParameterType());
    assertEquals(Long.class, mFlags.getFlag("long").getParameterType());
    assertEquals(Float.class, mFlags.getFlag("float").getParameterType());
    assertEquals(Double.class, mFlags.getFlag("double").getParameterType());
    assertEquals(File.class, mFlags.getFlag("file").getParameterType());
  }

  //Hard to convert test. does not improve jumble score. Potentially does nothing
  //public void testGetSet() {
  //  BeanPropertyTester bps = new BeanPropertyTester(mFlags);
  //  bps.testProperties();
  //}

  private static final String LS = System.lineSeparator();

  public void testHelpFlag() {
    final CFlags f = new CFlags();
    final Flag x = f.getFlag("help");
    assertNotNull(x);
    assertEquals(Character.valueOf('h'), x.getChar());
    assertEquals("print help on command-line flag usage", x.getDescription());
  }

  public void testVariousKinds() {
    Flag f = mFlags.registerOptional("aa", "bb");
    assertNotNull(f);
    assertEquals("bb", f.getDescription());
    f = mFlags.registerOptional('v', "cc", "dd");
    assertNotNull(f);
    assertEquals("dd", f.getDescription());
    f = mFlags.registerRequired(File.class, "hi", "there");
    assertNotNull(f);
    f = mFlags.registerRequired("zz", File.class, "hix", "therex");
    assertNotNull(f);
    f = mFlags.registerRequired('j', "zzz", File.class, "hiz", "therez");
    assertNotNull(f);
    f = mFlags.registerOptional("zzr", File.class, "him", "therem");
    assertNotNull(f);
    // Nice long description that wraps around. However, when run on
    // a regular user terminal, this may fail if the wrapping occurs
    // at a different width.
    f = mFlags.registerOptional('k', "zzx", File.class, "hih hih hih",
        "thereh thereh thereh thereh thereh thereh thereh thereh thereh thereh thereh thereh");
    assertNotNull(f);
    f = mFlags.registerOptional('l', "zzy", File.class, "hio", "thereo", null);
    assertNotNull(f);
    assertFalse(mFlags.setFlags("-l", "pox", "-j", "pox2"));
    assertEquals("You must provide values for --zz HIX HI", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("-l", "pox", "-j", "pox2", "-zz", "dog"));
    assertEquals("Unknown flag -zz", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("-l", "pox", "-j", "pox2", "--zz", "dog"));
    assertEquals("You must provide a value for HI", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("-l", "pox", "-j", "pox2", "--zz", "dog", "cat"));
    assertEquals("", mFlags.getParseMessage());
    TestUtils.containsAll(mFlags.getUsageString(), "Required flags: " + LS + "      --zz=HIX          therex" + LS
        + "  -j, --zzz=HIZ         therez" + LS + "      HI                there" + LS + LS
        + "Optional flags: " + LS + "      --aa              bb" + LS
        + "  -v, --cc              dd" + LS
        + "  -h, --help            print help on command-line flag usage" + LS
        + "      --zzr=HIM         therem",
        "  -k, --zzx=HIH HIH HIH thereh thereh thereh thereh thereh thereh",
        "  -l, --zzy=HIO         thereo" + LS);
    assertEquals("cat", mFlags.getAnonymousValue(0).toString());
    try {
      mFlags.getAnonymousValue(1);
    } catch (final IndexOutOfBoundsException e) {
      // ok
    }
    assertEquals(1, mFlags.getAnonymousValues(0).size());
    assertTrue(mFlags.setFlags("-l", "pox", "-j", "pox2=r", "--zz", "dog", "cat"));
    assertEquals("", mFlags.getParseMessage());
    mFlags.setDescription("flunky test");
    mFlags.setName("dogbreath");
    mFlags.setRemainderHeader("%%");
    TestUtils.containsAll(mFlags.getUsageString(), "Usage: dogbreath [OPTION]... --zz HIX -j HIZ HI %%" + LS + LS
        + "flunky test" + LS + LS
        + "Required flags: " + LS + "      --zz=HIX          therex" + LS
        + "  -j, --zzz=HIZ         therez" + LS + "      HI                there" + LS + LS
        + "Optional flags: " + LS + "      --aa              bb" + LS
        + "  -v, --cc              dd" + LS
        + "  -h, --help            print help on command-line flag usage" + LS
        + "      --zzr=HIM         therem" + LS
        + "  -k, --zzx=HIH HIH HIH thereh thereh thereh thereh thereh thereh",
        "  -l, --zzy=HIO         thereo" + LS);
    assertEquals("[OPTION]... --zz HIX -j HIZ HI", mFlags.getCompactFlagUsage());
    assertFalse(mFlags.setFlags("-l", "pox", "--help", "-j", "pox2=r", "--zz", "dog",
        "cat"));
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    assertFalse(mFlags.setFlags("-l", "pox", "-h", "-j", "pox2=r", "--zz", "dog",
        "cat"));
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    assertFalse(mFlags.setFlags("-l", "pox", "-j", "pox2=r", "--zz", "dog", "cat",
        "-h"));
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    assertFalse(mFlags.setFlags("-l", "pox", "-j", "pox2=r", "--zz", "dog", "cat",
        "--help"));
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
  }

  public void testInvalid() {
    mFlags.setValidator(new Validator() {
      @Override
      public boolean isValid(final CFlags f) {
        return false;
      }
    });
    assertFalse(mFlags.setFlags());
    mFlags.registerRequired("zz", Integer.class, "hix", "therex");
    assertFalse(mFlags.setFlags());
  }

  public void testMultiValue() {
    final Flag f = new Flag('x', "xx", "mv", 3, 4, Integer.class, "kk", 42, "try");
    mFlags.register(f);
    assertEquals("try", f.getCategory());
    assertFalse(mFlags.isSet("xx"));
    assertFalse(mFlags.setFlags("-x", "7"));
    assertEquals("You must provide a value for -x KK (2 more times)", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("--xx", "7"));
    assertEquals("You must provide a value for -x KK (2 more times)", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("-x", "7", "-x", "8"));
    assertEquals("You must provide a value for -x KK (1 more time)", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("-x", "7", "-x", "8", "-x", "9"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("-x", "7", "-x", "8", "-x", "9", "-x", "10"));
    assertEquals("", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("-x", "7", "-x", "8", "-x", "9", "-x", "10", "-x",
        "11"));
    assertEquals("Attempt to set flag -x too many times.", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("-x", "7", "-x", "8", "--xx", "9", "-x", "10"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("-x", "7", "-x", "8", "--xx=9", "-x", "10"));
    assertEquals("", mFlags.getParseMessage());
    assertNotNull(mFlags.getValues("xx"));
    assertNotNull(mFlags.getReceivedValues());
    assertTrue(mFlags.isSet("xx"));
    assertFalse(mFlags.isSet("yy"));
    assertFalse(mFlags.setFlags("-x", "7", "-x", "8", "--xx", "9", "-x", "10", "-x",
        "11"));
    assertEquals("Attempt to set flag -x too many times.", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("-x", "7", "-x", "8", "--xx", "9", "-x", "10", "-x",
        "11", "-h", "--helx"));
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    assertFalse(mFlags.setFlags("-x", "7", "-x", "8", "--xx", "9", "-x", "10", "-x",
        "11", "-o", "--help"));
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
  }

  public void testParseInt() {
    final Flag f = new AnonymousFlag("mv", Integer.class, "kk");
    mFlags.register(f);
    assertTrue(mFlags.setFlags("09"));
    assertEquals(9, mFlags.getAnonymousValue(0));
  }
  public void testMultiValueAnon() {
    final Flag f = new AnonymousFlag("mv", Integer.class, "kk");
    f.setMaxCount(4);
    f.setMinCount(3);
    mFlags.register(f);
    assertFalse(mFlags.setFlags("7"));
    assertEquals("You must provide a value for KK+ (2 more times)", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("7", "8"));
    assertEquals("You must provide a value for KK+ (1 more time)", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("7", "8", "9"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("7", "8", "9", "10"));
    assertEquals("", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("7", "8", "9", "10", "11"));
    assertEquals("Unexpected argument \"11\"", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("7", "8", "9", "10", "-h"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    assertFalse(mFlags.setFlags("7", "8", "9", "10", "--help"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    assertFalse(mFlags.setFlags("7", "8", "9", "10", "11", "--help"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.isSet(CFlags.HELP_FLAG));
    mFlags.setInvalidFlagHandler(new InvalidFlagHandler() {
      @Override
      public void handleInvalidFlags(final CFlags ff) {
        throw new ArithmeticException();
      }
    });
    try {
      mFlags.setFlags("7", "8", "9", "10", "11");
      fail();
    } catch (final ArithmeticException e) {
      // ok
    }
    assertTrue(mFlags.setFlags("7", "8", "9", "10"));
    assertNotNull(mFlags.getAnonymousFlags());
  }

  public void testRequired2() {
    Flag f = mFlags.registerRequired("zz", File.class, "hix", "therex");
    assertNotNull(f);
    f = mFlags.registerRequired('j', "zzz", File.class, "hiz", "therez");
    assertNotNull(f);
    assertFalse(mFlags.setFlags());
    assertEquals("You must provide values for --zz HIX -j HIZ", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("--zz", "h"));
    assertEquals("You must provide a value for -j HIZ", mFlags.getParseMessage());
//    assertFalse(mFlags.setFlags("--zz", "h", "-j", "test\nthis"));
//    assertEquals("Invalid value \"test\nthis\" for \"-j\". Value cannot contain new line characters.", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("--zz", "h", "t"));
    assertEquals("Unexpected argument \"t\"", mFlags.getParseMessage());
    assertFalse(mFlags.setFlags("--zz", "h", "t", "k"));
    assertEquals("Unexpected arguments \"t\" \"k\"", mFlags.getParseMessage());

    assertFalse(mFlags.setFlags("--uu", "-h"));
    assertFalse(mFlags.setFlags("--zz", "-h"));
    assertEquals("You must provide a value for -j HIZ", mFlags.getParseMessage());
    try {
      mFlags.setFlags("--zz", null);
      fail();
    } catch (final NullPointerException e) {
      // ok
    }
  }

  public void testRange() {
    final Flag f = new Flag('x', "xx", "mv", 1, 1, String.class, "kk", null, "");
    final HashSet<String> m = new HashSet<>();
    try {
      f.setParameterRange(m);
      fail();
    } catch (final IllegalArgumentException e) {
      // ok
    }
    m.add("value");
    f.setParameterRange(m);
    m.add("pox");
    f.setParameterRange(m);
    mFlags.register(f);
    assertFalse(mFlags.setFlags("--xx", "v"));
    assertEquals("Invalid value \"v\" for \"--xx\". Value supplied is not in the set of allowed values.",
        mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("--xx", "value"));
    assertEquals("", mFlags.getParseMessage());
    assertTrue(mFlags.setFlags("-x", "pox"));
    assertEquals("", mFlags.getParseMessage());
  }

  public void testCrossFlagChecks() {
    mFlags.registerOptional("a", "therex");
    mFlags.registerOptional("b", "therex");
    mFlags.registerOptional("c", "therex");
    mFlags.registerOptional("d", "therex");

    mFlags.setFlags("--a", "--b");

    assertTrue(mFlags.isSet("a"));
    assertTrue(mFlags.isSet("b"));

    // True if any one is set
    assertTrue(mFlags.checkOr("a", "b"));
    assertTrue(mFlags.checkOr("b", "c", "d"));
    assertFalse(mFlags.checkOr("c", "d"));

    // True if at most one is set
    assertTrue(mFlags.checkNand("a", "d"));
    assertTrue(mFlags.checkNand("c", "d"));
    assertFalse(mFlags.checkNand("a", "b"));
    assertTrue(mFlags.checkAtMostOne("a", "d"));
    assertTrue(mFlags.checkAtMostOne("c", "d"));
    assertFalse(mFlags.checkAtMostOne("a", "b"));
    assertFalse(mFlags.checkAtMostOne("a", "b", "c", "d"));

    // True if all are set
    assertTrue(mFlags.checkRequired("a", "b"));
    assertFalse(mFlags.checkRequired("a", "d"));

    // True if exactly one is set
    assertTrue(mFlags.checkXor("b", "c", "d"));
    assertFalse(mFlags.checkXor("a", "b"));
    assertFalse(mFlags.checkXor("c", "d"));

    // True if neither or both are
    assertTrue(mFlags.checkIff("a", "b"));
    assertTrue(mFlags.checkIff("c", "d"));
    assertFalse(mFlags.checkIff("a", "c"));

  }

  public void testSpecialAnonProps() {
    final AnonymousFlag f1 = new AnonymousFlag("mv", Integer.class, "kk");
    final AnonymousFlag f2 = new AnonymousFlag("mx", Integer.class, "zz");
    assertEquals(1, f2.compareTo(f1));
    assertEquals(-1, f1.compareTo(f2));
    assertEquals(0, f1.compareTo(f1));
  }

  public void testFlagInnerClass() {
    Flag f = new Flag('x', "xx", "mv", 0, 1, Integer.class, "kk", 42, "");
    try {
      f.setMaxCount(0);
      fail();
    } catch (final IllegalArgumentException e) {
      // ok
    }
    f.setMaxCount(2);
    f.setMinCount(2);
    try {
      f.setMaxCount(1);
      fail();
    } catch (final IllegalArgumentException e) {
      // ok
    }
    Collection<Object> c = f.getValues();
    assertTrue(c.contains(42));
    f = new Flag('x', "xx", "mv", 3, 4, null, "kk", 42, "");
    c = f.getValues();
    assertTrue(c.contains(Boolean.FALSE));
    final Flag f1 = new Flag('x', "xx", "mv", 3, 4, Integer.class, "kk", 42, "");
    final Flag f2 = new Flag('y', "xy", "my", 3, 4, Integer.class, "ky", 42, "");
    assertEquals(1, f2.compareTo(f1));
    assertEquals(-1, f1.compareTo(f2));
    assertEquals(0, f1.compareTo(f1));
    f = new Flag('x', "xx", "mv", 0, Integer.MAX_VALUE, Integer.class, "kk", 42, "");
    mFlags.register(f);
    assertEquals(LS + "Optional flags: " + LS
        + "  -h, --help  print help on command-line flag usage" + LS
        + "  -x, --xx=KK mv. May be specified 0 or more times (Default is 42)" + LS, mFlags
        .getUsageString());
    mFlags.setFlags("-x", "22");
    final List<FlagValue> i = mFlags.getReceivedValues();
    assertEquals(1, i.size());
    final FlagValue fv = i.get(0);
    assertNotNull(fv);
    assertEquals(f, fv.getFlag());
    assertEquals(22, fv.getValue());
    assertEquals("xx=22", fv.toString());
  }

  static final class TestMyEnum implements PseudoEnum {
    public static final TestMyEnum ONE = new TestMyEnum(0, "ONE");
    public static final TestMyEnum TWO = new TestMyEnum(1, "TWO");
    public static final TestMyEnum THREE = new TestMyEnum(2, "THREE");

    private final int mOrdinal;
    private final String mName;
    private TestMyEnum(final int ordinal, final String name) {
      mOrdinal = ordinal;
      mName = name;
    }
    @Override
    public String name() {
      return mName;
    }
    @Override
    public int ordinal() {
      return mOrdinal;
    }
    @Override
    public String toString() {
      return mName;
    }

    private static final EnumHelper<TestMyEnum> ENUM_HELPER = new EnumHelper<>(TestMyEnum.class, new TestMyEnum[] {ONE, TWO, THREE});
    public static TestMyEnum[] values() {
      return ENUM_HELPER.values();
    }
    public static TestMyEnum valueOf(final String str) {
      return ENUM_HELPER.valueOf(str);
    }
  }

  public void testEnum() {
    mFlags.registerRequired(TestMyEnum.class, "enum", "ENUM VALUE");

    assertTrue(mFlags.setFlags("one"));
    assertEquals(TestMyEnum.ONE, mFlags.getAnonymousValue(0));
  }

  public void testEnumDescription() {
    mFlags.registerRequired(TestMyEnum.class, "enum", "ENUM VALUE");

    final String str = mFlags.getUsageString();

    assertTrue(str, str.toLowerCase(Locale.getDefault()).contains("must be one of [one, two, three]"));
  }

  public void testEnum2() {
    mFlags.registerRequired(TestMyEnum.class, "enum", "enum value");
    assertFalse(mFlags.setFlags("nonvalue"));
  }

  public void testTypeChecking() {
    mFlags.registerRequired(Integer.class, "int", "");
    assertFalse(mFlags.setFlags("abc123"));
  }

  public void testInvalidFlagHandler() {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bos);
    final ByteArrayOutputStream berr = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(berr);
    final CFlags f = new CFlags("", out, err);
    f.setFlags("-h");
    err.flush();
    out.flush();
    assertEquals("Usage:  [OPTION]..." + LS + LS + "Optional flags: " + LS + "  -h, --help print help on command-line flag usage" + LS + LS + "", bos.toString());
    assertEquals("", berr.toString());
    bos.reset();
    f.setFlags("--no-such-flag");
    err.flush();
    assertEquals("Error: Unknown flag --no-such-flag" + LS + LS + "Usage:  [OPTION]..." + LS + LS + "Try \'--help\' for more information" + LS, berr.toString());
    assertEquals("", bos.toString());
  }

  public void testXHelp() {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(bos);
    final CFlags f = new CFlags("", err, null);
    f.registerExtendedHelp();
    f.registerOptional("Xoptional", "Test of --X options");
    f.setFlags("-h");
    err.flush();
    assertEquals("Usage:  [OPTION]..." + LS + LS + "Optional flags: " + LS
        + "  -h, --help print help on command-line flag usage" + LS
        + LS + "", bos.toString());

    bos.reset();
    f.setFlags("--Xhelp");
    err.flush();
    TestUtils.containsAll(bos.toString().replaceAll("\\s+", " "),
      "Usage: [OPTION]...",
      "Use them with caution",
      "Optional flags: ",
      " --Xhelp print help on extended command-line flag usage",
      " --Xoptional Test of --X options");
  }

  public void testUnregister() {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(bos);
    final CFlags flags = new CFlags("", err, null);

    flags.registerOptional('a', "A", "a");
    Flag f = flags.unregister("A");
    assertNotNull(f);

    f = flags.unregister("B");
    assertNull(f);
  }

  public void testXor() {
    mFlags.registerRequired(TestMyEnum.class, "enum", "ENUM VALUE");

    assertTrue(mFlags.setFlags("one"));
    assertEquals(TestMyEnum.ONE, mFlags.getAnonymousValue(0));
  }

}

