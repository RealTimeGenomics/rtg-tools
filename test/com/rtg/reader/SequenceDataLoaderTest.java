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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;

import com.rtg.mode.SequenceType;
import com.rtg.util.array.ArrayUtils;
import com.rtg.util.array.longindex.LongCreate;
import com.rtg.util.array.longindex.LongIndex;
import com.rtg.util.bytecompression.BitwiseByteArray;
import com.rtg.util.bytecompression.ByteArray;
import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class for SequenceDataLoader methods
 */
public class SequenceDataLoaderTest extends TestCase {
  private File mDir = null;

  @Override
  public void setUp() throws Exception {
    mDir = FileHelper.createTempDirectory();
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    FileHelper.deleteAll(mDir);
    mDir = null;
  }

  private DataFileOpenerFactory defaultOpenerFactory() {
    return new DataFileOpenerFactory(IndexFile.SEQUENCE_ENCODING_COMPRESSED, IndexFile.QUALITY_ENCODING_COMPRESSED, SequenceType.DNA);
  }

  private static class TestDataFileIndex extends DataFileIndex {

    private final long[] mIndex;
    TestDataFileIndex(long[] index) {
      mIndex = index;
    }

    @Override
    long dataSize(int fileIndex) {
      return mIndex[fileIndex * 2 + 1];
    }

    @Override
    long getTotalNumberSequences() {
      long tot = 0;
      for (final long l : mIndex) {
        tot += l;
      }
      return tot;
    }

    @Override
    int numberEntries() {
      return mIndex.length / 2;
    }

    @Override
    long numberSequences(int fileIndex) {
      return mIndex[fileIndex * 2];
    }

  }

  static final String POSITIONS_FASTA =
          ">r0" + LS + "acgtacgtacgt" + LS  // 12  0 -> 11
          + ">r1" + LS + "ACGTACGT" + LS    // 8   12 -> 19
          + ">r2" + LS + "ACGTACGT" + LS    // 8   20 -> 27
          + ">r3" + LS + "ACGTACGT" + LS    // 8   28 -> 35
          + ">r4" + LS + "ACGTACGT" + LS    // 8   36 -> 43
          + ">r5" + LS + "ACGTACGT" + LS;   // 8   44 -> 51

  public void testLoadPositions() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(POSITIONS_FASTA, mDir, new SdfId(1L));
    reader.close();
    final LongIndex index = LongCreate.createIndex(3);
    final long[] seqIndex = {6, 52};

    final long dataSize = SequenceDataLoader.loadPositions(index, new TestDataFileIndex(seqIndex), 2, 4, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));
    assertEquals(16, dataSize);
    assertEquals(0, index.get(0));
    assertEquals(8, index.get(1));
    assertEquals(16, index.get(2));

    final LongIndex index2 = LongCreate.createIndex(7);
    final long[] seqIndex2 = {6, 52};
    SequenceDataLoader.loadPositions(index2, new TestDataFileIndex(seqIndex2), 0, 6, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index2.length()), ByteArray.allocate(index2.length()));
    assertEquals(0, index2.get(0));
    assertEquals(12, index2.get(1));
    assertEquals(20, index2.get(2));
    assertEquals(28, index2.get(3));
    assertEquals(36, index2.get(4));
    assertEquals(44, index2.get(5));
    assertEquals(52, index2.get(6));

  }

  public void testLoadPositionsPartialFiles() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(POSITIONS_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(3);
    final long[] seqIndex = ArrayUtils.readLongArray(SdfFileUtils.sequenceIndexFile(mDir));

    final long dataSize = SequenceDataLoader.loadPositions(index, new TestDataFileIndex(seqIndex), 1, 3, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));
    assertEquals(16, dataSize);
    assertEquals(0, index.get(0));
    assertEquals(8, index.get(1));
    assertEquals(16, index.get(2));

  }




  static final String POSITIONS_FASTA_2 = ">r0" + LS + "ACGTACGTACGTACGT" + LS //16 0 file0
          + ">r1" + LS + "ACGTACGTACGTACGT" + LS                               //16 16 file0
          + ">r2" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 32 file1
          + ">r3" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 52 file2
          + ">r4" + LS + "ACGTACGTACGT" + LS                                   //12 72 file3
          + ">r5" + LS + "ACGTAC" + LS;                                        //6  84 file4

  public void testLoadPositionsSplit() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(POSITIONS_FASTA_2, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(3);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(new IndexFile(mDir).dataIndexVersion(), mDir);

    SequenceDataLoader.loadPositions(index, seqIndex, 3, 5, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));
    assertEquals(0, index.get(0));
    assertEquals(20, index.get(1));
    assertEquals(32, index.get(2));

    final LongIndex index2 = LongCreate.createIndex(3);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex2 = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex2 = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);

    SequenceDataLoader.loadPositions(index2, seqIndex2, 0, 2, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index2.length()), ByteArray.allocate(index2.length()));
    assertEquals(0, index2.get(0));
    assertEquals(16, index2.get(1));
    assertEquals(32, index2.get(2));
  }
  public void testLoadPositionsSingle() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(POSITIONS_FASTA_2, mDir, new SdfId(1L), 20);
    reader.close();
    final int[] results = {16, 16, 20, 20, 12, 6};
    for (int i = 0; i < 6; i++) {
      final LongIndex index = LongCreate.createIndex(2);
      //long[] seqIndex = {2, 1, 1, 1, 1};
      //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
      final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);

      SequenceDataLoader.loadPositions(index, seqIndex, i, i + 1, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));
      assertEquals(0, index.get(0));
      assertEquals(results[i], index.get(1));
    }
  }

  public void testLoadPositionsAll() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(POSITIONS_FASTA_2, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(7);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);

    SequenceDataLoader.loadPositions(index, seqIndex, 0, 6, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));
    assertEquals(0, index.get(0));
    assertEquals(16, index.get(1));
    assertEquals(32, index.get(2));
    assertEquals(52, index.get(3));
    assertEquals(72, index.get(4));
    assertEquals(84, index.get(5));
    assertEquals(90, index.get(6));
  }

  static final String POSITIONS_FASTQ = "@r0" + LS + "ACGTACGTACGTACGT" + LS //16 0 file0
          + "+" + LS + "ZZZZZZZZZZZZZZZZ" + LS
          + "@r1" + LS + "ACGTACGTACGTACGT" + LS                               //16 16 file0
          + "+" + LS + "XXXXXXXXXXXXXXXX" + LS
          + "@r2" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 32 file1
          + "+" + LS + "BBBBBBBBBBBBBBBBBBBB" + LS
          + "@r3" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 52 file2
          + "+" + LS + "YYYYYYYYYYYYYYYYYYYY" + LS
          + "@r4" + LS + "ACGTACGTACGT" + LS                                   //12 72 file3
          + "+" + LS + "DDDDDDDDDDDD" + LS
          + "@r5" + LS + "ACGTAC" + LS                                        //6  84 file4
          + "+" + LS + "EEEEEE" + LS;
  public void testPartialFastq() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNAFastq(POSITIONS_FASTQ, mDir, false);
    reader.close();

    final LongIndex index = LongCreate.createIndex(3);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);

    SequenceDataLoader.loadPositions(index, seqIndex, 3, 5, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));

    final BitwiseByteArray seqData = new BitwiseByteArray(32, CompressedByteArray.minBits(64));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    SequenceDataLoader.loadQuality(seqData, seqIndex, 3, 5, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);

    checkQualityRange(seqData, 0, 20, 'Y');
    checkQualityRange(seqData, 20, 32, 'D');

  }

  public void testFullFastq() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNAFastq(POSITIONS_FASTQ, mDir, false);
    reader.close();

    final LongIndex index = LongCreate.createIndex(7);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(new IndexFile(mDir).dataIndexVersion(), mDir);

    SequenceDataLoader.loadPositions(index, seqIndex, 0, 6, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));

    final BitwiseByteArray seqData = new BitwiseByteArray(90, CompressedByteArray.minBits(64));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    final long hash = SequenceDataLoader.loadQuality(seqData, seqIndex, 0, 6, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);

    assertTrue(new IndexFile(mDir).getQualityChecksum() == hash);
    checkQualityRange(seqData, 0, 16, 'Z');
    checkQualityRange(seqData, 16, 32, 'X');
    checkQualityRange(seqData, 32, 52, 'B');
    checkQualityRange(seqData, 52, 72, 'Y');
    checkQualityRange(seqData, 72, 84, 'D');
    checkQualityRange(seqData, 84, 90, 'E');

  }

  private void checkQualityRange(ByteArray data, int start, int end, char value) {
    for (int i = start; i < end; i++) {
      final byte qual = data.get(i);
      assertEquals(value - 33, qual);
    }
  }

  public void testDataLoad() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(POSITIONS_FASTA_2, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(3);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);
    SequenceDataLoader.loadPositions(index, seqIndex, 3, 5, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));


    final BitwiseByteArray seqData = new BitwiseByteArray(32, CompressedByteArray.minBits(5));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    SequenceDataLoader.loadData(seqData, seqIndex, 3, 5, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);


    for (int i = 0; i < seqData.length(); i++) {
      assertEquals(i % 4 + 1, seqData.get(i));
    }
  }

  static final String FILE_ALIGNED_FASTA = ">r0" + LS + "ACGTACGTACGTACGTACGT" + LS //20 0 file0
          + ">r1" + LS + "ACGTACGTACGTACGT" + LS                               //16 20 file0
          + ">r2" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 36 file1
          + ">r3" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 56 file2
          + ">r4" + LS + "ACGTACGTACGT" + LS                                   //12 76 file3
          + ">r5" + LS + "ACGTAC" + LS;                                        //6  88 file4
  public void testFileAlignedLoad() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(FILE_ALIGNED_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(2);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    final long[] seqIndex = ArrayUtils.readLongArray(SdfFileUtils.sequenceIndexFile(mDir));
    SequenceDataLoader.loadPositions(index, new TestDataFileIndex(seqIndex), 1, 2, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));


    final BitwiseByteArray seqData = new BitwiseByteArray(16, CompressedByteArray.minBits(5));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    SequenceDataLoader.loadData(seqData, new TestDataFileIndex(seqIndex), 1, 2, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);

    for (int i = 0; i < seqData.length(); i++) {
      assertEquals(i % 4 + 1, seqData.get(i));
    }
  }

  public void testHashFunction() throws IOException {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(FILE_ALIGNED_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(7);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);
    SequenceDataLoader.loadPositions(index, seqIndex, 0, 6, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));


    final BitwiseByteArray seqData = new BitwiseByteArray(94, CompressedByteArray.minBits(5));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    final long hash = SequenceDataLoader.loadData(seqData, seqIndex, 0, 6, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);

    final IndexFile indexFile = new IndexFile(mDir);
    assertTrue(hash == indexFile.getDataChecksum());
  }

  public void testReadChecksum() throws IOException {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(FILE_ALIGNED_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    final LongIndex index = LongCreate.createIndex(7);
    //long[] seqIndex = {2, 1, 1, 1, 1};
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.sequenceIndexFile(mDir));
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);
    SequenceDataLoader.loadPositions(index, seqIndex, 0, 6, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));

    final BitwiseByteArray seqData = new BitwiseByteArray(94, CompressedByteArray.minBits(5));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    SequenceDataLoader.loadData(seqData, seqIndex, 0, 6, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);
  }

  public void testZeroLengthRead() throws Exception {
    checkCheckSum(">null\n\n", 1);
  }

  private void checkCheckSum(String read, int numberOfSequences) throws IOException {
    final SequencesReader reader = ReaderTestUtils.getReaderDNA(read, mDir, new SdfId(1L), 20);
    reader.close();

    final LongIndex index = LongCreate.createIndex(numberOfSequences + 1);
    final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(DataFileIndex.DATASIZE_VERSION, mDir);
    SequenceDataLoader.loadPositions(index, seqIndex, 0, numberOfSequences, mDir, PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), ByteArray.allocate(index.length()), ByteArray.allocate(index.length()));

    final BitwiseByteArray seqData = new BitwiseByteArray(94, CompressedByteArray.minBits(5));
    final ByteArray checksums = ByteArray.allocate(index.length() - 1);
    final long hash = SequenceDataLoader.loadData(seqData, seqIndex, 0, numberOfSequences, mDir, index, checksums, defaultOpenerFactory(), PointerFileHandler.getHandler(new IndexFile(mDir), PointerFileHandler.SEQUENCE_POINTER), false);

    final IndexFile idxf = new IndexFile(mDir);
    assertEquals(idxf.getDataChecksum(), hash);
  }

  public void testTwoZeroLengthReads() throws Exception {
    checkCheckSum(">null1\n\n>null\n\n", 2);
  }
  public void testZeroLengthReadWithOtherReadsFirst() throws Exception {
    checkCheckSum(">read\nACGT\n>null\n\n", 2);
  }
  public void testZeroLengthReadWithOtherReadsAfter() throws Exception {
    checkCheckSum(">null\n\n>read\nACGT\n", 2);
  }
  public void testZeroLengthReadWithOtherReadsBeforeAndAfter() throws Exception {
    checkCheckSum(">read1\nACGTG\n>null\n\n>read\nACGT\n", 3);
  }

  public void testRollZeroLength() throws Exception {
    checkCheckSum(">read1\nACGTGATCGATGACGATGA\n>null\n\n>read\nACGT\n", 3); //19, 0, 4
    checkCheckSum(">read1\nACGTGATCGATGACGATGAC\n>null\n\n>read\nACGT\n", 3); //20, 0, 4
    checkCheckSum(">read1\nACGTGATCGATGACGATGAC\n>null\n\n>null2\n\n>read\nACGT\n", 4); //20, 0, 0, 4
  }
}
