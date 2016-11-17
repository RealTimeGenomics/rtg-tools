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

package com.rtg.vcf;

import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.vcf.annotation.DerivedAnnotations;
import com.rtg.vcf.header.VcfHeader;

/**
 * Annotates a VCF file with contents of a BED file.  Annotations are appended as VCF INFO fields.
 *
 */
public final class VcfAnnotatorCli extends AbstractCli {

  private static final String MODULE_NAME = "vcfannotate";
  private static final String INPUT_FLAG = "input";
  private static final String BED_IDS_FLAG = "bed-ids";
  private static final String BED_INFO_FLAG = "bed-info";
  private static final String VCF_IDS_FLAG = "vcf-ids";
  private static final String INFO_ID_FLAG = "info-id";
  private static final String INFO_DESCR_FLAG = "info-description";
  private static final String FILL_AN_AC_FLAG = "fill-an-ac";
  private static final String RELABEL_FLAG = "relabel";
  private static final String X_DERIVED_ANNOTATIONS_FLAG = "Xderived-annotations";


  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "annotate variants within a VCF file";
  }

  @Override
  protected void initFlags() {
    mFlags.registerExtendedHelp();
    mFlags.setDescription("Adds annotations to a VCF file, either to the VCF ID field, or as a VCF INFO sub-field.");
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired('i', INPUT_FLAG, File.class, "file", "VCF file containing variants to annotate. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, "file", "output VCF file name. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(BED_IDS_FLAG, File.class, "file", "add variant IDs from BED file").setCategory(REPORTING).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(BED_INFO_FLAG, File.class, "file", "add INFO annotations from BED file").setCategory(REPORTING).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(VCF_IDS_FLAG, File.class, "file", "add variant IDs from VCF file").setCategory(REPORTING).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(INFO_ID_FLAG, String.class, "string", "the INFO ID for BED INFO annotations", "ANN").setCategory(REPORTING);
    mFlags.registerOptional(INFO_DESCR_FLAG, String.class, "string", "if the BED INFO field is not already declared, use this description in the header", "Annotation").setCategory(REPORTING);
    mFlags.registerOptional(RELABEL_FLAG, File.class, "file", "relabel samples according to \"old-name new-name\" pairs in specified file").setCategory(REPORTING);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    final List<String> derivedRange = new ArrayList<>();
    for (final DerivedAnnotations derived : DerivedAnnotations.values()) {
      derivedRange.add(derived.toString());
    }
    mFlags.registerOptional(FILL_AN_AC_FLAG, "add or update the AN and AC INFO fields").setCategory(REPORTING);
    mFlags.registerOptional(X_DERIVED_ANNOTATIONS_FLAG, String.class, "STRING", "derived fields to add to VCF file").setParameterRange(derivedRange).setMaxCount(Integer.MAX_VALUE).enableCsv().setCategory(REPORTING);
    mFlags.setValidator(new SnpAnnotatorValidator());
  }

  private static class SnpAnnotatorValidator implements Validator {
    @Override
    public boolean isValid(CFlags flags) {
      final File input = (File) flags.getValue(INPUT_FLAG);
      if (!CommonFlags.isStdio(input)) {
        if (!input.exists()) {
          flags.setParseMessage("Given file \"" + input.getPath() + "\" does not exist.");
          return false;
        }
        if (input.isDirectory()) {
          flags.setParseMessage("Given file \"" + input.getPath() + "\" is a directory.");
          return false;
        }
      }
      if (!flags.checkNand(BED_IDS_FLAG, VCF_IDS_FLAG)) {
        return false;
      }
      if (flags.isSet(BED_INFO_FLAG) && !checkFileList(flags, BED_INFO_FLAG)) {
        return false;
      }
      if (flags.isSet(BED_IDS_FLAG) && !checkFileList(flags, BED_IDS_FLAG)) {
        return false;
      }
      if (flags.isSet(VCF_IDS_FLAG) && !checkFileList(flags, VCF_IDS_FLAG)) {
        return false;
      }
      final File o = (File) flags.getValue(CommonFlags.OUTPUT_FLAG);
      if (!CommonFlags.isStdio(o)) {
        final File output = VcfUtils.getZippedVcfFileName(!flags.isSet(CommonFlags.NO_GZIP), o);
        if (output.exists()) {
          flags.setParseMessage("The file \"" + output.getPath() + "\" already exists. Please remove it first or choose a different file");
          return false;
        }
      }
      return true;
    }

    private boolean checkFileList(CFlags flags, String flag) {
      final Collection<Object> files = flags.getValues(flag);
      for (final Object f : files) {
        final File file = (File) f;
        if (!file.exists()) {
          flags.setParseMessage("Given file \"" + file.getPath() + "\" does not exist.");
          return false;
        }
        if (file.isDirectory()) {
          flags.setParseMessage("Given file \"" + file.getPath() + "\" is a directory.");
          return false;
        }
      }
      return true;
    }
  }

  private Collection<File> getFiles(String flag) {
    final Collection<Object> inputFiles = mFlags.getValues(flag);
    final ArrayList<File> retFiles = new ArrayList<>();
    for (final Object file : inputFiles) {
      retFiles.add((File) file);
    }
    return retFiles;
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {

    final EnumSet<DerivedAnnotations> derived = EnumSet.noneOf(DerivedAnnotations.class);
    if (mFlags.isSet(X_DERIVED_ANNOTATIONS_FLAG)) {
      for (final Object anno : mFlags.getValues(X_DERIVED_ANNOTATIONS_FLAG)) {
        derived.add(DerivedAnnotations.valueOf(anno.toString().toUpperCase(Locale.getDefault())));
      }
    }
    if (mFlags.isSet(FILL_AN_AC_FLAG)) {
      derived.add(DerivedAnnotations.AN);
      derived.add(DerivedAnnotations.AC);
    }

    final List<VcfAnnotator> annotators = new ArrayList<>();
    if (mFlags.isSet(BED_INFO_FLAG)) {
      annotators.add(new BedVcfAnnotator((String) mFlags.getValue(INFO_ID_FLAG), (String) mFlags.getValue(INFO_DESCR_FLAG), getFiles(BED_INFO_FLAG)));
    }
    if (mFlags.isSet(BED_IDS_FLAG)) {
      annotators.add(new BedVcfAnnotator(null, null, getFiles(BED_IDS_FLAG)));
    } else if (mFlags.isSet(VCF_IDS_FLAG)) {
      annotators.add(new VcfIdAnnotator(getFiles(VCF_IDS_FLAG)));
    }
    for (DerivedAnnotations anno : derived) {
      annotators.add(anno.getAnnotation());
    }
    if (mFlags.isSet(RELABEL_FLAG)) {
      annotators.add(VcfSampleNameRelabeller.create((File) mFlags.getValue(RELABEL_FLAG)));
    }

    final File inputFile = (File) mFlags.getValue(INPUT_FLAG);
    final File output = (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
    final boolean gzip = !mFlags.isSet(CommonFlags.NO_GZIP);
    final boolean index = !mFlags.isSet(CommonFlags.NO_INDEX);
    final boolean stdout = CommonFlags.isStdio(output);
    try (VcfReader reader = VcfReader.openVcfReader(inputFile)) {
      final VcfHeader header = reader.getHeader();
      for (final VcfAnnotator annotator : annotators) {
        annotator.updateHeader(header);
      }
      header.addRunInfo();
      final File vcfFile = stdout ? null : VcfUtils.getZippedVcfFileName(gzip, output);
      try (VcfWriter writer = new AsyncVcfWriter(new DefaultVcfWriter(header, vcfFile, out, gzip, index))) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          for (final VcfAnnotator annotator : annotators) {
            annotator.annotate(rec);
          }
          writer.write(rec);
        }
      }
    }
    return 0;
  }
}
