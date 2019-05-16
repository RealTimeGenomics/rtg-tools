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

package com.rtg.vcf.header;

import static com.rtg.util.StringUtils.TAB;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rtg.reader.SdfId;
import com.rtg.reader.SequencesReader;
import com.rtg.util.Environment;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.VcfFormatException;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

/**
 * Class holding VCF header lines and sample names.
 */
public class VcfHeader {

  /** Current VCF version */
  public static final String VERSION = "4.1";
  /** Current VCF version */
  public static final String VERSION_VALUE = "VCFv" + VERSION;
  /** Comment character for VCF files */
  public static final char COMMENT_CHAR = '#';
  /** String used to indicate meta lines */
  public static final String META_STRING = "##";
  /** Start of reference sequence lines*/
  public static final String CONTIG_STRING = META_STRING + "contig";
  /** Start of alt lines*/
  public static final String ALT_STRING = META_STRING + "ALT";
  /** Start of info lines*/
  public static final String INFO_STRING = META_STRING + "INFO";
  /** Start of filter lines */
  public static final String FILTER_STRING = META_STRING + "FILTER";
  /** Start of format lines */
  public static final String FORMAT_STRING = META_STRING + "FORMAT";
  /** Start of sample lines */
  public static final String SAMPLE_STRING = META_STRING + "SAMPLE";
  /** Start of pedigree lines */
  public static final String PEDIGREE_STRING = META_STRING + "PEDIGREE";
  /** File format line prefix */
  public static final String VERSION_LINE_PREFIX = META_STRING + "fileformat";
  /** Full version string */
  public static final String VERSION_LINE = VERSION_LINE_PREFIX + "=" + VERSION_VALUE;

  /** Header line for VCF files */
  public static final String HEADER_BASE = "" + COMMENT_CHAR
      + "CHROM" + TAB
      + "POS" + TAB
      + "ID" + TAB
      + "REF" + TAB
      + "ALT" + TAB
      + "QUAL" + TAB
      + "FILTER" + TAB
      + "INFO";

  /** Header portion for format column */
  public static final String FORMAT_HEADER_STRING = "FORMAT";
  /** Header line for VCF files with samples */
  public static final String HEADER_LINE = HEADER_BASE + TAB + FORMAT_HEADER_STRING;
  /** Minimal string that can be used as a VCF header */
  public static final String MINIMAL_HEADER = VERSION_LINE + '\n' + HEADER_LINE;

  private static final String[] HEADER_COLUMNS = {"#CHROM", "POS", "ID", "REF", "ALT", "QUAL", "FILTER", "INFO"};

  private String mVersionLine;
  private final List<String> mGenericMetaInformationLines;
  private final List<ContigField> mContigLines;
  private final List<AltField> mAltLines;
  private final List<FilterField> mFilterLines;
  private final List<InfoField> mInfoLines;
  private final List<FormatField> mFormatLines;
  private final List<SampleField> mSampleLines;
  private final List<PedigreeField> mPedigreeLines;
  private final List<String> mSampleNames;
  private final HashMap<String, Integer> mNameToColumn;

  /** Create a new VCF header */
  public VcfHeader() {
    mGenericMetaInformationLines = new ArrayList<>();
    mSampleNames = new ArrayList<>();
    mContigLines = new ArrayList<>();
    mAltLines = new ArrayList<>();
    mFilterLines = new ArrayList<>();
    mInfoLines = new ArrayList<>();
    mFormatLines = new ArrayList<>();
    mSampleLines = new ArrayList<>();
    mPedigreeLines = new ArrayList<>();
    mNameToColumn = new HashMap<>();
  }

  /**
   * Get the SDF identifier from the header.
   * @return SDF identifier
   */
  public SdfId getSdfId() {
    for (final String s : getGenericMetaInformationLines()) {
      if (s.startsWith("##TEMPLATE-SDF-ID=")) {
        final String[] split = s.split("=");
        if (split.length != 2) {
          throw new NoTalkbackSlimException("Invalid VCF template SDF ID header line : " + s);
        }
        final SdfId sdfId;
        try {
          sdfId = new SdfId(split[1]);
        } catch (final NumberFormatException ex) {
          throw new NoTalkbackSlimException("Invalid VCF template SDF ID header line : " + s);
        }
        return sdfId;
      }
    }
    return new SdfId(0);
  }

  /**
   * Create a copy of this header
   * @return the copy
   */
  public VcfHeader copy() {
    final VcfHeader copy = new VcfHeader();
    copy.mVersionLine = mVersionLine;
    copy.mGenericMetaInformationLines.addAll(mGenericMetaInformationLines);
    copy.mSampleNames.addAll(mSampleNames);
    copy.mContigLines.addAll(mContigLines);
    copy.mAltLines.addAll(mAltLines);
    copy.mFilterLines.addAll(mFilterLines);
    copy.mInfoLines.addAll(mInfoLines);
    copy.mFormatLines.addAll(mFormatLines);
    copy.mSampleLines.addAll(mSampleLines);
    copy.mPedigreeLines.addAll(mPedigreeLines);
    copy.mNameToColumn.putAll(mNameToColumn);
    return copy;
  }

  /**
   * Add the common header fields used in typical new VCF files
   */
  public void addCommonHeader() {
    addMetaInformationLine(VERSION_LINE);
    final Calendar cal = Calendar.getInstance();
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    addMetaInformationLine(META_STRING + "fileDate=" + sdf.format(cal.getTime()));
    addMetaInformationLine(META_STRING + "source=" + Environment.getVersion());
    addRunInfo();
  }

  /**
   * Add header fields giving command line args, for appending within an an existing VCF header
   */
  public void addRunInfo() {
    if (CommandLine.getCommandLine() != null) {
      addMetaInformationLine(META_STRING + "CL=" + CommandLine.getCommandLine());
    }
  }

  /**
   * Add a reference line corresponding to the supplied reader
   * @param reference the reference SequencesReader
   */
  public void addReference(SequencesReader reference) {
    final SdfId sdfId = reference.getSdfId();
    if (sdfId != null && sdfId.available()) {
      addMetaInformationLine(VcfHeader.META_STRING + "TEMPLATE-SDF-ID=" + sdfId);
    }
    addMetaInformationLine(VcfHeader.META_STRING + "reference=" + reference.path());
  }

  /**
   * Add contig lines corresponding to the sequences present in a SAM header.
   * @param header the SAM header.
   */
  public void addContigFields(SAMFileHeader header) {
    final SAMSequenceDictionary dic =  header.getSequenceDictionary();
    for (final SAMSequenceRecord seq : dic.getSequences()) {
      final ContigField f = new ContigField(seq.getSequenceName(), seq.getSequenceLength());
      if (seq.getAttribute(SAMSequenceRecord.ASSEMBLY_TAG) != null) {
        f.put("as", seq.getAttribute(SAMSequenceRecord.ASSEMBLY_TAG));
      }
      if (seq.getAttribute(SAMSequenceRecord.MD5_TAG) != null) {
        f.put("md5", seq.getAttribute(SAMSequenceRecord.MD5_TAG));
      }
      if (seq.getAttribute(SAMSequenceRecord.SPECIES_TAG) != null) {
        f.put("species", seq.getAttribute(SAMSequenceRecord.SPECIES_TAG));
      }
      addContigField(f);
    }
  }

  /**
   * Add contig lines corresponding to those present in a VCF header.
   * @param header the SAM header.
   */
  public void addContigFields(VcfHeader header) {
    for (ContigField c : header.getContigLines()) {
      addContigField(c);
    }
  }

  /**
   * Add contig lines corresponding to the sequences present in a sequences reader.
   * @param reader the sequences reader.
   * @throws IOException if an IO error occurs whilst reading
   */
  public void addContigFields(SequencesReader reader) throws IOException {
    for (long i = 0; i < reader.numberSequences(); ++i) {
      addContigField(new ContigField(reader.name(i), reader.length(i)));
    }
  }

  private <T extends IdField<T>> T findIdField(List<T> src, String id) {
    return src.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
  }

  private <T extends TypedField<T>> void ensureContainsTypedField(List<T> dest, T field) {
    final T f = findIdField(dest, field.getId());
    if (f != null) {
      if (f.getType() != field.getType() || !f.getNumber().equals(field.getNumber())) {
        throw new VcfFormatException("A VCF " + f.fieldName() + " field " + field.getId() + " which is incompatible is already present in the VCF header.");
      }
      return; // Field already present
    }
    dest.add(field);
  }

  private <T extends IdField<T>> void ensureContainsIdField(List<T> dest, T field) {
    final T f = findIdField(dest, field.getId());
    if (f != null) {
      return; // Field already present
    }
    dest.add(field);
  }

  private <T extends IdField<T>> void addIdField(List<T> dest, T field) {
    final T f = findIdField(dest, field.getId());
    if (f != null) {
      if (!f.equals(field)) {
        throw new VcfFormatException("VCF header contains multiple " + f.fieldName() + " field declarations with the same ID=" + field.getId() + StringUtils.LS
          + f.toString() + StringUtils.LS
          + field.toString());
      }
      return; // Equivalent field already present
    }
    dest.add(field);
  }

  /**
   * Add an alt field
   * @param field the new alt field
   */
  public void addContigField(ContigField field) {
    addIdField(mContigLines, field);
  }

  /**
   * Add an alt field
   * @param field the new alt field
   */
  public void addAltField(AltField field) {
    addIdField(mAltLines, field);
  }

  /**
   * Add a filter field
   * @param id the filter field identifier
   * @param description the field description
   */
  public void addFilterField(String id, String description) {
    addFilterField(new FilterField(id, description));
  }

  /**
   * Add a filter field
   * @param field the filter field
   */
  public void addFilterField(FilterField field) {
    addIdField(mFilterLines, field);
  }

  /**
   * Add an info field
   * @param id the info field identifier
   * @param type the type of value
   * @param number the specifier for the number of occurrences
   * @param description the field description
   */
  public void addInfoField(String id, MetaType type, VcfNumber number, String description) {
    addInfoField(new InfoField(id, type, number, description));
  }

  /**
   * Add an info field
   * @param field the new info field
   */
  public void addInfoField(InfoField field) {
    addIdField(mInfoLines, field);
  }

  /**
   * Add a format field
   * @param id the format field identifier
   * @param type the type of value
   * @param number the specifier for the number of occurrences
   * @param description the field description
   */
  public void addFormatField(String id, MetaType type, VcfNumber number, String description) {
    addFormatField(new FormatField(id, type, number, description));
  }

  /**
   * Add a format field
   * @param field the new format field
   */
  public void addFormatField(FormatField field) {
    addIdField(mFormatLines, field);
  }


  /**
   * Add a format field
   * @param field the new format field
   */
  public void addSampleField(SampleField field) {
    addIdField(mSampleLines, field);
  }

  /**
   * Add a pedigree field
   * @param field the new pedigree field
   */
  public void addPedigreeField(PedigreeField field) {
    if (!mPedigreeLines.contains(field)) {
      mPedigreeLines.add(field);
    }
  }

  /**
   * Ensure that the header contains the specified ALT field (or one that is compatible)
   * @param field the new ALT field
   */
  public void ensureContains(AltField field) {
    ensureContainsIdField(mAltLines, field);
  }

  /**
   * Ensure that the header contains the specified info field (or one that is compatible)
   * @param field the new format field
   */
  public void ensureContains(FilterField field) {
    ensureContainsIdField(mFilterLines, field);
  }

  /**
   * Ensure that the header contains the specified info field (or one that is compatible)
   * @param field the new format field
   */
  public void ensureContains(InfoField field) {
    ensureContainsTypedField(mInfoLines, field);
  }

  /**
   * Ensure that the header contains the specified format field (or one that is compatible)
   * @param field the new format field
   */
  public void ensureContains(FormatField field) {
    ensureContainsTypedField(mFormatLines, field);
  }

  /**
   * @return get the meta line containing file format version
   */
  public String getVersionLine() {
    return mVersionLine;
  }

  /**
   * @param ver set the file format version value
   */
  public void setVersionValue(String ver) {
    mVersionLine = VERSION_LINE_PREFIX + "=" + ver;
  }

  /**
   * @return get just the value of the file format version line
   */
  public String getVersionValue() {
    if (mVersionLine == null) {
      return null;
    }
    final String[] split = mVersionLine.split("=", 2);
    if (split.length < 2) {
      throw new VcfFormatException("VCF version line does not contain a value");
    }
    return split[1];
  }

  /**
   * @return meta information lines
   */
  public List<String> getGenericMetaInformationLines() {
    return mGenericMetaInformationLines;
  }
  public List<ContigField> getContigLines() {
    return mContigLines;
  }
  public List<AltField> getAltLines() {
    return mAltLines;
  }
  public List<FilterField> getFilterLines() {
    return mFilterLines;
  }
  public List<InfoField> getInfoLines() {
    return mInfoLines;
  }
  public List<FormatField> getFormatLines() {
    return mFormatLines;
  }
  public List<SampleField> getSampleLines() {
    return mSampleLines;
  }
  public List<PedigreeField> getPedigreeLines() {
    return mPedigreeLines;
  }

  /**
   * Gets the FilterField corresponding to an ID
   * @param id the ID to retrieve
   * @return the corresponding filter field, or null if no field with that ID exists
   */
  public FilterField getFilterField(String id) {
    return findIdField(mFilterLines, id);
  }

  /**
   * Gets the InfoField corresponding to an ID
   * @param id the ID to retrieve
   * @return the corresponding info field, or null if no field with that ID exists
   */
  public InfoField getInfoField(String id) {
    return findIdField(mInfoLines, id);
  }

  /**
   * Gets the FormatField corresponding to an ID
   * @param id the ID to retrieve
   * @return the corresponding format field, or null if no field with that ID exists
   */
  public FormatField getFormatField(String id) {
    return findIdField(mFormatLines, id);
  }

  /**
   * Parse and add a header meta information line.
   * @param line meta information line
   * @return this, for call chaining
   */
  public VcfHeader addMetaInformationLine(String line) {
    if (isMetaLine(line)) {
      if (isContigLine(line)) {
        addContigField(parseContigLine(line));
      } else if (isAltLine(line)) {
        ensureContains(parseAltLine(line));
      } else if (isFilterLine(line)) {
        ensureContains(parseFilterLine(line));
      } else if (isInfoLine(line)) {
        ensureContains(parseInfoLine(line));
      } else if (isFormatLine(line)) {
        ensureContains(parseFormatLine(line));
      } else if (isSampleLine(line)) {
        addSampleField(parseSampleLine(line));
      } else if (isPedigreeLine(line)) {
        addPedigreeField(parsePedigreeLine(line));
      } else {
        if (isVersionLine(line)) {
          if (mVersionLine != null) {
            throw new VcfFormatException("More than one VCF version line found");
          }
          mVersionLine = line;
        } else {
          mGenericMetaInformationLines.add(line);
        }
      }
    } else {
      throw new VcfFormatException("Not a VCF meta information line");
    }
    return this;
  }

  /**
   * Add sample names contained in given header line
   * @param line column header line
   * @return this, for chain calling
   * @throws IllegalArgumentException if header is not correctly formed
   */
  public VcfHeader addColumnHeaderLine(String line) {
    final String[] split = line.split("\t");
    if (split.length < 8) {
      throw new VcfFormatException("VCF column header line missing required columns");
    }
    if (split.length == 9) {
     throw new VcfFormatException("VCF column header line contains format column but no sample columns");
    }
    for (int i = 0; i < 8; ++i) {
      if (!split[i].equals(HEADER_COLUMNS[i])) {
        throw new VcfFormatException("Incorrect VCF header column " + (i + 1) + " expected \"" + HEADER_COLUMNS[i] + "\" was \"" + split[i] + "\"");
      }
    }
    if (split.length > 9) {
      for (int i = 9; i < split.length; ++i) {
        addSampleName(split[i]);
      }
    }
    return this;
  }

  /**
   * Convert <code>contig</code> line into <code>ContigField</code> object
   * @param line the line
   * @return the object
   */
  public static ContigField parseContigLine(String line) {
    return new ContigField(line);
  }
  /**
   * Convert info line into <code>InfoField</code> object
   * @param line the line
   * @return the object
   */
  public static InfoField parseInfoLine(String line) {
    return new InfoField(line);
  }
  /**
   * Convert alt line into <code>AltField</code> object
   * @param line the line
   * @return the object
   */
  public static AltField parseAltLine(String line) {
    return new AltField(line);
  }
  /**
   * Convert filter line into <code>FilterField</code> object
   * @param line the line
   * @return the object
   */
  public static FilterField parseFilterLine(String line) {
    return new FilterField(line);
  }
  /**
   * Convert format line into <code>FormatField</code> object
   * @param line the line
   * @return the object
   */
  public static FormatField parseFormatLine(String line) {
    return new FormatField(line);
  }
  /**
   * Convert format line into <code>SampleField</code> object
   * @param line the line
   * @return the object
   */
  public static SampleField parseSampleLine(String line) {
    return new SampleField(line);
  }
  /**
   * Convert format line into <code>PedigreeField</code> object
   * @param line the line
   * @return the object
   */
  public static PedigreeField parsePedigreeLine(String line) {
    return new PedigreeField(line);
  }

  /**
   * @param line line of <code>VCF</code> file
   * @return true if it is a file format version meta line
   */
  public static boolean isVersionLine(String line) {
    return line.startsWith(VERSION_LINE_PREFIX + "=");
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is meta information
   */
  public static boolean isMetaLine(String line) {
    return line.startsWith(META_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is reference sequence line
   */
  public static boolean isContigLine(String line) {
    return line.startsWith(CONTIG_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is information line
   */
  public static boolean isInfoLine(String line) {
    return line.startsWith(INFO_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is alt line
   */
  public static boolean isAltLine(String line) {
    return line.startsWith(ALT_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is filter line
   */
  public static boolean isFilterLine(String line) {
    return line.startsWith(FILTER_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is format line
   */
  public static boolean isFormatLine(String line) {
    return line.startsWith(FORMAT_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is sample line
   */
  public static boolean isSampleLine(String line) {
    return line.startsWith(SAMPLE_STRING);
  }
  /**
   * @param line line of <code>VCF</code> file
   * @return if line is pedigree line
   */
  public static boolean isPedigreeLine(String line) {
    return line.startsWith(PEDIGREE_STRING);
  }

  /**
   * Add sample name
   * @param name name to be added
   * @return this, for call chaining
   */
  public VcfHeader addSampleName(String name) {
    if (mSampleNames.contains(name)) {
      throw new VcfFormatException("Duplicate sample name \"" + name + "\" in VCF header");
    }
    mNameToColumn.put(name, mSampleNames.size());
    mSampleNames.add(name);
    return this;
  }

  /**
   * Get the column index of the specified sample
   * @param name the sample name
   * @return the index of the specified sample, or null if the sample is unknown
   */
  public Integer getSampleIndex(String name) {
    return mNameToColumn.get(name);
  }

  /**
   * Remove sample names from this record
   * @param names hash set of names to remove
   */
  public void removeSamples(HashSet<String> names) {
    mSampleNames.removeIf(names::contains);
    mSampleLines.removeIf(sample -> names.contains(sample.getId()));
    //now reindex the mNameToColumn thing.
    mNameToColumn.clear();
    for (int i = 0; i < mSampleNames.size(); ++i) {
      mNameToColumn.put(mSampleNames.get(i), i);
    }
  }

  /**
   * Remove all samples from this header.
   */
  public void removeAllSamples() {
    mNameToColumn.clear();
    mSampleLines.clear();
    mSampleNames.clear();
  }

  /**
   * @return sample names
   */
  public List<String> getSampleNames() {
    return mSampleNames;
  }

  /**
   * @return number of samples in current header
   */
  public int getNumberOfSamples() {
    return mSampleNames.size();
  }

  /**
   * Rename a sample and any other associated meta information.  If there is no sample with <code>originalName</code> then no action is taken.
   * @param originalName original sample name
   * @param newName new sample name
   */
  public void relabelSample(final String originalName, final String newName) {
    final Integer index = getSampleIndex(originalName);
    if (index != null) {
      mSampleNames.set(index, newName);
      mNameToColumn.remove(originalName);
      mNameToColumn.put(newName, index);
      for (int k = 0; k < mSampleLines.size(); ++k) {
        final SampleField sample = mSampleLines.get(k);
        if (sample.getId().equals(originalName)) {
          mSampleLines.set(k, new SampleField(sample.toString().replaceFirst(originalName, newName)));
          break;
        }
      }
      for (final PedigreeField pedigree : mPedigreeLines) {
        pedigree.relabelSample(originalName, newName);
      }
    }
  }

  /**
   * @return header string
   */
  public String getColumnHeader() {
    final StringBuilder sb;
    if (getNumberOfSamples() > 0) {
      sb = new StringBuilder(HEADER_LINE);
      for (int i = 0; i < getNumberOfSamples(); ++i) {
        sb.append(TAB).append(mSampleNames.get(i));
      }
    } else {
      sb = new StringBuilder(HEADER_BASE);
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (mVersionLine != null) {
      sb.append(mVersionLine).append('\n');
    }
    for (String line : getGenericMetaInformationLines()) {
      sb.append(line).append('\n');
    }
    for (ContigField mContigLine : mContigLines) {
      sb.append(mContigLine).append('\n');
    }
    for (AltField mAltLine : mAltLines) {
      sb.append(mAltLine).append('\n');
    }
    for (FilterField mFilterLine : mFilterLines) {
      sb.append(mFilterLine).append('\n');
    }
    for (InfoField mInfoLine : mInfoLines) {
      sb.append(mInfoLine).append('\n');
    }
    for (FormatField mFormatLine : mFormatLines) {
      sb.append(mFormatLine).append('\n');
    }
    for (SampleField mSampleLine : mSampleLines) {
      sb.append(mSampleLine).append('\n');
    }
    for (PedigreeField mPedigreeLine : mPedigreeLines) {
      sb.append(mPedigreeLine).append('\n');
    }
    sb.append(getColumnHeader()).append("\n");
    return sb.toString();
  }

  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^=]+)=([^,\"]*)\\s*,?");
  private static final Pattern KEY_VALUE_WITH_QUOTES_PATTERN = Pattern.compile("^([^=]+)=\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"\\s*,?");

  static LinkedHashMap<String, String> parseMetaLine(String line, Pattern linePattern) {
    final Matcher m = linePattern.matcher(line.trim());
    if (!m.matches()) {
      throw new VcfFormatException("Could not parse VCF header line");
    }
    final LinkedHashMap<String, String> ret = new LinkedHashMap<>(4);
    //        V----------------------------------V
    //##INFO=<ID=a,Number=b,Type=c,Description="d">
    String inner = m.group(1).trim();
    while (inner.length() > 0) {
      Matcher keyValMatcher = KEY_VALUE_WITH_QUOTES_PATTERN.matcher(inner);
      if (keyValMatcher.find()) {
        final String key = keyValMatcher.group(1).trim();
        String val = keyValMatcher.group(2);
        if (val != null) {
          val = StringUtils.dumbUnQuote(val.trim());
        }
        ret.put(key, val);
      } else {
        keyValMatcher = KEY_VALUE_PATTERN.matcher(inner);
        if (keyValMatcher.find()) {
          final String key = keyValMatcher.group(1).trim();
          ret.put(key, keyValMatcher.group(2).trim());
        } else {
          throw new VcfFormatException("Could not parse VCF header line");
        }
      }
      inner = inner.substring(keyValMatcher.end());
    }
    return ret;
  }

  static void checkRequiredMetaKeys(Map<String, String> provided, String... required) {
    for (String key : required) {
      if (!provided.containsKey(key)) {
        throw new VcfFormatException("VCF header missing " + key + " declaration");
      }
    }
  }
}
