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

package com.rtg.util.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Assert;

import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;

/**
 * Support class for writing nano-regression tests for running within
 * JUnit suites.  Primarily for integration style testing (i.e. run
 * whole modules rather than probing individual classes).  The goal is
 * to abstract out the storage of input files (we can then have all
 * inputs in a test-relative location), and the storage, comparison
 * and updating of reference files.
 *
 * Resources are nominally stored under a filesystem reflecting the
 * package hierarchy, with a final "resources" level appended. This
 * arrangement can be accessed either by direct filesystem (see 1
 * below) or via CLASSPATH (option 2 below).
 *
 * 1) if <code>-Dregression.root</code> is defined use that (it must
 * be a valid directory, or multiple directories separated by
 * File.pathSeparator) use that. Writing will be enabled, and in the
 * case of multiple directories, storage of new results occurs in the
 * first directory specified (updates of existing results happen in-place).
 *
 * 2) otherwise uses <code>ClassLoader.getResource()</code> to load it
 * from CLASSPATH. Writing will be disabled.
 *
 * Provide an easy mechanism to update reference files from current
 * output. It should be possible to update the reference files for
 * just a single test if desired, and it should also be possible to
 * update the results for all tests. Ideally easy to update from
 * either command line or eclipse.
 *
 * To use the <code>NanoRegression</code> in a unit test: <ul>
 *
 * <li> Construct a <code>NanoRegression</code> with the test class in
 * the constructor. Typically this will be done in the test
 * <code>setUp</code>.
 *
 * <li> (optionally) Call <code>loadReference</code> to retrieve an input in the
 * form of a String (methods could be added to load binary data as a
 * byte array
 *
 * <li> Call <code>check()</code> some number of times, using an id
 * that should typically be unique per package along with the actual
 * observed string for comparison.
 *
 * <li> Call <code>finish()</code>, typically in the test
 * <code>tearDown</code>. This will throw an assertion failure if any of the
 * check methods failed during the test (unless test updating is enabled, in
 * in which case the expected result is updated). (You should therefore
 * encapsulate the call to finish in a try-finally to ensure that remaining
 * test resources are freed, including the nano regregession object itself.
 * e.g.:
 * <pre>
  public void tearDown() throws IOException {
    ...free other test resources
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }
 * </pre>
 * </ul>
 *
 * To enable result updating you must be running via the
 * <code>-Dregression.root</code> option to point to the root of the
 * test hierarchy. (This is already set up in the ant build.xml,
 * eclipse users will need to set this as a JVM arg). There are two
 * methods to instruct the class to update results: <ul>
 *
 * <li> Append a <code>true</code> to the NanoRegression constructor
 * call in your test temporarily and rerun the test. Probably of most
 * use to Eclipse users.
 *
 * <li> Set a regular expression of class names to enable writing via
 * <code>-Dregression.update</code> and rerun the test. The ant
 * run-test and runalltests targets will pass along this variable if
 * set.
 *
 * </ul>
 *
 */
public class NanoRegression {

  private interface RegressionStore {
    /**
     * Returns true if there is an existing reference associated with
     * the specified identifier.
     *
     * @param id a <code>String</code> identifier.
     * @return true if a reference exists.
     */
    boolean hasReference(String id);

    /**
     * Loads the expected results associated with the specified
     * identifier.
     *
     * @param id a <code>String</code> identifier.
     * @return the expected results for the given identifier.
     * @throws IOException if the expected data could not be found or loaded.
     */
    String loadReference(String id) throws IOException;

    /**
     * Saves expected results associated with the specified identifier.
     *
     * @param id a <code>String</code> identifier.
     * @param reference the reference results associated with the given identifier.
     * @throws IOException if the expected data could not be written.
     */
    void updateReference(String id, String reference) throws IOException;
  }

  static String getPackageName(Class<?> testClass) {
    final String packageName;
    if (testClass.getPackage() != null) {
      packageName = testClass.getPackage().getName();
    } else {
      final int lastDot = testClass.getName().lastIndexOf(".");
      if (lastDot != -1) {
        packageName = testClass.getName().substring(0, lastDot);
      } else {
        packageName = "";
      }
    }
    return packageName;
  }

  private static class FileRegressionStore implements RegressionStore {

    private final File mRepositoryDir;

    FileRegressionStore(Class<?> testClass, File rootDir) {
      if (!(rootDir.exists() && rootDir.isDirectory())) {
        throw new RuntimeException("Repository root directory " + rootDir + " does not exist");
      }
      final String packageName = getPackageName(testClass);
      final File classDir = new File(rootDir, packageName.replace(".", System.getProperty("file.separator")));
      mRepositoryDir = new File(classDir, "resources");
    }

    @Override
    public boolean hasReference(String id) {
      return getFileFromId(id).exists();
    }

    private File getFileFromId(String id) {
      final String pathId = id.replace("/", System.getProperty("file.separator"));
      return new File(mRepositoryDir, pathId);
    }

    @Override
    public String loadReference(String id) throws IOException {
      return FileUtils.fileToString(getFileFromId(id));
    }

    @Override
    public void updateReference(String id, String reference) throws IOException {
      if (!mRepositoryDir.exists()) {
        if (!mRepositoryDir.mkdirs()) {
          throw new IOException("Could not create output directory: " + mRepositoryDir);
        }
      }
      FileUtils.stringToFile(reference, getFileFromId(id));
    }
  }

  private static class ClasspathRegressionStore implements RegressionStore {

    private final ClassLoader mLoader;
    private final String mResourcePrefix;

    ClasspathRegressionStore(Class<?> testClass) {
      mLoader = testClass.getClassLoader();
      final String packageName = getPackageName(testClass);
      mResourcePrefix = packageName.replace(".", "/") + "/resources";
    }

    @Override
    public boolean hasReference(String id) {
      return mLoader.getResource(mResourcePrefix + "/" + id) != null;
    }

    @Override
    public String loadReference(String id) throws IOException {
      try (InputStream input = mLoader.getResourceAsStream(mResourcePrefix + "/" + id)) {
        return FileUtils.streamToString(input);
      }
    }

    @Override
    public void updateReference(String id, String reference) {
      throw new UnsupportedOperationException();
    }
  }

  private static class UnionRegressionStore implements RegressionStore {

    private final RegressionStore[] mDelegates;

    UnionRegressionStore(RegressionStore[] delegates) {
      mDelegates = delegates;
    }

    @Override
    public boolean hasReference(String id) {
      for (RegressionStore store : mDelegates) {
        if (store.hasReference(id)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String loadReference(String id) throws IOException {
      for (RegressionStore store : mDelegates) {
        if (store.hasReference(id)) {
          return store.loadReference(id);
        }
      }
      throw new NullPointerException();
    }

    @Override
    public void updateReference(String id, String reference) throws IOException {
      for (RegressionStore store : mDelegates) {
        if (store.hasReference(id)) {
          store.updateReference(id, reference);
          return;
        }
      }
      mDelegates[0].updateReference(id, reference); // Fall back to first, results may need manual moving to final destination
    }
  }

  private static class RegressionFailure {

    private final String mId;
    private final String mActual;
    private final String mMessage;

    RegressionFailure(String id, String actual, String message) {
      mId = id;
      mActual = actual;
      mMessage = message;
    }

    public String id() {
      return mId;
    }

    public String actual() {
      return mActual;
    }

    public String message() {
      return mMessage;
    }
  }


  // The root directory under which regression results will be stored
  private static final String PROP_ROOTDIR = "regression.root";

  // If this is set to a regexp that matches the current class name, then updates will be enabled
  private static final String PROP_UPDATE = "regression.update";


  // Name of the test class
  private final String mName;

  // Provide access to reference results
  private final RegressionStore mRepository;

  // If true, results updating has been enabled
  private final boolean mUpdateEnabled;

  // A list of all failures found so far
  private final ArrayList<RegressionFailure> mFailures = new ArrayList<>();


  /**
   * Creates a new <code>NanoRegression</code> instance.
   *
   * @param testClass the test class, used to determine storage location
   */
  public NanoRegression(Class<?> testClass) {
    this(testClass, false);
  }

  /**
   * Creates a new <code>NanoRegression</code> instance.
   *
   * @param testClass the test class, used to determine storage location
   * @param updateEnabled if true, result updating will be enabled,
   * for temporary use only.
   */
  public NanoRegression(Class<?> testClass, boolean updateEnabled) {
    mName = testClass.getName();

    if (updateEnabled) {
      mUpdateEnabled = true;
    } else {
      final String updateSpec = System.getProperty(PROP_UPDATE);
      mUpdateEnabled = (updateSpec != null) && testClass.getName().matches(updateSpec);
    }

    // Where to read and write regression files:
    final String manualRoot = System.getProperty(PROP_ROOTDIR);
    if (manualRoot != null) {
      if (mUpdateEnabled) {
        System.out.println("Warning: Reference updating for " + mName + " is now enabled.");
        System.out.println("Reference root = " + manualRoot);
      }
      final String[] roots = manualRoot.split(File.pathSeparator);
      if (roots.length == 1) {
        mRepository = new FileRegressionStore(testClass, new File(manualRoot));
      } else {
        final RegressionStore[] repositories = new RegressionStore[roots.length];
        int i = 0;
        for (String root : roots) {
          repositories[i++] = new FileRegressionStore(testClass, new File(root));
        }
        mRepository = new UnionRegressionStore(repositories);
      }
    } else {
      if (mUpdateEnabled) {
        throw new UnsupportedOperationException("Reference updates cannot be enabled when running from CLASSPATH");
      }
      mRepository = new ClasspathRegressionStore(testClass);
    }
  }

  /**
   * Returns true if there is an existing reference associated with
   * the specified identifier.
   *
   * @param id a <code>String</code> identifier.
   * @return true if a reference exists.
   */
  public boolean hasReference(String id) {
    return mRepository.hasReference(id);
  }

  /**
   * Loads the expected results associated with the specified
   * identifier.
   *
   * @param id a <code>String</code> identifier.
   * @return the expected results for the given identifier.
   * @throws IOException if the expected data could not be found or loaded.
   */
  public String loadReference(String id) throws IOException {
    return mRepository.loadReference(id);
  }

  /**
   * Saves expected results associated with the specified identifier.
   *
   * @param id a <code>String</code> identifier.
   * @param reference the reference results associated with the given identifier.
   * @throws IOException if the expected data could not be written.
   */
  public void updateReference(String id, String reference) throws IOException {
    mRepository.updateReference(id, reference);
  }

  private boolean updateEnabled() {
    return mUpdateEnabled;
  }

  /**
   * Compares the given results against reference results associated
   * with the specified identifier. If there is a comparison failure,
   * the results are accumulated so that all regression results can be
   * viewed at once when finish is called. This method assumes that
   * output contains platform-specific line endings (and so performs
   * the appropriate conversion during comparison against the expected
   * text).
   *
   * @param id a <code>String</code> identifier.
   * @param actual the current results
   * @throws IOException if the expected data could not be written.
   */
  public void check(String id, String actual) throws IOException {
    check(id, actual, true);
  }

  /**
   * Compares the given results against reference results associated
   * with the specified identifier. If there is a comparison failure,
   * the results are accumulated so that all regression results can be
   * viewed at once when finish is called. This method lets you specify
   * whether to convert reference line endings to the current platform
   * when comparing.
   *
   * @param id a <code>String</code> identifier.
   * @param actual the current results
   * @param convert if true, convert line endings in the reference results to the current platform.
   * @throws IOException if the expected data could not be written.
   */
  public void check(String id, String actual, boolean convert) throws IOException {
    if (hasReference(id)) {
      String expected = loadReference(id);
      if (convert) {
        expected = StringUtils.convertLineEndings(expected);
      }
//      Assert.assertEquals(expected, actual);
      final String result = TestUtils.compareLines(expected.split("\n"), actual.split("\n"));
      if (result != null) {
        System.err.println("Expected:<");
        System.err.println(expected);
        System.err.println(">");
        System.err.println("Actual:<");
        System.err.println(actual);
        System.err.println(">");
        mFailures.add(new RegressionFailure(id, actual, result));
      }
    } else {
      mFailures.add(new RegressionFailure(id, actual, "No reference results available."));
    }
  }

  /**
   * Finish a set of checks, and if there have been any failures, will
   * either update reference results or invoke an assertion failure
   * depending on whether updating has been enabled.
   *
   * @exception IOException if an error occurs.
   */
  public void finish() throws IOException {
    if (mFailures.size() > 0) {
      if (updateEnabled()) {
        for (final RegressionFailure failure : mFailures) {
          System.err.println("Updating reference: " + failure.id());
          mRepository.updateReference(failure.id(), failure.actual());
        }
        System.err.println("Regression " + mName + " updated results for " + mFailures.size() + " checks");
      } else {
        // Dump the failures and fail out
        Assert.fail("Regression " + mName + " failed " + mFailures.size() + " checks" + StringUtils.LS + getFailureString());
      }
      mFailures.clear();
    }
  }

  /**
   * @return a failure string
   */
  @SuppressWarnings("StringBufferMayBeStringBuilder")
  public String getFailureString() {
    final StringBuffer sb = new StringBuffer();
    for (final RegressionFailure failure : mFailures) {
      sb.append("Failed: ").append(mName).append(":").append(failure.id()).append(StringUtils.LS).append(failure.message());
    }
    return sb.toString();
  }

  /**
   * Little command line test
   *
   * @param args a <code>String</code> value
   * @exception IOException if an error occurs.
   */
  public static void main(String... args) throws IOException {
    final NanoRegression nr = new NanoRegression(NanoRegression.class);
    for (final String arg : args) {
      if (nr.hasReference(arg)) {
        System.out.println("Existing reference for " + arg);
        System.out.print(nr.loadReference(arg));
      } else {
        System.out.println("No existing reference for " + arg);
        nr.updateReference(arg, "hello world\n");
      }
    }
  }
}

