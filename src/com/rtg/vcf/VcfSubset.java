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
import static com.rtg.launcher.CommonFlags.NO_HEADER;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.STRING;
import static com.rtg.launcher.CommonFlags.STRING_OR_FILE;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.TsvParser;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.FilterField;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.SampleField;
import com.rtg.vcf.header.VcfHeader;

import htsjdk.samtools.util.RuntimeIOException;

/**
 */
public class VcfSubset extends AbstractCli {

  private static final String REMOVE_INFO = "remove-info";
  private static final String KEEP_INFO = "keep-info";
  private static final String REMOVE_INFOS = "remove-infos";

  private static final String REMOVE_FILTER = "remove-filter";
  private static final String KEEP_FILTER = "keep-filter";
  private static final String REMOVE_FILTERS = "remove-filters";

  private static final String REMOVE_SAMPLE = "remove-sample";
  private static final String KEEP_SAMPLE = "keep-sample";
  private static final String REMOVE_SAMPLES = "remove-samples";

  private static final String REMOVE_FORMAT = "remove-format";
  private static final String KEEP_FORMAT = "keep-format";

  private static final String REMOVE_QUAL = "remove-qual";

  private static final String REMOVE_ID = "remove-ids";

  private static final String REMOVE_UNUSED_ALTS = "Xremove-unused-alts";

  @Override
  public String moduleName() {
    return "vcfsubset";
  }

  @Override
  public String description() {
    return "create a VCF file containing a subset of the original columns";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Removes columnar data from VCF records.");
    CommonFlagCategories.setCategories(mFlags);

    mFlags.registerRequired('i', INPUT_FLAG, File.class, FILE, "VCF file containing variants to manipulate. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, FILE, "output VCF file. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    CommonFlags.initRegionOrBedRegionsFlags(mFlags);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    CommonFlags.initForce(mFlags);
    mFlags.registerOptional(NO_HEADER, "prevent VCF header from being written").setCategory(UTILITY);

    // Contents of FILTER
    mFlags.registerOptional(REMOVE_FILTER, String.class, STRING, "remove the specified FILTER tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_FILTER, String.class, STRING, "keep the specified FILTER tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(REMOVE_FILTERS, "remove all FILTER tags").setCategory(FILTERING);

    // Contents of INFO
    mFlags.registerOptional(REMOVE_INFO, String.class, STRING, "remove the specified INFO tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_INFO, String.class, STRING, "keep the specified INFO tag").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(REMOVE_INFOS, "remove all INFO tags").setCategory(FILTERING);

    // Contents of SAMPLE
    mFlags.registerOptional(REMOVE_SAMPLE, String.class, STRING_OR_FILE, "file containing sample IDs to remove, or a literal sample name").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_SAMPLE, String.class, STRING_OR_FILE, "file containing sample IDs to keep, or a literal sample name").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(REMOVE_SAMPLES, "remove all samples").setCategory(FILTERING);

    // Contents of FORMAT
    mFlags.registerOptional(REMOVE_FORMAT, String.class, STRING, "remove the specified FORMAT field").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_FORMAT, String.class, STRING, "keep the specified FORMAT field").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();

    // Contents of QUAL
    mFlags.registerOptional(REMOVE_QUAL, "remove the QUAL field").setCategory(FILTERING);

    // Contents of ID
    mFlags.registerOptional(REMOVE_ID, "remove the contents of the ID field").setCategory(FILTERING);

    // Contents of ALT
    mFlags.registerOptional(REMOVE_UNUSED_ALTS, "remove unused ALT alleles. Only GT is updated accordingly, other INFO/FORMAT fields are unaltered").setCategory(FILTERING);

    mFlags.setValidator(new VcfSubsetValidator());
  }

  private static class VcfSubsetValidator implements Validator {
    @Override
    public boolean isValid(final CFlags flags) {
      return CommonFlags.validateInputFile(flags, INPUT_FLAG)
        && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_FLAG)))
        && CommonFlags.validateRegions(flags)
        && flags.checkAtMostOne(REMOVE_INFOS, REMOVE_INFO, KEEP_INFO)
        && flags.checkAtMostOne(REMOVE_FILTERS, REMOVE_FILTER, KEEP_FILTER)
        && flags.checkAtMostOne(REMOVE_SAMPLES, REMOVE_SAMPLE, KEEP_SAMPLE)
        && flags.checkNand(REMOVE_FORMAT, KEEP_FORMAT);
    }
  }

  static class VcfSampleStripperFactory extends VcfAnnotatorFactory<VcfSampleStripper> {
    private static final int BAD_SAMPLE_MSG_LIMIT = 50;

    VcfSampleStripperFactory(CFlags flags) {
      super(flags);
    }
    @Override
    protected List<SampleField> getHeaderFields(VcfHeader header) {
      return header.getSampleLines();
    }
    @Override
    protected VcfSampleStripper makeRemoveAllAnnotator() {
      return new VcfSampleStripper(true);
    }
    @Override
    protected VcfSampleStripper makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
      return new VcfSampleStripper(fieldIdsSet, keep);
    }
    @Override
    protected void additionalChecks(String fieldname, Set<String> flagValues, VcfHeader header) {
      final Set<String> notFound = new LinkedHashSet<>(flagValues);
      notFound.removeAll(new HashSet<>(header.getSampleNames()));
      if (!notFound.isEmpty()) {
        final StringBuilder sb = new StringBuilder();
        int badCount = 0;
        for (final String value : notFound) {
          badCount++;
          if (badCount <= BAD_SAMPLE_MSG_LIMIT) {
            sb.append(' ').append(value);
          }
        }
        if (badCount > BAD_SAMPLE_MSG_LIMIT) {
          sb.append(" ...");
        }
        throw new NoTalkbackSlimException("" + badCount + " samples not contained in VCF:" + sb.toString());
      }
    }
    @Override
    protected Set<String> collectIds(String flag) {
      final Set<String> result = new LinkedHashSet<>();
      final TsvParser<Set<String>> p = new TsvParser<Set<String>>() {
        @Override
        protected void parseLine(String... parts) {
          result.add(parts[0]);
        }
      };
      for (final Object o : mFlags.getValues(flag)) {
        final String sampleOrFile = (String) o;
        if (sampleOrFile.length() > 0) {
          if (new File(sampleOrFile).exists()) {
            try {
              p.parse(new File(sampleOrFile));
            } catch (IOException e) {
              e.printStackTrace();
              throw new RuntimeIOException(e);
            }
          } else {
            result.add(sampleOrFile);
          }
        }
      }
      return result;
    }
    @Override
    public VcfSampleStripper make(VcfHeader header) {
      return processFlags(header, REMOVE_SAMPLE, KEEP_SAMPLE, REMOVE_SAMPLES, "Sample");
    }
  }
  static class VcfInfoStripperFactory extends VcfAnnotatorFactory<VcfInfoStripper> {
    VcfInfoStripperFactory(CFlags flags) {
      super(flags);
    }
    @Override
    protected List<InfoField> getHeaderFields(VcfHeader header) {
      return header.getInfoLines();
    }
    @Override
    protected VcfInfoStripper makeRemoveAllAnnotator() {
      return new VcfInfoStripper(true);
    }
    @Override
    protected VcfInfoStripper makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
      return new VcfInfoStripper(fieldIdsSet, keep);
    }
    @Override
    public VcfInfoStripper make(VcfHeader header) {
      return processFlags(header, REMOVE_INFO, KEEP_INFO, REMOVE_INFOS, "INFO");
    }
  }
  static class VcfFilterStripperFactory extends VcfAnnotatorFactory<VcfFilterStripper> {
    VcfFilterStripperFactory(CFlags flags) {
      super(flags);
    }
    @Override
    protected List<FilterField> getHeaderFields(VcfHeader header) {
      return header.getFilterLines();
    }
    @Override
    protected Collection<String> getHeaderIds(VcfHeader header) {
      final Collection<String> res = super.getHeaderIds(header);
      if (!res.contains(VcfUtils.FILTER_PASS)) {
        res.add(VcfUtils.FILTER_PASS);
      }
      return res;
    }
    @Override
    protected VcfFilterStripper makeRemoveAllAnnotator() {
      return new VcfFilterStripper(true);
    }
    @Override
    protected VcfFilterStripper makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
      return new VcfFilterStripper(fieldIdsSet, keep);
    }
    @Override
    public VcfFilterStripper make(VcfHeader header) {
      return processFlags(header, REMOVE_FILTER, KEEP_FILTER, REMOVE_FILTERS, "FILTER");
    }
  }
  static class VcfFormatStripperFactory extends VcfAnnotatorFactory<VcfFormatStripper> {
    VcfFormatStripperFactory(CFlags flags) {
      super(flags);
    }
    @Override
    protected List<FormatField> getHeaderFields(VcfHeader header) {
      return header.getFormatLines();
    }
    @Override
    protected VcfFormatStripper makeRemoveAllAnnotator() {
      throw new UnsupportedOperationException("Cannot remove all formats.");
    }
    @Override
    protected VcfFormatStripper makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
      return new VcfFormatStripper(fieldIdsSet, keep);
    }
    @Override
    public VcfFormatStripper make(VcfHeader header) {
      return processFlags(header, REMOVE_FORMAT, KEEP_FORMAT, null, "FORMAT");
    }
  }

  private <T extends VcfAnnotator> T addAnnotatorFromFlags(List<VcfAnnotator> annotators, VcfAnnotatorFactory<T> factory, VcfHeader header) {
    final T ann = factory.make(header);
    if (ann != null) {
      annotators.add(ann);
    }
    return ann;
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File output = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean gzip = !mFlags.isSet(NO_GZIP);

    final List<VcfAnnotator> annotators = new ArrayList<>();

    final File vcfFile = VcfUtils.getZippedVcfFileName(gzip, output);
    final VcfSampleStripperFactory sampleStripperFact = new VcfSampleStripperFactory(mFlags);
    try (final VcfReader reader = new VcfReaderFactory(mFlags).parser(new VcfSubsetParser(sampleStripperFact)).make(mFlags)) {
      final VcfHeader header = reader.getHeader();

      addAnnotatorFromFlags(annotators, new VcfInfoStripperFactory(mFlags), header);
      addAnnotatorFromFlags(annotators, new VcfFilterStripperFactory(mFlags), header);
      final VcfFormatStripper formatStripper = addAnnotatorFromFlags(annotators, new VcfFormatStripperFactory(mFlags), header);
      if (mFlags.isSet(REMOVE_UNUSED_ALTS)) {
        annotators.add(new VcfAltCleaner());
      }
      if (mFlags.isSet(REMOVE_QUAL)) {
        annotators.add(new VcfQualCleaner());
      }
      if (mFlags.isSet(REMOVE_ID)) {
        annotators.add(new VcfIdCleaner());
      }

      int skippedRecords = 0;
      for (final VcfAnnotator annotator : annotators) {
        annotator.updateHeader(header);
      }
      try (final VcfWriter writer = new VcfWriterFactory(mFlags).addRunInfo(true).make(header, vcfFile)) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          for (final VcfAnnotator annotator : annotators) {
            annotator.annotate(rec);
          }
          if (formatStripper != null) {
            if (formatStripper.keepRecord()) {
              writer.write(rec);
            } else {
              ++skippedRecords;
            }
          } else {
            writer.write(rec);
          }
        }
      }
      if (skippedRecords > 0) {
        Diagnostic.warning("Records skipped due to no remaining FORMAT fields: " + skippedRecords);
      }
    }
    return 0;
  }

}
