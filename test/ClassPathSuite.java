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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rtg.util.PortableRandom;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Discover all unit tests within class path, optionally matching a package prefix
 */
public class ClassPathSuite extends TestSuite {

  private static final boolean SHUFFLE_TESTS = Boolean.getBoolean("junit.shuffle.tests");
  private static final String PACKAGE_PREFIX = System.getProperty("junit.package.prefix", "com.rtg");
  private static final String CLASSPATH = System.getProperty("java.class.path");
  private static final String CLASS_EXT = ".class";
  private static final String JAR_EXT = ".jar";

  private String mPackagePrefix = null;

  ClassPathSuite() {
    this(PACKAGE_PREFIX);
  }

  ClassPathSuite(String packagePrefix) {
    mPackagePrefix = packagePrefix;
    final Class<?>[] testClasses = getTestClasses();
    if (SHUFFLE_TESTS) { // Run test classes in random order to help detect any stray inter-test dependencies
      shuffle(testClasses);
    }
    System.err.println("Found " + testClasses.length + " test classes with package prefix \"" + mPackagePrefix + "\"");
    for (Class<?> c : testClasses) {
      //System.err.println("Adding tests from: " + c.getSimpleName());
      addTestSuite(c);
    }
  }

  /**
   * Randomize an array in place.
   * @param arr a non-null array
   * @param <T> the type of array elements.
   */
  public static <T> void shuffle(T[] arr) {
    final PortableRandom r = new PortableRandom();
    System.err.println("Shuffling tests with seed: " + r.getSeed());
    for (int i = 0; i < arr.length; i++) {
      final int z = r.nextInt(arr.length - i);
      final T t = arr[i + z];
      arr[i + z] = arr[i];
      arr[i] = t;
    }
  }

  private Class<?>[] getTestClasses() {
    final List<Class<?>> testClasses = new LinkedList<>();

    for (String each : CLASSPATH.split(File.pathSeparator)) {
      final File root = new File(each);
      if (root.isDirectory()) {
        scanDirectory(testClasses, root, root);
      } else if (root.getName().endsWith(JAR_EXT)) {
        scanZipFile(testClasses, root);
      }
    }
    final Class<?>[] result = testClasses.toArray(new Class<?>[testClasses.size()]);
    // Make the order deterministic
    Arrays.sort(result, new Comparator<Class<?>>() {
      @Override
      public int compare(Class<?> o1, Class<?> o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    return result;
  }

  private void scanDirectory(List<Class<?>> classes, File root, File cur) {
    for (File each : cur.listFiles()) {
      if (each.isDirectory()) {
        scanDirectory(classes, root, each);
      } else {
        if (each.getName().endsWith(CLASS_EXT)) {
          addIfTest(classes, getClassName(each.getPath(), root.getPath()));
        }
      }
    }
  }

  private void scanZipFile(List<Class<?>> classes, File zipname) {
    try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipname))) {
      ZipEntry each;
      while ((each = zin.getNextEntry()) != null) {
        if (each.getName().endsWith(CLASS_EXT)) {
          addIfTest(classes, getClassName(each.getName(), ""));
        }
        zin.closeEntry();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addIfTest(List<Class<?>> classes, String className) {
    if (className.endsWith("Test") && className.startsWith(mPackagePrefix)) {
      try {
        final Class<?> clazz = Class.forName(className);
        if (isTestClass(clazz)) {
          classes.add(clazz);
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static String getClassName(String fileName, String classPathRoot) {
    String className = fileName.substring(classPathRoot.length());
    if (className.charAt(0) == File.separatorChar) {
      className = className.substring(1);
    }
    return className.substring(0, className.length() - CLASS_EXT.length()).replace(File.separatorChar, '.');
  }

  private static boolean isTestClass(Class<?> clazz) {
    if (isAbstractClass(clazz)) {
      return false;
    }
    return TestCase.class.isAssignableFrom(clazz);
  }

  private static boolean isAbstractClass(Class<?> clazz) {
    return (clazz.getModifiers() & Modifier.ABSTRACT) != 0;
  }

  /**
   * @return a test suite containing all tests found on the classpath matching the prefix set via java property
   */
  public static Test suite() {
    return new ClassPathSuite();
  }

}
