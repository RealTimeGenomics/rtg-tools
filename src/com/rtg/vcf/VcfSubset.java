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
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.FilterField;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.IdField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.SampleField;
import com.rtg.vcf.header.VcfHeader;

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
    mFlags.registerOptional(REMOVE_SAMPLE, String.class, STRING, "remove the specified sample").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
    mFlags.registerOptional(KEEP_SAMPLE, String.class, STRING, "keep the specified sample").setCategory(FILTERING).setMinCount(0).setMaxCount(Integer.MAX_VALUE).enableCsv();
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

  private abstract class AnnotatorAdder {

    abstract List<? extends IdField<?>> getHeaderFields(VcfHeader header);
    @SuppressWarnings("rawtypes")
    Collection<String> getHeaderIds(VcfHeader header) {
      return getHeaderFields(header).stream().map(IdField::getId).collect(Collectors.toSet());
    }
    void additionalChecks(Set<String> flagValues, VcfHeader header) { }
    abstract VcfAnnotator makeAnnotator(boolean removeAll);
    abstract VcfAnnotator makeAnnotator(Set<String> fieldIdsSet, boolean keep);

    private VcfAnnotator processFlags(List<VcfAnnotator> annotators, VcfHeader header, String removeFlag, String keepFlag, String fieldname) {
      return processFlags(annotators, header, removeFlag, keepFlag, null, fieldname, true);
    }

    private VcfAnnotator processFlags(List<VcfAnnotator> annotators, VcfHeader header, String removeFlag, String keepFlag, String removeAllFlag, String fieldname, boolean checkHeader) {
      if (removeAllFlag != null && mFlags.isSet(removeAllFlag)) {
        final VcfAnnotator annotator = makeAnnotator(true);
        annotators.add(annotator);
        return annotator;
      } else {
        if (mFlags.isSet(removeFlag) || mFlags.isSet(keepFlag)) {
          final boolean keep = !mFlags.isSet(removeFlag);
          final Set<String> ids = mFlags.getValues(mFlags.isSet(removeFlag) ? removeFlag : keepFlag).stream().map(f -> (String) f).collect(Collectors.toCollection(LinkedHashSet::new));

          if (checkHeader) {
            final Set<String> unknownIds = new LinkedHashSet<>(ids);
            unknownIds.removeAll(getHeaderIds(header));
            if (!unknownIds.isEmpty()) {
              Diagnostic.warning(fieldname + " fields not contained in VCF meta-information: " + StringUtils.join(' ', unknownIds));
            }
          }

          additionalChecks(ids, header);

          final VcfAnnotator annotator = makeAnnotator(ids, keep);
          annotators.add(annotator);
          return annotator;
        }
      }
      return null;
    }
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File output = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean gzip = !mFlags.isSet(NO_GZIP);

    final List<VcfAnnotator> annotators = new ArrayList<>();

    final File vcfFile = VcfUtils.getZippedVcfFileName(gzip, output);
    try (final VcfReader reader = VcfReader.openVcfReader(mFlags)) {
      final VcfHeader header = reader.getHeader();

      final AnnotatorAdder sampleAnnAdder = new AnnotatorAdder() {
        @Override
        List<SampleField> getHeaderFields(VcfHeader header) {
          return header.getSampleLines();
        }
        @Override
        VcfAnnotator makeAnnotator(boolean removeAll) {
          return new VcfSampleStripper(removeAll);
        }
        @Override
        VcfAnnotator makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
          return new VcfSampleStripper(fieldIdsSet, keep);
        }
        @Override
        void additionalChecks(Set<String> flagValues, VcfHeader header) {
          boolean fail = false;
          final StringBuilder sb = new StringBuilder();
          for (final String value : flagValues) {
            if (!header.getSampleNames().contains(value)) {
              fail = true;
              sb.append(value).append(' ');
            }
          }
          if (fail) {
            throw new NoTalkbackSlimException("Samples not contained in VCF: " + sb.toString().trim());
          }
        }
      };
      sampleAnnAdder.processFlags(annotators, header, REMOVE_SAMPLE, KEEP_SAMPLE, REMOVE_SAMPLES, "Sample", false);

      final AnnotatorAdder infoAnnAdder = new AnnotatorAdder() {
        @Override
        List<InfoField> getHeaderFields(VcfHeader header) {
          return header.getInfoLines();
        }
        @Override
        VcfAnnotator makeAnnotator(boolean removeAll) {
          return new VcfInfoStripper(removeAll);
        }
        @Override
        VcfAnnotator makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
          return new VcfInfoStripper(fieldIdsSet, keep);
        }
      };
      infoAnnAdder.processFlags(annotators, header, REMOVE_INFO, KEEP_INFO, REMOVE_INFOS, "INFO", true);

      final AnnotatorAdder filterAnnAdder = new AnnotatorAdder() {
        @Override
        List<FilterField> getHeaderFields(VcfHeader header) {
          return header.getFilterLines();
        }
        @Override
        Collection<String> getHeaderIds(VcfHeader header) {
          final Collection<String> res = super.getHeaderIds(header);
          if (!res.contains(VcfUtils.FILTER_PASS)) {
            res.add(VcfUtils.FILTER_PASS);
          }
          return res;
        }
        @Override
        VcfAnnotator makeAnnotator(boolean removeAll) {
          return new VcfFilterStripper(removeAll);
        }
        @Override
        VcfAnnotator makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
          return new VcfFilterStripper(fieldIdsSet, keep);
        }
      };
      filterAnnAdder.processFlags(annotators, header, REMOVE_FILTER, KEEP_FILTER, REMOVE_FILTERS, "FILTER", true);

      final AnnotatorAdder formatAnnAdder = new AnnotatorAdder() {
        @Override
        List<FormatField> getHeaderFields(VcfHeader header) {
          return header.getFormatLines();
        }
        @Override
        VcfAnnotator makeAnnotator(boolean removeAll) {
          throw new UnsupportedOperationException("Cannot remove all formats.");
        }
        @Override
        VcfAnnotator makeAnnotator(Set<String> fieldIdsSet, boolean keep) {
          return new VcfFormatStripper(fieldIdsSet, keep);
        }
      };
      final VcfFormatStripper formatStripper = (VcfFormatStripper) formatAnnAdder.processFlags(annotators, header, REMOVE_FORMAT, KEEP_FORMAT, "FORMAT");

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
      if (formatStripper != null) {
        formatStripper.updateHeader(header);
      }
      try (final VcfWriter writer = new VcfWriterFactory(mFlags).addRunInfo(true).make(header, vcfFile)) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          for (final VcfAnnotator annotator : annotators) {
            annotator.annotate(rec);
          }
          if (formatStripper != null) {
            formatStripper.annotate(rec);
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
