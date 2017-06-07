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
package com.rtg.sam;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rtg.tabix.BlockCompressedPositionReader;
import com.rtg.tabix.SequenceIndex;
import com.rtg.tabix.SequenceIndex.SequenceIndexChunk;
import com.rtg.tabix.SequenceIndexContainer;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FileUtils;

/**
 * Methods for use with BAM index file.
 */
public final class BamIndexer {


  /** file extension for <code>BAM</code> indices */
  public static final String BAM_INDEX_EXTENSION = ".bai";



  private BamIndexer() {

  }


  /**
   * Creates a BAM index from given BAM stream
   * @param bam BAM stream to index
   * @return the BAM index
   * @throws IOException If an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static SequenceIndexContainer createBamIndex(final InputStream bam) throws IOException, UnindexableDataException {
    final BamReader input = new BamReader(bam);
    return createBamIndexInternal(input, input.numReferences(), true);
  }

  /**
   * Creates a BAM index from given BAM stream
   * @param bam BAM stream to index
   * @param expectHeader whether BAM file being read contains a header
   * @param numReferences number of reference sequences
   * @return the BAM index
   * @throws IOException If an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static SequenceIndexContainer createBamIndexNoHeader(final InputStream bam, boolean expectHeader, int numReferences) throws IOException, UnindexableDataException {
    final BamReader input = new BamReader(new BgzfInputStream(bam), expectHeader);
    return createBamIndexInternal(input, numReferences, false);
  }

  private static SequenceIndexContainer createBamIndex(File bamFile) throws IOException, UnindexableDataException {
    try (FileInputStream in = new FileInputStream(bamFile)) {
      return createBamIndex(in);
    }
  }

  private static SequenceIndexContainer createBamIndexNoHeader(File bamFile, boolean expectHeader, int numReferences) throws IOException, UnindexableDataException {
    try (FileInputStream in = new FileInputStream(bamFile)) {
      return createBamIndexNoHeader(in, expectHeader, numReferences);
    }
  }

  private static SequenceIndexContainer createBamIndexInternal(BamReader input, int numReferences, boolean refLengths) throws IOException, UnindexableDataException {
    final SequenceIndex[] indexs = new SequenceIndex[numReferences];
    for (int i = 0; i < indexs.length; ++i) {
      indexs[i] = refLengths ? new SequenceIndex(input.referenceLength(i)) : new SequenceIndex();
    }
    final List<SequenceIndex> indexList = Arrays.asList(indexs);
    final long unmapped = TabixIndexer.populateIndex(indexList, new BamPositionReader(input), true);
    TabixIndexer.mergeChunks(indexList);
    return new SequenceIndexContainer(indexList, unmapped);
  }

  /**
   * Writes a BAM index from given BAM stream
   * @param bam BAM stream to index
   * @param bamIndex stream to write index to
   * @throws IOException If an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static void saveBamIndex(final InputStream bam, final OutputStream bamIndex) throws IOException, UnindexableDataException {
    writeIndex(createBamIndex(bam), bamIndex);
  }

  /**
   * Writes a BAM index from given BAM stream, assumes no header is present
   * @param bam BAM stream to index
   * @param bamIndex stream to write index to
   * @param expectHeader whether BAM file being read contains a header
   * @param numReferences number of reference sequences
   * @throws IOException If an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static void saveBamIndexNoHeader(final InputStream bam, final OutputStream bamIndex, boolean expectHeader, int numReferences) throws IOException, UnindexableDataException {
    writeIndex(createBamIndexNoHeader(bam, expectHeader, numReferences), bamIndex);
  }

  /**
   * Writes a BAM index from given BAM file
   * @param bamFile BAM file to index
   * @param bamIndex file to write index to
   * @throws IOException If an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static void saveBamIndex(final File bamFile, final File bamIndex) throws IOException, UnindexableDataException {
    final SequenceIndexContainer indexs = createBamIndex(bamFile);
    saveBamIndex(indexs, bamIndex);
  }

  /**
   * Writes a BAM index from given BAM file
   * @param bamFile BAM file to index
   * @param bamIndex file to write index to
   * @param expectHeader whether BAM file being read contains a header
   * @param numReferences number of reference sequences
   * @throws IOException If an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static void saveBamIndexNoHeader(final File bamFile, final File bamIndex, boolean expectHeader, int numReferences) throws IOException, UnindexableDataException {
    final SequenceIndexContainer indexs = createBamIndexNoHeader(bamFile, expectHeader, numReferences);
    saveBamIndex(indexs, bamIndex);
  }

  private static void saveBamIndex(SequenceIndexContainer bic, File bamIndex) throws IOException {
    try (OutputStream indexOut = FileUtils.createOutputStream(bamIndex)) {
      writeIndex(bic, indexOut);
    }
  }

  /**
   * Saves given BAM index to given stream
   * @param container index to write
   * @param output destination
   * @throws IOException if an IO Error occurs
   */
  static void writeIndex(final SequenceIndexContainer container, final OutputStream output) throws IOException {
    final List<SequenceIndex> indexs = container.getIndexes();
    final byte[] header = {(byte) 'B', (byte) 'A', (byte) 'I', 1};
    final byte[] buf = new byte[1024];
    output.write(header);
    ByteArrayIOUtils.intToBytesLittleEndian(indexs.size(), buf, 0);
    output.write(buf, 0, 4);
    for (final SequenceIndex bai : indexs) {
      ByteArrayIOUtils.intToBytesLittleEndian(bai.getBins().size(), buf, 0);
      output.write(buf, 0, 4);
      for (final Map.Entry<Integer, ArrayList<SequenceIndexChunk>> entry : bai.getBins().entrySet()) {
        ByteArrayIOUtils.intToBytesLittleEndian(entry.getKey(), buf, 0);
        output.write(buf, 0, 4);
        final ArrayList<SequenceIndexChunk> chunks = entry.getValue();
        ByteArrayIOUtils.intToBytesLittleEndian(chunks.size(), buf, 0);
        output.write(buf, 0, 4);
        TabixIndexer.writeChunks(output, chunks, buf);
      }
      ByteArrayIOUtils.intToBytesLittleEndian(bai.getLinearSize(), buf, 0);
      output.write(buf, 0, 4);
      for (int j = 0; j < bai.getLinearSize(); ++j) {
        ByteArrayIOUtils.longToBytesLittleEndian(bai.getLinearIndex(j), buf, 0);
        output.write(buf, 0, 8);
      }
    }
    ByteArrayIOUtils.longToBytesLittleEndian(container.numUnmappedNoCoordinates(), buf, 0);
    output.write(buf, 0, 8);
  }






  private static class BamPositionReader implements BlockCompressedPositionReader {
    private final BamReader mReader;
    BamPositionReader(BamReader reader) {
      mReader = reader;
    }

    @Override
    public String getRecord() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void seek(long virtualOffset) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getLengthOnReference() {
      final String cigar = mReader.getField(SamBamConstants.CIGAR_FIELD);
      if (cigar.equals("*")) {
        // For unmapped reads use length of sequence
        return mReader.getField(SamBamConstants.SEQ_FIELD).length();
      }
      return SamUtils.cigarRefLength(cigar);
    }

    @Override
    public String getReferenceName() {
      return mReader.getField(SamBamConstants.RNAME_FIELD);
    }

    @Override
    public int getReferenceId() {
      return mReader.getIntField(SamBamConstants.RNAME_FIELD);
    }

    @Override
    public int getStartPosition() {
      return mReader.getIntField(SamBamConstants.POS_FIELD) - 1;
    }

    @Override
    public long getVirtualOffset() {
      return mReader.virtualOffset();
    }

    @Override
    public long getNextVirtualOffset() {
      return mReader.nextVirtualOffset();
    }

    @Override
    public List<String> getSequenceNames() {
      final ArrayList<String> ret = new ArrayList<>();
      for (int i = 0; i < mReader.numReferences(); ++i) {
        ret.add(mReader.referenceName(i));
      }
      return ret;
    }

    @Override
    public boolean hasNext() {
      return mReader.hasNext();
    }

    @Override
    public void next() {
      mReader.next();
    }

    @Override
    public int getBinNum() {
      return mReader.getIntField(BamReader.BIN_FIELD);
    }

    @Override
    public boolean hasCoordinates() {
      return hasReference() && mReader.getIntField(SamBamConstants.POS_FIELD) != 0;
    }

    @Override
    public boolean isUnmapped() {
      return (SamBamConstants.SAM_READ_IS_UNMAPPED & mReader.getIntField(SamBamConstants.FLAG_FIELD)) != 0;
    }

    @Override
    public boolean hasReference() {
      return mReader.getIntField(SamBamConstants.RNAME_FIELD) != -1;
    }

    @Override
    public void close() throws IOException {
      mReader.close();
    }
  }

  /**
   * Get the name an index should have for a given data file
   * @param data the file we want to index
   * @return the file name the index should have.
   */
  public static File indexFileName(File data) {
    return new File(data.getParentFile(), data.getName() + BAM_INDEX_EXTENSION);
  }

  /**
   * Get the secondary possible name for an index to have for a given data file
   * (Will return same as indexFileName if there is no file extension)
   * @param data the file we want to index
   * @return the secondary file name the index could have
   */
  public static File secondaryIndexFileName(File data) {
    final int extensionIndex = data.getName().lastIndexOf('.');
    if (extensionIndex != -1) {
      return new File(data.getParentFile(), data.getName().substring(0, extensionIndex) + BamIndexer.BAM_INDEX_EXTENSION);
    }
    return indexFileName(data);
  }

  /**
   * Creates a BAM index
   * @param args Usage: <code>bamFile bamIndex</code>
   * @throws IOException if an IO error occurs
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public static void main(final String[] args) throws IOException, UnindexableDataException {
    if (args.length == 3) {
      BamIndexer.saveBamIndexNoHeader(new File(args[0]), new File(args[1]), true, Integer.parseInt(args[2]));
    } else {
      BamIndexer.saveBamIndex(new File(args[0]), new File(args[1]));
    }
  }

}
