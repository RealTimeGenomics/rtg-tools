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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reader.ReaderUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.Resources;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.io.FileUtils;

/**
 * Generate a reference manifest from an SDF an reference description file
 */
public class GenerateReferenceManifest extends AbstractCli {

  private static final ReferenceManifest.CheckTypes[] CHECKS = {
    ReferenceManifest.CheckTypes.NAME,
    ReferenceManifest.CheckTypes.LENGTH
  };
  private static final String DESCRIPTION_FLAG = "description";

  private interface GetCheckValue {
    String getValue(SequencesReader reader, long seqId) throws IOException;
  }

  private static GetCheckValue checkValueFactory(ReferenceManifest.CheckTypes type) {
    switch (type) {
      case NAME:
        //name is required to look up the sequence, so it is found from there
        throw new IllegalArgumentException("Special case, shouldn't be used here");
      case LENGTH:
        return new GetCheckValue() {
          @Override
          public String getValue(SequencesReader reader, long seqId) throws IOException {
            return Integer.toString(reader.length(seqId));
          }
        };
      case CRC32:
        return new GetCheckValue() {
          @Override
          public String getValue(SequencesReader reader, long seqId) throws IOException {
            return Byte.toString(reader.sequenceDataChecksum(seqId));
          }
        };
      default:
        throw new RuntimeException("Unregcognized value");
    }
  }

  /**
   * Generates a reference manifest from an SDF an reference description file
   * @param sdfDir directory of reference SDF
   * @param refTxtResource resource path to reference description file
   * @param writer writer to write output reference manifest
   * @param description description for the manifest
   * @throws IOException if an IO error occurs
   */
  public static void generateManifest(File sdfDir, String refTxtResource, Writer writer, String description) throws IOException {
    final String descUse = description != null ? description : ("Generated from " + sdfDir);
    try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(sdfDir)) {
      if (reader.hasNames() && reader.numberSequences() <= ReferenceManifest.MAX_SEQUENCE_NAMES) {
        try (InputStreamReader refReader = new InputStreamReader(Resources.getResourceAsStream(refTxtResource))) {
          final Map<String, Long> names = ReaderUtils.getSequenceNameMap(reader);
          final ReferenceGenome ref = new ReferenceGenome(reader, refReader, Sex.MALE);
          writer.write("#ref-manifest v2.0\n");
          writer.write("@desc\t" + descUse + "\n");
          writer.write("@source\t" + refTxtResource + "\n");
          writer.write("@checks\t" + Arrays.stream(CHECKS).map(t -> t.name().toLowerCase(Locale.getDefault())).collect(Collectors.joining("\t")) + "\n");
          final GetCheckValue[] getters = new GetCheckValue[CHECKS.length - 1];
          for (int i = 0; i < CHECKS.length - 1; i++) {
            getters[i] = checkValueFactory(CHECKS[i + 1]);
          }
          for (ReferenceSequence sequence : ref.sequences()) {
            if (sequence.isSpecified()) {
              final long seqId = names.get(sequence.name());
              writer.write(sequence.name());
              for (int i = 0; i < getters.length; i++) {
                writer.write("\t");
                writer.write(getters[i].getValue(reader, seqId));
              }
              writer.write("\n");
            }
          }
        }
      }
    }
  }

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  private static void initFlags(CFlags flags) {
    flags.registerExtendedHelp();
    flags.registerRequired('t', CommonFlags.TEMPLATE_FLAG, File.class, CommonFlags.SDF, "Reference to generate manifest for").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerRequired(String.class, CommonFlags.STRING, "Resource name of reference description").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, CommonFlags.FILE, "Output file name or - for standard out").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerOptional('d', DESCRIPTION_FLAG, String.class, "STRING", "description for manifest");
  }

  private static Writer getFileWriter(File out) throws IOException {
    if (FileUtils.isStdio(out)) {
      return new OutputStreamWriter(FileUtils.getStdoutAsOutputStream());
    }
    return new FileWriter(out);

  }
  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File outFile = (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
    try (Writer writer = getFileWriter(outFile)) {
      generateManifest((File) mFlags.getValue(CommonFlags.TEMPLATE_FLAG), (String) mFlags.getAnonymousValue(0), writer, (String) mFlags.getValue(DESCRIPTION_FLAG));
    }
    return 0;
  }

  @Override
  public String moduleName() {
    return "generate-reference-manifest";
  }

  /**
   * @param args command line arguments
   */
  public static void main(String[] args) {
    new GenerateReferenceManifest().mainExit(args);
  }
}
