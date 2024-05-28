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
package com.rtg.variant.cnv.cnveval;

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.NO_INDEX;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rtg.bed.BedRecord;
import com.rtg.bed.BedUtils;
import com.rtg.bed.BedWriter;
import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.sam.SamRangeUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.Interval;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogStream;
import com.rtg.variant.cnv.CnaType;
import com.rtg.variant.cnv.CnvRecordFilter;
import com.rtg.variant.cnv.cnveval.CnaVariant.RegionContext;
import com.rtg.vcf.AssertVcfSorted;
import com.rtg.vcf.PassOnlyFilter;
import com.rtg.vcf.VcfFilter;
import com.rtg.vcf.VcfFilterIterator;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.eval.RocContainer;
import com.rtg.vcf.eval.RocFilter;
import com.rtg.vcf.eval.RocFilterProxy;
import com.rtg.vcf.eval.RocSortOrder;
import com.rtg.vcf.eval.RocSortValueExtractor;
import com.rtg.vcf.eval.VariantSetType;
import com.rtg.vcf.eval.VcfEvalCli;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Compare called CNV set with a baseline CNV set
 */
public class CnvEvalCli extends LoggedCli {

  private static final String REGION_NAMES = "names";
  private static final String RETAIN_OVERLAPS = "Xretain-overlapping-calls";

  /** Defines the RocFilters we want to use with cnveval */
  public enum CnvRocFilter implements RocFilterProxy {
    /** Only duplications **/
    DUP(new RocFilter("DUP") {
      @Override
      public boolean accept(VcfRecord rec, int[] gt) {
        return CnaType.valueOf(rec) == CnaType.DUP;
      }
      @Override
      public boolean requiresGt() {
        return false;
      }
    }),
    /** Only deletions **/
    DEL(new RocFilter("DEL") {
      @Override
      public boolean accept(VcfRecord rec, int[] gt) {
        return CnaType.valueOf(rec) == CnaType.DEL;
      }
      @Override
      public boolean requiresGt() {
        return false;
      }
    });

    RocFilter mFilter;
    CnvRocFilter(RocFilter f) {
      mFilter = f;
    }
    @Override
    public RocFilter filter() {
      return mFilter;
    }
  }


  @Override
  public String moduleName() {
    return "cnveval";
  }

  @Override
  public String description() {
    return "evaluate called CNV regions for agreement with a baseline CNV set";
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(OUTPUT_FLAG);
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription(StringUtils.sentencify(description()));
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initOutputDirFlag(mFlags);
    mFlags.registerRequired('b', VcfEvalCli.BASELINE, File.class, FILE, "VCF file containing baseline variants").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('c', VcfEvalCli.CALLS, File.class, FILE, "VCF file containing called variants").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('e', VcfEvalCli.EVAL_REGIONS_FLAG, File.class, FILE, "evaluate with respect to the supplied regions of interest BED file").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional(REGION_NAMES, "include evaluation region names in annotated outputs").setCategory(REPORTING);
    mFlags.registerOptional(VcfEvalCli.ALL_RECORDS, "use all records regardless of FILTER status (Default is to only process records where FILTER is \".\" or \"PASS\")").setCategory(FILTERING);

    VcfEvalCli.registerVcfRocFlags(mFlags, VcfUtils.QUAL, CnvRocFilter.class);

    mFlags.registerOptional(VcfEvalCli.NO_ROC, "do not produce ROCs").setCategory(REPORTING);
    mFlags.registerOptional(RETAIN_OVERLAPS, "retain overlapping calls").setCategory(UTILITY);

    CommonFlags.initThreadsFlag(mFlags);
    CommonFlags.initNoGzip(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateInputFile(flags, VcfEvalCli.BASELINE, VcfEvalCli.CALLS, VcfEvalCli.EVAL_REGIONS_FLAG)
      && CommonFlags.validateThreads(flags)
      && CommonFlags.validateRegions(flags)
      && flags.checkNand(VcfEvalCli.SCORE_FIELD, VcfEvalCli.NO_ROC)
      && VcfEvalCli.validateVcfRocFlags(flags));
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final boolean index = !mFlags.isSet(NO_INDEX);
    final boolean passOnly = !mFlags.isSet(VcfEvalCli.ALL_RECORDS);
    final boolean retainOverlappingCalls = mFlags.isSet(RETAIN_OVERLAPS);

    final ReferenceRanges<String> evalRegions = loadEvaluationRegions((File) mFlags.getValue(VcfEvalCli.EVAL_REGIONS_FLAG));
    final CnaVariantSet baseline = loadVariantSet(VariantSetType.BASELINE, (File) mFlags.getValue(VcfEvalCli.BASELINE), passOnly, evalRegions, true);
    final CnaVariantSet calls = loadVariantSet(VariantSetType.CALLS, (File) mFlags.getValue(VcfEvalCli.CALLS), passOnly, evalRegions, !retainOverlappingCalls);

    findMatches(baseline, calls);

    // Create ROC container / extractor
    final int sampleCol = 0; // Only used if extracting FORMAT sort fields, we may want to flagify it.
    final String scoreField = mFlags.isSet(VcfEvalCli.NO_ROC) ? null : (String) mFlags.getValue(VcfEvalCli.SCORE_FIELD);
    final RocSortValueExtractor rocExtractor = RocSortValueExtractor.getRocSortValueExtractor(scoreField, (RocSortOrder) mFlags.getValue(VcfEvalCli.SORT_ORDER));
    if (rocExtractor.requiresSample()) {
      Diagnostic.warning("Specified score field " + rocExtractor + " requires a sample column, using first");
    }

    final RocContainer roc = new RocContainer(rocExtractor);
    final Set<String> variantSubsets = new HashSet<>(Arrays.asList(
        CnvEvalCli.CnvRocFilter.DEL.name(), CnvEvalCli.CnvRocFilter.DUP.name()
    ));
    roc.addFilters(VcfEvalCli.getRocFilters(mFlags, new ArrayList<>(), variantSubsets));

    roc.setHeader(baseline.getHeader());
    for (final CnaVariantList chrVars : baseline.values()) {
      for (final CnaVariant v : chrVars) {
        if (v.context() == RegionContext.NORMAL) {
          roc.incrementBaselineCount(v.record(), sampleCol, v.isCorrect());
        }
      }
    }

    roc.setHeader(calls.getHeader());
    for (final CnaVariantList chrVars : calls.values()) {
      for (final CnaVariant v : chrVars) {
        if (v.context() == RegionContext.NORMAL) {
          if (v.isCorrect()) {
            roc.addRocLine(v.record(), sampleCol, 1, 0, 1);
          } else {
            roc.addRocLine(v.record(), sampleCol, 0, 1, 0);
          }
        }
      }
    }

    writeVariants(baseline, rocExtractor, sampleCol, gzip, index);
    writeVariants(calls, rocExtractor, sampleCol, gzip, index);

    roc.missingScoreWarning();
    roc.writeRocs(outputDirectory(), gzip, false);
    roc.writeSummary(outputDirectory());

    return 0;
  }

  private void writeVariants(CnaVariantSet variants, final RocSortValueExtractor extractor, final int sampleCol, boolean gzip, boolean index) throws IOException {
    final VariantSetType setType = variants.variantSetType();
    final boolean writeNames = mFlags.isSet(REGION_NAMES);

    // Write out a BED file of each evaluation region indicating status
    final List<BedAnnotation> annots = BedAnnotation.makeBedAnnotations(setType, extractor, sampleCol, writeNames);
    final File bedFile = FileUtils.getZippedFileName(gzip, new File(outputDirectory(), setType == VariantSetType.BASELINE ? "baseline.bed" : "calls.bed"));
    Diagnostic.userLog("Writing " + setType.label() + " region results to " + bedFile);
    try (final BedWriter w = new BedWriter(FileUtils.createOutputStream(bedFile))) {
      final ArrayList<String> cols = new ArrayList<>(Arrays.asList("chrom", "start", "end"));
      for (BedAnnotation b : annots) {
        cols.add(b.name());
      }
      w.writeComment(StringUtils.join("\t", cols));

      for (final CnaVariantList chrVars : variants.values()) {
        for (final CnaVariant v : chrVars) {
          w.write(new BedRecord(v.record().getSequenceName(), v.getStart(), v.getEnd(),
                                annots.stream().map(a -> a.value(v)).toArray(String[]::new)));
        }
      }
    }
    if (gzip && index) {
      BedUtils.createBedTabixIndex(bedFile);
    }

    // Write out VCF records with appropriate overall status annotations.
    final File vcfFile = FileUtils.getZippedFileName(gzip, new File(outputDirectory(), setType == VariantSetType.BASELINE ? "baseline.vcf" : "calls.vcf"));
    Diagnostic.userLog("Writing " + setType.label() + " VCF results to " + vcfFile);
    final VcfHeader header = variants.getHeader();
    final String infoHit;
    final String infoMiss;
    final String infoFrac;
    if (setType == VariantSetType.BASELINE) {
      infoHit = "BASE_TP";
      infoMiss = "BASE_FN";
      infoFrac = "BASE_FRAC";
      header.ensureContains(new InfoField(infoMiss, MetaType.INTEGER, VcfNumber.ONE, "Evaluation region FN count"));
    } else {
      infoHit  = "CALL_TP";
      infoMiss = "CALL_FP";
      infoFrac = "CALL_FRAC";
      header.ensureContains(new InfoField(infoMiss, MetaType.INTEGER, VcfNumber.ONE, "Evaluation region FP count"));
    }
    header.ensureContains(new InfoField(infoHit, MetaType.INTEGER, VcfNumber.ONE, "Evaluation region TP count"));
    header.ensureContains(new InfoField(infoFrac, MetaType.FLOAT, VcfNumber.ONE, "Evaluation region TP fraction"));
    final String infoName = "NAME";
    if (writeNames) {
      header.ensureContains(new InfoField(infoName, MetaType.STRING, VcfNumber.ONE, "Evaluation region names information"));
    }
    try (final VcfWriter writer = new VcfWriterFactory(mFlags).addRunInfo(true).make(header, vcfFile)) {
      for (final CnaRecordStats stats : variants.records()) {
        final VcfRecord rec = stats.record();
        if (stats.hit() > 0 || stats.miss() > 0) {
          rec.setInfo(infoFrac, String.format("%.3g", stats.hitFraction()));
        }
        if (stats.hit() > 0) {
          rec.setInfo(infoHit, Integer.toString(stats.hit()));
        }
        if (stats.miss() > 0) {
          rec.setInfo(infoMiss, Integer.toString(stats.miss()));
        }
        if (writeNames && !stats.names().isEmpty()) {
          rec.setInfo(infoName, StringUtils.join(",", stats.names()));
        }
        writer.write(rec);
      }
    }
  }

  private void findMatches(CnaVariantSet baseline, CnaVariantSet calls) {
    // Sets each CnaVariant correctness status
    for (Map.Entry<String, CnaVariantList> chrCalls : calls.entrySet()) {
      CnaVariantList chrBaseline = baseline.get(chrCalls.getKey());
      if (chrBaseline != null) {
        chrBaseline = (CnaVariantList) chrBaseline.clone();
        for (final CnaVariant c : chrCalls.getValue()) {
          if (c.context() == RegionContext.NORMAL) {
            for (int bPos = 0; bPos < chrBaseline.size(); ++bPos) {
              final CnaVariant b = chrBaseline.get(bPos);
              // If the baseline variant is encapsulated by the call, has the same amplification type, and hasn't already been matched, consider them matched
              if (b.context() == RegionContext.NORMAL && Interval.overlaps(b, c) && c.cnaType() == b.cnaType() && !b.isCorrect()) {
                c.setCorrect(true);
                b.setCorrect(true);
                chrBaseline.remove(bPos); // Less stuff to go through next time
                break;
                //            } else if (b.getEnd() < c.getStart()) { // Assuming calls is sorted.
                //              chrBaseline.remove(bPos--);
              }
            }
          }
        }
      }
    }

    // Sets per record info
    baseline.computeRecordCounts();
    calls.computeRecordCounts();
  }


  private ReferenceRanges<String> loadEvaluationRegions(File regionsFile) throws IOException {
    final ReferenceRanges<String> evalRegions = SamRangeUtils.createBedReferenceRanges(regionsFile);
    // Sanity check regions for empty set and overlappage
    int numRegions = 0;
    for (final String chr : evalRegions.sequenceNames()) {
      for (final RangeList.RangeView<String> region : evalRegions.get(chr).getRangeList()) {
        final List<String> names = region.getMeta();
        if (names.size() > 1) {
          Diagnostic.warning("Overlapping regions in evaluation BED at " + chr + ":" + region + " with labels: " + names);
          // Currently this means that we'll end up with an extra "pseudo-ROI" corresponding to the overlap, so that region effectively gets more weight in the evaluation.
          // e.g.:
          //   |---A---|-AB-|---B---|
          // Alternative approaches:
          // 1) Merge the overlapping regions into a single region, eg:
          //   |---------A'---------|
          // 2) Merge the overlap into one side or other, eg:
          //   |---A-----A--|---B---|
          // 3) Weight each ROI by it's length, so that it doesn't matter?
        }
        ++numRegions;
      }
    }
    if (numRegions == 0) {
      throw new NoTalkbackSlimException("No regions found in evaluation BED");
    }
    Diagnostic.userLog("Read " + numRegions + " evaluation regions over " + evalRegions.sequenceNames().size() + " chromosomes");
    return evalRegions;
  }

  // Load in a CNV variant set, computing the intersection with evaluation regions
  private CnaVariantSet loadVariantSet(VariantSetType setType, File vcfFile, boolean passOnly, ReferenceRanges<String> evalRegions, boolean removeOverlappingCalls) throws IOException {
    final List<VcfFilter> svFilt = new ArrayList<>();
    svFilt.add(new AssertVcfSorted());
    if (passOnly) {
      svFilt.add(new PassOnlyFilter());
    }
    svFilt.add(new CnvRecordFilter(evalRegions.sequenceNames(), removeOverlappingCalls));
    try (final VcfIterator vr = new VcfFilterIterator(VcfReader.openVcfReader(vcfFile),  svFilt)) {
      final CnaVariantSet variants = new CnaVariantSet(vr.getHeader(), setType);
      while (vr.hasNext()) {
        final VcfRecord rec = vr.next();
        final int end = VcfUtils.getEnd(rec);

        // Grab all the evaluation regions that it intersects with
        Diagnostic.userLog("Got a variant record: " + rec);
        final RangeList<String> rr = evalRegions.get(rec.getSequenceName());
        final List<RangeList.RangeView<String>> chrEvalRegions = rr.getFullRangeList();
        for (int hit = rr.findFullRangeIndex(rec.getStart()); hit < chrEvalRegions.size() && chrEvalRegions.get(hit).getStart() < end; ++hit) {
          final RangeList.RangeView<String> d = chrEvalRegions.get(hit);
          if (d.hasRanges()) {
            //System.err.println("range index=" + hit + "/" + chrEvalRegions.size() + " region=" + d + " meta=" + d.getMeta());
            if (d.getMeta().size() > 1) {
              Diagnostic.warning("SV record encompasses region where multiple evaluation regions overlap at " + rec.getSequenceName() + ":" + d);
            }
            final String names = StringUtils.join(",", d.getMeta());
            final CnaVariant v = new CnaVariant(d, rec, names);
            if (setType == VariantSetType.BASELINE && v.spanType() == CnaVariant.SpanType.PARTIAL) {
              // We currently expect baseline variants to fully span any given evaluation region, see sanityCheck below
              Diagnostic.warning(StringUtils.titleCase(setType.label()) + " variant at " + new SequenceNameLocusSimple(rec.getSequenceName(), rec.getStart(), end) + " only partially spans evaluation region " + d);
            }
            variants.add(v);
          }
        }
      }
      variants.loaded();

      Diagnostic.info("Read " + variants.toString());
      return variants;
    }
  }

}
