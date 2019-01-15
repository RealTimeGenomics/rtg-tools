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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.rtg.mode.DNA;
import com.rtg.mode.DnaUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.intervals.SequenceIdLocus;
import com.rtg.util.intervals.SequenceIdLocusComparator;
import com.rtg.util.intervals.SequenceIdLocusSimple;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.VcfHeader;

/**
 * Generate the variants for a population
 */
public abstract class PopulationVariantGenerator {

  private static final int MAX_RETRIES = 100;

  protected final VariantPositionGenerator mVariantPositionGenerator;

  /**
   * Interface for generating variant positions
   */
  public interface VariantPositionGenerator {
    /**
     * @return position at which next population variant should be generated
     * @throws IOException if an IO error occurs
     */
    SequenceIdLocus nextVariantPosition() throws IOException;
  }

  /**
   * Describes a population variant by position, allele and distribution
   */
  public static class PopulationVariant extends SequenceIdLocusSimple {
    byte[] mRef = null;
    byte[][] mAlleles = null; // nucleotides of alternate alleles
    double[] mDistribution = null; // alternate allele frequencies

    /**
     * Create a population variant
     * @param pos the start position of the variant
     */
    public PopulationVariant(SequenceIdLocus pos) {
      super(pos.getSequenceId(), pos.getStart(), pos.getEnd());
    }

    @Override
    public int getEnd() {
      return (mRef == null) ? super.getEnd() : getStart() + mRef.length;
    }

    VcfRecord toVcfRecord(SequencesReader reference) throws IOException {
      final DecimalFormat f = new DecimalFormat("#.###");
      //collapsePopulationVariant();
      final VcfRecord rec = new VcfRecord(reference.name(getSequenceId()), getStart(), DnaUtils.bytesToSequenceIncCG(mRef));
      rec.setQuality(VcfRecord.MISSING);
      rec.setId(VcfRecord.MISSING);
      for (final byte[] mAllele : mAlleles) {
        rec.addAltCall(DnaUtils.bytesToSequenceIncCG(mAllele));
      }
      final String[] mAFStr = new String[mDistribution.length];
      for (int i = 0; i < mDistribution.length; ++i) {
        mAFStr[i] = f.format(mDistribution[i]);
      }
      rec.addInfo(VcfUtils.INFO_ALLELE_FREQ, mAFStr);
      return rec;
    }

  }


  protected PopulationVariantGenerator(VariantPositionGenerator variantPositionGenerator) {
    mVariantPositionGenerator = variantPositionGenerator;
  }

  /**
   * generate next population variant, inserts and deletes should have a one reference base prefix
   * @return the next population variant, or null if there are no more
   * @throws IOException when an IO error occurs
   */
  abstract PopulationVariant nextPopulationVariant() throws IOException;

  /**
   * @return the list of variants for the population, sorted in chromosome order
   * @throws IOException if an IO error occurs
   */
  public List<PopulationVariant> generatePopulation() throws IOException {
    final TreeSet<PopulationVariant> ret = new TreeSet<>(new SequenceIdLocusComparator());
    PopulationVariant variant;
    do {
      int retry = 0;
      do {
        if (retry++ >= MAX_RETRIES) {
          throw new RuntimeException("Too many tries during variant generation");
        }
        variant = nextPopulationVariant();
      } while (variant != null && !checkValid(variant, ret));
      if (variant != null) {
        ret.add(variant);
      }
    } while (variant != null && needMoreVariants());
    return new ArrayList<>(ret);
  }

  /**
   * Determine if more variants should be generated. The default implementation attempts to generate as
   * many variants as possible.
   * @return true if more variants need to be generated.
   */
  protected boolean needMoreVariants() {
    return true;
  }

  /**
   * Writes out variants to VCF file
   * @param vcfOutput output file (should have gzip extension)
   * @param variants variants to output
   * @param reference reference sequences reader
   * @param seed the seed used during generation
   * @throws java.io.IOException if there is a problem during writing
   */
  public static void writeAsVcf(File vcfOutput, List<PopulationVariant> variants, SequencesReader reference, long seed) throws IOException {
    final VcfHeader header = new VcfHeader();
    header.addCommonHeader();
    header.addMetaInformationLine(VcfHeader.META_STRING + "SEED=" + seed);
    header.addReference(reference);
    header.addContigFields(reference);
    header.addInfoField(VcfUtils.INFO_ALLELE_FREQ, VcfUtils.INFO_ALLELE_FREQ_TYPE, VcfUtils.INFO_ALLELE_FREQ_NUM, VcfUtils.INFO_ALLELE_FREQ_DESC);
    try (VcfWriter writer = new VcfWriterFactory().zip(vcfOutput != null && FileUtils.isGzipFilename(vcfOutput)).make(header, vcfOutput)) {
      for (PopulationVariant var : variants) {
        final VcfRecord rec = var.toVcfRecord(reference);
        writer.write(rec);
      }
    }
  }

  /**
   * Test this potential variant to see if it is acceptable within the overall set.
   * @param var the variant to test
   * @param set the variants already accepted
   * @return true if the variant will be included in the generated set of variants
   */
  protected boolean checkValid(PopulationVariant var, NavigableSet<PopulationVariant> set) {
    //check against n's, overlapping with other pop variants etc.
    assert var.mAlleles.length >= 1;
    return validNs(var) && validPos(var, set);
  }

  private static boolean validPos(PopulationVariant var, NavigableSet<PopulationVariant> set) {
    final int candStart = var.getStart();
    final int candEnd = var.getEnd();
    final PopulationVariant floor = set.floor(var);
    final PopulationVariant higher = set.higher(var);
    if (floor != null && var.getSequenceId() == floor.getSequenceId() && floor.getEnd() > candStart) {
      return false;
    }
    if (higher != null && var.getSequenceId() == higher.getSequenceId() && candEnd > higher.getStart()) {
      return false;
    }
    return true;
  }

  private static boolean validNs(PopulationVariant var) {
    for (int i = 0; i < var.mRef.length; ++i) {
      if (var.mRef[i] != DNA.N.ordinal()) {
        return true;
      }
    }
    return false; //all N's
  }
}
