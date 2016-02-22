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
package com.rtg.launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogStream;
import com.rtg.util.test.FileHelper;

/**
 * Tests corresponding class
 */
public class LoggedCliTest extends AbstractCliTest {

  static final String MODULENAME = "loggedCliTest";

  File mDir;

  @Override
  public void setUp() throws IOException {
    mDir = FileUtils.createTempDir("loggedclitest", "blah");
    super.setUp();
  }

  @Override
  public void tearDown() throws IOException {
    assertTrue(FileHelper.deleteAll(mDir));
    mDir = null;
    super.tearDown();
  }

  @Override
  protected AbstractCli getCli() {
    return new FakeCli(mDir, 0, false);
  }

  private static final class FakeCli extends LoggedCli {

    private final File mDir;
    private final int mCode;
    private final boolean mThrow;

    public FakeCli(File dir, int code, boolean throwException) {
      mDir = dir;
      mCode = code;
      mThrow = throwException;
    }

    final boolean[] mBools = {false};
    @Override
    protected int mainExec(OutputStream out, LogStream log) {
      mBools[0] = true;
      Diagnostic.warning("blah!");
      if (mThrow) {
        throw new SlimException();
      }
      return mCode;
    }

    @Override
    protected File outputDirectory() {
      return new File(mDir, "new");
    }

    @Override
    protected void initFlags() {
      mFlags = new CFlags();
      mFlags.registerOptional("blah", "nothing really");
    }

    @Override
    public String moduleName() {
      return MODULENAME;
    }
  }

  public void testLogging() throws Exception {
    final FakeCli tlcli = new FakeCli(mDir, 0, false);

    tlcli.initFlags();
    assertEquals(0, tlcli.mainExec(TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream()));

    final File newFile = new File(mDir, "new");
    assertTrue(newFile.exists());
    assertTrue(tlcli.mBools[0]);

    File logFile = null;
    for (File f : FileUtils.listFiles(newFile)) {
      if (f.toString().equals(newFile.toString() + StringUtils.FS + MODULENAME + ".log")) {
        logFile = f;
      }
    }
    assertNotNull(logFile);
    final String logContents = FileUtils.fileToString(logFile);
    TestUtils.containsAll(logContents
        , "Command line arguments: "
        , "Run Id: "
        , "Finished successfully in "
        , " s."
      );
    assertTrue(new File(newFile, "done").exists());
    final String doneContents = FileUtils.fileToString(new File(newFile, "done"));
    assertTrue(doneContents, doneContents.matches("Finished successfully in \\d+ s\\." + StringUtils.LS));
    assertTrue(new File(newFile, "progress").exists());
    final String progressContents = FileUtils.fileToString(new File(newFile, "progress"));
    TestUtils.containsAll(progressContents
        , "Started"
        , "Finished "
      );
  }

  public void testLoggingFail() throws Exception {
    final FakeCli tlcli = new FakeCli(mDir, 1, false);
    tlcli.initFlags();

    assertEquals(1, tlcli.mainExec(TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream()));

    final File newFile = new File(mDir, "new");
    assertTrue(newFile.exists());
    assertTrue(tlcli.mBools[0]);

    File logFile = null;
    for (File f : FileUtils.listFiles(newFile)) {
      if (f.toString().equals(newFile.toString() + StringUtils.FS + MODULENAME + ".log")) {
        logFile = f;
      }
    }
    assertNotNull(logFile);
    final String logContents = FileUtils.fileToString(logFile);
    TestUtils.containsAll(logContents
        , "Command line arguments: "
        , "Run failed in "
        , " s."
      );
    assertFalse(new File(newFile, "done").exists());
    assertTrue(new File(newFile, "progress").exists());
    final String progressContents = FileUtils.fileToString(new File(newFile, "progress"));
    TestUtils.containsAll(progressContents
        , "Started"
        , "Run failed"
      );
  }

  public void testLoggingThrown() throws Exception {
    final FakeCli tlcli = new FakeCli(mDir, 0, true);
    tlcli.initFlags();

    try {
      tlcli.mainExec(TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream());
      fail();
    } catch (SlimException e) {
      //expected
    }

    final File newFile = new File(mDir, "new");
    assertTrue(newFile.exists());
    assertTrue(tlcli.mBools[0]);

    File logFile = null;
    for (File f : FileUtils.listFiles(newFile)) {
      if (f.toString().equals(newFile.toString() + StringUtils.FS + MODULENAME + ".log")) {
        logFile = f;
      }
    }
    assertNotNull(logFile);
    final String logContents = FileUtils.fileToString(logFile);
    TestUtils.containsAll(logContents
        , "Command line arguments: "
        , "Run failed in "
        , " s."
      );
    assertFalse(new File(newFile, "done").exists());
    assertTrue(new File(newFile, "progress").exists());
    final String progressContents = FileUtils.fileToString(new File(newFile, "progress"));
    TestUtils.containsAll(progressContents
        , "Started"
        , "Run failed"
      );
  }

  public void testCleanDir() throws Exception {
    Diagnostic.setLogStream();
    final String moduleName = "loggedCliTest";

    final File dir = FileUtils.createTempDir("loggedclitest", "blah");

    try {
      final LoggedCli tlcli = new LoggedCli() {

        @Override
        protected int mainExec(OutputStream out, LogStream log) {
          assertTrue(outputDirectory().exists());
          cleanDirectory(); // Signal that we want to clean the output directory (if we created it)
          return 0;
        }

        @Override
        protected File outputDirectory() {
          return new File(dir, "new");
        }

        @Override
        protected void initFlags() {
          mFlags = new CFlags();
        }

        @Override
        public String moduleName() {
          return moduleName;
        }

      };

      // Can we create a directory
      final ByteArrayOutputStream errbaos = new ByteArrayOutputStream();
      tlcli.createDirectory(tlcli.outputDirectory());
      tlcli.cleanDirectory();
      tlcli.initFlags();
      final File newFile = new File(dir, "new");
      assertTrue(newFile.exists());

      // Output directory now already exists. We should not delete it when we run
      assertEquals(0, tlcli.mainExec(new ByteArrayOutputStream(), new PrintStream(errbaos)));
      assertTrue(newFile.exists());

      // If we use an output directory that we create, we should delete it
      assertTrue(FileHelper.deleteAll(newFile));
      assertEquals(0, tlcli.mainExec(new ByteArrayOutputStream(), new PrintStream(errbaos)));
      assertFalse(newFile.exists());
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testTimeDifference() {
    assertEquals(1, LoggedCli.timeDifference(2000, 1000), 0);
  }
}
