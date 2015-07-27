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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.annotation.AbstractDerivedAnnotation;
import com.rtg.vcf.annotation.AbstractDerivedFormatAnnotation;
import com.rtg.vcf.annotation.AlleleCountInGenotypesAnnotation;
import com.rtg.vcf.annotation.DerivedAnnotations;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 *
 *
 */
public final class VcfUtils {

  private VcfUtils() { }

  /** Character used to separate alleles in unphased genotypes */
  public static final char UNPHASED_SEPARATOR = '/';

  /** Character used to separate alleles in phased genotypes */
  public static final char PHASED_SEPARATOR = '|';

  /** VCF file suffix */
  public static final String VCF_SUFFIX = ".vcf";

  /** character indicating field value separator */
  public static final char VALUE_SEPARATOR = ';';

  /** character indicating field value is missing */
  public static final char MISSING_VALUE = '.';

  /** QUAL column label */
  public static final String QUAL = "QUAL";

  /** pass filter flag */
  public static final String FILTER_PASS = "PASS";

  /** missing field (e.g. filter/QUAL) */
  public static final String MISSING_FIELD = "" + MISSING_VALUE;

  /** confidence interval for POS columns */
  public static final String CONFIDENCE_INTERVAL_POS = "CIPOS";

  /** allele frequency for alt alleles */
  public static final String INFO_ALLELE_FREQ = "AF";

  /** allele frequency for alt alleles */
  public static final MetaType INFO_ALLELE_FREQ_TYPE = MetaType.FLOAT;

  /** allele frequency for alt alleles */
  public static final VcfNumber INFO_ALLELE_FREQ_NUM = new VcfNumber("A");

  /** allele frequency for alt alleles */
  public static final String INFO_ALLELE_FREQ_DESC = "Allele Frequency";

  /** combined depth field */
  public static final String INFO_COMBINED_DEPTH = "DP";

  /** genotype format field */
  public static final String FORMAT_GENOTYPE = "GT";

  /** AVR score format field */
  public static final String FORMAT_AVR = "AVR";

  /** genotype quality format field */
  public static final String FORMAT_GENOTYPE_QUALITY = "GQ";

  /** sample depth field */
  public static final String FORMAT_SAMPLE_DEPTH = "DP";

  /** ambiguity ratio field */
  public static final String FORMAT_AMBIGUITY_RATIO = "AR";

  /** VCF FORMAT field used to indicate de novo alleles */
  public static final String FORMAT_DENOVO = "DN";

  /** VCF FORMAT field used to indicate de novo score */
  public static final String FORMAT_DENOVO_SCORE = "DNP";

  private static int alleleId(char c) {
    if (c == VcfUtils.MISSING_VALUE) {
      return -1;
    } else if (c >= '0' && c <= '9') {
      return c - '0';
    } else {
      throw new NumberFormatException("Illegal character in genotype: " + c);
    }
  }

  private static int alleleId(String gt, int start, int length) {
    return (length == 1)
      ? alleleId(gt.charAt(start))
      : Integer.parseInt(gt.substring(start, start + length));
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
    final int gtlen = gt.length();
    if (gtlen == 1) { // Typical haploid call
      return new int[] {alleleId(gt.charAt(0)) };
    } else {
      int[] result = new int[2]; // Initialize assuming the most common case, diploid, and resize if needed
      int ploid = 0;
      int allelestart = 0;
      for (int i = 0; i < gtlen; i++) {
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
        throw new NumberFormatException("Malformed GT '" + gt + "'"); // NFE is for consistency with the parseInt stuff
      }
      return result;
    }
  }

  /**
   * @param f file
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
    final String name = file.getName().toLowerCase(Locale.getDefault());
    final File vcfFile = name.endsWith(VCF_SUFFIX) || gzip && name.endsWith(VCF_SUFFIX + FileUtils.GZ_SUFFIX) ? file : new File(file.getPath() + VCF_SUFFIX);
    return FileUtils.getZippedFileName(gzip, vcfFile);
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
   * If GT is either missing or is same as reference then return true
   * @param gt gt to check
   * @return true if gt does not represent a variant, false otherwise
   */
  public static boolean isNonVariantGt(String gt) {
    final int gtlen = gt.length();
    for (int i = 0; i < gtlen; i++) {
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
   * If GT is not missing and not the same as reference then return true
   * @param gt gt to check
   * @return true if gt represents a variant, false otherwise
   */
  public static boolean isVariantGt(String gt) {
    return !isNonVariantGt(gt);
  }

  /**
   * If any of the GT is non missing return true
   * @param gt gt to check
   * @return true if gt is not entirely should be skipped, false otherwise
   */
  public static boolean isNonMissingGt(String gt) {
    final int gtlen = gt.length();
    for (int i = 0; i < gtlen; i++) {
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
   * If all sub-alleles of the GT is missing return true
   * @param gt gt to check
   * @return true if gt should be skipped, false otherwise
   */
  public static boolean isMissingGt(String gt) {
    return !isNonMissingGt(gt);
  }

  /**
   * Returns the VCF header for given file
   * @param input file to read VCF header from
   * @return The VcfHeader
   * @throws IOException if error
   */
  public static VcfHeader getHeader(final File input) throws IOException {
    try (VcfReader reader = VcfReader.openVcfReader(input)) {
      return reader.getHeader();
    }
  }


  // The following could all be moved to VcfRecord itself

  /**
   * @param rec record to look up
   * @return true if the record was produced by the complex caller
   */
  public static boolean isComplexScored(VcfRecord rec) {
    return rec.getInfo().keySet().contains("XRX");
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
   * @param rec VCF record
   * @param sample index of sample to extract value from
   * @param field string ID of field to extract
   * @return the value converted into a double, or {@code Double.NaN} if missing
   */
  public static double getDoubleFormatFieldFromRecord(VcfRecord rec, int sample, String field) {
    final Map<String, ArrayList<String>> formatPerSample = rec.getFormatAndSample();
    if (formatPerSample.containsKey(field)) {
      final String fieldVal = formatPerSample.get(field).get(sample);
      try {
        if (fieldVal.equals(VcfRecord.MISSING)) {
          return Double.NaN;
        }
        return Double.parseDouble(fieldVal);
      } catch (NumberFormatException ex) {
        throw new NoTalkbackSlimException("Invalid numeric value \"" + fieldVal + "\" in \"" + field + "\" for VCF record :" + rec);
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * If field contains multiple values will get a most the first value.
   * @param rec VCF record
   * @param field string ID of field to extract
   * @return the value converted into a double, or {@code Double.NaN} if missing
   */
  public static double getDoubleInfoFieldFromRecord(VcfRecord rec, String field) {
    final Map<String, ArrayList<String>> infoField = rec.getInfo();
    if (infoField.containsKey(field)) {
      final String fieldVal = infoField.get(field).get(0); // TODO how to support multiple values
      try {
        if (fieldVal.equals(VcfRecord.MISSING)) {
          return Double.NaN;
        }
        return Double.parseDouble(fieldVal);
      } catch (NumberFormatException ex) {
        throw new NoTalkbackSlimException("Invalid numeric value \"" + fieldVal + "\" in \"" + field + "\" for VCF record :" + rec);
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * Tests if the given sample genotype in a record is homozygous or not
   * @param rec record to check
   * @param sample sample number
   * @return true iff homozygous
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isSnp(VcfRecord rec, int sample) {
    final int[] gtArray = validateGT(rec, sample);
    return isHomozygous(gtArray) && !isIdentity(gtArray);
  }

  /**
   * Tests if the given sample genotype in a record is homozygous or not
   * @param rec record to check
   * @param sample sample number
   * @return true iff homozygous
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isHomozygous(VcfRecord rec, int sample) {
    final int[] gtArray = validateGT(rec, sample);
    return isHomozygous(gtArray) && !isIdentity(gtArray);
  }

  /**
   * Tests if the given sample genotype in a record is heterozygous or not
   * @param rec record to check
   * @param sample sample number
   * @return true iff heterozygous
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isHeterozygous(VcfRecord rec, int sample) {
    final int[] gtArray = validateGT(rec, sample);
    return isHeterozygous(gtArray);
  }

  /**
   * Tests if the given sample genotype in a record is haploid or not
   * @param rec record to check
   * @param sample sample number
   * @return true iff haploid
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isHaploid(VcfRecord rec, int sample) {
    final int[] gtArray = validateGT(rec, sample);
    return gtArray.length == 1;
  }

  /**
   * Tests if the given sample genotype in a record is diploid or not
   * @param rec record to check
   * @param sample sample number
   * @return true iff diploid
   * @throws NoTalkbackSlimException if the sample is missing GT filed or invalid sample number
   */
  public static boolean isDiploid(VcfRecord rec, int sample) {
    final int[] gtArray = validateGT(rec, sample);
    return gtArray.length == 2;
  }

  private static int[] validateGT(VcfRecord rec, int sample) {
    final ArrayList<String> gtList = rec.getFormatAndSample().get(FORMAT_GENOTYPE);
    if (gtList == null) {
      throw new NoTalkbackSlimException("VCF record does not contain GT field, record: " + rec.toString());
    }
    if (sample > gtList.size()) {
      throw new NoTalkbackSlimException("Invalid sample number " + sample + ", record: " + rec.toString());
    }
    final String gt = gtList.get(sample);
    return splitGt(gt);
  }

  private static boolean isHomozygous(int[] gtArray) {
    return gtArray.length == 1 || gtArray[0] == gtArray[1];
  }

  private static boolean isHeterozygous(int[] gtArray) {
    return gtArray.length != 1 && gtArray[0] != gtArray[1];
  }

  private static boolean isIdentity(int[] gt) {
    for (int g : gt) {
      if (g != 0) {
        return false;
      }
    }
    return true;
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
      throw new IllegalArgumentException("Record did not contain enough samples: " + rec.toString());
    }
    if (!rec.getFormatAndSample().containsKey(FORMAT_GENOTYPE)) {
      return false;
    }
    final String gt = rec.getFormatAndSample().get(FORMAT_GENOTYPE).get(sampleId);
    final VariantType type = VariantType.getType(rec, splitGt(gt));
    return !type.isNonVariant() && !type.isSvType();
  }

  /**
   * Get an appropriate VCF annotator for this derived annotation.
   * @param derivedAnnotation the derived annotation to get an annotator for.
   * @return the VCF annotator for this derived annotation.
   */
  public static VcfAnnotator getAnnotator(DerivedAnnotations derivedAnnotation) {
    final AbstractDerivedAnnotation annotation = derivedAnnotation.getAnnotation();
    if (annotation instanceof AlleleCountInGenotypesAnnotation) {
      return new VcfInfoPerAltIntegerAnnotator(annotation);
    } else if (annotation instanceof AbstractDerivedFormatAnnotation) {
      final AbstractDerivedFormatAnnotation formatAnnotation = (AbstractDerivedFormatAnnotation) annotation;
      switch (formatAnnotation.getType()) {
        case DOUBLE:
          return new VcfFormatDoubleAnnotator(formatAnnotation);
        case INTEGER:
          return new VcfFormatIntegerAnnotator(formatAnnotation);
        case STRING:
          return new VcfFormatStringAnnotator(formatAnnotation);
        default:
          throw new IllegalArgumentException("Format annotation of type " + formatAnnotation.getType() + " not currently supported");
      }
    } else {
      switch (annotation.getType()) {
        case DOUBLE:
          return new VcfInfoDoubleAnnotator(annotation);
        case INTEGER:
          return new VcfInfoIntegerAnnotator(annotation);
        default:
          throw new IllegalArgumentException("Info annotation of type " + annotation.getType() + " not currently supported");
      }
    }
  }

  /**
   * Determines if this record has a redundant leading base (e.g. for indels)
   * @param rec the record to examine
   * @return true if the leading base of the variant is redundant.
   */
  public static boolean hasRedundantFirstNucleotide(final VcfRecord rec) {
    // Can only strip previous nucleotide if all alleles have the same first char
    final Character c = rec.getRefCall().charAt(0);
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
    for (int i = 1; i < alleles.length; i++) {
      final String allele = alts.get(i - 1);
      alleles[i] = prevNt ? allele.substring(1) : allele;
    }
    return alleles;
  }

  /**
   * Create a tabix index for a VCF file
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
   * Create a new instance from the specified VCF file
   * @param f a pointer to the VCF file
   * @return a new <code>ReferenceRegions</code> or null if the argument is null
   * @throws java.io.IOException when reading the file fails
   */
  public static ReferenceRegions regionsVcf(File f) throws IOException {
    if (f != null) {
      try (VcfReader reader = VcfReader.openVcfReader(f)) {
        return regions(reader);
      }
    } else {
      return null;
    }
  }

  /**
   * Create a new instance from the specified VCF file
   * @param reader the VCF reader
   * @return a new <code>ReferenceRegions</code>
   * @throws java.io.IOException when reading the file fails
   */
  public static ReferenceRegions regions(VcfReader reader) throws IOException {
    final ReferenceRegions regions = new ReferenceRegions();
    while (reader.hasNext()) {
      regions.add(reader.next());
    }
    return regions;
  }
}
