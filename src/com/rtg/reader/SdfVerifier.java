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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.intervals.LongRange;

/**
 */
public final class SdfVerifier extends AbstractCli {


  @Override
  public String moduleName() {
    return "sdfverify";
  }

  @Override
  protected void initFlags() {
    mFlags.registerRequired(File.class, CommonFlags.SDF, "the SDF to be verified");
    mFlags.setValidator(VALIDATOR);
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final PrintStream outStream = new PrintStream(out);
    try {
      final File input = (File) mFlags.getAnonymousValue(0);
      //final boolean fast = flags.isSet(FAST);
      final File inleft = new File(input, "left");
      final File inright = new File(input, "right");
      if (inleft.exists() && inright.exists()) {
        if (verifyDir(inleft) && verifyDir(inright)) {
          outStream.println("\nPaired-end SDF verified okay.");
          return 0;
        } else {
          return 1;
        }
      } else if (verifyDir(input)) {
        outStream.println("\nSingle-end SDF verified okay.");
        return 0;
      } else {
        return 1;
      }
    } finally {
      outStream.flush();
    }
  }

  private static final Validator VALIDATOR = flags -> {
    final File input = (File) flags.getAnonymousValue(0);
    return CommonFlags.validateSDF(input);
  };

  /**
   * Method to verify <code>Preread</code> directory
   * @param f file
   * @return true if all okay
   * @throws IOException if an IO error occurs
   */
  public static boolean verifyDir(final File f) throws IOException {
    try (SequencesReader reader = SequencesReaderFactory.createMemorySequencesReader(f, true, true, LongRange.NONE)) {
      //reader = SequencesReaderFactory.createDefaultSequencesReader(f);
      //reader.globalIntegrity();
      return verify(reader, f);
    } catch (final IOException | RuntimeException e) {
      Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
      return false;
    }
  }

  static boolean verify(final SequencesReader reader, final File input) {
    if (reader.sdfVersion() < IndexFile.SINGLE_CHECKSUM_VERSION) {
      Diagnostic.error(ErrorType.SDF_VERSION_INVALID, input.getAbsolutePath());
      return false;
    }
    try {
      if (reader.sdfVersion() < IndexFile.SEPARATE_CHECKSUM_VERSION) {
        //System.out.println("Verifying version 2-6 checksum");
        final long checksum = reader.dataChecksum();
        final long newChecksum = calcChecksumOld(reader);
        if (newChecksum == 0) {
          return false;
        }
        if (newChecksum != checksum) {
          Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
          return false;
        }
      } else {
        //System.out.println("Verifying version 7+ data checksum");
        long checksum = reader.dataChecksum();
        long newChecksum = calcDataChecksum(reader);
        if (newChecksum == 0) {
          return false;
        }
        if (newChecksum != checksum) {
          Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
          return false;
        }

        if (reader.hasQualityData()) {
          //System.out.println("Verifying version 7+ quality checksum");
          checksum = reader.qualityChecksum();
          newChecksum = calcQualityChecksum(reader);
          if (newChecksum == 0) {
            return false;
          }
          if (newChecksum != checksum) {
            Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
            return false;
          }
        }

        //System.out.println("Verifying version 7+ names checksum");
        checksum = reader.nameChecksum();
        newChecksum = calcNameChecksum(reader);
        if (newChecksum == 0) {
          return false;
        }
        if (newChecksum != checksum) {
          Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
          return false;
        }
        //check suffix checksum
        checksum = reader.suffixChecksum();
        newChecksum = calcNameSuffixChecksum(reader);
        if (newChecksum == 0) {
          return false;
        }
        if (newChecksum != checksum) {
          Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
          return false;
        }
      }
    } catch (final Throwable e) {
      Diagnostic.error(ErrorType.SDF_VERIFICATION_FAILED);
      return false;
    }
    return true;
  }

  private static long calcChecksumOld(final SequencesReader reader) throws IOException {
    final PrereadHashFunction prf = new PrereadHashFunction();
    long totalDone = 0;
    final long totalNumberOfSeq = reader.numberSequences();
    final long totalTides = reader.totalLength();
    for (long numberOfSeq = 0; numberOfSeq < totalNumberOfSeq; ++numberOfSeq) {
      if (numberOfSeq >= totalNumberOfSeq) {
        return 0;
      }
      final int currentSeqLen = reader.length(numberOfSeq);
      // Safety for OOM condition in case of corrupt length
      if (currentSeqLen < 0 || currentSeqLen > totalTides) {
        return 0;
      }

      final byte[] data = new byte[currentSeqLen];
      final int size = reader.read(numberOfSeq, data);
      if (size != currentSeqLen) {
        return 0;
      }
      totalDone += size;
      if (totalDone > totalTides) {
        return 0;
      }
      for (final byte b : data) {
        prf.irvineHash(b);
      }
      if (reader.hasQualityData()) {
        reader.readQuality(numberOfSeq, data);
        if (size != currentSeqLen) {
          return 0;
        }
        for (final byte b : data) {
          prf.irvineHash(b);
        }
      }
      prf.irvineHash(reader.name(numberOfSeq));
    }
    try (FileInputStream seqIndexIn = new FileInputStream(SdfFileUtils.sequenceIndexFile(reader.path()))) {
      prf.irvineHash(seqIndexIn);
    }
    try (FileInputStream labIndexIn = new FileInputStream(SdfFileUtils.labelIndexFile(reader.path()))) {
      prf.irvineHash(labIndexIn);
    }
    return prf.getHash();
  }


  private static long calcDataChecksum(final SequencesReader reader) throws IOException {
    final PrereadHashFunction dataf = new PrereadHashFunction();
    final long totalNumberOfSeq = reader.numberSequences();
    final long totalTides = reader.totalLength();
    long totalDone = 0;
    for (long numberOfSeq = 0; numberOfSeq < totalNumberOfSeq; ++numberOfSeq) {
      if (numberOfSeq >= totalNumberOfSeq) {
        return 0;
      }
      final int currentSeqLen = reader.length(numberOfSeq);
      // Safety for OOM condition in case of corrupt length
      if (currentSeqLen < 0 || currentSeqLen > totalTides) {
        return 0;
      }

      final byte[] data = new byte[currentSeqLen];
      final int size = reader.read(numberOfSeq, data);
      if (size != currentSeqLen) {
        return 0;
      }
      totalDone += size;
      if (totalDone > totalTides) {
        return 0;
      }
      for (final byte b : data) {
        dataf.irvineHash(b);
      }
      dataf.irvineHash((long) currentSeqLen);
    }
    return dataf.getHash();
  }

  private static long calcQualityChecksum(final SequencesReader reader) throws IOException {
    final PrereadHashFunction qualf = new PrereadHashFunction();
    final long totalNumberOfSeq = reader.numberSequences();
    final long totalTides = reader.totalLength();
    long totalDone = 0;
    for (long numberOfSeq = 0; numberOfSeq < totalNumberOfSeq; ++numberOfSeq) {
      if (numberOfSeq >= totalNumberOfSeq) {
        return 0;
      }
      final int currentSeqLen = reader.length(numberOfSeq);
      // Safety for OOM condition in case of corrupt length
      if (currentSeqLen < 0 || currentSeqLen > totalTides) {
        return 0;
      }

      final byte[] data = new byte[currentSeqLen];
      final int size = reader.readQuality(numberOfSeq, data);
      if (size != currentSeqLen) {
        return 0;
      }
      totalDone += size;
      if (totalDone > totalTides) {
        return 0;
      }
      for (final byte b : data) {
        qualf.irvineHash(b);
      }
      qualf.irvineHash((long) currentSeqLen);
    }
    return qualf.getHash();
  }

  private static long calcNameChecksum(final SequencesReader reader) throws IOException {
    final PrereadHashFunction namef = new PrereadHashFunction();
    final long totalNumberOfSeq = reader.numberSequences();
    long numberOfSeq = -1;
    final SequencesIterator it = reader.iterator();
    it.seek(0);
    do {
      ++numberOfSeq;
      if (numberOfSeq >= totalNumberOfSeq) {
        return 0;
      }
      namef.irvineHash(it.currentName());
      namef.irvineHash(it.currentName().length());
    } while (it.nextSequence());
    return namef.getHash();
  }

  private static long calcNameSuffixChecksum(final SequencesReader reader) throws IOException {
    final PrereadHashFunction namef = new PrereadHashFunction();
    final long totalNumberOfSeq = reader.numberSequences();
    long numberOfSeq = -1;
    final SequencesIterator it = reader.iterator();
    it.seek(0);
    do {
      ++numberOfSeq;
      if (numberOfSeq >= totalNumberOfSeq) {
        return 0;
      }
      namef.irvineHash(it.currentNameSuffix());
      namef.irvineHash(it.currentNameSuffix().length());
    } while (it.nextSequence());
    return namef.getHash();
  }

  /**
   * @param args command arguments
   */
  public static void main(final String[] args) {
    new SdfVerifier().mainExit(args);
  }
}
