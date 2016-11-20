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
package com.rtg.util.diagnostic;

import java.io.Serializable;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 * Enumeration of SLIM errors.
 *
 */
public final class ErrorType implements DiagnosticType, Serializable, PseudoEnum {

  private static int sCounter = -1;
  /**
   * An internal error has occurred.  This is used when something unexpected has
   * happened and is generally characterized by an unexpected <code>Throwable</code>
   * coming from the bowels of the engine.
   */
  public static final ErrorType SLIM_ERROR = new ErrorType(++sCounter, "SLIM_ERROR", 0);

  /**
   * An error has occurred and we have an appropriate message for the user, however we can't
   * be bothered making yet another error type object.
   * You should supply one argument which is the message to display for this error
   */
  public static final ErrorType INFO_ERROR = new ErrorType(++sCounter, "INFO_ERROR", 1);

  /**
   * A file, directory, or other resource was found that appears to have been written
   * by a more recent version of SLIM. The parameter is the offending resource.
   */
  public static final ErrorType FUTURE_VERSION_ERROR = new ErrorType(++sCounter, "FUTURE_VERSION_ERROR", 1);

  /**
   * User has specified an inappropriate size for a file chunk.  This error will
   * typically occur during the processing of command line arguments for specifying
   * a file chunking size.
   */
  public static final ErrorType BAD_FILE_CHUNK_SIZE = new ErrorType(++sCounter, "BAD_FILE_CHUNK_SIZE", 0);

  /**
   * Directory already exists.  This operation required a nonexistent name.
   * The parameter is the name of the directory.
   */
  public static final ErrorType DIRECTORY_EXISTS = new ErrorType(++sCounter, "DIRECTORY_EXISTS", 1);

  /**
   * Directory should exist but could not be opened as a directory.
   * The parameters is the name of the directory.
   */
  public static final ErrorType DIRECTORY_NOT_EXISTS = new ErrorType(++sCounter, "DIRECTORY_NOT_EXISTS", 1);

  /**
   * Directory could not be created. The parameter is the directory name.
   */
  public static final ErrorType DIRECTORY_NOT_CREATED = new ErrorType(++sCounter, "DIRECTORY_NOT_CREATED", 1);

  /**
   * Specified name is not a directory. The parameter is the name.
   */
  public static final ErrorType NOT_A_DIRECTORY = new ErrorType(++sCounter, "NOT_A_DIRECTORY", 1);

  /**
   * The file format specified was not acceptable.  The parameter is what was
   * given.
   */
  public static final ErrorType INVALID_FILE_FORMAT = new ErrorType(++sCounter, "INVALID_FILE_FORMAT", 1);

  /**
   * The value supplied for the maximum number of times that a hash code can
   * occur is less than 1.
   */
  public static final ErrorType INVALID_MAX_FREQUENCY = new ErrorType(++sCounter, "INVALID_MAX_FREQUENCY", 1);

  /**
   * The parameter was supposed to be a positive integer.
   */
  public static final ErrorType EXPECTED_POSITIVE = new ErrorType(++sCounter, "EXPECTED_POSITIVE", 1);

  /**
   * The parameter was supposed to be a nonnegative integer.
   */
  public static final ErrorType EXPECTED_NONNEGATIVE = new ErrorType(++sCounter, "EXPECTED_NONNEGATIVE", 1);

  /**
   * All disk space has been exhausted on some filesystem where SLIM was trying
   * to write data.
   */
  public static final ErrorType DISK_SPACE = new ErrorType(++sCounter, "DISK_SPACE", 1);

  /**
   * Sequence too long to be read or processed.
   */
  public static final ErrorType SEQUENCE_TOO_LONG = new ErrorType(++sCounter, "SEQUENCE_TOO_LONG", 1);

  /**
   * License is invalid.
   */
  public static final ErrorType INVALID_LICENSE = new ErrorType(++sCounter, "INVALID_LICENSE", 0);

  /**
   * File already exist. This operation requires a non-existent File.
   * The parameter is the name of the existing file.
   */
  public static final ErrorType FILE_EXISTS = new ErrorType(++sCounter, "FILE_EXISTS", 1);

  /**
   * File could not be created. The parameter is the file name.
   */
  public static final ErrorType FILE_NOT_CREATED = new ErrorType(++sCounter, "FILE_NOT_CREATED", 1);

  /**
   * An error occurred while reading a file. The parameter is the file name.
   */
  public static final ErrorType READING_ERROR = new ErrorType(++sCounter, "READING_ERROR", 1);

  /**
   * Unable to recognize the current environment (operating system, JVM or machine) sufficiently well to run.
   */
  public static final ErrorType UNABLE_TO_RECOGNIZE_ENVIRONMENT = new ErrorType(++sCounter, "UNABLE_TO_RECOGNIZE_ENVIRONMENT", 0);

  /**
   * A error has occurred while writing a file in the specified directory.
   */
  public static final ErrorType WRITING_ERROR = new ErrorType(++sCounter, "WRITING_ERROR", 1);

  /**
   * No valid input files were specified.
   */
  public static final ErrorType NO_VALID_INPUTS = new ErrorType(++sCounter, "NO_VALID_INPUTS", 0);

  /**
   * One or more input sequences are too long for the requested operation.
   */
  public static final ErrorType SEQUENCE_LENGTH_ERROR = new ErrorType(++sCounter, "SEQUENCE_LENGTH_ERROR", 0);

  /**
   * Colorspace only valid for DNA.
   */
  public static final ErrorType COLORSPACE_DNA = new ErrorType(++sCounter, "COLORSPACE_DNA", 0);

  /**
   * User has selected an invalid mask name.
   */
  public static final ErrorType INVALID_MASK = new ErrorType(++sCounter, "INVALID_MASK", 1);

  /**
   * User has selected a word size that is too large for the current application.
   */
  public static final ErrorType WORD_SIZE_TOO_LARGE = new ErrorType(++sCounter, "WORD_SIZE_TOO_LARGE", 2);

  /**
   * User has selected a word size that is invalid for the current application.
   */
  public static final ErrorType INVALID_WORD_SIZE = new ErrorType(++sCounter, "INVALID_WORD_SIZE", 3);

  /**
   * User has selected a step size that is invalid for the current application.
   */
  public static final ErrorType INVALID_STEP_SIZE = new ErrorType(++sCounter, "INVALID_STEP_SIZE", 1);

  /**
   * User has selected a repeat frequency that is invalid for the current application.
   */
  public static final ErrorType INVALID_REPEAT = new ErrorType(++sCounter, "INVALID_REPEAT", 1);

  /**
   * User has specified an SDF directory for input but it does not seem to contain a valid index file.
   */
  public static final ErrorType SDF_INDEX_NOT_VALID = new ErrorType(++sCounter, "SDF_INDEX_NOT_VALID", 1);

  /**
   * The given SDF sequences are of incorrect type
   */
  public static final ErrorType SDF_FILETYPE_ERROR = new ErrorType(++sCounter, "SDF_FILETYPE_ERROR", 2);

  /**
   * User has specified an invalid value for an integer flag. parameters are flag and minimum value
   */
  public static final ErrorType INVALID_INTEGER_FLAG_VALUE = new ErrorType(++sCounter, "INVALID_INTEGER_FLAG_VALUE", 2);

  /**
   * Step size must not exceed the word size.
   */
  public static final ErrorType STEP_SIZE_GREATER_THAN_WORD = new ErrorType(++sCounter, "STEP_SIZE_GREATER_THAN_WORD", 2);

  /**
   * The given format is invalid, parameter is unsupported format
   */
  public static final ErrorType OUTPUT_FORMAT_NOT_SUPPORTED = new ErrorType(++sCounter, "OUTPUT_FORMAT_NOT_SUPPORTED", 1);

  /**
   * The value of maxgap cannot be specified for the given output format.
   */
  public static final ErrorType MAXGAP_NOT_ALLOWED = new ErrorType(++sCounter, "MAXGAP_NOT_ALLOWED", 1);

  /**
   * verification failed
   */
  public static final ErrorType SDF_VERIFICATION_FAILED = new ErrorType(++sCounter, "SDF_VERIFICATION_FAILED", 0);

  /**
   * SDF file is older version, cannot be verified
   */
  public static final ErrorType SDF_VERSION_INVALID = new ErrorType(++sCounter, "SDF_VERSION_INVALID", 1);

  /**
   * SDF file does not exist
   */
  public static final ErrorType SDF_NOT_FOUND = new ErrorType(++sCounter, "SDF_NOT_FOUND", 1);

  /**
   * SDF file does not exist
   */
  public static final ErrorType NOT_SDF = new ErrorType(++sCounter, "NOT_SDF", 1);

  /**
   * SDF file is older version, cannot be verified
   */
  public static final ErrorType MASK_FLAGS_NOT_SET = new ErrorType(++sCounter, "MASK_FLAGS_NOT_SET", 0);

  /**
   * The specified flag "%1" has invalid value "%2". It should be less than or equal to "%3".
   */
  public static final ErrorType INVALID_MAX_INTEGER_FLAG_VALUE = new ErrorType(++sCounter, "INVALID_MAX_INTEGER_FLAG_VALUE", 3);

  /**
   * The "n" flag has been set but output format selected is "%1". It can only be used with either
   */
  public static final ErrorType TOPN_WITHOUT_OUTPUTFORMAT = new ErrorType(++sCounter, "TOPN_WITHOUT_OUTPUTFORMAT", 1);

  /**
   * Read file or read length is required
   */
  public static final ErrorType READS_FILE_OR_READLENGTH_REQUIRED = new ErrorType(++sCounter, "READS_FILE_OR_READLENGTH_REQUIRED", 0);

  /**
   * The specified flag "%1" has invalid value "%2". It should be greater than or equal to "%3".
   */
  public static final ErrorType INVALID_MIN_INTEGER_FLAG_VALUE = new ErrorType(++sCounter, "INVALID_MIN_INTEGER_FLAG_VALUE", 3);

  /**
   * The directory "%1" has been found but is not a valid directory created by format.
   */
  public static final ErrorType INVALID_FORMAT_DIRECTORY = new ErrorType(++sCounter, "INVALID_FORMAT_DIRECTORY", 1);

  /**
   * The word length "%1" should be less than the read length "%2".
   */
  public static final ErrorType WORD_NOT_LESS_READ = new ErrorType(++sCounter, "WORD_NOT_LESS_READ", 2);

  /**
   * \"%1" is an invalid expression for the number of threads to use.
   */
  public static final ErrorType THREAD_INVALID_EXPRESSION = new ErrorType(++sCounter, "THREAD_INVALID_EXPRESSION", 1);

  /**
   * Outputting numeric identifiers (rather than names) cannot be done for the format "%1".
   */
  public static final ErrorType CANNOT_USEIDS = new ErrorType(++sCounter, "CANNOT_USEIDS", 1);

  /**
   * There was not enough memory available for the task.
   */
  public static final ErrorType NOT_ENOUGH_MEMORY = new ErrorType(++sCounter, "NOT_ENOUGH_MEMORY", 0);

  /**
   * Quality section of sequence: %1 did not end with a new line. This could indicate there is more quality data than there should be.
   */
  public static final ErrorType BAD_FASTQ_QUALITY = new ErrorType(++sCounter, "BAD_FASTQ_QUALITY", 1);

  /**
   * Unrecognized symbols appeared before label symbol. Last sequence read was: "%1"
   */
  public static final ErrorType BAD_FASTA_LABEL = new ErrorType(++sCounter, "BAD_FASTA_LABEL", 1);

  /**
   * Warning for a sequence with unacceptable characters in the name. The parameter
   * is the (corrected) sequence name.
   */
  public static final ErrorType BAD_CHARS_NAME = new ErrorType(++sCounter, "BAD_CHARS_NAME", 1);

  /**
   * No quality section was found for sequence "%1", a quality section starts with a + symbol on a line by itself or with the sequence name.
   */
  public static final ErrorType NO_QUALITY_LABEL = new ErrorType(++sCounter, "NO_QUALITY_LABEL", 1);

  /**
   * Sequence "%1" does not contain as much quality data as sequence data.
   */
  public static final ErrorType NOT_ENOUGH_QUALITY = new ErrorType(++sCounter, "NOT_ENOUGH_QUALITY", 1);

  /**
   * Expected a nonnegative value "%1" for flag "%2".
   */
  public static final ErrorType EXPECTED_NONNEGATIVE_DOUBLE = new ErrorType(++sCounter, "EXPECTED_NONNEGATIVE_DOUBLE", 2);

  /**
   * Unable to locate "%1" as a properties file for priors.
   */
  public static final ErrorType PRIORS_NOT_FOUND = new ErrorType(++sCounter, "PRIORS_NOT_FOUND", 1);

  /**
   * Unable to locate key "%1" in properties file "%2".
   * Unable to locate key "%1" in prior option "%2".
   */
  public static final ErrorType PROPS_KEY_NOT_FOUND = new ErrorType(++sCounter, "PROPS_KEY_NOT_FOUND", 2);

  /**
   * Value "%1" for key '%2' in prior properties file "%3" invalid.
   * Value "%1" for key '%2' in prior option "%3" invalid.
   */
  public static final ErrorType PRIOR_KEY_VALUE_INVALID = new ErrorType(++sCounter, "PRIOR_KEY_VALUE_INVALID", 3);

  /**
   * Invalid Parameter given. %1
   */
  public static final ErrorType INVALID_PARAMETER = new ErrorType(++sCounter, "INVALID_PARAMETER", 1);

  /**
   * %1 property file "%2" is invalid (contains illegal Unicode escape characters).
   */
  public static final ErrorType PROPS_INVALID = new ErrorType(++sCounter, "PROPS_INVALID", 2);

  /**
   * %1 property file "%2" failed to load : "%3"
   */
  public static final ErrorType PROPS_LOAD_FAILED = new ErrorType(++sCounter, "PROPS_LOAD_FAILED", 3);

  /** Input looks like FASTQ rather than FASTA format. */
  public static final ErrorType FASTQ = new ErrorType(++sCounter, "FASTQ", 0);

  /** Input looks like FASTA rather than FASTQ format. */
  public static final ErrorType FASTA = new ErrorType(++sCounter, "FASTA", 0);

  /**
   * The specified flag "%1" has invalid value "%2". It should be greater than or equal to "%3" and less than or equal to "%4".
   */
  public static final ErrorType INVALID_INTEGER = new ErrorType(++sCounter, "INVALID_INTEGER", 4);

  /**
   * The specified word size "%1" is too large to support "%2" substitutions. Try lowering the word size or substitutions.
   */
  public static final ErrorType INVALID_LONG_READ_PARAMS = new ErrorType(++sCounter, "INVALID_LONG_READ_PARAMS", 2);

  /**
   * You cannot specify a step size when using short read mode.
   */
  public static final ErrorType INVALID_STEP_SHORT_READ = new ErrorType(++sCounter, "INVALID_STEP_SHORT_READ", 0);

  /** File not found. */
  public static final ErrorType FILE_NOT_FOUND = new ErrorType(++sCounter, "FILE_NOT_FOUND", 1);

  /** If the CG data contains reads of differing lengths */
  public static final ErrorType CG_LENGTH_ERROR = new ErrorType(++sCounter, "CG_LENGTH_ERROR", 1);

  /** General error reading file, and reason */
  public static final ErrorType FILE_READ_ERROR = new ErrorType(++sCounter, "FILE_READ_ERROR", 1);

  /** SAM file must be sorted */
  public static final ErrorType SAM_NOT_SORTED = new ErrorType(++sCounter, "SAM_NOT_SORTED", 0);

  /** SAM file %1 has bad format due to %2 */
  public static final ErrorType SAM_BAD_FORMAT = new ErrorType(++sCounter, "SAM_BAD_FORMAT", 2);

  /** SAM input has an irrecoverable error. %1 */
  public static final ErrorType SAM_BAD_FORMAT_NO_FILE = new ErrorType(++sCounter, "SAM_BAD_FORMAT_NO_FILE", 1);

  /** A file was required but given input %1 is not a file. */
  public static final ErrorType NOT_A_FILE =  new ErrorType(++sCounter, "NOT_A_FILE", 1);

  /** Not a CG SDF */
  public static final ErrorType NOT_A_CG_SDF =  new ErrorType(++sCounter, "NOT_A_CG_SDF", 1);

  /** Is a CG SDF */
  public static final ErrorType IS_A_CG_SDF =  new ErrorType(++sCounter, "IS_A_CG_SDF", 1);

  /** Word size of \"%1\" is too large for reads less than 64 in length, please choose a value &lt;= 32 */
  public static final ErrorType WORD_SIZE_TOO_LARGE_SHORT_READS =  new ErrorType(++sCounter, "WORD_SIZE_TOO_LARGE_SHORT_READS", 1);

  /** Reference does not match reads */
  public static final ErrorType WRONG_REFERENCE = new ErrorType(++sCounter, "WRONG_REFERENCE", 0);

  /** Error when SAM files cannot be merged because their headers are different. */
  public static final ErrorType SAM_INCOMPATIBLE_HEADER_ERROR = new ErrorType(++sCounter, "SAM_INCOMPATIBLE_HEADER_ERROR", 1);

  /** Error when SAM files cannot be merged because their headers are different. */
  public static final ErrorType FILES_NOT_FOUND = new ErrorType(++sCounter, "FILES_NOT_FOUND", 1);

  /** Reference does not match reads */
  public static final ErrorType CG_WRONG_VERSION = new ErrorType(++sCounter, "CG_WRONG_VERSION", 0);

  /** An IO error occurred: \"%1\" */
  public static final ErrorType IO_ERROR = new ErrorType(++sCounter, "IO_ERROR", 1);

  /** Quality data length did not match sequence length for sequence: \"%1\" */
  public static final ErrorType INVALID_QUALITY_LENGTH = new ErrorType(++sCounter, "INVALID_QUALITY_LENGTH", 1);

  /**
   * The given format is invalid, parameter is error msg
   */
  public static final ErrorType DOTNET_SDF_V4 = new ErrorType(++sCounter, "DOTNET_SDF_V4", 1);

  /** Reference does not match reads */
  public static final ErrorType NOT_A_CG_INPUT = new ErrorType(++sCounter, "NOT_A_CG_INPUT", 1);

  /** Not a paired end SDF folder */
  public static final ErrorType NOT_A_PAIRED_END_SDF = new ErrorType(++sCounter, "NOT_A_PAIRED_END_SDF", 1);

  /** reads &gt; 63 is not supported in current version */
  public static final ErrorType LONG_READ_NOT_SUPPORTED = new ErrorType(++sCounter, "LONG_READ_NOT_SUPPORTED", 0);

  /** Quality data was invalid. You may need to try a different format type. */
  public static final ErrorType INVALID_QUALITY = new ErrorType(++sCounter, "INVALID_QUALITY", 0);

  /** Mask params resulted in an invalid mask (skeleton etc) */
  public static final ErrorType INVALID_MASK_PARAMS = new ErrorType(++sCounter, "INVALID_MASK_PARAMS", 0);

  /** The use of a blacklist is not supported for this read set */
  public static final ErrorType BLACKLIST_LONG_READ_ONLY = new ErrorType(++sCounter, "BLACKLIST_LONG_READ_ONLY", 0);

  /** Number of parameters that must occur in conjunction with this warning. */
  private final int mParams;

  private final int mOrdinal;

  private final String mName;

  private ErrorType(final int ordinal, final String name, final int params) {
    mParams = params;
    mOrdinal = ordinal;
    mName = name;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public String name() {
    return mName;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  private static final EnumHelper<ErrorType> HELPER = new EnumHelper<>(ErrorType.class, new ErrorType[] {
    SLIM_ERROR,
    INFO_ERROR,
    FUTURE_VERSION_ERROR,
    BAD_FILE_CHUNK_SIZE,
    DIRECTORY_EXISTS,
    DIRECTORY_NOT_EXISTS,
    DIRECTORY_NOT_CREATED,
    NOT_A_DIRECTORY,
    INVALID_FILE_FORMAT,
    INVALID_MAX_FREQUENCY,
    EXPECTED_POSITIVE,
    EXPECTED_NONNEGATIVE,
    DISK_SPACE,
    SEQUENCE_TOO_LONG,
    INVALID_LICENSE,
    FILE_EXISTS,
    FILE_NOT_CREATED,
    READING_ERROR,
    UNABLE_TO_RECOGNIZE_ENVIRONMENT,
    WRITING_ERROR,
    NO_VALID_INPUTS,
    SEQUENCE_LENGTH_ERROR,
    COLORSPACE_DNA,
    INVALID_MASK,
    WORD_SIZE_TOO_LARGE,
    INVALID_WORD_SIZE,
    INVALID_STEP_SIZE,
    INVALID_REPEAT,
    SDF_INDEX_NOT_VALID,
    SDF_FILETYPE_ERROR,
    INVALID_INTEGER_FLAG_VALUE,
    STEP_SIZE_GREATER_THAN_WORD,
    OUTPUT_FORMAT_NOT_SUPPORTED,
    MAXGAP_NOT_ALLOWED,
    SDF_VERIFICATION_FAILED,
    SDF_VERSION_INVALID,
    SDF_NOT_FOUND,
    NOT_SDF,
    MASK_FLAGS_NOT_SET,
    INVALID_MAX_INTEGER_FLAG_VALUE,
    TOPN_WITHOUT_OUTPUTFORMAT,
    READS_FILE_OR_READLENGTH_REQUIRED,
    INVALID_MIN_INTEGER_FLAG_VALUE,
    INVALID_FORMAT_DIRECTORY,
    WORD_NOT_LESS_READ,
    THREAD_INVALID_EXPRESSION,
    CANNOT_USEIDS,
    NOT_ENOUGH_MEMORY,
    BAD_FASTQ_QUALITY,
    BAD_FASTA_LABEL,
    BAD_CHARS_NAME,
    NO_QUALITY_LABEL,
    NOT_ENOUGH_QUALITY,
    EXPECTED_NONNEGATIVE_DOUBLE,
    PRIORS_NOT_FOUND,
    PROPS_KEY_NOT_FOUND,
    PRIOR_KEY_VALUE_INVALID,
    INVALID_PARAMETER,
    PROPS_INVALID,
    PROPS_LOAD_FAILED,
    FASTQ,
    FASTA,
    INVALID_INTEGER,
    INVALID_LONG_READ_PARAMS,
    INVALID_STEP_SHORT_READ,
    FILE_NOT_FOUND,
    CG_LENGTH_ERROR,
    FILE_READ_ERROR,
    SAM_NOT_SORTED,
    SAM_BAD_FORMAT,
    SAM_BAD_FORMAT_NO_FILE,
    NOT_A_FILE,
    NOT_A_CG_SDF,
    IS_A_CG_SDF,
    WORD_SIZE_TOO_LARGE_SHORT_READS,
    WRONG_REFERENCE,
    SAM_INCOMPATIBLE_HEADER_ERROR,
    FILES_NOT_FOUND,
    CG_WRONG_VERSION,
    IO_ERROR,
    INVALID_QUALITY_LENGTH,
    DOTNET_SDF_V4,
    NOT_A_CG_INPUT,
    NOT_A_PAIRED_END_SDF,
    LONG_READ_NOT_SUPPORTED,
    INVALID_QUALITY,
    INVALID_MASK_PARAMS,
    BLACKLIST_LONG_READ_ONLY
  });

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str name of value
   * @return the enum value
   */
  public static ErrorType valueOf(final String str) {
    return HELPER.valueOf(str);
  }

  /**
   * @return list of enum values
   */
  public static ErrorType[] values() {
    return HELPER.values();
  }

  @Override
  public int getNumberOfParameters() {
    return mParams;
  }

  Object readResolve() {
    return values()[this.ordinal()];
  }

  @Override
  public String getMessagePrefix() {
    return "Error: ";
  }
}

