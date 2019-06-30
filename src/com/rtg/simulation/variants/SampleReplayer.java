/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.simulation.variants;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.DnaUtils;
import com.rtg.mode.SequenceType;
import com.rtg.reader.PrereadType;
import com.rtg.reader.SdfWriter;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SourceTemplateReadWriter;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.ReferenceSequence;
import com.rtg.reference.Sex;
import com.rtg.util.Constants;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VariantType;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.SampleField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Creates an SDF containing the genome corresponding to a sample, as specified by the
 * variants in the input VCF.
 *
 */
@TestClass(value = {"com.rtg.simulation.variants.SampleSimulatorTest", "com.rtg.simulation.variants.SampleReplayerTest"})
public class SampleReplayer {

  protected final SequencesReader mReference;
  private final byte[] mBuffer = new byte[FileUtils.BUFFER_SIZE];

  /**
   * @param reference input reference data
   */
  public SampleReplayer(SequencesReader reference) {
    mReference = reference;
  }

  /**
   * create a genome for a sample using the genotype specified for that sample
   * in the supplied VCF file.
   * @param sampleVcf indexed VCF file containing the genotype for the sample
   * @param outputDir output SDF directory
   * @param sample name of the sample to generate the genome for
   * @throws java.io.IOException if an IO error occurs
   */
  public void replaySample(File sampleVcf, File outputDir, String sample) throws IOException {
    final VcfHeader header = VcfUtils.getHeader(sampleVcf);
    final int sampleNum = header.getSampleIndex(sample);
    if (sampleNum == -1) {
      throw new NoTalkbackSlimException("sample '" + sample + "' not found");
    }
    Sex sex = Sex.EITHER;
    for (SampleField sampleField : header.getSampleLines()) {
      if (sampleField.getId().equals(sample)) {
        final Sex sexField = sampleField.getSex();
        if (sexField != null) {
          sex = sexField;
        }
      }
    }
    //System.err.println("setting sex to: " + sex);
    final StringBuilder referenceThing = new StringBuilder();
    referenceThing.append("version 1").append(StringUtils.LS);
    referenceThing.append("either\tdef\thaploid\tlinear").append(StringUtils.LS); // This comes into play if someone needs to map to this SDF without specifying the same sex
    try (SdfWriter sdf = new SdfWriter(outputDir, Constants.MAX_FILE_SIZE, PrereadType.UNKNOWN, false, true, true, SequenceType.DNA)) {
      sdf.setCommandLine(CommandLine.getCommandLine());
      final ReferenceGenome rg = new ReferenceGenome(mReference, sex, ReferencePloidy.AUTO);
      for (long i = 0; i < mReference.numberSequences(); ++i) {
        final ReferenceSequence refSeq = rg.sequence(mReference.name(i));
        final int count = refSeq.ploidy().count() >= 0 ? refSeq.ploidy().count() : 1; //effectively treats polyploid as haploid
        final String circleString = refSeq.isLinear() ? "linear" : "circular";
        final String ploidyStr = refSeq.ploidy().name().toLowerCase(Locale.ROOT);
        final String sexStr = sex.toString().toLowerCase(Locale.ROOT);
        if (count == 2) {
          referenceThing.append(sexStr).append("\tseq\t").append(deriveName(refSeq.name(), 0, count)).append("\thaploid\t").append(circleString).append("\t").append(deriveName(refSeq.name(), 1, count)).append(StringUtils.LS);
          referenceThing.append(sexStr).append("\tseq\t").append(deriveName(refSeq.name(), 1, count)).append("\thaploid\t").append(circleString).append("\t").append(deriveName(refSeq.name(), 0, count)).append(StringUtils.LS);
        } else if (count == 1) {
          referenceThing.append(sexStr).append("\tseq\t").append(deriveName(refSeq.name(), 0, count)).append("\t").append(ploidyStr).append("\t").append(circleString);
          if (refSeq.haploidComplementName() != null) {
            referenceThing.append("\t").append(refSeq.haploidComplementName());
          }
          referenceThing.append(StringUtils.LS);
        }
        replaySequence(sampleVcf, sdf, count, i, sampleNum, header);
      }
    }
    final File referenceFileName = new File(outputDir, ReferenceGenome.REFERENCE_FILE);
    FileUtils.stringToFile(referenceThing.toString(), referenceFileName);
    SourceTemplateReadWriter.writeMutationMappingFile(outputDir, mReference.getSdfId());
  }

  private static String deriveName(String seqName, int copyIteration, int maxCopies) {
    if (maxCopies > 1) {
      return seqName + "_" + copyIteration;
    }
    return seqName;
  }

  // Applies mutations to the specified sequence and writes to the SDF
  private void replaySequence(File sampleVcf, SdfWriter output, int count, long sequenceId, int sampleNum, VcfHeader header) throws IOException {
    for (int i = 0; i < count; ++i) {
      final String name = mReference.name(sequenceId);
      output.startSequence(deriveName(name, i, count));
      try (VcfReader vcfReader = VcfReader.openVcfReader(sampleVcf, new RegionRestriction(name))) {
        int currentPos = 0;
        while (vcfReader.hasNext()) {
          final VcfRecord vcf = vcfReader.next();
          final String gtStr = vcf.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sampleNum);
          final int[] gtInt = VcfUtils.splitGt(gtStr);
          if (gtInt.length != count) {
            throw new NoTalkbackSlimException("Genotype with incorrect ploidy for sample: " + header.getSampleNames().get(sampleNum) + " at " + name + ":" + vcf.getOneBasedStart() + " exp: " + count + " was : " + gtInt.length);
          }
          final int thisPosition = vcf.getStart();
          final int refEndPosition = vcf.getStart() + vcf.getRefCall().length();
          if (gtInt[i] == 0) {
            if (currentPos > thisPosition) {
              throw new NoTalkbackSlimException("Encountered ref allele that is overlapped by previous long variant (may be representable using an ALT of \"*\"), currently at " + name + ":" + (thisPosition + 1) + " already written to " + name + ":" + (currentPos + 1));
            }
          } else if (gtInt[i] > 0) {
            final String allele = vcf.getAltCalls().get(gtInt[i] - 1);
            final VariantType svType = VariantType.getSymbolicAlleleType(allele);
            if (svType == VariantType.SV_MISSING) {
              // Check that this site is in fact covered by an earlier variant
              if (currentPos <= thisPosition) {
                throw new NoTalkbackSlimException("Encountered deletion allele \"*\", but site is not covered by an earlier deletion, currently at " + name + ":" + (thisPosition + 1));
              }
            } else if (svType != null) {
              throw new NoTalkbackSlimException("Symbolic variants are not supported, currently at " + name + ":" + (thisPosition + 1));
            } else {
              writeRefToPosition(output, sequenceId, currentPos, thisPosition);
              final byte[] alleleBytes = DnaUtils.encodeArray(allele.getBytes());
              output.write(alleleBytes, null, alleleBytes.length);
              currentPos = refEndPosition;
            }
          }
        }
        writeRefToPosition(output, sequenceId, currentPos, mReference.length(sequenceId));
        output.endSequence();
      }
    }
  }

  private void writeRefToPosition(SdfWriter output, long sequenceId, int currentPos, int endPos) throws IOException {
    if (currentPos > endPos) {
      //System.err.println("Overlapping variants not supported, currently at " + mReference.currentName() + ":" + (endPos + 1) + " already written to " + mReference.currentName() + ":" + (currentPos + 1));
      final String name = mReference.name(sequenceId);
      throw new NoTalkbackSlimException("Overlapping variants not supported, currently at " + name + ":" + (endPos + 1) + " already written to " + name + ":" + (currentPos + 1));
    }
    int pos = currentPos;
    while (pos < endPos) {
      final int length = mReference.read(sequenceId, mBuffer, pos, Math.min(mBuffer.length, endPos - pos));
      output.write(mBuffer, null, length);
      pos += length;
    }
  }
}
