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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;


/**
 */
public class CommonFlagsTest extends TestCase {

  public void testFileLists() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Diagnostic.setLogStream(new PrintStream(baos));
    try {
      final String listFlag = "file-list";
      final File tmp = FileHelper.createTempDirectory();
      try {
        final File[] files = new File[5];
        final File listfile = new File(tmp, "file-list");
        try (FileWriter fw = new FileWriter(listfile)) {
          fw.append("# some kind of header to be ignored").append(StringUtils.LS).append("   ").append(StringUtils.LS); // Test comment skipping
          for (int i = 0; i < files.length; i++) {
            files[i] = new File(tmp, "file" + i);
            fw.append(" ").append(files[i].getPath()).append(" ").append(StringUtils.LS); // Test line trimming
          }
        }
        final CFlags flags = new CFlags("blah", TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
        flags.registerOptional(listFlag, File.class, "FILE", "files");
        final Flag reads = flags.registerRequired(File.class, "File", "input sam files");
        reads.setMinCount(0);
        reads.setMaxCount(4000);
        final String[] args = new String[files.length];
        for (int i = 0; i < files.length; i++) {
          args[i] = files[i].getPath();
        }
        flags.setFlags(args);
        final CFlags flags2 = new CFlags("blah", TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
        flags2.registerOptional(listFlag, File.class, "FILE", "files");
        //final Flag reads2 =
        flags2.registerRequired(File.class, "File", "input sam files");
        reads.setMinCount(0);
        reads.setMaxCount(4000);
        flags2.setFlags("--" + listFlag, listfile.getAbsolutePath());
        try {
          CommonFlags.getFileList(flags, listFlag, null, false);
          fail();
        } catch (final NoTalkbackSlimException e) {
          assertTrue(e.getMessage(), e.getMessage().contains("There were 5 invalid input file paths"));
        }
        for (int i = 0; i < files.length; i++) {
          assertEquals(i < 5, baos.toString().contains("File not found: \"" + files[i].getPath() + "\""));
        }
        baos.reset();

        for (final File file : files) {
          assertTrue(file.createNewFile());
        }
        CommonFlags.getFileList(flags, listFlag, null, false);
        CommonFlags.getFileList(flags2, listFlag, null, false);
        try {
          CommonFlags.getFileList(flags, listFlag, null, true);
          fail();
        } catch (final NoTalkbackSlimException e) {
          assertTrue(e.getMessage(), e.getMessage().contains("There were 5 invalid input file paths"));
        }

        for (int i = 0; i < files.length; i++) {
          assertEquals("files[" + i + "]=" + files[i].getPath() + " was " + (i < 5 ? "not " : "") + "contained in the string", i < 5, baos.toString().contains(files[i].getPath() + " is not a valid SDF"));
        }

        for (final File file : files) {
          assertTrue(file.delete());
          assertTrue(file.mkdir());
        }
        baos.reset();
        try {
          CommonFlags.getFileList(flags, listFlag, null, false);
          //System.err.println(baos.toString());
          fail();
        } catch (final NoTalkbackSlimException e) {
          assertTrue(e.getMessage(), e.getMessage().contains("There were 5 invalid input file paths"));
        }
        for (int i = 0; i < files.length; i++) {
          assertEquals(i < 5, baos.toString().contains(files[i].getPath() + "\" is not a file"));
        }

        baos.reset();
        try {
          CommonFlags.getFileList(flags2, listFlag, null, false);
          fail();
        } catch (final NoTalkbackSlimException e) {
          assertTrue(e.getMessage(), e.getMessage().contains("There were 5 invalid input file paths"));
        }
        for (int i = 0; i < files.length; i++) {
          assertEquals(i < 5, baos.toString().contains(files[i].getPath() + "\" is not a file"));
        }


        final File f1 = new File(tmp, "f1");
        assertTrue(f1.createNewFile());
        final File f2 = new File(tmp, "f2");
        assertTrue(f2.createNewFile());
        flags.setFlags(f1.getPath(), f2.getPath());
        final List<File> filesout = CommonFlags.getFileList(flags, listFlag, null, false);
        assertNotNull(filesout);
        assertEquals(2, filesout.size());
      } finally {
        assertTrue(FileHelper.deleteAll(tmp));
      }
    } finally {
      Diagnostic.setLogStream();
    }
  }

  public void testCheckFile() throws IOException {
    try (TestDirectory dir = new TestDirectory("commonflags")) {
      final CFlags flags = new CFlags("blah", TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
      final Flag inFlag = flags.registerRequired(File.class, "FILE", "");
      inFlag.setMinCount(0);
      inFlag.setMaxCount(Integer.MAX_VALUE);
      flags.registerOptional("input", File.class, "FILE", "i");

      final String[] files = new String[2];
      for (int i = 0; i < 2; i++) {
        final File f = File.createTempFile("testcheck", "tmp", dir);
        files[i] = f.getPath();
      }
      flags.setFlags(files);

      assertFalse(CommonFlags.checkFileList(flags, "blah", null, 1));
      assertEquals("More than 1 input files specified.", flags.getParseMessage());

      flags.setFlags();

      CommonFlags.checkFileList(flags, "blah", "input", 5);
      assertEquals("No input files specified in --blah or --input.", flags.getParseMessage());

      final File f1 = File.createTempFile("testcheck", "tmp", dir);
      final File f2 = File.createTempFile("testcheck", CommonFlags.RECALIBRATE_EXTENSION, dir);
      flags.setFlags(f1.getPath(), f2.getPath());
      assertFalse(CommonFlags.checkFileList(flags, "blah", null, 1, false));
      assertTrue(CommonFlags.checkFileList(flags, "blah", null, 1, true));
    }
  }

  public void testReaderRestriction() {
    final CFlags flags = new CFlags("blah", TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
    CommonFlags.initReadRange(flags);

    TestUtils.containsAll(flags.getUsageString(),
        "--end-read=INT", "exclusive upper bound on read id",
        "--start-read=INT", "inclusive lower bound on read id"
        );

    LongRange r = CommonFlags.getReaderRestriction(flags);

    assertEquals(-1, r.getStart());
    assertEquals(-1, r.getEnd());

    flags.setFlags("--" + CommonFlags.START_READ_ID, "3", "--" + CommonFlags.END_READ_ID, "5");

    r = CommonFlags.getReaderRestriction(flags);

    assertEquals(3, r.getStart());
    assertEquals(5, r.getEnd());
  }

  public void testValidateSDF() throws Exception {
    final File tmpFile = FileUtils.createTempDir("commonflags", "tmp");
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    try {

      final CFlags flags = new CFlags("blah", TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
      flags.registerRequired('i', CommonFlags.READS_FLAG, File.class, "SDF", "");
      final File sdf = new File(tmpFile, "sdf");
      flags.setFlags("-i", sdf.getPath());

      assertFalse(CommonFlags.validateReads(flags, true));
      assertTrue(mps.toString(), mps.toString().contains("The specified SDF, \"" + sdf.getPath() + "\", does not exist"));
      mps.reset();

      assertTrue(sdf.createNewFile());

      assertFalse(CommonFlags.validateReads(flags, true));
      assertTrue(mps.toString(), mps.toString().contains("The specified file, \"" + sdf.getPath() + "\", is not an SDF"));
      mps.reset();

      assertTrue(sdf.delete());
      assertTrue(sdf.mkdir());

      assertTrue(mps.toString(), CommonFlags.validateReads(flags, true));
    } finally {
      Diagnostic.setLogStream();
      FileHelper.deleteAll(tmpFile);
    }
  }

  public void testValidateStartEnd() {
    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());
    try {

      final CFlags flags = new CFlags("blah", TestUtils.getNullPrintStream(), mps.printStream());
      flags.registerOptional("start", Long.class, "LONG", "s");
      flags.registerOptional("end", Long.class, "LONG", "e");

      flags.setFlags("--start", "-1");
      assertFalse(CommonFlags.validateStartEnd(flags, "start", "end"));
      assertTrue(mps.toString().contains("--start should be positive"));

      mps.reset();
      flags.setFlags("--end", "-1");
      assertFalse(CommonFlags.validateStartEnd(flags, "start", "end"));
      assertTrue(mps.toString().contains("--end should be greater than 0"));

      mps.reset();
      flags.setFlags("--start", "3", "--end", "1");
      assertFalse(CommonFlags.validateStartEnd(flags, "start", "end"));
      assertTrue(mps.toString().contains("--start should be less than --end"));

      mps.reset();
      flags.setFlags("--start", "0", "--end", "" + Long.MAX_VALUE);
      assertFalse(CommonFlags.validateStartEnd(flags, "start", "end"));
      assertTrue(mps.toString().contains("You have specified too many sequences, please specify a range of less than " + Integer.MAX_VALUE));
    } finally {
      Diagnostic.setLogStream();
    }
  }

}
