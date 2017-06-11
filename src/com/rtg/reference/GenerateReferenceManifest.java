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

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.SDF;
import static com.rtg.launcher.CommonFlags.STRING;
import static com.rtg.launcher.CommonFlags.TEMPLATE_FLAG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.rtg.util.cli.Validator;
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
  private static final String REFERENCE_ID_FLAG = "reference-id";

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
   * @param refTxtName short name for description file
   * @param writer writer to write output reference manifest
   * @param description description for the manifest
   * @throws IOException if an IO error occurs
   */
  public static void generateManifest(File sdfDir, String refTxtName, Writer writer, String description) throws IOException {
    final String descUse = description != null ? description : ("Generated from " + sdfDir);
    final String refTxtResource = "com/rtg/reference/resources/" + refTxtName + ".txt";
    try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReader(sdfDir)) {
      if (reader.hasNames() && reader.numberSequences() <= ReferenceManifest.MAX_SEQUENCE_NAMES) {
        try (InputStream is = Resources.getResourceAsStream(refTxtResource)) {
          if (is == null) {
            throw new IOException("Expected to find existing resource at: " + refTxtResource);
          }
          try (InputStreamReader refReader = new InputStreamReader(is)) {
            final Map<String, Long> names = ReaderUtils.getSequenceNameMap(reader);
            final ReferenceGenome ref = new ReferenceGenome(reader, refReader, ReferenceGenome.SEX_ALL);
            writer.write("#ref-manifest v2.0\n");
            writer.write("@desc\t" + descUse + "\n");
            writer.write("@source\t" + refTxtResource + "\n");
            writer.write("@checks\t" + Arrays.stream(CHECKS).map(t -> t.name().toLowerCase(Locale.getDefault())).collect(Collectors.joining("\t")) + "\n");
            final GetCheckValue[] getters = new GetCheckValue[CHECKS.length - 1];
            for (int i = 0; i < CHECKS.length - 1; ++i) {
              getters[i] = checkValueFactory(CHECKS[i + 1]);
            }
            for (ReferenceSequence sequence : ref.sequences()) {
              if (sequence.isSpecified()) {
                final long seqId = names.get(sequence.name());
                writer.write(sequence.name());
                for (int i = 0; i < getters.length; ++i) {
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
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Creates a manifest that allows a reference.txt to be auto-installed during format. The reference configuration file should already exist in the references resource directory");
    CommonFlags.initForce(mFlags);
    mFlags.registerRequired('t', TEMPLATE_FLAG, File.class, SDF, "reference SDF to generate manifest for").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('r', REFERENCE_ID_FLAG, String.class, STRING, "short identifier of reference").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional('d', DESCRIPTION_FLAG, String.class, STRING, "short text description for manifest");
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, FILE, "resource directory into which manifest is written, or - for standard out").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.setValidator(new Validator() {
      @Override
      public boolean isValid(CFlags flags) {
        final File outdir = (File) flags.getValue(OUTPUT_FLAG);
        if (!FileUtils.isStdio(outdir)) {
          if (!outdir.exists() || !outdir.isDirectory()) {
            flags.setParseMessage("Resource directory is not an existing directory");
            return false;
          }
          final String id = (String) flags.getValue(REFERENCE_ID_FLAG);
          if (!CommonFlags.validateInputFile(flags, new File(outdir, id + ".txt"))) {
            return false;
          }
          if (!CommonFlags.validateOutputFile(flags, getManifestFile(outdir, id))) {
            return false;
          }
        }
        return true;
      }
    });
  }

  private static File getManifestFile(File out, String refId) {
    return FileUtils.isStdio(out) ? out : new File(out, refId + ".manifest");
  }

  private static Writer getFileWriter(File out, String refId) throws IOException {
    return new OutputStreamWriter(FileUtils.createOutputStream(getManifestFile(out, refId)));
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File outFile = (File) mFlags.getValue(OUTPUT_FLAG);
    final String refId = (String) mFlags.getValue(REFERENCE_ID_FLAG);
    try (Writer writer = getFileWriter(outFile, refId)) {
      generateManifest((File) mFlags.getValue(TEMPLATE_FLAG), refId, writer, (String) mFlags.getValue(DESCRIPTION_FLAG));
    }
    if (!FileUtils.isStdio(outFile)) {
      System.out.println("Done, don't forget to update the manifest.list if necessary");
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
