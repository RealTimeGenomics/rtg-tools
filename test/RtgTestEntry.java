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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
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

  private static final boolean COLLECT_TIMINGS = Boolean.valueOf(System.getProperty("junit.timing", "false"));
  private static final int TESTS_PER_ROW = 120;
  private static final long LONG_TEST_THRESHOLD = 200;
  private static final int TIMING_WIDTH = 120;

  private static class TimingListener extends RunListener {
    private final Map<String, Long> mTimings = new HashMap<>();
    private long mStart;
    private String mTestName = null;
    @Override
    public void testStarted(Description description) throws Exception {
      mStart = System.currentTimeMillis();
      mTestName = testName(description);
    }

    private String testName(Description description) {
      return description.getClassName() + "." + description.getMethodName();
    }

    @Override
    public void testFinished(Description description) throws Exception {
      final long end = System.currentTimeMillis();
      if (mTestName.equals(testName(description))) {
        mTimings.put(testName(description), end - mStart);
      } else {
        System.err.println("Can't time " + mTestName);
      }
    }

    static String timingFormatter(String name, long time, int width) {
      return name + StringUtils.leftPad(String.valueOf(time), Math.max(0, width - name.length()));
    }

    Map<String, Long> getTimings(long threshold) {
      final LinkedHashMap<String, Long> map = new LinkedHashMap<>();
      final List<Map.Entry<String, Long>> sorted = new ArrayList<>();
      sorted.addAll(mTimings.entrySet().stream().filter(e -> e.getValue() > threshold).collect(Collectors.toCollection(ArrayList::new)));
      sorted.sort((a, b) -> a.getValue() > b.getValue() ? -1 : a.getValue().equals(b.getValue()) ? 0 : 1 );
      sorted.forEach(e -> map.put(e.getKey(), e.getValue()));
      return map;
    }
  }

  @SuppressWarnings("unchecked")
  public static void main(final String[] args) throws ClassNotFoundException, InitializationError {
    final JUnitCore jUnitCore = new JUnitCore();
    jUnitCore.addListener(new TextListener(new RealSystem()));
    final TimingListener timing;
    if (COLLECT_TIMINGS) {
      timing =new TimingListener();
      jUnitCore.addListener(timing);
    } else {
      timing = null;
    }
    jUnitCore.addListener(new RunListener() {
      int mTestCount = 0;
      @Override
      public void testFinished(Description description) throws Exception {
        mTestCount++;
        if (mTestCount % TESTS_PER_ROW == 0) {
          System.out.println();
        }
      }
    });
    final List<Result> results = new ArrayList<>();
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
    if (timing != null) {
      final Map<String, Long> timings = timing.getTimings(LONG_TEST_THRESHOLD);
      if (timings.size() > 1) {
        System.out.println();
        System.out.println("Long tests");
        for (Map.Entry<String, Long> entry : timings.entrySet()) {
          System.out.println(TimingListener.timingFormatter(entry.getKey(), entry.getValue(), TIMING_WIDTH));
        }
      }
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
