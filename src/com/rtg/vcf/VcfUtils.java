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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.rtg.mode.DnaUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Utility functions and constants for VCF records and files.
 */
public final class VcfUtils {

  /** Types of structural variants that may be used in <code>SVTYPE</code> field */
  public enum SvType {
    /** Break-end */
    BND,
    /** Deletion */
    DEL,
    /** Duplication */
    DUP,
    /** Inversion */
    INV,
    /** Insertion */
    INS,
  }

  private VcfUtils() { }

  /** Character used to separate alleles in unphased genotypes. */
  public static final char UNPHASED_SEPARATOR = '/';

  /** Character used to separate alleles in phased genotypes. */
  public static final char PHASED_SEPARATOR = '|';

  /** VCF file suffix. */
  public static final String VCF_SUFFIX = ".vcf";

  /** Character indicating field value separator. */
  public static final char VALUE_SEPARATOR = ';';

  /** Character indicating field value is missing. */
  public static final char MISSING_VALUE = '.';

  /** Integer value for a missing genotype. */
  public static final int MISSING_GT = -1;

  /** Missing field (e.g. filter/QUAL). */
  public static final String MISSING_FIELD = "" + MISSING_VALUE;


  /** QUAL column label. */
  public static final String QUAL = "QUAL";

  /** Pass filter flag. */
  public static final String FILTER_PASS = "PASS";


  /** ALT allele used to indicate haplotype is missing due to spanning deletion. */
  public static final char ALT_SPANNING_DELETION = '*';

  /** ALT corresponding to deletion */
  public static final String ALT_DEL = "<" + SvType.DEL + ">";

  /** ALT corresponding to duplication */
  public static final String ALT_DUP = "<" + SvType.DUP + ">";

  /** ALT corresponding to insertion */
  public static final String ALT_INS = "<" + SvType.INS + ">";


  /** Type of structural variant */
  public static final String INFO_SVTYPE = "SVTYPE";

  /** Difference in length between REF and structural variant */
  public static final String INFO_SVLEN = "SVLEN";

  /** Confidence interval for POS columns. */
  public static final String INFO_CIPOS = "CIPOS";

  /** Confidence interval for END columns. */
  public static final String INFO_CIEND = "CIEND";

  /** End position of structural variant */
  public static final String INFO_END = "END";

  /** Imprecise structural variant */
  public static final String INFO_IMPRECISE = "IMPRECISE";

  /** Allele frequency for alt alleles. */
  public static final String INFO_ALLELE_FREQ = "AF";

  /** Allele frequency for alt alleles. */
  public static final MetaType INFO_ALLELE_FREQ_TYPE = MetaType.FLOAT;

  /** Allele frequency for alt alleles. */
  public static final VcfNumber INFO_ALLELE_FREQ_NUM = VcfNumber.ALTS;

  /** Allele frequency for alt alleles. */
  public static final String INFO_ALLELE_FREQ_DESC = "Allele Frequency";

  /** Combined depth field. */
  public static final String INFO_COMBINED_DEPTH = "DP";


  /** Genotype format field. */
  public static final String FORMAT_GENOTYPE = "GT";

  /** AVR score format field. */
  public static final String FORMAT_AVR = "AVR";

  /** Genotype quality format field. */
  public static final String FORMAT_GENOTYPE_QUALITY = "GQ";

  /** Sample depth field. */
  public static final String FORMAT_SAMPLE_DEPTH = "DP";

  /** Ambiguity ratio field. */
  public static final String FORMAT_AMBIGUITY_RATIO = "AR";

  /** Allelic depth field. */
  public static final String FORMAT_ALLELIC_DEPTH = "AD";

  /** Somatic status field. */
  public static final String FORMAT_SOMATIC_STATUS = "SS";

  
  /** VCF FORMAT field used to indicate de novo alleles. */
  public static final String FORMAT_DENOVO = "DN";

  /** VCF FORMAT field used to indicate de novo score. */
  public static final String FORMAT_DENOVO_SCORE = "DNP";

  /** VCF FORMAT field for allelic quality */
  public static final String FORMAT_ALLELE_QUALITY = "AQ";

  /** VCF FORMAT field for variant allele */
  public static final String FORMAT_VARIANT_ALLELE = "VA";

  /** Allelic depth, error adjusted */
  public static final String FORMAT_ADE = "ADE";


  /** Filter status field. */
  public static final String FORMAT_FILTER = "FT";


  private static int alleleId(char c) {
    if (c == VcfUtils.MISSING_VALUE) {
      return MISSING_GT;
    } else if (c >= '0' && c <= '9') {
      return c - '0';
    } else {
      throw new NumberFormatException("Illegal character in genotype: " + c);
    }
  }

  private static int alleleId(String gt, int start, int length) {
    return length == 1
      ? alleleId(gt.charAt(start))
      : Integer.parseInt(gt.substring(start, start + length));
  }

  private static String encodeId(final int v) {
    return v == -1 ? MISSING_FIELD : String.valueOf(v);
  }

  /**
   * Utility method for splitting a VCF genotype subfield into an array of
   * numeric allele identifiers.
   *
   * @param gt a VCF genotype subfield (the <code>GT</code> value).
   * @return a new <code>int[]</code> containing one int per allele. Any missing values ('.') will be assigned index -1.
   * @throws NumberFormatException if the subfield is malformed.
   */
  public static int[] splitGt(String gt) {
    try {
      final int gtlen = gt.length();
      if (gtlen == 1) { // Typical haploid call
        return new int[]{alleleId(gt.charAt(0))};
      } else {
        int[] result = new int[2]; // Initialize assuming the most common case, diploid, and resize if needed
        int ploid = 0;
        int allelestart = 0;
        for (int i = 0; i < gtlen; ++i) {
          final char c = gt.charAt(i);
          if (c == PHASED_SEPARATOR || c == UNPHASED_SEPARATOR) {
            if (ploid == result.length) { // More than diploid call!
              result = Arrays.copyOf(result, result.length + 1);
            }
            result[ploid++] = alleleId(gt, allelestart, i - allelestart);
            allelestart = i + 1;
          }
        }
        if (allelestart < gtlen) {
          if (ploid == result.length) { // More than diploid call!
            result = Arrays.copyOf(result, result.length + 1);
          }
          result[ploid++] = alleleId(gt, allelestart, gtlen - allelestart);
        }
        if (ploid < result.length) { // Can only happen if a haploid genotype with allele id > 9
          result = Arrays.copyOf(result, ploid);
        }
        if (ploid == 0) {
          throw new NumberFormatException();
        }
        return result;
      }
    } catch (NumberFormatException e) {
      throw new VcfFormatException("Malformed VCF GT value \"" + gt + "\"");
    }
  }

  /**
   * Utility method for creating a VCF genotype subfield from an array of
   * numeric allele identifiers.
   *
   * @param phased if true, use the phased separator
   * @param gt a VCF genotype subfield (the <code>GT</code> value).
   * @return a new <code>int[]</code> containing one int per allele. Any missing values ('.') will be assigned index -1.
   */
  public static String joinGt(boolean phased, int... gt) {
    final char sep = phased ? PHASED_SEPARATOR : UNPHASED_SEPARATOR;
    switch (gt.length) {
      case 0: // missing
        return MISSING_FIELD;
      case 1: // haploid
        return encodeId(gt[0]);
      case 2: // diploid
        return encodeId(gt[0]) + sep + encodeId(gt[1]);
      default: // polyploid
        final StringBuilder sb = new StringBuilder();
        for (final int c : gt) {
          if (sb.length() > 0) {
            sb.append(sep);
          }
          sb.append(encodeId(c));
        }
        return sb.toString();
    }
  }

  /**
   * Check whether the split GT array references valid allele IDs
   * @param rec the record containing ref and alt alleles
   * @param gt the split GT.
   * @return false if any of the GT elements are out of range.
   */
  public static boolean isValidGt(VcfRecord rec, int[] gt) {
    final int maxId = rec.getAltCalls().size();
    for (int gtId : gt) {
      if (gtId < MISSING_GT || gtId > maxId) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if the supplied file is has an extension indicative of VCF.
   * @param f file to test
   * @return true if file has <code>vcf</code> or gzipped <code>vcf</code> extension
   */
  public static boolean isVcfExtension(File f) {
    return f.getName().endsWith(VCF_SUFFIX) || f.getName().endsWith(VCF_SUFFIX + FileUtils.GZ_SUFFIX);
  }

  /**
   * Converts a VCF file name into one with an appropriate extension, depending on whether the file should be gzipped.
   * @param gzip true if the output file is destined to be GZIP compressed.
   * @param file the input file
   * @return the appropriately adjusted file
   */
  public static File getZippedVcfFileName(final boolean gzip, final File file) {
    return FileUtils.getOutputFileName(file, gzip, VCF_SUFFIX);
  }

  /**
   * Get the sample index for the given sample name. Dies gracefully if the input has
   * multiple samples but no name was provided, or if the name provided was not contained
   * in the available names.
   * @param header the VCF header
   * @param sampleName the sample name to find, may be null
   * @param label a label to use in error messages, e.g. "input"
   * @return the sample index
   * @throws NoTalkbackSlimException if we could not find the index.
   */
  public static int getSampleIndexOrDie(VcfHeader header, final String sampleName, final String label) {
    int sampleIndex = 0;
    if (sampleName != null) {
      sampleIndex = header.getSampleNames().indexOf(sampleName);
      if (sampleIndex == -1) {
        throw new NoTalkbackSlimException("Sample \"" + sampleName + "\" not found in " + label + " VCF.");
      }
    } else if (header.getSampleNames().size() > 1) {
      throw new NoTalkbackSlimException("No sample name provided but " + label + " is a multi-sample VCF.");
    }
    return sampleIndex;
  }

  /**
   * If GT is either missing or is same as reference then return true.
   * @param gt GT to check
   * @return true if gt does not represent a variant, false otherwise
   */
  public static boolean isNonVariantGt(int[] gt) {
    for (int g : gt) {
      if (g > 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * If GT is either missing or is same as reference then return true.
   * @param gt GT to check
   * @return true if gt does not represent a variant, false otherwise
   */
  public static boolean isNonVariantGt(String gt) {
    final int gtlen = gt.length();
    for (int i = 0; i < gtlen; ++i) {
      final char c = gt.charAt(i);
      switch (c) {
        case PHASED_SEPARATOR:
        case UNPHASED_SEPARATOR:
        case '0':
        case MISSING_VALUE:
          continue;
        default:
          return false;
      }
    }
    return true;
  }

  /**
   * If GT is not missing and not the same as reference then return true.
   * @param gt GT to check
   * @return true if gt represents a variant, false otherwise
   */
  public static boolean isVariantGt(int[] gt) {
    return !isNonVariantGt(gt);
  }

  /**
   * If GT is not missing and not the same as reference then return true.
   * @param gt GT to check
   * @return true if gt represents a variant, false otherwise
   */
  public static boolean isVariantGt(String gt) {
    return !isNonVariantGt(gt);
  }

  /**
   * @param gt GT to check
   * @return true if gt represents a phased genotype
   */
  public static boolean isPhasedGt(String gt) {
    return gt.indexOf(PHASED_SEPARATOR) >= 0;
  }

  /**
   * If any of the GT is non missing return true.
   * @param gt GT to check
   * @return true if gt is not entirely should be skipped, false otherwise
   */
  public static boolean isNonMissingGt(int[] gt) {
    for (final int g : gt) {
      if (g > MISSING_GT) {
        return true;
      }
    }
    return false;
  }

  /**
   * If any of the GT is non missing return true.
   * @param gt GT to check
   * @return true if gt is not entirely should be skipped, false otherwise
   */
  public static boolean isNonMissingGt(String gt) {
    final int gtlen = gt.length();
    for (int i = 0; i < gtlen; ++i) {
      final char c = gt.charAt(i);
      switch (c) {
        case PHASED_SEPARATOR:
        case UNPHASED_SEPARATOR:
        case MISSING_VALUE:
          continue;
        default:
          return true;
      }
    }
    return false;
  }

  /**
   * If all sub-alleles of the GT is missing return true.
   * @param gt GT to check
   * @return true if gt should be skipped, false otherwise
   */
  public static boolean isMissingGt(int[] gt) {
    return !isNonMissingGt(gt);
  }

  /**
   * If all sub-alleles of the GT is missing return true.
   * @param gt GT to check
   * @return true if gt should be skipped, false otherwise
   */
  public static boolean isMissingGt(String gt) {
    return !isNonMissingGt(gt);
  }

  /**
   * Returns the VCF header for given file.
   * @param input file to read VCF header from
   * @return The VcfHeader
   * @throws IOException if an I/O error occurs
   */
  public static VcfHeader getHeader(final File input) throws IOException {
    try (VcfReader reader = VcfReader.openVcfReader(input)) {
      return reader.getHeader();
    }
  }

  /**
   * Normalize by converting any DNA characters within a VCF allele into uppercase. Annoyingly,
   * VCF specification says that DNA bases are case-insensitive, while symbolic alleles,
   * chromosome names (which may be embedded inside breakends) are case-sensitive.
   * @param allele the VCF allele
   * @return the normalized allele
   */
  public static String normalizeAllele(String allele) {
    if (allele.length() == 0) {
      return allele;
    } else {
      int ntype = 0;
      final char[] res = allele.toCharArray();
      for (int i = 0; i < res.length; ++i) {
        final char c = res[i];
        switch (c) {
          case 'a':
          case 't':
          case 'c':
          case 'g':
          case 'n':
            if (ntype == 0) {  // Inside nucleotides
              res[i] = Character.toUpperCase(c);
            }
            break;
          case '<':
            if (ntype == 0) {
              ntype = 1;  // Start symbolic allele
            }
            break;
          case '>':
            if (ntype == 1) {
              ntype = 0;   // End symbolic allele
            }
            break;
          case '[':
          case ']':
            if (ntype == 0) {
              ntype = 2; // Start breakend
            } else if (ntype == 2) {
              ntype = 0; // End breakend
            }
            break;
          default:
            break;
        }
      }
      return new String(res);
    }
  }

  // The following could all be moved to VcfRecord itself

  /**
   * Test if a VCF record was produced by the RTG complex caller.
   * @param rec record to look up
   * @return true if the record was produced by the complex caller
   */
  public static boolean isComplexScored(VcfRecord rec) {
    return rec.getInfo().containsKey("XRX");
  }

  /**
   * Return genotype quality for the record if present,
   * @param rec record to lookup
   * @param sample sample number
   * @return genotype quality for the record if present, or it return <code>Double.NAN</code> if genotype quality is unavailable
   */
  public static double getGenotypeQuality(VcfRecord rec, int sample) {
    return getDoubleFormatFieldFromRecord(rec, sample, FORMAT_GENOTYPE_QUALITY);
  }

  /**
   * Get the value of a particular field for a sample in the form of a double.
   * @param rec VCF record
   * @param sample index of sample to extract value from
   * @param field string ID of field to extract
   * @return the value converted into a double, or {@code Double.NaN} if missing
   */
  public static double getDoubleFormatFieldFromRecord(VcfRecord rec, int sample, String field) {
    final ArrayList<String> format = rec.getFormat(field);
    if (format != null) {
      final String fieldVal = format.get(sample);
      try {
        if (VcfRecord.MISSING.equals(fieldVal)) {
          return Double.NaN;
        }
        return Double.parseDouble(fieldVal);
      } catch (final NumberFormatException ex) {
        throw new VcfFormatException("Invalid numeric value \"" + fieldVal + "\" in \"" + field + "\" for VCF record :" + rec);
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * Get a multivalued integer field for a sample in the form of an integer array.
   * @param rec VCF record
   * @param field string ID of field to extract
   * @return the value or null if missing
   */
  public static int[] getConfidenceInterval(final VcfRecord rec, final String field) {
    final ArrayList<String> info = rec.getInfo().get(field);
    if (info != null) {
      try {
        final int[] res = new int[info.size()];
        for (int k = 0; k < res.length; ++k) {
          final String fieldVal = info.get(k);
          if (VcfRecord.MISSING.equals(fieldVal)) {
            return null;
          }
          res[k] = Integer.parseInt(fieldVal);
        }
        return res;
      } catch (final NumberFormatException ex) {
        throw new VcfFormatException("Invalid value \"" + info + "\" in \"" + field + "\" for VCF record :" + rec);
      }
    } else {
      return null;
    }
  }

  /**
   * If field contains multiple values will get the first value.
   * @param rec VCF record
   * @param field string ID of field to extract
   * @return the value converted into a double, or {@code Double.NaN} if missing
   */
  public static double getDoubleInfoFieldFromRecord(VcfRecord rec, String field) {
    final Map<String, ArrayList<String>> infoField = rec.getInfo();
    if (infoField.containsKey(field)) {
      final String fieldVal = infoField.get(field).get(0);
      try {
        if (VcfRecord.MISSING.equals(fieldVal)) {
          return Double.NaN;
        }
        return Double.parseDouble(fieldVal);
      } catch (NumberFormatException ex) {
        throw new VcfFormatException("Invalid numeric value \"" + fieldVal + "\" in \"" + field + "\" for VCF record :" + rec);
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * If field contains multiple values will get the first value.
   * @param rec VCF record
   * @param field string ID of field to extract
   * @return the value converted into a double, or {@code Double.NaN} if missing
   */
  public static Integer getIntegerInfoFieldFromRecord(VcfRecord rec, String field) {
    final Map<String, ArrayList<String>> infoField = rec.getInfo();
    if (infoField.containsKey(field)) {
      final String fieldVal = infoField.get(field).get(0);
      try {
        if (VcfRecord.MISSING.equals(fieldVal)) {
          return null;
        }
        return Integer.valueOf(fieldVal);
      } catch (NumberFormatException ex) {
        throw new VcfFormatException("Invalid numeric value \"" + fieldVal + "\" in \"" + field + "\" for VCF record :" + rec);
      }
    } else {
      return null;
    }
  }

  /**
   * Tests if the given sample genotype in a record is homozygous ALT.
   * @param rec record to check
   * @param sample sample number
   * @return true iff homozygous
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isHomozygousAlt(VcfRecord rec, int sample) {
    final int[] gtArray = getValidGt(rec, sample);
    return isHomozygousAlt(gtArray);
  }

  /**
   * Tests if the given sample genotype in a record is heterozygous or not.
   * @param rec record to check
   * @param sample sample number
   * @return true iff heterozygous
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isHeterozygous(VcfRecord rec, int sample) {
    final int[] gtArray = getValidGt(rec, sample);
    return isHeterozygous(gtArray);
  }

  /**
   * Tests if the given sample genotype in a record is haploid or not.
   * @param rec record to check
   * @param sample sample number
   * @return true iff haploid
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isHaploid(VcfRecord rec, int sample) {
    final int[] gtArray = getValidGt(rec, sample);
    return gtArray.length == 1;
  }

  /**
   * Tests if the given sample genotype in a record is diploid or not.
   * @param rec record to check
   * @param sample sample number
   * @return true iff diploid
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isDiploid(VcfRecord rec, int sample) {
    final int[] gtArray = getValidGt(rec, sample);
    return gtArray.length == 2;
  }

  /**
   * @param gt the genotype allele ids
   * @return true if the GT is homozygous ALT
   */
  public static boolean isHomozygousAlt(int[] gt) {
    return isHomozygous(gt) && !isHomozygousRef(gt);
  }

  /**
   * @param gt the genotype allele ids
   * @return true if the GT is homozygous (including reference)
   */
  public static boolean isHomozygous(int[] gt) {
    return gt.length == 1 || gt[0] == gt[1];
  }

  /**
   * @param gt the genotype allele ids
   * @return true if the GT is heterozygous
   */
  public static boolean isHeterozygous(int[] gt) {
    return gt.length == 2 && gt[0] != gt[1];
  }

  /**
   * @param gt the genotype allele ids
   * @return true if the GT is homozygous REF
   */
  public static boolean isHomozygousRef(int[] gt) {
    for (int g : gt) {
      if (g != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the split GT for a sample, with validation checking
   * @param rec record to extract the GT from
   * @param sample the sample index
   * @return the split GT field
   * @throws NoTalkbackSlimException if the record does not contain GT values, the sample is invalid, or the GT itself is invalid.
   */
  public static int[] getValidGt(VcfRecord rec, int sample) {
    final int[] gtArr = splitGt(getValidGtStr(rec, sample));
    if (!isValidGt(rec, gtArr)) {
      throw new VcfFormatException("VCF record GT contains allele ID out of range, record: " + rec.toString());
    }
    return gtArr;
  }

  /**
   * Get the string GT for a sample, with validation checking
   * @param rec record to extract the GT from
   * @param sample the sample index
   * @return the split GT field
   * @throws NoTalkbackSlimException if the record does not contain GT values, or the sample is invalid.
   */
  public static String getValidGtStr(VcfRecord rec, int sample) {
    final ArrayList<String> gtList = rec.getFormat(FORMAT_GENOTYPE);
    if (gtList == null) {
      throw new VcfFormatException("VCF record does not contain GT field, record: " + rec.toString());
    }
    if (sample >= gtList.size()) {
      throw new VcfFormatException("Invalid sample number " + sample + ", record: " + rec.toString());
    }
    return gtList.get(sample);
  }

  /**
   * Check if the record contains a well-defined variant genotype for the specified sample.
   * Returns false if the record has no GT field, or if the GT is same as reference or refers to symbolic alleles.
   *
   * @param rec record to check
   * @param sampleId sample if to check
   * @return true if a well defined genotype is present, false otherwise
   */
  public static boolean hasDefinedVariantGt(VcfRecord rec, int sampleId) {
    if (sampleId >= rec.getNumberOfSamples()) {
      throw new VcfFormatException("Record did not contain enough samples: " + rec.toString());
    }
    if (!rec.hasFormat(FORMAT_GENOTYPE)) {
      return false;
    }
    final int[] gtArr = getValidGt(rec, sampleId);
    final VariantType type = VariantType.getType(rec, gtArr);
    return !type.isNonVariant() && (!type.isSvType() || type == VariantType.SV_MISSING);
  }

  /**
   * Determines if this record has a redundant leading base (e.g. for indels).
   * @param rec the record to examine
   * @return true if the leading base of the variant is redundant.
   */
  public static boolean hasRedundantFirstNucleotide(final VcfRecord rec) {
    final String ref = rec.getRefCall();
    final Character c = ref.length() == 0 ? VcfUtils.MISSING_VALUE : ref.charAt(0);
    for (final String alt : rec.getAltCalls()) {
      if (!c.equals(alt.charAt(0))) {
        return false;
      }
    }
    return !rec.getAltCalls().isEmpty();
  }

  /**
   * Get an array containing the set of all alleles as strings. If there is a redundant leading nucleotide, this is
   * stripped. The reference allele is first.
   * @param rec the record to get the alleles from
   * @return an array containing the alleles.
   */
  public static String[] getAlleleStrings(VcfRecord rec) {
    return getAlleleStrings(rec, hasRedundantFirstNucleotide(rec));
  }

  /**
   * Get an array containing the set of all alleles as strings. The reference allele is first.
   * @param rec the record to get the alleles from
   * @param prevNt if true, strip the leading base off each allele
   * @return an array containing the alleles.
   */
  public static String[] getAlleleStrings(VcfRecord rec, boolean prevNt) {
    final String[] alleles = new String[rec.getAltCalls().size() + 1];
    final List<String> alts = rec.getAltCalls();
    alleles[0] = prevNt ? rec.getRefCall().substring(1) : rec.getRefCall();
    for (int i = 1; i < alleles.length; ++i) {
      final String allele = alts.get(i - 1);
      alleles[i] = prevNt ? allele.substring(1) : allele;
    }
    return alleles;
  }

  /**
   * Create a tabix index for a VCF file.
   * @param fileToIndex the VCF file
   * @throws IOException if there is a problem
   */
  public static void createVcfTabixIndex(File fileToIndex) throws IOException {
    try {
      new TabixIndexer(fileToIndex).saveVcfIndex();
    } catch (final UnindexableDataException e) {
      Diagnostic.warning("Cannot produce TABIX index for: " + fileToIndex + ": " + e.getMessage());
    }
  }

  /**
   * Create a new instance from the specified VCF file.
   * @param f a pointer to the VCF file
   * @return a new <code>ReferenceRegions</code> or null if the argument is null
   * @throws java.io.IOException when reading the file fails
   */
  public static ReferenceRegions regionsVcf(File f) throws IOException {
    if (f != null) {
      try (VcfReader reader = VcfReader.openVcfReader(f)) {
        return ReferenceRegions.regions(reader);
      }
    } else {
      return null;
    }
  }

  /**
   * @param zippedVcf zipped file to check
   * @return true if this looks like a <code>VCF</code> file.
   * @throws IOException if an IO Error occurs
   */
  public static boolean isVcfFormat(File zippedVcf) throws IOException {
    try (BufferedReader r = new BufferedReader(new InputStreamReader(FileUtils.createGzipInputStream(zippedVcf, false)))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.startsWith("##fileformat=VCF")) {
          return true;
        }
        if (!line.startsWith("##")) {
          return false;
        }
      }
      return false;
    }
  }

  /**
   * Add additional lines to the header
   * @param header the header
   * @param extraHeaderLines lines to add
   */
  public static void addHeaderLines(VcfHeader header, Collection<String> extraHeaderLines) {
    for (final String extraLine : extraHeaderLines) {
      try {
        header.addMetaInformationLine(extraLine);
      } catch (final VcfFormatException e) {
        throw new VcfFormatException("Additional header line '" + extraLine + "', " + e.getMessage()); // Add context information
      }
    }
  }

  /**
   * Generate the haplotype obtained by asserting the first ALT allele in a VCF record. This is intended
   * for small test examples rather than whole chromosomes.
   * @param rec the record containing the variant. Breakpoint and symbolic alleles are supported
   * @param refs a dictionary that must contain the full reference sequence for any sequences required by the variant.
   * @return the asserted haplotype
   */
  public static String replayAllele(VcfRecord rec, Map<String, String> refs) {
    return replayAllele(rec, 1, refs);
  }

  /**
   * Generate the haplotype obtained by a given allele in a VCF record. This is intended
   * for small test examples rather than whole chromosomes.
   * @param rec the record containing the variant. Breakpoint and symbolic alleles are supported
   * @param alleleId the index of the allele to replay (using GT allele numbering)
   * @param refs a dictionary that must contain the full reference sequence for any sequences required by the variant.
   * @return the asserted haplotype
   */
  public static String replayAllele(VcfRecord rec, int alleleId, Map<String, String> refs) {
    final String alt = rec.getAllele(alleleId);
    final VariantType sType = VariantType.getSymbolicAlleleType(alt);
    if (sType == null) { // Non-symbolic
      final String fulllocal = refs.get(rec.getSequenceName());
      return fulllocal.substring(0, rec.getStart()) + alt + fulllocal.substring(rec.getStart() + rec.getRefCall().length());
    } else {
      switch (sType) {
        case SV_BREAKEND:
          return replayBreakpoint(rec.getSequenceName(), rec.getStart(), rec.getRefCall(), new BreakpointAlt(rec.getAltCalls().get(0)), refs);
        case SV_SYMBOLIC:
          return replaySymbolic(rec, alt, refs);
        case SV_MISSING:
          throw new RuntimeException("Cannot replay spanning deletion allele: " + alt);
        default:
          throw new RuntimeException("Unrecognized symbolic allele type: " + sType);
      }
    }
  }

  private static String replaySymbolic(VcfRecord rec, String alt, Map<String, String> refs) {
    final String fulllocal = refs.get(rec.getSequenceName());
    final int symbolStart = alt.indexOf('<');
    if (symbolStart == -1) {
      throw new VcfFormatException("Invalid symbolic allele: " + alt);
    }
    final String fullremote = refs.get(alt.substring(symbolStart));
    final Integer endSymbolic = VcfUtils.getIntegerInfoFieldFromRecord(rec, INFO_END);
    final int end = endSymbolic != null ? endSymbolic - 1 : rec.getStart() + rec.getRefCall().length();
    return fulllocal.substring(0, rec.getStart()) + alt.substring(0, symbolStart) + fullremote + fulllocal.substring(end);
  }

  private static String replayBreakpoint(String localChr, int pos, String ref, BreakpointAlt alt, Map<String, String> refs) {
    final String fulllocal = refs.get(localChr);
    final String fullremote = refs.get(alt.getRemoteChr());
    final String localpart = alt.isLocalUp() ? fulllocal.substring(0, pos) : fulllocal.substring(pos + ref.length());
    final String remotepart = alt.isRemoteUp() ? fullremote.substring(0, alt.getRemotePos()) : fullremote.substring(alt.getRemotePos() + 1);
    return alt.isLocalUp()
      ? localpart + alt.getRefSubstitution() + (alt.isRemoteUp() ? DnaUtils.reverseComplement(remotepart) : remotepart)
      : (alt.isRemoteUp() ? remotepart : DnaUtils.reverseComplement(remotepart)) + alt.getRefSubstitution() + localpart;
  }

}
