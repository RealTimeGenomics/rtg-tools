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
package com.rtg.tabix;

import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.util.BlockCompressedOutputStream;

/**
 */
public class BgZip extends AbstractCli {

  private static final String STDOUT_FLAG = "stdout";
  private static final String DECOMPRESS_FLAG = "decompress";
  private static final String FORCE_FLAG = "force";
  private static final String NO_TERMINATE_FLAG = "no-terminate";
  private static final String LEVEL_FLAG = "compression-level";

  @Override
  public String moduleName() {
    return "bgzip";
  }

  @Override
  public String description() {
    return "compress a file using block gzip";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Compress a file with block gzip.");
    CommonFlagCategories.setCategories(mFlags);

    mFlags.registerRequired(File.class, CommonFlags.FILE, "file to (de)compress, use '-' for standard input").setCategory(INPUT_OUTPUT).setMaxCount(Integer.MAX_VALUE);

    mFlags.registerOptional('c', STDOUT_FLAG, "write on standard output, keep original files unchanged. Implied when using standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional('d', DECOMPRESS_FLAG, "decompress").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional('f', FORCE_FLAG, "force overwrite of existing output file").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(NO_TERMINATE_FLAG, "if set, do not add the block gzip termination block").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional('l', LEVEL_FLAG, Integer.class, CommonFlags.INT, "the compression level to use, between 1 (least but fast) and 9 (highest but slow)", BlockCompressedOutputStream.getDefaultCompressionLevel()).setCategory(INPUT_OUTPUT);

    mFlags.setValidator(flags -> flags.checkInRange(LEVEL_FLAG, 1, 9)
      && flags.checkNand(STDOUT_FLAG, FORCE_FLAG)
      && flags.checkNand(DECOMPRESS_FLAG, NO_TERMINATE_FLAG)
      && flags.checkNand(DECOMPRESS_FLAG, LEVEL_FLAG));
  }

  private static String getOutputFilename(File inputFile, boolean decompress) {
    if (decompress) {
      if (FileUtils.isGzipFilename(inputFile)) {
        return FileUtils.removeExtension(inputFile).getPath();
      } else {
        return null;
      }
    } else {
      return inputFile.getPath() + FileUtils.GZ_SUFFIX;
    }
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {

    for (Object o : mFlags.getAnonymousValues(0)) {
      final File f = (File) o;
      final boolean stdin = FileUtils.isStdio(f);
      final boolean stdout = mFlags.isSet(STDOUT_FLAG) || stdin;
      try (final InputStream is = getInputStream(f, stdin)) {
        try (OutputStream os = getOutputStream(out, f, stdout)) {
          final byte[] buf = new byte[FileUtils.BUFFER_SIZE];
          int bytesRead;
          while ((bytesRead = is.read(buf)) != -1) {
            os.write(buf, 0, bytesRead);
          }
        }
      }
      if (!stdout) {
        if (!f.delete()) {
          throw new NoTalkbackSlimException("Could not delete " + f.getPath());
        }
      }
    }
    return 0;
  }

  private InputStream getInputStream(File f, boolean stdin) throws IOException {
    if (!stdin) {
      if (!f.exists()) {
        throw new NoTalkbackSlimException("The specified file, \"" + f.getPath() + "\" does not exist.");
      } else if (mFlags.isSet(DECOMPRESS_FLAG) && !FileUtils.isGzipFilename(f)) {
        throw new NoTalkbackSlimException("Input file not in GZIP format");
      }
    }
    if (mFlags.isSet(DECOMPRESS_FLAG)) {
      return stdin ? FileUtils.createGzipInputStream(System.in, true) : FileUtils.createGzipInputStream(f, true);
    } else {
      return stdin ? System.in : new FileInputStream(f);
    }
  }

  private OutputStream getOutputStream(OutputStream out, File f, boolean stdout) throws FileNotFoundException {
    final OutputStream os;
    final String outputFilename = getOutputFilename(f, mFlags.isSet(DECOMPRESS_FLAG));
    if (!stdout && !mFlags.isSet(FORCE_FLAG)) { //if we aren't forcibly overwriting files
      if (outputFilename == null) {
        throw new NoTalkbackSlimException("unrecognized gzip extension on file: " + f.getPath() + " -- aborting");
      }
      final File outfile = new File(outputFilename);
      if (outfile.exists()) {
        throw new NoTalkbackSlimException("Output file \"" + outfile.getPath() + "\" already exists.");
      }
    }
    if (mFlags.isSet(DECOMPRESS_FLAG)) {
      os = stdout ? out : new FileOutputStream(outputFilename);
    } else {
      final File file = new File(f.getPath() + FileUtils.GZ_SUFFIX);
      os = stdout
        ? new BlockCompressedOutputStream(out, null, (Integer) mFlags.getValue(LEVEL_FLAG), !mFlags.isSet(NO_TERMINATE_FLAG))
        : new BlockCompressedOutputStream(new FileOutputStream(file), file, (Integer) mFlags.getValue(LEVEL_FLAG), !mFlags.isSet(NO_TERMINATE_FLAG));
    }
    return os;
  }
}
