/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
package com.rtg;

import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.graph.RocPlotCli;
import com.rtg.reader.FastqTrim;
import com.rtg.reader.FormatCli;
import com.rtg.reader.Sdf2Fasta;
import com.rtg.reader.Sdf2Fastq;
import com.rtg.reader.Sdf2Sam;
import com.rtg.reader.SdfStatistics;
import com.rtg.reader.SdfSubseq;
import com.rtg.reader.SdfSubset;
import com.rtg.relation.PedFilterCli;
import com.rtg.relation.PedStatsCli;
import com.rtg.simulation.genome.GenomeSimulator;
import com.rtg.simulation.reads.CgSimCli;
import com.rtg.simulation.reads.ReadSimCli;
import com.rtg.simulation.variants.ChildSampleSimulatorCli;
import com.rtg.simulation.variants.DeNovoSampleSimulatorCli;
import com.rtg.simulation.variants.PedSampleSimulatorCli;
import com.rtg.simulation.variants.PopulationVariantSimulatorCli;
import com.rtg.simulation.variants.SampleReplayerCli;
import com.rtg.simulation.variants.SampleSimulatorCli;
import com.rtg.tabix.BgZip;
import com.rtg.tabix.ExtractCli;
import com.rtg.tabix.IndexerCli;
import com.rtg.util.License;
import com.rtg.variant.cnv.cnveval.CnvEvalCli;
import com.rtg.variant.sv.VcfSvDecomposer;
import com.rtg.variant.sv.bndeval.BndEvalCli;
import com.rtg.vcf.VcfAnnotatorCli;
import com.rtg.vcf.VcfDecomposerCli;
import com.rtg.vcf.VcfFilterCli;
import com.rtg.vcf.VcfMerge;
import com.rtg.vcf.VcfStatsCli;
import com.rtg.vcf.VcfSubset;
import com.rtg.vcf.eval.VcfEvalCli;
import com.rtg.vcf.mendelian.MendeliannessChecker;

/**
 * Commands available in the free tools product.
 */
public final class ToolsCommand {

  private ToolsCommand() { }

  static final Command FORMAT = new Command(new FormatCli(), CommandCategory.FORMAT, ReleaseLevel.GA);
  static final Command FASTQTRIM = new Command(new FastqTrim(), CommandCategory.FORMAT, ReleaseLevel.GA);
  static final Command SDF2FASTA = new Command(new Sdf2Fasta(), CommandCategory.FORMAT, ReleaseLevel.GA);
  static final Command SDF2FASTQ = new Command(new Sdf2Fastq(), CommandCategory.FORMAT, ReleaseLevel.GA);
  static final Command SDF2SAM = new Command(new Sdf2Sam(), CommandCategory.FORMAT, ReleaseLevel.GA);

  static final Command GENOMESIM = new Command(new GenomeSimulator(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command CGSIM = new Command(new CgSimCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command READSIM = new Command(new ReadSimCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command POPSIM = new Command(new PopulationVariantSimulatorCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command SAMPLESIM = new Command(new SampleSimulatorCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command CHILDSIM = new Command(new ChildSampleSimulatorCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command DENOVOSIM = new Command(new DeNovoSampleSimulatorCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);
  static final Command PEDSAMPLESIM = new Command(new PedSampleSimulatorCli(), CommandCategory.SIMULATE, ReleaseLevel.BETA);
  static final Command SAMPLEREPLAY = new Command(new SampleReplayerCli(), CommandCategory.SIMULATE, ReleaseLevel.GA);

  static final Command BGZIP = new Command(new BgZip(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command INDEX = new Command(new IndexerCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command EXTRACT = new Command(new ExtractCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command SDFSTATS = new Command(new SdfStatistics(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command SDFSUBSET = new Command(new SdfSubset(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command SDFSUBSEQ = new Command(new SdfSubseq(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command MENDELIAN = new Command(new MendeliannessChecker(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command VCFSTATS = new Command(new VcfStatsCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command VCFMERGE = new Command(new VcfMerge(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command VCFSUBSET = new Command(new VcfSubset(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command VCFFILTER = new Command(new VcfFilterCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command VCFANNOTATE = new Command(new VcfAnnotatorCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command VCFDECOMPOSE = new Command(new VcfDecomposerCli(), CommandCategory.UTILITY, ReleaseLevel.BETA);
  static final Command VCFEVAL = new Command(new VcfEvalCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command SVDECOMPOSE = new Command(new VcfSvDecomposer(), CommandCategory.UTILITY, ReleaseLevel.BETA);
  static final Command BNDEVAL = new Command(new BndEvalCli(), CommandCategory.UTILITY, ReleaseLevel.BETA);
  static final Command CNVEVAL = new Command(new CnvEvalCli(), CommandCategory.UTILITY, ReleaseLevel.ALPHA);
  static final Command PEDFILTER = new Command(new PedFilterCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command PEDSTATS = new Command(new PedStatsCli(), CommandCategory.UTILITY, ReleaseLevel.GA);
  static final Command ROCPLOT = new Command(new RocPlotCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Print version */
  static final Command VERSION = new Command(null, "VERSION", "print version and license information", CommandCategory.UTILITY, ReleaseLevel.GA, License.RTG_PROGRAM_KEY, null) {
    @Override
    public int mainInit(final String[] args, final OutputStream out, final PrintStream err) {
      return VersionCommand.mainInit(args, out);
    }
  };

  /** List modules and their license status. */
  static final LicenseCommand LICENSE = new LicenseCommand();

  /** Generate help for user. */
  static final HelpCommand HELP = new HelpCommand();

  /* This field determines the display order of the commands in the help / license output */
  private static final Command[] DISPLAY_ORDER = {
    // Formatting
    FORMAT, SDF2FASTA, SDF2FASTQ, SDF2SAM, FASTQTRIM,

    // Simulation
    GENOMESIM,                                           // Reference simulation
    CGSIM, READSIM,                                      // Read simulation
    POPSIM, SAMPLESIM, CHILDSIM, DENOVOSIM, PEDSAMPLESIM, // Variant simulation
    SAMPLEREPLAY,   // Variant simulation

    // Utility
    BGZIP, INDEX, EXTRACT,                        // General purpose
    SDFSTATS, SDFSUBSET, SDFSUBSEQ,            // SDF related
    MENDELIAN, VCFSTATS, VCFMERGE,                       // VCF related
    VCFFILTER, VCFANNOTATE, VCFSUBSET,
    VCFDECOMPOSE, VCFEVAL,
    SVDECOMPOSE, BNDEVAL, CNVEVAL,
    PEDFILTER, PEDSTATS,
    ROCPLOT,

    VERSION, LICENSE, HELP
  };

   /**
   * Provides access to list of commands
   */
  public static final CommandLookup INFO = new CommandLookup() {

    @Override
    public Command[] commands() {
      return DISPLAY_ORDER;
    }
  };

  static {
    LICENSE.setInfo(INFO);
    HELP.setInfo(INFO);
  }
}
