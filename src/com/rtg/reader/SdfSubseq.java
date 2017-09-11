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

import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.mode.DNA;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.mode.SequenceType;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;

/**
 * Class returns a subsequence of the residues in the given sequence.
 *
 */
public final class SdfSubseq extends AbstractCli {

  /** System dependent line separator, as byte array. */
  private static final byte[] LS_BYTES = System.lineSeparator().getBytes();

  private static final String FASTA_FLAG = "fasta";
  private static final String FASTQ_FLAG = "fastq";
  private static final String INPUT_FLAG = "input";
  private static final String SEQ_ID_FLAG = "sequence-id";
  private static final String REVERSE_FLAG = "reverse-complement";
  private static final String PRESERVE_FLAG = "Xpreserve-coordinates";

  private static final Validator VALIDATOR = new Validator() {

    @Override
    public boolean isValid(final CFlags flags) {
      if (!flags.checkNand(FASTA_FLAG, FASTQ_FLAG)) {
        return false;
      }
      if (!flags.checkNand(PRESERVE_FLAG, REVERSE_FLAG)) {
        return false;
      }
      for (Object o : flags.getAnonymousValues(0)) {
        final String restrictionString = (String) o;
        if (!RegionRestriction.validateRegion(restrictionString)) {
          flags.setParseMessage("The region value \"" + restrictionString + "\" is malformed.");
          return false;
        }
        final RegionRestriction restriction = new RegionRestriction(restrictionString);
        if (flags.isSet(SEQ_ID_FLAG)) {
          long seqId;
          try {
            seqId = Long.parseLong(restriction.getSequenceName());
          } catch (NumberFormatException e) {
            seqId = -1;
          }
          if (seqId < 0) {
            flags.setParseMessage("When --" + SEQ_ID_FLAG + " is set the <sequence_name> of the region \"" + restrictionString + "\" must be an integer greater than or equal to 0.");
            return false;
          }
        }
      }
      final File inputFile = (File) flags.getValue(INPUT_FLAG);
      if (ReaderUtils.isPairedEndDirectory(inputFile)) {
        flags.setParseMessage("Paired-end SDF not supported.");
        return false;
      }
      return true;
    }
  };

  private byte[] mCodeToBytes = null;
  private Map<String, Long> mNames = null;
  private final AbstractSdfWriter.SequenceNameHandler mHandler = new AbstractSdfWriter.SequenceNameHandler();

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription("Prints a subsequence of a given sequence in an SDF.");
    mFlags.registerRequired('i', INPUT_FLAG, File.class, CommonFlags.SDF, "input SDF").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired(String.class, CommonFlags.STRING, "the range to display. The format is one of <sequence_name>, <sequence_name>:start-end or <sequence_name>:start+length")
      .setCategory(FILTERING)
      .setMaxCount(Integer.MAX_VALUE);
    mFlags.registerOptional('r', REVERSE_FLAG, "if set, output in reverse complement").setCategory(UTILITY);
    mFlags.registerOptional('f', FASTA_FLAG, "if set, output in FASTA format").setCategory(UTILITY);
    mFlags.registerOptional('q', FASTQ_FLAG, "if set, output in FASTQ format").setCategory(UTILITY);
    mFlags.registerOptional('I', SEQ_ID_FLAG, "if set, use sequence id instead of sequence name in region (0-based)").setCategory(FILTERING);

    mFlags.registerOptional(PRESERVE_FLAG, "if set, pad start of sequence with N's to ensure sequence coordinates are preserved").setCategory(UTILITY);

    mFlags.setValidator(VALIDATOR);
  }

  @Override
  public String moduleName() {
    return "sdfsubseq";
  }

  @Override
  public String description() {
    return "extract a subsequence from an SDF as text";
  }

  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final boolean reverseComplement = mFlags.isSet(REVERSE_FLAG);
    try (SequencesReader reader = SequencesReaderFactory.createDefaultSequencesReaderCheckEmpty((File) mFlags.getValue(INPUT_FLAG))) {
      if (!mFlags.isSet(SEQ_ID_FLAG) && !reader.hasNames()) {
        throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "The input SDF does not have name data.");
      }
      if (mFlags.isSet(FASTQ_FLAG) && !reader.hasQualityData()) {
        throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "The input SDF does not have quality data.");
      }
      mCodeToBytes = getByteMapping(reader.type(), reverseComplement);

      if (!mFlags.isSet(SEQ_ID_FLAG)) {
        mNames = ReaderUtils.getSequenceNameMap(reader);
      }

      for (Object o : mFlags.getAnonymousValues(0)) {
        final RegionRestriction restriction = new RegionRestriction((String) o);
        final int result = extractSubseq(reader, restriction, reverseComplement, out, err);
        if (result != 0) {
          return result;
        }
      }
      out.flush();
    }
    return 0;
  }

  private int extractSubseq(final SequencesReader reader, final RegionRestriction restriction, final boolean reverseComplement, final OutputStream out, final PrintStream err) throws IOException {
    final int start = restriction.getStart() == RegionRestriction.MISSING ? 0 : restriction.getStart();
    final long sequenceId;
    if (!mFlags.isSet(SEQ_ID_FLAG)) {
      final String sequence = restriction.getSequenceName();
      final Long seqId = mNames.get(mHandler.handleSequenceName(sequence).label());
      if (seqId == null) {
        err.println("The sequence \"" + sequence + "\" could not be found.");
        return 1;
      }
      sequenceId = seqId;
    } else {
      sequenceId = Long.parseLong(restriction.getSequenceName());
    }
    if (sequenceId < 0 || sequenceId >= reader.numberSequences()) {
      err.println("The sequence id " + sequenceId + " is out of range, must be from 0 to " + (reader.numberSequences() - 1) + ".");
      return 1;
    }
    final int seqlength = reader.length(sequenceId);
    final int endpos = restriction.getEnd() == RegionRestriction.MISSING ? seqlength : restriction.getEnd(); // Convert from 1-based inclusive to 0-based exclusive
    final int length = endpos - start;
    if (start > seqlength) {
      err.println("Supplied start position \"" + (start + 1) + "\" reads past sequence end.");
      return 1;
    } else if (endpos > seqlength) {
      err.println("Supplied end position \"" + endpos + "\" reads past sequence end.");
      return 1;
    }
    final boolean isCoordsAltered = length < seqlength && !mFlags.isSet(PRESERVE_FLAG);
    char sequenceNameIdentifier = '>';
    if (mFlags.isSet(FASTQ_FLAG)) {
      sequenceNameIdentifier = '@';
    }
    if (mFlags.isSet(FASTA_FLAG) || mFlags.isSet(FASTQ_FLAG)) {
      final String name;
      if (reader.hasNames()) {
        name = isCoordsAltered ? reader.name(sequenceId) : reader.fullName(sequenceId);
      } else {
        name = String.valueOf(sequenceId);
      }
      out.write((sequenceNameIdentifier + name).getBytes());
      final String coords = isCoordsAltered ? "[" + (start + 1) + "," + (start + length) + "]" : "";
      out.write(coords.getBytes());
      if (reverseComplement) {
        out.write("-rc".getBytes());
      }
      out.write(LS_BYTES);
    }
    byte[] buff = new byte[length];
    reader.read(sequenceId, buff, start, length);
    if (reverseComplement) {
      for (int i = length - 1; i >= 0; --i) {
        out.write(mCodeToBytes[buff[i]]);
      }
    } else {
      if (mFlags.isSet(PRESERVE_FLAG)) {
        for (int i = 0; i < start; ++i) {
          out.write(mCodeToBytes[0]);
        }
      }
      for (int i = 0; i < length; ++i) {
        out.write(mCodeToBytes[buff[i]]);
      }
    }
    out.write(LS_BYTES);

    if (mFlags.isSet(FASTQ_FLAG)) {
      out.write('+');
      out.write(LS_BYTES);
      buff = new byte[length];
      reader.readQuality(sequenceId, buff, start, length);
      if (reverseComplement) {
        for (int i = length - 1; i >= 0; --i) {
          out.write(buff[i] + 33);
        }
      } else {
        if (mFlags.isSet(PRESERVE_FLAG)) {
          for (int i = 0; i < start; ++i) {
            out.write(33);
          }
        }
        for (int i = 0; i < length; ++i) {
          out.write(buff[i] + 33);
        }
      }
      out.write(LS_BYTES);
    }
    return 0;
  }

  static byte[] getByteMapping(SequenceType type, boolean reverseComplement) {
    if (type == SequenceType.DNA) {
      if (reverseComplement) {
        final byte[] dnaCodes = new DNAFastaSymbolTable().getOrdinalToAsciiTable();
        final byte[] compCodes = new byte[dnaCodes.length];
        for (final DNA d : DNA.values()) {
          compCodes[d.ordinal()] = dnaCodes[d.complement().ordinal()];
        }
        return compCodes;
      } else {
        return new DNAFastaSymbolTable().getOrdinalToAsciiTable();
      }
    } else {
      if (reverseComplement) {
        throw new NoTalkbackSlimException("Reverse complement cannot be used with protein SDFs.");
      }
      return new ProteinFastaSymbolTable().getOrdinalToAsciiTable();
    }
  }

}
