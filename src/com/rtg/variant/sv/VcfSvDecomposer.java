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

package com.rtg.variant.sv;

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.INPUT_FLAG;
import static com.rtg.launcher.CommonFlags.INT;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.NO_HEADER;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;
import static com.rtg.vcf.VcfUtils.INFO_CIEND;
import static com.rtg.vcf.VcfUtils.INFO_CIPOS;
import static com.rtg.vcf.VcfUtils.INFO_END;
import static com.rtg.vcf.VcfUtils.INFO_SVLEN;
import static com.rtg.vcf.VcfUtils.INFO_SVTYPE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.mode.DNA;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.BreakpointAlt;
import com.rtg.vcf.ReorderingVcfWriter;
import com.rtg.vcf.SymbolicAlt;
import com.rtg.vcf.VariantType;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.ContigField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Convert structural variants into breakend form.  All other records
 * are passed through without modification.
 */
public class VcfSvDecomposer extends AbstractCli {

  interface VcfRecordDecomposer {
    /**
     * Decompose the provided record. If no decomposition is possible, return null.
     * @param rec the input VCF record.
     * @return decomposed results, or null if no decomposition was possible.
     */
    VcfRecord[] decompose(VcfRecord rec);
  }

  private static final Collection<String> IGNORE_INFOS = new HashSet<>(Arrays.asList(INFO_SVTYPE, INFO_SVLEN, INFO_END, INFO_CIPOS, INFO_CIEND));

  private static final List<String> PRECISE_CIPOS = Collections.unmodifiableList(Arrays.asList("0", "0"));

  private static final String INDEL_LENGTH = "min-indel-length";
  private static final String RETAIN_ORIGINAL = "Xretain-original";

  // In some places we need to refer to the reference base immediately after the record REF. We don't have access to
  // the full reference, so we just use N as a placeholder
  private static final String NEXT_BASE = DNA.N.name();

  @Override
  public String moduleName() {
    return "svdecompose";
  }

  @Override
  public String description() {
    return "split composite structural variants into a breakend representation";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription(StringUtils.sentencify(description()));
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired('i', INPUT_FLAG, File.class, FILE, "VCF file containing variants to filter. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, FILE, "output VCF file name. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(INDEL_LENGTH, Integer.class, INT, "minimum length for converting precise insertions and deletions to breakend", 20).setCategory(UTILITY);
    mFlags.registerOptional(RETAIN_ORIGINAL, "retain original un-decomposed versions of all decomposed records").setCategory(UTILITY);
    mFlags.registerOptional(NO_HEADER, "prevent VCF header from being written").setCategory(UTILITY);
    CommonFlags.initForce(mFlags);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initForce(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateInputFile(flags, INPUT_FLAG)
      && flags.checkInRange(INDEL_LENGTH, 1, Integer.MAX_VALUE)
      && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_FLAG))));
  }

  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final File inputFile = (File) mFlags.getValue(INPUT_FLAG);
    final File output = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final boolean retainOriginal = mFlags.isSet(RETAIN_ORIGINAL);
    final boolean stdout = FileUtils.isStdio(output);
    final SvDecomposer d = new SvDecomposer((Integer) mFlags.getValue(INDEL_LENGTH));
    final Map<String, ArrayList<VcfRecord>> records = new LinkedHashMap<>();
    try (final VcfIterator reader = VcfReader.openVcfReader(inputFile)) {
      final VcfHeader header = reader.getHeader();
      header.getContigLines().stream().map(ContigField::getId).forEach(chr -> records.computeIfAbsent(chr, k -> new ArrayList<>())); // Ensure same sort order as header if possible
      while (reader.hasNext()) {
        final VcfRecord rec = reader.next();
        final VcfRecord[] decomposed = d.decompose(rec);
        if (decomposed != null) {
          add(records, decomposed);
        }
        if (decomposed == null || retainOriginal) {
          add(records, rec);
        }
      }

      records.values().forEach(v -> v.sort(new ReorderingVcfWriter.VcfPositionalComparator()));

      final File vcfFile = stdout ? null : VcfUtils.getZippedVcfFileName(gzip, output);
      try (final VcfWriter writer = new VcfWriterFactory(mFlags).addRunInfo(true).make(header, vcfFile, out)) {
        for (ArrayList<VcfRecord> recs : records.values()) {
          for (final VcfRecord rec : recs) {
            writer.write(rec);
          }
        }
      }
    }
    return 0;
  }

  static void add(final Map<String, ArrayList<VcfRecord>> records, final VcfRecord... recs) {
    Arrays.stream(recs).forEach(rec -> records.computeIfAbsent(rec.getSequenceName(), k -> new ArrayList<>()).add(rec));
  }

  static class SvDecomposer implements VcfRecordDecomposer {
    private final int mMinIndelLength;

    SvDecomposer(int minIndelLength) {
      mMinIndelLength = minIndelLength;
    }

    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String ref = rec.getAllele(0);
      final String alt = rec.getAllele(1);
      final VariantType vt = VariantType.getType(ref, alt);
      VcfRecord[] decomposed = null;
      if (vt == VariantType.SV_SYMBOLIC) {
        decomposed = decomposeSymbolic(rec, new SymbolicAlt(alt));
      } else if (vt.isIndelType() && Math.abs(ref.length() - alt.length()) >= mMinIndelLength) {
        decomposed = decomposeNonSymbolic(rec, ref, alt);
      }
      return decomposed;
    }

    private VcfRecord[] decomposeNonSymbolic(VcfRecord rec, String ref, String alt) {
      assert alt.length() != ref.length();
      if (alt.length() > ref.length()) {
        return new ShortInsDecomposer().decompose(rec);
      } else {
        return new ShortDelDecomposer().decompose(rec);
      }
    }

    private VcfRecord[] decomposeSymbolic(final VcfRecord rec, SymbolicAlt alt) {
      final ArrayList<String> svTypes = rec.getInfo().get(INFO_SVTYPE);
      if (svTypes != null && svTypes.size() == 1) { // We require (a single) SVTYPE in order to decompose symbolic alts
        final Integer end = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_END);
        if (end != null) { // We also require an END position
          // TODO: better handling of ref at remote ends and adjacent bases, currently just uses "N". Need to load reference
          switch (svTypes.get(0)) {

            case "DEL":
              return new SvDelDecomposer().decompose(rec);

            case "INS":
              return new SvInsDecomposer().decompose(rec);

            case "INV":
              return new SvInvDecomposer().decompose(rec);

            case "DUP":
              // In general DUP aren't really possible to decompose without the remote coordinates
              // But tandem DUP are a special case that can be handled, since we know where the duplicated sequence was placed.
              if (alt.getSubTypes().contains("TANDEM")) {
                return new SvTandemDupDecomposer().decompose(rec);
              }
              break;

            case "TRA":
              return new SvTraDecomposer().decompose(rec);

            // case "CNV":  // Treat the same as DUP/DEL?
            default:
              break;
          }
        }
      }
      return null; // Couldn't decompose
    }
  }

  abstract static class SvRecordDecomposer implements VcfRecordDecomposer {
    protected VcfRecord makeRecord(final String sequenceName, final int start, final String ref, final BreakpointAlt end, final VcfRecord rec, List<String> cipos) {
      // Copy across most of the content of the original record.
      final VcfRecord res = new VcfRecord(sequenceName, start, ref);
      res.setId(rec.getId());
      res.setQuality(rec.getQuality());
      res.addAltCall(end.toString());
      for (final String filter : rec.getFilters()) {
        res.addFilter(filter);
      }
      // Set SVTYPE to BND to reflect new record type, set CIPOS if needed, and copy INFO values except those in IGNORE_INFOS, copy all FORMAT values
      res.addInfo(INFO_SVTYPE, VcfUtils.SvType.BND.name());
      if (cipos != null) {
        cipos.forEach(v -> res.addInfo(INFO_CIPOS, v));
      }
      for (final Map.Entry<String, ArrayList<String>> info : rec.getInfo().entrySet()) {
        final String key = info.getKey();
        if (IGNORE_INFOS.contains(key)) {
          continue;
        }
        info.getValue().forEach(v -> res.addInfo(key, v));
      }
      res.setNumberOfSamples(rec.getNumberOfSamples());
      for (final Map.Entry<String, ArrayList<String>> format : rec.getFormatAndSample().entrySet()) {
        final String key = format.getKey();
        format.getValue().forEach(v -> res.addFormatAndSample(key, v));
      }
      return res;
    }
  }

  // Make equivalent breakends for the non-deleted section (both ends are on same sequence).
  static class ShortDelDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final String ref = rec.getRefCall();
      final int start = rec.getStart();
      final int end = rec.getStart() + ref.length() - 1;
      final String refFirst = ref.substring(0, 1); // Anchor base
      final String refLast = ref.substring(ref.length() - 1); // Deleted sequence
      final String alt = rec.getAllele(1);
      return new VcfRecord[] {
        makeRecord(sequenceName, start, refFirst, new BreakpointAlt(alt, true, sequenceName, end, false), rec, PRECISE_CIPOS),
        makeRecord(sequenceName, end, refLast, new BreakpointAlt(alt, false, sequenceName, start, true), rec, PRECISE_CIPOS),
      };
    }
  }

  // Make breakends going to a novel sequence
  // We use a symbolic contig which notionally corresponds to the sample chromosome with indels
  // Thus truth / query insertion calls that are nearby will have novel sequence coordinates that are also nearby,
  // facilitating breakend comparisons
  static class ShortInsDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final String novel = "<INS_" + rec.getSequenceName() + ">";
      final String ref = rec.getAllele(0);
      final String refFirst = ref.substring(0, 1); // first base
      final String refLast = ref.substring(ref.length() - 1); // last base
      final String alt = rec.getAllele(1);
      final int start = rec.getStart();
      final int insLength = alt.length() - 1;
      final int remoteEnd = rec.getStart() + insLength;
      return new VcfRecord[] {
        makeRecord(sequenceName, start, refFirst, new BreakpointAlt(alt.substring(0, 1), true, novel, start, false), rec, PRECISE_CIPOS),
        makeRecord(sequenceName, start + ref.length() - 1, refLast, new BreakpointAlt(alt.substring(insLength), false, novel, remoteEnd, true), rec, PRECISE_CIPOS),
      };
    }
  }

  // Make equivalent breakends for the non-deleted section (both ends are on same sequence).
  static class SvDelDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final int start = rec.getStart();
      final Integer end = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_END);
      final String ref = rec.getRefCall();
      return new VcfRecord[] {
        makeRecord(sequenceName, start, ref, new BreakpointAlt(ref, true, sequenceName, end - 1, false), rec, rec.getInfo().get(INFO_CIPOS)),
        makeRecord(sequenceName, end - 1, NEXT_BASE, new BreakpointAlt(ref, false, sequenceName, start, true), rec, rec.getInfo().get(INFO_CIEND)),
      };
    }
  }

  // Make breakends going to a novel sequence
  // We use a symbolic contig which notionally corresponds to the sample chromosome with indels
  // Thus truth / query insertion calls that are nearby will have novel sequence coordinates that are also nearby,
  // facilitating breakend comparisons
  static class SvInsDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final String novel = "<INS_" + rec.getSequenceName() + ">";
      final String ref = rec.getAllele(0);
      final int start = rec.getStart();
      final int insLength = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_SVLEN);
      final int remoteEnd = rec.getStart() + insLength;
      return new VcfRecord[] {
        makeRecord(sequenceName, start, ref, new BreakpointAlt(ref, true, novel, start, false), rec, rec.getInfo().get(INFO_CIPOS)),
        makeRecord(sequenceName, start, ref, new BreakpointAlt(NEXT_BASE, false, novel, remoteEnd, true), rec, rec.getInfo().get(INFO_CIEND)),
      };
    }
  }

  // Make equivalent breakends for both continuations of the current position.
  static class SvInvDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final int start = rec.getStart();
      final Integer end = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_END);
      final String ref = rec.getRefCall();
      return new VcfRecord[] {
        makeRecord(sequenceName, start, ref, new BreakpointAlt(ref, false, sequenceName, end, false), rec, rec.getInfo().get(INFO_CIPOS)),
        makeRecord(sequenceName, end, NEXT_BASE, new BreakpointAlt(NEXT_BASE, false, sequenceName, start, false), rec, rec.getInfo().get(INFO_CIEND)),
        makeRecord(sequenceName, start - 1, NEXT_BASE, new BreakpointAlt(NEXT_BASE, true, sequenceName, end - 1, true), rec, rec.getInfo().get(INFO_CIPOS)),
        makeRecord(sequenceName, end - 1, NEXT_BASE, new BreakpointAlt(NEXT_BASE, true, sequenceName, start - 1, true), rec, rec.getInfo().get(INFO_CIEND)),
      };
    }
  }

  static class SvTandemDupDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final int start = rec.getStart();
      final Integer end = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_END);
      final String ref = rec.getRefCall();
      return new VcfRecord[] {
        makeRecord(sequenceName, start, ref, new BreakpointAlt(ref, false, sequenceName, end, true), rec, rec.getInfo().get(INFO_CIPOS)),
        makeRecord(sequenceName, end, NEXT_BASE, new BreakpointAlt(NEXT_BASE, true, sequenceName, start, false), rec, rec.getInfo().get(INFO_CIEND)),
      };
    }
  }

  // These are NOT part of the VCF spec, but are produced by older versions of Delly. Convert to BND for compatibility
  static class SvTraDecomposer extends SvRecordDecomposer {
    @Override
    public VcfRecord[] decompose(VcfRecord rec) {
      final String sequenceName = rec.getSequenceName();
      final int start = rec.getStart();
      final Integer end = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_END);
      final String ref = rec.getRefCall();
      final BreakpointAlt a2 = makeBreakendFromTra(rec, ref, end);
      return a2 == null ? null : new VcfRecord[] {
        makeRecord(sequenceName, start, ref, a2, rec, rec.getInfo().get(INFO_CIPOS))
      };
    }

    static BreakpointAlt makeBreakendFromTra(VcfRecord rec, String ref, int end) {
      String seq2 = getStringInfoFieldFromRecord(rec, "CHR2");
      if (seq2 == null) {
        seq2 = rec.getSequenceName();
      }
      final String ct = getStringInfoFieldFromRecord(rec, "CT");
      return getBreakpointAlt(ref, seq2, end, ct);
    }

    static BreakpointAlt getBreakpointAlt(String ref, String seq2, int end, String ct) {
      switch (ct) {
        case "3to5":
          return new BreakpointAlt(ref, true, seq2, end, false);
        case "5to3":
          return new BreakpointAlt(ref, false, seq2, end, true);
        case "5to5":
          return new BreakpointAlt(ref, false, seq2, end, false);
        case "3to3":
          return new BreakpointAlt(ref, true, seq2, end, true);
        default:
          return null;
      }
    }

    private static String getStringInfoFieldFromRecord(VcfRecord rec, String field) {
      final Map<String, ArrayList<String>> infoField = rec.getInfo();
      if (infoField.containsKey(field)) {
        final String fieldVal = infoField.get(field).get(0);
        if (VcfRecord.MISSING.equals(fieldVal)) {
          return null;
        }
        return fieldVal;
      } else {
        return null;
      }
    }
  }

}
