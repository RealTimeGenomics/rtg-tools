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
import java.util.List;

import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.model.InitializationError;

/**
 * Test suite for running all tests. Run from the command
 * line with:
 * <pre>
 *
 * java AllTests
 *
 * or
 *
 * java -Djunit.package.prefix=com.rtg.util.io AllTests (to run all tests under com.rtg.util)
 *
 * </pre>
 */
public class RtgTestEntry {
  @SuppressWarnings("unchecked")
  public static void main(final String[] args) throws ClassNotFoundException, InitializationError {
    final JUnitCore jUnitCore = new JUnitCore();
    jUnitCore.addListener(new TextListener(new RealSystem()));
    List<Result> results = new ArrayList<>();
    if (args.length > 0) {
      for (final String arg : args) {
        final Class<?> klass = ClassLoader.getSystemClassLoader().loadClass(arg);
        System.err.println(klass.getName());
        results.add(jUnitCore.run(klass));
      }
    } else {
      final Class<?>[] classes = getClasses();
      results.add(jUnitCore.run(classes));
    }
    for (Result result : results) {
      if (!result.wasSuccessful()) {
        System.exit(1);
      }
    }
  }

  public static Class<?>[] getClasses() {
    final List<Class<?>> testClasses = new ClassPathSuite().getTestClasses();
    return testClasses.toArray(new Class<?>[testClasses.size()]);
  }

}
