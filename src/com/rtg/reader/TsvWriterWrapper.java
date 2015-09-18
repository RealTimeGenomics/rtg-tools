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

import static com.rtg.launcher.CommonFlags.STDIO_NAME;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.CommonFlags;
import com.rtg.mode.DnaUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;

/**
 * Wrapper for writing CG TSV format
 */
@TestClass("com.rtg.reader.Sdf2FastaTest")
public class TsvWriterWrapper implements WriterWrapper {

  private static final String[] EXTS = {".tsv"};

  private final SdfReaderWrapper mReader;

  private final LineWriter mOutput;


  /**
   * Convenience wrapper for writing.
   * @param baseOutput base output file name.
   * @param reader the reader that this writer is writing from.
   * @param gzip if true, compress the output.
   * @throws IOException if there is a problem constructing the writer.
   */
  public TsvWriterWrapper(File baseOutput, SdfReaderWrapper reader, boolean gzip) throws IOException {
    this(baseOutput, reader, gzip, EXTS);
  }

  protected TsvWriterWrapper(File baseOutput, SdfReaderWrapper reader, boolean gzip, String[] extensions) throws IOException {
    assert reader != null;
    assert extensions.length > 0;
    mReader = reader;
    if (reader.getPrereadType() != PrereadType.CG) {
      throw new NoTalkbackSlimException("Input data must be Complete Genomics format");
    }
    if (!reader.isPaired()) {
      throw new NoTalkbackSlimException("Input data is not paired-end!");
    }
    if (!reader.hasQualityData()) {
      throw new NoTalkbackSlimException("Input data does not contain quality data!");
    }
    final String readType;
    switch (reader.maxLength()) {
      case CgUtils.CG2_RAW_LENGTH:
      case CgUtils.CG2_PADDED_LENGTH:
        readType = "V2";
        break;
      case CgUtils.CG_RAW_READ_LENGTH:
        readType = "V1";
        break;
      default:
        throw new NoTalkbackSlimException("Input data doesn't look like a recognized Complete Genomics read structure");
    }

    // Strip any existing GZ or FASTA like suffix from the file name, but remember the primary type
    String output = baseOutput.toString();
    if (FileUtils.isGzipFilename(output)) {
      output = output.substring(0, output.length() - FileUtils.GZ_SUFFIX.length());
    }
    String ext = FileUtils.getExtension(output);
    if (Arrays.asList(extensions).contains(ext.toLowerCase(Locale.getDefault()))) {
      output = output.substring(0, output.lastIndexOf('.'));
    } else {
      ext = extensions[0];
    }
    mOutput = FastaWriterWrapper.getStream(CommonFlags.isStdio(output) ? STDIO_NAME : (output + ext), gzip);
    mOutput.append("#GENERATED_BY\tsdf2cg\n");
    mOutput.append("#TYPE\tREADS\n");
    mOutput.append("#READ_TYPE\t").append(readType).append("\n");
    mOutput.append("\n");
    mOutput.append(">flags\treads\tscores\n");
  }

  @Override
  public void writeSequence(long seqId, byte[] dataBuffer, byte[] qualityBuffer) throws IllegalStateException, IOException {
    mOutput.append("5\t");

    writeSeq(mReader.left(), seqId, dataBuffer);
    writeSeq(mReader.right(), seqId, dataBuffer);

    mOutput.append('\t');

    writeQuality(mReader.left(), seqId, dataBuffer);
    writeQuality(mReader.right(), seqId, dataBuffer);

    mOutput.writeln();
  }

  private void writeSeq(SequencesReader reader, long seqId, byte[] buffer) throws IOException {
    final int length = reader.read(seqId, buffer);
    if (length == CgUtils.CG2_RAW_LENGTH) {
      mOutput.write(DnaUtils.bytesToSequenceIncCG(buffer, 0, CgUtils.CG2_PAD_POSITION));
      mOutput.write('N');
      mOutput.write(DnaUtils.bytesToSequenceIncCG(buffer, CgUtils.CG2_PAD_POSITION, CgUtils.CG2_RAW_LENGTH - CgUtils.CG2_PAD_POSITION));
    } else {
      mOutput.write(DnaUtils.bytesToSequenceIncCG(buffer, 0, length));
    }
  }

  private void writeQuality(SequencesReader reader, long seqId, byte[] buffer) throws IOException {
    final int length = reader.readQuality(seqId, buffer);
    if (length == CgUtils.CG2_RAW_LENGTH) {
      mOutput.write(FastaUtils.rawToAsciiString(buffer, 0, CgUtils.CG2_PAD_POSITION));
      mOutput.write('!');
      mOutput.write(FastaUtils.rawToAsciiString(buffer, CgUtils.CG2_PAD_POSITION, CgUtils.CG2_RAW_LENGTH - CgUtils.CG2_PAD_POSITION));
    } else {
      mOutput.write(FastaUtils.rawToAsciiString(buffer, 0, length));
    }
  }

  @Override
  public void close() throws IOException {
    if (mOutput != null) {
      mOutput.close();
    }
  }
}

