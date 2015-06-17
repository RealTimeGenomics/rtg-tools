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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.rtg.sam.SamUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Unit tests for command line file list handling
 */
public class CommandLineFilesTest extends TestCase {
  private File mDir = null;
  private File mOne = null;
  private File mTwo = null;
  private MemoryPrintStream mLog = null;

  @Override
  public void setUp() throws IOException {
    mDir = FileUtils.createTempDir("CommandLineFilesTest", "tmpDir");
    mOne = new File(mDir, "one");
    mTwo = new File(mDir, "two");
    mLog = new MemoryPrintStream();
    Diagnostic.setLogStream(mLog.printStream());
  }

  @Override
  public void tearDown() {
    FileHelper.deleteAll(mDir);
    mDir = null;
    mOne = null;
    mTwo = null;
    mLog = null;
    Diagnostic.setLogStream();
  }

  public CFlags makeFlags() {
    final CFlags flags = new CFlags();
    flags.registerRequired(File.class, "FILE", "file list").setMaxCount(Integer.MAX_VALUE).setMinCount(0);
    flags.registerOptional(CommonFlags.INPUT_LIST_FLAG, File.class, "FILE", "File list flag");
    return flags;
  }

  public void testGetFiles() throws IOException {
    final CFlags flags = makeFlags();
    flags.setFlags(mOne.getPath(), mTwo.getPath());
    final CommandLineFiles cmf = new CommandLineFiles(CommonFlags.INPUT_LIST_FLAG, null);
    final List<File> files = cmf.getFileList(flags);
    assertEquals(2, files.size());
    fileContains(files, mOne);
    fileContains(files, mTwo);
  }

  public void makeFile(File f) throws IOException {
    assertTrue(f.createNewFile());
  }
  public void makeDir(File f) {
    assertTrue(f.mkdir());
  }

  public void testInputFileList() throws IOException {
    final File list = new File(mDir, "list");
    makeFile(mOne);
    makeFile(mTwo);
    FileUtils.stringToFile(mTwo + LS
        + "# please ignore me" + LS
        + "    " + mOne + LS,
      list);
    final CFlags flags = makeFlags();
    flags.setFlags("--" + CommonFlags.INPUT_LIST_FLAG, list.getPath());
    final CommandLineFiles cmf = CommandLineFiles.inputFiles();
    final List<File> files = cmf.getFileList(flags);
    assertEquals(2, files.size());
    fileContains(files, mTwo);
    fileContains(files, mOne);
  }

  public void testInputFileListManyInvalid() throws IOException {
    final File list = new File(mDir, "list");
    try (FileWriter fw = new FileWriter(list)) {
      for (int i = 0; i < 50; i++) {
        fw.write(new File(mDir, "no-such-file-" + i) + LS);
      }
    }
    final CFlags flags = makeFlags();
    flags.setFlags("--" + CommonFlags.INPUT_LIST_FLAG, list.getPath());
    final CommandLineFiles cmf = CommandLineFiles.inputFiles();
    try {
      cmf.getFileList(flags);
      fail();
    } catch (NoTalkbackSlimException e) {
      assertEquals(mLog.toString(), "There were more than " + CommandLineFiles.MAX_ERRORS + " invalid input file paths", e.getMessage());
    }
  }

  public static void fileContains(List<File> files, File f) {
    assertTrue("file " + f + " was not contained in the file list: " + files.toString(),  files.contains(f));
  }


  public void testInputFiles() throws IOException {
    final CommandLineFiles cmf = CommandLineFiles.inputFiles();
    makeFile(mOne);
    checkInvalid(cmf, "File not found: \"" + mTwo.getPath() + "\"", 1, defaultFlags());
    makeFile(mTwo);
    checkValid(cmf, defaultFlags());
  }
  public void testExists() throws IOException {
    final CommandLineFiles cmf = new CommandLineFiles(null, null, CommandLineFiles.EXISTS);
    makeFile(mOne);
    checkInvalid(cmf, "File not found: \"" + mTwo.getPath() + "\"", 1, defaultFlags());
    makeFile(mTwo);
    checkValid(cmf, defaultFlags());
  }

  public void testRegularFile() throws IOException {
    final CommandLineFiles cmf = new CommandLineFiles(null, null, CommandLineFiles.REGULAR_FILE);
    final CommandLineFiles cmf2 = new CommandLineFiles(null, null, CommandLineFiles.NOT_DIRECTORY);
    makeDir(mOne);
    makeFile(mTwo);
    checkInvalid(cmf, "A file name was expected, but \"" + mOne.getPath() + "\" is not a file", 1, defaultFlags());
    checkInvalid(cmf2, "A file name was expected, but \"" + mOne.getPath() + "\" is not a file", 1, defaultFlags());
    assertTrue(mOne.delete());
    makeFile(mOne);

    checkValid(cmf, defaultFlags());
    checkValid(cmf2, defaultFlags());
  }

  public void testDirectory() throws IOException {
      final CommandLineFiles cmf = new CommandLineFiles(null, null, CommandLineFiles.DIRECTORY);
      makeDir(mOne);
      makeFile(mTwo);
        checkInvalid(cmf, "The directory \"" + mTwo.getPath() + "\" does not exist", 1, defaultFlags());
      assertTrue(mTwo.delete());
      makeDir(mTwo);
      checkValid(cmf, defaultFlags());
  }

  public void testSDF() throws IOException {
      final CommandLineFiles cmf = CommandLineFiles.sdfFiles();
      makeDir(mOne);
      makeFile(mTwo);
        checkInvalid(cmf, mTwo.getPath() + " is not a valid SDF", 1, defaultFlags());
      assertTrue(mTwo.delete());
      makeDir(mTwo);
      checkValid(cmf, defaultFlags());
    }

  public void testNotExists() throws IOException {
    final CommandLineFiles cmf = new CommandLineFiles(null, null, CommandLineFiles.DOES_NOT_EXIST);
    makeFile(mOne);
    checkInvalid(cmf, "The file \"" + mOne.getPath() + "\" already exists", 1, defaultFlags());
    assertTrue(mOne.delete());
    checkValid(cmf, defaultFlags());
  }

  public void testTabix() throws IOException {
    final CommandLineFiles cmf = new CommandLineFiles(null, null, CommandLineFiles.TABIX);
    makeFile(new File(mOne + TabixIndexer.TABIX_EXTENSION));
    checkInvalid(cmf, "The file \"" + mTwo.getPath() + "\" does not have a tabix index", 1, defaultFlags());

    makeFile(new File(mTwo + TabixIndexer.TABIX_EXTENSION));
    checkValid(cmf, defaultFlags());
  }

  public void testVarianceInput() throws IOException {
    final CommandLineFiles cmf = new CommandLineFiles(null, null, CommandLineFiles.VARIANT_INPUT);

    makeFile(new File(mOne + TabixIndexer.TABIX_EXTENSION));
    checkInvalid(cmf, "The file \"" + mTwo.getPath() + "\" does not have a tabix index", 1, defaultFlags());
    makeFile(new File(mTwo + TabixIndexer.TABIX_EXTENSION));
    checkValid(cmf, defaultFlags());

    final File calibrate = new File(mTwo + CommonFlags.RECALIBRATE_EXTENSION);
    final List<File> filesCalibrated = cmf.getFileList(getFlags(new String[] {mOne.getPath(), mTwo.getPath(), calibrate.getPath()}));
    assertEquals(3, filesCalibrated.size());


    final CommandLineFiles cmf2 = new CommandLineFiles(null, null, CommandLineFiles.VARIANT_INPUT);
    final File f = new File(mDir, "test" + SamUtils.BAM_SUFFIX);
    makeFile(f);
    final String[] args = {f.getPath()};
    checkInvalid(cmf2, "The file \"" + f.getPath() + "\" does not have a valid index", 1, getFlags(args));
    File f2 = new File(mDir, "test" + SamUtils.BAI_SUFFIX);
//    System.err.println(f.getPath());
//    System.err.println(f2.getPath());
    makeFile(f2);
    List<File> files = cmf2.getFileList(getFlags(args));
    assertEquals(1, files.size());
    assertEquals(f.getPath(), files.get(0).getPath());
    assertTrue(f2.delete());

    f2 = new File(mDir, "test" + SamUtils.BAM_SUFFIX + SamUtils.BAI_SUFFIX);
//  System.err.println(f.getPath());
//  System.err.println(f2.getPath());
    makeFile(f2);
    files = cmf2.getFileList(getFlags(args));
    assertEquals(1, files.size());
    assertEquals(f.getPath(), files.get(0).getPath());
  }

  private CFlags getFlags(String[] args) {
    final CFlags flags = makeFlags();
    flags.setFlags(args);
    return flags;
  }

  public CFlags defaultFlags() {
    return getFlags(new String[] {mOne.getPath(), mTwo.getPath()});
  }

  // check that the cmf invalidates the flags passed with specified logMessage and number of errors
  public void checkInvalid(CommandLineFiles cmf, String logMessage, int errorCount, CFlags flags) throws IOException {
    try {
      cmf.getFileList(flags);
      fail();
    } catch (NoTalkbackSlimException e) {
      assertEquals(mLog.toString(), "There were " + errorCount + " invalid input file paths", e.getMessage());
      TestUtils.containsAll(mLog.toString(), logMessage);
    }
  }

  public void checkValid(CommandLineFiles cmf, CFlags flags) throws IOException {
    final List<File> files = cmf.getFileList(flags);
    assertEquals(2, files.size());
    fileContains(files, mOne);
    fileContains(files, mTwo);
  }

  public void testOutputFile() throws IOException {
    final CommandLineFiles cmf = CommandLineFiles.outputFile();
    final CFlags flags = new CFlags();
      flags.registerRequired(CommonFlags.OUTPUT_FLAG, File.class, "FILE", "output").setMaxCount(Integer.MAX_VALUE).setMinCount(0);

      final File subdir = new File(mOne, "subdir");
      final File subdir2 = new File(mTwo, "subdir2");
    flags.setFlags("--" + CommonFlags.OUTPUT_FLAG, subdir.getPath(), "--" + CommonFlags.OUTPUT_FLAG, subdir2.getPath());
    try {
      cmf.getFileList(flags);
      fail();
    } catch (NoTalkbackSlimException e) {
      TestUtils.containsAll(mLog.toString(), subdir + " can not be created");
      assertEquals(mLog.toString(), "There were 2 invalid input file paths", e.getMessage());
    }
    makeDir(mOne);
    makeDir(mTwo);
    final List<File> files = cmf.getFileList(flags);
    assertEquals(2, files.size());
    fileContains(files, subdir);
    fileContains(files, subdir2);
  }

  public void testEmpty() throws IOException {
    final CFlags flags = makeFlags();
    flags.setFlags();
    final CommandLineFiles cmf = CommandLineFiles.inputFiles();
    final List<File> files = cmf.getFileList(flags);
    assertEquals(0, files.size());
  }

}
