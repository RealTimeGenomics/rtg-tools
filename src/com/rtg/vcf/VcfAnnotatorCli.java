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

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.INPUT_FLAG;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.STRING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reader.SdfUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reader.SequencesReaderReferenceSource;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.annotation.DerivedAnnotations;
import com.rtg.vcf.annotation.SimpleTandemRepeatAnnotator;
import com.rtg.vcf.annotation.SplitContraryObservationAnnotator;
import com.rtg.vcf.annotation.VcfAnnotation;
import com.rtg.vcf.header.VcfHeader;

/**
 * Annotates a VCF file with contents of a BED file.  Annotations are appended as VCF INFO fields.
 */
public final class VcfAnnotatorCli extends AbstractCli {

  private static final String BED_IDS_FLAG = "bed-ids";
  private static final String BED_INFO_FLAG = "bed-info";
  private static final String VCF_IDS_FLAG = "vcf-ids";
  private static final String INFO_ID_FLAG = "info-id";
  private static final String INFO_DESCR_FLAG = "info-description";
  private static final String FILL_AN_AC_FLAG = "fill-an-ac";
  private static final String RELABEL_FLAG = "relabel";
  private static final String DERIVED_ANNOTATIONS_FLAG = "annotation";
  private static final String CLUSTER_FLAG = "Xcluster";
  private static final String STR_FLAG = "Xstr";

  /** All known annotators with zero-arg constructors */
  private static final Map<String, VcfAnnotator> ANNOTATORS = new TreeMap<>();
  static {
    for (DerivedAnnotations anno : DerivedAnnotations.values()) {
      final VcfAnnotation<?> annotator = anno.getAnnotation();
      ANNOTATORS.put(annotator.getName(), annotator);
    }
    ANNOTATORS.put("SCONT", new SplitContraryObservationAnnotator());
  }

  private SequencesReaderReferenceSource mRefSequencesSource = null;

  @Override
  public String moduleName() {
    return "vcfannotate";
  }

  @Override
  public String description() {
    return "annotate variants within a VCF file";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Adds annotations to a VCF file, either to the VCF ID field, or as a VCF INFO sub-field.");
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired('i', INPUT_FLAG, File.class, FILE, "VCF file containing variants to annotate. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, FILE, "output VCF file name. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(CommonFlags.NO_HEADER, "prevent VCF header from being written").setCategory(UTILITY);
    mFlags.registerOptional(BED_IDS_FLAG, File.class, FILE, "add variant IDs from BED file").setCategory(REPORTING).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(BED_INFO_FLAG, File.class, FILE, "add INFO annotations from BED file").setCategory(REPORTING).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(VCF_IDS_FLAG, File.class, FILE, "add variant IDs from VCF file").setCategory(REPORTING).setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional(INFO_ID_FLAG, String.class, STRING, "the INFO ID for BED INFO annotations", "ANN").setCategory(REPORTING);
    mFlags.registerOptional(INFO_DESCR_FLAG, String.class, STRING, "if the BED INFO field is not already declared, use this description in the header", "Annotation").setCategory(REPORTING);
    mFlags.registerOptional(RELABEL_FLAG, File.class, FILE, "relabel samples according to \"old-name new-name\" pairs in specified file").setCategory(REPORTING);
    VcfMerge.initAddHeaderFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    CommonFlags.initForce(mFlags);
    mFlags.registerOptional(FILL_AN_AC_FLAG, "add or update the AN and AC INFO fields").setCategory(REPORTING);
    mFlags.registerOptional('A', DERIVED_ANNOTATIONS_FLAG, String.class, STRING, "add computed annotation to VCF records").setParameterRange(ANNOTATORS.keySet()).setMaxCount(Integer.MAX_VALUE).enableCsv().setCategory(REPORTING);
    mFlags.registerOptional(CLUSTER_FLAG, "annotate records with number of nearby variants").setCategory(REPORTING);
    mFlags.registerOptional(STR_FLAG, File.class, "SDF", "annotate records with simple tandem repeat indicator based on given SDF").setCategory(REPORTING);
    mFlags.setValidator(new VcfAnnotatorValidator());
  }

  private static class VcfAnnotatorValidator implements Validator {
    @Override
    public boolean isValid(CFlags flags) {
      return CommonFlags.validateInputFile(flags, INPUT_FLAG)
        && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_FLAG)))
        && flags.checkNand(BED_IDS_FLAG, VCF_IDS_FLAG)
        && checkFileList(flags, BED_INFO_FLAG)
        && checkFileList(flags, BED_IDS_FLAG)
        && checkFileList(flags, VCF_IDS_FLAG)
        && (!flags.isSet(STR_FLAG) || CommonFlags.validateSDF(flags, STR_FLAG));
    }

    private boolean checkFileList(CFlags flags, String flag) {
      if (flags.isSet(flag)) {
        final Collection<?> files = flags.getValues(flag);
        for (final Object f : files) {
          if (!CommonFlags.validateInputFile(flags, (File) f)) {
            return false;
          }
        }
      }
      return true;
    }
  }

  private Collection<File> getFiles(String flag) {
    final Collection<?> inputFiles = mFlags.getValues(flag);
    return inputFiles.stream().map(file -> (File) file).collect(Collectors.toCollection(ArrayList::new));
  }

  private SequencesReader getReference(final File templateFile) throws IOException {
    SdfUtils.validateHasNames(templateFile);
    return SequencesReaderFactory.createDefaultSequencesReader(templateFile, LongRange.NONE);
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {

    final Set<String> derived = new LinkedHashSet<>();
    if (mFlags.isSet(DERIVED_ANNOTATIONS_FLAG)) {
      derived.addAll(mFlags.getValues(DERIVED_ANNOTATIONS_FLAG).stream().map(Object::toString).collect(Collectors.toList()));
    }
    if (mFlags.isSet(FILL_AN_AC_FLAG)) { // Convenience flag
      derived.add(DerivedAnnotations.AC.getAnnotation().getName());
      derived.add(DerivedAnnotations.AN.getAnnotation().getName());
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
    if (mFlags.isSet(STR_FLAG)) {
      mRefSequencesSource = getReference((File) mFlags.getValue(STR_FLAG)).referenceSource();
      annotators.add(new SimpleTandemRepeatAnnotator(mRefSequencesSource));
    }

    annotators.addAll(derived.stream().map(ANNOTATORS::get).collect(Collectors.toList()));

    if (mFlags.isSet(RELABEL_FLAG)) {
      annotators.add(VcfSampleNameRelabeller.create((File) mFlags.getValue(RELABEL_FLAG)));
    }

    final Collection<String> extraHeaderLines = VcfMerge.getHeaderLines(mFlags);
    final File inputFile = (File) mFlags.getValue(INPUT_FLAG);
    final File output = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final boolean stdout = FileUtils.isStdio(output);
    try (VcfReader reader = VcfReader.openVcfReader(inputFile)) {
      final VcfHeader header = reader.getHeader();
      VcfUtils.addHeaderLines(header, extraHeaderLines);
      for (final VcfAnnotator annotator : annotators) {
        annotator.updateHeader(header);
      }
      final File vcfFile = stdout ? null : VcfUtils.getZippedVcfFileName(gzip, output);
      try (VcfWriter writer = getVcfWriter(out, header, vcfFile)) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          for (final VcfAnnotator annotator : annotators) {
            annotator.annotate(rec);
          }
          writer.write(rec);
        }
      }
    }
    if (mRefSequencesSource != null) {
      mRefSequencesSource.close();
    }
    return 0;
  }

  private VcfWriter getVcfWriter(final OutputStream out, final VcfHeader header, final File vcfFile) throws IOException {
    final boolean isDensity = mFlags.isSet(CLUSTER_FLAG);
    final VcfWriter writer = new VcfWriterFactory(mFlags).addRunInfo(true).make(header, vcfFile, out);
    return isDensity ? new ClusterAnnotator(writer) : writer;
  }
}
