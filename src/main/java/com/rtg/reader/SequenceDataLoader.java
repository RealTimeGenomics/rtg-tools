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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.rtg.util.Pair;
import com.rtg.util.array.CommonIndex;
import com.rtg.util.bytecompression.ByteArray;
import com.rtg.util.io.FileUtils;

/**
 * Methods for loading data from preread files.
 */
public final class SequenceDataLoader {

  private SequenceDataLoader() { }

  static long loadData(ByteArray seqData, DataFileIndex seqIndex, long start, File dir, CommonIndex positions, ByteArray checksums, DataFileOpenerFactory openFact, PointerFileHandler handler, boolean checksumsLoaded) throws IOException {
    final ArrayList<File> dataFiles = new ArrayList<>();
    for (int i = 0; i < seqIndex.numberEntries(); ++i) {
      dataFiles.add(SdfFileUtils.sequenceDataFile(dir, i));
    }
    return loadData(seqData, seqIndex, start, dir, positions, checksums, dataFiles, openFact.getSequenceOpener(), handler, checksumsLoaded);
  }

  static long loadQuality(ByteArray qualData, DataFileIndex seqIndex, long start, File dir, CommonIndex positions, ByteArray checksums, DataFileOpenerFactory openFact, PointerFileHandler handler, boolean checksumsLoaded) throws IOException {
    final ArrayList<File> dataFiles = new ArrayList<>();
    for (int i = 0; i < seqIndex.numberEntries(); ++i) {
      dataFiles.add(SdfFileUtils.qualityDataFile(dir, i));
    }
    return loadData(qualData, seqIndex, start, dir, positions, checksums, dataFiles, openFact.getQualityOpener(), handler, checksumsLoaded);
  }

  static long loadData(ByteArray data, DataFileIndex seqIndex, long start, File dir, CommonIndex positions, ByteArray checksums, List<File> dataFiles, DataFileOpener opener, PointerFileHandler handler, boolean checksumsLoaded) throws IOException {
    int startFile = 0;
    long sequencesSoFar = 0;
    while (startFile < seqIndex.numberEntries() && sequencesSoFar + seqIndex.numberSequences(startFile) <= start) {
      sequencesSoFar += seqIndex.numberSequences(startFile);
      ++startFile;
    }
    long dataOffset = 0;
    final CRC32 checksum = new CRC32();
    final PrereadHashFunction hf = new PrereadHashFunction();
    int i = startFile;
    final long totalLength = positions.get(positions.length() - 1);
    if (totalLength == 0) { //if we're asked for x sequences which comprise of no data, do a shortcircuit.
      for (int j = 0; j < positions.length() - 1; ++j) {
        hf.irvineHash(0L);
      }
      return hf.getHash();
    }

    long currentSeq = 0;

    final File pointerFile = SdfFileUtils.sequencePointerFile(dir, startFile);
    final int intPosition = (int) (start - sequencesSoFar);
    final long firstFileSkip = handler.readPointer(pointerFile, intPosition); //FileUtils.getIntFromFile(pointerFile, intPosition);
    while (dataOffset < totalLength) {
      final File seqFile = dataFiles.get(i);
      // larger buffer than normal as we know the file sizes are many GB
      final long dataRead;
      try (InputStream stream = opener.open(seqFile, seqIndex.dataSize(i))) {
        if (i == startFile) {
          // skip to the first sequence we want
          FileUtils.skip(stream, firstFileSkip);
        }
        final Pair<Long, Long> loaded = load(data, stream, dataOffset, totalLength - dataOffset, positions, checksum, checksums, hf, checksumsLoaded, currentSeq);
        dataRead = loaded.getA();
        currentSeq = loaded.getB();
      }
      dataOffset += dataRead;
      ++i;
    }
    while (currentSeq < positions.length() - 1) {  //while there are more sequences to read (but no data to read!)
      hf.irvineHash(0L);
      ++currentSeq;
    }
    return hf.getHash();
  }

  /** A private method for loading from a file, then compressing and calculating checksums */
  private static Pair<Long, Long> load(ByteArray a, InputStream stream, long memoryOffset, long count, CommonIndex positions, CRC32 checksum, ByteArray checksums, PrereadHashFunction hf, boolean checksumsLoaded, long currentSeq) throws IOException {
    long tmpCurrentSeq = currentSeq;

    final byte[] buffer = new byte[1024 * 1024];
    long totalRead = 0;
    int bytesRead;
    long pos = memoryOffset;
    while (totalRead < count && (bytesRead = stream.read(buffer, 0, (int) Math.min(count - totalRead, buffer.length))) != -1) {
      a.set(pos, buffer, bytesRead);
      pos += bytesRead;
      int j = 0;
      while (j < bytesRead) {
        if (positions.get(tmpCurrentSeq + 1) == positions.get(tmpCurrentSeq)) {
          hf.irvineHash(0L);
          ++tmpCurrentSeq;
          continue;
        }
        final int len = (int) (positions.get(tmpCurrentSeq + 1) - (memoryOffset + totalRead + j));
        if (j + len <= bytesRead) {
          checksum.update(buffer, j, len);
          hf.irvineHash(buffer, j, len);
          hf.irvineHash(positions.get(tmpCurrentSeq + 1) - positions.get(tmpCurrentSeq));
          if (checksumsLoaded) {
            if ((byte) checksum.getValue() != checksums.get(tmpCurrentSeq)) {
              throw new CorruptSdfException("Sequence: " + tmpCurrentSeq + " failed checksum");
            }
          } else {
            checksums.set(tmpCurrentSeq, (byte) checksum.getValue());
          }
          ++tmpCurrentSeq;
          checksum.reset();
        } else {
          hf.irvineHash(buffer, j, bytesRead - j);
          checksum.update(buffer, j, bytesRead - j);
        }
        j += len;
      }
      totalRead += bytesRead;
    }
    return new Pair<>(totalRead, tmpCurrentSeq);
  }

  static long loadPositions(final CommonIndex positions, final DataFileIndex seqIndex, final long start, final long end, final File dir, PointerFileHandler handler, ByteArray checksums, ByteArray qualityChecksums) throws IOException {
    int startFile = 0;
    long sequencesSoFar = 0;
    while (startFile < seqIndex.numberEntries() && sequencesSoFar + seqIndex.numberSequences(startFile) <= start) {
      sequencesSoFar += seqIndex.numberSequences(startFile);
      ++startFile;
    }
    final long numberOfSequences = end - start;
    long posOffset = 0;
    int currentFile = startFile;
    long sequenceOffset = start - sequencesSoFar;
    final File f1 = SdfFileUtils.sequencePointerFile(dir, currentFile);
    final long offsetIntoPointerFile = handler.readPointer(f1, sequenceOffset); //FileUtils.getIntFromFile(f1, (int) sequenceOffset);
    long dataSize = -offsetIntoPointerFile;
    while (posOffset <= numberOfSequences && currentFile < seqIndex.numberEntries()) {
      final File f = SdfFileUtils.sequencePointerFile(dir, currentFile);
      final long intsInFile = seqIndex.numberSequences(currentFile); //f.length() / 4;
      final long endOffset = Math.min(intsInFile, end - sequencesSoFar + 1);
      final int read = handler.readPointers(f, (int) sequenceOffset, (int) endOffset, positions, posOffset, dataSize, checksums, qualityChecksums); //  ArrayUtils.readInts(f, (int) sequenceOffset, (int) endOffset, positions, posOffset, dataSize);
      posOffset += read;
      dataSize += seqIndex.dataSize(currentFile) ; //Bsd.sequenceDataFile(dir, currentFile).length();
      sequencesSoFar += read + sequenceOffset;
      sequenceOffset = 0;
      ++currentFile;
    }
    if (end == seqIndex.getTotalNumberSequences()) {
      final int fileNo = seqIndex.numberEntries() - 1;
      handler.readChecksums(SdfFileUtils.sequencePointerFile(dir, fileNo), seqIndex.numberSequences(fileNo), checksums, qualityChecksums, (int) (numberOfSequences - 1));
    }
    if (posOffset == numberOfSequences) {
      positions.set(posOffset, dataSize);
    }
    return positions.get(positions.length() - 1);
  }


}
