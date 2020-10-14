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
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.IOUtils;

/**
 * This class reports all the Environment variables.
 *
 */
public final class Environment {

  static {
    Locale.setDefault(Locale.US); // To ensure number formatting is consistent

    // Ughhh, on JDK-11 any time you use the default js engine it spits out a warning to stderr.
    // This disables it.
    System.setProperty("nashorn.option.no.deprecation.warning", "true");
    // But if you're using graal, this gives some compatibility
    System.setProperty("polyglot.js.nashorn-compat", "true");
  }

  // To disable checking for release update during testing
  private static boolean sCheckRelease = true;
  public static void setCheckRelease(final boolean enable) {
    sCheckRelease = enable;
  }

  private static Class<?> getOperatingSystemMXBeanClass() {
    Class<?> c;
    try {
      c = Class.forName("com.sun.management.OperatingSystemMXBean");
    } catch (ClassNotFoundException e) {
      c = null;
    }
    return c;
  }
  private static final Class<?> OPERATING_SYSTEM_MX_BEAN_CLASS = getOperatingSystemMXBeanClass();

  static final String RUNTIME_FREEMEM = "runtime.freememory";
  static final String RUNTIME_MAXMEM = "runtime.maxmemory";
  static final String RUNTIME_TOTALMEM = "runtime.totalMemory";

  static final String PROCESSOR_ARCH = "processor.arch";
  static final String PROCESSOR_COUNT = "processor.count";
  static final String DEFAULT_THREADS = "runtime.defaultThreads";

  static final String OS_FREEMEM = "os.freememory";
  static final String OS_TOTALMEM = "os.totalmemory";
  static final String HOST_NAME = "host.name";

  static final String VERSION_NOT_FOUND = "<not found>";

  private static final String OS_NAME = System.getProperty("os.name");

  /** True if we are running on Linux */
  public static final boolean OS_LINUX = OS_NAME.startsWith("Linux");

  /** True if we are running on Mac OS X */
  public static final boolean OS_MAC_OS_X = OS_NAME.startsWith("Mac OS X");

  /** True if we are running on Windows */
  public static final boolean OS_WINDOWS = OS_NAME.startsWith("Windows");

  private Environment() { }

  /**
   * Returns HashMap with following properties
   * <ul>
   *   <li> All system property </li>
   *   <li> Detailed memory information </li>
   *   <li> Detailed OS information </li>
   * </ul>
   * This method creates the map at call time, so all values are current.
   *
   * @return <code>HashMap </code> containing all required information
   */
  public static HashMap<String, String> getEnvironmentMap() {
    final HashMap<String, String> env = new HashMap<>();
    final Properties props = System.getProperties();

    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      // Get property name
      final String propName = (String) entry.getKey();
      // Get property value
      final String propValue = (String) entry.getValue();
      env.put(propName, propValue);

    }
    final Runtime rt = Runtime.getRuntime();
    env.put(RUNTIME_FREEMEM, String.valueOf(rt.freeMemory()));
    env.put(RUNTIME_MAXMEM, String.valueOf(rt.maxMemory()));
    env.put(RUNTIME_TOTALMEM, String.valueOf(rt.totalMemory()));

    final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    env.put(PROCESSOR_ARCH, String.valueOf(bean.getArch()));
    env.put(PROCESSOR_COUNT, String.valueOf(getAvailableProcessors()));
    try {
      final String freeMemory = String.valueOf(getFreeMemory());
      final String totalMemory = String.valueOf(getTotalMemory());

      env.put(OS_FREEMEM, freeMemory);
      env.put(OS_TOTALMEM, totalMemory);
    } catch (final RuntimeException e) {
      // ignore
    }
    env.put("lang.version", props.get("java.version").toString());
    env.put("vendor", props.get("java.vendor").toString());
    env.put(HOST_NAME, getHostName());
    return env;
  }

  /**
   * Method returns the free memory in system.
   *
   * @return long Free available memory in the system
   * @throws IllegalStateException if the free memory could not be determined
   */
  public static long getFreeMemory() {
    final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    final Class<?> c = OPERATING_SYSTEM_MX_BEAN_CLASS == null ? bean.getClass() : OPERATING_SYSTEM_MX_BEAN_CLASS;
    try {
      final Method m = c.getMethod("getFreePhysicalMemorySize", (Class<?>[]) null);
      return (Long) m.invoke(bean, (Object[]) null);
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (final NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }


  /**
   * Returns total memory available in the system
   * @return long total memory available in the system
   * @throws IllegalStateException if the total memory could not be determined
   */
  public static long getTotalMemory() {
    final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    final Class<?> c = OPERATING_SYSTEM_MX_BEAN_CLASS == null ? bean.getClass() : OPERATING_SYSTEM_MX_BEAN_CLASS;
    try {
      Method m;
      try {
       m = c.getMethod("getTotalPhysicalMemorySize", (Class<?>[]) null);
      } catch (final NoSuchMethodException e) {
       m = c.getMethod("getTotalPhysicalMemory", (Class<?>[]) null);
      }
      return (Long) m.invoke(bean, (Object[]) null);
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (final NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the host name
   * @return the name
   */
  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (final UnknownHostException e) {
      return "<Unknown Host>";
    }
  }

  /**
   * Get number of available processors.
   * @return number of available processors.
   */
  public static int getAvailableProcessors() {
    final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    if (bean != null) {
      return bean.getAvailableProcessors();
    }
    throw new IllegalStateException();
  }

  /**
   * Get the default number of threads
   * @return the number of threads defined specified by the environment if set or the number of available processors
   */
  public static int defaultThreads() {
    final String s = getEnvironmentMap().get(DEFAULT_THREADS);
    if (s == null) {
      return getAvailableProcessors();
    }
    try {
      return Math.max(1, Integer.parseInt(s));
    } catch (NumberFormatException e) {
      throw new NoTalkbackSlimException("the system configuration for default number of threads is invalid: " + s);
    }
  }

  /**
   * Check if we have a 64-bit JVM.
   * @return true iff we have a 64 bit JVM (or at least can reliably tell if we have one).
   */
  public static boolean is64BitVM() {
    final String bits = System.getProperty("sun.arch.data.model", "?");
    return "64".equals(bits);
  }

  static String getVersion(final String resource) {
    try {
      try (BufferedReader r = new BufferedReader(new InputStreamReader(Resources.getResourceAsStream(resource)))) {
        return r.readLine();
      }
    } catch (final IOException | NullPointerException e) {
      return VERSION_NOT_FOUND;
    }
  }

  /**
   * Example: <code>"RTG Metagenomics"</code>
   * @return the product name
   */
  public static String getProductName() {
    return getVersion("com/rtg/product.name");
  }

  /**
   * @return the product release number as a string
   */
  public static String getProductVersion() {
    return getVersion("com/rtg/product.version");
  }

  /**
   * @return one-line full version string incorporating product and core versions
   */
  public static String getVersion() {
    return getProductName() + " " + getProductVersion() + " / Core " + getCoreVersion();
  }

  /**
   * @return the core version string
   */
  public static String getCoreVersion() {
    final String buildversion = getVersion("com/rtg/build.version");
    final String buildtime = getVersion("com/rtg/build.time");
    return buildversion + " (" + buildtime + ")";
  }

  /**
   * Gets the most recent release version, according to github
   * @return the most recent release version, or null if it could not be determined
   */
  public static String getLatestReleaseVersion() {
    final String repo = getVersion("com/rtg/product.repo");
    if (!sCheckRelease || VERSION_NOT_FOUND.equals(repo)) {
      return null;
    }
    try {
      final URLConnection conn = new URL("https://api.github.com/repos/" + repo + "/releases/latest").openConnection();
      // Use short timeout so this doesn't take ages if there are network problems
      conn.setConnectTimeout(2000);
      conn.setReadTimeout(3000);
      final JSONObject dict = (JSONObject) new JSONParser().parse(IOUtils.readAll(conn.getInputStream()));
      return (String) dict.get("tag_name");
    } catch (IOException | ParseException e) {
      Diagnostic.developerLog(e);
      return null;
    }
  }

  /**
   * Test from the command line
   * @param args command line arguments
   */
  public static void main(String... args) {
    final Map<String, String> props = Environment.getEnvironmentMap();
    for (Map.Entry<String, String> entry : props.entrySet()) {
      System.out.println(entry.getKey() + "=" + entry.getValue());
    }
  }
}

