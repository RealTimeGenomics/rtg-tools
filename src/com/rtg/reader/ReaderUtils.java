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
package com.rtg.reader;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRegions;

/**
 * Utility functions for manipulating files that are not provided in the File
 * class.
 *
 */
public final class ReaderUtils {

  private ReaderUtils() {
  }

  /**
   * Tells you if the directory passed in contains a paired end SDF left / right folder
   *
   * @param dir the input directory
   * @return true if the left / right folders exist within the given directory; otherwise false
   */
  public static boolean isPairedEndDirectory(final File dir) {
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
      return false;
    }
    final File lDir = new File(dir, "left");
    final File rDir = new File(dir, "right");
    return lDir.exists() && rDir.exists() && lDir.isDirectory() && rDir.isDirectory();
  }

  /**
   * Gets the left end of an existing paired end SDF
   *
   * @param dir the input directory
   * @return the left end SDF directory
   * @throws CorruptSdfException if one of the directories has been deleted since the check
   */
  public static File getLeftEnd(final File dir) throws CorruptSdfException {
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
      throw new CorruptSdfException(dir);
    }
    final File lDir = new File(dir, "left");
    if (lDir.exists() && lDir.isDirectory()) {
      return lDir;
    }
    throw new CorruptSdfException(dir);
  }

  /**
   * Gets the right end of an existing paired end SDF
   *
   * @param dir the input directory
   * @return the right end SDF directory
   * @throws CorruptSdfException if one of the directories has been deleted since the check
   */
  public static File getRightEnd(final File dir) throws CorruptSdfException {
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
      throw new CorruptSdfException(dir);
    }
    final File rDir = new File(dir, "right");
    if (rDir.exists() && rDir.isDirectory()) {
      return rDir;
    }
    throw new CorruptSdfException(dir);
  }

  /**
   * Checks if the provided reader is empty and <code>NoTalkbackSlimException</code>
   * if it is.
   * @param reader the reader to check.
   * @throws NoTalkbackSlimException if the given sequences reader is null or empty.
   */
  public static void validateNotEmpty(SequencesReader reader) {
    if (reader == null || reader.numberSequences() == 0) {
      final String dir;
      if (reader == null || reader.path() == null) {
        dir = "<Unknown>";
      } else {
        dir = reader.path().getAbsolutePath();
      }
      throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "The SDF \"" + dir + "\" was empty.");
    }
  }

  /**
   * Get the GUID from the supplied SDF directory, if the SDF contains one.
   * @param readDir input SDF directory, correctly handles paired end and single end SDF
   * @return GUID for current SDF, 0 if no GUID is specified in the SDF
   * @throws IOException if error reading
   */
  public static SdfId getSdfId(File readDir) throws IOException {
    final File reads = ReaderUtils.isPairedEndDirectory(readDir) ? ReaderUtils.getLeftEnd(readDir) : readDir;
    return new IndexFile(reads).getSdfId();
  }

  /**
   * Construct a mapping from sequence name to sequence id.
   * @param sequences the sequences reader to construct the map from
   * @return the map from sequence name to sequence id
   * @throws IOException if an error occurs during reading
   */
  public static Map<String, Long> getSequenceNameMap(final SequencesReader sequences) throws IOException {
    final Map<String, Long> map = new LinkedHashMap<>((int) sequences.numberSequences());
    for (long i = 0; i < sequences.numberSequences(); i++) {
      map.put(sequences.name(i), i);
    }
    return map;
  }

  /**
   * Construct a mapping from sequence name to sequence id.
   * @param names the sequences reader to construct the map from
   * @return the map from sequence name to sequence id
   * @throws IOException if an error occurs during reading
   */
  public static Map<String, Long> getSequenceNameMap(final PrereadNamesInterface names) throws IOException {
    final Map<String, Long> map = new LinkedHashMap<>((int) names.length());
    for (long i = 0; i < names.length(); i++) {
      map.put(names.name(i), i);
    }
    return map;
  }

  /**
   * Check if a file is an SDF by attempting to get an SDF id
   * @param f file that may be an SDF
   * @return true if the file represents a readable SDF which has an id
   */
  public static boolean isSDF(File f) {
    try {
      return !new SdfId(0).equals(getSdfId(f));
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Get all the sequence names and their lengths.
   * @param genome reader for SDF file.
   * @return the map of names to lengths.
   * @throws IOException if an error occurs during reading
   */
  public static Map<String, Integer> getSequenceLengthMap(final SequencesReader genome) throws IOException {
    final Map<String, Integer> names = new LinkedHashMap<>();
    final long numberSequences = genome.numberSequences();
    for (long l = 0; l < numberSequences; l++) {
      names.put(genome.name(l), genome.length(l));
    }
    return names;
  }

  /**
   * Make a reference regions corresponding to the full length of all reference sequences
   * @param sequencesReader sequences reader used to determine sequence names and lengths
   * @return the ReferenceRegions
   * @throws IOException if an I/O error occurs
   */
  public static ReferenceRegions fullReferenceRegions(SequencesReader sequencesReader) throws IOException {
    final ReferenceRegions regions = new ReferenceRegions();
    for (int k = 0; k < sequencesReader.numberSequences(); k++) {
      regions.add(sequencesReader.names().name(k), 0, sequencesReader.length(k));
    }
    return regions;
  }

  /**
   * Will throw <code>InvalidParamsException</code> if the regions defined are not in the given template.
   * Note: this method assumes that the sequences reader provided has already been checked that it contains names.
   * @param reader the template sequences that regions should be valid with respect to
   * @param regions the regions to check
   * @throws IOException if an IO error occurs
   */
  public static void validateRegions(SequencesReader reader, ReferenceRegions regions) throws IOException {
    final List<String> missingChromosomes = new ArrayList<>();
    final Map<String, Long> nameMap = getSequenceNameMap(reader);
    missingChromosomes.addAll(regions.sequenceNames().stream().filter(chr -> !nameMap.containsKey(chr)).collect(Collectors.toList()));
    if (missingChromosomes.size() > 0) {
      throw new InvalidParamsException("The following sequences specified in the regions list are not present in the template: " + StringUtils.join(", ", missingChromosomes));
    }
  }
}
