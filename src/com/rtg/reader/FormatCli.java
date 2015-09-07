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
package com.rtg.reader;

import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.reader.FastqSequenceDataSource.FastQScoreType;
import com.rtg.sam.DefaultSamFilter;
import com.rtg.sam.DuplicateSamFilter;
import com.rtg.sam.SamBamConstants;
import com.rtg.sam.SamCommandHelper;
import com.rtg.sam.SamFilter;
import com.rtg.sam.SamFilterChain;
import com.rtg.sam.SamFilterParams;
import com.rtg.util.Constants;
import com.rtg.util.IORunnable;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.License;
import com.rtg.util.SimpleThreadPool;
import com.rtg.util.Utils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.InformationType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.InputFileUtils;
import com.rtg.util.io.LogStream;

import htsjdk.samtools.SAMReadGroupRecord;

/**
 * Perform the prereading of sequence data, into a format that is understood
 * by RTG.
 */
public final class FormatCli extends LoggedCli {

  /** SDF input format */
  public static final String SDF_FORMAT = "sdf";
  /** FASTA input format */
  public static final String FASTA_FORMAT = "fasta";
  /** FASTQ input format */
  public static final String FASTQ_FORMAT = "fastq";
  /** SAM / BAM paired end input format */
  public static final String SAM_PE_FORMAT = "sam-pe";
  /** SAM / BAM single end input format */
  public static final String SAM_SE_FORMAT = "sam-se";
  /** CG TSV format */
  public static final String TSV_FORMAT = "tsv";
  /** CG FASTQ format */
  public static final String CGFASTQ_FORMAT = "cg-fastq";
  /** CG SAM format */
  public static final String CGSAM_FORMAT = "cg-sam";

  private static final String XMAPPED_SAM = "Xmapped-sam";
  private static final String XDEDUP_SECONDARY = "Xdedup-secondary-alignments";

  static class BadFormatCombinationException extends IllegalArgumentException {
    public BadFormatCombinationException(String string) {
      super(string);
    }
  }

  /** Properties file with all non-logged strings used in the prereader. */
  static final String PREREAD_RESOURCE_BUNDLE = "com.rtg.reader.Prereader";
  /** Internationalized messages. */
  private static final ResourceBundle RESOURCE = ResourceBundle.getBundle(PREREAD_RESOURCE_BUNDLE, Locale.getDefault());
  /** input sequence format flag */
  public static final String FORMAT_FLAG = "format";
  private static final String PROTEIN_FLAG = RESOURCE.getString("PROTEIN_FLAG");
  private static final String EXCLUDE_FLAG = RESOURCE.getString("EXCLUDE_FLAG");
  private static final String DUST_FLAG = RESOURCE.getString("DUST_FLAG");
  /** flag for left input file */
  public static final String LEFT_FILE_FLAG = RESOURCE.getString("LEFT_FILE_FLAG");
  /** flag for right input file */
  public static final String RIGHT_FILE_FLAG = RESOURCE.getString("RIGHT_FILE_FLAG");
  private static final String MODULE_NAME = "format";
  /** flag to specify no quality should be stored in SDF */
  public static final String NO_QUALITY = "no-quality";
  /** flag to specify no names should be stored in SDF */
  public static final String NO_NAMES = "no-names";
  private static final String COMPRESS_FLAG = "Xcompress";
  private static final String DISABLE_DUPLICATE_DETECTOR = "allow-duplicate-names";
  private static final String READ_TRIM_FLAG = "trim-threshold";
  private static final String SELECT_READ_GROUP = "select-read-group";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "convert sequence data files to SDF";
  }

  @Override
  protected void initFlags() {
    mFlags.registerExtendedHelp();
    mFlags.setDescription("Converts the contents of sequence data files (FASTA/FASTQ/SAM/BAM) into the RTG Sequence Data File (SDF) format."); //RESOURCE.getString("PROGRAM_DESC")
    CommonFlagCategories.setCategories(mFlags);
    try {
      mFlags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, "SDF", "name of output SDF").setCategory(INPUT_OUTPUT);

      final Flag formatFlag = mFlags.registerOptional('f', FORMAT_FLAG, String.class, RESOURCE.getString("FORMAT_TYPE"), RESOURCE.getString("FORMAT_DESC"), RESOURCE.getString("FORMAT_FASTA")).setCategory(INPUT_OUTPUT);
      String[] formats = {FASTA_FORMAT, FASTQ_FORMAT, SAM_SE_FORMAT, SAM_PE_FORMAT };
      if (License.isDeveloper()) {
        formats = Utils.append(formats, CGFASTQ_FORMAT, CGSAM_FORMAT);
      }
      formatFlag.setParameterRange(formats);
      CommonFlags.initQualityFormatFlag(mFlags);
      mFlags.registerOptional(SELECT_READ_GROUP, String.class, "String", "when formatting from SAM/BAM input, only include reads with this read group ID").setCategory(FILTERING);
      mFlags.registerOptional('p', PROTEIN_FLAG, RESOURCE.getString("PROTEIN_DESC")).setCategory(INPUT_OUTPUT);
      mFlags.registerOptional(DUST_FLAG, RESOURCE.getString("DUST_DESC")).setCategory(FILTERING);
      mFlags.registerOptional(READ_TRIM_FLAG, Integer.class, CommonFlags.INT, "trim read ends to maximise base quality above the given threshold").setCategory(FILTERING);

      final Flag inputListFlag = mFlags.registerOptional('I', CommonFlags.INPUT_LIST_FLAG, File.class, "FILE", "file containing a list of input read files (1 per line)").setCategory(INPUT_OUTPUT);

      final Flag exFlag = mFlags.registerOptional(EXCLUDE_FLAG, String.class, RESOURCE.getString("STRING_TYPE"), RESOURCE.getString("EXCLUDE_DESC"));
      exFlag.setMinCount(0);
      exFlag.setMaxCount(Integer.MAX_VALUE);
      exFlag.setCategory(FILTERING);
      final Flag leftFlag = mFlags.registerOptional('l', LEFT_FILE_FLAG, File.class, "FILE", "left input file for FASTA/FASTQ paired end data").setCategory(INPUT_OUTPUT);
      final Flag rightFlag = mFlags.registerOptional('r', RIGHT_FILE_FLAG, File.class, "FILE", "right input file for FASTA/FASTQ paired end data").setCategory(INPUT_OUTPUT);
      mFlags.registerOptional(NO_QUALITY, "do not include quality data in the SDF output").setCategory(UTILITY);
      mFlags.registerOptional(NO_NAMES, "do not include name data in the SDF output").setCategory(UTILITY);
      mFlags.registerOptional(COMPRESS_FLAG, Boolean.class, "BOOL", "compress sdf", Boolean.TRUE).setCategory(UTILITY);
      mFlags.registerOptional(DISABLE_DUPLICATE_DETECTOR, "disable checking for duplicate sequence names").setCategory(UTILITY);
      SamCommandHelper.initSamRg(mFlags, "ILLUMINA", UTILITY);
      final Flag inFlag = mFlags.registerRequired(File.class, CommonFlags.FILE, RESOURCE.getString("INPUT_DESC"));
      inFlag.setMinCount(0);
      inFlag.setMaxCount(Integer.MAX_VALUE);
      inFlag.setCategory(INPUT_OUTPUT);
      mFlags.addRequiredSet(inFlag);
      mFlags.addRequiredSet(inputListFlag);
      mFlags.addRequiredSet(leftFlag, rightFlag);
      mFlags.setValidator(VALIDATOR);
      mFlags.registerOptional(XMAPPED_SAM, Boolean.class, "BOOL", "set to true to use the mapped SAM input implementation", Boolean.TRUE).setCategory(INPUT_OUTPUT);
      mFlags.registerOptional(XDEDUP_SECONDARY, "deduplicate secondary alignments by name").setCategory(FILTERING);

    } catch (final MissingResourceException e) {
      throw new SlimException(e);
    }
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }

  private static boolean validFile(final File file) {
    if (!file.exists()) {
      Diagnostic.error(ErrorType.FILE_NOT_FOUND, file.getPath());
      return false;
    } else if (file.isDirectory()) {
      Diagnostic.error(ErrorType.NOT_A_FILE, file.getPath());
      return false;
    }
    return true;
  }

  private static final Validator VALIDATOR = new Validator() {
    @Override
    public boolean isValid(final CFlags flags) {
      final boolean leftFileSet = flags.isSet(LEFT_FILE_FLAG);
      final boolean rightFileSet = flags.isSet(RIGHT_FILE_FLAG);

      final List<File> files;
      try {
        files = CommonFlags.getFileList(flags, CommonFlags.INPUT_LIST_FLAG, null, false);
      } catch (final IOException e) {
        flags.setParseMessage("An error occurred reading " + flags.getValue(CommonFlags.INPUT_LIST_FLAG));
        return false;
      }
      final String format = flags.getValue(FORMAT_FLAG).toString().toLowerCase(Locale.getDefault());
      if (files.size() == 0) {  //if no anonymous input files have been specified
        if (!leftFileSet && !rightFileSet) {
          flags.setParseMessage("No input files specified.");
          return false;
        } else if (!leftFileSet || !rightFileSet) {
          flags.setParseMessage("Both left and right reads must be specified.");
          return false;
        } else if (flags.isSet(PROTEIN_FLAG)) {
          flags.setParseMessage("Cannot set protein flag when left and right files are specified.");
          return false;
        } else if (format.equals(SAM_SE_FORMAT) || format.equals(SAM_PE_FORMAT)) {
          flags.setParseMessage("Do not use left and right flags when using SAM input.");
          return false;
        }
        int nonFiles = 0;
        if (!validFile((File) flags.getValue(LEFT_FILE_FLAG))) {
          nonFiles++;
        }
        if (!validFile((File) flags.getValue(RIGHT_FILE_FLAG))) {
          nonFiles++;
        }
        if (nonFiles > 0) {
          throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "There were " + nonFiles + " invalid input file paths");
        }
      } else if (leftFileSet || rightFileSet) { //anon input files specified and left or right specified
        flags.setParseMessage("Either specify individual input files or left and right files, not both.");
        return false;
      }
      if ((format.equals(SAM_PE_FORMAT) || format.equals(SAM_SE_FORMAT)) && flags.isSet(DUST_FLAG)) {
        flags.setParseMessage("--" + DUST_FLAG + " is not supported when --" + FORMAT_FLAG + " is " + format + ".");
        return false;
      }

      if (!validateQualityFormatFlags(flags, format)) {
        return false;
      }
      final File outputDir = (File) flags.getValue(CommonFlags.OUTPUT_FLAG);
      if (!CommonFlags.validateOutputDirectory(outputDir)) {
        return false;
      }
      if (flags.isSet(SamCommandHelper.SAM_RG) && !SamCommandHelper.validateSamRg(flags)) {
        return false;
      }
      if (flags.isSet(SELECT_READ_GROUP) && !(format.equals(SAM_SE_FORMAT) || format.equals(SAM_PE_FORMAT))) {
        flags.setParseMessage("--" + SELECT_READ_GROUP + " can only be used when formatting SAM/BAM");
        return false;
      }
      return true;
    }
  };

  /**
   * Helper method to get a data source
   * @param files files to read data from
   * @param format the input format
   * @param arm the arm of these files (or null)
   * @param mappedSam true to use mapped SAM / BAM implementation
   * @param flattenPaired if <code>paired</code> is false then this will load both arms into a single SDF
   * @param samReadGroup sam read group ID to restrict output to
   * @param dedupSecondary true to de-duplicate secondary alignments by name, only needed if input mappings include reads that do not contain a primary alignment (e.g. RTG ambiguous mappings)
   * @return the data source
   */
  public static SequenceDataSource getDnaDataSource(List<File> files, InputFormat format, PrereadArm arm, boolean mappedSam, boolean flattenPaired, String samReadGroup, boolean dedupSecondary) {
    if (format == InputFormat.FASTA) {
      return new FastaSequenceDataSource(files, new DNAFastaSymbolTable(), true, arm);
    } else if (format == InputFormat.FASTQ || format == InputFormat.FASTQ_CG) {
      return new FastqSequenceDataSource(files, FastQScoreType.PHRED, true, arm);
    } else if (format == InputFormat.SOLEXA) {
      return new FastqSequenceDataSource(files, FastQScoreType.SOLEXA, true, arm);
    } else if (format == InputFormat.SOLEXA1_3) {
      return new FastqSequenceDataSource(files, FastQScoreType.SOLEXA1_3, true, arm);
    } else if (format.isSam()) {
      final List<SamFilter> filters = new ArrayList<>();
      int filterFlags = SamBamConstants.SAM_SUPPLEMENTARY_ALIGNMENT; // Always ignore supplementary alignments
      if (!dedupSecondary) {
        filterFlags |= SamBamConstants.SAM_SECONDARY_ALIGNMENT;
      }
      filters.add(new DefaultSamFilter(new SamFilterParams.SamFilterParamsBuilder().requireUnsetFlags(filterFlags).create()));
      if (samReadGroup != null) {
        filters.add(new SamBamSequenceDataSource.FilterReadGroups(samReadGroup));
      }
      if (dedupSecondary) {
        filters.add(new DuplicateSamFilter());
      }
      if (format == InputFormat.SAM_CG) {
        return CgSamBamSequenceDataSource.fromInputFiles(files, flattenPaired, new SamFilterChain(filters));
      } else if (mappedSam) {
        return MappedSamBamSequenceDataSource.fromInputFiles(files, format.isPairedSam(), flattenPaired, new SamFilterChain(filters));
      } else {
        return SamBamSequenceDataSource.fromInputFiles(files, format.isPairedSam(), flattenPaired, new SamFilterChain(filters));
      }
    } else {
      throw new BadFormatCombinationException("Invalid file format=" + format);
    }
  }

  /**
   * Validates the flags for a quality encoding
   * @param flags the flags object
   * @param format the file format, anything other than <code>fastq</code> will cause this function to return true
   * @return true if quality encoding type is recognized or format other than <code>fastq</code> is chosen
   */
  public static boolean validateQualityFormatFlags(CFlags flags, String format) {
    if (format.equals(FASTQ_FORMAT)) {
      if (!flags.isSet(CommonFlags.QUALITY_FLAG)) {
        flags.setParseMessage("--" + CommonFlags.QUALITY_FLAG + " is required for \"fastq\" format.");
        return false;
      }
    } else {
      if (flags.isSet(CommonFlags.QUALITY_FLAG)) {
        flags.setParseMessage("--" + CommonFlags.QUALITY_FLAG + " is only allowed for \"fastq\" format.");
        return false;
      }
    }
    return true;
  }

  static class PrereadExecutor {
    private final boolean mProtein;
    private final boolean mDusting;
    private final InputFormat mInputFormat;
    private final File mOutDir;
    private final PrintStream mOut;
    private final Collection<String> mNamesToExclude;
    private final boolean mIncludeQuality;
    private final boolean mIncludeNames;
    private final boolean mCompressed;
    private final boolean mMappedSam;
    private final String mSamReadGroup;
    private final SAMReadGroupRecord mReadGroupRecord;
    private Integer mReadTrimQualityThreshold = null;
    private long mNumSequences = 0;
    private final boolean mDedupSecondary;

    /**
     * @param protein true if processing as protein
     * @param dusting true if dusting should be performed
     * @param inputFormat the input file format
     * @param outDir where the result is to be placed.
     * @param out Print Stream to print summary to.
     * @param namesToExclude names of sequences to be excluded
     * @param includeQuality true if quality data to be included in output, false otherwise
     * @param includeNames true if name data to be included in output, false otherwise
     * @param compressed whether <code>SDF</code> should be compressed
     * @param mappedSam true to use the mapped SAM implementation
     * @param samReadGroup the read group ID as selected by the user. May be null if there is only one read group, not mapping from SAM/BAM, or no read group has been given.
     * @param readGroupRecord the read group for the sam file
     * @param dedupSecondary true to de-duplicate secondary alignments by name. Useful for processing RTG's SAM/BAM output.
     */
    PrereadExecutor(boolean protein, boolean dusting, InputFormat inputFormat, File outDir, PrintStream out,
                    Collection<String> namesToExclude, boolean includeQuality, boolean includeNames, boolean compressed,
                    boolean mappedSam, String samReadGroup, SAMReadGroupRecord readGroupRecord, boolean dedupSecondary) {
      mProtein = protein;
      mDusting = dusting;
      mInputFormat = inputFormat;
      mOutDir = outDir;
      mOut = out;
      mNamesToExclude = namesToExclude;
      mIncludeQuality = includeQuality;
      mIncludeNames = includeNames;
      mCompressed = compressed;
      mMappedSam = mappedSam;
      mSamReadGroup = samReadGroup;
      mReadGroupRecord = readGroupRecord;
      mDedupSecondary = dedupSecondary;
    }

    void setReadTrimQualityThreshold(Integer threshold) {
      mReadTrimQualityThreshold = threshold;
    }

    private void formattingMessage(boolean paired) {
      Diagnostic.info(InformationType.INFO_USER, true, formattingMessage(paired, mInputFormat));
    }

    static String formattingMessage(boolean paired, InputFormat format) {
      final String inputFileType;
      switch(format) {
        case SAM_SE:
        case SAM_PE:
          inputFileType = "SAM/BAM";
          break;
        case SAM_CG:
          inputFileType = "CGSAM/BAM";
          break;
        case FASTQ_CG:
          inputFileType = "CGFASTQ";
          break;
        case FASTQ:
        case SOLEXA:
        case SOLEXA1_3:
          inputFileType = "FASTQ";
          break;
        case FASTA:
        default:
          inputFileType = "FASTA";
          break;
      }
      return "Formatting " + (paired ? "paired-end " : "") + inputFileType + " data";
    }

    /**
     * Performs prereads on a list of input files
     * @param files list of files to be used as input.
     * @throws IOException if an I/O error occurs.
     */
    public void performPreread(final List<File> files) throws IOException {
      final SequencesWriter writer;
      formattingMessage(mInputFormat.isPairedSam());
      final SequenceDataSource ds;
      if (mProtein) {
        if (mInputFormat != InputFormat.FASTA) {
          throw new BadFormatCombinationException("Incompatible sequence type and file format. format=" + mInputFormat + " protein=" + mProtein);
        }
        ds = new FastaSequenceDataSource(files, new ProteinFastaSymbolTable(), true, null);
        ds.setDusting(mDusting);
        writer = new SequencesWriter(ds, mOutDir, Constants.MAX_FILE_SIZE, mNamesToExclude, IndexFile.typeFromFormat(mInputFormat), mCompressed, mReadTrimQualityThreshold);
      } else {
        ds = getDnaDataSource(files, mInputFormat, null, mMappedSam, false, mSamReadGroup, mDedupSecondary);
        ds.setDusting(mDusting);
        if (mInputFormat.isPairedSam()) {
          writer = new AlternatingSequencesWriter(ds, mOutDir, Constants.MAX_FILE_SIZE, mNamesToExclude, IndexFile.typeFromFormat(mInputFormat), mCompressed, mReadTrimQualityThreshold);
        } else {
          writer = new SequencesWriter(ds, mOutDir, Constants.MAX_FILE_SIZE, mNamesToExclude, IndexFile.typeFromFormat(mInputFormat), mCompressed, mReadTrimQualityThreshold);
        }
      }
      writer.setReadGroup(mReadGroupRecord == null ? null : mReadGroupRecord.toString());
      // perform the actual work
      writer.processSequences(mIncludeQuality, mIncludeNames);

      final Counts inputCounts = new Counts(writer.getNumberOfSequences() + writer.getNumberOfExcludedSequences(), writer.getTotalLength() + writer.getExcludedResidueCount(), ds.getMaxLength() , ds.getMinLength());
      final Counts outputCounts = new Counts(writer.getNumberOfSequences(), writer.getTotalLength(), writer.getMaxLength(), writer.getMinLength());
      mNumSequences = mInputFormat.isPairedSam() ? writer.getNumberOfSequences() / 2 : writer.getNumberOfSequences();
      writeStats(files.toArray(new File[files.size()]), mInputFormat.isPairedSam(), inputCounts, outputCounts, writer.getSdfId(), ds.getDusted());
    }

    /**
     * Perform prereads on an explicit paired-end data set.
     * @param leftFile file containing the left arm reads
     * @param rightFile file containing the right arm reads
     * @throws IOException if an I/O error occurs.
     */
    public void performPreread(File leftFile, File rightFile) throws IOException {

      formattingMessage(true);
      final SequenceDataSource leftds = getDnaDataSource(Arrays.asList(leftFile), mInputFormat, PrereadArm.LEFT, mMappedSam, false, mSamReadGroup, mDedupSecondary);
      final SequenceDataSource rightds = getDnaDataSource(Arrays.asList(rightFile), mInputFormat, PrereadArm.RIGHT, mMappedSam, false, mSamReadGroup, mDedupSecondary);
      leftds.setDusting(mDusting);
      rightds.setDusting(mDusting);

      final SdfId sdfId = new SdfId();
      final PrereadType prereadType = IndexFile.typeFromFormat(mInputFormat);


      final SimpleThreadPool pool = new SimpleThreadPool(2, "paired-end prereader", true);
      pool.enableBasicProgress(2);
      final SequenceProcessor leftSequenceProc = new SequenceProcessor(leftds, prereadType, PrereadArm.LEFT, new File(mOutDir, "left"), sdfId, mNamesToExclude, mCompressed, mReadTrimQualityThreshold);
      final SequenceProcessor rightSequenceProc = new SequenceProcessor(rightds, prereadType, PrereadArm.RIGHT, new File(mOutDir, "right"), sdfId, mNamesToExclude, mCompressed, mReadTrimQualityThreshold);
      leftSequenceProc.setIncludeNames(mIncludeNames);
      rightSequenceProc.setIncludeNames(mIncludeNames);
      leftSequenceProc.setIncludeQuality(mIncludeQuality);
      rightSequenceProc.setIncludeQuality(mIncludeQuality);
      final String readGroup = mReadGroupRecord == null ? null : mReadGroupRecord.toString();
      leftSequenceProc.setReadGroup(readGroup);
      rightSequenceProc.setReadGroup(readGroup);
      try {
        pool.execute(leftSequenceProc);
        pool.execute(rightSequenceProc);
      } finally {
        pool.terminate();
      }

      final long leftSeqs = leftSequenceProc.mWriter.getNumberOfSequences();
      final long rightSeqs = rightSequenceProc.mWriter.getNumberOfSequences();
      if (leftSeqs != rightSeqs) {
        throw new NoTalkbackSlimException("Invalid input, paired end data must have same number of sequences. Left had: " + leftSeqs + " Right had: " + rightSeqs);
      }
      final long leftSeqsTotal = leftSequenceProc.mWriter.getTotalLength();
      final long rightSeqsTotal = rightSequenceProc.mWriter.getTotalLength();

      final long inputSeqs = leftSeqs + leftSequenceProc.mWriter.getNumberOfExcludedSequences() + rightSeqs + rightSequenceProc.mWriter.getNumberOfExcludedSequences();
      final long inputResidues = leftSeqsTotal + leftSequenceProc.mWriter.getExcludedResidueCount() + rightSeqsTotal + rightSequenceProc.mWriter.getExcludedResidueCount();
      final long inMax = Math.max(leftds.getMaxLength(), rightds.getMaxLength());
      final long inMin = Math.min(leftds.getMinLength(), rightds.getMinLength());
      final Counts inputCounts = new Counts(inputSeqs, inputResidues, inMax, inMin);

      final long outputSeqs = leftSeqs + rightSeqs;
      final long outputResidues = leftSeqsTotal + rightSeqsTotal;
      final long outMax = Math.max(leftSequenceProc.mWriter.getMaxLength(), rightSequenceProc.mWriter.getMaxLength());
      final long outMin = Math.min(leftSequenceProc.mWriter.getMinLength(), rightSequenceProc.mWriter.getMinLength());
      final Counts outputCounts = new Counts(outputSeqs, outputResidues, outMax, outMin);

      final long dusted = leftds.getDusted() + rightds.getDusted();
      mNumSequences = outputSeqs / 2;
      writeStats(new File[] {leftFile, rightFile}, true, inputCounts, outputCounts, sdfId, dusted);
    }

    private static class Counts {
      protected final long mSequences;
      protected final long mResidues;
      protected final long mMaxLen;
      protected final long mMinLen;

      Counts(long sequences, long residues, long maxLen, long minLen) {
        mSequences = sequences;
        mResidues = residues;
        mMaxLen = maxLen;
        mMinLen = minLen;
      }
    }

    private void writeStats(File[] files, boolean paired, Counts input, Counts output, SdfId sdfId, long dusted) {
      if (mOut != null) {
        final StringBuilder fileList = new StringBuilder();
        for (final File f : files) {
          fileList.append(" ").append(f.getName());
        }
        mOut.println();
        mOut.println("Input Data");
        mOut.println("Files              :" + fileList.toString());
        mOut.println("Format             : " + mInputFormat.toString());
        mOut.println("Type               : " + (mProtein ? "PROTEIN" : "DNA"));
        if (paired) {
          mOut.println("Number of pairs    : " + (input.mSequences / 2));
        }
        mOut.println("Number of sequences: " + input.mSequences);
        mOut.println("Total residues     : " + input.mResidues);
        if (input.mMaxLen >= input.mMinLen) {
          mOut.println("Minimum length     : " + input.mMinLen);
          mOut.println("Maximum length     : " + input.mMaxLen);
        }
        mOut.println("");
        mOut.println("Output Data");
        mOut.println("SDF-ID             : " + sdfId.toString());
        if (paired) {
          mOut.println("Number of pairs    : " + (output.mSequences / 2));
        }
        mOut.println("Number of sequences: " + output.mSequences);
        mOut.println("Total residues     : " + output.mResidues);
        if (output.mMaxLen >= output.mMinLen) {
          mOut.println("Minimum length     : " + output.mMinLen);
          mOut.println("Maximum length     : " + output.mMaxLen);
        }
        final long excluded = input.mSequences - output.mSequences;
        if (excluded > 0 || dusted > 0) {
          mOut.println("");
          if (excluded > 0) {
            if (paired) {
              mOut.println("There were " + (excluded / 2) + " pairs skipped due to filters");
            } else {
              mOut.println("There were " + excluded + " sequences skipped due to filters");
            }
          }
          if (dusted > 0) {
            mOut.println("There were " + dusted + " residues converted from lower case to unknowns");
          }
        }
      }
    }

    static final class SequenceProcessor implements IORunnable {
      final SequencesWriter mWriter;
      private boolean mIncludeQuality;
      private boolean mIncludeNames;

      SequenceProcessor(SequenceDataSource datasource, PrereadType prereadType, PrereadArm arm, File outDir, SdfId sdfId, Collection<String> namesToExclude, boolean compressed, Integer readTrimQualityThreshold) {
        mWriter = new SequencesWriter(datasource, outDir, Constants.MAX_FILE_SIZE, namesToExclude, prereadType, compressed, readTrimQualityThreshold);
        mWriter.setPrereadArm(arm);
        mWriter.setSdfId(sdfId);
      }

      void setIncludeQuality(boolean value) {
        mIncludeQuality = value;
      }
      void setIncludeNames(boolean value) {
        mIncludeNames = value;
      }

      void setReadGroup(String readGroup) {
        mWriter.setReadGroup(readGroup);
      }

      @Override
      public void run() throws IOException {
        mWriter.processSequences(mIncludeQuality, mIncludeNames);
      }
    }
  }

  @Override
  protected int mainExec(final OutputStream out, final LogStream initLog) throws IOException {
    final PrintStream outStream = new PrintStream(out);
    try {
      final File outputDir = (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
      try (PrintStream summaryStream = new PrintStream(new FileOutputStream(new File(outputDir, CommonFlags.SUMMARY_FILE)))) {
        final List<File> files = InputFileUtils.removeRedundantPaths(CommonFlags.getFileList(mFlags, CommonFlags.INPUT_LIST_FLAG, null, false));
        final String format = mFlags.getValue(FORMAT_FLAG).toString().toLowerCase(Locale.getDefault());
        final String qualityFormat = mFlags.isSet(CommonFlags.QUALITY_FLAG) ? mFlags.getValue(CommonFlags.QUALITY_FLAG).toString().toLowerCase(Locale.getDefault()) : null;
        final ArrayList<String> namesToExclude = new ArrayList<>();
        for (final Object o : mFlags.getFlag(EXCLUDE_FLAG).getValues()) {
          namesToExclude.add((String) o);
        }
        final InputFormat inputformat = getFormat(format, qualityFormat, true);
        if (mFlags.isSet(PROTEIN_FLAG)) {
          if (inputformat != InputFormat.FASTA) {
            throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Incompatible sequence type and file format. format=" + inputformat + " protein=" + true);
          }
        }
        final boolean useQuality = !mFlags.isSet(NO_QUALITY);
        final boolean useNames = !mFlags.isSet(NO_NAMES);
        try {
          final ByteArrayOutputStream bos = new ByteArrayOutputStream();
          final PrintStream ps = new PrintStream(bos);
          final String selectReadGroup = (String) mFlags.getValue(SELECT_READ_GROUP);
          final SAMReadGroupRecord samReadGroupRecord;
          if (mFlags.isSet(SamCommandHelper.SAM_RG)) {
            samReadGroupRecord = SamCommandHelper.validateAndCreateSamRG((String) mFlags.getValue(SamCommandHelper.SAM_RG), SamCommandHelper.ReadGroupStrictness.REQUIRED);
          } else if (SamCommandHelper.isSamInput(mFlags)) {
            SAMReadGroupRecord record;
            SAMReadGroupRecord current = null;
            File currentFile = null;
            boolean allRegularFiles = true;
            for (File f : files) {
              // Only try if input files are all regular files (not a pipe which can't be opened twice)
              allRegularFiles &= f.isFile();
              if (!allRegularFiles) {
                break;
              }
            }
            if (mFlags.isSet(SELECT_READ_GROUP)) {
              if (!allRegularFiles) {
                throw new InvalidParamsException("Can only specify a select read group when using regular files for input.");
              }
              for (File f : files) {
                record = SamCommandHelper.validateSelectedSamRG(f, selectReadGroup);
                if (current != null && !current.equals(record)) {
                  throw new InvalidParamsException("SAM read group information for ID \"" + selectReadGroup + "\" doesn't match between files \"" + currentFile.getPath() + "\" and \"" + f.getPath() + "\"");
                }
                current = record;
                currentFile = f;
              }
              if (current == null) {
                throw new InvalidParamsException("The selected read group \"" + selectReadGroup + "\" is not present in the input files");
              }
            } else if (allRegularFiles) {
              for (File f : files) {
                record = SamCommandHelper.validateAndCreateSamRG(f.getPath(), SamCommandHelper.ReadGroupStrictness.AT_MOST_ONE);
                if (record != null && current != null && record != current) {
                  throw new InvalidParamsException("Multiple read group information present in the input files, please select a SAM file with a single read group, select a read group with --" + SELECT_READ_GROUP + " or specify a read group header with --" + SamCommandHelper.SAM_RG);
                }
                if (record != null) {
                  current = record;
                }
              }
            }
            samReadGroupRecord = current;
          } else {
            samReadGroupRecord = null;
          }
          final PrereadExecutor pre = new PrereadExecutor(mFlags.isSet(PROTEIN_FLAG), mFlags.isSet(DUST_FLAG), inputformat, outputDir, ps, namesToExclude,
            useQuality, useNames, (Boolean) mFlags.getValue(COMPRESS_FLAG), (Boolean) mFlags.getValue(XMAPPED_SAM),
            selectReadGroup, samReadGroupRecord, mFlags.isSet(XDEDUP_SECONDARY));
          if (mFlags.isSet(READ_TRIM_FLAG)) {
            if (inputformat == InputFormat.FASTA) {
              throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Input must contain qualities to perform quality-based read trimming.");
            }
            pre.setReadTrimQualityThreshold((Integer) mFlags.getValue(READ_TRIM_FLAG));
          }

          if (files.size() == 0) {
            final File left = (File) mFlags.getValue(LEFT_FILE_FLAG);
            final File right = (File) mFlags.getValue(RIGHT_FILE_FLAG);
            if (InputFileUtils.checkIdenticalPaths(left, right)) {
              throw new NoTalkbackSlimException("Paths given for --" + LEFT_FILE_FLAG + " and --" + RIGHT_FILE_FLAG + " are the same file.");
            }
            pre.performPreread(left, right);
          } else {
            pre.performPreread(files);
          }


          if (useNames && !mFlags.isSet(DISABLE_DUPLICATE_DETECTOR)) {
            if (pre.mNumSequences > SdfUtils.MAX_NO_DUP_SEQUENCE_COUNT) {
              Diagnostic.warning("Too many sequences to check for duplicate sequence names.");
            } else {
              if (containsDuplicatedNames(outputDir)) {
                Diagnostic.warning(WarningType.INFO_WARNING, "Duplicate Sequence Names in Input");
              }
            }
          }
          ps.flush();
          outStream.print(bos.toString());
          summaryStream.print(bos.toString());
          ps.close();
        } catch (final IOException e) {
          if (outputDir.getUsableSpace() == 0) {
            throw new NoTalkbackSlimException(e, ErrorType.DISK_SPACE, outputDir.getPath());
          } else {
            throw e;
          }
        }
        return 0;
      }
    } finally {
      outStream.flush();
    }
  }

  private static boolean containsDuplicatedNames(final File outputDir) throws IOException {
    if (ReaderUtils.isPairedEndDirectory(outputDir)) {
      final boolean resLeft = containsDuplicatedNames(ReaderUtils.getLeftEnd(outputDir));
      final boolean resRight = containsDuplicatedNames(ReaderUtils.getRightEnd(outputDir));
      return resLeft || resRight;
    }
    final PrereadNamesInterface names = new PrereadNames(outputDir, LongRange.NONE);
    return NameDuplicateDetector.checkPrereadNames(names, new File(outputDir.getPath(), "duplicate-names.txt"));
  }

  /**
   * Get the format from command line strings
   * @param format file format
   * @param qualityFormat quality data encoding
   * @param usingQuality true if the quality values are going to be used
   * @return the input format
   */
  public static InputFormat getFormat(String format, String qualityFormat, boolean usingQuality) {
    final InputFormat inputformat;
    switch (format) {
      case FASTA_FORMAT:
        inputformat = InputFormat.FASTA;
        break;
      case FASTQ_FORMAT:
        if (usingQuality) {
          switch (qualityFormat) {
            case CommonFlags.SANGER_FORMAT:
              inputformat = InputFormat.FASTQ;
              break;
            case CommonFlags.SOLEXA_FORMAT:
              inputformat = InputFormat.SOLEXA;
              break;
            case CommonFlags.ILLUMINA_FORMAT:
              inputformat = InputFormat.SOLEXA1_3;
              break;
            default:
              throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Invalid quality format=" + qualityFormat);
          }
        } else {
          inputformat = InputFormat.FASTQ;
        }
        break;
      case CGFASTQ_FORMAT:
        inputformat = InputFormat.FASTQ_CG;
        break;
      case CGSAM_FORMAT:
        inputformat = InputFormat.SAM_CG;
        break;
      case SAM_SE_FORMAT:
        inputformat = InputFormat.SAM_SE;
        break;
      case SAM_PE_FORMAT:
        inputformat = InputFormat.SAM_PE;
        break;
      default:
        throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Invalid file format=" + format);
    }
    return inputformat;
  }
}
