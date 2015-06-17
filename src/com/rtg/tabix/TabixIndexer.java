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

package com.rtg.tabix;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.rtg.tabix.SequenceIndex.SequenceIndexChunk;
import com.rtg.util.io.ByteArrayIOUtils;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;

/**
 * Indexes files for use with <code>TABIX</code>
 */
public final class TabixIndexer {

  /** Maximum indexable position */
  public static final int MAXIMUM_REFERENCE_LENGTH = 512 * 1024 * 1024;

  /** Extension used on tabix files */
  public static final String TABIX_EXTENSION = ".tbi";

  private static final boolean DEBUG = false; //Boolean.valueOf(System.getProperty("dave.debug.index", "false"));

  private static final Pattern BED_SKIP_LINES = Pattern.compile("^(#|track\\s|browser\\s).*$");

  static final int META_BIN = 37450;

  private final IndexerInputHandler mInputHandler;


  /**
   * @param input file to index must be a BGZIP compressed file
   */
  public TabixIndexer(File input) {
    this(input, new File(input.getParentFile(), input.getName() + TABIX_EXTENSION));
  }

  /**
   * @param input file to index must be a BGZIP compressed file
   * @param index output file for index
   */
  public TabixIndexer(File input, File index) {
    this(new FileIndexerInputHandler(input, index));
  }

  /**
   * @param input data file stream, must be BGZIP compressed
   * @param index output stream for index
   */
  public TabixIndexer(InputStream input, OutputStream index) {
    this(new StreamIndexerInputHandler(input, index));
  }

  TabixIndexer(IndexerInputHandler handler) {
    mInputHandler = handler;
  }

  /**
   * Get a human readable error message when an index could not be created
   * @param outFile the file that we attempted to index
   * @param e the exception that was raised
   * @return the description of what went wrong
   */
  public static String getTabixWarningMessage(File outFile, UnindexableDataException e) {
    return "Cannot produce TABIX index for: " + outFile.getPath() + ": " + e.getMessage();
  }

  /**
   * Creates a <code>TABIX</code> index for given SAM file and saves it.
   * @throws IOException if an IO Error occurs.
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public void saveSamIndex() throws IOException, UnindexableDataException {
    saveIndex(new SamIndexerFactory());
  }

  /**
   * Creates a <code>TABIX</code> index for given TSV file and saves it.
   * @throws IOException if an IO Error occurs.
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public void saveTsvIndex() throws IOException, UnindexableDataException {
    saveIndex(new TsvIndexerFactory());
  }

  /**
   * Creates a <code>TABIX</code> index for given VCF file and saves it.
   * @throws IOException if an IO Error occurs.
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public void saveVcfIndex() throws IOException, UnindexableDataException {
    saveIndex(new VcfIndexerFactory());
  }

  /**
   * Creates a <code>TABIX</code> index for given AC file and saves it.
   * @throws IOException if an IO Error occurs.
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public void saveAlleleCountsIndex() throws IOException, UnindexableDataException {
    saveIndex(new AlleleCountsIndexerFactory());
  }

  /**
   * Creates a <code>TABIX</code> index for given BED file and saves it.
   * @throws IOException if an IO Error occurs.
   * @throws UnindexableDataException If data cannot be indexed because of properties of the data
   */
  public void saveBedIndex() throws IOException, UnindexableDataException {
    int skip = 0;
    //by setting true we indicate we want to be able to read at least a portion from the start of the inputstream without affecting the main run
    mInputHandler.start(true);
    try {
      final BlockCompressedLineReader bcli = new BlockCompressedLineReader(new BlockCompressedInputStream(mInputHandler.getInputStream()));
      String line;
      while ((line = bcli.readLine()) != null) {
        if (BED_SKIP_LINES.matcher(line).matches()) {
          skip++;
        } else {
          break;
        }
      }
    } finally {
      //resets the stream
      mInputHandler.close();
    }
    saveIndex(new BedIndexerFactory(skip));
  }

  void saveIndex(IndexerFactory factory) throws IOException, UnindexableDataException {
    saveIndex(mInputHandler, factory);
  }

  static void saveIndex(IndexerInputHandler handler, IndexerFactory fact) throws IOException, UnindexableDataException {
    try {
      handler.start(false);
      saveIndex(handler.getInputStream(), handler.getOutputStream(), fact);
    } catch (final UnindexableDataException e) {
      handler.unindexable();
      throw e;
    } finally {
      handler.close();
    }
    handler.finish();
  }

  static void saveIndex(InputStream is, OutputStream os, IndexerFactory fact) throws IOException, UnindexableDataException {
    final TabixOptions ops = fact.getOptions();
    try (BlockCompressedPositionReader in = fact.getReader(is)) {
      saveTabixIndex(in, os, ops);
    }

  }

  static void saveTabixIndex(BlockCompressedPositionReader in, final OutputStream samIndex, TabixOptions ops) throws IOException, UnindexableDataException {
    final ArrayList<SequenceIndex> indexs = new ArrayList<>();
    populateIndex(indexs, in, false);
    mergeChunks(indexs);
    try (BlockCompressedOutputStream indexOut = new BlockCompressedOutputStream(samIndex, null)) {
      writeIndex(indexs, ops, in.getSequenceNames(), indexOut);
    }
  }

  /**
   * Populate the index
   * @param indexes sequence index list to read into
   * @param input input reader to read from
   * @param addExtraFields true to add... meta bin chunks?
   * @return the total number of unmapped input records
   * @throws IOException if an exception occurs while reading
   * @throws UnindexableDataException if the data wasn't able to be indexed (unsorted, sequence too long)
   */
  public static long populateIndex(final List<SequenceIndex> indexes, final BlockCompressedPositionReader input, boolean addExtraFields) throws IOException, UnindexableDataException {
    long chunkBegin = -1;
    int lastRefId = -2; //-2 for beginning since -1 -> record with no reference
    int lastBin = -1;
    boolean first = true;
    int minBin = -1;
    int lastPos = -1; //for sorting test
    long totalUnmapped = 0;
    long refUnmapped = 0;
    long refMapped = 0;
    long refBegin = -1;
    while (input.hasNext()) {
      input.next();
      if (DEBUG) {
        debugRecord(input);
      }
      final boolean unmapped = input.isUnmapped();
      if (unmapped) {
        totalUnmapped++;
      }
      if (!unmapped || input.hasReference()) {
        final int refId = input.getReferenceId();
        if (lastRefId != refId) {
          if (lastRefId != -2) {
            if ((lastRefId > refId && refId != -1) || (lastRefId == -1 && refId >= 0)) {
              //-1 is > other id's for the purposes of indexing
              throw new UnindexableDataException("File is not sorted");
            }
            final long chunkEnd = input.getVirtualOffset();
            indexes.get(lastRefId).addChunk(lastBin, chunkBegin, chunkEnd);
            if (addExtraFields) {
              indexes.get(lastRefId).addChunk(META_BIN, refBegin, chunkEnd);
              indexes.get(lastRefId).addChunk(META_BIN, refMapped, refUnmapped);
            }
          }
          refBegin = input.getVirtualOffset();
          refMapped = 0;
          refUnmapped = 0;
          lastPos = -1;
          lastBin = -1;
          lastRefId = refId;
          while (indexes.size() <= lastRefId) {
            indexes.add(new SequenceIndex());
          }
          minBin = -1;
        }
        if (unmapped) {
          refUnmapped++;
        } else {
          refMapped++;
        }
      } else {
        if (lastRefId >= 0) {
          //no reference available, must be in unmapped territory
          final long chunkEnd = input.getVirtualOffset();
          indexes.get(lastRefId).addChunk(lastBin, chunkBegin, chunkEnd);
          if (addExtraFields) {
            indexes.get(lastRefId).addChunk(META_BIN, refBegin, chunkEnd);
            indexes.get(lastRefId).addChunk(META_BIN, refMapped, refUnmapped);
          }
          lastRefId = -1;
        }
      }
      if (!unmapped || input.hasCoordinates()) {
        final int bin = input.getBinNum();
        final int pos = input.getStartPosition();
        final int len = input.getLengthOnReference();
        if (lastPos > pos) {
          throw new UnindexableDataException("File is not sorted");
        }
        lastPos = pos;
        if (pos > MAXIMUM_REFERENCE_LENGTH || pos + len > MAXIMUM_REFERENCE_LENGTH) {
          throw new UnindexableDataException("maximum reference sequence length is exceeded");
        }
        final int linearLastBin = setLinearIndex(indexes.get(lastRefId), pos, len, input.getVirtualOffset(), minBin);
        if (first) {
          minBin = linearLastBin;
        }
        if (lastBin != bin) {
          final long chunkEnd = input.getVirtualOffset();
          if (lastBin != -1) {
            indexes.get(lastRefId).addChunk(lastBin, chunkBegin, chunkEnd);
          }
          lastBin = bin;
          chunkBegin = chunkEnd;
        }
      }
      first = false;
    }
    if (lastRefId >= 0) {
      indexes.get(lastRefId).addChunk(lastBin, chunkBegin, input.getNextVirtualOffset());
      if (addExtraFields) {
        indexes.get(lastRefId).addChunk(META_BIN, refBegin, input.getNextVirtualOffset());
        indexes.get(lastRefId).addChunk(META_BIN, refMapped, refUnmapped);
      }
    }
    return totalUnmapped;
  }

  private static final int LINEAR_SHIFT = 14;

  private static int setLinearIndex(final SequenceIndex index, final int begin, final int len, final long virtualOffset, int minBin) {
    final int b = begin >> LINEAR_SHIFT;
    final int e = (begin + len - 1) >> LINEAR_SHIFT;
    for (int i = b; i <= e; i++) {
      index.setLinearIndex(i, virtualOffset, minBin);
    }
    return e;
  }

  /**
   * Merge chunks
   * @param indexes the index containing chunks to merge
   */
  public static void mergeChunks(final List<SequenceIndex> indexes) {
    for (final SequenceIndex index : indexes) {
      for (final Map.Entry<Integer, ArrayList<SequenceIndexChunk>> entry : index.getBins().entrySet()) {
        final ArrayList<SequenceIndexChunk> list = entry.getValue();
        if (entry.getKey() == META_BIN) {
          continue;
        }
        int current = 0;
        SequenceIndexChunk currentChunk = list.get(current);
        for (int j = 1; j < list.size(); ) {
          final SequenceIndexChunk tempChunk = list.get(j);
          if (currentChunk.mChunkEnd >> 16 == tempChunk.mChunkBegin >> 16) {
            currentChunk.mChunkEnd = tempChunk.mChunkEnd;
            list.remove(j);
          } else {
            current++;
            currentChunk = list.get(current);
            j++;
          }
        }
      }
    }
  }


  /**
   * Saves given <code>TABIX</code> index to given stream
   * @param indexs index to write
   * @param ops supplies the tabix indexing configuration
   * @param names the list of sequence names
   * @param output destination
   * @throws IOException if an IO Error occurs
   */
  static void writeIndex(final List<SequenceIndex> indexs, TabixOptions ops, List<String> names, final BlockCompressedOutputStream output) throws IOException {

    final byte[] buf = new byte[1024];
    //magic
    final byte[] header = {(byte) 'T', (byte) 'B', (byte) 'I', 1};
    output.write(header);
    //no of sequences
    ByteArrayIOUtils.intToBytesLittleEndian(indexs.size(), buf, 0);
    output.write(buf, 0, 4);
    //format
    ByteArrayIOUtils.intToBytesLittleEndian(ops.mFormat, buf, 0);
    output.write(buf, 0, 4);
    //col_seq
    ByteArrayIOUtils.intToBytesLittleEndian(ops.mSeqCol + 1, buf, 0);
    output.write(buf, 0, 4);
    //col_beg
    ByteArrayIOUtils.intToBytesLittleEndian(ops.mStartCol + 1, buf, 0);
    output.write(buf, 0, 4);
    //col_end
    ByteArrayIOUtils.intToBytesLittleEndian(ops.mEndCol + 1, buf, 0);
    output.write(buf, 0, 4);
    //meta
    ByteArrayIOUtils.intToBytesLittleEndian(ops.mMeta, buf, 0);
    output.write(buf, 0, 4);
    //skip
    ByteArrayIOUtils.intToBytesLittleEndian(ops.mSkip, buf, 0);
    output.write(buf, 0, 4);
    //length of concatenated sequence names
    int totalLength = 0;
    for (final String s : names) {
      totalLength += s.getBytes().length + 1;
    }
    ByteArrayIOUtils.intToBytesLittleEndian(totalLength, buf, 0);
    output.write(buf, 0, 4);
    //sequences names, 0 terminated
    for (final String s : names) {
      output.write(s.getBytes());
      output.write(0);
    }

    for (final SequenceIndex bai : indexs) {
      ByteArrayIOUtils.intToBytesLittleEndian(bai.getBins().size(), buf, 0);
      output.write(buf, 0, 4);
      for (final Map.Entry<Integer, ArrayList<SequenceIndexChunk>> entry : bai.getBins().entrySet()) {
        ByteArrayIOUtils.intToBytesLittleEndian(entry.getKey(), buf, 0);
        output.write(buf, 0, 4);
        final ArrayList<SequenceIndexChunk> chunks = entry.getValue();
        ByteArrayIOUtils.intToBytesLittleEndian(chunks.size(), buf, 0);
        output.write(buf, 0, 4);
        writeChunks(output, chunks, buf);
      }
      ByteArrayIOUtils.intToBytesLittleEndian(bai.getLinearSize(), buf, 0);
      output.write(buf, 0, 4);
      for (int j = 0; j < bai.getLinearSize(); j++) {
        ByteArrayIOUtils.longToBytesLittleEndian(bai.getLinearIndex(j), buf, 0);
        output.write(buf, 0, 8);
      }
    }
  }

  /**
   * Write chunks to output
   * @param output the output stream to write to
   * @param chunks the sequence index chunks to write
   * @param buf a buffer
   * @throws IOException if an exception occurs while writing
   */
  public static void writeChunks(final OutputStream output, final List<SequenceIndexChunk> chunks, final byte[] buf) throws IOException {
    for (final SequenceIndexChunk currentChunk : chunks) {
      ByteArrayIOUtils.longToBytesLittleEndian(currentChunk.mChunkBegin, buf, 0);
      ByteArrayIOUtils.longToBytesLittleEndian(currentChunk.mChunkEnd, buf, 8);
      output.write(buf, 0, 16);
    }
  }

  private static void debugRecord(BlockCompressedPositionReader input) {
    System.err.println("Position: " + virtualOffsetString(input.getVirtualOffset()) + "-" + virtualOffsetString(input.getNextVirtualOffset()) + " Bin: " + input.getBinNum() + " refId: " + input.getReferenceId() + " pos: " + input.getStartPosition());
  }

  private static String virtualOffsetString(long l) {
    return "(" + (l >> 16) + ", " + (l & 0xFFFF) + ")";
  }

  static class TabixOptions {
    public static final int FORMAT_GENERIC = 0;
    public static final int FORMAT_SAM = 1;
    public static final int FORMAT_VCF = 2;
    int mFormat;
    int mSeqCol;
    int mStartCol;
    int mEndCol;
    int mMeta;
    int mSkip;
    boolean mZeroBased;

    /**
     * @param format format constant for data to be indexed.
     * @param seqCol 0-based column number containing reference sequence name
     * @param startCol 0-based column number containing reference start position
     * @param endCol 0-based column number containing reference end position
     * @param meta character indicating non data line in input.
     * @param skip number of lines to skip at beginning.
     * @param positionStyle true for <code>BED</code> style (0-based half-closed half-open), false for <code>GFF</code> style (1-based closed).
     */
    public TabixOptions(int format, int seqCol, int startCol, int endCol, char meta, int skip, boolean positionStyle) {
      mFormat = format | (positionStyle ? 0x10000 : 0);
      mSeqCol = seqCol;
      mStartCol = startCol;
      mEndCol = endCol;
      mMeta = (int) meta;
      mSkip = skip;
      mZeroBased = positionStyle;
    }

    /**
     * @param format format as it should be written in file (i.e. including position style)
     * @param seqCol 0-based column number containing reference sequence name
     * @param startCol 0-based column number containing reference start position
     * @param endCol 0-based column number containing reference end position
     * @param meta character indicating non data line in input.
     * @param skip number of lines to skip at beginning.
     */
    public TabixOptions(int format, int seqCol, int startCol, int endCol, int meta, int skip) {
      mFormat = format;
      mSeqCol = seqCol;
      mStartCol = startCol;
      mEndCol = endCol;
      mMeta = meta;
      mSkip = skip;
      mZeroBased = (mFormat & 0x10000) != 0;
    }
  }

  /**
   * Calculate bin given an alignment covering <code>[beg, end)</code>
   * @param beg start position (0-based)
   * @param end end position (0-based exclusive)
   * @return bin number
   */
  public static int reg2bin(int beg, int end) {
    final int actualEnd = end - 1;
    if (beg >> 14 == actualEnd >> 14) {
      return ((1 << 15) - 1) / 7 + (beg >> 14);
    }
    if (beg >> 17 == actualEnd >> 17) {
      return ((1 << 12) - 1) / 7 + (beg >> 17);
    }
    if (beg >> 20 == actualEnd >> 20) {
      return ((1 << 9) - 1) / 7 + (beg >> 20);
    }
    if (beg >> 23 == actualEnd >> 23) {
      return ((1 << 6) - 1) / 7 + (beg >> 23);
    }
    if (beg >> 26 == actualEnd >> 26) {
      return ((1 << 3) - 1) / 7 + (beg >> 26);
    }
    return 0;
  }

  /**
   * check if given file is block compressed
   * @param file file to check
   * @return true iff file is block compressed
   * @throws IOException if an IO error occurs
   */
  public static boolean isBlockCompressed(File file) throws IOException {
    final boolean result;
    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
      result = BlockCompressedInputStream.isValidFile(bis);
    }
    return result;
  }

  /**
   * Confirm given file is block compressed
   * @param input file to check
   * @throws IOException if an IO error occurs
   * @throws IllegalArgumentException if file is not block compressed format
   */
  private static void checkIsBlockCompressed(File input) throws IOException {
    if (!isBlockCompressed(input)) {
      throw new IllegalArgumentException("File: " + input.getPath() + " is not in block compressed gzip format");
    }
  }

  /** A factory used encapsulate tabix indexer options for a particular file format */
  public abstract static class IndexerFactory {
    protected final int mSkip;

    /**
     * Constructor
     * @param skip the number of initial lines to skip.
     */
    public IndexerFactory(int skip) {
      mSkip = skip;
    }

    abstract TabixOptions getOptions();
    abstract BlockCompressedPositionReader getReader(InputStream is) throws IOException;
  }

  static class BedIndexerFactory extends IndexerFactory {

    public BedIndexerFactory(int skip) {
      super(skip);
    }

    @Override
    public TabixOptions getOptions() {
      return new TabixOptions(TabixOptions.FORMAT_GENERIC, 0, 1, 2, '#', mSkip, true);
    }

    @Override
    public BlockCompressedPositionReader getReader(InputStream is) throws IOException {
      return new GenericPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(is)), getOptions());
    }
  }

  static class TsvIndexerFactory extends IndexerFactory {
    public TsvIndexerFactory() {
      super(0);
    }
    @Override
    public TabixOptions getOptions() {
      return new TabixOptions(TabixOptions.FORMAT_GENERIC, 0, 1, 1, '#', mSkip, false);
    }

    @Override
    public BlockCompressedPositionReader getReader(InputStream is) throws IOException {
      return new GenericPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(is)), getOptions());
    }
  }

  /** Indexer settings for VCF files */
  public static class VcfIndexerFactory extends IndexerFactory {
    /** Constructor, of course. */
    public VcfIndexerFactory() {
      super(0);
    }
    @Override
    public TabixOptions getOptions() {
      return new TabixOptions(TabixOptions.FORMAT_VCF, 0, 1, -1, '#', mSkip, false);
    }

    @Override
    public BlockCompressedPositionReader getReader(InputStream is) throws IOException {
      return new VcfPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(is)), mSkip);
    }
  }

  /** Indexer settings for SAM files */
  public static class SamIndexerFactory extends IndexerFactory {
    /** Constructor, of course. */
    public SamIndexerFactory() {
      super(0);
    }
    @Override
    public TabixOptions getOptions() {
      return new TabixOptions(TabixOptions.FORMAT_SAM, 2, 3, -1, '@', mSkip, false);
    }

    @Override
    public BlockCompressedPositionReader getReader(InputStream is) throws IOException {
      return new SamPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(is)), mSkip);
    }
  }

  static class AlleleCountsIndexerFactory extends IndexerFactory {
    public AlleleCountsIndexerFactory() {
      super(0);
    }
    @Override
    public TabixOptions getOptions() {
      return new TabixOptions(TabixOptions.FORMAT_VCF, 0, 1, -1, '#', mSkip, false);
    }

    @Override
    public BlockCompressedPositionReader getReader(InputStream is) throws IOException {
      return new AlleleCountsPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(is)), mSkip);
    }
  }

  /**
   * Get the name an index should have for a given data file
   * @param data the file we want to index
   * @return the file name the index should have.
   */
  public static File indexFileName(File data) {
    return new File(data.getParentFile(), data.getName() + TABIX_EXTENSION);
  }

  static boolean isBedSkipLine(String line) {
    return BED_SKIP_LINES.matcher(line).matches();
  }

  private interface IndexerInputHandler {
    /**
     * Called before accessing input stream or output stream.
     * @param prelim indicates we want preliminary access to the input stream (i.e. start will be called again). We don't have to guarantee that the entire stream may be read
     * @throws IOException if an IO error occurs
     */
    void start(boolean prelim) throws IOException;
    InputStream getInputStream();
    OutputStream getOutputStream();
    void close() throws IOException;
    void finish();
    void unindexable() throws IOException;
  }

  private static class FileIndexerInputHandler implements IndexerInputHandler {
    private final File mDataFile;
    private final File mOutputIndexFile;
    private InputStream mDataStream;
    private OutputStream mIndexStream;

    public FileIndexerInputHandler(File dataFile, File indexFile) {
      this.mDataFile = dataFile;
      this.mOutputIndexFile = indexFile;
    }

    @Override
    public void start(boolean prelim) throws IOException {
      if (!prelim) {
        if (!(mDataFile.length() == 0)) {
          checkIsBlockCompressed(mDataFile);
        }
        mIndexStream = new FileOutputStream(mOutputIndexFile);
      }
      mDataStream = new FileInputStream(mDataFile);
    }

    @Override
    public void close() throws IOException {
      try {
        if (mDataStream != null) {
          mDataStream.close();
        }
      } finally {
        if (mIndexStream != null) {
          mIndexStream.close();
        }
      }
    }

    @Override
    public void finish() {
    }

    @Override
    public InputStream getInputStream() {
      return mDataStream;
    }

    @Override
    public OutputStream getOutputStream() {
      return mIndexStream;
    }

    @Override
    public void unindexable() throws IOException {
      close();
      if (mOutputIndexFile.exists() && !mOutputIndexFile.delete()) {
        throw new IOException("Could not create index for \"" + mDataFile.getPath() + "\" and could not delete invalid partial index file \"" + mOutputIndexFile + "\"");
      }
    }
  }

  private static class StreamIndexerInputHandler implements IndexerInputHandler {
    private InputStream mDataStream;
    private final OutputStream mIndexStream;
    private boolean mPrelim;

    public StreamIndexerInputHandler(InputStream inData, OutputStream outIndex) {
      this.mDataStream = inData;
      this.mIndexStream = outIndex;
    }
    @Override
    public void finish() {
    }

    @Override
    public void close() throws IOException {
      if (mPrelim) {
        mDataStream.reset();
      }
    }

    @Override
    public InputStream getInputStream() {
      return mDataStream;
    }

    @Override
    public OutputStream getOutputStream() {
      return mIndexStream;
    }

    @Override
    public void start(boolean prelim) {
      if (prelim) {
        if (!mDataStream.markSupported()) {
          mDataStream = new BufferedInputStream(mDataStream);
        }
        mDataStream.mark(1024 * 1024);
      }
      mPrelim = prelim;
    }

    @Override
    public void unindexable() {
    }

  }
}
