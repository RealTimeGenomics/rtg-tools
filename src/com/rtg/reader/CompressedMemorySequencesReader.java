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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.rtg.mode.SequenceType;
import com.rtg.util.StringUtils;
import com.rtg.util.array.ArrayUtils;
import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.array.longindex.LongChunks;
import com.rtg.util.bytecompression.BitwiseByteArray;
import com.rtg.util.bytecompression.ByteArray;
import com.rtg.util.bytecompression.ByteBaseCompression;
import com.rtg.util.bytecompression.ByteCompression;
import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;
import com.rtg.util.intervals.LongRange;

/**
 * SequencesReader which caches the entire SDF in memory.
 * This can handle DNA or protein, and the quality data (if any) is compressed as well.
 * See <code>com.rtg.reader.SdfSpeed</code> for speed benchmarking of this class.
 */
public class CompressedMemorySequencesReader extends AbstractSequencesReader implements Integrity {
  /** Maximum quality value possible in an SDF */
  public static final int MAX_QUAL_VALUE = 64;

  private static final boolean DIRECT_SDF_LOAD = true; //Boolean.valueOf(System.getProperty("direct.sdf.load", "true"));

  /**
   * Creates a sequences reader from specified dir.
   * @param dir directory containing sequence data
   * @param loadNames whether to load names from disk or not
   * @param loadFullNames whether to load full names from disk or not
   * @param region subset of the SDF to load
   * @return Sequence reader for data
   * @throws IOException if an I/O error occurs
   */
  public static SequencesReader createSequencesReader(final File dir, final boolean loadNames, boolean loadFullNames, LongRange region) throws IOException {
    return createSequencesReader(dir, loadNames, loadFullNames, region, DIRECT_SDF_LOAD);
  }

  static SequencesReader createSequencesReader(final File dir, final boolean loadNames, boolean loadFullNames, LongRange region, boolean directLoad) throws IOException {
    final IndexFile index = new IndexFile(dir);
    if (index.getSequenceType() < 0 || index.getSequenceType() > SequenceType.values().length) {
      throw new CorruptSdfException(dir);
    }
    final SequenceType type = SequenceType.values()[index.getSequenceType()];
    final int range = type.numberKnownCodes() + type.firstValid();
    if (directLoad && index.getSequenceEncoding() == IndexFile.SEQUENCE_ENCODING_COMPRESSED) {
      return new CompressedMemorySequencesReader2(dir, index, loadNames, loadFullNames, region);
    } else {
      return new CompressedMemorySequencesReader(dir, index, range, loadNames, loadFullNames, region);
    }
  }

  /**
   * Creates a sequences reader from the specified source.
   * Not optimised, intended only for testing
   * @param source the data source
   * @return Sequence reader for data
   * @throws IOException if an I/O error occurs
   */
  public static SequencesReader createSequencesReader(final SequenceDataSource source) throws IOException {
    final List<byte[]> data = new ArrayList<>();
    final List<String> labels = new ArrayList<>();
    final List<Long> counts = new ArrayList<>();
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    while (source.nextSequence()) {
      final byte[] b = new byte[source.currentLength()];
      System.arraycopy(source.sequenceData(), 0, b, 0, source.currentLength());
      data.add(b);
      labels.add(source.name());
      counts.add((long) source.currentLength());
      min = Math.min(min, source.currentLength());
      max = Math.max(max, source.currentLength());
    }
    final byte[][] dataArray = data.toArray(new byte[data.size()][]);
    final String[] labelsArray = labels.toArray(new String[labels.size()]);
    return new CompressedMemorySequencesReader(dataArray, labelsArray, ArrayUtils.asLongArray(counts), min, max, source.type());
  }

  private final File mDirectory;
  private final File mCanonicalDirectory;
  private final IndexFile mIndex;
  private final ExtensibleIndex mPositions;
  private final BitwiseByteArray mSeqData;
  private final ByteArray mChecksums;
  private final QualityLoader mQualityLoader;
  private final LongRange mRegion; // Section of the sdf to load
  protected final long mStart; // first sequence to load
  protected final long mEnd; // last sequence to load
  private final boolean mFullNamesRequested;

  // Delayed initialization
  private ByteCompression mQualityData;
  private ByteArray mQualityChecksums;
  private NamesInterface mNames;
  private NamesInterface mNameSuffixes;

  private int mReadCount = 0;    // Number of reads read out (to indicate when to perform checksum check)
  private int mQualityCount = 0; // Number of qualities read out (to indicate when to perform checksum check)

  /**
   * Shallow copy constructor.
   * @param cmsr CompressedMemorySequencesReader to copy data from
   */
  public CompressedMemorySequencesReader(final CompressedMemorySequencesReader cmsr) {
    mDirectory = cmsr.mDirectory;
    mCanonicalDirectory = cmsr.mCanonicalDirectory;
    mIndex = cmsr.mIndex;
    mPositions = cmsr.mPositions;
    mChecksums = cmsr.mChecksums;
    mSeqData = cmsr.mSeqData;
    mNames = cmsr.mNames;
    mNameSuffixes = cmsr.mNameSuffixes;
    mQualityLoader = cmsr.mQualityLoader;
    mQualityData = cmsr.mQualityData;
    mQualityChecksums = cmsr.mQualityChecksums;
    mStart = cmsr.mStart;
    mEnd = cmsr.mEnd;
    mRegion = cmsr.mRegion;
    mFullNamesRequested = cmsr.mFullNamesRequested;
  }

  /**
   * Construct directly from already prepared components
   * @param originPath path to the original source of this data
   * @param indexFile index information
   * @param seqData compressed sequence data
   * @param qualityData compressed quality data
   * @param seqChecksums sequence data checksums
   * @param qualityChecksums quality data checksums
   * @param positions logical start position of each sequence
   * @param names names of sequences
   * @param nameSuffixes suffixes of names of sequences
   * @param region region restriction
   */
  public CompressedMemorySequencesReader(File originPath, IndexFile indexFile, BitwiseByteArray seqData, ByteCompression qualityData, ByteArray seqChecksums, ByteArray qualityChecksums, ExtensibleIndex positions, NamesInterface names, NamesInterface nameSuffixes, LongRange region) {
    mDirectory = originPath;
    mCanonicalDirectory = null;
    mIndex = indexFile;
    mSeqData = seqData;
    mQualityData = qualityData;
    mChecksums = seqChecksums;
    mQualityChecksums = qualityChecksums;
    mQualityLoader = null;
    mPositions = positions;
    mNames = names;
    mNameSuffixes = nameSuffixes;
    mFullNamesRequested = mNameSuffixes != null;
    mRegion = SequencesReaderFactory.resolveRange(indexFile, region);
    mStart = mRegion.getStart();
    mEnd = mRegion.getEnd();
    assert mEnd >= mStart;
    if (mEnd > indexFile.getNumberSequences()) {
      throw new IllegalArgumentException("End sequence is greater than number of sequences in SDF");
    }
    final StringBuilder sb = new StringBuilder("CompressedMemorySequencesReader from non SDF source");
    this.infoString(sb);
    Diagnostic.developerLog(sb.toString());
  }

  /**
   * Constructor for use in tests.  Has no quality data.
   * @param data the sequence data
   * @param labels the names
   * @param counts lengths of the sequence data
   * @param min minimum length
   * @param max maximum length
   * @param type the sequence type
   */
  public CompressedMemorySequencesReader(final byte[][] data, final String[] labels, final long[] counts, final int min, final int max, final SequenceType type) {
    assert data.length == counts.length;
    assert data.length == labels.length;
    final long totalLength = ArrayUtils.sum(counts);
    mIndex = new IndexFile(Long.MAX_VALUE, type.ordinal(), totalLength, max, min, counts.length);
    mDirectory = null;
    mCanonicalDirectory = null;
    mNames = new ArrayNames(labels);
    final int range = type.numberKnownCodes() + type.firstValid();
    mSeqData = new BitwiseByteArray(totalLength, CompressedByteArray.minBits(range));
    mQualityData = null;
    mQualityChecksums = null;
    mQualityLoader = null;
    mPositions = new LongChunks(data.length + 1);
    mChecksums = ByteArray.allocate(data.length);
    mStart = 0;
    mEnd = mIndex.getNumberSequences();
    mRegion = new LongRange(mStart, mEnd);
    long pos = 0;
    final CRC32 checksum = new CRC32();
    for (int i = 0; i < counts.length; ++i) {
      mPositions.set(i, pos);
      mSeqData.set(pos, data[i], (int) counts[i]);
      pos += counts[i];
      checksum.update(data[i], 0, (int) counts[i]);
      mChecksums.set(i, (byte) checksum.getValue());
      checksum.reset();
    }
    mPositions.set(counts.length, pos);
    mFullNamesRequested = false;
    final StringBuilder sb = new StringBuilder(LS).append("CompressedMemorySequencesReader-tests");
    this.infoString(sb);
    Diagnostic.userLog(sb.toString());
  }

  protected CompressedMemorySequencesReader(File dir, IndexFile index, int range, boolean loadNames, boolean loadFullNames, LongRange region) throws IOException {
    try {
      try {
        final long starttime = System.nanoTime();
        mFullNamesRequested = loadFullNames;
        mDirectory = dir;
        mCanonicalDirectory = dir.getCanonicalFile();
        mIndex = index;
        mRegion = SequencesReaderFactory.resolveRange(index, region);
        mStart = mRegion.getStart();
        mEnd = mRegion.getEnd();
        assert mEnd >= mStart;
        if (mEnd > index.getNumberSequences()) {
          throw new IllegalArgumentException("End sequence is greater than number of sequences in SDF");
        }
        final PointerFileHandler handler = PointerFileHandler.getHandler(index, PointerFileHandler.SEQUENCE_POINTER);
        final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(index.dataIndexVersion(), dir);
        mPositions = new LongChunks(mEnd - mStart + 1);
        mChecksums = ByteArray.allocate(mPositions.length() - 1);
        final DataFileOpenerFactory openerFactory = new DataFileOpenerFactory(mIndex.getSequenceEncoding(), mIndex.getQualityEncoding(), type());
        if (mIndex.hasQuality() && mIndex.hasPerSequenceChecksums()) {
          mQualityChecksums = initQualityChecksumArray();
        }
        mQualityLoader = new QualityLoader(openerFactory, handler, mQualityChecksums);
        long dataSize = 0;
        if (mEnd - mStart > 0) {
          dataSize = SequenceDataLoader.loadPositions(mPositions, seqIndex, mStart, mEnd, dir, handler, mChecksums, mQualityChecksums);
        }
        mSeqData = new BitwiseByteArray(dataSize, CompressedByteArray.minBits(range));
        if (mEnd - mStart > 0) {
          final long hash = SequenceDataLoader.loadData(mSeqData, seqIndex, mStart, mEnd, dir, mPositions, mChecksums, openerFactory, handler, mIndex.hasPerSequenceChecksums());
          if (mIndex.getVersion() >= IndexFile.SEPARATE_CHECKSUM_VERSION && mStart == 0 && mEnd == index.getNumberSequences()) {
            if (hash != mIndex.getDataChecksum()) {
              throw new CorruptSdfException("Sequence data failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
            } else {
              Diagnostic.developerLog("Sequence data passed checksum");
            }
          }
        }
        final long stoptime = System.nanoTime();
        final double timetaken = (stoptime - starttime) / 1000000000.0;
        final int speedMB = (int) (dataSize * 1000.0 / (stoptime - starttime));
        if (loadNames && mIndex.hasNames()) {
          loadNames();
          loadNameSuffixes(loadFullNames, mIndex.hasSequenceNameSuffixes());
        }
        final StringBuilder sb = new StringBuilder("CompressedMemorySequencesReader ").append(speedMB).append(" MB/sec, time ").append(timetaken).append(" sec").append(LS);
        infoString(sb);
        Diagnostic.developerLog(sb.toString());
      } catch (final ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
        throw new CorruptSdfException();
      }
    } catch (final NegativeArraySizeException e) {
      throw new CorruptSdfException();
    }
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(checkChecksums());
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

  @Override
  public long numberSequences() {
    return mEnd - mStart;
  }

  @Override
  public File path() {
    return mDirectory;
  }

  @Override
  public void close() { } // no need to do anything

  @Override
  public NamesInterface names() {
    if (mNames == null) {
      throw new IllegalStateException("Names have not been loaded or are not present in the SDF");
    }
    return mNames;
  }
  /**
   * Load the names if they haven't already been loaded.
   * @throws IOException if an I/O related error occurs
   */
  private void loadNames() throws IOException {
    mNames = new Names(mDirectory, mRegion, false);
    if (mIndex.getVersion() >= IndexFile.SEPARATE_CHECKSUM_VERSION && mRegion.getStart() == 0 && mRegion.getEnd() == mIndex.getNumberSequences()) {
      if (mNames.calcChecksum() != mIndex.getNameChecksum()) {
        throw new CorruptSdfException("Sequence names failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
      } else {
        Diagnostic.developerLog("Sequence names passed checksum");
      }
    }
  }

  private void loadNameSuffixes(boolean attemptLoad, boolean suffixExists) throws IOException {
    mNameSuffixes = attemptLoad && suffixExists ? new Names(mDirectory, mRegion, true) : new EmptyStringNames(mRegion.getLength());
    if (attemptLoad && suffixExists) {
      if (mRegion.getStart() == 0 && mRegion.getEnd() == mIndex.getNumberSequences()) {
        if (mNameSuffixes.calcChecksum() != mIndex.getNameSuffixChecksum()) {
          throw new CorruptSdfException("Sequence name suffixes failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
        } else {
          Diagnostic.developerLog("Sequence name suffixes passed checksum");
        }
      }
    }
  }

  @Override
  public long lengthBetween(final long start, final long end) {
    return mPositions.get(end) - mPositions.get(start);
  }
  @Override
  public int[] sequenceLengths(final long start, final long end) {
    final int[] a = new int[(int) (end - start)];
    for (long i = start; i < end; ++i) {
      a[(int) (i - start)] = (int) (mPositions.get(i + 1) - mPositions.get(i));
    }
    return a;
  }

  @Override
  public SequencesReader copy() {
    return new CompressedMemorySequencesReader(this);
  }



  // Direct access methods
  @Override
  public final String name(long sequenceIndex) {
    if (!mIndex.hasNames()) {
      throw new IllegalStateException("SDF contains no name data");
    }
    return names().name(sequenceIndex);
  }

  @Override
  public final String nameSuffix(long sequenceIndex) {
    if (!mIndex.hasNames()) {
      throw new IllegalStateException("SDF contains no name data");
    }
    if (!mFullNamesRequested) {
      throw new IllegalStateException("Full names were not loaded");
    }
    return mNameSuffixes.name(sequenceIndex);
  }

  @Override
  public final int length(final long sequenceIndex) {
    return (int) (mPositions.get(sequenceIndex + 1) - mPositions.get(sequenceIndex));
  }

  @Override
  public byte sequenceDataChecksum(long sequenceIndex) throws IOException {
    return mChecksums.get(sequenceIndex);
  }

  @Override
  public int read(final long sequenceIndex, final byte[] dataOut) {
    final int length = read(sequenceIndex, dataOut, 0, length(sequenceIndex));
    ++mReadCount;
    // check every Nth (256) read for CRC corruption w.r.t. the original file.
    if ((mReadCount & 0xff) == 0) {
      checkChecksum(sequenceIndex, dataOut, length, mChecksums.get(sequenceIndex));
    }
    return length;
  }

  @Override
  public int read(final long sequenceIndex, final byte[] dataOut, final int start, final int length) {
    final int fullLength = length(sequenceIndex);
    if ((start + length) > fullLength) {
      throw new IllegalArgumentException("Requested data not a subset of sequence data.");
    }
    if (length > dataOut.length) {
      throw new IllegalArgumentException("Array too small got: " + dataOut.length + " required: " + length);
    }
    mSeqData.get(dataOut, mPositions.get(sequenceIndex) + start, length);
    return length;
  }

  @Override
  public int readQuality(final long sequenceIndex, final byte[] dest) throws IOException {
    return readQuality(sequenceIndex, dest, 0, length(sequenceIndex));
  }

  @Override
  public int readQuality(long sequenceIndex, byte[] dest, int start, int length) throws IOException {
    pullInQuality();
    if (mQualityData == null) {
      return 0;
    }
    final int fullLength = length(sequenceIndex);
    if ((start + length) > fullLength) {
      throw new IllegalArgumentException("Requested data not a subset of sequence data.");
    } else if (length > dest.length) {
      throw new IllegalArgumentException("Array too small got: " + dest.length + " required: " + length);
    }
    mQualityData.get(dest, sequenceIndex, start, length);

    // check every Nth (256) read for CRC corruption w.r.t. the original file.
    ++mQualityCount;
    if ((mQualityCount & 0xff) == 0 && length > 0) {
      checkChecksum(sequenceIndex, dest, length, mQualityChecksums.get(sequenceIndex));
    }
    return length;
  }

  void pullInQuality() throws IOException {
    if (mQualityData == null && mIndex.hasQuality()) {
      mQualityData = mQualityLoader.getQuality();
      mQualityChecksums = mQualityLoader.getQualityChecksums();
      final StringBuilder sb = new StringBuilder();
      sb.append(LS);
      sb.append("Memory Usage\tbytes\tlength").append(LS);
      infoQuality(sb);
      Diagnostic.developerLog(sb.toString());
    }
  }

  ByteArray initQualityChecksumArray() {
    return ByteArray.allocate(mPositions.length() - 1);
  }

  void infoString(final StringBuilder sb) {
    sb.append("Memory Usage\tbytes\tlength").append(LS);
    long totalBytes = 0;
    sb.append("\t\t").append(StringUtils.commas(mSeqData.bytes())).append("\t").append(StringUtils.commas(mSeqData.length())).append("\tSeqData").append(LS);
    totalBytes +=  mSeqData.bytes();
    sb.append("\t\t").append(StringUtils.commas(mChecksums.bytes())).append("\t").append(StringUtils.commas(mChecksums.length())).append("\tSeqChecksums").append(LS);
    totalBytes +=  mPositions.bytes();
    totalBytes += infoQuality(sb);
    if (mNames != null) {
      sb.append("\t\t").append(StringUtils.commas(mNames.bytes())).append("\t").append(StringUtils.commas(mNames.length())).append("\tNames").append(LS);
      totalBytes +=  mNames.bytes();
    }
    if (mNameSuffixes != null) {
      sb.append("\t\t").append(StringUtils.commas(mNameSuffixes.bytes())).append("\t").append(StringUtils.commas(mNameSuffixes.length())).append("\tSuffixes").append(LS);
      totalBytes +=  mNameSuffixes.bytes();
    }
    sb.append("\t\t").append(StringUtils.commas(mPositions.bytes())).append("\t").append(StringUtils.commas(mPositions.length())).append("\tPositions").append(LS);
    totalBytes +=  mPositions.bytes();
    sb.append("\t\t").append(StringUtils.commas(totalBytes)).append("\t\tTotal bytes").append(LS);
  }

  private long infoQuality(StringBuilder sb) {
    long qualBytes = 0;
    if (mQualityData != null) {
      sb.append("\t\t").append(StringUtils.commas(mQualityData.bytes())).append("\t").append(StringUtils.commas(mPositions.get(mPositions.length() - 1))).append("\tQualityData").append(LS);
      qualBytes +=  mQualityData.bytes();
      sb.append("\t\t").append(StringUtils.commas(mQualityChecksums.bytes())).append("\t").append(StringUtils.commas(mPositions.get(mPositions.length() - 1))).append("\tQualityChecksums").append(LS);
      qualBytes +=  mQualityChecksums.bytes();
    }
    return qualBytes;
  }

  boolean checkChecksums() {
    final CRC32 checksum = new CRC32();
    final byte[] buffer = new byte[(int) maxLength()];
    for (long i = 0; i < numberSequences(); ++i) {
      mSeqData.get(buffer, mPositions.get(i), length(i));
      checksum.reset();
      checksum.update(buffer, 0, length(i));
      if ((byte) checksum.getValue() != mChecksums.get(i)) {
        return false;
      }
      if (mQualityData != null) {
        // now check quality checksums
        mQualityData.get(buffer, i, 0, length(i));
        checksum.reset();
        checksum.update(buffer, 0, length(i));
        if ((byte) checksum.getValue() != mQualityChecksums.get(i)) {
          return false;
        }
      }
    }
    return true;
  }

  static boolean checkChecksum(final long seqId, final byte[] data, final int length, final int sum) {
    final CRC32 checksum = new CRC32();
    checksum.update(data, 0, length);
    if ((byte) checksum.getValue() == sum) {
      return true;
    } else {
      Diagnostic.userLog(String.format("CHECKSUM FAILED FOR SEQUENCE %d%n" + "EXPECTED %04X BUT WAS %04X", seqId, sum, (byte) checksum.getValue()));
      return false;
    }
  }

  int getChecksum(final long seqId) {
    return mChecksums.get(seqId);
  }

  private class QualityLoader {
    private final DataFileOpenerFactory mOpenerFactory;
    private final PointerFileHandler mHandler;

    QualityLoader(DataFileOpenerFactory fact, PointerFileHandler handler, ByteArray qualityChecksums) {
      mOpenerFactory = fact;
      mHandler = handler;
      mQualityChecksums = qualityChecksums;
    }

    // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
    volatile boolean mInit = false;
    private volatile ByteArray mQualityChecksums;
    private volatile ByteCompression mQualityData;
    private synchronized void init() throws IOException {
      if (!mInit) {
        Diagnostic.developerLog("loading quality data...");
        final boolean preloaded;
        if (mQualityChecksums == null) {
          mQualityChecksums = initQualityChecksumArray();
          preloaded = false;
        } else {
          preloaded = true;
        }
        final long dataSize = mPositions.get(mPositions.length() - 1);
        final ByteArray qualData = new CompressedByteArray(dataSize, MAX_QUAL_VALUE, false);

        final DataFileIndex seqIndex = DataFileIndex.loadSequenceDataFileIndex(mIndex.dataIndexVersion(), mDirectory);
        final long hash = SequenceDataLoader.loadQuality(qualData, seqIndex, mStart, mEnd, mDirectory, mPositions, mQualityChecksums, mOpenerFactory, mHandler, preloaded);
        if (mStart == 0 && mEnd == mIndex.getNumberSequences() && mIndex.getVersion() >= IndexFile.SEPARATE_CHECKSUM_VERSION) {
          if (hash != mIndex.getQualityChecksum()) {
            throw new CorruptSdfException("Sequence qualities failed checksum - SDF may be corrupt: \"" + mDirectory + "\"");
          } else {
            Diagnostic.developerLog("Sequence qualities passed checksum");
          }
        }
        mQualityData = new ByteBaseCompression(qualData, mPositions);
        mQualityData.freeze();
        Diagnostic.developerLog("Loaded qualities for CompressedMemorySequencesReader");
        mInit = true;
        Diagnostic.developerLog("finished loading quality data");
      }
    }
    ByteCompression getQuality() throws IOException {
      if (!mInit) {
        init();
      }
      return mQualityData;
    }
    ByteArray getQualityChecksums() throws IOException {
      if (!mInit) {
        init();
      }
      return mQualityChecksums;
    }
  }

  @Override
  public boolean compressed() {
    return mIndex.getSequenceEncoding() == IndexFile.SEQUENCE_ENCODING_COMPRESSED;
  }


  @Override
  public IndexFile index() {
    return mIndex;
  }


}
