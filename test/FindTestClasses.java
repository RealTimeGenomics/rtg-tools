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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;

/**
 */
public class FindTestClasses {
  private FindTestClasses () {}

  /**
   * Returns a comma separated list of test classes that correspond to the classes specified on the command line
   * @param args
   */
  public static void main(String[] args) throws ClassNotFoundException {
    final List<String> testClasses = new ArrayList<>();
    for (String className : args) {
      final Class<?> aClass = FindTestClasses.class.getClassLoader().loadClass(className);
      final TestClass[] annotationsByType = aClass.getAnnotationsByType(TestClass.class);
      if (annotationsByType.length == 0) {
        testClasses.add(defaultTestClass(aClass));
      } else {
        for (TestClass testClass : annotationsByType) {
          Collections.addAll(testClasses, testClass.value());
        }
      }
    }
    System.out.println(StringUtils.join(",", testClasses));
  }

  /**
   * Default test class when nothing specified by annotation
   * @param aClass a target class
   * @return the default test class name
   */
  private static String defaultTestClass(Class<?> aClass) {
    Class<?> outer = aClass;
    while (outer.getEnclosingClass() != null) {
      outer = outer.getEnclosingClass();
    }
    return outer.getName().replaceAll("Abstract", "Dummy") + "Test";
  }
}
