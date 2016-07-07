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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rtg.reference.ReferenceGenome;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.io.FileUtils;

/**
 * format:
 * <code>
 * REF-MANIFEST
 * CHECKS:\tNAME\t[checkType1]\t[checkType2]\t...
 * REF-TXT: [resourcePath]
 * SEQ:\t[seqName]\t[checkVal1]\t[checkVal2]\t...
 * SEQ:\t[seqName]\t[checkVal1]\t[checkVal2]\t...
 * ...
 * </code>
 */
public final class ReferenceDetector {

  private static final String MAGIC_LINE = "REF-MANIFEST";
  private static final String CHECK_MAGIC = "CHECKS:";
  private static final String NAME_MAGIC = "NAME";
  private static final String REF_TXT_MAGIC = "REF-TXT:";
  private static final String SEQ_MAGIC = "SEQ:";

  private final List<CheckType> mChecks;
  private final String mRefTxt;
  private final List<CheckValues> mValues;

  private ReferenceDetector(List<CheckType> checks, String refTxt, List<CheckValues> values) {
    mChecks = checks;
    mRefTxt = refTxt;
    mValues = values;
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

  private enum CheckTypes {
    LENGTH {
      @Override
      CheckType type() {
        return CheckLength.SINGLETON;
      }
    },
    CRC32 {
      @Override
      CheckType type() {
        return null;
      }
    };

    abstract CheckType type();
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
    try (OutputStream stream = FileUtils.createOutputStream(refTxt)) {
      final InputStream refTxtStream = Resources.getResourceAsStream(mRefTxt);
      FileUtils.streamToStream(refTxtStream, stream, 4096);
    }
  }

  private static List<CheckType> loadCheckTypes(String checksLine) {
    if (checksLine != null) {
      final String[] split = StringUtils.split(checksLine, '\t');
      if (split.length < 1 && !CHECK_MAGIC.equals(split[0])) {
        throw new IllegalArgumentException("Invalid CHECKS line in reference manifest");
      }
      if (split.length < 2 && !NAME_MAGIC.equals(split[1])) {
        throw new IllegalArgumentException("Invalid CHECKS line, " + NAME_MAGIC + " must be first check type");
      }
      final List<CheckType> ret = new ArrayList<>();
      for (int i = 2; i < split.length; i++) {
        ret.add(CheckTypes.valueOf(split[i]).type());
      }
      return ret;
    }
    throw new NullPointerException("Expected CHECKS line in reference manifest");
  }

  /**
   * Loads the manifest in the supplied stream
   * @param referenceManifest stream containing manifest. This will be closed as part of this call.
   * @return the loaded manifest
   * @throws IOException if an IO error occurs
   */
  public static ReferenceDetector loadManifest(InputStream referenceManifest) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(referenceManifest))) {
      final String magicLine = reader.readLine();
      if (!MAGIC_LINE.equals(magicLine)) {
        throw new IOException("Invalid reference manifest");
      }
      final String checkTypesLine = reader.readLine();
      final List<CheckType> checkTypes;
      try {
        checkTypes = loadCheckTypes(checkTypesLine);
      } catch (IllegalArgumentException | NullPointerException e) {
        throw new IOException(e.getMessage());
      }
      final String refTxtLine = reader.readLine();
      if (refTxtLine == null) {
        throw new IOException("Unexpected end of reference manifest");
      }
      final String[] refTxtSplit = StringUtils.split(refTxtLine, '\t');
      if (refTxtSplit.length != 2 || !REF_TXT_MAGIC.equals(refTxtSplit[0])) {
        throw new IOException("Illegal " + REF_TXT_MAGIC + " line: '" + refTxtLine + "'");
      }
      final String refTxt = refTxtSplit[1];
      final List<CheckValues> seqCheckValues = new ArrayList<>();
      String seqLine;
      while ((seqLine = reader.readLine()) != null) {
        final String[] seqLineSplit = StringUtils.split(seqLine, '\t');
        //magic, seqName, otherChecks
        if (seqLineSplit.length != checkTypes.size() + 2 || !SEQ_MAGIC.equals(seqLineSplit[0])) {
          throw new IOException("Invalid reference manifest line: '" + seqLine + "'");
        }
        final CheckValues v = new CheckValues(seqLineSplit[1], Arrays.copyOfRange(seqLineSplit, 2, seqLineSplit.length));
        seqCheckValues.add(v);
      }
      return new ReferenceDetector(checkTypes, refTxt, seqCheckValues);
    }
  }
}
