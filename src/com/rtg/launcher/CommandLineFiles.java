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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.rtg.sam.SamUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Class for extracting a list of files from a CFlags object following the conventions we tend to use.
 * You construct a CommandLineFiles object by describing the flags that files will be passed on, then you list your
 * any constraints those files must meet.
 * For example if the files should exist add the <code>EXISTS</code> constraint.
 * The first MAX_ERRORS files which don't
 * Not thread safe.
 */
public class CommandLineFiles {

  static final int MAX_ERRORS = 10;

  /**
   * Describes a method for checking if a file meets a criteria
   */
  public interface FileConstraint {
    /**
     * Checks for a specific property of a file passed on the command line
     * Implementors should call <code>files.error(ErrorType, String)</code> for each problem encountered with the files.
     * @param f the file to validate
     * @param files where errors are reported
     * @return true if the file meets the constraint's criteria otherwise false
     */
     boolean validate(File f, CommandLineFiles files);
  }

  /**
   * The file should exist.
   */
  public static final FileConstraint EXISTS = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      if (!f.exists()) {
        files.error(ErrorType.FILE_NOT_FOUND, f.getPath());
        return false;
      }
      return true;
    }
  };
  /**
   * The file should not already exist
   */
  public static final FileConstraint DOES_NOT_EXIST = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      if (f.exists()) {
        files.error(ErrorType.FILE_EXISTS, f.getPath());
        return false;
      }
      return true;
    }
  };
  /**
   * the files should be normal files, not directories - be careful as this often disables the use of bash process redirection.
   */
  public static final FileConstraint REGULAR_FILE = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      if (!f.isFile()) {
        files.error(ErrorType.NOT_A_FILE, f.getPath());
        return false;
      }
      return true;
    }
  };
  /**
   * The files should not be existing directories - this is similar to REGULAR_FILE but doesn't break bash process redirection.
   */
  public static final FileConstraint NOT_DIRECTORY = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      if (f.isDirectory()) {
        files.error(ErrorType.NOT_A_FILE, f.getPath());
        return false;
      }
      return true;
    }
  };

  /**
   * The files should be existing directories
   */
  public static final FileConstraint DIRECTORY = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      if (!f.isDirectory()) {
        files.error(ErrorType.DIRECTORY_NOT_EXISTS, f.getPath());
        return false;
      }
      return true;
    }
  };
  /**
   * Specifies that the input file should be a valid SDF file. Currently this is only checking that this is a
   * directory.
   */
  public static final FileConstraint SDF = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {

      if (!f.isDirectory()) {
        files.error(ErrorType.INFO_ERROR, f.getPath() + " is not a valid SDF");
        return false;
      }
      return true;
    }
  };
  /**
   * Specifies that each input file should have a corresponding tabix index
   */
  public static final FileConstraint TABIX = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      final File tabix = new File(f.getPath() + TabixIndexer.TABIX_EXTENSION);
      if (!tabix.exists()) {
        files.error(ErrorType.INFO_ERROR, "The file \"" + f.getPath() + "\" does not have a tabix index");
        return false;
      }
      return true;
    }
  };
  /**
   * Specifies that the parent directory should be writable so you can create files there
   */
  public static final FileConstraint CAN_CREATE = new FileConstraint() {
      @Override
      public boolean validate(File f, CommandLineFiles files) {
        if (f.getParentFile() == null || !f.getParentFile().canWrite()) {
          files.error(ErrorType.INFO_ERROR, f.getPath() + " can not be created");
          return false;
        }
        return true;
      }
    };

    /**
     * Specifies that each input file should have a corresponding tabix or bam index
     */
  public static final FileConstraint VARIANT_INPUT = new FileConstraint() {
    @Override
    public boolean validate(File f, CommandLineFiles files) {
      if (f.getName().toLowerCase(Locale.getDefault()).endsWith(CommonFlags.RECALIBRATE_EXTENSION)) {
        return true;
      }
      if (f.getName().toLowerCase(Locale.getDefault()).endsWith(SamUtils.BAM_SUFFIX)) {
        final File bamIndexA = new File(f.getPath() + SamUtils.BAI_SUFFIX);
        final File bamIndexB = new File(f.getPath().substring(0, f.getPath().length() - SamUtils.BAM_SUFFIX.length()) + SamUtils.BAI_SUFFIX);
        if (!bamIndexA.exists() && !bamIndexB.exists()) {
          files.error(ErrorType.INFO_ERROR, "The file \"" + f.getPath() + "\" does not have a valid index");
          return false;
        }
      } else {
        final File tabix = new File(f.getPath() + TabixIndexer.TABIX_EXTENSION);
        if (!tabix.exists()) {
          files.error(ErrorType.INFO_ERROR, "The file \"" + f.getPath() + "\" does not have a tabix index");
          return false;
        }
      }
      return true;
    }
  };

  /**
   * @return a <code>CommandLineFile</code> object for our standard input files
   */
  public static CommandLineFiles inputFiles() {
    return new CommandLineFiles(CommonFlags.INPUT_LIST_FLAG, null, EXISTS, NOT_DIRECTORY);
  }
  /**
   * @return a <code>CommandLineFile</code> object for our standard output files
   */
  public static CommandLineFiles outputFile() {
    return new CommandLineFiles(null, CommonFlags.OUTPUT_FLAG, DOES_NOT_EXIST, CAN_CREATE);
  }
  /**
   * @return a <code>CommandLineFile</code> object for our SDF input files
   */
  public static CommandLineFiles sdfFiles() {
    return new CommandLineFiles(CommonFlags.INPUT_LIST_FLAG, null, EXISTS, SDF);
  }


  private final String mListFileFlag;
  private final String mSingleInputFlag;

  // The way this is being handled makes this class not thread safe...
  private int mErrorCount;
  private final List<FileConstraint> mConstraints = new ArrayList<>();


 /**
   * Construct a file list collector with the specified constraints.
   * @param listFileFlag name of the flag containing the name of a file that is a list of input files. May be null if there is no corresponding flag
   * @param singleInputFlag name of the flag that contains individual file names. If null will look at the anonymous flag.
   * @param constraints list of constraints that should be placed on the file list.
   *
   */
  public CommandLineFiles(String listFileFlag, String singleInputFlag, FileConstraint... constraints) {
    this(listFileFlag, singleInputFlag);
    mConstraints.addAll(Arrays.asList(constraints));
  }

  /**
   * Construct a file list collector with an empty constraint set.
   * @param listFileFlag name of the flag containing the name of a file that is a list of input files. May be null if there is no corresponding flag
   * @param singleInputFlag name of the flag that contains individual file names. If null will look at the anonymous flag.
   */
  public CommandLineFiles(String listFileFlag, String singleInputFlag) {
    mListFileFlag = listFileFlag;
    mSingleInputFlag = singleInputFlag;
  }

  /**
   * @param constraint an additional constraint files must meet in order to be valid.
   */
  public void addConstraint(FileConstraint constraint) {
    mConstraints.add(constraint);
  }

  /**
   * Check that the file meets all constraints specified by this class
   * @param f the file to check
   * @return true if the file is valid. false otherwise.
   */
  public boolean validate(File f) {
    for (FileConstraint constraint : mConstraints) {
      if (!constraint.validate(f, this)) {
        return false;
      }

    }
    return true;
  }

  /**
   * Log an error in file validation.
   * @param type the type of error detected
   * @param message a textual description of the error, you should probably include the filename.
   */
  public void error(ErrorType type, String message) {
    if (mErrorCount < MAX_ERRORS) {
      Diagnostic.error(type, message);
    }
  }

   /**
    * Get the list of files from the anonymous flag and/or read a list of files from a file.
    * @param flags the command line flags to parse files from
    * @return the list of files to process or null if there are missing files in the list
    * @throws java.io.IOException if reading the file list fails
    * @throws NoTalkbackSlimException if too many files fail validation.
    */
  public List<File> getFileList(CFlags flags) throws IOException {
    final List<File> files = new ArrayList<>();
    mErrorCount = 0;

    final Collection<Object> fValues;
    if (mSingleInputFlag == null) {
      fValues = flags.getAnonymousValues(0);
    } else {
      fValues = flags.getValues(mSingleInputFlag);
    }
    for (final Object o : fValues) {
      final File f = (File) o;
      if (!validate(f)) {
        if (++mErrorCount > MAX_ERRORS) {
          throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "There were more than " + MAX_ERRORS + " invalid input file paths");
        }
      }
      files.add(f);
    }

    if (mListFileFlag != null && flags.isSet(mListFileFlag)) {
      final File listFile = (File) flags.getValue(mListFileFlag);
      try (BufferedReader br = new BufferedReader(new FileReader(listFile))) {
        for (String line = br.readLine(); line != null; line = br.readLine()) {
          line = line.trim();
          if ((line.length() > 0) && !line.startsWith("#")) {
            final File f = new File(line);
            if (!validate(f)) {
              if (++mErrorCount > MAX_ERRORS) {
                throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "There were more than " + MAX_ERRORS + " invalid input file paths");
              }
            }
            files.add(f);
          }
        }
      }
    }

    if (mErrorCount > 0) {
      throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "There were " + mErrorCount + " invalid input file paths");
    }
    return files;
  }

  /**
   * Just read a list of files into a collection
   * @param listFile file containing list of files
   * @return the list of files
   * @throws IOException if an IO error occurs
   */
  public static Collection<File> getFileList(File listFile) throws IOException {
    final List<File> files = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(listFile))) {
      for (String line = br.readLine(); line != null; line = br.readLine()) {
        line = line.trim();
        if ((line.length() > 0) && !line.startsWith("#")) {
          final File f = new File(line);
          files.add(f);
        }
      }
    }
    return files;
  }
}
