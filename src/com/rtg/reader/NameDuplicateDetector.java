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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import com.rtg.util.StringUtils;
import com.rtg.util.array.IndexSorter;
import com.rtg.util.array.intindex.IntCreate;
import com.rtg.util.array.intindex.IntIndex;
import com.rtg.util.array.longindex.LongCreate;
import com.rtg.util.array.longindex.LongIndex;

/**
 */
public class NameDuplicateDetector {

  private static class NullDetector extends NameDuplicateDetector {

    public NullDetector() {
      super(0);
    }


    @Override
    public void addPair(String name, int seqId, int readerIndex) {
    }

    @Override
    public boolean checkSequenceDuplicates(SequencesReader[] readers, File duplicatesOutputFile) {
      return false;
    }

  }

  static NameDuplicateDetector getNullDetector() {
    return new NullDetector();
  }

  final IntIndex mHashes;
  final LongIndex mIndexIds;
  int mCount = 0;

  /**
   * Constructor
   * @param size the number of sequences to detect duplicate names in
   */
  public NameDuplicateDetector(long size) {
    mHashes = IntCreate.createIndex(size);
    mIndexIds = LongCreate.createIndex(size);
  }

  /**
   * Function to add the next pair object
   * @param name sequence name
   * @param seqId sequence id
   * @param readerIndex array index of reader
   */
  public void addPair(String name, int seqId, int readerIndex) {
    assert mCount < mHashes.length();
    mHashes.setInt(mCount, name.hashCode());
    mIndexIds.set(mCount, (((long) readerIndex) << 32) | (((long) seqId) & 0xFFFFFFFFL));
    mCount++;
  }

  static int sequenceId(long indexId) {
    return (int) (indexId & 0xFFFFFFFFL);
  }

  static int readerIndex(long indexId) {
    return (int) ((indexId >> 32) & 0xFFFFFFFFL);
  }

  /**
   * A method to check if there are any duplicate sequence names in a
   * given set of sequence readers and output any duplicates found to
   * the given file name.
   * @param prereadNames the SDF names to check for duplicates in
   * @param duplicatesOutputFile file to output duplicates to
   * @return true if there were duplicates detected
   * @throws IOException if there is an error
   */
  public boolean checkPrereadDuplicates(PrereadNamesInterface[] prereadNames, File duplicatesOutputFile) throws IOException {
    assert mCount == mHashes.length();
    boolean duplicatesDetected = false;
    if (mCount > 0) {
      OutputStream output = null;
      try {
        sort();
        long currentHash = mHashes.get(0);
        final Set<String> names = new HashSet<>();
        for (long i = 1; i < mHashes.length(); i++) {
          final long nextHash = mHashes.get(i);
          if (nextHash > currentHash) {
            currentHash = nextHash;
            if (!names.isEmpty()) {
              names.clear();
            }
          } else if (nextHash == currentHash) {
            if (names.isEmpty()) {
              final long lastIndexId = mIndexIds.get(i - 1);
              names.add(prereadNames[readerIndex(lastIndexId)].name(sequenceId(lastIndexId)));
            }
            final long indexId = mIndexIds.get(i);
            final String name = prereadNames[readerIndex(indexId)].name(sequenceId(indexId));
            if (!names.add(name)) {
              duplicatesDetected = true;
              if (output == null && duplicatesOutputFile != null) {
                output = new FileOutputStream(duplicatesOutputFile, true);
              }
              if (output != null) {
                output.write((name + StringUtils.LS).getBytes());
              }
            }
          } else {
            throw new RuntimeException("List sorting failed");
          }
        }
      } finally {
        if (output != null) {
          output.close();
        }
      }
    }
    return duplicatesDetected;
  }

  /**
   * A method to check if there are any duplicate sequence names in a
   * given set of sequence readers and output any duplicates found to
   * the given file name.
   * @param readers the sequence readers to check for duplicates in
   * @param duplicatesOutputFile file to output duplicates to
   * @return true if there were duplicates detected
   * @throws IOException if there is an error
   */
  public boolean checkSequenceDuplicates(SequencesReader[] readers, File duplicatesOutputFile) throws IOException {
    final PrereadNamesInterface[] prereadNames = new PrereadNamesInterface[readers.length];
    for (int i = 0; i < readers.length; i++) {
      prereadNames[i] = readers[i] != null ? readers[i].names() : null;
    }
    return checkPrereadDuplicates(prereadNames, duplicatesOutputFile);
  }

  void sort() {
    IndexSorter.sort(mHashes, mIndexIds, mHashes.length());
  }

  /**
   * Run the duplicate detection on one set of input sequences.
   * @param sequences the sequences to check for duplicates in.
   * @param outputFile the file to output the duplicate names to.
   * @return true if duplicates are in the given sequence, false otherwise.
   * @throws IOException if the sequences readers throw an exception.
   */
  public static boolean checkSequence(SequencesReader sequences, File outputFile) throws IOException {
    return checkPrereadNames(sequences.names(), outputFile);
  }

  /**
   * Run the duplicate detection on one set of input sequences.
   * @param prereadNames the SDF names to check for duplicates in.
   * @param outputFile the file to output the duplicate names to.
   * @return true if duplicates are in the given sequence, false otherwise.
   * @throws IOException if the sequences readers throw an exception.
   */
  public static boolean checkPrereadNames(PrereadNamesInterface prereadNames, File outputFile) throws IOException {
    final long numberSequences = prereadNames.length();
    final NameDuplicateDetector dupDetector = new NameDuplicateDetector(numberSequences);
    for (long i = 0; i < numberSequences; i++) {
      dupDetector.addPair(prereadNames.name(i), (int) i, 0);
    }
    return dupDetector.checkPrereadDuplicates(new PrereadNamesInterface[] {prereadNames}, outputFile);
  }
}
