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
package com.rtg.variant.sv.bndeval;

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.INT;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.REGION_SPEC;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.sam.SamRangeUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.IntervalComparator;
import com.rtg.util.intervals.Range;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.LogStream;
import com.rtg.vcf.AllMatchFilter;
import com.rtg.vcf.AltVariantTypeFilter;
import com.rtg.vcf.AssertVcfSorted;
import com.rtg.vcf.BreakpointAlt;
import com.rtg.vcf.PassOnlyFilter;
import com.rtg.vcf.VariantType;
import com.rtg.vcf.VcfFilter;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.eval.RocContainer;
import com.rtg.vcf.eval.RocFilter;
import com.rtg.vcf.eval.RocSortOrder;
import com.rtg.vcf.eval.RocSortValueExtractor;
import com.rtg.vcf.eval.VariantSetType;
import com.rtg.vcf.eval.VcfEvalCli;
import com.rtg.vcf.header.VcfHeader;

/**
 * Compare called structural variant breakend set with a baseline breakend set
 */
public class BndEvalCli extends LoggedCli {

  private static final String TOLERANCE = "tolerance";
  private static final String BIDIRECTIONAL = "bidirectional";
  private static final String OUTPUT_MODE = "output-mode";
  static final String MODE_ANNOTATE = "annotate";
  static final String MODE_SPLIT = "split";

  private RocContainer mRoc = null;
  private int mSampleCol;
  private int mTolerance;
  private boolean mBidirectional;

  @Override
  public String moduleName() {
    return "bndeval";
  }

  @Override
  public String description() {
    return "evaluate called breakends for agreement with a baseline breakend set";
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(OUTPUT_FLAG);
  }

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription("Evaluate called breakends for agreement with a baseline breakend set. Outputs a weighted ROC file which can be viewed with rtg rocplot and VCF files containing false positives (called breakends not matched in the baseline), false negatives (baseline breakends not matched in the call set), and true positives (breakends that match between the baseline and calls).");
    CommonFlags.initOutputDirFlag(mFlags);
    mFlags.registerRequired('b', VcfEvalCli.BASELINE, File.class, FILE, "VCF file containing baseline variants").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('c', VcfEvalCli.CALLS, File.class, FILE, "VCF file containing called variants").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(CommonFlags.RESTRICTION_FLAG, String.class, CommonFlags.REGION, "if set, only read VCF records within the specified range. " + REGION_SPEC).setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(CommonFlags.BED_REGIONS_FLAG, File.class, "File", "if set, only read VCF records that overlap the ranges contained in the specified BED file").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional(VcfEvalCli.ALL_RECORDS, "use all records regardless of FILTER status (Default is to only process records where FILTER is \".\" or \"PASS\")").setCategory(FILTERING);
    mFlags.registerOptional(TOLERANCE, Integer.class, INT, "positional tolerance for breakend matching", 100).setCategory(FILTERING);
    mFlags.registerOptional(BIDIRECTIONAL, "if set, allow matches between flipped breakends").setCategory(FILTERING);

    mFlags.registerOptional('f', VcfEvalCli.SCORE_FIELD, String.class, CommonFlags.STRING, "the name of the VCF FORMAT field to use as the ROC score. Also valid are \"QUAL\", \"INFO.<name>\" or \"FORMAT.<name>\" to select the named VCF FORMAT or INFO field", "INFO.DP").setCategory(REPORTING);
    mFlags.registerOptional('O', VcfEvalCli.SORT_ORDER, RocSortOrder.class, CommonFlags.STRING, "the order in which to sort the ROC scores so that \"good\" scores come before \"bad\" scores", RocSortOrder.DESCENDING).setCategory(REPORTING);
    final Flag<String> modeFlag = mFlags.registerOptional('m', OUTPUT_MODE, String.class, CommonFlags.STRING, "output reporting mode", MODE_SPLIT).setCategory(REPORTING);
    modeFlag.setParameterRange(new String[]{MODE_SPLIT, MODE_ANNOTATE});


    CommonFlags.initNoGzip(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.validateInputFile(flags, VcfEvalCli.BASELINE, VcfEvalCli.CALLS, VcfEvalCli.EVAL_REGIONS_FLAG)
      && CommonFlags.validateRegions(flags)
      && flags.checkInRange(TOLERANCE, 0, Integer.MAX_VALUE)
      && VcfEvalCli.validateScoreField(flags));
  }

  private static final class ChrBndVariants extends ArrayList<BndVariant> { }

  private static final class GenomeBndVariants extends LinkedHashMap<String, ChrBndVariants> {
    BndVariant[] byId() {
      int tot = 0;
      for (final ChrBndVariants chrVars : values()) {
        tot += chrVars.size();
      }
      final BndVariant[] res = new BndVariant[tot];
      for (final ChrBndVariants chrVars : values()) {
        for (final BndVariant v : chrVars) {
          assert res[v.getId()] == null;
          res[v.getId()] = v;
        }
      }
      for (final BndVariant v : res) {
        assert res[v.getId()] != null;
      }
      return res;
    }
  }


  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final boolean passOnly = !mFlags.isSet(VcfEvalCli.ALL_RECORDS);
    mTolerance = (Integer) mFlags.getValue(TOLERANCE);
    mBidirectional = mFlags.isSet(BIDIRECTIONAL);

    final ReferenceRanges<String> regions;
    if (mFlags.isSet(CommonFlags.BED_REGIONS_FLAG)) {
      Diagnostic.developerLog("Loading BED regions");
      regions = SamRangeUtils.createBedReferenceRanges((File) mFlags.getValue(CommonFlags.BED_REGIONS_FLAG));
    } else if (mFlags.isSet(CommonFlags.RESTRICTION_FLAG)) {
      regions = SamRangeUtils.createExplicitReferenceRange(new RegionRestriction((String) mFlags.getValue(CommonFlags.RESTRICTION_FLAG)));
    } else {
      regions = null;
    }

    final GenomeBndVariants baseline = loadVariantSet(VariantSetType.BASELINE, (File) mFlags.getValue(VcfEvalCli.BASELINE), passOnly, regions);
    final GenomeBndVariants calls = loadVariantSet(VariantSetType.CALLS, (File) mFlags.getValue(VcfEvalCli.CALLS), passOnly, regions);


    findMatches(baseline, calls);
    weightMatches(baseline);

    // Create ROC container / extractor
    final RocSortValueExtractor rocExtractor = RocSortValueExtractor.getRocSortValueExtractor((String) mFlags.getValue(VcfEvalCli.SCORE_FIELD), (RocSortOrder) mFlags.getValue(VcfEvalCli.SORT_ORDER));
    if (rocExtractor.requiresSample()) {
      Diagnostic.warning("Specified score field " + rocExtractor + " requires a sample column, using first");
    }
    mSampleCol = 0;
    mRoc = new RocContainer(rocExtractor);
    mRoc.addFilter(RocFilter.ALL);

    writeVariants(VariantSetType.BASELINE, (File) mFlags.getValue(VcfEvalCli.BASELINE), passOnly, regions, baseline.byId(), gzip);
    writeVariants(VariantSetType.CALLS, (File) mFlags.getValue(VcfEvalCli.CALLS), passOnly, regions, calls.byId(), gzip);
    mRoc.missingScoreWarning();
    mRoc.writeRocs(outputDirectory(), gzip, false);
    mRoc.writeSummary(outputDirectory());

    return 0;
  }

  // Load in a set of BND variants, converting each to a geometry object and assigning an id
  private GenomeBndVariants loadVariantSet(VariantSetType setType, File vcfFile, boolean passOnly, ReferenceRanges<String> regions) throws IOException {
    final VcfFilter allFilter = makeVcfFilter(passOnly);
    int variantId = 0;
    try (final VcfIterator vr = VcfReader.openVcfReader(vcfFile, regions)) {
      allFilter.setHeader(vr.getHeader());
      final GenomeBndVariants variants = new GenomeBndVariants();
      while (vr.hasNext()) {
        final VcfRecord rec = vr.next();
        if (allFilter.accept(rec)) {
          final BreakpointGeometry geometry = makeGeometry(rec, mTolerance);
          variants.computeIfAbsent(rec.getSequenceName(), k -> new ChrBndVariants()).add(new BndVariant(geometry, variantId));
          ++variantId;
        }
      }
      for (final ChrBndVariants chrVars : variants.values()) {
        chrVars.sort(IntervalComparator.SINGLETON);
      }
      Diagnostic.info("Read " + setType.label() + " variant set containing " + variantId + " BND variants on " + variants.keySet().size() + " chromosomes");
      return variants;
    }
  }

  private VcfFilter makeVcfFilter(boolean passOnly) {
    final List<VcfFilter> svFilt = new ArrayList<>();
    svFilt.add(new AssertVcfSorted());
    if (passOnly) {
      svFilt.add(new PassOnlyFilter());
    }
    svFilt.add(new AltVariantTypeFilter(EnumSet.of(VariantType.SV_BREAKEND)));
    return new AllMatchFilter(svFilt);
  }

  private static BreakpointGeometry makeGeometry(VcfRecord rec, int tolerance) {
    // TODO - perhaps incorporate CIPOS into the geometry (although this could give an apparent advantage to imprecise tools)
    // TODO - instead of hardcoding the first ALT, allow this to be sample-specific (will probably require some degree of ploidy-awareness)
    final BreakpointAlt b = new BreakpointAlt(rec.getAltCalls().get(0));

    final Orientation orientation = Orientation.orientation(b.isLocalUp() ? 1 : -1, b.isRemoteUp() ? 1 : -1);
    final int localPos = rec.getStart();
    final int xLo = localPos - orientation.x(tolerance); // Actually only used for r
    final int yLo = b.getRemotePos() - orientation.y(tolerance); // Actually only used for r
    final int xHi = localPos + orientation.x(tolerance + 1);
    final int yHi = b.getRemotePos() + orientation.y(tolerance + 1);
    final int rLo = orientation.r(xLo, yLo);
    final int rHi = orientation.r(xHi, yHi);
    // We shift the xHi and yHi values by tolerance (alternatively we could adjust both by tolerance/2)
    return new BreakpointGeometry(orientation, rec.getSequenceName(), b.getRemoteChr(), localPos, xHi, b.getRemotePos(), yHi, rLo, rHi);
  }

  private void findMatches(Map<String, ChrBndVariants> baseline, Map<String, ChrBndVariants> calls) {
    for (Map.Entry<String, ChrBndVariants> chrCalls : calls.entrySet()) {
      for (final BndVariant c : chrCalls.getValue()) {
        findMatches(baseline, c, c.getBreakpoint());
        if (mBidirectional) {
          findMatches(baseline, c, c.getBreakpoint().flip());
        }
      }
    }
  }

  static final Comparator<Range> START_COMPARATOR = (o1, o2) -> {
    if (o1.getStart() < o2.getStart()) {
      return -1;
    } else if (o1.getStart() > o2.getStart()) {
      return 1;
    }
    return 0;
  };

  private void findMatches(Map<String, ChrBndVariants> baseline, BndVariant c, AbstractBreakpointGeometry g) {
    final ChrBndVariants bndVariants = baseline.computeIfAbsent(g.getXName(), k -> new ChrBndVariants());
    // Determine earliest and latest position where a baseline variant could start that overlaps the supplied geometry
    // Since we don't segregate breakends by orientations, we use the min/max'es to get the outer range to scan over
    final int start = Math.min(g.getXLo(), g.getXHi()) - (mTolerance + 1);
    final int end = Math.max(g.getXLo(), g.getXHi());
    int pos = Collections.binarySearch(bndVariants, new Range(start, start), START_COMPARATOR);
    if (pos < 0) {
      pos = -(pos + 1);
    }
    while (pos < bndVariants.size() && bndVariants.get(pos).getStart() <= end) {
      final BndVariant b = bndVariants.get(pos);
      if (g.overlap(b.getBreakpoint())) {
        setMatch(b, c);
        //break;
      }
      pos++;
    }
  }

  private void setMatch(BndVariant baseline, BndVariant called) {
    Diagnostic.developerLog("Found match between:\n" + baseline.getBreakpoint() + "\n" + called.getBreakpoint());
    called.setCorrect(true);
    called.matches().add(baseline);
    baseline.setCorrect(true);
    baseline.matches().add(called);
  }

  // Distribute baseline weight among all the calls that matched it
  private void weightMatches(GenomeBndVariants baseline) {
    baseline.values().stream().flatMap(ChrBndVariants::stream).forEach(bVar -> {
      final double weight = 1.0 / bVar.matches().size();
      bVar.matches().forEach(cVar -> cVar.addWeight(weight));
    });
  }

  private void writeVariants(VariantSetType setType, File inVcf, boolean passOnly, ReferenceRanges<String> regions, BndVariant[] variants, boolean gzip) throws IOException {
    final VcfFilter allFilter = makeVcfFilter(passOnly);
    int variantId = 0;
    try (final VcfIterator vr = VcfReader.openVcfReader(inVcf, regions)) {
      allFilter.setHeader(vr.getHeader());
      try (final BndEvalVcfWriter w = makeWriter(setType, vr.getHeader(), outputDirectory(), gzip)) {
        while (vr.hasNext()) {
          final VcfRecord rec = vr.next();
          BndVariant v = null;
          if (allFilter.accept(rec)) {
            v = variants[variantId];
            switch (setType) {
              case BASELINE:
                mRoc.incrementBaselineCount(rec, mSampleCol, v.isCorrect());
                break;
              case CALLS:
                if (v.isCorrect()) {
                  mRoc.addRocLine(rec, mSampleCol, v.weight(), 0, 1);
                } else {
                  mRoc.addRocLine(rec, mSampleCol, 0, 1, 0);
                }
                break;
              default:
                throw new RuntimeException("Unknown variant set type");
            }
            ++variantId;
          }
          w.writeVariant(setType, rec, v);
        }
      }
    }
    if (variantId != variants.length) {
      throw new IOException("Could not find all original variants in " + inVcf + " during output stage");
    }
  }

  private BndEvalVcfWriter makeWriter(VariantSetType setType, VcfHeader header, File outdir, boolean gzip) throws IOException {
    final String mode = (String) mFlags.getValue(OUTPUT_MODE);
    switch (mode) {
      case MODE_ANNOTATE:
        return new AnnotatingBndEvalVcfWriter(setType, header, outdir, gzip);
      case MODE_SPLIT:
        return new SplitBndEvalVcfWriter(setType, header, outdir, gzip);
      default:
        throw new NoTalkbackSlimException("Unsupported output mode:" + mode);
    }
  }
}
