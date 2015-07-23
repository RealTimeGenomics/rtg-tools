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


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.rtg.reference.ReferenceGenome;
import com.rtg.util.Constants;
import com.rtg.util.Environment;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.intervals.RegionRestriction;

/**
 * Container for common flags
 */
public final class CommonFlags {

  private CommonFlags() { }

  /** output filter flag */
  public static final String OUTPUT_FILTER_FLAG = "output-filter";
  /** orientation of proper pairs */
  public static final String PAIR_ORIENTATION_FLAG = "orientation";
  /** minimum fragment flag */
  public static final String MIN_FRAGMENT_SIZE = "min-fragment-size";
  /** maximum fragment size flag */
  public static final String MAX_FRAGMENT_SIZE = "max-fragment-size";
  /** sort flag */
  public static final String SORT_FLAG = "sort";
  /** Flag which decides if unmapped sequences are to be written. */
  public static final String EXCLUDE_FLAG = "Xexclude";
  /** Flag for using ids instead of names of sequences in output. */
  public static final String USEIDS_FLAG = "Xuseids";
  /** flag for disabling compression of output files */
  public static final String NO_GZIP = "no-gzip";
  /** Specify directory for temporary files */
  public static final String TEMP_DIR = "tempdir";
  /** indel flag */
  public static final String INDELS_FLAG = "indels";
  /** template flag */
  public static final String TEMPLATE_FLAG = "template";
  /** reads flag */
  public static final String READS_FLAG = "input";
  /** Name of the output file. */
  public static final String OUTPUT_FLAG = "output";
  /** threads flag */
  public static final String THREADS_FLAG = "threads";
  /** Flag for disabling the maximum file check **/
  public static final String NO_MAX_FILES_FLAG = "Xno-max-files";
  /** the read id of the first read to map */
  public static final String START_READ_ID = "start-read";
  /** the read id of the last read to map */
  public static final String END_READ_ID = "end-read";
  /** list of input files */
  public static final String INPUT_LIST_FLAG = "input-list-file";
  /** flag for suppressing TABIX and BAM index creation */
  public static final String NO_INDEX = "no-index";
  /** Flag name for restricting operation to within a single region. */
  public static final String RESTRICTION_FLAG = "region";
  /** Flag name for restricting operation to within regions contained in a BED file. */
  public static final String BED_REGIONS_FLAG = "bed-regions";

  /** input sequence quality type flag */
  public static final String QUALITY_FLAG = "quality-format";
  /** Sanger quality format */
  public static final String SANGER_FORMAT = "sanger";
  /** Solexa quality format */
  public static final String SOLEXA_FORMAT = "solexa";
  /** Illumina quality format */
  public static final String ILLUMINA_FORMAT = "illumina";
  static final String[] QUALITY_FORMAT_OPTIONS = {SANGER_FORMAT, SOLEXA_FORMAT, ILLUMINA_FORMAT};

  // Property name that says where to load AVR models from
  static final String ENVIRONMENT_MODELS_DIR = "models.dir";
  // Default AVR model name looked for within the above dir
  private static final String MODEL_DEFAULT = "illumina-exome.avr";
  // Name to explicitly specify no AVR model to be used
  private static final String AVR_NONE_NAME = "none";

  /** flag for loading an AVR model to use for predictions */
  public static final String AVR_MODEL_FILE_FLAG = "avr-model";
  /** Minimum AVR Score Filter Flag */
  public static final String FILTER_AVR_FLAG = "min-avr-score";

  //commonly used flag description strings
  /** DIR */
  public static final String DIR = "DIR";
  /** SDF */
  public static final String SDF = "SDF";
  /** INT */
  public static final String INT = "INT";
  /** FLOAT */
  public static final String FLOAT = "FLOAT";
  /** BOOL */
  public static final String BOOL = "BOOL";
  /** FILE */
  public static final String FILE = "FILE";
  /** STRING */
  public static final String STRING = "STRING";
  /** FILE OR SDF */
  public static final String SDF_OR_FILE = "SDF|FILE";

  /** File name argument used to indicate read from stdin or write to stdout */
  public static final String STDIO_NAME = "-";

  /** Name of summary file. */
  public static final String SUMMARY_FILE = "summary.txt";

  /** Filename extension used to indicate SAM/BAM calibration files */
  public static final String RECALIBRATE_EXTENSION = ".calibration";


  /**
   * Validate the reads flags.
   * @param flags the flags
   * @param sdf true if reads are in SDF format
   * @return true if valid, otherwise false
   */
  public static boolean validateReads(CFlags flags, boolean sdf) {
    if (sdf && !validateSDF(flags, READS_FLAG)) {
      return false;
    }
    return validateStartEnd(flags, CommonFlags.START_READ_ID, CommonFlags.END_READ_ID);
  }

  /**
   * Validate start and end flag for sequences
   * @param flags object containing flags
   * @param startFlag flag representing start sequence
   * @param endFlag flag representing end sequence
   * @return true if valid
   */
  public static boolean validateStartEnd(CFlags flags, String startFlag, String endFlag) {
    if (flags.isSet(startFlag)) {
      if ((Long) flags.getValue(startFlag) < 0) {
        flags.error("--" + startFlag + " should be positive");
        return false;
      }
    }
    if (flags.isSet(endFlag)) {
      if ((Long) flags.getValue(endFlag) < 1) {
        flags.error("--" + endFlag + " should be greater than 0");
        return false;
      }
    }
    if (flags.isSet(startFlag) && flags.isSet(endFlag)) {
      final long start = (Long) flags.getValue(startFlag);
      final long end = (Long) flags.getValue(endFlag);
      if (start >= end) {
        flags.error("--" + startFlag + " should be less than --" + endFlag);
        return false;
      }
      if (end - start > Integer.MAX_VALUE) {
        flags.error("You have specified too many sequences, please specify a range of less than " + Integer.MAX_VALUE);
        return false;
      }
    }
    return true;
  }

  /**
   * Validate the templates flags.
   * @param flags the flags
   * @return true if valid, otherwise false
   */
  public static boolean validateTemplate(final CFlags flags) {
    return validateSDF(flags, TEMPLATE_FLAG);
  }

  /**
   * Basic SDF file check
   * @param flags flags to check
   * @param flagName name of the flag
   * @return true if indicated path exists and is a directory
   */
  public static boolean validateSDF(CFlags flags, String flagName) {
    return validateSDF((File) flags.getFlag(flagName).getValue());
  }

  /**
   * Basic SDF file check
   * @param file file to check
   * @return true if indicated path exists and is a directory
   */
  public static boolean validateSDF(File file) {
    if (!file.exists()) {
      Diagnostic.error(ErrorType.SDF_NOT_FOUND, file.getPath());
      return false;
    }
    if (!file.isDirectory()) {
      Diagnostic.error(ErrorType.NOT_SDF, file.getPath());
      return false;
    }
    return true;
  }

  /**
   * Validate the output dir flags.
   * @param flags the flags
   * @return true if valid, otherwise false
   */
  public static boolean validateOutputDirectory(final CFlags flags) {
    final File outputDir = (File) flags.getValue(OUTPUT_FLAG);
    return validateOutputDirectory(outputDir);
  }

  /**
   * Validate the threads flags.
   * @param flags the flags
   * @return true if valid, otherwise false
   */
  public static boolean validateThreads(final CFlags flags) {
    if (flags.isSet(THREADS_FLAG)) {
      final int threads = (Integer) flags.getValue(THREADS_FLAG);
      if (threads <= 0) {
        Diagnostic.error(ErrorType.INVALID_MIN_INTEGER_FLAG_VALUE, "--" + THREADS_FLAG, threads + "", "1");
        return false;
      }
      final int maxThreads = Environment.getAvailableProcessors() * 10;
      if (threads > maxThreads) {
        Diagnostic.error(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "--" + THREADS_FLAG, threads + "", maxThreads + "");
        return false;
      }
    }
    return true;
  }

  /**
   * Validates an integer flag has a value between two values
   * @param flags the flags
   * @param flagName the flag name to check
   * @param lowValue the low value (inclusive)
   * @param highValue the high value (inclusive)
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public static boolean validateFlagBetweenValues(CFlags flags, String flagName, int lowValue, int highValue) {
    final int value = (Integer) flags.getValue(flagName);
    if (value < lowValue) {
      Diagnostic.error(ErrorType.INVALID_MIN_INTEGER_FLAG_VALUE, "--" + flagName, value + "", lowValue + "");
      return false;
    }
    if (value > highValue) {
      Diagnostic.error(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "--" + flagName, value + "", highValue + "");
      return false;
    }
    return true;
  }

  /**
   * Validates an float flag has a value between two values
   * @param flags the flags
   * @param flagName the flag name to check
   * @param lowValue the low value (inclusive)
   * @param highValue the high value (inclusive)
   * @return true if the flag exists and is between the values, otherwise false.
   */
  public static boolean validateFlagBetweenValues(CFlags flags, String flagName, double lowValue, double highValue) {
    final double value = (double) flags.getValue(flagName);
    if (value < lowValue) {
      Diagnostic.error(ErrorType.INVALID_MIN_INTEGER_FLAG_VALUE, "--" + flagName, value + "", lowValue + "");
      return false;
    }
    if (value > highValue) {
      Diagnostic.error(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "--" + flagName, value + "", highValue + "");
      return false;
    }
    return true;
  }

  /**
   *
   * @param flags shared flags
   */
  public static void initNoMaxFile(CFlags flags) {
    flags.registerOptional(NO_MAX_FILES_FLAG, "override maximum number of files").setCategory(CommonFlagCategories.UTILITY);
  }

  /**
   * Initialise mapping IO flags (input, output, template)
   * @param flags shared flags
   */
  public static void initOutputDirFlag(CFlags flags) {
    flags.registerRequired('o', OUTPUT_FLAG, File.class, DIR, "directory for output").setCategory(CommonFlagCategories.INPUT_OUTPUT);
  }

  /**
   * Initialize flag for specifying quality format of FASTQ data
   * @param flags shared flags
   */
  public static void initQualityFormatFlag(CFlags flags) {
    final Flag qualFormatFlag = flags.registerOptional('q', QUALITY_FLAG, String.class, "FORMAT", "format of quality data for fastq files (use sanger for Illumina 1.8+)").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    qualFormatFlag.setParameterRange(QUALITY_FORMAT_OPTIONS);
  }

  /**
   * initialize flag to not compress output.
   * @param flags flags to register with
   */
  public static void initNoGzip(CFlags flags) {
    flags.registerOptional('Z', NO_GZIP, "do not gzip the output").setCategory(CommonFlagCategories.UTILITY);
  }

  /**
   * initialize flag to read number of threads.
   * @param flags flags to register with
   */
  public static void initThreadsFlag(CFlags flags) {
      flags.registerOptional('T', THREADS_FLAG, Integer.class, INT, "number of threads. Defaults to the number of available cores").setCategory(CommonFlagCategories.UTILITY);
  }

  /**
   * initialize flag to suppress TABIX and BAM index creation
   * @param flags flags to register with
   */
  public static void initIndexFlags(CFlags flags) {
    flags.registerOptional(NO_INDEX, "do not produce indexes for output files").setCategory(CommonFlagCategories.UTILITY);
  }

  /**
   * Initialise flags for ranges of reads
   * @param flags shared flags
   */
  public static void initReadRange(final CFlags flags) {
    flags.registerOptional(CommonFlags.START_READ_ID, Long.class, "INT", "inclusive lower bound on read id").setCategory(CommonFlagCategories.FILTERING);
    flags.registerOptional(CommonFlags.END_READ_ID, Long.class, "INT", "exclusive upper bound on read id").setCategory(CommonFlagCategories.FILTERING);
  }

  /**
   * Initialise flag for minimum AVR score
   * @param flags shared flags
   */
  public static void initMinAvrScore(final CFlags flags) {
    flags.registerOptional(FILTER_AVR_FLAG, Double.class, "Float", "if set, fail variants with AVR scores below this value").setCategory(CommonFlagCategories.REPORTING);
  }

  /**
   * Initialise flag for AVR model to use for prediction
   *
   * @param flags shared flags
   * @param anonymous if true, register the flag as an anonymous flag
   * @return the newly registered flag
   */
  public static Flag initAvrModel(final CFlags flags, boolean anonymous) {
    String description = "name of AVR model to use when scoring variants";
    boolean hasDefault = false;
    final String modelDirName = Environment.getEnvironmentMap().get(ENVIRONMENT_MODELS_DIR);
    if (modelDirName != null) {
      final File modelDir = new File(modelDirName);
      if (modelDir.exists() && modelDir.isDirectory()) {
        final String[] models = modelDir.list(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.endsWith(".avr");
          }
        });
        if (!anonymous && models != null && models.length > 0) {
          final String[] newModels = new String[models.length + 1];
          newModels[0] = AVR_NONE_NAME;
          System.arraycopy(models, 0, newModels, 1, models.length);
          description += " (Must be one of " + Arrays.toString(newModels) + " or a path to a model file)";
          if (Arrays.asList(newModels).contains(MODEL_DEFAULT)) {
            hasDefault = true;
          }
        }
      }
    }
    final Flag modelFlag = anonymous
        ? flags.registerRequired(File.class, "file", description).setCategory(CommonFlagCategories.REPORTING)
        : flags.registerOptional(AVR_MODEL_FILE_FLAG, File.class, "file", description).setCategory(CommonFlagCategories.REPORTING);
    if (!anonymous && hasDefault) {
      modelFlag.setParameterDefault(MODEL_DEFAULT);
    }
    return modelFlag;
  }

  // If the environment for models is set up and the default model exists within it, return the default model, otherwise null
  private static File defaultAvrModel() {
    final String modelsDir = Environment.getEnvironmentMap().get(ENVIRONMENT_MODELS_DIR);
    if (modelsDir == null) {
      return null;
    } else {
      final File modelsDirFile = new File(modelsDir);
      if (!modelsDirFile.exists() || !modelsDirFile.isDirectory()) {
        throw new InvalidParamsException("The AVR models directory cannot be found or is not a directory: " + modelsDir);
      }
      final File model = new File(modelsDir, MODEL_DEFAULT);
      return model.exists() ? model : null;
    }
  }

  /**
   * Gets the AVR model to use for prediction. If the user has supplied a model name, it will be first
   * searched for as an actual file name, or secondly as a name within the environmental model directory
   * (if configured)
   *
   *
   * @param flags shared flags
   * @param anonymous true if the model was registered as the only anonymous flag
   * @return a File pointing at the specified AVR model, or null if no AVR model is to be used
   * @throws InvalidParamsException if the user specified a model that does not exist, or a models directory which does
   * not exist or is not a directory.
   */
  public static File getAvrModel(final CFlags flags, boolean anonymous) {
    File avrModel = defaultAvrModel();
    final Flag flag = anonymous ? flags.getAnonymousFlag(0) : flags.getFlag(AVR_MODEL_FILE_FLAG);
    if (flag != null && flag.isSet()) {
      final String modelsDir = Environment.getEnvironmentMap().get(ENVIRONMENT_MODELS_DIR);
      final File userModel = (File) flag.getValue();
      if (AVR_NONE_NAME.equals(userModel.toString())) {
        return null;
      } else if (userModel.exists()) {
        avrModel = userModel;
      } else if (modelsDir != null) {
        avrModel = new File(modelsDir, userModel.getName());
      }
      if (avrModel == null || !avrModel.exists()) {
        throw new InvalidParamsException("The specified AVR model could not be found: " + userModel.toString());
      }
    }
    return avrModel;
  }

  /**
   * Returns true if the input file is the stdin/stdout indicator
   * @param file to test
   * @return true if the file indicates stdin/stdout should be used
   */
  public static boolean isStdio(File file) {
    return isStdio(file.toString());
  }

  /**
   * Returns true if the input file is the stdin/stdout indicator
   * @param filename to test
   * @return true if the file indicates stdin/stdout should be used
   */
  public static boolean isStdio(String filename) {
    return STDIO_NAME.equals(filename);
  }

  /**
   * Check the file list and anonymous file input flags.
   * @param flags the flags to check
   * @param fileListFlag the flag name for the file list
   * @param singleInputFlag the flag name for single inputs, null if using anonymous
   * @param maxFiles the maximum number of files allowed
   * @return <code>true</code> if all okay <code>false</code> otherwise
   */
  public static boolean checkFileList(CFlags flags, String fileListFlag, String singleInputFlag, int maxFiles) {
    return checkFileList(flags, fileListFlag, singleInputFlag, maxFiles, false);
  }

  /**
   * Check the file list and anonymous file input flags.
   * @param flags the flags to check
   * @param fileListFlag the flag name for the file list
   * @param singleInputFlag the flag name for single inputs, null if using anonymous
   * @param maxFiles the maximum number of files allowed
   * @param ignoreCalibrationFiles ignores *.calibration files while counting number of files
   * @return <code>true</code> if all okay <code>false</code> otherwise
   */
  public static boolean checkFileList(CFlags flags, String fileListFlag, String singleInputFlag, int maxFiles, boolean ignoreCalibrationFiles) {
    final Collection<File> files;
    try {
      files = new CommandLineFiles(fileListFlag, singleInputFlag, CommandLineFiles.EXISTS).getFileList(flags);
    } catch (final NoTalkbackSlimException e) {
      flags.setParseMessage(e.getMessage());
      return false;
    } catch (final IOException e) {
      flags.setParseMessage("An error occurred reading " + flags.getValue(fileListFlag));
      return false;
    }
    if (getSize(files, ignoreCalibrationFiles) == 0) {
      flags.setParseMessage("No input files specified" + (null == singleInputFlag ? "" : (" in --" + fileListFlag + " or --" + singleInputFlag)) + ".");
      return false;
    } else if (getSize(files, ignoreCalibrationFiles) > maxFiles && !flags.isSet(NO_MAX_FILES_FLAG)) {
      flags.setParseMessage("More than " + maxFiles + " input files specified.");
      return false;
    }
    return true;
  }

  /**
   * Function counts the files size
   * @param files Collection containing files
   * @param ignoreCalibrationFiles if true, ignores <code>.calibration</code> file count
   * @return count of files
   */
  private static int getSize(Collection<File> files, boolean ignoreCalibrationFiles) {
    if (!ignoreCalibrationFiles) {
      return files.size();
    }
    int count = 0;
    for (final File f : files) {
      if (!f.getName().endsWith(RECALIBRATE_EXTENSION)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Get the list of files from the anonymous flag and a file list file flag.
   * @param flags the flags to get the list from
   * @param fileListFlag the flag name for the file list
   * @param singleInputFlag the flag name for single inputs, null if using anonymous
   * @param sdf check for valid SDF directories instead of for files
   * @return the list of files to process or null if there are missing files in the list
   * @throws IOException if reading the file list fails
   */
  public static List<File> getFileList(CFlags flags, String fileListFlag, String singleInputFlag, boolean sdf) throws IOException {
    final CommandLineFiles files = new CommandLineFiles(fileListFlag, singleInputFlag, CommandLineFiles.EXISTS);
    if (sdf) {
      files.addConstraint(CommandLineFiles.SDF);
    } else {
      files.addConstraint(CommandLineFiles.NOT_DIRECTORY); // REGULAR_FILE breaks bash-fu
    }
    return files.getFileList(flags);
  }

  /**
   * Test if the supplied directory already exists.
   * @param directory directory to test
   * @return true if directory is valid, otherwise a false and calls
   * <code>Diagnostic.error</code> with details of the reason it is invalid.
   * @exception NullPointerException if <code>directory</code> is null.
   */
  public static boolean validateOutputDirectory(File directory) {
    if (directory.exists()) {
      Diagnostic.error(ErrorType.DIRECTORY_EXISTS, directory.getPath());
      return false;
    }
    return true;
  }

  /**
   * Checks the supplied output file is not '-', for commands that do not allow writing to stdout.
   * @param outfile the output file
   * @return false if validation failed
   */
  public static boolean validateNotStdout(File outfile) {
    if (isStdio(outfile)) {
      Diagnostic.error("This command does not support sending output to stdout");
      return false;
    }
    return true;
  }

  /**
   * Get the region defined by the start read and end read flags.
   * @param flags the flags to get the region from
   * @return the region to process
   */
  public static LongRange getReaderRestriction(CFlags flags) {
    final long start = flags.isSet(CommonFlags.START_READ_ID) ? (Long) flags.getValue(CommonFlags.START_READ_ID) : LongRange.MISSING;
    final long end = flags.isSet(CommonFlags.END_READ_ID) ? (Long) flags.getValue(CommonFlags.END_READ_ID) : LongRange.MISSING;
    return new LongRange(start, end);
  }

  /**
   * Check that the sex and template values are compatible.
   * @param flags the flags to check
   * @param sexFlag the flag specifying the sex
   * @param pedigreeFlag the flag specifying the pedigree
   * @param templateFlag the flag specifying the template
   * @return <code>true</code> if all okay <code>false</code> otherwise
   */
  public static boolean validateSexTemplateReference(CFlags flags, String sexFlag, String pedigreeFlag, String templateFlag) {
    return validateSexTemplateReference(flags, sexFlag, pedigreeFlag, (File) flags.getValue(templateFlag));
  }

  /**
   * Check that the sex and template values are compatible.
   * @param flags the flags to check
   * @param sexFlag the flag specifying the sex
   * @param pedigreeFlag the flag specifying the pedigree
   * @param template the File corresponding to the template SDF  @return <code>true</code> if all okay <code>false</code> otherwise
   * @return <code>true</code> if all okay <code>false</code> otherwise
   */
  public static boolean validateSexTemplateReference(CFlags flags, String sexFlag, String pedigreeFlag, File template) {
    if (flags.isSet(sexFlag) || (pedigreeFlag != null && flags.isSet(pedigreeFlag))) {
      if (!new File(template, ReferenceGenome.REFERENCE_FILE).isFile()) {
        flags.error("Sex-specific processing was specified but " + template + " is missing a '" + ReferenceGenome.REFERENCE_FILE + "'");
        return false;
      }
    }
    return true;
  }

  /**
   * Returns number of I/O threads from a threads parameter (where 1
   * thread is reserved for main processing).  The total number of
   * threads is user supplied, otherwise the minimum of the number of
   * available processors and Constants.MAX_THREADS.
   * @param threads number of threads
   * @return an <code>int</code> value
   */
  public static int parseIOThreads(Integer threads) {
    final int result = threads == null ? Math.min(Constants.MAX_IO_THREADS, Environment.defaultThreads()) : threads;
    return Math.max(1, result - 1);
  }

  /**
   * Returns number of threads.  If the <code>threads</code> parameter is null then the default thread number will be returned
   * @param threads number of threads
   * @return an <code>int</code> value
   */
  public static int parseThreads(Integer threads) {
    return threads == null ? Environment.defaultThreads() : threads;
  }

  /**
   * Check that the region and bed-regions flags are appropriate, if set.
   * @param flags the flags to check
   * @return <code>true</code> if all okay <code>false</code> otherwise
   */
  public static boolean validateRegions(CFlags flags) {
    if (flags.isSet(RESTRICTION_FLAG)) {
      final String region = (String) flags.getValue(RESTRICTION_FLAG);
      if (!RegionRestriction.validateRegion(region)) {
        flags.setParseMessage("The value \"" + region + "\" for \"--" + RESTRICTION_FLAG + "\" is malformed.");
        return false;
      }
    }
    if (flags.isSet(BED_REGIONS_FLAG)) {
      final File bedRegionsFile = (File) flags.getFlag(BED_REGIONS_FLAG).getValue();
      if (!bedRegionsFile.exists()) {
        Diagnostic.error(ErrorType.FILE_NOT_FOUND, "The specified file, \"" + bedRegionsFile.getPath() + "\", does not exist.");
        return false;
      }
    }
    if (!flags.checkNand(RESTRICTION_FLAG, BED_REGIONS_FLAG)) {
      return false;
    }
    return true;
  }
}
