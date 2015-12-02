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
package com.rtg;

import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.graph.RocPlotCli;
import com.rtg.reader.FormatCli;
import com.rtg.reader.Sdf2Sam;
import com.rtg.reader.Sdf2Fasta;
import com.rtg.reader.Sdf2Fastq;
import com.rtg.reader.SdfStatistics;
import com.rtg.reader.SdfSubseq;
import com.rtg.reader.SdfSubset;
import com.rtg.relation.PedFilterCli;
import com.rtg.relation.PedStatsCli;
import com.rtg.tabix.BgZip;
import com.rtg.tabix.ExtractCli;
import com.rtg.tabix.IndexerCli;
import com.rtg.util.License;
import com.rtg.vcf.VcfAnnotatorCli;
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

  /** For formatting data files for use by Slim */
  static final Command FORMAT = new Command(new FormatCli(), CommandCategory.FORMAT, ReleaseLevel.GA);

  /** For converting Slim's data format into FASTA format */
  static final Command SDF2FASTA = new Command(new Sdf2Fasta(), CommandCategory.FORMAT, ReleaseLevel.GA);

  /** For converting Slim's data format into FASTQ format */
  static final Command SDF2FASTQ = new Command(new Sdf2Fastq(), CommandCategory.FORMAT, ReleaseLevel.GA);

  /** For converting SDF into SAM/BAM format */
  static final Command SDF2SAM = new Command(new Sdf2Sam(), CommandCategory.FORMAT, ReleaseLevel.GA);

  /** BGZips an input file (for use with index module) */
  static final Command BGZIP = new Command(new BgZip(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Indexes our various output formats that have records based on reference position */
  static final Command INDEX = new Command(new IndexerCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Extracts regions from indexed files */
  static final Command EXTRACT = new Command(new ExtractCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Print statistics about prereads */
  static final Command SDFSTATS = new Command(new SdfStatistics(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Creates a subset of an SDF file */
  static final Command SDFSUBSET = new Command(new SdfSubset(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Creates a subset of an SDF file */
  static final Command SDFSUBSEQ = new Command(new SdfSubseq(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Runs stand alone Mendelian checking */
  static final Command MENDELIAN = new Command(new MendeliannessChecker(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** VCF stats class */
  static final Command VCFSTATS = new Command(new VcfStatsCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** VCF merge class */
  static final Command VCFMERGE = new Command(new VcfMerge(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** VCF subset class */
  static final Command VCFSUBSET = new Command(new VcfSubset(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** SNP filter class */
  static final Command VCFFILTER = new Command(new VcfFilterCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** SNP filter class */
  static final Command VCFANNOTATE = new Command(new VcfAnnotatorCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Evaluates variant calling accuracy on a given baseline variant set */
  static final Command VCFEVAL = new Command(new VcfEvalCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** PED filter class */
  static final Command PEDFILTER = new Command(new PedFilterCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** PED stats class */
  static final Command PEDSTATS = new Command(new PedStatsCli(), CommandCategory.UTILITY, ReleaseLevel.GA);

  /** Roc plot tool */
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
    FORMAT, SDF2FASTA, SDF2FASTQ, SDF2SAM,

    // Utility
    BGZIP, INDEX, EXTRACT,                        // General purpose
    SDFSTATS, SDFSUBSET, SDFSUBSEQ,            // SDF related
    MENDELIAN, VCFSTATS, VCFMERGE,                       // VCF related
    VCFFILTER, VCFANNOTATE, VCFSUBSET, VCFEVAL,
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
