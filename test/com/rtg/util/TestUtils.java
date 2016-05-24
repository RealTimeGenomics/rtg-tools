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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.rtg.mode.DNA;
import com.rtg.mode.Protein;
import com.rtg.mode.TranslatedFrame;
import com.rtg.util.integrity.Exam.ExamException;
import com.rtg.util.integrity.IntegralAbstract;
import com.rtg.util.test.FileHelper;

import org.junit.Assert;

/**
 * Utilities useful when doing testing.
 */
public final class TestUtils {

  private TestUtils() {
  }

  /**
   * Test standard properties of an Enum.  Checks that the string
   * representation of the Enum elements matches the expected
   * representation.  Checks the ordinal values match their position
   * in the values() array.  Checks the valueOf() method functions
   * correctly.
   *
   * @param clazz the class of the Enum to test
   * @param expectedToString the expected output of the array representation of the enum.
   * @param <T> the enum type being tested
   */
  public static <T extends Enum<T>> void testEnum(final Class<T> clazz, final String expectedToString) {
    // Check toString of values
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        try {
          final Method values = clazz.getMethod("values");
          values.setAccessible(true);
          final Object[] r = (Object[]) values.invoke(null);
          Assert.assertEquals(expectedToString, Arrays.toString(r));

          final Method valueOf = clazz.getMethod("valueOf", String.class);
          valueOf.setAccessible(true);


          final Method ordinal = clazz.getMethod("ordinal");
          ordinal.setAccessible(true);

          final Method nameMethod = clazz.getMethod("name");
          nameMethod.setAccessible(true);

          // Check ordinal and valueOf
          for (int i = 0; i < r.length; i++) {
            final int oi = (Integer) ordinal.invoke(r[i]);
            Assert.assertEquals("Enum member " + r[i] + " at position " + i + " had ordinal " + oi, i, oi);
            final String name = (String) nameMethod.invoke(r[i]);
            Assert.assertEquals(r[i], valueOf.invoke(null, name));
            Assert.assertEquals(Enum.valueOf(clazz, name), r[i]);
          }
        } catch (final RuntimeException e) {
          throw e;
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    });
  }

  /**
   * Test standard properties of a PsuedoEnum.  Checks that the string
   * representation of the PsuedoEnum elements matches the expected
   * representation.  Checks the ordinal values match their position
   * in the values() array.  Checks the valueOf() method functions
   * correctly.
   *
   * @param clazz the class of the Enum to test
   * @param expectedToString the expected output of the array representation of the enum.
   * @param <T> the enum type being tested
   */
  public static <T extends PseudoEnum> void testPseudoEnum(final Class<T> clazz, final String expectedToString) {
    // Check toString of values
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        try {
          final Method values = clazz.getMethod("values");
          values.setAccessible(true);
          final Object[] r = (Object[]) values.invoke(null);
          Assert.assertEquals(expectedToString, Arrays.toString(r));

          final Method valueOf = clazz.getMethod("valueOf", String.class);
          valueOf.setAccessible(true);


          final Method ordinal = clazz.getMethod("ordinal");
          ordinal.setAccessible(true);

          final Method nameMethod = clazz.getMethod("name");
          nameMethod.setAccessible(true);

          // Check ordinal and valueOf
          for (int i = 0; i < r.length; i++) {
            final int oi = (Integer) ordinal.invoke(r[i]);
            Assert.assertEquals("Enum member " + r[i] + " at position " + i + " had ordinal " + oi, i, oi);
            final String name = (String) nameMethod.invoke(r[i]);
            Assert.assertEquals(r[i], valueOf.invoke(null, name));
          }
        } catch (final RuntimeException e) {
          throw e;
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    });
  }

  /**
   * Test equals function of a set of objects. The input is grouped.
   * All objects within the group should be equal to each other and not
   * equal to all objects outside the group. Also tests that nothing
   * equal to null.
   *
   * @param groups to be checked against each other.
   */
  public static void equalsTest(final Object[][] groups) {
    for (int i = 0; i < groups.length; i++) {
      final Object[] gi = groups[i];

      //Test nothing in this group equal to null
      for (Object aGi1 : gi) {
        Assert.assertTrue(!aGi1.equals(null));
      }

      //test everything in this group equal to each other.
      for (final Object element : gi) {
        for (final Object element2 : gi) {
          Assert.assertEquals(element, element2);
          Assert.assertEquals(element.hashCode(), element2.hashCode());
        }
      }

      //test everything in this group not equal to things in other groups
      for (int j = 0; j < groups.length; j++) {
        if (j != i) {
          final Object[] gj = groups[j];
          for (Object aGi : gi) {
            for (Object aGj : gj) {
              Assert.assertTrue(!aGi.equals(aGj));
            }
          }
        }
      }
    }
  }

  /**
   * Test equals and hashCode() function of a set of objects. The input is grouped.
   * All objects within the group should be equal to each other and not
   * equal to all objects outside the group. Also tests that nothing
   * equal to null.
   *
   * @param groups to be checked against each other.
   */
  public static void equalsHashTest(final Object[][] groups) {
    for (int i = 0; i < groups.length; i++) {
      final Object[] gi = groups[i];

      //Test nothing in this group equal to null
      for (final Object element : gi) {
        Assert.assertFalse(element.equals(null));
        Assert.assertTrue("gi=" + element.toString() + " gi.hashCode()=" + element.hashCode(), element.hashCode() != 0); //strictly neednt be true but very likely to be
      }

      //test everything in this group equal to each other.
      for (final Object element : gi) {
        for (final Object element2 : gi) {
          Assert.assertEquals(element, element2);
          Assert.assertEquals(element.hashCode(), element2.hashCode());
        }
      }

      //test everything in this group not equal to things in other groups
      for (int j = 0; j < groups.length; j++) {
        if (j != i) {
          final Object[] gj = groups[j];
          for (int k = 0; k < gi.length; k++) {
            for (int l = 0; l < gj.length; l++) {
              Assert.assertFalse("Objects should not be equals(): i=" + i + " j=" + j + " k=" + k + " l=" + l + " g[i,k]=" + gi[k] + " g[j,l]=" + gj[l], gi[k].equals(gj[l]));
              //            strictly this next neednt be true - but highly likely to be if the job has been done well
              Assert.assertTrue(gi[k].toString() + " has same hashcode as " + gj[l].toString(), gi[k].hashCode() != gj[l].hashCode());
            }
          }
        }
      }
    }
  }

  /**
   * Check that the objects in an array are equal to themselves and
   * different to all other objects in the array.
   * Also check that nothing equals null and run all integrity checks.
   * @param array being checked.
   */
  public static void equalsTest(final Object[] array) {
    for (int i = 0; i < array.length; i++) {
      final Object objectI = array[i];
      if (objectI instanceof IntegralAbstract) {
        try {
          ((IntegralAbstract) objectI).integrity();
        } catch (final ExamException e) {
          throw new RuntimeException("i=" + i + " " + objectI.toString(), e);
        }
      }
    }
    for (int i = 0; i < array.length; i++) {
      final Object objectI = array[i];
      Assert.assertEquals(objectI, objectI);
      Assert.assertFalse(objectI.equals(null));
      for (int j = 0; j < i; j++) {
        Assert.assertTrue(!array[j].equals(objectI));
      }
      for (int j = i + 1; j < array.length; j++) {
        Assert.assertTrue(i + ":" + objectI + " == " + j + ":" + array[j], !array[j].equals(objectI));
      }
    }
  }

  /**
   * Check that the objects in an array are equal to themselves and
   * different to all other objects in the array.
   * Also check that nothing equals null, run all integrity checks and check hash codes.
   * @param array being checked.
   */
  public static void equalsHashTest(final Object[] array) {
    for (int i = 0; i < array.length; i++) {
      final Object objectI = array[i];
      Assert.assertFalse(objectI.hashCode() == 0);
      if (objectI instanceof IntegralAbstract) {
        try {
          ((IntegralAbstract) objectI).integrity();
        } catch (final ExamException e) {
          throw new RuntimeException("i=" + i + " " + objectI.toString(), e);
        }
      }
    }
    for (int i = 0; i < array.length; i++) {
      final Object objectI = array[i];
      Assert.assertEquals(objectI, objectI);
      Assert.assertFalse(objectI.equals(null));
      for (int j = 0; j < i; j++) {
        Assert.assertTrue(!array[j].equals(objectI));
        Assert.assertTrue(array[j].hashCode() != objectI.hashCode());
      }
      for (int j = i + 1; j < array.length; j++) {
        Assert.assertTrue(i + ":" + objectI + " == " + j + ":" + array[j], !array[j].equals(objectI));
      }
    }
  }

  /**
   * Test the ordering of a set of objects.
   * They should be ordered strictly monotonicly in the array.
   * Also checks consistency of equals and hashCode with the ordering.
   * This is the generic version.
   * @param ator a comparator to use when doing test.
   * @param compare an array of objects which should be in strictly
   * ascending order.
   * @param checkHash if true then check that all hash codes different
   * (needn't strictily be true so may need to turn this off in tricky cases).
   * @param <T> type to compare.
   */
  public static <T> void testOrder(final Comparator<T> ator, final T[] compare, final boolean checkHash) {
    for (int i = 0; i < compare.length; i++) {
      for (int j = 0; j < i; j++) {
        //System.err.println(i+":"+j);
        Assert.assertTrue(ator + ":" + compare[j] + ":" + compare[i], ator.compare(compare[j], compare[i]) < 0);
        Assert.assertFalse(compare[j].equals(compare[i]));
        Assert.assertTrue(String.valueOf(ator.compare(compare[i], compare[j])), ator.compare(compare[i], compare[j]) > 0);
        Assert.assertFalse(compare[i].equals(compare[j]));
        if (checkHash) {
          Assert.assertFalse(compare[i].hashCode() == compare[j].hashCode());
        }
      }
      Assert.assertEquals(0, ator.compare(compare[i], compare[i]));
      Assert.assertTrue(compare[i].equals(compare[i]));
      Assert.assertFalse(compare[i].equals(null));
    }
  }

  /**
   * Test the ordering of a set of comparable objects.
   * They should be ordered strictly monotonicly in the array.
   * Also tests that equals and hashCode are consistent with the ordering.
   * @param array a set of Comparable objects which should be in strictly
   * ascending order.
   * @param checkHash if true then check that all hash codes different
   * (needn't strictily be true so may need to turn this off in tricky cases).
   * @param <T> the type being compared
   */
  public static <T extends Comparable<T>> void testOrder(final T[] array, final boolean checkHash) {
    for (int i = 0; i < array.length; i++) {
      Assert.assertEquals(0, array[i].compareTo(array[i]));
      Assert.assertFalse(array[i].equals(null));
      try {
        array[i].compareTo(null);
        Assert.fail();
      } catch (final NullPointerException e) {
        // expected
      }
      for (int j = 0; j < i; j++) {
        Assert.assertTrue(array[j].compareTo(array[i]) < 0);
        Assert.assertTrue(array[i].compareTo(array[j]) > 0);
        Assert.assertFalse(array[j].equals(array[i]));
        if (checkHash) {
          Assert.assertFalse(array[j].hashCode() == array[i].hashCode());
        }
      }
    }
  }

  /**
   * Check if two arrays are equal allowing for the tricky case when the items are themselves arrays.
   * The methods in <code>Arrays</code> don't go this far.
   * Two objects are counted as equal if they arent arrays and their equals method succeeds or if they are
   * both null.
   * If they are both arrays then they are equal if they have the same length and each individual member is equal
   * recursively according to the definition here.
   * @param a first object to be tested.
   * @param b second object to be tested.
   * @return true iff a and b are equal. (See above for details).
   */
  public static boolean equals(final Object a, final Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    if ((a instanceof Object[]) && (b instanceof Object[])) {
      final Object[] aa = (Object[]) a;
      final Object[] ba = (Object[]) b;
      if (aa.length != ba.length) {
        return false;
      }
      for (int i = 0; i < aa.length; i++) {
        if (!equals(aa[i], ba[i])) { //note recursive call
          return false;
        }
      }
      return true;
    } else {
      return a.equals(b);
    }
  }


  /**
   * Check if two arrays are equal - if not then print a message displaying them.
   * @param a first array to be checked
   * @param b second array to be checked
   */
  public static void assertEquals(final Object[] a, final Object[] b) {
    if (equals(a, b)) {
      return;
    }
    final String msg =
        "Expected: " + Arrays.toString(a) + " Actual:" + Arrays.toString(b);
    Assert.fail(msg);
  }

  /**
   * Check if two <code>int</code> arrays are equal - if not then print a message displaying them.
   * @param a first array to be checked
   * @param b second array to be checked
   */
  public static void assertEquals(final int[] a, final int[] b) {
    if (Arrays.equals(a, b)) {
      return;
    }
    final String msg =
        "Expected: " + Arrays.toString(a) + " Actual:" + Arrays.toString(b);
    Assert.fail(msg);
  }

  /**
   * Check if two <code>byte</code> arrays are equal - if not then print a message displaying them.
   * @param a first array to be checked
   * @param b second array to be checked
   */
  public static void assertEquals(final byte[] a, final byte[] b) {
    if (Arrays.equals(a, b)) {
      return;
    }
    final String msg =
      "Expected: " + Arrays.toString(a) + " Actual:" + Arrays.toString(b);
    Assert.fail(msg);
  }

  /**
   * Retrieves a field in the given object. Private fields are retrieved as well
   * as other ones.
   *
   * @param fieldName name of the field to retrieve.
   * @param instance the object to retrieve the field from.
   * @return the field value.
   * @throws RuntimeException on a reflection error. Since this is primarily a
   * testing method, there is no need to throw a checked exception.
   */
  public static Object getField(final String fieldName, final Object instance) {
    return AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        try {
          final Field f = instance.getClass().getDeclaredField(fieldName);
          f.setAccessible(true);
          return f.get(instance);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  /**
   * Retrieves a static field in the given object. Private fields are retrieved as well
   * as other ones.
   *
   * @param fieldName name of the field to retrieve.
   * @param theClass the name of the class.
   * @return the field value.
   * @throws RuntimeException on a reflection error. Since this is primarily a
   * testing method, there is no need to throw a checked exception.
   */
  public static Object getField(final String fieldName, final Class<?> theClass) {
    return AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        try {
          final Field f = theClass.getDeclaredField(fieldName);
          f.setAccessible(true);
          return f.get(null);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  /**
   * Check that a number of strings are contained in str.
   * @param str string to be checked.
   * @param subs all the items that should be in the string.
   */
  public static void containsAll(final String str, final String... subs) {
    boolean ok = true;
    final StringBuilder sb = new StringBuilder();
    for (final String sub : subs) {
      if (!str.contains(sub)) {
        sb.append("'").append(sub).append("' was not contained in:").append(str).append(StringUtils.LS);
        ok = false;
      }
      Assert.assertTrue(sb.toString(), ok);
    }
  }

  /**
   * Checks that the contents of a string match those contained in a resource.
   * @param resource the resource name
   * @param actual the actual string
   */
  public static void assertResourceEquals(final String resource, final String actual) {
    final String expected;
    try {
      expected = FileHelper.resourceToString(resource).trim();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final String exp = StringUtils.convertLineEndings(expected);
    Assert.assertEquals(exp, actual.trim());
  }

  /**
   * Test the ordering of a set of comparable objects.
   * They should be ordered strictly monotonicly in the array.
   * Each item in the sub arrays should be equal and be equivalent according to
   * <code>compareTo</code> and have equal hashCodes.
   * Also tests that equals and hashCode are consistent with the ordering.
   * Provides a more stringent test of an impelemention of <code>Comparable</code>
   * than <code>testOrder(Comparable[], boolean)</code>.
   * @param array a set of sets of Comparable objects which should be in strictly
   * ascending order.
   * @param checkHash if true then check that all hash codes different
   * (needn't strictily be true so may needto turn this off in tricky cases).
   * @param <T> Comparable type
   */
  public static <T extends Comparable<T>> void testOrder(final T[][] array, final boolean checkHash) {
    for (int i = 0; i < array.length; i++) {
      TestUtils.testEquivalent("" + i, array[i]);
      Assert.assertFalse("Array element: " + i + " is null", array[i].equals(null));
      for (int j = 0; j < i; j++) {
        TestUtils.testLess(j + ":" + i, array[j], array[i], checkHash);
      }
    }
  }

  /**
   * All members of array should be equal and <code>compareTo</code> between all pairs
   * should be 0.
   * @param message to be used when reporting errors.
   * @param array to be checked for equivalence.
   */
  private static <T extends Comparable<T>> void testEquivalent(final String message, final T[] array) {
    if (array.length == 0) {
      return;
    }
    final int hash = array[0].hashCode();
    for (int i = 0; i < array.length; i++) {
      final String msg = message + ":" + i;
      final T ai = array[i];
      Assert.assertFalse(msg, ai.equals(null));
      Assert.assertEquals(msg, hash, ai.hashCode());
      for (int j = 0; j < array.length; j++) {
        final String ms = msg + ":" + j;
        Assert.assertEquals(ms, 0, ai.compareTo(array[j]));
        Assert.assertTrue(ms, ai.equals(array[j]));
      }
    }
  }

  /**
   * All pairs of items from the arrays should be not equal and
   * if checkHash is true then their hashCodes should differ.
   * All items from a should compare less than all the items from b.
   * @param msg to be used when reporting errors.
   * @param a lesser array to be checked.
   * @param b greater array to be checked.
   * @param checkHash if true then do hashCode checks (because if the inherent unpredicatbility
   * of hashCode it may be necessary to leave this false in some cases).
   */
  private static <T extends Comparable<T>> void testLess(final String msg, final T[] a, final T[] b, final boolean checkHash) {
    for (int i = 0; i < a.length; i++) {
      final T ai = a[i];
      for (int j = 0; j < b.length; j++) {
        final T bj = b[j];
        final String m = msg + ": " + i + ":" + j + " : " + ai + " : " + bj;
        Assert.assertTrue(m, ai.compareTo(bj) < 0);
        Assert.assertTrue(m, bj.compareTo(ai) > 0);
        Assert.assertFalse(m, ai.equals(bj));
        Assert.assertFalse(m, bj.equals(ai));
        if (checkHash) {
          Assert.assertFalse(m, ai.hashCode() == bj.hashCode());
        }
      }
    }
  }

  /**
   * Take a string of LS (either platform) separated strings and split them into an array containing one string per line.
   * @param in string of lines to be split.
   * @return an array containing individual lines.
   */
  public static String[] splitLines(final String in) {
    //return StringUtils.LS.equals("\n") ? in.split("\\\n") : in.split("\\\r\\\n"); //there must be a better way
    final BufferedReader sr = new BufferedReader(new StringReader(in));
    final ArrayList<String> results = new ArrayList<>();
    String line;
    try {
      while ((line = sr.readLine()) != null) {
        results.add(line);
      }
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return results.toArray(new String[results.size()]);
  }

  /**
   * Take a string of LS separated strings and sort them and return them as a concatenated (LS separated) string.
   * @param in string of lines to be sorted.
   * @return sorted result.
   */
  public static String sortLines(final String in) {
    final String[] split = splitLines(in);
    Arrays.sort(split);
    //System.err.println(Arrays.toString(split));
    final StringBuilder sb = new StringBuilder();
    for (final String s : split) {
      sb.append(s).append(StringUtils.LS);
    }
    return sb.toString();
  }

  /**
   * Concatenates lines in input
   * @param lines an array of lines
   * @return a string with the lines in reverse order.
   */
  public static String cat(final String[] lines) {
    final StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line).append(StringUtils.LS);
    }
    return sb.toString();
  }

  /**
   * Joins whitespace-trimmed lines of input, separated by space.
   * @param input a string
   * @return a string with the lines unwrapped.
   */
  public static String unwrap(final String input) {
    final String[] lines = splitLines(input);
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append(lines[i].trim());
    }
    return sb.toString();
  }

  /**
   * Compares the lines in two strings for equivalence, ignoring line endings and whitespace.
   *
   * @param expected a string.
   * @param actual a string.
   * @param sort true iff the lines aer to be sorted before a comparison is done
   * (use it if there is non-determinism in the order of the results)
   * @return true if the lines are the same.
   */
  public static boolean sameLines(final String expected, final String actual, final boolean sort) {
    final String[] erecs = splitLines(expected.trim());
    if (sort) {
      Arrays.sort(erecs);
    }
    final String[] arecs = splitLines(actual.trim());
    if (sort) {
      Arrays.sort(arecs);
    }
    final boolean same = sameLines(erecs, arecs);
    if (!same) {
      System.err.println("Actual output was:\n" + actual);
      System.err.println("Expected output was:\n" + expected);
    }
    return same;
  }

  /**
   * Checks whether two arrays of lines are the same.
   * @param erecs expected array of lines to be checked.
   * @param arecs actual array of lines to be checked.
   * @return true iff the arrays are the same length and all  expected lines are equal to the actual lines.
   */
  public static boolean sameLines(final String[] erecs, final String[] arecs) {
    final String result = compareLines(erecs, arecs);
    if (result != null) {
      System.err.println(result);
    }
    return result == null;
  }

  /**
   * Checks whether two arrays of lines are the same.
   * @param erecs expected array of lines to be checked.
   * @param arecs actual array of lines to be checked.
   * @return null if the lines are equal, otherwise a message describing the differences.
   */
  public static String compareLines(final String[] erecs, final String[] arecs) {
    int i = 0;
    for (; i < erecs.length && i < arecs.length; i++) {
      if (!erecs[i].trim().equals(arecs[i].trim())) {
        return "" + (i + 1) + "c" + (i + 1) + StringUtils.LS
          + "< " + erecs[i].trim() + StringUtils.LS
          + "---" + StringUtils.LS
          + "> " + arecs[i].trim() + StringUtils.LS + StringUtils.LS
          + cat(arecs);
     }
    }
    if (i < erecs.length) {
      return "" + (i + 1) + "d" + (i + 1) + StringUtils.LS
        + "< " + erecs[i].trim() + StringUtils.LS;
    } else if (i < arecs.length) {
      return "" + (i + 1) + ">" + (i + 1) + StringUtils.LS
        + "> " + arecs[i].trim() + StringUtils.LS;
    }
    return null;
  }

  /**
   * Compares the lines in two strings checking that the expected lines are prefixes of the actual lines.
   *
   * @param expected a string.
   * @param actual a string.
   * @param sort true iff the lines aer to be sorted before a comparison is done
   * (use it if there is non-determinism in the order of the results)
   * @return true if ll the expected lines are prefixes of the actual lines.
   */
  public static boolean startLines(final String expected, final String actual, final boolean sort) {
    final String[] erecs = splitLines(expected.trim());
    if (sort) {
      Arrays.sort(erecs);
    }
    final String[] arecs = splitLines(actual.trim());
    if (sort) {
      Arrays.sort(arecs);
    }
    final boolean same = startLines(erecs, arecs);
    if (!same) {
      System.err.println("Actual output was:\n" + actual);
      System.err.println("Expected output was:\n" + expected);
    }
    return same;
  }

  /**
   * Checks whether the expected lines are prefixes of the actual lines.
   * @param erecs expected array of lines to be checked.
   * @param arecs another array of lines to be checked.
   * @return true iff the arrays are the same length and all expected lines are prefixes of the actual lines.
   */
  public static boolean startLines(final String[] erecs, final String[] arecs) {
    boolean same = true;
    if (erecs.length != arecs.length) {
      System.err.println("Expected " + erecs.length + " actual " + arecs.length);
      same = false;
    } else {
      for (int i = 0; i < erecs.length; i++) {
        if (!arecs[i].trim().startsWith(erecs[i].trim())) {
          System.err.println("Line " + i + StringUtils.LS + ">Expected: " + erecs[i].trim()
              + StringUtils.LS + ">actual  : " + arecs[i].trim());
          same = false;
          break;
        }
      }
    }
    return same;
  }

  /**
   * Strips the SAM header lines out of a string containing SAM output.
   *
   * @param sam the SAM output
   * @return a string which has had SAM header lines removed.
   * @exception java.io.IOException if an error occurs.
   */
  public static String stripSAMHeader(final String sam) throws IOException {
    return stripLines(sam, "@", "\n");
  }

  /**
   * Takes a SAM string and strips out header fields that typically change from run to run.
   * @param samString the full SAM string
   * @return the sanitized SAM
   */
  public static String sanitizeSAMHeader(String samString) {
    return samString.replaceAll("@PG.*\n", "")
      .replaceAll("@CO.*\n", "");
  }

  /**
   * Strips all lines starting with a character out of a string.
   *
   * @param text the textual output
   * @param prefix the prefix indicating lines to strip.
   * @param outLs the line separator to use on output.
   * @return a string which has had lines removed, and using \n as the line separator.
   * @exception java.io.IOException if an error occurs.
   */
  public static String stripLines(final String text, final String prefix, final String outLs) throws IOException {

    final StringBuilder sb = new StringBuilder();
    final BufferedReader sr = new BufferedReader(new StringReader(text));
    String line;
    while ((line = sr.readLine()) != null) {
      if (!line.startsWith(prefix)) {
        sb.append(line).append(outLs);
      }
    }
    return sb.toString();
  }

  /**
   * Stream that discards stuff written to it
   * @return the stream
   */
  public static OutputStream getNullOutputStream() {
    return NullStreamUtils.getNullOutputStream();
  }

  /**
   * Stream that discards stuff written to it
   * @return the stream
   */
  public static PrintStream getNullPrintStream() {
    return NullStreamUtils.getNullPrintStream();
  }

  /**
   * Turns dna string into protein
   * @param dna the base string
   * @return the protein string
   */
  public static String dnaToProtein(final String dna) {
    final byte[] dnaBytes = new byte[dna.length()];
    for (int i = 0; i < dna.length(); i++) {
      final byte b;
      switch (dna.charAt(i)) {
        case 'a':
        case 'A':
          b = (byte) DNA.A.ordinal();
          break;
        case 'C':
        case 'c':
          b = (byte) DNA.C.ordinal();

          break;
        case 'G':
        case 'g':
          b = (byte) DNA.G.ordinal();
          break;
        case 'T':
        case 't':
        case 'U':
        case 'u':
          b = (byte) DNA.T.ordinal();
          break;
        default:
          b = (byte) DNA.N.ordinal();
          break;
      }
      dnaBytes[i] = b;
    }
    final StringBuilder ret = new StringBuilder();
    for (int i = 0; i + 2 < dnaBytes.length; i += 3) {
      ret.append(Protein.values()[TranslatedFrame.codonToAmino(dnaBytes[i], dnaBytes[i + 1], dnaBytes[i + 2])].toString());
    }
    return ret.toString();
  }

  /**
   * Takes a VCF string and strips out header fields that typically change from run to run.
   * @param vcfString the full VCF string
   * @return the sanitized VCF
   */
  public static String sanitizeVcfHeader(String vcfString) {
    return vcfString.replace("Version", "")
      .replaceAll("##CL=.*\n", "")
      .replaceAll("##TEMPLATE-SDF-ID=.*\n", "")
      .replaceAll("##RUN-ID=.*\n", "")
      .replaceAll("##fileDate=.*\n", "")
      .replaceAll("##reference=.*\n", "")
      .replaceAll("##source=.*\n", "");
  }

  /**
   * Takes a VCF string and strips out all of the header except the CHROM line.
   * @param vcfString the full VCF string
   * @return the sanitized VCF
   */
  public static String stripVcfHeader(String vcfString) {
    return vcfString.replaceAll("##.*\n", "");
  }
}
