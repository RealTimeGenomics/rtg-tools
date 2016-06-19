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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.rtg.util.ClassPathScanner;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 * Discover all unit tests within class path, optionally matching a package prefix
 */
public class ClassPathSuite {

  private static final String SHUFFLE_TESTS = System.getProperty("junit.shuffle.tests", "true");
  private static final String PACKAGE_PREFIX = System.getProperty("junit.package.prefix", "com.rtg");
  final List<Class<?>> mTestClasses;

  ClassPathSuite() {
    this(PACKAGE_PREFIX);
  }

  ClassPathSuite(String packagePrefix) {
    Diagnostic.setLogStream();
    final ClassPathScanner scanner = new ClassPathScanner(packagePrefix);
    final List<Class<?>> testClasses = scanner.getClasses(clazz -> clazz.getName().endsWith("Test") && isTestClass(clazz));
    if (SHUFFLE_TESTS != null) { // Run test classes in random order to help detect any stray inter-test dependencies
      shuffle(testClasses);
    }
    System.err.println("Found " + testClasses.size() + " test classes with package prefix \"" + packagePrefix + "\"");
    shuffle(testClasses);
    mTestClasses = testClasses;
  }

  List<Class<?>> getTestClasses() {
    return mTestClasses;
  }

  /**
   * Randomize an array in place.
   * @param arr a non-null array
   * @param <T> the type of array elements.
   */
  private static <T> void shuffle(List<T> arr) {
    final PortableRandom r = Boolean.valueOf(SHUFFLE_TESTS) ? new PortableRandom() : new PortableRandom(Long.parseLong(SHUFFLE_TESTS));
    System.err.println("Shuffling tests with seed: " + r.getSeed());
    for (int i = 0; i < arr.size(); i++) {
      final int z = r.nextInt(arr.size() - i);
      Collections.swap(arr, i + z, i);
    }
  }

  private static boolean isTestClass(Class<?> clazz) {
    if (isAbstractClass(clazz)) {
      return false;
    }
    if (TestCase.class.isAssignableFrom(clazz)) {
      return true;
    }
    for (Method method : clazz.getMethods()) {
      if (method.isAnnotationPresent(Test.class)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAbstractClass(Class<?> clazz) {
    return (clazz.getModifiers() & Modifier.ABSTRACT) != 0;
  }
}
