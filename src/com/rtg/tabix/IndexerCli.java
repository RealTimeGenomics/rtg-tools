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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.sam.BamIndexer;
import com.rtg.sam.SamUtils;
import com.rtg.util.Utils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.WarningType;

/**
 * Provides front end indexing various formats.
 */
public class IndexerCli extends AbstractCli {

  private static final String INPUT_FORMAT = "format";

  /**
   * Supported formats for indexer
   */
  public enum IndexFormat {
    /** <code>SAM</code> format */
    SAM(".sam.gz"),
    /** <code>BAM</code> format */
    BAM(".bam"),
    /** <code>SV</code> format */
    SV(".sv"),
    /** Coverage format */
    COVERAGETSV(".tsv.gz"),
    /** BED format */
    BED(".bed.gz"),
    /** <code>VCF</code> format */
    VCF(".vcf.gz"),
    /** Automatically determine format */
    AUTO(null);

    private final String mExtension;

    IndexFormat(final String extension) {
      mExtension = extension;
    }

    String getExtension() {
      return mExtension;
    }
  }

  @Override
  public String moduleName() {
    return "index";
  }

  @Override
  public String description() {
    return "create a tabix index";
  }

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  private static void initFlags(CFlags flags) {
    flags.registerExtendedHelp();
    flags.setDescription("Creates index files for block compressed TAB-delimited genome position files.");
    CommonFlagCategories.setCategories(flags);
    flags.setValidator(new IndexerValidator());
    flags.registerOptional(CommonFlags.FORCE, "If the file isn't block compressed then perform this step as well").setCategory(CommonFlagCategories.UTILITY);
    final Flag inFlag = flags.registerRequired(File.class, "FILE", "block compressed files containing data to be indexed");
    inFlag.setCategory(CommonFlagCategories.INPUT_OUTPUT);
    inFlag.setMinCount(0);
    inFlag.setMaxCount(Integer.MAX_VALUE);
    inFlag.setPsuedoMinMaxRangeString(0, Integer.MAX_VALUE);
    final Flag listFlag = flags.registerOptional('I', CommonFlags.INPUT_LIST_FLAG, File.class, "FILE", "file containing a list of block compressed files (1 per line) containing genome position data").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerOptional('f', INPUT_FORMAT, IndexFormat.class, "FORMAT", "format of input to index", IndexFormat.AUTO).setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.addRequiredSet(inFlag);
    flags.addRequiredSet(listFlag);
  }

  private static class IndexerValidator implements Validator {
    @Override
    public boolean isValid(CFlags flags) {
      return CommonFlags.checkFileList(flags, CommonFlags.INPUT_LIST_FLAG, null, Integer.MAX_VALUE);
    }
  }

  private static IndexFormat getIndexFormat(final Collection<File> inputFiles, final IndexFormat suppliedFormat) {
    if (suppliedFormat != IndexFormat.AUTO) {
      return suppliedFormat;
    }
    IndexFormat res = null;
    for (final File f : inputFiles) {
      final String name = f.getName();
      for (final IndexFormat format : IndexFormat.values()) {
        final String ext = format.getExtension();
        if (ext != null && name.endsWith(ext)) {
          if (res != null && res != format) {
            return null; // Inputs appear to be mixed file types
          }
          res = format;
        }
      }
    }
    return res;
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    int retCode = 0;
    Collection<File> inputFiles = CommonFlags.getFileList(mFlags, CommonFlags.INPUT_LIST_FLAG, null, false);
    final IndexFormat format = getIndexFormat(inputFiles, (IndexFormat) mFlags.getValue(INPUT_FORMAT));
    if (format == null) {
      Diagnostic.warning("Could not automatically determine file format type, please use --" + INPUT_FORMAT);
      return 1;
    }
    if (mFlags.isSet(CommonFlags.FORCE)) {
      inputFiles = IndexUtils.ensureBlockCompressed(inputFiles);
    }
    for (final File f : inputFiles) {
      if (!TabixIndexer.isBlockCompressed(f)) {
        Diagnostic.warning("Cannot create index for " + f.getPath() + " as it is not in bgzip format.");
        retCode = 1;
        continue;
      }
      File indexFile =  new File(f.getParentFile(), f.getName() + TabixIndexer.TABIX_EXTENSION);
      final boolean indexExisted = indexFile.exists();
      try {
        switch (format) {
          case SAM:
            if (!SamUtils.looksLikeSam(f)) {
              Diagnostic.warning("File: " + f.getPath() + " does not have any headers, are you sure it is a SAM file?");
            }
            Diagnostic.info("Creating index for: " + f.getPath() + " (" + indexFile.getName() + ")");
            new TabixIndexer(f, indexFile).saveSamIndex();
            break;
          case SV:
            if (!Utils.isSvOutput(f)) {
              Diagnostic.warning("Cannot create index for " + f.getPath() + " as it is not a supported SV file.");
              retCode = 1;
              continue;
            }
            Diagnostic.info("Creating index for: " + f.getPath() + " (" + indexFile.getName() + ")");
            new TabixIndexer(f, indexFile).saveTsvIndex();
            break;
          case VCF:
            if (!Utils.isVcfFormat(f)) {
              Diagnostic.warning("Cannot create index for " + f.getPath() + " as it is not in VCF format.");
              retCode = 1;
              continue;
            }
            Diagnostic.info("Creating index for: " + f.getPath() + " (" + indexFile.getName() + ")");
            new TabixIndexer(f, indexFile).saveVcfIndex();
            break;
          case COVERAGETSV:
            if (!Utils.isCoverageOutput(f)) {
              Diagnostic.warning("Cannot create index for " + f.getPath() + " as it is not in the supported Coverage output format.");
              retCode = 1;
              continue;
            }
            Diagnostic.info("Creating index for: " + f.getPath() + " (" + indexFile.getName() + ")");
            new TabixIndexer(f, indexFile).saveBedIndex();
            break;
          case BED:
            Diagnostic.info("Creating index for: " + f.getPath() + " (" + indexFile.getName() + ")");
            new TabixIndexer(f, indexFile).saveBedIndex();
            break;
          case BAM:
            if (!SamUtils.isBAMFile(f)) {
              Diagnostic.warning("Cannot create index for " + f.getPath() + " as it is not a BAM file.");
              retCode = 1;
              continue;
            }
            //override file name for bam index
            indexFile =  new File(f.getParentFile(), f.getName() + BamIndexer.BAM_INDEX_EXTENSION);
            Diagnostic.info("Creating index for: " + f.getPath() + " (" + indexFile.getName() + ")");
            BamIndexer.saveBamIndex(f, indexFile);
            break;
          default:
            throw new RuntimeException();
        }
      } catch (IOException e) {
        if (indexFile.exists() && !indexExisted) {
          if (!indexFile.delete()) {
            Diagnostic.warning(WarningType.INFO_WARNING, "Could not delete file \"" + indexFile.getPath() + "\"");
          }
        }
        throw e;
      } catch (UnindexableDataException e) {
        if (indexFile.exists() && !indexExisted) {
          if (!indexFile.delete()) {
            Diagnostic.warning(WarningType.INFO_WARNING, "Could not delete file \"" + indexFile.getPath() + "\"");
          }
        }
        throw new NoTalkbackSlimException("Could not create index: " + e.getMessage());
      }
    }
    return retCode;
  }

}
