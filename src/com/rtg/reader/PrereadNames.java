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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FileUtils;

/**
 * In memory copy of sequence names.
 *
 */
public class PrereadNames implements PrereadNamesInterface {


  static int loadPointers(ArrayList<int[]> pointersList, final File preread, final long start, final long end, final DataFileIndex nameIndex, boolean suffixes) throws IOException {
    int startFile = 0;
    long namesSoFar = 0;
    while (startFile < nameIndex.numberEntries() && namesSoFar + nameIndex.numberSequences(startFile) <= start) {
      namesSoFar += nameIndex.numberSequences(startFile);
      startFile++;
    }
    int k = startFile;
    File source;
    final File pointerFile;
    if (suffixes) {
      pointerFile = SdfFileUtils.labelSuffixPointerFile(preread, startFile);
    } else {
      pointerFile = SdfFileUtils.labelPointerFile(preread, startFile);
    }
    final int firstPointer = FileUtils.getIntFromFile(pointerFile, (int) (start - namesSoFar));
    while (namesSoFar < end && (source = suffixes ? SdfFileUtils.labelSuffixPointerFile(preread, k) : SdfFileUtils.labelPointerFile(preread, k)).exists()) {
      try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(source), FileUtils.BUFFERED_STREAM_SIZE)) {
        final long length;
        final int pointerOffset;
        if (k == startFile) {
          FileUtils.streamSkip(bis, (start - namesSoFar) * 4);
          pointerOffset = firstPointer;
          length = Math.min(end - namesSoFar, nameIndex.numberSequences(k) - (start - namesSoFar)) + 1;
          namesSoFar = start;
        } else {
          pointerOffset = 0;
          length = Math.min(end - namesSoFar, nameIndex.numberSequences(k)) + 1;
        }
        final int[] pointers = new int[(int) length];
        final int pointersRead = suckPointers(bis, pointers, pointerOffset);
        if (pointersRead == length - 1) {
          pointers[pointers.length - 1] = (int) nameIndex.dataSize(k) - pointerOffset; //(int) nameFile.length() - pointerOffset;
        } else if (pointersRead < length) {
          throw new IOException();
        }
        namesSoFar += length - 1;
        pointersList.add(pointers);
      }
      k++;
    }
    return firstPointer;
  }

  private static byte[] suckDataIn(final InputStream source, long length) throws IOException {
    final byte[] res = new byte[(int) length];
    int remaining = (int) length;
    int read;
    while (remaining > 0 && (read = source.read(res, (int) length - remaining, remaining)) > 0) {
      remaining -= read;
    }
    if (remaining > 0) {
      throw new IOException();
    }
    return res;
  }

  private static int suckPointers(final InputStream source, final int[] res, int pointerOffset) throws IOException {
    int remaining = res.length * 4;
    int read;
    final byte[] pointerBytes = new byte[remaining];
    while (remaining > 0 && (read = source.read(pointerBytes, pointerBytes.length - remaining, remaining)) > 0) {
      remaining -= read;
    }
    int i;
    int count = 0;
    for (i = 0; i < pointerBytes.length - remaining; i += 4) {
      res[i / 4] = ByteArrayIOUtils.bytesToIntBigEndian(pointerBytes, i) - pointerOffset;
      count++;
    }
    return count;
  }


  private final ArrayList<byte[]> mNames = new ArrayList<>();
  private final ArrayList<int[]> mPointers = new ArrayList<>();

  private final boolean mSuffixes;

  /** Constructor only for testing purposes. */
  PrereadNames() {
    mSuffixes = false;
  }


  /**
   * Construct from a preread.
   * @param preread preread directory
   * @param region the region of the SDF to load names for
   * @exception IOException if an I/O error occurs.
   */
  public PrereadNames(File preread, LongRange region) throws IOException {
    this(preread, region, false);
  }

  /**
   * Construct from a preread.
   * @param preread preread directory
   * @param region the region of the SDF to load names for
   * @param suffixes load name suffixes instead of names
   * @exception IOException if an I/O error occurs.
   */
  public PrereadNames(final File preread, LongRange region, boolean suffixes) throws IOException {
    mSuffixes = suffixes;
    final IndexFile id = new IndexFile(preread);
    if (!id.hasNames()) {
      throw new FileNotFoundException("Error: SDF contains no name data");
    }
    final long start = Math.max(region.getStart(), 0);
    final long end = region.getEnd() == LongRange.MISSING ? id.getNumberSequences() : region.getEnd();
    assert end >= start;
    if (end > id.getNumberSequences()) {
      throw new IllegalArgumentException("End sequence is greater than number of sequences in SDF");
    }
    if (end - start > 0) {
      //final long[] nameIndex = ArrayUtils.readLongArray(Bsd.labelIndexFile(preread));
      final DataFileIndex nameIndex;
      if (mSuffixes) {
        nameIndex = DataFileIndex.loadLabelSuffixDataFileIndex(id.dataIndexVersion(), preread);
      } else {
        nameIndex = DataFileIndex.loadLabelDataFileIndex(id.dataIndexVersion(), preread);
      }
      final int firstNameOffset = loadPointers(mPointers, preread, start, end, nameIndex, mSuffixes);

      loadNames(mNames, mPointers, preread, start, nameIndex, firstNameOffset, mSuffixes);
    }
  }

 @Override
  public long calcChecksum() {
    final PrereadHashFunction namef = new PrereadHashFunction();
    for (int k = 0; k < mNames.size(); k++) {
      final byte[] names = mNames.get(k);
      final int[] pointers = mPointers.get(k);
      for (int iid = 0; iid < pointers.length - 1; iid++) {
        final int start = pointers[iid];
        // -1 below accounts for terminating nul byte
        final int end = pointers[iid + 1] - 1;
        final int len = end - start;
        namef.irvineHash(names, pointers[iid], len);
        namef.irvineHash(len);
      }
    }
    return namef.getHash();
  }

  @Override
  public String name(final long id) {
    int k = 0;
    long lid = id;
    // -1 below accounts for sentinel
    int len = mPointers.get(k).length - 1;
    while (lid >= len) {
      lid -= len;
      len = mPointers.get(++k).length - 1;
    }
    final int iid = (int) lid;
    final int start = mPointers.get(k)[iid];
    // -1 below accounts for terminating nul byte
    final int end = mPointers.get(k)[iid + 1] - 1;
    return new String(mNames.get(k), start, end - start);
  }

  @Override
  public void writeName(final Appendable a, final long id) throws IOException {
    int k = 0;
    long lid = id;
    // -1 below accounts for sentinel
    int len = mPointers.get(k).length - 1;
    while (lid >= len) {
      lid -= len;
      len = mPointers.get(++k).length - 1;
    }
    final int iid = (int) lid;
    // -1 below accounts for terminating nul byte
    final int end = mPointers.get(k)[iid + 1] - 1;
    final byte[] raw = mNames.get(k);
    for (int i = mPointers.get(k)[iid]; i < end; i++) {
      a.append((char) raw[i]);
    }
  }

  @Override
  public void writeName(final OutputStream os, final long id) throws IOException {
    int k = 0;
    long lid = id;
    // -1 below accounts for sentinel
    int len = mPointers.get(k).length - 1;
    while (lid >= len) {
      lid -= len;
      len = mPointers.get(++k).length - 1;
    }
    final int iid = (int) lid;
    final int start = mPointers.get(k)[iid];
    // -1 below accounts for terminating nul byte
    final int end = mPointers.get(k)[iid + 1] - 1;
    os.write(mNames.get(k), start, end - start);
  }

  private static void loadNames(ArrayList<byte[]> names, ArrayList<int[]> pointers, File preread, long start, DataFileIndex nameIndex, int firstNameOffset, boolean suffixes) throws IOException {
    int startFile = 0;
    long namesSoFar = 0;
    while (startFile < nameIndex.numberEntries() && namesSoFar + nameIndex.numberSequences(startFile) <= start) {
      namesSoFar += nameIndex.numberSequences(startFile);
      startFile++;
    }
    int k = startFile;
    for (int[] pointerArray : pointers) {
      final File source = suffixes ? SdfFileUtils.labelSuffixDataFile(preread, k) : SdfFileUtils.labelDataFile(preread, k);
      try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(source), FileUtils.BUFFERED_STREAM_SIZE)) {
        if (k == startFile) {
          FileUtils.streamSkip(bis, firstNameOffset);
        }
        final int length = pointerArray[pointerArray.length - 1];
        names.add(suckDataIn(bis, length));
      }
      k++;
    }
  }

  @Override
  public long bytes() {
    long length = 0;
    for (byte[] name : mNames) {
      if (name != null) {
        length += name.length;
      }
    }
    long poiLength = 0;
    for (int[] poi : mPointers) {
      if (poi != null) {
        poiLength += poi.length;
      }
    }
    return (mNames.size() + mPointers.size()) * 8L + length + poiLength * 4;
  }

  @Override
  public long length() {
    if (mPointers.size() > 0) {
      long length = 0;
      for (final int[] pointers : mPointers) {
        if (pointers.length > 0) {
          length += pointers.length - 1;
        }
      }
      return length;
    }
    return 0;
  }

  /**
   * Quick print names from an SDF
   * @param args command-line arguments
   * @throws IOException all the time.
   */
  public static void main(String[] args) throws IOException {
    final PrereadNames names = new PrereadNames(new File(args[0]), LongRange.NONE);
    for (int i = 0; i < names.length(); i++) {
      System.out.println(i + ": " + names.name(i));
    }
  }

}

