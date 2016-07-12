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

package com.rtg.reference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rtg.reader.ReaderUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.GeneralParser;
import com.rtg.util.Resources;
import com.rtg.util.io.FileUtils;

/**
 * format:
 * <code>
 * REF-MANIFEST
 * DESC: Some text to describe installed reference.txt file
 * CHECKS:\tNAME\t[checkType1]\t[checkType2]\t...
 * REF-TXT: [resourcePath]
 * SEQ:\t[seqName]\t[checkVal1]\t[checkVal2]\t...
 * SEQ:\t[seqName]\t[checkVal1]\t[checkVal2]\t...
 * ...
 * </code>
 */
public final class ReferenceDetector {

  private static final String CHECK_MAGIC = "checks";
  private static final String DESC_MAGIC = "desc";
  private static final String NAME_MAGIC = "name";
  private static final String SOURCE_MAGIC = "source";

  private final List<CheckType> mChecks;
  private final String mDesc;
  private final String mRefTxt;
  private final List<CheckValues> mValues;

  private ReferenceDetector(List<CheckType> checks, String refTxt, List<CheckValues> values, String desc) {
    mChecks = checks;
    mRefTxt = refTxt;
    mValues = values;
    mDesc = desc;
  }

  private abstract static class CheckType {
    protected abstract String typeName();
    protected abstract boolean checkValue(SequencesReader reader, long seqId, String checkValue) throws IOException;
    public String toString() {
      return "CheckType(" + typeName() + ")";
    }
  }

  private static final class CheckLength extends CheckType {

    private static final CheckLength SINGLETON = new CheckLength();

    private CheckLength() { }

    @Override
    protected String typeName() {
      return "LENGTH";
    }

    @Override
    protected boolean checkValue(SequencesReader reader, long seqId, String checkValue) throws IOException {
      return reader.length(seqId) == Long.parseLong(checkValue);
    }
  }

  private static final class CheckChecksum extends CheckType {

    private CheckChecksum() { }
    private static final CheckChecksum SINGLETON = new CheckChecksum();

    @Override
    protected String typeName() {
      return "CRC32";
    }

    @Override
    protected boolean checkValue(SequencesReader reader, long seqId, String checkValue) throws IOException {
      return reader.sequenceDataChecksum(seqId) == Byte.parseByte(checkValue);
    }
  }



  private static CheckType getCheckType(ReferenceManifest.CheckTypes type) {
    switch (type) {
      case NAME:
        //Name is required to look up the sequence in the first place, so it is checked as a side-effect of that
        throw new IllegalArgumentException("Name is special case");
      case LENGTH:
        return CheckLength.SINGLETON;
      case CRC32:
        return CheckChecksum.SINGLETON;
      default:
        throw new IllegalArgumentException("Unrecognized check type");
    }
  }

  private static class CheckValues {
    private final String mSeqName;
    private final List<String> mValues;
    CheckValues(String seqName, String... values) {
      mSeqName = seqName;
      mValues = Arrays.asList(values);
    }

    public String getSequenceName() {
      return mSeqName;
    }

    public int count() {
      return mValues.size();
    }

    public String getValue(int index) {
      return mValues.get(index);
    }
  }

  /**
   * Checks whether given sequences reader matches criteria from loaded manifest
   * @param sr sequences to check
   * @return true if sequences match
   * @throws IOException if an IO error occurs
   */
  public boolean checkReference(SequencesReader sr) throws IOException {
    if (sr.hasNames()) {
      final Map<String, Long> sequenceNameMap = ReaderUtils.getSequenceNameMap(sr);
      for (CheckValues cv : mValues) {
        if (!sequenceNameMap.containsKey(cv.getSequenceName())) {
          return false;
        }
        final long sequenceId = sequenceNameMap.get(cv.getSequenceName());
        for (int i = 0; i < mChecks.size(); i++) {
          final CheckType check = mChecks.get(i);
          if (!check.checkValue(sr, sequenceId, cv.getValue(i))) {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Description supplied by manifest
   * @return the description
   */
  public String getDesc() {
    return mDesc;
  }

  /**
   * Installs linked reference configuration into SDF
   * @param sr the SDF to install to
   * @throws IOException if an IO error occurs
   */
  public void installReferenceConfiguration(SequencesReader sr) throws IOException {
    final File sdfDir = sr.path();
    if (sdfDir == null) {
      throw new IOException("Could not find SDF directory");
    }
    final File refTxt = new File(sdfDir, ReferenceGenome.REFERENCE_FILE);
    if (refTxt.exists()) {
      throw new IOException(refTxt + " already exists");
    }
    try (OutputStream stream = FileUtils.createOutputStream(refTxt);
        InputStream refTxtStream = Resources.getResourceAsStream(mRefTxt)) {
        FileUtils.streamToStream(refTxtStream, stream, 4096);
    }
  }

  private static List<CheckType> loadCheckTypes(String... checks) {
    if (checks.length < 1 && !NAME_MAGIC.equals(checks[0])) {
      throw new IllegalArgumentException("Invalid CHECKS line, " + NAME_MAGIC + " must be first check type");
    }
    final List<CheckType> ret = new ArrayList<>();
    for (int i = 1; i < checks.length; i++) {
      ret.add(getCheckType(ReferenceManifest.CheckTypes.valueOf(checks[i].toUpperCase(Locale.getDefault()))));
    }
    return ret;
  }

  /**
   * Loads the manifest in the supplied stream
   * @param referenceManifest stream containing manifest. This will be closed as part of this call.
   * @return the loaded manifest
   * @throws IOException if an IO error occurs
   */
  public static ReferenceDetector loadManifest(InputStream referenceManifest) throws IOException {
    try (final ReferenceManifestParser parser = new ReferenceManifestParser(referenceManifest)) {
      parser.parse();
      return parser.getReferenceDetector();
    }
  }

  private static class ReferenceManifestParser extends GeneralParser {
    private List<CheckType> mCheckTypes = null;
    private String mSource;
    private final List<CheckValues> mValues;
    private String mDesc;
    private boolean mHeaderDone;

    ReferenceManifestParser(InputStream stream) {
      super(stream);
      mValues = new ArrayList<>();
    }

    public ReferenceDetector getReferenceDetector() {
      return new ReferenceDetector(mCheckTypes, mSource, mValues, mDesc);
    }

    @Override
    protected void parseAtLine(String key, String... elements) {
      switch (key) {
        case CHECK_MAGIC:
          mCheckTypes = loadCheckTypes(elements);
          break;
        case SOURCE_MAGIC:
          mSource = getSingleValue(key, elements);
          break;
        case DESC_MAGIC:
          mDesc = getSingleValue(key, elements);
          break;
        default:
          throw new IllegalArgumentException("Unexpected line: @" + key);
      }
      if (mCheckTypes != null && mSource != null && mDesc != null) {
        mHeaderDone = true;
      }
    }

    @Override
    protected void parseRegularLine(String... elements) {
      if (!mHeaderDone) {
        throw new IllegalArgumentException("@" + CHECK_MAGIC + ", @" + SOURCE_MAGIC + " and @" + DESC_MAGIC + " are required before table data");
      }

      if (elements.length < mCheckTypes.size() + 1) {
        throw new IllegalArgumentException("Expected " + (mCheckTypes.size() + 1) + " columns for table data");
      }
      final String name = elements[0];
      mValues.add(new CheckValues(name, Arrays.copyOfRange(elements, 1, elements.length)));
    }

    @Override
    protected void parseHashLine(String comment) {
    }
  }
}
