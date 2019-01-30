/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.simulation.reads;

import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.rtg.bed.BedUtils;
import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.mode.SequenceType;
import com.rtg.reader.CachingSequencesReader;
import com.rtg.reader.FastqUtils;
import com.rtg.reader.NamesInterface;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.sam.SamCommandHelper;
import com.rtg.simulation.DistributionSampler;
import com.rtg.simulation.IntSampler;
import com.rtg.simulation.SimulationUtils;
import com.rtg.taxonomy.TaxonomyUtils;
import com.rtg.util.DoubleMultiSet;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.MathUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.Utils;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogStream;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;
import com.rtg.variant.MachineErrorParams;
import com.rtg.variant.MachineErrorParamsBuilder;

import htsjdk.samtools.SAMReadGroupRecord;

/**
 * Module wrapper for the standard read type generators
 */
public class ReadSimCli extends LoggedCli {

  static final String MODULE_NAME = "readsim";

  // Common to all machines
  static final String INPUT = "input";
  static final String SEED = "seed";
  static final String COMMENT = "comment";
  static final String MACHINE_TYPE = "machine";
  static final String MACHINE_ERROR_PRIORS = "Xmachine-errors";
  static final String COVERAGE = "coverage";
  static final String READS = "num-reads";
  static final String DISTRIBUTION = "distribution";
  static final String TAXONOMY_DISTRIBUTION = "taxonomy-distribution";
  static final String ABUNDANCE = "abundance";
  static final String DNA_FRACTION = "dna-fraction";
  static final String N_RATE = "n-rate";
  static final String QUAL_RANGE = "qual-range";

  // Options for library generation
  static final String MAX_FRAGMENT = "max-fragment-size";   // Fragment size from which reads are taken
  static final String MIN_FRAGMENT = "min-fragment-size";
  static final String FRAGMENT_SIZE_DIST = "Xfragment-size-distribution"; // Explicit fragment size distribution
  static final String ALLOW_UNKNOWNS = "allow-unknowns";
  //static final String TRIM_FRAGMENT_BELL = "Xtrim-fragment-bell"; // Not currently implemented

  // Single-end Illumina machine
  static final String READLENGTH = "read-length";

  // Paired-end Illumina machine
  static final String LEFT_READLENGTH = "left-read-length";
  static final String RIGHT_READLENGTH = "right-read-length";

  // 454 machine
  static final String MIN_TOTAL_454_LENGTH = "454-min-total-size";
  static final String MAX_TOTAL_454_LENGTH = "454-max-total-size";

  // iontorrent machine
  static final String MIN_TOTAL_IONTORRENT_LENGTH = "ion-min-total-size";
  static final String MAX_TOTAL_IONTORRENT_LENGTH = "ion-max-total-size";

  static final String CAT_FRAGMENTS = "Fragment Generation";
  static final String CAT_ILLUMINA_SE = "Illumina SE";
  static final String CAT_ILLUMINA_PE = "Illumina PE";
  static final String CAT_454_PE = "454 SE/PE";
  static final String CAT_ION_SE = "IonTorrent SE";
  static final String CAT_CG = "Complete Genomics";

  private static final String NO_NAMES = "no-names";
  private static final String NO_QUAL = "no-qualities";

  static final String MNP_EVENT_RATE = "Xmnp-event-rate";
  static final String INS_EVENT_RATE = "Xinsert-event-rate";
  static final String DEL_EVENT_RATE = "Xdelete-event-rate";
  static final String BED_FILE = "Xbed-file";

  private static final String IN_MEMORY_TEMPLATE = "Xin-memory-template";

  private static final String PCR_DUP_RATE = "Xpcr-duplicate-rate";
  private static final String CHIMERA_RATE = "Xchimera-rate";

  private PortableRandom mRandom = null;
  private AbstractMachineErrorParams mPriors = null;

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "generate simulated reads from a sequence";
  }

  @Override
  protected File outputDirectory() {
    File f = (File) mFlags.getValue(OUTPUT_FLAG);
    if (FastqUtils.isFastqExtension(f) || FileUtils.isStdio(f)) {
      try {
        f = FileUtils.createTempDir("readsim", null);
        cleanDirectory();
      } catch (final IOException e) {
        throw new NoTalkbackSlimException("Could not create temporary directory " + e.getMessage());
      }
    }
    return f;
  }

  protected MachineType getMachineType() {
    return MachineType.valueOf(mFlags.getValue(MACHINE_TYPE).toString().toLowerCase(Locale.ROOT));
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates reads from a reference genome.");
    mFlags.setCategories(UTILITY, INPUT_OUTPUT, CAT_FRAGMENTS, CAT_ILLUMINA_PE, CAT_ILLUMINA_SE, CAT_454_PE, CAT_ION_SE, CAT_CG, UTILITY);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, CommonFlags.SDF, "name for reads output SDF").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('t', INPUT, File.class, CommonFlags.SDF, "SDF containing input genome").setCategory(INPUT_OUTPUT);
    CommonFlags.initForce(mFlags);
    final Flag<Double> covFlag = mFlags.registerOptional('c', COVERAGE, Double.class, CommonFlags.FLOAT, "coverage, must be positive").setCategory(CAT_FRAGMENTS);
    final Flag<Integer> nFlag = mFlags.registerOptional('n', READS, Integer.class, CommonFlags.INT, "number of reads to be generated").setCategory(CAT_FRAGMENTS);

    // Fragmenter
    mFlags.registerOptional('N', ALLOW_UNKNOWNS, "allow reads to be drawn from template fragments containing unknown nucleotides").setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional('D', DISTRIBUTION, File.class, CommonFlags.FILE, "file containing probability distribution for sequence selection").setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional(TAXONOMY_DISTRIBUTION, File.class, CommonFlags.FILE, "file containing probability distribution for sequence selection expressed by taxonomy id").setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional(ABUNDANCE, "if set, the user-supplied distribution represents desired abundance").setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional(DNA_FRACTION, "if set, the user-supplied distribution represents desired DNA fraction").setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional(N_RATE, Double.class, CommonFlags.FLOAT, "rate that the machine will generate new unknowns in the read", 0.0).setCategory(CAT_FRAGMENTS);

    mFlags.registerOptional('s', SEED, Long.class, CommonFlags.INT, "seed for random number generator").setCategory(UTILITY);
    mFlags.registerOptional(COMMENT, String.class, CommonFlags.STRING, "comment to include in the generated SDF").setCategory(UTILITY);
    SamCommandHelper.initSamRg(mFlags, "ILLUMINA", UTILITY);

    mFlags.addRequiredSet(covFlag);
    mFlags.addRequiredSet(nFlag);

    mFlags.registerOptional('q', QUAL_RANGE, String.class, CommonFlags.STRING, "set the range of base quality values permitted e.g.: 3-40 (Default is fixed qualities corresponding to overall machine base error rate)").setCategory(UTILITY);

    //reduce wastage
    mFlags.registerOptional(NO_NAMES, "do not create read names in the output SDF").setCategory(UTILITY);
    mFlags.registerOptional(NO_QUAL, "do not create read qualities in the output SDF").setCategory(UTILITY);

    // Override rate options
    mFlags.registerOptional(MNP_EVENT_RATE, Double.class, CommonFlags.FLOAT, "override the overall MNP event rate in the priors").setCategory(UTILITY);
    mFlags.registerOptional(INS_EVENT_RATE, Double.class, CommonFlags.FLOAT, "override the overall insertion event rate in the priors").setCategory(UTILITY);
    mFlags.registerOptional(DEL_EVENT_RATE, Double.class, CommonFlags.FLOAT, "override the overall deletion event rate in the priors").setCategory(UTILITY);
    // Limit to bed regions
    mFlags.registerOptional(BED_FILE, File.class, CommonFlags.FILE, "simulate exome capture by only generating reads that lie over the specified regions").setCategory(UTILITY);

    mFlags.registerOptional(PCR_DUP_RATE, Double.class, CommonFlags.FLOAT, "set the PCR duplication error rate", 0.0).setCategory(UTILITY);
    mFlags.registerOptional(CHIMERA_RATE, Double.class, CommonFlags.FLOAT, "set the chimeric fragment error rate", 0.0).setCategory(UTILITY);

    mFlags.registerOptional(IN_MEMORY_TEMPLATE, Boolean.class, "BOOL", "whether to load the template into memory", Boolean.TRUE).setCategory(UTILITY);

    mFlags.setValidator(new ReadSimCliValidator());
    initMachineFlags();
  }
  protected void initMachineFlags() {
    initIlluminaFlags();
    init454Flags();
    initIonFlags();
    final Flag<String> machType = mFlags.registerRequired(MACHINE_TYPE, String.class, CommonFlags.STRING, "select the sequencing technology to model").setCategory(INPUT_OUTPUT);
    machType.setParameterRange(new String[]{MachineType.ILLUMINA_SE.name(), MachineType.ILLUMINA_PE.name(), MachineType.COMPLETE_GENOMICS.name(), MachineType.COMPLETE_GENOMICS_2.name(), MachineType.FOURFIVEFOUR_PE.name(), MachineType.FOURFIVEFOUR_SE.name(), MachineType.IONTORRENT.name()});
    mFlags.registerOptional('E', MACHINE_ERROR_PRIORS, String.class, CommonFlags.STRING, "selects the sequencer machine error settings. One of [default, illumina, ls454_se, ls454_pe, complete, completegenomics, iontorrent]").setCategory(UTILITY);
  }

  protected void initIlluminaFlags() {
    // Illumina SE
    mFlags.registerOptional('r', READLENGTH, Integer.class, CommonFlags.INT, "target read length, must be positive").setCategory(CAT_ILLUMINA_SE);
    mFlags.registerOptional('M', MAX_FRAGMENT, Integer.class, CommonFlags.INT, "maximum fragment size", 250).setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional('m', MIN_FRAGMENT, Integer.class, CommonFlags.INT, "minimum fragment size", 200).setCategory(CAT_FRAGMENTS);
    // Illumina PE
    mFlags.registerOptional('L', LEFT_READLENGTH, Integer.class, CommonFlags.INT, "target read length on the left side").setCategory(CAT_ILLUMINA_PE);
    mFlags.registerOptional('R', RIGHT_READLENGTH, Integer.class, CommonFlags.INT, "target read length on the right side").setCategory(CAT_ILLUMINA_PE);
    mFlags.registerOptional(FRAGMENT_SIZE_DIST, File.class, CommonFlags.FILE, "file containing probability distribution for fragment lengths").setCategory(CAT_FRAGMENTS);
  }

  protected void init454Flags() {
    mFlags.registerOptional(MAX_TOTAL_454_LENGTH, Integer.class, CommonFlags.INT, "maximum 454 read length (in paired end case the sum of the left and the right read lengths)").setCategory(CAT_454_PE);
    mFlags.registerOptional(MIN_TOTAL_454_LENGTH, Integer.class, CommonFlags.INT, "minimum 454 read length (in paired end case the sum of the left and the right read lengths)").setCategory(CAT_454_PE);
  }

  protected void initIonFlags() {
    mFlags.registerOptional(MAX_TOTAL_IONTORRENT_LENGTH, Integer.class, CommonFlags.INT, "maximum IonTorrent read length").setCategory(CAT_ION_SE);
    mFlags.registerOptional(MIN_TOTAL_IONTORRENT_LENGTH, Integer.class, CommonFlags.INT, "minimum IonTorrent read length").setCategory(CAT_ION_SE);
  }

  private Machine createMachine() {
    final MachineType mt = getMachineType();
    final long seed = mRandom.nextLong();
    final Machine result;
    if (mt == MachineType.ILLUMINA_SE) {
      final IlluminaSingleEndMachine m = new IlluminaSingleEndMachine(mPriors, seed);
      m.setReadLength((Integer) mFlags.getValue(READLENGTH));
      result = m;
    } else if (mt == MachineType.ILLUMINA_PE) {
      final IlluminaPairedEndMachine m = new IlluminaPairedEndMachine(mPriors, seed);
      m.setLeftReadLength((Integer) mFlags.getValue(LEFT_READLENGTH));
      m.setRightReadLength((Integer) mFlags.getValue(RIGHT_READLENGTH));
      result = m;
    } else if (mt == MachineType.FOURFIVEFOUR_PE) {
      final FourFiveFourPairedEndMachine m = new FourFiveFourPairedEndMachine(mPriors, seed);
      m.setMinPairSize((Integer) mFlags.getValue(MIN_TOTAL_454_LENGTH));
      m.setMaxPairSize((Integer) mFlags.getValue(MAX_TOTAL_454_LENGTH));
      result = m;
    } else if (mt == MachineType.FOURFIVEFOUR_SE) {
      final FourFiveFourSingleEndMachine m = new FourFiveFourSingleEndMachine(mPriors, seed);
      m.setMinSize((Integer) mFlags.getValue(MIN_TOTAL_454_LENGTH));
      m.setMaxSize((Integer) mFlags.getValue(MAX_TOTAL_454_LENGTH));
      result = m;
    } else if (mt == MachineType.COMPLETE_GENOMICS) {
      result = new CompleteGenomicsV1Machine(mPriors, seed);
    } else if (mt == MachineType.COMPLETE_GENOMICS_2) {
      result = new CompleteGenomicsV2Machine(mPriors, seed);
    } else if (mt == MachineType.IONTORRENT) {
      final IonTorrentSingleEndMachine m = new IonTorrentSingleEndMachine(mPriors, seed);
      m.setMinSize((Integer) mFlags.getValue(MIN_TOTAL_IONTORRENT_LENGTH));
      m.setMaxSize((Integer) mFlags.getValue(MAX_TOTAL_IONTORRENT_LENGTH));
      result = m;
    } else {
      throw new IllegalArgumentException("Unrecognized machine type: " + mt);
    }
    if (mFlags.isSet(QUAL_RANGE)) {
      final String range = (String) mFlags.getValue(QUAL_RANGE);
      final String[] vals = range.split("-");
      try {
        final int l = Integer.parseInt(vals[0]);
        final int u = Integer.parseInt(vals[1]);
        result.setQualRange((byte) l, (byte) u);
      } catch (final NumberFormatException e) {
        throw new NoTalkbackSlimException("Malformed quality range " + range);
      }
    }
    return result;
  }

  private ReadWriter createReadWriter(Machine m, File f) throws IOException {
    if (FastqUtils.isFastqExtension(f) || FileUtils.isStdio(f)) {
      return new FastqReadWriter(f);
    } else {
      final SdfReadWriter rw = new SdfReadWriter(f, m.isPaired(), m.prereadType(), !mFlags.isSet(NO_NAMES), !mFlags.isSet(NO_QUAL));
      rw.setComment((String) mFlags.getValue(COMMENT));
      if (mFlags.isSet(SamCommandHelper.SAM_RG)) {
        final SAMReadGroupRecord rg = SamCommandHelper.validateAndCreateSamRG((String) mFlags.getValue(SamCommandHelper.SAM_RG), SamCommandHelper.ReadGroupStrictness.REQUIRED);
        if (rg.getPlatform() != null && !m.machineType().compatiblePlatform(rg.getPlatform())) {
          Diagnostic.warning("The specified SAM read group platform '" + rg.getPlatform() + "' is not recommended for these sequencer settings. The recommended platform is '" + m.machineType().platform() + '"');
        }
        rw.setReadGroup(rg);
      }
      return rw;
    }
  }

  protected String getPriorsNameFlagValue() {
    if (mFlags.isSet(MACHINE_ERROR_PRIORS)) {
      return (String) mFlags.getValue(MACHINE_ERROR_PRIORS);
    }
    return null;
  }

  private AbstractMachineErrorParams createPriors() throws IOException {
    String priorsName = getPriorsNameFlagValue();
    if (priorsName == null) {
      final MachineType mt = getMachineType();
      priorsName = mt.priors();
    }
    try {
      final AbstractMachineErrorParams machineErrors = MachineErrorParams.builder().errors(priorsName).create();
      // Override rates if appropriate
      if (mFlags.isSet(MNP_EVENT_RATE) || mFlags.isSet(INS_EVENT_RATE) || mFlags.isSet(DEL_EVENT_RATE)) {
        final MachineErrorParamsBuilder mb = new MachineErrorParamsBuilder(machineErrors);
        if (mFlags.isSet(MNP_EVENT_RATE)) {
          mb.errorMnpEventRate((Double) mFlags.getValue(MNP_EVENT_RATE));
        }
        if (mFlags.isSet(INS_EVENT_RATE)) {
          mb.errorInsEventRate((Double) mFlags.getValue(INS_EVENT_RATE));
        }
        if (mFlags.isSet(DEL_EVENT_RATE)) {
          mb.errorDelEventRate((Double) mFlags.getValue(DEL_EVENT_RATE));
        }
        return mb.create();
      } else {
        return machineErrors;
      }
    } catch (final InvalidParamsException e) {
      mFlags.setParseMessage("Could not load machine priors: " + priorsName);
      return null;
    }
  }

  private Map<String, Double> createSelectionDistribution(final InputStream is) throws IOException {
    HashMap<String, Double> map = new HashMap<>();
    double sum = 0;
    try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.length() > 0 && line.charAt(0) != '#') {
          final String[] parts = line.trim().split("\\s+");
          if (parts.length != 2) {
            throw new IOException("Malformed line: " + line);
          }
          try {
            final double p = Double.parseDouble(parts[0]);
            if (p < 0 || p > 1) {
              throw new IOException("Malformed line: " + line);
            }
            sum += p;
            final String species = parts[1].trim();
            if (map.containsKey(species)) {
              throw new IOException("Duplicated key: " + line);
            }
            map.put(species, p);
          } catch (final NumberFormatException e) {
            throw new IOException("Malformed line: " + line, e);
          }
        }
      }
    }
    if (Math.abs(sum - 1) > 0.00001) {
      Diagnostic.warning("Input distribution sums to: " + String.format("%1.5g", sum));
      final Map<String, Double> oldmap = map;
      map = new HashMap<>();
      for (Map.Entry<String, Double> entry : oldmap.entrySet()) {
        map.put(entry.getKey(), entry.getValue() / sum);
      }
    }
    return map;
  }

  void checkReadersForFragments(SequencesReader input) {
    final int minFragment = (Integer) mFlags.getValue(MIN_FRAGMENT);
    final long minSequenceLength = input.minLength();
    final long maxSequenceLength = input.maxLength();
    if (minFragment > maxSequenceLength) {
      throw new NoTalkbackSlimException("All template sequences are too short for specified fragment lengths.");
    } else if (minFragment > minSequenceLength) {
      Diagnostic.warning(WarningType.INFO_WARNING, "The template contains some sequences that have length less than the minimum fragment length and these will not be present in the output.");
    }
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final File input = (File) mFlags.getValue(INPUT);
    try (final SequencesReader reader = getReader(input)) {
      if (reader.numberSequences() > Integer.MAX_VALUE) {
        throw new NoTalkbackSlimException("Too many sequences");
      }
      final boolean byDnaFraction = mFlags.isSet(DNA_FRACTION);
      final double[] selectionProb;
      if (mFlags.isSet(DISTRIBUTION)) {
        selectionProb = loadDistribution(reader, (File) mFlags.getValue(DISTRIBUTION), byDnaFraction);
      } else if (mFlags.isSet(TAXONOMY_DISTRIBUTION)) {
        selectionProb = loadTaxonomyDistribution(reader, (File) mFlags.getValue(TAXONOMY_DISTRIBUTION), byDnaFraction);
      } else {
        selectionProb = null;
      }
      checkReadersForFragments(reader);
      if (reader.type() != SequenceType.DNA) {
        throw new NoTalkbackSlimException("Input SDF must be DNA");
      }
      if (mFlags.isSet(SEED)) {
        mRandom = new PortableRandom((Long) mFlags.getValue(SEED));
      } else {
        mRandom = new PortableRandom();
      }
      final long seed = mRandom.getSeed();
      mPriors = createPriors();
      if (mPriors == null) {
        mFlags.error(mFlags.getInvalidFlagMsg());
        cleanDirectory();
        return 1;
      }
      // Construct appropriate GenomeFragmenter / Machine / ReadWriter and validate
      final GenomeFragmenter gf = getGenomeFragmenter(reader, SimulationUtils.createDistribution(reader, selectionProb));

      final Machine m;

      final Double pcrDupRate = (Double) mFlags.getValue(PCR_DUP_RATE);
      final Double chimeraRate = (Double) mFlags.getValue(CHIMERA_RATE);
      if (pcrDupRate > 0.0 || chimeraRate > 0.0) {
        m = new ErrorMachine(seed, createMachine(), pcrDupRate, chimeraRate);
      } else {
        m = createMachine();
      }

      Diagnostic.userLog("ReadSimParams" + LS + " input=" + input + LS + " machine=" + m.prereadType() + LS + " output=" + outputDirectory() + LS + (mFlags.isSet(READS) ? " num-reads=" + mFlags.getValue(READS) + LS : "") + (mFlags.isSet(COVERAGE) ? " coverage=" + mFlags.getValue(COVERAGE) + LS : "") + (selectionProb == null ? "" : " distribution=" + Arrays.toString(selectionProb) + LS) + " allow-unknowns=" + mFlags.isSet(ALLOW_UNKNOWNS) + LS + " max-fragment=" + mFlags.getValue(MAX_FRAGMENT) + LS + " min-fragment=" + mFlags.getValue(MIN_FRAGMENT) + LS + " seed=" + seed + LS + LS + mPriors + LS);
      final File f = (File) mFlags.getValue(OUTPUT_FLAG);
      try (ReadWriter rw = getNFilter(createReadWriter(m, f))) {
        m.setReadWriter(rw);
        gf.setMachine(m);
        // Run generation
        if (mFlags.isSet(READS)) {
          fragmentByCount(gf, rw);
        } else {
          fragmentByCoverage(reader.totalLength(), gf, m);
        }
        final double effectiveCoverage = (double) m.residues() / reader.totalLength();
        if (selectionProb != null) {
          FileUtils.stringToFile(gf.fractionStatistics(), new File(outputDirectory(), "fractions.tsv"));
        }
        if (!FileUtils.isStdio(f)) {
          Diagnostic.info("Generated " + rw.readsWritten() + " reads, effective coverage " + Utils.realFormat(effectiveCoverage, 2));
          Diagnostic.info(m.formatActionsHistogram());
        }
      }
    }
    return 0;
  }

  private SequencesReader getReader(File input) throws IOException {
    // For non-in-memory reader, use a cache since we'll be doing a lot of random access to relatively few sequences
    return (Boolean) mFlags.getValue(IN_MEMORY_TEMPLATE)
      ? SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(input, true, false, LongRange.NONE)
      : new CachingSequencesReader(SequencesReaderFactory.createDefaultSequencesReaderCheckEmpty(input));
  }

  private double[] loadDistribution(SequencesReader reader, File file, boolean byDnaFraction) throws IOException {
    Diagnostic.userLog("Using standard distribution");
    final int numSeq = (int) reader.numberSequences();
    final double[] selectionDist = new double[numSeq];
    try (FileInputStream is = new FileInputStream(file)) {
      final Map<String, Double> selectionMap = createSelectionDistribution(is);
      Diagnostic.userLog("Sequence distribution:" + selectionMap);

      final NamesInterface names = reader.names();
      final int[] lengths = reader.sequenceLengths(0, numSeq);
      double sum = 0;
      for (int k = 0; k < numSeq; ++k) {
        final Double p = selectionMap.get(names.name(k));
        if (p != null) {
          sum += p;
          selectionDist[k] = byDnaFraction ? p : p * lengths[k];
        }
      }
      if (Math.abs(sum - 1) > 0.00001) {
        throw new NoTalkbackSlimException("Some sequences not seen in supplied template, sum:" + String.format("%1.5g", sum));
      }
      if (sum <= 0) {
        throw new NoTalkbackSlimException("After intersection of supplied distribution with reference SDF, distribution was empty!");
      }
    }
    Diagnostic.userLog("Distribution complete");
    return MathUtils.renormalize(selectionDist);
  }

  private double[] loadTaxonomyDistribution(SequencesReader reader, File file, boolean byDnaFraction) throws IOException {
    if (!TaxonomyUtils.hasTaxonomyInfo(reader)) {
      throw new NoTalkbackSlimException("Input SDF does not contain taxonomy information, cannot use taxonomy distribution.");
    }
    Diagnostic.userLog("Using taxonomy distribution");
    final TaxonomyDistribution dist;
    try (final FileInputStream is = new FileInputStream(file)) {
      dist = new TaxonomyDistribution(is, TaxonomyUtils.loadTaxonomyMapping(reader), reader, byDnaFraction ? TaxonomyDistribution.DistributionType.DNA_FRACTION : TaxonomyDistribution.DistributionType.ABUNDANCE);
    }
    Diagnostic.userLog("Distribution complete");
    return dist.getDistribution();
  }

  private void verifyRegionsMatchSdf(SequencesReader reader, ReferenceRegions referenceRegions) throws IOException {
    // Verify regions sequencess match the reader
    final Collection<String> regionSequenceNames = referenceRegions.sequenceNames();
    final int total = regionSequenceNames.size();
    final NamesInterface names = reader.names();
    int removed = 0;
    for (long k = 0; k < names.length(); ++k) {
      if (regionSequenceNames.remove(names.name(k))) {
        ++removed;
      }
    }
    if (removed == 0) {
      throw new NoTalkbackSlimException("Sequence names in region file have no overlap with supplied SDF");
    } else {
      Diagnostic.info(removed + "/" + total + " sequences founds in supplied regions occurred in SDF");
    }
  }

  private GenomeFragmenter getGenomeFragmenter(SequencesReader reader, DistributionSampler distribution) throws IOException {
    final SequencesReader[] readers = {
      reader
    };
    assert reader.hasNames();
    final DistributionSampler[] distributions = {
      distribution
    };
    final GenomeFragmenter gf;
    if (mFlags.isSet(BED_FILE)) {
      final ReferenceRegions referenceRegions = BedUtils.regions((File) mFlags.getValue(BED_FILE));
      verifyRegionsMatchSdf(reader, referenceRegions);
      gf = new FilteringFragmenter(referenceRegions, mRandom.nextLong(), distributions, readers);
    } else {
      gf = new GenomeFragmenter(mRandom.nextLong(), distributions, readers);
    }
    final IntSampler lengthSelector;
    if (mFlags.isSet(FRAGMENT_SIZE_DIST)) {
      lengthSelector = loadLengthDistribution((File) mFlags.getValue(FRAGMENT_SIZE_DIST));
    } else {
      lengthSelector = new MinMaxGaussianSampler((Integer) mFlags.getValue(MIN_FRAGMENT), (Integer) mFlags.getValue(MAX_FRAGMENT));
    }
    gf.setLengthChooser(lengthSelector);
    gf.allowNs(mFlags.isSet(ALLOW_UNKNOWNS));
    return gf;
  }

  private static IntSampler loadLengthDistribution(File file) throws IOException {
    final DoubleMultiSet<Integer> h = new DoubleMultiSet<>();
    try (BufferedReader b = new BufferedReader(FileUtils.createReader(file, true))) {
      String line;
      int max = Integer.MIN_VALUE;
      while ((line = b.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0 || line.startsWith("#")) {
          continue;
        }
        final String[] parts = line.split("\\s");
        if (parts.length != 2) {
          throw new IOException("Expected two fields on line: " + line);
        }
        try {
          final int length = Integer.parseInt(parts[0]);
          if (length <= 0) {
            throw new IOException("Length must be greater than 0, on line: " + line);
          }
          final double count = Double.parseDouble(parts[1]);
          if (count < 0) {
            throw new IOException("Count cannot be negative, on line: " + line);
          }
          max = Math.max(max, length);
          h.add(length, count);
        } catch (NumberFormatException e) {
          throw new IOException("Malformed number on line: " + line, e);
        }
      }
      if (max == Integer.MIN_VALUE) {
        throw new IOException("No distribution information contained in file: " + file);
      }
      final double[] dist = new double[max];
      for (int i = 0; i < max; i++) {
        dist[i] = h.get(i);
      }
      return new DistributionSampler(dist);
    }
  }

  private void fragmentByCoverage(long totalResidues, GenomeFragmenter gf, Machine m) throws IOException {
    final double targetCoverage = (Double) mFlags.getValue(COVERAGE);
    double coverage;
    long percentageDone = 0;
    do {
      gf.makeFragment();
      coverage = (double) m.residues() / totalResidues;
      final long percentage = (long) (coverage / targetCoverage * 100);
      if (percentage > percentageDone) {
        percentageDone = percentage;
        Diagnostic.progress(Math.min(100, percentageDone) + "% of reads generated");
      }
    } while (coverage < targetCoverage);
  }

  private void fragmentByCount(GenomeFragmenter gf, ReadWriter writer) throws IOException {
    final int targetReads = (Integer) mFlags.getValue(READS);
    final int percentageIncrement = Math.max(targetReads / 100, 1);
    int lastIncrement = 0;
    int written;
    while ((written = writer.readsWritten()) < targetReads) {
      if (written - lastIncrement >= percentageIncrement) {
        Diagnostic.progress((int) Math.min(100, written / (double) targetReads * 100.0) + "% of reads generated");
        lastIncrement = written;
      }
      gf.makeFragment();
    }
  }

  private ReadWriter getNFilter(final ReadWriter internal) {
    final ReadWriter rw;
    final double nRate = (Double) mFlags.getValue(N_RATE);
    if (nRate > 0) {
      rw = new UnknownBaseReadWriter(internal, nRate, new PortableRandom(mRandom.getSeed()));
    } else {
      rw = internal;
    }
    return rw;
  }
}
